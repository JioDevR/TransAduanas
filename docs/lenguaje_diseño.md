# Manual del Lenguaje de Diseño Oficial (SGA Design System)

**Sistema:** SGA - Plataforma de Gestión Aduanera Integral  
**Versión del Sistema de Diseño:** 2.0 (Editorial Cálido & Titanio Carbón)  
**Propósito:** Estandarizar la interfaz visual, tipografía, paleta cromática, espaciados y componentes para garantizar coherencia, elegancia y alta usabilidad en todos los módulos de la aplicación.

---

## 1. Filosofía de Diseño

El sistema visual de **SGA** adopta una estética **Warm Editorial (Minimalismo Editorial Cálido)** inspirada en interfaces ejecutivas modernas de alta gama:
- **Calidez y profesionalismo:** Reemplaza los blancos fríos estériles y los grises azulados genéricos por una paleta basada en tonos arena, marfil, lino y café espresso.
- **Claridad jerárquica:** Tipografía nítida con contraste semántico evidente, reduciendo la fatiga visual de operadores que pasan jornadas completas frente al sistema.
- **Eficiencia espacial:** Escala de espaciados armónica basada en múltiplos de 4px, optimizando la densidad de datos sin aglomerar los elementos.

---

## 2. Tipografía Oficial Definitiva

### 2.1. Fuentes Corporativas
El proyecto adopta formalmente dos familias tipográficas estándar:

| Uso | Familia Tipográfica | Pesos Cargados | Justificación |
| :--- | :--- | :--- | :--- |
| **Principal / UI General** | **Inter** (`var(--font-sans)`) | 300, 400, 500, 600, 700, 800 | Excelente legibilidad en pantallas de alta densidad, trazos geométricos humanistas, neutralidad ejecutiva. |
| **Datos Normativos & Aduaneros** | **JetBrains Mono** (`var(--font-mono)`) | 400, 500, 600, 700 | Alineación perfecta para códigos de expedientes (`EXP-2026-0001`), cotizaciones (`COT-2026-0001`), DAMs, RUCs y cifras monetarias en dólares (`$ USD`). |

### 2.2. Escala Modular Tipográfica (Type Scale)

```css
:root {
  --font-size-2xs: 0.6875rem; /* 11px: Kickers, micro-etiquetas, metadatos secundarios */
  --font-size-xs:  0.75rem;   /* 12px: Fechas, badges compactos, subtítulos de celda */
  --font-size-sm:  0.8125rem; /* 13px: Entradas de texto, botones secundarios, celdas de tabla */
  --font-size-base:0.875rem;  /* 14px: Cuerpo principal de lectura, descripciones */
  --font-size-md:  0.9375rem; /* 15px: Subtítulos intermedios, opciones de menú */
  --font-size-lg:  1.125rem;  /* 18px: Títulos de tarjetas analíticas y modales */
  --font-size-xl:  1.25rem;   /* 20px: Encabezados de sección maestra */
  --font-size-2xl: 1.45rem;   /* 23.2px: Título de página en la barra superior (Topbar) */
  --font-size-3xl: 1.875rem;  /* 30px: Valores numéricos destacados en tarjetas KPI */
}
```

### 2.3. Jerarquía y Pesos Tipográficos
- **Regular (400):** Textos de lectura, valores de celdas secundarias, notas al pie.
- **Medium (500):** Etiquetas de formularios, botones auxiliares, migas de pan.
- **Semi-Bold (600):** Títulos de tablas, encabezados de columnas, nombres de clientes.
- **Bold (700):** Títulos principales, identificadores de sub-opciones, kickers en mayúsculas.
- **Extra-Bold (800):** Cifras métricas de alto impacto (KPI counters).

---

## 3. Paleta Cromática y Tokens Semánticos

El sistema cuenta con soporte bidireccional automático para **Modo Claro** y **Modo Oscuro**, manteniendo siempre la identidad cálida del SGA.

```mermaid
graph LR
    subgraph Modo Claro (Warm Editorial)
        LC1[Base: #f6f3ee] --> LC2[Superficie: #ffffff]
        LC2 --> LC3[Borde: #e4dacb]
        LC3 --> LC4[Espresso: #352f2a]
        LC4 --> LC5[Kicker: #9a7f60]
    end

    subgraph Modo Oscuro (Titanio Carbón)
        DC1[Base: #1e1e1b] --> DC2[Superficie: #282723]
        DC2 --> DC3[Borde: #444138]
        DC3 --> DC4[Oro Cálido: #d6b476]
        DC4 --> DC5[Kicker: #e0c287]
    end
```

