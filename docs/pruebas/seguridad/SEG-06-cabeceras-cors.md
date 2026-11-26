# SEG-06: cabeceras HTTP y CORS

Objetivo: comprobar las cabeceras básicas del backend, la lista de orígenes CORS y el tratamiento de solicitudes no permitidas.

## Tipo y herramientas

Prueba de seguridad de configuración HTTP/CORS. Las comprobaciones 1 a 4 son pruebas del sistema del backend con HTTP local real mediante JUnit Jupiter, Spring Boot Test, Java HttpClient, H2 y Maven Surefire. La comprobación 5 es de integración con Spring MockMvc y la cadena real de filtros de seguridad. MockMvc simula solicitudes servlet; no se utiliza Mockito en estos casos.

Desde `backend-sga`, en PowerShell:

```powershell
.\mvnw.cmd test "-Dtest=SecurityHeadersCorsTest"
```

| Comprobación | Criterio de aprobación |
| --- | --- |
| SEG-06.1 | Respuesta pública 200 y protegida 401 con JSON, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` y caché `no-store`. Sin cabecera de permiso CORS cuando no hay Origin. |
| SEG-06.2 | Origen local autorizado: preflight 200, origen exacto sin comodín, credenciales y cabeceras solicitadas permitidas, `Vary` incluye Origin. GET sin sesión sigue devolviendo 401. |
| SEG-06.3 | Origen externo ficticio, Origin null y puerto parecido: OPTIONS y GET devuelven 403 sin reflejar el origen ni permitir credenciales. |
| SEG-06.4 | Preflight con método o cabecera fuera de la lista: 403. No se ejecuta el método TRACE; se envía únicamente una consulta OPTIONS sobre su permiso. |
| SEG-06.5 | HSTS con vigencia de un año cuando la solicitud simulada es segura; ausente en HTTP. Se comprueba con MockMvc, sin negociar TLS real. |

Se usa el origen local `http://localhost:5174`, ya incluido en la configuración predeterminada del proyecto. No se modifica CORS para hacer pasar las pruebas. Si la instalación define otra lista mediante `CORS_ALLOWED_ORIGINS`, el caso positivo debe adaptarse a un origen autorizado de ese entorno; no se debe ampliar la lista solo para pasar una prueba.

Las cadenas de Origin son datos de cabecera: no se realizan conexiones a los dominios ficticios. Las peticiones solo van a 127.0.0.1 con puerto aleatorio y una base H2 de prueba. No se imprimen contraseñas ni tokens.

## Interpretación

401 en un recurso protegido, 403 para un origen/método/cabecera no permitido y ausencia de HSTS en HTTP son comportamientos esperados. CORS restringe el acceso entre orígenes desde el navegador; no sustituye la autenticación ni la autorización.

Se registra además la presencia o ausencia de Content-Security-Policy, Referrer-Policy y Permissions-Policy como observaciones complementarias. No forman parte de los criterios básicos enumerados: no se considera que una API JSON deba tener automáticamente la misma política que una página HTML. La política de las interfaces web y del proxy de despliegue requiere revisión propia.

El resultado debe mostrar cinco comprobaciones sin fallos y BUILD SUCCESS. Los mensajes `[SEG-06.n]` permiten explicar cada caso al docente.

Alcance: respuestas JSON seleccionadas del backend y configuración CORS local. No se revisa el proxy publicado, certificados, conexión HTTPS real, cabeceras de todo el frontend ni enforcement de CORS en un navegador. La comprobación de HSTS acredita la condición del filtro, no la configuración TLS de despliegue.
