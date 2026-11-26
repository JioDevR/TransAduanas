"""PM03: consulta Git y recupera copias de versiones ya registradas, sin revertir el proyecto."""
from configuracion_mantenimiento import raiz_proyecto,ruta_despliegue,ruta_herramientas,ruta_laboratorio,evidencias_originales,mysql_home,java_home,powershell,programa_git
from pathlib import Path
from datetime import datetime,timezone,timedelta
import argparse,subprocess,hashlib,json,os,shutil,csv

ap=argparse.ArgumentParser(description='Prueba 3 - Versionado de scripts')
ap.add_argument('--proyecto',default=str(raiz_proyecto(__file__)))
ap.add_argument('--archivo',default='docs/pruebas/mantenimiento/PM03/versionado-laboratorio.ps1')
ap.add_argument('--commit-inicial',default=os.getenv('PM03_COMMIT_INICIAL','4c74a5f69faf8da8f0da60a5872bc194fbf117dc'))
ap.add_argument('--commit-cambio',default=os.getenv('PM03_COMMIT_CAMBIO','2c46ec0fc962ed44d9cf61ea8fa6edd0cfc55d26'))
ap.add_argument('--commit-recuperacion',default=os.getenv('PM03_COMMIT_RECUPERACION','2c05ba6a31cccbb0e760cda116e887f8ed235ed8'))
ap.add_argument('--rama-publicada',default='mantenimiento/cierre-pruebas')
ap.add_argument('--commit-publicado',default=os.getenv('PM03_COMMIT_PUBLICADO','4ad89d5968a575205728d056dbedea140b1f2179'))
args=ap.parse_args();root=Path(args.proyecto).resolve()
out=root/'evidencias-mantenimiento/Evidencia_Prueba_3';i=2
while out.exists():out=root/'evidencias-mantenimiento'/f'Evidencia_Prueba_3_{i}';i+=1
out.mkdir(parents=True,exist_ok=False)
git=programa_git()
env=os.environ.copy();env['GIT_TERMINAL_PROMPT']='0';env['GCM_INTERACTIVE']='Never'
def run(*args):
    r=subprocess.run([git,'-C',str(root),*args],env=env,capture_output=True,timeout=30)
    if r.returncode:raise RuntimeError('No se pudo completar la consulta Git: '+args[0])
    return r.stdout
sha=lambda data:hashlib.sha256(data).hexdigest()
rel=args.archivo
initial=args.commit_inicial;modified=args.commit_cambio;restored=args.commit_recuperacion
branch=args.rama_publicada;published=args.commit_publicado;expected_remote='https://github.com/JioDevR/TransAduanas.git'
current=(root/rel).read_bytes();head_before=run('rev-parse','HEAD').strip();status_before=run('status','--porcelain','--untracked-files=no')
result={}
try:
    valid=run('rev-parse','--is-inside-work-tree').strip()==b'true'
    local=run('branch','--show-current').decode().strip()
    versions=[run('show',commit+':'+rel) for commit in [initial,modified,restored]]
    for label,data in zip(['version_inicial.ps1','version_modificada.ps1','version_recuperada.ps1'],versions):(out/label).write_bytes(data)
    run('merge-base','--is-ancestor',initial,modified);run('merge-base','--is-ancestor',modified,restored)
    remote=run('remote','get-url','origin').decode().strip()
    remote_ok=remote.rstrip('/')==expected_remote
    if not remote_ok:raise RuntimeError('El remoto no corresponde al repositorio grupal esperado.')
    remote_info=run('ls-remote','--refs','origin','refs/heads/'+branch).decode().strip()
    remote_commit=remote_info.split()[0] if remote_info else ''
    unchanged=current==(root/rel).read_bytes() and head_before==run('rev-parse','HEAD').strip() and status_before==run('status','--porcelain','--untracked-files=no')
    checks=[('C01','Repositorio e historial disponibles',valid and bool(local)),('C02','Versiones registradas en tres commits',len(versions)==3 and all(versions)),('C03','Cambio entre versiones registrado',versions[0]!=versions[1]),('C04','Recuperacion exacta de la version inicial',versions[0]==versions[2]),('C05','Script actual coincide y no fue alterado',unchanged and current==versions[2]),('C06','Repositorio grupal configurado',remote_ok),('C07','Rama del grupo publicada en GitHub',remote_commit==published)]
    criteria=[{'codigo':code,'comprobacion':label,'resultado':'CUMPLE' if passed else 'NO CUMPLE'} for code,label,passed in checks]
    ok=all(v for _,_,v in checks)
    result={'prueba':'PM03','nombre':'Versionado de scripts','tipo':'Consulta actual de historial y recuperacion de versiones ya registradas; no se crean commits nuevos ni se revierte el proyecto.','fecha_lima':datetime.now(timezone(timedelta(hours=-5))).isoformat(),'estado':'EXITOSO' if ok else 'FALLO','rama_local':local,'commits':{'inicial':initial,'cambio':modified,'recuperacion':restored},'hash_inicial':sha(versions[0]),'hash_modificado':sha(versions[1]),'hash_recuperado':sha(versions[2]),'hash_actual':sha(current),'repositorio_grupal':expected_remote,'rama_grupal':branch,'commit_remoto':remote_commit,'criterios_cumplidos':sum(v for _,_,v in checks),'criterios_totales':7,'criterios':criteria,'proyecto_sin_modificar':unchanged,'alcance':'Recuperacion de contenido con git show hacia archivos de evidencia. Solo lectura del repositorio local y remoto; no checkout, restore, reset, commit, push ni ejecucion de scripts.'}
    with (out/'criterios.csv').open('w',encoding='utf-8-sig',newline='') as f:w=csv.DictWriter(f,fieldnames=list(criteria[0]));w.writeheader();w.writerows(criteria)
    lines=['Prueba 3 - Versionado de scripts','','Historial del script:',initial[:7]+' - Version inicial',modified[:7]+' - Cambio de comentario',restored[:7]+' - Recuperacion de la version inicial','']
    lines.extend(label+': '+('CUMPLE' if passed else 'NO CUMPLE') for _,label,passed in checks)
    lines+=['','Rama del grupo: '+branch,'Commit en GitHub: '+remote_commit[:7],f'Criterios cumplidos: {result["criterios_cumplidos"]}/7','Estado: '+result['estado']]
except (RuntimeError,OSError,subprocess.TimeoutExpired) as error:
    result={'prueba':'PM03','nombre':'Versionado de scripts','fecha_lima':datetime.now(timezone(timedelta(hours=-5))).isoformat(),'estado':'NO COMPLETADA','diagnostico':str(error),'alcance':'La consulta no se pudo completar; no se cambio el proyecto ni se afirma un fallo de la aplicacion.'}
    lines=['Prueba 3 - Versionado de scripts','','No se completo la verificacion.',str(error),'Estado: NO COMPLETADA']
(out/'resultado.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
text='\n'.join(lines);(out/'resultado.txt').write_text(text+'\n',encoding='utf-8');print(text)
raise SystemExit(0 if result['estado']=='EXITOSO' else 1)
