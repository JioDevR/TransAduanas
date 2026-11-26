# Pruebas de monitoreo de TransAduanas

Esta entrega reúne las pruebas M01 a M15, sus manuales, las tablas de resultados e incidencias y la configuración de Prometheus, Grafana, Loki, Alloy y Blackbox Exporter.

## Archivos principales

- `Manual_pruebas_monitoreo_TransAduanas.docx` y su PDF: instrucciones, herramientas y evidencias que mostrar en cada prueba.
- `Ejecutar-CasoMonitoreo.ps1`: ejecuta una prueba por vez.
- `Mostrar-EvidenciaMonitoreo.ps1`: consulta la última ejecución con resultado guardado. Comprobar que corresponda a las capturas del informe.
- `Resultados_e_incidencias_monitoreo_TransAduanas.docx` y su PDF: resultados e historial de incidencias del cierre documentado.
- `Tarea Semana 16 corregida.docx`: informe con las figuras y tablas.
- `monitoreo/`, desde la raíz del repositorio: instalación, arranque y configuración de las herramientas.

## Preparación y ejecución

El manual corresponde al laboratorio Windows ubicado en `C:\Users\asus\Music\TransAduanas`. Requiere Java 21, Python, MySQL, Nginx, las interfaces compiladas y las herramientas descritas en el manual. Las rutas locales de Nginx y de las herramientas deben existir en el equipo de ejecución.

La instrumentación del backend incluye registros HTTP con identificador, seguimiento JDBC sin datos SQL y métricas de Micrometer. El perfil `monitoreo` configura las métricas en `127.0.0.1:9086`; las credenciales de la aplicación y la base de datos deben configurarse localmente. El perfil se activa mediante `SPRING_PROFILES_ACTIVE=monitoreo`, conservando los demás perfiles que use el despliegue.

Desde la raíz del proyecto, con el entorno preparado según el manual:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File `
  ".\docs\pruebas\monitoreo\Ejecutar-CasoMonitoreo.ps1" `
  -Codigo M01
```

Cambiar M01 por la prueba elegida. M14 con carga real necesita el parámetro adicional que indica el manual. No ejecutar varias pruebas simultáneamente.

## Evidencias y alcance de esta entrega

Los documentos conservan los resultados obtenidos en el laboratorio. Las carpetas completas de cada intento permanecen en `evidencias-monitoreo` en el equipo original y se excluyen de Git para preservar el historial local, archivos operativos y posibles datos de sesión. La entrega no contiene contraseñas administrativas de Grafana ni credenciales locales.

Los scripts `Actualizar-Backend-Metricas.ps1` y `Actualizar-Backend-Jdbc.ps1` documentan las migraciones históricas entre versiones del laboratorio; requieren su estado y respaldo originales. Para una instalación nueva, compilar la versión de esta rama y preparar el entorno siguiendo el manual, sin reproducir esas migraciones como si fueran una instalación inicial.

Esta rama incorpora la instrumentación de monitoreo sobre el código actual de `main`. Los resultados históricos del informe no equivalen a una nueva ejecución de M01 a M15 sobre esta rama.

## Validación de la rama

El 26 de noviembre de 2026 se compiló el backend con Java 21 y se ejecutó:

```powershell
.\mvnw.cmd -o "-Dtest=HttpRequestLoggingFilterTest,JdbcRequestTracingTest,MonitoringEndpointsTest" test
```

Resultado: 9 tests ejecutados, 0 fallos, 0 errores y 0 omitidos. Se verificaron registros HTTP, contexto y privacidad JDBC, publicación de métricas, autenticación de la aplicación y restricciones de los endpoints de administración. La prueba de integración usó H2 y puertos temporales, sin modificar la base MySQL del laboratorio.

También se comprobó la sintaxis de los scripts Python y PowerShell, los archivos JSON, la integridad de los documentos Word y la ausencia de credenciales locales entre los archivos preparados para Git.
