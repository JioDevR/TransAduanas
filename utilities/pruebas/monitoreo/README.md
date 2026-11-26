# Pruebas de monitoreo de TransAduanas

## Revisión del 26 de noviembre de 2026

El manual Word y PDF contiene los comandos actuales y las herramientas de M01 a M15. Sus páginas de pruebas se conservan; la página 21 explica las correcciones antes de repetir.

Se corrigió la preparación del contador de M04, se añadieron comprobaciones de CPU a M09 y M14 y se adaptó M10 al almacenamiento configurado por el lanzador protegido. Consultar [LEEME-ENTREGA.md](LEEME-ENTREGA.md) para el alcance de las verificaciones y los requisitos de instalación.

Las notas siguientes conservan el desarrollo y las incidencias originales del laboratorio. Los estados históricos o pendientes allí descritos no sustituyen el resultado de una ejecución nueva.

# Prueba 1 de monitoreo Ingesta y búsqueda de logs

Se ejecutaron dos rondas de diez consultas GET de lectura al catálogo de puertos, con un segundo de separación entre consultas.

| Ronda | Resultado | Estado |
|---|---|---|
| Inicial | 10/10 HTTP 200; diez registros localizables por fecha y endpoint, sin campo de servicio. | Falló por cumplimiento parcial. |
| Después de corregir | 10/10 HTTP 200; diez registros localizables por fecha, service=sga-backend y endpoint. | Exitoso dentro del alcance local. |

## Corrección aplicada

El generador `../despliegue/Preparar-Nginx.ps1` incluye un formato de acceso con service, frontend, endpoint, request_id y upstream. La API se identifica como sga-backend; las páginas se identifican por su interfaz y puerto. Esta identidad proviene de la configuración del servidor, no de un campo enviado por el cliente. Se validó con nginx -t y se recargó Nginx con respaldo. No se reinició el backend ni se modificaron datos de negocio.

La búsqueda comprobada es local sobre access.log. No se declara implementada una plataforma centralizada de ingesta ni la propagación de trazas dentro de Java. El nuevo request_id de Nginx no demuestra todavía correlación de logs del backend.

## Evidencias y repetición

Las dos ejecuciones y sus scripts quedan conservados en `../../../evidencias-monitoreo/M01-20261120-151148-63db51` y `../../../evidencias-monitoreo/M01-20261120-151755-19b129`. Cada una contiene resultado.json, resultado.txt, consultas.csv y eventos-nginx.log. El registro conjunto es `../../../evidencias-monitoreo/registro-prueba-01.csv`. Los respaldos de configuración y la aplicación están en una carpeta correccion-M01-* dentro de esas evidencias.

