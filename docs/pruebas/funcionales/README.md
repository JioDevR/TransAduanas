# Manual principal de las 15 pruebas para la sustentación

**Empieza aquí para preparar y mostrar las pruebas al docente.** Este es el manual de la selección vigente: PU01–PU04, PI01–PI04, PS01–PS04 y PA01–PA03.

## Cómo está organizada la entrega

| Qué necesitas | Dónde está | Para qué sirve |
|---|---|---|
| Las 15 de la sustentación | **Este manual** | Tipo, herramientas, regla comprobada, comandos y evidencias de cada caso. |
| Ejecutar un caso de esta selección | `backend-sga/ejecutar-prueba-docente.ps1` | Elegir un número del 1 al 15 de la tabla de este manual. |
| Catálogo que usa ese ejecutor | `backend-sga/casos-docente.json` | Relacionar número, código, clase Java, método y herramientas. |
| Inventario completo del backend | [Inventario de las 346 pruebas](../../../test/README.md) | Consultar todos los casos y ejecutar grupos completos. |
| Casos anteriores adicionales | [CP complementarios](CASOS-CP-COMPLEMENTARIOS.md) | Consulta opcional; no son la selección vigente para la sustentación. |

Los documentos `docs/compilacion_despliegue/` y `docs/docker_imagenes_y_despliegue.md` describen otra parte del proyecto y **no forman parte del manual ni de los pasos de estas pruebas**. No necesitas utilizarlos para esta sustentación.

## Orden recomendado para sustentar

1. Explica la diferencia entre las 346 pruebas del proyecto y las 15 seleccionadas con la tabla siguiente.
2. Presenta las herramientas usadas para cada tipo.
3. Sigue la preparación de PowerShell.
4. Ejecuta cada caso por su número y explica los resultados observados.
5. Muestra el resumen de las 15 juntas y conserva sus evidencias.

## Total del proyecto y selección de la sustentación

| Tipo | Total existente en el backend | Elegidas para sustentar | Códigos de esta selección |
|---|---:|---:|---|
| Unitarias | 293 | 4 | PU01–PU04 |
| Integración | 30 | 4 | PI01–PI04 |
| Sistema del backend | 19 | 4 | PS01–PS04 |
| Aceptación automatizada | 4 | 3 | PA01–PA03 |
| **Total** | **346** | **15** | Un método por caso |

Puedes explicarlo así: “Ejecutamos 346 pruebas del backend y seleccionamos 15 casos para explicar en detalle los cuatro tipos. Las 15 están incluidas en las 346; no son casos adicionales ni significa que se hayan eliminado las demás”.

Esta es la selección **actual de 15 métodos existentes**, distribuida en **4 unitarias + 4 de integración + 4 de sistema del backend + 3 de aceptación automatizada**. No se agregan 15 pruebas al total del proyecto: se seleccionan 15 de las 346 existentes. La etiqueta JUnit `docente15` identifica exactamente esta selección.

Los códigos PU/PI/PS/PA evitan confundirla con la selección anterior CP01–CP15, que solo contenía integración y sistema. Los métodos Java y los casos CP anteriores se conservan; pueden seguir ejecutándose como pruebas complementarias.

## Herramientas usadas por tipo

| Tipo | Herramientas realmente usadas | Qué se aísla o integra |
|---|---|---|
| Unitarias (4) | JUnit Jupiter 6, Mockito con MockitoExtension, @Mock y @InjectMocks; Maven Surefire | Servicio real aislado; repositorios y otros colaboradores simulados. Sin H2 ni HTTP para la operación. |
| Integración (4) | JUnit Jupiter 6, Spring Boot Test, Spring Data JPA e H2; Maven Surefire | Servicio, repositorio y persistencia reales de prueba; llamada directa al servicio. |
| Sistema del backend (4) | JUnit Jupiter 6, Java HttpClient, Spring Boot Test e H2; Maven Surefire | Peticiones HTTP reales al servidor en un puerto local aleatorio. |
| Aceptación automatizada (3) | JUnit Jupiter 6, Java HttpClient, Spring Boot Test e H2; Maven Surefire | Escenarios del negocio dado/cuando/entonces por HTTP y comprobación de resultados y permisos. |

