# Qué hace cada bloque de las pruebas

## Descargar el proyecto

Si aún no tienes el proyecto, descarga el repositorio. Si ya existe en `Music\TransAduanas`, abre esa carpeta y usa la preparación común de CAPTURAS.md.

## Actualizar el proyecto

Comprueba la rama actual antes de actualizar el repositorio. Los scripts incluidos se pueden ejecutar sin cambiar a la rama histórica de seguridad.

## Comprobar las versiones

Muestra las versiones de Java, Node.js, npm y Maven, y la rama actual del proyecto.

## Preparar el entorno

Ubica el proyecto, comprueba que exista el script de pruebas y crea la carpeta de resultados. Después compila el backend y prepara las dependencias que necesita Java para ejecutar las pruebas.

## Probar el backend

Arranca el backend con una base H2 temporal y ejecuta diez casos de seguridad. Guarda la salida y muestra una tabla con el resultado de cada caso.

## Probar el frontend SGA

Instala las dependencias del SGA y ejecuta ocho casos en un navegador. Comprueba la sesión y las restricciones de acceso, guarda los resultados y las imágenes, y abre la carpeta de evidencias.

## Instalar Chromium si hace falta

Descarga el navegador que Playwright puede utilizar para ejecutar las pruebas del frontend.

## Probar MySQL

Crea una instancia local separada de MySQL y ejecuta ocho casos sobre permisos y acceso a los datos. Muestra si la cuenta de lectura pudo realizar las operaciones y compara la cantidad de clientes antes y después.

## Probar el sistema integrado

Ejecuta ocho casos que comprueban juntos la API, la autenticación, los permisos y los datos temporales. Incluye la separación entre empresas y la desactivación y reactivación de una cuenta.

## Ejecutar la prueba general

Ejecuta los cuatro bloques de backend, frontend, MySQL y sistema. Al terminar, muestra cuántos casos se ejecutaron, cuántos pasaron y cuántos presentaron hallazgos.

## Ejecutar la colección con Newman

Instala Newman y ejecuta las 16 solicitudes de la colección de Postman desde la terminal. Guarda el resultado de las 32 verificaciones.

## Preparar Postman Desktop

Mantiene abierto el servidor de pruebas y genera un entorno con las direcciones y credenciales temporales. Ese entorno permite ejecutar la colección desde Postman mientras la terminal sigue abierta.

## Ejecutar ffuf y ZAP

Solicita las rutas de ambas herramientas y arranca el laboratorio local. Además de los casos HTTP, revisa seis rutas con ffuf y, si se proporciona ZAP, analiza pasivamente tres peticiones. Guarda los informes para revisarlos.

## Revisar las dependencias del backend

Obtiene la lista de librerías que usa el backend y consulta OSV para buscar avisos de vulnerabilidades asociados a sus versiones. Guarda el informe de la consulta.

## Revisar las dependencias de los frontends

Revisa las librerías de producción del SGA, el portal de clientes y la landing mediante npm audit. Guarda un informe por aplicación y muestra la cantidad de alertas encontradas.

## Revisar la configuración con Python

Lee archivos del proyecto y busca cinco patrones relacionados con la seguridad. Muestra qué encontró, en qué archivo y en qué línea, y guarda el informe en configuracion.json.

## Mostrar los resultados guardados

Lee el informe de una ejecución anterior y muestra sus casos ordenados en una tabla.

## Mostrar un caso concreto

Selecciona un caso del informe cargado y muestra todos sus detalles. En el ejemplo de BD06, permite revisar la cantidad de clientes antes y después de las operaciones.
