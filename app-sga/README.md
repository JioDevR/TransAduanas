# SGA Frontend - Sistema de Gestión Aduanera

Frontend modular desarrollado en **Vue 3**, **Vite**, **PrimeVue** (tema Aura), **Pinia**, **Vue Router 4** y **Axios**, implementando una **Arquitectura Modular basada en Características (Feature-Based Architecture)** y conectado a la API REST de **SGA Backend** (`http://localhost:8080/api/v1`).

---

## 1. Arquitectura Modular basada en Características

El proyecto organiza el código en dominios de negocio desacoplados y cohesivos:

```text
SGA/frontend/
├── index.html
├── vite.config.js
├── src/
│   ├── main.js                        # Inicialización de Vue, PrimeVue, Pinia y Router
│   ├── App.vue                         # Punto de anclaje de router-view
│   ├── style.css                       # Tokens de diseño y estilos globales minimalistas
│   │
│   ├── core/                           # Transversal y configuración central
│   │   ├── api/
│   │   │   └── apiClient.js            # Axios configurado con interceptores JWT Bearer y 401
│   │   ├── layouts/
│   │   │   └── AppLayout.vue           # Layout general: Sidebar vertical y Topbar
│   │   └── router/
│   │       └── index.js                # Enrutador central con guardias de Auth y Roles
│   │
│   ├── features/                       # MÓDULOS POR CARACTERÍSTICA (Features)
│   │   ├── auth/                       # Feature: Autenticación
│   │   │   ├── views/LoginView.vue     # Vista minimalista de login
│   │   │   ├── services/authService.js # Peticiones a /api/v1/auth
│   │   │   ├── stores/authStore.js     # Estado Pinia de sesión y permisos
│   │   │   └── routes/auth.routes.js   # Rutas de login
│   │   │
│   │   ├── admin/                      # Feature: Administración
│   │   │   └── users/                  # Sub-feature: Gestión de Usuarios
│   │   │       ├── views/UserManagementView.vue   # Vista principal del Admin
│   │   │       ├── components/
│   │   │       │   ├── UserTable.vue              # Tabla con búsqueda, filtros y acciones
│   │   │       │   ├── UserFormDialog.vue         # Modal para crear, editar y cambiar rol
│   │   │       │   └── UserRoleBadge.vue          # Badges estilizados por rol
│   │   │       ├── services/userService.js        # Consumo de /api/v1/usuarios, /roles, /sedes
│   │   │       └── routes/admin.routes.js         # Rutas protegidas para rol ADMINISTRADOR_TI
│   │   │
│   │   └── roles-modules/              # Vistas operativas por rol institucional
│   │       ├── views/
│   │       │   ├── RecepcionComercialView.vue     # Rol: AGENTE_COMERCIAL (/recepcion-comercial)
│   │       │   ├── GestionAduaneraView.vue        # Rol: SUPERVISOR_ADUANERO (/gestion-aduanera)
│   │       │   ├── LiquidacionAduaneraView.vue    # Rol: SUPERVISOR_ADUANERO (/gestion-aduanera/liquidacion)
│   │       │   ├── AgenteAduaneroView.vue         # Roles: AGENTE_ADUANERO, SUPERVISOR_ADUANERO (/agente-aduanero)
│   │       │   ├── TransporteFlotaView.vue        # Rol: SUPERVISOR_TRANSPORTE (/transporte-flota)
│   │       │   ├── FinanzasView.vue               # Rol: CONTADOR (/finanzas)
│   │       │   └── LegalAuditoriaView.vue         # Rol: ASESOR_LEGAL (/legal-auditoria)
│   │       └── routes/roles.routes.js             # Rutas por rol
│   │
│   └── shared/                         # Componentes reutilizables
│       ├── components/
│       │   ├── AppSidebar.vue          # Barra vertical de navegación a la izquierda
│       │   └── ModulePlaceholder.vue   # Vista limpia en blanco informativa por rol
```

---

## 2. Características Implementadas

1. **Login de Usuario y Contraseña**:
   - Tarjeta minimalista con inputs y botón de inicio de sesión con estado de carga.
   - Conexión con `/api/v1/auth/login`.
   - Credenciales rápidas preconfiguradas para pruebas (`admin` / `Password123!`).
   - Almacenamiento seguro del token JWT y redirección automática según rol.

2. **Barra Vertical a la Izquierda (Sidebar)**:
   - Diseño limpio y minimalista.
   - Identificador del sistema (SGA) y badge del rol activo del usuario conectado.
   - Selector modular y sub-opciones para los 8 roles institucionales (`ADMINISTRADOR_TI`, `GERENTE_GENERAL`, `AGENTE_COMERCIAL`, `SUPERVISOR_ADUANERO`, `AGENTE_ADUANERO`, `SUPERVISOR_TRANSPORTE`, `CONTADOR`, `ASESOR_LEGAL`).
   - Botón de colapso rápido para optimizar espacio en pantalla.
   - Perfil de usuario en la base y botón para cerrar sesión (`Logout`).

3. **Panel del Administrador - Gestor de Usuarios**:
   - Resumen con tarjetas de métricas en tiempo real (Usuarios totales, Activos, Desactivados, Roles).
   - **Listado y Filtros**: Búsqueda interactiva por usuario, nombre, correo o cargo, filtro por rol y filtro por estado activo/desactivado.
   - **Crear Usuario**: Diálogo modal para registrar nuevos operadores asignando rol y sede.
   - **Editar y Cambiar Rol**: Permite actualizar nombres, contacto y reasignar el rol o sede en caliente.
   - **Desactivación / Reactivación Lógica**: Botones de baja lógica (`DELETE /api/v1/usuarios/{id}`) y reactivación (`PATCH /api/v1/usuarios/{id}/reactivar`).
   - **Exportación PDF**: Botón directo para descargar el reporte oficial generado por JasperReports en el backend.

4. **Módulos Operativos por Rol**:
   - Interfaces operativas completas para los 8 roles institucionales con diseño editorial y responsivo.

---

## 3. Instrucciones de Ejecución

### Requisitos
- Backend Spring Boot de SGA ejecutándose en el puerto `8085` (`./mvnw spring-boot:run` dentro de `SGA/backend`).

### Iniciar el Frontend
```bash
cd SGA/frontend
npm run dev
```

La aplicación estará disponible en:
`http://localhost:5173`
