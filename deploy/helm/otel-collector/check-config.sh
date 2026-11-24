#!/usr/bin/env bash
# Confere a configuracao do coletor com a imagem real (ADOT, a mesma do chart), sem AWS: renderiza o chart, troca os
# enderecos da AWS por um servidor falso local, sobe o coletor com credenciais ficticias e manda OTLP de verdade.
# Afirma: o coletor sobe (componentes e chaves validos), o filtro derruba o que nao e do painel, o EMF sai com o
# namespace e as dimensoes declaradas, o log vai para /capzzpay/<service.name> e o trace e assinado para o servico xray.
# Exige docker (imagem baixada do ECR Publico), helm, curl e python3. OTEL_CONFIG_CHECK=skip pula (so fora do CI).
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
if [ "${OTEL_CONFIG_CHECK:-}" = skip ]; then echo "AVISO: check-config do coletor PULADO (OTEL_CONFIG_CHECK=skip)" >&2; exit 0; fi
command -v docker >/dev/null || { echo "ERRO: docker ausente (OTEL_CONFIG_CHECK=skip pula)" >&2; exit 1; }

image="$(awk '/^  repository:/{r=$2} /^  tag:/{t=$2} END{print r ":" t}' "$here/values.yaml")"
tmp="$(mktemp -d)"
name="otel-check-$$"
port=$((20000 + $$ % 20000))
fake_pid=""
cleanup() {
  docker rm -f "$name" >/dev/null 2>&1 || true
  if [ -n "$fake_pid" ]; then kill "$fake_pid" 2>/dev/null || true; wait "$fake_pid" 2>/dev/null || true; fi
  rm -rf "$tmp"
}
trap cleanup EXIT
fail() { echo "FALHA: $*" >&2; docker logs "$name" 2>&1 | grep -v attn | tail -20 >&2 || true; exit 1; }

# Config renderizada, com os enderecos da AWS trocados pelo servidor falso.
helm template oc "$here" | awk '/config.yaml: \|/{f=1;next} f&&/^---/{exit} f' | sed 's/^    //' > "$tmp/rendered.yaml"
[ -s "$tmp/rendered.yaml" ] || fail "config do coletor nao renderizou"
fake="http://host.docker.internal:$port"
python3 -I - "$tmp/rendered.yaml" "$tmp/config.yaml" "$fake" <<'PY'
import sys
src, dst, fake = sys.argv[1:4]
t = open(src).read()
a = "https://xray.sa-east-1.amazonaws.com/v1/traces"
assert a in t, "endpoint do X-Ray fora do lugar"
t = t.replace(a, fake + "/v1/traces")
for exp in ("  awsemf:\n", "  awscloudwatchlogs:\n"):
    assert exp in t, exp
    t = t.replace(exp, exp + f"    endpoint: {fake}\n")
open(dst, "w").write(t)
PY

cat > "$tmp/fake.py" <<'PY'
import gzip, http.server, sys
log = open(sys.argv[2], "a")
class H(http.server.BaseHTTPRequestHandler):
    def do_POST(self):
        b = self.rfile.read(int(self.headers.get("content-length", 0)))
        if self.headers.get("content-encoding") == "gzip":
            b = gzip.decompress(b)
        auth = self.headers.get("authorization", "")
        scope = auth.split("Credential=")[1].split(",")[0] if "Credential=" in auth else "-"
        log.write("REQ %s %s %s\n" % (self.path, self.headers.get("x-amz-target", "-"), scope))
        if self.path.startswith("/v1/traces"):
            log.write("BODY <trace %d bytes>\n" % len(b))
        else:
            log.write("BODY %s\n" % b.decode("utf-8", "replace").replace("\n", " "))
        log.flush()
        self.send_response(200); self.send_header("content-type", "application/x-amz-json-1.1"); self.end_headers(); self.wfile.write(b"{}")
    def log_message(self, *a): pass
http.server.HTTPServer(("0.0.0.0", int(sys.argv[1])), H).serve_forever()
PY
: > "$tmp/fake.log"
python3 -I "$tmp/fake.py" "$port" "$tmp/fake.log" &
fake_pid=$!

docker run -d --name "$name" --add-host host.docker.internal:host-gateway -p "127.0.0.1:0:4318" -v "$tmp:/c:ro" \
  -e AWS_ACCESS_KEY_ID=teste -e AWS_SECRET_ACCESS_KEY=teste -e AWS_REGION=sa-east-1 \
  "$image" --config=file:/c/config.yaml >/dev/null
host_port=$(docker port "$name" 4318/tcp | head -1 | sed 's/.*://')

