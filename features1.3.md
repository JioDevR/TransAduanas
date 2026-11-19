# Cambios — features/1.3

- Centralización del preset de PrimeVue en `sgaUI/sgaThemePreset.js`.
- Nuevos tokens de diseño compartidos en `sgaUI/sga-theme.css`.
- Estilos de autenticación movidos a `sgaUI/sga-auth.css`.
- Documentación del sistema de diseño en `sgaUI/README.md`.
- `app-sga` y `app-portal-cliente` usan el preset compartido; se elimina el preset duplicado en sus `main.js`.
- `app-sga/src/style.css` ya no duplica colores, radios, sombras ni overrides de PrimeVue.
- Alias `@primevue/themes` agregado en los `vite.config.js` de ambas apps.
- `LoginView` reescrito con componentes PrimeVue y clases `sga-auth-*`.
- `resolvePostLoginRoute` extraído y cubierto en `LoginView.test.js`.
- Modales de aduanas migrados a componentes PrimeVue.
- Modales de almacén y transporte migrados a componentes PrimeVue.
- Modales de catálogos y usuarios del portal migrados a componentes PrimeVue.
- Headers de agente aduanero y transporte reescritos con componentes PrimeVue.
- `BasePagination` usa `Button` y `Tag` de PrimeVue.
- Nuevo catálogo de referencia `app-primevue-showcase` con secciones de componentes, `sga-components`, `sga-masters` y `sga-mocks`.

## Cascarón principal unificado (sgaUI)

- Nuevo `sgaUI/sga-shell.css` con el cascarón reutilizable (`.sga-shell`, `.sga-sidebar`, `.sga-topbar`, `.sga-nav-btn`, `.sga-module-card`, `.sga-page-content`): sidebar de 256px (colapsado 72px), topbar de 64px y gutter de 2rem (1rem en pantallas ≤900px).
- Paleta oscura recalibrada en `sgaUI/sga-theme.css` y `sgaUI/sgaThemePreset.js` a espresso suave: `surface-950 #231f1c`, `surface-900 #2b2622`, `surface-800 #38322d`, `surface-700 #4a433c`; `primary.contrastColor` pasa a `#231f1c`.
- `sgaUI/README.md` documenta `sga-shell.css`, `sga-auth.css`, la nueva paleta oscura y las métricas del cascarón.
- `AppLayout.vue` reescrito sobre `.sga-shell` (920 → 206 líneas): topbar en una sola línea con título limpio, pill `SEDE` y perfil de usuario; el menú de cuenta migra a `Popover` + `ToggleSwitch` + `Button` de PrimeVue y desaparecen el dropdown manual y los listeners de `click`.
- `AppSidebar.vue` reducido a navegación declarativa por módulo mediante `currentNavItems` (1100 → 260 líneas), con tarjeta `GESTOR DE MÓDULOS` anclada al pie, estado de colapso local y sin kickers/subtítulos duplicados.
- `navigationStore` expone `DEFAULT_MODULE_SUB_OPTION` y la acción `resetModuleToDefaultView(module)`.
- El router restaura la vista inicial del módulo cuando la ruta no trae `?tab` (comercial, gestión aduanera, agente aduanero, transporte y flota, legal y auditoría, finanzas) en lugar de conservar la sub-opción anterior.
- Test de `resetModuleToDefaultView` por módulo en `navigationStore.test.js`.

## Portal Cliente B2B sobre el cascarón sgaUI

- Nuevo `app-portal-cliente/src/core/layouts/PortalSidebar.vue`: sidebar `.sga-sidebar` con tarjeta de empresa autenticada (nombre + RUC), navegación en píldora con punto indicador y colapso a 72px.
- `PortalLayout.vue` reescrito sobre `.sga-shell` (topbar de una línea): título por ruta, campana de notificaciones con badge y popover de cuenta con tema (ToggleSwitch) y cierre de sesión. Se eliminan el header de dos barras y el footer corporativo.
- Notificaciones deja de ser ítem del menú lateral y pasa al icono de la campana en la topbar; el conteo se carga con `watch` inmediato sobre la ruta.
- `app-portal-cliente/src/style.css` ya no duplica tokens de color, radios, sombras ni overrides de PrimeVue: los hereda de `sgaUI/sga-theme.css`.

