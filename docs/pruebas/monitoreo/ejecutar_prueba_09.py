from pathlib import Path
from datetime import datetime, timezone, timedelta
import argparse
import base64
import ctypes as ct
from ctypes import wintypes as wt
import csv
import json
import math
import msvcrt
import shutil
import subprocess
import time
import urllib.request
import urllib.error
import urllib.parse
import uuid

parser=argparse.ArgumentParser()
parser.add_argument('--proyecto',default=r'C:\Users\asus\Music\TransAduanas')
parser.add_argument('--endpoint',default='/api/v1/catalogos/puertos',choices=['/api/v1/catalogos/puertos','/api/v1/catalogos/tipos-carga'])
args=parser.parse_args()
endpoint=args.endpoint
root=Path(args.proyecto)
lock=(root/'monitoreo/M09-ejecucion.lock').open('a+b')
lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0)
try:msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
except OSError:raise RuntimeError('Ya existe un muestreo M09 en curso; no se inicia otra carga.') from None
tz=timezone(timedelta(hours=-5))
created=datetime.now(tz)
marker='M09-'+created.strftime('%Y%m%d-%H%M%S')+'-'+uuid.uuid4().hex[:6]
out=root/'evidencias-monitoreo'/marker
out.mkdir(parents=True,exist_ok=False)
shutil.copy2(__file__,out/Path(__file__).name)
private=json.loads((Path.home()/'Music/TransAduanas-herramientas/monitoreo/credenciales-locales.json').read_text(encoding='utf-8-sig'))
auth='Basic '+base64.b64encode((private['usuario']+':'+private['password']).encode()).decode()
del private

def powershell(text):
    result=subprocess.run(['powershell.exe','-NoProfile','-Command',text],capture_output=True,text=True,check=True)
    return json.loads(result.stdout.strip().lstrip('\ufeff'))

baseline=powershell("$p=Get-NetTCPConnection -LocalPort 8086 -State Listen | Select-Object -First 1; $j=Get-CimInstance Win32_Process -Filter ('ProcessId='+$p.OwningProcess); $m=Get-CimInstance Win32_OperatingSystem; $c=Get-CimInstance Win32_ComputerSystem; @{PID=[int]$j.ProcessId; WorkingSet=[double]$j.WorkingSetSize; PrivateBytes=[double]$j.PrivatePageCount; CpuSeconds=([double]$j.KernelModeTime+[double]$j.UserModeTime)/10000000; TotalVisibleBytes=[double]$m.TotalVisibleMemorySize*1024; FreeBytes=[double]$m.FreePhysicalMemory*1024; LogicalProcessors=[int]$c.NumberOfLogicalProcessors} | ConvertTo-Json -Compress")
(out/'windows-cim-inicial.json').write_text(json.dumps(baseline,indent=2),encoding='utf-8')

class MemoryStatus(ct.Structure):
    _fields_=[('length',wt.DWORD),('load',wt.DWORD),('total_phys',ct.c_ulonglong),('avail_phys',ct.c_ulonglong),('total_page',ct.c_ulonglong),('avail_page',ct.c_ulonglong),('total_virtual',ct.c_ulonglong),('avail_virtual',ct.c_ulonglong),('avail_extended',ct.c_ulonglong)]
class ProcessMemory(ct.Structure):
    _fields_=[('cb',wt.DWORD),('page_faults',wt.DWORD),('peak_working_set',ct.c_size_t),('working_set',ct.c_size_t),('quota_peak_paged',ct.c_size_t),('quota_paged',ct.c_size_t),('quota_peak_nonpaged',ct.c_size_t),('quota_nonpaged',ct.c_size_t),('pagefile',ct.c_size_t),('peak_pagefile',ct.c_size_t),('private_bytes',ct.c_size_t)]
