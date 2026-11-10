import json, re, sys
from pathlib import Path

root = Path(sys.argv[1])
def check(id, relative_path, expression, finding):
    lines = (root / relative_path).read_text(encoding='utf-8-sig').splitlines()
    match = next((n for n, line in enumerate(lines, 1) if re.search(expression, line)), None)
    return {'id':id, 'archivo':relative_path, 'linea':match,
            'estado':'Patrón confirmado' if match else 'Patrón ausente', 'detalle':finding}

base='backend/src/main/'
results=[
    check('CFG01',base+'resources/application.properties',r'^jwt\.secret=\$\{JWT_SECRET:[^}]+}', 'Existe un secreto de firma JWT de respaldo conocido desde el código.'),
    check('CFG02',base+'java/pe/sga/config/DataInitializer.java',r'passwordEncoder\.encode\("[^"]+"\)', 'El inicializador asigna una contraseña fija a una cuenta demo.'),
    check('CFG03',base+'resources/application.properties',r'^spring\.datasource\.url=.*useSSL=false', 'El JDBC por defecto desactiva TLS. Evaluar si la base de datos sale de localhost.'),
    check('CFG04','db/docker-compose.yml',r'^\s+MYSQL_ROOT_PASSWORD:\s+[^$\s]', 'El compose incluye una contraseña fija para root.'),
    check('CFG05','app-sga/src/features/auth/stores/authStore.js',r'localStorage\.setItem\(.sga_token', 'El token queda en almacenamiento accesible desde JavaScript. El impacto depende de que exista XSS.')
]
print(json.dumps(results,ensure_ascii=True,indent=2))
