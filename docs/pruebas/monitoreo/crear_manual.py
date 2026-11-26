from pathlib import Path
import json
from contenido_didactico import D
from contenido_breve import B
from resultados_y_muestras import E
from herramientas_por_prueba import H
from actualizar_manual_monitoreo import actualizar_manual
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

ROOT = Path(r'C:\Users\asus\Music\TransAduanas')
OUT = ROOT / 'docs/pruebas/monitoreo'
BASE = ROOT / 'evidencias-monitoreo'
CASES = [
('M01','Ingesta y búsqueda de logs','Baja','Menos de 1 minuto',
 'PowerShell, HTTP y access.log de Nginx.',
 'Mantén Nginx y el backend disponibles. El ejecutor envía diez consultas identificadas y busca sus registros en el archivo de acceso real.',
 'Diez respuestas HTTP 200 y diez eventos encontrados con fecha, servicio y endpoint. Revisa que correspondan al identificador de esta ejecución.',
 'consultas.csv; eventos-nginx.log; resultado.json.',
 'Demostramos que las solicitudes reales dejan evidencia consultable. Relacionamos cada respuesta con su registro y comprobamos que no faltó ninguno de los diez eventos.',
 '10 de 10 respuestas 200 y 10 eventos encontrados.',
 'Esta prueba comprueba ingesta y búsqueda en el archivo local. La centralización en Loki y su recuperación se comprueban en M15.'),
('M02','Estructura de los registros','Baja','Menos de 1 minuto',
 'PowerShell, filtro HTTP de Java y logs de Nginx y backend.',
 'Ejecuta las tres consultas del caso. Compara el identificador de solicitud de la respuesta con las líneas del proxy y del backend.',
 'HTTP 200 en las tres consultas. El log Java debe incluir fecha, nivel INFO, servicio, endpoint e identificador; este último debe coincidir entre las capas.',
 'campos-verificados.csv; eventos-backend.log; eventos-nginx.log.',
 'La estructura permite buscar una misma operación entre componentes. El identificador enlaza proxy y Java; el nivel de severidad corresponde al registro de aplicación.',
 'Tres respuestas 200 con correlación y campos válidos.',
 'El access.log de Nginx es un registro de acceso y no necesita presentar INFO como un log de aplicación. Esta precisión del criterio quedó conservada en el historial.'),
('M03','Rotación de logs','Baja','Aproximadamente 1 minuto',
 'Java 21, javac, SLF4J, Logback y Python.',
 'Comprueba java y javac en la preparación. El script extrae las librerías del JAR y ejecuta un emisor aislado con 1500 mensajes y un límite de rotación reducido a 64 KB.',
 'Archivos rotados y comprimidos en logs-laboratorio. La verificación de secuencia debe mostrar todos los mensajes sin pérdidas ni duplicados.',
 'inventario-logs.csv; secuencia-verificada.csv; logback-laboratorio.xml; carpeta logs-laboratorio.',
 'Aceleramos la rotación en un laboratorio con las librerías reales. Así verificamos el cambio de archivos y la conservación de los mensajes sin llenar el log operativo.',
 '1500 mensajes INFO; 13 archivos gzip; cero mensajes perdidos o duplicados.',
 'El límite de laboratorio es 64 KB. No se demostró llenar 50 MB en producción ni esperar los días de retención configurados.'),
('M04','Conteo de solicitudes','Baja','Entre 1 y 2 minutos',
 'Python, Actuator y Micrometer, Prometheus y Grafana.',
 'Deja terminar sesenta lecturas durante unos sesenta segundos. El lanzador utiliza el catálogo público tipos-carga para separar el tráfico de prueba del sondeo automático de puertos.',
 'Sesenta HTTP 200 y aumento de 60 en el contador del endpoint, contrastado con las métricas nativas, Prometheus y la consulta a Grafana. Revisa también la tasa de la ventana estable.',
 'consultas.csv; conteos-por-solicitud.csv; panel-prometheus-grafana.csv.',
 'Comparamos un contador antes y después de una carga conocida. El aumento debe coincidir con las sesenta solicitudes y comprobarse también en las herramientas de monitoreo.',
 'En la ejecución registrada sobre puertos: contador de 12 a 72 y tasa estable de 1 solicitud por segundo.',
 'El contador absoluto cambiará en cada repetición. El panel global de cinco minutos incluye otras rutas; no equivale al contador filtrado de este caso. La opción tipos-carga conserva el criterio de 60 lecturas.'),
('M05','Inventario de procesos','Baja','Menos de 1 minuto',
 'PowerShell, Windows CIM, servicios y conexiones TCP.',
 'El ejecutor toma dos inventarios con dos segundos de separación. Relaciona los servicios, sus supervisores y procesos hijos con los puertos de MySQL, Java y Nginx.',
 'Las diez verificaciones deben cumplirse. Los PID deben ser coherentes dentro de esta ejecución y los puertos deben pertenecer al proceso esperado.',
 'inventario-1.csv; inventario-2.csv; inventario-completo.csv.',
 'No basta con encontrar un proceso java.exe. Vinculamos el servicio con su proceso real y su puerto para saber qué instancia sostiene la aplicación.',
 'Dos muestras de procesos y diez verificaciones correctas.',
 'Los PID pueden cambiar al reiniciar Windows o un servicio. No deben coincidir con los números de la evidencia histórica.'),
('M06','Latencia de consultas','Media','Entre 1 y 2 minutos',
 'Python, HTTP autenticado, Micrometer, Prometheus y Grafana.',
 'Introduce un usuario interno del SGA cuando se solicite; la contraseña queda oculta. Se realizan sesenta GET de Clientes, aproximadamente uno por segundo.',
 'Sesenta respuestas 200. Revisa percentiles p50, p95 y p99, las duraciones del cliente y del backend y el histograma. Los datos de Prometheus y Grafana deben corresponder a su misma consulta.',
 'percentiles.csv; comparacion-cliente-backend.csv; histograma-delta.csv; panel-prometheus-grafana.csv.',
 'Medimos la distribución de tiempos, no solo un promedio. El cliente incluye red y proxy, mientras el backend mide su procesamiento; por eso las duraciones pueden diferir.',
 'Histograma nativo de GET Clientes 200: p50 9,350 ms; p95 18,641 ms; p99 165,535 ms. Cliente de la muestra: 40,906; 54,513; 170,180 ms.',
 'El histograma nativo es acumulado desde el arranque y aproxima percentiles por intervalos. Los percentiles del cliente usan las sesenta lecturas. Estos valores son observaciones, no un SLA aprobado.'),
('M07','Correlación de una solicitud','Media','Menos de 1 minuto',
 'Python, X-Request-ID, Nginx, filtro Java e instrumentación JDBC.',
 'Usa una cuenta del SGA. El script realiza una consulta de Clientes y busca el mismo identificador en la respuesta, el proxy, Java y los eventos JDBC reales.',
 'Respuesta 200, identificador coincidente y eventos executeQuery de MySQL asociados a esa solicitud. instrumentacion-activa.json debe confirmar el despliegue con la corrección JDBC.',
 'eventos-nginx.log; eventos-backend.log; instrumentacion-activa.json.',
 'Seguimos una operación desde el proxy hasta sus consultas JDBC mediante un identificador común. Esto permite ubicar en qué capa ocurrió un problema.',
 'Una respuesta 200 y tres eventos JDBC executeQuery correlacionados.',
 'La instrumentación está incluida en el backend 0.5.5. Es correlación por registros estructurados; no se implementó una plataforma de trazas distribuidas como Tempo. No se registra el texto SQL ni sus parámetros.'),
('M08','Memoria JVM y pausas de GC','Media','Entre 1 y 2 minutos',
 'Python, métricas JVM de Micrometer, Prometheus y Grafana.',
 'Mantén la carga normal de sesenta lecturas y deja que el script recoja siete muestras. Observa uso de heap, áreas de memoria y contadores de recolección de basura.',
 'Métricas de memoria y GC disponibles y coherentes entre fuentes. La ausencia de nuevas pausas durante una muestra corta es válida si los contadores existen y se consultaron correctamente.',
 'muestras-prometheus-grafana.csv; metricas-antes.prom; metricas-despues.prom.',
 'Vigilamos cuánta memoria utiliza la JVM y si aparecen pausas de recolección. En este intervalo no fue necesario que el recolector realizara un nuevo ciclo.',
 'Siete muestras; heap entre 96,690 y 128,690 MiB; cero nuevas pausas de GC.',
 'Heap es una parte de la memoria de Java. Un minuto de observación no demuestra ausencia de fugas ni capacidad máxima; tampoco se forzó un GC para provocar un evento.'),
('M09','CPU y memoria del sistema','Media','Entre 1 y 2 minutos',
 'Python, Windows CIM y contadores nativos, Micrometer, Prometheus y Grafana.',
 'El lanzador envía sesenta lecturas a tipos-carga y recoge siete muestras. El ejecutor puede usar CIM si Windows restringe la apertura directa del proceso Java.',
 'CPU del host y del proceso Java, memoria disponible de Windows y memoria del proceso. Contrasta el tiempo de CPU acumulado con las métricas de aplicación y las consultas a Grafana.',
 'muestras-windows-prometheus-grafana.csv; contraste-cpu-acumulada.json; windows-cim-inicial.json.',
 'Distinguimos el consumo de todo Windows del consumo de Java. Esa separación evita atribuir a la aplicación la actividad de otros programas del equipo.',
 'CPU Windows entre 9,62 y 16,45 %; Java entre 0,16 y 0,49 %; working set entre 501,91 y 506,55 MiB.',
 'Los valores históricos se obtuvieron con puertos; el lanzador actual aísla la carga usando tipos-carga. La RAM y el working set de Windows se evidencian en CSV; no son equivalentes al heap del panel JVM.'),
('M10','Actividad de disco y archivos','Media','Entre 1 y 2 minutos',
 'Python, API de documentos, SHA-256, Windows CIM, Prometheus y Grafana.',
 'Utiliza una cuenta SGA con permiso de crear, descargar y anular documentos GENERAL; se recomienda ADMINISTRADOR_TI. El script genera tres TXT sintéticos de 512 KiB y los carga con diez segundos de separación.',
 'Tres HTTP 201; coincidencia de tamaño y SHA-256 entre el archivo enviado, almacenado y descargado; muestras de actividad del disco y anulación final por API.',
 'cargas.csv; verificacion-archivos.csv; actividad-latencia-disco.csv; limpieza-documentos.json.',
 'Relacionamos operaciones reales de archivos con observaciones del almacenamiento y comprobamos la integridad mediante SHA-256. Los documentos utilizados son exclusivamente de prueba.',
 'Tres archivos válidos y nueve muestras de C:. Latencia media por intervalo entre 0,096 y 0,476 ms; anulación de los tres documentos con HTTP 200.',
 'El contador del volumen incluye otros procesos y la caché. La anulación es lógica: los archivos físicos permanecen por el comportamiento de auditoría de la aplicación; no se borran documentos reales.'),
('M11','Disponibilidad de páginas y API','Alta','Entre 2 y 3 minutos',
 'Nginx de laboratorio, Python, Blackbox Exporter, Prometheus y Grafana.',
 'Deja libres 15173, 15174 y 15180. El script crea un Nginx aislado con los mismos dist y backend; luego usa raíces inexistentes y un upstream no disponible para generar fallos controlados.',
 'Línea base HTTP 200, tres páginas con 500 y API con 502 en el laboratorio, cuatro alertas pending y firing tras el tiempo configurado; recuperación a 200 y alertas inactivas.',
 'linea-tiempo.csv; comprobaciones-http.csv; grafana-respuestas.json; verificacion-final.json.',
 'La prueba es exitosa cuando el monitoreo detecta una indisponibilidad conocida y luego reconoce la recuperación. Los HTTP 500 y 502 del laboratorio son el estímulo esperado.',
 'Cuatro alertas detectadas con for de 15 segundos y recuperación de las cuatro rutas. Las rutas principales continuaron en HTTP 200.',
 'No se modifica el servicio Nginx principal. Al terminar, se detiene la instancia aislada y objetivos-pruebas.json vuelve a su contenido previo. No se validó envío de avisos externos.'),
('M12','Disponibilidad de MySQL','Alta','Entre 2 y 3 minutos',
 'MySQL y backend aislados, Python, Blackbox, Prometheus y Grafana.',
 'Deja libres MySQL 13306, backend 18086 y gestión 19086. El script inicializa una base vacía con datos sintéticos y arranca el mismo JAR; detiene y reinicia únicamente ese MySQL de laboratorio.',
 'El catálogo pasa de 200 a 500 al perder la base; la alerta pasa por pending y firing. Al reiniciar MySQL, el mismo backend recupera HTTP 200 y la alerta queda inactiva.',
 'linea-tiempo.csv; consultas-http.json; correlacion-sql-http.csv; eventos-correlacionados.log.',
 'El proceso Java puede seguir vivo aunque una función dependiente de MySQL falle. Por eso verificamos una operación de negocio además del indicador de salud.',
 'Catálogo 200, después 500 por caída de MySQL, alerta tras 15 segundos y recuperación a 200. /api/v1/health permaneció UP.',
 'Ese health propio informa disponibilidad del proceso; no comprueba la base de datos. El laboratorio no copia datos reales. No detengas el servicio principal MySQL84 para esta prueba.'),
('M13','Alerta por errores HTTP','Alta','Aproximadamente 11 a 12 minutos',
 'MySQL y backend aislados, Prometheus temporal, Python y Grafana.',
 'Reserva 13307 para MySQL, 18087 para backend, 19087 para gestión y 19090 para Prometheus. El ejecutor reproduce la regla original en la instancia temporal y genera errores reales al detener la base aislada.',
 'Tasa HTTP 5xx mayor que 1 %, estado pending y firing después de cinco minutos desde activeAt. Tras reiniciar MySQL, debe desaparecer la alerta cuando la ventana de cinco minutos deje de contener errores.',
 'linea-tiempo.csv; regla-original.yml; resumen-fases.json; reglas-evaluadas-firing.json; revision-evaluador.json.',
 'Una regla con for de cinco minutos exige persistencia; no se activa por un único error. También esperamos la salida de los errores de la ventana antes de afirmar que la alerta se recuperó.',
 'Firing a los 300,729 segundos desde activeAt, tasa de errores de 50 % durante el fallo y recuperación a 0 %. Duración total cercana a 11,5 minutos.',
 'La fuente Grafana Laboratorio M13 se elimina al finalizar; presenta los archivos guardados. Se conservó una evaluación inicial corregida por una muestra anterior a la primera recolección. La regla principal no fue alterada.'),
('M14','Alerta por saturación de CPU','Alta','Aproximadamente 6 a 7 minutos',
 'Workers Python de baja prioridad, contadores Windows, Micrometer, Prometheus y Grafana.',
 'Primero ejecuta la preparación sin carga. Para la prueba completa, utiliza el segundo comando con -CargaRealCPU y deja terminar la recuperación. La carga afecta al Windows real y puede reducir la fluidez del equipo.',
 'CPU del host superior al 85 %, alerta pending y luego firing después de cinco minutos desde activeAt; workers detenidos y alerta inactiva al recuperarse. Las cuatro rutas principales deben conservar HTTP 200.',
 'linea-tiempo.csv; workers.json; resumen-fases.json; servicios-iniciales.json; servicios-finales.json.',
 'La regla mide la CPU de todo el equipo. Aplicamos una carga acotada, observamos la permanencia del umbral durante cinco minutos y verificamos que la alerta se resolvió al retirar la carga.',
 'CPU cercana al 96 % con 12 workers; firing a los 304,831 segundos; recuperación y cuatro rutas HTTP 200.',
 'No hubo VM disponible: la evidencia corresponde al host compartido real. Sin -CargaRealCPU solo hay preparación, no una prueba completa exitosa. Los workers tienen límite de 390 segundos y vigilancia del proceso padre.'),
('M15','Interrupción de ingesta de logs','Alta','Aproximadamente 3 a 4 minutos',
 'Python, Alloy, Loki, logs Java, Prometheus y Grafana.',
 'Mantén Loki y el backend en ejecución. El script genera lecturas cada cinco segundos, pausa únicamente Alloy durante unos dos minutos y lo reinicia usando sus posiciones persistentes.',
 'El backend sigue respondiendo y escribiendo logs, pero Loki no recibe líneas nuevas durante la pausa. Debe activarse IngestaLogsInterrumpida, resolverse al reiniciar Alloy y recuperarse cada evento marcado de la pausa.',
 'linea-tiempo.csv; correlacion-eventos.csv; eventos-recuperados-loki.log; grafana-logs.json; verificacion-final.json.',
 'Separamos la generación de logs de su transporte. Aunque el backend continúa funcionando, detectamos la interrupción de ingesta y verificamos que el recolector recuperó los eventos pendientes.',
 'Pausa de 127,158 segundos; 32 lecturas HTTP 200; recuperación de 25 de 25 eventos de la pausa y dos eventos duplicados; consulta Grafana con 34 líneas.',
 'La entrega es al menos una vez: un duplicado no demuestra pérdida. Loki usa hora de ingesta y conserva la hora original en el texto. Hubo un intento previo con timeout conservado como incidencia; su causa raíz no quedó demostrada.'),
]

