[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$Laboratorio,
    [Parameter(Mandatory=$true)][string]$Helper,
    [Parameter(Mandatory=$true)][ValidateSet('A','B','C')][string]$Instancia,
    [switch]$EsperarLiberacion
)
$ErrorActionPreference = 'Stop'
$taskLab = (Get-Item -LiteralPath $Laboratorio -ErrorAction Stop).FullName
$taskMarker = Get-Content -LiteralPath (Join-Path $taskLab 'laboratorio.json') -Raw | ConvertFrom-Json
if ($taskMarker.Prueba -ne 'PM08' -or $taskMarker.Raiz -ne $taskLab) { throw 'Laboratorio PM08 no identificado.' }
. $Helper
$taskWatch = [Diagnostics.Stopwatch]::StartNew()
$taskStart = [DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(-5))
$taskResult = Invoke-TareaConBloqueo -RutaBloqueo (Join-Path $taskLab 'tarea.lock') -Accion {
    $taskEntry = [pscustomobject]@{Instancia=$Instancia;Proceso=$PID;Entrada=[DateTimeOffset]::UtcNow.ToString('o')}
    [IO.File]::WriteAllText((Join-Path $taskLab ('entrada-'+$Instancia+'.json')),($taskEntry | ConvertTo-Json),[Text.UTF8Encoding]::new($false))
    if ($EsperarLiberacion) {
        $taskWait = [Diagnostics.Stopwatch]::StartNew()
        while (-not (Test-Path -LiteralPath (Join-Path $taskLab 'continuar-A'))) {
            if ($taskWait.Elapsed.TotalSeconds -gt 25) { throw 'El coordinador no libero la barrera a tiempo.' }
            Start-Sleep -Milliseconds 50
        }
    }
    $taskCounterPath = Join-Path $taskLab 'contador.txt'
    $taskCount = [int][IO.File]::ReadAllText($taskCounterPath)
    [IO.File]::WriteAllText($taskCounterPath,([string]($taskCount+1)),[Text.UTF8Encoding]::new($false))
    $taskExit = [pscustomobject]@{Instancia=$Instancia;Salida=[DateTimeOffset]::UtcNow.ToString('o')}
    [IO.File]::WriteAllText((Join-Path $taskLab ('salida-'+$Instancia+'.json')),($taskExit | ConvertTo-Json),[Text.UTF8Encoding]::new($false))
    return ('Accion completada '+$Instancia)
}
$taskWatch.Stop()
$taskSummary = [pscustomobject]@{
    Instancia=$Instancia;Proceso=$PID
    NombreTarea='PM08 Tarea compartida de laboratorio'
    FechaInicio=$taskStart.ToString('o')
    FechaFin=[DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(-5)).ToString('o')
    DuracionSegundos=$taskWatch.Elapsed.TotalSeconds
    Estado=$taskResult.Estado;Adquirido=$taskResult.Adquirido
    CodigoNativo=$taskResult.CodigoNativo
    CodigoSalida=0
    Nota='OmitidaPorBloqueo es un resultado controlado de concurrencia, no un fallo de la aplicacion.'
}
[IO.File]::WriteAllText((Join-Path $taskLab ('resumen-'+$Instancia+'.json')),($taskSummary | ConvertTo-Json),[Text.UTF8Encoding]::new($false))
$taskSummary | ConvertTo-Json
exit 0
