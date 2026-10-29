# 6. Motor Híbrido de Reportes PDF, Almacenamiento y Bitácora

**Sistema de Gestión Aduanera - Transaduanas**  
*Generación de Documentos Vectoriales, Bóveda Documental y Logging Rotativo*

---

## Índice del Capítulo
1. [¿Por qué un Motor Híbrido de Reportes? (Explicación para no técnicos)](#1-por-qué-un-motor-híbrido-de-reportes-explicación-para-no-técnicos)
2. [Arquitectura Dual de Generación de PDFs](#2-arquitectura-dual-de-generación-de-pdfs)
   - [2.1 Vía A: JasperReports 7.0.8 (Reportes Numéricos y Tabulares)](#21-vía-a-jasperreports-708-reportes-numéricos-y-tabulares)
   - [2.2 Vía B: Thymeleaf + OpenHTMLtoPDF (Documentos Ejecutivos de Diseño)](#22-vía-b-thymeleaf--openhtmltopdf-documentos-ejecutivos-de-diseño)
3. [Catálogo de Plantillas HTML Oficiales (`templates/reports/`)](#3-catálogo-de-plantillas-html-oficiales-templatesreports)
4. [Bóveda de Almacenamiento y Gestión de Archivos (Multipart)](#4-bóveda-de-almacenamiento-y-gestión-de-archivos-multipart)
   - [4.1 Estructura en Disco y Configuración](#41-estructura-en-disco-y-configuración)
   - [4.2 Políticas de Seguridad y Prevención de Vulnerabilidades](#42-políticas-de-seguridad-y-prevención-de-vulnerabilidades)
5. [Sistema de Bitácora y Logging Rotativo (`logback-spring.xml`)](#5-sistema-de-bitácora-y-logging-rotativo-logback-springxml)

---

## 1. ¿Por qué un Motor Híbrido de Reportes? (Explicación para no técnicos)

### La Analogía de la Imprenta y el Diseñador Gráfico
En una empresa existen dos tipos de documentos impresos muy distintos:
1. **Los Padrones Tabulares Masivos:** Por ejemplo, una lista de 500 contenedores o un informe contable de 20 páginas con columnas estrechas, números alineados al centavo y subtotales por página. Para esto se necesita una **máquina calculadora de alta precisión**.
2. **Las Cartas de Presentación y Certificados Comerciales:** Por ejemplo, una cotización formal que se entrega al gerente de una multinacional o un certificado aduanero con logotipo de alta resolución, colores corporativos cálidos, tipografía ejecutiva y diseño impecable. Para esto se necesita un **diseñador gráfico**.

En lugar de forzar una sola herramienta para hacer mal ambos trabajos, el **Sistema de Gestión Aduanera - Transaduanas** implementa un **Motor Híbrido**:
- Para padrones masivos utiliza **JasperReports**.
- Para documentos de presentación y catálogos corporativos utiliza **Thymeleaf + OpenHTMLtoPDF**, permitiendo diseñar los PDFs con las mismas tecnologías con las que se construyen las páginas web modernas (HTML5 y CSS3).

---

## 2. Arquitectura Dual de Generación de PDFs

```
                               ┌────────────────────────────────┐
                               │  PETICIÓN: Generar Reporte PDF │
                               └───────────────┬────────────────┘
                                               │
                       ┌───────────────────────┴───────────────────────┐
                       ▼                                               ▼
         ¿Padrón masivo / Jasper?                       ¿Diseño visual HTML/CSS?
                       │                                               │
        ┌──────────────┴──────────────┐                 ┌──────────────┴──────────────┐
        │       JASPERREPORTS         │                 │     THYMELEAF + OPENHTML    │
        │ 1. Compila plantilla JRXML  │                 │ 1. Lee plantilla HTML5      │
        │ 2. Inyecta JRDataSource     │                 │ 2. Renderiza variables      │
        │ 3. JasperCompileManager     │                 │ 3. Aplica estilos Warm CSS  │
        │ 4. JasperExportManager      │                 │ 4. OpenHTMLtoPDF -> PDFBox  │
        └──────────────┬──────────────┘                 └──────────────┬──────────────┘
                       │                                               │
                       └───────────────────────┬───────────────────────┘
                                               ▼
                               ┌────────────────────────────────┐
                               │  Array de Bytes (byte[]) PDF   │
                               │  Cabecera: application/pdf     │
                               │  Content-Disposition: inline   │
                               └────────────────────────────────┘
```

### 2.1 Vía A: JasperReports 7.0.8 (Reportes Numéricos y Tabulares)
Implementado en `ReporteJasperServiceImpl.java`:
- Compila las expresiones Java dentro de las plantillas en memoria utilizando el compilador integrado Eclipse JDT (`jasperreports-jdt`).
- Mapea colecciones de DTOs en objetos `JRBeanCollectionDataSource`.
- Emite un flujo de salida `ByteArrayOutputStream` que se entrega al cliente con cabecera `Content-Type: application/pdf`, abriendo el documento directamente en el visor de PDF del navegador sin guardar archivos temporales en el disco del servidor.

### 2.2 Vía B: Thymeleaf + OpenHTMLtoPDF (Documentos Ejecutivos de Diseño)
Ubicado en `templates/reports/`:
1. **Paso de Datos:** El servicio reúne la información de la base de datos (sedes, puertos, mercancías, clientes) y la coloca dentro de un objeto `org.thymeleaf.context.Context`.
2. **Renderizado HTML:** El motor Thymeleaf procesa las etiquetas dinámicas (`th:text`, `th:each`, `th:if`) y genera una cadena HTML5 limpia y bien formada.
3. **Conversión Vectorial con OpenHTMLtoPDF:**
   ```java
   ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
   PdfRendererBuilder builder = new PdfRendererBuilder();
   builder.useFastMode();
   builder.withHtmlContent(htmlContent, null);
   builder.toStream(outputStream);
   builder.run();
   ```
4. **Reglas CSS Avanzadas para Impresión (@page):**
   Las plantillas utilizan directivas CSS paged media para garantizar que los saltos de página y los membretes salgan perfectos:
   ```css
   @page {
       size: A4 portrait;
       margin: 15mm 15mm 20mm 15mm;
       @bottom-right {
           content: "Página " counter(page) " de " counter(pages);
           font-family: 'Inter', sans-serif;
           font-size: 8pt;
           color: #8C827A;
       }
   }
   ```

---

## 3. Catálogo de Plantillas HTML Oficiales (`templates/reports/`)

Las plantillas se encuentran en [`backend/src/main/resources/templates/reports/`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/src/main/resources/templates/reports/):

| Plantilla | Archivo Físico | Contenido y Presentación Visual |
| :--- | :--- | :--- |
| **Padrón de Sedes** | [`reporte-sedes.html`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/src/main/resources/templates/reports/reporte-sedes.html) | Catálogo de sedes corporativas y terminales portuarios, direcciones fiscales, teléfonos de contacto y tipología operativa con distintivos visuales. |
| **Catálogo de Puertos** | [`reporte-puertos.html`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/src/main/resources/templates/reports/reporte-puertos.html) | Padrón de 80 puertos y aeropuertos nacionales y mundiales clasificados por país, código UN/LOCODE y modalidad (Marítimo / Aéreo). |
| **Tipologías de Carga** | [`reporte-tipos-carga.html`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/src/main/resources/templates/reports/reporte-tipos-carga.html) | Guía técnica de tipologías de carga (FCL, LCL, Reefer, IMO, Proyecto, etc.) con sus requisitos de estiba y normativas de manipulación. |
| **Padrón Arancelario** | [`reporte-mercancias.html`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/src/main/resources/templates/reports/reporte-mercancias.html) | Catálogo de subpartidas NANDINA de 10 dígitos, porcentaje de arancel base Ad-Valórem y entidades reguladoras sectoriales (MTC, DIGEMID, SENASA, SUCAMEC). |

---

## 4. Bóveda de Almacenamiento y Gestión de Archivos (Multipart)

### 4.1 Estructura en Disco y Configuración
El servicio `StorageServiceImpl.java` gestiona el almacenamiento físico de archivos adjuntos subidos por operadores y clientes:

- **Propiedad de configuración:** `app.storage.upload-dir=${STORAGE_DIR:./uploads/documentos}`
- **Límites de tamaño configurados en Spring Boot:**
  - `spring.servlet.multipart.max-file-size=25MB` (Tamaño máximo por archivo individual).
  - `spring.servlet.multipart.max-request-size=100MB` (Tamaño máximo de la petición completa).
  - En el Portal del Cliente B2B, la subida de vouchers de pago cuenta con una validación adicional estricta de **máximo 10 MB** para no congestionar el canal de red.

### 4.2 Políticas de Seguridad y Prevención de Vulnerabilidades
Para proteger el servidor de ataques cibernéticos a través de archivos adjuntos, se aplican las siguientes reglas:
1. **Renombrado Determinista con UUID:** Nunca se guarda el archivo en disco con el nombre original provisto por el usuario. Se genera un nombre único irreversible:  
   `UUID.randomUUID().toString() + "_" + extensionLimpia`  
   *(Ejemplo: `a8b2f910-4321-4d9f-9012-32a10b987654.pdf`)*.
2. **Mitigación de *Path Traversal* (Rutas Relativas Peligrosas):** Se sanea la ruta para evitar caracteres como `../` o `..\\` que intenten escribir archivos fuera del directorio de subidas.
3. **Lista Blanca de Extensiones Permitidas:** Solo se admiten extensiones válidas de documentos y comprobantes aduaneros: `.pdf`, `.png`, `.jpg`, `.jpeg`. Se rechazan automáticamente archivos ejecutables (`.exe`, `.sh`, `.bat`, `.js`).

---

## 5. Sistema de Bitácora y Logging Rotativo (`logback-spring.xml`)

El backend cuenta con una configuración avanzada de auditoría y diagnóstico en [`backend/src/main/resources/logback-spring.xml`](file:///home/jiodev/Documents/Proyects/TransAduanas/backend/src/main/resources/logback-spring.xml):

### Políticas del Logger:
- **Consola (STDOUT):** Formato coloreado con fecha, hilo de ejecución, nivel de log (`INFO`, `WARN`, `ERROR`), clase emisora y mensaje.
- **Archivo Rotativo (`RollingFileAppender`):**
  - **Ubicación del archivo activo:** `backend/logs/sga-backend.log`
  - **Patrón de rotación:** `backend/logs/archived/sga-backend-%d{yyyy-MM-dd}.%i.log.gz`
  - **Rotación por Tamaño:** Cuando el archivo diario alcanza los **10 MB**, se comprime automáticamente en un archivo `.gz`.
  - **Retención Histórica:** Se conservan logs de los últimos **30 días**, con un tope máximo total de **3 GB**, eliminando automáticamente los archivos más antiguos para no saturar el almacenamiento del servidor.

### Consola de Logs en Tiempo Real (`AdminLogsController`):
El Administrador TI no necesita conectarse por SSH al servidor para saber qué está ocurriendo: desde la interfaz web de **Administración TI > Consola del Sistema**, el frontend consume el endpoint:  
`GET /api/v1/admin/logs/tail?lines=200`  
Permitiendo diagnosticar errores, rastrear peticiones y auditar el sistema en tiempo real.