doc=Document()
sec=doc.sections[0]
sec.page_width=Inches(8.5);sec.page_height=Inches(11)
sec.top_margin=sec.bottom_margin=sec.left_margin=sec.right_margin=Inches(1)
sec.header_distance=sec.footer_distance=Inches(.45)
for name in ['Normal','Title','Subtitle','Heading 1','Heading 2','Heading 3','List Bullet','List Number']:
 s=doc.styles[name];s.font.name='Arial';s.font.color.rgb=RGBColor(0,0,0)
for s in doc.styles:
 for border in list(s.element.iter(qn('w:pBdr'))):
  border.getparent().remove(border)
normal=doc.styles['Normal'];normal.font.size=Pt(11)
normal.paragraph_format.space_after=Pt(6);normal.paragraph_format.line_spacing=1.05
for name,size in [('Title',23),('Heading 1',18),('Heading 2',12)]:
 doc.styles[name].font.size=Pt(size)
 doc.styles[name].paragraph_format.space_before=Pt(9)
 doc.styles[name].paragraph_format.space_after=Pt(6)
doc.styles['Heading 2'].font.bold=True
header=sec.header.paragraphs[0];header.text='TransAduanas  |  Manual de pruebas de monitoreo';header.style='Normal'
header.runs[0].font.size=Pt(9)
footer=sec.footer.paragraphs[0];footer.alignment=2
footer.add_run('Página ').font.size=Pt(9)
fld=OxmlElement('w:fldSimple');fld.set(qn('w:instr'),'PAGE');footer._p.append(fld)
commands=[]
def p(text,style=None):return doc.add_paragraph(text,style)
def h(text):return doc.add_heading(text,2)
def new(title):doc.add_page_break();doc.add_heading(title,1)
def code(text):
 commands.append(text)
 para=doc.add_paragraph();para.paragraph_format.space_before=Pt(3);para.paragraph_format.space_after=Pt(8)
 para.paragraph_format.keep_together=True
 run=para.add_run(text);run.font.name='Consolas';run.font.size=Pt(9)
 for t in run._r.findall(qn('w:t')):t.set(qn('xml:space'),'preserve')
 return para
