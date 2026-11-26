"""Lee solamente la ruta de almacenamiento, sin devolver credenciales."""
from pathlib import Path
import base64
import json
import os
import re
import subprocess
import xml.etree.ElementTree as ET


def leer_almacenamiento_backend(xml_path):
    config = ET.parse(xml_path).getroot()
    for item in config.findall('env'):
        if item.get('name') == 'STORAGE_DIR' and item.get('value'):
            return Path(item.get('value')).resolve(), 'WinSW'
    arguments = config.findtext('arguments', '')
    match = re.search(r'-Configuracion\s+(?:"([^"]+)"|(\S+))', arguments, re.I)
    if not match:
        raise RuntimeError('No se encontro STORAGE_DIR ni una configuracion segura del backend.')
    config_path = Path(match.group(1) or match.group(2))
    public = json.loads(config_path.read_text(encoding='utf-8-sig'))
    if public.get('Entorno', {}).get('STORAGE_DIR'):
        return Path(public['Entorno']['STORAGE_DIR']).resolve(), 'Configuracion segura'
    # El lanzador protege todas sus variables con DPAPI. No se muestran ni
    # persisten las otras variables: el unico dato devuelto es STORAGE_DIR.
    script = r'''
$ErrorActionPreference='Stop'
[Console]::OutputEncoding=[Text.UTF8Encoding]::new($false)
Add-Type -AssemblyName System.Security
$cfg=Get-Content -LiteralPath $env:M10_CONFIGURACION -Raw | ConvertFrom-Json
$bytes=$null
try {
    $bytes=[Security.Cryptography.ProtectedData]::Unprotect(
        [IO.File]::ReadAllBytes($cfg.Secretos),$null,
        [Security.Cryptography.DataProtectionScope]::LocalMachine)
    $variables=[Text.Encoding]::UTF8.GetString($bytes) | ConvertFrom-Json
    $directorio=[string]$variables.STORAGE_DIR
    if ([string]::IsNullOrWhiteSpace($directorio)) {throw 'STORAGE_DIR no disponible'}
    @{Directorio=$directorio} | ConvertTo-Json -Compress
} finally {
    if ($null -ne $bytes) {[Array]::Clear($bytes,0,$bytes.Length)}
    $variables=$null
}
'''
    env = os.environ.copy()
    env['M10_CONFIGURACION'] = str(config_path)
    encoded = base64.b64encode(script.encode('utf-16-le')).decode('ascii')
    completed = subprocess.run(
        ['powershell.exe', '-NoProfile', '-NonInteractive', '-EncodedCommand', encoded],
        env=env, capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=15)
    if completed.returncode:
        raise RuntimeError('No se pudo leer la ruta protegida de almacenamiento. Revisar permisos de lectura; no se mostraron credenciales.')
    directory = json.loads(completed.stdout.strip().lstrip('\ufeff')).get('Directorio')
    if not directory:
        raise RuntimeError('La configuracion segura no contiene una ruta de almacenamiento.')
    return Path(directory).resolve(), 'Configuracion protegida DPAPI'
