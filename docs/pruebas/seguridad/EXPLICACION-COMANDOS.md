# Explicación de los comandos de seguridad

Explicaciones de los comandos utilizados en CAPTURAS.md y README.md. Los comandos repetidos tienen una sola explicación.

## Repositorio y versiones

| Comando | Explicación |
| --- | --- |
| `git clone --branch … --single-branch …` | Descarga el proyecto y prepara la rama de seguridad indicada. |
| `cd` o `Set-Location` | Cambia la carpeta de trabajo de la terminal. |
| `git fetch origin` | Actualiza la información de las ramas disponibles en GitHub. |
| `git switch pruebas/seguridad-transaduanas` | Cambia a la rama de pruebas de seguridad. |
| `git pull --ff-only origin pruebas/seguridad-transaduanas` | Descarga los cambios de la rama y actualiza el clon si puede avanzar directamente. |
| `git branch --show-current` | Muestra el nombre de la rama actual. |
| `java -version` | Muestra la versión de Java que reconoce la terminal. |
| `node -v` | Muestra la versión de Node.js. |
| `npm.cmd -v` | Muestra la versión de npm. |
| `python --version` | Muestra la versión de Python. |
| `.\mvnw.cmd -v` | Muestra las versiones de Maven y Java que usa el proyecto. |

## Rutas y variables

| Elemento | Explicación |
| --- | --- |
| `$raiz` | Guarda la carpeta principal del clon. |
| `$env:USERPROFILE` | Representa la carpeta del usuario de Windows. |
| `$backend` | Guarda la ubicación del backend. |
| `$kit` | Guarda la ubicación de los scripts de seguridad. |
| `$script` | Guarda la ruta del script que se ejecutará. |
| `$resultado` | Indica la carpeta donde se guardan los resultados. |
| `$archivoDeps` y `$deps` | Guardan la ruta del listado de dependencias y su contenido. |
| `$cp` | Guarda la ruta del archivo que indica a Java dónde encontrar las clases y dependencias. |
| `$mysqlBin` | Guarda la carpeta que contiene los ejecutables de MySQL. |
| `$ffuf` y `$zap` | Guardan las rutas de las herramientas indicadas en el bloque. Para el ejecutor conjunto, ZAP corresponde al archivo JAR. |
| `$archivo`, `$carpeta` y `$ruta` | Identifican el nombre, la carpeta y la ruta completa del informe que se consulta. |
| `$casos` y las variables `$casosFrontend`, `$casosMySQL`, `$casosSistema` | Guardan los resultados leídos desde un informe JSON. |
| `$conteo` | Guarda el caso BD06 para mostrar la cantidad de filas antes y después. |
| `Get-Location` | Obtiene la carpeta actual de la terminal. |
| `Join-Path` | Une una carpeta con un nombre de archivo o una subcarpeta. |
| `Resolve-Path` | Obtiene la ruta absoluta de un archivo o una carpeta existente. |
| `Test-Path -PathType Leaf` | Comprueba que el archivo indicado existe. |
| `New-Item -ItemType Directory -Force` | Crea la carpeta de resultados o utiliza la que ya existe. |
| `Read-Host` | Espera que escribas el dato solicitado y pulses Enter. |
| `Get-Command python -CommandType Application` | Busca el ejecutable de Python que reconoce la terminal. |
| `$python.Source` | Contiene la ruta del ejecutable de Python encontrado. |
| `$env:JAVA_HOME` | Indica la carpeta de instalación de Java configurada en la terminal. |

## Preparación y ejecución

| Comando u opción | Explicación |
| --- | --- |
| `.\mvnw.cmd -B compile dependency:build-classpath` | Compila el backend y obtiene las rutas de sus dependencias. |
| `-B` | Ejecuta Maven en modo por lotes. |
| `-DincludeScope=test` | Incluye dependencias necesarias para el laboratorio, como H2. |
| `-Dmdep.outputFile=…` | Indica dónde guardar las rutas de dependencias obtenidas por Maven. |
| `.Trim()` | Quita espacios y saltos de línea de los extremos del texto leído. |
| `Set-Content` aplicado al classpath | Guarda las clases compiladas y las dependencias en un archivo para arrancar el backend. |
| `node …seguridad-http.mjs …` | Arranca el backend local con H2 y ejecuta los diez casos HTTP. |
| `--frontend` | Añade los ocho casos del SGA en navegador a la ejecución HTTP. |
| `npm.cmd ci` | Instala las dependencias con las versiones del archivo package-lock.json y reemplaza node_modules. |
| `.\node_modules\.bin\playwright.cmd install chromium` | Descarga Chromium para las pruebas de navegador cuando se necesita. |
| `node …seguridad-mysql.mjs …` | Prepara una instancia local separada de MySQL y ejecuta ocho comprobaciones de permisos y datos. |
| `--sistema` | Añade ocho casos que comprueban el funcionamiento integrado de la API, las cuentas y los permisos. |
| `node …seguridad-general.mjs …` | Ejecuta los bloques de backend, navegador, MySQL y sistema, y resume los 34 casos. |
| `$codigo = $LASTEXITCODE` | Conserva el código de salida del último programa ejecutado. |

## Postman