def label(name,text):
 para=doc.add_paragraph();para.add_run(name+': ').bold=True;para.add_run(text);return para
def launch(c,extra=''):
 return ('powershell.exe -NoProfile -ExecutionPolicy Bypass -File `\n'
 '  ".\\docs\\pruebas\\monitoreo\\Ejecutar-CasoMonitoreo.ps1" `\n'
 f'  -Codigo {c}'+extra)

doc.add_heading('Manual de pruebas de monitoreo de TransAduanas',0)
p('Aplicación práctica y explicación para el docente','Subtitle')
p('Revisión explicada del 21 de noviembre de 2026. Proyecto ubicado en C:\\Users\\asus\\Music\\TransAduanas. Backend de referencia 0.5.5.')
p('Cada prueba incluye un título sencillo, qué hace, su comando, el resultado esperado y qué mostrar como evidencia. Se mantienen las quince pruebas y sus criterios de evaluación.')
h('Cómo utilizarlo')
p('Prepara el entorno de las páginas 2 y 3 y ejecuta una prueba por vez. En Qué mostrar se indican los datos y archivos concretos de esa ejecución. Usa el comando de la página 3 para abrir su carpeta y comprueba la fecha antes de seleccionar los archivos.')
p('Los quince casos cuentan con una última ejecución EXITOSO dentro de sus alcances. Los fallos anteriores, ajustes del evaluador e incidencias de preparación permanecen registrados. Esta guía no sustituye ni borra ese historial.')
h('Contenido')
p('Preparación y comandos: páginas 2 y 3.\nBaja importancia M01 a M05: páginas 4 a 8.\nMedia importancia M06 a M10: páginas 9 a 13.\nAlta importancia M11 a M15: páginas 14 a 18.\nRegistro y atención de incidencias: página 19.\nGuion de exposición y respuestas al docente: página 20.')
h('Qué significa aprobar una prueba')
p('Se aprueba cuando se cumplen sus verificaciones y se conserva la evidencia. En un laboratorio de alertas, provocar un fallo controlado es parte del estímulo: el éxito consiste en detectarlo y comprobar la recuperación. Una interrupción imprevista del ejecutor se registra y se diagnostica por separado.')
p('Presenta resultados obtenidos y sus límites. No afirmes que se probaron notificaciones externas, retención de varios días, ausencia de fugas de memoria o una plataforma de trazas distribuidas cuando la evidencia no lo demuestra.')

