# Manuales de pruebas — TransAduanas 0.5.2

Actualizado el 20 de noviembre de 2026. Abre PowerShell en la carpeta que contiene `backend-sga`, `app-sga`, `app-b2b` y `app-landing`.

| Orden | Manual | Qué ejecutar |
|---|---|---|
| 1 | [Funcionales](funcionales/README.md) | 15 seleccionadas: 4 unitarias, 4 integración, 4 sistema y 3 aceptación. |
| 2 | [Seguridad](seguridad/README.md) | 13 casos JWT/permisos, aislamiento por HTTP y auditoría npm. |
| 3 | [Despliegue](despliegue/README.md) | 15 comprobaciones de construcción, configuración, MySQL, publicación, servicio y recuperación. |
| Consulta | [Inventario del backend](../../test/README.md) | 346 pruebas completas. |

## Preparación común

```powershell
$p = (Get-Location).Path
Get-Item "$p\backend-sga\pom.xml"
java -version
node --version
npm.cmd --version
```

Si no aparece `pom.xml`, abre primero la raíz correcta. Entorno verificado: Java 21, Maven Wrapper 3.9.16, Node 24. No se necesita Maven instalado globalmente. La primera ejecución necesita red para descargar dependencias; `-o` solo sirve si Maven ya las tiene en caché. `npm ci` necesita caché o acceso al registro.

Las pruebas backend crean sus datos en memoria o H2 temporal. No requieren iniciar el servicio Windows, MySQL ni Nginx. Para las pruebas de despliegue sí se usa el laboratorio real descrito en su manual. No cargues nuevamente `schema.sql` o `seed.sql` sobre la base existente.

## Resultados ya observados y resultados pendientes

En la revisión de las correcciones pasaron 346 pruebas backend, 130 SGA y 63 B2B: **539 en total**. Las 13 de seguridad están dentro de las 346. El 0.5.2 arrancó con MySQL en un proceso temporal y la consulta de Camposol coincidió con SQL. SGA y B2B quedaron sin avisos en npm audit; la landing no tenía avisos en la revisión inicial.

Esto no equivale a haber actualizado el servicio Windows: su último estado observado era 0.5.0, con 0.5.1 conservada como anterior. No se publicaron los cambios en Nexus ni se ejecutaron despliegue/rollback del servicio corregido. Esos pasos están documentados para ejecutarse y guardar evidencia nueva. Este trabajo de documentación no los ejecuta.

## Cómo presentar las evidencias

Muestra comando, caso, resultado esperado y resultado obtenido. Conserva `Tests run / Failures / Errors / Skipped` y los XML de Surefire, o el resumen de Vitest. Una consulta de un log anterior no es una ejecución nueva. Un rechazo 401, 403 o 405 puede ser el resultado correcto; un 500 inesperado no debe aceptarse como prueba de seguridad aprobada.

Los casos CP01–CP15 del despliegue son complementarios y difieren de las 15 seleccionadas con `docente15`. Ambos conjuntos pertenecen a las 346; no se suman como pruebas adicionales. Las futuras 15 de monitoreo serán otro plan, a preparar con el material teórico e indicaciones de evaluación.


## Activación posterior: 20 de noviembre de 2026

Tras la autorización explícita del usuario se activó 0.5.2 en el servicio Windows. Se verificaron Running, el SHA256 E11719C5DBF56D3B98F5720ECA8660F33F9180D4FBB478CE558553DCC3696496, login y consulta autenticada de Camposol. Nginx fue configurado con respaldo e iniciado; backend directo, ambos proxies, tres interfaces y la ruta interna devolvieron HTTP 200. El estado registra Actual 0.5.2 y Anterior 0.5.0; el JAR histórico 0.5.1 se conserva en releases.

Esta activación reemplaza el estado pendiente descrito antes. No incluyó publicación Nexus ni una nueva prueba de rollback. Evidencias: evidencias-revision/activacion-052.log, activacion-052-resultado.txt y activacion-052-verificacion-final.json. Las interfaces publicadas apuntan a los dist de esta copia: no muevas ni elimines la carpeta sin actualizar Nginx.


## Carpeta de trabajo en Music

La copia para continuar las pruebas está en `C:\Users\asus\Music\TransAduanas-0.5.2`. Abre PowerShell en esa raíz antes de definir `$p = (Get-Location).Path`. El servicio usa su JAR estable en Music/TransAduanas-desplegado; Nginx publica los dist de esta copia. Los logs anteriores conservan sus rutas como evidencia histórica. Consulta INICIAR-AQUI.txt.
