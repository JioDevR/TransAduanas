param([Parameter(Mandatory=$true)][string]$Configuracion)
$ErrorActionPreference='Stop'
try {
    Add-Type -AssemblyName System.Security
    $taskCfg=Get-Content -LiteralPath $Configuracion -Raw|ConvertFrom-Json
    $taskBytes=[Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($taskCfg.Secretos),$null,[Security.Cryptography.DataProtectionScope]::LocalMachine)
    try { $taskEnv=[Text.Encoding]::UTF8.GetString($taskBytes)|ConvertFrom-Json }
    finally { [Array]::Clear($taskBytes,0,$taskBytes.Length) }
    $taskIdentity=[Security.Principal.WindowsIdentity]::GetCurrent()
    $taskPrincipal=[Security.Principal.WindowsPrincipal]::new($taskIdentity)
    if($taskPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator) -or $taskIdentity.User.Value -eq 'S-1-5-18'){throw 'La identidad del backend debe ser limitada.'}
    $taskInfo=[Diagnostics.ProcessStartInfo]::new()
    $taskInfo.FileName=$taskCfg.Java
    $taskInfo.Arguments=$taskCfg.Argumentos
    $taskInfo.WorkingDirectory=$taskCfg.Directorio
    $taskInfo.UseShellExecute=$false
    $taskInfo.CreateNoWindow=$true
    foreach($taskProperty in $taskEnv.PSObject.Properties){$taskInfo.EnvironmentVariables[$taskProperty.Name]=[string]$taskProperty.Value}
    foreach($taskProperty in $taskCfg.Entorno.PSObject.Properties){$taskInfo.EnvironmentVariables[$taskProperty.Name]=[string]$taskProperty.Value}
    $taskRecord=[pscustomobject]@{Cuenta=$taskIdentity.Name;SID=$taskIdentity.User.Value;AdministradorEfectivo=$false;SecretosCifradosCargados=$true;Fecha=[DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(-5)).ToString('o')}
    [IO.File]::WriteAllText((Join-Path $taskCfg.Logs 'identidad-arranque.json'),($taskRecord|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
    $taskProcess=[Diagnostics.Process]::Start($taskInfo)
    $taskEnv=$null
    $taskProcess.WaitForExit()
    exit $taskProcess.ExitCode
} catch {
    # No volcar entorno, datos descifrados ni argumentos de excepciones.
    [Console]::Error.WriteLine('No se pudo iniciar el backend seguro. Tipo: '+$_.Exception.GetType().Name)
    exit 1
}
