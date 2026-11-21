<template>
  <aside :class="['sga-sidebar', { collapsed: isCollapsed }]">
    <!-- Cabecera de Marca -->
    <div class="sga-sidebar-brand">
      <div class="sga-brand-badge" title="TransAduanas - Portal de Clientes B2B">
        <i class="pi pi-box"></i>
      </div>
      <div v-if="!isCollapsed" class="sga-brand-info">
        <span class="sga-brand-name">TransAduanas</span>
        <span class="sga-brand-tag">PORTAL CLIENTE</span>
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
    </div>

    <!-- Tarjeta de Empresa Autenticada -->
    <div
      v-if="!isCollapsed"
      class="portal-company-card"
      :title="`${authStore.empresaNombre} — RUC: ${authStore.ruc}`"
    >
      <div class="portal-company-icon">
        <i class="pi pi-building"></i>
      </div>
      <div class="portal-company-meta">
        <span class="portal-company-name">{{ authStore.empresaNombre }}</span>
        <div class="portal-company-ruc-row">
          <span class="portal-ruc-badge">RUC</span>
          <span class="portal-ruc-num">{{ authStore.ruc || '—' }}</span>
        </div>
      </div>
    </div>

    <!-- Navegación Principal del Portal B2B -->
    <nav class="sga-sidebar-nav" aria-label="Navegación Portal B2B">
      <ul class="sga-nav-list">
        <li v-for="item in NAV_ITEMS" :key="item.to">
          <button
            type="button"
            :class="['sga-nav-btn', { active: isItemActive(item.to) }]"
            @click="navigateTo(item.to)"
            :title="isCollapsed ? item.label : ''"
          >
            <span class="sga-nav-dot"></span>
            <span v-if="!isCollapsed" class="sga-nav-label">{{ item.label }}</span>
          </button>
        </li>
      </ul>
    </nav>
  </aside>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/core/stores/authStore'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isCollapsed = ref(false)
const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const NAV_ITEMS = [
  { to: '/dashboard', label: 'Resumen General' },
  { to: '/cotizaciones', label: 'Mis Cotizaciones' },
  { to: '/expedientes', label: 'Expedientes & Tracking' },
  { to: '/pagos', label: 'Cobros y Pagos' }
]

const isItemActive = (path) => {
  return route.path === path || route.path.startsWith(`${path}/`)
}

const navigateTo = (path) => {
  if (route.path !== path) {
    router.push(path)
  }
}
</script>

<style scoped>
.portal-company-card {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  padding: 0.7rem 0.75rem;
  border-radius: var(--radius-lg);
  background: var(--color-bg-base);
  border: 1px solid var(--color-border);
}

.portal-company-icon {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border-subtle);
  color: var(--color-text-muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.9rem;
  flex-shrink: 0;
}

.portal-company-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 0.2rem;
}

.portal-company-name {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--color-text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.portal-company-ruc-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.portal-ruc-badge {
  font-size: 0.6rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  padding: 0.08rem 0.35rem;
  border-radius: var(--radius-xs);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-muted);
}

.portal-ruc-num {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 0.72rem;
  font-weight: 500;
  color: var(--color-text-muted);
}
</style>
