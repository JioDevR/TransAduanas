[CmdletBinding(SupportsShouldProcess=$true)]
param(
    [Parameter(Mandatory=$true)][string]$CarpetaLaboratorio,
    [Parameter(Mandatory=$true)][string]$DirectorioResultados,
    [ValidateRange(1,3650)][int]$DiasRetencion=7
)
$ErrorActionPreference='Stop'
$taskWatch=[Diagnostics.Stopwatch]::StartNew()
$taskStart=[DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(-5))
$taskRootItem=Get-Item -LiteralPath $CarpetaLaboratorio
if (-not $taskRootItem.PSIsContainer -or ($taskRootItem.Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw 'Se requiere carpeta ordinaria de laboratorio.' }
$taskRoot=$taskRootItem.FullName.TrimEnd([IO.Path]::DirectorySeparatorChar)
$taskMarker=Get-Content -LiteralPath (Join-Path $taskRoot 'laboratorio.json') -Raw | ConvertFrom-Json
if ($taskMarker.Prueba -ne 'PM10' -or $taskMarker.Raiz -ne $taskRoot) { throw 'Marca PM10 incorrecta.' }
$taskCopiesItem=Get-Item -LiteralPath (Join-Path $taskRoot 'copias')
if (-not $taskCopiesItem.PSIsContainer -or ($taskCopiesItem.Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw 'La carpeta copias debe ser ordinaria.' }
$taskCopies=$taskCopiesItem.FullName.TrimEnd([IO.Path]::DirectorySeparatorChar)
if ([IO.Directory]::GetParent($taskCopies).FullName -ne $taskRoot) { throw 'Copias fuera del laboratorio.' }
$taskManifest=Get-Content -LiteralPath (Join-Path $taskRoot 'inventario_respaldos.json') -Raw | ConvertFrom-Json
$taskAvailable=@();$taskEvents=@();$taskSeen=@{}
Add-Type -AssemblyName System.IO.Compression.FileSystem
# Validar todos los candidatos antes de eliminar el primero.
foreach ($taskCopy in $taskManifest.Copias) {
    $taskName=[string]$taskCopy.Archivo
    if ([IO.Path]::GetFileName($taskName) -ne $taskName -or [IO.Path]::GetExtension($taskName) -ne '.zip' -or $taskSeen.ContainsKey($taskName)) { throw 'Inventario ambiguo o nombre fuera del alcance.' }
    $taskSeen[$taskName]=$true
    $taskPath=[IO.Path]::GetFullPath((Join-Path $taskCopies $taskName))
    if ([IO.Path]::GetDirectoryName($taskPath) -ne $taskCopies) { throw 'Copia fuera del directorio validado.' }
    if (-not (Test-Path -LiteralPath $taskPath -PathType Leaf)) {
        $taskEvents += [pscustomobject]@{Archivo=$taskName;Accion='Ausente';Motivo='No existe; puede corresponder a una depuracion anterior'}
        continue
    }
    $taskFile=Get-Item -LiteralPath $taskPath
    if ($taskFile.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'No se procesan enlaces.' }
    $taskHasher=[Security.Cryptography.SHA256]::Create()
    $taskInput=[IO.File]::OpenRead($taskPath)
    try { $taskHash=([BitConverter]::ToString($taskHasher.ComputeHash($taskInput))).Replace('-','') }
    finally { $taskInput.Dispose(); $taskHasher.Dispose() }
    if ($taskHash -ne $taskCopy.SHA256) { throw 'Integridad no confirmada: no se inicia la depuracion.' }
    $taskZip=$null
    try {
        $taskZip=[IO.Compression.ZipFile]::OpenRead($taskPath)
        if ($taskZip.Entries.Count -eq 0) { throw 'ZIP vacio.' }
        foreach ($taskEntry in $taskZip.Entries) {
            $taskStream=$taskEntry.Open()
            try { $taskStream.CopyTo([IO.Stream]::Null) } finally { $taskStream.Dispose() }
        }
    } finally { if ($null -ne $taskZip) { $taskZip.Dispose() } }
    $taskDate=[DateTimeOffset]::Parse([string]$taskCopy.FechaUTC).ToUniversalTime()
    $taskAvailable += [pscustomobject]@{Archivo=$taskName;Ruta=$taskPath;FechaUTC=$taskDate;SHA256=$taskHash}
}
if ($taskAvailable.Count -eq 0) { throw 'No hay una copia integra que pueda protegerse; no se depura.' }
$taskNewest=$taskAvailable | Sort-Object @{Expression='FechaUTC';Descending=$true},Archivo | Select-Object -First 1
$taskLimit=[DateTimeOffset]::UtcNow.AddDays(-$DiasRetencion)
foreach ($taskCopy in $taskAvailable) {
    if ($taskCopy.Archivo -eq $taskNewest.Archivo) {
        $taskEvents += [pscustomobject]@{Archivo=$taskCopy.Archivo;Accion='Conservada';Motivo='Ultima copia integra protegida'}
    } elseif ($taskCopy.FechaUTC -ge $taskLimit) {
        $taskEvents += [pscustomobject]@{Archivo=$taskCopy.Archivo;Accion='Conservada';Motivo='Copia vigente'}
    } elseif ($PSCmdlet.ShouldProcess($taskCopy.Ruta,'Eliminar copia vencida de laboratorio')) {
        # Solo archivos inventariados, íntegros y vencidos; nunca se eliminan carpetas.
        [IO.File]::Delete($taskCopy.Ruta)
        $taskEvents += [pscustomobject]@{Archivo=$taskCopy.Archivo;Accion='Eliminada';Motivo='Vencida y no es la ultima integra'}
    } else {
        $taskEvents += [pscustomobject]@{Archivo=$taskCopy.Archivo;Accion='No eliminada';Motivo='Simulacion o accion no confirmada'}
    }
}
$taskWatch.Stop()
$taskSummary=[pscustomobject]@{
    NombreTarea='PM10 Retencion de respaldos de laboratorio'
    IdEjecucion=[Guid]::NewGuid().ToString('N')
    FechaInicio=$taskStart.ToString('o')
    FechaFin=[DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(-5)).ToString('o')
    DuracionSegundos=$taskWatch.Elapsed.TotalSeconds
    Estado=$(if ($WhatIfPreference) {'Simulacion'} else {'Exitoso'})
    CodigoSalida=0
    Simulacion=[bool]$WhatIfPreference
    RaizLaboratorio=$taskRoot
    DiasRetencion=$DiasRetencion
    LimiteUTC=$taskLimit.ToString('o')
    UltimaCopiaProtegida=$taskNewest.Archivo
    Eliminadas=@($taskEvents | Where-Object Accion -eq 'Eliminada').Count
    Conservadas=@($taskEvents | Where-Object Accion -eq 'Conservada').Count
    Eventos=$taskEvents
    Alcance='Integridad por SHA256 y lectura ZIP. No acredita restauracion de MySQL, cifrado ni copias externas. No se procesan archivos ajenos al inventario.'
}
$null=New-Item -ItemType Directory -Path $DirectorioResultados -Force -WhatIf:$false
$taskJson=$taskSummary | ConvertTo-Json -Depth 6
[IO.File]::WriteAllText((Join-Path $DirectorioResultados ('retencion-'+$taskSummary.IdEjecucion+'.json')),$taskJson,[Text.UTF8Encoding]::new($false))
Write-Output $taskJson
exit 0
