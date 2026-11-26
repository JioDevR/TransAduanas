from pathlib import Path
from datetime import datetime, timezone, timedelta
import argparse
import base64
import hashlib
import json
import math
import re
import time
import urllib.request
import urllib.error
import urllib.parse

parser=argparse.ArgumentParser()
parser.add_argument('--proyecto',default=r'C:\Users\asus\Music\TransAduanas')
args=parser.parse_args()
root=Path(args.proyecto)
evidence=sorted((root/'evidencias-monitoreo').glob('integracion-prometheus-grafana-*'))[-1]
tools=Path.home()/'Music/TransAduanas-herramientas/monitoreo'
tz=timezone(timedelta(hours=-5))
marker='INTEGRACION-'+datetime.now(tz).strftime('%Y%m%d-%H%M%S')
private=json.loads((tools/'credenciales-locales.json').read_text(encoding='utf-8-sig'))
auth='Basic '+base64.b64encode((private['usuario']+':'+private['password']).encode()).decode()
del private

def fetch(url,data=None,admin=False):
    headers={}
    if admin:headers['Authorization']=auth
    if data is not None:headers['Content-Type']='application/json'
    request=urllib.request.Request(url,data=json.dumps(data).encode() if data is not None else None,headers=headers)
    with urllib.request.urlopen(request,timeout=15) as r:return json.load(r)

def text(url):
    with urllib.request.urlopen(url,timeout=15) as r:return r.read().decode()

def query(expr):
    return fetch('http://127.0.0.1:9090/api/v1/query?'+urllib.parse.urlencode({'query':expr}))

def native_counter(metrics):
    return sum(float(line.rsplit(' ',1)[1]) for line in metrics.splitlines()
               if line.startswith('http_server_requests_seconds_count{') and
               'uri="/api/v1/catalogos/puertos"' in line and 'status="200"' in line and 'method="GET"' in line)

expr='sum(http_server_requests_seconds_count{job="transaduanas-backend",uri="/api/v1/catalogos/puertos",status="200",method="GET"})'
before_metrics=text('http://127.0.0.1:9086/actuator/prometheus')
before=native_counter(before_metrics)
checks=[]
print('Validacion de integracion: diez lecturas suaves del catalogo, sin alterar pruebas M01-M06.',flush=True)
for i in range(1,11):
    url=f'http://127.0.0.1:5180/api/v1/catalogos/puertos?monitoreo={marker}&consulta={i}'
    with urllib.request.urlopen(url,timeout=10) as r:
        payload=json.load(r)
        checks.append({'Consulta':i,'HTTP':r.status,'Success':bool(payload.get('success')),'RequestId':r.headers.get('X-Request-ID','')})
    if i<10:time.sleep(1)
after_metrics=text('http://127.0.0.1:9086/actuator/prometheus')
after=native_counter(after_metrics)
(evidence/'metricas-backend.prom').write_text(after_metrics,encoding='utf-8')
for i in range(8):
    values=query(expr)['data']['result']
    prom_count=float(values[0]['value'][1]) if values else 0
    if prom_count>=after:break
    time.sleep(2)
targets=fetch('http://127.0.0.1:9090/api/v1/targets')
ds_health=fetch('http://127.0.0.1:3000/api/datasources/uid/transaduanas-prometheus/health',admin=True)
dashboard=fetch('http://127.0.0.1:3000/api/dashboards/uid/transaduanas-monitoreo')
expressions={
    'A':'up{job="transaduanas-backend"}',
    'B':expr,
    'C':'sum(jvm_memory_used_bytes{job="transaduanas-backend",area="heap"})',
    'D':'histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{job="transaduanas-backend",uri="/api/v1/catalogos/puertos"}[5m])))'
}
now=int(time.time()*1000)
grafana=fetch('http://127.0.0.1:3000/api/ds/query',{
    'from':str(now-900000),'to':str(now),
    'queries':[{'refId':ref,'datasource':{'uid':'transaduanas-prometheus','type':'prometheus'},
                'expr':expression,'instant':True,'range':False,'format':'time_series',
                'intervalMs':5000,'maxDataPoints':100} for ref,expression in expressions.items()]
},admin=True)
numeric={}
for ref,item in grafana.get('results',{}).items():
    samples=[]
    for frame in item.get('frames',[]):
        fields=frame.get('schema',{}).get('fields',[])
        arrays=frame.get('data',{}).get('values',[])
        for index,field in enumerate(fields):
            if field.get('type')=='number' and index<len(arrays):
                samples.extend(x for x in arrays[index] if isinstance(x,(int,float)) and math.isfinite(x))
    numeric[ref]=samples
