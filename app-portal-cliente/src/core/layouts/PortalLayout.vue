<template>
  <div class="sga-shell">
    <PortalSidebar />

    <div class="sga-main">
      <!-- Topbar Minimalista en una sola línea (sgaUI) -->
      <header class="sga-topbar">
        <h1 class="sga-topbar-title">{{ currentTitle }}</h1>

        <div class="sga-topbar-actions">
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
            @click="router.push('/notificaciones')"
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
              <span class="sga-user-name">{{ authStore.usuarioNombre }}</span>
              <span class="sga-user-role">Acceso Cliente</span>
            </span>
            <i class="pi pi-chevron-down sga-user-chevron"></i>
          </button>

          <Popover ref="userPopoverRef">
            <div class="sga-popover-body">
              <div class="sga-popover-header">
                <span class="sga-user-avatar">{{ avatarInitials }}</span>
                <div class="sga-user-meta">
                  <span class="sga-user-name">{{ authStore.usuarioNombre }}</span>
                  <span class="sga-user-role">Acceso Cliente B2B</span>
                </div>
              </div>

              <div class="sga-popover-details">
                <div v-if="authStore.empresaNombre" class="sga-popover-row">
                  <span>EMPRESA</span>
                  <strong>{{ authStore.empresaNombre }}</strong>
                </div>
                <div v-if="authStore.ruc" class="sga-popover-row">
                  <span>RUC</span>
                  <strong class="portal-mono-num">{{ authStore.ruc }}</strong>
                </div>
                <div v-if="authStore.user?.username" class="sga-popover-row">
                  <span>USUARIO</span>
                  <strong>{{ authStore.user.username }}</strong>
                </div>
              </div>

              <div class="sga-popover-theme">
                <span class="sga-popover-theme-label">
                  <i :class="['pi', theme.isDark.value ? 'pi-moon' : 'pi-sun']"></i>
                  <span>{{ theme.isDark.value ? 'Modo Oscuro' : 'Modo Claro' }}</span>
                </span>
                <ToggleSwitch :modelValue="theme.isDark.value" @update:modelValue="theme.toggleTheme" />
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
import PortalSidebar from '@/core/layouts/PortalSidebar.vue'
import { useAuthStore } from '@/core/stores/authStore'
import { useTheme } from '@/shared/composables/useTheme'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import { portalNotificationService, unreadNotificationsCount } from '@/features/notificaciones/services/portalNotificationService'
import '../../../../sgaUI/sga-shell.css'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const theme = useTheme()
const { confirmAction } = useConfirmation()

const userPopoverRef = ref(null)
const unreadCount = unreadNotificationsCount

const ROUTE_TITLES = {
  Cotizaciones: 'Mis Cotizaciones Logísticas',
  CotizacionDetalle: 'Detalle de Cotización',
  Expedientes: 'Expedientes & Tracking DAM',
  Pagos: 'Cobros y Pagos',
  RegistrarPago: 'Registrar Pago / Abono Bancario',
  SubsanarPago: 'Subsanar Abono Bancario',
  Notificaciones: 'Notificaciones & Avisos'
}

const currentTitle = computed(() => {
  return ROUTE_TITLES[route.name] || route.meta?.title?.replace(' — Portal TransAduanas', '') || 'Portal B2B'
})

const avatarInitials = computed(() => {
  const parts = (authStore.usuarioNombre || 'CL').trim().split(/\s+/)
  if (parts.length >= 2) {
    return `${parts[0].charAt(0)}${parts[1].charAt(0)}`.toUpperCase()
  }
  return parts[0].substring(0, 2).toUpperCase()
})

const toggleUserPopover = (event) => {
  userPopoverRef.value?.toggle(event)
}

async function cargarResumenNotificaciones() {
  if (!authStore.isAuthenticated) return
  try {
    await portalNotificationService.getResumenNoLeidas()
  } catch (error) {
    console.debug('Error al obtener resumen de notificaciones:', error)
  }
}

watch(() => route.path, cargarResumenNotificaciones, { immediate: true })

async function handleLogout() {
  userPopoverRef.value?.hide()
  const confirmed = await confirmAction({
    header: 'Cerrar Sesión Empresarial',
    message: '¿Está seguro de que desea salir del portal corporativo TransAduanas?',
    icon: 'pi pi-sign-out',
    acceptLabel: 'Cerrar Sesión',
    rejectLabel: 'Permanecer',
    acceptClass: 'p-button-danger'
  })

  if (confirmed) {
    authStore.logout()
    router.replace('/login')
  }
}
</script>

<style scoped>
.portal-mono-num {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
}
</style>
