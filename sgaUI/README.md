# SGA Theme — Lenguaje de Diseño Unificado (Warm Editorial Pro)

Esta carpeta centraliza el sistema de diseño unificado **`sgaUI`** para servir como guía y referencia única tanto para el **Frontend Interno (`app-sga`)** como para el **Portal B2B (`app-portal-cliente`)** y prototipos sobre **PrimeVue 4**.

---

## Archivos de Referencia en `sgaUI/`

| Archivo | Propósito |
| :--- | :--- |
| [`sgaThemePreset.js`](./sgaThemePreset.js) | Preset oficial de PrimeVue 4 (`definePreset(Aura, ...)`), listo para importar en `main.js`. |
| [`sga-theme.css`](./sga-theme.css) | Reglas CSS base (base `16px`, escala tipográfica `--sga-text-*`, números tabulares, listados, colas `.sga-queue-buttons`, botones `ConfirmDialog` y elevación). Importa `sga-dashboard.css`, `sga-forms.css` y `sga-responsive.css`. |
| [`sga-dashboard.css`](./sga-dashboard.css) | Patrón unificado de dashboards operativos (`.sga-dash-header`, `.sga-filters-card`, `.sga-kpis-grid`, `.sga-kpi-card`, `.sga-charts-grid`, `.sga-doughnut-wrap`). |
| [`sga-forms.css`](./sga-forms.css) | Patrón unificado de formularios `Split-Screen` (`.sga-split-layout`, `.sga-form-header`), stepper horizontal (`.sga-stepper-bar`), campos/etiquetas (`.sga-form-field`, `.sga-form-label`), diálogos (`.sga-dialog-body`, `.sga-dialog-footer`) y banners (`.sga-banner`). |
| [`sga-responsive.css`](./sga-responsive.css) | Estándar oficial de responsividad: 4 breakpoints (`1200px`, `900px`, `640px`), grillas seguras (`.sga-grid-2/3/4/auto`), visibilidad condicional y protección anti-zoom en móvil. |
| [`useResponsive.js`](./useResponsive.js) | Composable reactivo estándar (`useResponsive`, `resolveViewportState`, `SGA_BREAKPOINTS`) sincronizado con los breakpoints CSS para adaptar drawers, modales y lógica de vista. |
| [`sga-auth.css`](./sga-auth.css) | Estilos reutilizables de pantalla de acceso (`.sga-auth-page`, `.sga-auth-card`, `.sga-auth-brand`, `.sga-auth-form`). |
| [`sga-shell.css`](./sga-shell.css) | Cascarón principal unificado (`.sga-shell`, `.sga-sidebar` con Drawer móvil en `<= 900px`, `.sga-topbar`, `.sga-module-card`, `.sga-page-content`) y espaciados del layout. |
| [`README.md`](./README.md) | Documentación completa de paleta, tipografía, espaciado, responsividad y guía de uso. |

---

## 1. Paleta de Colores

La identidad **Warm Editorial Pro** separa el **color estructural** (arena, latte, espresso) del **color de acción** (cobre/terracota), evitando que los botones principales y estados activos se confundan con el texto.

### A. Acento Primario Funcional (`primary`)

| Modo | Token | Hex | Uso principal |
| :--- | :--- | :--- | :--- |
| **Claro** | `primary.color` | `#d48252` | Botones CTA, switches activos, tabs activos, bordes de foco |
| **Claro** | `primary.hoverColor` | `#bf6e40` | Estado hover de botones y acciones primarias |
| **Claro** | `primary.contrastColor` | `#ffffff` | Texto/ícono sobre fondo primario |
| **Oscuro** | `primary.color` | `#f0b892` | Botones CTA, switches y controles activos en fondo oscuro |
| **Oscuro** | `primary.hoverColor` | `#f6cbb0` | Hover en modo oscuro |
| **Oscuro** | `primary.contrastColor` | `#231f1c` | Texto espresso suave sobre acento durazno pastel |

### B. Superficies (`surface`) y Regla de Polaridad en PrimeVue 4

> [!IMPORTANT]
> En **PrimeVue 4 (`Aura`)**, `colorScheme.dark.surface` **NO se invierte**: `surface.0` siempre representa el tono más claro (usado para texto) y `surface.950` el tono más oscuro (usado para fondos).

