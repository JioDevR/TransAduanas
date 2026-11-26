[CmdletBinding()]
param([string]$Herramientas="$env:USERPROFILE\Music\TransAduanas-herramientas\monitoreo")
$ErrorActionPreference='Stop'
$manifest=Get-Content "$Herramientas\versiones.json" -Raw|ConvertFrom-Json
$logs=Join-Path $Herramientas 'datos\logs'
New-Item -ItemType Directory -Path $logs -Force|Out-Null
$previousLimit=$env:GOMEMLIMIT
$previousGC=$env:GOGC
function Iniciar-LogProceso($Nombre,$Puerto,$Config,$Argumentos){
    $exe=($manifest|Where-Object nombre -eq $Nombre).ejecutable
    if(-not $exe -or -not(Test-Path -LiteralPath $exe)){throw "Falta el binario de $Nombre"}
    $puertoActual=@(Get-NetTCPConnection -LocalPort $Puerto -State Listen -ErrorAction SilentlyContinue)
    if($puertoActual.Count){
        $p=Get-CimInstance Win32_Process -Filter "ProcessId=$($puertoActual[0].OwningProcess)"
        if($p.ExecutablePath -ne $exe -or $p.CommandLine -notlike "*$Config*"){throw "Puerto $Puerto ocupado por otro proceso."}
        return [pscustomobject]@{nombre=$Nombre;processId=$p.ProcessId;ejecutable=$exe;puerto=$Puerto;config=$Config}
    }
    $proc=Start-Process -FilePath $exe -ArgumentList $Argumentos -WorkingDirectory (Split-Path $exe) -WindowStyle Hidden -PassThru -RedirectStandardOutput "$logs\$Nombre-out.log" -RedirectStandardError "$logs\$Nombre-error.log"
    return [pscustomobject]@{nombre=$Nombre;processId=$proc.Id;ejecutable=$exe;puerto=$Puerto;config=$Config}
}
try{
    $env:GOMEMLIMIT='128MiB';$env:GOGC='60'
    $lconfig=Join-Path $PSScriptRoot 'loki\loki.yml'
    $entries=@(Iniciar-LogProceso 'loki' 3100 $lconfig @("-config.file=`"$lconfig`""))
    $env:GOMEMLIMIT='96MiB'
    $aconfig=Join-Path $PSScriptRoot 'alloy\config.alloy'
    $entries+=Iniciar-LogProceso 'alloy' 12345 $aconfig @('run',"`"$aconfig`"",'--server.http.listen-addr=127.0.0.1:12345',"--storage.path=`"$(Join-Path $Herramientas 'datos\alloy')`"",'--disable-reporting')
    $entries|ConvertTo-Json|Set-Content "$Herramientas\procesos-logs.json" -Encoding UTF8
}finally{$env:GOMEMLIMIT=$previousLimit;$env:GOGC=$previousGC}
foreach($url in @('http://127.0.0.1:3100/ready','http://127.0.0.1:12345/-/ready')){
    $ok=$false
    for($i=0;$i -lt 60;$i++){
        try{if((Invoke-WebRequest $url -UseBasicParsing -TimeoutSec 2).StatusCode -eq 200){$ok=$true;break}}catch{}
        Start-Sleep -Seconds 1
    }
    if(-not $ok){throw "No se confirmo $url; revisar logs de Loki/Alloy."}
}
Write-Output 'Loki disponible en 127.0.0.1:3100; Alloy en 127.0.0.1:12345.'
