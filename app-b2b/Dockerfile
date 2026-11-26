# ---------- Etapa 1: compilación ----------
# Requiere contexto en la raíz del repositorio para acceder a ../sgaUI:
#   docker build -f app-b2b/Dockerfile -t transaduanas/app-b2b:latest .
FROM docker.io/library/node:22-alpine AS builder
WORKDIR /workspace/app-b2b

COPY app-b2b/package.json app-b2b/package-lock.json ./
RUN npm ci

COPY sgaUI /workspace/sgaUI
COPY app-b2b .
ARG VITE_API_URL=/api/v1
ENV VITE_API_URL=${VITE_API_URL}
RUN npm run build

# ---------- Etapa 2: servidor estático ----------
FROM docker.io/library/nginx:1.27-alpine
COPY app-b2b/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /workspace/app-b2b/dist /usr/share/nginx/html
EXPOSE 80
