[CmdletBinding()]
param([string]$Herramientas="$env:USERPROFILE\Music\TransAduanas-herramientas\monitoreo")
$ErrorActionPreference='Stop'
$proyecto=Split-Path $PSScriptRoot -Parent
$manifest=Get-Content (Join-Path $Herramientas 'versiones.json') -Raw|ConvertFrom-Json
$prom=($manifest|Where-Object nombre -eq 'prometheus').ejecutable
$grafana=($manifest|Where-Object nombre -eq 'grafana').ejecutable
$datos=Join-Path $Herramientas 'datos'
$logs=Join-Path $datos 'logs'
New-Item -ItemType Directory -Path $logs,(Join-Path $datos 'prometheus'),(Join-Path $datos 'grafana') -Force|Out-Null
$secretPath=Join-Path $Herramientas 'credenciales-locales.json'
if(-not(Test-Path $secretPath)){
    $random=New-Object byte[] 48;$rng=[Security.Cryptography.RandomNumberGenerator]::Create()
    try{$rng.GetBytes($random)}finally{$rng.Dispose()}
    $private=[pscustomobject]@{usuario='admin';password=([Guid]::NewGuid().ToString('N')+[Guid]::NewGuid().ToString('N'));secret_key=[Convert]::ToBase64String($random)}
    $private|ConvertTo-Json|Set-Content $secretPath -Encoding UTF8
    $acl=Get-Acl $secretPath;$acl.SetAccessRuleProtection($true,$false)
    foreach($sid in @([Security.Principal.WindowsIdentity]::GetCurrent().User,[Security.Principal.SecurityIdentifier]::new('S-1-5-18'),[Security.Principal.SecurityIdentifier]::new('S-1-5-32-544'))){
        $acl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new($sid,'FullControl','Allow'))
    }
    Set-Acl $secretPath $acl
}
$private=Get-Content $secretPath -Raw|ConvertFrom-Json
function Iniciar-Proceso {
    param([string]$Nombre,[string]$Exe,[string[]]$Argumentos,[int]$Puerto,[string]$Config)
    Get-Item -LiteralPath $Exe -ErrorAction Stop|Out-Null
    $ocupado=@(Get-NetTCPConnection -LocalPort $Puerto -State Listen -ErrorAction SilentlyContinue)
    if($ocupado.Count){
        $ownerId=$ocupado[0].OwningProcess
        $existing=Get-CimInstance Win32_Process -Filter "ProcessId=$ownerId"
        if($existing.ExecutablePath -ne $Exe -or $existing.CommandLine -notlike "*$Config*"){throw "Puerto $Puerto ocupado por otro proceso. No se detuvo."}
        return [pscustomobject]@{nombre=$Nombre;processId=$ownerId;ejecutable=$Exe;puerto=$Puerto}
    }
    $p=Start-Process -FilePath $Exe -ArgumentList $Argumentos -WorkingDirectory (Split-Path $Exe) -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $logs "$Nombre-out.log") -RedirectStandardError (Join-Path $logs "$Nombre-error.log")
    return [pscustomobject]@{nombre=$Nombre;processId=$p.Id;ejecutable=$Exe;puerto=$Puerto}
}
$pconfig=Join-Path $PSScriptRoot 'prometheus\prometheus.yml'
$gconfig=Join-Path $PSScriptRoot 'grafana\grafana.ini'
$procesos=@()
$blackbox=($manifest|Where-Object nombre -eq 'blackbox').ejecutable
if(-not $blackbox){throw 'Falta Blackbox Exporter en versiones.json. Ejecutar instalar_herramientas.py.'}
$bconfig=Join-Path $PSScriptRoot 'blackbox\blackbox.yml'
$procesos+=Iniciar-Proceso -Nombre blackbox -Exe $blackbox -Puerto 9115 -Config $bconfig -Argumentos @("--config.file=`"$bconfig`"",'--web.listen-address=127.0.0.1:9115')
$procesos+=Iniciar-Proceso -Nombre prometheus -Exe $prom -Puerto 9090 -Config $pconfig -Argumentos @("--config.file=`"$pconfig`"","--storage.tsdb.path=`"$(Join-Path $datos 'prometheus')`"",'--storage.tsdb.retention.time=2d','--storage.tsdb.retention.size=256MB','--web.listen-address=127.0.0.1:9090')
$envNames=@('MONITORING_GRAFANA_DATA','MONITORING_GRAFANA_LOGS','MONITORING_GRAFANA_PROVISIONING','MONITORING_DASHBOARDS_DIR','MONITORING_HOME_DASHBOARD','MONITORING_GRAFANA_ADMIN_PASSWORD','MONITORING_GRAFANA_SECRET_KEY')
$previous=@{};foreach($name in $envNames){$previous[$name]=[Environment]::GetEnvironmentVariable($name,'Process')}
try{
    $env:MONITORING_GRAFANA_DATA=(Join-Path $datos 'grafana').Replace('\','/')
    $env:MONITORING_GRAFANA_LOGS=$logs.Replace('\','/')
    $env:MONITORING_GRAFANA_PROVISIONING=(Join-Path $PSScriptRoot 'grafana\provisioning').Replace('\','/')
    $env:MONITORING_DASHBOARDS_DIR=(Join-Path $PSScriptRoot 'grafana\dashboards').Replace('\','/')
    $env:MONITORING_HOME_DASHBOARD=(Join-Path $PSScriptRoot 'grafana\dashboards\transaduanas.json').Replace('\','/')
    $env:MONITORING_GRAFANA_ADMIN_PASSWORD=$private.password
    $env:MONITORING_GRAFANA_SECRET_KEY=$private.secret_key
    $grafanaHome=Split-Path (Split-Path $grafana)
    $arguments=@('server',"--homepath=`"$grafanaHome`"","--config=`"$gconfig`"")
    if((Split-Path $grafana -Leaf) -eq 'grafana-server.exe'){$arguments=$arguments[1..($arguments.Length-1)]}
    $procesos+=Iniciar-Proceso -Nombre grafana -Exe $grafana -Puerto 3000 -Config $gconfig -Argumentos $arguments
}finally{
    foreach($name in $envNames){[Environment]::SetEnvironmentVariable($name,$previous[$name],'Process')}
    $private=$null
}
$procesos|ConvertTo-Json|Set-Content (Join-Path $Herramientas 'procesos.json') -Encoding UTF8
foreach($url in 'http://127.0.0.1:9090/-/ready','http://127.0.0.1:3000/api/health'){
    $ok=$false
    for($i=0;$i -lt 45;$i++){
        try{if((Invoke-WebRequest $url -UseBasicParsing -TimeoutSec 2).StatusCode -eq 200){$ok=$true;break}}catch{}
        Start-Sleep -Seconds 1
    }
    if(-not $ok){throw "No se confirmo el arranque: $url. Consultar logs en $logs"}
}
Write-Output 'Prometheus: http://localhost:9090'
Write-Output 'Grafana: http://localhost:3000/d/transaduanas-monitoreo'
Write-Output 'Lectura del panel: acceso local como Viewer. Credencial administrativa privada conservada fuera del proyecto.'


& (Join-Path $PSScriptRoot "Iniciar-Logs.ps1")
