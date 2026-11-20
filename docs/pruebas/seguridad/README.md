# Manual de pruebas de seguridad — versión 0.5.2

Estos casos verifican comportamiento de seguridad existente. No sustituyen una auditoría completa. Se ejecutan con Java 21 y Maven Wrapper desde `backend-sga`; no necesitan la base original ni las claves privadas del servicio.

## Ejecutar las 13 juntas

Desde la raíz del proyecto:

```powershell
Set-Location .\backend-sga
.\mvnw.cmd test "-Dtest=JwtTokenProviderTest,SecurityConfigTest" "-Dsurefire.reportNameSuffix=seguridad"
```

Esperado: **13 pruebas, 0 fallos, 0 errores, 0 omitidas y BUILD SUCCESS**. Son 6 unitarias JWT y 7 de integración Spring/MockMvc. Los colaboradores de negocio de MockMvc están simulados; este conjunto no demuestra persistencia MySQL ni una sesión completa en navegador.

## Casos y comprobaciones

Para ejecutar uno, sustituye `CLASE#METODO` por el selector exacto de la tabla:

```powershell
.\mvnw.cmd test "-Dtest=CLASE#METODO"
```

| N.º | Nombre y resultado esperado | Selector exacto |
|---|---|---|
| 1 | Token interno válido: firma y claims esperados. | `JwtTokenProviderTest#generateToken_usuarioInternoValido_retornaClaimsCorrectos` |
| 2 | Token del portal: identidad y empresa en claims. | `JwtTokenProviderTest#generateTokenForPortal_usuarioClienteValido_retornaClaimsB2B` |
| 3 | Secreto corto: inicio y firma rechazados con WeakKeyException. | `JwtTokenProviderTest#generateToken_secretCorto_rechazado` |
| 4 | Token expirado: validación false y excepción al leer claims. | `JwtTokenProviderTest#validateToken_tokenExpirado_retornaFalse` |
| 5 | Token nulo, vacío o espacios: validación false. | `JwtTokenProviderTest#validateToken_nuloOVacio_retornaFalse` |
| 6 | Token malformado o firmado con otra clave: rechazo. | `JwtTokenProviderTest#validateToken_malformadoOFirmaInvalida_retornaFalse` |
| 7 | Ruta login accesible sin token: 200 con servicio simulado. No comprueba contraseña real. | `SecurityConfigTest#authLogin_sinToken_esPermitidoPorSecurityConfig` |
| 8 | Perfil sin token: 401 y success=false. | `SecurityConfigTest#authMe_sinToken_retorna401Unauthorized` |
| 9 | Perfil con token interno válido: 200 y usuario esperado. | `SecurityConfigTest#authMe_conTokenInternoValido_retorna200` |
| 10 | Token del portal en perfil interno: 403. | `SecurityConfigTest#authMe_conTokenPortal_retorna403Forbidden` |
| 11 | Comercial sin permiso de transporte: 403 en ambos endpoints. | `SecurityConfigTest#despachoMisDespachos_rolSinPermisoTransporte_retorna403` |
| 12 | Supervisor con permiso de transporte: 200. | `SecurityConfigTest#despachoMisDespachos_supervisorTransporte_retorna200` |
| 13 | Roles de solo lectura: GET 200; POST 405, Allow=GET y success=false. | `SecurityConfigTest#roles_soloLectura_rechazaPost` |

La prueba 3 ya no acepta claves cortas mediante relleno con ceros. La 13 ya no acepta HTTP 500. Las claves de prueba se declaran en el código de test y no sirven para configurar el servicio real.

## Comprobaciones por HTTP real del backend

Desde la misma carpeta:

```powershell
.\mvnw.cmd test "-Dtest=SystemApiTest" "-Dsurefire.reportNameSuffix=seguridad-http"
.\mvnw.cmd test "-Dtest=AcceptanceBusinessTest#AC04_dadoDocumentoCuandoOtroClienteDescargaEntoncesDenegado" "-Dsurefire.reportNameSuffix=aislamiento"
```

Esperado: 12 pruebas en SystemApiTest y 1 en el selector de aceptación. La primera clase combina salud, negocio y seguridad; no son 12 pruebas exclusivamente de seguridad. Comprueba, entre otros, credenciales incorrectas 401, ausencia de token 401, token inválido 401 y rol sin permiso 403. El caso de aceptación verifica descarga del propietario y rechazo 403 para otra empresa. Usan servidor HTTP local y H2 temporal; están incluidos en las 346 del backend.

## Auditoría de dependencias de interfaces

Vuelve a la raíz del proyecto:

```powershell
Set-Location ..
npm.cmd --prefix .\app-sga ci
npm.cmd --prefix .\app-b2b ci
npm.cmd --prefix .\app-landing ci
npm.cmd --prefix .\app-sga audit
npm.cmd --prefix .\app-b2b audit
npm.cmd --prefix .\app-landing audit
```

En esta entrega, `follow-redirects` está fijado por los lockfiles en 1.16.3; después de actualizar se observaron cero avisos en SGA y B2B. La auditoría consulta información que cambia: si aparecen avisos nuevos, guarda el resultado y evalúalos antes de aprobar. No ejecutes automáticamente `npm audit fix --force`, porque puede cambiar otras versiones.

Repite pruebas y build después de cambiar dependencias:

```powershell
npm.cmd --prefix .\app-sga test
npm.cmd --prefix .\app-b2b test
$env:VITE_API_URL = '/api/v1'
$env:VITE_SGA_APP_URL = 'http://localhost:5180'
$env:VITE_PORTAL_CLIENTE_URL = 'http://localhost:5174'
npm.cmd --prefix .\app-sga run build -- --mode staging
npm.cmd --prefix .\app-b2b run build -- --mode staging
```

## Evidencias y errores frecuentes

Guarda los XML `*-seguridad.xml`, `*-seguridad-http.xml` y `*-aislamiento.xml` de `backend-sga/target/surefire-reports`, más los resúmenes de npm. Un caso individual debe mostrar 1 prueba; el grupo JWT/MockMvc debe mostrar 13. No dupliques esos conteos al presentar el total general.

Si la aplicación real no arranca por JWT_SECRET ausente o débil, configura una clave privada aleatoria de al menos 32 bytes según la [guía de instalación](../../instalacion.md). No reincorpores el secreto predeterminado. Si Mockito no puede adjuntar su agente en un entorno aislado, ejecuta en PowerShell local con los permisos adecuados y conserva el error del entorno separado del resultado de las pruebas.

Continúa con el [manual de despliegue](../despliegue/README.md).

## Verificación de este manual

El 20 de noviembre de 2026 se repitió el grupo de 13 casos con la versión corregida: 13 aprobados, cero fallos/errores/omisiones. Evidencia: evidencias-revision/manual-seguridad.log y XML con sufijo manual-seguridad en target/surefire-reports. Las comprobaciones HTTP y auditorías npm se habían aprobado durante la revisión de la corrección; no se volvieron a ejecutar al editar este manual.
