# Pruebas de seguridad SEG-01 a SEG-10

Esta entrega reúne los diez casos de seguridad y conserva las pruebas funcionales existentes. Cada manual explica herramientas, comandos, criterio, datos ficticios y límites. Los resultados previos corresponden a su entorno de ejecución; repita los comandos al cambiar main.

| Código | Manual y herramientas |
|---|---|
| SEG-01 | [Dependencias](SEG-01-dependencias.md): Maven Dependency Plugin, OSV, Python. |
| SEG-02 | [Secretos](SEG-02-exposicion-secretos.md): Gitleaks y clasificación manual. |
| SEG-03 | [Aislamiento](SEG-03-aislamiento-clientes.md): JUnit, Spring Boot Test, HttpClient, H2. |
| SEG-04 | [Inyección SQL](SEG-04-inyeccion-sql.md): JUnit, HTTP real y H2. |
| SEG-05 | [XSS](SEG-05-defensa-xss.md): Playwright y componentes Vue reales en Chromium. |
| SEG-06 | [Cabeceras y CORS](SEG-06-cabeceras-cors.md): JUnit, HTTP y MockMvc para condición HSTS. |
| SEG-07 | [Validación y JSON](SEG-07-validacion-deserializacion.md): JUnit, HTTP y H2. |
| SEG-08 | [Archivos](SEG-08-carga-archivos.md): JUnit, HTTP, H2 y disco de pruebas. |
| SEG-09 | [ZAP pasivo](SEG-09-zap-pasivo.md): ZAP local y JUnit; requiere SEG09_ZAP_JAR. |
| SEG-10 | [Rendimiento limitado](SEG-10-rendimiento-controlado.md): JUnit y HttpClient; requiere SEG10_RUN=true. |

## Ejecución del backend

Desde backend-sga:

```powershell
.\mvnw.cmd test
.\mvnw.cmd test "-Dgroups=docente15"
```

La suite habitual omite SEG-09 y SEG-10 si no se activan expresamente. No contabilice una omisión como aprobación. SEG-01, SEG-02 y SEG-05 se ejecutan con sus herramientas separadas. No son diez casos JUnit equivalentes: cada código tiene el alcance indicado en su manual.

[Manual de las 15 funcionales seleccionadas](../funcionales/README.md).

## Correcciones necesarias incluidas

Se conserva main y se aplican únicamente correcciones de seguridad: rechazo de claves JWT cortas y secreto externo; contraseñas iniciales sin literales en código; inicializador deshabilitado por defecto; respuestas HTTP 400/405/413/415; restricciones de carga de archivos. Se conservan los tipos enum y las llamadas de auditoría de main.

Los cambios en db/docker-compose.yml, db/seed.sql y db/init-seed.sh eliminan contraseñas/hashes fijos de los datos de laboratorio y requieren valores externos. Son correcciones de SEG-02; no se añade un manual general de compilación o despliegue. Cambiar estos archivos no rota credenciales de bases ya existentes.

## Datos y confidencialidad

No subir .env, propiedades locales, credenciales, target, node_modules, dist, sesiones/perfiles de ZAP, archivos de usuarios o resultados sin revisar. Los datos y claves declarados en los tests son sintéticos; no se deben usar en servicios reales. Las evidencias se generan localmente en carpetas excluidas de Git.
