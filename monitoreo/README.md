# Prometheus y Grafana para TransAduanas

## Arquitectura y rutas

TransAduanas 0.5.4 incorpora Spring Boot Actuator y Micrometer Prometheus. El backend publica métricas reales en `http://127.0.0.1:9086/actuator/prometheus`; el puerto de negocio sigue siendo 8086 y sus permisos permanecen activos. El puerto 9086 solo escucha en 127.0.0.1 y expone health/prometheus. No se exponen env, configuración ni secretos.

Prometheus consulta cada cinco segundos y conserva las series localmente, con retención de dos días y límite de 256 MB. Grafana usa esa fuente de datos y un dashboard provisionado desde JSON.

- Grafana: http://localhost:3000/d/transaduanas-monitoreo
- Prometheus: http://localhost:9090
- Métricas backend: http://127.0.0.1:9086/actuator/prometheus

Las herramientas portables y sus datos se conservan fuera del proyecto, en `Music/TransAduanas-herramientas/monitoreo`. El proyecto contiene configuración y scripts reproducibles; no se deben subir binarios, bases de datos, logs ni credenciales.

## Herramientas y fuentes oficiales

- Prometheus 3.13.5 LTS para Windows amd64: https://prometheus.io/download/
- Grafana OSS 13.3.1 para Windows amd64: https://grafana.com/grafana/download?edition=oss&platform=windows
- Actuator/Micrometer: https://docs.spring.io/spring-boot/4.1/reference/actuator/metrics.html
- Provisioning Grafana: https://grafana.com/docs/grafana/latest/administration/provisioning/

Los SHA256 de ambos paquetes se validan antes de extraerlos; versiones.json de la carpeta de herramientas conserva URLs y hashes. instalar_herramientas.py descarga únicamente esos paquetes oficiales fijados. No se requiere Docker ni una cuenta de Grafana Cloud.

## Inicio y parada

Después de instalar las herramientas y activar el JAR con el perfil staging, abre PowerShell en la raíz del proyecto:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\monitoreo\Iniciar-Monitoreo.ps1
```

Para detener solo las dos herramientas iniciadas por este módulo:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\monitoreo\Detener-Monitoreo.ps1
```

El script verifica los ejecutables asociados a los PID antes de detenerlos. No inicia ni detiene MySQL, Nginx o el servicio del backend. No elimina sus series históricas. Las herramientas se mantienen mientras sus procesos estén activos; después de reiniciar Windows se debe usar el script de inicio. No se instala un servicio o tarea de arranque sin solicitarlo.

## Paneles

El dashboard incluye recolección del backend, solicitudes acumuladas, solicitudes/s, proporción de HTTP 5xx, p50/p95/p99 del servidor, tasa por endpoint, memoria JVM, CPU del host/proceso, tiempo de GC, conexiones del pool MySQL, hilos Java y alertas activas.

Los percentiles del panel son aproximaciones del histograma de Micrometer en una ventana de cinco minutos. No tienen por qué coincidir exactamente con los nearest-rank de los CSV anteriores; tampoco son los tiempos de red observados por el cliente. El indicador up significa que se pudo leer el endpoint de métricas, no que todas las operaciones del negocio funcionen.

Las métricas de CPU y memoria JVM no sustituyen un exporter completo de Windows. Las conexiones Hikari no equivalen a un exporter completo de MySQL. No se declara implementada ingesta centralizada de logs ni trazas distribuidas al integrar estas dos herramientas.

## Acceso local y credenciales

Las interfaces escuchan solo en 127.0.0.1. El panel se puede consultar localmente como Viewer sin una cuenta administrativa. Esta función permite ver métricas agregadas, no editar dashboards ni usuarios. La contraseña administrativa aleatoria se guarda con permisos restringidos en `Music/TransAduanas-herramientas/monitoreo/credenciales-locales.json`, fuera del proyecto. No usar admin/admin ni copiar ese archivo al repositorio o ZIP.

## Reglas y pruebas

Prometheus carga cuatro reglas de laboratorio: falta de recolección, tasa de HTTP 5xx, latencia p95 y CPU del host. Los umbrales son propuestas para las futuras pruebas; instalar las reglas no demuestra que se hayan probado sus activaciones, recuperaciones o notificaciones. No se configuraron correo, Slack u otros receptores externos ni Alertmanager.

