[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$Nginx,
    [string]$Proyecto = ([IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..')))
)
$ErrorActionPreference = 'Stop'
$exe = Join-Path $Nginx 'nginx.exe'
Get-Item -LiteralPath $exe -ErrorAction Stop | Out-Null
New-Item -ItemType Directory -Path (Join-Path $Nginx 'logs'),(Join-Path $Nginx 'temp') -Force | Out-Null
$rutaWeb = (Resolve-Path -LiteralPath $Proyecto).Path.Replace('\','/')
foreach ($app in @('app-sga','app-b2b','app-landing')) {
    Get-Item -LiteralPath "$Proyecto\$app\dist\index.html" -ErrorAction Stop | Out-Null
}
$config = Join-Path $Nginx 'conf\nginx.conf'
$respaldo = "$config.respaldo-$(Get-Date -Format 'yyyyMMdd-HHmmss-fff')"
$existia = Test-Path -LiteralPath $config
if ($existia) { Copy-Item -LiteralPath $config -Destination $respaldo }
$servidores = foreach ($sitio in @(
    @{App='app-landing'; Puerto=5173},
    @{App='app-sga'; Puerto=5180},
    @{App='app-b2b'; Puerto=5174}
)) {
@"
    server {
        listen 127.0.0.1:$($sitio.Puerto);
        server_name localhost;
        root "$rutaWeb/$($sitio.App)/dist";
        index index.html;
        location / { try_files `$uri `$uri/ /index.html; }
        location /api/ {
            proxy_pass http://127.0.0.1:8086;
            proxy_set_header Host `$host;
            proxy_set_header X-Real-IP `$remote_addr;
            proxy_set_header X-Forwarded-For `$proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto `$scheme;
        }
    }
"@
}
$texto = @"
worker_processes 1;
error_log logs/error.log;
pid logs/nginx.pid;
events { worker_connections 1024; }
http {
    include mime.types;
    default_type application/octet-stream;
    access_log logs/access.log;
    sendfile on;
    client_max_body_size 100m;
$($servidores -join "`n")
}
"@
try {
    [IO.File]::WriteAllText($config,$texto,[Text.UTF8Encoding]::new($false))
    Push-Location -LiteralPath $Nginx
    try {
        & $exe -t
        if ($LASTEXITCODE -ne 0) { throw 'Nginx rechazo la configuracion.' }
    } finally { Pop-Location }
} catch {
    if ($existia) { Copy-Item -LiteralPath $respaldo -Destination $config -Force }
    else { Remove-Item -LiteralPath $config -ErrorAction SilentlyContinue }
    throw
}
Write-Output 'Configuracion Nginx validada. No se inicio ni recargo el servidor.'
if ($existia) { Write-Output "Respaldo: $respaldo" }
