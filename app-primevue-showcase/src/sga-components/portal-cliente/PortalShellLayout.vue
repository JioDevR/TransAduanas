<template>
  <div class="sga-shell">
    <PortalSidebar
      :active-key="activeKey"
      :unread-count="unreadCount"
      :company-name="companyName"
      :company-ruc="companyRuc"
      @select="(key) => emit('update:activeKey', key)"
    />

    <div class="sga-main">
      <!-- Topbar Minimalista en una sola línea (Copiada de app-sga y adaptada a B2B) -->
      <header class="sga-topbar">
        <h1 class="sga-topbar-title">{{ currentTitle }}</h1>

        <div class="sga-topbar-actions">
          <!-- Pill de Empresa / RUC (Equivalente a SEDE en app-sga) -->
          <div
            class="sga-sede-pill"
            :title="`Empresa Autenticada: ${companyName} — RUC ${companyRuc}`"
          >
            <span class="sga-sede-badge">RUC</span>
            <span class="sga-sede-text portal-mono-num">
              {{ companyRuc }} — {{ companyName }}
            </span>
          </div>

          <!-- Acceso rápido a Notificaciones -->
          <Button
            type="button"
            icon="pi pi-bell"
            severity="secondary"
            variant="text"
            rounded
            :badge="unreadCount > 0 ? (unreadCount > 99 ? '99+' : String(unreadCount)) : undefined"
            badgeSeverity="danger"
            aria-label="Centro de Notificaciones"
            title="Centro de Notificaciones"
            @click="emit('update:activeKey', 'notificaciones')"
          />

          <!-- Perfil de Usuario + Popover -->
          <button
            type="button"
            class="sga-user-trigger"
            @click="toggleUserPopover"
            aria-label="Opciones de cuenta"
          >
            <span class="sga-user-avatar">{{ avatarInitials }}</span>
            <span class="sga-user-meta">
              <span class="sga-user-name">{{ userName }}</span>
              <span class="sga-user-role">Acceso Cliente</span>
            </span>
            <i class="pi pi-chevron-down sga-user-chevron"></i>
          </button>

          <Popover ref="userPopoverRef">
            <div class="sga-popover-body">
              <div class="sga-popover-header">
                <span class="sga-user-avatar">{{ avatarInitials }}</span>
                <div class="sga-user-meta">
                  <span class="sga-user-name">{{ userName }}</span>
                  <span class="sga-user-role">Acceso Cliente B2B</span>
                </div>
              </div>

              <div class="sga-popover-details">
                <div class="sga-popover-row">
                  <span>EMPRESA</span>
                  <strong>{{ companyName }}</strong>
                </div>
                <div class="sga-popover-row">
                  <span>RUC</span>
                  <strong class="portal-mono-num">{{ companyRuc }}</strong>
                </div>
                <div class="sga-popover-row">
                  <span>USUARIO</span>
                  <strong>{{ userUsername }}</strong>
                </div>
              </div>

              <div class="sga-popover-theme">
                <span class="sga-popover-theme-label">
                  <i :class="['pi', isDark ? 'pi-moon' : 'pi-sun']"></i>
                  <span>{{ isDark ? 'Modo Oscuro' : 'Modo Claro' }}</span>
                </span>
                <ToggleSwitch :modelValue="isDark" @update:modelValue="emit('toggle-theme')" />
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

      <!-- Contenido Principal -->
      <main class="sga-page-content">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import Button from 'primevue/button'
import Popover from 'primevue/popover'
import ToggleSwitch from 'primevue/toggleswitch'
import PortalSidebar from './PortalSidebar.vue'
import '../../../../sgaUI/sga-shell.css'

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
  },
  userName: {
    type: String,
    default: 'María Fernanda López'
  },
  userUsername: {
    type: String,
    default: 'mflopez.lg'
  },
  isDark: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:activeKey', 'toggle-theme', 'logout'])

const userPopoverRef = ref(null)

const toggleUserPopover = (event) => {
  userPopoverRef.value?.toggle(event)
}

const handleLogout = () => {
  userPopoverRef.value?.hide()
  emit('logout')
}

const PORTAL_TITLES = {
  cotizaciones: 'Mis Cotizaciones Logísticas',
  expedientes: 'Expedientes & Tracking DAM',
  pagos: 'Cobros, Pagos & Conciliaciones',
  notificaciones: 'Notificaciones & Avisos Operativos'
}

const currentTitle = computed(() => {
  return PORTAL_TITLES[props.activeKey] || 'Portal del Cliente B2B'
})

const avatarInitials = computed(() => {
  const parts = (props.userName || 'CL').trim().split(/\s+/)
  if (parts.length >= 2) {
    return `${parts[0].charAt(0)}${parts[1].charAt(0)}`.toUpperCase()
  }
  return parts[0].substring(0, 2).toUpperCase()
})
</script>

<style scoped>
.portal-mono-num {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
}
</style>