ready=""
for _ in $(seq 1 30); do
  if docker logs "$name" 2>&1 | grep -q 'Everything is ready'; then ready=1; break; fi
  [ "$(docker inspect -f '{{.State.Running}}' "$name")" = true ] || fail "coletor parou ao subir (configuracao invalida)"
  sleep 1
done
[ -n "$ready" ] || fail "coletor nao ficou pronto em 30 s"
echo "ok: coletor subiu com a configuracao renderizada"

now=$(python3 -c 'import time; print(int(time.time() * 1e9))')
res='"resource":{"attributes":[{"key":"service.name","value":{"stringValue":"transfers"}}]}'
post() { curl -fsS -o /dev/null -H 'content-type: application/json' -d "$2" "http://127.0.0.1:$host_port/v1/$1"; }
post traces '{"resourceSpans":[{'"$res"',"scopeSpans":[{"spans":[{"traceId":"4efaaf4d1e8720b39541901950019ee5","spanId":"0123456789abcdef","name":"Debit","kind":1,"startTimeUnixNano":"'"$now"'","endTimeUnixNano":"'"$((now + 1000000))"'","attributes":[{"key":"transfer.id","value":{"stringValue":"t-1"}}]}]}]}]}'
# Duas leituras do contador (o awsemf descarta a primeira: so exporta a diferenca) e um metrico fora do painel.
for i in 1 2; do
  post metrics '{"resourceMetrics":[{'"$res"',"scopeMetrics":[{"metrics":[
    {"name":"transfers.finished","sum":{"aggregationTemporality":2,"isMonotonic":true,"dataPoints":[
      {"asInt":"'"$((i * 3))"'","timeUnixNano":"'"$now"'","attributes":[{"key":"status","value":{"stringValue":"FAILED"}},{"key":"reason","value":{"stringValue":"daily-limit-exceeded"}}]}]}},
    {"name":"outbox.pending","gauge":{"dataPoints":[{"asInt":"4","timeUnixNano":"'"$now"'"}]}},
    {"name":"jvm.memory.used","gauge":{"dataPoints":[{"asInt":"9","timeUnixNano":"'"$now"'"}]}}]}]}]}'
  now=$((now + 60000000000))
  sleep 1
done
post logs '{"resourceLogs":[{'"$res"',"scopeLogs":[{"logRecords":[{"timeUnixNano":"'"$now"'","severityText":"INFO","body":{"stringValue":"hello"}}]}]}]}'

for _ in $(seq 1 30); do
  grep -q 'logGroupName":"/capzzpay/transfers' "$tmp/fake.log" && grep -q '"transfers.finished":3' "$tmp/fake.log" && grep -q '^REQ /v1/traces' "$tmp/fake.log" && break
  sleep 1
done
has() { grep -qF -- "$1" "$tmp/fake.log" || fail "$2"; }
has 'REQ /v1/traces - ' 'trace nao chegou ao endpoint OTLP do X-Ray'
grep -E '^REQ /v1/traces' "$tmp/fake.log" | grep -q '/sa-east-1/xray/aws4_request' || fail "trace nao foi assinado para o servico xray em sa-east-1"
has '\"Namespace\":\"capzzpay\"' 'EMF sem o namespace capzzpay'
has '\"Name\":\"transfers.finished\"' 'EMF sem transfers.finished'
has '\"Dimensions\":[[\"status\"],[\"reason\",\"status\"]]' 'transfers.finished sem as dimensoes status e status+reason'
has '\"Name\":\"outbox.pending\"' 'EMF sem outbox.pending'
has '\"Dimensions\":[[\"service.name\"]]' 'outbox.pending sem a dimensao service.name'
has 'logGroupName":"/capzzpay/transfers"' 'log nao foi para /capzzpay/transfers'
if grep -qF 'jvm.memory.used' "$tmp/fake.log"; then fail "metrica fora do painel (jvm.memory.used) passou pelo filtro"; fi
echo "ok: trace assinado para o xray, EMF com namespace e dimensoes, log em /capzzpay/transfers, filtro derrubou jvm.memory.used"

# Mutante: uma chave invalida na config tem de derrubar o coletor (o teste nao pode passar de primeira por acaso).
sed 's/check_interval: 1s/check_interval_invalida: 1s/' "$tmp/config.yaml" > "$tmp/bad.yaml"
mkdir "$tmp/bad" && mv "$tmp/bad.yaml" "$tmp/bad/config.yaml"
if docker run --rm -v "$tmp/bad:/c:ro" -e AWS_ACCESS_KEY_ID=t -e AWS_SECRET_ACCESS_KEY=t "$image" --config=file:/c/config.yaml >/dev/null 2>&1; then
  fail "configuracao invalida foi aceita"
fi
echo "ok: configuracao invalida recusada"
echo "check-config do coletor: ok"
