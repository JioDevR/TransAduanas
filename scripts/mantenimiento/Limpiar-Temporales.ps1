[CmdletBinding(SupportsShouldProcess=$true)]
param(
    [Parameter(Mandatory=$true)][string]$CarpetaLaboratorio,
    [Parameter(Mandatory=$true)][string]$DirectorioResultados,
    [ValidateRange(1,3650)][int]$DiasRetencion = 7
)
$ErrorActionPreference = 'Stop'
$taskStart = [DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(-5))
$taskWatch = [Diagnostics.Stopwatch]::StartNew()
$taskRootItem = Get-Item -LiteralPath $CarpetaLaboratorio -ErrorAction Stop
$taskRoot = $taskRootItem.FullName.TrimEnd([IO.Path]::DirectorySeparatorChar)
if (-not $taskRootItem.PSIsContainer -or ($taskRootItem.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
    throw 'Se requiere una carpeta de laboratorio ordinaria.'
}
$taskMarkerPath = Join-Path $taskRoot '.transaduanas-laboratorio.json'
$taskMarker = Get-Content -LiteralPath $taskMarkerPath -Raw | ConvertFrom-Json
if ($taskMarker.Prueba -ne 'PM05' -or $taskMarker.Raiz -ne $taskRoot) {
    throw 'La marca no identifica este laboratorio PM05.'
}
$taskTempItem = Get-Item -LiteralPath (Join-Path $taskRoot 'temporales') -ErrorAction Stop
if (-not $taskTempItem.PSIsContainer -or ($taskTempItem.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
    throw 'La carpeta temporales no es un directorio ordinario.'
}
$taskTempRoot = $taskTempItem.FullName.TrimEnd([IO.Path]::DirectorySeparatorChar)
if ([IO.Directory]::GetParent($taskTempRoot).FullName -ne $taskRoot) {
    throw 'La carpeta temporal debe ser hija directa del laboratorio.'
}
$taskLimitUtc = [DateTime]::UtcNow.AddDays(-$DiasRetencion)
$taskEvents = @()
foreach ($taskFile in (Get-ChildItem -LiteralPath $taskTempRoot -File -ErrorAction Stop)) {
    $taskResolvedFile = [IO.Path]::GetFullPath($taskFile.FullName)
    if ([IO.Path]::GetDirectoryName($taskResolvedFile) -ne $taskTempRoot) {
        throw 'El archivo queda fuera del directorio de temporales validado.'
    }
    $taskReason = $null
    $taskAction = 'Conservado'
    $taskError = $null
    if ($taskFile.Attributes -band [IO.FileAttributes]::ReparsePoint) {
        $taskReason = 'Enlace excluido'
    } elseif ($taskFile.Extension.ToLowerInvariant() -notin @('.tmp','.temp')) {
        $taskReason = 'Extension no temporal'
    } elseif ($taskFile.LastWriteTimeUtc -ge $taskLimitUtc) {
        $taskReason = 'No vencido'
    } else {
        $taskReason = 'Temporal vencido'
        if ($PSCmdlet.ShouldProcess($taskResolvedFile,'Eliminar temporal vencido de laboratorio')) {
            try {
                # Una eliminación individual; nunca se eliminan carpetas ni se usa -Recurse.
                [IO.File]::Delete($taskResolvedFile)
                $taskAction = 'Eliminado'
            } catch {
                $taskAction = 'Fallo'; $taskError = $_.Exception.GetType().FullName
            }
        } else {
            $taskAction = 'No eliminado'
        }
    }
    $taskEvents += [pscustomobject]@{
        Archivo=$taskResolvedFile; UltimaEscrituraUtc=$taskFile.LastWriteTimeUtc.ToString('o')
        Motivo=$taskReason; Accion=$taskAction; Error=$taskError
    }
}
$taskWatch.Stop()
$taskFailed = @($taskEvents | Where-Object Accion -eq 'Fallo').Count
$taskResult = [pscustomobject]@{
    NombreTarea='PM05 Limpieza de temporales de laboratorio'
    IdEjecucion=[Guid]::NewGuid().ToString('N')
    FechaInicio=$taskStart.ToString('o')
    FechaFin=[DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(-5)).ToString('o')
    DuracionSegundos=[Math]::Round($taskWatch.Elapsed.TotalSeconds,6)
    Estado=$(if ($taskFailed) {'Fallo'} elseif ($WhatIfPreference) {'Simulacion'} else {'Exitoso'})
    CodigoSalida=$(if ($taskFailed) {1} else {0})
    RaizLaboratorio=$taskRoot
    DirectorioTemporal=$taskTempRoot
    LimiteUtc=$taskLimitUtc.ToString('o')
    DiasRetencion=$DiasRetencion
    Eliminados=@($taskEvents | Where-Object Accion -eq 'Eliminado').Count
    Simulacion=[bool]$WhatIfPreference
    Eventos=$taskEvents
    Alcance='Solo .tmp y .temp vencidos del nivel directo de temporales. Sin recorrer documentos, respaldos, uploads ni subcarpetas.'
}
# WhatIf impide la limpieza; el resumen se escribe de todas formas como evidencia.
$null = New-Item -ItemType Directory -Path $DirectorioResultados -Force -WhatIf:$false
$taskResultFile = Join-Path $DirectorioResultados ('limpieza-' + $taskResult.IdEjecucion + '.json')
$taskJson = $taskResult | ConvertTo-Json -Depth 6
[IO.File]::WriteAllText($taskResultFile,$taskJson,[Text.UTF8Encoding]::new($false))
Write-Output $taskJson
exit $taskResult.CodigoSalida