## Rediseño comercial, bandeja aduanera y reporte ejecutivo PDF

- `RecepcionComercialView` se descompone en paneles: `ComercialDashboardPanel` (KPIs, 3 gráficos con `primevue/chart` y exportación de periodos HOY / 7D / MES), `CotizacionesTablePanel`, `SolicitudesSinAsignarPanel` y `ClientesDirectoryPanel`, cada uno con vista tabla y vista canvas (`*CanvasGrid`).
- `CotizacionRegistrationForm` pasa de 4098 a 255 líneas: la lógica vive en `useCotizacionForm` y los pasos en `CotizacionStepCliente`, `CotizacionStepRegimen`, `CotizacionStepCostos` y `CotizacionStepDocumentos`; `CotizacionFormHeader` y `CotizacionStepperNav` se rediseñan como wizard split-screen.
- Nuevo `SgaListShell` compartido (y `ComercialListShell` comercial) para alternar vista tabla / canvas en los listados de ambas bandejas.
- `GestionAduaneraView` usa `AduanasCotizacionesPanel`, `AduanasCotizacionesCanvasGrid` y `CotizacionRevisionDialog`; el estado se extrae a `useAduanasCotizaciones` y los formateadores a `aduanasFormatters`.
- Nuevo endpoint `GET /cotizaciones/reporte-comercial-pdf` (`ReporteJasperServiceImpl` + plantilla `reports/reporte-comercial.html`) con filtros por rango de fechas, asesor y régimen; `comercialService.exportarReporteComercialPdf` lo consume como blob y lo abre en pestaña nueva.
- `sgaUI/sga-theme.css` agrega los tokens `--sga-card-padding-y/x`, `--sga-card-radius` y `--sga-subpanel-padding`, con las clases `.sga-card-surface`, `.sga-list-panel` y `.sga-table-card`; el README documenta el patrón wizard minimalista.
- Dependencia `chart.js` agregada; nuevos tests para `useComercialDashboard`, `useAduanasCotizaciones`, `aduanasFormatters` y el PDF del reporte comercial.

## Gestión aduanera por bandejas y apertura en vista completa

- `GestionAduaneraView` baja de 1933 a 550 líneas: orquesta `DashboardAduaneroPanel`, `AduanasCotizacionesPanel`, `AduanasAperturaPanel` y `AduanasExpedientesPanel`, cada listado con vista tabla y vista canvas (`AduanasAperturaCanvasGrid`, `AduanasExpedientesCanvasGrid`).
- `DashboardAduaneroPanel` delega las métricas en `useAduanasDashboard` y añade gráficos Chart.js de canales SUNAT, tendencia y etapas del expediente.
- La apertura de expedientes deja de ser un modal: nuevo `AperturaExpedienteFormView` en la ruta `/gestion-aduanera/apertura/:idCotizacion`, con `useAperturaExpedienteForm`, aviso de elegibilidad y acción «Formalizar y Aperturar Expediente».
- Se eliminan `AperturaExpedienteView` y `LiquidacionAduaneraView`, la ruta `/gestion-aduanera/liquidacion` y las sub-opciones `declaraciones`, `clasificacion` y `tributos` del sidebar y del store de navegación.
- `resolvePeriodoRange` se muda a `shared/utils/dateUtils` (y se reexporta desde `useComercialDashboard`); `aduanasFormatters` agrega `getCanalSeverity` y `getExpedienteSeverity`.
- Tests nuevos para `useAduanasDashboard`, `useAduanasApertura`, `useAduanasExpedientes` y `useAperturaExpedienteForm` (130 tests en total).