new('Preparación del entorno')
p('Abre PowerShell y sitúate en la carpeta real del proyecto. Conserva esta ubicación para todos los comandos del manual.')
code('Set-Location -LiteralPath "C:\\Users\\asus\\Music\\TransAduanas"\npython --version\njava -version\njavac -version\nGet-Service MySQL84,TransAduanasBackend')
p('Python debe estar disponible y Java y javac deben corresponder al JDK 21. Los servicios MySQL84 y TransAduanasBackend deben indicar Running. Si están detenidos, inicia únicamente esos servicios desde una consola de administrador:')
code('Start-Service -Name MySQL84,TransAduanasBackend')
h('Comprobar Nginx')
p('Ejecuta el inicio protegido. Si ya hay un maestro válido y los puertos 5173, 5174 y 5180 están disponibles, informa que Nginx ya está iniciado y no abre otra instancia. Si detecta duplicados o un nginx.pid incoherente, se detiene para permitir el diagnóstico.')
code('powershell.exe -NoProfile -ExecutionPolicy Bypass -File `\n  ".\\monitoreo\\Iniciar-NginxSeguro.ps1"')
p('Solo cuando no existe una instancia, este comando valida la configuración e inicia Nginx. Sus raíces deben apuntar a los dist reales bajo Music\\TransAduanas. No ejecutes nginx.exe por separado después del inicio: dos maestros pueden compartir puertos y dejar nginx.pid apuntando a una instancia diferente de la que los atiende.')
p('Una repetición de M05 detectó precisamente ese problema. Se cerró ordenadamente el duplicado, se restituyó nginx.pid al maestro original y M05 volvió a cumplir sus diez verificaciones. El intento fallido permanece en el historial.')
h('Iniciar las herramientas de monitoreo')
code('powershell.exe -NoProfile -ExecutionPolicy Bypass -File `\n  ".\\monitoreo\\Iniciar-Monitoreo.ps1"')
p('Se utilizan Prometheus 9090, Grafana 3000, Blackbox 9115, Loki 3100 y Alloy 12345, ligados a la interfaz local. El inicio comprueba procesos existentes; espera a que los servicios estén listos. Este conjunto debe iniciarse nuevamente después de reiniciar Windows.')

