"""Servidor local del ensayo PA01: artefactos compilados y proxy de lectura."""
from http.server import ThreadingHTTPServer,BaseHTTPRequestHandler
from pathlib import Path
from urllib.parse import urlsplit,unquote
import argparse,mimetypes,urllib.request,urllib.error
p=argparse.ArgumentParser();p.add_argument('--carpeta',required=True);p.add_argument('--puerto',type=int,required=True);p.add_argument('--backend',required=True);a=p.parse_args()
ROOT=Path(a.carpeta).resolve();target=urlsplit(a.backend)
assert target.scheme=='http' and target.hostname=='127.0.0.1' and target.port==8085
assert a.puerto in [5180,5174] and (ROOT/'index.html').is_file()
PUBLIC={'/api/v1/health','/api/v1/catalogos/tipos-carga','/api/v1/catalogos/puertos'}
class Handler(BaseHTTPRequestHandler):
    server_version='TransAduanasPA01';sys_version=''
    def reply(self,status,body,kind):
        self.send_response(status);self.send_header('Content-Type',kind);self.send_header('Content-Length',str(len(body)));self.send_header('X-Prueba','PA01');self.end_headers()
        if self.command!='HEAD':self.wfile.write(body)
    def do_GET(self):
        path=unquote(urlsplit(self.path).path)
        if path.startswith('/api/'):
            if path not in PUBLIC:return self.reply(405,b'Ruta fuera del ensayo','text/plain')
            try:
                with urllib.request.urlopen(a.backend+path,timeout=12) as r:return self.reply(r.status,r.read(),r.headers.get('Content-Type','application/json'))
            except urllib.error.HTTPError as e:return self.reply(e.code,b'Consulta no completada','text/plain')
            except OSError:return self.reply(502,b'Backend no disponible','text/plain')
        if any(part in ['..','.git'] or part.startswith('.') for part in Path(path).parts if part not in ['/','\\']):return self.reply(403,b'Ruta no permitida','text/plain')
        file=(ROOT/path.lstrip('/')).resolve()
        if not file.is_relative_to(ROOT):return self.reply(403,b'Ruta no permitida','text/plain')
        if file==ROOT or not Path(path).suffix:file=ROOT/'index.html'
        if not file.is_file():return self.reply(404,b'Archivo no encontrado','text/plain')
        return self.reply(200,file.read_bytes(),mimetypes.guess_type(file.name)[0] or 'application/octet-stream')
    do_HEAD=do_GET
    def do_POST(self):self.reply(405,b'PA01 solo permite lectura','text/plain')
    do_PUT=do_POST;do_DELETE=do_POST;do_PATCH=do_POST
    def log_message(self,fmt,*args):pass
ThreadingHTTPServer(('127.0.0.1',a.puerto),Handler).serve_forever()
