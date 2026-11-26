$ErrorActionPreference='Stop'
$proyecto=Split-Path $PSScriptRoot -Parent
$python="$env:USERPROFILE\AppData\Local\Programs\Python\Python314\python.exe"
$script=Join-Path $proyecto 'docs\pruebas\monitoreo\ejecutar_prueba_09.py'
$salida=Join-Path $proyecto 'monitoreo\M09-ejecucion.log'
& $python -u $script *> $salida
exit $LASTEXITCODE
