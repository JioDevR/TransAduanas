[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateRange(1, 15)]
    [int]$Caso
)

$ErrorActionPreference = 'Stop'
$catalogo = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'casos-docente.json') -Raw -Encoding UTF8 |
    ConvertFrom-Json
$seleccion = @($catalogo | Where-Object { $_.numero -eq $Caso })
if ($seleccion.Count -ne 1) { throw 'El catalogo no identifica un caso unico.' }
$seleccion = $seleccion[0]
$codigo = $seleccion.codigo
$selector = '{0}#{1}' -f $seleccion.clase, $seleccion.metodo
$marca = '{0}-{1}' -f (Get-Date -Format 'yyyyMMdd-HHmmss-fff'), ([Guid]::NewGuid().ToString('N').Substring(0, 8))
$sufijo = "docente-$codigo-$marca"
$salida = Join-Path $PSScriptRoot "target\evaluacion-docente\$codigo\$marca"
$wrapper = Join-Path $PSScriptRoot 'mvnw.cmd'
if (-not (Test-Path -LiteralPath $wrapper -PathType Leaf)) { throw 'No se encontro mvnw.cmd.' }
New-Item -ItemType Directory -Path $salida -Force | Out-Null
$detalle = Join-Path $salida 'resultados-verificados.txt'
@("Caso: $Caso - $codigo", "Nombre: $($seleccion.nombre)", "Tipo: $($seleccion.tipo)",
    "Herramientas: $($seleccion.herramientas)", "Objetivo esperado: $($seleccion.objetivo)",
    "Selector: $selector", "Inicio: $((Get-Date).ToString('o'))") |
    Set-Content -LiteralPath (Join-Path $salida 'ejecucion.txt') -Encoding UTF8
Write-Host "$codigo - $($seleccion.tipo) - $($seleccion.nombre)"
Write-Host "Herramientas: $($seleccion.herramientas)"
Write-Host "Evidencias: $salida"
Push-Location -LiteralPath $PSScriptRoot
try {
    $ErrorActionPreference = 'Continue'
    & $wrapper test "-Dtest=$selector" "-Dsurefire.reportNameSuffix=$sufijo" 2>&1 |
        Tee-Object -FilePath (Join-Path $salida 'maven.log')
    $codigoMaven = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    # El sufijo unico evita aceptar un XML de otra ejecucion, caso o grupo.
    $reportes = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'target\surefire-reports') `
            -Filter "TEST-*-$sufijo.xml" -ErrorAction SilentlyContinue)
    $aprobado = $false
    $resumen = 'No se genero exactamente un XML nuevo del caso seleccionado.'
    if ($reportes.Count -eq 1) {
        Copy-Item -LiteralPath $reportes[0].FullName -Destination $salida
        [xml]$xml = Get-Content -LiteralPath $reportes[0].FullName -Raw -Encoding UTF8
        $suite = $xml.testsuite
        $metodos = @($suite.SelectNodes('testcase'))
        $texto = @($suite.SelectNodes('.//system-out') | ForEach-Object { $_.InnerText }) -join "`n"
        $lineas = @($texto -split '\r?\n' | Where-Object { $_ -match '^\[(PU|PI|PS|PA|CP)\d{2}\] ' })
        $lineas | Set-Content -LiteralPath $detalle -Encoding UTF8
        $mensajeFinal = "[$codigo] RESULTADO | APROBADO: todas las aserciones del metodo terminaron correctamente."
        $resumen = "Pruebas: $($suite.tests); fallos: $($suite.failures); errores: $($suite.errors); omitidas: $($suite.skipped)"
        $aprobado = $codigoMaven -eq 0 -and [int]$suite.tests -eq 1 -and
            [int]$suite.failures -eq 0 -and [int]$suite.errors -eq 0 -and [int]$suite.skipped -eq 0 -and
            $metodos.Count -eq 1 -and $metodos[0].name -eq $seleccion.metodo -and
            $suite.name -eq "$($seleccion.clase)($sufijo)" -and
            @($lineas | Where-Object { $_ -eq $mensajeFinal }).Count -eq 1
    }
    $estado = if ($aprobado) { 'APROBADO' } else { 'NO APROBADO' }
    @("Fin: $((Get-Date).ToString('o'))", "Codigo Maven: $codigoMaven", $resumen, "Estado: $estado") |
        Add-Content -LiteralPath (Join-Path $salida 'ejecucion.txt') -Encoding UTF8
    Write-Host "$codigo - $estado - $resumen"
    Write-Host "Resultados explicativos: $detalle"
    if (-not $aprobado) { throw "El caso $codigo no aprobo; revisa $salida" }
} finally {
    $ErrorActionPreference = 'Stop'
    Pop-Location
}
