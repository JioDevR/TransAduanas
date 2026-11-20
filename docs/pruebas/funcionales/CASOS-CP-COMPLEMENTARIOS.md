# Casos CP complementarios (selección anterior)

Estos CP01–CP15 siguen disponibles como pruebas adicionales, pero **no son la nueva selección de 15 con los cuatro tipos**. Consulta el [manual de evaluación](README.md).

# Guía para demostrar las 15 pruebas funcionales

## Organización de todas las pruebas

El [manual general por tipo](../../../test/README.md) organiza las 346 pruebas Java del backend en **293 unitarias, 30 de integración, 19 de sistema del backend y 4 de aceptación automatizada**, con comandos para ejecutar cada grupo. Esta guía se limita a los 15 casos CP01–CP15, incluidos dentro de esos grupos; no son 15 casos adicionales.

Cada caso tiene una etiqueta JUnit: CP01, CP02, CP04, CP05, CP07, CP08, CP12 y CP13 pertenecen a `integracion`; CP03, CP06, CP09, CP10, CP11, CP14 y CP15 a `sistema`. Los comandos individuales de esta guía no cambian.

## Preparación

Usa la versión actualizada de la rama `mejoras/pruebas-funcionales`. Abre PowerShell en la carpeta principal de tu copia del repositorio y ejecuta:

```powershell
Set-Location -LiteralPath .\backend-sga
java -version
.\mvnw.cmd -v
```

**Importante:** el ejecutable se escribe `mvnw.cmd`: no agregues una barra entre `mvnw` y `.cmd`. Puedes verificar la ubicación con `Get-Item .\mvnw.cmd` después de entrar a `backend-sga`. El encabezado de PowerShell por sí solo no ejecuta ninguna prueba: pega el comando del caso y pulsa Enter.

Se requiere Java 21. Maven Wrapper descargará las dependencias que falten. No necesitas iniciar manualmente MySQL, el backend ni las aplicaciones web para estas pruebas.

Cada prueba prepara sus propios datos ficticios, usa servicios reales del backend y una base H2 temporal. Los datos de estas pruebas no aparecen en MySQL ni en el formulario de clientes de la aplicación habitual. Los casos que llaman servicios directamente no demuestran el funcionamiento de la interfaz ni su validación HTTP.

## Cómo leer la evidencia ante el docente

1. Explica el objetivo del caso antes de ejecutarlo.
2. Muestra el comando y las líneas identificadas con `[CPxx]`.
3. Lee los datos iniciales y los valores guardados o respuestas recibidas.
4. Cuando aparezca `RECHAZO ESPERADO`, explica que el sistema bloqueó una operación inválida. Eso es un comportamiento correcto y no un fallo de la prueba.
5. Cierra con `RESULTADO | APROBADO` y `Tests run`, `Failures`, `Errors` y `Skipped`. Un `BUILD SUCCESS` aislado no describe lo que se verificó.

Los mensajes contienen valores obtenidos durante la ejecución. Los IDs, códigos, fechas y cantidades iniciales de filas pueden cambiar. No necesitas que coincidan con los de una ejecución anterior. Si una aserción falla, no presentes ese caso como aprobado: revisa su mensaje de error.

El documento del caso 2 es el número identificador del cliente, no un archivo adjunto. Los identificadores ficticios de las pruebas permiten comprobar unicidad y persistencia; no prueban la validez tributaria de un RUC real.

## Tipo de prueba y alcance real

Todas las CP01–CP15 son **pruebas funcionales automatizadas de caja gris**: comprueban reglas o respuestas del negocio y consultan repositorios para preparar o verificar datos. “Funcional” describe qué se verifica; “unitaria”, “integración” o “sistema” describen su nivel y alcance.

- **Integración de servicios y persistencia (8 casos):** llaman servicios Spring reales y consultan JPA/H2. No comprueban el controlador HTTP ni el formulario del navegador, aunque se arranque todo el contexto de Spring.
- **Sistema del backend por API HTTP (7 casos):** la operación pasa por HTTP real, servidor local, seguridad cuando corresponde, controladores, servicios y persistencia. También son integración de API y base. “Sistema” se limita al backend con H2; no equivale a un E2E de la aplicación web completa ni de MySQL.
- **Ninguno de estos 15 casos es unitario:** las dependencias de negocio y persistencia son reales, sin mocks. Tampoco son pruebas manuales de aceptación, de rendimiento o una auditoría de seguridad.

