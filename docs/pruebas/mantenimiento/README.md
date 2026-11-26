# Pruebas de mantenimiento de TransAduanas

Ejecutores PM01 a PM15 y prueba adicional PA01. La adicional tiene su propio lanzador y su propia carpeta de evidencias. Este cambio no modifica las clases Java, la base de datos, las aplicaciones Vue ni su configuracion.

## Ejecucion

Abrir PowerShell en la raiz del proyecto. Los ejecutores Python se pueden invocar directamente si PowerShell bloquea archivos ps1:

```powershell
python .\docs\pruebas\mantenimiento\ejecutar_PM05_informe.py
python .\docs\pruebas\mantenimiento\adicionales\PA01\ejecutar_PA01.py
```

Tambien estan disponibles los lanzadores:

```powershell
.\docs\pruebas\mantenimiento\Ejecutar-Prueba.ps1 5
.\docs\pruebas\mantenimiento\Ejecutar-Adicional.ps1
```

`-Plan` permite consultar el ejecutor y el interprete sin realizar la prueba. Una nueva ejecucion conserva los resultados anteriores y utiliza un sufijo cuando la carpeta ya existe. Revisar el estado y los criterios del resultado nuevo; un codigo de proceso aislado no sustituye esa revision.

## Pruebas incluidas

| Codigo | Comprobacion |
|---|---|
| PM01 | Documentacion y enlaces del proyecto |
| PM02 | Guia de mantenimiento y lista de tareas |
| PM03 | Historial y recuperacion de contenido con Git |
| PM04 | Registro de una consulta y contraste con un fallo historico |
| PM05 | Limpieza de temporales de laboratorio |
| PM06 | Rotacion, compresion y retencion con Logback |
| PM07 | Una ejecucion automatica con Programador de tareas |
| PM08 | Bloqueo de concurrencia entre procesos |
| PM09 | Repeticion de limpieza sin cambios adicionales |
| PM10 | Retencion de respaldos ficticios y proteccion de la ultima copia |
| PM11 | Integridad y descifrado de un respaldo existente |
| PM12 | Restauracion aislada del respaldo y consultas al backend copiado |
| PM13 | Reversion de un JAR conservado en laboratorio |
| PM14 | Regresion antes y despues de cambiar dependencias PDFBox en copias |
| PM15 | Identidades, permisos, secretos y ejecucion limitada |
| PA01 | Disponibilidad e integracion de lectura entre frontend, backend y MySQL |

## Herramientas y rutas

Requieren Windows y Python 3. PM06 necesita JDK 21; PM11 y PM12 requieren cryptography; PM12, PM13, PM15 y PA01 necesitan las herramientas MySQL. PM14 usa Maven y sus dependencias preparadas en cache. PM07 puede pedir la elevacion de Windows para registrar una tarea temporal; PM15 exige que la ejecucion de sus scripts no tenga administrador efectivo.

Las rutas se calculan a partir de la raiz del clon. Estas variables permiten seleccionar una instalacion existente sin editar los ejecutores:

| Variable | Uso |
|---|---|
| TRANSADUANAS_PROYECTO | Raiz del proyecto que se quiere comprobar |
| TRANSADUANAS_DESPLIEGUE | Directorio con current/app.jar, estado.json y configuracion protegida |
| TRANSADUANAS_HERRAMIENTAS | Directorio de herramientas y WinSW |
| TRANSADUANAS_LABORATORIOS | Directorio privado para las copias temporales |
| TRANSADUANAS_EVIDENCIAS_ORIGINALES | Registros y archivos historicos conservados fuera de Git |
| TRANSADUANAS_PYTHON | Python que seleccionara el lanzador, si se configura |
| JAVA_HOME | JDK instalado |
| MYSQL_HOME | Instalacion de MySQL con su directorio bin |
| MAVEN_CMD | Ruta a mvn.cmd, cuando no esta en PATH ni en la cache del wrapper |
| GIT_EXE | Ruta a Git, cuando no esta en PATH |

