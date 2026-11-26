$ErrorActionPreference='Stop'
$tools="$env:USERPROFILE\Music\TransAduanas-herramientas\monitoreo"
$path=Join-Path $tools 'procesos-logs.json'
if(-not(Test-Path $path)){return}
foreach($entry in (Get-Content $path -Raw|ConvertFrom-Json)){
    $p=Get-CimInstance Win32_Process -Filter "ProcessId=$($entry.processId)"
    if(-not $p){continue}
    if($p.ExecutablePath -ne $entry.ejecutable -or $p.CommandLine -notlike "*$($entry.config)*"){throw 'El proceso de logs no coincide; no se detuvo.'}
    Stop-Process -Id $entry.processId -ErrorAction Stop
    Write-Output "Detenido $($entry.nombre). Datos y logs conservados."
}
