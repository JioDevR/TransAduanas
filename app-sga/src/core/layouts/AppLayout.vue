<template>
  <div class="sga-shell">
    <div
      v-if="mobileMenuOpen"
      class="sga-drawer-backdrop"
      @click="mobileMenuOpen = false"
    />

    <AppSidebar
      :mobile-open="mobileMenuOpen"
      @close-mobile="mobileMenuOpen = false"
    />

    <div class="sga-main">
      <!-- Topbar Minimalista en una sola línea -->
      <header class="sga-topbar">
        <div class="sga-topbar-left">
          <button
            type="button"
            class="sga-mobile-menu-btn"
            aria-label="Abrir menú de navegación"
            :aria-expanded="mobileMenuOpen"
            @click="mobileMenuOpen = true"
          >
            <i class="pi pi-bars"></i>
          </button>

          <h1 class="sga-topbar-title">{{ currentTitle }}</h1>
        </div>

        <div class="sga-topbar-actions">
          <div
            v-if="authStore.user?.codigoSede"
            class="sga-sede-pill"
            :title="`Sede Asignada: ${authStore.user.codigoSede} — ${authStore.user.nombreSede || 'Principal'}`"
          >
            <span class="sga-sede-badge">SEDE</span>
            <span class="sga-sede-text">
              {{ authStore.user.codigoSede }} — {{ authStore.user.nombreSede || 'Sede Central Callao' }}
            </span>
          </div>

          <button
            type="button"
            class="sga-user-trigger"
            @click="toggleUserPopover"
            aria-label="Opciones de cuenta"
          >
            <span class="sga-user-avatar">{{ avatarInitials }}</span>
            <span class="sga-user-meta">
              <span class="sga-user-name">{{ authStore.userFullName || authStore.user?.username }}</span>
              <span class="sga-user-role">{{ displayUserRole }}</span>
            </span>
            <i class="pi pi-chevron-down sga-user-chevron"></i>
          </button>

          <Popover ref="userPopoverRef">
            <div class="sga-popover-body">
              <div class="sga-popover-header">
                <span class="sga-user-avatar">{{ avatarInitials }}</span>
                <div class="sga-user-meta">
                  <span class="sga-user-name">{{ authStore.userFullName || authStore.user?.username }}</span>
                  <span class="sga-user-role">{{ displayUserRole }}</span>
                </div>
              </div>

              <div class="sga-popover-details">
                <div v-if="authStore.user?.codigoSede" class="sga-popover-row">
                  <span>SEDE</span>
                  <strong>{{ authStore.user.codigoSede }} — {{ authStore.user.nombreSede || 'Principal' }}</strong>
                </div>
                <div v-if="authStore.user?.username" class="sga-popover-row">
                  <span>USUARIO</span>
                  <strong>{{ authStore.user.username }}</strong>
                </div>
              </div>

              <div class="sga-popover-theme">
                <span class="sga-popover-theme-label">
                  <i :class="['pi', isDark ? 'pi-moon' : 'pi-sun']"></i>
                  <span>{{ isDark ? 'Modo Oscuro' : 'Modo Claro' }}</span>
                </span>
                <ToggleSwitch :modelValue="isDark" @update:modelValue="toggleTheme" />
              </div>

              <Button
                label="Cerrar Sesión"
                icon="pi pi-sign-out"
                severity="danger"
                variant="outlined"
                size="small"
                fluid
                @click="handleLogout"
              />
            </div>
          </Popover>
        </div>
      </header>

      <!-- Vista Activa -->
      <main class="sga-page-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Button from 'primevue/button'
import Popover from 'primevue/popover'
import ToggleSwitch from 'primevue/toggleswitch'
import AppSidebar from '@/shared/components/AppSidebar.vue'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useTheme } from '@/shared/composables/useTheme'
import { useResponsive } from '@/shared/composables/useResponsive'
import { ROLE_LABELS } from '@/core/auth/roles'
import '../../../../sgaUI/sga-shell.css'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const navigationStore = useNavigationStore()
const { isDark, toggleTheme } = useTheme()
const { isTabletOrBelow } = useResponsive()

const mobileMenuOpen = ref(false)
const userPopoverRef = ref(null)

watch(isTabletOrBelow, (tabletDown) => {
  if (!tabletDown) mobileMenuOpen.value = false
})

watch(
  () => route.fullPath,
  () => {
    mobileMenuOpen.value = false
  }
)

const toggleUserPopover = (event) => {
  userPopoverRef.value?.toggle(event)
}

const handleLogout = () => {
  userPopoverRef.value?.hide()
  authStore.logout()
  router.replace('/login')
}

