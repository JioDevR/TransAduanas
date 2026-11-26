[CmdletBinding()]
param([switch]$Plan)
$ErrorActionPreference='Stop'
$taskScript=Join-Path $PSScriptRoot 'adicionales\PA01\ejecutar_PA01.py'
$taskPython=Get-Command python.exe -ErrorAction SilentlyContinue
$taskExe=if($taskPython){$taskPython.Source}else{Join-Path $env:LOCALAPPDATA 'Programs\Python\Python314\python.exe'}
if(-not(Test-Path -LiteralPath $taskExe)){throw 'Se requiere Python 3 disponible.'}
if($Plan){[pscustomobject]@{Prueba='PA01';Ejecutor=$taskScript;Interprete=$taskExe;Ejecutado=$false}|ConvertTo-Json -Compress;exit 0}
& $taskExe $taskScript
exit $LASTEXITCODE
