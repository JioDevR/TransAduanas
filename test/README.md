# Plan de Pruebas de Software — TransAduanas Platform (Backend)

Esta carpeta centraliza la planificación, especificación y registro de resultados de las pruebas de software del backend (`pe.sga`, Spring Boot 4.1.2 / Java 21).

> [!NOTE]
> Las pruebas ejecutables en Java viven en `backend/src/test/java/pe/sga/` (donde Maven/Surefire las detecta automáticamente) y su documentación previa en `docs/pruebas/`.

## Estructura de la carpeta `test/`

| Carpeta | Contenido / Propósito | Estado | Documento |
|---|---|---|---|
| `01-gestion-repositorio/` | Estrategia de ramas, commits convencionales, Pull Requests y rollbacks | Planificado | [README.md](./01-gestion-repositorio/README.md) |
| `02-pruebas-unitarias/` | Pruebas con JUnit 6 + Mockito (normales, límite, nulos y excepciones) en 6 suites (38 casos) | Implementado (Fase 2A) | [README.md](./02-pruebas-unitarias/README.md) |
| `03-pruebas-integracion/` | Pruebas de integración con Testcontainers (`mysql:8.4`), JPA y generación de PDF | Planificado | [README.md](./03-pruebas-integracion/README.md) |
| `04-pruebas-sistema-e2e-aceptacion/` | Flujos críticos HTTP, códigos de respuesta, gestión de `DEF01`, Postman y aceptación | Planificado | [README.md](./04-pruebas-sistema-e2e-aceptacion/README.md) |
| `05-cicd-jacoco-seguridad/` | Pipeline en GitHub Actions, cobertura JaCoCo (70%–80%) y seguridad en `logback-spring.xml` | Planificado | [README.md](./05-cicd-jacoco-seguridad/README.md) |
| `06-documentacion-y-evidencias/` | Inventario de fichas formales (requisitos, casos, ejecución, informe final) y evidencias | Planificado | [README.md](./06-documentacion-y-evidencias/README.md) |

## Resumen del Stack y Metas

- **Backend**: Java 21, Spring Boot `4.1.2`, Maven Wrapper (`./mvnw`).
- **Motor de pruebas**: JUnit Jupiter `6.0.3`, Mockito `5.23.0`, Spring Boot Test `4.1.2`.
- **Base de datos de integración**: MySQL `8.4` mediante Testcontainers (alineado con `db/docker-compose.yml`).
- **Meta de cobertura (JaCoCo)**: mínimo **70%–80%** en lógica crítica (`pe.sga.service.impl.*` y `pe.sga.config.*`).