### 3.1. Tokens en Modo Claro (`:root`)

```css
:root {
  /* Fondos Base */
  --color-bg-base:    #f6f3ee; /* Fondo general de aplicación (Arena lino) */
  --color-bg-sidebar: #ede6db; /* Fondo de la barra vertical */
  --color-bg-surface: #ffffff; /* Superficie de tarjetas, modales y tablas */
  --color-bg-subtle:  #f2ebe1; /* Headers de tabla, chips y estados hover */
  --color-bg-muted:   #efe8de; /* Separadores y fondos secundarios */

  /* Bordes */
  --color-border:        #e4dacb; /* Borde estándar de componentes */
  --color-border-subtle: #ede5d8; /* Borde tenue de divisores internos */
  --color-border-strong: #d5c8b6; /* Borde enfatizado para hover y foco */

  /* Identidad y Énfasis */
  --color-primary:       #352f2a; /* Espresso tostado para botones activos */
  --color-primary-hover: #484039; /* Espresso iluminado al pasar el cursor */
  --color-primary-text:  #ffffff; /* Texto sobre botones principales */
  --color-accent-kicker: #9a7f60; /* Bronce caramelo para categorías */

  /* Textos */
  --color-text-main:   #2b2520; /* Carbón profundo para títulos y lectura */
  --color-text-muted:  #6f6356; /* Tono medio para descripciones */
  --color-text-subtle: #998c7e; /* Tono atenuado para fechas y metadatos */

  /* Estados Funcionales Semánticos Suaves */
  --color-success-bg:     #eaf6ed;
  --color-success-border: #bce5c6;
  --color-success-text:   #1e6638;

  --color-warning-bg:     #fdf5e6;
  --color-warning-border: #f8dab3;
  --color-warning-text:   #8c5b16;

  --color-danger-bg:      #fdf0f0;
  --color-danger-border:  #f6c8c8;
  --color-danger-text:    #9e2a2b;

  --color-info-bg:        #eef4fb;
  --color-info-border:    #cadff4;
  --color-info-text:      #21598f;
}
```

### 3.2. Tokens en Modo Oscuro (`.dark-theme`)

```css
.dark-theme {
  /* Fondos Base */
  --color-bg-base:    #1e1e1b; /* Gris carbón cálido (evita negro puro #000) */
  --color-bg-sidebar: #191815; /* Titanio profundo para el sidebar */
  --color-bg-surface: #282723; /* Gris piedra cálido para tarjetas y modales */
  --color-bg-subtle:  #2e2d27; /* Headers de tabla y chips */
  --color-bg-muted:   #383630; /* Fondos atenuados */

  /* Bordes */
  --color-border:        #444138;
  --color-border-subtle: #36342c;
  --color-border-strong: #5c564a;

  /* Identidad y Énfasis */
  --color-primary:       #d6b476; /* Arena dorada luminosa */
  --color-primary-hover: #e5c78f;
  --color-primary-text:  #1d1b15; /* Contraste oscuro sobre botón dorado */
  --color-accent-kicker: #e0c287;

  /* Textos */
  --color-text-main:   #f6f2ea; /* Blanco marfil cálido */
  --color-text-muted:  #c4bcab; /* Avena suave */
  --color-text-subtle: #968e7e; /* Gris piedra medio */

  /* Estados Funcionales Semánticos en Modo Oscuro */
  --color-success-bg:     #1c2b1f;
  --color-success-border: #2d5738;
  --color-success-text:   #7ee79d;

  --color-warning-bg:     #342914;
  --color-warning-border: #695222;
  --color-warning-text:   #fcd34d;

  --color-danger-bg:      #321818;
  --color-danger-border:  #652a2a;
  --color-danger-text:    #fca5a5;

  --color-info-bg:        #172635;
  --color-info-border:    #254b6d;
  --color-info-text:      #93c5fd;
}
```

---

## 4. Sistema de Espaciado Estándar (Rejilla de 4px)

Para erradicar valores arbitrarios en márgenes y rellenos, se establece una escala proporcional estricta:

