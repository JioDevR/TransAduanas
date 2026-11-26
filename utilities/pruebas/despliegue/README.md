# Manual de las 15 pruebas de despliegue

Este es el único manual de despliegue de esta entrega. Reúne preparación, comandos, explicación, evidencias y recuperación. Los archivos `.ps1` y la plantilla XML que lo acompañan son herramientas ejecutables, no otros manuales.

El recorrido corresponde a `backend-sga`, `app-sga`, `app-b2b` y `app-landing`, después de descargar esta rama actualizada o fusionarla con main. Se realizó en un laboratorio Windows local; no se presenta como evidencia de instalación en otro equipo ni publicación en Internet.

## Cómo usarlo

1. Revisar la preparación de abajo y después seguir las pruebas 1 a 15 en orden.
2. Copiar solo el contenido de cada bloque de comandos. Ejecutar las líneas una por una en la terminal indicada.
3. Si una prueba falla, conservar el error y no continuar como si hubiera aprobado.
4. Tomar captura del comando y del resultado indicado en «Qué mostrar». Las consultas de registros anteriores no equivalen a ejecutar la prueba otra vez.

Las pruebas 1 a 12 usan principalmente PowerShell normal. Las acciones de servicios y las pruebas 13 a 15 requieren PowerShell como administrador. Los bloques de cada prueba incluyen sus rutas necesarias: no dependen de variables definidas en otra terminal.

**Una terminal normal y otra como administrador no comparten sus variables.** Una asignación como `$r = ...` no muestra mensajes: guarda una ruta. Si se cierra la terminal, hay que repetir las asignaciones correspondientes.

## Preparación del laboratorio

### Herramientas y archivos necesarios

- Java 21 y el Maven Wrapper incluido en `backend-sga`.
- Node y npm para compilar las interfaces; sus dependencias se instalan desde los lockfiles.
- MySQL con la base original `sgadb`; Nginx para publicar las interfaces.
- Nexus y el usuario de publicación ya preparado para las pruebas 12 y 13.
- WinSW con el servicio `TransAduanasBackend`, su XML privado y los JAR históricos 0.5.0 y 0.5.1 conservados para las pruebas 13 a 15.

Los comandos usan `Music\TransAduanas` como carpeta del proyecto. Si está en otro lugar, cambiar esa ruta. No ejecutar `schema.sql` o `seed.sql` para repetir estas pruebas: no hace falta borrar los datos originales.

### Consultar los servicios

```powershell
Get-Service MySQL84, SonatypeNexusRepository, TransAduanasBackend
Get-Process nginx -ErrorAction SilentlyContinue
```

`Running` significa servicio encendido; `Stopped`, detenido. Si la segunda consulta no muestra procesos, Nginx no está ejecutándose. Estas consultas no inician ni detienen nada, y por sí solas no demuestran respuesta HTTP.

Si MySQL está detenido, iniciarlo como administrador:

```powershell
Start-Service MySQL84
```

### Preparar Nginx con las carpetas nuevas

Hacerlo una vez, después de tener los tres `dist` compilados. Si todavía no existen, usar los comandos de la prueba 9 durante esta preparación. En la demostración se pueden volver a compilar.

```powershell
$p = "$env:USERPROFILE\Music\TransAduanas"
$nginx = "$env:USERPROFILE\Downloads\nginx-1.31.8\nginx-1.31.8"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$p\docs\pruebas\despliegue\Preparar-Nginx.ps1" -Nginx $nginx
```

El script conserva un respaldo de `nginx.conf`, genera las rutas actuales y ejecuta `nginx -t`. Debe mostrar configuración válida. No inicia ni recarga el servidor. Si cambias de terminal, vuelve a definir `$nginx` antes de los bloques siguientes.

Si Nginx está detenido:

```powershell
Start-Process "$nginx\nginx.exe" -WorkingDirectory $nginx -WindowStyle Hidden
```

Si ya estaba funcionando, aplicar la configuración nueva mediante una recarga:

```powershell
Push-Location $nginx
.\nginx.exe -s reload
Pop-Location
```

