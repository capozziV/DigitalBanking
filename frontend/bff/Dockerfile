# Imagem do BFF (NestJS) com Node sem root e so as dependencias de producao. Contexto: frontend/ (workspaces npm).
#   docker build -f frontend/bff/Dockerfile -t digitalbanking/bff:local frontend
# Sem HEALTHCHECK: no Kubernetes valem as probes (GET /bff/health); no compose, o healthcheck do servico.
FROM node:24-alpine@sha256:116b800c3754b440ee0aa3ed2f20f5bf3d99a554bfd9adeea09ea8017f6e85f5 AS build
WORKDIR /app
COPY package.json package-lock.json ./
COPY web/package.json web/
COPY bff/package.json bff/
COPY e2e/package.json e2e/
RUN npm ci --workspace bff --include-workspace-root=false
COPY bff bff
RUN npm run build --workspace bff

FROM node:24-alpine@sha256:116b800c3754b440ee0aa3ed2f20f5bf3d99a554bfd9adeea09ea8017f6e85f5
ENV NODE_ENV=production
WORKDIR /app
COPY package.json package-lock.json ./
COPY web/package.json web/
COPY bff/package.json bff/
COPY e2e/package.json e2e/
RUN npm ci --workspace bff --include-workspace-root=false --omit=dev && npm cache clean --force
COPY --from=build /app/bff/dist bff/dist
USER 1000:1000
EXPOSE 3000
WORKDIR /app/bff
CMD ["node", "dist/main.js"]
