# Pruebas de seguridad de TransAduanas

## Entorno y datos

- Laboratorio local en 127.0.0.1 con puerto aleatorio, JDK 21, Node.js 24, Spring Boot y H2 temporal. El ejecutor usa los binarios de la copia incluida en el ZIP y genera contraseñas al azar para las cuentas ficticias. La base H2 se elimina al cerrar Java.
- María Salas de la Torre, agente comercial, y Lucía Mendoza Vargas, administradora. Ana Torres Velarde representa a Transportes Costa del Sur S.A.C. y Diego Paredes Ríos a Comercial Bahía Clara S.A.C. Todos son datos inventados.
- El caso de contraseña predeterminada usa la cuenta admin que crea el proyecto. El caso de firma JWT arranca con el valor de respaldo del proyecto y genera el token solo dentro del laboratorio. Usa este ejecutor exclusivamente sobre una copia local con datos desechables.
- La evidencia no incluye contraseñas generadas ni tokens. No se modificó MySQL ni se ejecutó Postman.

## Cómo repetirlo en PowerShell

1. Extrae TransAduanas en una carpeta de laboratorio. Los scripts están en docs/pruebas/seguridad. Descarga ffuf para Windows y, si quieres reproducir el análisis pasivo, la versión multiplataforma de ZAP. Extrae ambos.
2. Abre PowerShell en la raíz de TransAduanas. Cambia las tres rutas de ejemplo de acuerdo con tu equipo.

```powershell
$kit = (Resolve-Path .\docs\pruebas\seguridad).Path
$ffuf = 'C:\Herramientas\ffuf\ffuf.exe'
$zap = 'C:\Herramientas\ZAP_2.17.0\zap-2.17.0.jar'
$backend = (Resolve-Path .\backend).Path
$resultado = Join-Path (Get-Location).Path 'evidencia-seguridad'
New-Item -ItemType Directory -Force -Path $resultado | Out-Null
cd $backend
.\mvnw.cmd -B compile dependency:build-classpath "-DincludeScope=test" "-Dmdep.outputFile=target/dependencias-seguridad.txt"
if ($LASTEXITCODE -ne 0) { throw 'Maven no completó la preparación' }
$deps = (Get-Content .\target\dependencias-seguridad.txt -Raw).Trim()
$cp = Join-Path $resultado 'classpath-local.txt'
"$backend\target\classes;$deps" | Set-Content -Encoding utf8 $cp
node "$kit\seguridad-http.mjs" $backend $cp "$resultado\http.json" $ffuf $zap 2>&1 |
  Tee-Object -FilePath "$resultado\ejecucion-http.txt"
```

- Si tienes ffuf pero no ZAP, omite el último argumento. Para probar solo la API, omite ambos argumentos.
- Si las clases de la copia ya están compiladas, puedes ejecutar solo el objetivo Maven dependency:build-classpath. En esta revisión el entorno de Windows impidió recompilar Java por permisos, así que las pruebas de HTTP se ejecutaron sobre los binarios de la copia. Se verificó por separado que Maven sí genera el classpath.
- El ejecutor imprime el estado y el código observado. La matriz adjunta incluye el criterio esperado. Un Falló en SEC04, SEC05 o SEC06 es un hallazgo de seguridad, aunque Node pueda terminar con código de salida 0. Revisa el JSON y la tabla de abajo.
- Para capturas propias, muestra la terminal al terminar, luego abre http.json, ffuf-rutas.json o zap-pasivo.json. El puerto y los tiempos varían. No compartas classpath-local.txt.

### Dependencias del backend (Maven y OSV)

Desde backend, en la misma sesión PowerShell:

```powershell
.\mvnw.cmd -B dependency:tree "-Dscope=runtime" "-DoutputType=json" "-DoutputFile=$resultado\backend-runtime.json"
node "$kit\seguridad-osv.mjs" "$resultado\backend-runtime.json" "$resultado\osv-backend.json"
```

OSV recibe coordenadas y versiones de paquetes Maven. El resultado refleja lo que devolvió la base de avisos el día de la consulta.

### Dependencias web (npm)

Desde la raíz del proyecto, después de volver con cd ..:

```powershell
foreach ($app in 'app-sga','app-portal-cliente','landing') {
  Push-Location $app
  npm.cmd audit --omit=dev --json > "$resultado\npm-audit-$app.json"
  "$app código=$LASTEXITCODE"
  Pop-Location
}
```

### Configuración

Con Python 3, desde la raíz:

```powershell
python "$kit\check-config.py" . > "$resultado\configuracion.json"
```

## Casos de la guía adaptados al proyecto

| ID | Qué se prueba | Esperado | Observado |
| --- | --- | --- | --- |
| SEC01 | Listar usuarios sin token | 401 | 401 |
| SEC02 | Listar usuarios con JWT inválido | 401 | 401 |
| SEC03 | María intenta listar usuarios | 403, con un 200 de Lucía como control positivo | 403; Lucía recibió 200 |
| SEC04 | Token emitido fuera del login con el secreto de respaldo, usando el nombre de usuario de Lucía | 401 | 200 |
| SEC05 | Inicio de sesión con la contraseña demo de admin | 401 | 200 |
| SEC06 | María envía JSON truncado, array, objeto donde se espera texto y un campo @type acompañado de un tipo incompatible | 400 por solicitud y sin nuevas filas visibles | 500 en los cuatro casos; el listado quedó igual |
| SEC07 | Variante SQLi sobre el nombre de usuario de María | 401 | 401 |
| SEC08 | Preflight CORS con origen no autorizado | 403 sin permitir el origen | 403 sin la cabecera |
| SEC09 | 20 solicitudes secuenciales con token inválido | 20 respuestas 401 | 20 respuestas 401 |
| SEC10 | Ana descarga su comprobante y Diego intenta descargar exactamente el mismo documento desde otra empresa | 200 para Ana y rechazo para Diego | 200 PDF para Ana y 403 para Diego |

