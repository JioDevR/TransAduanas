# Compilación de interfaces — entrega 0.5.2

Usa Node 24 y npm desde la raíz que contiene app-sga, app-b2b y app-landing. Para pruebas de publicación/proxy consulta las pruebas 9 y 10 del [manual de despliegue](../pruebas/despliegue/README.md).

```powershell
node --version
npm.cmd --version
$env:VITE_API_URL = '/api/v1'
$env:VITE_SGA_APP_URL = 'http://localhost:5180'
$env:VITE_PORTAL_CLIENTE_URL = 'http://localhost:5174'
npm.cmd --prefix .\app-sga ci
npm.cmd --prefix .\app-b2b ci
npm.cmd --prefix .\app-landing ci
npm.cmd --prefix .\app-sga test
npm.cmd --prefix .\app-b2b test
npm.cmd --prefix .\app-sga run build -- --mode staging
npm.cmd --prefix .\app-b2b run build -- --mode staging
npm.cmd --prefix .\app-landing run build -- --mode staging
```

Ejecuta cada línea y revisa su resultado antes de seguir. SGA tiene 130 pruebas y B2B 63. La landing no tiene script test; su compilación no cuenta como una prueba automatizada adicional. Cada build debe generar dist/index.html en su propio módulo.

`npm ci` conserva las versiones del lockfile. La entrega corregida usa follow-redirects 1.16.3 en SGA y B2B. Revisa npm audit según el [manual de seguridad](../pruebas/seguridad/README.md).

Las variables VITE se incorporan durante el build. Para cambiar direcciones hay que recompilar. `/api/v1` necesita un proxy que envíe `/api/` al backend; el laboratorio Nginx usa 8086. El servidor Vite de desarrollo usa la configuración de sus vite.config.js y no demuestra el despliegue de dist.

| Módulo | Archivos que se publican | Puerto del laboratorio |
|---|---|---:|
| SGA | app-sga/dist | 5180 |
| Portal B2B | app-b2b/dist | 5174 |
| Landing | app-landing/dist | 5173 |

Las advertencias de tamaño de chunks no son errores de compilación. Un build correcto no garantiza que Nginx esté iniciado, que el proxy responda o que funcione el login: verifica esos pasos en el manual de despliegue.
