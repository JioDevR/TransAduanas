# Las 15 pruebas para la evaluación: cuatro tipos y herramientas
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

En PowerShell, desde la raíz del proyecto actualizado:

```powershell
Set-Location -LiteralPath .\backend-sga
java -version
.\mvnw.cmd -v
```

Se requiere Java 21. No necesitas iniciar MySQL, el backend o el navegador. Maven inicia lo necesario; los datos son ficticios. Escribe `mvnw.cmd` sin una barra entre `mvnw` y `.cmd`.

## Selección completa

| N.º | Código | Tipo | Qué prueba |
|---|---|---|---|
| 1 | PU01 | Unitaria | Normalización y alta de cliente |
| 2 | PU02 | Unitaria | Rechazo de documento duplicado |
| 3 | PU03 | Unitaria | Compuerta financiera sin pago aprobado |
| 4 | PU04 | Unitaria | Aprobación de sobrepago |
| 5 | PI01 | Integracion | Formalización persistida de prospecto |
| 6 | PI02 | Integracion | Registro real y rechazo de duplicado |
| 7 | PI03 | Integracion | Apertura persistida de expediente |
| 8 | PI04 | Integracion | Cálculo y persistencia de liquidación |
| 9 | PS01 | Sistema del backend | Consulta HTTP de puertos |
| 10 | PS02 | Sistema del backend | Solicitud HTTP de cotización |
| 11 | PS03 | Sistema del backend | Carga y descarga HTTP de archivo |
| 12 | PS04 | Sistema del backend | Autenticación HTTP y perfil JWT |
| 13 | PA01 | Aceptacion automatizada | Escenario de formalización comercial |
| 14 | PA02 | Aceptacion automatizada | Escenario de perfil y permisos del portal |
| 15 | PA03 | Aceptacion automatizada | Escenario de aislamiento documental |

## Ejecutar las 15 seleccionadas juntas

```powershell
.\mvnw.cmd test "-Dgroups=docente15" "-Dsurefire.reportNameSuffix=docente15"
```

El resultado esperado es **15 pruebas, 0 fallos, 0 errores y 0 omitidas**. Se ejecuta un método por caso; las varias aserciones de un método no cuentan como pruebas adicionales. El orden entre clases lo decide JUnit/Maven; para exponer en orden usa los comandos individuales.

## Ejecutar y explicar cada una

Todos los comandos siguientes se ejecutan desde `backend-sga`. El ejecutor nuevo guarda una carpeta independiente con el XML, la salida y un resumen. Las líneas `[PUxx]`, `[PIxx]`, `[PSxx]` o `[PAxx]` muestran el tipo, herramientas, objetivo y resultado. `OBJETIVO` describe lo que se espera comprobar; `RESULTADO APROBADO` solo aparece al terminar correctamente las aserciones reales del método. Los casos CP también conservan sus mensajes de valores observados. Si un caso falla, muestra NO APROBADO y no lo presentes como éxito.

### 1. PU01 — Normalización y alta de cliente

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

### 2. PU02 — Rechazo de documento duplicado

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

### 3. PU03 — Compuerta financiera sin pago aprobado

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

### 4. PU04 — Aprobación de sobrepago

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

### 5. PI01 — Formalización persistida de prospecto

**Tipo:** Integracion.

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

### 6. PI02 — Registro real y rechazo de duplicado

**Tipo:** Integracion.

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

### 7. PI03 — Apertura persistida de expediente

**Tipo:** Integracion.

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

### 8. PI04 — Cálculo y persistencia de liquidación

**Tipo:** Integracion.

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

### 9. PS01 — Consulta HTTP de puertos

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

### 10. PS02 — Solicitud HTTP de cotización

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

### 11. PS03 — Carga y descarga HTTP de archivo

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

### 12. PS04 — Autenticación HTTP y perfil JWT

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

### 13. PA01 — Escenario de formalización comercial

**Tipo:** Aceptacion automatizada.

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

### 14. PA02 — Escenario de perfil y permisos del portal

**Tipo:** Aceptacion automatizada.

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

### 15. PA03 — Escenario de aislamiento documental

**Tipo:** Aceptacion automatizada.

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

## Evidencias

El ejecutor guarda resultados en `backend-sga/target/evaluacion-docente/CODIGO/FECHA-HORA/`: `ejecucion.txt`, `maven.log`, `resultados-verificados.txt` y XML de Surefire. Solo aprueba con código Maven cero, exactamente un método esperado, sin fallos ni omisiones y con el mensaje final JUnit correspondiente.

La ejecución conjunta genera archivos `*-docente15.xml` en `backend-sga/target/surefire-reports`. Copia los reportes si quieres conservarlos antes de repetir el mismo comando. Todo `target` es local y Git lo ignora. Un rechazo esperado por una regla del negocio puede ser el resultado correcto de una prueba.

## Relación con el resto de pruebas

El [inventario general](../../../test/README.md) contiene las 346 pruebas y comandos para todos los grupos. Los [CP complementarios](CASOS-CP-COMPLEMENTARIOS.md) mantienen los antiguos 15 casos y su ejecutor `ejecutar-caso-funcional.ps1`; ese ejecutor anterior **no es el que selecciona estos nuevos 15 casos de cuatro tipos**.


## Verificación de la selección actual

El 19 de noviembre de 2026, sobre la rama compatible con main `0e43733`, se verificó:

- Selección `docente15`: **15 métodos, 0 fallos, 0 errores y 0 omitidas**; 4 unitarias, 4 integración, 4 sistema del backend y 3 aceptación automatizada.
- Los **15 comandos individuales** de `ejecutar-prueba-docente.ps1` terminaron APROBADOS en Windows PowerShell 5.1. Se comprobaron el método y clase esperados, el XML nuevo y los mensajes de tipo, herramientas y resultado de cada caso.
- Suite completa tras añadir los metadatos: **346 pruebas, 0 fallos, 0 errores y 0 omitidas**. No se quitaron aserciones ni se añadieron copias artificiales de los tests.
- No se modifica código productivo, pom.xml, base de datos ni frontends. Los datos H2 y reportes generados permanecen en target.

Las aserciones originales siguen siendo la comprobación: los metadatos y mensajes no sustituyen el resultado de JUnit. Hay que repetir las pruebas si cambia el código o main.
