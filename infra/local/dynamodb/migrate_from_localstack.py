#!/usr/bin/env python3
"""Copia a tabela `statement` do DynamoDB do LocalStack para o DynamoDB Local de uma stack que ja existe.

Rode ANTES de recriar ou parar o LocalStack (ele nao tem persistencia), com o `statement` parado.
Passo a passo em docs/rodando-localmente.md. So usa a biblioteca padrao do Python e o CLI do Docker.

- Da origem so le (DescribeTable e Scan): nada e alterado nem apagado la.
- No destino so cria o que falta (PutItem condicional): nada e sobrescrito nem apagado, e repetir nao copia de novo.
- Copia o item inteiro, seja lancamento (sequence >= 1) ou conta com o dono (sequence 0).
- Falha se a tabela de origem nao existe, se as chaves, a contagem ou o conteudo divergem, ou se a origem mudou
  durante a copia. Nunca imprime o conteudo dos itens.
- Os endpoints tem de ser as portas publicadas pelos conteineres do projeto informado, nesta maquina.

Uso: migrate_from_localstack.py --project <projeto> --source-endpoint <LocalStack> --target-endpoint <DynamoDB Local>
"""
import argparse
import json
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request

TABLE = "statement"
LOOPBACK = ("127.0.0.1", "localhost")


class MigrationError(Exception):
    pass


class DynamoDbHttpClient:
    def __init__(self, endpoint):
        self.endpoint = endpoint
        # Sem proxy do ambiente: os dados nao saem da maquina.
        self.opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))

    def call(self, operation, payload):
        request = urllib.request.Request(
            self.endpoint,
            json.dumps(payload).encode(),
            {
                "Content-Type": "application/x-amz-json-1.0",
                "X-Amz-Target": f"DynamoDB_20120810.{operation}",
                # Os emuladores nao conferem a assinatura; o LocalStack tira a conta e a regiao daqui.
                "Authorization": "AWS4-HMAC-SHA256 Credential=test/20260101/us-east-1/dynamodb/aws4_request, "
                "SignedHeaders=host, Signature=0",
            },
        )
        try:
            with self.opener.open(request, timeout=30) as response:
                return json.load(response)
        except urllib.error.HTTPError as error:
            # So o tipo do erro: o corpo pode trazer o item.
            body = error.read().decode(errors="replace")
            try:
                kind = json.loads(body).get("__type", "").split("#")[-1]
            except ValueError:
                kind = ""
            raise MigrationError(f"{operation} at {self.endpoint}: HTTP {error.code} {kind}") from None
        except OSError as error:
            raise MigrationError(f"{operation} at {self.endpoint}: {error}") from None


def schema(client):
    table = client.call("DescribeTable", {"TableName": TABLE})["Table"]
    types = {a["AttributeName"]: a["AttributeType"] for a in table["AttributeDefinitions"]}
    return sorted((k["AttributeName"], k["KeyType"], types.get(k["AttributeName"])) for k in table["KeySchema"])


def read_items(client):
    items = {}
    request = {"TableName": TABLE, "ConsistentRead": True}
    while True:
        page = client.call("Scan", request)
        for item in page["Items"]:
            items[(item["accountId"]["S"], item["sequence"]["N"])] = item
        if not page.get("LastEvaluatedKey"):
            return items
        request["ExclusiveStartKey"] = page["LastEvaluatedKey"]


def migrate(source, target):
    """Returns (items in the source, items copied now); raises MigrationError on any divergence."""
    if schema(source) != schema(target):
        raise MigrationError("the key schema of the two tables differs")
    origin = read_items(source)
    existing = read_items(target)

    extra = existing.keys() - origin.keys()
    if extra:
        raise MigrationError(f"{len(extra)} items exist only in the target; nothing was copied")
    changed = [key for key, item in existing.items() if item != origin[key]]
    if changed:
        raise MigrationError(f"{len(changed)} items have other content in the target; nothing was copied")

    missing = [item for key, item in origin.items() if key not in existing]
    for item in missing:
        target.call(
            "PutItem",
            {"TableName": TABLE, "Item": item, "ConditionExpression": "attribute_not_exists(accountId)"},
        )

    if read_items(source) != origin:
        raise MigrationError("the source changed during the copy; stop the statement and run again")
    copied = read_items(target)
    if len(copied) != len(origin):
        raise MigrationError(f"count differs after the copy: source {len(origin)}, target {len(copied)}")
    if copied != origin:
        raise MigrationError("content differs after the copy")
    return len(origin), len(missing)


def run_docker(*args):
    result = subprocess.run(("docker",) + args, capture_output=True, text=True)
    if result.returncode:
        raise MigrationError(f"docker {args[0]} failed: {result.stderr.strip()}")
    return result.stdout.strip()


def check_stack(project, source, target, docker=run_docker):
    def running(service):
        return docker(
            "ps",
            "-q",
            "--filter",
            f"label=com.docker.compose.project={project}",
            "--filter",
            f"label=com.docker.compose.service={service}",
        ).split()

    for service, container_port, endpoint in (("localstack", 4566, source), ("dynamodb", 8000, target)):
        url = urllib.parse.urlsplit(endpoint)
        if url.scheme != "http" or url.hostname not in LOOPBACK:
            raise MigrationError(f"{endpoint}: only http on loopback is accepted")
        containers = running(service)
        if len(containers) != 1:
            raise MigrationError(f"project {project}: expected one running {service}, found {len(containers)}")
        published = docker("port", containers[0], f"{container_port}/tcp").splitlines()
        if str(url.port) not in {line.rsplit(":", 1)[-1] for line in published}:
            raise MigrationError(f"{endpoint} is not the {service} of project {project}")
    if running("statement"):
        raise MigrationError(f"project {project}: stop the statement before copying")


def main():
    parser = argparse.ArgumentParser(description="Copia o extrato do LocalStack para o DynamoDB Local.")
    parser.add_argument("--project", required=True, help="projeto do docker compose")
    parser.add_argument("--source-endpoint", required=True, help="LocalStack, ex.: http://127.0.0.1:4566")
    parser.add_argument("--target-endpoint", required=True, help="DynamoDB Local, ex.: http://127.0.0.1:8000")
    args = parser.parse_args()
    try:
        check_stack(args.project, args.source_endpoint, args.target_endpoint)
        total, copied = migrate(DynamoDbHttpClient(args.source_endpoint), DynamoDbHttpClient(args.target_endpoint))
    except MigrationError as error:
        print(f"FALHA: {error}", file=sys.stderr)
        sys.exit(1)
    print(f"OK: {total} itens na origem e no destino, conteudo identico ({copied} copiados agora)")


if __name__ == "__main__":
    main()
