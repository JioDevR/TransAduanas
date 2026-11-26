[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)]
    [ValidateSet('Preparar','Desplegar','Revertir')][string]$Accion,
    [string]$Jar,
    [string]$Version,
    [string]$Raiz = "$env:USERPROFILE\Music\TransAduanas-desplegado",
    [string]$Servicio = 'TransAduanasBackend'
)
$ErrorActionPreference = 'Stop'
$estadoArchivo = Join-Path $Raiz 'estado.json'
$actual = Join-Path $Raiz 'current\app.jar'
function Leer-Estado {
    if (Test-Path -LiteralPath $estadoArchivo) {
        return Get-Content -LiteralPath $estadoArchivo -Raw | ConvertFrom-Json
    }
    return $null
}
function Esperar-Salud {
    for ($intento=0; $intento -lt 30; $intento++) {
        try {
            $salud = Invoke-RestMethod 'http://127.0.0.1:8086/api/v1/health' -TimeoutSec 3
            if ($salud.data.status -eq 'UP') { return }
        } catch { }
        Start-Sleep -Seconds 2
    }
    throw 'El backend no alcanzo UP. Revisar los registros.'
}
function Comprobar-Release($release) {
    if ($null -eq $release) { throw 'Falta una version de destino.' }
    if ((Get-FileHash -LiteralPath $release.Ruta -Algorithm SHA256).Hash -ne $release.Hash) {
        throw 'El artefacto conservado no coincide con su hash.'
    }
}
foreach ($carpeta in @('current','releases','logs','config')) {
    New-Item -ItemType Directory -Path (Join-Path $Raiz $carpeta) -Force | Out-Null
}
$estado = Leer-Estado
if ($Accion -eq 'Revertir') {
    if ($null -eq $estado -or $null -eq $estado.Anterior) {
        throw 'No existe una version anterior desplegada. No se simula un rollback.'
    }
    $destino = $estado.Anterior
} else {
    if ([string]::IsNullOrWhiteSpace($Jar) -or [string]::IsNullOrWhiteSpace($Version)) {
        throw 'Preparar y Desplegar requieren -Jar y -Version.'
    }
    $fuente = (Resolve-Path -LiteralPath $Jar).Path
    if ([IO.Path]::GetExtension($fuente) -ne '.jar') { throw 'Se requiere un archivo JAR.' }
    $hash = (Get-FileHash -LiteralPath $fuente -Algorithm SHA256).Hash
    $carpetaRelease = Join-Path $Raiz "releases\$hash"
    New-Item -ItemType Directory -Path $carpetaRelease -Force | Out-Null
    $copia = Join-Path $carpetaRelease 'app.jar'
    if (-not (Test-Path -LiteralPath $copia)) {
        Copy-Item -LiteralPath $fuente -Destination $copia
    }
    $destino = [PSCustomObject]@{Version=$Version; Hash=$hash; Ruta=$copia}
    Comprobar-Release $destino
    $destino | ConvertTo-Json | Set-Content (Join-Path $carpetaRelease 'artefacto.json') -Encoding UTF8
}
Comprobar-Release $destino
if ($Accion -eq 'Preparar') {
    if (Get-Service -Name $Servicio -ErrorAction SilentlyContinue) {
        throw 'Preparar solo se permite antes de instalar el servicio.'
    }
    if (Test-Path -LiteralPath $actual) {
        throw 'Ya hay un JAR actual. No se sobrescribio. Usar Desplegar cuando corresponda.'
    }
    Copy-Item -LiteralPath $destino.Ruta -Destination $actual
    [PSCustomObject]@{Actual=$destino; Anterior=$null} | ConvertTo-Json -Depth 5 |
        Set-Content -LiteralPath $estadoArchivo -Encoding UTF8
    Write-Output 'Primer artefacto preparado. No se ha arrancado ningun servicio.'
    exit 0
}
$identidad = [Security.Principal.WindowsIdentity]::GetCurrent()
if (-not ([Security.Principal.WindowsPrincipal]::new($identidad)).IsInRole(
    [Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw 'La ejecucion de Desplegar/Revertir requiere PowerShell como administrador.'
}
$svc = Get-Service -Name $Servicio -ErrorAction Stop
if ($null -eq $estado) { throw 'No hay estado inicial. Preparar primero el artefacto.' }
Comprobar-Release $estado.Actual
if ((Get-FileHash -LiteralPath $actual -Algorithm SHA256).Hash -ne $estado.Actual.Hash) {
    throw 'El JAR actual fue cambiado fuera del procedimiento.'
}
if ($destino.Hash -eq $estado.Actual.Hash) {
    throw 'Los artefactos son identicos. No hay cambio de version que desplegar o revertir.'
}
if ($svc.Status -ne 'Stopped') {
    Stop-Service -Name $Servicio -ErrorAction Stop
    (Get-Service -Name $Servicio).WaitForStatus('Stopped', [TimeSpan]::FromSeconds(60))
}
if (Get-NetTCPConnection -LocalPort 8086 -State Listen -ErrorAction SilentlyContinue) {
    throw 'El puerto 8086 sigue ocupado por otro proceso. No se terminara automaticamente.'
}
try {
    Copy-Item -LiteralPath $destino.Ruta -Destination $actual -Force
    Start-Service -Name $Servicio
    Esperar-Salud
    [PSCustomObject]@{Actual=$destino; Anterior=$estado.Actual} | ConvertTo-Json -Depth 5 |
        Set-Content -LiteralPath $estadoArchivo -Encoding UTF8
    Write-Output "Operacion completada: $Accion. Hash: $($destino.Hash). Health: UP."
    Write-Output 'Falta verificar la funcion de negocio y el proxy para aprobar la prueba.'
} catch {
    $fallo = $_
    try {
        Stop-Service -Name $Servicio -ErrorAction Stop
        (Get-Service -Name $Servicio).WaitForStatus('Stopped', [TimeSpan]::FromSeconds(60))
        Copy-Item -LiteralPath $estado.Actual.Ruta -Destination $actual -Force
        Start-Service -Name $Servicio
        Esperar-Salud
        Write-Warning 'Se recupero el artefacto previo; la operacion solicitada fallo.'
    } catch {
        Write-Warning 'No se pudo confirmar la recuperacion automatica. Revisar servicio y logs.'
    }
    throw $fallo
}
