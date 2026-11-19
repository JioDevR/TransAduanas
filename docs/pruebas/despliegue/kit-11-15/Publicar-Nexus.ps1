[CmdletBinding()]
param([string]$Proyecto = "$env:USERPROFILE\Music\TransAduanas")
$ErrorActionPreference = 'Stop'
# Solo ejecutar durante la prueba formal, despues de configurar Nexus.
if ([string]::IsNullOrWhiteSpace($env:NEXUS_USERNAME) -or
    [string]::IsNullOrWhiteSpace($env:NEXUS_PASSWORD)) {
    throw 'Configurar NEXUS_USERNAME y NEXUS_PASSWORD en esta terminal sin compartirlas.'
}
$backend = Join-Path $Proyecto 'backend'
$settings = Join-Path $Proyecto 'docs\pruebas\despliegue\settings-nexus.xml.example'
Get-Item -LiteralPath $settings -ErrorAction Stop | Out-Null
Push-Location -LiteralPath $backend
try {
    $pom = [xml](Get-Content -LiteralPath '.\pom.xml' -Raw)
    $version = [string]$pom.project.version
    if ($version -match 'SNAPSHOT') { throw 'Este procedimiento requiere una version release.' }
    & .\mvnw.cmd -B -ntp -s $settings -Ppublicar-nexus deploy '-Dtest=BusinessFunctionalCasesTest'
    if ($LASTEXITCODE -ne 0) { throw 'La publicacion Maven fallo. Conservar el primer error.' }
    $jar = Join-Path $backend "target\sga-backend-$version.jar"
    $uri = "http://127.0.0.1:8081/repository/maven-releases/pe/sga/sga-backend/$version/sga-backend-$version.jar"
    $temporal = Join-Path ([IO.Path]::GetTempPath()) ("nexus-" + [guid]::NewGuid() + '.jar')
    $clave = ConvertTo-SecureString $env:NEXUS_PASSWORD -AsPlainText -Force
    $credencial = [PSCredential]::new($env:NEXUS_USERNAME, $clave)
    try {
        # No enviar estas credenciales a un servidor externo por HTTP.
        Invoke-WebRequest -Uri $uri -Credential $credencial -OutFile $temporal -UseBasicParsing
        $local = (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash
        $remoto = (Get-FileHash -LiteralPath $temporal -Algorithm SHA256).Hash
        if ($local -ne $remoto) { throw 'El JAR descargado no coincide con el publicado.' }
        [PSCustomObject]@{Version=$version; SHA256=$local; DescargaCoincide=$true}
    } finally {
        if (Test-Path -LiteralPath $temporal) { Remove-Item -LiteralPath $temporal }
    }
} finally { Pop-Location }
