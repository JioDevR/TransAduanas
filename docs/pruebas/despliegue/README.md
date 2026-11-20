# Manual de las 15 pruebas de despliegue — TransAduanas 0.5.2

Actualizado el 20 de noviembre de 2026. Cada prueba indica qué ejecutar y cómo aprobarla. Los resultados esperados deben comprobarse de nuevo; no se presentan los pasos pendientes como ya realizados.

## Preparación y carpetas

Usa una copia extraída del paquete corregido. En PowerShell normal, abre su raíz y define:

```powershell
$p = (Get-Location).Path
$backend = Join-Path $p 'backend-sga'
$jar = Join-Path $backend 'target\sga-backend-0.5.2.jar'
$nginx = "$env:USERPROFILE\Downloads\nginx-1.31.8\nginx-1.31.8"
$r = "$env:USERPROFILE\Music\TransAduanas-desplegado"
$xml = "$env:USERPROFILE\Music\TransAduanas-herramientas\winsw\TransAduanasBackend.xml"
$kit = Join-Path $p 'docs\pruebas\despliegue\kit-11-15'
Get-Item "$backend\pom.xml"
```

En cada terminal nueva debes definir otra vez las variables. PowerShell normal y administrador no comparten sus variables. Para los bloques administrativos abre PowerShell **como administrador**, ubícate en la misma raíz y repite la preparación.

Requisitos: Java 21, Node 24, Maven Wrapper incluido, MySQL con `sgadb` existente, Nginx y servicio WinSW `TransAduanasBackend` instalado. Nexus y credenciales de publicación se necesitan para la prueba 12. No se incluyen claves privadas ni herramientas dentro del paquete. No ejecutes schema.sql ni seed.sql sobre la base existente.

Antes del recorrido, anota el estado:

```powershell
Get-Service MySQL84, SonatypeNexusRepository, TransAduanasBackend
Get-Content "$r\estado.json"
Get-FileHash "$r\current\app.jar"
```

Último estado observado: actual 0.5.0 y anterior 0.5.1. **Léelo de nuevo**: puede cambiar. La versión nueva es 0.5.2. No sobrescribas 0.5.1 en Nexus ni edites estado.json para simular una actualización. Conserva los JAR históricos y el respaldo privado del servicio fuera de Git.

Las pruebas 7 y 13–15 interrumpen el backend brevemente; hazlas en una ventana de laboratorio. Los builds modifican target y dist de esta copia. No recompiles target si el JAR manual se está ejecutando desde allí. El servicio usa una copia estable en `$r\current`, independiente de target.

## 1. Entorno y validación Maven

**Terminal:** normal. **Objetivo:** comprobar herramientas y POM, sin iniciar la aplicación.

```powershell
Set-Location $backend
java -version
.\mvnw.cmd -v
.\mvnw.cmd validate
```

**Aprobación/evidencia:** Java 21, Maven Wrapper disponible y BUILD SUCCESS. No exige que los números de parche coincidan exactamente con una captura histórica.

## 2. Compilación limpia

```powershell
Set-Location $backend
.\mvnw.cmd clean compile
```

**Aprobación/evidencia:** compilación de fuentes y BUILD SUCCESS. En esta entrega son 193 fuentes Java. La cantidad por sí sola no demuestra corrección funcional.

## 3. Funciones antes del despliegue

```powershell
Set-Location $backend
.\mvnw.cmd test "-Dtest=BusinessFunctionalCasesTest"
```

**Aprobación/evidencia:** 15 pruebas CP, cero fallos, errores u omisiones. H2 temporal; no MySQL original. Esta selección complementaria coincide con el pipeline de despliegue. Para las 15 de la sustentación de cuatro tipos usa el [manual funcional](../funcionales/README.md): `-Dgroups=docente15`. No son el mismo conjunto.

## 4. Generación y contenido del JAR

Después de aprobar la prueba 3, sin cambiar fuentes:

