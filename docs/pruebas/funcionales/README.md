# Quince pruebas funcionales de negocio

Esta guía corresponde a las 15 funcionalidades del listado y del documento de resultados. Cada caso dispone de un método independiente en `backend/src/test/java/pe/sga/functional/BusinessFunctionalCasesTest.java`.

## Requisitos y entorno

Usa Windows, PowerShell y Java 21. Maven Wrapper descarga las dependencias que falten. Spring Boot inicia en un puerto local aleatorio, usa H2 en memoria y almacena los adjuntos de prueba en `backend/target/qa-files`.

No hace falta iniciar MySQL, el backend ni los frontends manualmente. Las cuentas y sesiones se preparan únicamente para ejecutar las operaciones del negocio. Los datos se crean durante cada prueba; puedes ejecutar cualquier caso sin realizar el anterior. Las operaciones no se muestran en el navegador: sus aserciones, registros y reportes se comprueban mediante el ejecutor.

## Abrir la terminal

Desde la carpeta principal del proyecto:

```powershell
Set-Location -LiteralPath .\backend
java -version
.\mvnw.cmd -v
```

Si la terminal está en otra ubicación, entra a la carpeta real del proyecto. En este equipo:

```powershell
Set-Location -LiteralPath "$env:USERPROFILE\Music\TransAduanas\backend"
```

## Ejecutar cada caso

Desde `backend`, cambia el número del caso en el comando:

```powershell
.\ejecutar-caso-funcional.ps1 -Caso 1
```

| Caso | Funcionalidad | Comprobación esperada |
|---|---|---|
| 1 | Formalización de prospecto a cliente | El prospecto cambió a CLIENTE, dejó de figurar como prospecto y conservó el nuevo estado en la base. |
| 2 | Registro de clientes | El cliente quedó persistido con documento y correo correctos. Un documento duplicado generó un rechazo y no agregó otra fila. |
| 3 | Consulta de puertos | HTTP: listado no vacío 200; detalle 200 idéntico al registro listado; identificador inexistente 404. |
| 4 | Validación de anticipos | Un abono de USD 25,00 sobre USD 100,00 quedó APROBADO, con saldo USD 75,00. Al observarlo, su estado cambió a OBSERVADO y el total aprobado volvió a cero. |
| 5 | Búsqueda de clientes | La búsqueda por razón social en minúsculas recuperó un único cliente activo con el identificador esperado. |
| 6 | Consulta de tipos de carga | HTTP: listado no vacío 200; detalle 200 idéntico al registro listado; identificador inexistente 404. |
| 7 | Apertura de expedientes | Sin pago aprobado, la apertura fue rechazada y no creó expediente. Con adelanto aprobado, creó y persistió el expediente asociado. Un segundo intento fue rechazado. |
| 8 | Actualización de clientes | La nueva razón social quedó persistida y se recuperó correctamente al consultar el cliente. |
| 9 | Consulta de sedes | El listado devolvió sedes, el detalle coincidió con el registro listado y una sede inexistente devolvió 404. |
| 10 | Consulta de expedientes | El listado devolvió el expediente creado. La consulta del detalle devolvió su código e identificador correctos con HTTP 200. |
| 11 | Solicitud de cotización | HTTP: solicitud válida 201 y una nueva cotización persistida, con el cliente correcto y FOB 1000,00. Un FOB negativo devolvió 400 sin agregar registros. |
| 12 | Aceptación de cotización | La aceptación quedó registrada, mantuvo SIN_PAGO y fijó el saldo en USD 100,00. Una segunda aceptación fue rechazada. |
| 13 | Liquidación financiera | La emisión persistió subtotal S/ 550,00, IGV S/ 99,00, total S/ 1649,00 y USD 439,73 a TC 3,75. La consulta por expediente recuperó la liquidación emitida. |
| 14 | Carga y descarga de documentos | La carga multipart devolvió 201 y creó el registro del adjunto. La descarga devolvió 200, el nombre y el contenido exactos del archivo original. |
| 15 | Consulta de mercancías | Con una mercancía de prueba persistida: listado 200; detalle y búsqueda por código 200 coincidentes; identificador inexistente 404. |

