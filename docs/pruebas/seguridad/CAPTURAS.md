# Capturas de seguridad en Windows

Desde la carpeta donde quieres guardar el repositorio, ejecuta en PowerShell:

```powershell
git clone --branch pruebas/seguridad-transaduanas --single-branch https://github.com/JioDevR/TransAduanas.git
cd TransAduanas
```

Abre esa carpeta en VS Code. Ejecuta el bloque [Compila el backend y prepara el classpath](README.md#2-compila-el-backend-y-prepara-el-classpath) del README. Verifica Java 21 y `BUILD SUCCESS`. Conserva la misma terminal para que se mantengan las variables de los siguientes bloques.

## 1. Backend

```powershell
Set-Location $backend
node "$kit\seguridad-http.mjs" $backend $cp "$resultado\http.json" 2>&1 |
  Tee-Object -FilePath "$resultado\ejecucion-http.txt"
if ($LASTEXITCODE -ne 0) { throw 'Revisa ejecucion-http.txt.' }
Get-Content "$resultado\http.json" -Raw | ConvertFrom-Json |
  Sort-Object id | Format-Table id, resultado, observado -Wrap
```

Para la captura, muestra `SEC03` (403 para María), `SEC04` (200 con el JWT de respaldo), `SEC06` (cuatro respuestas 500) y `SEC10` (200 para Ana y 403 para Diego). Esta ejecución HTTP usa H2.

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
Get-Content "$resultado\frontend-resultados.json" -Raw | ConvertFrom-Json |
  Format-Table id, resultado, observado -Wrap
Invoke-Item $resultado
```

Abre las capturas `frontend-FE01-sin-sesion.png`, `frontend-FE02-login-rechazado.png` y `frontend-FE04-rol-comercial.png`. Muestran la redirección al login, el rechazo de credenciales y el regreso a Recepción Comercial al intentar entrar a Finanzas. La terminal también registra el inicio de sesión válido y el 403 de la API de usuarios desde el navegador. Las pruebas visuales corresponden al SGA interno. Para portal y landing se conservan los informes de dependencias.

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
Get-Content "$resultado\mysql-resultados.json" -Raw | ConvertFrom-Json |
  Format-Table id, esperado, observado, resultado -Wrap
```

Para la captura, muestra `1045` (contraseña incorrecta), `1142` (operaciones sin permiso) y `BD06 filas antes=1 después=1`. El conteo viene de `SELECT COUNT(*) FROM SGAdb.cliente` ejecutado antes y después del intento de insertar. Los permisos se configuraron en la cuenta del laboratorio. Para auditar los permisos del usuario de producción se necesita una revisión aparte.

## 4. Orden para exponer las siete fases

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
