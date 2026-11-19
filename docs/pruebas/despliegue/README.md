# Pruebas de despliegue de TransAduanas

[Manual de demostración de las 15 pruebas (Word)](Manual-pruebas-despliegue.docx)

El manual contiene comandos PowerShell, requisitos previos, resultados esperados y explicaciones breves para demostrar las pruebas. Incluye el enlace de SGA y la recuperación de la versión anterior en la prueba 13. Cada ejecución nueva debe comprobarse; los resultados históricos no sustituyen esa comprobación.

## Archivos

| Archivo | Uso |
| --- | --- |
| `Preparar-Artefactos-Local.ps1` | Ejecutar las 15 pruebas seleccionadas, compilar backend y tres interfaces, conservar reportes y hashes. |
| `kit-11-15/Publicar-Nexus.ps1` | Publicar una versión release nueva en Nexus y comparar el JAR descargado con el generado. |
| `settings-nexus.xml.example` | Leer las credenciales Nexus desde variables de entorno. No contiene contraseñas. |
| `kit-11-15/Preparar-Servicio.ps1` | Preparar la configuración del servicio WinSW antes de instalarlo. Requiere el XML privado local. |
| `kit-11-15/Gestionar-Despliegue.ps1` | Preparar el primer artefacto, desplegar otra versión o recuperar la anterior con comprobación de hash y salud. |
| `PREPARACION-CI.md` | Explicar el workflow manual de GitHub Actions y sus límites. |

## Antes de ejecutar

Esta entrega corresponde a la estructura con `backend`, `app-sga`, `app-portal-cliente` y `landing` usada en el laboratorio. La rama principal incorporó posteriormente nombres como `backend-sga`, `app-b2b` y `app-landing`: revisar y adaptar rutas antes de integrar esta rama con esa estructura. No se cambian las carpetas locales ni se fusiona automáticamente con `main`.

Requisitos del laboratorio: Java 21, Node/npm para compilar interfaces, MySQL para los datos originales, Nginx para las interfaces y Nexus para publicación/descarga. WinSW y su XML privado se preparan por separado. No se distribuyen ejecutables de herramientas ni archivos privados.

Las rutas predeterminadas parten de `$env:USERPROFILE\Music\TransAduanas`. Los scripts aceptan parámetros como `-Proyecto` o `-Raiz` para otras ubicaciones. Definir las variables de ruta en cada terminal nueva: una terminal normal y otra como administrador no comparten las variables definidas con `$env:` o `$nombre`.

## Automatización local (prueba 11)

Detener el backend manual antes de ejecutar: el proceso recompila `target` y reconstruye las interfaces. Desde la raíz del proyecto:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\docs\pruebas\despliegue\Preparar-Artefactos-Local.ps1
```

Los resultados se guardan fuera del repositorio, en `Music\TransAduanas-evidencias\pipeline-local-*`. Comprobar 15 pruebas sin errores y cuatro compilaciones completas. Este resultado no valida por sí solo el despliegue ni representa una ejecución de GitHub Actions.

## Despliegue y recuperación (pruebas 13 y 15)

En PowerShell como administrador, definir las rutas en esa misma terminal:

```powershell
$r = "$env:USERPROFILE\Music\TransAduanas-desplegado"
$kit = "$env:USERPROFILE\Music\TransAduanas\docs\pruebas\despliegue\kit-11-15"
$jar = "$env:USERPROFILE\Downloads\sga-backend-0.5.1.jar"
```

Consultar los archivos antes de desplegar:

```powershell
Get-FileHash $jar | Format-List
Get-Content "$r\estado.json"
```

Si la versión actual es 0.5.0, desplegar el JAR 0.5.1 previamente descargado y comprobado:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$kit\Gestionar-Despliegue.ps1" -Accion Desplegar -Jar $jar -Version 0.5.1
```

Para volver a 0.5.0, comprobar primero que `Actual` sea 0.5.1 y `Anterior` sea 0.5.0. Solo en ese caso:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$kit\Gestionar-Despliegue.ps1" -Accion Revertir
```

`Revertir` intercambia las versiones registradas; no es un reinicio. Si un despliegue falla, el script intenta recuperar automáticamente la versión previa: consultar el estado y la salud antes de pedir otra reversión. No editar `estado.json` para simular resultados. Si los artefactos son idénticos, no hay cambio real que aplicar.

Después de desplegar o revertir, comprobar `estado.json`, la respuesta `UP` y una función de negocio:

```powershell
(Invoke-RestMethod 'http://localhost:5180/api/v1/health').data.status
Start-Process 'http://localhost:5180'
```

En SGA, buscar Camposol S.A. y comparar con MySQL. El proxy requiere Nginx activo. [SGA](http://localhost:5180) y [Clientes](http://localhost:5180/recepcion-comercial?tab=clientes) apuntan al equipo donde se abre el navegador.

## Publicación y alcance

La versión release 0.5.1 del laboratorio ya se publicó: no repetir `deploy` sobre ella si Nexus impide sobrescribir releases. Mostrar el archivo en Nexus o preparar una versión nueva cuando corresponda.

Las pruebas funcionales automatizadas usan H2 temporal. Las consultas de negocio del despliegue usan MySQL. El material documenta el laboratorio local; todavía no constituye evidencia de instalación y funcionamiento en un segundo equipo ni de publicación en Internet.

No subir el XML privado de WinSW, credenciales, `application-local.properties`, respaldos de base de datos, JAR, carpetas `dist`, `target` o `node_modules` en este cambio.
