# Capturas de seguridad en Windows

Desde la carpeta donde quieres guardar el repositorio, ejecuta en PowerShell:

```powershell
git clone --branch pruebas/seguridad-transaduanas --single-branch https://github.com/JioDevR/TransAduanas.git TransAduanas-seguridad
cd .\TransAduanas-seguridad
```

Abre el clon en VS Code. Si ya lo tenías, abre PowerShell en la carpeta del proyecto y actualiza la rama:

```powershell
git switch pruebas/seguridad-transaduanas
git pull --ff-only origin pruebas/seguridad-transaduanas
```

## Preparación común

Usa PowerShell con JDK 21 y Node.js 24. El bloque prepara las variables y compila con Maven. Cambia la primera ruta si guardaste el clon en otra carpeta. Ejecuta este bloque una vez por terminal, incluso si vas directo a Postman.

La primera compilación puede descargar Maven y las dependencias del proyecto. Continúa cuando aparezca `BUILD SUCCESS`.

```powershell
. {
    $raiz = "$env:USERPROFILE\Music\TransAduanas-seguridad"
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

Si Node muestra `Cannot find module 'C:\seguridad-http.mjs'`, vuelve a pegar la preparación completa en esa terminal y continúa con el apartado que necesitas.

## 1. Backend

Ejecuta este bloque en la misma terminal para obtener los diez resultados:

```powershell
. {
    Set-Location -LiteralPath $backend -ErrorAction Stop
    node $script $backend $cp "$resultado\http.json" 2>&1 |
        Tee-Object -FilePath "$resultado\ejecucion-http.txt"
    if ($LASTEXITCODE -ne 0) { throw "La ejecución se interrumpió. Revisa $resultado\ejecucion-http.txt." }
    $casos = Get-Content -LiteralPath "$resultado\http.json" -Raw -Encoding UTF8 | ConvertFrom-Json
    $casos | Sort-Object id | Format-Table id, resultado, observado -Wrap
}
```

Captura la tabla de `SEC01` a `SEC10`. Para mostrarla otra vez, pega las dos últimas líneas del bloque en la misma terminal. En la ejecución de referencia, `SEC04`, `SEC05` y `SEC06` figuran como `Falló` porque reprodujeron hallazgos. Esta ejecución HTTP usa H2.

## 2. Frontend SGA en navegador

El script abre Edge o Chrome automáticamente y usa el backend local de pruebas. Si ninguno está instalado en su ruta habitual, ejecuta `.\node_modules\.bin\playwright.cmd install chromium` después de `npm.cmd ci` y antes de volver a `backend`.

```powershell
Set-Location (Join-Path $raiz 'app-sga')
npm.cmd ci
if ($LASTEXITCODE -ne 0) { throw 'npm ci falló en app-sga.' }
Set-Location $backend
node "$kit\seguridad-http.mjs" $backend $cp "$resultado\http-frontend.json" --frontend 2>&1 |
  Tee-Object -FilePath "$resultado\ejecucion-frontend.txt"
if ($LASTEXITCODE -ne 0) { throw 'Revisa ejecucion-frontend.txt.' }
$casosFrontend = Get-Content -LiteralPath "$resultado\frontend-resultados.json" -Raw -Encoding UTF8 | ConvertFrom-Json
$casosFrontend | Sort-Object id | Format-Table id, resultado, observado -Wrap
Invoke-Item $resultado
```

Captura la tabla de `FE01` a `FE08`. El último comando abre la carpeta de resultados. Allí están `frontend-FE01-sin-sesion.png`, `frontend-FE02-login-rechazado.png`, `frontend-FE04-rol-comercial.png` y `frontend-FE07-sesion-cerrada.png`. La tabla también documenta los casos que no generan imagen, como la contraseña oculta (`FE06`) y el rechazo de una redirección externa (`FE08`). Las pruebas visuales corresponden al SGA interno. Para portal y landing se conservan los informes de dependencias.

## 3. Base de datos MySQL

Este script crea una instancia separada en un puerto local libre, aplica `db/schema.sql` y crea una cuenta de lectura. Usa los binarios de MySQL Community Server 8.4. La contraseña de tu servicio de Windows no interviene.

```powershell
$mysqlBin = 'C:\Program Files\MySQL\MySQL Server 8.4\bin'
if (-not (Test-Path (Join-Path $mysqlBin 'mysqld.exe'))) {
  $mysqlBin = Read-Host 'Ruta de la carpeta bin de MySQL, sin comillas'
}
Set-Location $raiz
node "$kit\seguridad-mysql.mjs" $mysqlBin $raiz $resultado 2>&1 |
  Tee-Object -FilePath "$resultado\ejecucion-mysql.txt"
