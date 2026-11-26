param([string]$Proyecto=(Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path)
$ErrorActionPreference='Stop'
$taskEvidenceRoot=Join-Path $Proyecto 'evidencias-mantenimiento'
$taskOut=Join-Path $taskEvidenceRoot 'Evidencia_Prueba_7';$taskNumber=2
while(Test-Path -LiteralPath $taskOut){$taskOut=Join-Path $taskEvidenceRoot ('Evidencia_Prueba_7_'+$taskNumber);$taskNumber++}
$null=New-Item -ItemType Directory -Path $taskOut
$taskLab=Join-Path $taskOut 'laboratorio';$null=New-Item -ItemType Directory -Path $taskLab
$taskSource=Join-Path $Proyecto 'scripts\mantenimiento\Consultar-Disponibilidad.ps1'
$taskPS=Join-Path $env:WINDIR 'System32\WindowsPowerShell\v1.0\powershell.exe'
$taskName='TransAduanas_Prueba_7_'+[guid]::NewGuid().ToString('N').Substring(0,8)
$taskDescription='Prueba 7: consulta de disponibilidad en laboratorio; '+$taskName
$taskRegistered=$false;$taskRemoved=$false;$taskStatus='NO COMPLETADA';$taskError=$null
function Save-Json([string]$Name,$Value){[IO.File]::WriteAllText((Join-Path $taskOut $Name),($Value|ConvertTo-Json -Depth 10),[Text.UTF8Encoding]::new($false))}
function File-Hash([string]$Path){$h=[Security.Cryptography.SHA256]::Create();try{([BitConverter]::ToString($h.ComputeHash([IO.File]::ReadAllBytes($Path)))).Replace('-','')}finally{$h.Dispose()}}
try{
    if((Get-Service Schedule).Status -ne 'Running'){throw 'El Programador de tareas no esta activo.'}
    $taskZone=Get-TimeZone
    if($taskZone.BaseUtcOffset -ne [TimeSpan]::FromHours(-5) -or $taskZone.SupportsDaylightSavingTime){throw 'La zona del equipo no coincide con la prevista para esta comprobacion.'}
    $taskBefore=File-Hash $taskSource
    $taskConfig=@{Script=$taskSource;Resultados=(Join-Path $taskLab 'registros')}
    Save-Json 'configuracion.json' $taskConfig
    $taskWorker=Join-Path $taskLab 'consulta_programada.ps1'
    $taskWorkerCode=@'
param([string]$Configuracion)
$ErrorActionPreference='Stop'
$taskCfg=Get-Content -LiteralPath $Configuracion -Raw|ConvertFrom-Json
$taskIdentity=[Security.Principal.WindowsIdentity]::GetCurrent()
$taskPrincipal=[Security.Principal.WindowsPrincipal]::new($taskIdentity)
$taskStart=[pscustomobject]@{Inicio=[DateTimeOffset]::Now.ToString('o');Proceso=$PID;Cuenta=$taskIdentity.Name;Administrador=$taskPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)}
$taskRecord=Join-Path $PSScriptRoot ('inicio-'+[guid]::NewGuid().ToString('N')+'.json')
[IO.File]::WriteAllText($taskRecord,($taskStart|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
& (Join-Path $PSHOME 'powershell.exe') -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $taskCfg.Script -DirectorioResultados $taskCfg.Resultados *> (Join-Path $PSScriptRoot 'salida_consulta.txt')
exit $LASTEXITCODE
'@
    [IO.File]::WriteAllText($taskWorker,$taskWorkerCode,[Text.UTF8Encoding]::new($false))
    $taskArgs='-NoProfile -NonInteractive -ExecutionPolicy Bypass -File "'+$taskWorker+'" -Configuracion "'+(Join-Path $taskOut 'configuracion.json')+'"'
    $taskAction=New-ScheduledTaskAction -Execute $taskPS -Argument $taskArgs -WorkingDirectory $taskLab
    $taskAt=(Get-Date).AddSeconds(25)
    $taskAt=[DateTime]::new($taskAt.Year,$taskAt.Month,$taskAt.Day,$taskAt.Hour,$taskAt.Minute,$taskAt.Second)
    $taskTrigger=New-ScheduledTaskTrigger -Once -At $taskAt
    $taskIdentity=[Security.Principal.WindowsIdentity]::GetCurrent()
    $taskPrincipal=New-ScheduledTaskPrincipal -UserId $taskIdentity.User.Value -LogonType Interactive -RunLevel Limited
    $taskSettings=New-ScheduledTaskSettingsSet -ExecutionTimeLimit (New-TimeSpan -Minutes 1) -MultipleInstances IgnoreNew -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries
    if(Get-ScheduledTask -TaskName $taskName -TaskPath '\' -ErrorAction SilentlyContinue){throw 'El nombre ya existe; no se sobrescribe.'}
    $null=Register-ScheduledTask -TaskName $taskName -TaskPath '\' -Action $taskAction -Trigger $taskTrigger -Principal $taskPrincipal -Settings $taskSettings -Description $taskDescription
    $taskRegistered=$true
    $taskXML=Export-ScheduledTask -TaskName $taskName -TaskPath '\'
    $taskXML=$taskXML -replace 'encoding="UTF-16"','encoding="UTF-8"'
    [IO.File]::WriteAllText((Join-Path $taskOut 'tarea.xml'),$taskXML,[Text.UTF8Encoding]::new($false))
    Save-Json 'programacion.json' @{Tarea=$taskName;HoraProgramada=([DateTimeOffset]$taskAt).ToString('o');InicioManual=$false;Frecuencia='Una vez';Privilegios='Limited';ZonaWindows=$taskZone.Id}
    $taskDeadline=$taskAt.AddSeconds(80)
    do{
        Start-Sleep -Seconds 1
        $taskCurrent=Get-ScheduledTask -TaskName $taskName -TaskPath '\'
        $taskInfo=Get-ScheduledTaskInfo -TaskName $taskName -TaskPath '\'
        $taskStarts=@(Get-ChildItem -LiteralPath $taskLab -File -Filter 'inicio-*.json')
        $taskSummaries=@(Get-ChildItem -LiteralPath $taskConfig.Resultados -File -Filter 'resumen-*.json' -ErrorAction SilentlyContinue)
        $taskDone=$taskStarts.Count -gt 0 -and $taskSummaries.Count -gt 0 -and $taskCurrent.State -ne 'Running' -and $taskInfo.LastRunTime -ge $taskAt.AddSeconds(-1)
    }while(-not $taskDone -and (Get-Date) -lt $taskDeadline)
    if(-not $taskDone){throw 'No se confirmo la ejecucion automatica dentro de la ventana de observacion.'}
    $taskStartRecord=Get-Content -LiteralPath $taskStarts[0].FullName -Raw|ConvertFrom-Json
    $taskSummary=Get-Content -LiteralPath $taskSummaries[0].FullName -Raw|ConvertFrom-Json
    $taskStarted=[DateTimeOffset]::Parse($taskStartRecord.Inicio)
    $taskDelay=($taskStarted-([DateTimeOffset]$taskAt)).TotalSeconds
    if($taskCurrent.Actions.Count -ne 1 -or $taskCurrent.Actions[0].Execute -ne $taskPS -or $taskCurrent.Actions[0].Arguments -ne $taskArgs -or $taskCurrent.Description -ne $taskDescription){throw 'La definicion cambio; no se retira una tarea ajena.'}
    Unregister-ScheduledTask -TaskName $taskName -TaskPath '\' -Confirm:$false
    $taskRegistered=$false;$taskRemoved=-not [bool](Get-ScheduledTask -TaskName $taskName -TaskPath '\' -ErrorAction SilentlyContinue)
    $taskChecks=@(
        @{Codigo='C01';Comprobacion='Disparador horario de una sola ejecucion';Cumple=(@($taskCurrent.Triggers).Count -eq 1 -and $taskCurrent.Triggers[0].Enabled)},
        @{Codigo='C02';Comprobacion='Proceso iniciado con privilegios limitados';Cumple=(-not $taskStartRecord.Administrador)},
        @{Codigo='C03';Comprobacion='Inicio por horario sin lanzamiento manual';Cumple=($taskDelay -ge 0 -and $taskInfo.LastRunTime -ge $taskAt)},
        @{Codigo='C04';Comprobacion='Una sola ejecucion y un solo registro';Cumple=($taskStarts.Count -eq 1 -and $taskSummaries.Count -eq 1)},
        @{Codigo='C05';Comprobacion='Programador finaliza con codigo cero';Cumple=($taskInfo.LastTaskResult -eq 0)},
        @{Codigo='C06';Comprobacion='Consulta de disponibilidad correcta';Cumple=($taskSummary.Estado -eq 'Exitoso' -and $taskSummary.CodigoSalida -eq 0 -and @($taskSummary.Comprobaciones|Where-Object {-not $_.Cumple}).Count -eq 0)},
        @{Codigo='C07';Comprobacion='Sin ejecuciones omitidas';Cumple=($taskInfo.NumberOfMissedRuns -eq 0)},
        @{Codigo='C08';Comprobacion='Tarea retirada y script original intacto';Cumple=($taskRemoved -and (File-Hash $taskSource) -eq $taskBefore)}
    )
    $taskPassed=@($taskChecks|Where-Object {$_.Cumple}).Count
    $taskStatus=if($taskPassed -eq 8){'EXITOSO'}else{'FALLO'}
    Save-Json 'resultado.json' @{Prueba='PM07';Nombre='Programacion de tareas';Estado=$taskStatus;Fecha=[DateTimeOffset]::Now.ToString('o');HoraProgramada=([DateTimeOffset]$taskAt).ToString('o');InicioReal=$taskStarted.ToString('o');RetrasoSegundos=$taskDelay;CodigoSalidaProgramador=$taskInfo.LastTaskResult;Ejecuciones=$taskStarts.Count;EjecucionesOmitidas=$taskInfo.NumberOfMissedRuns;TareaRetirada=$taskRemoved;Administrador=$taskStartRecord.Administrador;Comprobaciones=$taskChecks;CriteriosCumplidos=$taskPassed;CriteriosTotales=8;ResultadoConsulta=$taskSummary;Alcance='Tarea temporal por horario con sesion interactiva y privilegios limitados; consulta de lectura del backend. No valida ejecucion sin sesion ni al reiniciar Windows. No se utiliza Start-ScheduledTask ni se cambia una tarea existente.'}
    $taskLines=@('Prueba 7 - Programacion de tareas','','Herramienta: Programador de tareas de Windows','Tarea: Revision de disponibilidad','Frecuencia: Una sola ejecucion',('Hora programada: '+$taskAt.ToString('HH:mm:ss')),('Inicio real: '+$taskStarted.ToString('HH:mm:ss.fff')),('Retraso observado: {0:N3} segundos' -f $taskDelay),'Inicio automatico: SI',('Ejecuciones registradas: '+$taskStarts.Count),('Codigo de salida: '+$taskInfo.LastTaskResult),('Ejecuciones omitidas: '+$taskInfo.NumberOfMissedRuns),'')
    foreach($taskCheck in $taskSummary.Comprobaciones){$taskLines+=($taskCheck.Nombre+': '+$taskCheck.Obtenido)}
    $taskLines+=@('','Privilegios: Limitados',('Tarea temporal retirada: '+$(if($taskRemoved){'SI'}else{'NO'})),('Criterios cumplidos: '+$taskPassed+'/8'),('Estado: '+$taskStatus))
}catch{
    $taskError=$_.Exception.Message
    Save-Json 'diagnostico.json' @{Estado='NO COMPLETADA';Tarea=$taskName;Error=$taskError;Linea=$_.InvocationInfo.ScriptLineNumber}
    $taskLines=@('Prueba 7 - Programacion de tareas','','No se completo la comprobacion.',$taskError,'Estado: NO COMPLETADA')
}finally{
    if($taskRegistered){
        $taskCurrent=Get-ScheduledTask -TaskName $taskName -TaskPath '\' -ErrorAction SilentlyContinue
        if($taskCurrent -and $taskCurrent.State -ne 'Running' -and $taskCurrent.Description -eq $taskDescription -and $taskCurrent.Actions[0].Arguments -eq $taskArgs){Unregister-ScheduledTask -TaskName $taskName -TaskPath '\' -Confirm:$false;$taskRemoved=$true}
    }
    [IO.File]::WriteAllText((Join-Path $taskOut 'resultado.txt'),($taskLines -join "`r`n"),[Text.UTF8Encoding]::new($false))
}
$taskLines
if($taskStatus -ne 'EXITOSO'){exit 1}
