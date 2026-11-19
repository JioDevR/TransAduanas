<template>
  <aside :class="['sga-sidebar', { collapsed: isCollapsed }]">
    <!-- Cabecera de Marca (Adaptada a Portal B2B) -->
    <div class="sga-sidebar-brand">
      <div class="sga-brand-badge">B2B</div>
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

    <!-- Navegación Principal del Portal B2B -->
    <nav class="sga-sidebar-nav" aria-label="Navegación Portal B2B">
      <ul class="sga-nav-list">
        <li v-for="item in navItems" :key="item.key">
          <button
            type="button"
            :class="['sga-nav-btn', { active: activeKey === item.key }]"
            @click="emit('select', item.key)"
            :title="isCollapsed ? item.label : ''"
          >
            <span class="sga-nav-dot"></span>
            <span v-if="!isCollapsed" class="sga-nav-label">{{ item.label }}</span>
            <Tag
              v-if="!isCollapsed && item.badge > 0"
              :value="item.badge > 99 ? '99+' : String(item.badge)"
              severity="danger"
              rounded
              class="portal-nav-badge"
            />
          </button>
        </li>
      </ul>
    </nav>

    <!-- Tarjeta de Empresa Vinculada (Equivalente al Gestor de Módulos de app-sga) -->
    <div v-if="!isCollapsed" class="sga-module-card" :title="`${companyName} (RUC: ${companyRuc})`">
      <div class="sga-module-kicker">
        <span class="sga-pulse-dot"></span>
        <span>EMPRESA VINCULADA</span>
      </div>

      <div class="sga-static-module-pill">
        <span class="sga-pill-num">RUC</span>
        <span class="portal-ruc-text">{{ companyRuc }}</span>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref, computed } from 'vue'
import Tag from 'primevue/tag'

const props = defineProps({
  activeKey: {
    type: String,
    default: 'cotizaciones'
  },
  unreadCount: {
    type: Number,
    default: 0
  },
  companyName: {
    type: String,
    default: 'LG Electronics Perú S.A.'
  },
  companyRuc: {
    type: String,
    default: '20511894520'
  }
})

const emit = defineEmits(['select'])

const isCollapsed = ref(false)
const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const navItems = computed(() => [
  { key: 'cotizaciones', label: 'Mis Cotizaciones', badge: 0 },
  { key: 'expedientes', label: 'Expedientes & Tracking', badge: 0 },
  { key: 'pagos', label: 'Cobros y Pagos', badge: 0 },
  { key: 'notificaciones', label: 'Notificaciones', badge: props.unreadCount }
])
</script>

<style scoped>
.portal-nav-badge {
  margin-left: auto;
  font-size: 0.7rem;
  padding: 0.1rem 0.45rem;
}

.portal-ruc-text {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
