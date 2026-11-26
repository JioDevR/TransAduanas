# Manual de las 15 pruebas de despliegue

Actualizado el 26 de noviembre de 2026. Este es el único manual de despliegue. Incluye preparación, comandos, resultados esperados y recuperación. Los scripts `.ps1` son herramientas que ejecutan esos pasos.

Este documento usa las carpetas de `main`: `backend-sga`, `app-sga`, `app-b2b` y `app-landing`. La versión se lee del POM de la copia que se está probando: no se presupone 0.5.1, 0.5.2 ni la versión de otro equipo. No cambies la versión de un JAR renombrando el archivo.

## Preparación de cada terminal

Abre PowerShell en la raíz del checkout o del ZIP extraído. Si lo tienes en la ubicación del laboratorio, puedes entrar con:

```powershell
Set-Location "$env:USERPROFILE\Music\TransAduanas"
$p = (Get-Location).Path
$backend = Join-Path $p 'backend-sga'
```

Lee la versión y guarda las rutas cortas:

```powershell
[xml]$pom = Get-Content "$backend\pom.xml" -Raw -ErrorAction Stop
$version = [string]$pom.project.version
$jar = Join-Path $backend "target\sga-backend-$version.jar"
```

```powershell
$kit = Join-Path $p 'docs\pruebas\despliegue\kit-11-15'
$r = "$env:USERPROFILE\Music\TransAduanas-desplegado"
$xml = "$env:USERPROFILE\Music\TransAduanas-herramientas\winsw\TransAduanasBackend.xml"
```

Repite esta preparación en cada terminal nueva, también en la de administrador: no comparten variables. Las asignaciones no imprimen mensajes. Si la carpeta del proyecto es otra, ajusta `Set-Location` antes de definir `$p`. Un error que menciona `C:\backend-sga` o una ruta que empieza en `\docs` suele indicar que faltó definir una variable.

Requisitos: Java 21, Node/npm para compilar interfaces, MySQL con los datos del laboratorio, Nginx, Nexus cuando corresponda y el servicio WinSW preparado para las pruebas 13 a 15. El XML privado, contraseñas, JAR históricos y herramientas se mantienen fuera de Git. El ZIP no instala servicios automáticamente.

No ejecutes `schema.sql` ni `seed.sql` sobre la base existente para repetir estas pruebas. Los builds modifican `target` y `dist`; no recompiles el JAR mientras se ejecuta manualmente desde `target`. El servicio usa la copia estable en `$r\current`.

## Usar el sistema que ya está desplegado

No hace falta VS Code, Maven, npm ni otro JAR manual. Consulta:

```powershell
Get-Service MySQL84, TransAduanasBackend
Get-Process nginx -ErrorAction SilentlyContinue
```

MySQL y el backend deben estar `Running`. Si están detenidos, inícialos en PowerShell como administrador:

```powershell
Start-Service MySQL84
Start-Service TransAduanasBackend
```

Si Nginx no tiene procesos, define su carpeta e inícialo:

```powershell
$nginx = "$env:USERPROFILE\Downloads\nginx-1.31.8\nginx-1.31.8"
Start-Process "$nginx\nginx.exe" -WorkingDirectory $nginx -WindowStyle Hidden
```

La ruta de Nginx es un ejemplo del laboratorio: ajústala si tu instalación está en otro lugar. No inicies otra instancia si ya está activo. Para aplicar una ruta de proyecto nueva, usa la prueba 10.

Comprueba la respuesta:

```powershell
(Invoke-RestMethod 'http://localhost:5180/api/v1/health' -ErrorAction Stop).data.status
```

