from pathlib import Path
import json
from docx import Document
from docx.shared import Inches,Pt,RGBColor
from docx.enum.table import WD_TABLE_ALIGNMENT,WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

project=Path(r'C:\Users\asus\Music\TransAduanas')
registry=json.loads((project/'evidencias-monitoreo/registro-monitoreo.json').read_text(encoding='utf-8-sig'))
if len(registry['Resultados'])!=15:raise RuntimeError('Todavia no estan registrados los quince casos; no se genera un cierre incompleto.')
latest={r['CodigoPrueba']:r for r in registry['Resultados']}
def raw(code):return json.loads((project/latest[code]['Evidencia']).read_text(encoding='utf-8-sig'))
short={
'M01':'10 consultas HTTP 200 y diez registros localizables por fecha, servicio y endpoint. Búsqueda local de logs.',
'M02':'3 consultas HTTP 200. El mismo requestId aparece en Nginx y Java; fecha, servicio y nivel nativo Java disponibles.',
'M03':'1500 eventos recuperados, sin pérdidas ni duplicados; 13 archivos comprimidos. Rotación con umbral de 64 KiB en laboratorio.',
'M04':'60 consultas HTTP 200. Micrometer, Prometheus y Grafana registraron +60; tasa observada de 1 solicitud/s durante carga estable.',
'M05':'Java, Nginx y MySQL identificados por PID, servicio y puertos. Diez verificaciones correctas en dos muestras.',
'M06':'60 consultas HTTP 200. Histograma nativo p50/p95/p99 de 9,350 / 18,641 / 165,535 ms, coincidente en Prometheus y Grafana.',
'M07':'Consulta HTTP 200 correlacionada mediante el mismo requestId en Nginx, Java y tres ejecuciones JDBC MySQL completadas.',
'M08':'60 consultas HTTP 200 y siete muestras. Heap de 96,690 a 128,690 MiB; contadores GC disponibles, sin pausas nuevas durante la ventana.',
'M09':'60 consultas HTTP 200. CPU Windows del host de 9,62 a 16,45 %; memoria residente Java de 501,91 a 506,55 MiB. Tiempo CPU contrastado.',
'M10':'Tres cargas de 512 KiB, todas HTTP 201; tamaños y SHA-256 correctos. Nueve muestras de disco; latencia media por intervalo de 0,096 a 0,476 ms.',
'M11':'En Nginx aislado: cuatro rutas HTTP 200, fallos controlados 500/502 y recuperación a 200. Cuatro alertas activadas y cerradas; rutas habituales disponibles.',
'M12':'MySQL aislado detenido y recuperado. Catálogo HTTP 200 → 500 → 200 mientras /api/v1/health siguió UP. Alerta de negocio activada y cerrada.',
'M13':'Regla original de tasa 5xx >1 %. Firing tras 300,7 s desde activeAt, con tasa de 50 %. Recuperación hasta tasa 0 % y alerta inactiva.',
'M14':'CPU real del Windows compartido alrededor de 96 %. Firing tras 304,8 s desde activeAt; workers detenidos, CPU reducida y alerta inactiva.',
}
m15=raw('M15')
short['M15']=(f"Alerta tras 127,2 s sin ingesta; recuperación y cierre. Eventos de pausa recibidos: {m15['EventosPausaRecuperados']}/{m15['EventosDurantePausa']}. Dos eventos de la muestra tuvieron repetición; entrega al menos una vez.")
tools={
'M01':'Nginx; PowerShell.',
'M02':'Nginx; Logback; PowerShell.',
'M03':'Java 21; SLF4J/Logback; Python.',
'M04':'Actuator/Micrometer; Prometheus; Grafana; Nginx; Logback; Python.',
'M05':'PowerShell/CIM; Get-NetTCPConnection; nginx.pid.',
'M06':'Actuator/Micrometer; Prometheus; Grafana; Logback; Nginx; Python.',
'M07':'Nginx; Logback/MDC; trazabilidad JDBC; Python.',
'M08':'Actuator/Micrometer; Prometheus; Grafana; Python.',
'M09':'Win32/CIM; PowerShell; Actuator/Micrometer; Prometheus; Grafana; Python.',
'M10':'CIM/PerfDisk; Nginx; Logback; Actuator/Micrometer; Prometheus; Grafana; Python.',
'M11':'Nginx aislado; Blackbox Exporter; Prometheus; Grafana; Python.',
'M12':'MySQL 8.4; Java 21; Blackbox; Prometheus; Grafana; Logback/JDBC; Python/CIM.',
'M13':'Java 21; MySQL aislado; Micrometer; Prometheus aislado; Grafana; Logback; Python/CIM.',
'M14':'Python; Win32; Actuator/Micrometer; Prometheus; Grafana.',
'M15':'Alloy; Loki; Prometheus; Grafana; Logback; Nginx; Python.'}
names={r['CodigoPrueba']:r['Prueba'] for r in registry['Resultados']}
names.update({'M01':'Ingesta y búsqueda de logs','M03':'Rotación de logs','M07':'Correlación de una solicitud','M11':'Disponibilidad de páginas y API','M14':'Alerta por saturación de CPU','M15':'Interrupción de ingesta de logs'})
doc=Document();sec=doc.sections[0]
sec.page_width=Inches(8.5);sec.page_height=Inches(11)
sec.top_margin=sec.bottom_margin=sec.left_margin=sec.right_margin=Inches(1)
normal=doc.styles['Normal'];normal.font.name='Arial';normal.font.size=Pt(11);normal.font.color.rgb=RGBColor(0,0,0)
normal.paragraph_format.space_after=Pt(5);normal.paragraph_format.line_spacing=1.05
for name in ['Title','Heading 1','Caption']:
    style=doc.styles[name];style.font.name='Arial';style.font.color.rgb=RGBColor(0,0,0)
    style.font.size=Pt(15 if name=='Title' else 11);style.font.bold=name!='Caption'
    for border in list(style.element.iter(qn('w:pBdr'))):border.getparent().remove(border)