* **Modo Claro (`light`):**
  * Fondo general (`surface.50`): `#fbf9f6` (Arena cálida)
  * Tarjetas / Modales (`surface.0`): `#ffffff` (Blanco puro)
  * Bordes sutiles (`surface.200` / `surface.300`): `#ede6db` / `#e4dacb`
  * Texto principal (`surface.800`): `#352f2a` (Espresso)
  * Texto secundario (`surface.600`): `#6f6356`
  * Resaltado / Selección (`highlight.background`): `#fdf1e8` con texto `#824728`

* **Modo Oscuro (`dark`):**
  * Fondo general (`surface.950`): `#231f1c` (Espresso suave / cacao ahumado)
  * Sidebar (`--color-bg-sidebar`): `#1d1a17`
  * Tarjetas / Modales / Topbar (`surface.900`): `#2b2622`
  * Bordes sutiles (`surface.800`): `#38322d`
  * Texto principal (`surface.0` / `surface.50`): `#ffffff` / `#f6f2ea`
  * Texto secundario (`surface.400`): `#968e7e`
  * Resaltado / Selección (`highlight.background`): `rgba(240, 184, 146, 0.16)`

---

## 2. Tipografía

* **Base Raíz:** `:root` normalizado a `16px` (`1rem = 16px`), con tamaño operativo de lectura `14px` (`--sga-text-base: 0.875rem`) en `body`.
* **Fuente Principal (UI):** `'Inter', system-ui, sans-serif` (`line-height: 1.5`).
* **Fuente Monoespaciada (Códigos y Datos):** `'JetBrains Mono', monospace`.
* **Números Tabulares (`font-variant-numeric: tabular-nums`):** Aplicado a `code`, `.p-datatable`, `.p-inputnumber-input`, `.font-mono`, `.sga-mono`, `.sga-kpi-value`, `.sga-amount-cell` y `.sga-total-amount`.
* **Escala Tipográfica Estándar (`sga-theme.css`):**
  * `--sga-text-2xs` (`0.6875rem` / `11px`): Micro-etiquetas de formulario (`.sga-form-label`) y kickers.
  * `--sga-text-xs` (`0.75rem` / `12px`): Metadatos, fechas, sub-etiquetas de celda y hints.
  * `--sga-text-sm` (`0.8125rem` / `13px`): Botones `small`, filtros y celdas densas.
  * `--sga-text-base` (`0.875rem` / `14px`): Cuerpo operativo estándar.
  * `--sga-text-md` (`1rem` / `16px`): Subtítulos destacados e inputs en móvil (`<= 640px`, evita auto-zoom en iOS).
  * `--sga-text-lg` (`clamp(1rem, 0.96rem + 0.2vw, 1.125rem)`): Títulos de tarjeta (`.p-card-title`) y topbar.
  * `--sga-text-xl` (`clamp(1.125rem, 1.04rem + 0.4vw, 1.35rem)`): Títulos de vista/formulario (`.sga-form-title`, `.sga-panel-title`).
  * `--sga-text-2xl` (`clamp(1.25rem, 1.1rem + 0.7vw, 1.65rem)`): Cifras KPI y totales de proforma.

---

## 3. Espaciado, Densidad y Geometría

* **Rejilla base:** Múltiplos de `4px` / `8px`.
* **Radios de borde (`borderRadius`):**
  * Inputs, selects y botones (`formField`, `button`): `10px`
  * Tarjetas, paneles y modales (`card`, `panel`, `dialog`): `14px`
* **Padding:**
  * Inputs (`formField`): `0.55rem 0.85rem`
  * Tarjetas (`card.body`): `1.25rem` (`gap: 0.75rem`)
  * Tablas (`DataTable` celdas y cabeceras): `0.65rem 0.9rem` (densidad operativa para visualizar más registros sin saturar).
