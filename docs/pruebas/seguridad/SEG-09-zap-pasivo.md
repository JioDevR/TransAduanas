# SEG-09: análisis pasivo con OWASP ZAP

Objetivo: inspeccionar tráfico y respuestas HTTP normales del backend local con reglas pasivas de ZAP, conservando un informe de alertas.

Tipo: análisis dinámico de seguridad pasivo (DAST pasivo), coordinado por una prueba del sistema del backend. Herramientas: OWASP ZAP instalado localmente, JUnit Jupiter, Spring Boot Test, Java HttpClient, H2 y Maven Surefire. No se usa Mockito en este caso.

## Comandos para el docente

Desde `backend-sga`, en PowerShell, con ZAP instalado:

```powershell
$env:SEG09_ZAP_JAR = 'C:\Program Files\ZAP\Zed Attack Proxy\zap-2.17.0.jar'
if (!(Test-Path -LiteralPath $env:SEG09_ZAP_JAR)) {
    throw 'Ajuste SEG09_ZAP_JAR a la ubicación del JAR de ZAP instalado.'
}
.\mvnw.cmd test "-Dtest=SecurityZapPassiveTest"
```

La ruta debe corresponder a la versión instalada. El proyecto usa Java 21. La clase requiere habilitación mediante `SEG09_ZAP_JAR` para no exigir ZAP en cada ejecución normal de la suite. Sin esa variable, JUnit omite el caso: una omisión NO significa que SEG-09 se haya ejecutado ni aprobado.

## Procedimiento automatizado

1. Arranca el backend con puerto aleatorio, H2 temporal y datos ficticios.
2. Inicia un proceso propio de ZAP en 127.0.0.1 con puerto libre, perfil aislado, clave API aleatoria, sin interfaz y `-silent`. No modifica una sesión de ZAP que el usuario tenga abierta.
3. Activa modo seguro y escaneo pasivo; comprueba que haya reglas pasivas habilitadas.
4. Envía seis solicitudes GET normales a través del proxy de ZAP:

| Ruta | Resultado esperado |
| --- | --- |
| /api/v1/health | 200 |
| /api/v1/catalogos/puertos | 200 |
| /api/v1/catalogos/tipos-carga | 200 |
| /api/v1/catalogos/mercancias | 200 |
| /api/v1/clientes | 401 sin sesión |
| /api/v1/portal/perfil | 401 sin sesión |

5. Verifica que ZAP haya registrado al menos los seis mensajes y espera que la cola pasiva permanezca vacía. No basta con obtener respuestas directamente del backend.
6. Comprueba que no se haya iniciado ningún escaneo activo ni spider.
7. Exporta un informe reducido con versión, reglas habilitadas, tráfico, conteos y alertas; no exporta claves API, contraseñas, tokens, cuerpos completos ni evidencias de entrada de las alertas.
8. Cierra únicamente su propio proceso de ZAP.

## Criterio de resultado

Una prueba automatizada SEG-09, seis rutas observadas. Para aprobar la verificación automática deben funcionar los seis GET, haberse registrado el tráfico, haber terminado el escaneo pasivo, haber reglas habilitadas, no existir escaneos activos/spider y no haber alertas altas o medias. Las alertas bajas e informativas se conservan y se revisan; un resultado técnico favorable no equivale a cero alertas de cualquier nivel.

Si ZAP encuentra alertas altas o medias, el caso falla y conserva el informe para su revisión; no se deshabilitan reglas para forzar la aprobación. Si ZAP no inicia o no termina, tampoco debe registrarse como aprobado.

Evidencia: `backend-sga/target/seg09-zap/resultado.json`. Cada ejecución crea además un perfil privado dentro de esa carpeta. No comparta el perfil, configuración o sesión completos; pueden contener claves temporales y tráfico. Comparta el informe reducido y el resumen revisado.

## Alcance

Solo seis rutas JSON del backend, sin sesión. No incluye frontend, navegación autenticada, carga de archivos, motor MySQL, HTTPS real ni escaneo activo. Un análisis pasivo no demuestra por sí solo ausencia de XSS o inyección SQL: esas comprobaciones corresponden a los otros casos de seguridad ejecutados.

Documentación oficial: [API de ZAP y análisis pasivo](https://www.zaproxy.org/docs/api/) y [opciones de línea de comandos](https://www.zaproxy.org/docs/desktop/cmdline/).

## Resultado verificado

ZAP 2.17.0, 61 reglas pasivas habilitadas, seis rutas revisadas, contador de 11 mensajes registrados, cola final vacía y ninguna alerta de cualquier nivel. Cero escaneos activos y cero spiders. JUnit: una prueba ejecutada, cero fallos, cero errores, ninguna omitida; BUILD SUCCESS. No fue necesario modificar el código de producción.
