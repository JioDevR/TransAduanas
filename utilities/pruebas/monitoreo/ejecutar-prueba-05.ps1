[CmdletBinding()]
param(
    [string]$Proyecto='C:\Users\asus\Music\TransAduanas',
    [string]$Nginx='C:\Users\asus\Downloads\nginx-1.31.8\nginx-1.31.8'
)
$ErrorActionPreference='Stop'
$inicio=[DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(-5))
$marca='M05-'+$inicio.ToString('yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,6)
$salida=Join-Path $Proyecto "evidencias-monitoreo\$marca"
New-Item -ItemType Directory -Path $salida -Force|Out-Null
@(
    'Identificar Java, MySQL y Nginx por ejecutable, PID y proceso propietario de cada puerto.',
    'Comprobar servicios MySQL84 y TransAduanasBackend en estado Running.',
    'Puertos esperados: Java 8086, MySQL 3306 y Nginx 5173/5174/5180 en escucha.',
    'Permitir que el PID del servicio sea un proceso supervisor: verificar la relacion de parentesco con el propietario del puerto.',
    'En Nginx comprobar la relacion maestro/trabajador usando nginx.pid; no exigir un PID diferente por interfaz.',
    'Realizar dos muestreos separados por dos segundos; un cambio de PID valido se documenta y no se considera un fallo por si solo.',
    'Exito: todos los componentes identificados correctamente en ambos muestreos. No se evalua una alerta de caida en esta prueba.'
)|Set-Content "$salida\criterios-previos.txt" -Encoding UTF8

function Es-Descendiente {
    param([uint32]$Proceso,[uint32]$Ancestro,[hashtable]$Mapa)
    $actual=$Proceso
    $visitados=@{}
    for($i=0;$i -lt 30;$i++){
        if($actual -eq $Ancestro -and $actual -ne 0){return $true}
        if($actual -eq 0 -or $visitados.ContainsKey($actual) -or -not $Mapa.ContainsKey($actual)){return $false}
        $visitados[$actual]=$true
        $actual=[uint32]$Mapa[$actual].ParentProcessId
    }
    return $false
}

function Tomar-Muestra {
    param([int]$Numero)
    $servicios=@(Get-CimInstance Win32_Service|Where-Object{$_.Name -in 'TransAduanasBackend','MySQL84'}|Select-Object Name,State,ProcessId)
    $todos=@(Get-CimInstance Win32_Process|Select-Object Name,ProcessId,ParentProcessId)
    $mapa=@{}
    foreach($proceso in $todos){$mapa[[uint32]$proceso.ProcessId]=$proceso}
    $procesos=@($todos|Where-Object{$_.Name -in 'java.exe','javaw.exe','nginx.exe','mysqld.exe' -or $_.ProcessId -in @($servicios.ProcessId)})
    $escuchas=@(Get-NetTCPConnection -State Listen -ErrorAction Stop|Where-Object{$_.LocalPort -in 8086,3306,5173,5174,5180}|Select-Object LocalAddress,LocalPort,OwningProcess)
    $maestro=0
    $archivoMaestro=Join-Path $Nginx 'logs\nginx.pid'
    if(Test-Path $archivoMaestro){[uint32]::TryParse((Get-Content $archivoMaestro -Raw).Trim(),[ref]$maestro)|Out-Null}
    $maestroValido=$maestro -ne 0 -and $mapa.ContainsKey([uint32]$maestro) -and $mapa[[uint32]$maestro].Name -eq 'nginx.exe'
    $filas=@()
    foreach($objetivo in @(
        @{Componente='Backend Java';Puerto=8086;Servicio='TransAduanasBackend';Ejecutables=@('java.exe','javaw.exe')},
        @{Componente='MySQL';Puerto=3306;Servicio='MySQL84';Ejecutables=@('mysqld.exe')},
        @{Componente='Nginx landing';Puerto=5173;Servicio='';Ejecutables=@('nginx.exe')},
        @{Componente='Nginx portal B2B';Puerto=5174;Servicio='';Ejecutables=@('nginx.exe')},
        @{Componente='Nginx SGA';Puerto=5180;Servicio='';Ejecutables=@('nginx.exe')}
    )){
        $puertos=@($escuchas|Where-Object{$_.LocalPort -eq $objetivo.Puerto})
        $servicio=@($servicios|Where-Object{$_.Name -eq $objetivo.Servicio})
        $padre=if($objetivo.Servicio -and $servicio.Count -eq 1){[uint32]$servicio[0].ProcessId}else{[uint32]$maestro}
        $estado=if($objetivo.Servicio){if($servicio.Count -eq 1){$servicio[0].State}else{'No encontrado'}}else{if($maestroValido){'Proceso maestro presente'}else{'Maestro no identificado'}}
        $servicioCorrecto=if($objetivo.Servicio){$servicio.Count -eq 1 -and $estado -eq 'Running'}else{$maestroValido}
        if(-not $puertos.Count){
            $filas+=[pscustomobject]@{Muestra=$Numero;Componente=$objetivo.Componente;Ejecutable='';PID=0;PIDSupervisor=$padre;Estado=$estado;Puerto=$objetivo.Puerto;Direccion='';Escucha=$false;ParentescoCorrecto=$false;Correcto=$false}
        }
        foreach($puerto in $puertos){
            $owner=[uint32]$puerto.OwningProcess
            $existe=$mapa.ContainsKey($owner)
            $nombre=if($existe){$mapa[$owner].Name}else{''}
            $relacion=Es-Descendiente -Proceso $owner -Ancestro $padre -Mapa $mapa
            $correcto=$servicioCorrecto -and $existe -and $nombre -in $objetivo.Ejecutables -and $relacion
            $filas+=[pscustomobject]@{Muestra=$Numero;Componente=$objetivo.Componente;Ejecutable=$nombre;PID=$owner;PIDSupervisor=$padre;Estado=$estado;Puerto=$objetivo.Puerto;Direccion=$puerto.LocalAddress;Escucha=$true;ParentescoCorrecto=$relacion;Correcto=[bool]$correcto}
        }
    }
    $muestra=[pscustomobject]@{Muestra=$Numero;FechaLima=[DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(-5)).ToString('o');Servicios=$servicios;ProcesosRelevantes=$procesos;MaestroNginx=$maestro;Puertos=$escuchas;Inventario=$filas}
    $muestra|ConvertTo-Json -Depth 7|Set-Content "$salida\muestra-$Numero.json" -Encoding UTF8
    $filas|Export-Csv "$salida\inventario-$Numero.csv" -NoTypeInformation -Encoding UTF8
    Write-Output $muestra
}

try{
    $m1=Tomar-Muestra -Numero 1
    Start-Sleep -Seconds 2
    $m2=Tomar-Muestra -Numero 2
}catch{
    ('Error del ejecutor o del proveedor de Windows; no se atribuye automaticamente al proyecto: '+$_.Exception.Message)|Set-Content "$salida\error-ejecucion.txt" -Encoding UTF8
    throw
}
$filas=@($m1.Inventario)+@($m2.Inventario)
$ok=@($filas|Where-Object{-not $_.Correcto}).Count -eq 0 -and $m1.Inventario.Count -ge 5 -and $m2.Inventario.Count -ge 5
$cambios=@()
foreach($componente in @($m1.Inventario.Componente|Select-Object -Unique)){
    $a=@($m1.Inventario|Where-Object{$_.Componente -eq $componente}|Select-Object -ExpandProperty PID -Unique|Sort-Object)-join ','
    $b=@($m2.Inventario|Where-Object{$_.Componente -eq $componente}|Select-Object -ExpandProperty PID -Unique|Sort-Object)-join ','
    if($a -ne $b){$cambios+="$componente : $a -> $b"}
}
$estado=if($ok){'EXITOSO'}else{'FALLO'}
$detalle=(@($m2.Inventario|ForEach-Object{"$($_.Componente): $($_.Ejecutable), PID $($_.PID), puerto $($_.Puerto), $($_.Estado)"})-join '; ')
$razon=if($ok){'Java, MySQL y Nginx identificados con sus puertos de escucha y parentesco supervisor/trabajador en dos muestreos. '+$detalle}else{'Uno o mas procesos, servicios o puertos no cumplen el inventario esperado. Consultar inventario-1.csv e inventario-2.csv.'}
$resultado=[pscustomobject]@{
    Prueba=5;CodigoPrueba='M05';Nombre='Inventario de procesos';Importancia='Baja';Identificador=$marca;
    InicioLima=$inicio.ToString('o');FinLima=[DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(-5)).ToString('o');
    Ejecucion='REALIZADA: dos muestreos de procesos, servicios y puertos; sin detener ni iniciar componentes';
    Herramientas=@('Windows PowerShell','CIM: Win32_Service y Win32_Process','Get-NetTCPConnection','Nginx nginx.pid','Inventario de consola y CSV');
    Estado=$estado;Resultado=$razon;Incidencia=if($ok){''}else{'Falta un proceso/servicio/puerto esperado o no se pudo vincular con su supervisor. Ver las filas Correcto=False.'};
    Muestreos=2;FilasVerificadas=$filas.Count;FilasCorrectas=@($filas|Where-Object{$_.Correcto}).Count;CambiosPIDObservados=$cambios;InventarioFinal=$m2.Inventario;
    Alcance='Inventario local de salud de procesos y puertos. El PID de un servicio supervisor puede diferir del proceso que escucha; no se evaluan alertas de caida ni disponibilidad de negocio.';
    Cambios='Ninguno en configuracion, procesos, servicios ni datos; solo lecturas de Windows y archivos de evidencia.'
}
$resultado|ConvertTo-Json -Depth 7|Set-Content "$salida\resultado.json" -Encoding UTF8
$filas|Export-Csv "$salida\inventario-completo.csv" -NoTypeInformation -Encoding UTF8
@('PRUEBA 05 - INVENTARIO DE PROCESOS',"ESTADO: $estado","RESULTADO: $razon","Herramientas: $($resultado.Herramientas-join '; ')","Muestreos: 2; filas verificadas: $($resultado.FilasVerificadas); correctas: $($resultado.FilasCorrectas)","Cambios de PID observados: $($cambios.Count)","ALCANCE: $($resultado.Alcance)",'No se reiniciaron ni detuvieron procesos. Las pruebas 06 a 15 siguen pendientes.')|Set-Content "$salida\resultado.txt" -Encoding UTF8
Copy-Item -LiteralPath $PSCommandPath -Destination "$salida\ejecutar-prueba-05.ps1"
$m2.Inventario|Format-Table Componente,Ejecutable,PID,PIDSupervisor,Puerto,Estado,Correcto -AutoSize|Out-String|Write-Output
Write-Output "Estado: $estado";Write-Output "Evidencias: $salida"
