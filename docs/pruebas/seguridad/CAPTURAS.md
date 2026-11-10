# Capturas de seguridad en Windows

Desde la carpeta donde quieres guardar el repositorio, ejecuta en PowerShell:

```powershell
git clone --branch pruebas/seguridad-transaduanas --single-branch https://github.com/JioDevR/TransAduanas.git TransAduanas-seguridad
cd .\TransAduanas-seguridad
```

Abre el clon en VS Code. Si ya lo tenías, entra en la rama `pruebas/seguridad-transaduanas` y ejecuta `git pull --ff-only` desde el clon para actualizar la guía.

## 1. Backend

Copia **todo** el bloque en una terminal PowerShell. Usa la ruta de tu clon en la primera asignación de `$raiz`. El ejemplo apunta a `Music\TransAduanas-seguridad`. Este bloque prepara las variables, compila con Maven y ejecuta los diez casos de backend. Conserva la misma terminal para los demás apartados. Si abres otra terminal, vuelve a pegar el bloque completo.

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

Después de ejecutar el bloque 1 y `npm.cmd ci` en `app-sga`, vuelve a esa misma terminal. Usa la ruta de MySQL del apartado 3:

```powershell
Set-Location $raiz
node "$kit\seguridad-general.mjs" $backend $cp $mysqlBin $resultado 2>&1 |
  Tee-Object -FilePath "$resultado\ejecucion-general.txt"
$codigo = $LASTEXITCODE
"Código de la prueba general: $codigo"
```

Captura las seis líneas del resumen. Incluye las cuatro de Backend, Frontend SGA, Base de datos MySQL y Sistema, además de `Resultado general` y `Código de la prueba general`. El código `1` significa que se ejecutaron todos los casos y hubo hallazgos. El código `2` indica una ejecución incompleta. El resumen JSON está en la carpeta `general-*` cuya ruta imprime la terminal.

## 6. Postman

Para repetir los **16 casos** por consola con Newman, ejecuta primero `npm.cmd ci` en `app-sga`, como en el apartado 2. La colección se divide en Backend, Frontend, Base de datos por API y Sistema, con cuatro casos por carpeta:

```powershell
Set-Location $backend
npm.cmd install --prefix "$resultado\postman-tools" newman@6.2.1 --no-audit --no-fund
if ($LASTEXITCODE -ne 0) { throw 'No se pudo instalar Newman.' }
node "$kit\seguridad-http.mjs" $backend $cp "$resultado\http-postman.json" --postman 2>&1 |
  Tee-Object -FilePath "$resultado\ejecucion-postman.txt"
if ($LASTEXITCODE -ne 0) { throw 'Revisa ejecucion-postman.txt.' }
```

Para sacar una captura en la aplicación **Postman Desktop**, arranca solo el servidor de laboratorio con este comando:

```powershell
Set-Location $backend
node "$kit\seguridad-http.mjs" $backend $cp "$resultado\http-postman.json" --postman-servidor
```

1. Deja la terminal abierta. El ejecutor imprime la ruta de `postman-local.postman_environment.json`.
2. En Postman Desktop, pulsa **Import**. Importa `docs\pruebas\seguridad\TransAduanas.postman_collection.json` desde el clon y luego el entorno local que imprimió la terminal.
3. Selecciona **TransAduanas local - descartar al terminar** como entorno activo.
4. Abre **TransAduanas - Seguridad por componente**, pulsa **Run** y selecciona **Run manually**. Ejecuta una iteración con las 16 solicitudes seleccionadas. Conserva el orden de **Base de datos por API** para que el conteo se compruebe después de los intentos de alta.
5. Toma las cinco capturas indicadas abajo antes de cerrar el laboratorio.
6. Vuelve a PowerShell y pulsa **Ctrl+C**. Elimina de Postman el entorno importado. Evita mostrar o compartir sus tokens temporales.

| Captura | Qué mostrar |
| --- | --- |
| Resumen | Nombre de la colección y los resultados de las 16 solicitudes. En la ejecución de referencia, las 32 aserciones pasaron. |
| Backend | `BE04`, origen CORS externo. Estado `403` y las dos aserciones. |
| Frontend | `FE04`, usuario comercial por el proxy del SGA. Estado `403` y las dos aserciones. |
| Base de datos por API | `BD04`, conteo posterior sin nuevas altas. Estado `200` y las dos aserciones. Ábrelo desde la ejecución completa. |
| Sistema | `SIS02`, Diego intenta descargar el comprobante de Ana. Estado `403` y las dos aserciones. |

Abre los casos individuales desde los resultados de la ejecución. Procura que se vean el nombre del caso, el estado HTTP y las aserciones, sin mostrar el encabezado `Authorization` ni los valores de los tokens.

En Postman, Frontend prueba la respuesta HTML y el proxy HTTP de Vite. Base de datos comprueba registros **a través de la API con H2**. Para capturar la navegación real y los permisos SQL directos, usa los apartados 2 y 3.

## 7. Orden para exponer las siete fases

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