(evidence/'grafana-consultas.json').write_text(json.dumps(grafana,ensure_ascii=False,indent=2),encoding='utf-8')
(evidence/'prometheus-targets.json').write_text(json.dumps(targets,ensure_ascii=False,indent=2),encoding='utf-8')
(evidence/'consultas-integracion.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2),encoding='utf-8')
protected={}
for u in ['http://127.0.0.1:8086/actuator/prometheus','http://127.0.0.1:5180/api/v1/clientes','http://127.0.0.1:9086/actuator/env']:
    try:
        with urllib.request.urlopen(u,timeout=10) as r:protected[u]=r.status
    except urllib.error.HTTPError as err:protected[u]=err.code
state=json.loads((Path.home()/'Music/TransAduanas-desplegado/estado.json').read_text(encoding='utf-8-sig'))
hash_actual=hashlib.sha256((Path.home()/'Music/TransAduanas-desplegado/current/app.jar').read_bytes()).hexdigest()
up=any(t['health']=='up' and t['labels'].get('job')=='transaduanas-backend' for t in targets['data']['activeTargets'])
ok=(state['Actual']['Version']=='0.5.4' and hash_actual==state['Actual']['Hash'].lower()
    and all(c['HTTP']==200 and c['Success'] for c in checks)
    and after-before>=10 and prom_count>=after and up and ds_health.get('status')=='OK'
    and len(dashboard['dashboard']['panels'])==12
    and numeric.get('A')==[1] and any(v>=after for v in numeric.get('B',[]))
    and any(v>0 for v in numeric.get('C',[])) and any(v>0 for v in numeric.get('D',[]))
    and protected['http://127.0.0.1:8086/actuator/prometheus']==401
    and protected['http://127.0.0.1:5180/api/v1/clientes']==401
    and protected['http://127.0.0.1:9086/actuator/env']>=400)
result={
    'Estado':'COMPLETADA' if ok else 'REQUIERE_REVISION','FechaLima':datetime.now(tz).isoformat(),
    'BackendActivo':state['Actual']['Version'],'VersionAnteriorConservada':state['Anterior']['Version'],'SHA256':hash_actual,
    'PrometheusTargetUP':up,'GrafanaDatasourceHealth':ds_health,'Dashboard':dashboard['dashboard']['title'],
    'Paneles':len(dashboard['dashboard']['panels']),'LecturasDeComprobacion':len(checks),
    'ContadorNativoAntes':before,'ContadorNativoDespues':after,'ContadorPrometheus':prom_count,
    'GrafanaValoresReales':numeric,'CamposGrafana':{'A':'up','B':'contador solicitudes','C':'heap bytes','D':'p95 segundos aproximado'},
    'ProteccionEndpoints':protected,'Alcance':'Validacion tecnica de integracion; no equivale a repetir formalmente M04/M06, ni ejecuta M07-M15. No valida activacion de alertas ni notificaciones.'
}
(evidence/'estado-integracion.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
(evidence/'validacion-integracion.txt').write_text(
    'INTEGRACION PROMETHEUS Y GRAFANA\n'+json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(result,ensure_ascii=False,indent=2),flush=True)
if not ok:raise SystemExit(2)
