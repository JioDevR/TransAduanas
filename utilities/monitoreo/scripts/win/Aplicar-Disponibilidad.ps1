$ErrorActionPreference='Stop'
$proyecto=Split-Path $PSScriptRoot -Parent
$herramientas="$env:USERPROFILE\Music\TransAduanas-herramientas\monitoreo"
$manifest=Get-Content "$herramientas\versiones.json" -Raw|ConvertFrom-Json
$prom=($manifest|Where-Object nombre -eq 'prometheus').ejecutable
$promtool=Join-Path (Split-Path $prom) 'promtool.exe'
$config=Join-Path $PSScriptRoot 'prometheus\prometheus.yml'
$evidencia=Join-Path $proyecto 'evidencias-monitoreo\integracion-disponibilidad-M11'
& $promtool check config $config *> "$evidencia\validacion-configuracion.txt"
if($LASTEXITCODE -ne 0){throw 'La configuracion Prometheus no paso su validacion.'}
# Start the new exporter before restarting only the owned Prometheus process.
& (Join-Path $PSScriptRoot 'Iniciar-Monitoreo.ps1')
$state=Get-Content "$herramientas\procesos.json" -Raw|ConvertFrom-Json
$entry=$state|Where-Object nombre -eq 'prometheus'
$running=Get-CimInstance Win32_Process -Filter "ProcessId=$($entry.processId)"
if($running.ExecutablePath -ne $prom -or $running.CommandLine -notlike "*$config*"){throw 'El proceso Prometheus no coincide. No se detuvo.'}
Stop-Process -Id $entry.processId -ErrorAction Stop
Wait-Process -Id $entry.processId -Timeout 15 -ErrorAction SilentlyContinue
& (Join-Path $PSScriptRoot 'Iniciar-Monitoreo.ps1')
'Aplicados sondeos HTTP con Blackbox; backend y Nginx habituales sin reinicio.' | Set-Content "$evidencia\aplicacion.txt" -Encoding utf8
