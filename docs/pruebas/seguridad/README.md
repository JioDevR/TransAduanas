# Pruebas de seguridad de TransAduanas

Para ejecutar las pruebas en Windows y preparar las evidencias del informe, sigue [CAPTURAS.md](CAPTURAS.md). Ahí están los comandos, la ubicación de los resultados y los pasos de Postman Desktop.

Para las pruebas HTTP del backend, el script arranca Spring Boot en un puerto libre de `127.0.0.1`, usa H2 en memoria y lo detiene al terminar. Los requisitos de MySQL, frontend y Postman están en sus apartados de la guía de capturas.

Los usuarios y empresas son ficticios. María Salas de la Torre tiene el rol de agente comercial y Lucía Mendoza Vargas el de administradora. Ana Torres Velarde y Diego Paredes Ríos pertenecen a empresas distintas. La cuenta `admin` del proyecto se usa para comprobar la contraseña predeterminada y preparar los datos.

## Resultados por componente

| Componente | Pruebas ejecutadas | Resultado |
| --- | --- | --- |
| Backend Spring Boot | 10 casos HTTP con H2. | 7 pasaron. Se reprodujeron los hallazgos de SEC04, SEC05 y SEC06. |
| Frontend SGA | 8 casos en navegador con Playwright y el backend local. | 8 pasaron. Se verificaron sesión, navegación, contraseña oculta y redirección externa. |
| Base de datos | 8 casos con SQL directo en MySQL 8.4.13, en una instancia separada y con el esquema del proyecto. | 8 pasaron. El usuario de lectura no pudo insertar, borrar ni leer una columna restringida. El conteo fue 1 antes y 1 después. |
| Sistema | 8 casos con Spring Boot, JWT y datos en H2. | 8 pasaron. Se comprobó el flujo de baja y reactivación de una cuenta del portal y la separación entre empresas. |
| Postman | 16 solicitudes en cuatro carpetas, ejecutadas con Newman y Postman Desktop. | 32 aserciones aprobadas en cada ejecución de referencia. |

La prueba general ejecutó 34 casos sin Postman. Pasaron 31 y se reprodujeron tres hallazgos. Consulta [los archivos que controlan la seguridad](ARCHIVOS-SEGURIDAD.md) y [los comandos para tomar capturas](CAPTURAS.md). MySQL se probó aparte. Las pruebas HTTP, de navegador y de sistema usan H2. Los reportes de ffuf, ZAP pasivo y dependencias siguen disponibles.

## 1. Abre el proyecto y verifica las versiones

En VS Code, abre la carpeta principal de TransAduanas, la que contiene `backend` y `app-sga`. Abre **Terminal → Nueva terminal**, elige **PowerShell** y ejecuta:

```powershell
Set-Location -LiteralPath "$env:USERPROFILE\Music\TransAduanas" -ErrorAction Stop
git branch --show-current
java -version
node -v
npm.cmd -v
```

Usa **JDK 21** y **Node.js 24**. No necesitas cambiar a la rama histórica de seguridad para ejecutar los scripts incluidos en este proyecto. Si lo guardaste en otra carpeta, ajusta la ruta de este bloque y `$raiz` en los siguientes. Si acabas de instalar Java o Node, cierra VS Code y ábrelo otra vez para actualizar el PATH.

## 2. Compila el backend y prepara el classpath

Pega este bloque en PowerShell. Usa el proyecto existente en `$env:USERPROFILE\Music\TransAduanas`; ajusta `$raiz` si lo guardaste en otra carpeta. Maven necesita Internet la primera vez. Deja abierta la misma terminal para los pasos siguientes, porque usan las variables de este bloque.

```powershell
. {
    $raiz = "$env:USERPROFILE\Music\TransAduanas"
    $backend = Join-Path $raiz 'backend'
    $kit = Join-Path $raiz 'docs\pruebas\seguridad'
    $script = Join-Path $kit 'seguridad-http.mjs'
    if (-not (Test-Path -LiteralPath $script -PathType Leaf)) {
        throw "No encontré $script. Corrige la ruta de la primera asignación."
    }

    $resultado = Join-Path $backend 'target\seguridad-local'
    New-Item -ItemType Directory -Path $resultado -Force -ErrorAction Stop | Out-Null
    $archivoDeps = Join-Path $resultado 'dependencias.txt'
    $cp = Join-Path $resultado 'classpath-local.txt'

    Set-Location -LiteralPath $backend -ErrorAction Stop
    .\mvnw.cmd -B compile dependency:build-classpath "-DincludeScope=test" "-Dmdep.outputFile=$archivoDeps"
    if ($LASTEXITCODE -ne 0) { throw 'Maven falló. Revisa el primer ERROR de la salida.' }
    $deps = (Get-Content -LiteralPath $archivoDeps -Raw -ErrorAction Stop).Trim()
    "$backend\target\classes;$deps" | Set-Content -LiteralPath $cp -Encoding utf8 -ErrorAction Stop
}
```

Verifica que la compilación termine con **BUILD SUCCESS**. El alcance `test` del classpath incluye H2. Si Maven está usando otro JDK, verifica `$env:JAVA_HOME` y vuelve a ejecutar `.\mvnw.cmd -v` antes de compilar.