kernel=ct.WinDLL('kernel32',use_last_error=True)
psapi=ct.WinDLL('psapi',use_last_error=True)
kernel.OpenProcess.argtypes=[wt.DWORD,wt.BOOL,wt.DWORD];kernel.OpenProcess.restype=wt.HANDLE
kernel.CloseHandle.argtypes=[wt.HANDLE];kernel.CloseHandle.restype=wt.BOOL
kernel.GetProcessTimes.argtypes=[wt.HANDLE]+[ct.POINTER(wt.FILETIME)]*4;kernel.GetProcessTimes.restype=wt.BOOL
kernel.GetSystemTimes.argtypes=[ct.POINTER(wt.FILETIME)]*3;kernel.GetSystemTimes.restype=wt.BOOL
kernel.GlobalMemoryStatusEx.argtypes=[ct.POINTER(MemoryStatus)];kernel.GlobalMemoryStatusEx.restype=wt.BOOL
psapi.GetProcessMemoryInfo.argtypes=[wt.HANDLE,ct.POINTER(ProcessMemory),wt.DWORD];psapi.GetProcessMemoryInfo.restype=wt.BOOL
handle=kernel.OpenProcess(0x1000,False,baseline['PID'])
process_source='Win32' if handle else 'CIM (Win32_Process)'
def ticks(value):return (value.dwHighDateTime<<32)+value.dwLowDateTime
def windows():
    memory=MemoryStatus();memory.length=ct.sizeof(memory)
    process=ProcessMemory();process.cb=ct.sizeof(process)
    creation,exit_time,kernel_time,user_time=[wt.FILETIME() for _ in range(4)]
    idle,total_kernel,total_user=[wt.FILETIME() for _ in range(3)]
    calls=[kernel.GlobalMemoryStatusEx(ct.byref(memory)),kernel.GetSystemTimes(ct.byref(idle),ct.byref(total_kernel),ct.byref(total_user))]
    if handle:
        calls.extend([psapi.GetProcessMemoryInfo(handle,ct.byref(process),ct.sizeof(process)),kernel.GetProcessTimes(handle,ct.byref(creation),ct.byref(exit_time),ct.byref(kernel_time),ct.byref(user_time))])
        process_cpu=(ticks(kernel_time)+ticks(user_time))/10000000
        creation_ticks=ticks(creation);working_set=process.working_set;private_bytes=process.private_bytes
    else:
        data=powershell("$p=Get-CimInstance Win32_Process -Filter 'ProcessId="+str(baseline['PID'])+"'; if(-not $p){throw 'El proceso Java ya no existe.'}; @{CpuSeconds=([double]$p.KernelModeTime+[double]$p.UserModeTime)/10000000; CreationTicks=([datetime]$p.CreationDate).ToFileTimeUtc(); WorkingSet=[double]$p.WorkingSetSize; PrivateBytes=[double]$p.PrivatePageCount} | ConvertTo-Json -Compress")
        process_cpu=data['CpuSeconds'];creation_ticks=data['CreationTicks'];working_set=data['WorkingSet'];private_bytes=data['PrivateBytes']
    if not all(calls):raise ct.WinError(ct.get_last_error())
    return {'Reloj':time.perf_counter(),'PID':baseline['PID'],'CreationTicks':creation_ticks,'CpuProcesoSegundos':process_cpu,
        'HostIdleTicks':ticks(idle),'HostTotalTicks':ticks(total_kernel)+ticks(total_user),'WorkingSetBytes':working_set,'PrivateBytes':private_bytes,
        'RAMTotalBytes':memory.total_phys,'RAMDisponibleBytes':memory.avail_phys,'RAMUsadaBytes':memory.total_phys-memory.avail_phys}

def api(url,payload=None,grafana=False):
    headers={'Authorization':auth} if grafana else {}
    if payload is not None:headers['Content-Type']='application/json'
    request=urllib.request.Request(url,data=json.dumps(payload).encode() if payload is not None else None,headers=headers)
    with urllib.request.urlopen(request,timeout=10) as response:return json.load(response)
job='job="transaduanas-backend"'
expressions={'CPUJAVA':'process_cpu_usage{'+job+'} * 100','CPUHOST':'system_cpu_usage{'+job+'} * 100',
    'CPUTIME':'process_cpu_time_ns_total{'+job+'} / 1e9','CPUS':'system_cpu_count{'+job+'}',
    'JVMHEAP':'sum(jvm_memory_used_bytes{'+job+',area="heap"})','JVMNONHEAP':'sum(jvm_memory_used_bytes{'+job+',area="nonheap"})',
    'START':'process_start_time_seconds{'+job+'}','UP':'up{'+job+'}'}
def prom(expr,when):
    data=api('http://127.0.0.1:9090/api/v1/query?'+urllib.parse.urlencode({'query':expr,'time':when}))['data']['result']
    return float(data[0]['value'][1]) if data else None
def native_cpu():
    with urllib.request.urlopen('http://127.0.0.1:9086/actuator/prometheus',timeout=10) as response:text=response.read().decode()
    for line in text.splitlines():
        if line.startswith('process_cpu_time_ns_total{'):return float(line.rsplit(' ',1)[1])/1e9,text
    raise RuntimeError('No esta disponible el tiempo CPU nativo.')

