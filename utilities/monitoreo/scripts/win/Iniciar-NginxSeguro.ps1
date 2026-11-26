[CmdletBinding()]
param([string]$Nginx='C:\Users\asus\Downloads\nginx-1.31.8\nginx-1.31.8')
$ErrorActionPreference='Stop'
$Nginx=(Resolve-Path -LiteralPath $Nginx).Path
$exe=Join-Path $Nginx 'nginx.exe'
$pidfile=Join-Path $Nginx 'logs\nginx.pid'
$puertos=@(5173,5174,5180)
$mutex=New-Object System.Threading.Mutex($false,'Local\TransAduanasNginxInicio')
$tomado=$false
function Comprobar-Instancia {
 $procesos=@(Get-CimInstance Win32_Process -Filter "Name='nginx.exe'" |
  Where-Object {$_.ExecutablePath -eq $exe})
 $escuchas=@(Get-NetTCPConnection -State Listen -ErrorAction Stop |
  Where-Object {$_.LocalPort -in $puertos})
 if(-not $procesos.Count){
  if($escuchas.Count){throw 'Puertos web ocupados sin instancia Nginx identificada. No se inicia otro proceso.'}
  return $false
 }
 $ids=@($procesos.ProcessId)
 $maestros=@($procesos|Where-Object {$_.ParentProcessId -notin $ids})
 if($maestros.Count -ne 1){throw 'Existen varias instancias o procesos Nginx sin maestro unico. Revisar M05 antes de iniciar o recargar.'}
 $maestro=[uint32]$maestros[0].ProcessId
 if(-not(Test-Path -LiteralPath $pidfile)){throw 'Nginx ya esta iniciado pero falta nginx.pid. No se crea otra instancia.'}
 if((Get-Content -LiteralPath $pidfile -Raw).Trim() -ne [string]$maestro){throw 'nginx.pid no coincide con el maestro actual. No se crea otra instancia.'}
 foreach($puerto in $puertos){
  $filas=@($escuchas|Where-Object {$_.LocalPort -eq $puerto})
  if(-not $filas.Count -or @($filas|Where-Object {$_.OwningProcess -notin $ids}).Count){throw "Puerto $puerto ausente o perteneciente a otra instancia. Revisar M05."}
 }
 return $true
}
try{
 $tomado=$mutex.WaitOne(0)
 if(-not $tomado){throw 'Ya hay un inicio de Nginx en curso.'}
 if(Comprobar-Instancia){Write-Output 'Nginx ya esta iniciado: maestro unico y puertos correctos. No se abrio otra instancia.';return}
 Push-Location -LiteralPath $Nginx
 try{
  & $exe -t
  if($LASTEXITCODE -ne 0){throw 'Nginx rechazo la configuracion. No se inicio.'}
  $p=Start-Process -FilePath $exe -WorkingDirectory $Nginx -WindowStyle Hidden -PassThru
 }finally{Pop-Location}
 $limite=(Get-Date).AddSeconds(10)
 do{
  Start-Sleep -Milliseconds 500
  try{$listo=Comprobar-Instancia}catch{if((Get-Date) -ge $limite){throw};$listo=$false}
 }while(-not $listo -and (Get-Date) -lt $limite)
 if(-not $listo){throw 'Nginx no completo el inicio. Revisar logs/error.log antes de reintentar.'}
 Write-Output 'Nginx iniciado y verificado: maestro unico y puertos 5173, 5174 y 5180.'
}finally{
 if($tomado){$mutex.ReleaseMutex()}
 $mutex.Dispose()
}
