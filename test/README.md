# Manual de pruebas por tipo — TransAduanas

Este manual organiza **las 346 pruebas ejecutables del backend** por su alcance real. La clasificación usa etiquetas JUnit `@Tag`; los nombres de archivos o carpetas no determinan el tipo. Los grupos son excluyentes para ejecutar y contar: cada prueba pertenece a un solo grupo principal.

## Selección de 15 para presentar al docente

La [selección actual para la evaluación](../docs/pruebas/funcionales/README.md) contiene **4 unitarias, 4 de integración, 4 de sistema del backend y 3 de aceptación automatizada**. Usa la etiqueta `docente15`, incluye herramientas y comandos para cada caso y sustituye la selección anterior que solo tenía integración y sistema. Son 15 métodos del inventario siguiente, no 15 pruebas nuevas ni un quinto grupo principal.

```powershell
# Desde backend-sga: solo las 15 seleccionadas para el docente
.\mvnw.cmd test "-Dgroups=docente15" "-Dsurefire.reportNameSuffix=docente15"
```

## Preparación

Desde la raíz del repositorio, en PowerShell:

```powershell
Set-Location -LiteralPath .\backend-sga
java -version
.\mvnw.cmd -v
```

Se requiere Java 21 y dependencias Maven disponibles o conexión para descargarlas. Los comandos siguientes se ejecutan desde `backend-sga`. El archivo se escribe `mvnw.cmd`, sin una barra entre `mvnw` y `.cmd`.

## Resumen

| Grupo principal | Etiqueta JUnit | Cantidad | Qué comprueba |
|---|---|---:|---|
| Unitarias | `unitaria` | 293 | Una clase o contrato en memoria; dependencias del negocio simuladas cuando corresponde. |
| Integración | `integracion` | 30 | Colaboración real con Spring/seguridad, persistencia H2, archivos o plantillas/PDF según el caso. |
| Sistema del backend por HTTP | `sistema` | 19 | Recorrido HTTP real por el backend con H2. |
| Aceptación automatizada del backend | `aceptacion` | 4 | Escenarios dado/cuando/entonces sobre reglas del negocio y permisos, ejecutados por HTTP. |
| **Total** | | **346** | Cada método se cuenta una sola vez. |

“Funcional” describe el objetivo de verificar comportamiento, no un grupo adicional excluyente. Las pruebas de aceptación también recorren el sistema del backend, pero se agrupan por separado para no contarlas dos veces. No son una aceptación formal del docente o usuario. Seguridad es una finalidad transversal: hay comprobaciones unitarias JWT, integración de filtros y permisos HTTP.

## 1. Pruebas unitarias

Aíslan la clase bajo prueba. Usan Mockito para simular servicios o repositorios cuando son dependencias externas del caso; otras comprueban objetos o serialización en memoria. No necesitan MySQL, H2, navegador ni servidor HTTP.

```powershell
.\mvnw.cmd test "-Dgroups=unitaria" "-Dsurefire.reportNameSuffix=unitaria"
```

**Resultado esperado:** 293 pruebas sin fallos. Por ejemplo, un repositorio simulado indica que ya existe un documento y el servicio debe rechazarlo. Eso no demuestra una consulta o inserción en una base real.

Para ejecutar solo las 7 unitarias de clientes:

```powershell
.\mvnw.cmd test "-Dtest=ClienteServiceImplTest"
```

Para ejecutar solo el rechazo unitario de un documento duplicado:

```powershell
.\mvnw.cmd test "-Dtest=ClienteServiceImplTest#crear_documentoDuplicado_lanzaBadRequest"
```

## 2. Pruebas de integración

Comprueban colaboración real entre componentes. El grupo distingue estos alcances:

- Servicio, JPA y H2: `IntegrationPersistenceTest` y 8 casos CP.
- Configuración Spring MVC y filtros de seguridad con MockMvc: `SecurityConfigTest`. Usa colaboradores simulados; no prueba base de datos ni servidor HTTP real.
- Almacenamiento y sistema de archivos temporal: `LocalStorageServiceTest`.
- Plantillas Thymeleaf y generación PDF real con repositorios simulados: `ReporteComercialPdfTest`. No es integración con una base de datos.

```powershell
.\mvnw.cmd test "-Dgroups=integracion" "-Dsurefire.reportNameSuffix=integracion"
```

**Resultado esperado:** 30 pruebas sin fallos. Las CP de este grupo son **CP01, CP02, CP04, CP05, CP07, CP08, CP12 y CP13**.

## 3. Pruebas de sistema del backend

Recorren HTTP real en un puerto local aleatorio: controlador, seguridad cuando corresponde, servicios y persistencia. Usan H2 temporal. No prueban navegador, frontend completo, infraestructura de despliegue ni MySQL real.

```powershell
.\mvnw.cmd test "-Dgroups=sistema" "-Dsurefire.reportNameSuffix=sistema"
```

**Resultado esperado:** 19 pruebas sin fallos: 12 en `SystemApiTest` y **CP03, CP06, CP09, CP10, CP11, CP14 y CP15**.

## 4. Otras: aceptación automatizada

Son 4 escenarios de `AcceptanceBusinessTest`: formalización de prospecto, reactivación, perfil y permisos del portal, y descarga de documentos restringida a su empresa. Se ejecutan por HTTP real con H2, en formato de escenarios del negocio. Su nivel técnico también es sistema del backend; aquí se separan por su finalidad.

```powershell
.\mvnw.cmd test "-Dgroups=aceptacion" "-Dsurefire.reportNameSuffix=aceptacion"
```

