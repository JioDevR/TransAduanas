# SGA Theme — Lenguaje de Diseño Unificado (Warm Editorial Pro)

Esta carpeta centraliza el sistema de diseño unificado **`sgaUI`** para servir como guía y referencia única tanto para el **Frontend Interno (`app-sga`)** como para el **Portal B2B (`app-portal-cliente`)** y prototipos sobre **PrimeVue 4**.

---

## Archivos de Referencia en `sgaUI/`

| Archivo | Propósito |
| :--- | :--- |
| [`sgaThemePreset.js`](./sgaThemePreset.js) | Preset oficial de PrimeVue 4 (`definePreset(Aura, ...)`), listo para importar en `main.js`. |
| [`sga-theme.css`](./sga-theme.css) | Reglas CSS base (tipografía, números tabulares, fondos adaptativos, elevación de tarjetas y densidad de tablas). |
| [`README.md`](./README.md) | Documentación completa de paleta, tipografía, espaciado, geometría y guía de uso. |

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
| **Oscuro** | `primary.contrastColor` | `#161412` | Texto espresso oscuro sobre acento durazno pastel |

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
  * Fondo general (`surface.950`): `#161412` (Espresso profundo)
  * Tarjetas / Modales / Topbar (`surface.900`): `#1f1c19`
  * Bordes sutiles (`surface.800`): `#2e2a25`
  * Texto principal (`surface.0` / `surface.50`): `#ffffff` / `#f6f2ea`
  * Texto secundario (`surface.400`): `#968e7e`
  * Resaltado / Selección (`highlight.background`): `rgba(240, 184, 146, 0.16)`

---

## 2. Tipografía

* **Fuente Principal (UI):** `'Inter', system-ui, sans-serif` (tamaño base `14px`, `line-height: 1.5`).
* **Fuente Monoespaciada (Códigos y Datos):** `'JetBrains Mono', monospace`.
* **Números Tabulares (`font-variant-numeric: tabular-nums`):** Aplicado a `DataTable`, `InputNumber` y `code` para alinear verticalmente códigos de expediente (`EXP-001`), RUC, montos USD y pesos VGM.
* **Jerarquía de Títulos:**
  * Títulos de sección: `1.45rem`, peso `700`, `letter-spacing: -0.02em`.
  * Títulos de tarjeta (`.p-card-title`): `1.05rem`, peso `600`, `letter-spacing: -0.015em`.

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
