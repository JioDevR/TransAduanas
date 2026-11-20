# Validación de compatibilidad con main

Fecha: 19 de noviembre de 2026. Base integrada: `origin/main` en `0e43733`.

## Cambios comprobados

- Se conservaron los cambios de la aplicación de main y sus nombres de carpetas: `backend-sga`, `app-sga`, `app-b2b` y `app-landing`.
- Se adaptaron el workflow, la automatización local, el publicador Nexus y el manual. La automatización y el publicador detectan la raíz desde el script.
- Se corrigió la ruta opcional de configuración local del backend. El perfil staging valida el esquema y desactiva el inicializador.
- Los tests antiguos de cotizaciones recibieron el mock del nuevo servicio de reportes requerido por el constructor actual.
- El generador de configuración de Nginx utiliza los nombres nuevos, valida los dist y guarda respaldo antes de escribir. No inicia ni recarga el servidor.

## Comprobaciones ejecutadas

| Comprobación | Resultado |
| --- | --- |
| `mvnw.cmd -B -ntp clean verify "-Dtest=BusinessFunctionalCasesTest"`, desde `backend-sga` | BUILD SUCCESS; 15 pruebas, cero fallos, errores u omisiones; JAR 0.5.1 generado. |
| `mvnw.cmd -B -ntp test "-Dtest=ControllerFunctionalTest,AllControllersFunctionalTest"` | BUILD SUCCESS; 239 pruebas, cero fallos, errores u omisiones. |
| `npm.cmd ci` y `npm.cmd run build -- --mode staging` en cada interfaz | Correcto en app-sga, app-b2b y app-landing; se generaron los tres dist. |
| Analizador de sintaxis de PowerShell | Los cinco scripts se analizaron sin errores. |
| `Preparar-Nginx.ps1` y `nginx -t` | Configuración válida en una carpeta de herramientas aislada, sin iniciar procesos del servidor. |
| Manual Word | Revisado visualmente con las rutas nuevas y las instrucciones de recuperación. |

Las pruebas y compilaciones se ejecutaron en una copia aislada del checkout integrado, sin importar credenciales del laboratorio. Las pruebas seleccionadas usan H2; no modificaron MySQL ni los servicios Windows del usuario.

## Límites

No se ejecutó el workflow en GitHub Actions, ni se repitió la publicación en Nexus, ni el despliegue/reversión del servicio real. No se afirma haber ejecutado todas las suites del proyecto ni una instalación en otro equipo. Esas comprobaciones requieren las herramientas, variables y permisos del destino.

El XML privado de WinSW y la configuración activa de Nginx están fuera de Git: descargar los cambios no los modifica. El manual explica cómo cargar el XML y preparar Nginx para el checkout actualizado. Un cambio en la carpeta externa donde esté el JAR requiere revisar la configuración local del servicio.
