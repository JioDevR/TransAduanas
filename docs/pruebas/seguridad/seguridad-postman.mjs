import { readFileSync, unlinkSync, writeFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import { join } from 'node:path'
import { startFrontend } from './seguridad-frontend.mjs'

export async function testPostman({ repository, output, interactive, variables }) {
  const frontend = await startFrontend(repository, variables.baseUrl)
  const environment = {
    name: 'TransAduanas local - descartar al terminar',
    values: Object.entries({ ...variables, frontendUrl: frontend.url }).map(([key, value]) => ({
      key, value: String(value), type: key.endsWith('Token') ? 'secret' : 'default', enabled: true
    })),
    _postman_variable_scope: 'environment'
  }
  const localEnvironment = join(output, 'postman-local.postman_environment.json')
  try {
    if (interactive) {
      writeFileSync(localEnvironment, JSON.stringify(environment, null, 2), { flag: 'wx' })
      try {
        console.log('Importa en Postman la colección y este entorno local:', localEnvironment)
        console.log('Mantén la terminal abierta. Pulsa Ctrl+C al terminar.')
        await new Promise(done => process.once('SIGINT', done))
      } finally {
        unlinkSync(localEnvironment)
      }
      return
    }

    const require = createRequire(import.meta.url)
    const newman = require(join(output, 'postman-tools/node_modules/newman'))
    const collection = JSON.parse(readFileSync(new URL('./TransAduanas.postman_collection.json', import.meta.url), 'utf8'))
    const summary = await new Promise((done, reject) => {
      newman.run({ collection, environment, reporters: [], timeoutRequest: 15000 }, (error, result) => {
        if (error) reject(error)
        else done(result)
      })
    })
    // Conservar únicamente estados y aserciones. Los objetos de Newman contienen headers y tokens.
    const cases = summary.run.executions.map(item => ({
      caso: item.item.name, http: item.response?.code ?? null,
      aserciones: (item.assertions ?? []).map(test => ({ nombre: test.assertion, paso: !test.error && !test.skipped }))
    }))
    const failed = summary.run.stats.assertions.failed
    const assertions = summary.run.stats.assertions.total
    writeFileSync(join(output, 'postman-resultados.json'), JSON.stringify({ solicitudes: cases.length, aserciones: assertions, fallos: failed, casos: cases }, null, 2))
    for (const row of cases) console.log(row.caso, '| HTTP', row.http, '|', row.aserciones.map(a => a.paso ? 'Pasó' : 'Falló').join(', '))
    console.log('Postman/Newman:', cases.length, 'solicitudes;', assertions, 'aserciones;', failed, 'fallos.')
    if (cases.length !== 16 || assertions !== 32 || failed !== 0 || summary.run.failures.length) process.exitCode = 1
  } finally {
    await frontend.close()
  }
}
