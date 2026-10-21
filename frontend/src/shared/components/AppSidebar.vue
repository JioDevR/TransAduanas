<template>
  <aside :class="['app-sidebar', { 'collapsed': isCollapsed, 'mobile-open': isMobileOpen }]">
    <!-- Brand Header -->
    <div class="sidebar-brand">
      <div class="brand-logo">
        <span>SGA</span>
      </div>
      <div v-if="!isCollapsed" class="brand-info">
        <span class="brand-name">SGA</span>
        <span class="brand-tag">GESTIÓN ADUANERA</span>
      </div>
      <button
        type="button"
        class="toggle-btn"
        @click="toggleCollapse"
        title="Colapsar barra lateral"
      >
        <i :class="isCollapsed ? 'pi pi-chevron-right' : 'pi pi-chevron-left'"></i>
      </button>
    </div>

    <!-- Selector de Contexto / Módulo Activo -->
    <div v-if="!isCollapsed" class="sidebar-module-selector-card">
      <div class="module-selector-kicker">
        <span class="pulse-indicator"></span>
        <span>{{ authStore.canSwitchModules ? 'MÓDULO ACTIVO' : 'MÓDULO ASIGNADO' }}</span>
      </div>

      <!-- Selector para Roles con Permiso de Conmutación (Admin TI y Gerente) -->
      <div v-if="authStore.canSwitchModules" class="module-dropdown-wrapper">
        <select
          v-model="navigationStore.activeModule"
          class="module-custom-select"
          @change="handleModuleSelectChange"
        >
          <option value="administracion-ti">00 Administración TI</option>
          <option value="gerencia-general">01 Gerencia General</option>
          <option value="comercial">02 Recepción Comercial</option>
          <option value="gestion-aduanera">03 Gestión Aduanera</option>
          <option value="agente-aduanero">04 Agente Aduanero</option>
          <option value="transporte-flota">05 Transporte y Flota</option>
          <option value="finanzas">06 Finanzas</option>
          <option value="legal-auditoria">07 Legal y Auditoría</option>
        </select>
        <i class="pi pi-chevron-down select-chevron-icon"></i>
      </div>

      <!-- Indicador Fijo para Operadores con Rol Específico -->
      <div v-else class="static-module-pill">
        <span class="pill-num">{{ moduleNumberCode }}</span>
        <span class="pill-title">{{ activeModuleShortTitle }}</span>
      </div>
    </div>

    <!-- Navegación de Sub-opciones Dinámicas según el Módulo Activo -->
    <nav class="sidebar-nav">
      <div class="nav-section-title" v-if="!isCollapsed">
        <span>SUB-OPCIONES // {{ activeModuleShortTitle }}</span>
      </div>

      <!-- ======================================================== -->
      <!-- SUB-OPCIONES: 00 ADMINISTRACIÓN TI -->
      <!-- ======================================================== -->
      <ul v-if="navigationStore.currentModule === 'administracion-ti'" class="nav-list">
        <!-- Sub-opción 01: Usuarios -->
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.administracionTiSubOption === 'users' || navigationStore.administracionTiSubOption === 'create-user' }]"
            @click="handleAdminSubClick('users')"
            :title="isCollapsed ? 'Gestión de Usuarios' : ''"
          >
            <span class="nav-num">01</span>
            <span v-if="!isCollapsed" class="nav-label">Usuarios</span>
          </button>
        </li>

        <!-- Sub-opción 02: Acerca de -->
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.administracionTiSubOption === 'about' }]"
            @click="handleAdminSubClick('about')"
            :title="isCollapsed ? 'Acerca de SGA' : ''"
          >
            <span class="nav-num">02</span>
            <span v-if="!isCollapsed" class="nav-label">Acerca de</span>
          </button>
        </li>

        <!-- Sub-opción 03: Logs del Sistema -->
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.administracionTiSubOption === 'logs' }]"
            @click="handleAdminSubClick('logs')"
            :title="isCollapsed ? 'Consola de Logs TI' : ''"
          >
            <span class="nav-num">03</span>
            <span v-if="!isCollapsed" class="nav-label">Logs TI</span>
          </button>
        </li>
      </ul>

      <!-- ======================================================== -->
      <!-- SUB-OPCIONES: 01 GERENCIA GENERAL -->
      <!-- ======================================================== -->
      <ul v-else-if="navigationStore.currentModule === 'gerencia-general'" class="nav-list">
        <!-- Sub-opción 01: Vista General (KPIs) -->
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.gerenciaGeneralSubOption === 'overview' }]"
            @click="handleGerenciaSubClick('overview')"
            :title="isCollapsed ? 'Vista General Ejecutiva' : ''"
          >
            <span class="nav-num">01</span>
            <span v-if="!isCollapsed" class="nav-label">Vista General</span>
          </button>
        </li>

        <!-- Sub-opción 02: Sedes Territoriales -->
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.gerenciaGeneralSubOption === 'sedes' }]"
            @click="handleGerenciaSubClick('sedes')"
            :title="isCollapsed ? 'Gestión de Sedes' : ''"
          >
            <span class="nav-num">02</span>
            <span v-if="!isCollapsed" class="nav-label">Sedes</span>
          </button>
        </li>

        <!-- Sub-opción 03: Catálogos Auxiliares -->
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.gerenciaGeneralSubOption === 'catalogos' }]"
            @click="handleGerenciaSubClick('catalogos')"
            :title="isCollapsed ? 'Catálogos Auxiliares' : ''"
          >
            <span class="nav-num">03</span>
            <span v-if="!isCollapsed" class="nav-label">Catálogos Auxiliares</span>
          </button>
        </li>

        <!-- Sub-opción 04: Parque de Flota -->
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.gerenciaGeneralSubOption === 'flota' }]"
            @click="handleGerenciaSubClick('flota')"
            :title="isCollapsed ? 'Parque Automotor & Flota' : ''"
          >
            <span class="nav-num">04</span>
            <span v-if="!isCollapsed" class="nav-label">Parque de Flota</span>
          </button>
        </li>

        <!-- Sub-opción 05: Conductores MTC -->
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.gerenciaGeneralSubOption === 'conductores' }]"
            @click="handleGerenciaSubClick('conductores')"
            :title="isCollapsed ? 'Conductores MTC' : ''"
          >
            <span class="nav-num">05</span>
            <span v-if="!isCollapsed" class="nav-label">Conductores MTC</span>
          </button>
        </li>
      </ul>

      <!-- ======================================================== -->
      <!-- SUB-OPCIONES: 02 RECEPCIÓN COMERCIAL -->
      <!-- ======================================================== -->
      <ul v-else-if="navigationStore.currentModule === 'comercial'" class="nav-list">
        <!-- Sub-opción 01: Dashboard Comercial -->
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.comercialSubOption === 'dashboard' }]"
            @click="handleComercialSubClick('dashboard')"
            :title="isCollapsed ? 'Dashboard Comercial' : ''"
          >
            <span class="nav-num">01</span>
            <span v-if="!isCollapsed" class="nav-label">Dashboard Comercial</span>
          </button>
        </li>

        <!-- Sub-opción 02: Cotizaciones -->
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.comercialSubOption === 'cotizaciones' }]"
            @click="handleComercialSubClick('cotizaciones')"
            :title="isCollapsed ? 'Cotizaciones Comerciales' : ''"
          >
            <span class="nav-num">02</span>
            <span v-if="!isCollapsed" class="nav-label">Cotizaciones</span>
          </button>
        </li>

        <!-- Sub-opción 03: Clientes & Prospectos -->
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.comercialSubOption === 'clientes' }]"
            @click="handleComercialSubClick('clientes')"
            :title="isCollapsed ? 'Directorio de Clientes & Prospectos' : ''"
          >
            <span class="nav-num">03</span>
            <span v-if="!isCollapsed" class="nav-label">Clientes & Prospectos</span>
          </button>
        </li>

        <!-- Sub-opción 04: Expedientes Aduaneros -->
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.comercialSubOption === 'ordenes' }]"
            @click="handleComercialSubClick('ordenes')"
            :title="isCollapsed ? 'Expedientes Aduaneros' : ''"
          >
            <span class="nav-num">04</span>
            <span v-if="!isCollapsed" class="nav-label">Expedientes Aduaneros</span>
          </button>
        </li>
      </ul>

      <!-- ======================================================== -->
      <!-- SUB-OPCIONES: 03 GESTIÓN ADUANERA -->
      <!-- ======================================================== -->
      <ul v-else-if="navigationStore.currentModule === 'gestion-aduanera'" class="nav-list">
        <!-- Sub-opción 01: Bandeja de Cotizaciones -->
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.gestionAduaneraSubOption === 'cotizaciones' }]"
            @click="handleGestionAduaneraSubClick('cotizaciones')"
            :title="isCollapsed ? 'Bandeja de Cotizaciones' : ''"
          >
            <span class="nav-num">01</span>
            <span v-if="!isCollapsed" class="nav-label">Bandeja Cotizaciones</span>
          </button>
        </li>

        <!-- Sub-opción 02: Gestión de Expedientes -->
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.gestionAduaneraSubOption === 'expedientes' }]"
            @click="handleGestionAduaneraSubClick('expedientes')"
            :title="isCollapsed ? 'Gestión de Expedientes' : ''"
          >
            <span class="nav-num">02</span>
            <span v-if="!isCollapsed" class="nav-label">Gestión de Expedientes</span>
          </button>
        </li>

        <!-- Sub-opción 03: Declaraciones DAM (Liquidación) -->
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.gestionAduaneraSubOption === 'declaraciones' }]"
            @click="handleGestionAduaneraSubClick('declaraciones')"
            :title="isCollapsed ? 'Declaraciones DAM' : ''"
          >
            <span class="nav-num">03</span>
            <span v-if="!isCollapsed" class="nav-label">Declaraciones DAM</span>
          </button>
        </li>

        <!-- Sub-opción 04: Clasificación Arancelaria -->
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.gestionAduaneraSubOption === 'clasificacion' }]"
            @click="handleGestionAduaneraSubClick('clasificacion')"
            :title="isCollapsed ? 'Clasificación Arancelaria' : ''"
          >
            <span class="nav-num">04</span>
            <span v-if="!isCollapsed" class="nav-label">Clasificación Arancelaria</span>
          </button>
        </li>

        <!-- Sub-opción 05: Cálculo de Tributos -->
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.gestionAduaneraSubOption === 'tributos' }]"
            @click="handleGestionAduaneraSubClick('tributos')"
            :title="isCollapsed ? 'Cálculo de Tributos' : ''"
          >
            <span class="nav-num">05</span>
            <span v-if="!isCollapsed" class="nav-label">Cálculo de Tributos</span>
          </button>
        </li>
      </ul>

      <!-- ======================================================== -->
      <!-- SUB-OPCIONES: 04 AGENTE ADUANERO -->
      <!-- ======================================================== -->
      <ul v-else-if="navigationStore.currentModule === 'agente-aduanero'" class="nav-list">
        <!-- Sub-opción 01: Dashboard de Almacén -->
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.agenteAduaneroSubOption === 'almacen' }]"
            @click="handleAgenteAduaneroSubClick('almacen')"
            :title="isCollapsed ? 'Dashboard de Almacén' : ''"
          >
            <span class="nav-num">01</span>
            <span v-if="!isCollapsed" class="nav-label">Dashboard de Almacén</span>
          </button>
        </li>
        <!-- Sub-opción 02: Recepción Contenedores -->
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.agenteAduaneroSubOption === 'recepcion' }]"
            @click="handleAgenteAduaneroSubClick('recepcion')"
            :title="isCollapsed ? 'Recepción Contenedores' : ''"
          >
            <span class="nav-num">02</span>
            <span v-if="!isCollapsed" class="nav-label">Recepción Contenedores</span>
          </button>
        </li>
        <!-- Sub-opción 03: Control de Bultos & Salidas -->
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.agenteAduaneroSubOption === 'bultos' }]"
            @click="handleAgenteAduaneroSubClick('bultos')"
            :title="isCollapsed ? 'Control de Bultos' : ''"
          >
            <span class="nav-num">03</span>
            <span v-if="!isCollapsed" class="nav-label">Control de Bultos</span>
          </button>
        </li>
      </ul>

      <!-- ======================================================== -->
      <!-- SUB-OPCIONES: 05 TRANSPORTE Y FLOTA -->
      <!-- ======================================================== -->
      <ul v-else-if="navigationStore.currentModule === 'transporte-flota'" class="nav-list">
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.transporteFlotaSubOption === 'cabina' }]"
            @click="handleTransporteFlotaSubClick('cabina')"
            :title="isCollapsed ? 'Mi Trabajo Actual (Cabina)' : ''"
          >
            <span class="nav-num">01</span>
            <span v-if="!isCollapsed" class="nav-label">Mi Trabajo Actual</span>
          </button>
        </li>
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.transporteFlotaSubOption === 'camion' }]"
            @click="handleTransporteFlotaSubClick('camion')"
            :title="isCollapsed ? 'Camión Asignado' : ''"
          >
            <span class="nav-num">02</span>
            <span v-if="!isCollapsed" class="nav-label">Camión Asignado</span>
          </button>
        </li>
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.transporteFlotaSubOption === 'viajes' }]"
            @click="handleTransporteFlotaSubClick('viajes')"
            :title="isCollapsed ? 'Historial de Mis Viajes' : ''"
          >
            <span class="nav-num">03</span>
            <span v-if="!isCollapsed" class="nav-label">Historial de Viajes</span>
          </button>
        </li>
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.transporteFlotaSubOption === 'despachos' }]"
            @click="handleTransporteFlotaSubClick('despachos')"
            :title="isCollapsed ? 'Monitoreo Global' : ''"
          >
            <span class="nav-num">04</span>
            <span v-if="!isCollapsed" class="nav-label">Monitoreo Global</span>
          </button>
        </li>
      </ul>

      <!-- ======================================================== -->
      <!-- SUB-OPCIONES: 06 FINANZAS -->
      <!-- ======================================================== -->
      <ul v-else-if="navigationStore.currentModule === 'finanzas'" class="nav-list">
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.finanzasSubOption === 'liquidacion' }]"
            @click="handleFinanzasSubClick('liquidacion')"
            :title="isCollapsed ? 'Liquidación de Cuentas' : ''"
          >
            <span class="nav-num">01</span>
            <span v-if="!isCollapsed" class="nav-label">Liquidación de Cuentas</span>
          </button>
        </li>
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.finanzasSubOption === 'facturacion' }]"
            @click="handleFinanzasSubClick('facturacion')"
            :title="isCollapsed ? 'Facturación & Pagos' : ''"
          >
            <span class="nav-num">02</span>
            <span v-if="!isCollapsed" class="nav-label">Facturación & Pagos</span>
          </button>
        </li>
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.finanzasSubOption === 'anticipos' }]"
            @click="handleFinanzasSubClick('anticipos')"
            :title="isCollapsed ? 'Anticipos de Cotización Pre-Expediente' : ''"
          >
            <span class="nav-num">03</span>
            <span v-if="!isCollapsed" class="nav-label">Anticipos Cotización</span>
          </button>
        </li>

        <!-- Sub-opción 04: Acción Rápida - Registrar Anticipo / Pago -->
        <li class="nav-item">
          <button
            type="button"
            class="nav-sub-btn action-item"
            @click="handleFinanzasAction('registrar-anticipo')"
            :title="isCollapsed ? 'Registrar Pago / Anticipo' : ''"
          >
            <span class="nav-num">04</span>
            <span v-if="!isCollapsed" class="nav-label">+ Registrar Anticipo</span>
          </button>
        </li>
      </ul>

      <!-- ======================================================== -->
      <!-- SUB-OPCIONES: 07 LEGAL Y AUDITORÍA -->
      <!-- ======================================================== -->
      <ul v-else-if="navigationStore.currentModule === 'legal-auditoria'" class="nav-list">
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.legalAuditoriaSubOption === 'expedientes' }]"
            @click="handleLegalAuditoriaSubClick('expedientes')"
            :title="isCollapsed ? 'Expedientes & Recursos' : ''"
          >
            <span class="nav-num">01</span>
            <span v-if="!isCollapsed" class="nav-label">Expedientes & Recursos</span>
          </button>
        </li>
        <li class="nav-item">
          <button
            type="button"
            :class="['nav-sub-btn', { active: navigationStore.legalAuditoriaSubOption === 'auditorias' }]"
            @click="handleLegalAuditoriaSubClick('auditorias')"
            :title="isCollapsed ? 'Auditorías SUNAT' : ''"
          >
            <span class="nav-num">02</span>
            <span v-if="!isCollapsed" class="nav-label">Auditorías SUNAT</span>
          </button>
        </li>
      </ul>
    </nav>


  </aside>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useNavigationStore } from '@/core/stores/navigationStore'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const navigationStore = useNavigationStore()