Las ejecuciones M01–M06 anteriores se mantienen con su alcance local. La validación de esta integración es una evidencia técnica separada y no se cuenta como la prueba M07 ni como una nueva aprobación de los 15 casos. Corresponde repetir M04/M06 con el panel para completar evidencia de la instrumentación nativa.

## Activación del backend instrumentado

El script de actualización conserva el JAR previo y recupera el anterior si falla su comprobación. En la raíz del proyecto, desde PowerShell **como administrador**:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\monitoreo\Actualizar-Backend-Metricas.ps1
```

La integración solo queda completa cuando el endpoint de métricas responde en 9086, el target transaduanas-backend está UP en Prometheus y las consultas del dashboard devuelven series reales. Instalar los binarios o abrir Grafana sin datos no basta. Consultar evidencias-monitoreo/integracion-prometheus-grafana-* para el estado observado.


## Integración comprobada el 20 de noviembre de 2026

La versión 0.5.4 quedó activa y la anterior 0.5.3 está conservada. Prometheus mostró el target UP. El contador del catálogo pasó de 2 a 12 tras diez lecturas y Prometheus recogió 12; Grafana consultó esa misma serie y devolvió 12. También se comprobaron valores numéricos de JVM y p95 mediante la fuente de datos de Grafana, cuya salud fue OK. El dashboard de doce paneles se revisó visualmente y mostró series reales. Las rutas de negocio y las tres interfaces respondieron; Clientes y las métricas por el puerto de negocio siguieron requiriendo autenticación, y /actuator/env permaneció bloqueado.

Se aprobaron 26 pruebas específicas del backend antes del despliegue. La verificación de integración quedó registrada en evidencias-monitoreo/integracion-prometheus-grafana-20261120-182612/validacion-integracion.txt. No se modificaron los resultados históricos de M01-M06 ni se adelantó M07. Las reglas están instaladas, pero sus escenarios de fallo y notificaciones siguen pendientes de pruebas propias.

El script de actualización es para cambiar desde 0.5.3 al parche 0.5.4. No lo vuelvas a ejecutar si estado.json ya registra 0.5.4; no es necesario para abrir los paneles o iniciar las dos herramientas después de reiniciar Windows.

## Estado actual: corrección JDBC 0.5.5

Backend activo 0.5.5; anterior conservado 0.5.4. Se añadió correlación de ejecuciones JDBC por requestId y jdbc_span_id, sin SQL, parámetros ni datos personales. M07 repetida: EXITOSO. Ver docs/pruebas/monitoreo/README.md y evidencias-monitoreo/M07-20261120-190700-0a89d3.

Actualizar-Backend-Metricas.ps1 es el procedimiento histórico de 0.5.3 a 0.5.4; no se ejecuta sobre la versión actual. Actualizar-Backend-Jdbc.ps1 aplica de 0.5.4 a 0.5.5 y ya fue ejecutado. Si se ejecuta con la misma versión y hash, evita el reinicio. Para abrir Grafana y Prometheus siguen vigentes Iniciar-Monitoreo.ps1 y las URLs anteriores. El reinicio del backend reinició sus contadores Micrometer; las series históricas de Prometheus y las evidencias anteriores se conservan.


## Disponibilidad HTTP integrada para M11

El stack incluye Blackbox Exporter 0.29.1, limitado a 127.0.0.1:9115. Iniciar-Monitoreo.ps1 inicia Blackbox antes de Prometheus; Detener-Monitoreo.ps1 utiliza el registro de procesos para detener las tres herramientas, sin tocar backend/Nginx. instalar_herramientas.py incluye el binario Blackbox y verifica su SHA256. La incorporación ya está aplicada; no hay que ejecutar Aplicar-Disponibilidad.ps1 para abrir los paneles.

El job transaduanas-http comprueba las tres páginas en 5173/5174/5180 y GET /api/v1/catalogos/puertos mediante Nginx. Se exige HTTP 200 y contenido HTML o success=true, según el módulo. La alerta DisponibilidadHTTPFallida observa probe_success=0 durante 15 s. up solo significa que Prometheus pudo recolectar al exporter: no sustituye probe_success. Las alertas son visibles en Prometheus y en las consultas de Grafana; no se configuró notificación externa. Grafana ahora tiene 15 paneles.

Configuración: blackbox/blackbox.yml, prometheus/prometheus.yml, prometheus/reglas.yml y grafana/dashboards/transaduanas.json. objetivos-pruebas.json normalmente contiene []; M11 lo usa temporalmente para targets etiquetados entorno=m11-aislado y restaura su contenido. Para repetir solo M11, con las herramientas activas, ejecutar python .\docs\pruebas\monitoreo\ejecutar_prueba_11.py desde la raíz. No necesita credenciales de negocio ni elevación Windows. No ejecutar a la vez otro laboratorio que use esos targets o los puertos 15173/15174/15180/15999.

La M11 comprobó detección y recuperación en una copia Nginx aislada; no interrumpió los puertos habituales. Evidencias y alcance en docs/pruebas/monitoreo/README.md. El sondeo permanente hace una consulta normal de lectura de catálogo cada cinco segundos: las futuras mediciones de contadores deben tener en cuenta ese tráfico o utilizar un endpoint diferente.

Fuentes oficiales: [Blackbox Exporter y hashes publicados](https://prometheus.io/download/), [sondeos multi-target](https://prometheus.io/docs/guides/multi-target-exporter/) y [configuración Blackbox 0.29.1](https://github.com/prometheus/blackbox_exporter/blob/v0.29.1/CONFIGURATION.md).


## Repetición aislada de M12

Con el stack de monitoreo activo y al menos 1,5 GiB de RAM disponible, ejecutar python .\docs\pruebas\monitoreo\ejecutar_prueba_12.py desde la raíz. Requiere los binarios MySQL Server 8.4 en Program Files, Java 21 y el JAR actual 0.5.5; utiliza puertos libres 13306/18086/19086. No necesita las credenciales de negocio originales ni permisos para detener servicios Windows. El script crea credenciales aleatorias exclusivamente para la instancia nueva, las restringe con una ACL de .NET y elimina el archivo temporal al terminar. El primer bootstrap de root se protege con contraseña antes de iniciar el backend de laboratorio.

La prueba detiene y recupera solo MySQL del laboratorio. El mismo backend aislado debe recuperar sus consultas sin reinicio; la ruta de liveness /api/v1/health puede indicar UP aunque MySQL no esté disponible. Los objetivos temporales se publican en objetivos-pruebas.json con entorno=m12-aislado y se retiran en finally. Los procesos temporales se detienen; los datos sintéticos inactivos quedan fuera del proyecto y las evidencias dentro de evidencias-monitoreo. No ejecutar en paralelo otro caso que use el archivo de targets. El resultado observado y sus límites están en docs/pruebas/monitoreo/README.md.

Referencia de inicialización: [MySQL 8.4, Initializing the Data Directory](https://dev.mysql.com/doc/refman/8.4/en/data-directory-initialization.html).


## Repetición aislada de M13

Con el stack activo y al menos 1,5 GiB disponibles, ejecutar python .\docs\pruebas\monitoreo\ejecutar_prueba_13.py desde la raíz. Requiere MySQL 8.4, Java 21 y el JAR actual 0.5.5, además del Prometheus/Grafana ya instalados. Utiliza 13307/18087/19087/19090, todos locales y libres. Se debe reservar aproximadamente once minutos: for=5m se verifica en tiempo real y la recuperación puede necesitar otros cinco minutos por rate[5m]. No se aceleran relojes ni se reducen esos tiempos.

El script conserva la regla habitual, crea una copia exacta para un Prometheus aislado y un datasource Grafana temporal; no mezcla esas series con las de la aplicación habitual. La instancia MySQL sintética se detiene y recupera sin reiniciar el backend de prueba. Las credenciales de laboratorio son aleatorias y el archivo temporal se elimina al finalizar. Los procesos y datasource temporales se retiran al terminar, y las evidencias se conservan. No ejecutar en paralelo otro caso de monitoreo.

La expresión de tasa actual usa todas las series HTTP disponibles, sin excluir expresamente rutas Actuator del denominador. M13 valida esa configuración existente. El resultado y sus límites se encuentran en docs/pruebas/monitoreo/README.md.


## M14 con CPU real del host

La M14 completa se ejecutó después de autorización expresa del usuario para cargar el Windows compartido. No fue una ejecución en VM. El ejecutor está en docs/pruebas/monitoreo/ejecutar_prueba_14.py; sin opciones solo comprueba requisitos y sale sin carga. La opción --autorizar-carga-real-host habilita los workers de CPU reales, de baja prioridad y afinidad por CPU lógica. No ejecutar esa opción sin reservar el uso de CPU del equipo durante unos seis minutos. Ver la evidencia y el alcance real en el README de pruebas.

Cada worker tiene límite temporal y sale si termina su proceso padre. El ejecutor permite cancelar creando DETENER.txt en su carpeta de evidencias, y detiene los workers ante dos muestras consecutivas de disponibilidad incorrecta. Al finalizar retira la carga y comprueba la recuperación de CPUHostAlta. Las métricas y la alerta son las habituales del proyecto, sin alterar el umbral 85 % ni for=5m. Las pruebas promtool de la preparación son complementarias y usan tiempo simulado; no sustituyen la prueba real.


## Ingesta de logs y cierre M15

Loki 3.7.10 recibe los logs en 127.0.0.1:3100 (gRPC interno en 127.0.0.1:9096). Alloy 1.22.0 lee sga-backend.log y access.log y expone su estado en 127.0.0.1:12345. Ambos binarios se obtuvieron de sus repositorios oficiales y se verificaron sus hashes. Binarios, posiciones y almacenamiento se conservan fuera del proyecto, bajo TransAduanas-herramientas/monitoreo. Las configuraciones están en loki/loki.yml y alloy/config.alloy.

Iniciar-Monitoreo.ps1 ahora inicia también Iniciar-Logs.ps1; Detener-Monitoreo.ps1 llama a Detener-Logs.ps1. Los procesos de Loki/Alloy se identifican en procesos-logs.json y se verifica ejecutable/PID antes de detenerlos. El destino de los mensajes internos de Alloy se fija a stderr y no necesita registrar un origen Windows Event Log con administrador. Loki fija su dirección interna a 127.0.0.1 para que las consultas funcionen con el listener local. Tras la prueba se volvió a iniciar Alloy con el script habitual para que sus mensajes futuros vuelvan a datos/logs, dejando intactas las evidencias del caso.

El datasource transaduanas-loki y el panel de logs permiten buscar en Grafana. Hay 18 paneles. Al primer inicio tail_from_end evita importar todo el histórico local; los reinicios posteriores reutilizan posiciones. La entrega se verifica al menos una vez y puede haber repeticiones al recuperar un cierre abrupto: M15 observó dos eventos repetidos y recuperó 25/25 eventos de pausa. No se declara entrega exactamente una vez. El timestamp de Loki es de ingesta; el timestamp original permanece en el texto del evento.

IngestaLogsInterrumpida observa dos minutos sin incremento de loki_distributor_lines_received_total, con backend y Loki disponibles, y añade for=5s. Se apoya en los sondeos periódicos normales y en los eventos periódicos de M15. La alerta indica ausencia de ingesta; no determina por sí sola la causa raíz ni debe interpretarse como fallo cuando una aplicación está legítimamente silenciosa. RecolectorLogsNoDisponible y ReceptorLogsNoDisponible comprueban además up de Alloy/Loki durante 30 s; la M15 no se limita a esas alertas de proceso.

Para repetir M15, con el stack y las interfaces activos, ejecutar python .\docs\pruebas\monitoreo\ejecutar_prueba_15.py desde la raíz. Solo detiene el Alloy propio, mantiene eventos de lectura reales y lo recupera con el mismo storage. El ejecutor inicia Alloy directamente con salidas a archivo para evitar el bloqueo del lanzador PowerShell con captura de salida. No necesita credenciales del negocio ni elevación de Windows. No ejecutar otro caso a la vez.

Última M15: EXITOSO, 32 consultas HTTP 200, 127,158 s sin incremento del receptor, alerta firing y posterior cierre, 25/25 eventos de pausa recuperados y consultados desde Grafana. El intento previo con timeout y la ejecución incompleta se conservan junto con sus diagnósticos. No se ha validado retención transcurrida de 48 horas ni notificación externa.

Fuentes: https://github.com/grafana/loki/releases/tag/v3.7.10 ; https://github.com/grafana/alloy/releases/tag/v1.22.0 ; https://grafana.com/docs/alloy/latest/reference/components/loki/loki.source.file/ .
