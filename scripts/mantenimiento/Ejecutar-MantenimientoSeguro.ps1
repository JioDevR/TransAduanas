param([Parameter(Mandatory=$true)][string]$Configuracion,[switch]$Comprobar)
$ErrorActionPreference='Stop'
try {
    $taskCfg=Get-Content -LiteralPath $Configuracion -Raw|ConvertFrom-Json
    $taskIdentity=[Security.Principal.WindowsIdentity]::GetCurrent()
    $taskPrincipal=[Security.Principal.WindowsPrincipal]::new($taskIdentity)
    if($taskPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator) -or $taskIdentity.User.Value -eq 'S-1-5-18'){throw 'Se requiere cuenta limitada.'}
    $taskResults=@();$taskPS=Join-Path $PSHOME 'powershell.exe'
    function Run-Task([string]$Nombre,[string[]]$Parametros){
        $taskText=& $taskPS -NoProfile -NonInteractive -ExecutionPolicy Bypass -File (Join-Path $taskCfg.Scripts $Nombre) @Parametros
        if($LASTEXITCODE -ne 0){throw ('Fallo de script de mantenimiento: '+$Nombre)}
        $taskText|Out-File -LiteralPath (Join-Path $taskCfg.Resultados ($Nombre+'.salida.txt')) -Encoding utf8
        return [pscustomobject]@{Script=$Nombre;CodigoSalida=0}
    }
    . (Join-Path $taskCfg.Scripts 'Invoke-ConBloqueo.ps1')
    $taskLock=Invoke-TareaConBloqueo -RutaBloqueo (Join-Path $taskCfg.Resultados 'mantenimiento.lock') -Accion { 'Bloqueo adquirido' }
    if(-not $taskLock.Adquirido){throw 'No se adquirio el bloqueo.'}
    $taskResults+=[pscustomobject]@{Script='Invoke-ConBloqueo.ps1';CodigoSalida=0}
    $taskResults+=Run-Task 'Consultar-Disponibilidad.ps1' @('-DirectorioResultados',(Join-Path $taskCfg.Resultados 'disponibilidad'))
    if($Comprobar){
        $taskLab=Join-Path $taskCfg.Resultados ('laboratorio-'+[guid]::NewGuid().ToString('N'))
        $null=New-Item -ItemType Directory -Path (Join-Path $taskLab 'temporales') -Force
        @{Prueba='PM05';Raiz=$taskLab}|ConvertTo-Json|Set-Content (Join-Path $taskLab '.transaduanas-laboratorio.json') -Encoding UTF8
        $taskOld=Join-Path $taskLab 'temporales\vencido.tmp';[IO.File]::WriteAllText($taskOld,'Dato sintetico')
        (Get-Item -LiteralPath $taskOld).LastWriteTimeUtc=[DateTime]::UtcNow.AddDays(-20)
        $taskKeep=Join-Path $taskLab 'temporales\vigente.tmp';[IO.File]::WriteAllText($taskKeep,'Conservar')
        $taskResults+=Run-Task 'Limpiar-Temporales.ps1' @('-CarpetaLaboratorio',$taskLab,'-DirectorioResultados',(Join-Path $taskCfg.Resultados 'limpieza'))
        if((Test-Path -LiteralPath $taskOld) -or -not(Test-Path -LiteralPath $taskKeep)){throw 'Limpieza no verificada.'}
        $taskCopies=Join-Path $taskLab 'retencion';$null=New-Item -ItemType Directory -Path (Join-Path $taskCopies 'copias') -Force
        @{Prueba='PM10';Raiz=$taskCopies}|ConvertTo-Json|Set-Content (Join-Path $taskCopies 'laboratorio.json') -Encoding UTF8
        Add-Type -AssemblyName System.IO.Compression
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $taskList=@()
        foreach($taskPair in @(@('vencido.zip',20),@('vigente.zip',0))){
            $taskZipPath=Join-Path $taskCopies ('copias\'+$taskPair[0]);$taskZip=[IO.Compression.ZipFile]::Open($taskZipPath,[IO.Compression.ZipArchiveMode]::Create)
            try{$taskWriter=[IO.StreamWriter]::new($taskZip.CreateEntry('dato.txt').Open());try{$taskWriter.Write('Dato sintetico')}finally{$taskWriter.Dispose()}}finally{$taskZip.Dispose()}
            $taskHasher=[Security.Cryptography.SHA256]::Create();try{$taskHash=([BitConverter]::ToString($taskHasher.ComputeHash([IO.File]::ReadAllBytes($taskZipPath)))).Replace('-','')}finally{$taskHasher.Dispose()}
            $taskList+=@{Archivo=$taskPair[0];SHA256=$taskHash;FechaUTC=[DateTime]::UtcNow.AddDays(-[int]$taskPair[1]).ToString('o')}
        }
        @{Copias=$taskList}|ConvertTo-Json -Depth 5|Set-Content (Join-Path $taskCopies 'inventario_respaldos.json') -Encoding UTF8
        $taskResults+=Run-Task 'Retener-Respaldos.ps1' @('-CarpetaLaboratorio',$taskCopies,'-DirectorioResultados',(Join-Path $taskCfg.Resultados 'retencion'))
        if((Test-Path (Join-Path $taskCopies 'copias\vencido.zip')) -or -not(Test-Path (Join-Path $taskCopies 'copias\vigente.zip'))){throw 'Retencion no verificada.'}
    }
    $taskRecord=[pscustomobject]@{Estado='Exitoso';Cuenta=$taskIdentity.Name;SID=$taskIdentity.User.Value;AdministradorEfectivo=$false;ComprobacionCompleta=[bool]$Comprobar;Scripts=$taskResults;Fecha=[DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(-5)).ToString('o')}
    [IO.File]::WriteAllText((Join-Path $taskCfg.Resultados 'ultima-ejecucion.json'),($taskRecord|ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))
    exit 0
}catch{if($taskCfg -and $taskCfg.Resultados){$taskErrorRecord=@{Tipo=$_.Exception.GetType().FullName;Mensaje=$_.Exception.Message;Linea=$_.InvocationInfo.ScriptLineNumber};[IO.File]::WriteAllText((Join-Path $taskCfg.Resultados 'error-comprobacion.json'),($taskErrorRecord|ConvertTo-Json),[Text.UTF8Encoding]::new($false))};[Console]::Error.WriteLine('Mantenimiento no completado. Tipo: '+$_.Exception.GetType().Name);exit 1}
