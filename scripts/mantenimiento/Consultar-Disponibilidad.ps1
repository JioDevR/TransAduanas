[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)]
    [string]$DirectorioResultados
)
$ErrorActionPreference = 'Stop'
# Solo consulta el servicio y su endpoint de salud; nunca los inicia ni detiene.
$taskZoneOffset = [TimeSpan]::FromHours(-5)
$taskStart = [DateTimeOffset]::UtcNow.ToOffset($taskZoneOffset)
$taskWatch = [Diagnostics.Stopwatch]::StartNew()
$taskChecks = @()
try {
    $taskService = Get-Service -Name 'TransAduanasBackend' -ErrorAction Stop
    $taskChecks += [pscustomobject]@{
        Nombre = 'Servicio Windows'
        Esperado = 'Running'
        Obtenido = [string]$taskService.Status
        Cumple = ($taskService.Status -eq 'Running')
        Error = $null
    }
} catch {
    $taskChecks += [pscustomobject]@{
        Nombre = 'Servicio Windows'; Esperado = 'Running'; Obtenido = $null
        Cumple = $false; Error = $_.Exception.GetType().FullName
    }
}
try {
    $taskResponse = Invoke-WebRequest -Uri 'http://127.0.0.1:8086/api/v1/health' -UseBasicParsing -TimeoutSec 10 -ErrorAction Stop
    $taskPayload = $taskResponse.Content | ConvertFrom-Json
    $taskChecks += [pscustomobject]@{
        Nombre = 'Salud HTTP'
        Esperado = 'HTTP 200 y UP'
        Obtenido = ('HTTP {0}; {1}' -f [int]$taskResponse.StatusCode,[string]$taskPayload.data.status)
        Cumple = ([int]$taskResponse.StatusCode -eq 200 -and $taskPayload.data.status -eq 'UP')
        Error = $null
    }
} catch {
    $taskChecks += [pscustomobject]@{
        Nombre = 'Salud HTTP'; Esperado = 'HTTP 200 y UP'; Obtenido = $null
        Cumple = $false; Error = $_.Exception.GetType().FullName
    }
}
$taskWatch.Stop()
$taskEnd = [DateTimeOffset]::UtcNow.ToOffset($taskZoneOffset)
$taskOk = (@($taskChecks | Where-Object { -not $_.Cumple }).Count -eq 0)
$taskExit = if ($taskOk) { 0 } else { 1 }
$taskResult = [pscustomobject]@{
    NombreTarea = 'T01 Revision de disponibilidad TransAduanas'
    IdEjecucion = [Guid]::NewGuid().ToString('N')
    FechaInicio = $taskStart.ToString('o')
    FechaFin = $taskEnd.ToString('o')
    ZonaHoraria = 'America/Lima UTC-05:00'
    DuracionSegundos = [Math]::Round($taskWatch.Elapsed.TotalSeconds,6)
    Estado = $(if ($taskOk) { 'Exitoso' } else { 'Fallo' })
    CodigoSalida = $taskExit
    Comprobaciones = $taskChecks
    Alcance = 'Consulta de lectura. No valida todas las funciones de negocio.'
}
$null = New-Item -ItemType Directory -Path $DirectorioResultados -Force -ErrorAction Stop
$taskFile = Join-Path $DirectorioResultados ('resumen-' + $taskResult.IdEjecucion + '.json')
$taskJson = $taskResult | ConvertTo-Json -Depth 6
[IO.File]::WriteAllText($taskFile,$taskJson,[Text.UTF8Encoding]::new($false))
Write-Output $taskJson
Write-Output ('Evidencia: ' + $taskFile)
exit $taskExit
