# SEG-05: defensa frente a XSS en campos de la interfaz B2B

Resultado observado: APROBADA, cuatro comprobaciones sin fallos.

Objetivo: comprobar que valores no confiables se muestran como texto y no se convierten en código HTML ejecutable en los componentes reales del portal.

## Tipo y herramientas

Prueba de seguridad XSS mediante integración de componentes en navegador real. Herramientas: Playwright, Microsoft Edge (Chromium), Vue, Vite y aserciones de Node.js. No utiliza JUnit ni Mockito. El navegador trabaja oculto contra un servidor temporal en 127.0.0.1 y se bloquean las solicitudes a otros destinos.

Se montan los componentes reales `PortalCotizacionProformaPreview.vue` y `PortalDocumentosUploadPanel.vue` con datos ficticios proporcionados por el banco de pruebas. No se usa una copia de sus plantillas.

| Comprobación | Campo y verificación |
| --- | --- |
| SEG-05.1 | Razón social: etiqueta de script mostrada literalmente, sin ejecución. |
| SEG-05.2 | Puerto de origen: etiqueta con evento mostrada como texto, sin crear imagen ni evento. |
| SEG-05.3 | Nombre de archivo: comillas y marcado conservados como texto y como valor completo de `title`, sin introducir atributos nuevos. |
| SEG-05.4 | Descripción de archivo: HTML y entidades conservados literalmente, sin elementos SVG ni eventos ejecutados. |

Cada caso exige que el texto esperado aparezca, que no se creen elementos script/imagen/SVG/iframe dentro del componente, que no haya atributos de evento inyectados, que la marca de ejecución siga desactivada y que no haya errores de página. Antes se comprueba también la visualización de una razón social normal. Se guarda una captura PNG por caso.

## Comandos para el docente

Desde la carpeta `app-b2b`, en PowerShell:

```powershell
npm.cmd ci --ignore-scripts --no-audit --no-fund
npm.cmd run test:seg05
```

El equipo debe tener Node.js compatible con las dependencias del proyecto. El ejecutor busca Edge o Chrome en las rutas habituales de Windows. Para otro navegador Chromium instalado se puede indicar su ejecutable:

```powershell
$env:SEG05_BROWSER_PATH = 'C:\ruta\al\navegador.exe'
npm.cmd run test:seg05
```

Si no hay navegador instalado, Playwright puede instalar su Chromium con `npx.cmd playwright install chromium`. El ejecutor termina con código distinto de cero si falta el navegador o falla una verificación; no registra un resultado favorable en esos casos.

Evidencia predeterminada: `app-b2b/test-results/seg05/resultado.json` y cuatro capturas. La ruta puede cambiarse con `SEG05_OUTPUT_DIR`. La carpeta `test-results` queda excluida de Git.

Verificación adicional del portal:

```powershell
npm.cmd test
npm.cmd run build
```

En esta ejecución: 63 pruebas existentes aprobadas y compilación correcta. SEG-05 se ejecuta mediante su comando separado; `npm test` por sí solo no ejecuta este banco de navegador.

## Interpretación y alcance

Los cuatro casos aprobaron sin modificar los componentes de producción. Se añadieron el ejecutor, la dependencia de desarrollo Playwright fijada en package.json/package-lock.json, el comando y este manual.

Esta prueba comprueba el tratamiento de texto en dos componentes B2B y un atributo de título. No recorre la creación y recuperación del dato a través del backend; no certifica XSS almacenado de extremo a extremo, todas las pantallas, los enlaces dinámicos, las vistas previas de archivos ni todas las formas posibles de XSS. La búsqueda estática no encontró `v-html`, `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `dangerouslySetInnerHTML` o `document.write` en los directorios src de app-b2b, app-sga y app-landing; esa ausencia es evidencia auxiliar, no una garantía completa.

SEG-05 es una prueba de seguridad con cuatro comprobaciones, no cuatro códigos SEG distintos.