JUnit ejecuta y verifica aserciones; Mockito simula dependencias solo en las cuatro unitarias; H2 es la base temporal de los otros tipos; HttpClient envía peticiones en sistema y aceptación; Maven/Surefire selecciona los casos y genera reportes. No se usan Selenium, Cucumber, Postman ni Testcontainers para estos 15 métodos.

Sistema y aceptación comparten herramientas porque cambia el propósito, no necesariamente el programa usado. Las de aceptación también recorren técnicamente el backend completo, pero se cuentan por separado por su escenario de negocio. No equivalen a aceptación formal del docente. Ninguno de estos casos demuestra el navegador, el frontend completo o MySQL real.

## Preparación

Los comandos se escriben en la terminal, no en la barra de direcciones de GitHub ni en el navegador. Tras abrir PowerShell, pega el comando y pulsa Enter; ver únicamente su encabezado no significa que se haya ejecutado una prueba.

En PowerShell, desde la raíz del proyecto actualizado:

```powershell
Set-Location -LiteralPath .\backend-sga
java -version
.\mvnw.cmd -v
```

Se requiere Java 21. No necesitas iniciar MySQL, el backend o el navegador. Maven inicia lo necesario; los datos son ficticios. Escribe `mvnw.cmd` sin una barra entre `mvnw` y `.cmd`.

## Índice de las 15 seleccionadas

