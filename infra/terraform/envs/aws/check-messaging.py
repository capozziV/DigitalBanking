#!/usr/bin/env python3
"""Compara a mensageria do Terraform (modules/messaging/resources.json) com os scripts do LocalStack.

Le infra/local/localstack/init/ready.d/*.sh e extrai topicos, filas, DLQs, assinaturas, atributos FIFO,
visibilidade, maxReceiveCount e a tabela DynamoDB; sai com 1 se algo diferir. As filas *-inspection.fifo
sao so locais (sem consumidor) e ficam de fora. Toda fila e todo topico precisam ser FIFO nos dois lados.

Uso: check-messaging.py [ready.d] [resources.json]
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
READY = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "infra/local/localstack/init/ready.d"
SPEC = Path(sys.argv[2]) if len(sys.argv) > 2 else ROOT / "infra/terraform/modules/messaging/resources.json"

CONSUMER_CALL = re.compile(r"^create_consumer_queue\s+(\S+)\s+(\S+)\s+(\S+)\s*$", re.M)
DEFAULT = re.compile(r"\$\{(\w+):-(\d+)\}")


def block_after(text, start):
    """Texto do comando aws_local que comeca em `start` (ate a primeira linha sem barra final)."""
    lines = text[start:].splitlines()
    out = []
    for line in lines:
        out.append(line)
        if not line.rstrip().endswith("\\"):
            break
    return "\n".join(out)


def fifo_in(text, block):
    """A fila e FIFO se o --attributes do bloco, literal ou lido de uma variavel do script, define FifoQueue=true."""
    attrs = re.search(r"--attributes\s+(\"\$(\w+)\"|\S+)", block)
    if not attrs:
        return False
    value = attrs.group(1)
    if attrs.group(2):
        assignment = re.search(rf"^\s*(?:local\s+)?{attrs.group(2)}=(.*)$", text, re.M)
        value = assignment.group(1) if assignment else ""
    return bool(re.search(r"FifoQueue(=|\\?\"?:\\?\"?)true", value))


def parse_local():
    topics, queues, dlqs, subs, errors = {}, {}, {}, set(), []
    visibility, max_receive, table = set(), set(), None
    for script in sorted(READY.glob("*.sh")):
        text = script.read_text()
        for var, value in DEFAULT.findall(text):
            if var.endswith("MAX_RECEIVE_COUNT"):
                max_receive.add(int(value))
            if var.endswith("VISIBILITY_TIMEOUT_SECONDS"):
                visibility.add(int(value))
        for match in re.finditer(r"sns create-topic", text):
            cmd = block_after(text, match.start())
            name = re.search(r"--name\s+(\S+)", cmd).group(1).strip('"')
            fifo = "FifoTopic=true" in cmd
            if name.startswith("$"):
                if not fifo:
                    errors.append(f"{script.name}: topico {name} sem FifoTopic=true")
            else:
                topics[name] = fifo
        literal = []
        for match in re.finditer(r"sqs create-queue", text):
            cmd = block_after(text, match.start())
            name = re.search(r"--queue-name\s+(\S+)", cmd).group(1).strip('"')
            fifo = fifo_in(text, cmd)
            if name.startswith("$"):
                if not fifo:
                    errors.append(f"{script.name}: fila {name} sem FifoQueue=true")
            else:
                queues[name] = fifo
                literal.append((name, "RedrivePolicy" in text and "--attributes \"$attributes\"" in cmd))
        for queue, dlq, topic in CONSUMER_CALL.findall(text):
            queues[queue] = queues[dlq] = True
            topics.setdefault(topic, True)
            dlqs[queue] = dlq
            subs.add((topic, queue))
        # Fila com DLQ literal (statement): a que usa RedrivePolicy e a fila; a outra e a DLQ.
        with_redrive = [n for n, redrive in literal if redrive]
        without = [n for n, redrive in literal if not redrive and n.endswith("-dlq.fifo")]
        topic = re.search(r"sns create-topic\s*\\\s*\n\s*--name\s+(\S+)", text)
        if with_redrive and without and topic and not CONSUMER_CALL.search(text):
            dlqs[with_redrive[0]] = without[0]
            subs.add((topic.group(1), with_redrive[0]))
        t = re.search(r"dynamodb create-table.*?--table-name\s+(\S+).*?--attribute-definitions\s+(.*?)\\\n\s*--key-schema\s+(.*?)\\\n\s*--billing-mode\s+(\S+)", text, re.S)
        if t:
            table = (t.group(1), t.group(2).strip(), t.group(3).strip(), t.group(4))
    return topics, queues, dlqs, subs, visibility, max_receive, table, errors


def main():
    topics, queues, dlqs, subs, visibility, max_receive, table, errors = parse_local()
    spec = json.loads(SPEC.read_text())

    local_queues = {k: v for k, v in queues.items() if not k.endswith("-inspection.fifo")}
    local_topics = dict(topics)
    tf_topics = {t["name"]: t["fifo"] for t in spec["topics"]}
    tf_queues = {q["name"]: q["fifo"] for q in spec["queues"] + spec["dead_letter_queues"]}
    tf_dlqs = {q["name"]: q["dlq"] for q in spec["queues"]}
    tf_subs = {(q["topic"], q["name"]) for q in spec["queues"]}

    def diff(label, local, tf):
        for k in sorted(set(local) - set(tf)):
            errors.append(f"{label} {k}: existe no LocalStack e falta no Terraform")
        for k in sorted(set(tf) - set(local)):
            errors.append(f"{label} {k}: existe no Terraform e falta no LocalStack")

    diff("topico", local_topics, tf_topics)
    diff("fila", local_queues, tf_queues)
    diff("DLQ de", dlqs, tf_dlqs)
    for k in sorted(set(dlqs) & set(tf_dlqs)):
        if dlqs[k] != tf_dlqs[k]:
            errors.append(f"DLQ de {k}: LocalStack {dlqs[k]}, Terraform {tf_dlqs[k]}")
    for label, local, tf in (("topico", local_topics, tf_topics), ("fila", local_queues, tf_queues)):
        for k in sorted(set(local) & set(tf)):
            if not (local[k] and tf[k] and k.endswith(".fifo")):
                errors.append(f"{label} {k}: precisa ser FIFO nos dois lados (LocalStack {local[k]}, Terraform {tf[k]}, sufixo .fifo)")
    for pair in sorted(subs - tf_subs):
        errors.append(f"assinatura {pair[0]} -> {pair[1]}: existe no LocalStack e falta no Terraform")
    for pair in sorted(tf_subs - subs):
        errors.append(f"assinatura {pair[0]} -> {pair[1]}: existe no Terraform e falta no LocalStack")
    if visibility != {spec["visibility_timeout_seconds"]}:
        errors.append(f"visibilidade: LocalStack {sorted(visibility)}, Terraform {spec['visibility_timeout_seconds']}")
    if max_receive != {spec["max_receive_count"]}:
        errors.append(f"maxReceiveCount: LocalStack {sorted(max_receive)}, Terraform {spec['max_receive_count']}")

    tables = spec["dynamodb_tables"]
    if table is None or len(tables) != 1:
        errors.append("tabela DynamoDB: esperava uma no LocalStack e uma no Terraform")
    else:
        name, attrs, keys, billing = table
        t = tables[0]
        want_attrs = {f"AttributeName={t['hash_key']['name']},AttributeType={t['hash_key']['type']}",
                      f"AttributeName={t['range_key']['name']},AttributeType={t['range_key']['type']}"}
        want_keys = [f"AttributeName={t['hash_key']['name']},KeyType=HASH", f"AttributeName={t['range_key']['name']},KeyType=RANGE"]
        if name != t["name"] or set(attrs.split()) != want_attrs or keys.split() != want_keys or billing != "PAY_PER_REQUEST":
            errors.append(f"tabela {name}: atributos ou chaves diferem do Terraform ({t})")

    if errors:
        print("check-messaging: diferencas entre o LocalStack e o Terraform:", file=sys.stderr)
        for e in errors:
            print(f"  - {e}", file=sys.stderr)
        sys.exit(1)
    print(f"check-messaging: ok ({len(tf_topics)} topicos, {len(tf_queues)} filas, {len(tf_dlqs)} DLQs)")


main()
