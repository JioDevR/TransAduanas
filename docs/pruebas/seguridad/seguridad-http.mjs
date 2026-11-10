import { readFileSync, writeFileSync, mkdirSync, mkdtempSync } from 'node:fs'
import { spawn, execFile } from 'node:child_process'
import { createHmac, randomBytes } from 'node:crypto'
import { resolve, dirname } from 'node:path'
import { promisify } from 'node:util'
const args = process.argv.slice(2)
const [backendPath, cpPath, outputPath, ffufPath, zapJar] = args.filter(arg => arg !== '--frontend')
const backend = resolve(backendPath)
const cp = readFileSync(cpPath, 'utf8').trim()
const secret = readFileSync(resolve(backend,'src/main/resources/application.properties'),'utf8')
  .match(/^jwt\.secret=\$\{JWT_SECRET:([^}]+)\}/m)[1]
const initializer=readFileSync(resolve(backend,'src/main/java/pe/sga/config/DataInitializer.java'),'utf8')
const demoPassword=initializer.match(/existsByUsername\("admin"\)[\s\S]*?passwordEncoder\.encode\("([^"]+)"\)/)?.[1]
if(!demoPassword) throw Error('No se encontró la contraseña demo en el inicializador de esta versión')
const encode = value => Buffer.from(JSON.stringify(value)).toString('base64url')
function token(subject) {
  const algorithm = Buffer.byteLength(secret) >= 64 ? '512' : Buffer.byteLength(secret) >= 48 ? '384' : '256'
  const unsigned = encode({alg:'HS'+algorithm,typ:'JWT'})+'.'+encode({
    sub:subject,tipoUsuario:'PERSONAL_INTERNO',exp:Math.floor(Date.now()/1000)+90
  })
  return unsigned+'.'+createHmac('sha'+algorithm,secret).update(unsigned).digest('base64url')
}
const env = {...process.env}
delete env.JWT_SECRET
const child = spawn('java',['-cp',cp,'pe.sga.SgaApplication','--server.address=127.0.0.1','--server.port=0',
  '--spring.datasource.url=jdbc:h2:mem:security_http;MODE=MySQL;DB_CLOSE_DELAY=-1','--spring.datasource.driver-class-name=org.h2.Driver',
  '--spring.config.import=', '--spring.datasource.username=sa','--spring.datasource.password=','--spring.jpa.hibernate.ddl-auto=create-drop','--spring.jpa.show-sql=false',
  '--app.storage.upload-dir=./target/qa-files'],{cwd:backend,env,windowsHide:true})
