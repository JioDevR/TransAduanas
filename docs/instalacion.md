# Guía de Instalación y Despliegue — Sistema de Gestión Aduanera - Transaduanas

**Sistema de Gestión Aduanera - Transaduanas**

> [!IMPORTANT]
> **Alcance de este documento:**  
> Esta guía está dividida en dos modalidades de despliegue:
> 1. **[Instalación Local (Entorno de Desarrollo)](#1-instalación-local-entorno-de-desarrollo):** Documentación completa con todos los pasos necesarios para levantar y probar el proyecto de forma nativa en tu máquina local.
> 2. **[Instalación con Docker](#2-instalación-con-docker):** Sección reservada para el despliegue contenerizado completo (en proceso de implementación).

---

## Índice General

- [1. Instalación Local (Entorno de Desarrollo)](#1-instalación-local-entorno-de-desarrollo)
  - [1.1 Arquitectura en Ejecución Local](#11-arquitectura-en-ejecución-local)
  - [1.2 Requisitos Previos del Sistema](#12-requisitos-previos-del-sistema)
  - [1.3 Paso 1: Configuración e Inicialización de la Base de Datos MySQL Local](#13-paso-1-configuración-e-inicialización-de-la-base-de-datos-mysql-local)
  - [1.4 Paso 2: Configuración y Ejecución del Backend (Spring Boot)](#14-paso-2-configuración-y-ejecución-del-backend-spring-boot)
  - [1.5 Paso 3: Instalación y Ejecución del Frontend Interno (SGA Operativo)](#15-paso-3-instalación-y-ejecución-del-frontend-interno-sga-operativo)
  - [1.6 Paso 4: Instalación y Ejecución del Portal del Cliente B2B](#16-paso-4-instalación-y-ejecución-del-portal-del-cliente-b2b)
  - [1.7 Matriz de Puertos y Servicios Activos](#17-matriz-de-puertos-y-servicios-activos)
  - [1.8 Credenciales de Prueba para Evaluar el Proyecto (`seed.sql`)](#18-credenciales-de-prueba-para-evaluar-el-proyecto-seedsql)
  - [1.9 Ejecución de Pruebas Automatizadas (Testing)](#19-ejecución-de-pruebas-automatizadas-testing)
  - [1.10 Solución de Problemas Frecuentes (Troubleshooting)](#110-solución-de-problemas-frecuentes-troubleshooting)
- [2. Instalación con Docker](#2-instalación-con-docker)

---

# 1. Instalación Local (Entorno de Desarrollo)

Esta modalidad permite levantar y probar todos los módulos del **Sistema de Gestión Aduanera - Transaduanas** directamente sobre tu estación de trabajo para desarrollo, evaluación o demostración funcional.

### 1.1 Arquitectura en Ejecución Local

En modo local, el ecosistema se compone de **4 servicios** ejecutándose en `localhost`:

```
┌─────────────────────────────────────────┐     ┌─────────────────────────────────────────┐
│     FRONTEND INTERNO (SGA Operativo)    │     │         PORTAL DEL CLIENTE B2B          │
│         Vue 3 + Vite 8 (HMR)            │     │         Vue 3 + Vite 6 (HMR)            │
│          http://localhost:5180          │     │          http://localhost:5174          │
└────────────────────┬────────────────────┘     └────────────────────┬────────────────────┘
                     │  Proxy Vite (/api -> :8085)                   │  Proxy Vite (/api -> :8085)
                     └──────────────────────┬────────────────────────┘
                                            ▼
                     ┌───────────────────────────────────────────────┐
                     │            BACKEND API REST (SGA)             │
                     │         Spring Boot 4.1.2 / Java 21           │
                     │             http://localhost:8085             │
                     └──────────────────────┬────────────────────────┘
                                            │  JDBC (Puerto 3306)
                                            ▼
                     ┌───────────────────────────────────────────────┐
                     │           BASE DE DATOS RELACIONAL            │
                     │            MySQL 8.4 LTS (SGAdb)              │
                     │              localhost:3306                   │
                     └───────────────────────────────────────────────┘
```

---

### 1.2 Requisitos Previos del Sistema

Antes de comenzar, verifica que tengas instaladas las siguientes herramientas en tu sistema operativo:

| Herramienta | Versión Requerida | Comando de Verificación | Propósito |
| :--- | :---: | :--- | :--- |
| **Java JDK** | **21 LTS** | `java -version` | Compilación y ejecución del backend Spring Boot 4.1.2. |
| **Apache Maven** | **3.9+** | `mvn -version` | Gestión de dependencias Java, compilación de Lombok y arranque del servidor. |
| **Node.js** | **18.x / 20.x LTS+** | `node -v` | Entorno de ejecución para los servidores de desarrollo Vite. |
| **npm** | **9.x+** | `npm -v` | Gestor de paquetes para `app-sga/` y `app-portal-cliente/`. |
| **MySQL Server** | **8.0 / 8.4 LTS** | `mysql --version` | Motor de base de datos relacional InnoDB activo en el puerto `3306`. |

---

### 1.3 Paso 1: Configuración e Inicialización de la Base de Datos MySQL Local

El sistema utiliza una base de datos llamada `SGAdb` con juego de caracteres `utf8mb4` (`utf8mb4_unicode_ci`). Los scripts SQL oficiales se encuentran en la carpeta [`db/`](file:///home/jiodev/Documents/Proyects/TransAduanas/db):

- [`db/schema.sql`](file:///home/jiodev/Documents/Proyects/TransAduanas/db/schema.sql): Crea la base de datos `SGAdb` y las **17 tablas relacionales** con sus llaves foráneas e índices.
- [`db/seed.sql`](file:///home/jiodev/Documents/Proyects/TransAduanas/db/seed.sql): Inserta los datos iniciales completos para probar el proyecto (6 sedes, 80 puertos/aeropuertos, 17 tipos de carga, 52 subpartidas arancelarias NANDINA, 65 usuarios internos en los 8 roles y 80 empresas clientes con cuentas del Portal B2B).

**Ejecuta los siguientes comandos desde la raíz del repositorio:**

```bash
# 1. Crear la base de datos SGAdb y la estructura de 17 tablas
mysql -u root -p < db/schema.sql

# 2. Poblar la base de datos con los catálogos maestros, usuarios y clientes de prueba
mysql -u root -p < db/seed.sql
```

> [!TIP]
> También puedes abrir [`db/schema.sql`](file:///home/jiodev/Documents/Proyects/TransAduanas/db/schema.sql) y [`db/seed.sql`](file:///home/jiodev/Documents/Proyects/TransAduanas/db/seed.sql) desde **MySQL Workbench**, **DBeaver** o **DataGrip** y ejecutarlos en ese mismo orden.

---

### 1.4 Paso 2: Configuración y Ejecución del Backend (Spring Boot)

#### A. Configurar Credenciales Locales (`application-local.properties`)

Para no exponer tu usuario y contraseña de MySQL en Git, el archivo [`backend/src/main/resources/application.properties`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/src/main/resources/application.properties) importa automáticamente el archivo `backend/application-local.properties` (ignorado en `.gitignore`).

1. Ingresa al directorio `backend/` y crea tu archivo local a partir de la plantilla [`backend/application-local.properties.example`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/application-local.properties.example):

   ```bash
   cd backend
   cp application-local.properties.example application-local.properties
   ```

2. Abre `backend/application-local.properties` y coloca el usuario y contraseña de tu servidor MySQL local:

   ```properties
   # Credenciales de conexión a MySQL Local
   spring.datasource.url=jdbc:mysql://localhost:3306/SGAdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
   spring.datasource.username=root
   spring.datasource.password=tu_contraseña_mysql_local

   # Puerto de ejecución del backend (por defecto: 8085)
   server.port=8085
   ```

#### B. Variables de Entorno Opcionales

Si prefieres usar variables de entorno en lugar de editar el archivo de propiedades, el backend admite las siguientes:

| Variable de Entorno | Valor por Defecto | Descripción |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8085` | Puerto HTTP del servidor Spring Boot. |
| `DB_URL` | `jdbc:mysql://localhost:3306/SGAdb?...` | URL JDBC de conexión a MySQL. |
| `DB_USERNAME` | `root` | Usuario de MySQL. |
| `DB_PASSWORD` | *(vacío)* | Contraseña de MySQL. |
| `JPA_DDL_AUTO` | `update` | Validación/actualización de esquema con Hibernate. |
| `JPA_SHOW_SQL` | `true` | Muestra las consultas SQL en consola para depuración. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:5180,http://localhost:5174` | Orígenes locales permitidos por CORS. |
| `STORAGE_DIR` | `./uploads/documentos` | Carpeta local donde se guardan los archivos subidos al sistema. |

#### C. Compilar e Iniciar el Backend

Desde la carpeta `backend/`, ejecuta:

```bash
# 1. Compilar el proyecto y descargar dependencias Maven
mvn clean compile

# 2. Levantar el servidor Spring Boot en el puerto 8085
mvn spring-boot:run
```

- **URL Base de la API:** `http://localhost:8085/api/v1`
- **Verificación de Salud (Health Check):** `http://localhost:8085/api/v1/health`

---

### 1.5 Paso 3: Instalación y Ejecución del Frontend Interno (SGA Operativo)

El **Frontend Interno** ([`app-sga/`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-sga)) es la aplicación web principal donde operan los **8 roles institucionales**. Corre en el puerto `5180` y redirige automáticamente las llamadas `/api` hacia `http://localhost:8085`.

Abre una **nueva terminal** desde la raíz del proyecto y ejecuta:

```bash
# 1. Ingresar al directorio del frontend interno
cd app-sga

# 2. Instalar dependencias de Node.js
npm install

# 3. Iniciar el servidor de desarrollo Vite
npm run dev
```

- **URL de Acceso al Sistema Interno:** `http://localhost:5180`

---

### 1.6 Paso 4: Instalación y Ejecución del Portal del Cliente B2B

El **Portal del Cliente B2B** ([`app-portal-cliente/`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-portal-cliente)) es la plataforma externa donde los clientes corporativos (importadores y exportadores) consultan el tracking de sus expedientes, revisan cotizaciones, registran pagos y descargan su documentación. Corre en el puerto `5174`.

Abre una **tercera terminal** desde la raíz del proyecto y ejecuta:

```bash
# 1. Ingresar al directorio del portal de clientes
cd app-portal-cliente

# 2. Instalar dependencias de Node.js
npm install

# 3. Iniciar el servidor de desarrollo Vite
npm run dev
```

- **URL de Acceso al Portal del Cliente B2B:** `http://localhost:5174`

---

### 1.7 Matriz de Puertos y Servicios Activos

Una vez completados los 4 pasos anteriores, tendrás todo el ecosistema activo en tu entorno local:

| Servicio | Carpeta | Tecnología | Puerto | URL Local |
| :--- | :--- | :--- | :---: | :--- |
| **1. Base de Datos (`SGAdb`)** | [`db/`](file:///home/jiodev/Documents/Proyects/TransAduanas/db) | MySQL 8.4 InnoDB | `3306` | `jdbc:mysql://localhost:3306/SGAdb` |
| **2. Backend API REST** | [`backend/`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend) | Spring Boot 4.1.2 / Java 21 | `8085` | `http://localhost:8085/api/v1/health` |
| **3. Frontend Interno (SGA)** | [`app-sga/`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-sga) | Vue 3 + Vite 8 + PrimeVue | `5180` | `http://localhost:5180` |
| **4. Portal del Cliente B2B** | [`app-portal-cliente/`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-portal-cliente) | Vue 3 + Vite 6 + PrimeVue | `5174` | `http://localhost:5174` |

---

### 1.8 Credenciales de Prueba para Evaluar el Proyecto (`seed.sql`)

Para probar el flujo completo del proyecto de extremo a extremo, utiliza las siguientes cuentas preconfiguradas:

#### A. Usuarios Internos por Rol Institucional (`http://localhost:5180`)
**Contraseña única para todos los usuarios internos:** **`Password123!`**

| Rol Institucional | Usuario Principal | Otros Usuarios Disponibles (`seed.sql`) | Ruta de Inicio por Defecto |
| :--- | :---: | :--- | :--- |
| **`ADMINISTRADOR_TI`** | `admin` | — | `/administracion-ti/dashboard` |
| **`GERENTE_GENERAL`** | `gerente` | — | `/gerencia-general/dashboard` |
| **`AGENTE_COMERCIAL`** | `comercial` | `dcastillo`, `nvalverde`, `mrodriguez` | `/recepcion-comercial/dashboard` |
| **`SUPERVISOR_ADUANERO`** | `supervisor` | `rcabrera`, `apena`, `dmiranda` | `/gestion-aduanera/apertura` |
| **`AGENTE_ADUANERO`** | `agente` | `fbalbuena`, `lmejia`, `ccordova` | `/deposito-temporal/ingreso` |
| **`SUPERVISOR_TRANSPORTE`** | `transporte` | `ebravor`, `wgalarza`, `osanchez` | `/despachos-ruta/flota` |
| **`CONTADOR`** | `contador` | `rsalazar`, `gmontoya`, `lcornejo` | `/validacion-financiera/pagos` |
| **`ASESOR_LEGAL`** | `legal` | `mcamacho`, `jvilchez`, `cportugal` | `/asesoria-legal/normativa` |

#### B. Cuentas Corporativas del Portal B2B (`http://localhost:5174`)
En el Portal B2B se inicia sesión con el **número de RUC (11 dígitos)** y la contraseña **`Password123!`**:

| Empresa Importadora / Exportadora | Usuario (RUC) | Contraseña |
| :--- | :---: | :---: |
| **Camposol S.A.** | `20370146994` | `Password123!` |
| **LG Electronics Peru S.A.** | `20511894520` | `Password123!` |
| **Danper Trujillo S.A.C.** | `20440058771` | `Password123!` |
| **Compañía Minera Antamina S.A.** | `20330790762` | `Password123!` |
| **Ferreyros S.A.** | `20100055237` | `Password123!` |
| **Alicorp S.A.A.** | `20100055238` | `Password123!` |

---

### 1.9 Ejecución de Pruebas Automatizadas (Testing)

Para ejecutar las pruebas unitarias y verificar la compilación de los módulos frontend:

```bash
# Pruebas unitarias y build del Frontend Interno
cd app-sga
npm run test
npm run build

# Pruebas unitarias y build del Portal del Cliente B2B
cd ../app-portal-cliente
npm run test
npm run build
```

---

### 1.10 Solución de Problemas Frecuentes (Troubleshooting)

1. **Error `Access denied for user 'root'@'localhost'` al iniciar el Backend:**
   - Verifica que hayas creado `backend/application-local.properties` y que `spring.datasource.password` tenga la contraseña correcta de tu servidor MySQL local.
2. **Error `Unknown database 'SGAdb'`:**
   - Asegúrate de haber ejecutado primero `mysql -u root -p < db/schema.sql` y luego `mysql -u root -p < db/seed.sql` (Paso 1).
3. **Errores de red (`Network Error` o `502`) al iniciar sesión en Frontend o Portal B2B:**
   - Verifica que el backend esté corriendo en `http://localhost:8085/api/v1/health` antes de realizar peticiones desde `http://localhost:5180` o `http://localhost:5174`.

---

# 2. Instalación con Docker

<!-- Pendiente de implementación -->
