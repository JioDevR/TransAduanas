$ErrorActionPreference='Stop'
$proyecto=Split-Path $PSScriptRoot -Parent
$jar=Join-Path $proyecto 'backend-sga\target\sga-backend-0.5.5.jar'
$kit=Join-Path $proyecto 'docs\pruebas\despliegue\kit-11-15\Gestionar-Despliegue.ps1'
$raiz="$env:USERPROFILE\Music\TransAduanas-desplegado"
$salida=Join-Path $proyecto 'evidencias-monitoreo\correccion-M07-0.5.5'
New-Item -ItemType Directory -Path $salida -Force | Out-Null
$identidad=[Security.Principal.WindowsIdentity]::GetCurrent()
if(-not ([Security.Principal.WindowsPrincipal]::new($identidad)).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)){
    throw 'Se requiere PowerShell como administrador para actualizar el servicio Windows.'
}
$old=Get-Content "$raiz\estado.json" -Raw|ConvertFrom-Json
$hash=(Get-FileHash -LiteralPath $jar).Hash
Start-Transcript -LiteralPath "$salida\aplicacion.log" -Append | Out-Null
try {
    if($old.Actual.Version -eq '0.5.5' -and $old.Actual.Hash -eq $hash){
        Write-Output 'La correccion 0.5.5 ya esta desplegada; no se reinicia el servicio.'
    }else{
        if($old.Actual.Version -ne '0.5.4'){throw 'Se esperaba 0.5.4 antes de desplegar esta correccion.'}
        if(-not(Test-Path "$salida\estado-antes.json")){Copy-Item "$raiz\estado.json" "$salida\estado-antes.json"}
        & $kit -Accion Desplegar -Jar $jar -Version '0.5.5'
    }
    if((Get-FileHash "$raiz\current\app.jar").Hash -ne $hash){throw 'El JAR activo no coincide.'}
    $metricas=Invoke-WebRequest http://127.0.0.1:9086/actuator/prometheus -UseBasicParsing -TimeoutSec 10
    if($metricas.StatusCode -ne 200 -or $metricas.Content -notmatch 'hikaricp_connections'){throw 'No se confirmaron las metricas del pool JDBC.'}
    foreach($url in @('http://127.0.0.1:5173/','http://127.0.0.1:5174/','http://127.0.0.1:5180/','http://127.0.0.1:5180/api/v1/catalogos/puertos')){
        $respuesta=Invoke-WebRequest $url -UseBasicParsing -TimeoutSec 10
        if($respuesta.StatusCode -ne 200){throw "Fallo la comprobacion HTTP de $url"}
    }
    Copy-Item "$raiz\estado.json" "$salida\estado-despues.json" -Force
    "DESPLEGADO: 0.5.5; SHA256=$hash; health UP, tres interfaces y catalogo HTTP 200, metricas Hikari disponibles. Falta repetir M07 para cerrar la incidencia." | Set-Content "$salida\aplicacion-resultado.txt" -Encoding utf8
    Get-Content "$salida\aplicacion-resultado.txt"
}catch{
    $falloInicial=$_
    $actual=Get-Content "$raiz\estado.json" -Raw|ConvertFrom-Json
    if($old.Actual.Hash -ne $hash -and $actual.Actual.Hash -eq $hash -and $actual.Anterior.Hash -eq $old.Actual.Hash){
        try{& $kit -Accion Revertir}catch{Write-Warning 'No se confirmo la recuperacion. Revisar servicio y logs.'}
    }
    ('NO CONFIRMADO: '+$falloInicial.Exception.Message) | Set-Content "$salida\aplicacion-resultado.txt" -Encoding utf8
    throw $falloInicial
}finally{Stop-Transcript | Out-Null}