const displayUserRole = computed(() => {
  return ROLE_LABELS[authStore.userRole] || authStore.userRole || 'Operador SGA'
})

const MODULE_TITLES = {
  'administracion-ti': {
    default: 'Administración TI',
    sub: {
      dashboard: 'Centro de Control y Monitoreo del Sistema TI',
      users: 'Usuarios Internos',
      'create-user': 'Ficha de Registro de Operador',
      'portal-users': 'Cuentas Corporativas B2B',
      'create-portal-user': 'Gestión de Usuario Portal B2B',
      auditoria: 'Registro de Eventos y Seguridad',
      almacenamiento: 'Almacenamiento y Repositorio Documental',
      'roles-accesos': 'Almacenamiento y Repositorio Documental',
      about: 'Ficha Técnica del Sistema SGA'
    },
    getSub: () => navigationStore.administracionTiSubOption
  },
  'gerencia-general': {
    default: 'Gerencia General',
    sub: {
      dashboard: 'Dashboard Ejecutivo y Métricas Generales',
      overview: 'Dashboard Ejecutivo y Métricas Generales',
      sedes: 'Gestión Integral de Sedes y Agencias Territoriales',
      puertos: 'Puertos y Terminales Aduaneros',
      'tipos-carga': 'Catálogo de Tipos de Carga',
      mercancias: 'Mercancías y Subpartidas Arancelarias',
      catalogos: 'Puertos y Terminales Aduaneros'
    },
    getSub: () => navigationStore.gerenciaGeneralSubOption
  },
  comercial: {
    default: 'Recepción Comercial',
    sub: {
      dashboard: 'Dashboard',
      cotizaciones: 'Registro de Cotizaciones Comerciales',
      solicitudes: 'Solicitudes Sin Asignar (Portal B2B)',
      clientes: 'Directorio de Clientes & Prospectos'
    },
    getSub: () => navigationStore.comercialSubOption
  },
  'gestion-aduanera': {
    default: 'Gestión Aduanera',
    sub: {
      dashboard: 'Dashboard y Control Operativo Aduanero',
      cotizaciones: 'Bandeja de Cotizaciones para Revisión Técnica',
      apertura: 'Apertura de Expedientes Aduaneros',
      expedientes: 'Gestión Integral de Expedientes Aduaneros'
    },
    getSub: () => navigationStore.gestionAduaneraSubOption
  },
  'agente-aduanero': {
    default: 'Agente Aduanero',
    sub: {
      dashboard: 'Dashboard Operativo del Agente Aduanero',
      todos: 'Expedientes Aduaneros Asignados',
      transito: 'Control de Tránsito Internacional y Arribo',
      inspeccion: 'Aforo y Diligencias ante SUNAT',
      levante: 'Levante Autorizado y Despacho Final'
    },
    getSub: () => navigationStore.agenteAduaneroSubOption
  },
  'transporte-flota': {
    default: 'Transporte y Flota',
    sub: {
      dashboard: 'Dashboard Operativo de Transporte & Flota',
      despachos: 'Monitoreo Global de Despachos',
      unidades: 'Parque Automotor & Flota de Transporte',
      conductores: 'Directorio de Conductores Acreditados MTC'
    },
    getSub: () => navigationStore.transporteFlotaSubOption
  },
  finanzas: {
    default: 'Finanzas',
    sub: {
      dashboard: 'Dashboard Financiero & Tesorería',
      'abonos-b2b': 'Revisión y Validación de Abonos B2B',
      'pre-expedientes': 'Pagos Pre-Expedientes (Cotizaciones)',
      anticipos: 'Pagos Pre-Expedientes (Cotizaciones)',
      liquidaciones: 'Liquidación Aduanera & Control de Cuentas',
      'por-liquidar': 'Expedientes Pendientes de Liquidación',
      conciliacion: 'Facturación & Conciliación Bancaria',
      facturacion: 'Facturación & Conciliación Bancaria'
    },
    getSub: () => navigationStore.finanzasSubOption
  }
}

const currentTitle = computed(() => {
  const cfg = MODULE_TITLES[navigationStore.currentModule]
  if (cfg) {
    const subKey = cfg.getSub?.()
    return (subKey && cfg.sub?.[subKey]) || cfg.default
  }
  return route.meta?.title?.replace(' - SGA', '') || 'Operaciones'
})

const avatarInitials = computed(() => {
  if (!authStore.user) return 'CM'
  const n = authStore.user.nombres?.charAt(0) || ''
  const a = authStore.user.apellidos?.charAt(0) || ''
  return (n + a).toUpperCase() || authStore.user.username?.substring(0, 2).toUpperCase() || 'AD'
})
</script>
