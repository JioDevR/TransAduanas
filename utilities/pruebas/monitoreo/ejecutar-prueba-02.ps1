[CmdletBinding()]
param([string]$Proyecto='C:\Users\asus\Music\TransAduanas',[string]$Nginx='C:\Users\asus\Downloads\nginx-1.31.8\nginx-1.31.8')
$ErrorActionPreference='Stop'
$inicio=[DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(-5))
$marca='M02-'+$inicio.ToString('yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,6)
$salida=Join-Path $Proyecto "evidencias-monitoreo\$marca"
New-Item -ItemType Directory -Path $salida -Force|Out-Null
$criterios=@(
    'Consultas normales: tres GET de lectura con HTTP 200 y success=true.',
    'Accesos Nginx: fecha, servicio, request_id y estado HTTP; no se exige nivel de aplicacion a este tipo de log.',
    'Eventos HTTP de Java: fecha, nivel nativo de Logback, servicio e identificador de solicitud.',
    'Los identificadores de respuesta, acceso Nginx y evento Java deben coincidir para cada consulta.',
    'No se exige request_id a eventos de arranque o tareas sin una solicitud HTTP asociada.'
)
$criterios|Set-Content "$salida\criterios-previos.txt" -Encoding UTF8
$consultas=@()
for($n=1;$n -le 3;$n++){
    $uri="http://127.0.0.1:5180/api/v1/catalogos/puertos?monitoreo=$marca&consulta=$n"
    $codigo=0;$success=$false;$id='';$err=''
    try{
        $r=Invoke-WebRequest $uri -UseBasicParsing -TimeoutSec 10 -Headers @{'User-Agent'='TransAduanas-Prueba-Monitoreo-02'} -ErrorAction Stop
        $codigo=[int]$r.StatusCode;$success=[bool](($r.Content|ConvertFrom-Json).success);$id=[string]$r.Headers['X-Request-ID']
    }catch{$err=$_.Exception.Message;if($_.Exception.Response){$codigo=[int]$_.Exception.Response.StatusCode}}
    $consultas+=[pscustomobject]@{Consulta=$n;Fecha=[DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(-5)).ToString('o');HTTP=$codigo;SuccessJSON=$success;RequestIdRespuesta=$id;URL=$uri;Error=$err}
    Write-Output "Consulta $n/3: HTTP $codigo; success=$success; request_id=$id"
    if($n -lt 3){Start-Sleep -Seconds 1}
}
Start-Sleep -Milliseconds 700
$consultas|Export-Csv "$salida\consultas.csv" -NoTypeInformation -Encoding UTF8
$eventos=@(Get-Content (Join-Path $Nginx 'logs\access.log') -Encoding UTF8|Where-Object{$_ -like "*$marca*"})
$eventos|Set-Content "$salida\eventos-nginx.log" -Encoding UTF8
$logJava="$env:USERPROFILE\Music\TransAduanas-desplegado\logs\sga-backend.log"
$java=@(Get-Content $logJava -Encoding UTF8 -Tail 1000)
$verificacion=@();$javaDePrueba=@()
foreach($c in $consultas){
    $id=$c.RequestIdRespuesta
    $acceso=@();$backend=@()
    if($id -match '^[a-f0-9]{32}$'){
        $acceso=@($eventos|Where-Object{$_ -like "*request_id=`"$id`"*"})
        $backend=@($java|Where-Object{$_ -like "*request_id=$id *request_completed *"})
    }
    $lineaN=$acceso -join "`n";$lineaJ=$backend -join "`n"
    $fechaN=$lineaN -match '\[\d{2}/[A-Za-z]{3}/\d{4}:\d{2}:\d{2}:\d{2} [+-]\d{4}\]'
    $fechaJ=$lineaJ -match '^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3} '
    $nivel='';if($lineaJ -match '\s(TRACE|DEBUG|INFO|WARN|ERROR)\s+service='){$nivel=$matches[1]}
    $okN=$acceso.Count -eq 1 -and $fechaN -and $lineaN -match '\bservice="sga-backend"' -and $lineaN -match '" 200 '
    $okJ=$backend.Count -eq 1 -and $fechaJ -and $nivel -eq 'INFO' -and $lineaJ -match '\bservice=sga-backend\b' -and $lineaJ -match 'endpoint=/api/v1/catalogos/puertos status=200 '
    $verificacion+=[pscustomobject]@{Consulta=$c.Consulta;RequestId=$id;LogNginxValido=[bool]$okN;EventosJava=$backend.Count;FechaJavaValida=$fechaJ;NivelJava=$nivel;LogJavaValido=[bool]$okJ;CorrelacionCorrecta=([bool]$okN -and [bool]$okJ)}
    $javaDePrueba+=$backend
}
$javaDePrueba|Set-Content "$salida\eventos-backend.log" -Encoding UTF8
$verificacion|Export-Csv "$salida\campos-verificados.csv" -NoTypeInformation -Encoding UTF8
$correctas=@($verificacion|Where-Object{$_.CorrelacionCorrecta}).Count
$unicos=@($consultas|Select-Object -ExpandProperty RequestIdRespuesta -Unique).Count
$http=@($consultas|Where-Object{$_.HTTP -eq 200 -and $_.SuccessJSON}).Count
$ok=$http -eq 3 -and $eventos.Count -eq 3 -and $correctas -eq 3 -and $unicos -eq 3
$estado=if($ok){'EXITOSO'}else{'FALLO'}
$razon=if($ok){'Tres consultas normales HTTP 200. Tres eventos Java con fecha, nivel INFO nativo, servicio y request_id; coinciden con los tres accesos Nginx y las respuestas.'}else{'No se cumplieron todos los criterios de estructura y correlacion. Ver campos-verificados.csv; no se provocaron fallos artificiales.'}
$resultado=[pscustomobject]@{
    Prueba=2;CodigoPrueba='M02';Nombre='Estructura de los registros';Importancia='Baja';Identificador=$marca;
    InicioLima=$inicio.ToString('o');FinLima=[DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(-5)).ToString('o');
    Ejecucion='REALIZADA: tres GET normales de lectura, secuenciales; criterios escritos antes de las consultas';
    Herramientas=@('Windows PowerShell','Invoke-WebRequest','Nginx access.log','Logback sga-backend.log','MDC y filtro HTTP de Spring');
    Consultas=3;HTTP200=$http;EventosNginx=$eventos.Count;EventosJava=$javaDePrueba.Count;CorrelacionesCorrectas=$correctas;RequestIdUnicos=$unicos;
    Estado=$estado;Resultado=$razon;Criterios=$criterios;VersionCriterio=2;
    Incidencia=if($ok){''}else{'Campos o correlacion incompletos en la evidencia nueva. Consultar los detalles de cada consulta.'};
    Alcance='Estructura de eventos HTTP de acceso Nginx y aplicacion Java. Correlacion local comprobada; no acredita ingesta centralizada ni trazas distribuidas.';
    RevisionMetodologica='Un access.log no necesita un nivel INFO/ERROR propio de aplicacion. La evaluacion inicial de Nginx fue demasiado estricta; se corrigio y se completo la instrumentacion real del backend.'
}
$resultado|ConvertTo-Json -Depth 5|Set-Content "$salida\resultado.json" -Encoding UTF8
@('PRUEBA 02 - ESTRUCTURA DE LOS REGISTROS',"ESTADO: $estado","RESULTADO: $razon","Consultas HTTP 200: $http/3","Eventos de acceso Nginx: $($eventos.Count)/3","Eventos Java: $($javaDePrueba.Count)/3","Correlaciones correctas: $correctas/3","IDs unicos: $unicos/3",'El nivel INFO comprobado es emitido por Logback; no se invento un nivel para Nginx.',"ALCANCE: $($resultado.Alcance)","REVISION: $($resultado.RevisionMetodologica)",'Las pruebas 03 a 15 siguen pendientes.')|Set-Content "$salida\resultado.txt" -Encoding UTF8
Copy-Item -LiteralPath $PSCommandPath -Destination "$salida\ejecutar-prueba-02.ps1"
Write-Output "Estado: $estado";Write-Output "Resultado: $razon";Write-Output "Evidencias: $salida"
