[CmdletBinding()]
param(
 [Parameter(Mandatory=$true)][ValidateSet('M01','M02','M03','M04','M05','M06','M07','M08','M09','M10','M11','M12','M13','M14','M15')][string]$Codigo,
 [string]$Proyecto='C:\Users\asus\Music\TransAduanas',
 [string]$Python='python',
 [switch]$CargaRealCPU
)
$ErrorActionPreference='Stop'
if($CargaRealCPU -and $Codigo -ne 'M14'){throw '-CargaRealCPU solo corresponde a M14.'}
$archivos=@{
 M01='ejecutar-prueba-01.ps1';M02='ejecutar-prueba-02.ps1';M03='ejecutar_prueba_03.py';
 M04='ejecutar_prueba_04_prometheus.py';M05='ejecutar-prueba-05.ps1';M06='ejecutar_prueba_06_prometheus.py';
 M07='ejecutar_prueba_07.py';M08='ejecutar_prueba_08.py';M09='ejecutar_prueba_09.py';M10='ejecutar_prueba_10.py';
 M11='ejecutar_prueba_11.py';M12='ejecutar_prueba_12.py';M13='ejecutar_prueba_13.py';M14='ejecutar_prueba_14.py';M15='ejecutar_prueba_15.py'
}
$ruta=Join-Path $Proyecto ('docs\pruebas\monitoreo\'+$archivos[$Codigo])
if(-not(Test-Path -LiteralPath $ruta)){throw "Falta el ejecutor: $ruta"}
$antesUsuario=$env:MONITOR_USERNAME;$antesPassword=$env:MONITOR_PASSWORD
$secure=$null;$ptr=[IntPtr]::Zero
try{
 if($Codigo -in @('M06','M07','M10') -and (-not $env:MONITOR_USERNAME -or -not $env:MONITOR_PASSWORD)){
  $usuario=Read-Host 'Usuario interno del SGA para esta prueba'
  if([string]::IsNullOrWhiteSpace($usuario)){throw 'Se requiere un usuario del SGA.'}
  $secure=Read-Host 'Contrasena SGA (oculta)' -AsSecureString
  $ptr=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
  $env:MONITOR_USERNAME=$usuario
  $env:MONITOR_PASSWORD=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
 }
 Write-Output "Ejecutando $Codigo con $($archivos[$Codigo]). Una prueba por vez."
 if($ruta.EndsWith('.ps1')){
  & $ruta -Proyecto $Proyecto
 }else{
  $argumentos=@($ruta,'--proyecto',$Proyecto)
  if($Codigo -in @('M04','M09')){$argumentos+=@('--endpoint','/api/v1/catalogos/tipos-carga')}
  if($Codigo -eq 'M14' -and $CargaRealCPU){$argumentos+='--autorizar-carga-real-host'}
  & $Python @argumentos
  if($LASTEXITCODE -ne 0){throw "El ejecutor termino con codigo $LASTEXITCODE. Conservar el diagnostico y comprobar resultado.json si existe."}
 }
 Write-Output 'Revise el Estado y las verificaciones del resultado nuevo; un codigo de proceso 0 no sustituye esa revision.'
}finally{
 $env:MONITOR_USERNAME=$antesUsuario;$env:MONITOR_PASSWORD=$antesPassword
 if($ptr -ne [IntPtr]::Zero){[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)}
 if($secure){$secure.Dispose()}
}
