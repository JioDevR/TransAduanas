<template>
  <aside :class="['sga-sidebar', { collapsed: isEffectiveCollapsed, 'is-mobile-open': mobileOpen }]">
    <!-- Cabecera de Marca -->
    <div class="sga-sidebar-brand">
      <div class="sga-brand-badge">SGA</div>
      <div v-if="!isEffectiveCollapsed" class="sga-brand-info">
        <span class="sga-brand-name">TransAduanas</span>
        <span class="sga-brand-tag">SGA</span>
      </div>
      <button
        type="button"
        class="sga-collapse-btn"
        @click="toggleCollapse"
        :title="isCollapsed ? 'Expandir barra lateral' : 'Colapsar barra lateral'"
        :aria-label="isCollapsed ? 'Expandir barra lateral' : 'Colapsar barra lateral'"
      >
        <i :class="isCollapsed ? 'pi pi-chevron-right' : 'pi pi-chevron-left'"></i>
      </button>
      <button
        type="button"
        class="sga-drawer-close-btn"
        @click="$emit('close-mobile')"
        aria-label="Cerrar menú lateral"
      >
        <i class="pi pi-times"></i>
      </button>
    </div>

    <!-- Navegación Dinámica según Módulo Activo -->
    <nav class="sga-sidebar-nav">
      <ul class="sga-nav-list">
        <li v-for="item in currentNavItems" :key="item.key">
          <button
            type="button"
            :class="['sga-nav-btn', { active: isItemActive(item) }]"
            @click="handleNavItemClick(item)"
            :title="isEffectiveCollapsed ? item.label : ''"
          >
            <span class="sga-nav-dot"></span>
            <span v-if="!isEffectiveCollapsed" class="sga-nav-label">{{ item.label }}</span>
          </button>
        </li>
      </ul>
    </nav>

    <!-- Gestor de Módulos (al pie del Sidebar) -->
    <div v-if="!isEffectiveCollapsed" class="sga-module-card">
      <div class="sga-module-kicker">
        <span class="sga-pulse-dot"></span>
        <span>{{ authStore.canSwitchModules ? 'GESTOR DE MÓDULOS' : 'MÓDULO ASIGNADO' }}</span>
      </div>

      <Select
        v-if="authStore.canSwitchModules"
        v-model="navigationStore.activeModule"
        :options="MODULE_OPTIONS"
        optionLabel="label"
        optionValue="value"
        fluid
        @change="handleModuleSelectChange"
      />

      <div v-else class="sga-static-module-pill">
        <span class="sga-pill-num">{{ moduleNumberCode }}</span>
        <span>{{ activeModuleShortTitle }}</span>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Select from 'primevue/select'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useNavigationStore } from '@/core/stores/navigationStore'

