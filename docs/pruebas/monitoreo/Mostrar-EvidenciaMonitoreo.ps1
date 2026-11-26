[CmdletBinding()]
param(
 [Parameter(Mandatory=$true)][ValidatePattern('^M(0[1-9]|1[0-5])$')][string]$Codigo,
 [string]$Proyecto='C:\Users\asus\Music\TransAduanas',
 [switch]$Abrir
)
$ErrorActionPreference='Stop'
$carpeta=Get-ChildItem -LiteralPath (Join-Path $Proyecto 'evidencias-monitoreo') -Directory -Filter "$Codigo-*" |
 Where-Object {Test-Path -LiteralPath (Join-Path $_.FullName 'resultado.json')} |
 Sort-Object Name -Descending | Select-Object -First 1
if(-not $carpeta){throw "No hay resultado completo para $Codigo. Revisar preparacion.txt o los logs del intento."}
$r=Get-Content -LiteralPath (Join-Path $carpeta.FullName 'resultado.json') -Raw|ConvertFrom-Json
[pscustomobject]@{Codigo=$Codigo;Ejecucion=$r.Identificador;Fecha=$r.InicioLima;Estado=$r.Estado;Resultado=$r.Resultado;Alcance=$r.Alcance}|Format-List
Get-ChildItem -LiteralPath $carpeta.FullName -File | Select-Object Name,Length | Format-Table -AutoSize
Write-Output "Carpeta: $($carpeta.FullName)"
Write-Output 'Compruebe la fecha: una preparacion incompleta no reemplaza un resultado anterior.'
if($Abrir){Invoke-Item -LiteralPath $carpeta.FullName}