Las variables solo deben definirse para la sesion y con rutas de la instalacion del equipo. Los servicios de referencia son MySQL84, TransAduanasBackend y TransAduanasMantenimiento. El backend de operacion usa 8086; los laboratorios usan otros puertos y rechazan destinos ocupados en vez de cerrar procesos ajenos. Una instalacion con otros nombres o puertos requiere revisar esos supuestos antes de ejecutar.

## Historial y archivos externos

Las pruebas de restauracion, reversion y seguridad necesitan artefactos y permisos efectivos del entorno. Un clon del repositorio, por si solo, no contiene un respaldo, claves DPAPI ni un despliegue Windows. Los valores de referencia corresponden a la instalacion donde se realizaron las pruebas; las validaciones pueden detectar diferencias legitimas en otra version del proyecto.

- PM01 y PM02 comprueban los manuales y el runbook que existan en el proyecto seleccionado; no generan documentacion para forzar su aprobacion.
- PM03 conserva los identificadores del ensayo anterior como referencia. Para otro historial se pueden indicar `--archivo`, `--commit-inicial`, `--commit-cambio`, `--commit-recuperacion`, `--rama-publicada` y `--commit-publicado`. El contenido recuperado con Git debe corresponder a los commits indicados. El cambio original fue un comentario; no acredita un cambio funcional del script.
- PM04 incorpora un registro historico real sin rutas personales en `referencias/PM04_fallo_historico.json`; no provoca ni reproduce ese fallo.
- PM11 y PM12 leen `PM11_20261122-222724/resultado_PM11.json` dentro del directorio de evidencias originales. Ese registro apunta al respaldo cifrado y a su clave protegida, ambos externos al repositorio. PM11 tambien requiere los registros genuinos `cierre_pendientes/PM11_copia_USB.json` y `cierre_pendientes/PM11_resguardo_confirmado.json`. No crear confirmaciones ficticias para superar los criterios.
- PM13 necesita la restauracion detenida de PM12 y las versiones actual y anterior conservadas en el despliegue. No revierte el servicio original.
- PM14 prepara la misma comparacion 2.0.24/2.0.37 en dos copias sin editar el POM original. Comprueba los tres JAR contra los SHA512 registrados de Apache en `referencias/PM14/descargas_verificadas.csv`; los JAR deben estar en la cache Maven o en la descarga conservada del entorno. Ejecuta Maven offline: una cache incompleta debe prepararse antes del ensayo. La cantidad de casos depende de la version del codigo.
- PM15 compara tambien una ejecucion historica bajo identidad dedicada: `PM15_correccion_20261123-095003/mantenimiento_cuenta_dedicada.json`, dentro de las evidencias originales. La comprobacion actual con token limitado no se presenta como si fuera aquella ejecucion de servicio.
- PA01 tiene una guia propia en `adicionales/PA01/README.md`. Usa el JAR y los dist existentes en un entorno temporal, no instala paquetes ni reinicia el servicio original. No prueba login ni toda la interfaz visual.

Los objetivos RTO y RPO siguen en revision. Las mediciones de laboratorio no acreditan un objetivo de recuperacion acordado.

## Resultados y evidencias

Los resumenes quedan en `evidencias-mantenimiento/Evidencia_Prueba_N/resultado.txt`; PA01 usa `Evidencia_Adicional_1`. Para mostrar el resultado guardado:

```powershell
Get-Content .\evidencias-mantenimiento\Evidencia_Prueba_5\resultado.txt
Get-Content .\evidencias-mantenimiento\Evidencia_Adicional_1\resultado.txt
```

`resultado.json`, los CSV de criterios y los registros de la misma carpeta sustentan el resumen. Mostrar siempre el alcance y la fecha de la ejecucion correspondiente.

`referencias/resultados_instalacion_referencia.json` contiene un resumen sin datos privados de ejecuciones anteriores del equipo. No se trata de una nueva ejecucion sobre este clon ni de una certificacion de la version actual de main.

Las evidencias completas, los respaldos, SQL extraidos, documentos, claves y configuraciones locales se conservan fuera de Git. La revision de codigo y la publicacion de los ejecutores no sustituyen una repeticion de las pruebas en el entorno configurado.
