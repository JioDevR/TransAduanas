# 6. Guía de Ejecución, Pruebas Automatizadas y Compilación de `app-sga`

**Sistema de Gestión Aduanera - Transaduanas (`app-sga`)**  
*Testing con Vitest, Build de Producción y Solución de Problemas*

---

## Índice del Capítulo
1. [Puesta en Marcha en Entorno de Desarrollo](#1-puesta-en-marcha-en-entorno-de-desarrollo)
2. [Suite de Pruebas Unitarias con Vitest](#2-suite-de-pruebas-unitarias-con-vitest)
   - [2.1 Pruebas de Roles y Permisos](#21-pruebas-de-roles-y-permisos)
   - [2.2 Pruebas de la Calculadora de Cotizaciones](#22-pruebas-de-la-calculadora-de-cotizaciones)
   - [2.3 Pruebas de Utilidades y Descarga de Archivos](#23-pruebas-de-utilidades-y-descarga-de-archivos)
   - [2.4 Ejecución de Pruebas](#24-ejecución-de-pruebas)
3. [Compilación para Producción (Build)](#3-compilación-para-producción-build)
4. [Solución de Problemas Frecuentes (Troubleshooting)](#4-solución-de-problemas-frecuentes-troubleshooting)

---

## 1. Puesta en Marcha en Entorno de Desarrollo

Para ejecutar la aplicación interna `app-sga` de forma local:

```bash
# 1. Ingresar a la carpeta de la aplicación
cd app-sga

# 2. Instalar dependencias de Node.js
npm install

# 3. Iniciar el servidor de desarrollo Vite con Hot Module Replacement (HMR)
npm run dev
```

- **URL de Acceso:** `http://localhost:5180`
- **Requisito Previo:** El backend de Spring Boot debe estar activo en `http://localhost:8085` para permitir el inicio de sesión y la consulta de datos.

---

## 2. Suite de Pruebas Unitarias con Vitest

`app-sga` incluye pruebas unitarias automatizadas desarrolladas con **Vitest**, que garantizan que las reglas matemáticas y los controles de seguridad no se rompan tras modificaciones en el código.

### 2.1 Pruebas de Roles y Permisos
- **Archivo:** [`src/core/auth/__tests__/roles.test.js`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-sga/src/core/auth/__tests__/roles.test.js)
- **Qué valida:** Verifica que los 8 roles institucionales estén correctamente definidos, que cada rol tenga asignada una ruta de inicio (`homePath`) válida y que no existan inconsistencias de nomenclatura entre frontend y backend.

### 2.2 Pruebas de la Calculadora de Cotizaciones
- **Archivos:**
  - `src/features/comercial/composables/__tests__/useCotizacionCalculator.test.js`
  - `src/features/comercial/composables/__tests__/useModalidadPuertos.test.js`
  - `src/features/comercial/components/__tests__/CotizacionCorrectionView.test.js`
- **Qué valida:** Comprueba las fórmulas matemáticas de flete marítimo y aéreo, el cálculo del Ad-Valórem según el arancel NANDINA, la aplicación del IGV (18%) y que los puertos se filtren adecuadamente según la modalidad seleccionada.

### 2.3 Pruebas de Utilidades y Descarga de Archivos
- **Archivo:** [`src/shared/utils/__tests__/download.test.js`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-sga/src/shared/utils/__tests__/download.test.js)
- **Qué valida:** Verifica que la función de descarga invoque `URL.createObjectURL(blob)`, simule el clic del enlace temporal y ejecute inmediatamente `URL.revokeObjectURL(...)` para evitar fugas de memoria en el navegador.

### 2.4 Ejecución de Pruebas

```bash
# Ejecutar toda la suite de pruebas unitarias
npm run test
```

---

## 3. Compilación para Producción (Build)

Para generar los archivos estáticos optimizados y minificados para desplegar en un servidor web de producción (como Nginx, Apache o un contenedor Docker):

```bash
# Compilar el proyecto para producción
npm run build
```

- **Directorio de Salida:** `app-sga/dist/`
- **División de Chunks:** Gracias a la configuración de Rollup en `vite.config.js`, el build separa automáticamente las librerías en fragmentos independientes:
  - `vendor-vue-[hash].js`: Vue 3, Vue Router y Pinia.
  - `vendor-primevue-[hash].js`: Componentes y temas de PrimeVue.
  - `vendor-http-[hash].js`: Cliente Axios.
  - `index-[hash].js` e `index-[hash].css`: Código y estilos específicos de la aplicación.

Para previsualizar localmente el build de producción antes de subirlo al servidor:
```bash
npm run preview
```

---

## 4. Solución de Problemas Frecuentes (Troubleshooting)

1. **Error de red o pantalla blanca al iniciar sesión:**
   - Verifica que el backend de Spring Boot esté encendido en `http://localhost:8085/api/v1/health`. El proxy de Vite redirige las llamadas `/api` a ese puerto.
2. **Error de puerto ocupado (`Port 5180 is in use`):**
   - Si otra terminal dejó el puerto `5180` tomado, puedes detener el proceso previo (`kill -9 $(lsof -t -i:5180)`) o permitir que Vite seleccione el puerto consecutivo (`5181`).
3. **Bucle de redirección al Login:**
   - Si un usuario tiene un token antiguo o corrupto guardado en su navegador, abre la consola del navegador (`F12`), ve a la pestaña **Application > Local Storage** y presiona "Clear All" para resetear la sesión.