const isCollapsed = ref(false)
const isMobileOpen = ref(false)

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const activeModuleShortTitle = computed(() => {
  const map = {
    'administracion-ti': 'ADMINISTRACIÓN TI',
    'gerencia-general': 'GERENCIA GENERAL',
    'comercial': 'RECEPCIÓN COMERCIAL',
    'gestion-aduanera': 'GESTIÓN ADUANERA',
    'agente-aduanero': 'AGENTE ADUANERO',
    'transporte-flota': 'TRANSPORTE Y FLOTA',
    'finanzas': 'FINANZAS',
    'legal-auditoria': 'LEGAL Y AUDITORÍA'
  }
  return map[navigationStore.currentModule] || 'OPERACIONES'
})

const moduleNumberCode = computed(() => {
  const codeMap = {
    'administracion-ti': '00',
    'gerencia-general': '01',
    'comercial': '02',
    'gestion-aduanera': '03',
    'agente-aduanero': '04',
    'transporte-flota': '05',
    'finanzas': '06',
    'legal-auditoria': '07'
  }
  return codeMap[navigationStore.currentModule] || '00'
})

// Cambio de módulo desde el selector dropdown del sidebar
const handleModuleSelectChange = () => {
  const mod = navigationStore.activeModule
  if (mod === 'administracion-ti') {
    navigationStore.setAdministracionTiSubOption('users')
    if (route.path !== '/administracion-ti/usuarios') {
      router.push('/administracion-ti/usuarios')
    }
  } else if (mod === 'gerencia-general') {
    navigationStore.setGerenciaGeneralSubOption('sedes')
    if (route.path !== '/gerencia-general/sedes') {
      router.push('/gerencia-general/sedes')
    }
  } else if (mod === 'comercial') {
    navigationStore.setComercialSubOption('cotizaciones')
    if (route.path !== '/recepcion-comercial') {
      router.push('/recepcion-comercial')
    }
  } else if (mod === 'gestion-aduanera') {
    navigationStore.setGestionAduaneraSubOption('cotizaciones')
    if (route.path !== '/gestion-aduanera') {
      router.push('/gestion-aduanera')
    }
  } else if (mod === 'agente-aduanero') {
    navigationStore.setAgenteAduaneroSubOption('almacen')
    if (route.path !== '/agente-aduanero') {
      router.push('/agente-aduanero')
    }
  } else if (mod === 'transporte-flota') {
    navigationStore.setTransporteFlotaSubOption('despachos')
    if (route.path !== '/transporte-flota') {
      router.push('/transporte-flota')
    }
  } else if (mod === 'finanzas') {
    navigationStore.setFinanzasSubOption('liquidacion')
    if (route.path !== '/finanzas') {
      router.push('/finanzas')
    }
  } else if (mod === 'legal-auditoria') {
    navigationStore.setLegalAuditoriaSubOption('expedientes')
    if (route.path !== '/legal-auditoria') {
      router.push('/legal-auditoria')
    }
  }
}

