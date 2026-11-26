[CmdletBinding()]
param([Parameter(Mandatory=$true,Position=0)][ValidateRange(1,15)][int]$Numero,[switch]$Plan)
$ErrorActionPreference='Stop'
$taskScript=Join-Path $PSScriptRoot ('ejecutar_PM{0:00}_informe.{1}' -f $Numero,$(if($Numero -eq 7){'ps1'}else{'py'}))
if(-not(Test-Path -LiteralPath $taskScript)){throw 'No se encontro el ejecutor de la prueba.'}
if($Numero -eq 7){
    $taskExe=Join-Path $env:WINDIR 'System32\WindowsPowerShell\v1.0\powershell.exe'
}else{
    $taskCandidates=@()
    if($env:TRANSADUANAS_PYTHON){$taskCandidates+=$env:TRANSADUANAS_PYTHON}
    $taskPythonCommand=Get-Command python.exe -ErrorAction SilentlyContinue
    if($taskPythonCommand){$taskCandidates+= $taskPythonCommand.Source}
    $taskCandidates+=Join-Path $env:LOCALAPPDATA 'Programs\Python\Python314\python.exe'
    $taskExe=$null
    foreach($taskCandidate in ($taskCandidates|Select-Object -Unique)){
        if(-not(Test-Path -LiteralPath $taskCandidate)){continue}
        $taskCheck=if($Numero -in @(11,12)){'import cryptography'}else{'import sys'}
        $taskPrevious=$ErrorActionPreference;$ErrorActionPreference='Continue'
        try{& $taskCandidate -c $taskCheck 2>$null; $taskCode=$LASTEXITCODE}finally{$ErrorActionPreference=$taskPrevious}
        if($taskCode -eq 0){$taskExe=$taskCandidate;break}
    }
    if(-not $taskExe){throw 'No se encontro Python compatible. Las pruebas 11 y 12 requieren cryptography.'}
}
if($Plan){[pscustomobject]@{Prueba=$Numero;Ejecutor=$taskScript;Interprete=$taskExe;Ejecutado=$false}|ConvertTo-Json -Compress;exit 0}
if($Numero -eq 7){& $taskExe -NoProfile -ExecutionPolicy Bypass -File $taskScript}else{& $taskExe $taskScript}
exit $LASTEXITCODE
