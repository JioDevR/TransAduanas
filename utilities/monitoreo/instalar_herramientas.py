from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
import tarfile
import urllib.request
import zipfile

root = Path(r'C:\Users\asus\Music\TransAduanas-herramientas\monitoreo')
(root / 'descargas').mkdir(parents=True, exist_ok=True)
assets = [
    {'nombre':'loki','version':'3.7.10','url':'https://github.com/grafana/loki/releases/download/v3.7.10/loki-windows-amd64.exe.zip','sha256':'7e3ae97518b290cbcaf9dc359af00e9d0a541333fe131fe7ce3056197306bfaf'},
    {'nombre':'alloy','version':'1.22.0','url':'https://github.com/grafana/alloy/releases/download/v1.22.0/alloy-windows-amd64.exe.zip','sha256':'ba0303f8aaa8f7b457015e1454aff509a18358ebe0b583b3b276c5652ca679a7'},
    {'nombre':'blackbox', 'version':'0.29.1',
     'url':'https://github.com/prometheus/blackbox_exporter/releases/download/v0.29.1/blackbox_exporter-0.29.1.windows-amd64.zip',
     'sha256':'d997412c28d6a8949eb1880197c77f7b6dc887915ae0303c4e48f674a54f7081'},
    {'nombre':'prometheus', 'version':'3.13.5',
     'url':'https://github.com/prometheus/prometheus/releases/download/v3.13.5/prometheus-3.13.5.windows-amd64.zip',
     'sha256':'846398b50b90e02f69960342190a1d01bf9a335b3aeee69bfd6ead41d3a314c5'},
    {'nombre':'grafana', 'version':'13.3.1',
     'url':'https://dl.grafana.com/grafana/release/13.3.1/grafana_13.3.1_42849527790_windows_amd64.tar.gz',
     'sha256':'cb78dceb990948fe7f1a189a0737fa7f6cf212076fc139a57d48cbc45dc0393f'},
]

def install(a):
    path = root / 'descargas' / a['url'].rsplit('/',1)[1]
    if not path.exists():
        temp = path.with_suffix(path.suffix+'.part')
        print('Descargando '+a['nombre']+' '+a['version'], flush=True)
        req=urllib.request.Request(a['url'],headers={'User-Agent':'TransAduanas-monitoring-setup'})
        with urllib.request.urlopen(req,timeout=90) as response,temp.open('wb') as f:
            while True:
                chunk=response.read(1024*1024)
                if not chunk:break
                f.write(chunk)
        temp.replace(path)
    actual=hashlib.file_digest(path.open('rb'),'sha256').hexdigest()
    if actual != a['sha256']:
        raise RuntimeError('SHA256 no coincide para '+a['nombre']+'; no se extrae ni ejecuta.')
    dest=root/(('blackbox_exporter' if a['nombre']=='blackbox' else a['nombre'])+'-'+a['version'])
    if not dest.exists():
        dest.mkdir()
        if path.name.endswith('.zip'):
            with zipfile.ZipFile(path) as z:
                for name in z.namelist():
                    if not (dest/name).resolve().is_relative_to(dest.resolve()):raise RuntimeError('Ruta de ZIP no valida')
                z.extractall(dest)
        else:
            with tarfile.open(path,'r:gz') as t:
                for m in t.getmembers():
                    if not(dest/m.name).resolve().is_relative_to(dest.resolve()) or m.issym() or m.islnk():
                        raise RuntimeError('Ruta de TAR no valida')
                t.extractall(dest,filter='data')
    candidates=list(dest.rglob({'prometheus':'prometheus.exe','grafana':'grafana.exe','blackbox':'blackbox_exporter.exe','loki':'loki-windows-amd64.exe','alloy':'alloy-windows-amd64.exe'}[a['nombre']]))
    if not candidates and a['nombre']=='grafana':candidates=list(dest.rglob('grafana-server.exe'))
    if len(candidates)!=1:raise RuntimeError('No se identifica ejecutable unico '+a['nombre'])
    a.update({'archivo':str(path),'ejecutable':str(candidates[0]),'sha256_verificado':actual})
    print('Verificado y extraido: '+a['nombre']+' '+a['version'],flush=True)
    return a

with ThreadPoolExecutor(max_workers=2) as pool:
    installed=list(pool.map(install,assets))
(root/'versiones.json').write_text(json.dumps(installed,ensure_ascii=False,indent=2),encoding='utf-8')
print('Herramientas listas en '+str(root),flush=True)
