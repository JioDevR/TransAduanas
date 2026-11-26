[CmdletBinding()]
param(
    [string]$Raiz = "$env:USERPROFILE\Music\TransAduanas-desplegado",
    [string]$Herramientas = "$env:USERPROFILE\Music\TransAduanas-herramientas\winsw"
)
$ErrorActionPreference = 'Stop'
# Solo prepara archivos. No instala, inicia o detiene servicios.
$archivoXml = Join-Path $Herramientas 'TransAduanasBackend.xml'
if (Get-Service -Name TransAduanasBackend -ErrorAction SilentlyContinue) {
    throw 'El servicio ya existe. Revisar su configuracion antes de modificarla.'
}
[xml]$config = Get-Content -LiteralPath $archivoXml -Raw
$java = [string]$config.service.executable
if (-not (Test-Path -LiteralPath $java -PathType Leaf)) { throw 'No se encontro Java.' }
foreach ($nombre in @('DB_PASSWORD','JWT_SECRET')) {
    $nodo = $config.SelectSingleNode("/service/env[@name='$nombre']")
    if ($null -eq $nodo -or [string]::IsNullOrWhiteSpace($nodo.GetAttribute('value')) -or
        $nodo.GetAttribute('value') -eq 'PENDIENTE_CONFIGURAR') {
        throw "Falta configurar $nombre. No compartir el XML privado."
    }
}
foreach ($carpeta in @('current','releases','logs','config')) {
    New-Item -ItemType Directory -Path (Join-Path $Raiz $carpeta) -Force | Out-Null
}
# El servicio no debe leer application-local.properties del checkout.
$config.service.workingdirectory = $Raiz
# Ruta relativa al directorio estable: evita el separador de unidad en -Xlog.
$gc = 'logs/gc.log'
$config.service.arguments = '-Xms256m -Xmx1024m "-Xlog:gc*:file=' + $gc +
    ':time,uptime,level,tags:filecount=5,filesize=10M" -jar "' +
    (Join-Path $Raiz 'current\app.jar') +
    '" --spring.profiles.active=staging --spring.jpa.hibernate.ddl-auto=validate --app.data-initializer.enabled=false --spring.config.import=optional:file:./config/application-local.properties'
$config.SelectSingleNode('/service/logpath').InnerText = [string](Join-Path $Raiz 'logs')
$config.service.startmode = 'Manual'
# Respaldo privado con las mismas restricciones que el original.
$respaldo = "$archivoXml.respaldo-$(Get-Date -Format 'yyyyMMdd-HHmmss-fff')"
New-Item -ItemType File -Path $respaldo | Out-Null
Set-Acl -LiteralPath $respaldo -AclObject (Get-Acl -LiteralPath $archivoXml)
Copy-Item -LiteralPath $archivoXml -Destination $respaldo -Force
$config.Save($archivoXml)
Write-Output 'Configuracion preparada: JAR independiente de target, inicio manual y logs JVM.'
Write-Output 'No se ha instalado ni arrancado el servicio. Falta colocar el primer JAR.'