if ($LASTEXITCODE -ne 0) { throw 'Revisa ejecucion-mysql.txt y los logs en mysql-aislado-*.' }
$casosMySQL = Get-Content -LiteralPath "$resultado\mysql-resultados.json" -Raw -Encoding UTF8 | ConvertFrom-Json
$casosMySQL | Sort-Object id | Format-Table id, esperado, observado, resultado -Wrap
$conteo = $casosMySQL | Where-Object id -eq 'BD06'
"BD06 | Filas antes: $($conteo.esperado) | Filas después: $($conteo.observado)"
```

En una sola imagen, captura la tabla de `BD01` a `BD08` y la línea `BD06 | Filas antes: 1 | Filas después: 1`. El conteo viene de `SELECT COUNT(*) FROM SGAdb.cliente` ejecutado antes y después de los intentos de insertar y borrar. Los permisos se configuraron en la cuenta del laboratorio.

## 4. Sistema integrado

Estos casos atraviesan la API, el filtro JWT, los servicios y los registros de H2. La prueba desactiva y reactiva a Diego en el laboratorio y comprueba qué puede hacer su cuenta en cada estado.

Cuando aparezca `Esperando el arranque del backend de pruebas`, deja abierta la terminal. El script espera hasta 120 segundos a que Tomcat y los datos de prueba estén listos antes de ejecutar los casos.

```powershell
Set-Location $backend
node "$kit\seguridad-http.mjs" $backend $cp "$resultado\http-sistema.json" --sistema 2>&1 |
  Tee-Object -FilePath "$resultado\ejecucion-sistema.txt"
if ($LASTEXITCODE -ne 0) { throw 'Revisa ejecucion-sistema.txt.' }
$casosSistema = Get-Content -LiteralPath "$resultado\sistema-resultados.json" -Raw -Encoding UTF8 | ConvertFrom-Json
$casosSistema | Sort-Object id | Format-Table id, resultado, observado -Wrap
```

Captura la tabla de `SIS01` a `SIS08`. Para explicar el flujo de la cuenta, señala `SIS04` a `SIS08`.

## 5. Prueba general sin Postman

Después de la preparación común y de instalar las dependencias de `app-sga` con `npm.cmd ci`, vuelve a esa misma terminal. Usa la ruta de MySQL del apartado 3:

```powershell
$mysqlBin = 'C:\Program Files\MySQL\MySQL Server 8.4\bin'
if (-not (Test-Path (Join-Path $mysqlBin 'mysqld.exe'))) {
  throw 'Corrige la ruta de la carpeta bin de MySQL en la primera línea.'
}
Set-Location $raiz
node "$kit\seguridad-general.mjs" $backend $cp $mysqlBin $resultado 2>&1 |
  Tee-Object -FilePath "$resultado\ejecucion-general.txt"
