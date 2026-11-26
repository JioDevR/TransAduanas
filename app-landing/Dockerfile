# ---------- Etapa 1: compilación ----------
FROM docker.io/library/node:22-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
ARG VITE_SGA_APP_URL=http://localhost:5180
ARG VITE_PORTAL_CLIENTE_URL=http://localhost:5174
ENV VITE_SGA_APP_URL=${VITE_SGA_APP_URL} \
    VITE_PORTAL_CLIENTE_URL=${VITE_PORTAL_CLIENTE_URL}
RUN npm run build

# ---------- Etapa 2: servidor estático ----------
FROM docker.io/library/nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
