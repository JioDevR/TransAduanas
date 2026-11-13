[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateRange(1, 15)]
    [int]$Caso
)

$ErrorActionPreference = 'Stop'
$casos = @{
    1 = @{ Nombre = 'Formalización de prospecto a cliente'; Suite = 'BusinessFunctionalCasesTest'; Metodos = 'CP01_formalizacionProspectoACliente' }
    2 = @{ Nombre = 'Registro de clientes'; Suite = 'BusinessFunctionalCasesTest'; Metodos = 'CP02_registroClienteYDuplicado' }
    3 = @{ Nombre = 'Consulta de puertos'; Suite = 'BusinessFunctionalCasesTest'; Metodos = 'CP03_puertosListaDetalleReal' }
    4 = @{ Nombre = 'Validación de anticipos'; Suite = 'BusinessFunctionalCasesTest'; Metodos = 'CP04_validarAnticipoPersisteYRecalcula' }
    5 = @{ Nombre = 'Búsqueda de clientes'; Suite = 'BusinessFunctionalCasesTest'; Metodos = 'CP05_busquedaClientesPorRazonSocial' }
    6 = @{ Nombre = 'Consulta de tipos de carga'; Suite = 'BusinessFunctionalCasesTest'; Metodos = 'CP06_tiposCargaListaDetalleReal' }
    7 = @{ Nombre = 'Apertura de expedientes'; Suite = 'BusinessFunctionalCasesTest'; Metodos = 'CP07_aperturaExigePagoYPersiste' }
    8 = @{ Nombre = 'Actualización de clientes'; Suite = 'BusinessFunctionalCasesTest'; Metodos = 'CP08_actualizacionClientePersiste' }
    9 = @{ Nombre = 'Consulta de sedes'; Suite = 'BusinessFunctionalCasesTest'; Metodos = 'CP09_sedesListaDetalleReal' }
    10 = @{ Nombre = 'Consulta de expedientes'; Suite = 'BusinessFunctionalCasesTest'; Metodos = 'CP10_consultaListadoYDetalleExpediente' }
    11 = @{ Nombre = 'Solicitud de cotización'; Suite = 'BusinessFunctionalCasesTest'; Metodos = 'CP11_solicitudPortalCreaRegistroYRechazaInvalido' }
    12 = @{ Nombre = 'Aceptación de cotización'; Suite = 'BusinessFunctionalCasesTest'; Metodos = 'CP12_aceptarCotizacionPersisteSaldoYObservacion' }
    13 = @{ Nombre = 'Liquidación financiera'; Suite = 'BusinessFunctionalCasesTest'; Metodos = 'CP13_liquidacionEmitePersisteYConsulta' }
    14 = @{ Nombre = 'Carga y descarga de documentos'; Suite = 'BusinessFunctionalCasesTest'; Metodos = 'CP14_cargaYDescargaConservaContenido' }
    15 = @{ Nombre = 'Consulta de mercancías'; Suite = 'BusinessFunctionalCasesTest'; Metodos = 'CP15_mercanciasListaDetalleCodigoReal' }
}

$seleccion = $casos[$Caso]
$codigo = 'CP{0:D2}' -f $Caso
$selector = '{0}#{1}' -f $seleccion.Suite, $seleccion.Metodos
$wrapper = Join-Path $PSScriptRoot 'mvnw.cmd'
if (-not (Test-Path -LiteralPath $wrapper -PathType Leaf)) {
    throw "No se encontro Maven Wrapper: $wrapper"
}
# Cada ejecucion tiene su propia carpeta; las evidencias anteriores se conservan.
$marca = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$salida = Join-Path $PSScriptRoot "target\casos-funcionales\$codigo\$marca"
New-Item -ItemType Directory -Path $salida -Force | Out-Null
$inicio = Get-Date
@("Caso: $codigo", "Nombre: $($seleccion.Nombre)", "Inicio: $($inicio.ToString('o'))", "Selector: $selector") |
    Set-Content -LiteralPath (Join-Path $salida 'ejecucion.txt') -Encoding UTF8
Write-Host "$codigo - $($seleccion.Nombre)"
Write-Host "Evidencias: $salida"
Push-Location -LiteralPath $PSScriptRoot
try {
    # Los mensajes nativos no sustituyen el codigo de salida de Maven.
    $ErrorActionPreference = 'Continue'
    & $wrapper test "-Dtest=$selector" 2>&1 |
        Tee-Object -FilePath (Join-Path $salida 'maven.log')
    $codigoMaven = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    $reporte = Join-Path $PSScriptRoot "target\surefire-reports\TEST-pe.sga.functional.$($seleccion.Suite).xml"
    $reporteActual = (Test-Path -LiteralPath $reporte -PathType Leaf) -and
        ((Get-Item -LiteralPath $reporte).LastWriteTime -ge $inicio)
    if ($reporteActual) {
        Copy-Item -LiteralPath $reporte -Destination $salida
        [xml]$xml = Get-Content -LiteralPath $reporte -Raw
        $suite = $xml.testsuite
        $resumen = "Pruebas: $($suite.tests); fallos: $($suite.failures); errores: $($suite.errors); omitidas: $($suite.skipped)"
    } else {
        $resumen = 'No se genero un reporte nuevo. Revisar maven.log; no considerar aprobado.'
    }
    $estado = if ($codigoMaven -eq 0 -and $reporteActual -and
        [int]$suite.tests -gt 0 -and [int]$suite.failures -eq 0 -and
        [int]$suite.errors -eq 0 -and [int]$suite.skipped -eq 0) { 'APROBADO' } else { 'NO APROBADO' }
    @("Fin: $((Get-Date).ToString('o'))", "Codigo Maven: $codigoMaven", $resumen, "Estado: $estado") |
        Add-Content -LiteralPath (Join-Path $salida 'ejecucion.txt') -Encoding UTF8
    Write-Host "$codigo - $estado - $resumen"
    if ($estado -ne 'APROBADO') { throw "El caso $codigo no aprobo. Revisar $salida" }
} finally {
    Pop-Location
}
