[CmdletBinding()]
param([string]$Gitleaks = 'gitleaks', [switch]$Historial)
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
$output = Join-Path $root ('backend-sga/target/seg02-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
$emptyIgnore = Join-Path $output 'ignore-vacio'
New-Item -ItemType Directory -Force -Path $emptyIgnore | Out-Null
$arguments = @('dir', $root)
if ($Historial) { $arguments = @('git', $root, '--log-opts=--all') }
$arguments += @('--config', (Join-Path $PSScriptRoot 'gitleaks-seg02.toml'), '--redact=100', '--no-banner', '--no-color', '--ignore-gitleaks-allow', '--gitleaks-ignore-path', $emptyIgnore, '--max-target-megabytes=5', '--report-format=json', '--report-path', (Join-Path $output 'candidatos-redactados.json'))
$resolvedGitleaks = (Get-Command $Gitleaks -CommandType Application -ErrorAction Stop).Source
$previousErrorPreference = $ErrorActionPreference
try {
    # Gitleaks escribe mensajes normales en stderr; PowerShell 5.1 no debe tratarlos como fallo fatal.
    $ErrorActionPreference = 'Continue'
    & $resolvedGitleaks @arguments *> (Join-Path $output 'ejecucion.log')
    $code = $LASTEXITCODE
} finally { $ErrorActionPreference = $previousErrorPreference }
if ($code -notin @(0,1)) { throw 'Gitleaks no completo el analisis; revisar el registro local.' }
if ($code -eq 1) { Write-Output 'SEG-02: PENDIENTE de clasificar candidatos. No todos son vulnerabilidades: revisar ejemplos, datos de test y referencias a variables.' }
else { Write-Output 'SEG-02: sin candidatos detectados en el alcance ejecutado.' }
Write-Output "Evidencia local con valores ocultos: $output"
Write-Output 'La revision de historial e instalaciones existentes se debe registrar por separado.'
