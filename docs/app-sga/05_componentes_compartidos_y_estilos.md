# 5. Componentes Compartidos, Composables y Sistema de Diseño

**Sistema de Gestión Aduanera - Transaduanas (`app-sga`)**  
*Guía de Tokens CSS "Warm Editorial", Modo Claro/Oscuro y Componentes `shared/`*

---

## Índice del Capítulo
1. [El Sistema de Diseño "Warm Editorial"](#1-el-sistema-de-diseño-warm-editorial)
   - [1.1 Paleta Cromática y Tokens de Color](#11-paleta-cromática-y-tokens-de-color)
   - [1.2 Tipografía Dual (Inter & JetBrains Mono)](#12-tipografía-dual-inter--jetbrains-mono)
   - [1.3 Rejilla de Espaciado Armónico de 4px](#13-rejilla-de-espaciado-armónico-de-4px)
2. [Alternancia Nativa de Modo Claro / Modo Oscuro](#2-alternancia-nativa-de-modo-claro--modo-oscuro)
3. [Biblioteca de Componentes Compartidos (`shared/components/`)](#3-biblioteca-de-componentes-compartidos-sharedcomponents)
   - [3.1 `AppSidebar.vue`: Barra Lateral Inteligente por Rol](#31-appsidebarvue-barra-lateral-inteligente-por-rol)
   - [3.2 `BaseModal.vue`: Cuadro de Diálogo Accesible](#32-basemodalvue-cuadro-de-diálogo-accesible)
   - [3.3 `BasePagination.vue`: Paginación Estándar de Tablas](#33-basepaginationvue-paginación-estándar-de-tablas)
   - [3.4 `HeroBannerCard.vue` & `MetricCard.vue`: Paneles Ejecutivos](#34-herobannercardvue--metriccardvue-paneles-ejecutivos)
   - [3.5 `DocumentosManagerModal.vue`: Gestor de Archivos Adjuntos](#35-documentosmanagermodalvue-gestor-de-archivos-adjuntos)
4. [Composables Reutilizables (`shared/composables/`)](#4-composables-reutilizables-sharedcomposables)
   - [4.1 `useNotification.js`: Sistema de Alertas Toast](#41-usenotificationjs-sistema-de-alertas-toast)
   - [4.2 `useConfirmation.js`: Modales de Confirmación](#42-useconfirmationjs-modales-de-confirmación)
   - [4.3 `usePagination.js`: Lógica Matemática de Paginación](#43-usepaginationjs-lógica-matemática-de-paginación)
   - [4.4 `download.js`: Descarga Segura con Revocación de Blobs](#44-downloadjs-descarga-segura-con-revocación-de-blobs)

---

## 1. El Sistema de Diseño "Warm Editorial"

Definido en [`app-sga/src/style.css`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-sga/src/style.css), el sistema visual busca erradicar la frialdad genérica de las aplicaciones corporativas mediante una identidad cálida y profesional.

### 1.1 Paleta Cromática y Tokens de Color
Los colores están nombrados semánticamente con variables CSS nativas:

| Familia de Tokens | Variables CSS Clave | Uso en la Aplicación |
| :--- | :--- | :--- |
| **Arena (*Sand*)** | `--color-sand-50` a `--color-sand-900` | Fondos principales, tarjetas de datos y bordes sutiles en modo claro. |
| **Latte / Lino** | `--color-latte-100` a `--color-latte-500` | Superficies secundarias, campos de entrada de formulario y paneles de filtros. |
| **Café Espresso** | `--color-espresso-700` a `--color-espresso-950` | Tipografía principal, títulos ejecutivos y fondos oscuros en modo noche. |
| **Semáforo SUNAT** | `--color-sunat-green`, `--color-sunat-orange`, `--color-sunat-red` | Insignias de canal aduanero oficial: Verde (Levante), Naranja (Documentario), Rojo (Físico). |
| **Estados del Sistema** | `--color-success`, `--color-warning`, `--color-danger`, `--color-info` | Alertas del sistema, validación de formularios y botones de acción crítica. |

### 1.2 Tipografía Dual (Inter & JetBrains Mono)
- **`var(--font-sans)` — Inter:** Tipografía humanista con pesos `300` a `800`. Diseñada para lectura continua sin fatiga visual en listados de cientos de filas.
- **`var(--font-mono)` — JetBrains Mono:** Tipografía monoespaciada con caracteres del mismo ancho exacto. Se utiliza para códigos de expedientes (`EXP-2026-0042`), números de DAM, RUCs de 11 dígitos y columnas de montos en dólares (`$ 12,450.00`), garantizando que los decimales y los números queden perfectamente alineados verticalmente.

### 1.3 Rejilla de Espaciado Armónico de 4px
No se usan márgenes improvisados. Todos los espacios derivan de una escala matemática de 4px:
- `--space-1`: 4px (micro-separaciones de iconos y badges)
- `--space-2`: 8px (padding interno de botones e inputs)
- `--space-3`: 12px (espacio entre campos de un formulario)
- `--space-4`: 16px (padding de tarjetas y celdas de tablas)
- `--space-6`: 24px (separación entre secciones principales)
- `--space-8`: 32px (márgenes exteriores de la pantalla)

---

## 2. Alternancia Nativa de Modo Claro / Modo Oscuro

El sistema no requiere recargar hojas de estilo adicionales para alternar entre día y noche:
- El composable [`useTheme.js`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-sga/src/shared/composables/useTheme.js) gestiona la preferencia del usuario y la guarda en `localStorage.getItem('sga_theme')`.
- Al cambiar de modo, inyecta el atributo `data-theme="dark"` o `data-theme="light"` en la etiqueta raíz `<html>`.
- Las variables CSS se reasignan instantáneamente: los fondos claros pasan a tonos carbón/espresso profundo y la tipografía pasa a tonos marfil cálido, reduciendo el consumo energético en pantallas OLED y protegiendo la vista de los operadores aduaneros nocturnos.

---

## 3. Biblioteca de Componentes Compartidos (`shared/components/`)

Todos los componentes reutilizables se ubican en [`app-sga/src/shared/components/`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-sga/src/shared/components/):

### 3.1 `AppSidebar.vue`: Barra Lateral Inteligente por Rol
- **Detecta el rol del usuario conectado:** Lee `authStore.userRole` y filtra dinámicamente la lista de módulos permitidos.
- **Resaltado de ruta activa:** Mantiene una barra de acento dorado en la opción actualmente abierta.
- **Modo Colapsable:** Permite contraer la barra lateral a modo de solo iconos para ganar espacio horizontal en pantallas de laptops o tablets de almacén.

### 3.2 `BaseModal.vue`: Cuadro de Diálogo Accesible
- Diálogo modal corporativo que implementa estándares de accesibilidad **WAI-ARIA**:
  - **Atrapamiento de foco (*Focus Trap*):** Evita que la tecla `Tab` navegue a elementos que están detrás del modal.
  - **Cierre por Teclado:** Al presionar la tecla `ESC`, emite el evento `close`.
  - **Bloqueo de Scroll:** Evita que el fondo de la página se desplace mientras el modal está abierto.

### 3.3 `BasePagination.vue`: Paginación Estándar de Tablas
- Componente de barra inferior para tablas de datos:
  - Selector de filas por página (10, 25, 50, 100).
  - Indicador de rango: *"Mostrando 1 a 10 de 145 registros"*.
  - Botones accesibles de "Primera", "Anterior", números de página interactivos y "Siguiente".

### 3.4 `HeroBannerCard.vue` & `MetricCard.vue`: Paneles Ejecutivos
- **`HeroBannerCard`:** Banner superior estilizado con mensaje de bienvenida personalizado por nombre, fecha y acciones destacadas.
- **`MetricCard`:** Tarjeta individual de indicador (KPI) con icono temático, valor numérico formateado, título y comparador de tendencia.

### 3.5 `DocumentosManagerModal.vue`: Gestor de Archivos Adjuntos
- Modal integral para ver, subir y descargar archivos adjuntos vinculados a una cotización, expediente aduanero o liquidación.

---

## 4. Composables Reutilizables (`shared/composables/`)

### 4.1 `useNotification.js`: Sistema de Alertas Toast
Permite disparar mensajes flotantes de retroalimentación desde cualquier componente con una sola línea de código:
```javascript
const { showSuccess, showError, showWarning, showInfo } = useNotification()

showSuccess('Operación Exitosa', 'El expediente EXP-2026-0012 fue aprobado correctamente.')
showError('Compuerta Bloqueada', 'No se puede crear el expediente sin pago validado.')
```

### 4.2 `useConfirmation.js`: Modales de Confirmación
Muestra un diálogo de seguridad antes de ejecutar acciones irreversibles de negocio (como anular una cotización, cambiar un canal aduanero o dar de baja a un chofer):
```javascript
const { confirmAction } = useConfirmation()

confirmAction({
  title: '¿Confirmar Salida de Carga?',
  message: 'Esta acción validará el ticket VGM y autorizará el retiro hacia transporte.',
  onConfirm: async () => { ... }
})
```

### 4.3 `usePagination.js`: Lógica Matemática de Paginación
Encapsula el cálculo de índices para tablas:
- `currentPage`, `itemsPerPage`, `totalPages`.
- `paginatedData`: Retorna el segmento de arreglo cortado con `.slice(start, end)`.

### 4.4 `download.js`: Descarga Segura con Revocación de Blobs
> [!TIP]
> **Prevención de Fuga de Memoria:**  
> Cuando se descarga un archivo PDF o imagen generado en el navegador, la función crea una URL temporal con `URL.createObjectURL(blob)`. La utilidad [`download.js`](file:///home/jiodev/Documents/Proyects/TransAduanas/app-sga/src/shared/utils/download.js) revoca automáticamente la URL mediante `URL.revokeObjectURL(...)` tras la descarga, asegurando que el navegador no acumule memoria residual.