// Navegación en sub-opciones de Gerencia General
const handleGerenciaSubClick = (subOption) => {
  navigationStore.setActiveModule('gerencia-general')
  navigationStore.setGerenciaGeneralSubOption(subOption)
  const pathMap = {
    overview: '/gerencia-general/resumen',
    sedes: '/gerencia-general/sedes',
    catalogos: '/gerencia-general/catalogos',
    flota: '/gerencia-general/flota',
    conductores: '/gerencia-general/conductores'
  }
  const target = pathMap[subOption] || '/gerencia-general/sedes'
  if (route.path !== target) {
    router.push(target)
  }
}

// Navegación en sub-opciones de Administración TI
const handleAdminSubClick = (subOption) => {
  navigationStore.setActiveModule('administracion-ti')
  navigationStore.setAdministracionTiSubOption(subOption)
  let target = '/administracion-ti/usuarios'
  if (subOption === 'about') {
    target = '/administracion-ti/acerca-de'
  } else if (subOption === 'logs') {
    target = '/administracion-ti/logs'
  }
  if (route.path !== target) {
    router.push(target)
  }
}

// Navegación en sub-opciones de Comercial
const handleComercialSubClick = (subOption) => {
  navigationStore.setActiveModule('comercial')
  navigationStore.setComercialSubOption(subOption)
  if (route.path !== '/recepcion-comercial') {
    router.push('/recepcion-comercial')
  }
}

