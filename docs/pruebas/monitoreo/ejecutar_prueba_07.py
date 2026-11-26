from pathlib import Path
from datetime import datetime, timezone, timedelta
import argparse
import hashlib
import json
import os
import re
import shutil
import time
import urllib.request
import urllib.error
import uuid
import zipfile

parser=argparse.ArgumentParser()
parser.add_argument('--proyecto',default=r'C:\Users\asus\Music\TransAduanas')
args=parser.parse_args()
root=Path(args.proyecto)
tz=timezone(timedelta(hours=-5))
started=datetime.now(tz)
marker='M07-'+started.strftime('%Y%m%d-%H%M%S')+'-'+uuid.uuid4().hex[:6]
out=root/'evidencias-monitoreo'/marker
out.mkdir(parents=True,exist_ok=False)
shutil.copy2(__file__,out/Path(__file__).name)
backend=Path.home()/'Music/TransAduanas-desplegado/logs/sga-backend.log'
nginx=Path.home()/'Downloads/nginx-1.31.8/nginx-1.31.8/logs/access.log'
jar=Path.home()/'Music/TransAduanas-desplegado/current/app.jar'
(out/'criterios-previos.txt').write_text(
    'M07: correlacion de una solicitud de Clientes por requestId o traceId en Nginx, backend y acceso a MySQL.\n'
    'Una lectura GET normal, autenticada y sin concurrencia; no se provocan errores ni se modifica configuracion.\n'
    'Respuesta HTTP 200, success=true y data de tipo lista, sin persistir sus datos.\n'
    'X-Request-ID valido y exactamente un evento Nginx y un request_completed Java con el mismo ID.\n'
    'Debe existir evidencia de ejecucion SQL/JDBC vinculada explicitamente al mismo ID (o span conectado).\n'
    'La respuesta correcta, el codigo fuente y las metricas agregadas no sustituyen una traza SQL correlacionada.\n'
    'Si solo se correlacionan Nginx y backend, el estado global sera FALLO por cobertura incompleta, sin atribuir fallo funcional a MySQL.\n'
    'Herramientas Prometheus/Grafana proveen metricas agregadas; no se declaran recolectores de trazas.\n',encoding='utf-8')

def fetch(url,payload=None,headers=None):
    headers=dict(headers or {})
    if payload is not None:headers['Content-Type']='application/json'
    req=urllib.request.Request(url,data=json.dumps(payload).encode() if payload is not None else None,headers=headers)
    with urllib.request.urlopen(req,timeout=15) as response:
        return response.status,dict(response.headers),json.load(response)

username=os.environ.get('MONITOR_USERNAME','admin')
password=os.environ.get('MONITOR_PASSWORD')
if not password:raise RuntimeError('Falta credencial de laboratorio; no se envia la consulta.')
try:
    _,_,session=fetch('http://127.0.0.1:5180/api/v1/auth/login',{'username':username,'password':password})
    token=session['data']['token']
except Exception:
    (out/'preparacion.txt').write_text('Autenticacion no disponible. M07 no ejecutada; no se atribuye un fallo de correlacion.',encoding='utf-8')
    raise RuntimeError('No se pudo preparar la sesion de laboratorio.') from None
del password,session

offset=backend.stat().st_size
status=0;success=False;is_list=False;rid='';error=''
begin=time.perf_counter()
try:
    status,headers,data=fetch('http://127.0.0.1:5180/api/v1/clientes?soloActivos=true&monitoreo='+marker,
        headers={'Authorization':'Bearer '+token,'User-Agent':'TransAduanas-M07-Correlacion'})
    rid=next((v for k,v in headers.items() if k.lower()=='x-request-id'),'')
    success=data.get('success') is True
    is_list=isinstance(data.get('data'),list)
    del data,headers
except urllib.error.HTTPError as exc:status=exc.code;error='HTTP '+str(exc.code)
except Exception as exc:error=type(exc).__name__
elapsed=(time.perf_counter()-begin)*1000
del token
time.sleep(.5)
with backend.open('rb') as file:
    file.seek(offset)
    lines=file.read().decode('utf-8',errors='replace').splitlines()
valid_id=bool(re.fullmatch('[a-f0-9]{32}',rid))
related=[line for line in lines if valid_id and 'request_id='+rid in line]
java=[line for line in related if 'request_completed method=GET endpoint=/api/v1/clientes status=200 ' in line]
access=[line for line in nginx.read_text(encoding='utf-8').splitlines() if marker in line]
nginx_matches=[line for line in access if valid_id and 'request_id="'+rid+'"' in line and re.search(r'" 200 \d+ ',line)]
sql=[line for line in related if 'jdbc_query_completed db_system=mysql ' in line
     and 'outcome=SUCCESS ' in line and re.search(r'jdbc_span_id=[a-f0-9]{32}\b',line)]

