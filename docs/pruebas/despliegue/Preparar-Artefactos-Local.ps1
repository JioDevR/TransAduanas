[CmdletBinding()]
param(
    [string]$Proyecto = ([IO.Path]::GetFullPath((Join-Path $PSScriptRoot "../../.."))),
    [string]$Salida = "$env:USERPROFILE\Music\TransAduanas-evidencias"
)
$ErrorActionPreference = 'Stop'
# Automatizacion local. Preparar el script no implica ejecutarlo.
# Este proceso recompila target: no usar con el backend manual ejecutandose.
if (Get-NetTCPConnection -LocalPort 8086 -State Listen -ErrorAction SilentlyContinue) {
    throw 'El backend esta activo en 8086. Coordinar su parada antes de ejecutar esta prueba.'
}
if (Get-Process java,javaw -ErrorAction SilentlyContinue) {
    Write-Warning 'Hay otros procesos Java activos. No se detendran automaticamente.'
}
Get-Item -LiteralPath "$Proyecto\backend-sga\mvnw.cmd" -ErrorAction Stop | Out-Null
foreach ($app in @('app-sga','app-b2b','app-landing')) {
    Get-Item -LiteralPath "$Proyecto\$app\package-lock.json" -ErrorAction Stop | Out-Null
}
$destino = Join-Path $Salida ("pipeline-local-" + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
New-Item -ItemType Directory -Path $destino | Out-Null
$nombresEnv = @('VITE_API_URL','VITE_SGA_APP_URL','VITE_PORTAL_CLIENTE_URL')
$anteriores = @{}
foreach ($nombre in $nombresEnv) {
    $anteriores[$nombre] = [Environment]::GetEnvironmentVariable($nombre,'Process')
}
$transcripcion = $false
$ubicacion = $false
try {
    Start-Transcript -LiteralPath "$destino\ejecucion.txt" | Out-Null
    $transcripcion = $true
    Push-Location -LiteralPath $Proyecto
    $ubicacion = $true
    if ((Test-Path -LiteralPath (Join-Path $Proyecto '.git')) -and (Get-Command git -ErrorAction SilentlyContinue)) {
        & git rev-parse HEAD | Set-Content "$destino\commit.txt" -Encoding UTF8
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo identificar el commit.' }
        & git status --short --untracked-files=all | Set-Content "$destino\cambios-locales.txt" -Encoding UTF8
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo registrar el estado local.' }
    } else {
        'Copia distribuida sin metadatos Git; no se atribuye un commit.' |
            Set-Content "$destino\commit.txt" -Encoding UTF8
        $fuentes = @()
        foreach ($modulo in @('backend-sga','app-sga','app-b2b','app-landing')) {
            $fuentes += Get-ChildItem -LiteralPath (Join-Path $Proyecto "$modulo/src") -File -Recurse
        }
        $fuentes += Get-Item "$Proyecto\backend-sga\pom.xml"
        foreach ($app in @('app-sga','app-b2b','app-landing')) {
            $fuentes += Get-Item "$Proyecto\$app\package.json", "$Proyecto\$app\package-lock.json"
        }
        $fuentes | Get-FileHash -Algorithm SHA256 |
            Export-Csv "$destino\fuentes-sha256.csv" -NoTypeInformation -Encoding UTF8
    }
    Write-Output 'Se construyen los archivos locales; revisar procedencia y hashes conservados.'
    java -version
    if ($LASTEXITCODE -ne 0) { throw 'Java no esta disponible.' }
    node --version
    if ($LASTEXITCODE -ne 0) { throw 'Node no esta disponible.' }
    npm.cmd --version
    Push-Location -LiteralPath "$Proyecto\backend-sga"
    try {
        & .\mvnw.cmd -v
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar Maven.' }
        & .\mvnw.cmd -B -ntp clean verify '-Dtest=BusinessFunctionalCasesTest'
        $codigoMaven = $LASTEXITCODE
        $reporte = 'target\surefire-reports\TEST-pe.sga.functional.BusinessFunctionalCasesTest.xml'
        if (Test-Path -LiteralPath $reporte) {
            Copy-Item -LiteralPath $reporte -Destination $destino
        }
        if ($codigoMaven -ne 0) { throw 'Fallo Maven. Revisar ejecucion.txt y el reporte si se genero.' }
        [xml]$xml = Get-Content -LiteralPath $reporte -Raw
        $suite = $xml.testsuite
        if ([int]$suite.tests -ne 15 -or [int]$suite.failures -ne 0 -or
            [int]$suite.errors -ne 0 -or [int]$suite.skipped -ne 0) {
            throw 'Se requieren 15 pruebas, cero fallos, errores y omisiones.'
        }
        [xml]$pom = Get-Content -LiteralPath '.\pom.xml' -Raw
        $version = [string]$pom.project.version
        $jar = "target\sga-backend-$version.jar"
        New-Item -ItemType Directory -Path "$destino\backend" | Out-Null
        Copy-Item -LiteralPath $jar -Destination "$destino\backend" -ErrorAction Stop
    } finally { Pop-Location }
    $env:VITE_API_URL = '/api/v1'
    $env:VITE_SGA_APP_URL = 'http://localhost:5180'
    $env:VITE_PORTAL_CLIENTE_URL = 'http://localhost:5174'
    foreach ($app in @('app-sga','app-b2b','app-landing')) {
        Push-Location -LiteralPath "$Proyecto\$app"
        try {
            npm.cmd ci
            if ($LASTEXITCODE -ne 0) { throw "Fallo npm ci en $app." }
            npm.cmd run build -- --mode staging
            if ($LASTEXITCODE -ne 0) { throw "Fallo la compilacion de $app." }
            Get-Item -LiteralPath '.\dist\index.html' -ErrorAction Stop | Out-Null
            Copy-Item -LiteralPath '.\dist' -Destination "$destino\$app" -Recurse
        } finally { Pop-Location }
    }
    # Materializar la lista antes de crear el manifiesto para evitar incluirlo.
    $archivos = @(Get-ChildItem -LiteralPath "$destino\backend" -File -Recurse)
    foreach ($app in @('app-sga','app-b2b','app-landing')) {
        $archivos += @(Get-ChildItem -LiteralPath "$destino\$app" -File -Recurse)
    }
    $hashes = @($archivos | Get-FileHash -Algorithm SHA256 -ErrorAction Stop)
    $hashes | Export-Csv -LiteralPath "$destino\hashes.csv" -NoTypeInformation -Encoding UTF8
    'CORRECTO: 15 pruebas y cuatro compilaciones completadas. Falta validar el despliegue.' |
        Set-Content -LiteralPath "$destino\resultado.txt" -Encoding UTF8
    Write-Output "Automatizacion local completada. Evidencias: $destino"
} catch {
    ('FALLIDO: ' + $_.Exception.Message) | Set-Content -LiteralPath "$destino\resultado.txt" -Encoding UTF8
    throw
} finally {
    foreach ($nombre in $nombresEnv) {
        [Environment]::SetEnvironmentVariable($nombre,$anteriores[$nombre],'Process')
    }
    if ($ubicacion) { Pop-Location }
    if ($transcripcion) { Stop-Transcript | Out-Null }
}
