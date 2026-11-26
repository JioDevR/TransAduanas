from pathlib import Path
import csv
import json
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('--proyecto', default=r'C:\Users\asus\Music\TransAduanas')
args = parser.parse_args()
root = Path(args.proyecto)
base = root / 'evidencias-monitoreo'
revision_path = base / 'revisiones-criterio.json'
revisiones = json.loads(revision_path.read_text(encoding='utf-8-sig')) if revision_path.exists() else {}
historial = []
for path in base.glob('M[0-9][0-9]-*/resultado.json'):
    r = json.loads(path.read_text(encoding='utf-8-sig'))
    n = int(r['Prueba'])
    historial.append({
        'CodigoPrueba': f'M{n:02}',
        'Prueba': r['Nombre'],
        'Ejecucion': r['Identificador'],
        'FechaLima': r['InicioLima'],
        'Herramientas': '; '.join(r['Herramientas']),
        'Resultado': r['Resultado'],
        'Estado': 'Exitoso' if r['Estado'] == 'EXITOSO' else 'Fallo',
        'Evidencia': path.relative_to(root).as_posix(),
        'Incidencia': r.get('Incidencia', ''),
        'Alcance': r.get('Alcance', ''),
        'RevisionCriterio': revisiones.get(f'M{n:02}', {}).get('Aclaracion', ''),
    })
historial.sort(key=lambda r: (r['CodigoPrueba'], r['FechaLima']))
codigos = sorted({r['CodigoPrueba'] for r in historial})
resultados, incidencias = [], []
for codigo in codigos:
    rondas = [r for r in historial if r['CodigoPrueba'] == codigo]
    ultimo = rondas[-1]
    resultados.append({k: ultimo[k] for k in (
        'CodigoPrueba', 'Prueba', 'Herramientas', 'Resultado',
        'Estado', 'Ejecucion', 'FechaLima', 'Evidencia', 'Alcance')})
    fallos = [r for r in rondas if r['Estado'] == 'Fallo']
    if fallos:
        inicial = fallos[0]
        revision = revisiones.get(codigo, {})
        descripcion = revision.get('DescripcionValidada') or inicial['Incidencia'] or inicial['Resultado']
        cerrada = ultimo['Estado'] == 'Exitoso' and ultimo['FechaLima'] > fallos[-1]['FechaLima']
        incidencias.append({
            'CodigoIncidencia': f'INC-{codigo}-001',
            'CodigoPrueba': codigo,
            'Incidencia': descripcion,
            'EstadoIncidencia': revision.get('EstadoAlCerrar', 'Corregida y verificada') if cerrada else 'Abierta',
            'EjecucionDeteccion': inicial['Ejecucion'],
            'EjecucionVerificacion': ultimo['Ejecucion'] if cerrada else '',
            'EvidenciaDeteccion': inicial['Evidencia'],
            'EvidenciaVerificacion': ultimo['Evidencia'] if cerrada else '',
            'AclaracionEvaluacion': revision.get('Aclaracion', ''),
        })

# Preserve preparation/execution incidents without turning them into application test failures.
extra_path = base / 'incidencias-ejecucion.json'
if extra_path.exists():
    incidencias.extend(json.loads(extra_path.read_text(encoding='utf-8-sig')))

def guardar_csv(nombre, rows):
    if not rows:
        return
    with (base / nombre).open('w', encoding='utf-8-sig', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)

guardar_csv('resultados-pruebas.csv', resultados)
guardar_csv('incidencias-pruebas.csv', incidencias)
guardar_csv('historial-ejecuciones.csv', historial)
registro = {
    'Criterio': 'Resultados por prueba: ultima ejecucion. Historial: todas las ejecuciones. Incidencias: se conservan incluso despues de corregidas.',
    'Resultados': resultados,
    'Incidencias': incidencias,
    'Historial': historial,
    'Pendientes': [f'M{i:02}' for i in range(1, 16) if f'M{i:02}' not in codigos],
}
(base / 'registro-monitoreo.json').write_text(json.dumps(registro, ensure_ascii=False, indent=2), encoding='utf-8')
print(f'Pruebas evaluadas: {len(resultados)}; ejecuciones conservadas: {len(historial)}; incidencias: {len(incidencias)}.')
for r in resultados:
    print(r['CodigoPrueba'], r['Estado'], r['Ejecucion'])
for i in incidencias:
    print(i['CodigoIncidencia'], i['EstadoIncidencia'])
