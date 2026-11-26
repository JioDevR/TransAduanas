import { chromium } from 'playwright'
import { createServer } from 'vite'
import assert from 'node:assert/strict'
import { existsSync } from 'node:fs'
import { mkdir, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const root = fileURLToPath(new URL('../', import.meta.url))
const output = path.resolve(process.env.SEG05_OUTPUT_DIR || path.join(root, 'test-results/seg05'))
await mkdir(output, { recursive: true })
const results = []
let server, browser
const entry = `
import { createApp, h, nextTick } from 'vue'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import Preview from '/src/features/cotizaciones/components/PortalCotizacionProformaPreview.vue'
import Documents from '/src/features/cotizaciones/components/PortalDocumentosUploadPanel.vue'
let app
window.seg05Mount = async (kind, props) => {
  if (app) app.unmount()
  app = createApp({ render: () => h(kind === 'preview' ? Preview : Documents, props) })
  app.use(PrimeVue, { unstyled: true }).use(ToastService)
  app.mount('#app')
  await nextTick()
}
window.seg05Ready = true
`

try {
  const executablePath = process.env.SEG05_BROWSER_PATH || [
    'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
    'C:/Program Files/Google/Chrome/Application/chrome.exe'
  ].find(existsSync)
  server = await createServer({
    root,
    server: { host: '127.0.0.1', port: 0, open: false },
    plugins: [{
      name: 'seg05-local-fixture',
      resolveId(id) { if (id === '/__seg05__/entry.js') return '\0seg05-entry' },
      load(id) { if (id === '\0seg05-entry') return entry },
      configureServer(vite) {
        vite.middlewares.use((req, res, next) => {
          if (req.url?.split('?')[0] !== '/__seg05__/') return next()
          res.setHeader('Content-Type', 'text/html; charset=utf-8')
          res.end('<!doctype html><html><head><meta charset="utf-8"><title>SEG-05 local</title></head><body><div id="app"></div><script type="module" src="/__seg05__/entry.js"></script></body></html>')
        })
      }
    }]
  })
  await server.listen()
  const port = server.httpServer.address().port
  browser = await chromium.launch({ headless: true, ...(executablePath ? { executablePath } : {}) })
  const page = await browser.newPage()
  const errors = []
  page.on('pageerror', error => errors.push(error.message))
  // El navegador de prueba solo puede comunicarse con el servidor local de esta ejecución.
  await page.route('**/*', route => {
    const url = new URL(route.request().url())
    return url.hostname === '127.0.0.1' && url.port === String(port)
      ? route.continue() : route.abort()
  })
  await page.goto(`http://127.0.0.1:${port}/__seg05__/`)
  await page.waitForFunction(() => window.seg05Ready === true)
  await page.evaluate(() => { window.__SEG05_EXECUTED = false })
  const mount = (kind, props) => page.evaluate(async ({ kind, props }) => {
    await window.seg05Mount(kind, props)
  }, { kind, props })
  const base = { form: { valorFob: 100, validezDias: 15 }, empresa: { razonSocial: 'Empresa ficticia SEG-05' } }
  await mount('preview', base)
  assert.equal((await page.locator('.proforma-client-name').textContent()).trim(), base.empresa.razonSocial)

  const cases = [
    {
      code: 'SEG-05.1', description: 'Razón social: etiqueta de script queda como texto',
      kind: 'preview', selector: '.proforma-client-name',
      value: '<script>window.__SEG05_EXECUTED=true</script>',
      props(value) { return { ...base, empresa: { razonSocial: value } } }
    },
    {
      code: 'SEG-05.2', description: 'Puerto: etiqueta con evento queda como texto',
      kind: 'preview', selector: '#app',
      value: '<img src="seg05-inexistente" onerror="window.__SEG05_EXECUTED=true">',
      props(value) { return { ...base, selectedPuertoEnvio: { codigoPuerto: value } } }
    },
    {
      code: 'SEG-05.3', description: 'Archivo: nombre y atributo title conservan el valor literal',
      kind: 'documents', selector: '.queued-name',
      value: '\"><img src=x onerror="window.__SEG05_EXECUTED=true">.pdf',
      props(value) { return { mode: 'deferred', modelValue: [{ name: value, size: 10, categoria: 'GENERAL' }] } },
      attribute: 'title'
    },
    {
      code: 'SEG-05.4', description: 'Descripción: HTML y entidades no se interpretan como elementos',
      kind: 'documents', selector: '.meta-note',
      value: '<svg onload="window.__SEG05_EXECUTED=true"></svg> &lt;b&gt;SEG05&lt;/b&gt;',
      props(value) { return { mode: 'deferred', modelValue: [{ name: 'ficticio.pdf', size: 10, categoria: 'GENERAL', descripcion: value }] } }
    }
  ]
  for (const test of cases) {
    await mount(test.kind, test.props(test.value))
    assert.ok((await page.locator(test.selector).textContent()).includes(test.value), 'Debe conservarse el texto literal')
    if (test.attribute) assert.equal(await page.locator(test.selector).getAttribute(test.attribute), test.value)
    // Da oportunidad a eventos de carga/error; además se comprueba la estructura del DOM.
    await page.waitForTimeout(100)
    assert.equal(await page.locator('#app script, #app img, #app svg, #app iframe').count(), 0)
    assert.equal(await page.locator('#app [onerror], #app [onload]').count(), 0)
    assert.equal(await page.evaluate(() => window.__SEG05_EXECUTED), false)
    assert.deepEqual(errors, [], 'No debe haber errores de ejecución en la página')
    const screenshot = `${test.code}.png`
    await page.screenshot({ path: path.join(output, screenshot), fullPage: true })
    results.push({ codigo: test.code, descripcion: test.description, resultado: 'APROBADO', captura: screenshot })
    console.log(`[${test.code}] APROBADO: texto literal, sin elementos inyectados ni ejecución.`)
  }
  await writeFile(path.join(output, 'resultado.json'), JSON.stringify({
    prueba: 'SEG-05', resultado: 'APROBADA', herramientas: ['Playwright', 'Chromium', 'Vue', 'Vite', 'Node assert'],
    navegador: browser.version(), casos: results,
    alcance: 'Componentes reales B2B montados en navegador local; datos proporcionados por fixture. Sin backend ni recorrido completo de almacenamiento.'
  }, null, 2), 'utf8')
  console.log('SEG-05 APROBADA: 4 comprobaciones, 0 fallos.')
} catch (error) {
  await writeFile(path.join(output, 'resultado.json'), JSON.stringify({ prueba: 'SEG-05', resultado: 'NO APROBADA', casos: results, error: error.message }, null, 2), 'utf8')
  console.error('SEG-05 NO APROBADA:', error.message)
  process.exitCode = 1
} finally {
  if (browser) await browser.close()
  if (server) await server.close()
}
