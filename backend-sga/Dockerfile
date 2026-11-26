# ---------- Etapa 1: compilación ----------
FROM docker.io/library/eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Descriptor y wrapper primero, para aprovechar la caché de capas
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Código fuente y empaquetado
COPY src src
RUN ./mvnw clean package -DskipTests -B \
    && cp target/sga-backend-*.jar /build/app.jar

# ---------- Etapa 2: ejecución ----------
FROM docker.io/library/eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S sga && adduser -S sga -G sga \
    && mkdir -p /app/uploads/documentos /app/logs \
    && chown -R sga:sga /app

COPY --from=builder --chown=sga:sga /build/app.jar app.jar

ENV LOG_PATH=/app/logs
USER sga
EXPOSE 8085

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