// Disparador de acciones rápidas de Comercial (modales)
const handleComercialAction = (action) => {
  navigationStore.setActiveModule('comercial')
  if (route.path !== '/recepcion-comercial') {
    router.push('/recepcion-comercial')
  }
  navigationStore.triggerComercialAction(action)
}

// Navegación en sub-opciones de Gestión Aduanera (incluye Liquidación)
const handleGestionAduaneraSubClick = (subOption) => {
  navigationStore.setActiveModule('gestion-aduanera')
  navigationStore.setGestionAduaneraSubOption(subOption)
  if (['declaraciones', 'clasificacion', 'tributos'].includes(subOption)) {
    if (route.path !== '/gestion-aduanera/liquidacion') {
      router.push('/gestion-aduanera/liquidacion')
    }
  } else {
    if (route.path !== '/gestion-aduanera') {
      router.push('/gestion-aduanera')
    }
  }
}

// Navegación en sub-opciones de Agente Aduanero
const handleAgenteAduaneroSubClick = (subOption) => {
  navigationStore.setActiveModule('agente-aduanero')
  navigationStore.setAgenteAduaneroSubOption(subOption)
  if (route.path !== '/agente-aduanero') {
    router.push('/agente-aduanero')
  }
}

// Navegación en sub-opciones de Transporte y Flota
const handleTransporteFlotaSubClick = (subOption) => {
  navigationStore.setActiveModule('transporte-flota')
  navigationStore.setTransporteFlotaSubOption(subOption)
  if (route.path !== '/transporte-flota') {
    router.push('/transporte-flota')
  }
}

