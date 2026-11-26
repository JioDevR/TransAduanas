# SEG-08: restricciones de carga de archivos

Objetivo: comprobar que las cargas documentales inválidas se rechacen sin insertar registros ni dejar archivos, conservando la carga y descarga válidas.

Tipo: prueba de seguridad de carga de archivos, ejecutada como prueba del sistema del backend. Herramientas: JUnit Jupiter, Spring Boot Test, Java HttpClient, H2, sistema de archivos real y Maven Surefire. No se usa Mockito en estos casos.

Desde `backend-sga`, en PowerShell:

```powershell
.\mvnw.cmd test "-Dtest=SecurityUploadTest"
```

| Comprobación | Resultado esperado |
| --- | --- |
| SEG-08.1 | TXT de 1024 bytes aceptado (201) con nombre generado; descarga íntegra como attachment. |
| SEG-08.2 | Extensiones HTML, EXE y PDF.HTML rechazadas (400), sin registros ni archivos nuevos. Los contenidos son texto ficticio e inerte. |
| SEG-08.3 | Extensión PDF con MIME HTML o con contenido de texto sin firma PDF rechazada (400). |
| SEG-08.4 | Archivo vacío rechazado (400), sin guardar. |
| SEG-08.5 | Nombre con ruta relativa rechazado (400), sin escribir archivos. |
| SEG-08.6 | Archivo de 1025 bytes, un byte sobre el límite de prueba, rechazado (413). |
| SEG-08.7 | Lote con un archivo válido y otro inválido rechazado antes de guardar cualquiera. |
| SEG-08.8 | TXT enviado como application/octet-stream normalizado a text/plain; preview conserva el contenido como texto y usa nosniff. |

La clase configura un límite reducido de 1 KB por archivo y 8 KB por solicitud para comprobar el mecanismo sin enviar archivos grandes. El proyecto mantiene su configuración normal de 25 MB por archivo y 100 MB por solicitud: esta ejecución no transfiere 25 MB ni verifica ese límite exacto de despliegue.

Entorno: HTTP en 127.0.0.1 con puerto aleatorio, H2 temporal y carpeta `backend-sga/target/seg08-files`. Los casos rechazados comparan cantidad de registros y archivos antes y después. Los archivos aceptados de prueba se conservan en esa carpeta; no se usan documentos reales. El volumen de datos es mínimo y no se hacen pruebas de carga.

## Resultado inicial y cambios

La primera ejecución tuvo dos comprobaciones aprobadas y cinco fallidas: se aceptaban extensiones no permitidas, MIME/contenido incompatible, archivos vacíos y lotes inválidos; el exceso de tamaño devolvía 500. Los controles de descarga y nombre con ruta sí funcionaron.

Se añadió `UploadPolicy` en la entrada de `DocumentoServiceImpl` para validar el lote completo antes de escribir. Permite TXT, CSV, PDF, PNG, JPG/JPEG, DOC, DOCX, XLS y XLSX; exige correspondencia de MIME (con alias comunes y tipo genérico), comprueba firmas básicas de formatos binarios y usa un MIME canónico al registrar el documento. Se rechazan nombres con rutas, controles, comillas o longitud excesiva.

`GlobalExceptionHandler` ahora responde 413 para `MaxUploadSizeExceededException`, con mensaje genérico. No se relajaron los criterios de las pruebas para aceptar respuestas incorrectas.

Las ocho comprobaciones deben mostrar APROBADO y el resumen debe indicar cero fallos y BUILD SUCCESS. SEG-08 es un solo código de seguridad con ocho comprobaciones internas.

Para verificar también el backend completo:

```powershell
.\mvnw.cmd test
```

## Alcance y límites

Las firmas básicas no son un antivirus ni una validación completa de PDF, imágenes o documentos Office. DOCX/XLSX se comprueban como contenedores ZIP, no se inspecciona todo su contenido; DOC/XLS usan la firma del contenedor OLE. No se certifica ausencia de macros, contenido activo, fórmulas peligrosas en CSV ni archivos maliciosos. No se extrajeron archivos comprimidos durante estas pruebas.

La prueba se aplica al endpoint documental y al almacenamiento local evaluado. No comprueba otros endpoints que puedan usar directamente el servicio de almacenamiento, fallos de disco a mitad de escritura, archivos históricos ya almacenados ni la configuración del proxy de producción. La validación previa evita archivos parciales por lotes inválidos; no pretende una transacción atómica de base de datos y disco frente a cualquier fallo operativo.

## Resultado final verificado

SEG-08: ocho comprobaciones aprobadas, sin fallos. Suite final del backend: 381 pruebas, cero fallos, cero errores, ninguna omitida y BUILD SUCCESS. Las 381 corresponden a todo el backend.