* **Cascarón Principal (`sga-shell.css`):**
  * Sidebar: ancho `256px` (colapsado `72px`), padding `1rem 0.75rem`, ítems en píldora (`border-radius: 9999px`) con punto indicador y tarjeta `GESTOR DE MÓDULOS` anclada al pie (`margin-top: auto`).
  * Topbar: altura mínima `64px` en una sola línea (`padding: 0.75rem 2rem`), **título único de vista (`<h1 class="sga-topbar-title">`)** a la izquierda y pill `SEDE` + perfil a la derecha.
  * **Regla de Título Único:** El título principal de la pantalla vive exclusivamente en `.sga-topbar`. Las vistas internas no deben duplicar un título `<h1>`/`<h2>` debajo de la topbar; su barra superior (`.sga-dash-header` / `.sga-form-header`) debe mostrar únicamente estado/metadatos contextuales (`.sga-dash-meta`) y acciones (`<Button size="small">`).
  * Contenido principal: `padding: 1.5rem 2rem` (en móvil `<= 900px` pasa a `1rem`).
* **Elevación:** En lugar de sombras pesadas, se combinan `1px solid var(--p-surface-200)` (claro) / `var(--p-surface-800)` (oscuro) con sombra mínima.

---

## 4. Cómo integrarlo en `app-sga` o `app-portal-cliente`

1. Importar las fuentes `Inter` y `JetBrains Mono` en `index.html`.
2. Importar `SgaThemePreset` desde `sgaUI/sgaThemePreset.js` en `main.js`:

```js
import PrimeVue from 'primevue/config'
import SgaThemePreset from '../../sgaUI/sgaThemePreset'
import '../../sgaUI/sga-theme.css'

app.use(PrimeVue, {
  theme: {
    preset: SgaThemePreset,
    options: {
      darkModeSelector: '.dark-theme'
    }
  }
})
```

3. Puedes previsualizar y comparar este tema en vivo contra los presets oficiales de PrimeVue en el proyecto de pruebas [`app-primevue-showcase`](../app-primevue-showcase) (`http://localhost:5190/`).

---

## 5. Patrón de Asistente / Wizard Minimalista (`Split-Screen`)

Referencia aplicada en el wizard de cotización comercial (`app-sga/src/features/comercial`) y apertura de expedientes (`app-sga/src/features/aduanas`), estandarizada en [`sga-forms.css`](./sga-forms.css):

* **Cabecera directa (`.sga-form-header`, `.sga-form-title`, `.sga-form-actions`):** Una sola fila con título principal (`1.45rem`, peso `700`) a la izquierda y dos botones compactos (`size="small"`) a la derecha: `Cancelar` (secundario `outlined`) y botón primario contextual (`Continuar →` en pasos intermedios; acción de guardado en el último paso). Sin kickers ni párrafos introductorios.
* **Stepper horizontal (`.sga-stepper-bar`, `.sga-stepper-item`, `.is-active`, `.is-completed`):** Barra única (`border-radius: 14px`, `1px solid var(--color-border)`) con círculo numerado (`26px`), micro-etiqueta (`PASO 0X`) y título. Al estar el título visible en el stepper, se omite la cabecera repetida dentro de la tarjeta de cada paso.
* **Layout `Split-Screen` (`.sga-split-layout`, `.sga-split-main`, `.sga-split-aside`):** Rejilla `minmax(0, 1fr) 360px` (`gap: 1.5rem`, colapsa a `1fr` en `<= 1120px`). La columna lateral `sticky` muestra el resumen en vivo con `--font-mono` y `tabular-nums`, sin duplicar los botones de acción del header.
* **Padding lateral estandarizado:** Todas las tarjetas de contenido (`.sga-card-surface` / pasos del wizard / resumen lateral) usan `--sga-card-padding-y: 1.75rem` y `--sga-card-padding-x: 2rem` (`1.25rem` en `<= 768px`), y los subpaneles internos usan `--sga-subpanel-padding: 1.25rem 1.35rem`, garantizando aire uniforme en esquinas y bordes laterales.
* **Formularios internos, diálogos y banners:** Etiquetas en micro-mayúsculas (`.sga-form-label` / `.sga-field-label`, `0.6875rem`, peso `700`, `letter-spacing: 0.04em`, `--color-text-muted`), campos en columna (`.sga-form-field`), pies de diálogo (`.sga-dialog-footer`), banners de estado (`.sga-banner` con `.is-info`, `.is-warn`, `.is-danger`, `.is-success`) y controles PrimeVue 4 (`InputText`, `InputNumber`, `Select`, `Textarea`) con `size="small"` y `fluid`.

---

## 6. Patrón de Dashboards Operativos (`sga-dashboard.css`)