$codigo = $LASTEXITCODE
"Código de la prueba general: $codigo"
```

Captura las seis líneas del resumen. Incluye las cuatro de Backend, Frontend SGA, Base de datos MySQL y Sistema, además de `Resultado general` y `Código de la prueba general`. El código `1` significa que se ejecutaron todos los casos y hubo hallazgos. El código `2` indica una ejecución incompleta. El resumen JSON está en la carpeta `general-*` cuya ruta imprime la terminal.

## 6. Postman

Después de la preparación común, instala las dependencias de `app-sga`. Este paso sirve para Newman y Postman Desktop:

```powershell
Set-Location -LiteralPath (Join-Path $raiz 'app-sga') -ErrorAction Stop
npm.cmd ci
if ($LASTEXITCODE -ne 0) { throw 'npm ci falló en app-sga.' }
```

### Por consola con Newman

La colección contiene **16 solicitudes**. Hay cuatro por bloque (Backend, Frontend, Base de datos por API y Sistema).

```powershell
Set-Location $backend
npm.cmd install --prefix "$resultado\postman-tools" newman@6.2.1 --no-audit --no-fund
if ($LASTEXITCODE -ne 0) { throw 'No se pudo instalar Newman.' }
node "$kit\seguridad-http.mjs" $backend $cp "$resultado\http-postman.json" --postman 2>&1 |
  Tee-Object -FilePath "$resultado\ejecucion-postman.txt"
