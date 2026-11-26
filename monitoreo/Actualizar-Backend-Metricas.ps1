$ErrorActionPreference='Stop'
$proyecto=Split-Path $PSScriptRoot -Parent
$jar=Join-Path $proyecto 'backend-sga\target\sga-backend-0.5.4.jar'
$kit=Join-Path $proyecto 'docs\pruebas\despliegue\kit-11-15\Gestionar-Despliegue.ps1'
$raiz="$env:USERPROFILE\Music\TransAduanas-desplegado"
$e=Get-ChildItem (Join-Path $proyecto 'evidencias-monitoreo') -Directory -Filter 'integracion-prometheus-grafana-*'|Sort-Object Name -Descending|Select-Object -First 1
$salida=$e.FullName
$old=Get-Content "$raiz\estado.json" -Raw|ConvertFrom-Json
$hash=(Get-FileHash $jar).Hash
Start-Transcript -LiteralPath "$salida\aplicacion.log"|Out-Null
try{
    if($old.Actual.Version -ne '0.5.3'){throw 'Se esperaba 0.5.3 antes de aplicar la integracion.'}
    Copy-Item "$raiz\estado.json" "$salida\estado-antes.json"
    & $kit -Accion Desplegar -Jar $jar -Version '0.5.4'
    $r=Invoke-WebRequest http://127.0.0.1:9086/actuator/prometheus -UseBasicParsing -TimeoutSec 10
    if($r.StatusCode -ne 200 -or $r.Content -notmatch 'jvm_memory_used_bytes'){throw 'No se confirmaron metricas reales en el puerto separado.'}
    $business=Invoke-WebRequest http://127.0.0.1:5180/api/v1/catalogos/puertos -UseBasicParsing -TimeoutSec 10
    if($business.StatusCode -ne 200){throw 'El proxy de negocio no responde.'}
    if((Get-FileHash "$raiz\current\app.jar").Hash -ne $hash){throw 'El JAR activo no coincide.'}
    Copy-Item "$raiz\estado.json" "$salida\estado-despues.json"
    "CORRECTO: 0.5.4 activa; metricas JVM reales en 127.0.0.1:9086; negocio HTTP 200; anterior 0.5.3 conservada. SHA256=$hash"|Set-Content "$salida\aplicacion-resultado.txt" -Encoding UTF8
}catch{
    $errorInicial=$_
    $actual=Get-Content "$raiz\estado.json" -Raw|ConvertFrom-Json
    if($actual.Actual.Hash -eq $hash -and $actual.Anterior.Hash -eq $old.Actual.Hash){
        try{& $kit -Accion Revertir}catch{Write-Warning 'No se confirmo la recuperacion. Revisar servicio y logs.'}
    }
    ('FALLIDO: '+$errorInicial.Exception.Message)|Set-Content "$salida\aplicacion-resultado.txt" -Encoding UTF8
    throw $errorInicial
}finally{Stop-Transcript|Out-Null}