| N.º | Código | Tipo | Qué prueba |
|---|---|---|---|
| 1 | [PU01](#caso-01) | Unitaria | Normalización y alta de cliente |
| 2 | [PU02](#caso-02) | Unitaria | Rechazo de documento duplicado |
| 3 | [PU03](#caso-03) | Unitaria | Compuerta financiera sin pago aprobado |
| 4 | [PU04](#caso-04) | Unitaria | Aprobación de sobrepago |
| 5 | [PI01](#caso-05) | Integración | Formalización persistida de prospecto |
| 6 | [PI02](#caso-06) | Integración | Registro real y rechazo de duplicado |
| 7 | [PI03](#caso-07) | Integración | Apertura persistida de expediente |
| 8 | [PI04](#caso-08) | Integración | Cálculo y persistencia de liquidación |
| 9 | [PS01](#caso-09) | Sistema del backend | Consulta HTTP de puertos |
| 10 | [PS02](#caso-10) | Sistema del backend | Solicitud HTTP de cotización |
| 11 | [PS03](#caso-11) | Sistema del backend | Carga y descarga HTTP de archivo |
| 12 | [PS04](#caso-12) | Sistema del backend | Autenticación HTTP y perfil JWT |
| 13 | [PA01](#caso-13) | Aceptación automatizada | Escenario de formalización comercial |
| 14 | [PA02](#caso-14) | Aceptación automatizada | Escenario de perfil y permisos del portal |
| 15 | [PA03](#caso-15) | Aceptación automatizada | Escenario de aislamiento documental |

## Ejecutar las 15 seleccionadas juntas

```powershell
.\mvnw.cmd test "-Dgroups=docente15" "-Dsurefire.reportNameSuffix=docente15"
```

El resultado esperado es **15 pruebas, 0 fallos, 0 errores y 0 omitidas**. Se ejecuta un método por caso; las varias aserciones de un método no cuentan como pruebas adicionales. El orden entre clases lo decide JUnit/Maven; para exponer en orden usa los comandos individuales.

## Ejecutar y explicar cada una

Todos los comandos siguientes se ejecutan desde `backend-sga`. El ejecutor nuevo guarda una carpeta independiente con el XML, la salida y un resumen. Las líneas `[PUxx]`, `[PIxx]`, `[PSxx]` o `[PAxx]` muestran el tipo, herramientas, objetivo y resultado. `OBJETIVO` describe lo que se espera comprobar; `RESULTADO APROBADO` solo aparece al terminar correctamente las aserciones reales del método. Los casos CP también conservan sus mensajes de valores observados. Si un caso falla, muestra NO APROBADO y no lo presentes como éxito.

### A. Pruebas unitarias: casos 1 al 4

<a id="caso-01"></a>

#### 1. PU01 — Normalización y alta de cliente

**Tipo:** Unitaria.

**Herramientas:** JUnit Jupiter 6 + Mockito (MockitoExtension, @Mock, @InjectMocks); Maven Surefire.

**Qué comprobar:** Entrada con espacios y campos opcionales nulos: devuelve documento y razon social normalizados, tipo RUC y estado PROSPECTO; el guardado es simulado.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-prueba-docente.ps1 -Caso 1
```

**Alternativa Maven:**

```powershell
.\mvnw.cmd test "-Dtest=ClienteServiceImplTest#crear_datosValidosConOpcionalesNulos_aplicaDefaultsYGuarda"
```

<a id="caso-02"></a>

#### 2. PU02 — Rechazo de documento duplicado

**Tipo:** Unitaria.

**Herramientas:** JUnit Jupiter 6 + Mockito (MockitoExtension, @Mock, @InjectMocks); Maven Surefire.

**Qué comprobar:** Mockito simula que existe el documento; el servicio lanza BadRequestException y nunca llama a save.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-prueba-docente.ps1 -Caso 2
```

**Alternativa Maven:**

```powershell
.\mvnw.cmd test "-Dtest=ClienteServiceImplTest#crear_documentoDuplicado_lanzaBadRequest"
```

<a id="caso-03"></a>

#### 3. PU03 — Compuerta financiera sin pago aprobado

**Tipo:** Unitaria.

**Herramientas:** JUnit Jupiter 6 + Mockito (MockitoExtension, @Mock, @InjectMocks); Maven Surefire.

**Qué comprobar:** Cotizacion aprobada sin pago aprobado: rechaza la apertura y nunca guarda un expediente en el repositorio simulado.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-prueba-docente.ps1 -Caso 3
```

**Alternativa Maven:**

```powershell
.\mvnw.cmd test "-Dtest=ExpedienteServiceImplTest#crear_sinPagoAprobado_lanzaBadRequest"
```

<a id="caso-04"></a>

#### 4. PU04 — Aprobación de sobrepago

**Tipo:** Unitaria.

**Herramientas:** JUnit Jupiter 6 + Mockito (MockitoExtension, @Mock, @InjectMocks); Maven Surefire.

**Qué comprobar:** Pago USD 600 sobre cotizacion USD 500: APROBADO, saldo cero, PAGADO_TOTAL y notificacion con destinatario y mensaje verificados; repositorios simulados.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-prueba-docente.ps1 -Caso 4
```

**Alternativa Maven:**

```powershell
.\mvnw.cmd test "-Dtest=CotizacionPagoServiceImplTest#validarPago_aprobarConSobrepago_recalculaACeroYNotifica"
```

### B. Pruebas de integración: casos 5 al 8

<a id="caso-05"></a>

#### 5. PI01 — Formalización persistida de prospecto

**Tipo:** Integración.

**Herramientas:** JUnit Jupiter 6 + Spring Boot Test + Spring Data JPA + H2; Maven Surefire.

**Qué comprobar:** El servicio convierte PROSPECTO a CLIENTE; esProspecto=false y el nuevo estado se consulta en H2.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-prueba-docente.ps1 -Caso 5
```

**Alternativa Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP01_formalizacionProspectoACliente"
```

<a id="caso-06"></a>

#### 6. PI02 — Registro real y rechazo de duplicado

**Tipo:** Integración.

**Herramientas:** JUnit Jupiter 6 + Spring Boot Test + Spring Data JPA + H2; Maven Surefire.

**Qué comprobar:** Documento y correo se leen de H2; un duplicado genera rechazo y el conteo permanece sin otra fila.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-prueba-docente.ps1 -Caso 6
```

**Alternativa Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP02_registroClienteYDuplicado"
```

<a id="caso-07"></a>

#### 7. PI03 — Apertura persistida de expediente

**Tipo:** Integración.

**Herramientas:** JUnit Jupiter 6 + Spring Boot Test + Spring Data JPA + H2; Maven Surefire.

**Qué comprobar:** Sin pago no crea expediente; con adelanto aprobado lo persiste y vincula; una segunda apertura se rechaza.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-prueba-docente.ps1 -Caso 7
```

**Alternativa Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP07_aperturaExigePagoYPersiste"
```

<a id="caso-08"></a>

#### 8. PI04 — Cálculo y persistencia de liquidación

**Tipo:** Integración.

**Herramientas:** JUnit Jupiter 6 + Spring Boot Test + Spring Data JPA + H2; Maven Surefire.

**Qué comprobar:** Verifica subtotal PEN 550, IGV 99, total PEN 1649, USD 439.73 y recupera por expediente la liquidacion guardada en H2.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-prueba-docente.ps1 -Caso 8
```

**Alternativa Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP13_liquidacionEmitePersisteYConsulta"
```

### C. Pruebas de sistema del backend: casos 9 al 12

<a id="caso-09"></a>

#### 9. PS01 — Consulta HTTP de puertos

**Tipo:** Sistema del backend.

**Herramientas:** JUnit Jupiter 6 + Java HttpClient + Spring Boot Test + H2; Maven Surefire.

**Qué comprobar:** HTTP 200 en listado y detalle coincidente; identificador inexistente devuelve 404.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-prueba-docente.ps1 -Caso 9
```

**Alternativa Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP03_puertosListaDetalleReal"
```

<a id="caso-10"></a>

#### 10. PS02 — Solicitud HTTP de cotización

**Tipo:** Sistema del backend.

**Herramientas:** JUnit Jupiter 6 + Java HttpClient + Spring Boot Test + H2; Maven Surefire.

**Qué comprobar:** Solicitud FOB 1000 devuelve 201 y persiste; FOB negativo devuelve 400 sin agregar filas.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-prueba-docente.ps1 -Caso 10
```

**Alternativa Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP11_solicitudPortalCreaRegistroYRechazaInvalido"
```

<a id="caso-11"></a>

#### 11. PS03 — Carga y descarga HTTP de archivo

**Tipo:** Sistema del backend.

**Herramientas:** JUnit Jupiter 6 + Java HttpClient + Spring Boot Test + H2; Maven Surefire.

**Qué comprobar:** Carga multipart 201, registro persistido y descarga 200 con nombre y contenido exactos.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-prueba-docente.ps1 -Caso 11
```

**Alternativa Maven:**

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest#CP14_cargaYDescargaConservaContenido"
```

<a id="caso-12"></a>

#### 12. PS04 — Autenticación HTTP y perfil JWT

**Tipo:** Sistema del backend.

**Herramientas:** JUnit Jupiter 6 + Java HttpClient + Spring Boot Test + H2; Maven Surefire.

**Qué comprobar:** Login devuelve token no vacio; GET /auth/me con ese token devuelve 200 y el username del usuario autenticado.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-prueba-docente.ps1 -Caso 12
```

**Alternativa Maven:**

```powershell
.\mvnw.cmd test "-Dtest=SystemApiTest#SY02_loginYPerfilJWT"
```

### D. Pruebas de aceptación automatizada: casos 13 al 15

<a id="caso-13"></a>

#### 13. PA01 — Escenario de formalización comercial

**Tipo:** Aceptación automatizada.

**Herramientas:** JUnit Jupiter 6 + Java HttpClient + Spring Boot Test + H2; Maven Surefire.

**Qué comprobar:** Dado un prospecto, cuando un comercial lo formaliza por HTTP, entonces deja de ser prospecto y queda CLIENTE en respuesta y base.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-prueba-docente.ps1 -Caso 13
```

**Alternativa Maven:**

```powershell
.\mvnw.cmd test "-Dtest=AcceptanceBusinessTest#AC01_dadoProspectoCuandoFormalizaEntoncesEsCliente"
```

<a id="caso-14"></a>

#### 14. PA02 — Escenario de perfil y permisos del portal

**Tipo:** Aceptación automatizada.

**Herramientas:** JUnit Jupiter 6 + Java HttpClient + Spring Boot Test + H2; Maven Surefire.

**Qué comprobar:** Dado un cliente autenticado, obtiene su perfil con su ID y documento; el acceso a usuarios internos devuelve 403.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-prueba-docente.ps1 -Caso 14
```

**Alternativa Maven:**

```powershell
.\mvnw.cmd test "-Dtest=AcceptanceBusinessTest#AC03_dadoPortalCuandoConsultaEntoncesPerfilPropio"
```

<a id="caso-15"></a>

#### 15. PA03 — Escenario de aislamiento documental

**Tipo:** Aceptación automatizada.

**Herramientas:** JUnit Jupiter 6 + Java HttpClient + Spring Boot Test + H2; Maven Surefire.

**Qué comprobar:** El propietario carga y descarga su archivo; otro cliente recibe 403 y no obtiene el contenido.

**Comando recomendado:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar-prueba-docente.ps1 -Caso 15
```

**Alternativa Maven:**

```powershell
.\mvnw.cmd test "-Dtest=AcceptanceBusinessTest#AC04_dadoDocumentoCuandoOtroClienteDescargaEntoncesDenegado"
```

## Qué mostrar y cómo interpretar el resultado

| Mensaje | Qué significa |
|---|---|
| `TIPO` y `HERRAMIENTAS` | Clasificación y herramientas del método seleccionado. |
| `OBJETIVO` | Condiciones esperadas que el código de la prueba debe comprobar; todavía no es el resultado. |
| `METODO` | Clase y método Java ejecutados. |
| `VERIFICADO`, `ANTES` o `RECHAZO ESPERADO`, si el caso los imprime | Valores observados o rechazo que se esperaba comprobar. |
| `RESULTADO ... APROBADO` | JUnit terminó correctamente las aserciones del método. |
| `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` | Un caso individual terminó sin fallos ni omisiones. |
| `Tests run: 15, ...` | Resultado de la selección completa, no de las 346. |

Un rechazo de documento duplicado o acceso de otra empresa puede ser el comportamiento correcto. No confundas ese rechazo esperado con un fallo de la prueba. Los valores de identificadores y fechas pueden variar entre ejecuciones.

## Si un comando no inicia

Desde `backend-sga`, comprueba:

```powershell
Get-Location
Get-Item .\mvnw.cmd
Get-Item .\ejecutar-prueba-docente.ps1
```

Si no aparecen los archivos, vuelve a la carpeta correcta del proyecto actualizado. Escribe `.\mvnw.cmd`, sin otra barra antes de `.cmd`. Usa el comando recomendado `powershell.exe -NoProfile -ExecutionPolicy Bypass -File ...` para ejecutar el script sin cambiar permanentemente la política del equipo.

## Evidencias

El ejecutor guarda resultados en `backend-sga/target/evaluacion-docente/CODIGO/FECHA-HORA/`: `ejecucion.txt`, `maven.log`, `resultados-verificados.txt` y XML de Surefire. Solo aprueba con código Maven cero, exactamente un método esperado, sin fallos ni omisiones y con el mensaje final JUnit correspondiente.

La ejecución conjunta genera archivos `*-docente15.xml` en `backend-sga/target/surefire-reports`. Copia los reportes si quieres conservarlos antes de repetir el mismo comando. Todo `target` es local y Git lo ignora. Un rechazo esperado por una regla del negocio puede ser el resultado correcto de una prueba.

## Relación con el resto de pruebas

El [inventario general](../../../test/README.md) contiene las 346 pruebas y comandos para todos los grupos. Los [CP complementarios](CASOS-CP-COMPLEMENTARIOS.md) mantienen los antiguos 15 casos y su ejecutor `ejecutar-caso-funcional.ps1`; ese ejecutor anterior **no es el que selecciona estos nuevos 15 casos de cuatro tipos**.


## Verificación de la versión corregida 0.5.2

El 20 de noviembre de 2026 pasaron las 346 pruebas del backend, incluidas las de esta selección. Las correcciones cambian el rechazo de métodos HTTP a 405 y exigen un secreto JWT externo de al menos 32 bytes al iniciar la aplicación real. Las pruebas Spring declaran su clave ficticia y su H2: no copies claves del servicio para ejecutar estos casos.

Al actualizar este manual se repitió la selección docente15: 15 aprobadas, cero fallos/errores/omisiones. También se ejecutó el caso 1 del ejecutor: PU01 APROBADO. Los logs están en evidencias-revision/manual-docente15.log y manual-ejecutor.log. No se repitieron los otros 14 comandos individuales en esta actualización. Conserva los XML de cada ejecución; las cifras anteriores no sustituyen tus resultados nuevos.

## Pruebas de interfaces y relación con seguridad

Desde la raíz del proyecto, con Node 24 y npm:

```powershell
npm.cmd --prefix .\app-sga ci
npm.cmd --prefix .\app-b2b ci
npm.cmd --prefix .\app-sga test
npm.cmd --prefix .\app-b2b test
```

Resultados observados: SGA, 130 pruebas; B2B, 63. La landing no tiene script de pruebas automatizadas y se verifica mediante su compilación y publicación. Las 539 pruebas corresponden a 346 backend + 130 SGA + 63 B2B. Seguridad ya está incluida en el backend y no vuelve a sumarse.

Continúa con el [manual de seguridad](../seguridad/README.md) y el [manual de despliegue](../despliegue/README.md). Las pruebas de monitoreo se prepararán después con el material teórico y las indicaciones de evaluación.