initial=windows()
if initial['RAMTotalBytes']!=baseline['TotalVisibleBytes']:raise RuntimeError('La memoria total Windows no coincide en las lecturas previas; revisar preparacion.')
(out/'criterios-previos.txt').write_text(
    'M09: 60 GET de lectura al catalogo, una solicitud por segundo, sin concurrencia ni saturacion forzada.\n'
    'Comparacion CPU Java: tiempo acumulado Micrometer frente a GetProcessTimes o KernelModeTime+UserModeTime de Win32_Process/CIM, con lecturas Windows antes/despues del endpoint; tolerancia de redondeo 0.05 s.\n'
    'CPU de Java por intervalo Windows = delta CPU / (delta tiempo monotonic * procesadores logicos) * 100. Host = delta (kernel+user-idle)/delta(kernel+user)*100.\n'
    'CPU host/proceso Windows y Micrometer: valores validos 0..100; se muestran diferencias de ventanas, sin exigir igualdad entre intervalos asincronos.\n'
    'Memoria: RAM total/disponible Windows con Win32 y working set/private bytes Java con Win32 o CIM; contrastar RAM total con CIM.\n'
    'Working set inicial CIM frente a Win32: tolerancia 64 MiB por diferencias de instante. No es un umbral de rendimiento.\n'
    'Memoria JVM heap/nonheap desde Prometheus/Grafana: disponible y no negativa; NO equivale a RSS/working set, private bytes o RAM host.\n'
    'Siete muestras; Prometheus y fuente Grafana comparados con mismo instante, tolerancia numerica relativa/absoluta 1e-9.\n'
    '60 respuestas HTTP 200/success=true; PID, inicio de JVM y CPU logicas estables; target UP.\n'
    f'El contador nativo de GET {endpoint} HTTP 200 debe incrementar exactamente 60 para controlar trafico simultaneo sobre ese endpoint.\n'
    'No hay exporter Windows en el panel: los contadores RAM host/proceso se muestran en consola y CSV, no se declaran como series Grafana.\n'
    'La prueba valida observabilidad bajo baja carga; no acredita limites de capacidad, alertas o ausencia de fugas.\n',encoding='utf-8')
snapshots=[];raw_graf=[];native_samples=[];requests=[]
t0=time.perf_counter();previous=initial
def snapshot():
    global previous
    before=windows();ncpu,raw=native_cpu();after=windows()
    catalog_count=sum(float(line.rsplit(' ',1)[1]) for line in raw.splitlines() if line.startswith('http_server_requests_seconds_count{') and all(label in line for label in [f'uri="{endpoint}"','method="GET"','status="200"']))
    native_samples.append({'WindowsAntes':before['CpuProcesoSegundos'],'MicrometerCpuSegundos':ncpu,'WindowsDespues':after['CpuProcesoSegundos'],'ContadorCatalogo':catalog_count})
    (out/('metricas-nativas-'+str(len(native_samples))+'.prom')).write_text(raw,encoding='utf-8')
    total=after['HostTotalTicks']-previous['HostTotalTicks'];idle=after['HostIdleTicks']-previous['HostIdleTicks']
    elapsed=after['Reloj']-previous['Reloj']
    row={'Segundos':round(time.perf_counter()-t0,3),'PID':after['PID'],
        'Windows_CPUJavaPct':(after['CpuProcesoSegundos']-previous['CpuProcesoSegundos'])/elapsed/baseline['LogicalProcessors']*100,
        'Windows_CPUHostPct':(total-idle)/total*100 if total else None,
        'Windows_CpuAcumuladaSegundos':after['CpuProcesoSegundos'],
        'Windows_WorkingSetBytes':after['WorkingSetBytes'],'Windows_PrivateBytes':after['PrivateBytes'],
        'Windows_RAMTotalBytes':after['RAMTotalBytes'],'Windows_RAMDisponibleBytes':after['RAMDisponibleBytes'],'Windows_RAMUsadaBytes':after['RAMUsadaBytes']}
    now_ms=int(time.time()*1000)-1000
    reply=api('http://127.0.0.1:3000/api/ds/query',{'from':str(now_ms-300000),'to':str(now_ms),
        'queries':[{'refId':key,'datasource':{'uid':'transaduanas-prometheus','type':'prometheus'},'expr':expr,
            'instant':True,'range':False,'format':'time_series','intervalMs':5000,'maxDataPoints':100} for key,expr in expressions.items()]},True)
    row['InstanteEvaluacionUnix']=now_ms/1000
    for key,expr in expressions.items():
        values=[]
        for frame in reply.get('results',{}).get(key,{}).get('frames',[]):
            for index,field in enumerate(frame.get('schema',{}).get('fields',[])):
                if field.get('type')=='number':values.extend(frame['data']['values'][index])
        row['Grafana_'+key]=values[-1] if values else None
        row['Prometheus_'+key]=prom(expr,now_ms/1000)
    snapshots.append(row);raw_graf.append(reply);previous=after
    print(f'M09 t={row["Segundos"]}s; CPU Windows host/Java={row["Windows_CPUHostPct"]:.2f}/{row["Windows_CPUJavaPct"]:.2f}%; '
          f'working set Java={row["Windows_WorkingSetBytes"]/1048576:.2f} MiB; RAM disponible={row["Windows_RAMDisponibleBytes"]/1073741824:.2f} GiB',flush=True)

