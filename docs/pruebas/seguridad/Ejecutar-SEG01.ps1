[CmdletBinding()]
param([string]$Python = 'python')
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
$backend = Join-Path $root 'backend-sga'
$output = Join-Path $backend ('target/seg01-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Force -Path $output | Out-Null
Push-Location $backend
try {
    & .\mvnw.cmd 'org.apache.maven.plugins:maven-dependency-plugin:3.10.0:tree' '-DoutputType=json' "-DoutputFile=$output/dependencias-arbol.json"
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo resolver el arbol Maven.' }
    Write-Output 'Se enviaran a api.osv.dev solo coordenadas publicas y versiones de dependencias Maven.'
    & $Python (Join-Path $PSScriptRoot 'ejecutar_seg01_osv.py') $output
    if ($LASTEXITCODE -ne 0) { throw 'La consulta OSV no termino correctamente.' }
    $result = Get-Content -LiteralPath (Join-Path $output 'resultado.json') -Raw -Encoding UTF8 | ConvertFrom-Json
    Write-Output "SEG-01: $($result.resultado)"
    Write-Output "Evidencia: $output"
    if ($result.asociaciones_dependencia_aviso -gt 0) { throw 'Hay avisos que requieren revision; no registrar aprobada sin evaluar.' }
} finally { Pop-Location }
