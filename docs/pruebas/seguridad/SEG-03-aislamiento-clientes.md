# SEG-03: aislamiento entre clientes

Objetivo: comprobar que una cuenta B2B no obtiene ni modifica operaciones o archivos de otra empresa.

Tipo: prueba de seguridad de autorización horizontal, ejecutada como prueba del sistema del backend. Herramientas: JUnit Jupiter, Spring Boot Test, Java HttpClient, H2 y Maven Surefire. Se utilizan HTTP y persistencia reales; estos casos no usan Mockito.

Desde `backend-sga`, ejecutar en PowerShell:

```powershell
.\mvnw.cmd test "-Dtest=SecurityTenantIsolationTest,AcceptanceBusinessTest#AC04_dadoDocumentoCuandoOtroClienteDescargaEntoncesDenegado"
```

| Comprobación | Resultado requerido |
| --- | --- |
| SEG-03.1: detalle y listado de cotizaciones | Propietario 200; otra empresa 404 sin datos; listado solo propio; sin sesión 401. |
| SEG-03.2: aceptar cotización ajena | Otra empresa 404 sin cambiar el registro; el propietario sí puede aceptarla con 200. |
| SEG-03.3: notificaciones | El listado no revela la ajena; otra empresa no puede marcarla; el propietario sí. |
| SEG-03.4: descarga de documento (caso AC04 existente) | Propietario sube y descarga el contenido ficticio; otra empresa recibe 403 sin el contenido. |

Un 403 o 404 en el intento ajeno es el resultado esperado de protección, no un fallo de la prueba. Los controles positivos comprueban que el recurso existe y que el propietario sí puede usarlo. Las operaciones rechazadas se verifican también en la base de datos cuando corresponde.

Las cuentas de los nuevos casos son ficticias con contraseñas aleatorias, no se imprimen contraseñas ni tokens. El caso existente de documentos usa una credencial sintética exclusiva de pruebas. Las solicitudes van a 127.0.0.1 con puerto aleatorio y base H2 temporal; no acceden a MySQL de producción. El caso de documentos escribe un archivo ficticio en el almacenamiento configurado por el entorno de prueba.

Alcance: cotizaciones, notificaciones y un documento. No certifica todos los endpoints del proyecto ni la interfaz web. SEG-03 es una prueba de seguridad con cuatro comprobaciones; no son cuatro códigos SEG diferentes.
