import { existsSync, writeFileSync } from 'node:fs'
import { join, resolve } from 'node:path'
import { pathToFileURL } from 'node:url'

export async function testFrontend({ repository, backendUrl, output, password, wrongPassword }) {
  const root = resolve(repository, 'app-sga')
  const modulePath = name => pathToFileURL(join(root, 'node_modules', name)).href
  const { createServer } = await import(modulePath('vite/dist/node/index.js'))
  const { chromium } = await import(modulePath('playwright/index.mjs'))
  const vite = await createServer({
    root,
    configFile: join(root, 'vite.config.js'),
    define: { 'import.meta.env.VITE_API_URL': JSON.stringify('/api/v1') },
    server: { host: '127.0.0.1', port: 0, proxy: { '/api': { target: backendUrl, changeOrigin: false } } }
  })
  const rows = []
  let browser
  function record(id, expected, actual, ok) {
    rows.push({ id, esperado: expected, observado: actual, resultado: ok ? 'Pasó' : 'Falló' })
    console.log(id, rows.at(-1).resultado, '|', actual)
  }
  try {
    await vite.listen()
    const url = 'http://127.0.0.1:' + vite.httpServer.address().port
    const installed = [
      process.env.SEGURIDAD_BROWSER,
      'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
      'C:/Program Files/Google/Chrome/Application/chrome.exe'
    ].filter(Boolean).find(existsSync)
    browser = await chromium.launch(installed ? { executablePath: installed, headless: true } : { headless: true })
    const page = await browser.newPage({ viewport: { width: 1280, height: 780 } })
    await page.route('**/*', route => new URL(route.request().url()).origin === url ? route.continue() : route.abort())
    const pathname = () => new URL(page.url()).pathname
    const shot = async name => {
      await page.screenshot({ path: join(output, name), animations: 'disabled', mask: [page.locator('#password')] })
    }
    const submit = async value => {
      await page.locator('#username').fill('maria.salas')
      await page.locator('#password').fill(value)
      const [response] = await Promise.all([
        page.waitForResponse(res => res.url().endsWith('/api/v1/auth/login') && res.request().method() === 'POST', { timeout: 12000 }),
        page.locator('#btn-login-submit').click()
      ])
      return response.status()
    }
    await page.goto(url + '/finanzas')
    await page.waitForURL('**/login?**')
    record('FE01', 'Ruta privada redirige a /login', pathname(), pathname() === '/login')
    await shot('frontend-FE01-sin-sesion.png')
    const inputType = await page.locator('#password').getAttribute('type')
    record('FE06', 'Contraseña oculta por defecto', 'type=' + inputType, inputType === 'password')

    const rejected = await submit(wrongPassword)
    const error = page.locator('.error-message, [role="alert"]').first()
    await error.waitFor({ timeout: 4000 }).catch(() => {})
    const hasToken = await page.evaluate(() => Boolean(localStorage.getItem('sga_token')))
    record('FE02', 'HTTP 401 y sin token', 'HTTP ' + rejected + ', token presente: ' + hasToken, rejected === 401 && !hasToken)
    await shot('frontend-FE02-login-rechazado.png')

    const accepted = await submit(password)
    await page.waitForURL('**/recepcion-comercial')
    record('FE03', 'HTTP 200 y acceso a Recepción Comercial', 'HTTP ' + accepted + ', ' + pathname(), accepted === 200 && pathname() === '/recepcion-comercial')
    await page.goto(url + '/finanzas')
    await page.waitForURL('**/recepcion-comercial')
    record('FE04', 'María vuelve a Recepción Comercial al intentar Finanzas', pathname(), pathname() === '/recepcion-comercial')
    await shot('frontend-FE04-rol-comercial.png')

    const apiStatus = await page.evaluate(async () => {
      const token = localStorage.getItem('sga_token')
      return (await fetch('/api/v1/usuarios', { headers: { Authorization: 'Bearer ' + token } })).status
    })
    record('FE05', 'La API deniega el acceso directo con el JWT de María', 'HTTP ' + apiStatus, apiStatus === 403)

    await page.locator('.user-profile-header').click()
    await page.locator('.dropdown-logout-action').click()
    await page.waitForURL('**/login')
    const sessionCleared = await page.evaluate(() => !localStorage.getItem('sga_token') && !localStorage.getItem('sga_user'))
    record('FE07', 'Cerrar sesión elimina el token y el perfil', 'sesión eliminada: ' + sessionCleared, sessionCleared)
    await shot('frontend-FE07-sesion-cerrada.png')

    await page.goto(url + '/login?redirect=' + encodeURIComponent('//untrusted.example/salida'))
    const redirectedLogin = await submit(password)
    await page.waitForURL('**/recepcion-comercial')
    record('FE08', 'El login ignora un redirect externo', 'HTTP ' + redirectedLogin + ', ' + pathname(),
      redirectedLogin === 200 && new URL(page.url()).origin === url)
  } finally {
    if (browser) await browser.close()
    await vite.close()
    writeFileSync(join(output, 'frontend-resultados.json'), JSON.stringify(rows, null, 2))
  }
  if (rows.length !== 8 || rows.some(row => row.resultado === 'Falló')) process.exitCode = 1
}