try:
    time.sleep(1)
    snapshot()
    last_send=time.perf_counter()-1;next_snapshot=10
    load_start=time.perf_counter()
    for number in range(1,61):
        due=max(load_start+number-1,last_send+1);time.sleep(max(0,due-time.perf_counter()));last_send=time.perf_counter()
        status=0;success=False;error=''
        try:
            request=urllib.request.Request('http://127.0.0.1:5180'+endpoint+'?monitoreo='+marker+'&consulta='+str(number),headers={'User-Agent':'TransAduanas-M09-OS-Health'})
            with urllib.request.urlopen(request,timeout=5) as response:status=response.status;success=json.load(response).get('success') is True
        except urllib.error.HTTPError as exc:status=exc.code;error='HTTP '+str(exc.code)
        except Exception as exc:error=type(exc).__name__
        requests.append({'Consulta':number,'SegundosEnvio':round(last_send-load_start,6),'HTTP':status,'SuccessJSON':success,'Error':error})
        if time.perf_counter()-load_start>=next_snapshot:snapshot();next_snapshot+=10
    time.sleep(max(0,max(load_start+60,last_send+1)-time.perf_counter()))
    window=time.perf_counter()-load_start
    snapshot()
    final=windows()
finally:
    if handle:kernel.CloseHandle(handle)

def finite(value):return isinstance(value,(int,float)) and math.isfinite(value) and value>=0
checks={
    'HTTP60Correctos':len(requests)==60 and all(r['HTTP']==200 and r['SuccessJSON'] for r in requests),
    'SieteMuestras':len(snapshots)==7,
    'CargaUnica60Verificada':native_samples[-1]['ContadorCatalogo']-native_samples[0]['ContadorCatalogo']==60,
    'MemoriaTotalWin32CoincideCIM':all(row['Windows_RAMTotalBytes']==baseline['TotalVisibleBytes'] for row in snapshots),
    'WorkingSetInicialContrastado':abs(initial['WorkingSetBytes']-baseline['WorkingSet'])<=64*1048576,
    'MemoriaWindowsValida':all(0<row['Windows_RAMDisponibleBytes']<=row['Windows_RAMTotalBytes'] and row['Windows_WorkingSetBytes']>0 and row['Windows_PrivateBytes']>0 for row in snapshots),
    'CPUWindowsValida':all(finite(row[key]) and row[key]<=100.1 for row in snapshots for key in ['Windows_CPUHostPct','Windows_CPUJavaPct']),
    'CPUJavaMicrometerCoincideWindows':all(row['WindowsAntes']-.05<=row['MicrometerCpuSegundos']<=row['WindowsDespues']+.05 for row in native_samples),
    'SeriesPrometheusGrafanaValidas':all(finite(row[engine+'_'+key]) for row in snapshots for engine in ['Prometheus','Grafana'] for key in expressions),
    'SeriesPrometheusGrafanaCoinciden':all(finite(row['Grafana_'+key]) and finite(row['Prometheus_'+key]) and math.isclose(row['Grafana_'+key],row['Prometheus_'+key],rel_tol=1e-9,abs_tol=1e-9) for row in snapshots for key in expressions),
    'CPUMicrometerValida':all(finite(row['Prometheus_'+key]) and row['Prometheus_'+key]<=100 for row in snapshots for key in ['CPUJAVA','CPUHOST']),
    'ProcesadoresCoinciden':all(row['Prometheus_CPUS']==baseline['LogicalProcessors'] for row in snapshots),
    'JVMEstable':initial['CreationTicks']==final['CreationTicks'] and len({row['Prometheus_START'] for row in snapshots})==1 and all(row['Prometheus_UP']==1 for row in snapshots)
}
ok=all(checks.values())
def span(key):
    values=[row[key] for row in snapshots if finite(row[key])]
    return [min(values),max(values)] if values else [None,None]