doc.styles['Caption'].font.italic=False
doc.add_paragraph('Resultados de monitoreo de TransAduanas','Title')
doc.add_paragraph('Pruebas ejecutadas del 20 al 21 de noviembre de 2026. La tabla de resultados presenta la última ejecución verificada de cada caso. Las incidencias y las evaluaciones iniciales se conservan en el registro y en las evidencias del proyecto.')
doc.add_paragraph('Se cubrieron logs, métricas de aplicación, procesos, CPU, memoria, disco y alertas. Los fallos controlados de los ensayos de alerta fueron estímulos previstos. M14 utilizó CPU real del host con autorización; M11, M12 y M13 utilizaron copias aisladas.')

def table(headers,widths):
    t=doc.add_table(rows=1,cols=len(headers));t.alignment=WD_TABLE_ALIGNMENT.CENTER;t.autofit=False
    tPr=t._tbl.tblPr
    borders=OxmlElement('w:tblBorders')
    for edge in ['top','left','bottom','right','insideH','insideV']:
        b=OxmlElement('w:'+edge);b.set(qn('w:val'),'single');b.set(qn('w:sz'),'4');b.set(qn('w:color'),'000000');borders.append(b)
    tPr.append(borders)
    ind=OxmlElement('w:tblInd');ind.set(qn('w:w'),'0');ind.set(qn('w:type'),'dxa');tPr.append(ind)
    margins=OxmlElement('w:tblCellMar')
    for edge,value in [('top','50'),('bottom','50'),('left','65'),('right','65')]:
        el=OxmlElement('w:'+edge);el.set(qn('w:w'),value);el.set(qn('w:type'),'dxa');margins.append(el)
    tPr.append(margins)
    for col,width in zip(t.columns,widths):col.width=Inches(width)
    for cell,word,width in zip(t.rows[0].cells,headers,widths):cell.width=Inches(width);cell.text=word
    repeat=OxmlElement('w:tblHeader');repeat.set(qn('w:val'),'true');t.rows[0]._tr.get_or_add_trPr().append(repeat)
    return t
def format_row(row,header=False):
    row._tr.get_or_add_trPr().append(OxmlElement('w:cantSplit'))
    for idx,cell in enumerate(row.cells):
        cell.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.CENTER
        for p in cell.paragraphs:
            p.paragraph_format.space_after=Pt(0);p.paragraph_format.space_before=Pt(0);p.paragraph_format.line_spacing=1.0
            p.alignment=WD_ALIGN_PARAGRAPH.CENTER if header or idx==0 or idx==len(row.cells)-1 else WD_ALIGN_PARAGRAPH.LEFT
            for run in p.runs:run.font.name='Arial';run.font.size=Pt(11);run.bold=header
def caption(number,title):
    p=doc.add_paragraph(style='Caption');p.paragraph_format.space_before=Pt(5);p.paragraph_format.space_after=Pt(5)
    p.add_run('Tabla ').bold=True
    fld=OxmlElement('w:fldSimple');fld.set(qn('w:instr'),' SEQ Tabla \\* ARABIC ')
    run=OxmlElement('w:r');pr=OxmlElement('w:rPr');pr.append(OxmlElement('w:b'));run.append(pr);text=OxmlElement('w:t');text.text=str(number);run.append(text);fld.append(run);p._p.append(fld)
    p.add_run('. '+title).italic=True
    return p
doc.add_paragraph('Resultados de las pruebas','Heading 1')
t=table(['Código y prueba','Herramientas utilizadas','Resultado obtenido','Estado'],[1.15,1.55,3.05,.75]);format_row(t.rows[0],True)
for i in range(1,16):
    code=f'M{i:02}';row=t.add_row();values=[code+'\n'+names[code],tools[code],short[code],latest[code]['Estado']]
    for cell,value,width in zip(row.cells,values,[1.15,1.55,3.05,.75]):cell.width=Inches(width);cell.text=value
    format_row(row)
    for p in row.cells[0].paragraphs:
        for run in p.runs:run.font.size=Pt(10.5)