let startup = ''
child.stdout.on('data',chunk=>startup+=chunk)
child.stderr.on('data',chunk=>startup+=chunk)
const rows=[]
const record=(id,ok,observado,detalle='')=>{const row={id,resultado:ok?'Pasó':'Falló',observado,detalle};rows.push(row);console.log(id,row.resultado,observado.join(' | '),detalle)}
const describe=response=>`HTTP ${response.status} type=${response.headers.get('content-type')??'(ausente)'}`
try {
  for(let i=0;i<100&&!startup.includes('Inicialización limpia')&&!startup.includes('Inicializaci');i++){
    if(child.exitCode!==null)throw Error(startup.slice(-3500))
    await new Promise(r=>setTimeout(r,250))
  }
  const port=startup.match(/Tomcat started on port (\d+)/)?.[1]
  if(!port)throw Error(startup.slice(-3500))
  const call=(path,method='GET',jwt,body,headers={})=>fetch('http://127.0.0.1:'+port+path,{
    method,headers:{...headers,...(jwt?{Authorization:'Bearer '+jwt}:{}),...(typeof body==='string'?{'Content-Type':'application/json'}:{})},
    body, signal:AbortSignal.timeout(8000)
  })
  const users='/api/v1/usuarios', login='/api/v1/auth/login', clients='/api/v1/clientes'
  let r
  for(let i=0;i<60;i++){
    r=await call(login,'POST',null,JSON.stringify({username:'admin',password:demoPassword}))
    if(r.status===200)break
    await new Promise(r=>setTimeout(r,250))
  }
  record('SEC05',r.status===401,[describe(r)])
  if(r.status!==200) throw Error('No se pudo preparar el laboratorio con la cuenta inicial')
  const bootstrap=(await r.json()).data.token
  const people=[
    {username:'maria.salas',nombres:'María',apellidos:'Salas de la Torre',rol:'AGENTE_COMERCIAL',numeroDocumento:'99000041'},
    {username:'lucia.mendoza',nombres:'Lucía',apellidos:'Mendoza Vargas',rol:'ADMINISTRADOR_TI',numeroDocumento:'99000042'}
  ]
  const accounts={}
  let mariaPassword
  for(const person of people){
    const password=randomBytes(24).toString('base64url')+'!A9'
    const created=await call(users,'POST',bootstrap,JSON.stringify({...person,password,
      tipoDocumento:'DNI',correoInstitucional:person.username+'@transaduanas.example'}))
    if(created.status!==201)throw Error('Alta de '+person.username+' devolvió '+created.status)
    const authenticated=await call(login,'POST',null,JSON.stringify({username:person.username,password}))
    if(authenticated.status!==200)throw Error('Login de '+person.username+' devolvió '+authenticated.status)
    accounts[person.username]=(await authenticated.json()).data.token
    if (person.username === 'maria.salas') mariaPassword = password
  }
  const admin=accounts['lucia.mendoza']
  const maria=accounts['maria.salas']
  const allowed=await call(users,'GET',admin)
  if(allowed.status!==200)throw Error('Control positivo de Lucía: '+allowed.status)
  r=await call(users,'GET',maria);record('SEC03',r.status===403,[describe(r)],
    'María Salas de la Torre, agente comercial. Lucía Mendoza Vargas, administradora, recibió 200 en la misma ruta.')
  r=await call(users); record('SEC01',r.status===401,[describe(r)])
  r=await call(users,'GET','not.a.jwt'); record('SEC02',r.status===401,[describe(r)])
  r=await call(users,'GET',token('lucia.mendoza'));record('SEC04',r.status===401,[describe(r)],'Suplantación de Lucía Mendoza Vargas en el laboratorio con la clave JWT de respaldo.')
  const list=async()=>{const response=await call(clients,'GET',admin);return (await response.json()).data?.length}
  const company=await call(clients,'POST',maria,JSON.stringify({
    tipoDocumento:'RUC',numeroDocumento:'20990000041',razonSocial:'Transportes Costa del Sur S.A.C.',
    contactoRepresentante:'Ana Torres Velarde',correoContacto:'ana.torres@costadelsur.example',estadoCliente:'PROSPECTO',estadoActivo:true
  }))
  if(company.status!==201)throw Error('Alta del cliente ficticio devolvió '+company.status)
  const another=await call(clients,'POST',maria,JSON.stringify({
    tipoDocumento:'RUC',numeroDocumento:'20990000042',razonSocial:'Comercial Bahía Clara S.A.C.',
    contactoRepresentante:'Diego Paredes Ríos',correoContacto:'diego.paredes@bahiaclara.example',estadoCliente:'CLIENTE',estadoActivo:true
  }))
  if(another.status!==201)throw Error('Alta del segundo cliente ficticio devolvió '+another.status)
  const portalAccounts=[
    {company,username:'ana.torres',nombres:'Ana',apellidos:'Torres Velarde'},
    {company:another,username:'diego.paredes',nombres:'Diego',apellidos:'Paredes Ríos'}
  ]
  for(const contact of portalAccounts){
    const idCliente=(await contact.company.json()).data.idCliente
    const password=randomBytes(24).toString('base64url')+'!A9'
    const created=await call('/api/v1/admin/usuarios-portal','POST',admin,JSON.stringify({
      idCliente,username:contact.username,password,nombres:contact.nombres,apellidos:contact.apellidos,
      correoElectronico:contact.username+'@transaduanas.example',debeCambiarPassword:false
    }))
    if(created.status!==201)throw Error('Alta portal '+contact.username+': '+created.status)
    const session=await call('/api/v1/portal/auth/login','POST',null,JSON.stringify({username:contact.username,password}))
    if(session.status!==200)throw Error('Login portal '+contact.username+': '+session.status)
    contact.token=(await session.json()).data.token
  }
  const form=new FormData()
  form.set('file',new Blob(['%PDF-1.4\nComprobante sintético de Ana\n%%EOF'],{type:'application/pdf'}),'comprobante-ana.pdf')
  const upload=await call('/api/v1/portal/pagos/upload-comprobante','POST',portalAccounts[0].token,form)
  if(upload.status!==201)throw Error('Carga de documento ficticio devolvió '+upload.status)
  const idDocumento=(await upload.json()).data.idDocumento
  if(!Number.isInteger(idDocumento))throw Error('El documento no devolvió un identificador válido')
  const download='/api/v1/documentos/'+idDocumento+'/descargar'
  const own=await call(download,'GET',portalAccounts[0].token)
  const other=await call(download,'GET',portalAccounts[1].token)
  const ownText=await own.text()
  const otherText=await other.text()
  record('SEC10',own.status===200&&ownText.startsWith('%PDF')&&[403,404].includes(other.status)&&!otherText.includes('Comprobante sintético'),
    [describe(own),describe(other)],'Ana Torres Velarde, propietaria, y Diego Paredes Ríos, de otra empresa, probaron la misma descarga.')
  const before=await list()
  const fuzz=[]
  for(const body of ['{','[]','{"numeroDocumento":{"x":1}}','{"@type":"java.lang.Runtime","numeroDocumento":[]}']){
    const res=await call(clients,'POST',maria,body);fuzz.push(res);await res.arrayBuffer()
  }
  const after=await list()
  record('SEC06',fuzz.every(res=>res.status===400)&&Number.isInteger(before)&&before===after,fuzz.map(describe),`Clientes visibles antes=${before}; después=${after}`)
  r=await call(login,'POST',null,JSON.stringify({username:"maria.salas' OR '1'='1",password:'invalid'}))
  record('SEC07',r.status===401,[describe(r)])
  r=await call(users,'OPTIONS',null,undefined,{'Origin':'https://untrusted.example','Access-Control-Request-Method':'GET','Access-Control-Request-Headers':'Authorization'})
  record('SEC08',r.status===403&&!r.headers.has('access-control-allow-origin'),[describe(r)],'Access-Control-Allow-Origin='+(r.headers.get('access-control-allow-origin')??'(ausente)'))
  const times=[],codes=[]
  for(let i=0;i<20;i++){
    const start=performance.now()
    const res=await call(users,'GET','invalid-'+i)
    await res.arrayBuffer()
    times.push(performance.now()-start);codes.push(res.status)
  }
  times.sort((a,b)=>a-b)
  record('SEC09',codes.every(x=>x===401),[`${codes.filter(x=>x===401).length}/20 HTTP 401 p95=${times[18].toFixed(1)} ms max=${times[19].toFixed(1)} ms`])
  if(args.includes('--frontend')){
    const { testFrontend } = await import('./seguridad-frontend.mjs')
    await testFrontend({
      repository: dirname(backend), backendUrl: 'http://127.0.0.1:' + port,
      output: dirname(resolve(outputPath)), password: mariaPassword,
      wrongPassword: randomBytes(24).toString('base64url')
    })
  }
  if(ffufPath){
    const directory=dirname(resolve(outputPath))
    const words=resolve(directory,'rutas-ffuf.txt')
    const json=resolve(directory,'ffuf-rutas.json')
    writeFileSync(words,'health\nclientes\nusuarios\nauth/me\nportal/perfil\nruta-inexistente-seguridad\n')
    const url='http://127.0.0.1:'+port+'/api/v1/FUZZ'
    const args=['-noninteractive','-s','-w',words,'-u',url,'-mc','all','-t','1','-rate','4','-timeout','5','-of','json','-o',json]
    console.log('FFUF',args.join(' '))
    const config=resolve(directory,'ffuf-config')
    mkdirSync(resolve(config,'ffuf','scraper'),{recursive:true})
    await promisify(execFile)(resolve(ffufPath),args,{timeout:30000,env:{...process.env,LOCALAPPDATA:config,XDG_CONFIG_HOME:config}})
    const responses=JSON.parse(readFileSync(json,'utf8')).results
    console.log('FFUF',responses.map(x=>x.url.split('/api/v1/')[1]+'='+x.status).join(' | '))
  }
  if(zapJar){
    const directory=dirname(resolve(outputPath))
    const home=resolve(mkdtempSync(resolve(directory,'zap-run-')),'home')

    const base='http://127.0.0.1:'+port
    const requests=[['/api/v1/health',200],['/api/v1/clientes',401],['/api/v1/usuarios',401]]
      .map(([route,status])=>'  - url: '+base+route+'\n    method: GET\n    responseCode: '+status).join('\n')
    const plan=resolve(directory,'zap-plan.yaml')
    writeFileSync(plan,`env:
  contexts:
  - name: TransAduanas local
    urls:
    - ${base}
  parameters:
    failOnError: true
    failOnWarning: false
jobs:
- type: requestor
  requests:
${requests}
- type: passiveScan-wait
  parameters:
    maxDuration: 2
- type: report
  parameters:
    template: traditional-json
    reportDir: ${JSON.stringify(directory.replaceAll('\\','/'))}
    reportFile: zap-pasivo
    reportTitle: TransAduanas - escaneo pasivo local
`)
    try {
      const args=['-jar',resolve(zapJar),'-dir',home,'-cmd','-config','autoupdate.checkOnStart=false','-autorun',plan]
      const run=await promisify(execFile)('java',args,{timeout:150000,cwd:dirname(resolve(zapJar))})
      writeFileSync(resolve(directory,'ejecucion-zap.txt'),run.stdout+'\n'+run.stderr)
      console.log('ZAP: revisar zap-pasivo.json')
    } catch(error) {
      writeFileSync(resolve(directory,'ejecucion-zap.txt'),String(error.stdout ?? '')+'\n'+String(error.stderr ?? '')+'\n'+error.message)
      throw Error('ZAP terminó con error. Revisa ejecucion-zap.txt')
    }
  }
} catch(error) {
  console.error(error)
  process.exitCode=1
} finally {
  writeFileSync(outputPath,JSON.stringify(rows,null,2))
  child.kill()
}