// Navegación en sub-opciones de Legal y Auditoría
const handleLegalAuditoriaSubClick = (subOption) => {
  navigationStore.setActiveModule('legal-auditoria')
  navigationStore.setLegalAuditoriaSubOption(subOption)
  if (route.path !== '/legal-auditoria') {
    router.push('/legal-auditoria')
  }
}

// Navegación en sub-opciones de Finanzas
const handleFinanzasSubClick = (subOption) => {
  navigationStore.setActiveModule('finanzas')
  navigationStore.setFinanzasSubOption(subOption)
  if (route.path !== '/finanzas') {
    router.push('/finanzas')
  }
}

// Disparador de acciones rápidas de Finanzas (modales)
const handleFinanzasAction = (action) => {
  navigationStore.setActiveModule('finanzas')
  if (route.path !== '/finanzas') {
    router.push('/finanzas')
  }
  navigationStore.triggerFinanzasAction(action)
}


</script>

<style scoped>
.app-sidebar {
  width: 250px;
  min-width: 250px;
  background: var(--color-bg-sidebar);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  height: 100vh;
  position: sticky;
  top: 0;
  transition: width var(--transition-normal);
  z-index: 100;
  user-select: none;
}

.app-sidebar.collapsed {
  width: 76px;
  min-width: 76px;
}

