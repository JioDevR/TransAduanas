from pathlib import Path
from datetime import datetime, timezone, timedelta
from collections import Counter
import argparse
import csv
import gzip
import hashlib
import json
import os
import re
import shutil
import subprocess
import uuid
import xml.etree.ElementTree as ET
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('--proyecto', default=r'C:\Users\asus\Music\TransAduanas')
args = parser.parse_args()
project = Path(args.proyecto)
tz = timezone(timedelta(hours=-5))
start = datetime.now(tz)
marker = 'M03-' + start.strftime('%Y%m%d-%H%M%S') + '-' + uuid.uuid4().hex[:6]
out = project / 'evidencias-monitoreo' / marker
out.mkdir(parents=True, exist_ok=False)
(out / 'lib').mkdir()
(out / 'classes').mkdir()
logs = out / 'logs-laboratorio'
ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
version = ET.parse(project / 'backend-sga/pom.xml').getroot().find('m:version', ns).text
jar = project / 'backend-sga/target' / f'sga-backend-{version}.jar'
source_config = project / 'backend-sga/src/main/resources/logback-spring.xml'
original_bytes = source_config.read_bytes()
original_hash = hashlib.sha256(original_bytes).hexdigest()

(out / 'criterios-previos.txt').write_text(
    '1. Emitir 1500 eventos INFO numerados mediante Logback real del JAR del proyecto.\n'
    '2. Provocar rotacion por tamano en una carpeta aislada con limite de 64 KB.\n'
    '3. Debe existir al menos un archivo .log.gz legible y el archivo activo.\n'
    '4. Recuperar exactamente los 1500 eventos: ninguna perdida ni duplicado.\n'
    '5. El primer evento debe ser localizable en un archivo rotado y el ultimo en el activo.\n'
    '6. No modificar los logs ni la configuracion del servicio instalado.\n', encoding='utf-8')

print('M03: preparando laboratorio aislado.', flush=True)
with zipfile.ZipFile(jar) as z:
    libs = [n for n in z.namelist() if n.startswith('BOOT-INF/lib/') and
            any(Path(n).name.startswith(prefix) for prefix in
                ('logback-classic-', 'logback-core-', 'slf4j-api-'))]
    if len(libs) != 3:
        raise RuntimeError('No se identificaron las tres dependencias reales de logging.')
    for name in libs:
        (out / 'lib' / Path(name).name).write_bytes(z.read(name))
    embedded_config = z.read('BOOT-INF/classes/logback-spring.xml')

(out / 'logback-original.xml').write_bytes(embedded_config)
tree = ET.ElementTree(ET.fromstring(embedded_config))
root = tree.getroot()
root.set('scan', 'false')
changes = []
for prop in root.findall('property'):
    if prop.get('name') == 'LOG_PATH':
        changes.append('LOG_PATH: carpeta logs-laboratorio de esta ejecucion.')
        prop.set('value', logs.as_posix())
for max_file in root.findall('.//maxFileSize'):
    changes.append(f'maxFileSize: {max_file.text} -> 64KB solo en laboratorio.')
    max_file.text = '64KB'
# Avoid console volume while retaining both real file appenders and their policies.
for logger in root.findall('root'):
    for ref in list(logger.findall('appender-ref')):
        if ref.get('ref') == 'CONSOLE':
            logger.remove(ref)
changes += ['scan=false solo en laboratorio.', 'Salida de consola desactivada solo en laboratorio.']
config = out / 'logback-laboratorio.xml'
tree.write(config, encoding='utf-8', xml_declaration=True)
(out / 'adaptaciones-laboratorio.txt').write_text('\n'.join(changes), encoding='utf-8')

probe = Path(__file__).resolve().parent / 'RotationProbe.java'
shutil.copy2(probe, out / probe.name)
shutil.copy2(Path(__file__), out / 'ejecutar_prueba_03.py')
classpath = os.pathsep.join([str(out / 'classes'), str(out / 'lib' / '*')])
java, javac = shutil.which('java'), shutil.which('javac')
if not java or not javac:
    raise RuntimeError('Se requiere JDK con java y javac para ejecutar esta prueba.')
compile_command = [javac, '-encoding', 'UTF-8', '-cp', classpath,
                   '-d', str(out / 'classes'), str(out / probe.name)]
compile_result = subprocess.run(compile_command, capture_output=True, text=True,
                                encoding='utf-8', errors='replace', timeout=30)
(out / 'compilacion.txt').write_text(compile_result.stdout + compile_result.stderr, encoding='utf-8')
if compile_result.returncode:
    raise RuntimeError('El ejecutor no compilo. Es un error de ejecucion, no un fallo atribuido al proyecto.')

print('M03: emitiendo 1500 eventos pequenos; pausa cada 25 eventos.', flush=True)
command = [java, '-Xms32m', '-Xmx96m', '-cp', classpath, 'RotationProbe',
           str(config), marker, '1500']
run = subprocess.run(command, capture_output=True, text=True, encoding='utf-8',
                     errors='replace', timeout=45)
(out / 'ejecucion-java.txt').write_text(run.stdout + run.stderr, encoding='utf-8')
print(run.stdout.strip(), flush=True)

