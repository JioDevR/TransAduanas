<div align="center">

# TransAduanas

**Sistema de Gestión Aduanera (SGA)**
Plataforma integral para digitalizar y controlar las operaciones aduaneras y logísticas en el Perú.

![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![Vue](https://img.shields.io/badge/Vue-3.5-42b883?logo=vuedotjs&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-8-646CFF?logo=vite&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?logo=mysql&logoColor=white)
![PrimeVue](https://img.shields.io/badge/PrimeVue-4-41B883)

[Instalación](docs/instalacion.md) · [Compilación](docs/compilacion.md) · [Docker](docs/docker-image-build.md) · [Pruebas](docs/pruebas/funcionales/README.md)

</div>

---

## Tabla de contenidos

- [Descripción](#descripción)
- [Características principales](#características-principales)
- [Arquitectura](#arquitectura)
- [Stack tecnológico](#stack-tecnológico)
- [Roles y accesos](#roles-y-accesos)
- [Inicio rápido](#inicio-rápido)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Pruebas](#pruebas)
- [Documentación](#documentación)

---

## Descripción

**TransAduanas** unifica en una sola plataforma el trabajo de las áreas comercial, aduanera, de almacén y de finanzas de una agencia de aduanas. Opera conforme a la normativa de **SUNAT** y ofrece un **Portal B2B** donde los clientes importadores y exportadores consultan sus cotizaciones, pagos y el tracking de sus expedientes.

El sistema cubre el ciclo completo de una operación: cotización → validación de pago → apertura de expediente → trámite de DAM → control de almacén y pesaje VGM → liquidación final.

## Características principales

- **Compuerta financiera:** no se crea un expediente si la cotización no tiene un pago o anticipo validado por Finanzas.
- **Borrado lógico (soft delete):** no se ejecutan `DELETE` sobre registros de negocio. Se usan banderas de estado (`estado_activo`) o estados terminales (`ANULADO`, `INACTIVO`) para conservar la auditoría.
- **Control de acceso por rol (RBAC):** autorización en la API con Spring Security (`@PreAuthorize`) y en el cliente con guards de rutas.
- **Portal B2B de autogestión:** tracking, cotizaciones, pagos, notificaciones y proforma en PDF.
- **Archivo documental centralizado:** factura comercial, packing list, BL, guías y vouchers vinculados a cotizaciones o expedientes.
- **Reportes PDF institucionales:** plantillas Thymeleaf renderizadas con OpenHTMLtoPDF.
- **Almacén y pesaje VGM:** registro de ingreso de carga y de pesaje certificado.
- **Sistema de diseño propio (`sgaUI`):** _Warm Editorial_ sobre PrimeVue 4, con modo claro y oscuro y diseño responsivo.

## Arquitectura

```text
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ app-landing  │  │   app-sga    │  │   app-b2b    │
│ Sitio público│  │ Personal     │  │ Portal de    │
│   :5173      │  │ interno :5180│  │ clientes:5174│
└──────────────┘  └──────┬───────┘  └──────┬───────┘
                         │  HTTP + JWT     │
                         └────────┬────────┘
                                  ▼
                   ┌──────────────────────────────┐
                   │   backend-sga (REST /api/v1) │
                   │ Spring Boot 4 · Java 21 :8085│
                   │ Controller → Service → Repo  │
                   └──────────────┬───────────────┘
                                  │ JDBC
                                  ▼
                   ┌──────────────────────────────┐
                   │      MySQL 8.4 (SGAdb)       │
                   │            :3306             │
                   └──────────────────────────────┘
```

**Backend:** arquitectura en capas (Controller → Service → Repository). Las entidades JPA no se exponen: la API usa DTOs validados con Jakarta Validation y devuelve un envoltorio común `ApiResponseDTO<T>`. Un `GlobalExceptionHandler` centraliza los errores HTTP. La autenticación es stateless con JWT.

**Frontend:** SPAs de Vue 3 (Composition API) organizadas por funcionalidad (`features/`). Usan Pinia para el estado, Vue Router con guards y Axios con interceptores para el token y el manejo de 401.

## Stack tecnológico

| Capa | Tecnologías |
| :-- | :-- |
| **Backend** | Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation, Thymeleaf), JJWT, OpenHTMLtoPDF, Lombok, Maven |
| **Frontend** | Vue 3, Vite, Vue Router, Pinia, PrimeVue 4 (preset Aura personalizado), PrimeIcons, Axios |
| **Base de datos** | MySQL 8.4 InnoDB, `utf8mb4`, zona horaria `-05:00` |
| **Pruebas** | JUnit 5, Mockito, Spring Boot Test, H2 |
| **Infraestructura** | Docker / Podman Compose, GitHub Actions (manual) |

## Roles y accesos

| Rol | Responsabilidad |
| :-- | :-- |
| `ADMINISTRADOR_TI` | Usuarios, cuentas del Portal B2B, sedes, catálogos y logs |
| `GERENTE_GENERAL` | Dashboard ejecutivo y supervisión general |
| `AGENTE_COMERCIAL` | Clientes y cotizaciones |
| `SUPERVISOR_ADUANERO` | Auditoría de cotizaciones, expedientes y DAM |
| `AGENTE_ADUANERO` | Almacén, pesaje VGM y levante |
| `CONTADOR` | Validación de pagos, detracciones y liquidaciones |

Los clientes acceden al **Portal B2B** con su RUC.

## Inicio rápido

### Requisitos

- JDK 21
- Node.js 20 o superior y npm
- MySQL 8.4

### 1. Base de datos

```bash
mysql -u root -p < db/schema.sql
mysql -u root -p < db/seed.sql
```

### 2. Backend

```bash
cd backend-sga
cp application-local.properties.example application-local.properties   # edita las credenciales de MySQL
./mvnw spring-boot:run
```

La API queda en `http://localhost:8085/api/v1`. Para comprobar que responde, usa `GET /api/v1/health`.

### 3. Frontends

```bash
cd app-sga && npm install && npm run dev      # http://localhost:5180
cd app-b2b && npm install && npm run dev      # http://localhost:5174
cd app-landing && npm install && npm run dev  # http://localhost:5173
```

> Para levantar todo en contenedores, sigue la [instalación con Docker Compose](docs/instalacion.md#2-instalación-con-docker-compose).

### Servicios y puertos

| Servicio | Directorio | Puerto |
| :-- | :-- | :-: |
| Base de datos | [`db/`](db) | 3306 |
| API REST | [`backend-sga/`](backend-sga) | 8085 |
| Landing pública | [`app-landing/`](app-landing) | 5173 |
| Portal B2B | [`app-b2b/`](app-b2b) | 5174 |
| SGA interno | [`app-sga/`](app-sga) | 5180 |

> [!NOTE]
> Con los datos de `seed.sql` puedes iniciar sesión con usuarios de prueba. Las credenciales están en la [guía de instalación](docs/instalacion.md#110-credenciales-de-prueba). Son solo para desarrollo: cámbialas en cualquier otro entorno.

## Estructura del repositorio

```text
TransAduanas/
├── backend-sga/     # API REST (Spring Boot, Java 21)
├── app-sga/         # Frontend interno por roles (Vue 3)
├── app-b2b/         # Portal B2B de clientes (Vue 3)
├── app-landing/     # Sitio público (Vue 3)
├── sgaUI/           # Sistema de diseño compartido (tema PrimeVue y CSS)
├── db/              # schema.sql, seed.sql y docker-compose.yml
├── docs/            # Instalación, compilación, Docker y pruebas
└── .github/         # Workflows de CI
```

## Pruebas

El backend tiene un inventario de **346 pruebas** (unitarias, de integración, de sistema y de aceptación) clasificadas con etiquetas JUnit `@Tag`.

```bash
cd backend-sga
./mvnw test                                  # todas las pruebas
./mvnw test -Dgroups=docente15               # selección de 15 para sustentación
```

Más detalle en [`docs/pruebas/`](docs/pruebas/funcionales/README.md).

## Documentación

| Documento | Contenido |
| :-- | :-- |
| [`docs/instalacion.md`](docs/instalacion.md) | Instalación para desarrollo, credenciales de prueba y despliegue con Docker Compose y solución de problemas |
| [`docs/compilacion.md`](docs/compilacion.md) | Compilación del backend (JAR) y de los frontends |
| [`docs/docker-image-build.md`](docs/docker-image-build.md) | Construcción de imágenes Docker y orquestación con Compose |
| [`sgaUI/README.md`](sgaUI/README.md) | Sistema de diseño: paleta, tipografía, componentes y responsividad |
| [`docs/pruebas/`](docs/pruebas/funcionales/README.md) | Casos funcionales y kit de despliegue de pruebas |
| [`docs/db-er-diagram.drawio`](docs/db-er-diagram.drawio) | Diagrama entidad-relación |