.sidebar-brand {
  height: 72px;
  display: flex;
  align-items: center;
  padding: 0 1.25rem;
  gap: 0.85rem;
  border-bottom: 1px solid var(--color-border);
  position: relative;
}

.brand-logo {
  width: 42px;
  height: 42px;
  background: var(--color-primary);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary-text);
  font-weight: 800;
  font-size: 0.95rem;
  letter-spacing: 0.05em;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(53, 47, 42, 0.25);
}

.brand-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  white-space: nowrap;
}

.brand-name {
  font-weight: 700;
  font-size: 1.15rem;
  letter-spacing: -0.01em;
  color: var(--color-text-main);
  line-height: 1.15;
}

.brand-tag {
  font-size: 0.625rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--color-text-subtle);
  margin-top: 0.15rem;
}

.toggle-btn {
  margin-left: auto;
  background: transparent;
  border: none;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: 0.4rem;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  transition: all var(--transition-fast);
}

.toggle-btn:hover {
  color: var(--color-text-main);
  background: var(--color-bg-muted);
}

/* Card Selector de Módulo / Perfil Activo */
.sidebar-module-selector-card {
  margin: 1rem 0.85rem 0.5rem;
  padding: 0.65rem 0.75rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.module-selector-kicker {
  font-size: 0.6rem;
  font-weight: 800;
  letter-spacing: 0.1em;
  color: var(--color-accent-kicker);
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.pulse-indicator {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #16a34a;
  box-shadow: 0 0 4px rgba(22, 163, 74, 0.5);
}

.module-dropdown-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.module-custom-select {
  width: 100%;
  appearance: none;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0.45rem 1.65rem 0.45rem 0.6rem;
  font-size: 0.775rem;
  font-weight: 700;
  color: var(--color-text-main);
  cursor: pointer;
  outline: none;
  transition: all var(--transition-fast);
}

.module-custom-select:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(53, 47, 42, 0.15);
}

.select-chevron-icon {
  position: absolute;
  right: 0.55rem;
  font-size: 0.65rem;
  color: var(--color-text-subtle);
  pointer-events: none;
}

.static-module-pill {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0.45rem 0.65rem;
}

.pill-num {
  font-size: 0.7rem;
  font-weight: 800;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.pill-title {
  font-size: 0.775rem;
  font-weight: 700;
  color: var(--color-text-main);
}

/* Navegación y Sub-opciones */
.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 0.75rem 0.85rem;
}

.nav-section-title {
  font-size: 0.65rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--color-accent-kicker);
  font-weight: 800;
  padding: 0 0.5rem 0.65rem;
}

