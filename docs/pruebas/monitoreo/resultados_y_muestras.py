# Resultado esperado y evidencia concreta por caso. No ejecuta pruebas.
E = {
'M01': ('Diez consultas con respuesta HTTP 200 y diez anotaciones encontradas. HTTP 200 significa que el servidor respondió correctamente.', [
 'La terminal con el resultado final y la carpeta de evidencias.',
 'consultas.csv con las diez filas y su columna HTTP en 200.',
 'eventos-nginx.log con las anotaciones de esas consultas, su fecha y su ruta.']),
'M02': ('Tres consultas correctas y tres coincidencias entre los registros de Nginx y Java. Cada consulta debe conservar su misma etiqueta de identificación.', [
 'La terminal con el resultado final.',
 'campos-verificados.csv con las tres filas y CorrelacionCorrecta en True, que significa verdadero.',
 'Una anotación de eventos-nginx.log y otra de eventos-backend.log con el mismo request_id.']),
'M03': ('1500 mensajes escritos y 1500 recuperados, con cero perdidos y cero repetidos. Deben existir archivos antiguos comprimidos y un archivo actual.', [
 'La terminal con los totales de mensajes, archivos comprimidos, pérdidas y repeticiones.',
 'La carpeta logs-laboratorio con el archivo actual y los archivos terminados en .gz.',
 'secuencia-verificada.csv con Apariciones en 1: cada mensaje aparece una sola vez.']),
'M04': ('Sesenta consultas correctas. El contador debe aumentar en sesenta en Micrometer, Prometheus y Grafana. Los valores iniciales pueden cambiar; lo importante es el aumento.', [
 'La terminal con las 60 consultas correctas y los tres aumentos de 60.',
 'panel-prometheus-grafana.csv con los contadores iniciales y finales de ambas herramientas.',
 'conteos-por-solicitud.csv, donde se sigue el aumento durante la prueba.']),
'M05': ('Todas las filas deben indicar Correcto=True. Se revisan cinco componentes en dos momentos: diez verificaciones correctas. Los números PID pueden cambiar al reiniciar.', [
 'La tabla de la terminal con Componente, PID, PIDSupervisor, Puerto y Correcto.',
 'inventario-completo.csv con los dos muestreos y todas las filas Correcto en True.',
 'resultado.json con FilasVerificadas y FilasCorrectas en 10.']),
'M06': ('Sesenta consultas correctas y sus tiempos de respuesta. Deben aparecer p50, p95 y p99: ayudan a distinguir los tiempos habituales de los más lentos. Los tiempos pueden variar.', [
 'La terminal con el total de consultas correctas y los tiempos calculados.',
 'percentiles.csv con p50, p95 y p99, y sus columnas ClienteMs y BackendMs. Ms indica milisegundos.',
 'panel-prometheus-grafana.csv con la comparación de las mediciones consultadas.']),
'M07': ('Una consulta correcta y su misma etiqueta de identificación en Nginx, Java y las operaciones de MySQL. En la ejecución de referencia se encontraron tres operaciones de base de datos.', [
 'La terminal con HTTP 200 y el resumen de la relación entre componentes.',
 'eventos-nginx.log y eventos-backend.log con el mismo request_id.',
 'Las líneas jdbc_query_completed de esa consulta: muestran las operaciones de acceso a MySQL.']),
'M08': ('Siete muestras de memoria y los contadores de limpieza automática de Java, llamada GC. Cero pausas nuevas es válido si las mediciones se obtuvieron correctamente.', [
 'La terminal con el número de muestras, el rango de memoria y las pausas nuevas de GC.',
 'muestras-prometheus-grafana.csv con las columnas de memoria HEAP y pausas GCCOUNT.',
 'Durante la prueba, los paneles Memoria usada por la JVM y Tiempo consumido en GC de Grafana.']),
'M09': ('Siete muestras del uso del procesador y de la memoria. Debe distinguirse el consumo de todo Windows del de Java; los valores cambian según la actividad del equipo.', [
 'La terminal con los porcentajes del equipo y de Java, y la memoria del proceso.',
 'muestras-windows-prometheus-grafana.csv con Windows_CPUHostPct y Windows_CPUJavaPct.',
 'Durante la prueba, el panel CPU del host y del proceso de Grafana. La memoria medida por Windows se muestra en el archivo.']),
'M10': ('Tres cargas con HTTP 201 y tres archivos verificados. HTTP 201 significa documento creado. La huella SHA-256 debe coincidir al guardar y descargar cada archivo.', [
 'La terminal con las tres cargas correctas y la verificación de archivos.',
 'verificacion-archivos.csv con ArchivoFisicoSHA256Correcto en True para los tres archivos.',
 'actividad-latencia-disco.csv con las mediciones de escritura del disco y limpieza-documentos.json con la anulación de los documentos de prueba.']),
'M11': ('En la copia de prueba: respuestas 200, después errores 500/502 y finalmente 200. Las cuatro alertas deben activarse y cerrarse tras la recuperación. Las páginas habituales deben seguir respondiendo.', [
 'La terminal con la detección y recuperación de las cuatro rutas.',
 'linea-tiempo.csv con las fases normal, fallo y recuperación; AlertasFiring debe pasar de 0 a 4 y volver a 0.',
 'comprobaciones-http.csv con los códigos 200, 500/502 y 200 del laboratorio.']),
'M12': ('La consulta de laboratorio debe pasar de HTTP 200 a 500 y volver a 200. Debe activarse una alerta y cerrarse al recuperar MySQL. El indicador UP de Java puede mantenerse.', [
 'La terminal con el resumen de caída y recuperación.',
 'linea-tiempo.csv con NegocioHTTP en 200, 500 y 200; y Firing en 0, 1 y 0.',
 'Las filas de la caída con HealthUP en True: Java estaba activo, pero la consulta fallaba.']),
'M13': ('La alerta debe activarse después de cinco minutos con más de 1 % de errores. Al recuperarse el sistema y salir los errores del intervalo observado, debe quedar inactiva.', [
 'La terminal con el tiempo transcurrido hasta la activación y el cierre final.',
 'linea-tiempo.csv con EstadoAlerta: pending, firing e inactive; significan pendiente, activa e inactiva.',
 'Las columnas SegundosDesdeActiveAt y PorcentajePrometheus, para mostrar el tiempo de espera y el porcentaje de errores.']),
'M14': ('El uso del procesador debe superar el 85 % durante cinco minutos. La alerta debe activarse y cerrarse cuando se retire la carga. La preparación sin carga no completa la prueba.', [
 'La terminal con los porcentajes de uso y el resultado final de la ejecución completa.',
 'linea-tiempo.csv con CPUWindowsPct, EstadoAlerta y SegundosDesdeActiveAt durante la carga y la recuperación.',
 'resultado.json con WorkersDetenidos y RutasDisponibles en True dentro de Verificaciones: terminó la carga y las páginas siguieron respondiendo.']),
'M15': ('Debe detectarse la falta de nuevos registros durante unos dos minutos. Al reiniciar Alloy, deben llegar todos los registros identificados durante la pausa y cerrarse la alerta. Puede haber repeticiones.', [
 'La terminal con la cantidad de registros de la pausa recuperados y el cierre de la alerta.',
 'correlacion-eventos.csv: las filas de la pausa deben tener EventosHTTPenLoki igual o mayor que 1.',
 'linea-tiempo.csv con la pausa y recuperación, y eventos-recuperados-loki.log con las anotaciones recuperadas.']),
}