```powershell
Set-Location $backend
.\mvnw.cmd package "-DskipTests"
jar tf $jar | Select-String 'SgaApplication.class|application-staging.properties|BOOT-INF/lib/spring-boot-[0-9]'
Get-FileHash $jar
```

**Aprobación/evidencia:** BUILD SUCCESS y entradas de aplicación, perfil staging y Spring Boot. `skipTests` no acredita pruebas nuevas; evita repetir la ejecución anterior. Usa el hash de tu compilación; puede diferir del distribuido.

## 5. Instalación Maven local

```powershell
Set-Location $backend
.\mvnw.cmd install "-DskipTests"
Get-FileHash $jar, "$env:USERPROFILE\.m2\repository\pe\sga\sga-backend\0.5.2\sga-backend-0.5.2.jar"
```

**Aprobación/evidencia:** BUILD SUCCESS y dos hashes iguales. No publica en Nexus ni inicia el backend.

## 6. Configuración staging y secretos externos

```powershell
Get-Content "$backend\src\main\resources\application-staging.properties"
```

**Aprobación/evidencia:** puerto predeterminado 8086, conexión mediante DB_URL/DB_USERNAME/DB_PASSWORD, JWT_SECRET externo, CORS_ALLOWED_ORIGINS, STORAGE_DIR, `ddl-auto=validate` e inicializador desactivado. No muestres el XML privado ni sus valores. JWT_SECRET es obligatorio y debe contener al menos 32 bytes aleatorios; una clave corta ya no se rellena con ceros.

## 7. Arranque manual con MySQL

**Administrador:** detén solo el servicio indicado para liberar 8086:

```powershell
Stop-Service TransAduanasBackend
Get-Service TransAduanasBackend
Get-NetTCPConnection -LocalPort 8086 -State Listen -ErrorAction SilentlyContinue
```

Si 8086 sigue ocupado, identifica el proceso; no termines indiscriminadamente otros procesos Java. En **PowerShell normal**, repite las variables de preparación y carga el entorno sin imprimir secretos:

```powershell
$c = [xml](Get-Content $xml -Raw -ErrorAction Stop)
foreach ($e in $c.service.env) {
    [Environment]::SetEnvironmentVariable($e.name, $e.value, 'Process')
}
Set-Location $backend
java -jar $jar --spring.profiles.active=staging --server.port=8086 --spring.config.import= --spring.jpa.hibernate.ddl-auto=validate --app.data-initializer.enabled=false
```

Mantén esa terminal abierta para las pruebas 8–10. En otra terminal:

```powershell
(Invoke-RestMethod http://localhost:8086/api/v1/health -ErrorAction Stop).data.status
```

**Aprobación/evidencia:** staging, conexión MySQL y validación del esquema sin errores, aplicación iniciada, HTTP 200 y UP. Health devuelve un estado y versión constantes: UP no verifica por sí mismo la conexión de negocio; complétalo con la prueba 8. No pongas H2/create-drop en el laboratorio MySQL.

## 8. Lectura de negocio y comparación SQL

Primero construye/publica las interfaces con la prueba 9 y la preparación de Nginx de la 10 si aún no están disponibles. En SGA, inicia sesión con una cuenta válida del laboratorio, abre Clientes y busca Camposol.

En otra terminal, abre MySQL (solicita la contraseña sin mostrarla):

```powershell
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe' -u root -p sgadb
```

Dentro de MySQL:

```sql
SELECT id_cliente, razon_social FROM cliente WHERE razon_social LIKE '%Camposol%';
```

**Aprobación/evidencia:** el dato de SGA coincide con SQL. En la revisión se obtuvo id 1, Camposol S.A.; no fuerces ese valor si tu base ha cambiado. Captura solo la fila necesaria. Escribe `exit` para salir del cliente SQL.

## 9. Construcción de las tres interfaces

Terminal normal, desde la raíz. Ejecuta cada comando y comprueba su resultado antes de continuar:

```powershell
Set-Location $p
$env:VITE_API_URL = '/api/v1'
$env:VITE_SGA_APP_URL = 'http://localhost:5180'
$env:VITE_PORTAL_CLIENTE_URL = 'http://localhost:5174'
npm.cmd --prefix .\app-sga ci
npm.cmd --prefix .\app-b2b ci
npm.cmd --prefix .\app-landing ci
npm.cmd --prefix .\app-sga run build -- --mode staging
npm.cmd --prefix .\app-b2b run build -- --mode staging
npm.cmd --prefix .\app-landing run build -- --mode staging
```

**Aprobación/evidencia:** los tres builds terminan sin errores y cada carpeta contiene dist/index.html. No uses npm run dev para demostrar publicación del build. Las advertencias de tamaño de chunks deben distinguirse de un fallo de compilación.

## 10. Nginx, proxies y recarga SPA

Con los dist de la prueba 9, genera configuración para **esta copia**. El script respalda nginx.conf y prueba su sintaxis:

```powershell
Set-Location $p
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$p\docs\pruebas\despliegue\Preparar-Nginx.ps1" -Nginx $nginx -Proyecto $p
```

Solo si pasa nginx -t, inicia Nginx si está detenido:

```powershell
Start-Process "$nginx\nginx.exe" -WorkingDirectory $nginx -WindowStyle Hidden
```

Si ya hay Nginx activo, usa **en cambio** una recarga:

```powershell
Push-Location $nginx
.\nginx.exe -s reload
Pop-Location
```

No ejecutes ambas alternativas. Comprueba:

```powershell
foreach ($puerto in 8086, 5180, 5174) {
    "$puerto : $((Invoke-RestMethod "http://localhost:$puerto/api/v1/health" -ErrorAction Stop).data.status)"
}
(Invoke-WebRequest http://localhost:5173 -UseBasicParsing).StatusCode
```

Abre http://localhost:5180/recepcion-comercial?tab=clientes y recarga con Ctrl+R. Abre también http://localhost:5174 y http://localhost:5173.

**Aprobación/evidencia:** tres UP, landing 200, páginas visibles y ruta interna recargada sin 404. nginx -t sin servidor activo no basta. Si hay conexión rechazada, verifica el proceso Nginx; 502 indica que el proxy no alcanza el backend. Conservar la configuración respaldada permite recuperar las rutas anteriores.

Al terminar, detén el backend **manual** con Ctrl+C. Si vas a interrumpir el recorrido aquí, inicia de nuevo el servicio original como administrador y consulta health; no dejes el laboratorio detenido.

## 11. Proceso automatizado local

Repite las variables de preparación desde la raíz. Antes, confirma que el backend manual terminó y que 8086 no está ocupado. Si el servicio está activo y vas a ejecutar este procedimiento, deténlo como administrador durante la ventana de pruebas.

```powershell
Set-Location $p
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$p\docs\pruebas\despliegue\Preparar-Artefactos-Local.ps1" -Proyecto $p -Salida "$p\evidencias-pipeline"
```

**Aprobación/evidencia:** 15 CP aprobadas, JAR 0.5.2, tres dist, hashes.csv y resultado.txt CORRECTO dentro de una carpeta nueva pipeline-local-*. La versión actual del script funciona con un ZIP sin .git: registra esa condición y genera fuentes-sha256.csv, sin atribuir el commit de otra carpeta. En checkout propio registra commit y cambios locales. No despliega ni ejecuta GitHub Actions.

Si vas a continuar con 13, el gestor iniciará el servicio. Si terminas aquí o el proceso falla, recupera el servicio original con Start-Service y verifica health. Conserva la carpeta de evidencias incluso si el pipeline falla.

## 12. Publicación nueva en Nexus y verificación de descarga

No sobrescribas 0.5.0/0.5.1. Comprueba en Nexus si 0.5.2 ya existe. Si existe, descargar y comparar verifica disponibilidad, pero no demuestra una publicación nueva. Si tu JAR difiere, prepara otra versión en el POM y adapta todos los comandos antes de publicar.