stats={key:span(key) for key in ['Windows_CPUHostPct','Windows_CPUJavaPct','Prometheus_CPUHOST','Prometheus_CPUJAVA','Windows_WorkingSetBytes','Windows_PrivateBytes','Windows_RAMDisponibleBytes','Prometheus_JVMHEAP','Prometheus_JVMNONHEAP']}
text=(f'60 lecturas: {sum(r["HTTP"]==200 and r["SuccessJSON"] for r in requests)} correctas; {len(snapshots)} muestras; Java PID {baseline["PID"]}, {baseline["LogicalProcessors"]} CPU logicas. '
      f'CPU Windows host {stats["Windows_CPUHostPct"][0]:.2f}-{stats["Windows_CPUHostPct"][1]:.2f}%; Java {stats["Windows_CPUJavaPct"][0]:.2f}-{stats["Windows_CPUJavaPct"][1]:.2f}%. '
      f'Working set Java {stats["Windows_WorkingSetBytes"][0]/1048576:.2f}-{stats["Windows_WorkingSetBytes"][1]/1048576:.2f} MiB. '
      f'Contraste tiempo CPU Windows/Micrometer: {checks["CPUJavaMicrometerCoincideWindows"]}.')
result={'Prueba':9,'CodigoPrueba':'M09','Nombre':'CPU y memoria del sistema','Importancia':'Media',
    'Identificador':marker,'InicioLima':created.isoformat(),'FinLima':datetime.now(tz).isoformat(),
    'Ejecucion':'REALIZADA: carga secuencial de lectura, contadores Windows Win32/CIM y series Micrometer/Prometheus/Grafana.',
    'Herramientas':['Windows Win32 (GetSystemTimes, GetProcessTimes, GlobalMemoryStatusEx, GetProcessMemoryInfo)','PowerShell / CIM y Get-NetTCPConnection','Spring Boot Actuator / Micrometer','Prometheus 3.13.5','Grafana OSS 13.3.1','Python (carga, muestreo y contraste)'],
    'Estado':'EXITOSO' if ok else 'FALLO','Resultado':text,'Incidencia':'' if ok else 'Verificaciones incumplidas: '+', '.join(key for key,value in checks.items() if not value),
    'Verificaciones':checks,'PIDJava':baseline['PID'],'ProcesadoresLogicos':baseline['LogicalProcessors'],'EstadisticasMinMax':stats,
    'Muestras':len(snapshots),'VentanaEnvioSegundos':window,'ConsultasPromQL':expressions,'FuenteContadoresProceso':process_source,'EndpointPrueba':endpoint,
    'Alcance':'CPU host/proceso y memoria Windows observadas durante baja carga. CPU Java contrastada por tiempo acumulado Windows/Micrometer. Porcentaje CPU Windows y Micrometer usa intervalos diferentes: se reportan ambos sin exigir igualdad. RAM host y working set/private bytes se registran en consola/CSV nativos, no son series del panel Grafana; el panel expone CPU y memoria JVM. Heap/nonheap, working set, private bytes y RAM host son magnitudes distintas. No acredita saturacion, umbrales de capacidad ni ausencia de fugas.',
    'Cambios':'Sin modificaciones de configuracion, servicios ni datos de negocio; no se fuerzan CPU o GC.'}
for name,data in [('resultado.json',result),('grafana-respuestas.json',raw_graf),('contraste-cpu-acumulada.json',native_samples)]:
    (out/name).write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
for name,rows in [('consultas.csv',requests),('muestras-windows-prometheus-grafana.csv',snapshots)]:
    with (out/name).open('w',encoding='utf-8-sig',newline='') as file:
        writer=csv.DictWriter(file,fieldnames=list(rows[0]));writer.writeheader();writer.writerows(rows)
(out/'resultado.txt').write_text('M09 - CPU Y MEMORIA DEL SISTEMA\nESTADO: '+result['Estado']+'\n'+text+'\n'+result['Alcance']+'\n',encoding='utf-8')
print('Estado: '+result['Estado']+'\n'+text+'\nEvidencias: '+str(out),flush=True)
