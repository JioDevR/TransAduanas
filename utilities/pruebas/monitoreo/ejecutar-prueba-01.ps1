[CmdletBinding()]
param(
    [string]$Proyecto='C:\Users\asus\Music\TransAduanas',
    [string]$Nginx='C:\Users\asus\Downloads\nginx-1.31.8\nginx-1.31.8'
)
$ErrorActionPreference='Stop'
$marca='M01-'+[DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(-5)).ToString('yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,6)
$salida=Join-Path $Proyecto "evidencias-monitoreo\$marca"
New-Item -ItemType Directory -Path $salida -Force | Out-Null
$acceso=Join-Path $Nginx 'logs\access.log'
$backend="$env:USERPROFILE\Music\TransAduanas-desplegado\logs\sga-backend.log"
Get-Item -LiteralPath $acceso -ErrorAction Stop | Out-Null
$inicio=[DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(-5))
$filas=@()
for($n=1;$n -le 10;$n++) {
    $uri="http://127.0.0.1:5180/api/v1/catalogos/puertos?monitoreo=$marca&consulta=$n"
    $reloj=[Diagnostics.Stopwatch]::StartNew()
    $codigo=0;$exito=$false;$errorHttp=''
    try {
        $r=Invoke-WebRequest -Uri $uri -Method Get -UseBasicParsing -TimeoutSec 10 -Headers @{'User-Agent'='TransAduanas-Prueba-Monitoreo-01'} -ErrorAction Stop
        $codigo=[int]$r.StatusCode
        $exito=[bool](($r.Content | ConvertFrom-Json).success)
    } catch {
        $errorHttp=$_.Exception.Message
        if($_.Exception.Response){$codigo=[int]$_.Exception.Response.StatusCode}
    }
    $reloj.Stop()
    $filas += [pscustomobject]@{Consulta=$n;Fecha=[DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(-5)).ToString('o');URL=$uri;HTTP=$codigo;SuccessJSON=$exito;DuracionMs=[math]::Round($reloj.Elapsed.TotalMilliseconds,2);Error=$errorHttp}
    Write-Output "Consulta $n/10: HTTP $codigo; success=$exito"
    if($n -lt 10){Start-Sleep -Seconds 1}
}
Start-Sleep -Milliseconds 700
$filas | Export-Csv -LiteralPath "$salida\consultas.csv" -NoTypeInformation -Encoding UTF8
$eventos=@(Get-Content -LiteralPath $acceso -Encoding UTF8 | Where-Object {$_ -like "*$marca*"})
$eventos | Set-Content -LiteralPath "$salida\eventos-nginx.log" -Encoding UTF8
$eventosBackend=@()
if(Test-Path -LiteralPath $backend){$eventosBackend=@(Get-Content -LiteralPath $backend -Encoding UTF8 | Where-Object {$_ -like "*$marca*"})}
if($eventosBackend.Count){$eventosBackend | Set-Content -LiteralPath "$salida\eventos-backend.log" -Encoding UTF8}
$fechas=@($filas | ForEach-Object {([DateTimeOffset]::Parse($_.Fecha)).ToString('dd/MMM/yyyy',[Globalization.CultureInfo]::InvariantCulture)} | Select-Object -Unique)
$porFechaEndpoint=@($eventos | Where-Object { $linea=$_; ($linea -match 'GET /api/v1/catalogos/puertos\?') -and @($fechas | Where-Object {$linea.Contains($_)}).Count -gt 0 })
$porServicio=@($porFechaEndpoint | Where-Object {$_ -match '(?i)\bservice="sga-backend"(?:\s|$)'})
$ordinales=@($eventos | ForEach-Object {if($_ -match '[?&]consulta=(\d+)'){[int]$matches[1]}} | Sort-Object -Unique)
$aprobado=(@($filas | Where-Object {$_.HTTP -eq 200 -and $_.SuccessJSON}).Count -eq 10) -and $eventos.Count -eq 10 -and $porFechaEndpoint.Count -eq 10 -and $ordinales.Count -eq 10 -and $porServicio.Count -eq 10
$estado=if($aprobado){'EXITOSO'}else{'FALLO'}
$razon=if($aprobado){'Diez consultas registradas y recuperables por fecha, servicio y endpoint.'}elseif($eventos.Count -eq 10 -and $porFechaEndpoint.Count -eq 10 -and $porServicio.Count -eq 0){'Se registraron y localizaron las diez consultas por fecha y endpoint. El formato de acceso no incluye el servicio en cada evento: la busqueda por servicio no queda demostrada.'}else{'No se cumplieron todos los criterios. Consultar los conteos y evidencias.'}
$resultado=[pscustomobject]@{
    Prueba=1;Nombre='Ingesta y busqueda de logs';Importancia='Baja';Ejecucion='REALIZADA: diez GET de lectura, secuenciales, separados por un segundo';
    InicioLima=$inicio.ToString('o');FinLima=[DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(-5)).ToString('o');
    Identificador=$marca;Herramientas=@('Windows PowerShell','Invoke-WebRequest','Nginx access.log','Get-Content / busqueda de texto');
    Consultas=10;HTTP200=@($filas | Where-Object {$_.HTTP -eq 200}).Count;RespuestaNegocioCorrecta=@($filas | Where-Object {$_.SuccessJSON}).Count;
    EventosNginx=$eventos.Count;OrdinalesUnicos=$ordinales.Count;BusquedaFechaEndpoint=$porFechaEndpoint.Count;EventosConCampoServicio=$porServicio.Count;EventosBackendConIdentificador=$eventosBackend.Count;
    Estado=$estado;Resultado=$razon;Alcance='Almacenamiento y busqueda local de accesos de Nginx. No acredita una plataforma centralizada de ingesta.';
    ServicioEsperado='sga-backend';ServicioRegistrado=if($porServicio.Count -eq 10){'sga-backend, comprobado en los diez eventos'}else{'No se verifico sga-backend en los diez eventos'};
    Cambios='Sin cambios de configuracion, codigo, servicios o datos de negocio. Solo se generaron consultas de lectura y evidencias.'
}
$resultado | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath "$salida\resultado.json" -Encoding UTF8
@(
    'PRUEBA 01 - INGESTA Y BUSQUEDA DE LOGS',
    "Inicio Lima: $($resultado.InicioLima)",
    "Fin Lima: $($resultado.FinLima)",
    "Identificador: $marca",
    "Herramientas: $($resultado.Herramientas -join '; ')",
    "Consultas HTTP 200: $($resultado.HTTP200)/10",
    "Respuestas JSON success=true: $($resultado.RespuestaNegocioCorrecta)/10",
    "Eventos nuevos de Nginx: $($resultado.EventosNginx)/10",
    "Consultas unicas: $($resultado.OrdinalesUnicos)/10",
    "Busqueda por fecha y endpoint: $($resultado.BusquedaFechaEndpoint)/10",
    "Eventos con campo servicio: $($resultado.EventosConCampoServicio)/10",
    "Eventos backend con el identificador de prueba: $($resultado.EventosBackendConIdentificador)",
    "ESTADO: $estado",
    "RESULTADO: $razon",
    "ALCANCE: $($resultado.Alcance)",
    'Un fallo de criterio de monitoreo no significa que la consulta de negocio haya fallado.',
    'La falta de campo servicio no se reemplaza por una deduccion de la configuracion del proxy.',
    'Pruebas 02 a 15: NO EJECUTADAS en esta sesion.'
) | Set-Content -LiteralPath "$salida\resultado.txt" -Encoding UTF8
Copy-Item -LiteralPath $PSCommandPath -Destination "$salida\ejecutar-prueba-01.ps1"
Write-Output "Estado: $estado"
Write-Output "Resultado: $razon"
Write-Output "Evidencias: $salida"