# Inspect the actual deployed artifact; do not infer deployed settings solely from source.
with zipfile.ZipFile(jar) as archive:
    logback=archive.read('BOOT-INF/classes/logback-spring.xml').decode()
    staging=archive.read('BOOT-INF/classes/application-staging.properties').decode()
    libs=[name.rsplit('/',1)[-1] for name in archive.namelist() if name.startswith('BOOT-INF/lib/') and name.endswith('.jar')]
sql_level=re.search(r'<logger\s+name="org.hibernate.SQL"\s+level="([^"]+)"',logback)
inspection={
    'SHA256JarActivo':hashlib.file_digest(jar.open('rb'),'sha256').hexdigest(),
    'NivelOrgHibernateSQL':sql_level.group(1) if sql_level else 'No declarado',
    'ShowSQLStagingFalse':bool(re.search(r'^spring.jpa.show-sql=false\s*$',staging,re.M)),
    'BibliotecasRelacionadasConTrazas':[name for name in libs if re.search('tracing|opentelemetry|p6spy|datasource-proxy|zipkin|brave',name,re.I)],
    'EventosSQLCorrelacionadosEnLogBackend':len(sql),
    'Limite':'Se buscan operaciones JDBC reales completadas en MySQL, correlacionadas por requestId y con jdbc_span_id. No se exige recolector distribuido adicional ni se confunden metricas agregadas con trazas. No se guardan SQL, parametros ni resultados.'
}
checks={'RespuestaCorrecta':status==200 and success and is_list,'RequestIdValido':valid_id,
        'EventoNginxUnico':len(access)==len(nginx_matches)==1,'EventoBackendUnico':len(java)==1,
        'AccesoMySQLCorrelacionado':len(sql)>0}
ok=all(checks.values())
incidence='' if ok else ('No se dispone de una traza SQL/JDBC vinculada al requestId de la consulta de Clientes. '
    'La correlacion Nginx-backend funciona, pero el tramo hacia MySQL no es observable con la instrumentacion activa.'
    if all(value for key,value in checks.items() if key!='AccesoMySQLCorrelacionado') else
    'No se cumplieron las verificaciones de respuesta o correlacion; consultar verificaciones y evidencias.')
summary=(f'Una consulta de Clientes: HTTP {status}; ID {rid}; eventos correlacionados Nginx={len(nginx_matches)}, '
         f'backend={len(java)}, SQL/JDBC={len(sql)}. '+('Correlacion completa.' if ok else incidence))
result={'Prueba':7,'CodigoPrueba':'M07','Nombre':'Correlacion de una solicitud','Importancia':'Media',
    'Identificador':marker,'InicioLima':started.isoformat(),'FinLima':datetime.now(tz).isoformat(),
    'Ejecucion':'REALIZADA: una lectura autenticada normal de Clientes a traves de Nginx; busqueda por requestId e inspeccion del JAR activo.',
    'Herramientas':['Nginx access.log','Logback y MDC requestId del backend','Python (HTTP y busqueda de evidencias)','Inspeccion del JAR desplegado'],
    'Estado':'EXITOSO' if ok else 'FALLO','Resultado':summary,'Incidencia':incidence,'Verificaciones':checks,
    'RequestId':rid,'HTTP':status,'DuracionClienteMs':round(elapsed,3),'ErrorSolicitud':error,
    'InspeccionInstrumentacion':inspection,
    'Alcance':'Se comprueba la correlacion disponible en el despliegue activo; las metricas agregadas de Prometheus/Grafana no acreditan trazas individuales de SQL. No se habilito logging SQL ni se modifico el servicio para esta ejecucion.',
    'Cambios':'No se modificaron clientes, configuracion ni servicios. El inicio de sesion normal puede actualizar ultimoAcceso. No se guardan credenciales, token ni datos de clientes.'}
(out/'resultado.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
(out/'resultado.txt').write_text('M07 - CORRELACION DE UNA SOLICITUD\nESTADO: '+result['Estado']+'\n'+summary+'\n'+result['Alcance']+'\n',encoding='utf-8')
(out/'eventos-nginx.log').write_text('\n'.join(access)+'\n',encoding='utf-8')
(out/'eventos-backend.log').write_text('\n'.join(related)+'\n',encoding='utf-8')
(out/'instrumentacion-activa.json').write_text(json.dumps(inspection,ensure_ascii=False,indent=2),encoding='utf-8')
print('Estado: '+result['Estado']+'\n'+summary+'\nEvidencias: '+str(out),flush=True)