files = sorted(logs.rglob('*.log')) + sorted(logs.rglob('*.log.gz'))
counts = Counter()
locations = {}
inventory = []
for f in files:
    text = gzip.open(f, 'rt', encoding='utf-8').read() if f.suffix == '.gz' else f.read_text(encoding='utf-8')
    found = []
    for line in text.splitlines():
        if f'monitor_test={marker} ' not in line:
            continue
        match = re.search(r'\bseq=(\d+) payload=(X+)\s*$', line)
        if not match or len(match.group(2)) != 384:
            raise RuntimeError('Se encontro un evento de prueba truncado o ilegible: ' + str(f))
        seq = int(match.group(1))
        counts[seq] += 1
        found.append(seq)
        locations.setdefault(seq, []).append(f.relative_to(out).as_posix())
    inventory.append({'Archivo': f.relative_to(out).as_posix(), 'Bytes': f.stat().st_size,
                      'Comprimido': f.suffix == '.gz', 'EventosPrueba': len(found),
                      'PrimerSeq': min(found) if found else '', 'UltimoSeq': max(found) if found else ''})

expected = set(range(1, 1501))
missing = sorted(expected - set(counts))
duplicates = {k: v for k, v in counts.items() if v > 1}
extra = sorted(set(counts) - expected)
archives = [f for f in files if f.suffix == '.gz']
first_archived = any(name.endswith('.gz') for name in locations.get(1, []))
last_active = any(name == 'logs-laboratorio/sga-backend.log' for name in locations.get(1500, []))
source_unchanged = hashlib.sha256(source_config.read_bytes()).hexdigest() == original_hash
ok = (run.returncode == 0 and len(archives) > 0 and sum(counts.values()) == 1500
      and not missing and not duplicates and not extra and first_archived
      and last_active and source_unchanged)

def write_csv(name, rows, columns):
    with (out / name).open('w', encoding='utf-8-sig', newline='') as stream:
        writer = csv.DictWriter(stream, fieldnames=columns)
        writer.writeheader()
        writer.writerows(rows)

write_csv('inventario-logs.csv', inventory, list(inventory[0]) if inventory else
          ['Archivo', 'Bytes', 'Comprimido', 'EventosPrueba', 'PrimerSeq', 'UltimoSeq'])
write_csv('secuencia-verificada.csv',
          [{'Secuencia': n, 'Apariciones': counts[n], 'Archivos': '; '.join(locations.get(n, []))}
           for n in range(1, 1501)], ['Secuencia', 'Apariciones', 'Archivos'])
result_text = (f'Se emitieron 1500 eventos y se recuperaron {sum(counts.values())}. '
               f'Archivos comprimidos: {len(archives)}; perdidos: {len(missing)}; '
               f'secuencias duplicadas: {len(duplicates)}. '
               f'Primer evento en archivo rotado: {first_archived}; ultimo en activo: {last_active}.')
result = {
    'Prueba': 3, 'CodigoPrueba': 'M03', 'Nombre': 'Rotacion de logs', 'Importancia': 'Baja',
    'Identificador': marker, 'InicioLima': start.isoformat(), 'FinLima': datetime.now(tz).isoformat(),
    'Ejecucion': 'REALIZADA: Logback real del JAR, configuracion copiada a laboratorio y 1500 eventos secuenciales.',
    'Herramientas': ['JDK Java 21 (java y javac)', 'SLF4J y Logback del JAR del proyecto',
                    'Python: gzip, busqueda de secuencias y CSV'],
    'Estado': 'EXITOSO' if ok else 'FALLO', 'Resultado': result_text,
    'Incidencia': '' if ok else 'No se cumplio al menos un criterio de rotacion o integridad; consultar el resultado detallado.',
    'VersionProyecto': version, 'Dependencias': [Path(n).name for n in libs],
    'EventosEmitidos': 1500, 'EventosRecuperados': sum(counts.values()), 'EventosUnicos': len(counts),
    'Perdidos': missing, 'Duplicados': duplicates, 'SecuenciasFueraDeRango': extra,
    'ArchivosComprimidos': len(archives), 'PrimerEventoArchivado': first_archived,
    'UltimoEventoEnActivo': last_active, 'ConfiguracionFuenteSinCambios': source_unchanged,
    'ConfiguracionJARIgualAFuente': embedded_config.replace(b'\r\n', b'\n') == original_bytes.replace(b'\r\n', b'\n'),
    'SHA256Jar': hashlib.sha256(jar.read_bytes()).hexdigest(), 'AdaptacionesLaboratorio': changes,
    'Alcance': 'Rotacion por tamano, compresion y busqueda local con las librerias reales del proyecto; limite de 64 KB solo en laboratorio. No prueba el limite productivo de 50 MB ni la retencion de 30/60 dias.',
    'Cambios': 'Ninguno en codigo, configuracion o logs del servicio activo. Solo archivos del laboratorio y evidencias.'
}
(out / 'resultado.json').write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
(out / 'resultado.txt').write_text(
    'PRUEBA 03 - ROTACION DE LOGS\n'
    f'ESTADO: {result["Estado"]}\nRESULTADO: {result_text}\n'
    f'HERRAMIENTAS: {"; ".join(result["Herramientas"])}\n'
    f'ALCANCE: {result["Alcance"]}\n'
    f'Primera secuencia: {locations.get(1, [])}\nUltima secuencia: {locations.get(1500, [])}\n'
    f'Configuracion fuente sin cambios: {source_unchanged}\n'
    'Las pruebas 04 a 15 siguen pendientes.\n', encoding='utf-8')
print(f'Estado: {result["Estado"]}\nResultado: {result_text}\nEvidencias: {out}', flush=True)
