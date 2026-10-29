# 5. Componentes Compartidos, Servicios de Dominio y Pruebas B2B

**Sistema de Gestión Aduanera - Transaduanas (`app-portal-cliente`)**  
*Capa Shared, Servicios de API, Testing con Vitest y Guía de Puesta en Marcha*

---

## Índice del Capítulo
1. [Capa Compartida (`shared/`)](#1-capa-compartida-shared)
   - [1.1 `BaseModal.vue`: Diálogo Accesible para Clientes](#11-basemodalvue-diálogo-accesible-para-clientes)
   - [1.2 `BasePagination.vue`: Paginador de Tablas](#12-basepaginationvue-paginador-de-tablas)
   - [1.3 `download.js`: Utilidad de Descarga con Revocación de Blobs](#13-downloadjs-utilidad-de-descarga-con-revocación-de-blobs)
   - [1.4 `useTheme.js` & `useNotification.js`](#14-usethemejs--usenotificationjs)
2. [Servicios de Dominio de la API B2B](#2-servicios-de-dominio-de-la-api-b2b)
   - [2.1 `portalExpedienteService.js`](#21-portalexpedienteservicejs)
   - [2.2 `portalCotizacionService.js`](#22-portalcotizacionservicejs)
   - [2.3 `portalPagoService.js`](#23-portalpagoservicejs)
   - [2.4 `portalNotificationService.js`](#24-portalnotificationservicejs)
3. [Pruebas Unitarias Automatizadas con Vitest](#3-pruebas-unitarias-automatizadas-con-vitest)
4. [Puesta en Marcha y Compilación para Producción](#4-puesta-en-marcha-y-compilación-para-producción)
5. [Solución de Problemas Frecuentes (Troubleshooting)](#5-solución-de-problemas-frecuentes-troubleshooting)

---

## 1. Capa Compartida (`shared/`)

Ubicada en [`app-portal-cliente/src/shared/`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-portal-cliente/src/shared/), reúne utilidades y componentes transversales:

### 1.1 `BaseModal.vue`: Diálogo Accesible para Clientes
> [!NOTE]
> **Integración Reciente de Accesibilidad:**  
> Se integró `BaseModal.vue` para unificar todos los diálogos del portal (vista previa de facturas, detalles de expedientes y confirmación de pagos), garantizando que cumplan con las directivas de accesibilidad WAI-ARIA (cierre automático con la tecla `ESC`, bloqueo del scroll de fondo y atrapamiento del foco del teclado).

### 1.2 `BasePagination.vue`: Paginador de Tablas
Proporciona controles de paginación limpios y responsivos en las tablas de cotizaciones y tracking aduanero.

### 1.3 `download.js`: Utilidad de Descarga con Revocación de Blobs
Permite descargar archivos PDF y vouchers bancarios sin saturar la memoria del navegador:
```javascript
export function downloadBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', filename)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  // Revocación inmediata para prevenir fugas de memoria
  window.URL.revokeObjectURL(url)
}
```

### 1.4 `useTheme.js` & `useNotification.js`
- **`useTheme.js`:** Administra el Modo Oscuro y Modo Claro aplicando el atributo `data-theme` a la etiqueta `<html>` y guardando la preferencia del cliente en `localStorage.getItem('portal_theme')`.
- **`useNotification.js`:** Expone los métodos `showSuccess`, `showError` y `showWarning` para presentar notificaciones *Toast* no intrusivas en la esquina superior de la pantalla.

---

## 2. Servicios de Dominio de la API B2B

Ubicados dentro de cada módulo en `features/*/services/`, consumen la instancia `apiClient`:

### 2.1 `portalExpedienteService.js`
- `getExpedientes()`: Consulta la lista de expedientes de la empresa (`GET /api/v1/portal/expedientes`).
- `getExpedienteById(id)`: Obtiene los detalles de la DAM, chofer y contenedor (`GET /api/v1/portal/expedientes/{id}`).
- `descargarDocumento(idDocumento)`: Descarga un archivo digital con `responseType: 'blob'` (`GET /api/v1/portal/documentos/descargar/{id}`).

### 2.2 `portalCotizacionService.js`
- `getCotizaciones()`: Lista las proformas de la empresa (`GET /api/v1/portal/cotizaciones`).
- `descargarPdf(idCotizacion)`: Descarga el PDF formal de la cotización (`GET /api/v1/cotizaciones/{id}/pdf`).

### 2.3 `portalPagoService.js`
- `getEstadoCuenta()`: Retorna el consolidado de facturación y saldo vivo en USD (`GET /api/v1/portal/dashboard`).
- `getObligacionesPendientes()`: Retorna la **deuda heredada** de cotizaciones y liquidaciones (`GET /api/v1/portal/obligaciones-pendientes`).
- `registrarPago(formData)`: Envía el voucher bancario y datos de la operación con `multipart/form-data` (`POST /api/v1/portal/pagos`).

### 2.4 `portalNotificationService.js`
- `getNotificaciones()`: Lista los avisos y alertas (`GET /api/v1/portal/notificaciones`).
- `marcarLeida(idNotificacion)`: Cambia el estado a leído (`PUT /api/v1/portal/notificaciones/{id}/leer`).

---

## 3. Pruebas Unitarias Automatizadas con Vitest

El portal cuenta con una suite de pruebas automatizadas en [`app-portal-cliente/src/core/stores/__tests__/authStore.test.js`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-portal-cliente/src/core/stores/__tests__/authStore.test.js):

### Qué valida la suite:
1. **Inicio de sesión exitoso:** Verifica que el token JWT y los datos del RUC se almacenen adecuadamente en el estado de Pinia y en `localStorage`.
2. **Cierre de sesión seguro (`logout`):** Confirma que se borren completamente los tokens y se restablezca el estado.
3. **Resiliencia ante JSON corrupto:** Simula un valor dañado o manipulado en el almacenamiento del navegador y comprueba que `authStore` no arroje excepciones y limpie la caché de forma segura.

### Comando para ejecutar las pruebas:
```bash
cd app-portal-cliente
npm run test
```

---

## 4. Puesta en Marcha y Compilación para Producción

### Ejecución en Modo de Desarrollo:
```bash
# 1. Ingresar a la carpeta del portal
cd app-portal-cliente

# 2. Instalar dependencias
npm install

# 3. Iniciar el servidor local en el puerto 5174
npm run dev
```

- **Acceso:** `http://localhost:5174`
- **Cuentas de Prueba Pre-Aprovisionadas:**
  - **Camposol S.A.:** RUC `20370146994` / Contraseña `Password123!`
  - **LG Electronics Peru S.A.:** RUC `20511894520` / Contraseña `Password123!`

### Compilación para Producción:
```bash
# Generar los archivos estáticos para el servidor web
npm run build
```
- Los archivos optimizados y minificados se generan en la carpeta `app-portal-cliente/dist/`.

---

## 5. Solución de Problemas Frecuentes (Troubleshooting)

1. **Error al subir el voucher bancario (`File too large`):**
   - El portal limita estrictamente el tamaño de los comprobantes a **10 MB**. Si el archivo supera este límite, la interfaz mostrará una alerta indicando que se debe comprimir la imagen o PDF.
2. **El RUC no inicia sesión:**
   - Verifica que el RUC tenga exactamente 11 dígitos numéricos y que exista en la tabla `usuario_cliente` de la base de datos `SGAdb`.
3. **Pantalla bloqueada en "Actualización Obligatoria de Contraseña":**
   - Esta pantalla es obligatoria para cuentas nuevas con bandera `debeCambiarPassword = true`. El cliente debe definir una nueva contraseña de al menos 8 caracteres para desbloquear el acceso a sus cotizaciones y expedientes.
