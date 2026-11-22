#!/usr/bin/env python3
"""Compara o Terraform (modules/messaging) com os recursos locais: scripts do LocalStack e tabela do DynamoDB Local.

Le infra/local/localstack/init/ready.d/*.sh com um interpretador minimo do bash que os scripts usam
(atribuicoes, `$(...)` com o CLI da AWS, a funcao `create_consumer_queue`, comentarios ignorados) e
junta todas as chamadas de todos os scripts: cada `create-topic`, `create-queue` e `subscribe` e conferido
sozinho, sem que o ultimo script a criar um recurso esconda o que os anteriores criaram.

Confere, contra modules/messaging/resources.json e main.tf:
- nomes de topicos, filas, DLQs e assinaturas (topico -> fila);
- topicos com FifoTopic=true e filas com FifoQueue=true, sempre com ContentBasedDeduplication=false;
- filas de consumo com VisibilityTimeout e RedrivePolicy (DLQ e maxReceiveCount) nos atributos do create-queue;
- assinaturas com RawMessageDelivery=true;
- a tabela DynamoDB do extrato (chaves, tipos e cobranca), definida em infra/local/dynamodb/init/statement-table.json;
  ela nao pode voltar para os scripts do LocalStack, que nao tem persistencia (#234).
As filas *-inspection.fifo sao so locais (sem consumidor) e ficam de fora da comparacao de nomes.

Uso: check-messaging.py [ready.d] [resources.json] [main.tf] [statement-table.json]
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
READY = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "infra/local/localstack/init/ready.d"
SPEC = Path(sys.argv[2]) if len(sys.argv) > 2 else ROOT / "infra/terraform/modules/messaging/resources.json"
MAIN_TF = Path(sys.argv[3]) if len(sys.argv) > 3 else SPEC.parent / "main.tf"
TABLE = Path(sys.argv[4]) if len(sys.argv) > 4 else ROOT / "infra/local/dynamodb/init/statement-table.json"

IGNORED = ("set", "export", "echo", "if", "fi", "then", "else", "local")


class ScriptError(Exception):
    pass


def logical_lines(text):
    """Linhas do script sem comentarios, com as continuacoes (barra no fim) juntadas."""
    out, current = [], ""
    for raw in text.splitlines():
        line = strip_comment(raw)
        if line.rstrip().endswith("\\"):
            current += line.rstrip()[:-1] + " "
            continue
        current += line
        if current.strip():
            out.append(current.strip())
        current = ""
    if current.strip():
        out.append(current.strip())
    return out


def strip_comment(line):
    """Tira o comentario (`#` fora de aspas, no inicio de palavra) da linha."""
    quote, i = None, 0
    while i < len(line):
        ch = line[i]
        if quote == "'":
            if ch == "'":
                quote = None
        elif ch == "\\":
            i += 1
        elif quote == '"':
            if ch == '"':
                quote = None
        elif ch in "'\"":
            quote = ch
        elif ch == "#" and (i == 0 or line[i - 1].isspace()):
            return line[:i]
        i += 1
    return line


def expand(name, env):
    m = re.fullmatch(r"(\w+):-(.*)", name)
    if m:
        value = env.get(m.group(1))
        return value if value else m.group(2)
    if name not in env:
        raise ScriptError(f"variavel ${name} sem valor conhecido")
    return env[name]


def words(text, env):
    """Divide em palavras como o bash: aspas simples e duplas, escapes e expansao de $var e ${var:-padrao}."""
    out, word, i, quote, has_word = [], "", 0, None, False
    while i < len(text):
        ch = text[i]
        if quote == "'":
            if ch == "'":
                quote = None
            else:
                word += ch
            i += 1
            continue
        if ch == "\\":
            nxt = text[i + 1] if i + 1 < len(text) else ""
            if quote == '"' and nxt not in '$`"\\':
                word += ch
            else:
                word += nxt
            i += 2
            has_word = True
            continue
        if ch == "$":
            m = re.match(r"\$\{([^}]*)\}|\$(\w+)", text[i:])
            if not m:
                raise ScriptError(f"expansao nao suportada em: {text}")
            word += expand(m.group(1) or m.group(2), env)
            i += m.end()
            has_word = True
            continue
        if quote == '"':
            if ch == '"':
                quote = None
            else:
                word += ch
            i += 1
            continue
        if ch in "'\"":
            quote = ch
            has_word = True
        elif ch.isspace():
            if has_word:
                out.append(word)
            word, has_word = "", False
        else:
            word += ch
            has_word = True
        i += 1
    if quote:
        raise ScriptError(f"aspas sem fechar em: {text}")
    if has_word:
        out.append(word)
    # Redirecionamentos nao mudam o recurso criado.
    clean, skip = [], False
    for w in out:
        if skip:
            skip = False
            continue
        if w in (">", ">>"):
            skip = True
            continue
        if re.fullmatch(r"\d?>&\d|\d?>/dev/null", w):
            continue
        clean.append(w)
    return clean


def options(args):
    """--opcao valor [valor...] -> {opcao: [valores]}."""
    opts, key = {}, None
    for a in args:
        if a.startswith("--"):
            key = a[2:]
            opts.setdefault(key, [])
        elif key is None:
            raise ScriptError(f"argumento solto: {a}")
        else:
            opts[key].append(a)
    return opts


def one(opts, key, where):
    values = opts.get(key)
    if not values or len(values) != 1:
        raise ScriptError(f"{where}: esperava um valor em --{key}")
    return values[0]


def attributes(value):
    """Atributos do CLI: JSON ou o atalho Chave=Valor,Chave=Valor."""
    if value is None:
        return {}
    if value.lstrip().startswith("{"):
        return {k: str(v) for k, v in json.loads(value).items()}
    attrs = {}
    for pair in value.split(","):
        k, _, v = pair.partition("=")
        attrs[k] = v
    return attrs


class Local:
    """Recursos que os scripts criam, sem perder nenhuma chamada."""

    def __init__(self):
        self.topics = []  # (script, nome, atributos)
        self.queues = []  # (script, nome, atributos)
        self.subs = []  # (script, topico, fila, atributos)

    def run(self, script, lines, env, functions):
        """Executa as linhas; devolve o valor do ultimo comando (o que `$(...)` captura)."""
        i, last = 0, ""
        while i < len(lines):
            line = lines[i]
            m = re.fullmatch(r"(\w+)\(\)\s*\{", line)
            if m:
                body, depth = [], 1
                i += 1
                while depth:
                    if i >= len(lines):
                        raise ScriptError(f"funcao {m.group(1)} sem fechar")
                    if lines[i].endswith("{"):
                        depth += 1
                    if lines[i] == "}":
                        depth -= 1
                        if not depth:
                            break
                    body.append(lines[i])
                    i += 1
                if m.group(1) != "aws_local":
                    functions[m.group(1)] = body
                i += 1
                continue
            last = self.statement(script, line, env, functions)
            i += 1
        return last

    def statement(self, script, line, env, functions):
        if line.startswith("if "):
            raise ScriptError(f"{script}: if nao suportado: {line}")
        m = re.fullmatch(r"(\w+)=\$\((.*)\)", line)
        if m:
            env[m.group(1)] = self.command(script, words(m.group(2), env), env, functions)
            return ""
        if re.fullmatch(r"(local\s+)?\w+=.*", line):
            body = line[len("local "):] if line.startswith("local ") else line
            for w in words(body, env):
                name, _, value = w.partition("=")
                env[name] = value
            return ""
        ws = words(line, env)
        if not ws or ws[0] in IGNORED:
            return ""
        return self.command(script, ws, env, functions)

    def command(self, script, ws, env, functions):
        head = ws[0]
        if head != "aws_local" and head in functions:
            local_env = dict(env)
            for n, arg in enumerate(ws[1:], start=1):
                local_env[str(n)] = arg
            return self.run(script, functions[head], local_env, functions)
        if head != "aws_local":
            raise ScriptError(f"{script}: comando nao reconhecido: {' '.join(ws)}")
        service, op, opts = ws[1], ws[2], options(ws[3:])
        where = f"{script}: {service} {op}"
        if (service, op) == ("sns", "create-topic"):
            name = one(opts, "name", where)
            self.topics.append((script, name, attributes((opts.get("attributes") or [None])[0])))
            return f"topic:{name}"
        if (service, op) == ("sqs", "create-queue"):
            name = one(opts, "queue-name", where)
            self.queues.append((script, name, attributes((opts.get("attributes") or [None])[0])))
            return f"queue:{name}"
        if (service, op) == ("sqs", "get-queue-attributes"):
            return one(opts, "queue-url", where)
        if (service, op) == ("sns", "subscribe"):
            topic, endpoint = one(opts, "topic-arn", where), one(opts, "notification-endpoint", where)
            if not topic.startswith("topic:") or not endpoint.startswith("queue:"):
                raise ScriptError(f"{where}: topico ou fila de origem desconhecida ({topic}, {endpoint})")
            if one(opts, "protocol", where) != "sqs":
                raise ScriptError(f"{where}: protocolo diferente de sqs")
            self.subs.append((script, topic[6:], endpoint[6:], attributes((opts.get("attributes") or [None])[0])))
            return ""
        if service == "dynamodb":
            raise ScriptError(f"{where}: a tabela fica no DynamoDB Local ({TABLE.name}), nunca no LocalStack")
        raise ScriptError(f"{where}: chamada nao reconhecida")


def parse_local():
    local = Local()
    for script in sorted(READY.glob("*.sh")):
        env = {}
        functions = {"queue_arn": [r'aws_local sqs get-queue-attributes --queue-url "$1" --attribute-names QueueArn']}
        local.run(script.name, logical_lines(script.read_text()), env, functions)
    return local


def main():
    errors = []
    try:
        local = parse_local()
    except ScriptError as e:
        print(f"check-messaging: script nao entendido: {e}", file=sys.stderr)
        sys.exit(1)
    spec = json.loads(SPEC.read_text())
    visibility, max_receive = str(spec["visibility_timeout_seconds"]), str(spec["max_receive_count"])

    tf_topics = {t["name"] for t in spec["topics"]}
    tf_queues = {q["name"]: q for q in spec["queues"]}
    tf_dlqs = {q["name"] for q in spec["dead_letter_queues"]}
    tf_subs = {(q["topic"], q["name"]) for q in spec["queues"]}
    for t in spec["topics"]:
        if not (t["fifo"] and t["name"].endswith(".fifo")):
            errors.append(f"Terraform: topico {t['name']} precisa ser FIFO (fifo=true e sufixo .fifo)")
    for q in spec["queues"] + spec["dead_letter_queues"]:
        if not (q["fifo"] and q["name"].endswith(".fifo")):
            errors.append(f"Terraform: fila {q['name']} precisa ser FIFO (fifo=true e sufixo .fifo)")

    # Cada chamada e conferida sozinha: nenhum script sobrescreve o outro.
    for script, name, attrs in local.topics:
        if attrs.get("FifoTopic") != "true":
            errors.append(f"{script}: topico {name} sem FifoTopic=true")
        if attrs.get("ContentBasedDeduplication") != "false":
            errors.append(f"{script}: topico {name} sem ContentBasedDeduplication=false")
    local_dlqs = {}
    for script, name, attrs in local.queues:
        if attrs.get("FifoQueue") != "true":
            errors.append(f"{script}: fila {name} sem FifoQueue=true")
        if attrs.get("ContentBasedDeduplication") != "false":
            errors.append(f"{script}: fila {name} sem ContentBasedDeduplication=false")
        if name in tf_queues:
            if attrs.get("VisibilityTimeout") != visibility:
                errors.append(f"{script}: fila {name} com VisibilityTimeout {attrs.get('VisibilityTimeout')}, Terraform {visibility}")
            if "RedrivePolicy" not in attrs:
                errors.append(f"{script}: fila {name} sem RedrivePolicy nos atributos")
                continue
            policy = json.loads(attrs["RedrivePolicy"])
            target = str(policy.get("deadLetterTargetArn", ""))
            if not target.startswith("queue:"):
                errors.append(f"{script}: fila {name} com DLQ desconhecida ({target})")
                continue
            local_dlqs.setdefault(name, set()).add(target[6:])
            if str(policy.get("maxReceiveCount")) != max_receive:
                errors.append(f"{script}: fila {name} com maxReceiveCount {policy.get('maxReceiveCount')}, Terraform {max_receive}")
        elif "RedrivePolicy" in attrs:
            errors.append(f"{script}: fila {name} com RedrivePolicy e fora das filas do Terraform")
    for script, topic, queue, attrs in local.subs:
        if attrs.get("RawMessageDelivery") != "true":
            errors.append(f"{script}: assinatura {topic} -> {queue} sem RawMessageDelivery=true")

    def diff(label, local_set, tf_set):
        for k in sorted(local_set - tf_set):
            errors.append(f"{label} {k}: existe no LocalStack e falta no Terraform")
        for k in sorted(tf_set - local_set):
            errors.append(f"{label} {k}: existe no Terraform e falta no LocalStack")

    def compared(name):
        return not name.endswith("-inspection.fifo")

    diff("topico", {n for _, n, _ in local.topics}, tf_topics)
    diff("fila", {n for _, n, _ in local.queues if compared(n)}, set(tf_queues) | tf_dlqs)
    diff("assinatura", {f"{t} -> {q}" for _, t, q, _ in local.subs if compared(q)}, {f"{t} -> {q}" for t, q in tf_subs})
    for name, q in tf_queues.items():
        if name in local_dlqs and local_dlqs[name] != {q["dlq"]}:
            errors.append(f"DLQ de {name}: LocalStack {sorted(local_dlqs[name])}, Terraform {q['dlq']}")

    # O Terraform fixa no main.tf o que o resources.json nao traz: deduplicacao por conteudo e entrega crua.
    tf = MAIN_TF.read_text()
    for resource in ('aws_sns_topic" "this', 'aws_sqs_queue" "dlq', 'aws_sqs_queue" "this'):
        block = re.search(rf'resource "{resource}" \{{(.*?)\n\}}', tf, re.S)
        if not block or not re.search(r"^\s*content_based_deduplication\s*=\s*false\s*$", block.group(1), re.M):
            errors.append(f"Terraform: {resource.split(chr(34))[0]} sem content_based_deduplication = false")
    sub = re.search(r'resource "aws_sns_topic_subscription" "this" \{(.*?)\n\}', tf, re.S)
    if not sub or not re.search(r"^\s*raw_message_delivery\s*=\s*true\s*$", sub.group(1), re.M):
        errors.append("Terraform: assinatura sem raw_message_delivery = true")

    tables = spec["dynamodb_tables"]
    if len(tables) != 1:
        errors.append("tabela DynamoDB: esperava uma no Terraform")
    else:
        t = tables[0]
        want = {
            "TableName": t["name"],
            "AttributeDefinitions": [{"AttributeName": t[k]["name"], "AttributeType": t[k]["type"]}
                                     for k in ("hash_key", "range_key")],
            "KeySchema": [{"AttributeName": t["hash_key"]["name"], "KeyType": "HASH"},
                          {"AttributeName": t["range_key"]["name"], "KeyType": "RANGE"}],
            "BillingMode": "PAY_PER_REQUEST",
        }
        try:
            table = json.loads(TABLE.read_text())
        except (OSError, ValueError) as e:
            errors.append(f"tabela DynamoDB: nao li {TABLE} ({e})")
        else:
            if table != want:
                errors.append(f"tabela {t['name']}: {TABLE.name} difere do Terraform ({want})")

    if errors:
        print("check-messaging: diferencas entre o ambiente local e o Terraform:", file=sys.stderr)
        for e in errors:
            print(f"  - {e}", file=sys.stderr)
        sys.exit(1)
    print(f"check-messaging: ok (1 tabela, {len(tf_topics)} topicos, {len(tf_queues) + len(tf_dlqs)} filas, {len(tf_dlqs)} DLQs, "
          f"{len(local.topics)} create-topic, {len(local.queues)} create-queue, {len(local.subs)} subscribe conferidos)")


main()
