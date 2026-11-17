#!/usr/bin/env python3
"""Compara o roteamento do ALB interno (modules/edge-alb/routes.json) com as `location` do nginx local.

Cada `location` que faz proxy para um servico vira um casador canonico:
  `location = /x`   -> exato  /x       (ALB: "/x")
  `location /x/`    -> prefixo /x/     (ALB: "/x/*")
  `location /x`     -> prefixo /x      (ALB: "/x*")
  `location ~ re`   -> regex re        (ALB: regex_values)
O servico sai da porta do proxy_pass. As `location` do actuator (proxy_pass com caminho /actuator/) sao so
locais: o ALB checa a saude direto no pod e nao expoe o actuator na borda, entao elas nao podem aparecer no ALB.
Tambem confere as portas dos servicos e que toda regex avalia antes das regras de prefixo (prioridade menor),
como no nginx, onde a regex vence o prefixo sem ^~.

Uso: check-alb-routes.py [nginx.conf] [routes.json]; sai com 1 se houver diferenca.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
NGINX = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "infra/local/nginx/nginx.conf"
ROUTES = Path(sys.argv[2]) if len(sys.argv) > 2 else ROOT / "infra/terraform/modules/edge-alb/routes.json"

LOCATION = re.compile(r"location\s+(=|~)?\s*(\S+)\s*\{(.*?)\}", re.S)
PROXY = re.compile(r"proxy_pass\s+http://[^:/\s]+:(\d+)(/\S*)?;")
FIXED = re.compile(r"return\s+(\d+)")


def nginx_routes(text):
    routes, local_only, fixed = set(), set(), set()
    for modifier, path, body in LOCATION.findall(text):
        if modifier == "=":
            matcher = ("exact", path)
        elif modifier == "~":
            matcher = ("regex", path)
        else:
            matcher = ("prefix", path)
        proxy = PROXY.search(body)
        if proxy:
            port, uri = int(proxy.group(1)), proxy.group(2)
            if uri and uri.startswith("/actuator/"):
                local_only.add(matcher)
            else:
                routes.add((matcher, port))
            continue
        status = FIXED.search(body)
        if status:
            fixed.add((matcher, int(status.group(1))))
            continue
        raise SystemExit(f"location sem proxy_pass nem return: {path}")
    return routes, local_only, fixed


def alb_matcher(pattern):
    if "?" in pattern or "*" in pattern[:-1]:
        raise SystemExit(f"curinga fora do fim nao tem equivalente no nginx: {pattern}")
    if pattern.endswith("/*"):
        return ("prefix", pattern[:-1])
    if pattern.endswith("*"):
        return ("prefix", pattern[:-1])
    return ("exact", pattern)


def alb_routes(config):
    services = config["services"]
    routes, priorities = set(), []
    for rule in config["rules"]:
        port = services[rule["service"]]
        matchers = [alb_matcher(p) for p in rule.get("paths", [])]
        matchers += [("regex", r) for r in rule.get("regex", [])]
        if len(matchers) > 3:
            raise SystemExit(f"regra {rule['priority']}: o ALB aceita no maximo 3 valores por condicao")
        for matcher in matchers:
            routes.add((matcher, port))
            priorities.append((matcher[0], rule["priority"]))
    return routes, priorities


def main():
    nginx, local_only, fixed = nginx_routes(NGINX.read_text())
    config = json.loads(ROUTES.read_text())
    alb, priorities = alb_routes(config)
    errors = []

    for missing in sorted(nginx - alb):
        errors.append(f"no nginx e fora do ALB: {missing}")
    for extra in sorted(alb - nginx):
        errors.append(f"no ALB e fora do nginx: {extra}")
    for matcher in sorted(local_only & {m for m, _ in alb}):
        errors.append(f"actuator e so local, mas esta no ALB: {matcher}")

    nginx_ports = {port for _, port in nginx}
    if nginx_ports != set(config["services"].values()):
        errors.append(f"portas diferentes: nginx {sorted(nginx_ports)}, ALB {sorted(config['services'].values())}")

    regex_max = max((p for kind, p in priorities if kind == "regex"), default=0)
    prefix_min = min((p for kind, p in priorities if kind != "regex"), default=10**6)
    if regex_max >= prefix_min:
        errors.append(f"regex com prioridade {regex_max} avalia depois do prefixo com prioridade {prefix_min}")

    if fixed != {(("exact", "/healthz"), 200)}:
        errors.append(f"respostas fixas do nginx mudaram (o ALB responde so /healthz 200): {sorted(fixed)}")

    for error in errors:
        print(f"ERRO: {error}")
    if errors:
        return 1
    print(f"ok: {len(alb)} casadores do ALB batem com o nginx; {len(local_only)} location de actuator so locais")
    return 0


if __name__ == "__main__":
    sys.exit(main())
