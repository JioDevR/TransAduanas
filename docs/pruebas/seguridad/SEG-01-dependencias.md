# SEG-01 Dependencias del backend

Desde la raíz del repositorio, con Java 21 y Python 3:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\docs\pruebas\seguridad\Ejecutar-SEG01.ps1
```

Herramientas: Maven Dependency Plugin 3.10.0, API pública OSV y Python estándar. El ejecutor resuelve el árbol real y consulta solo coordenadas públicas y versiones en api.osv.dev; no envía código, claves o datos de usuarios. Requiere internet.

Resultado, inventario y avisos se generan en backend-sga/target/seg01-FECHA. Si hay avisos o falla la consulta no se debe registrar como aprobada sin revisar. El número de dependencias depende de la versión: las 147 del informe previo pertenecen a la copia modificada, no son un resultado garantizado para main. El control positivo del ejecutor verifica que OSV pueda devolver avisos para una dependencia conocida vulnerable; ese paquete no se añade al proyecto.

Alcance: dependencias Maven resueltas; no frontend, plugins, JDK, sistema operativo ni contenedores. Sin avisos en OSV no equivale a seguridad absoluta.
