# 6. Motor de Reportes PDF, Almacenamiento y Bitácora

**Sistema de Gestión Aduanera - Transaduanas**  
*Generación de Documentos Vectoriales (Thymeleaf + OpenHTMLtoPDF), Bóveda Documental (`LocalStorageServiceImpl`) y Logging Rotativo*

---

## Índice del Capítulo
1. [¿Cómo funciona el Motor de Reportes PDF? (Explicación para no técnicos)](#1-cómo-funciona-el-motor-de-reportes-pdf-explicación-para-no-técnicos)
2. [Arquitectura de Generación de PDFs (Thymeleaf + OpenHTMLtoPDF)](#2-arquitectura-de-generación-de-pdfs-thymeleaf--openhtmltopdf)
3. [Catálogo de Plantillas HTML Oficiales (`templates/reports/`)](#3-catálogo-de-plantillas-html-oficiales-templatesreports)
4. [Bóveda de Almacenamiento y Gestión de Archivos (`LocalStorageServiceImpl`)](#4-bóveda-de-almacenamiento-y-gestión-de-archivos-localstorageserviceimpl)
   - [4.1 Estructura en Disco y Configuración](#41-estructura-en-disco-y-configuración)
   - [4.2 Políticas de Seguridad y Prevención de Vulnerabilidades](#42-políticas-de-seguridad-y-prevención-de-vulnerabilidades)
5. [Sistema de Bitácora y Logging Rotativo (`logback-spring.xml`)](#5-sistema-de-bitácora-y-logging-rotativo-logback-springxml)

---

## 1. ¿Cómo funciona el Motor de Reportes PDF? (Explicación para no técnicos)

### La Analogía del Diseñador Editorial Web
En versiones preliminares se contempló combinar motores separados para tablas y diseño; actualmente, el **Sistema de Gestión Aduanera - Transaduanas** utiliza un **motor unificado basado en Thymeleaf + OpenHTMLtoPDF** (habiendo retirado por completo las dependencias no usadas de JasperReports del `pom.xml`).

Esto permite diseñar tanto padrones administrativos masivos (usuarios internos, cuentas del Portal B2B, sedes) como catálogos aduaneros (puertos, tipos de carga, subpartidas NANDINA) utilizando **HTML5 y CSS3** con la misma identidad visual *Warm Editorial* del frontend, convirtiéndolos en memoria a documentos PDF vectoriales de alta fidelidad sin generar archivos temporales en disco.

---

## 2. Arquitectura de Generación de PDFs (Thymeleaf + OpenHTMLtoPDF)

```
┌────────────────────────────────────────┐
│  PETICIÓN: GET .../exportar-pdf        │
└───────────────────┬────────────────────┘
                    │
                    ▼
┌────────────────────────────────────────┐
│  ReporteJasperServiceImpl.java         │
│  1. Consulta repositorios (readOnly)   │
│  2. Construye mapa de variables + KPIs │
│  3. SpringTemplateEngine (Thymeleaf)   │
│     procesa plantilla HTML5            │
│  4. PdfRendererBuilder (OpenHTMLtoPDF  │
│     1.0.10 -> PDFBox) renderiza PDF    │
└───────────────────┬────────────────────┘
                    │
                    ▼
┌────────────────────────────────────────┐
│  ByteArrayInputStream (PDF en memoria) │
│  Cabecera: application/pdf             │
│  Content-Disposition: inline / attach  │
└────────────────────────────────────────┘
```

Implementado en [`ReporteJasperServiceImpl.java`](../../backend/src/main/java/pe/sga/service/impl/ReporteJasperServiceImpl.java) (ubicado en `templates/reports/`):
1. **Paso de Datos:** El servicio reúne la información desde los repositorios JPA (`UsuarioRepository`, `UsuarioClienteRepository`, `SedeRepository`, `CatalogoPuertoRepository`, `CatalogoTipoCargaRepository`, `CatalogoMercanciaRepository`) y la coloca en un `org.thymeleaf.context.Context`.
2. **Renderizado HTML:** `SpringTemplateEngine` procesa las etiquetas dinámicas (`th:text`, `th:each`, `th:if`, fragmentos `ti-listado-base`) y genera una cadena HTML5 limpia y bien formada.
3. **Conversión Vectorial con OpenHTMLtoPDF (`1.0.10`):**
   ```java
   ByteArrayOutputStream os = new ByteArrayOutputStream();
   PdfRendererBuilder builder = new PdfRendererBuilder();
   builder.useFastMode();
   builder.withHtmlContent(html, null);
   builder.toStream(os);
   builder.run();
   ```
4. **Reglas CSS Avanzadas para Impresión (`@page`):**
   Las plantillas utilizan directivas CSS paged media para garantizar que los saltos de página, membretes y numeración salgan consistentes.

---

## 3. Catálogo de Plantillas HTML Oficiales (`templates/reports/`)

Las plantillas se encuentran en [`backend/src/main/resources/templates/reports/`](../../backend/src/main/resources/templates/reports/):

| Plantilla | Archivo Físico | Contenido y Presentación Visual |
| :--- | :--- | :--- |
| **Layout Base TI** | [`ti-listado-base.html`](../../backend/src/main/resources/templates/reports/ti-listado-base.html) | Plantilla estructural base compartida por los padrones administrativos de TI (cabecera corporativa, métricas KPI y pie paginado). |
| **Usuarios Internos** | [`reporte-usuarios.html`](../../backend/src/main/resources/templates/reports/reporte-usuarios.html) | Padrón oficial de colaboradores internos (activos o general) con usuario, correo institucional, teléfono, cargo/rol y sede. |
| **Usuarios Portal B2B** | [`reporte-usuarios-portal.html`](../../backend/src/main/resources/templates/reports/reporte-usuarios-portal.html) | Listado de cuentas corporativas B2B del Portal de Clientes con RUC/usuario, razón social, representante y correo de contacto. |
| **Padrón de Sedes** | [`reporte-sedes.html`](../../backend/src/main/resources/templates/reports/reporte-sedes.html) | Catálogo de sedes corporativas y terminales portuarios, direcciones fiscales, teléfonos de contacto y tipología operativa. |
| **Catálogo de Puertos** | [`reporte-puertos.html`](../../backend/src/main/resources/templates/reports/reporte-puertos.html) | Padrón de puertos y aeropuertos nacionales y mundiales clasificados por país, código UN/LOCODE y modalidad (Marítimo / Aéreo). |
| **Tipologías de Carga** | [`reporte-tipos-carga.html`](../../backend/src/main/resources/templates/reports/reporte-tipos-carga.html) | Guía técnica de tipologías de carga (FCL, LCL, Reefer, IMO, Proyecto, etc.) con sus requisitos de estiba y manipulación. |
| **Padrón Arancelario** | [`reporte-mercancias.html`](../../backend/src/main/resources/templates/reports/reporte-mercancias.html) | Catálogo de subpartidas NANDINA de 10 dígitos, porcentaje de arancel base Ad-Valórem y permisos sectoriales (VUCE, DIGEMID, SENASA, SUCAMEC). |

---

## 4. Bóveda de Almacenamiento y Gestión de Archivos (`LocalStorageServiceImpl`)

### 4.1 Estructura en Disco y Configuración
El servicio [`LocalStorageServiceImpl.java`](../../backend/src/main/java/pe/sga/service/impl/LocalStorageServiceImpl.java) (implementación de `IStorageService`) gestiona el almacenamiento físico organizado por subrutas (`subpath`) de los archivos adjuntos subidos por operadores y clientes:

- **Propiedad de configuración:** `app.storage.upload-dir=${STORAGE_DIR:./uploads/documentos}`
- **Límites de tamaño configurados en Spring Boot:**
  - `spring.servlet.multipart.max-file-size=25MB` (Tamaño máximo por archivo individual).
  - `spring.servlet.multipart.max-request-size=100MB` (Tamaño máximo de la petición completa).
  - En el Portal del Cliente B2B, la subida de vouchers de pago cuenta con validación adicional estricta de **máximo 10 MB** para no congestionar el canal de red.

### 4.2 Políticas de Seguridad y Prevención de Vulnerabilidades
Para proteger el servidor de ataques a través de archivos adjuntos, se aplican las siguientes reglas:
1. **Renombrado Determinista con UUID:** Nunca se guarda el archivo en disco con el nombre original provisto por el usuario. Se genera un nombre único irreversible conservando únicamente la extensión en minúsculas:  
   `UUID.randomUUID().toString() + "." + extension`  
   *(Ejemplo: `a8b2f910-4321-4d9f-9012-32a10b987654.pdf`)*.
2. **Mitigación de *Path Traversal* (Rutas Relativas Peligrosas):** Se normaliza la ruta y se rechazan explícitamente secuencias `..` o resoluciones que salgan de `rootLocation`, tanto en `store(...)` como en `loadAsResource(...)` y `delete(...)`.
3. **Lista Blanca de Extensiones / MIME:** La validación documental en `DocumentoServiceImpl` y el portal restringe los adjuntos a formatos válidos de documentos y comprobantes aduaneros (`.pdf`, `.png`, `.jpg`, `.jpeg`), rechazando ejecutables.

---

## 5. Sistema de Bitácora y Logging Rotativo (`logback-spring.xml`)

El backend cuenta con una configuración avanzada de auditoría y diagnóstico en [`backend/src/main/resources/logback-spring.xml`](../../backend/src/main/resources/logback-spring.xml):

### Políticas del Logger:
- **Consola (STDOUT):** Formato coloreado con fecha, hilo de ejecución, nivel de log (`INFO`, `WARN`, `ERROR`), clase emisora y mensaje.
- **Archivo Rotativo (`RollingFileAppender`):**
  - **Ubicación del archivo activo:** `backend/logs/sga-backend.log`
  - **Patrón de rotación:** `backend/logs/archived/sga-backend-%d{yyyy-MM-dd}.%i.log.gz`
  - **Rotación por Tamaño:** Cuando el archivo diario alcanza los **10 MB**, se comprime automáticamente en un archivo `.gz`.
  - **Retención Histórica:** Se conservan logs de los últimos **30 días**, con un tope máximo total de **3 GB**, eliminando automáticamente los archivos más antiguos para no saturar el almacenamiento del servidor.

### Consola de Logs en Tiempo Real (`AdminLogsController`):
El Administrador TI no necesita conectarse por SSH al servidor para saber qué está ocurriendo: desde la interfaz web de **Administración TI > Consola del Sistema**, el frontend consume los endpoints de [`AdminLogsController.java`](../../backend/src/main/java/pe/sga/controller/AdminLogsController.java):  
- `GET /api/v1/admin/logs?tipo=all&lines=200` (lectura en vivo)  
- `GET /api/v1/admin/logs/download?tipo=all` (descarga del archivo)  
- `POST /api/v1/admin/logs/clear?tipo=all` (limpieza de bitácora)