| Caso | Funcionalidad | Clasificación principal | Recorrido de la operación verificada |
|---|---|---|---|
| CP01 | Formalización de prospecto a cliente | Integración de servicios y persistencia | Servicio real → JPA/H2 |
| CP02 | Registro de clientes | Integración de servicios y persistencia | Servicio real → JPA/H2 |
| CP03 | Consulta de puertos | Sistema del backend por API HTTP | HTTP → controlador/seguridad → servicio → JPA/H2 |
| CP04 | Validación de anticipos | Integración de servicios y persistencia | Servicio real → JPA/H2 |
| CP05 | Búsqueda de clientes | Integración de servicios y persistencia | Servicio real → JPA/H2 |
| CP06 | Consulta de tipos de carga | Sistema del backend por API HTTP | HTTP → controlador/seguridad → servicio → JPA/H2 |
| CP07 | Apertura de expedientes | Integración de servicios y persistencia | Servicio real → JPA/H2 |
| CP08 | Actualización de clientes | Integración de servicios y persistencia | Servicio real → JPA/H2 |
| CP09 | Consulta de sedes | Sistema del backend por API HTTP | HTTP → controlador/seguridad → servicio → JPA/H2 |
| CP10 | Consulta de expedientes | Sistema del backend por API HTTP | HTTP → controlador/seguridad → servicio → JPA/H2 |
| CP11 | Solicitud de cotización | Sistema del backend por API HTTP | HTTP → controlador/seguridad → servicio → JPA/H2 |
| CP12 | Aceptación de cotización | Integración de servicios y persistencia | Servicio real → JPA/H2 |
| CP13 | Liquidación financiera | Integración de servicios y persistencia | Servicio real → JPA/H2 |
| CP14 | Carga y descarga de documentos | Sistema del backend por API HTTP | HTTP → controlador/seguridad → servicio → JPA/H2 |
| CP15 | Consulta de mercancías | Sistema del backend por API HTTP | HTTP → controlador/seguridad → servicio → JPA/H2 |

Los accesos auxiliares de login o preparación no cambian la clasificación: se clasifica la operación comprobada. H2 en modo MySQL no sustituye una prueba sobre MySQL real.

## Casos individuales

Todos los comandos siguientes se ejecutan desde `backend-sga`. Usa el primer comando de cada caso para conservar automáticamente las evidencias. El comando Maven es una alternativa y también muestra los mensajes explicativos.

### CP01. Formalización de prospecto a cliente

**Tipo:** Integración de servicios y persistencia; llamada directa al servicio, sin probar la interfaz HTTP de esa operación.

**Qué explicar y comprobar:** El prospecto cambió a CLIENTE, dejó de figurar como prospecto y conservó el nuevo estado en la base.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-caso-funcional.ps1 -Caso 1
```

**Alternativa con Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP01_formalizacionProspectoACliente"
```

En la salida, muestra las líneas `[CP01]` y verifica que el resultado final del caso sea `APROBADO`.

### CP02. Registro de clientes

**Tipo:** Integración de servicios y persistencia; llamada directa al servicio, sin probar la interfaz HTTP de esa operación.

**Qué explicar y comprobar:** El cliente quedó persistido con documento y correo correctos. Un documento duplicado generó un rechazo y no agregó otra fila.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-caso-funcional.ps1 -Caso 2
```

**Alternativa con Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP02_registroClienteYDuplicado"
```

En la salida, muestra las líneas `[CP02]` y verifica que el resultado final del caso sea `APROBADO`.

### CP03. Consulta de puertos

**Tipo:** Sistema del backend por API HTTP; integración real de API y persistencia.

**Qué explicar y comprobar:** HTTP: listado no vacío 200; detalle 200 idéntico al registro listado; identificador inexistente 404.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-caso-funcional.ps1 -Caso 3
```

**Alternativa con Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP03_puertosListaDetalleReal"
```

En la salida, muestra las líneas `[CP03]` y verifica que el resultado final del caso sea `APROBADO`.

### CP04. Validación de anticipos

**Tipo:** Integración de servicios y persistencia; llamada directa al servicio, sin probar la interfaz HTTP de esa operación.

**Qué explicar y comprobar:** Un abono de USD 25,00 sobre USD 100,00 quedó APROBADO, con saldo USD 75,00. Al observarlo, su estado cambió a OBSERVADO y el total aprobado volvió a cero.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-caso-funcional.ps1 -Caso 4
```

**Alternativa con Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP04_validarAnticipoPersisteYRecalcula"
```

En la salida, muestra las líneas `[CP04]` y verifica que el resultado final del caso sea `APROBADO`.

### CP05. Búsqueda de clientes

**Tipo:** Integración de servicios y persistencia; llamada directa al servicio, sin probar la interfaz HTTP de esa operación.

**Qué explicar y comprobar:** La búsqueda por razón social en minúsculas recuperó un único cliente activo con el identificador esperado.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-caso-funcional.ps1 -Caso 5
```

**Alternativa con Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP05_busquedaClientesPorRazonSocial"
```

En la salida, muestra las líneas `[CP05]` y verifica que el resultado final del caso sea `APROBADO`.