| Token CSS | Equivalencia en REM | Valor en Píxeles | Propósito Típico |
| :--- | :--- | :--- | :--- |
| `--space-1` | `0.25rem` | `4px` | Separación mínima entre microelementos, chips. |
| `--space-2` | `0.5rem` | `8px` | Espacio entre ícono y etiqueta de botón, padding compacto. |
| `--space-3` | `0.75rem` | `12px` | Brecha entre controles de filtro e inputs inline. |
| `--space-4` | `1.0rem` | `16px` | Relleno estándar interno de celdas y tarjetas de grid. |
| `--space-5` | `1.25rem` | `20px` | Relleno de modales y separación entre paneles. |
| `--space-6` | `1.5rem` | `24px` | Separación vertical entre bloques maestros de la página. |
| `--space-8` | `2.0rem` | `32px` | Separador de secciones completas. |
| `--space-10` | `2.5rem` | `40px` | Márgenes perimetrales de pantallas amplias. |
| `--space-12` | `3.0rem` | `48px` | Espacio superior/inferior para estados vacíos o héroes. |

### 4.1. Arquitectura de Ancho de Contenedores y Vistas (Full-Width Layout)

Para mantener la armonía con la barra superior (Topbar), definir bordes perimetrales holgados y erradicar inconsistencias donde ciertas vistas se perciben comprimidas o asimétricas:

1. **Relleno Lateral Perimetral Oficial (`3.5rem` / `56px`):** Se estandariza el padding horizontal tanto en la barra superior (`.app-topbar: padding: 0.85rem 3.5rem;`) como en el contenedor general de vistas (`.page-content: padding: 2rem 3.5rem 3rem;`). Esta separación perimetral de 3.5rem genera un encuadre visual despejado y elegante en toda la interfaz sin apretar los componentes contra el sidebar o los bordes de la ventana.
2. **Ancho Fluido al 100% en Todas las Vistas:** Todos los módulos del sistema (Recepción Comercial, Gestión Aduanera, Finanzas, Transporte de Flota, Liquidación, etc.) operan al 100% del ancho disponible (`width: 100%`) dentro de `.page-content`.
3. **Prohibición de `max-width` Restrictivos en Vistas Completas:** Se prohíbe el uso de propiedades como `max-width: 1400px; margin: 0 auto;` en las raíces de vista (`.ventas-view`, `.finanzas-view`, `.cotizacion-registration-form-view`, `.cliente-registration-form-view`), asegurando que en pantallas panorámicas el contenido ocupe el ancho correspondiente con márgenes uniformes.
4. **Estandarización de Columnas en Formularios con Panel Lateral:** Los formularios de alta densidad con barra de previsualización (como Proforma Oficial o Credencial del Cliente) definen la rejilla `.form-content-grid` con `grid-template-columns: 1fr 400px;` y `gap: var(--space-6, 1.5rem);`, adaptándose a una sola columna en pantallas menores a 1080px.

---

## 5. Radios y Elevaciones (Sombras)

```css
:root {
  /* Radios de Curvatura */
  --radius-xs:   0.25rem; /* 4px: Badges diminutos, kickers */
  --radius-sm:   0.375rem;/* 6px: Botones de tabla, inputs */
  --radius-md:   0.5rem;  /* 8px: Botones estándar, selectores */
  --radius-lg:   0.75rem; /* 12px: Tarjetas, contenedores de tabla */
  --radius-xl:   1.0rem;  /* 16px: Diálogos modales y paneles flotantes */
  --radius-pill: 9999px;  /* Píldora: Tags de estado, pills de sede */

  /* Sombras Sutiles */
  --shadow-subtle:   0 1px 3px rgba(45, 35, 25, 0.04), 0 1px 2px rgba(45, 35, 25, 0.02);
  --shadow-card:     0 4px 16px -2px rgba(50, 40, 30, 0.04), 0 2px 6px -1px rgba(50, 40, 30, 0.02);
  --shadow-elevated: 0 12px 28px -6px rgba(45, 35, 25, 0.08), 0 4px 10px -2px rgba(45, 35, 25, 0.03);
}
```

---

## 6. Catálogo de Componentes Estandarizados

### 6.1. Barra Superior (Topbar)
- **Altura fija:** `min-height: 76px;`
- **Área izquierda (`.topbar-left`):**
  - Miga de pan sutil con separador cálido (`/`).
  - Kicker de módulo en mayúsculas (`0.625rem`, bold, tracking `0.08em`).
  - Título de sección (`1.4rem`, bold, letter-spacing `-0.015em`).
  - Subtítulo explicativo editorial (`0.8125rem`, text-muted) que sustituye banners invasivos.
- **Área derecha (`.topbar-right`):**
  - Sede Pill fija: `SEDE [Código] — [Nombre]`.
  - Botón de alternancia de tema (Modo Claro / Modo Oscuro).
  - Perfil de usuario con avatar con iniciales y rol asignado.

