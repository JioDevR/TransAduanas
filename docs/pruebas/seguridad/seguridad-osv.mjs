import { readFileSync, writeFileSync } from 'node:fs'
const [treePath, outputPath] = process.argv.slice(2)
const tree = JSON.parse(readFileSync(treePath,'utf8').replace(/^\uFEFF/,''))
const dependencies = new Map()
function visit(node) {
  for (const dep of node.children ?? []) {
    if (dep.scope !== 'test') {
      const name = dep.groupId + ':' + dep.artifactId
      dependencies.set(name + '@' + dep.version, {package:{ecosystem:'Maven',name},version:dep.version})
      visit(dep)
    }
  }
}
visit(tree)
const queries=[...dependencies.values()]
if(!queries.length)throw Error('El árbol no contiene dependencias')
const response=await fetch('https://api.osv.dev/v1/querybatch',{
  method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({queries}),signal:AbortSignal.timeout(45000)
})
if(!response.ok)throw Error('OSV HTTP '+response.status)
const data=await response.json()
if(data.results?.length!==queries.length)throw Error('OSV no devolvió una respuesta por componente')
const inventory=queries.map((dependency,i)=>({
  componente:dependency.package.name, version:dependency.version,
  avisos:(data.results[i].vulns ?? []).map(v=>v.id)
}))
writeFileSync(outputPath,JSON.stringify({fuente:'https://api.osv.dev/v1/querybatch',inventario:inventory},null,2))
console.log('Dependencias consultadas:',inventory.length)
console.log('Componentes con avisos:',inventory.filter(x=>x.avisos.length).length)
for(const item of inventory.filter(x=>x.avisos.length)) console.log(item.componente,item.version,item.avisos.join(', '))