## 3. Ejecuta los 10 casos HTTP

Sigue en la **misma terminal**, dentro de `backend`:

```powershell
node "$kit\seguridad-http.mjs" $backend $cp "$resultado\http.json" 2>&1 |
  Tee-Object -FilePath "$resultado\ejecucion-http.txt"
if ($LASTEXITCODE -ne 0) { throw 'El script se interrumpió. Revisa ejecucion-http.txt.' }
Get-Content "$resultado\http.json" -Raw | ConvertFrom-Json |
  Sort-Object id | Format-Table id, resultado, observado -Wrap
```

Abre `backend\target\seguridad-local\ejecucion-http.txt` para la captura. En la versión evaluada aparecieron diez casos. `SEC04`, `SEC05` y `SEC06` salieron como `Falló` porque reprodujeron problemas del proyecto. Un error de arranque o de preparación del laboratorio aparece como un error del script y puede dejar la lista incompleta.

## 4. Ejecuta ffuf y ZAP si los vas a mostrar

Descarga el ZIP de **ffuf para Windows** y la edición **Crossplatform de ZAP**. Extrae ambos. Para ZAP conserva toda la carpeta extraída, incluidos sus complementos. [Descargas de ffuf](https://github.com/ffuf/ffuf/releases) · [Descargas de ZAP](https://www.zaproxy.org/download/)

En la **misma terminal**, pega el bloque. Cuando te pregunte, escribe la ruta completa del archivo, sin comillas. Para usar solo ffuf, deja en blanco la ruta de ZAP.

```powershell
$ffuf = Read-Host 'Ruta completa de ffuf.exe'
if (-not (Test-Path -LiteralPath $ffuf -PathType Leaf)) { throw 'No se encontró ffuf.exe.' }
$zap = Read-Host 'Ruta completa de zap-2.17.0.jar (Enter para omitir ZAP)'
$opciones = @((Resolve-Path -LiteralPath $ffuf).Path)
if ($zap) {
  if (-not (Test-Path -LiteralPath $zap -PathType Leaf)) { throw 'No se encontró el JAR de ZAP.' }
  $opciones += (Resolve-Path -LiteralPath $zap).Path
}

node "$kit\seguridad-http.mjs" $backend $cp "$resultado\http-herramientas.json" @opciones 2>&1 |
  Tee-Object -FilePath "$resultado\ejecucion-herramientas.txt"
if ($LASTEXITCODE -ne 0) { throw 'Revisa ejecucion-herramientas.txt y, si usaste ZAP, ejecucion-zap.txt.' }
```

Este bloque vuelve a arrancar el laboratorio y genera `ffuf-rutas.json`. Si incluiste ZAP, también genera `zap-pasivo.json` y `ejecucion-zap.txt`. Busca `Automation plan succeeded!` en el log. La ejecución registrada cubrió seis rutas con ffuf y tres peticiones anónimas con ZAP. Una ruta inventada también devolvió `401`, así que ese código por sí solo no confirma que la ruta exista.

## 5. Revisa las dependencias y la configuración

**Backend.** En la misma terminal, desde `backend`:

```powershell
Set-Location $backend
.\mvnw.cmd -B dependency:tree "-Dscope=runtime" "-DoutputType=json" "-DoutputFile=$resultado\backend-runtime.json"
if ($LASTEXITCODE -ne 0) { throw 'Maven no generó el árbol de dependencias.' }
node "$kit\seguridad-osv.mjs" "$resultado\backend-runtime.json" "$resultado\osv-backend.json"
if ($LASTEXITCODE -ne 0) { throw 'La consulta a OSV falló. Revisa el mensaje de Node.' }
```

**Frontends.** Este bloque vuelve a la carpeta principal y entra a cada aplicación. Usa los archivos `package-lock.json` del repositorio.

```powershell
Set-Location $raiz
foreach ($app in 'app-sga', 'app-portal-cliente', 'landing') {
  Push-Location (Join-Path $raiz $app)
  try {
    npm.cmd audit --omit=dev --json > "$resultado\npm-audit-$app.json"
    $codigo = $LASTEXITCODE
    $informe = Get-Content "$resultado\npm-audit-$app.json" -Raw | ConvertFrom-Json
    if ($informe.error -or -not $informe.metadata) { throw "npm audit no completó la revisión de $app." }
    "$app | código $codigo | alertas $($informe.metadata.vulnerabilities.total)"
  } finally {
    Pop-Location
  }
}
```

Un código `1` acompañado de un informe válido indica que npm encontró alertas. En la ejecución guardada, SGA y el portal registraron el mismo aviso alto de `follow-redirects`. Landing registró cero. OSV no devolvió avisos para las 124 dependencias Maven consultadas. [Aviso de follow-redirects](https://github.com/advisories/GHSA-mrwp-f3q8-3g9p)

**Configuración.** Con Python 3 instalado, copia el bloque completo en PowerShell. Cambia la ruta de la primera asignación si guardaste el clon en otra carpeta. Puedes ejecutarlo desde una terminal nueva.

```powershell
& {
    $raiz = "$env:USERPROFILE\Music\TransAduanas"
    $script = Join-Path $raiz 'docs\pruebas\seguridad\check-config.py'
    $resultado = Join-Path $raiz 'backend\target\seguridad-local'

    if (-not (Test-Path -LiteralPath $script -PathType Leaf)) {
        throw "No encontré $script. Corrige la ruta del proyecto en la primera asignación."
    }
    # Seleccionar una sola instalación y omitir los alias de Microsoft Store.
    $python = Get-Command python -CommandType Application -All -ErrorAction Stop |
        Where-Object { $_.Source -notlike '*\Microsoft\WindowsApps\*' } |
        Select-Object -First 1
    if (-not $python) {
        throw 'No encontré una instalación de Python en PATH. Instala Python 3 y vuelve a abrir la terminal.'
    }
    New-Item -ItemType Directory -Path $resultado -Force -ErrorAction Stop |
        Out-Null

    & $python.Source --version
    if ($LASTEXITCODE -ne 0) {
        throw 'Python no pudo arrancar. Revisa su instalación y vuelve a abrir la terminal.'
    }
    $salida = & $python.Source $script $raiz
    if ($LASTEXITCODE -ne 0) {
        throw 'Falló la revisión de configuración. Revisa el mensaje de Python.'
    }

    $casos = ($salida -join [Environment]::NewLine) |
        ConvertFrom-Json -ErrorAction Stop
    $archivo = Join-Path $resultado 'configuracion.json'
    $salida | Set-Content -LiteralPath $archivo -Encoding UTF8 -ErrorAction Stop
    $casos | Sort-Object id | Format-Table id, estado, archivo, linea, detalle -Wrap
    "Informe guardado en $archivo"
}
```

Captura la tabla de `CFG01` a `CFG05`. `Patrón confirmado` indica que el script encontró la configuración descrita en el código. El archivo JSON conserva el detalle de cada coincidencia.

## Resultados guardados

| Caso | Qué se probó | Esperado | Observado |
| --- | --- | --- | --- |
| SEC01 | Listar usuarios sin token | 401 | 401 |
| SEC02 | Listar usuarios con JWT inválido | 401 | 401 |
| SEC03 | María y Lucía intentaron listar usuarios | 403 para María y 200 para Lucía | 403 y 200 |
| SEC04 | JWT de Lucía firmado fuera del login con la clave de respaldo | 401 | 200 |
| SEC05 | Login con la contraseña predeterminada de admin | 401 | 200 |
| SEC06 | María envió cuatro cuerpos JSON inválidos | 400 en cada solicitud | Cuatro respuestas 500. La cantidad de clientes visibles quedó igual. |
| SEC07 | Variante de inyección SQL en el nombre de usuario del login | 401 | 401 |
| SEC08 | Solicitud CORS desde un origen ajeno | 403 sin permitir el origen | 403 sin cabecera de origen permitido |
| SEC09 | 20 solicitudes seguidas con token inválido | 20 respuestas 401 | 20 respuestas 401 |
| SEC10 | Ana y Diego intentaron descargar el comprobante de Ana | 200 para Ana y rechazo para Diego | 200 para Ana y 403 para Diego |

Las salidas de la ejecución original están en [evidencia/](evidencia/). Para comparar tu ejecución, usa [la matriz de resultados](evidencia/matriz-seguridad.csv). La ráfaga de SEC09 solo comprueba las 20 solicitudes del caso. ZAP analizó las respuestas de tres rutas anónimas y no reportó alertas de sitio en esa muestra.

## Herramientas y referencias

| Página | Herramienta o referencia | Uso en esta revisión |
| --- | --- | --- |
| 27 | [Zenmap/Nmap](https://nmap.org/zenmap/) | Referencia para revisar puertos. |
| 29 | [OWASP Top 10](https://owasp.org/Top10/) | Referencia para clasificar riesgos web. |
| 31 | [Burp Suite](https://portswigger.net/burp) | Referencia para interceptar y repetir peticiones. |
| 33 | [ffuf](https://www.kali.org/tools/ffuf/) | Se ejecutó con una lista de seis rutas. |
| 35 | [JMeter](https://jmeter.apache.org/) | Referencia para pruebas de carga. |
| 36 | [Arachni](https://ecsypno.com/pages/arachni-web-application-security-scanner-framework) | Referencia de un escáner web. |
| 38 | [Snyk](https://docs.snyk.io/developer-tools/snyk-cli/commands/test) | Referencia para revisar dependencias. Se usaron npm audit y OSV. |
| 46 | [ZAP](https://www.zaproxy.org/docs/automate/automation-framework/) | Se ejecutó un plan pasivo con tres peticiones. |
| 46 | [sqlmap](https://sqlmap.org/) | Referencia de inyección SQL. El caso SEC07 se ejecutó con Node.js. |

La primera revisión se hizo sobre los binarios del ZIP y cubrió la referencia `5b8b29d022d4f335df96129b0814f9704e5b06d7`. La [guía de capturas](CAPTURAS.md) muestra cómo compilar desde el clon y obtener evidencia de una ejecución nueva.
