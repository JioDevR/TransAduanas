[CmdletBinding()]
param([string]$Herramientas="$env:USERPROFILE\Music\TransAduanas-herramientas\monitoreo")
$ErrorActionPreference='Stop'
$state=Join-Path $Herramientas 'procesos.json'
if(-not(Test-Path $state)){throw 'No hay registro de procesos del stack.'}
foreach($entry in (Get-Content $state -Raw|ConvertFrom-Json)){
    $p=Get-CimInstance Win32_Process -Filter "ProcessId=$($entry.processId)"
    if(-not $p){continue}
    if($p.ExecutablePath -ne $entry.ejecutable){throw "El PID $($entry.processId) corresponde a otro ejecutable; no se detuvo."}
    Stop-Process -Id $entry.processId -ErrorAction Stop
    Write-Output "Detenido: $($entry.nombre)"
}
Write-Output 'El backend y sus datos no fueron detenidos ni eliminados.'

& (Join-Path $PSScriptRoot "Detener-Logs.ps1")
