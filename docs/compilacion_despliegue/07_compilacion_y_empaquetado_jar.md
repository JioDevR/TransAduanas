# Compilación y empaquetado del backend — 0.5.2

Para ejecutar las 15 pruebas de despliegue y conservar evidencias usa el [manual vigente](../pruebas/despliegue/README.md). Esta guía resume solo la construcción; no inicia servicios ni acredita un despliegue.

Desde la raíz del proyecto, en PowerShell:

```powershell
Set-Location .\backend-sga
java -version
.\mvnw.cmd -v
.\mvnw.cmd clean verify
Get-FileHash .\target\sga-backend-0.5.2.jar
```

Se requiere Java 21 y dependencias descargadas o conexión para Maven. Resultado esperado en esta versión: 346 pruebas, cero fallos/errores/omisiones, BUILD SUCCESS y target/sga-backend-0.5.2.jar. Las pruebas usan datos simulados o H2 temporal y no necesitan MySQL.

Si se acaba de aprobar una selección y no cambió el código, `package "-DskipTests"` genera el paquete sin repetir tests; no equivale a haber ejecutado las 346. Usa `install "-DskipTests"` para instalar una copia en .m2 y compara sus hashes según la prueba 5 del manual.

Para iniciar el JAR se necesitan las variables del entorno. Desde 0.5.2, JWT_SECRET es obligatorio y las claves menores de 32 bytes se rechazan. El perfil staging usa MySQL, valida el esquema sin alterarlo y desactiva el inicializador. Sigue la prueba 7 del manual; no pegues credenciales en los comandos ni uses H2/create-drop sobre la base original.

No compiles con clean mientras el JAR manual se ejecuta desde target. El servicio Windows usa una copia estable separada. Nunca sustituyas un JAR activo copiándolo manualmente; usa el gestor con su registro y recuperación.