### CP06. Consulta de tipos de carga

**Tipo:** Sistema del backend por API HTTP; integración real de API y persistencia.

**Qué explicar y comprobar:** HTTP: listado no vacío 200; detalle 200 idéntico al registro listado; identificador inexistente 404.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-caso-funcional.ps1 -Caso 6
```

**Alternativa con Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP06_tiposCargaListaDetalleReal"
```

En la salida, muestra las líneas `[CP06]` y verifica que el resultado final del caso sea `APROBADO`.

### CP07. Apertura de expedientes

**Tipo:** Integración de servicios y persistencia; llamada directa al servicio, sin probar la interfaz HTTP de esa operación.

**Qué explicar y comprobar:** Sin pago aprobado, la apertura fue rechazada y no creó expediente. Con adelanto aprobado, creó y persistió el expediente asociado. Un segundo intento fue rechazado.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-caso-funcional.ps1 -Caso 7
```

**Alternativa con Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP07_aperturaExigePagoYPersiste"
```

En la salida, muestra las líneas `[CP07]` y verifica que el resultado final del caso sea `APROBADO`.

### CP08. Actualización de clientes

**Tipo:** Integración de servicios y persistencia; llamada directa al servicio, sin probar la interfaz HTTP de esa operación.

**Qué explicar y comprobar:** La nueva razón social quedó persistida y se recuperó correctamente al consultar el cliente.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-caso-funcional.ps1 -Caso 8
```

**Alternativa con Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP08_actualizacionClientePersiste"
```

En la salida, muestra las líneas `[CP08]` y verifica que el resultado final del caso sea `APROBADO`.

### CP09. Consulta de sedes

**Tipo:** Sistema del backend por API HTTP; integración real de API y persistencia.

**Qué explicar y comprobar:** El listado devolvió sedes, el detalle coincidió con el registro listado y una sede inexistente devolvió 404.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-caso-funcional.ps1 -Caso 9
```

**Alternativa con Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP09_sedesListaDetalleReal"
```

En la salida, muestra las líneas `[CP09]` y verifica que el resultado final del caso sea `APROBADO`.

### CP10. Consulta de expedientes

**Tipo:** Sistema del backend por API HTTP; integración real de API y persistencia.

**Qué explicar y comprobar:** El listado devolvió el expediente creado. La consulta del detalle devolvió su código e identificador correctos con HTTP 200.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-caso-funcional.ps1 -Caso 10
```

**Alternativa con Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP10_consultaListadoYDetalleExpediente"
```

En la salida, muestra las líneas `[CP10]` y verifica que el resultado final del caso sea `APROBADO`.

### CP11. Solicitud de cotización

**Tipo:** Sistema del backend por API HTTP; integración real de API y persistencia.

**Qué explicar y comprobar:** HTTP: solicitud válida 201 y una nueva cotización persistida, con el cliente correcto y FOB 1000,00. Un FOB negativo devolvió 400 sin agregar registros.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-caso-funcional.ps1 -Caso 11
```

**Alternativa con Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP11_solicitudPortalCreaRegistroYRechazaInvalido"
```

En la salida, muestra las líneas `[CP11]` y verifica que el resultado final del caso sea `APROBADO`.

### CP12. Aceptación de cotización

**Tipo:** Integración de servicios y persistencia; llamada directa al servicio, sin probar la interfaz HTTP de esa operación.

**Qué explicar y comprobar:** La aceptación quedó registrada, mantuvo SIN_PAGO y fijó el saldo en USD 100,00. Una segunda aceptación fue rechazada.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-caso-funcional.ps1 -Caso 12
```

**Alternativa con Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP12_aceptarCotizacionPersisteSaldoYObservacion"
```

En la salida, muestra las líneas `[CP12]` y verifica que el resultado final del caso sea `APROBADO`.

### CP13. Liquidación financiera

**Tipo:** Integración de servicios y persistencia; llamada directa al servicio, sin probar la interfaz HTTP de esa operación.

**Qué explicar y comprobar:** La emisión persistió subtotal S/ 550,00, IGV S/ 99,00, total S/ 1649,00 y USD 439,73 a TC 3,75. La consulta por expediente recuperó la liquidación emitida.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-caso-funcional.ps1 -Caso 13
```

**Alternativa con Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP13_liquidacionEmitePersisteYConsulta"
```

En la salida, muestra las líneas `[CP13]` y verifica que el resultado final del caso sea `APROBADO`.

### CP14. Carga y descarga de documentos

**Tipo:** Sistema del backend por API HTTP; integración real de API y persistencia.

**Qué explicar y comprobar:** La carga multipart devolvió 201 y creó el registro del adjunto. La descarga devolvió 200, el nombre y el contenido exactos del archivo original.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-caso-funcional.ps1 -Caso 14
```

**Alternativa con Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP14_cargaYDescargaConservaContenido"
```