**Resultado esperado:** 4 pruebas sin fallos. Una aprobación automática no sustituye la validación manual del docente o usuario.

## Inventario completo de clases

Todas las rutas siguientes son relativas a `backend-sga/src/test/java/pe/sga/`.

| Clase | Cantidad | Grupo principal |
|---|---:|---|
| `DataInitializerTest` | 2 | unitaria |
| `JwtTokenProviderTest` | 6 | unitaria |
| `SecurityConfigTest` | 7 | integracion |
| `AllControllersFunctionalTest` | 219 | unitaria |
| `ControllerFunctionalTest` | 20 | unitaria |
| `TransporteDTOTest` | 3 | unitaria |
| `AcceptanceBusinessTest` | 4 | aceptacion |
| `BusinessFunctionalCasesTest` | 15 | Mixta: 8 integración / 7 sistema |
| `IntegrationPersistenceTest` | 8 | integracion |
| `SystemApiTest` | 12 | sistema |
| `TransporteEntidadesTest` | 1 | unitaria |
| `ClienteServiceImplTest` | 7 | unitaria |
| `ControlAlmacenServiceImplTest` | 5 | unitaria |
| `CotizacionPagoServiceImplTest` | 7 | unitaria |
| `DespachoTransporteServiceTest` | 8 | unitaria |
| `ExpedienteServiceImplTest` | 7 | unitaria |
| `LiquidacionFinancieraServiceTest` | 6 | unitaria |
| `LocalStorageServiceTest` | 6 | integracion |
| `PortalServiceImplTest` | 2 | unitaria |
| `ReporteComercialPdfTest` | 1 | integracion |

Los nombres `AllControllersFunctionalTest` y `ControllerFunctionalTest` se mantienen para no romper sus selectores actuales. Sus dependencias están simuladas: sus casos son unitarios de controlador pese al nombre. `TransporteEntidadesTest`, aunque esté en la carpeta `repository`, solo comprueba objetos en memoria y no consulta una base.

## CP01–CP15 complementarios: selección anterior

La [guía de CP complementarios](../docs/pruebas/funcionales/CASOS-CP-COMPLEMENTARIOS.md) conserva su numeración. La [selección actual del docente](../docs/pruebas/funcionales/README.md) usa códigos PU/PI/PS/PA y cubre los cuatro tipos. Los comandos individuales y el ejecutor `ejecutar-caso-funcional.ps1` siguen disponibles.

Ejecutar los 15 casos juntos:

```powershell
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest"
```

Ese comando mezcla 8 de integración y 7 de sistema; no incluye las 293 unitarias ni los 4 escenarios de aceptación. Los mensajes `[CPxx]` se conservan para esos casos complementarios. La selección actual agrega mensajes `[PUxx]`, `[PIxx]`, `[PSxx]` y `[PAxx]` con el tipo, herramientas, objetivo y resultado de cada método seleccionado; los demás casos siguen mostrando principalmente el resumen Maven y sus errores si fallan.

## Ejecutar todo

```powershell
.\mvnw.cmd test
```

Ejecuta los cuatro grupos: 346 pruebas en esta versión. Maven compila todos los tests antes de aplicar la selección; si hay un error de compilación, puede bloquear un grupo aunque el archivo con error pertenezca a otro.

## Evidencias y exposición

Los cuatro comandos agregan un sufijo al nombre del reporte, para distinguir grupos y evitar que las dos selecciones de `BusinessFunctionalCasesTest` reemplacen el mismo archivo:

```text
backend-sga/target/surefire-reports/
  TEST-pe.sga.functional.BusinessFunctionalCasesTest-integracion.xml
  TEST-pe.sga.functional.BusinessFunctionalCasesTest-sistema.xml
  TEST-pe.sga.functional.AcceptanceBusinessTest-aceptacion.xml
  ...-unitaria.xml
```

Surefire genera XML y TXT por clase. Muestra el comando, la etiqueta elegida y el resumen `Tests run / Failures / Errors / Skipped`. Un rechazo esperado puede ser una prueba aprobada. Los reportes de `target` son locales y Git los ignora. Repetir el mismo grupo reemplaza sus reportes: copia los archivos si necesitas conservar otra ejecución.

La ejecución sin `surefire.reportNameSuffix` usa los nombres originales de `target/surefire-reports`, cuyos archivos se reemplazan al repetir una clase. No mezcles reportes antiguos con los de la ejecución actual. Los datos H2 desaparecen al finalizar la JVM; no aparecerán en el listado de clientes de MySQL.

## Alcance pendiente

Este inventario cubre únicamente pruebas Java existentes del backend. No declara implementados Testcontainers/MySQL, pruebas visuales E2E con navegador, carga/rendimiento, CI/CD o una auditoría de seguridad. Esos trabajos requieren ejecución y evidencias propias. Los tests frontend existentes en otros módulos quedan fuera de este inventario y de los 346 casos.


## Verificación de esta organización

El 19 de noviembre de 2026 se ejecutaron los cuatro grupos por separado sobre la rama compatible con `main` en `0e43733`, usando Java 21. Los resultados fueron 293 unitarias, 30 de integración, 19 de sistema y 4 de aceptación; 0 fallos, 0 errores y 0 omitidas. Se revisaron los XML con sufijo de grupo para comprobar **346 métodos únicos, sin duplicación entre grupos**. Se usó `-o` porque las dependencias ya estaban en caché; los comandos del manual permiten descargarlas en otros equipos.