const props = defineProps({
  mobileOpen: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close-mobile'])

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const navigationStore = useNavigationStore()

const isCollapsed = ref(false)
const isEffectiveCollapsed = computed(() => isCollapsed.value && !props.mobileOpen)
const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const handleNavItemClick = (item) => {
  item.onClick()
  if (props.mobileOpen) {
    emit('close-mobile')
  }
}

const MODULE_OPTIONS = [
  { value: 'administracion-ti', label: '00 Administración TI' },
  { value: 'gerencia-general', label: '01 Gerencia General' },
  { value: 'comercial', label: '02 Recepción Comercial' },
  { value: 'gestion-aduanera', label: '03 Gestión Aduanera' },
  { value: 'agente-aduanero', label: '04 Agente Aduanero' },
  { value: 'transporte-flota', label: '05 Transporte y Flota' },
  { value: 'finanzas', label: '06 Finanzas' }
]

const MODULE_META = {
  'administracion-ti': { code: '00', title: 'ADMINISTRACIÓN TI' },
  'gerencia-general': { code: '01', title: 'GERENCIA GENERAL' },
  comercial: { code: '02', title: 'RECEPCIÓN COMERCIAL' },
  'gestion-aduanera': { code: '03', title: 'GESTIÓN ADUANERA' },
  'agente-aduanero': { code: '04', title: 'AGENTE ADUANERO' },
  'transporte-flota': { code: '05', title: 'TRANSPORTE Y FLOTA' },
  finanzas: { code: '06', title: 'FINANZAS' }
}

const activeModuleShortTitle = computed(() => MODULE_META[navigationStore.currentModule]?.title || 'OPERACIONES')
const moduleNumberCode = computed(() => MODULE_META[navigationStore.currentModule]?.code || '00')

const pushIfChanged = (target) => {
  if (typeof target === 'string') {
    if (route.path !== target) router.push(target)
    return
  }
  const samePath = route.path === target.path
  const sameTab = (route.query?.tab || undefined) === (target.query?.tab || undefined)
  if (!samePath || !sameTab) router.push(target)
}

const handleModuleSelectChange = () => {
  const firstItem = currentNavItems.value[0]
  if (firstItem) {
    firstItem.onClick()
  }
}

const selectSub = (moduleKey, setter, subOption, pathOrResolver) => {
  navigationStore.setActiveModule(moduleKey)
  setter(subOption)
  const target = typeof pathOrResolver === 'function' ? pathOrResolver(subOption) : pathOrResolver
  pushIfChanged(target)
}

const handleFinanzasAction = (action) => {
  navigationStore.setActiveModule('finanzas')
  if (route.path !== '/finanzas') {
    router.push({ path: '/finanzas', query: { tab: 'pre-expedientes', action } })
  } else {
    router.replace({ query: { ...route.query, tab: 'pre-expedientes', action } })
  }
  navigationStore.triggerFinanzasAction(action)
}

const currentNavItems = computed(() => {
  const mod = navigationStore.currentModule

  if (mod === 'administracion-ti') {
    const go = (sub, path) => () => selectSub('administracion-ti', navigationStore.setAdministracionTiSubOption, sub, path)
    return [
      { key: 'dashboard', label: 'Dashboard TI', match: ['dashboard'], onClick: go('dashboard', '/administracion-ti/dashboard') },
      { key: 'users', label: 'Usuarios', match: ['users', 'create-user'], onClick: go('users', '/administracion-ti/usuarios') },
      { key: 'portal-users', label: 'Usuarios B2B', match: ['portal-users', 'create-portal-user'], onClick: go('portal-users', '/administracion-ti/usuarios-portal') },
      { key: 'auditoria', label: 'Registro TI', match: ['auditoria'], onClick: go('auditoria', '/administracion-ti/auditoria') },
      { key: 'almacenamiento', label: 'Almacenamiento', match: ['almacenamiento', 'roles-accesos'], onClick: go('almacenamiento', '/administracion-ti/almacenamiento') },
      { key: 'about', label: 'Acerca de', match: ['about'], onClick: go('about', '/administracion-ti/acerca-de') }
    ]
  }

  if (mod === 'gerencia-general') {
    const go = (sub, path) => () => selectSub('gerencia-general', navigationStore.setGerenciaGeneralSubOption, sub, path)
    return [
      { key: 'dashboard', label: 'Dashboard Ejecutivo', match: ['dashboard', 'overview'], onClick: go('dashboard', '/gerencia-general/dashboard') },
      { key: 'sedes', label: 'Sedes', match: ['sedes'], onClick: go('sedes', '/gerencia-general/sedes') },
      { key: 'puertos', label: 'Puertos & Terminales', match: ['puertos', 'catalogos'], onClick: go('puertos', '/gerencia-general/puertos') },
      { key: 'tipos-carga', label: 'Tipos de Carga', match: ['tipos-carga'], onClick: go('tipos-carga', '/gerencia-general/tipos-carga') },
      { key: 'mercancias', label: 'Mercancías & Partidas', match: ['mercancias'], onClick: go('mercancias', '/gerencia-general/mercancias') }
    ]
  }

  if (mod === 'comercial') {
    const go = (sub) => () => selectSub('comercial', navigationStore.setComercialSubOption, sub, { path: '/recepcion-comercial', query: { tab: sub } })
    return [
      { key: 'dashboard', label: 'Dashboard Comercial', match: ['dashboard'], onClick: go('dashboard') },
      { key: 'cotizaciones', label: authStore.userRole === 'AGENTE_COMERCIAL' ? 'Mis Cotizaciones' : 'Cotizaciones', match: ['cotizaciones', 'ordenes'], onClick: go('cotizaciones') },
      { key: 'solicitudes', label: 'Solicitudes Sin Asignar', match: ['solicitudes'], onClick: go('solicitudes') },
      { key: 'clientes', label: 'Clientes & Prospectos', match: ['clientes'], onClick: go('clientes') }
    ]
  }

  if (mod === 'gestion-aduanera') {
    const go = (sub) => () =>
      selectSub('gestion-aduanera', navigationStore.setGestionAduaneraSubOption, sub, {
        path: '/gestion-aduanera',
        query: { tab: sub }
      })
    return [
      { key: 'dashboard', label: 'Dashboard Operativo', match: ['dashboard'], onClick: go('dashboard') },
      { key: 'cotizaciones', label: 'Bandeja Cotizaciones', match: ['cotizaciones'], onClick: go('cotizaciones') },
      { key: 'apertura', label: 'Apertura Expedientes', match: ['apertura'], onClick: go('apertura') },
      { key: 'expedientes', label: 'Gestión de Expedientes', match: ['expedientes'], onClick: go('expedientes') }
    ]
  }

  if (mod === 'agente-aduanero') {
    const go = (sub) => () =>
      selectSub('agente-aduanero', navigationStore.setAgenteAduaneroSubOption, sub, {
        path: '/agente-aduanero',
        query: sub === 'dashboard' ? {} : { tab: sub }
      })
    return [
      { key: 'dashboard', label: 'Dashboard Operativo', match: ['dashboard'], onClick: go('dashboard') },
      { key: 'todos', label: 'Expedientes', match: ['todos'], onClick: go('todos') },
      { key: 'transito', label: 'En Tránsito / Arribo', match: ['transito'], onClick: go('transito') },
      { key: 'inspeccion', label: 'Aforo & Diligencia SUNAT', match: ['inspeccion'], onClick: go('inspeccion') },
      { key: 'levante', label: 'Con Levante & Despacho', match: ['levante'], onClick: go('levante') }
    ]
  }

  if (mod === 'transporte-flota') {
    const go = (sub) => () => selectSub('transporte-flota', navigationStore.setTransporteFlotaSubOption, sub, { path: '/transporte-flota', query: { tab: sub } })
    return [
      { key: 'dashboard', label: 'Dashboard Operativo', match: ['dashboard'], onClick: go('dashboard') },
      { key: 'despachos', label: 'Monitoreo & Despachos', match: ['despachos'], onClick: go('despachos') },
      { key: 'unidades', label: 'Parque de Flota', match: ['unidades'], onClick: go('unidades') },
      { key: 'conductores', label: 'Directorio Conductores', match: ['conductores'], onClick: go('conductores') }
    ]
  }

  if (mod === 'finanzas') {
    const go = (sub) => () => selectSub('finanzas', navigationStore.setFinanzasSubOption, sub, { path: '/finanzas', query: { tab: sub } })
    return [
      { key: 'dashboard', label: 'Dashboard Financiero', match: ['dashboard'], onClick: go('dashboard') },
      { key: 'abonos-b2b', label: 'Revisión de Abonos B2B', match: ['abonos-b2b'], onClick: go('abonos-b2b') },
      { key: 'pre-expedientes', label: 'Pagos Pre-Expedientes', match: ['pre-expedientes'], onClick: go('pre-expedientes') },
      { key: 'liquidaciones', label: 'Liquidación de Cuentas', match: ['liquidaciones'], onClick: go('liquidaciones') },
      { key: 'conciliacion', label: 'Facturación & Conciliación', match: ['conciliacion'], onClick: go('conciliacion') },
      { key: 'registrar-anticipo', label: '+ Registrar Abono', match: [], onClick: () => handleFinanzasAction('registrar-anticipo') }
    ]
  }

  return []
})

const activeSubOptionByModule = computed(() => {
  const map = {
    'administracion-ti': navigationStore.administracionTiSubOption,
    'gerencia-general': navigationStore.gerenciaGeneralSubOption,
    comercial: navigationStore.comercialSubOption,
    'gestion-aduanera': navigationStore.gestionAduaneraSubOption,
    'agente-aduanero': navigationStore.agenteAduaneroSubOption,
    'transporte-flota': navigationStore.transporteFlotaSubOption,
    finanzas: navigationStore.finanzasSubOption
  }
  return map[navigationStore.currentModule] || ''
})

const isItemActive = (item) => {
  if (item.key === 'registrar-anticipo') {
    return navigationStore.finanzasActionTrigger === 'registrar-anticipo'
  }
  return item.match.includes(activeSubOptionByModule.value)
}
</script>
