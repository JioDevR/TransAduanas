# 1. Introducción y Arquitectura de `app-portal-cliente`

**Sistema de Gestión Aduanera - Transaduanas (`app-portal-cliente`)**  
*Portal de Autogestión y Extranet B2B para Clientes Importadores y Exportadores*

---

## Índice del Capítulo
1. [¿Qué es el Portal B2B y qué valor aporta? (Explicación para no técnicos)](#1-qué-es-el-portal-b2b-y-qué-valor-aporta-explicación-para-no-técnicos)
2. [Arquitectura de Software y Tecnologías Centrales](#2-arquitectura-de-software-y-tecnologías-centrales)
3. [Estructura del Proyecto por Dominios de Cliente](#3-estructura-del-proyecto-por-dominios-de-cliente)
4. [Filosofía Visual: Identidad Corporativa y Modo Oscuro](#4-filosofía-visual-identidad-corporativa-y-modo-oscuro)
5. [Árbol Físico de Directorios de `app-portal-cliente/src/`](#5-árbol-físico-de-directorios-de-app-portal-clientesrc)

---

## 1. ¿Qué es el Portal B2B y qué valor aporta? (Explicación para no técnicos)

### La Analogía de la Banca por Internet Corporativa
Antes de que existieran las aplicaciones bancarias, si una empresa quería saber si un cheque había sido cobrado o necesitaba una constancia de transferencia, tenía que llamar por teléfono al banco, esperar que el ejecutivo estuviera desocupado o ir personalmente a una ventanilla. Hoy en día, el cliente entra a su banca electrónica con su número de empresa y consulta saldos, descarga facturas y hace transferencias a cualquier hora del día.

En el sector aduanero tradicional ocurre exactamente lo mismo: los importadores llaman constantemente a la agencia para preguntar:
- *¿Ya salió mi contenedor de China?*
- *¿Qué canal de SUNAT me tocó: Verde, Naranja o Rojo?*
- *¿A qué cuenta bancaria deposito el adelanto?*
- *¿Me pueden reenviar la copia de la Factura y el BL?*

**`app-portal-cliente`** es la extranet de autogestión B2B que erradica todas estas llamadas y correos:
- Las empresas importadoras y exportadoras inician sesión con su **número de RUC (11 dígitos)**.
- Ven en una línea de tiempo gráfica el avance de sus contenedores minuto a minuto.
- Conocen de inmediato el canal de la aduana (Verde = levante inmediato, Naranja = revisión documental, Rojo = aforo físico).
- Descargan sus facturas, packing lists y conocimientos de embarque (BL) con un solo clic.
- Consultan sus cuotas pendientes y suben sus comprobantes de pago bancarios directamente al sistema para que el contador los valide.

---

## 2. Arquitectura de Software y Tecnologías Centrales

El portal de clientes es una aplicación web desacoplada e independiente del sistema interno:

```
[ Cliente Importador / RUC ] ──▶ [ app-portal-cliente ] (Vue 3 / Vite 6 en puerto 5174)
                                          │
                                          │ Proxy inverso (/api -> :8085)
                                          ▼
                               [ sga-backend API ] (Spring Boot / Java 21)
                                          │
                                          ▼
                               [ Base de Datos MySQL 8.4 ]
```

- **Framework:** **Vue 3** con la Composition API y sintaxis reactiva moderna `<script setup>`.
- **Servidor de Desarrollo:** **Vite 6** ejecutándose en el puerto **`5174`** con proxy inverso automático hacia el backend en el puerto `8085`.
- **Enrutamiento:** **Vue Router 4** con guardias de navegación para usuarios con cambio de contraseña obligatorio.
- **Gestión de Estado:** **Pinia 3** para el almacenamiento de credenciales y datos de la empresa cliente en `authStore.js`.
- **Componentes:** **PrimeVue 4** adaptado a la paleta corporativa *Warm Editorial*.

---

## 3. Estructura del Proyecto por Dominios de Cliente

El código de `app-portal-cliente` está organizado por las necesidades clave de un cliente corporativo:

```
app-portal-cliente/src/
├── core/                                  # Infraestructura compartida
│   ├── api/apiClient.js                   # Instancia Axios con inyección de JWT y logout en 401
│   ├── layouts/PortalLayout.vue           # Shell contenedor (Barra superior con perfil RUC y navegación)
│   ├── router/index.js                    # Enrutador con guardia de cambio de contraseña obligatorio
│   └── stores/authStore.js                # Estado de sesión de la empresa cliente (RUC y Token)
│
├── features/                              # Dominios de negocio del cliente
│   ├── auth/                              # Login B2B y Cambio Obligatorio de Contraseña Temporal
│   ├── cotizaciones/                      # Consulta de proformas y presupuestos en USD
│   ├── expedientes/                       # Línea de tiempo y Tracking de contenedores y canal DAM
│   ├── pagos/                             # Consulta de estado de cuenta, deuda heredada y subida de vouchers
│   └── notificaciones/                    # Bandeja in-app de avisos aduaneros
│
└── shared/                                # Utilidades y componentes reutilizables
    ├── components/BaseModal.vue           # Diálogos modales accesibles
    ├── components/BasePagination.vue      # Paginación estándar de tablas
    ├── composables/useTheme.js            # Alternancia nativa Modo Claro / Modo Oscuro
    ├── composables/useNotification.js     # Alertas flotantes Toast
    └── utils/download.js                  # Descarga segura de archivos con revocación de Blob URLs
```

---

## 4. Filosofía Visual: Identidad Corporativa y Modo Oscuro

El portal comparte el lenguaje de diseño **Warm Editorial** de Transaduanas, transmitiendo seriedad, calidez y transparencia comercial:
- **Montos Claros en Dólares (`$ USD`):** Toda la información financiera y de cotizaciones se expresa en moneda internacional con tipografía monoespaciada `JetBrains Mono`.
- **Semáforo SUNAT Intuitivo:** Insignias de colores normalizadas para los canales aduaneros:
  - 🟢 **Verde:** Levante autorizado, mercancía en camino a despacho.
  - 🟠 **Naranja:** Sujeto a revisión documentaria por la aduana.
  - 🔴 **Rojo:** Sujeto a reconocimiento físico en depósito temporal.
- **Modo Oscuro Integrado:** El cliente puede alternar en cualquier momento entre modo claro y modo oscuro desde la barra superior, guardando su preferencia en el navegador.

---

## 5. Árbol Físico de Directorios de `app-portal-cliente/src/`

```
app-portal-cliente/
├── index.html                             # Entrada HTML con fuentes Inter y JetBrains Mono
├── package.json                           # Manifiesto de dependencias (Vue 3.5, Vite 6, PrimeVue 4)
├── vite.config.js                         # Configuración de puerto 5174 y proxy hacia :8085
└── src/
    ├── App.vue                            # Contenedor raíz con proveedor de Toasts
    ├── main.js                            # Inicialización de Vue 3, Pinia y PrimeVue
    ├── style.css                          # Tokens de diseño y variables CSS Warm Editorial
    ├── core/                              # Layout, Router, API Client y AuthStore
    ├── features/                          # Vistas y servicios de Cotizaciones, Expedientes, Pagos
    └── shared/                            # Componentes y utilidades compartidas
```