.nav-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.nav-item {
  width: 100%;
}

.nav-sub-btn {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.65rem 0.85rem;
  border-radius: 10px;
  background: transparent;
  border: 1px solid transparent;
  color: var(--color-text-muted);
  font-size: 0.84rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
  text-align: left;
}

.nav-num {
  font-size: 0.725rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  padding: 0.15rem 0.45rem;
  border-radius: 6px;
  min-width: 26px;
  text-align: center;
  transition: all var(--transition-fast);
}

.nav-sub-btn:hover {
  color: var(--color-text-main);
  background: var(--color-bg-subtle);
}

.nav-sub-btn:hover .nav-num {
  background: var(--color-bg-muted);
  color: var(--color-text-main);
}

/* Estado Activo: Espresso Profundo */
.nav-sub-btn.active {
  color: var(--color-primary-text);
  background: var(--color-primary);
  border-color: var(--color-primary);
  box-shadow: 0 2px 8px rgba(53, 47, 42, 0.2);
}

.nav-sub-btn.active .nav-num {
  background: rgba(0, 0, 0, 0.2);
  color: var(--color-primary-text);
}

.action-pill {
  margin-left: auto;
  font-size: 0.75rem;
  font-weight: 800;
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-sub-btn.active .action-pill {
  background: rgba(0, 0, 0, 0.2);
  color: var(--color-primary-text);
}

.action-icon {
  margin-left: auto;
  font-size: 0.8rem;
  color: var(--color-text-subtle);
}

.action-item {
  color: var(--color-text-main);
  font-weight: 600;
}

.action-item .nav-num {
  background: var(--color-bg-subtle);
  color: var(--color-accent-kicker);
}

.nav-label {
  overflow: hidden;
  text-overflow: ellipsis;
}


</style>
