# SEG-02: exposición de secretos

Se eliminaron las contraseñas fijas detectadas en el código de creación de cuentas, Docker Compose y los scripts de revisión. Los datos sintéticos de las pruebas automatizadas no son credenciales de producción.

## Inicio y acceso

- La inicialización de datos queda deshabilitada si no se configura `app.data-initializer.enabled=true`.
- Para inicializar el administrador se debe proporcionar `BOOTSTRAP_ADMIN_PASSWORD` con al menos 16 caracteres mediante el entorno. El valor no se imprime ni se incluye en el repositorio. Conserve esta contraseña en un gestor seguro.
- Las cuentas B2B creadas automáticamente reciben internamente una credencial aleatoria de 32 bytes. Esta no se muestra ni se envía en notificaciones. Para habilitar el acceso humano, el administrador debe usar el restablecimiento de contraseña existente y comunicar la contraseña temporal por un canal privado. Se exige cambiarla al entrar.
- Docker Compose requiere `MYSQL_ROOT_PASSWORD` y `DB_PASSWORD` en el entorno. Configure `JWT_SECRET` para el backend mediante el entorno.
- Estos cambios afectan cuentas nuevas. Las contraseñas y secretos ya almacenados deben cambiarse también en la instalación donde se hayan utilizado los valores antiguos. Cambiar Docker Compose no cambia contraseñas de una base de datos existente.

## Comprobación

Desde la carpeta `backend-sga`, en PowerShell:

```powershell
.\mvnw.cmd test
```

La revisión usa Gitleaks con los valores ocultos y revisión de los candidatos. No se deben ocultar hallazgos reales para obtener una salida favorable. Los informes antiguos se conservan como evidencia del antes.

## Alcance y criterio

Aprobación de la copia actual: sin credenciales fijas utilizables confirmadas en código/configuración y verificaciones automatizadas correctas. Los marcadores, datos sintéticos y referencias a variables se justifican individualmente.

El historial local previamente escaneado pertenece a otro directorio. No se ha reescrito ni eliminado. Las credenciales históricas no desaparecen al cambiar la copia actual; si alguna se utilizó debe revocarse o rotarse. La aprobación del archivo actual no certifica la rotación de servicios ni la limpieza del historial.

## Datos SQL de laboratorio

`db/seed.sql` ya no contiene contraseñas ni hashes fijos. Docker exige `SGA_SEED_PASSWORD_HASH`: un hash BCrypt generado localmente de una contraseña privada de laboratorio. El script `db/init-seed.sh` valida su formato y lo proporciona a la sesión SQL sin imprimirlo. Para importar el SQL manualmente se debe establecer `@SGA_SEED_PASSWORD_HASH` en la misma sesión. No use cuentas de semilla compartidas fuera del laboratorio. No ejecute el SQL directamente sin esa variable.
## Ejecutar en esta rama

Instale Gitleaks en su equipo. Desde la raíz:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\docs\pruebas\seguridad\Ejecutar-SEG02.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\docs\pruebas\seguridad\Ejecutar-SEG02.ps1 -Historial
```

El ejecutor no imprime secretos. Sus candidatos se deben clasificar antes de indicar aprobación; incluye ejemplos y claves sintéticas de test. No se reescribe historial ni se rotan credenciales de instalaciones existentes.

La configuración y los ejemplos heredados de main que no se modifican en esta entrega siguen requiriendo revisión antes de reutilizar sus valores fuera del laboratorio. Los resultados de la copia local previa no certifican automáticamente esta rama. No se incorporan los scripts ni los documentos de despliegue/monitoreo ajenos a las pruebas.
