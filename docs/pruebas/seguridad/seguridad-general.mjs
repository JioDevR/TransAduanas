import { mkdirSync, mkdtempSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const [backendPath, classpath, mysqlBin, results] = process.argv.slice(2)
if (!backendPath || !classpath || !mysqlBin || !results) {
  throw new Error('Uso: node seguridad-general.mjs <backend> <classpath> <bin MySQL> <resultados>')
}
const backend = resolve(backendPath)
const kit = dirname(fileURLToPath(import.meta.url))
const output = resolve(results)
mkdirSync(output, { recursive: true })
const runDir = mkdtempSync(join(output, 'general-'))
console.log('Carpeta de esta ejecución:', runDir)

const run = (file, args) => spawnSync(process.execPath, [join(kit, file), ...args], {
  stdio: 'inherit', windowsHide: true, timeout: 180000
})
const http = run('seguridad-http.mjs', [backend, resolve(classpath), join(runDir, 'http.json'), '--frontend', '--sistema'])
const mysql = run('seguridad-mysql.mjs', [resolve(mysqlBin), dirname(backend), runDir])
const groups = [
  ['Backend', 'http.json', 10], ['Frontend SGA', 'frontend-resultados.json', 8],
  ['Base de datos MySQL', 'mysql-resultados.json', 8], ['Sistema', 'sistema-resultados.json', 8]
]
const summary = groups.map(([componente, file, esperados]) => {
  let cases = []
  try { cases = JSON.parse(readFileSync(join(runDir, file), 'utf8')) } catch (error) {
    if (error.code !== 'ENOENT') throw error
  }
  const conformes = cases.filter(row => row.resultado === 'Pasó').length
  return { componente, esperados, ejecutados: cases.length, conformes, noConformes: cases.length - conformes }
})
const incomplete = summary.some(row => row.ejecutados !== row.esperados) || [http, mysql].some(child => child.error || child.status === null)
const unexpectedExit = [http, mysql].some(child => child.status !== 0) && !summary.some(row => row.noConformes > 0)
const result = { estado: incomplete || unexpectedExit ? 'Incompleto' : summary.some(row => row.noConformes) ? 'Completo con hallazgos' : 'Completo', componentes: summary }
writeFileSync(join(runDir, 'general-resultados.json'), JSON.stringify(result, null, 2))
for (const row of summary) console.log(row.componente + ':', row.ejecutados + '/' + row.esperados, 'casos;', row.conformes, 'conformes;', row.noConformes, 'no conformes.')
console.log('Resultado general:', result.estado)
process.exitCode = result.estado === 'Incompleto' ? 2 : result.estado === 'Completo con hallazgos' ? 1 : 0