Para publicar 0.5.2 por primera vez, en terminal normal desde la raíz, solicita la credencial sin mostrar su contraseña:

```powershell
$cred = Get-Credential -Message 'Cuenta de publicacion de Nexus local'
$env:NEXUS_USERNAME = $cred.UserName
$env:NEXUS_PASSWORD = $cred.GetNetworkCredential().Password
try {
    & "$kit\Publicar-Nexus.ps1" -Proyecto $p
} finally {
    Remove-Item Env:NEXUS_USERNAME, Env:NEXUS_PASSWORD -ErrorAction SilentlyContinue
    $cred = $null
}
```

**Aprobación/evidencia:** Maven deploy finaliza con BUILD SUCCESS y DescargaCoincide=True; JAR y POM visibles en maven-releases/pe/sga/sga-backend/0.5.2. Este script ejecuta nuevamente los 15 CP y construye el JAR. Después usa ese archivo para desplegar; conserva su hash. Error 401/403: permisos/credencial; 409: release ya publicada, no fuerces sobrescritura.

El servicio Nexus Running o su portada 200 no acreditan publicación. Sin credenciales, marca esta prueba pendiente; no inventes una aprobación.

## 13. Despliegue automático de 0.5.2

**Administrador**, variables de preparación repetidas. Si usas el JAR descargado de Nexus, cambia `$jar` por su ruta y verifica igualdad de hash con el publicado. También puedes verificar un despliegue local desde `$jar`, indicándolo expresamente; eso no demuestra la procedencia Nexus.

```powershell
Get-Content "$r\estado.json"
Get-FileHash $jar
& "$kit\Gestionar-Despliegue.ps1" -Accion Desplegar -Jar $jar -Version '0.5.2'
(Invoke-RestMethod http://localhost:8086/api/v1/health).data.status
(Invoke-RestMethod http://localhost:5180/api/v1/health).data.status
Get-Content "$r\estado.json"
Get-FileHash "$r\current\app.jar", $jar
```

**Aprobación/evidencia:** Operacion completada: Desplegar, actual 0.5.2, anterior igual a la versión que había antes, hashes iguales, UP directo y por proxy, y Camposol consultable al recargar SGA. Si el JAR ya está activo con el mismo hash, el script rechaza una actualización idéntica: no cuentes ese rechazo como un despliegue nuevo.

El gestor intenta recuperar el JAR previo si la operación falla. Revisa el mensaje y estado real. No edites estado.json ni intercambies versiones manualmente para conseguir una captura.

## 14. Administración del servicio y diagnóstico GC

**Administrador**, con la 13 aprobada:

```powershell
Stop-Service TransAduanasBackend
(Get-Service TransAduanasBackend).WaitForStatus('Stopped', [TimeSpan]::FromSeconds(60))
Get-Service TransAduanasBackend
Start-Service TransAduanasBackend
Get-Service TransAduanasBackend
```

Espera a que la aplicación arranque; luego:

```powershell
(Invoke-RestMethod http://localhost:8086/api/v1/health).data.status
Get-Content "$r\logs\gc.log" -Tail 8
```

**Aprobación/evidencia:** Stopped, Running, UP y líneas reales de GC. Running no asegura que la aplicación haya terminado de iniciar. No exijas cifras de memoria ni pausas idénticas a las de otra ejecución. Si gc.log falta, revisa los argumentos privados de WinSW; no simules el archivo.

## 15. Rollback y recuperación de la versión corregida

**Administrador.** Revisa antes que Actual sea 0.5.2 y Anterior sea la versión previa real, normalmente 0.5.0 en el laboratorio observado:

```powershell
Get-Content "$r\estado.json"
& "$kit\Gestionar-Despliegue.ps1" -Accion Revertir
Get-Content "$r\estado.json"
Get-FileHash "$r\current\app.jar"
(Invoke-RestMethod http://localhost:5180/api/v1/health).data.status
```

