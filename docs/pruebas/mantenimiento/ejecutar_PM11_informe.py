"""PM11: nueva verificación del respaldo real existente; no exporta ni restaura la base."""
from configuracion_mantenimiento import raiz_proyecto,ruta_despliegue,ruta_herramientas,ruta_laboratorio,evidencias_originales,mysql_home,java_home,powershell,programa_git
from pathlib import Path
from datetime import datetime,timezone,timedelta
import argparse,hashlib,json,csv,io,re,zipfile,ctypes
from ctypes import wintypes
try:
    from cryptography.hazmat.primitives.ciphers.aead import AESGCM
except ImportError:
    raise SystemExit('Se requiere un Python con el paquete cryptography para verificar AES-GCM.')

ap=argparse.ArgumentParser(description='Prueba 11 - Verificacion del respaldo integral')
ap.add_argument('--proyecto',default=str(raiz_proyecto(__file__)))
ap.add_argument('--evidencias-originales',default=str(evidencias_originales()))
args=ap.parse_args();root=Path(args.proyecto).resolve();origin=Path(args.evidencias_originales)
out=root/'evidencias-mantenimiento/Evidencia_Prueba_11';n=2
while out.exists():out=root/'evidencias-mantenimiento'/f'Evidencia_Prueba_11_{n}';n+=1
out.mkdir(parents=True,exist_ok=False)
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
sha=lambda b:hashlib.sha256(b).hexdigest()
class Blob(ctypes.Structure):
    _fields_=[('cbData',wintypes.DWORD),('pbData',ctypes.POINTER(ctypes.c_ubyte))]
def unwrap(data):
    buf=(ctypes.c_ubyte*len(data)).from_buffer_copy(data);incoming=Blob(len(data),buf);outgoing=Blob()
    crypt=ctypes.WinDLL('crypt32',use_last_error=True);kernel=ctypes.WinDLL('kernel32',use_last_error=True)
    crypt.CryptUnprotectData.argtypes=[ctypes.POINTER(Blob),ctypes.c_void_p,ctypes.c_void_p,ctypes.c_void_p,ctypes.c_void_p,wintypes.DWORD,ctypes.POINTER(Blob)]
    kernel.LocalFree.argtypes=[ctypes.c_void_p];kernel.LocalFree.restype=ctypes.c_void_p
    if not crypt.CryptUnprotectData(ctypes.byref(incoming),None,None,None,None,1,ctypes.byref(outgoing)):raise ctypes.WinError(ctypes.get_last_error())
    try:return ctypes.string_at(outgoing.pbData,outgoing.cbData)
    finally:kernel.LocalFree(ctypes.cast(outgoing.pbData,ctypes.c_void_p))
def count_rows(values):
    depth=0;quote=False;escaped=False;count=0
    for char in values:
        if quote:
            if escaped:escaped=False
            elif char==92:escaped=True
            elif char==39:quote=False
        elif char==39:quote=True
        elif char==40:
            if depth==0:count+=1
            depth+=1
        elif char==41:depth-=1
    if quote or depth!=0:raise ValueError('No se pudo validar el formato de un INSERT del respaldo.')
    return count

