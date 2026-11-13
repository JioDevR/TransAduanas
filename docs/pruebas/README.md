# Pruebas automatizadas del backend

Esta carpeta relaciona los casos documentados con las pruebas ejecutables en `backend/src/test/java/pe/sga`. Las dependencias añadidas a `backend/pom.xml` tienen alcance `test`. No se modifica código productivo.

## Casos funcionales del 1 al 15

La guía actual de las 15 funcionalidades y sus comandos individuales está en [funcionales/README.md](funcionales/README.md). El ejecutor `backend/ejecutar-caso-funcional.ps1` permite elegir un caso y guardar su evidencia por ejecución. Las evidencias históricas descritas más abajo deben distinguirse de los resultados de una ejecución nueva.

## Ejecutar desde el proyecto

Requisitos: Java 21, conexión para descargar dependencias Maven y permiso para conexiones locales. Abrir una terminal en `backend` y ejecutar:

```powershell
.\ejecutar-pruebas.cmd
```

El script usa el repositorio Maven local predeterminado de Windows para el agente Mockito. Si se usa otro repositorio, adaptar la ruta del agente. En otros sistemas:

```sh
./mvnw test '-DargLine=-javaagent:'"$HOME"'/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar'
```

Las pruebas funcionales cargan Spring Boot y usan servicios, JPA y H2 en memoria en modo MySQL. Abren un puerto local aleatorio. Deshabilitan la importación de configuración local, no usan la base MySQL del proyecto y escriben archivos QA en `backend/target/qa-files`. La base temporal desaparece al cerrar la JVM.

## Cobertura y límites

| Suite | Nivel | Casos |
|---|---|---:|
| ControllerFunctionalTest | Unitarias con Mockito | 20 |
| AllControllersFunctionalTest | Unitarias con Mockito | 219 |
| IntegrationPersistenceTest | Integración de servicios, JPA y H2 | 8 |
| SystemApiTest | Sistema del backend por HTTP real | 12 |
| AcceptanceBusinessTest | Aceptación automatizada por API | 4 |

Las 239 unitarias incluyen los 21 controladores; invocan Java directamente con servicios simulados y no activan filtros HTTP, `@Valid` ni `@PreAuthorize`. Los 24 casos restantes prueban escenarios seleccionados; no implican cobertura HTTP de todos los controladores. La aceptación del usuario de negocio, las pantallas del frontend y MySQL están pendientes.

## Defecto abierto DEF01

`SystemApiTest.SY04_rechazoSinToken` espera HTTP 401 con `application/json` para una consulta de clientes sin token. El servidor devuelve HTTP 401 con `text/html;charset=UTF-8`. Se conserva la aserción y el defecto, por lo que Maven termina con `BUILD FAILURE` por una prueba fallida. No es un error de compilación. El acceso permanece rechazado.

Ver `evidencias/resumen.json` para el resultado de la ejecución y los XML de Surefire adjuntos para los métodos individuales. El log muestra un problema de serialización de `LocalDateTime` en la respuesta de seguridad; no se implementa la corrección en esta rama.

## Evidencias y capturas

1. Abrir el proyecto en el IDE y mostrar `backend/src/test/java/pe/sga` en el explorador.
2. Ejecutar el script en la terminal del IDE.
3. Capturar explorador, archivo de prueba y resultado general con fecha y totales.
4. Capturar los resúmenes de cada suite y el detalle del fallo SY04.
5. Consultar los reportes originales recién generados en `backend/target/surefire-reports` y la respuesta de SY04 en `backend/target/qa-evidence/SY04-response.txt`.

Los XML publicados omiten propiedades del equipo y logs extensos; conservan nombres, tiempos y estados de las pruebas. Los inventarios CSV enlazan los códigos de las fichas con sus métodos. Las capturas se toman de la ejecución real dentro del proyecto.

## Postman

`TransAduanas.postman_collection.json` contiene nueve solicitudes preparadas y **no ejecutadas**. Configurar sus variables y utilizar un entorno QA antes de ejecutarla. No contiene credenciales reales. Los resultados HTTP de las suites anteriores se obtienen con Java HttpClient, no con Postman.
