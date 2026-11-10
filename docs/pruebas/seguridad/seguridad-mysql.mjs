import { existsSync, mkdirSync, mkdtempSync, readFileSync, unlinkSync, writeFileSync } from 'node:fs'
import { createServer } from 'node:net'
import { randomBytes } from 'node:crypto'
import { spawn, spawnSync } from 'node:child_process'
import { dirname, join, resolve } from 'node:path'
import { setTimeout } from 'node:timers/promises'

const [mysqlBin, repository, evidence] = process.argv.slice(2)
if (!mysqlBin || !repository || !evidence) {
  throw new Error('Uso: node seguridad-mysql.mjs <carpeta bin de MySQL> <raíz de TransAduanas> <carpeta de resultados>')
}
const bin = resolve(mysqlBin)
const output = resolve(evidence)
const schema = readFileSync(resolve(repository, 'db/schema.sql'), 'utf8')
const server = join(bin, 'mysqld.exe')
const client = join(bin, 'mysql.exe')
for (const file of [server, client]) if (!existsSync(file)) throw new Error('No se encontró ' + file)
mkdirSync(output, { recursive: true })
const lab = mkdtempSync(join(output, 'mysql-aislado-'))
const data = join(lab, 'data')
const startup = join(lab, 'inicializar.sql')
const password = () => randomBytes(24).toString('hex') + '!aA7'
const rootPassword = password()
const readPassword = password()
const listener = createServer()
await new Promise((done, reject) => listener.once('error', reject).listen(0, '127.0.0.1', done))
const port = listener.address().port
await new Promise(done => listener.close(done))
const baseArgs = ['--no-defaults', '--basedir=' + dirname(bin), '--datadir=' + data]
const initialize = spawnSync(server, [...baseArgs, '--initialize-insecure', '--log-error=' + join(lab, 'mysql-init.log')], {
  windowsHide: true, encoding: 'utf8', timeout: 120000
})
if (initialize.status !== 0) throw new Error('MySQL no pudo inicializar el laboratorio. Revisa mysql-init.log.')
writeFileSync(startup, "ALTER USER 'root'@'localhost' IDENTIFIED BY '" + rootPassword + "';\n")
const child = spawn(server, [...baseArgs, '--bind-address=127.0.0.1', '--port=' + port, '--mysqlx=OFF',
  '--init-file=' + startup, '--log-error=' + join(lab, 'mysql-servidor.log')], { windowsHide: true, stdio: 'ignore' })
let startError
child.on('error', error => { startError = error })
const cases = []
function sql(user, pass, input) {
  return spawnSync(client, ['--no-defaults', '--protocol=TCP', '--host=127.0.0.1', '--port=' + port,
    '--user=' + user, '--connect-timeout=2', '--batch', '--skip-column-names', '--default-character-set=utf8mb4'], {
    input, encoding: 'utf8', windowsHide: true, timeout: 20000, env: { ...process.env, MYSQL_PWD: pass }
  })
}
function admin(input) {
  const result = sql('root', rootPassword, input)
  if (result.status !== 0) throw new Error('Falló la preparación SQL del laboratorio: ' + (result.stderr ?? '').split('\n')[0])
  return result.stdout.trim()
}
function record(id, expected, actual) {
  const row = { id, esperado: expected, observado: actual, resultado: expected === actual ? 'Pasó' : 'Falló' }
  cases.push(row)
  console.log(id, row.resultado, '| esperado:', expected, '| observado:', actual)
}
function status(result) {
  return result.status === 0 ? result.stdout.trim() : (result.stderr ?? '').match(/ERROR (\d+)/)?.[1] ?? 'Sin código SQL'
}
try {
  let ready = false
  for (let attempt = 0; attempt < 60; attempt++) {
    if (startError || child.exitCode !== null) break
    if (sql('root', rootPassword, 'SELECT 1;').status === 0) { ready = true; break }
    await setTimeout(500)
  }
  if (!ready) throw new Error('No arrancó MySQL aislado. Revisa mysql-servidor.log.')
  const [actualDir, actualPort, version] = admin('SELECT @@datadir, @@port, VERSION();').split('\t')
  if (resolve(actualDir).toLowerCase() !== resolve(data).toLowerCase() || Number(actualPort) !== port) {
    throw new Error('El servidor no coincide con la instancia aislada')
  }
  unlinkSync(startup)
  console.log('MySQL', version, '| instancia aislada en 127.0.0.1:' + port)
  admin(schema)
  admin(`INSERT INTO SGAdb.cliente (tipo_documento, numero_documento, razon_social, contacto_representante, correo_contacto)
    VALUES ('RUC','20990000041','Transportes Costa del Sur S.A.C.','Ana Torres Velarde','ana.torres@costadelsur.example');
    CREATE DATABASE laboratorio_reservado;
    CREATE TABLE laboratorio_reservado.control (id INT PRIMARY KEY);
    INSERT INTO laboratorio_reservado.control VALUES (1);
    CREATE USER 'maria_salas'@'localhost' IDENTIFIED BY '${readPassword}';
    GRANT SELECT (id_cliente, razon_social) ON SGAdb.cliente TO 'maria_salas'@'localhost';`)
  const count = 'SELECT COUNT(*) FROM SGAdb.cliente;'
  const before = admin(count)
  const viewer = query => status(sql('maria_salas', readPassword, query))
  record('BD01', '1045', status(sql('maria_salas', password(), 'SELECT 1;')))
  record('BD02', 'Transportes Costa del Sur S.A.C.', viewer('SELECT razon_social FROM SGAdb.cliente;'))
  record('BD03', '1142', viewer("INSERT INTO SGAdb.cliente (tipo_documento, numero_documento, razon_social) VALUES ('RUC','20990000042','Comercial Bahia Clara S.A.C.');"))
  record('BD04', '1142', viewer('SELECT username FROM SGAdb.usuario;'))
  record('BD05', '1142', viewer('SELECT id FROM laboratorio_reservado.control;'))
  record('BD07', '1143', viewer('SELECT correo_contacto FROM SGAdb.cliente;'))
  record('BD08', '1142', viewer('DELETE FROM SGAdb.cliente WHERE id_cliente = 1;'))
  record('BD06', before, admin(count))
  console.log('BD06 filas antes=' + before + ' después=' + cases.at(-1).observado)
} finally {
  if (existsSync(startup)) unlinkSync(startup)
  if (child.exitCode === null) {
    const stop = spawnSync(join(bin, 'mysqladmin.exe'), ['--no-defaults', '--protocol=TCP', '--host=127.0.0.1',
      '--port=' + port, '--user=root', '--connect-timeout=2', 'shutdown'], {
      windowsHide:true, timeout:10000, env:{...process.env, MYSQL_PWD:rootPassword}
    })
    if (stop.status !== 0) child.kill()
  }
  writeFileSync(join(output, 'mysql-resultados.json'), JSON.stringify(cases, null, 2))
}
if (cases.length !== 8 || cases.some(row => row.resultado === 'Falló')) process.exitCode = 1
