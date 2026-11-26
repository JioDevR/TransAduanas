# ---------- Etapa 1: compilación ----------
# Requiere contexto en la raíz del repositorio para acceder a ../sgaUI:
#   docker build -f app-sga/Dockerfile -t transaduanas/app-sga:latest .
FROM docker.io/library/node:22-alpine AS builder
WORKDIR /workspace/app-sga

COPY app-sga/package.json app-sga/package-lock.json ./
RUN npm ci

COPY sgaUI /workspace/sgaUI
COPY app-sga .
ARG VITE_API_URL=/api/v1
ARG VITE_PORTAL_URL=http://localhost:5174
ENV VITE_API_URL=${VITE_API_URL} \
    VITE_PORTAL_URL=${VITE_PORTAL_URL}
RUN npm run build

# ---------- Etapa 2: servidor estático ----------
FROM docker.io/library/nginx:1.27-alpine
COPY app-sga/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /workspace/app-sga/dist /usr/share/nginx/html
EXPOSE 80