**Aprobación/evidencia:** versión anterior recuperada con su hash guardado, UP y consulta de Camposol. El rollback intercambia Actual y Anterior; no lo ejecutes dos veces sin leer el estado. Solo recupera el JAR: no restaura MySQL ni frontends.

Para terminar con las correcciones activas antes del monitoreo, vuelve a desplegar 0.5.2 y repite la comprobación de negocio:

```powershell
& "$kit\Gestionar-Despliegue.ps1" -Accion Desplegar -Jar $jar -Version '0.5.2'
Get-Content "$r\estado.json"
(Invoke-RestMethod http://localhost:5180/api/v1/health).data.status
```

## Estado y evidencias de esta entrega

| Comprobación | Estado documentado |
|---|---|
| Suite backend corregida | 346 aprobadas, cero fallos/errores/omisiones. |
| SGA y B2B | 130 y 63 pruebas aprobadas; builds staging correctos. |
| Landing | Build correcto en revisión inicial; fuentes sin cambios. |
| JAR 0.5.2 e instalación .m2 | Generados y hashes coincidentes. |
| Arranque 0.5.2 con MySQL | Verificado en proceso temporal 18086 con validate; Camposol coincide con SQL. |
| nginx -t de la copia corregida | Verificado en configuración aislada; no demuestra proxy activo. |
| Publicación Nexus 0.5.2 | Pendiente de credenciales y ejecución. |
| Actualización/rollback/reinicio del servicio | Procedimiento preparado; no ejecutado en esta revisión. |
| Interfaces del laboratorio | Última consulta: conexión rechazada; hay que iniciar/configurar Nginx según la 10. |
| GitHub Actions | Preparado para ejecución manual; no ejecutado. |

Conserva evidencias fuera de target antes de cualquier clean. En cada prueba guarda comando, fecha, salida y criterio de aprobación. `evidencias-revision` contiene resultados de revisión; los nuevos resultados de tu ejecución deben guardarse por separado.

Como ayuda para el laboratorio original existe `evidencias-revision/Actualizar-Servicio-052.ps1`, preparado para el estado inicial 0.5.0. Agrupa despliegue, rollback, nuevo despliegue 0.5.2, reinicio y Nginx. No sustituye las 15 pruebas ni la publicación Nexus. Primero revisa sus rutas y precondiciones; para exponer cada prueba usa los bloques individuales de este manual.

No copies claves privadas en capturas ni en el ZIP. Una clave rotada obliga a iniciar sesión otra vez. Para preparar las pruebas de monitoreo, termina con 0.5.2 identificada por su JAR/hash, MySQL consultable y los proxies funcionando; UP por sí solo no acredita todo esto.


## Activación posterior: 20 de noviembre de 2026

Tras la autorización explícita del usuario se activó 0.5.2 en el servicio Windows. Se verificaron Running, el SHA256 E11719C5DBF56D3B98F5720ECA8660F33F9180D4FBB478CE558553DCC3696496, login y consulta autenticada de Camposol. Nginx fue configurado con respaldo e iniciado; backend directo, ambos proxies, tres interfaces y la ruta interna devolvieron HTTP 200. El estado registra Actual 0.5.2 y Anterior 0.5.0; el JAR histórico 0.5.1 se conserva en releases.

Esta activación reemplaza el estado pendiente descrito antes. No incluyó publicación Nexus ni una nueva prueba de rollback. Evidencias: evidencias-revision/activacion-052.log, activacion-052-resultado.txt y activacion-052-verificacion-final.json. Las interfaces publicadas apuntan a los dist de esta copia: no muevas ni elimines la carpeta sin actualizar Nginx.


## Carpeta de trabajo en Music

La copia para continuar las pruebas está en `C:\Users\asus\Music\TransAduanas-0.5.2`. Abre PowerShell en esa raíz antes de definir `$p = (Get-Location).Path`. El servicio usa su JAR estable en Music/TransAduanas-desplegado; Nginx publica los dist de esta copia. Los logs anteriores conservan sus rutas como evidencia histórica. Consulta INICIAR-AQUI.txt.
