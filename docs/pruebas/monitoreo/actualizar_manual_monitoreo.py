"""Actualiza las instrucciones de preparacion conservando el manual existente."""
from copy import deepcopy
from docx.shared import Pt

CPU_PERMISO = '''$grupo = Get-LocalGroup -SID 'S-1-5-32-558'
$cuenta = 'NT SERVICE\\TransAduanasBackend'
if ((Get-LocalGroupMember -Group $grupo.Name).Name -notcontains $cuenta) {
    Add-LocalGroupMember -Group $grupo.Name -Member $cuenta
}
Restart-Service -Name TransAduanasBackend -ErrorAction Stop
Get-Service -Name TransAduanasBackend'''

CPU_LECTURA = '''$r = Invoke-WebRequest -UseBasicParsing -TimeoutSec 10 -Uri `
  'http://127.0.0.1:9086/actuator/prometheus'
$r.Content -split "`n" | Where-Object {
    $_ -match '^(system_cpu_usage|process_cpu_usage)\\{'
}'''


def _text(p, value):
    if p.runs:
        p.runs[0].text = value
        for run in p.runs[1:]:
            run.text = ''
    else:
        p.add_run(value)


def actualizar_manual(doc):
    for p in doc.paragraphs:
        if p.text.startswith('Revisión explicada del '):
            _text(p, 'Revisión del 26 de noviembre de 2026. Proyecto ubicado en C:\\Users\\asus\\Music\\TransAduanas. Backend de referencia 0.5.5.')
        elif p.text.startswith('Cada prueba incluye un título sencillo,'):
            _text(p, 'Cada prueba incluye un título sencillo, qué hace, las herramientas utilizadas, el comando, el resultado esperado y qué mostrar como evidencia. Se mantienen las quince pruebas y sus criterios de evaluación.')
        elif p.text.startswith('Los quince casos cuentan con una última ejecución EXITOSO'):
            _text(p, 'Se conservan las ejecuciones y las incidencias de las quince pruebas. La revisión del 26 de noviembre comprobó requisitos y repitió algunos casos; no equivale a ejecutar de nuevo los quince escenarios completos.')
        elif p.text.startswith('Preparación y comandos: páginas 2 y 3.'):
            if 'Correcciones antes de repetir' not in p.text:
                _text(p, p.text + '\nCorrecciones antes de repetir: página 21.')

    notes = {
        'M04': 'Si el contador aún no existe, hace una consulta de preparación con un identificador separado. Después espera que backend, Prometheus y Grafana coincidan. Esa consulta queda fuera de las 60 medidas. Si no se sincronizan, informa NO_EJECUTADA.',
        'M09': 'Antes de las 60 consultas comprueba que la CPU esté disponible. Un valor -1 indica que falta la medición; la prueba no inicia la carga. Revisa el permiso de la cuenta del servicio como se explica en la página 21.',
        'M10': 'La ruta de almacenamiento se lee de la configuración actual del backend. El archivo leer_almacenamiento_backend.py debe acompañar al ejecutor; admite el XML anterior y la configuración protegida, sin mostrar credenciales.',
        'M14': 'Antes de generar carga comprueba que la CPU tenga un valor válido. Si la medición deja de ser válida durante el escenario, detiene la carga. Consulta la página 21 si aparece NO_EJECUTADA por CPU no disponible.',
    }
    headings = [p for p in doc.paragraphs if p.style.name == 'Heading 1']
    for heading in headings:
        code = heading.text.split(' ', 1)[0]
        if code not in notes:
            continue
        paragraphs = doc.paragraphs
        start = paragraphs.index(next(p for p in paragraphs if p._p is heading._p))
        end = next((i for i in range(start+1,len(paragraphs)) if paragraphs[i].style.name=='Heading 1'),len(paragraphs))
        section = paragraphs[start:end]
        if notes[code] not in [p.text for p in section]:
            command = next(p for p in section if p.text == 'Comando')
            added = command.insert_paragraph_before(notes[code], style='Normal')
            normal = next(p for p in section if p.style.name=='Normal' and p.text)
            if normal._p.pPr is not None:
                old = added._p.pPr
                if old is not None:
                    added._p.remove(old)
                added._p.insert(0, deepcopy(normal._p.pPr))
        if code == 'M10':
            for i,p in enumerate(section[:-1]):
                if p.text == 'Herramientas utilizadas':
                    toolp = section[i+1]
                    if 'PowerShell y DPAPI' not in toolp.text:
                        toolp.add_run(' PowerShell y DPAPI: permiten leer la ruta de almacenamiento protegida.').font.size=Pt(11)
                    break

    title = 'Correcciones antes de repetir'
    if title not in [p.text for p in doc.paragraphs]:
        h = doc.add_paragraph(title,style='Heading 1')
        h.paragraph_format.page_break_before = True
        doc.add_paragraph('Aplica la corrección que corresponda y repite solo el caso afectado. Conserva el intento anterior y comprueba la fecha de la nueva evidencia.')
        doc.add_paragraph('Si M09 o M14 no obtiene la CPU',style='Heading 2')
        doc.add_paragraph('Si las métricas muestran -1 y la cuenta del servicio no tiene acceso a los contadores, abre PowerShell como administrador y ejecuta este bloque una sola vez. Se añade acceso de lectura a los contadores y se reinicia únicamente el backend; espera a que vuelva a estar disponible.')
        _code(doc,CPU_PERMISO)
        doc.add_paragraph('Debe mostrar Running. Luego comprueba las dos métricas:')
        _code(doc,CPU_LECTURA)
        doc.add_paragraph('Ambas deben mostrar valores entre 0 y 1, por ejemplo 0.15 equivale a 15 %. Si siguen en -1, no repitas la prueba todavía: conserva el diagnóstico y revisa el servicio. No cambies el dato a cero ni reduzcas el umbral.')
        doc.add_paragraph('Preparación de M04 y M10',style='Heading 2')
        doc.add_paragraph('M04 puede crear primero el contador de su ruta. Esa consulta de preparación no se cuenta entre las 60 ni debe mezclarse con sus registros. M10 obtiene la ruta real de almacenamiento mediante su archivo auxiliar; no necesita volver a guardar contraseñas en el XML.')
        doc.add_paragraph('Qué significa NO_EJECUTADA',style='Heading 2')
        doc.add_paragraph('Indica que faltó un requisito antes de comenzar la medición. Si el escenario ya comenzó y no cumple sus verificaciones, su estado corresponde a FALLO. PREPARADO en M14 solo comprueba requisitos: no acredita la alerta con carga real.')
        doc.add_paragraph('Qué se verificó el 26 de noviembre',style='Heading 2')
        doc.add_paragraph('M01, M02, M03, M05 y M09 se repitieron con resultado exitoso; M04 también se corrigió y repitió con éxito. De M10 se verificó la lectura del almacenamiento y de M14 la preparación sin carga. Los demás escenarios completos no se repitieron en esa revisión.')
    return [CPU_PERMISO,CPU_LECTURA]


def _code(doc,text):
    p=doc.add_paragraph()
    p.paragraph_format.space_before=Pt(0)
    p.paragraph_format.space_after=Pt(6)
    p.paragraph_format.line_spacing=1
    for i,line in enumerate(text.splitlines()):
        if i: p.add_run('\n')
        run=p.add_run(line)
        run.font.name='Consolas'
        run.font.size=Pt(8.5)