try:
    old=read(origin/'PM11_20261122-222724/resultado_PM11.json')
    external=read(origin/'cierre_pendientes/PM11_copia_USB.json')
    consent=read(origin/'cierre_pendientes/PM11_resguardo_confirmado.json')
    original=Path(old['respaldo_cifrado']);keypath=Path(old['clave_protegida'])
    before={str(p):sha(p.read_bytes()) for p in [original,keypath]}
    encrypted=original.read_bytes();header=b'TRANSADUNAS-PM11-AES256GCM-v1\n'
    assert encrypted.startswith(header),'Formato de respaldo no identificado.'
    key=unwrap(keypath.read_bytes());assert len(key)==32
    clear=AESGCM(key).decrypt(encrypted[len(header):len(header)+12],encrypted[len(header)+12:],header)
    with zipfile.ZipFile(io.BytesIO(clear)) as z:
        crc_ok=z.testzip() is None
        manifest=json.loads(z.read('manifiesto.json'))
        sql=z.read(manifest['sql']['archivo'])
        tables=set(x.decode('ascii') for x in re.findall(rb'^CREATE TABLE `([^`]+)`',sql,re.M))
        actual_counts={t:0 for t in tables}
        for match in re.finditer(rb'^INSERT INTO `([^`]+)` VALUES (.*);\r?$',sql,re.M):
            t=match.group(1).decode('ascii');assert t in actual_counts
            actual_counts[t]+=count_rows(match.group(2))
        docs=manifest['documentos'];doc_checks=[]
        for number,(rel,meta) in enumerate(sorted(docs.items()),1):
            content=z.read('documentos/'+rel)
            doc_checks.append({'documento':number,'bytes':len(content),'sha256_correcto':sha(content)==meta['sha256'] and len(content)==meta['bytes']})
        sql_ok=sha(sql)==manifest['sql']['sha256'] and len(sql)==manifest['sql']['bytes'] and tables==set(manifest['tablas_y_filas'])
        data_ok=actual_counts==manifest['tablas_y_filas']
    untouched=before=={str(p):sha(p.read_bytes()) for p in [original,keypath]}
    external_ok=external['sha256']==sha(encrypted) and external['verificacion_lectura_USB_y_descifrado_portable'] and consent['confirmacion']=='Ya resguardé el USB y la clave por separado fuera del equipo'
    checks=[('C01','Archivo original y checksum intactos',sha(encrypted)==old['sha256_respaldo_cifrado'] and untouched),('C02','Descifrado autenticado AES-256-GCM y clave DPAPI',len(key)==32 and bool(clear)),('C03','Integridad del contenedor ZIP',crc_ok),('C04','SQL y estructura de tablas verificados',sql_ok),('C05','Recuento de filas SQL coincide con el manifiesto',data_ok),('C06','Documentos completos con hashes correctos',len(doc_checks)==old['documentos'] and all(d['sha256_correcto'] for d in doc_checks)),('C07','Registro previo de copia externa y resguardo coherentes',external_ok)]
    ok=all(v for _,_,v in checks)
    criteria=[{'codigo':c,'comprobacion':label,'resultado':'CUMPLE' if v else 'NO CUMPLE'} for c,label,v in checks]
    result={'prueba':'PM11','nombre':'Verificacion del respaldo integral','tipo':'Verificacion actual de un respaldo real ya existente; no genera respaldo nuevo ni restaura MySQL.','fecha_verificacion':datetime.now(timezone(timedelta(hours=-5))).isoformat(),'fecha_respaldo_original':manifest['fecha_lima'],'estado':'EXITOSO' if ok else 'FALLO','base_de_datos':manifest['base_de_datos'],'tablas':len(tables),'filas':sum(actual_counts.values()),'documentos':len(doc_checks),'bytes_SQL':len(sql),'bytes_respaldo':len(encrypted),'sha256_respaldo':sha(encrypted),'tablas_y_filas_comprobadas':actual_counts,'cifrado':'AES-256-GCM','proteccion_clave':'DPAPI CurrentUser','comprobacion_documentos':doc_checks,'copia_externa':'Verificacion tecnica anterior registrada; no se accedio al USB en esta ejecucion.','resguardo':'Confirmado por el usuario previamente; no es una observacion fisica del ejecutor.','criterios':criteria,'criterios_cumplidos':sum(v for _,_,v in checks),'criterios_totales':7,'originales_sin_cambios':untouched,'alcance':'Comprueba cifrado y contenido del respaldo real existente y coherencia del registro previo externo. No acredita la actualidad del respaldo frente a cambios posteriores, un respaldo nuevo, una nueva lectura USB, ni restauracion MySQL; esta ultima corresponde a PM12. No muestra ni escribe SQL, documentos o claves descifradas.'}
    for name,data in [('criterios.csv',criteria),('tablas.csv',[{'tabla':t,'filas':n} for t,n in sorted(actual_counts.items())]),('documentos.csv',doc_checks)]:
        with (out/name).open('w',encoding='utf-8-sig',newline='') as f:w=csv.DictWriter(f,fieldnames=list(data[0]));w.writeheader();w.writerows(data)
    lines=['Prueba 11 - Verificacion del respaldo integral','','Respaldo real existente del '+datetime.fromisoformat(manifest['fecha_lima']).strftime('%d/%m/%Y'),'Base de datos: '+manifest['base_de_datos'],f'Tablas verificadas: {len(tables)}',f'Registros del respaldo: {sum(actual_counts.values())}',f'Documentos verificados: {len(doc_checks)}','','Cifrado: AES-256-GCM','Proteccion de clave: DPAPI','Descifrado autenticado: CORRECTO' if checks[1][2] else 'Descifrado: REVISAR','Integridad del SQL: '+('CORRECTA' if sql_ok and data_ok else 'REVISAR'),'Integridad de documentos: '+('CORRECTA' if checks[5][2] else 'REVISAR'),'Respaldo original sin cambios: '+('SI' if untouched else 'NO'),'','Copia USB: verificacion previa registrada','Resguardo separado: confirmado por el usuario',f'Criterios cumplidos: {result["criterios_cumplidos"]}/7','Estado: '+result['estado']]
    del key,clear,sql
except Exception as error:
    result={'prueba':'PM11','estado':'NO COMPLETADA','tipo_error':type(error).__name__,'alcance':'No se pudo completar la verificacion; no se modifico el respaldo ni la aplicacion.'}
    lines=['Prueba 11 - Verificacion del respaldo integral','','No se pudo completar la verificacion.','Tipo de error: '+type(error).__name__,'Estado: NO COMPLETADA']
(out/'resultado.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
text='\n'.join(lines);(out/'resultado.txt').write_text(text+'\n',encoding='utf-8');print(text)
raise SystemExit(0 if result['estado']=='EXITOSO' else 1)