### 6.2. Barra Lateral (Sidebar)
- **Estructura numerada:** Todas las opciones se enumeran con prefijo de dos dígitos (`01`, `02`, `03`, etc.).
- **Estado activo:** Resaltado mediante el token `--color-primary` con número en contraste suave.
- **Acciones directas:** No saturar la lista vertical con botones de acción operativa; delegar a los dashboards de cada módulo.

### 6.3. Tarjetas de Métricas (KPI Cards)
- Formato de 3 columnas en desktop, 2 en tablet y 1 en móvil.
- Encabezado con número de orden, nombre de la métrica e ícono temático.
- Valor numérico destacado en `2.15rem` con fuente `Inter` bold.
- Insignia semántica de apoyo (ej. monto en `$ USD`, porcentaje de conversión `%`).
- Pie con divisoria punteada para información contextual secundaria.

### 6.4. Botones Estándar

| Clase | Visual | Uso |
| :--- | :--- | :--- |
| `.btn-espresso` | Fondo Espresso / Arena (Dark), texto contraste, sombra suave. | Acción principal positiva (`+ Nueva Cotización`, `Guardar`, `Confirmar`). |
| `.btn-outline-warm` | Fondo superficie, borde cálido, hover a sutil. | Acciones secundarias (`Cancelar`, `Actualizar`, `Bandeja`). |
| `.action-btn-outline` | Botón compacto para filas de tabla. | Acciones operativas de fila (`Docs`, `Editar`, `Corregir`). |
| `.btn-danger-action` | Fondo tenue rojizo al hover. | Acciones destructivas (`Eliminar`, `Anular`). |

### 6.5. Desplegables y Selectores (Dropdowns & Selects)

El diseño de desplegables en SGA estandariza tanto los selectores nativos HTML (`<select>`) como los componentes de catálogo interactivo (`PrimeVue <Select>` / `.sga-custom-select`).

#### A. Selectores Nativos HTML (`<select>`, `.form-select`, `.select-input`, `.filter-select`, `.form-select-styled`)
- **Eliminación del glifo genérico del SO:** Se fuerza `appearance: none !important; -webkit-appearance: none !important; -moz-appearance: none !important;` para eliminar la flecha rígida del sistema operativo.
- **Chevron SVG Cálido Integrado (Anti-Repetición Estricta):**
  - **Modo Claro:** Chevron en tono tierra cálido (`#6f6356`), renderizado inline como vector SVG codificado en base64 en `background-image`.
  - **Modo Oscuro:** Chevron en tono dorado arena (`#d6b476`).
  - **Bloqueo de Repetición:** Se exige `background-repeat: no-repeat !important;` con posición `background-position: right 0.75rem center !important;` y dimensión `background-size: 1rem 1rem !important;`.
- **Prohibición de Shorthand `background:`:** Queda terminantemente prohibido utilizar el atajo CSS `background: ...` en selectores nativos o clases agrupadas, ya que resetea la propiedad `background-repeat` a `repeat` causando una hilera repetida de chevrons (`V V V V V`). Debe emplearse siempre `background-color`.
- **Relleno y alineación:** `padding: 0.6rem 2.25rem 0.6rem 0.85rem !important;` garantizando un margen de seguridad a la derecha para que el texto seleccionado jamás se superponga con la flecha desplegable.
- **Opciones internas (`option`):** Heredan el fondo de superficie (`var(--color-bg-surface)`) y color de texto principal (`var(--color-text-main)`), erradicando destellos blancos en temas oscuros.
- **Anillo de Foco Accesible:** `box-shadow: 0 0 0 3px rgba(53, 47, 42, 0.08);` en claro y `rgba(214, 180, 118, 0.15);` en oscuro.

```css
/* Ejemplo canónico para selectores nativos */
select, .form-select, .select-input, .filter-select, .form-select-styled {
  appearance: none !important;
  font-family: var(--font-sans) !important;
  font-size: var(--font-size-sm);
  color: var(--color-text-main) !important;
  background-color: var(--color-bg-surface) !important;
  border: 1px solid var(--color-border) !important;
  border-radius: var(--radius-md) !important;
  padding: 0.6rem 2.25rem 0.6rem 0.85rem !important;
  background-image: url("data:image/svg+xml,...chevron-svg...") !important;
  background-repeat: no-repeat !important;
  background-position: right 0.75rem center !important;
  background-size: 1rem 1rem !important;
}
```