new('Comandos y revisión de resultados')
h('Comprobar disponibilidad antes de empezar')
code('$urls = @(\n "http://127.0.0.1:5173/", "http://127.0.0.1:5174/",\n "http://127.0.0.1:5180/",\n "http://127.0.0.1:5180/api/v1/catalogos/puertos",\n "http://127.0.0.1:8086/api/v1/health",\n "http://127.0.0.1:9086/actuator/prometheus",\n "http://127.0.0.1:9090/-/ready",\n "http://127.0.0.1:3000/api/health",\n "http://127.0.0.1:3100/ready", "http://127.0.0.1:12345/-/ready"\n)\nforeach ($url in $urls) {\n  try { "$url -> $((Invoke-WebRequest -Uri $url `\n    -UseBasicParsing -TimeoutSec 10).StatusCode)" }\n  catch { "$url -> ERROR: $($_.Exception.Message)" }\n}')
p('Todas estas comprobaciones deben responder 200. Si Loki todavía está iniciando, espera y repite solo la comprobación. Revisa Prometheus en http://localhost:9090/targets y sus alertas en http://localhost:9090/alerts.')
h('Abrir la evidencia de cualquier caso')
code('powershell.exe -NoProfile -ExecutionPolicy Bypass -File `\n  ".\\docs\\pruebas\\monitoreo\\Mostrar-EvidenciaMonitoreo.ps1" `\n  -Codigo M01 -Abrir')
p('Sustituye M01 por el caso que acabas de ejecutar. El visor muestra el último resultado.json existente y abre su carpeta. Comprueba fecha e identificador: una preparación incompleta puede dejar como último resultado uno anterior. Un código de salida 0 no garantiza que Estado sea EXITOSO.')
h('Credenciales y paneles')
p('M06, M07 y M10 solicitan usuario y contraseña del SGA, con entrada oculta y alcance temporal al proceso. El acceso de Grafana es distinto. Su configuración privada está en TransAduanas-herramientas\\monitoreo\\credenciales-locales.json; no muestres ese archivo ni contraseñas en evidencias.')
p('Abre http://localhost:3000/d/transaduanas-monitoreo. El tablero tiene 18 paneles; ajusta el intervalo de tiempo a la ejecución. Los CSV y JSON guardados son la evidencia persistente si el panel ya muestra otros minutos. Copia cada bloque completo; el acento grave final de una línea continúa el comando y no debe llevar espacios después.')