| Comando u opción | Explicación |
| --- | --- |
| `npm.cmd install --prefix … newman@6.2.1 --no-audit --no-fund` | Instala Newman 6.2.1 en la carpeta indicada y omite los mensajes de auditoría y financiación durante esa instalación. |
| `--postman` | Añade la ejecución de las 16 solicitudes de la colección mediante Newman. |
| `--postman-servidor` | Mantiene el laboratorio abierto y genera el entorno temporal para ejecutar la colección en Postman Desktop. |
| `Ctrl+C` | Solicita el cierre del laboratorio que permanece abierto en la terminal. |

## ffuf y ZAP

| Elemento | Explicación |
| --- | --- |
| `$ffuf = Read-Host …` | Solicita la ruta completa de ffuf.exe. |
| `$zap = Read-Host …` | Solicita la ruta completa del JAR de ZAP; Enter deja ZAP fuera de esa ejecución. |
| `$opciones = @(…)` | Crea una lista con la ruta de ffuf. |
| `$opciones += …` | Añade la ruta de ZAP a la lista cuando se proporciona. |
| `@opciones` al llamar a Node | Entrega cada ruta de la lista como un argumento del script. |
| Ejecución HTTP con las rutas de ffuf y ZAP | Ejecuta los casos HTTP y añade la búsqueda de seis rutas con ffuf y el análisis pasivo de tres peticiones con ZAP. |

## Dependencias y configuración

| Comando u opción | Explicación |
| --- | --- |
| `.\mvnw.cmd -B dependency:tree` | Obtiene el árbol de dependencias del backend. |
| `-Dscope=runtime` | Selecciona las dependencias necesarias durante la ejecución de la aplicación. |
| `-DoutputType=json` | Solicita el árbol de dependencias en formato JSON. |
| `-DoutputFile=…` | Indica dónde guardar ese árbol. |
| `node …seguridad-osv.mjs …` | Consulta OSV para buscar avisos de vulnerabilidades asociados a las dependencias Maven. |
| `foreach ($app in …)` | Repite la revisión para SGA, portal de clientes y landing. |
| `Push-Location` | Entra en la carpeta de una aplicación y recuerda la carpeta anterior. |
| `Pop-Location` | Regresa a la carpeta guardada. |
| `npm.cmd audit --omit=dev --json` | Consulta vulnerabilidades de las dependencias de producción y devuelve un informe JSON. |
| `$informe.metadata.vulnerabilities.total` | Obtiene el total de alertas registrado por npm audit. |
| `python …check-config.py …` | Revisa cinco patrones de configuración y devuelve el archivo, la línea y el detalle de cada caso. |
| `& $python.Source $script $raiz` | Ejecuta esa revisión con el Python encontrado y la ruta del proyecto. |
| `$salida -join [Environment]::NewLine` | Reúne las líneas devueltas por Python para leerlas como un documento JSON. |

## Guardado y visualización de resultados

| Comando u opción | Explicación |
| --- | --- |
| `2>&1` | Une los mensajes de error con la salida normal del programa. |
| `\|` | Envía la salida de un comando al siguiente. |
| `Tee-Object -FilePath …` | Muestra la salida en la terminal y también la guarda en un archivo. |
| `>` | Guarda la salida en un archivo y reemplaza su contenido anterior. |
| `Get-Content -Raw` | Lee todo el contenido de un archivo como texto. |
| `-Encoding UTF8` | Lee o guarda el texto con una codificación que permite conservar las tildes. |
| `ConvertFrom-Json` | Convierte el informe JSON en datos que PowerShell puede ordenar y filtrar. |
| `Sort-Object id` | Ordena las filas por el identificador del caso. |
| `Format-Table … -Wrap` | Presenta los campos elegidos como una tabla y ajusta el texto largo. |
| `Where-Object id -eq 'BD06'` | Selecciona el caso BD06 de los resultados cargados. |
| `Format-List *` | Muestra todos los campos del caso seleccionado. |
| `Set-Content` aplicado al informe | Guarda el resultado en el archivo indicado. |
| `Invoke-Item $resultado` | Abre la carpeta de resultados en el Explorador de archivos. |
| `SELECT COUNT(*) FROM SGAdb.cliente` | Cuenta los registros de clientes para comparar la cantidad antes y después de los intentos de escritura. |

## Control de ejecución en PowerShell

| Elemento | Explicación |
| --- | --- |
| `. { … }` | Ejecuta un bloque y conserva sus variables en la terminal actual. |
| `& { … }` | Ejecuta un bloque con variables propias. |
| `&` delante de una ruta | Ejecuta el programa indicado por esa ruta. |
| `if (…)` | Comprueba una condición antes de continuar. |
| `-not` y `-or` | Expresan una negación y una alternativa en una condición. |
| `$LASTEXITCODE -ne 0` | Comprueba si el último programa devolvió un código distinto de cero. |
| `throw` | Detiene el bloque y muestra el motivo del error. |
| `try … finally` | Garantiza que se ejecute la limpieza final, como regresar a la carpeta anterior. |
| `-LiteralPath` | Utiliza la ruta escrita sin interpretar sus caracteres como comodines. |
| `-ErrorAction Stop` | Hace que un error del comando de PowerShell detenga la ejecución. |
| `Out-Null` | Oculta una salida que no se necesita mostrar. |
