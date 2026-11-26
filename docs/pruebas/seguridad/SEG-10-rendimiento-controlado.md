# SEG-10: rendimiento y disponibilidad controlados

Objetivo: medir tiempos de respuesta y comprobar que el servicio y la autorización sigan funcionando durante un ensayo pequeño de solicitudes normales. Es una prueba no funcional de rendimiento y disponibilidad, complementaria a seguridad; no es estrés, saturación ni una prueba de denegación de servicio.

Herramientas: JUnit Jupiter, Spring Boot Test, Java HttpClient, H2 y Maven Surefire. Clasificación de ejecución: prueba del sistema del backend, con HTTP y persistencia reales. No se utiliza Mockito, JMeter ni k6.

## Comandos para el docente

Desde `backend-sga`, en PowerShell:

```powershell
$env:SEG10_RUN = 'true'
.\mvnw.cmd test "-Dtest=SecurityControlledPerformanceTest"
```

Después puede quitar la activación de esa sesión:

```powershell
Remove-Item Env:\SEG10_RUN
```

La prueba requiere activación explícita para evitar mediciones accidentales en la suite habitual. Sin SEG10_RUN=true, JUnit la omite: una omisión no significa que se haya ejecutado ni aprobado.

## Procedimiento y criterios

| Paso | Comprobación |
| --- | --- |
| Preparación | Crear una cuenta ficticia y obtener una sesión válida; los valores no se imprimen ni se exportan. |
| Antes | Salud 200 con UP; clientes sin sesión reciben 401 sin datos. Registrar cantidades de clientes y usuarios. |
| Calentamiento | Una solicitud válida por ruta, excluida de las estadísticas. |
| Medición | Diez rondas de tres GET: salud, catálogo de puertos y clientes con sesión. Total: 30 solicitudes medidas, secuenciales, con pausa de 100 ms después de cada respuesta. |
| Después | Salud y autorización siguen correctas; cantidades de clientes y usuarios sin cambios desde el inicio de la medición. |

Se valida el código HTTP 200 y el contenido JSON de cada respuesta medida. Se detiene el ensayo si aparece un error de conexión, contenido o estado, en lugar de insistir sobre un servicio que falla.

Umbrales de referencia definidos para este ensayo local, no un SLA previamente acordado del proyecto:

- 30 muestras completas, cero errores.
- Percentil 95 global de hasta 2000 ms.
- Tiempo máximo de hasta 5000 ms; cada solicitud tiene un timeout de cinco segundos.
- Controles antes/después correctos y cantidades de clientes y usuarios sin cambios.

Se informa mínimo, promedio, p50, p95 y máximo globales y por ruta. El percentil usa el método de rango más próximo: posición ceil(p*n) en tiempos ordenados. Con solo diez muestras por ruta, su p95 coincide con el máximo: no se debe presentar como una estimación precisa para producción.

El cronómetro mide desde el envío hasta recibir el cuerpo HTTP completo. No incluye la pausa ni la validación JSON posterior. El tiempo de arranque de Spring y el calentamiento se excluyen. La duración total de la fase medida y su tasa de solicitudes sí incluyen las pausas; esa tasa no es la capacidad máxima del sistema.

Se realizan además tres solicitudes de calentamiento y cinco auxiliares (login y controles), para un total de 38 solicitudes del guion cuando termina normalmente. Una prueba JUnit SEG-10, no 30 pruebas JUnit distintas.

## Evidencia

`backend-sga/target/seg10-rendimiento/resultado.json` contiene las mediciones y controles. `mediciones.csv` contiene cada ruta, estado y duración sin token, contraseña ni cuerpo HTTP. Los mensajes `[SEG-10]` muestran el resumen. Un resultado favorable debe incluir BUILD SUCCESS y una prueba ejecutada, no omitida.

## Alcance

Servidor HTTP en 127.0.0.1 con puerto aleatorio, base H2 temporal y datos ficticios de pequeño volumen. Un solo cliente secuencial, sin concurrencia y con pausas. Los resultados dependen del equipo y de su actividad durante la ejecución. No certifica capacidad máxima, resistencia a ataques de disponibilidad, comportamiento bajo carga alta, MySQL, interfaz, red remota ni HTTPS de despliegue.

## Resultado observado

APROBADA: una prueba JUnit ejecutada, 30 solicitudes medidas, cero errores. Promedio 12.76 ms, p95 21.47 ms y máximo 24.30 ms. Controles antes y después correctos; cantidades de clientes y usuarios sin cambios. BUILD SUCCESS. Son mediciones de esta ejecución local, no una garantía para otro equipo o producción.
