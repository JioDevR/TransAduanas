[CmdletBinding()]
param([Parameter(Mandatory=$true)][ValidateSet('baseline','parche')][string]$Fase,[Parameter(Mandatory=$true)][string]$Configuracion)
$ErrorActionPreference='Stop'
$taskConfig=Get-Content -LiteralPath $Configuracion -Raw | ConvertFrom-Json
$taskOut=[string]$taskConfig.laboratorio
$taskProject=[string]$taskConfig.$Fase
if (-not $taskProject.StartsWith($taskOut,[StringComparison]::OrdinalIgnoreCase)) { throw 'Ruta fuera del laboratorio.' }
Set-Location -LiteralPath $taskProject
$env:JAVA_HOME=$taskConfig.java_home
$env:MAVEN_OPTS='-Xms64m -Xmx384m'
foreach ($taskName in @(Get-ChildItem Env: | Where-Object { $_.Name -match '^(SPRING_|DB_|JPA_|JWT_|STORAGE_|SERVER_|MONITORING_)' -or $_.Name -in @('JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS') } | Select-Object -ExpandProperty Name)) {
    Remove-Item -LiteralPath ('Env:\'+$taskName)
}
$env:LOG_PATH=Join-Path $taskProject 'target\logs-laboratorio'
$taskTestList=@($taskConfig.test_clases) -join ','
$taskTmp=Join-Path $taskProject 'target\tmp'
$taskArgs=@('-o','-B','-ntp','-s',[string]$taskConfig.settings,('-Dtest='+$taskTestList),('-DargLine=-Xms64m -Xmx384m -Djava.io.tmpdir="'+$taskTmp+'"'),'-DfailIfNoTests=true','package')
& $taskConfig.maven @taskArgs
exit $LASTEXITCODE
