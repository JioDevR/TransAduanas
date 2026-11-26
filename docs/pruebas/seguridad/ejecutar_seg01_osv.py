"""SEG-01: inventory Maven dependencies and query public OSV advisories.

Only public package coordinates/versions leave the machine. No source, secrets,
user records or authentication material are uploaded. Does not alter the POM.
"""
from pathlib import Path
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import csv
import hashlib
import json
import sys
import time
import re
import urllib.request
import urllib.parse

evidence = Path(sys.argv[1]).resolve()
tree = json.loads((evidence / 'dependencias-arbol.json').read_text(encoding='utf-8-sig'))
started = datetime.now(timezone.utc).isoformat()

def save(name, obj):
    (evidence / name).write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

packages = {}
def walk(node, parents):
    coord = f"{node['groupId']}:{node['artifactId']}"
    key = (coord, node['version'])
    item = packages.setdefault(key, {
        'dependencia': coord, 'version': node['version'], 'ambitos': [],
        'directa': len(parents) == 0, 'rutas': [],
    })
    if node.get('scope', '') not in item['ambitos']:
        item['ambitos'].append(node.get('scope', ''))
    path = parents + [f"{coord}:{node['version']}"]
    if path not in item['rutas']:
        item['rutas'].append(path)
    for child in node.get('children', []):
        walk(child, path)

for child in tree.get('children', []):
    walk(child, [])
inventory = sorted(packages.values(), key=lambda p: (p['dependencia'], p['version']))
save('inventario-dependencias.json', inventory)
with (evidence / 'inventario-dependencias.csv').open('w', encoding='utf-8-sig', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=['dependencia', 'version', 'ambitos', 'directa'])
    writer.writeheader()
    for p in inventory:
        writer.writerow({k: '; '.join(p[k]) if k == 'ambitos' else p[k]
                         for k in writer.fieldnames})
print(f"Inventario: {len(inventory)} coordenadas/versiones unicas", flush=True)

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise ValueError('No se permiten redirecciones de las consultas OSV.')

def request(url, payload=None):
    parsed = urllib.parse.urlparse(url)
    if parsed.scheme != 'https' or parsed.hostname != 'api.osv.dev':
        raise ValueError('Solo se permite el destino OSV autorizado.')
    if payload is not None:
        if parsed.path != '/v1/querybatch' or set(payload) != {'queries'}:
            raise ValueError('Operacion de consulta no autorizada.')
        for query in payload['queries']:
            if set(query) - {'package', 'version', 'page_token'}:
                raise ValueError('Campo adicional no permitido.')
            if set(query['package']) != {'ecosystem', 'name'} or query['package']['ecosystem'] != 'Maven':
                raise ValueError('Paquete inesperado.')
            if not re.fullmatch(r'[A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+', query['package']['name']):
                raise ValueError('Coordenada Maven invalida.')
    body = None if payload is None else json.dumps(payload).encode('utf-8')
    req = urllib.request.Request(url, data=body, headers={
        'Content-Type': 'application/json',
        'User-Agent': 'DependencyReview/1.0',
    })
    for attempt in range(3):
        try:
            with urllib.request.build_opener(NoRedirect()).open(req, timeout=35) as response:
                return json.load(response)
        except Exception:
            if attempt == 2:
                raise
            time.sleep(1 + attempt)

queries = [{'package': {'ecosystem': 'Maven', 'name': p['dependencia']}, 'version': p['version']}
           for p in inventory]
save('osv-consulta-inicial.json', {'queries': queries})
matches = [[] for _ in queries]
pending = list(enumerate(queries))
page = 0
while pending:
    if page > 20:
        raise RuntimeError('Se alcanzo el limite de paginacion; resultado inconcluso.')
    payload = {'queries': [q for _, q in pending]}
    result = request('https://api.osv.dev/v1/querybatch', payload)
    save(f'osv-consulta-pagina-{page}.json', payload)
    save(f'osv-respuesta-pagina-{page}.json', result)
    if len(result.get('results', [])) != len(pending):
        raise RuntimeError('La respuesta OSV no cubre todas las consultas.')
    next_pending = []
    for (index, query), entry in zip(pending, result['results']):
        matches[index].extend(v['id'] for v in entry.get('vulns', []))
        if entry.get('next_page_token'):
            next_pending.append((index, {**query, 'page_token': entry['next_page_token']}))
    pending = next_pending
    page += 1

ids = sorted({v for group in matches for v in group})
print(f"OSV: {len(ids)} avisos distintos encontrados; descargando detalles", flush=True)
(evidence / 'avisos-osv').mkdir(exist_ok=True)
def fetch(advisory_id):
    if not re.fullmatch(r'[A-Za-z0-9-]+', advisory_id):
        raise ValueError('Identificador de aviso invalido.')
    advisory = request('https://api.osv.dev/v1/vulns/' + urllib.parse.quote(advisory_id, safe=''))
    if advisory.get('id') != advisory_id:
        raise RuntimeError('Identificador de aviso inesperado.')
    save('avisos-osv/' + advisory_id + '.json', advisory)
    return advisory_id, advisory

with ThreadPoolExecutor(max_workers=4) as pool:
    advisories = dict(pool.map(fetch, ids))