Debe mostrar `UP`. Después abre [SGA](http://localhost:5180), [Portal B2B](http://localhost:5174) o [Landing](http://localhost:5173). `localhost` corresponde al dispositivo donde se abre el navegador. Estas direcciones no demuestran una instalación en otro equipo ni una publicación en Internet.

Si MySQL o el backend se detienen, Nginx no podrá consultar el negocio. Si Nginx se detiene, las interfaces no aceptarán conexiones aunque el backend responda. Después de reiniciar o suspender la computadora, revisar los componentes de nuevo.

## 1. Entorno y validación Maven

**Terminal normal.** Comprueba herramientas y POM, sin iniciar la aplicación:

```powershell
Set-Location $backend
java -version
.\mvnw.cmd -v
```

```powershell
.\mvnw.cmd validate
```

**Resultado y captura:** Java 21, versión de Maven y `BUILD SUCCESS`. Demuestra que Maven puede procesar el proyecto; no que la aplicación esté funcionando.

## 2. Compilación limpia

**Terminal normal, sin un JAR manual ejecutándose desde target.**

```powershell
Set-Location $backend
.\mvnw.cmd clean compile
```

**Resultado y captura:** Maven elimina `target`, compila las fuentes y termina con `BUILD SUCCESS`. Demuestra que puede regenerar la compilación desde el código. La cantidad de archivos puede cambiar entre versiones.

## 3. Funciones antes de desplegar

```powershell
Set-Location $backend
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest"
```

**Resultado y captura:** `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0` y `BUILD SUCCESS`. Comprueba las 15 funciones seleccionadas por el pipeline. H2 es una base temporal en memoria; no se instala a mano y no utiliza el MySQL original.

Si aparece una clase compilada faltante, repetir con `clean test` y la misma selección. No atribuirlo a H2 sin leer el primer `Caused by`. Estas 15 comprobaciones no equivalen a todas las suites del repositorio.

## 4. Paquete ejecutable JAR

Después de aprobar la prueba 3 y sin cambiar fuentes:

```powershell
Set-Location $backend
.\mvnw.cmd package "-DskipTests"
```

```powershell
jar tf $jar | Select-String 'SgaApplication.class|application-staging.properties|BOOT-INF/lib/spring-boot-[0-9]'
Get-FileHash $jar
```

**Resultado y captura:** `BUILD SUCCESS`, clase de inicio, configuración staging y biblioteca Spring Boot dentro del JAR. `skipTests` evita repetir la prueba anterior; no acredita una ejecución nueva de pruebas. Conservar el hash de este binario, no exigir el de una compilación histórica.

## 5. Instalación Maven local

```powershell
Set-Location $backend
.\mvnw.cmd install "-DskipTests"
```

```powershell
$jarInstalado = "$env:USERPROFILE\.m2\repository\pe\sga\sga-backend\$version\sga-backend-$version.jar"
Get-FileHash $jar, $jarInstalado | Format-List Path, Hash
```

**Resultado y captura:** `BUILD SUCCESS` y dos hashes iguales. Maven guardó el mismo contenido en su repositorio local. No inició la aplicación ni publicó en Nexus. Si usas un repositorio Maven local personalizado, ajusta `$jarInstalado`.

## 6. Configuración del entorno de despliegue

```powershell
Get-Content "$backend\src\main\resources\application-staging.properties"
```

**Resultado y captura:** puerto predeterminado 8086, variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `STORAGE_DIR`, esquema en `validate` e inicializador desactivado.

`${NOMBRE}` significa que el valor se toma del entorno. Por ejemplo, `STORAGE_DIR` aporta la carpeta de documentos. No mostrar valores privados. Esta prueba revisa lo declarado; la 7 comprueba el arranque con esa configuración.

## 7. Arranque manual del backend

**Administrador:** detener el servicio del laboratorio para liberar el puerto:

```powershell
Stop-Service TransAduanasBackend
Get-NetTCPConnection -LocalPort 8086 -State Listen -ErrorAction SilentlyContinue
```

Si aparece otra conexión, identifica el proceso antes de continuar. Para otro backend manual, usa Ctrl+C en su terminal; no cierres procesos Java desconocidos.

**Terminal normal:** repite la preparación de variables de este manual y carga el XML privado sin mostrar sus valores:

```powershell
$c = [xml](Get-Content $xml -Raw -ErrorAction Stop)
foreach ($e in $c.service.env) { [Environment]::SetEnvironmentVariable($e.name, $e.value, 'Process') }
```

```powershell
Set-Location $backend
java -jar $jar --spring.profiles.active=staging --spring.config.import=
```

El perfil staging valida el esquema y desactiva el inicializador. Mantener esta terminal abierta para las pruebas 8 a 10. Desde otra terminal:

```powershell
(Invoke-RestMethod 'http://localhost:8086/api/v1/health' -ErrorAction Stop).data.status
```

**Resultado y captura:** perfil staging, conexión y esquema sin errores, `Tomcat started`, `Started SgaApplication` y `UP`. Si el XML usa otro SERVER_PORT, ajústalo para este recorrido en esa terminal. Si falta `JWT_SECRET`, repetir la carga del XML en la terminal del JAR. La clave JWT no es la contraseña MySQL.

`UP` por sí solo no demuestra que las funciones de negocio estén disponibles ni identifica con certeza el binario; comprobar el JAR/hash y la prueba 8.

## 8. Consulta de los datos originales

Si las interfaces todavía no se han compilado y publicado, preparar primero los pasos 9 y 10. En [SGA](http://localhost:5180), iniciar sesión, entrar a Clientes y buscar Camposol.

En otra terminal, entrar a MySQL con la contraseña solicitada de forma interactiva:

```powershell
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe' -u root -p sgadb
```

Dentro de MySQL, ejecutar:

```sql
SELECT id_cliente, razon_social FROM cliente WHERE razon_social LIKE '%Camposol%';
```

**Resultado y captura:** el registro mostrado en SGA coincide con SQL. No forzar un ID histórico si los datos cambiaron. Escribir `exit` para salir de MySQL. Esto demuestra lectura de negocio desde el despliegue, no solo respuesta del endpoint health.

## 9. Construcción de las tres interfaces

**Terminal normal, desde la raíz.** Definir las direcciones de staging:

```powershell
Set-Location $p
$env:VITE_API_URL = '/api/v1'
$env:VITE_SGA_APP_URL = 'http://localhost:5180'
```

```powershell
$env:VITE_PORTAL_CLIENTE_URL = 'http://localhost:5174'
```

Instalar desde los lockfiles y comprobar cada resultado antes de continuar:

```powershell
npm.cmd --prefix .\app-sga ci
npm.cmd --prefix .\app-b2b ci
npm.cmd --prefix .\app-landing ci
```

```powershell
npm.cmd --prefix .\app-sga run build -- --mode staging
npm.cmd --prefix .\app-b2b run build -- --mode staging
npm.cmd --prefix .\app-landing run build -- --mode staging
```

**Resultado y captura:** cada build termina sin errores y genera `dist/index.html`. Capturar el nombre de la interfaz y `built in ...`; no hace falta mostrar todos los assets. No usar `npm run dev` para demostrar publicación del build. Los avisos de tamaño no equivalen a un fallo de compilación.

## 10. Publicación con Nginx y recarga interna

**Terminal normal.** Nginx debe apuntar a la carpeta real de esta copia, no al nombre de otra extracción. Con los tres dist disponibles:

```powershell
$nginx = "$env:USERPROFILE\Downloads\nginx-1.31.8\nginx-1.31.8"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$p\docs\pruebas\despliegue\Preparar-Nginx.ps1" -Nginx $nginx -Proyecto $p
```

El script respalda `nginx.conf` y ejecuta `nginx -t`. Solo continuar si pasa. Si Nginx está detenido:

```powershell
Start-Process "$nginx\nginx.exe" -WorkingDirectory $nginx -WindowStyle Hidden
```

Si ya está activo, usar **en cambio** una recarga:

```powershell
Push-Location $nginx
.\nginx.exe -s reload
Pop-Location
```

No ejecutar ambas alternativas. Si la recarga devuelve `Access is denied`, usar una terminal como administrador con `$nginx` definido de nuevo; no declarar aplicado un cambio que no se recargó.

```powershell
foreach ($puerto in 8086,5180,5174) {
    "$puerto : $((Invoke-RestMethod "http://localhost:$puerto/api/v1/health" -ErrorAction Stop).data.status)"
}
```

Abrir las tres páginas y [Clientes](http://localhost:5180/recepcion-comercial?tab=clientes), y recargar con Ctrl+R.

**Resultado y captura:** `UP` en las tres consultas, páginas disponibles y ruta interna sin 404/500. Si API funciona pero las páginas devuelven 500, comprobar los `root` de `nginx.conf` y los `dist/index.html`: se observó ese error al cambiar el nombre de la carpeta sin actualizar Nginx.

La configuración activa está fuera del repositorio, en la instalación de Nginx. Fusionar un PR o extraer un ZIP no la actualiza automáticamente. El formato de acceso generado incluye servicio, interfaz, endpoint e identificador de solicitud para facilitar el diagnóstico.

## 11. Automatización local

**Terminal normal.** Antes, detener el JAR manual con Ctrl+C; si el servicio está activo, detenerlo como administrador. El script rechaza comenzar con el puerto 8086 ocupado. No usar las interfaces durante su reconstrucción.

Comando completo, desde cualquier ubicación con `$p` preparado:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$p\docs\pruebas\despliegue\Preparar-Artefactos-Local.ps1" -Proyecto $p
```

**Resultado y captura:** 15 pruebas sin fallos, JAR de la versión del POM, tres dist y `resultado.txt` con `CORRECTO`. Los resultados quedan fuera del checkout, en `Music\TransAduanas-evidencias\pipeline-local-*`. El script también admite un ZIP sin `.git`: registra esa condición y conserva hashes de fuentes, sin atribuir un commit de otra carpeta.

Consulta corta del resultado guardado, que **no vuelve a ejecutar** la prueba:

```powershell
$base = "$env:USERPROFILE\Music\TransAduanas-evidencias"
$e = Get-ChildItem $base -Directory -Filter 'pipeline-local-*' | Sort-Object Name -Descending | Select-Object -First 1
```

```powershell
Get-Content (Join-Path $e.FullName 'resultado.txt')
Get-ChildItem $e.FullName -Name
```

Si no hay una carpeta de ejecución, ejecutar primero el comando completo. «Falta validar el despliegue» significa que todavía faltan arranque, proxy y función de negocio. No es una ejecución de GitHub Actions. Si terminas aquí, recuperar el servicio del laboratorio con `Start-Service TransAduanasBackend` como administrador y verificar health.

## 12. Publicación en Nexus

Comprobar primero si la versión del POM ya existe en Nexus. No sobrescribir releases históricas. Si existe con otro contenido, preparar una versión nueva antes de empezar el recorrido y repetir los pasos necesarios; no cambiar solo el nombre del archivo.

Si Nexus está detenido, como administrador:

```powershell
Start-Service SonatypeNexusRepository
```

Esperar a que [Nexus](http://127.0.0.1:8081) cargue. Para publicar por primera vez, en terminal normal con las rutas preparadas:

```powershell
$cred = Get-Credential -Message 'Cuenta de publicacion de Nexus local'
$env:NEXUS_USERNAME = $cred.UserName
$env:NEXUS_PASSWORD = $cred.GetNetworkCredential().Password
```

```powershell
try { & "$kit\Publicar-Nexus.ps1" -Proyecto $p }
finally { Remove-Item Env:NEXUS_USERNAME, Env:NEXUS_PASSWORD -ErrorAction SilentlyContinue; $cred = $null }
```

**Resultado y captura:** Maven deploy `BUILD SUCCESS`, `DescargaCoincide=True`, JAR y POM visibles en Browse → maven-releases → pe → sga → sga-backend → versión. El script vuelve a probar y construir; conservar el hash del archivo publicado para los siguientes pasos.

401/403 indica autenticación/permisos; 409 puede indicar una release ya existente. Descargar y comparar una versión publicada demuestra disponibilidad, no una publicación nueva. Sin credenciales, marcar pendiente. No publicar credenciales ni mostrar su valor en capturas.

## 13. Despliegue del backend

**Administrador:** repetir la preparación de rutas en esta terminal. Detener un backend manual si sigue abierto. Leer el estado antes de cambiarlo:

```powershell
Get-Content "$r\estado.json"
Get-FileHash $jar
```

Para un JAR descargado de Nexus, ajustar `$jar` y comprobar su hash. Si el archivo pertenece a otra versión, ajustar `$version` a su identidad real, no a un POM diferente. Un despliegue desde un JAR local no acredita su procedencia Nexus.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$kit\Gestionar-Despliegue.ps1" -Accion Desplegar -Jar $jar -Version $version
```

```powershell
Get-Content "$r\estado.json"
Get-FileHash "$r\current\app.jar", $jar
(Invoke-RestMethod 'http://localhost:5180/api/v1/health').data.status
```

**Resultado y captura:** `Operacion completada: Desplegar`, `Health: UP`, Actual con el destino esperado, Anterior con el origen real y hashes coincidentes. Recargar SGA y buscar Camposol antes de aprobar.

### Recuperar o repetir la prueba 13

«Artefactos idénticos» significa que no hay cambio de contenido. Para preparar una repetición, leer `estado.json` y confirmar que Anterior sea exactamente la versión que se desea recuperar. Solo entonces:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$kit\Gestionar-Despliegue.ps1" -Accion Revertir
Get-Content "$r\estado.json"
```

Debe mostrar `Operacion completada: Revertir` y `Health: UP`. Comprobar el hash, el proxy y Camposol. Después se puede repetir Desplegar con el JAR de destino. No fijar que el origen sea siempre 0.5.0: consultar el estado real.

Si el despliegue falla, el gestor intenta recuperar automáticamente el JAR previo. Si ya lo recuperó, otra reversión podría volver a cambiar de versión. Revisar mensajes, estado y salud antes de pedirla. No editar `estado.json` para simular un cambio.

## 14. Servicio Windows y registros de JVM

**Administrador**, después de la prueba 13:

```powershell
Stop-Service TransAduanasBackend
Get-Service TransAduanasBackend
```

Capturar `Stopped`. Después:

```powershell
Start-Service TransAduanasBackend
Get-Service TransAduanasBackend
```

Esperar el arranque de la aplicación y comprobar:

```powershell
(Invoke-RestMethod 'http://localhost:8086/api/v1/health').data.status
Get-Content "$r\logs\gc.log" -Tail 8
```

**Resultado y captura:** `Stopped`, `Running`, `UP` y líneas GC reales. `Running` no garantiza que el arranque haya terminado. Si gc.log no existe, revisar los argumentos de WinSW; no simularlo. Los valores de memoria y pausas pueden variar: esto permite diagnóstico y no demuestra por sí mismo una mejora de rendimiento.

## 15. Recuperación de la versión anterior

**Administrador.** Confirmar qué contienen Actual y Anterior. Solo continuar si el destino anterior es el que se quiere recuperar:

```powershell
Get-Content "$r\estado.json"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$kit\Gestionar-Despliegue.ps1" -Accion Revertir
```

```powershell
Get-Content "$r\estado.json"
Get-FileHash "$r\current\app.jar"
(Invoke-RestMethod 'http://localhost:5180/api/v1/health').data.status
```

**Resultado y captura:** versión anterior con su hash conservado, `UP`, página disponible y consulta de Camposol. Recupera únicamente el JAR; no restaura MySQL ni frontends. Revertir intercambia Actual y Anterior: no repetirlo sin leer el estado.

Si se necesita terminar con la versión de destino activa, volver a ejecutar el comando Desplegar de la prueba 13 y repetir las comprobaciones de negocio.

## VS Code y proyectos Maven duplicados

`.vscode/settings.json` excluye las carpetas de evidencias de monitoreo y mantenimiento de la importación Java. Las evidencias no se borran y pueden abrirse normalmente; se evita que sus POM de respaldo se importen como otro backend con el mismo groupId/artifactId.

Si el aviso ya estaba en caché, guardar el trabajo y ejecutar `Java: Clean Java Language Server Workspace`. Esta operación del editor no inicia ni detiene MySQL, el servicio o Nginx.

## Archivos y alcance

Se conservan el pipeline local, el generador Nginx, los scripts de preparar servicio/publicar/desplegar y `settings-nexus.xml.example`. Son necesarios para ejecutar las pruebas, no manuales duplicados. El XML real de WinSW, `nginx.conf` activo, datos, respaldos, herramientas, `node_modules`, `target` y `dist` no forman parte de este cambio.

El workflow `.github/workflows/preparar-artefactos.yml` se activa manualmente con `workflow_dispatch`; debe estar en la rama predeterminada para mostrar la opción de ejecución. Compila y conserva artefactos/reportes, pero no instala los servicios del laboratorio ni despliega en Nexus o WinSW. Las evidencias del pipeline local no acreditan una ejecución de GitHub Actions.

Cada resultado debe comprobarse en la copia y versión que se estén usando. Este manual no convierte resultados históricos en aprobaciones de una nueva ejecución. Para demostrar traslado a otro equipo hay que configurar y probar ese destino aparte.

## Validación de estas correcciones

El 26 de noviembre de 2026, sobre main `37da034`, se ejecutaron `clean verify -Dtest=BusinessFunctionalCasesTest` (15 pruebas sin fallos, errores u omisiones) y los tres builds staging desde sus lockfiles. Los cinco scripts y los 44 bloques PowerShell del manual pasaron el analizador. Nginx aceptó la configuración generada en una carpeta aislada.

No se ejecutó el pipeline completo ni GitHub Actions, ni se publicaron releases en Nexus o se detuvo/revirtió el servicio del laboratorio para esta revisión. Las compilaciones se hicieron en una copia separada. La versión del POM de ese main era 0.5.1; no se cambió a la versión de otra copia local.