Mantener Nginx activo para las pruebas 8, 10 y 13 a 15. El backend manual de la prueba 7 debe permanecer abierto hasta la 10 y detenerse con Ctrl+C antes de la 11.

### Direcciones de las páginas

- [SGA](http://localhost:5180)
- [Clientes de SGA](http://localhost:5180/recepcion-comercial?tab=clientes)
- [Portal de clientes](http://localhost:5174)
- [Landing](http://localhost:5173)
- [Nexus](http://127.0.0.1:8081)

`localhost` apunta al equipo donde se abre el navegador. Estas direcciones no son direcciones públicas.

## Recorrido de pruebas

### Prueba 1 — Entorno y validación Maven

**Objetivo:** Se verificaron Java 21.0.13, Maven 3.9.16 y la configuración Maven de TransAduanas.

**Dónde:** PowerShell normal, desde backend-sga.

```powershell
Set-Location "$env:USERPROFILE\Music\TransAduanas\backend-sga"
```

**Ejecutar:**

```powershell
java --version
.\mvnw.cmd -v
.\mvnw.cmd validate
```

**Qué mostrar y capturar:** Java 21, Maven 3.9.16 y BUILD SUCCESS.

**Qué explicar:** “Compruebo que tenemos las herramientas necesarias y que Maven puede procesar el proyecto. Todavía no estoy iniciando la aplicación.”

### Prueba 2 — Compilación limpia

**Objetivo:** Maven eliminó target y compiló nuevamente los 193 archivos Java del backend sin errores.

**Dónde:** PowerShell normal, desde backend-sga. Las copias de despliegue están fuera de target.

```powershell
Set-Location "$env:USERPROFILE\Music\TransAduanas\backend-sga"
```

**Ejecutar:**

```powershell
.\mvnw.cmd clean compile
```

**Qué mostrar y capturar:** Deleting ...target, Compiling 193 source files y BUILD SUCCESS.

**Qué explicar:** “Quito la compilación anterior y compruebo que el código actual puede generar nuevamente sus archivos. Esto es necesario antes de crear el paquete de despliegue.”

### Prueba 3 — Funciones antes de desplegar

**Objetivo:** Las 15 pruebas funcionales seleccionadas finalizaron sin fallos, errores ni omisiones.

**Dónde:** PowerShell normal, desde backend-sga. Se usa H2 aislado, no la base MySQL original.

```powershell
Set-Location "$env:USERPROFILE\Music\TransAduanas\backend-sga"
```

**Ejecutar:**

```powershell
.\mvnw.cmd clean test "-Dtest=BusinessFunctionalCasesTest"
```

**Qué mostrar y capturar:** Tests run: 15, Failures: 0, Errors: 0, Skipped: 0 y BUILD SUCCESS.

**Qué explicar:** “Antes de desplegar compruebo que las funciones seleccionadas siguen respondiendo correctamente. Esto no garantiza que todo el sistema esté libre de errores.”

**H2 aislado:** base temporal en memoria creada por las pruebas. No se instala ni se configura a mano y no usa los datos originales de MySQL. `clean` regenera las clases para evitar archivos compilados faltantes.

### Prueba 4 — Paquete ejecutable JAR

**Objetivo:** Se generó sga-backend-0.5.1.jar y se identificaron la clase principal, la configuración staging y la biblioteca Spring Boot.

**Dónde:** PowerShell normal, desde backend-sga. No cambiar el código después de la prueba 3.

```powershell
Set-Location "$env:USERPROFILE\Music\TransAduanas\backend-sga"
```

**Generar y revisar:**

```powershell
.\mvnw.cmd package "-DskipTests"
jar tf .\target\sga-backend-0.5.1.jar | Select-String 'SgaApplication.class|application-staging.properties|BOOT-INF/lib/spring-boot-[0-9]'
```

**Qué mostrar y capturar:** BUILD SUCCESS y las tres entradas del JAR. SkipTests evita repetir la prueba anterior.

**Qué explicar:** “Genero el archivo que vamos a desplegar y reviso componentes de su contenido. El arranque lo comprobaremos en otra prueba.”

### Prueba 5 — Instalación en el repositorio local

**Objetivo:** Maven guardó la versión 0.5.1 en .m2. Los hashes del JAR generado y el instalado coincidieron.

**Dónde:** PowerShell normal, desde backend-sga.

```powershell
Set-Location "$env:USERPROFILE\Music\TransAduanas\backend-sga"
```

**Instalar y comparar:**

```powershell
.\mvnw.cmd install "-DskipTests"
Get-FileHash -LiteralPath ".\target\sga-backend-0.5.1.jar", "$env:USERPROFILE\.m2\repository\pe\sga\sga-backend\0.5.1\sga-backend-0.5.1.jar" | Format-List Path, Hash
```

**Qué mostrar y capturar:** Installing..., BUILD SUCCESS y dos hashes iguales.

**Qué explicar:** “Compruebo que Maven guardó una copia idéntica del paquete. Esto lo almacena en mi equipo; no inicia la aplicación ni publica en Nexus.”

### Prueba 6 — Configuración para ejecutar la aplicación

**Objetivo:** Se revisó el archivo staging: puerto 8086, variables externas, esquema en modo validate e inicializador desactivado.

**Dónde:** PowerShell normal, desde backend-sga. Mostrar el archivo de configuración del entorno, no el XML privado.

```powershell
Set-Location "$env:USERPROFILE\Music\TransAduanas\backend-sga"
```

**Mostrar:**

```powershell
Get-Content .\src\main\resources\application-staging.properties
```

**Qué mostrar y capturar:** Las opciones del entorno; no debe verse una contraseña real.

**Qué explicar:** “La prueba 1 revisa cómo construir el proyecto con Maven. Esta revisa cómo debe arrancar la aplicación en el entorno elegido. Su aplicación real se confirma en la 7.”

**Sobre `${...}`:** el valor se lee de una variable de entorno. `JWT_SECRET` aporta la clave de tokens; `CORS_ALLOWED_ORIGINS`, las direcciones permitidas; `STORAGE_DIR`, la carpeta de documentos. Leer el archivo revisa lo declarado; la prueba 7 comprueba que el backend puede utilizarlo.

### Prueba 7 — Arranque manual del JAR

**Objetivo:** El JAR 0.5.1 inició con staging en el puerto 8086 y respondió UP.

**Dónde:** Usar dos terminales: una como administrador para detener el servicio y otra normal, ubicada en backend-sga, para arrancar el JAR. Cargar las variables en la terminal del JAR.

```powershell
Set-Location "$env:USERPROFILE\Music\TransAduanas\backend-sga"
```

Definir la ruta privada en la terminal normal donde se ejecutará el JAR:

```powershell
$xml = "$env:USERPROFILE\Music\TransAduanas-herramientas\winsw\TransAduanasBackend.xml"
```

**Primero, como administrador:**

```powershell
Stop-Service TransAduanasBackend
Get-NetTCPConnection -LocalPort 8086 -State Listen -ErrorAction SilentlyContinue
```

**Después, en la terminal normal del JAR:**

```powershell
$c = [xml](Get-Content $xml -Raw -ErrorAction Stop)
foreach ($e in $c.service.env) { [Environment]::SetEnvironmentVariable($e.name, $e.value, "Process") }
```

**Arrancar y mantener la terminal abierta:**

```powershell
java -jar .\target\sga-backend-0.5.1.jar --spring.profiles.active=staging
```

**En otra terminal:**

```powershell
(Invoke-RestMethod http://localhost:8086/api/v1/health -ErrorAction Stop).data.status
```

**Qué mostrar y capturar:** Perfil staging, Tomcat en 8086, Started SgaApplication y respuesta UP.

**Qué explicar:** “Inicio la aplicación desde el paquete generado, sin ejecutar el proyecto desde el IDE. Compruebo que arranca y responde; aún no que todas sus funciones trabajen.”

**Si falla:** puerto ocupado → detener el backend manual anterior con Ctrl+C o el servicio indicado; no cerrar otros procesos Java. Falta JWT_SECRET → repetir la lectura del XML en la misma terminal del JAR. `Unable to access jarfile` → comprobar la carpeta y generar el JAR en la prueba 4. La clave JWT no es la contraseña MySQL. No publicar el XML privado.

**Al terminar:** dejar el JAR abierto para las pruebas 8 a 10. Consultar health desde otra terminal. Detener el JAR con Ctrl+C antes de la prueba 11.

### Prueba 8 — Aplicación conectada a MySQL

**Objetivo:** La consulta de Camposol S.A. en SGA coincidió con la base original; el registro SQL tiene ID 1.

**Dónde:** Navegador con SGA en localhost:5180 y backend/MySQL/Nginx activos. En Clientes, buscar Camposol.

**Entrar a MySQL desde PowerShell:**

```powershell
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe' -u root -p sgadb
```

**Dentro de MySQL:**

```sql
SELECT id_cliente, razon_social FROM cliente WHERE razon_social LIKE '%Camposol%';
```

**Qué mostrar y capturar:** La búsqueda en SGA y la fila SQL con Camposol S.A.; comparar los campos visibles.

**Qué explicar:** “Compruebo que la aplicación desplegada puede consultar los datos originales. No basta con que responda UP: aquí vemos un dato de negocio.”

**Página:** [abrir SGA](http://localhost:5180), iniciar sesión, entrar a Clientes y buscar Camposol. Con sesión iniciada también se puede abrir [la ruta de clientes](http://localhost:5180/recepcion-comercial?tab=clientes).

### Prueba 9 — Compilación de las tres interfaces

**Objetivo:** SGA, el portal de clientes y la landing generaron sus archivos dist correctamente.

**Dónde:** PowerShell normal, desde la raíz del proyecto. Dependencias instaladas; no recargar las páginas durante los builds.

```powershell
$p = "$env:USERPROFILE\Music\TransAduanas"
Set-Location $p
```

Definir las direcciones antes de compilar, en esta misma terminal:

```powershell
$env:VITE_API_URL = '/api/v1'
$env:VITE_SGA_APP_URL = 'http://localhost:5180'
$env:VITE_PORTAL_CLIENTE_URL = 'http://localhost:5174'
```

Si faltan dependencias, instalar una vez desde los lockfiles:

```powershell
npm.cmd --prefix .\app-sga ci
npm.cmd --prefix .\app-b2b ci
npm.cmd --prefix .\app-landing ci
```

**Ejecutar cada línea por separado:**

```powershell
npm.cmd --prefix .\app-sga run build -- --mode staging
npm.cmd --prefix .\app-b2b run build -- --mode staging
npm.cmd --prefix .\app-landing run build -- --mode staging
```

**Qué mostrar y capturar:** Nombre de cada interfaz, dist/index.html y built in ... sin errores. No mostrar todos los assets.

**Qué explicar:** “Genero los archivos que el servidor web utilizará para publicar las tres interfaces. No estoy usando el servidor de desarrollo npm run dev.”

### Prueba 10 — Comunicación con Nginx

**Objetivo:** El backend y ambos proxies respondieron UP. La ruta de clientes cargó después de recargar.

**Dónde:** PowerShell normal y navegador. Backend manual y Nginx activos.

**Consultar:**

```powershell
foreach ($puerto in 8086, 5180, 5174) {
    "$puerto : $((Invoke-RestMethod "http://localhost:$puerto/api/v1/health").data.status)"
}
```

**En el navegador, recargar con Ctrl+R:**

http://localhost:5180/recepcion-comercial?tab=clientes

**Qué mostrar y capturar:** 8086, 5180 y 5174 en UP; la página interna recarga sin 404.

**Qué explicar:** “Compruebo que las interfaces publicadas se comunican con el backend mediante Nginx y que la navegación sigue funcionando al recargar.”

### Prueba 11 — Proceso automatizado local

**Objetivo:** Un script ejecutó las 15 pruebas, construyó backend e interfaces y guardó reportes, paquetes y hashes.

**Dónde:** PowerShell normal, desde la raíz. Detener antes el backend manual con Ctrl+C. No usar las páginas mientras se reconstruyen.

```powershell
$p = "$env:USERPROFILE\Music\TransAduanas"
Set-Location $p
```

**Ejecución completa:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$p\docs\pruebas\despliegue\Preparar-Artefactos-Local.ps1"
```

**Consulta corta del resultado conservado:**

```powershell
$base = "$env:USERPROFILE\Music\TransAduanas-evidencias"
$e = Get-ChildItem $base -Directory -Filter "pipeline-local-*" | Sort-Object Name -Descending | Select-Object -First 1
```

Si existe una ejecución guardada, mostrar su resultado:

```powershell
Get-Content (Join-Path $e.FullName "resultado.txt")
Get-ChildItem $e.FullName -Name
```

**Qué mostrar y capturar:** 15 pruebas correctas y cuatro construcciones; Automatizacion local completada. La consulta corta muestra CORRECTO y los archivos guardados.

**Qué explicar:** “Compruebo que los pasos se pueden repetir automáticamente. La consulta corta solo muestra el registro anterior; no ejecuta otra vez la prueba. No usamos GitHub Actions.”

La consulta corta muestra la ejecución guardada más reciente. Si no hay carpetas `pipeline-local-*`, no hay un registro que consultar: ejecutar primero el proceso completo. La frase «Falta validar el despliegue» significa que aún deben comprobarse el arranque, proxy y funciones del sistema.

### Prueba 12 — Versión del proyecto en Nexus

**Objetivo:** Se publicó 0.5.1 en maven-releases y el archivo descargado coincidió con el original.

**Dónde:** Nexus debe estar Running y su página cargada. Entrar con transaduanas-deploy.

**Si Nexus está detenido, como administrador:**

```powershell
Start-Service SonatypeNexusRepository
```

**Cuando Nexus haya terminado de iniciar:**

```powershell
Start-Process http://127.0.0.1:8081
```

**En pantalla:**

Browse → maven-releases → pe → sga → sga-backend → 0.5.1
Seleccionar sga-backend-0.5.1.jar y mostrar los detalles y el POM.

**Qué mostrar y capturar:** Repositorio, versión 0.5.1, JAR y POM. Complementar con el registro original: BUILD SUCCESS y DescargaCoincide True.

**Qué explicar:** “Aquí se ve el paquete que ya publicamos. Compruebo su disponibilidad, no una publicación nueva. Repetir 0.5.1 da 409 porque no se permite sobrescribirla.”

### Prueba 13 — Despliegue automático del backend

**Objetivo:** El script desplegó el JAR verificado de Nexus. El backend y el proxy respondieron UP y Camposol siguió disponible.

**Dónde:** PowerShell como administrador, desde la raíz. Servicio instalado; backend manual detenido. Descargar el JAR de Nexus a Descargas. Antes de repetir, estado.json debe indicar actual 0.5.0.

**Definir en esta misma terminal como administrador:**

```powershell
$r = "$env:USERPROFILE\Music\TransAduanas-desplegado"
$kit = "$env:USERPROFILE\Music\TransAduanas\docs\pruebas\despliegue\kit-11-15"
$jar = "$env:USERPROFILE\Downloads\sga-backend-0.5.1.jar"
```

**Verificar el archivo y el estado actual:**

```powershell
Get-FileHash $jar | Format-List
Get-Content "$r\estado.json"
```

**Desplegar:**

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$kit\Gestionar-Despliegue.ps1" -Accion Desplegar -Jar $jar -Version 0.5.1
```

**Después, comprobar:**

```powershell
(Invoke-RestMethod http://localhost:5180/api/v1/health).data.status
```

**Qué mostrar y capturar:** Operacion completada: Desplegar, hash B y Health UP. Luego recargar SGA y buscar Camposol.

**Qué explicar:** “El script coloca el paquete y arranca el servicio. Compruebo que la actualización funciona. Esta automatización despliega el backend, no los frontends.”

**Página:** [abrir SGA](http://localhost:5180), iniciar sesión, entrar a Clientes y buscar Camposol. Con sesión iniciada también se puede abrir [la ruta de clientes](http://localhost:5180/recepcion-comercial?tab=clientes).

#### Recuperar o preparar una repetición de la prueba 13

No volver a desplegar el mismo archivo si ya está colocado. Consultar primero `estado.json`. Para volver a 0.5.0, debe indicar **Actual 0.5.1 y Anterior 0.5.0**. En esa misma terminal como administrador:

```powershell
Get-Content "$r\estado.json"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$kit\Gestionar-Despliegue.ps1" -Accion Revertir
```

Debe mostrar `Operacion completada: Revertir` y `Health: UP`. Confirmar Actual 0.5.0 y después repetir el comando Desplegar si se desea demostrar otra vez la actualización a 0.5.1.

Si un despliegue falla, el script intenta recuperar automáticamente el JAR previo. Si ya quedó Actual 0.5.0, no ejecutar otra reversión: intercambiaría las versiones de nuevo. Si informa que no confirmó la recuperación, revisar servicio y logs antes de continuar. No editar `estado.json` para simular un cambio.

### Prueba 14 — Control y diagnóstico del servicio

**Objetivo:** Se detuvo y reinició el servicio; respondió UP y generó el registro GC de Java.

**Dónde:** PowerShell como administrador. Son dos momentos: capturar Stopped y después Running. Esperar el arranque antes de consultar health.

```powershell
$r = "$env:USERPROFILE\Music\TransAduanas-desplegado"
```

**Detener:**

```powershell
Stop-Service TransAduanasBackend
Get-Service TransAduanasBackend
```

**Iniciar:**

```powershell
Start-Service TransAduanasBackend
Get-Service TransAduanasBackend
```

**Cuando la aplicación esté lista:**

```powershell
(Invoke-RestMethod http://localhost:8086/api/v1/health).data.status
Get-Content "$r\logs\gc.log" -Tail 8
```

**Qué mostrar y capturar:** Stopped, Running, UP y líneas gc/heap. La muestra previa fue 176M→44M y pausa de 9,462 ms; las cifras pueden variar.

**Qué explicar:** “Compruebo que puedo administrar el backend como servicio y consultar información de su memoria. No estoy demostrando una mejora de rendimiento.”

### Prueba 15 — Regreso al backend anterior

**Objetivo:** Se recuperó 0.5.0 con su hash original; el sistema respondió UP y volvió a consultar Camposol.

**Dónde:** PowerShell como administrador, desde la raíz. Antes: Actual 0.5.1 y Anterior 0.5.0. Revisarlo porque Revertir intercambia las versiones.

```powershell
$r = "$env:USERPROFILE\Music\TransAduanas-desplegado"
$kit = "$env:USERPROFILE\Music\TransAduanas\docs\pruebas\despliegue\kit-11-15"
```

**Revisar y recuperar:**

```powershell
Get-Content "$r\estado.json"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$kit\Gestionar-Despliegue.ps1" -Accion Revertir
```

**Comprobar el resultado:**

```powershell
Get-Content "$r\estado.json"
Get-FileHash "$r\current\app.jar" | Format-List
(Invoke-RestMethod http://localhost:5180/api/v1/health).data.status
```

**Qué mostrar y capturar:** Operacion completada: Revertir; Actual 0.5.0, hash A y UP. Recargar SGA y buscar Camposol.

**Qué explicar:** “Compruebo que puedo retirar una actualización y volver al backend anterior. Recupero el JAR; no restauro la base de datos ni los frontends.”

**Página:** [abrir SGA](http://localhost:5180), iniciar sesión, entrar a Clientes y buscar Camposol. Con sesión iniciada también se puede abrir [la ruta de clientes](http://localhost:5180/recepcion-comercial?tab=clientes).

## Archivos necesarios que se conservan

| Archivo | Función |
| --- | --- |
| `Preparar-Artefactos-Local.ps1` | Ejecutar las 15 pruebas y construir backend e interfaces, guardando evidencia fuera del repositorio. |
| `Preparar-Nginx.ps1` | Generar y validar la configuración de Nginx para las carpetas actuales. |
| `kit-11-15/Publicar-Nexus.ps1` | Publicar una release nueva y verificar la descarga. Requiere NEXUS_USERNAME y NEXUS_PASSWORD. |
| `kit-11-15/Preparar-Servicio.ps1` | Preparar el XML privado de WinSW antes de instalar el servicio. No ejecutarlo sobre un servicio ya instalado. |
| `kit-11-15/Gestionar-Despliegue.ps1` | Preparar el JAR inicial, desplegar y recuperar la versión anterior. |
| `settings-nexus.xml.example` | Plantilla Maven que lee las credenciales desde variables de entorno. |

No se incluyen contraseñas, XML privados, respaldos SQL ni binarios de herramientas. Los scripts no reemplazan la instalación de MySQL, Nexus, WinSW o Nginx.

## Workflow de GitHub Actions

El archivo `.github/workflows/preparar-artefactos.yml` está incorporado y adaptado a las carpetas actuales. Se activa manualmente con `workflow_dispatch`; para que GitHub muestre esa opción, debe estar en la rama predeterminada. No se ejecuta automáticamente por push o pull request.

Cuando se solicite, preparará Java 21 y Node 24, ejecutará las 15 pruebas, empaquetará el JAR, compilará las tres interfaces y conservará reportes y artefactos durante 14 días. Usa los archivos del commit remoto, no los cambios locales sin subir. Los frontends se construyen por separado y los reportes del backend se intentan conservar aunque falle Maven.

Este workflow no instala MySQL o Nginx del laboratorio, no administra WinSW, no publica automáticamente en Nexus y no despliega la aplicación. Las pruebas usan H2 aislado; la comprobación de datos originales corresponde a MySQL en las pruebas del laboratorio. Conservar un JAR como artifact no equivale a ejecutar Maven deploy.

## Hashes de referencia y versiones

JAR histórico 0.5.0:

```text
D4E6D2DCB1DDAC49B1EF669782817866003D566310FFA42C7FC6FFECB7115258
```

JAR histórico 0.5.1 publicado en Nexus:

```text
C7B9081A5D098894DCCC6F62D283F0EBFA4F18A0622AD83A945C72C52FF58A29
```

La comparación de hashes verifica que se usa el mismo contenido. Una compilación del código actualizado puede generar otro hash: estos valores describen los binarios históricos. No sobrescribir la release 0.5.1 existente en Nexus; si se necesita publicar otro binario, preparar una versión nueva. Después de la prueba 15 queda el backend histórico 0.5.0; eso no cambia la versión del POM del código.

## Comprobación de compatibilidad con main

Validado el 19 de noviembre de 2026, después de integrar main en `0e43733`:

| Comprobación | Resultado obtenido |
| --- | --- |
| Compilación y empaquetado del backend con BusinessFunctionalCasesTest | BUILD SUCCESS; 15 pruebas, cero fallos, errores u omisiones; JAR generado. |
| ControllerFunctionalTest y AllControllersFunctionalTest | BUILD SUCCESS; 239 pruebas, cero fallos, errores u omisiones. |
| npm ci y build staging en app-sga, app-b2b y app-landing | Las tres compilaciones finalizaron y generaron sus dist. |
| Analizador de PowerShell | Cinco scripts sin errores de sintaxis. |
| Generador de configuración y nginx -t | Configuración aceptada en una carpeta de herramientas aislada; no se inició el servidor. |

Estas verificaciones se hicieron en una copia aislada del checkout integrado, sin modificar MySQL ni los servicios Windows del laboratorio. Se adaptaron las rutas y los tests de cotizaciones al constructor actual del controlador.

No se ejecutó GitHub Actions ni se repitieron publicación en Nexus, despliegue y recuperación del servicio real durante esa revisión. Tampoco se ejecutaron todas las suites del proyecto ni una instalación en otro equipo. Los resultados anteriores no garantizan una ejecución nueva: comprobar los servicios, archivos y respuestas indicados en cada prueba.

El XML privado de WinSW y la configuración activa de Nginx están fuera de Git: fusionar o descargar esta rama no los modifica automáticamente.