El caso 15 crea una mercancía controlada antes de consultar el catálogo. El caso 7 prepara una cotización aprobada y comprueba la regla financiera de apertura. La validación de importes, estados, duplicados y registros inexistentes corresponde a las reglas funcionales del negocio.

Si PowerShell indica que los scripts están bloqueados, puedes ejecutar una vez mediante:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-caso-funcional.ps1 -Caso 1
```

Esta opción no cambia permanentemente la política. También puedes usar directamente Maven con el método correspondiente:

```powershell
# 1. Formalización de prospecto a cliente
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP01_formalizacionProspectoACliente"

# 2. Registro de clientes
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP02_registroClienteYDuplicado"

# 3. Consulta de puertos
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP03_puertosListaDetalleReal"

# 4. Validación de anticipos
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP04_validarAnticipoPersisteYRecalcula"

# 5. Búsqueda de clientes
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP05_busquedaClientesPorRazonSocial"

# 6. Consulta de tipos de carga
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP06_tiposCargaListaDetalleReal"

# 7. Apertura de expedientes
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP07_aperturaExigePagoYPersiste"

# 8. Actualización de clientes
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP08_actualizacionClientePersiste"

# 9. Consulta de sedes
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP09_sedesListaDetalleReal"

# 10. Consulta de expedientes
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP10_consultaListadoYDetalleExpediente"

# 11. Solicitud de cotización
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP11_solicitudPortalCreaRegistroYRechazaInvalido"

# 12. Aceptación de cotización
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP12_aceptarCotizacionPersisteSaldoYObservacion"

# 13. Liquidación financiera
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP13_liquidacionEmitePersisteYConsulta"

# 14. Carga y descarga de documentos
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP14_cargaYDescargaConservaContenido"

# 15. Consulta de mercancías
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP15_mercanciasListaDetalleCodigoReal"

```

## Ejecutar las quince pruebas juntas

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest"
```

Este comando ejecuta exactamente los 15 métodos de esta clase. La ejecución verificada registró 15 pruebas, 0 fallos, 0 errores y 0 omitidas. Una ejecución posterior debe evaluarse por sus propios reportes.

## Resultados y evidencias

El ejecutor muestra el caso, la salida de Maven y el resumen. Cada ejecución guarda archivos nuevos en:

```text
backend/target/casos-funcionales/CP01/FECHA-HORA/
  ejecucion.txt
  maven.log
  TEST-pe.sga.functional.BusinessFunctionalCasesTest.xml
```

El código de carpeta cambia según el caso. El ejecutor solo indica APROBADO cuando Maven termina con código cero y genera un XML nuevo con al menos una prueba, sin fallos, errores ni omisiones. Un rechazo esperado por una regla del negocio puede ser una comprobación aprobada.

Los comandos directos de Maven generan su reporte en:

```text
backend/target/surefire-reports/TEST-pe.sga.functional.BusinessFunctionalCasesTest.xml
```

No confundas un reporte anterior con el resultado actual. Para la exposición, captura el número del caso, el comando y su resumen final. Si el entorno falla antes de generar un XML, documenta la incidencia y no lo presentes como aprobado.

## Archivos del proyecto

- `backend/src/test/java/pe/sga/functional/BusinessFunctionalCasesTest.java`: los 15 casos funcionales.
- `backend/src/test/java/pe/sga/functional/FullStackTestBase.java`: preparación de Spring Boot, H2, HTTP y datos de prueba.
- `backend/ejecutar-caso-funcional.ps1`: selección individual y conservación de evidencias.

Los resultados cubren el backend, sus servicios y persistencia de pruebas. La interfaz web, MySQL y la instalación en otro dispositivo requieren sus propias ejecuciones.