Referencia compartida por **Comercial** (`ComercialDashboardPanel.vue`) y **Gestión Aduanera** (`DashboardAduaneroPanel.vue`), centralizada en [`sga-dashboard.css`](./sga-dashboard.css):

* **Cabecera y barra de filtros:** `.sga-dash-header` (con `.sga-dash-meta` para estado/píldoras y `.sga-dash-actions` para botones, sin duplicar el título de la topbar), `.sga-filters-card`, `.sga-filters-grid`, `.sga-filter-field`, `.sga-filter-reset`.
* **Tarjetas KPI:** `.sga-kpis-grid` y `.sga-kpi-card` con variantes `.is-success`, `.is-warn`, `.is-danger` y píldora de porcentaje/estado `.sga-kpi-pill`.
* **Gráficos Chart.js:** `.sga-charts-grid`, `.sga-chart-card`, `.sga-chart-head`, `.sga-chart-box` (`height: 240px`), y centro de dona `.sga-doughnut-wrap` / `.sga-doughnut-center`.

---

## 7. Listados, Colas de Trabajo y Confirmaciones (`sga-theme.css`)

* **Listados Duales (Tabla / Canvas):** `.sga-list-panel`, `.sga-panel-header`, `.sga-table-card`, `.sga-table-toolbar`, `.sga-canvas-grid`, `.sga-canvas-card`, `.sga-empty-state`.
* **Sub-barra de Colas y Notas:** `.sga-queue-buttons` y `.sga-status-note` para el slot `#sub-bar` de `SgaListShell.vue`.
* **ConfirmDialog:** `.sga-confirm-accept-btn`, `.sga-confirm-reject-btn` y `.sga-confirm-delete-btn` con soporte automático para modo claro y oscuro.

---

## 8. Estándar de Responsividad y Reactividad (`sga-responsive.css` + `useResponsive.js`)

Todo el ecosistema (`app-sga` y `app-b2b`) opera bajo **4 breakpoints oficiales**:

| Tier | Media Query | Dispositivos | Comportamiento Estándar |
| :--- | :--- | :--- | :--- |
| **`desktop`** | `> 1200px` | Monitores y laptops estándar | Sidebar sticky (`256px` / `72px`), `--sga-shell-gutter: 2rem`, Split-Screen (`1fr 360px`), grids `.sga-grid-3/4` completos |
| **`tablet-lg`** | `<= 1200px` | Laptops compactas / Tablet landscape | Split-Screen colapsa a `1fr`, `.sga-grid-4` pasa a 2 columnas |
| **`tablet`** | `<= 900px` | Tablets portrait y phablets | **Sidebar pasa a Drawer deslizable con backdrop y botón hamburguesa en Topbar** (`.sga-mobile-menu-btn`), `--sga-shell-gutter: 1.25rem`, `.sga-grid-3` pasa a 2 columnas, `.sga-stepper-bar` 2×2 |
| **`mobile`** | `<= 640px` | Smartphones (`320px–640px`) | `--sga-shell-gutter: 0.875rem`, **1 sola columna** en todos los grids y filtros, inputs a `16px` (anti-zoom iOS), tablas contenidas con scroll táctil dentro de `.sga-table-card` sin desbordar el viewport |

* **Uso reactivo en Vue (`useResponsive`):**
  ```js
  import { useResponsive } from '@/shared/composables/useResponsive'
  const { isMobile, isTabletOrBelow, viewportTier, getDialogWidth } = useResponsive()
  ```
  Úsese para adaptar dinámicamente anchos de `Dialog` (`getDialogWidth('540px')`), cerrar drawers al expandir la ventana o conmutar comportamiento en vistas complejas.
* **Reglas obligatorias contra desbordamiento de Grids y Selects:**
  * Usar siempre `minmax(0, 1fr)` en columnas CSS Grid (nunca `repeat(N, 1fr)` sin `minmax(0, 1fr)`), y `min-width: 0` en hijos de grid/flex que contengan `Select` o texto largo.
  * El contenedor `.sga-split-main` define `container-type: inline-size` (`container-name: sga-main`), permitiendo usar `@container sga-main (max-width: 600px)` para colapsar formularios de 2 columnas cuando el panel principal se estrecha (por ejemplo con el Sidebar expandido en laptops de `1280px`).