for idx, row in enumerate(CASES,1):
 c,name,importance,duration,tools,steps,expected,files,speech,observed,scope=row
 titulo,que,lectura,frase=B[c]
 resultado,mostrar=E[c]
 new(c+' '+titulo)
 h('Qué hace la prueba')
 p(que)
 if c=='M03':p('Usa un laboratorio con archivos separados; no llena el registro del sistema activo.')
 if c=='M10':p('Solo usa archivos creados para esta prueba. Al terminar, los documentos quedan inactivos; sus archivos físicos se conservan según el diseño de la aplicación.')
 h('Herramientas utilizadas')
 p('; '.join(nombre+': '+funcion for nombre,funcion in H[c])+'.')
 h('Comando')
 p('Ejecuta desde la carpeta del proyecto. Duración aproximada: '+duration.lower()+'.')
 if c in ('M06','M07','M10'):
  p('Introduce tu usuario y contraseña del SGA cuando se soliciten.')
  if c=='M10':p('La cuenta debe poder crear y anular documentos.')
 if c=='M14':
  p('Preparación sin carga:')
  code(launch(c))
  p('Prueba completa con carga real del equipo; puede reducir su fluidez:')
  code(launch(c,' -CargaRealCPU'))
 else:code(launch(c))
 h('Resultado esperado')
 p(resultado)
 p('El resultado final debe indicar EXITOSO si se cumplen las verificaciones. Si aparece FALLO, muestra ese resultado y la causa registrada.')
 h('Qué mostrar')
 for item in mostrar:p(item,'List Bullet')