#### B. Selectores de Catálogo Avanzado (`PrimeVue <Select>` / `.p-select` / `.sga-custom-select`)
- **Caja de Selección:**
  - Superficie limpia con `background: var(--color-bg-surface)` y borde `var(--color-border)`.
  - Placeholder atenuado con token `var(--color-text-subtle)`.
  - Transición suave de 150ms al enfocar o pasar el cursor.
- **Panel Desplegable Flotante (`.p-select-overlay`):**
  - Desplegado mediante Teleport con `z-index: 9999`.
  - Bordes pulidos `var(--radius-lg)` con sombra profunda `--shadow-elevated`.
  - **Buscador integrado:** Cabecera con campo de texto que filtra en tiempo real puertos, clientes o tipos de carga sin recargar la página.
  - **Lista de Opciones (`.p-select-option`):** Ítems con espaciado ergonómico (`padding: 0.5rem 0.75rem`), esquinas redondeadas (`--radius-md`) y resaltado en hover (`var(--color-bg-subtle)`).
  - **Opción Activa:** Fondo suave de selección (`var(--color-bg-muted)`) y texto en color primario resaltado.

### 6.6. Barras de Encabezado de Formularios y Modales (Form Header Bars)

Para erradicar la sobrecarga cognitiva y la fatiga de navegación en formularios de alta complejidad (ej. *Nueva Cotización*, *Nuevo Cliente*), se establece la **Barra de Encabezado** como estándar transversal:

1. **Única Fuente de Verdad para Acciones (Principio Anti-Duplicidad):**
   - Todos los botones de control final (`Cancelar`, `Guardar Cambios`, `Emitir Cotización`) residen **exclusivamente en la cabecera superior derecha** del formulario.
   - **Regla Estricta:** Se prohíbe duplicar botones idénticos en tarjetas laterales ("Checklist de Requisitos para Emisión") o en barras inferiores flotantes. Las tarjetas laterales deben limitarse a previsualizaciones informativas (ej. Proforma Viva, Credencial del Cliente).
2. **Estructura Modular del Encabezado (`.form-header-bar`):**
   - **Enlace de Retorno (`.btn-back-link`):** Flecha direccional sutil para volver a la tabla o bandeja anterior sin perder contexto.
   - **Fila de Kickers (`.kicker-row`):** Micro-etiqueta temática (`.badge-kicker`) y pastilla de modo operativo (`.mode-pill`: `.mode-create` para altas nuevas, `.mode-edit` para modificaciones).
   - **Título Principal (`.form-main-title`):** Tipografía `Inter` en peso bold 700 con tracking `-0.015em`, descartando fuentes serif para mantener la uniformidad contemporánea.
   - **Descripción Editorial (`.form-main-desc`):** Resumen conciso en tono cálido que guía al operador aduanero en el llenado de los campos requeridos.
   - **Acciones Agrupadas (`.header-actions`):** Alineación a la derecha con botón secundario `.btn-outline-warm` y botón primario `.btn-espresso` con estado de carga interactivo (`pi-spinner`).

---

## 7. Directrices de Implementación para Desarrolladores

1. **Prohibido el uso de colores hexadecimales duros (`#...`) inline:** Usar siempre las variables de diseño (`var(--color-...)`).
2. **Respetar la tipografía tabular:** Los identificadores normativos (`DAM`, `Cotización`, `Expediente`, `RUC`, montos) deben llevar siempre la clase `.font-mono` para asegurar alineación visual vertical.
3. **Mantener libre de banners la vista de trabajo:** La contextualización de páginas debe residir en el Topbar para preservar el área útil de las tablas de datos.
4. **Prohibición de duplicar botones de guardado:** En formularios modales o vistas completas, colocar los botones de guardado/cancelado únicamente en la cabecera principal (`.form-header-bar`), eliminando recuadros redundantes de la barra lateral.
5. **Estandarización de Desplegables:** Todos los elementos `<select>` y `PrimeVue <Select>` deben emplear las clases globales `.form-select`, `.select-input`, `.filter-select` o `.form-select-styled`, asegurando `appearance: none !important` y `background-repeat: no-repeat !important` para erradicar la multiplicación de chevrons y mantener el chevron dorado en Modo Oscuro y tierra en Modo Claro.
6. **Prohibición de Atajo `background:` en Inputs/Selects:** Emplear siempre `background-color` explícito para no invalidar el `background-repeat: no-repeat` ni la posición del vector desplegable.
7. **Nivelación de Ancho en Recepción Comercial y Vistas Maestras:** Usar `width: 100%` en la vista contenedora y en formularios maestros, descartando `max-width` estrechos y nivelando el diseño al estándar panorámico de la aplicación.