caption(1,'Resultados de las 15 pruebas de monitoreo de TransAduanas')
p=doc.add_paragraph();p.add_run('Nota. ').italic=True
p.add_run('El estado corresponde al alcance observado. M03 usó rotación de 64 KiB en laboratorio; M06 presenta percentiles aproximados del histograma. M08 no registró pausas GC nuevas. En M09 y M10, parte de la medición del sistema se registró en CSV nativos. La latencia de disco corresponde al volumen completo, no solo a las cargas. No se validaron notificaciones externas ni un SLA de capacidad.')

inc_heading=doc.add_paragraph('Incidencias detectadas y verificadas','Heading 1')
inc_heading.paragraph_format.page_break_before=True
t2=table(['Código','Incidencia y corrección','Estado'],[.62,4.92,.96]);format_row(t2.rows[0],True)
compact={
'INC-M01-001':'Instrumentación. Los registros de acceso no identificaban el servicio. Se ajustó el formato Nginx y se verificaron diez consultas correctas.',
'INC-M02-001':'Instrumentación. Java carecía de servicio, requestId y finalización HTTP. Se añadió el filtro y se verificaron tres consultas. El nivel de access.log de Nginx no es requisito.',
'INC-M07-001':'Instrumentación. La correlación terminaba en el backend, sin ejecución JDBC vinculada. Se añadió trazabilidad JDBC sin parámetros ni datos personales y se repitió M07 con éxito.',
'INC-M09-PREP-001':'Preparación. Windows denegó la lectura directa del proceso del servicio. Se utilizaron los contadores CIM accesibles y se contrastaron con Micrometer.',
'INC-M09-EJEC-001':'Ejecución. UAC inició tarde un segundo generador. Se invalidaron las observaciones y se repitió con bloqueo de ejecución e incremento HTTP de exactamente 60.',
'INC-M11-PREP-001':'Preparación. Faltaba el directorio temporal del Nginx aislado. Se creó antes de la simulación. El resumen inicial inválido se conservó para auditoría.',
'INC-M12-PREP-001':'Preparación. PowerShell 5.1 no cargaba el módulo para aplicar la ACL temporal. Se corrigió mediante .NET. Los intentos iniciales no llegaron a provocar la caída de MySQL.',
'INC-M13-EVAL-001':'Evaluación. Una lectura anterior a la primera recolección se contó como caída. Se reevaluaron 46 muestras desde la línea base, todas up=1, conservando el informe inicial.',
'INC-M15-001':'Ejecución. Un timeout dejó sin verificar el backlog. Se añadieron reintentos y se repitió: 25/25 eventos recuperados. La causa raíz sigue sin determinar.',
'INC-M15-PREP-001':'Preparación. El destino Windows Event Log de Alloy requería permisos de administrador. Se configuró stderr con salida a archivo y se verificó el arranque sin elevación.',
'INC-M15-PREP-002':'Preparación. Loki anunciaba una IP LAN, pero gRPC solo escuchaba en loopback; las consultas expiraban. Se corrigió la dirección interna a 127.0.0.1 y se verificó la búsqueda.',
'INC-M15-EJEC-001':'Ejecución. El lanzador PowerShell no devolvía el control al iniciar Alloy. Se sustituyó por Popen directo con salidas a archivo y se repitió la prueba.',
}
for item in sorted(registry['Incidencias'],key=lambda r:(int(r['CodigoPrueba'][1:]),0 if 'PREP' in r['CodigoIncidencia'] else 1 if 'EJEC' in r['CodigoIncidencia'] else 2)):
    code=item['CodigoPrueba'];identifier=item['CodigoIncidencia'];row=t2.add_row()
    desc=compact.get(identifier,item['Incidencia'])
    for cell,value,width in zip(row.cells,[code,desc,item['EstadoIncidencia']],[.62,4.92,.96]):cell.width=Inches(width);cell.text=value
    format_row(row)
caption(2,'Incidencias de instrumentación y ejecución de las pruebas de monitoreo')
p=doc.add_paragraph();p.add_run('Nota. ').italic=True
p.add_run('Las incidencias de preparación, ejecución o evaluación no se presentan como fallos funcionales del proyecto. Los informes originales, correcciones y verificaciones permanecen en evidencias-monitoreo. Los HTTP 500/502 previstos y la carga CPU autorizada no son incidencias inesperadas.')
doc.core_properties.title='Resultados de monitoreo de TransAduanas';doc.core_properties.subject='Resultados e incidencias de quince pruebas de monitoreo'
doc.core_properties.author='Equipo TransAduanas';doc.core_properties.comments=''
for style in doc.styles:
    for border in list(style.element.iter(qn('w:pBdr'))):border.getparent().remove(border)
out=project/'docs/pruebas/monitoreo/Resultados_e_incidencias_monitoreo_TransAduanas.docx'
doc.save(out)
print(out)
