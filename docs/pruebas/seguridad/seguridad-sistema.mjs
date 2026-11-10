import { writeFileSync } from 'node:fs'
import { join } from 'node:path'

export async function testSistema({ call, admin, maria, portalAccounts, download, output }) {
  const [ana, diego] = portalAccounts
  const profile = '/api/v1/portal/perfil'
  const portalLogin = '/api/v1/portal/auth/login'
  const portalAdmin = '/api/v1/admin/usuarios-portal'
  const diegoAdminPath = portalAdmin + '/' + diego.idUsuarioCliente
  const rows = []
  const record = (id, expected, actual, ok) => {
    rows.push({ id, esperado: expected, observado: actual, resultado: ok ? 'Pasó' : 'Falló' })
    console.log(id, rows.at(-1).resultado, '|', actual)
  }
  const credentials = JSON.stringify({ username: diego.username, password: diego.password })
  try {
    const profileAna = await call(profile, 'GET', ana.token)
    const profileDiego = await call(profile, 'GET', diego.token)
    const ownerAna = (await profileAna.json()).data?.idCliente
    const ownerDiego = (await profileDiego.json()).data?.idCliente
    record('SIS01', 'Los perfiles recuperan el ID de su propia empresa',
      'HTTP ' + profileAna.status + ' y ' + profileDiego.status + ', empresas ' + ownerAna + ' y ' + ownerDiego,
      profileAna.ok && profileDiego.ok && ownerAna === ana.idCliente && ownerDiego === diego.idCliente && ownerAna !== ownerDiego)

    const internal = await call(profile, 'GET', maria)
    record('SIS02', 'La sesión interna no puede abrir el perfil B2B', 'HTTP ' + internal.status, internal.status === 403)
    const portal = await call('/api/v1/usuarios', 'GET', ana.token)
    record('SIS03', 'La sesión B2B no puede listar usuarios internos', 'HTTP ' + portal.status, portal.status === 403)

    const denied = await call(diegoAdminPath, 'DELETE', maria)
    const stillActive = await call(profile, 'GET', diego.token)
    record('SIS04', 'María no puede desactivar a Diego y Diego conserva acceso',
      'baja HTTP ' + denied.status + ', perfil HTTP ' + stillActive.status, denied.status === 403 && stillActive.status === 200)

    const disabled = await call(diegoAdminPath, 'DELETE', admin)
    const oldToken = await call(profile, 'GET', diego.token)
    record('SIS05', 'La baja autorizada bloquea el JWT anterior',
      'baja HTTP ' + disabled.status + ', perfil HTTP ' + oldToken.status, disabled.status === 200 && oldToken.status === 401)

    const inactiveLogin = await call(portalLogin, 'POST', null, credentials)
    record('SIS06', 'La cuenta inactiva tampoco puede iniciar sesión', 'HTTP ' + inactiveLogin.status, inactiveLogin.status === 401)

    const activated = await call(diegoAdminPath + '/reactivar', 'PATCH', admin)
    const newSession = await call(portalLogin, 'POST', null, credentials)
    const newToken = (await newSession.json()).data?.token
    const restored = await call(profile, 'GET', newToken)
    record('SIS07', 'La reactivación autorizada permite un nuevo login y recuperar el perfil propio',
      'reactivación HTTP ' + activated.status + ', login HTTP ' + newSession.status + ', perfil HTTP ' + restored.status,
      activated.status === 200 && newSession.status === 200 && restored.status === 200 && (await restored.json()).data?.idCliente === diego.idCliente)

    const restoredDownload = await call(download, 'GET', newToken)
    const ownerDownload = await call(download, 'GET', ana.token)
    record('SIS08', 'Reactivar a Diego no le da acceso al comprobante de Ana',
      'Diego HTTP ' + restoredDownload.status + ', Ana HTTP ' + ownerDownload.status,
      restoredDownload.status === 403 && ownerDownload.status === 200 && (await ownerDownload.text()).startsWith('%PDF'))
  } finally {
    writeFileSync(join(output, 'sistema-resultados.json'), JSON.stringify(rows, null, 2))
  }
  if (rows.length !== 8 || rows.some(row => row.resultado === 'Falló')) process.exitCode = 1
}