En SEC06 se verificó deserialización de tipos JSON y manejo de errores. La prueba no demuestra ejecución de código. SEC07 cubre una variante de SQLi en el login. SEC09 es una comprobación pequeña de robustez; no mide capacidad máxima ni resistencia a DoS.

## Qué aporta cada enlace del PDF

- Pág. 27, Zenmap es la interfaz de Nmap para inventariar puertos. https://nmap.org/zenmap/
- Pág. 29, OWASP Top 10 es una referencia de categorías de riesgo. https://owasp.org/Top10/
- Pág. 31, Burp Suite permite interceptar y repetir solicitudes en pruebas manuales. https://portswigger.net/burp
- Pág. 33, ffuf sirve para reemplazar FUZZ por cada valor de una lista. Se ejecutó sobre seis rutas locales con un hilo y límite de 4 solicitudes por segundo. https://www.kali.org/tools/ffuf/ y https://github.com/ffuf/ffuf/releases
- Pág. 35, JMeter sirve para diseñar pruebas de carga con concurrencia y métricas. https://jmeter.apache.org/
- Pág. 36, Arachni es un escáner web. El enlace del PDF apunta a una página del proveedor. https://ecsypno.com/pages/arachni-web-application-security-scanner-framework
- Pág. 38, snyk test consulta vulnerabilidades en dependencias. Se usaron npm audit y OSV para esta fase. https://docs.snyk.io/developer-tools/snyk-cli/commands/test
- Pág. 46, las direcciones app:8080 y target son ejemplos. El comando ZAP de la guía corresponde a un escaneo pasivo con descubrimiento. En Windows se usó un plan del Automation Framework con tres solicitudes directas locales y análisis pasivo. https://www.zaproxy.org/docs/automate/automation-framework/
- Pág. 46, sqlmap automatiza pruebas de inyección SQL. La variante de login de este laboratorio se hizo por HTTP con Node.js. https://sqlmap.org/
- Consultas de avisos de backend. https://google.github.io/osv.dev/post-v1-querybatch/
- Auditoría de dependencias npm. https://docs.npmjs.com/cli/v11/commands/npm-audit/
- Aviso de follow-redirects. https://github.com/advisories/GHSA-mrwp-f3q8-3g9p

## Qué puedes mostrar en tu trabajo

1. Objetivo y alcance. Verificar acceso por rol, aislamiento entre clientes, autenticación y manejo de entradas en el backend local, más revisión de configuración y dependencias.
2. Evidencia de autorización. Captura SEC03 junto con SEC10. Los controles positivos muestran que las rutas y el documento eran accesibles para sus propietarios o roles autorizados.
3. Hallazgos. Eliminar el secreto JWT de respaldo y exigir uno externo. Limitar las cuentas demo a un perfil local. Responder 400 a errores de lectura de JSON, sin detalles internos.
4. Vulnerabilidades de dependencias. npm audit reportó una alerta alta de follow-redirects en SGA y portal y cero en landing. OSV respondió sin avisos para las 124 coordenadas Maven de ejecución consultadas. No se comprobó explotabilidad de la alerta npm en navegador.
5. Cobertura. Las pruebas fueron de laboratorio. Quedaron fuera un escaneo web activo, pruebas XSS en navegador, carga sostenida y CI. Postman queda para una etapa posterior.

## Inventario de evidencia de esta ejecución

- http.json y ejecucion-http.txt. Diez casos HTTP, siete conformes y tres hallazgos. SEC09 registró p95 de 3.1 ms en la ráfaga local.
- matriz-seguridad.csv. Resultados para consultar al preparar el documento.
- ffuf-rutas.json y rutas-ffuf.txt. Seis solicitudes de reconocimiento. Salud devolvió 200, las rutas privadas rechazaron y la ruta inventada recibió 401, así que un 401 por sí solo no confirma que exista una ruta.
- zap-pasivo.json, zap-plan.yaml y ejecucion-zap.txt. ZAP 2.17.0 completó el plan en tres endpoints anónimos y no reportó alertas de sitio. El log registró que faltaban Firefox/geckodriver para integración con navegador. El plan de solicitudes directas y escaneo pasivo terminó correctamente. No es una evaluación de la interfaz ni un escaneo activo.
- backend-runtime.json, osv-backend.json y ejecucion-osv.txt. Se consultaron 124 coordenadas Maven de ejecución, con cero avisos devueltos.
- npm-audit-*.json. Una alerta alta en SGA, una en portal y cero en landing. Es el mismo aviso, no dos vulnerabilidades distintas.
- configuracion.json y check-config.py. Cinco patrones de configuración detectados, con ruta y número de línea.
- seguridad-http.mjs y seguridad-osv.mjs. Ejecutores.
- Zenmap/Nmap, Burp, JMeter, Arachni, Snyk y sqlmap quedaron como referencias revisadas. No hay resultados atribuidos a esas herramientas.

Proyecto revisado: rama incluida pruebas/controladores-multinivel, referencia 5b8b29d022d4f335df96129b0814f9704e5b06d7.
ZIP original SHA-256: 491ED668E020385378E24163F0FB654B9223B8AEF6D71C021EDF8F4DBD84F01C.
