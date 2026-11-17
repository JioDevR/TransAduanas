# Preparación de integración continua de TransAduanas

Estado: archivo preparado; pendiente incorporar al proyecto y ejecutar en GitHub. No constituye evidencia de una ejecución exitosa.

## Archivo y ubicación futura

Copiar `.github/workflows/preparar-artefactos.yml` de esta entrega a la misma ruta relativa dentro de `C:\Users\asus\Music\TransAduanas`. No reemplaza el código de la aplicación.

El workflow solo tiene `workflow_dispatch`: se solicitará manualmente más adelante. Para que GitHub muestre la ejecución manual, el archivo debe estar en la rama predeterminada del repositorio. No se configura ejecución por push o pull request durante esta preparación.

Antes de incorporarlo, la rama que vaya a ejecutarse debe contener las 15 pruebas de BusinessFunctionalCasesTest, las correcciones de compilación y del frontend, application-staging.properties, la condición de DataInitializer y los package-lock.json actuales. GitHub obtiene archivos del commit remoto; no utiliza cambios locales sin subir. No subir application-local.properties, contraseñas, tokens, respaldos ni node_modules.

## Qué hará cuando se ejecute

| Paso | Terminal o herramienta | Instrucción | Resultado esperado | Utilidad |
| --- | --- | --- | --- | --- |
| Preparar backend | PowerShell en Windows de GitHub | Java Temurin 21 y Maven Wrapper del proyecto | Versiones registradas en el log | Compilar fuera del entorno del desarrollador |
| Verificar y empaquetar | PowerShell / Maven | `.\mvnw.cmd -B -ntp clean verify "-Dtest=BusinessFunctionalCasesTest"` | BUILD SUCCESS y JAR ejecutable | Verificar esas 15 pruebas antes de conservar el artefacto |
| Comprobar reporte | PowerShell / XML de Surefire | Revisar tests, failures, errors y skipped | Exactamente 15; cero fallos, errores u omisiones | Evitar presentar una ejecución sin pruebas como exitosa |
| Identificar JAR | PowerShell | Get-FileHash con SHA256 y registro del commit | Hash y commit descargables | Relacionar el binario con la versión del código |
| Preparar frontends | PowerShell / Node 24 y npm | `npm.cmd ci` en cada frontend | Dependencias instaladas desde el lockfile | Reproducir la instalación sin node_modules local |
| Compilar frontends | PowerShell / Vite | `npm.cmd run build -- --mode staging` | dist/index.html en los tres frontends | Generar archivos para el servidor web |
| Conservar resultados | GitHub Actions Artifacts | upload-artifact | JAR, tres dist y reportes descargables durante 14 días | Obtener archivos y evidencia de la ejecución |

Los tres frontends se compilan en trabajos separados. Si uno falla, los otros siguen para facilitar el diagnóstico. Solo se conserva un frontend si su compilación termina correctamente.

## Alcance de los datos y de las direcciones

Las pruebas funcionales seleccionadas utilizan H2 aislado según FullStackTestBase. No se conectan a tu MySQL ni comprueban la migración de la base original. Esa comprobación pertenece al despliegue con los datos reales que se realizará aparte.

El workflow usa /api/v1 para las llamadas al backend y enlaces de la landing a localhost:5180 y localhost:5174. Son artefactos destinados al laboratorio local con Nginx. Para un servidor externo se definirán sus direcciones antes de compilar: localhost en un navegador señala al dispositivo de quien navega.

No ejecuta pruebas de interfaz en navegador, no pone en marcha Nginx, no instala un servicio y no despliega automáticamente. Guardar archivos en Actions Artifacts tampoco equivale a publicar con Maven deploy en Nexus, Artifactory u otro repositorio Maven.

## Evidencias para una ejecución futura

1. Pantalla de GitHub Actions con el commit y el resultado de cada trabajo.
2. Log de Maven con las 15 pruebas y BUILD SUCCESS.
3. XML de Surefire descargado.
4. JAR, hash y commit descargados; comparación del hash tras descargar.
5. Logs de compilación de los tres frontends y sus dist descargados.

Si un trabajo falla, registrar el error y su estado real; no declararlo aprobado porque otro trabajo terminó bien. Los reportes del backend se intentan conservar incluso si Maven falla, siempre que se hayan generado.

## Referencias oficiales

- https://github.com/actions/checkout
- https://github.com/actions/setup-java
- https://github.com/actions/setup-node
- https://github.com/actions/upload-artifact

Las versiones mayores de las acciones se tomaron de la documentación oficial consultada durante la preparación. La compatibilidad completa con este repositorio se confirmará en su primera ejecución; todavía no se ha ejecutado el workflow.