new('Registro y atención de incidencias')
h('Guardar el resultado sin borrar intentos anteriores')
p('Cada ejecutor crea una carpeta con el código, fecha y sufijo único bajo evidencias-monitoreo. Conserva resultado.json, las verificaciones y los archivos de soporte; no reemplaces un fallo anterior por una carpeta vacía ni cambies su estado para presentar una aprobación.')
code('python ".\\docs\\pruebas\\monitoreo\\actualizar_registros.py"')
p('Este comando reconstruye resultados-pruebas.csv, historial-ejecuciones.csv, incidencias-pruebas.csv y registro-monitoreo.json dentro de evidencias-monitoreo. El primer archivo muestra la última ejecución por caso; el historial conserva todas. Antes de repetir pruebas, había 15 casos, 21 ejecuciones y 12 incidencias registradas.')
p('Las tablas finales Resultados_e_incidencias_monitoreo_TransAduanas.docx y su PDF son una fotografía del cierre anterior. La actualización de CSV no reescribe automáticamente sus cifras; si se presentan nuevas ejecuciones, hay que revisar y actualizar el informe final.')
h('Si una prueba no termina o indica FALLO')
p('Anota el código, la hora, el mensaje real, el identificador de la carpeta y qué verificación incumplió. Comprueba los servicios, los puertos y la versión del backend. Si no existe resultado.json, revisa preparación y logs: no presentes un fallo del lanzador como una caída demostrada del sistema.')
p('Al corregir un problema, conserva el intento inicial y ejecuta de nuevo solo el caso afectado. En M15, por ejemplo, se conservó un timeout previo; la repetición recuperó los 25 eventos, pero eso no demuestra por sí solo la causa raíz del timeout.')
h('Interrumpir M14 de forma prevista')
p('Durante una M14 completa, desde otra consola situada en la raíz del proyecto, crea el archivo de parada en la carpeta más reciente de ese caso:')
code('$caso = Get-ChildItem ".\\evidencias-monitoreo" -Directory `\n  -Filter "M14-*" | Sort-Object Name -Descending |\n  Select-Object -First 1\nif ($caso) {\n  New-Item -Path (Join-Path $caso.FullName "DETENER.txt") `\n    -ItemType File -Force\n}')
p('El ejecutor comprobará la señal y retirará la carga. Conserva el resultado de la interrupción; una ejecución detenida antes del criterio no acredita la alerta completa. Para los demás laboratorios deja actuar la limpieza del ejecutor y comprueba su verificación final.')

