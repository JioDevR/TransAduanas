param([Parameter(Mandatory=$true)][string]$Configuracion,[Parameter(Mandatory=$true)][ValidateSet('disponibilidad','limpieza','retencion','bloqueo')][string]$Caso)
$ErrorActionPreference='Stop'
$taskCfg=Get-Content -LiteralPath $Configuracion -Raw|ConvertFrom-Json
$taskPrincipal=[Security.Principal.WindowsPrincipal]::new([Security.Principal.WindowsIdentity]::GetCurrent())
$taskAdmin=$taskPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
$taskToken=[pscustomobject]@{AdministradorEfectivo=$taskAdmin;Sistema=([Security.Principal.WindowsIdentity]::GetCurrent().User.Value -eq 'S-1-5-18');VariableSinteticaPresente=(-not [string]::IsNullOrEmpty($env:TRANSADUANAS_PM15_SECRET));CuentaDedicadaServicio=$false}
[IO.File]::WriteAllText((Join-Path $taskCfg.out ('token-'+$Caso+'.json')),($taskToken|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
if($taskAdmin){throw 'No ejecutar esta comprobacion con administrador efectivo.'}
switch($Caso){
    disponibilidad { & (Join-Path $taskCfg.scripts 'Consultar-Disponibilidad.ps1') -DirectorioResultados (Join-Path $taskCfg.out 'disponibilidad') }
    limpieza { & (Join-Path $taskCfg.scripts 'Limpiar-Temporales.ps1') -CarpetaLaboratorio $taskCfg.limpieza -DirectorioResultados (Join-Path $taskCfg.out 'limpieza-resultados') }
    retencion { & (Join-Path $taskCfg.scripts 'Retener-Respaldos.ps1') -CarpetaLaboratorio $taskCfg.retencion -DirectorioResultados (Join-Path $taskCfg.out 'retencion-resultados') }
    bloqueo {
        . (Join-Path $taskCfg.scripts 'Invoke-ConBloqueo.ps1')
        $taskResult=Invoke-TareaConBloqueo -RutaBloqueo (Join-Path $taskCfg.out 'pm15.lock') -Accion { [pscustomobject]@{VariableEntornoDisponible=(-not [string]::IsNullOrEmpty($env:TRANSADUANAS_PM15_SECRET));SinEscribirValor=$true} }
        $taskResult|ConvertTo-Json -Depth 5
    }
}