En la salida, muestra las líneas `[CP14]` y verifica que el resultado final del caso sea `APROBADO`.

### CP15. Consulta de mercancías

**Tipo:** Sistema del backend por API HTTP; integración real de API y persistencia.

**Qué explicar y comprobar:** Con una mercancía de prueba persistida: listado 200; detalle y búsqueda por código 200 coincidentes; identificador inexistente 404.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-caso-funcional.ps1 -Caso 15
```

**Alternativa con Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP15_mercanciasListaDetalleCodigoReal"
```

En la salida, muestra las líneas `[CP15]` y verifica que el resultado final del caso sea `APROBADO`.

## Ejecutar los 15 casos juntos

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest"
```

La clase ejecuta los casos en orden de CP01 a CP15. El resultado esperado es 15 pruebas, 0 fallos, 0 errores y 0 omitidas; comprueba el resumen de tu ejecución.

## Dónde encontrar los resultados

Con el ejecutor individual, cada ejecución crea su propia carpeta:

```text
backend-sga/target/casos-funcionales/CPxx/FECHA-HORA/
  resultados-verificados.txt
  ejecucion.txt
  maven.log
  TEST-pe.sga.functional.BusinessFunctionalCasesTest.xml
```

`resultados-verificados.txt` contiene los pasos y valores del caso sin el ruido del arranque. `maven.log` conserva la salida completa. El ejecutor exige código de salida correcto, XML nuevo con una prueba sin fallos ni omisiones y mensaje final de éxito.

Con Maven directo, el reporte actual se guarda en:

```text
backend-sga/target/surefire-reports/TEST-pe.sga.functional.BusinessFunctionalCasesTest.xml
```

Este archivo se reemplaza al repetir la clase. No confundas reportes anteriores con la ejecución que estás presentando.

## Ejemplo para explicar el caso 2

“Primero registro una empresa y consulto la base de pruebas para verificar su documento y correo. El conteo aumenta en una fila. Después intento registrar la misma empresa con el mismo documento: el servicio lanza el rechazo esperado. Finalmente vuelvo a contar y compruebo que la cantidad se mantiene, por lo que no se guardó un duplicado.”

Apoya esta explicación mostrando los valores reales impresos por esa ejecución. El rechazo se captura con `assertThrows`, por eso no aparece como un error de Maven.

## Alcance y conservación de evidencias

Las evidencias nuevas se generan en `backend-sga/target`, que Git ignora. Guarda la carpeta de cada ejecución individual para presentarla al docente; los datos y los archivos generados no necesitan subirse al repositorio. Los reportes históricos de otras carpetas no sustituyen una ejecución actual.

Estas pruebas cubren los escenarios de backend descritos, no una certificación completa del sistema, su interfaz web o su ejecución sobre MySQL.

## Compatibilidad con main

Se incorporó `main` en `0e43733` (19 de noviembre de 2026), que renombró `backend` a `backend-sga`. Los comandos usan la ruta nueva. La incorporación se hace en la rama de pruebas; no modifica directamente `main`.

`CotizacionController` ahora requiere `IReporteJasperService` en su constructor. Se adaptaron los tests existentes de controladores para inyectar ese mock adicional. Esas clases son pruebas unitarias de controlador, distintas de las CP01–CP15. Maven compila todos los tests incluso al seleccionar un caso; un constructor desactualizado podía bloquear también los comandos individuales.

El ejecutor ubica Maven por `$PSScriptRoot`, sin depender del nombre anterior del módulo. Usa la versión de la rama que incorpora estos cambios. Si main vuelve a cambiar, repite la comparación y las pruebas antes de integrar.


### Verificación realizada el 19 de noviembre de 2026

Sobre la integración con `main` en `0e43733`, usando Java 21 y Windows PowerShell 5.1:

- `mvnw.cmd -o test`: **346 pruebas, 0 fallos, 0 errores y 0 omitidas**, incluidas las 15 funcionales. `-o` usa dependencias ya presentes en la caché; omítelo para permitir descargas en otro equipo.
- Los **15 comandos individuales** con `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-caso-funcional.ps1 -Caso N`, para N de 1 a 15, terminaron APROBADOS.
- Se comprobó para cada caso el XML nuevo con exactamente su método, una prueba sin fallos ni omisiones y el archivo `resultados-verificados.txt` con su mensaje final de éxito.
- Los 15 selectores Maven documentados coinciden con los métodos reales de Java y con los selectores del ejecutor.
- No hay diferencias respecto de ese `main` en código productivo, `pom.xml`, base de datos ni frontends. Las adaptaciones propias de la rama son de pruebas y documentación.

Estos resultados corresponden a las revisiones y el entorno indicados, no garantizan cambios posteriores de main ni sustituyen una prueba sobre MySQL o un navegador.