Desde la raíz del proyecto, repetir únicamente esta prueba:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\docs\pruebas\monitoreo\ejecutar-prueba-01.ps1
```

Las pruebas 2 a 15 siguen pendientes. El resultado inicial no se elimina ni se convierte en aprobado: se conserva junto con la repetición exitosa.

## Registro acumulado y segunda prueba

La primera prueba conserva el fallo inicial y la repetición exitosa. La segunda se ejecutó con tres consultas de lectura: todas respondieron HTTP 200; los tres accesos tienen fecha, servicio y request_id único, pero ninguno incluye nivel o severidad explícita. Estado M02: Falló; incidencia INC-M02-001 abierta. No se aplicaron correcciones durante esta prueba.

Las dos tablas finales se construirán desde `../../../evidencias-monitoreo/resultados-pruebas.csv` (código, herramientas, resultado y estado) e `../../../evidencias-monitoreo/incidencias-pruebas.csv` (código de prueba, incidencia y situación). Se conserva también historial-ejecuciones.csv con todas las rondas y registro-monitoreo.json como registro estructurado. La tabla de resultados usa la última ejecución de cada prueba; el historial y las incidencias conservan los fallos previos.

Evidencia de M02: `../../../evidencias-monitoreo/M02-20261120-152659-980f94/resultado.txt`. La falta de nivel no se sustituye por el código HTTP: 200 es un resultado de la petición, no una severidad del evento. La inspección del formato Logback es una revisión de configuración, no una demostración de trazas dentro del backend.

Las pruebas 3 a 15 siguen pendientes. Para actualizar los registros, ejecutar `python .\docs\pruebas\monitoreo\actualizar_registros.py` desde la raíz del proyecto.


## Rectificación del criterio y corrección de M02

La primera evaluación de M02 fue demasiado estricta al exigir un nivel INFO/ERROR propio de aplicación al access.log de Nginx. Esa ausencia no constituye un defecto de Nginx ni un fallo funcional. El registro revisiones-criterio.json conserva esta rectificación y separa la interpretación del hallazgo real.

La carencia confirmada era que los eventos de Java no incluían servicio explícito ni requestId, y no había registro HTTP de finalización para correlacionarlos. El parche 0.5.3 añade HttpRequestLoggingFilter y MDC, limpia el contexto al terminar y no registra query strings, cuerpos ni credenciales. Logback registra fecha, nivel nativo, service=sga-backend y request_id. Se mantiene la 0.5.2 previa como respaldo. Antes de aplicar se aprobaron 29 pruebas específicas de regresión.

Los criterios de repetición se escribieron antes de ejecutar tres consultas GET normales. Se verificaron los campos apropiados de cada fuente: Nginx con fecha, servicio, ID y estado HTTP; Java con fecha, nivel nativo, servicio e ID. Los tres IDs de respuesta coincidieron con los registros de ambas fuentes. Resultado de M02: EXITOSO en M02-20261120-153751-a23ca1. No se provocaron fallos en las peticiones de esta prueba.

Se conserva la evidencia anterior sin alterarla. La incidencia de las tablas finales describe la falta real de campos del backend; la exigencia inicial de nivel para Nginx queda identificada como rectificación metodológica. Evidencia nueva: ../../../evidencias-monitoreo/M02-20261120-153751-a23ca1/resultado.txt. Solo se ha repetido M02; las pruebas 3 a 15 siguen pendientes.

## Prueba 3 Rotación de logs

M03 se ejecutó en M03-20261120-174029-550831 con Logback y SLF4J extraídos del JAR 0.5.3 del proyecto, usando una copia de su configuración. Se redujo maxFileSize de 50 MB a 64 KB únicamente en la carpeta de laboratorio; la configuración del proyecto y los logs del servicio activo quedaron intactos.

Resultado: EXITOSO. Se emitieron y recuperaron 1500 eventos, con 1500 secuencias únicas, cero pérdidas y cero duplicados. Se generaron 13 archivos .log.gz legibles; el primer evento se encontró en un archivo rotado y el último en el activo. Logback informó cero errores. Se verificó que la configuración del JAR coincide con la fuente del proyecto.

Herramientas: Java 21, javac, librerías reales SLF4J/Logback y Python para descomprimir y comprobar las secuencias. Evidencias: ../../../evidencias-monitoreo/M03-20261120-174029-550831/resultado.txt, inventario-logs.csv y secuencia-verificada.csv. Este resultado acredita rotación por tamaño y conservación local, no retención transcurrida de 30/60 días ni una ejecución al límite productivo de 50 MB.

La ejecución ya está añadida a resultados-pruebas.csv y al historial. No se registró una incidencia nueva porque no se observó un fallo. Las pruebas 4 a 15 siguen pendientes.

## Prueba 4 Conteo de solicitudes

M04 se ejecutó en M04-20261120-175847-ed2e05. Se enviaron 60 consultas GET de lectura al catálogo de puertos, de forma secuencial y a una tasa objetivo de una por segundo. Un observador independiente leyó los eventos request_completed del backend desde la posición inicial del archivo; el contador no se incrementó desde el generador de tráfico.

Resultado: EXITOSO en el alcance local definido antes de empezar. Se obtuvieron 60 respuestas HTTP 200 con success=true, 60 IDs distintos, 60 eventos Nginx y 60 eventos Java, exactamente uno por solicitud en cada log. La ventana medida fue de 60,024 segundos, con una tasa media de 0,9996 solicitudes/s. No se detectó tráfico ajeno ni errores del observador.

Se utilizó un monitor de consola y series CSV, no un stack de métricas de aplicación. El proyecto no tenía Prometheus/Grafana configurados y esta prueba no los instala ni acredita un contador Micrometer. La adaptación de herramienta y los criterios quedaron escritos en criterios-previos.txt antes de enviar consultas. El panel en vivo incluye una lectura final tras 0,5 segundos de espera para vaciar logs; la tasa de resultado usa la ventana medida de 60,024 segundos, excluyendo esa espera.

Evidencias: ../../../evidencias-monitoreo/M04-20261120-175847-ed2e05/resultado.txt; consultas.csv, conteos-por-solicitud.csv, eventos-nginx.log, eventos-backend.log, panel-en-vivo.txt, panel-en-vivo.csv y tasa-por-intervalo.csv. El resultado se añadió al registro acumulado. No hubo incidencias nuevas ni cambios en configuración, servicios o datos de negocio. Las pruebas 5 a 15 siguen pendientes.

## Prueba 5 Inventario de procesos

M05 se ejecutó en M05-20261120-180658-6e8563 usando Windows PowerShell, CIM (Win32_Service/Win32_Process), Get-NetTCPConnection y nginx.pid. Los criterios se registraron antes de los dos muestreos, separados por dos segundos.

Resultado: EXITOSO. Se identificó java.exe PID 11776 escuchando en 8086, mysqld.exe PID 6136 en 3306 y nginx.exe trabajador PID 21012 en 5173/5174/5180. Los servicios TransAduanasBackend y MySQL84 estaban Running. Se comprobó el parentesco con los supervisores PID 13140 y 5060, respectivamente, y con el maestro Nginx PID 14732. Los PID de supervisor y trabajador pueden diferir legítimamente; tampoco se exige un proceso Nginx distinto para cada interfaz.

Las diez verificaciones de puerto/proceso, cinco en cada muestra, fueron correctas y no hubo cambios de PID entre muestras. El inventario es local y acredita presencia, estado y escucha; no prueba aún alertas de caída ni disponibilidad funcional de negocio. No se iniciaron ni detuvieron procesos, no se enviaron consultas a la API y no se modificó configuración o datos.

Evidencias: ../../../evidencias-monitoreo/M05-20261120-180658-6e8563/resultado.txt, muestra-1.json, muestra-2.json, inventario-1.csv, inventario-2.csv e inventario-completo.csv. Se actualizó el registro acumulado y no se añadió una incidencia nueva. Las pruebas 6 a 15 siguen pendientes.

## Prueba 6 Latencia de consultas

M06 se ejecutó en M06-20261120-181342-7246da con 60 consultas GET autenticadas de Clientes activos, una por segundo y sin concurrencia. Las 60 respondieron HTTP 200 con success=true; se correlacionaron 60 eventos Nginx y 60 eventos Java por IDs distintos, exactamente uno de cada fuente por consulta. Se hicieron seis actualizaciones del monitor local de percentiles.

Resultado: EXITOSO como prueba de recolección y visualización local de latencias. Cliente p50/p95/p99: 43,384 / 55,581 / 164,533 ms. Backend p50/p95/p99: 10 / 15 / 145 ms. Se verificaron de nuevo los percentiles a partir del CSV de las 60 mediciones con el método nearest-rank, posición ceil(p*n).

Herramientas: Python (urllib, lector de logs independiente y cálculo de percentiles), Logback y filtro HTTP del backend, Nginx y monitor de consola/CSV. El tiempo del cliente incluye proxy, red local y lectura de respuesta; no debe exigirse igualdad con el tiempo de procesamiento Java. El backend registra milisegundos enteros. Esta muestra pequeña no acredita un SLA ni capacidad bajo concurrencia alta; con 60 valores el p99 de este método coincide con el máximo observado. No se impuso un umbral artificial para declarar un fallo de rendimiento.

No se guardaron credenciales, token ni datos de clientes. Las consultas no crearon ni modificaron clientes; el único cambio habitual de autenticación es la actualización de ultimoAcceso del usuario por el servicio existente. No se cambiaron configuración ni servicios.

Evidencias: ../../../evidencias-monitoreo/M06-20261120-181342-7246da/resultado.txt; consultas.csv, comparacion-cliente-backend.csv, percentiles.csv, panel-percentiles.csv, panel-en-vivo.txt y los registros de Nginx/Java. Se actualizó el registro acumulado; no hubo incidencias nuevas. Las pruebas 7 a 15 siguen pendientes.

Para repetir: establecer MONITOR_USERNAME y MONITOR_PASSWORD en la sesión de forma privada y ejecutar python .\docs\pruebas\monitoreo\ejecutar_prueba_06.py; eliminar esas variables al terminar. No incluir sus valores en capturas o archivos.


## Instrumentación nativa posterior a M06

Se integraron Spring Boot Actuator/Micrometer, Prometheus y Grafana en el parche 0.5.4, con métricas locales en un puerto separado. El dashboard muestra series reales y el target está UP. La configuración, panel JSON, reglas, scripts e instrucciones están en ../../../monitoreo/README.md. Los binarios y datos se mantienen fuera del proyecto.

Las mediciones previas M04 y M06 siguen siendo verificaciones locales mediante logs y scripts; no se transforman retroactivamente en pruebas de Prometheus/Grafana. La comprobación técnica de integración se conserva aparte. El siguiente paso acordado puede ser repetir esos casos con las nuevas herramientas antes de continuar el plan. M07-M15 no se ejecutaron durante esta integración.

## Repetición M04 con Prometheus y Grafana

M04-20261120-185104-4eda85: EXITOSO. Se realizaron 60 lecturas secuenciales del catálogo de puertos en 60,025 segundos, con 60 respuestas HTTP 200 correctas. El contador específico de este endpoint pasó de 12 a 72 en Micrometer, Prometheus y las consultas a la fuente de datos de Grafana: incremento de 60 en las tres herramientas. La consulta PromQL rate con ventana de 30 segundos mostró 1 solicitud/s durante la carga estable. Se correlacionaron 60 eventos Java y 60 Nginx, sin tráfico ajeno.

Se conservaron los criterios previos, las consultas, métricas antes/después, respuestas de Grafana y logs en ../../../evidencias-monitoreo/M04-20261120-185104-4eda85/. La consulta del caso filtra el endpoint de puertos; el dashboard general incluye otras rutas y utiliza 5 minutos para su tasa, por lo que sus valores absolutos no tienen que coincidir. El resultado valida conteo y tasa con las herramientas integradas; no acredita alertas ni pruebas de carga concurrente.

El registro acumulado utiliza esta repetición como último resultado de M04 y conserva la ejecución local anterior. No se detectaron incidencias nuevas. M06 todavía no se ha repetido con Prometheus/Grafana; M07-M15 siguen pendientes. No se ejecutaron otros casos en esta sesión.

## Repetición M06 con Prometheus y Grafana

M06-20261120-185718-6347b6: EXITOSO. Se realizaron 60 consultas autenticadas de Clientes en una ventana de envío de 60,024 s, sin concurrencia: 60 HTTP 200, 60 IDs únicos y un evento por solicitud en cada log Java/Nginx. No se detectó tráfico ajeno ni errores del observador.

Micrometer y Prometheus registraron un incremento de 60. El histograma tuvo 60 observaciones adicionales en +Inf, buckets delta monótonos y suma de duraciones creciente. Hubo seis consultas de visualización a la fuente de Grafana. Los percentiles nativos finales p50/p95/p99 fueron 9,350 / 18,641 / 165,535 ms, coincidentes entre Prometheus y Grafana; el contador inicial de Clientes era cero. Son percentiles interpolados del histograma acumulado, filtrado a GET Clientes HTTP 200. El dashboard general utiliza rate de 5 minutos y todas las rutas, por lo que sus números pueden diferir de esta consulta específica.

Como contraste, los percentiles nearest-rank del cliente fueron 40,906 / 54,513 / 170,180 ms, y los del filtro Java 8 / 16 / 156 ms. Las diferencias son esperables por el alcance y método de medición. Solo en el cálculo local nearest-rank con 60 valores, p99 corresponde al máximo observado. La prueba valida recolección y visualización de latencia bajo baja carga, sin acreditar un SLA ni capacidad bajo concurrencia elevada.

Evidencias: ../../../evidencias-monitoreo/M06-20261120-185718-6347b6/resultado.json; panel-prometheus-grafana.csv, histograma-delta.csv, métricas antes/después, respuestas de Grafana, consultas y correlación de logs. No se guardaron credenciales, tokens ni datos de clientes. La autenticación normal puede actualizar ultimoAcceso. Se conserva M06 anterior en el historial. Sin incidencias nuevas. M07-M15 siguen pendientes; no se ejecutó otro caso.

## Prueba M07 Correlación de una solicitud

M07-20261120-190134-afbdea: FALLO por cobertura incompleta de trazabilidad. Se ejecutó una sola lectura autenticada normal GET /api/v1/clientes?soloActivos=true a través de Nginx, sin provocar errores ni cambiar la configuración. La respuesta fue HTTP 200, success=true y lista válida; no se almacenaron datos de clientes.

El requestId 09b1b510365665563edee0308aa46244 apareció exactamente una vez en el acceso Nginx y una vez en el evento request_completed del backend. No se encontró un evento SQL/JDBC vinculado al mismo identificador. El JAR activo tiene org.hibernate.SQL en WARN y show-sql=false; no se encontraron bibliotecas de tracing, P6Spy ni datasource-proxy en su inventario. Las métricas de Prometheus/Grafana son agregadas y no sustituyen una traza individual de SQL.

Esta incidencia es de observabilidad del tramo backend-MySQL, no de funcionamiento de la consulta ni de disponibilidad de MySQL. No se desactivó ninguna herramienta para producirla. La prueba mantiene el alcance original de correlación de extremo a extremo y no declara éxito parcial como éxito global. Para cerrar la incidencia se necesita instrumentar la ejecución JDBC con el contexto requestId/traceId y repetir M07; solo registrar el método del repositorio no prueba la ejecución SQL. No se habilitó logging de parámetros ni se guardaron credenciales, tokens o datos personales.

Evidencias: ../../../evidencias-monitoreo/M07-20261120-190134-afbdea/resultado.json; criterios-previos.txt, eventos-nginx.log, eventos-backend.log e instrumentacion-activa.json. Se conserva la ejecución fallida y se registra una incidencia abierta. M08-M15 siguen pendientes.

## Corrección y verificación M07: backend 0.5.5

Se añadió JdbcRequestTracing.java, instrumentación de las conexiones y sentencias del pool Hikari. Cada ejecución JDBC dentro de una solicitud HTTP registra el requestId del MDC, un jdbc_span_id, el motor de base de datos, la operación, el resultado y la duración. El evento se emite después de invocar al driver real; no se limita a anunciar la llamada a un repositorio. No registra SQL, parámetros, resultados ni mensajes de excepciones. Mantiene el tipo HikariDataSource, las métricas y el ciclo de vida del pool. Es correlación mediante logs estructurados locales, no una instalación de tracing distribuido OpenTelemetry/Tempo.

Pasaron 28 pruebas automatizadas: trazabilidad JDBC (consulta real H2, correlación, privacidad, propagación de excepción), filtro HTTP, API de sistema, seguridad y métricas. El paquete 0.5.5 se desplegó con SHA256 A46B84CD94257E9A2CC41382A2DA91E3CD1B2F740B7C00A676C7D5C0C51B50E9; se conserva 0.5.4 como anterior. Las tres interfaces y el catálogo respondieron HTTP 200; health UP; métricas Hikari disponibles; Prometheus UP y Grafana saludable. Clientes anónimo y métricas por puerto de negocio respondieron 401; /actuator/env respondió 403, como se esperaba.

M07 se repitió en M07-20261120-190700-0a89d3: EXITOSO. Una lectura normal de Clientes respondió HTTP 200; el requestId a584aa906f0cb1720d0e57b4f3d369cd apareció en un acceso Nginx, un request_completed Java y tres operaciones executeQuery completadas en MySQL, cada una con su propio jdbc_span_id. La ejecución fallida M07-20261120-190134-afbdea queda conservada. INC-M07-001 queda corregida y verificada; no se borra del inventario de incidencias.

Evidencias de corrección: ../../../evidencias-monitoreo/correccion-M07-0.5.5/. Evidencias de repetición: ../../../evidencias-monitoreo/M07-20261120-190700-0a89d3/. Para repetir únicamente el caso, establecer MONITOR_USERNAME y MONITOR_PASSWORD de forma privada y ejecutar python .\docs\pruebas\monitoreo\ejecutar_prueba_07.py, eliminando después esas variables. El despliegue ya fue aplicado; no hay que repetirlo para ejecutar M07. M08-M15 siguen pendientes.


## Prueba M08 Memoria JVM y pausas de GC

M08-20261120-191117-a69e2c: EXITOSO. 60/60 lecturas correctas; 7 muestras. Heap observado en Prometheus/Grafana: 96.690 a 128.690 MiB. Pausas GC nuevas entre capturas nativas: 0; tiempo acumulado adicional: 0.000 ms. Sin GC forzado.

Se realizaron 60 lecturas normales del catálogo de puertos, una por segundo y sin concurrencia. Se consultaron heap, nonheap, número de pausas, tiempo acumulado de GC y tasa de tiempo consumido en GC de 5 minutos. Las consultas a Prometheus y a la fuente de datos de Grafana usaron el mismo instante de evaluación, evitando comparar dos recolecciones distintas. Los paneles existentes de memoria y GC se conservaron como evidencia de configuración. No se exige igualdad entre una lectura nativa instantánea del heap y una muestra previa recolectada por Prometheus.

Ventana de envío: 60.089 s; muestras: 7. Contador GC nativo: 2 -> 2. Tiempo GC nativo acumulado: 12.000 -> 12.000 ms. Estos contadores incluyen eventos anteriores al caso; solo su incremento corresponde a la ventana observada. No se invocó GC manual ni se alteraron configuración, servicios o datos de negocio.

Alcance: Recoleccion y consulta de memoria y pausas de GC bajo baja carga. Cero pausas nuevas es una observacion valida; no se exige un evento GC artificial. No acredita ausencia de fugas, estres, full GC ni estabilidad prolongada. El heap nativo y el recolectado tienen instantes distintos; se contrastan Prometheus y Grafana con el mismo instante de evaluacion.

Evidencias: ../../../evidencias-monitoreo/M08-20261120-191117-a69e2c/resultado.json; muestras-prometheus-grafana.csv, consultas.csv, métricas antes/después, respuestas de Grafana, paneles-configurados.json y criterios-previos.txt. No se detectaron incidencias nuevas. M09-M15 siguen pendientes; no se ejecutó otro caso.


## Prueba M09 CPU y memoria del sistema

M09-20261120-191921-1ec065: EXITOSO. 60 lecturas: 60 correctas; 7 muestras; Java PID 28440, 12 CPU logicas. CPU Windows host 9.62-16.45%; Java 0.16-0.49%. Working set Java 501.91-506.55 MiB. Contraste tiempo CPU Windows/Micrometer: True.

Se realizaron 60 lecturas secuenciales del catálogo de puertos en 60.021 s. El incremento del contador nativo fue exactamente 60, verificando que la medición final no incluyó otra carga sobre ese endpoint. Fuente de los contadores del proceso: CIM (Win32_Process); host: GetSystemTimes y GlobalMemoryStatusEx. Se contrastó el tiempo de CPU acumulado de Java en Micrometer con lecturas Windows antes y después, con tolerancia de redondeo de 0,05 s. La CPU Java por intervalo se normalizó por los procesadores lógicos. Las series de Prometheus y Grafana coincidieron al fijar el mismo instante de consulta.

RAM disponible: 2.678-2.987 GiB. Memoria privada de Java: 582.738-586.965 MiB. Se contrastó la RAM total visible mediante CIM y Win32. La memoria JVM heap/nonheap se consultó en el panel; no se equipara a working set, private bytes o RAM host. CPU host/proceso Windows y Micrometer usan ventanas diferentes, por lo que sus porcentajes se informan sin exigir igualdad entre intervalos distintos.

Preparación: el primer intento no inició carga por WinError 5 al leer el proceso del servicio. El intento elevado arrancó con retraso y coincidió con otro generador; esas observaciones se invalidaron y se conservan como resultado-observacion-no-controlada.json o preparación interrumpida. Se detuvo el generador restante antes de la repetición válida. Se incorporaron bloqueo de ejecución y contraste del contador HTTP. Ambas incidencias de preparación/ejecución se conservan en incidencias-ejecucion.json y se incluyen en la tabla acumulada, sin clasificarlas como fallo funcional del proyecto.

Alcance: CPU host/proceso y memoria Windows observadas durante baja carga. CPU Java contrastada por tiempo acumulado Windows/Micrometer. Porcentaje CPU Windows y Micrometer usa intervalos diferentes: se reportan ambos sin exigir igualdad. RAM host y working set/private bytes se registran en consola/CSV nativos, no son series del panel Grafana; el panel expone CPU y memoria JVM. Heap/nonheap, working set, private bytes y RAM host son magnitudes distintas. No acredita saturacion, umbrales de capacidad ni ausencia de fugas.

Evidencias: ../../../evidencias-monitoreo/M09-20261120-191921-1ec065/resultado.json; muestras-windows-prometheus-grafana.csv, contraste-cpu-acumulada.json, windows-cim-inicial.json, consultas.csv, métricas nativas y respuestas de Grafana. Sin cambios de configuración, servicios o datos de negocio. M10-M15 siguen pendientes.


## Prueba M10 Actividad de disco y archivos

M10-20261120-194650-f4d6ce: EXITOSO. 3/3 cargas HTTP 201 de 512 KiB; 3/3 archivos fisicos y descargas verificados. 9 muestras de C:, 34628096 bytes escritos en el volumen. Latencia media de escritura por intervalo: 0.096-0.476 ms. Incrementos de cargas Micrometer/Prometheus/Grafana: 3.0/3.0/3.

Se cargaron tres archivos TXT sintéticos de 512 KiB, separados por diez segundos, como documentos GENERAL sin relación con clientes, cotizaciones ni expedientes. Se verificaron el tamaño comunicado por la API, la descarga y el archivo físico mediante SHA-256, junto con un evento de almacenamiento, uno HTTP Java y uno Nginx por requestId. Se consultaron el contador HTTP de cargas y el espacio libre/total mediante Micrometer, Prometheus y la fuente de datos de Grafana.

Se observaron los contadores Windows de C: mediante Win32_PerfRawData_PerfDisk_LogicalDisk cada cinco segundos durante unos 40 segundos. Los bytes/s se calcularon por diferencia de contador y tiempo; la latencia media de cada intervalo se calculó como ((delta AvgDisksecPerWrite)/Frequency_PerfTime)/delta AvgDisksecPerWrite_Base, convirtiendo a milisegundos y contemplando wrap uint32. Un intervalo sin operaciones no se interpreta como una latencia medida de cero. Método: [Microsoft, PerformanceCounterType.AverageTimer32](https://learn.microsoft.com/en-us/dotnet/api/system.diagnostics.performancecountertype).

Alcance: Los contadores de volumen incluyen otros procesos: su volumen de escritura y latencia no se atribuyen exclusivamente a las cargas. La cache impide exigir igualdad entre bytes fisicos y archivos. Se confirma escritura de la aplicacion mediante hash y logs correlacionados. I/O y latencia Windows se registran en consola/CSV; Grafana consulta HTTP y espacio de disco, sin exporter I/O Windows. No se fuerza flush ni se evalua durabilidad tras corte electrico o capacidad maxima.

Cambios y limpieza: Se crearon y anularon logicamente tres documentos GENERAL de prueba sin asociarlos a entidades de negocio. Los archivos fisicos sinteticos (1.5 MiB) se conservan segun el comportamiento de la aplicacion; no se borra directamente informacion del almacenamiento. Login normal puede actualizar ultimoAcceso.

Evidencias: ../../../evidencias-monitoreo/M10-20261120-194650-f4d6ce/resultado.json; cargas.csv, verificacion-archivos.csv, actividad-latencia-disco.csv, contadores-disco-raw.json, prometheus-grafana.csv, respuestas de Grafana, logs correlacionados, limpieza-documentos.json, métricas antes/después y criterios-previos.txt. No se detectaron incidencias nuevas. M11-M15 siguen pendientes; no se ejecutó otro caso.


## Prueba M11 Disponibilidad de páginas y API

M11-20261120-200411-fa7c01: EXITOSO. Se comprobó el ciclo HTTP 200 -> 500/502 controlados -> 200 de las tres páginas y del catálogo de puertos. La copia aislada de Nginx usó 15173/15174/15180, los mismos dist del proyecto y el backend real para la fase saludable. El fallo cambió únicamente la raíz estática de esa copia a una inexistente y el upstream API al puerto 15999 sin listener. Se registró pending y luego firing para los cuatro objetivos; tras recuperar las rutas, los cuatro sondeos volvieron a 1 y no quedaron alertas pending/firing. Grafana confirmó cuatro alertas durante el fallo y cero al recuperar. Las rutas habituales permanecieron HTTP 200 y el backend siguió UP en todas las muestras.

Se integró Blackbox Exporter 0.29.1, descargado del repositorio oficial y verificado con SHA256 d997412c28d6a8949eb1880197c77f7b6dc887915ae0303c4e48f674a54f7081. Se agregó el job transaduanas-http, las reglas DisponibilidadHTTPFallida (for 15 s) y SondeoHTTPNoDisponible (for 30 s), y tres paneles Grafana de disponibilidad, estado HTTP y duración. No se declara probado el escenario propio de caída del exporter. Sondeo cada 5 s; la regla de 15 s es un umbral propuesto de laboratorio, no un SLA validado.

Los 500/502 se indujeron para verificar la detección: no son fallos inesperados del proyecto. El cierre se comprobó como desaparición de alertas activas/pending; no se configuraron ni probaron correo, Alertmanager u otras notificaciones externas. La primera preparación no arrancó por la ausencia de temp; su resumen automático era incorrecto y quedó invalidado. Esa incidencia del ejecutor está conservada como INC-M11-PREP-001, corregida y verificada.

Al finalizar se restauró la configuración de la copia, se retiraron sus objetivos temporales y se detuvo solo el Nginx de laboratorio. Sus tres puertos quedaron cerrados; los cuatro sondeos habituales siguen activos. No se modificó la configuración Nginx habitual, no se reiniciaron backend o MySQL y no se alteraron datos de negocio. Los logs y series históricas se conservan.

Evidencias: ../../../evidencias-monitoreo/M11-20261120-200411-fa7c01/resultado.json; linea-tiempo.csv, comprobaciones-http.csv, estados JSON por muestra, grafana-respuestas.json, verificacion-final.json, configuraciones normal/fallo y logs del Nginx aislado. Configuración y respaldos de la integración: ../../../evidencias-monitoreo/integracion-disponibilidad-M11/. M12-M15 siguen pendientes.


## Prueba M12 Disponibilidad de MySQL

M12-20261120-201716-b49501: EXITOSO. Se utilizó el mismo JAR activo 0.5.5 (SHA256 a46b84cd94257e9a2cc41382a2da91e3cd1b2f740b7c00a676c7d5c0c51b50e9), ejecutado como proceso independiente con heap máximo 256 MiB en 18086 y management 19086. MySQL 8.4.13 se inicializó en un datadir nuevo fuera del proyecto y escuchó solo en 127.0.0.1:13306, con buffer pool de 32 MiB. Se creó el esquema desde las entidades del proyecto y un único puerto sintético, sin exportar ni consultar la base original para formar el laboratorio. El inicializador de demostración permaneció desactivado.

Se detuvo únicamente MySQL del laboratorio mediante mysqladmin shutdown. El puerto 13306 quedó cerrado, la lectura real GET /api/v1/catalogos/puertos pasó de HTTP 200 a 500 y /api/v1/health siguió HTTP 200 con data.status=UP. Los sondeos diferenciaron negocio=0 y health=1. La regla DisponibilidadHTTPFallida pasó por pending y firing solo para el negocio, y Grafana mostró una alerta activa. Se reinició la misma instancia MySQL sobre sus datos sintéticos, sin reiniciar el backend de prueba: el catálogo volvió a HTTP 200, ambos sondeos regresaron a 1 y quedaron cero alertas pending/firing.

Las consultas quedaron correlacionadas por requestId con logs Java: ejecuciones JDBC MySQL exitosas en la fase normal y recuperación, y eventos HTTP 500 durante la interrupción. Health se refiere específicamente a la ruta propia /api/v1/health; no se afirma que /actuator/health permanezca UP. La alerta usada comprueba la función dependiente de MySQL; no se instaló mysql_exporter ni se validaron notificaciones externas. Los HTTP 500 fueron el estímulo previsto, no una incidencia inesperada del servicio habitual.

Los servicios MySQL84 y TransAduanasBackend conservaron PID y estado Running. Las tres páginas y la API habituales respondieron HTTP 200 en todas las muestras. El JAR activo no cambió. Al terminar, se retiraron los targets del laboratorio, se detuvieron sus procesos y los puertos 13306/18086/19086 quedaron cerrados. El archivo privado de conexión temporal se eliminó. Los datos sintéticos inactivos se conservan bajo TransAduanas-herramientas/monitoreo/laboratorios, fuera del proyecto; no se borraron datos originales.

Preparación: los dos primeros intentos se detuvieron al aplicar los permisos del archivo privado debido a la carga del módulo de seguridad de PowerShell 5.1. No iniciaron el backend ni indujeron la caída; sus instancias MySQL se cerraron y los archivos temporales se eliminaron. Se corrigió aplicando la ACL con .NET, comprobado con un archivo sin secretos. INC-M12-PREP-001 queda corregida y verificada, separada de fallos de aplicación.

Evidencias: ../../../evidencias-monitoreo/M12-20261120-201716-b49501/resultado.json; linea-tiempo.csv, consultas-http.json, grafana-respuestas.json, estados por muestra, servicios iniciales/finales, correlacion-sql-http.csv, eventos-correlacionados.log y logs MySQL/backend. M13-M15 siguen pendientes; no se ejecutó otro caso.


## Prueba M13 Alerta por errores HTTP

M13-20261120-202421-f685a0: EXITOSO. Regla original >1% con rate[5m] y for=5m. Pending observado=True; firing tras 300.729 s desde activeAt. Tasa al confirmar firing=50.0%; recuperacion=inactive, tasa=0.0%.

Se ejecutó el mismo JAR activo 0.5.5 en una copia del backend en 18087/19087, conectado a MySQL aislado 13307 con un catálogo sintético. Un Prometheus temporal en 19090 recogió las métricas Micrometer de esa copia y evaluó una copia exacta de la regla TasaErroresHTTPAlta. La regla habitual permaneció intacta y no se incorporaron métricas del laboratorio al Prometheus habitual. Grafana utilizó un datasource temporal específico.

Se conservaron rate[5m], el umbral >0,01 y for=5m. La carga fue una consulta health y una de catálogo por ciclo de cinco segundos, secuenciales. El estímulo consistió en cerrar únicamente MySQL de laboratorio: el catálogo generó HTTP 500 reales y health permaneció HTTP 200/UP. Se observaron pending y firing, comprobando los 300 segundos desde activeAt. La tasa se mantuvo por encima del 1 % en las observaciones de pending/firing. Al recuperar la misma instancia MySQL no se reinició backend ni Prometheus y se mantuvieron lecturas normales hasta que la ventana real de cinco minutos dejó de superar el umbral. Grafana confirmó tanto la activación como el cierre. El cierre se refiere al estado de la alerta, no a notificaciones externas.

La expresión original usa todas las series HTTP disponibles y no excluye expresamente rutas Actuator del denominador; la medición respeta ese alcance, sin presentarla como tasa exclusiva de negocio. Los errores 500 fueron el estímulo previsto y no se registran como incidencias naturales del servicio habitual. Las tres páginas y la API habituales permanecieron HTTP 200 y los servicios Windows conservaron sus PID.

Al terminar se retiró el datasource temporal de Grafana, se detuvieron solo los procesos creados y se eliminó el archivo privado de conexión. La base y TSDB sintéticas inactivas se conservan fuera del proyecto; los logs y resultados dentro de evidencias-monitoreo. No se detectaron incidencias imprevistas de la aplicación; se corrigió una incidencia del evaluador, detallada a continuación.

Evidencias: ../../../evidencias-monitoreo/M13-20261120-202421-f685a0/resultado.json; regla-original.yml, resumen-fases.json, linea-tiempo.csv, consultas-http.json, consultas.jsonl, estados JSON por muestra, grafana-respuestas.json, validación Prometheus, servicios iniciales/finales y logs del laboratorio. M14-M15 siguen pendientes; no se ejecutó otro caso.

Revision del evaluador M13: el informe inicial marco fallo al tratar una lectura de arranque sin serie up como indisponibilidad. Se conserva en resultado-evaluacion-inicial.json. Se reevaluaron las 46 muestras desde la linea base establecida a los 33,016 s, todas up=1; no se descarto ninguna muestra posterior, no se repitio la carga ni se modificaron la regla o los tiempos. Resultado final EXITOSO. INC-M13-EVAL-001 corregida y verificada. Ver revision-evaluador.json.

Conteo HTTP M13: 4 health y 4 catálogo HTTP 200 en la fase normal; 67 health HTTP 200 y 67 catálogo HTTP 500 durante el estímulo. Al iniciar la recuperación hubo una consulta de catálogo HTTP 500 mientras se restablecía la conexión, seguida de 63 HTTP 200; los 64 health fueron HTTP 200. Esta transición se conserva en conteos-http.json y consultas-http.json. El cierre final se comprobó con tasa 0 % y alerta inactiva.


## Preparación M14 previa a la autorización de carga real

El 21 de noviembre de 2026 se revisó CPUHostAlta: system_cpu_usage{job="transaduanas-backend"} > 0.85 con for=5m. No hay distribución WSL ni una VM disponible; Windows reporta virtualización de firmware deshabilitada y 12 procesadores lógicos. Se preparó un ejecutor con workers BELOW_NORMAL, afinidad por CPU, límite de 390 s y salida si muere su proceso padre; el worker tiene además tope interno de 400 s. La carga requiere la opción explícita --autorizar-carga-real-host. Sin esa opción el ejecutor comprueba métricas, memoria, disponibilidad y sale sin generar carga.

La validación promtool de la regla original pasó: 85 % exacto no dispara, 90 % no dispara antes de cinco minutos, dispara a los cinco minutos, se cierra al bajar y una caída intermedia reinicia la espera. Se usaron series sintéticas y tiempo simulado de promtool: esto es preparación/unidad de regla, NO una ejecución completa de M14 ni evidencia de saturación real. Los 12 workers se comprobaron secuencialmente con duración cero, sin entrar en su bucle de carga.

Las herramientas de monitoreo y Nginx estaban apagados; se iniciaron y las cuatro rutas habituales respondieron HTTP 200. No se generó carga CPU real. Se pidió al usuario elegir entre ejecutar carga real acotada en el host compartido o conservar solo la validación de la regla como parcial. No se registra M14 como exitosa o fallida hasta resolver ese alcance. M15 no se ejecutó.

Evidencias: carpetas preparacion-M14-*; promtool.txt, casos-promtool.json, regla-cpu.yml, preparacion.txt, servicios-iniciales.json, comprobacion-workers-sin-carga.json y entorno.json. Ejecutores preparados en ejecutar_prueba_14.py y worker_cpu_m14.py. Cancelación de la ejecución real mediante DETENER.txt en su carpeta de evidencias; el ejecutor detiene los workers automáticamente si observa dos muestras consecutivas de disponibilidad incorrecta.


## Prueba M14 Alerta por saturación de CPU

M14-20261121-093032-b9fd9c: EXITOSO. Firing despues de 304.83103 s desde activeAt; recuperacion=inactive.

El usuario autorizó expresamente completar la prueba mediante carga CPU real acotada en el host Windows compartido, al no disponer de una VM. La preparación unitaria promtool y los workers de duración cero no se confundieron con esta ejecución completa. Se usaron workers separados por procesador lógico, con prioridad BELOW_NORMAL, ciclos de carga/descanso y límites de tiempo; no se falsificaron las métricas. El ejecutor comprobó CPU real con GetSystemTimes y la serie system_cpu_usage de Micrometer, recogida por el Prometheus habitual y consultada desde Grafana.

La regla CPUHostAlta se mantuvo intacta: >0,85 y for=5m. Se observaron pending, firing después de al menos 300 segundos de activeAt y posterior cierre al retirar la carga. Las consultas de páginas y API se comprobaron durante la prueba. El resultado acredita el comportamiento de la alerta bajo este estímulo; no constituye una prueba de capacidad o SLA de negocio. No se validó entrega de notificaciones externas.

El laboratorio de carga fue el host real, no una VM. Los workers temporales quedaron detenidos, los servicios se verificaron al finalizar y las reglas no se modificaron. No hubo incidencias imprevistas.

Evidencias: ../../../evidencias-monitoreo/M14-20261121-093032-b9fd9c/resultado.json; linea-tiempo.csv, resumen-fases.json, workers.json, grafana-respuestas.json, estados de alerta por muestra y servicios iniciales/finales. Las comprobaciones de preparación se conservan en preparacion-M14-*. M15 sigue pendiente; no se ejecutó otro caso.


## Prueba M15 Interrupción de ingesta de logs

M15-20261121-100508-2e34da: EXITOSO. 32 consultas normales; 25 durante la pausa. Sin cambios del contador receptor durante 127.158 s. Alerta IngestaLogsInterrumpida firing y cierre=inactive. Eventos de pausa recuperados en Loki: 25/25; consulta Grafana con 34 lineas.

Se integró ingesta continua real: Alloy 1.22.0 lee los archivos Java y Nginx y los envía a Loki 3.7.10. Ambos binarios se descargaron de los repositorios oficiales y sus SHA256 se contrastaron con los publicados. Se guardan posiciones persistentes en TransAduanas-herramientas/monitoreo/datos/alloy, y Loki almacena localmente con retención configurada de 48 horas. No se afirma haber esperado 48 horas para validar esa retención. Se añadieron jobs de Prometheus, datasource Loki y tres paneles Grafana; ahora hay 18 paneles.

En M15 se enviaron 32 consultas normales de catálogo, todas HTTP 200. Se interrumpió únicamente Alloy: Loki no recibió nuevas líneas durante 127,158 segundos, mientras la aplicación siguió generando eventos. La alerta IngestaLogsInterrumpida usa increase del contador real del receptor sobre 2m, condicionada a backend y Loki UP, con for=5s adicional. Se observó pending y firing en Prometheus y Grafana. Al recuperar Alloy, el contador creció y la alerta quedó inactiva. Los 25/25 eventos generados durante la pausa se encontraron en Loki y se consultaron desde Grafana.

El criterio es entrega al menos una vez por requestId. Eventos con más de una ocurrencia en Loki en esta ejecución: 2. La relectura tras un cierre abrupto puede repetir eventos; no se declara entrega exactamente una vez ni se cuentan esas repeticiones como pérdida. Loki utiliza fecha de ingesta; la fecha original sigue dentro de cada línea. No se validaron notificaciones externas. Backend y MySQL conservaron PID y estado; los datos de negocio no se modificaron. Alloy quedó recuperado.

Se conservaron las incidencias previas: destino del registro de eventos Windows de Alloy, dirección interna de consultas Loki, lanzador PowerShell que no devolvía control y un intento que terminó por timeout. El timeout no permitió comprobar el backlog de aquel intento y no demuestra pérdida de eventos; su causa raíz no pudo determinarse con certeza. Se agregaron reintentos acotados, se corrigió el lanzador y la repetición completa fue exitosa. El informe fallido y la ejecución incompleta se conservan; la incidencia de timeout se clasifica como mitigada y verificada, no como causa raíz demostrada corregida.

Evidencias: ../../../evidencias-monitoreo/M15-20261121-100508-2e34da/resultado.json; linea-tiempo.csv, correlacion-eventos.csv, consultas-http.json, eventos-recuperados-loki.log, grafana-metricas.json, grafana-logs.json y servicios iniciales/finales. Configuración y diagnósticos de integración en integracion-logs-M15. Con este caso quedan ejecutadas las 15 pruebas; los resultados actuales y los fallos originales permanecen en el registro acumulado.
