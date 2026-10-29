# 3. Autenticación B2B, Flujo de Primer Acceso y Seguridad

**Sistema de Gestión Aduanera - Transaduanas (`app-portal-cliente`)**  
*Inicio de Sesión por RUC, Cambio Obligatorio de Contraseña y Protección de Rutas*

---

## Índice del Capítulo
1. [El Modelo de Acceso para Clientes Corporativos](#1-el-modelo-de-acceso-para-clientes-corporativos)
2. [Flujo de Autenticación por RUC (11 Dígitos)](#2-flujo-de-autenticación-por-ruc-11-dígitos)
3. [El Flujo de Primer Acceso (Cambio Obligatorio de Contraseña)](#3-el-flujo-de-primer-acceso-cambio-obligatorio-de-contraseña)
4. [Gestión de Sesión con `authStore.js`](#4-gestión-de-sesión-con-authstorejs)
5. [Guardias de Navegación del Router B2B](#5-guardias-de-navegación-del-router-b2b)
6. [Resiliencia ante Almacenamiento Corrupto en el Navegador](#6-resiliencia-ante-almacenamiento-corrupto-en-el-navegador)

---

## 1. El Modelo de Acceso para Clientes Corporativos

En el **Portal del Cliente B2B**, la identidad no se maneja mediante nombres de usuario personales como en el sistema interno (`admin`, `comercial`), sino mediante el **número de RUC oficial de la empresa (11 dígitos)** registrado ante la SUNAT (por ejemplo, `20370146994` para Camposol S.A. o `20511894520` para LG Electronics Peru S.A.).

Este diseño garantiza que:
1. Toda la información mostrada pertenezca estrictamente a la persona jurídica titular del RUC.
2. Los apoderados y personal de comercio exterior de la empresa cliente compartan un acceso corporativo unificado para sus trámites.

---

## 2. Flujo de Autenticación por RUC (11 Dígitos)

El acceso se realiza en [`app-portal-cliente/src/features/auth/views/PortalLoginView.vue`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-portal-cliente/src/features/auth/views/PortalLoginView.vue):

1. **Entrada de Datos:** El cliente ingresa su RUC de 11 dígitos y su contraseña.
2. **Validación en Cliente:** El formulario comprueba que el RUC contenga únicamente dígitos numéricos y tenga longitud exacta de 11 caracteres antes de emitir la petición.
3. **Petición al Backend:** Se envía un `POST /api/v1/auth/login` con `{ "username": "20370146994", "password": "..." }`.
4. **Respuesta del Servidor:**
   - Si las credenciales son válidas, el backend entrega el token JWT con rol `ROLE_CLIENTE`, los datos de la empresa y la bandera booleana `debeCambiarPassword`.
   - `authStore` almacena el token en `localStorage` bajo la clave `portal_token` y el objeto de usuario en `portal_user`.

---

## 3. El Flujo de Primer Acceso (Cambio Obligatorio de Contraseña)

> [!IMPORTANT]
> **Seguridad Bancaria de Primer Acceso:**  
> Cuando el personal aduanero aprueba el primer expediente de un importador, el sistema genera automáticamente una contraseña temporal de bienvenida. Para proteger los datos aduaneros y financieros del cliente, el sistema **fuerza obligatoriamente a cambiar esta clave en el primer inicio de sesión**.

```
[ Cliente ingresa credenciales temporales ] ──▶ [ Login Exitoso ]
                                                       │
                                      ¿debeCambiarPassword == true?
                                                       │
                                          ┌────────────┴────────────┐
                                          ▼ SÍ                      ▼ NO
                     [ Bloqueo de Navegación ]             [ Acceso concedido ]
                     [ Forzar /cambiar-password ]          [ Redirigir a /cotizaciones ]
                                  │
                     [ Cliente ingresa nueva clave ]
                                  │
                     [ PUT /api/v1/portal/auth/cambiar-password ]
                                  │
                     [ debeCambiarPassword = false ]
                                  ▼
                     [ Desbloqueo total del Portal ]
```

- **Vista:** [`PortalCambioPasswordObligatorioView.vue`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-portal-cliente/src/features/auth/views/PortalCambioPasswordObligatorioView.vue)
- **Reglas de Contraseña:** Exige ingresar la clave temporal actual, una nueva contraseña de al menos 8 caracteres (con números y letras) y la confirmación idéntica.
- **Bloqueo Absoluto:** Si el usuario intenta cambiar la URL manualmente en el navegador para saltar a `/cotizaciones` o `/expedientes`, el enrutador lo intercepta y lo regresa inmediatamente a la pantalla de cambio obligatorio.

---

## 4. Gestión de Sesión con `authStore.js`

Ubicado en [`app-portal-cliente/src/core/stores/authStore.js`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-portal-cliente/src/core/stores/authStore.js):

- **Estado (`State`):**
  - `token`: Cadena JWT Bearer.
  - `user`: Datos del cliente (`idCliente`, `numeroDocumento`, `razonSocial`, `debeCambiarPassword`).
- **Getters:**
  - `isAuthenticated`: `true` si existe un token no nulo.
  - `clienteRuc`: Retorna el RUC formateado para mostrar en la barra superior.
  - `clienteRazonSocial`: Razón social de la empresa importadora/exportadora.
- **Acciones:**
  - `login(credentials)`: Autentica y guarda sesión.
  - `logout()`: Limpia `localStorage` y redirige a `/login`.
  - `actualizarPasswordExitoso()`: Marca `user.debeCambiarPassword = false` tras completar el cambio de clave.

---

## 5. Guardias de Navegación del Router B2B

Implementado en [`app-portal-cliente/src/core/router/index.js`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-portal-cliente/src/core/router/index.js):

```javascript
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  const isAuth = authStore.isAuthenticated

  // 1. Título de la pestaña del navegador
  if (to.meta?.title) {
    document.title = to.meta.title
  }

  // 2. Si la ruta requiere autenticación y el cliente no ha iniciado sesión
  if (to.matched.some(record => record.meta?.requiresAuth) && !isAuth) {
    return next('/login')
  }

  // 3. Si está autenticado
  if (isAuth) {
    const debeCambiar = authStore.user?.debeCambiarPassword === true

    // Caso A: Debe cambiar contraseña obligatoria
    if (debeCambiar) {
      if (to.path !== '/cambiar-password-obligatorio') {
        return next('/cambiar-password-obligatorio')
      }
      return next()
    }

    // Caso B: Ya cambió contraseña e intenta regresar al login o pantalla obligatoria
    if (!debeCambiar && (to.path === '/cambiar-password-obligatorio' || to.path === '/login')) {
      return next('/cotizaciones')
    }
  }

  next()
})
```

---

## 6. Resiliencia ante Almacenamiento Corrupto en el Navegador

> [!TIP]
> **Mejora Reciente de Robustez:**  
> En versiones previas, si el contenido de `localStorage.getItem('portal_user')` se dañaba o contenía caracteres inválidos, la aplicación sufría un error de pantalla en blanco (`SyntaxError: Unexpected token in JSON at position 0`).

En `authStore.js`, la deserialización se encuentra protegida con manejo defensivo:

```javascript
try {
  const rawUser = localStorage.getItem('portal_user')
  this.user = rawUser ? JSON.parse(rawUser) : null
} catch (e) {
  console.warn('Datos de sesión corruptos en LocalStorage. Reseteando sesión de forma segura.')
  localStorage.removeItem('portal_user')
  localStorage.removeItem('portal_token')
  this.user = null
  this.token = null
}
```

Si el navegador detecta datos alterados, limpia la caché corrupta pacíficamente y solicita volver a iniciar sesión sin arrojar errores en la consola del usuario.