severity_order = {'CRITICAL': 5, 'HIGH': 4, 'MODERATE': 3, 'MEDIUM': 3, 'LOW': 2, 'UNKNOWN': 0}
severity_es = {'CRITICAL': 'Critica', 'HIGH': 'Alta', 'MODERATE': 'Media', 'MEDIUM': 'Media', 'LOW': 'Baja', 'UNKNOWN': 'No determinada'}
findings = []
for package, advisory_ids in zip(inventory, matches):
    for aid in sorted(set(advisory_ids)):
        advisory = advisories[aid]
        if advisory.get('withdrawn'):
            continue
        relevant = [a for a in advisory.get('affected', [])
                    if a.get('package', {}).get('ecosystem') == 'Maven'
                    and a.get('package', {}).get('name') == package['dependencia']]
        if not relevant:
            raise RuntimeError(f"El aviso {aid} no identifica el paquete consultado.")
        levels = [advisory.get('database_specific', {}).get('severity', 'UNKNOWN')]
        levels += [a.get('database_specific', {}).get('severity', 'UNKNOWN') for a in relevant]
        level = max(levels, key=lambda v: severity_order.get(v, 0))
        if level not in severity_order:
            level = 'UNKNOWN'
        fixes = sorted({event['fixed'] for a in relevant for ran in a.get('ranges', [])
                        for event in ran.get('events', []) if 'fixed' in event})
        cvss = advisory.get('severity', [])
        runtime = bool(set(package['ambitos']) & {'compile', 'runtime'})
        findings.append({
            'dependencia': package['dependencia'], 'version': package['version'],
            'ambitos': package['ambitos'], 'directa': package['directa'],
            'id_osv': aid, 'cve': sorted(a for a in advisory.get('aliases', []) if a.startswith('CVE-')),
            'aliases': advisory.get('aliases', []),
            'severidad_fuente': level, 'severidad': severity_es[level], 'cvss': cvss,
            'resumen': advisory.get('summary', 'Sin resumen'),
            'versiones_corregidas_segun_aviso': fixes,
            'estado': 'Pendiente de validar aplicabilidad; sin correccion aplicada',
            'plan': ('Priorizar actualizacion compatible de la dependencia o de su dependencia padre; '
                     'revisar el aviso y repetir SCA y pruebas. No actualizar automaticamente.'
                     if runtime else 'Revisar actualizacion compatible del entorno de pruebas; no equiparar exposicion de test con produccion.'),
            'ruta': package['rutas'], 'fuente': 'https://osv.dev/vulnerability/' + aid,
            'referencias': advisory.get('references', []),
        })
findings.sort(key=lambda p: (-severity_order[p['severidad_fuente']], p['dependencia'], p['id_osv']))
save('hallazgos.json', findings)
fields = ['dependencia', 'version', 'ambitos', 'directa', 'id_osv', 'cve', 'severidad',
          'resumen', 'versiones_corregidas_segun_aviso', 'estado', 'plan', 'fuente']
with (evidence / 'tabla-vulnerabilidades.csv').open('w', encoding='utf-8-sig', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=fields)
    writer.writeheader()
    for p in findings:
        writer.writerow({key: '; '.join(p[key]) if isinstance(p[key], list) else p[key] for key in fields})

summary = {
    'prueba': 'SEG-01', 'nombre': 'Dependencias vulnerables del backend',
    'fecha_inicio_utc': started, 'fecha_fin_utc': datetime.now(timezone.utc).isoformat(),
    'proyecto': f"{tree['groupId']}:{tree['artifactId']}:{tree['version']}",
    'herramientas': ['Maven Dependency Plugin 3.10.0', 'API publica OSV.dev', 'Python estandar'],
    'dependencias_unicas': len(inventory),
    'dependencias_ejecucion': sum(bool(set(p['ambitos']) & {'compile', 'runtime'}) for p in inventory),
    'dependencias_solo_test': sum(set(p['ambitos']) == {'test'} for p in inventory),
    'dependencias_con_hallazgos': len({(p['dependencia'], p['version']) for p in findings}),
    'asociaciones_dependencia_aviso': len(findings),
    'avisos_osv_no_retirados': len({p['id_osv'] for p in findings}),
    'cve_distintos': sorted({v for p in findings for v in p['cve']}),
    'asociaciones_por_severidad': dict(Counter(p['severidad'] for p in findings)),
    'analisis_completo': True,
    'resultado': 'CON HALLAZGOS' if findings else 'SIN AVISOS CONOCIDOS EN OSV PARA LAS VERSIONES CONSULTADAS',
    'limites': [
        'Coincidencias SCA no demuestran explotabilidad ni compromiso del sistema.',
        'Las versiones corregidas son las publicadas en los rangos del aviso; no se aplican automaticamente ni garantizan compatibilidad.',
        'Sin coincidencias no significa ausencia de vulnerabilidades o cobertura completa de la base.',
        'Solo dependencias del arbol Maven; no plugins de construccion, SO, JDK, contenedores o frontends.',
        'No equivale a OWASP Dependency-Check, Snyk ni una consulta completa de NVD.',
    ],
}
save('resultado.json', summary)
print(json.dumps(summary, ensure_ascii=False, indent=2), flush=True)
for row in findings[:20]:
    print(f"{row['severidad']} | {row['dependencia']}:{row['version']} | {','.join(row['cve']) or row['id_osv']}", flush=True)