if ($LASTEXITCODE -ne 0) { throw 'Revisa ejecucion-postman.txt.' }
```

### En Postman Desktop

Para sacar la captura en la aplicación, arranca el servidor de laboratorio en la misma terminal:

```powershell
Set-Location $backend
node "$kit\seguridad-http.mjs" $backend $cp "$resultado\http-postman.json" --postman-servidor
```

1. Deja la terminal abierta. El entorno se genera en `backend\target\seguridad-local\postman-local.postman_environment.json` y la terminal imprime su ruta completa. Si reiniciaste el servidor, usa el entorno de la nueva sesión.
2. En Postman Desktop, abre un espacio de trabajo privado y pulsa **Import**. Importa `docs\pruebas\seguridad\TransAduanas.postman_collection.json` desde el clon y luego el entorno que imprimió la terminal. Si aparece **Upgrade to v3**, cambia del modo Git local al espacio de trabajo antes de importar los JSON.
3. Si Postman muestra **Unlock your vault** al importar el entorno, usa tu clave de **Postman Vault** para proteger los JWT temporales. Conserva esa clave y los tokens fuera de las capturas.
4. En el selector de arriba a la derecha, cambia **No Environment** por **TransAduanas local - descartar al terminar**.
5. En **Collections**, haz clic en el nombre **TransAduanas - Seguridad por componente** y pulsa **Run**. Si el botón no aparece, abre **Tools → Runner** en la barra inferior y arrastra el nombre de la colección al Runner.
6. Selecciona **Functional → Local**, deja **1 iteración** y las **16 solicitudes** marcadas. Conserva el orden de **Base de datos por API**. Pulsa **Start run**.
7. Guarda una captura del resumen en **Run results**. La ejecución de referencia mostró **32 aprobadas, 0 fallidas, 0 omitidas y 0 errores**. Si necesitas evidencia por componente, usa la tabla de abajo.
8. Vuelve a PowerShell y pulsa **Ctrl+C**. Elimina de Postman el entorno importado.

| Captura | Qué mostrar |
| --- | --- |
| Resumen | Nombre de la colección y los resultados de las 16 solicitudes. En la ejecución de referencia, las 32 aserciones pasaron. |
| Backend | `BE04`, origen CORS externo. Estado `403` y las dos aserciones. |
| Frontend | `FE04`, usuario comercial por el proxy del SGA. Estado `403` y las dos aserciones. |
| Base de datos por API | `BD04`, conteo posterior sin nuevas altas. Estado `200` y las dos aserciones. Ábrelo desde la ejecución completa. |
| Sistema | `SIS02`, Diego intenta descargar el comprobante de Ana. Estado `403` y las dos aserciones. |

Abre los casos individuales desde los resultados de la ejecución. Procura que se vean el nombre del caso, el estado HTTP y las aserciones, sin mostrar el encabezado `Authorization` ni los valores de los tokens.

En Postman, Frontend prueba la respuesta HTML y el proxy HTTP de Vite. Base de datos comprueba registros **a través de la API con H2**. Para capturar la navegación real y los permisos SQL directos, usa los apartados 2 y 3.

## 7. Consultar resultados guardados

Abre PowerShell en la carpeta principal del clon. Cambia el valor de `$archivo` según la tabla para mostrar la salida de tu ejecución. Este bloque solo lee el archivo.

```powershell
$archivo = 'http.json'
$carpeta = Join-Path (Get-Location).Path 'backend\target\seguridad-local'
$ruta = Join-Path $carpeta $archivo
$casos = Get-Content -LiteralPath $ruta -Raw -Encoding UTF8 -ErrorAction Stop | ConvertFrom-Json
$casos | Sort-Object id | Format-Table id, resultado, esperado, observado -Wrap
```

| Bloque | Archivo |
| --- | --- |
| Backend | `http.json` |
| Frontend | `frontend-resultados.json` |
| MySQL | `mysql-resultados.json` |
| Sistema | `sistema-resultados.json` |

Para mostrar un caso concreto, por ejemplo el conteo de MySQL después de cargar su archivo, usa:

```powershell
$casos | Where-Object id -eq 'BD06' | Format-List *
```

Para la prueba general, el resumen de terminal queda en `backend\target\seguridad-local\ejecucion-general.txt`. Para Postman Desktop usa **Run results**. Si ejecutaste Newman, abre `backend\target\seguridad-local\ejecucion-postman.txt`.

## 8. Qué mostrar por tipo de prueba

| Tipo | Evidencia y dónde encontrarla |
| --- | --- |
| Penetración | Backend, `SEC10`. Muestra que el titular recibió el comprobante con HTTP 200 y la otra empresa recibió HTTP 403. |
| Vulnerabilidades | [README, análisis de dependencias y ZAP](README.md). Muestra el informe de la herramienta y el alcance que se examinó. |
| Seguridad | Backend, `SEC01` a `SEC03`. Muestra las respuestas de autenticación y autorización. Añade las tablas del SGA, MySQL y sistema para documentar cada componente. |
| Deserialización | Backend, `SEC06`. Muestra las respuestas HTTP observadas con JSON inválido y el conteo antes y después en la salida de terminal. |
| Configuración | [README, revisión de configuración](README.md). Muestra `configuracion.json` y la referencia al archivo que revisa cada caso. |
| Fuzzing | [README, búsqueda de rutas con ffuf](README.md). Muestra `ffuf-rutas.json` y el listado de rutas usado en esa ejecución. |
| Otros | Backend, `SEC09`, y el resumen de la prueba general. Muestra el resultado de las 20 solicitudes secuenciales y los totales por componente. |

## 9. Orden para exponer las siete fases

| Fase | Qué mostrar |
| --- | --- |
| 1. Preparación | Arranque del backend con H2 y de MySQL en una instancia de laboratorio. |
| 2. Reconocimiento y dependencias | `ffuf-rutas.json`, `npm-audit-*.json` y `osv-backend.json`. |
| 3. Escaneo web | `zap-pasivo.json` de tres rutas anónimas. Escaneo activo pendiente. |
| 4. Acceso e inyección | Casos de rol y propiedad del backend, capturas del SGA, permisos de MySQL y variante SQLi en login. |
| 5. Entradas inválidas | `SEC06` en `http.json`. Cuatro respuestas 500 y el mismo conteo visible de clientes. |
| 6. Robustez | `SEC09` en `http.json`. Veinte solicitudes secuenciales. Carga concurrente pendiente. |
| 7. Reporte y versión | Rama con código, reportes y capturas. Pipeline de CI pendiente. |

Para repetir ffuf, ZAP y los análisis de dependencias, usa los bloques correspondientes del [README](README.md). Los archivos originales están en [evidencia](evidencia/).
