# Imagem do web: Angular compilado e servido por nginx sem root. Contexto: frontend/ (workspaces npm).
#   docker build -f frontend/web/Dockerfile -t digitalbanking/web:local frontend
# Escuta na porta 80 como usuario nao root: no compose o Docker ja libera portas baixas; no Kubernetes o chart
# liga o sysctl net.ipv4.ip_unprivileged_port_start=0 no pod. Sem HEALTHCHECK: valem as probes e o do compose.
FROM node:24-alpine@sha256:116b800c3754b440ee0aa3ed2f20f5bf3d99a554bfd9adeea09ea8017f6e85f5 AS build
WORKDIR /app
COPY package.json package-lock.json ./
COPY web/package.json web/
COPY bff/package.json bff/
COPY e2e/package.json e2e/
RUN npm ci --workspace web --include-workspace-root=false
COPY web web
RUN npm run build --workspace web

FROM nginxinc/nginx-unprivileged:1.29-alpine@sha256:0c79d56aee561a1d81c63f00eee5fb5fe29279560cdc55e91425133104c7fbe6
COPY web/nginx.conf /etc/nginx/nginx.conf
COPY --from=build /app/web/dist/web/browser /usr/share/nginx/html
USER 101:101
EXPOSE 80
# Entrypoint proprio: o da imagem tenta reescrever /etc/nginx/conf.d, que fica somente leitura no pod.
ENTRYPOINT ["nginx", "-g", "daemon off;"]