new('Guion de exposición y respuestas al docente')
h('Demostración de cinco minutos')
p('0:00 a 0:30. Presenta TransAduanas, sus componentes y el objetivo: observar registros, rendimiento y salud del sistema. Muestra las tres páginas y el tablero de Grafana.')
p('0:30 a 1:30. Usa M01 o M02 para mostrar solicitudes y logs. Enlaza un X-Request-ID con Java y JDBC mediante la evidencia de M07. Explica que no se exponen datos SQL ni credenciales.')
p('1:30 a 2:30. Muestra el aumento de solicitudes de M04, percentiles de M06 y muestras JVM y Windows de M08 y M09. Explica que cada métrica tiene una unidad y alcance diferentes.')
p('2:30 a 4:00. Abre las líneas de tiempo guardadas de M11 a M15. Recorre normal, pending, firing y recuperación. M13 y M14 duran más de cinco minutos; presenta sus evidencias completas en lugar de iniciarlas todas durante la exposición.')
p('4:00 a 5:00. Presenta las tablas finales de resultados e incidencias. Explica una corrección real y muestra su repetición exitosa. Cierra indicando los alcances efectivamente comprobados.')
h('Respuestas breves')
label('¿Por qué Python es una herramienta?', 'Automatiza solicitudes, toma muestras, consulta las API y contrasta evidencias. Micrometer instrumenta, Prometheus recolecta y evalúa reglas, Grafana visualiza, Alloy transporta y Loki almacena y busca logs.')
label('¿UP significa que todo funciona?', 'No. M12 mostró el health propio UP mientras una consulta que necesitaba MySQL devolvía 500. Se necesita una verificación funcional además de la disponibilidad del proceso.')
label('¿Por qué una prueba con HTTP 500 puede ser exitosa?', 'Porque en el laboratorio el error es un estímulo previsto. Se evalúa que la alerta lo detecte y que la recuperación sea verificable. Un 500 imprevisto en la línea base sí requiere diagnóstico.')
label('¿Por qué los números cambian?', 'Dependemos del tráfico, la ventana temporal, los procesos de Windows y el estado de la JVM. Se verifica el criterio de cada caso, no la repetición exacta de una medición histórica.')
label('¿Se perdió información al pausar Alloy?', 'En M15 se recuperaron los 25 eventos de la pausa. Hubo duplicados compatibles con entrega al menos una vez; se identificaron por su marcador. No se afirma una garantía universal sin duplicados.')
label('¿Se probaron avisos por correo?', 'No. Se verificaron los estados de alerta en Prometheus y su consulta en Grafana. La entrega a destinatarios externos no fue parte de estas quince pruebas.')

doc.core_properties.title='Manual de pruebas de monitoreo de TransAduanas'
doc.core_properties.subject='Procedimientos y evidencias de las quince pruebas de monitoreo'
doc.core_properties.author='Equipo del proyecto TransAduanas'
OUT.mkdir(parents=True,exist_ok=True)
commands.extend(actualizar_manual(doc))
doc.save(OUT/'Manual_pruebas_monitoreo_TransAduanas.docx')
(OUT/'Comandos_manual_monitoreo_TransAduanas.txt').write_text(
 'COMANDOS DEL MANUAL DE MONITOREO DE TRANSADUANAS\n'
 'Leer el manual antes de aplicar. Ejecutar un solo caso por vez.\n'
 'M14 -CargaRealCPU genera carga real del equipo.\n\n'+
 '\n\n'.join(f'BLOQUE {i}\n{x}' for i,x in enumerate(commands,1))+'\n',encoding='utf-8-sig')
print(OUT/'Manual_pruebas_monitoreo_TransAduanas.docx')
(Path(__file__).parent/'comandos-manual.json').write_text(json.dumps(commands,ensure_ascii=False),encoding='utf-8-sig')
