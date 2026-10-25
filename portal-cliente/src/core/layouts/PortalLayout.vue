<template>
  <div class="portal-layout">
    <!-- Topbar Corporativo B2B -->
    <header class="portal-header">
      <div class="header-container">
        <!-- Logo y Marca -->
        <div class="header-brand">
          <div class="brand-logo" title="TransAduanas - Portal de Clientes">
            <i class="pi pi-shield"></i>
          </div>
          <div class="brand-info">
            <div class="brand-title-row">
              <span class="brand-name">TransAduanas</span>
              <span class="badge-portal">PORTAL B2B</span>
            </div>
            <span class="brand-sub">Gestión Aduanera & Logística Internacional</span>
          </div>
        </div>

        <!-- Información de la Empresa Autenticada -->
        <div class="header-company">
          <div class="company-badge-icon">
            <i class="pi pi-building"></i>
          </div>
          <div class="company-meta">
            <span class="company-name">{{ authStore.empresaNombre }}</span>
            <span class="company-ruc">RUC: {{ authStore.ruc }}</span>
          </div>
        </div>

        <!-- Acciones: Tema, Notificaciones y Usuario -->
        <div class="header-actions">
          <button 
            type="button" 
            class="action-btn theme-toggle" 
            @click="theme.toggleTheme" 
            :title="theme.isDark.value ? 'Cambiar a Modo Claro' : 'Cambiar a Modo Oscuro'"
            aria-label="Alternar tema visual"
          >
            <i :class="theme.isDark.value ? 'pi pi-sun' : 'pi pi-moon'"></i>
          </button>

          <!-- Botón de Notificaciones con Badge -->
          <router-link 
            to="/notificaciones" 
            class="action-btn notif-btn" 
            title="Centro de Notificaciones"
            aria-label="Ver Notificaciones"
          >
            <i class="pi pi-bell"></i>
            <span v-if="unreadCount > 0" class="badge-unread">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </router-link>

          <div class="user-profile">
            <div class="user-avatar" :title="authStore.usuarioNombre">
              {{ userInitial }}
            </div>
            <div class="user-text">
              <span class="user-name">{{ authStore.usuarioNombre }}</span>
              <span class="user-role">Acceso Cliente</span>
            </div>
          </div>

          <button 
            type="button" 
            class="action-btn logout-btn" 
            @click="handleLogout" 
            title="Cerrar Sesión Empresarial"
            aria-label="Cerrar sesión"
          >
            <i class="pi pi-sign-out"></i>
          </button>
        </div>
      </div>

      <!-- Barra de Navegación Principal -->
      <nav class="portal-nav">
        <div class="nav-container">
          <router-link to="/cotizaciones" class="nav-item" active-class="active">
            <i class="pi pi-file-edit"></i>
            <span>Mis Cotizaciones</span>
          </router-link>

          <router-link to="/expedientes" class="nav-item" active-class="active">
            <i class="pi pi-box"></i>
            <span>Expedientes & Tracking DAM</span>
          </router-link>

          <router-link to="/pagos" class="nav-item" active-class="active">
            <i class="pi pi-wallet"></i>
            <span>Cobros y Pagos</span>
          </router-link>

          <router-link to="/notificaciones" class="nav-item" active-class="active">
            <i class="pi pi-bell"></i>
            <span>Notificaciones</span>
            <span v-if="unreadCount > 0" class="nav-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </router-link>
        </div>
      </nav>
    </header>

    <!-- Contenido Principal -->
    <main class="portal-main">
      <div class="main-container">
        <router-view />
      </div>
    </main>

    <!-- Footer Ejecutivo -->
    <footer class="portal-footer">
      <div class="footer-container">
        <div class="footer-legal">
          <span>&copy; 2026 TransAduanas S.A. • Agencia de Aduana & Operador Logístico Integral</span>
          <span class="footer-sep">•</span>
          <span>Autorizado por SUNAT / MTC</span>
        </div>
        <div class="footer-contact">
          <i class="pi pi-headphones"></i>
          <span>Central Operativa 24/7: (01) 618-9000 • comex@transaduanas.pe</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/core/stores/authStore'
import { useTheme } from '@/shared/composables/useTheme'
import { portalNotificationService } from '@/features/notificaciones/services/portalNotificationService'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const theme = useTheme()

const unreadCount = ref(0)

const userInitial = computed(() => {
  const name = authStore.usuarioNombre || 'C'
  return name.charAt(0).toUpperCase()
})

async function cargarResumenNotificaciones() {
  if (!authStore.isAuthenticated) return
  try {
    const count = await portalNotificationService.getResumenNoLeidas()
    unreadCount.value = typeof count === 'number' ? count : 0
  } catch (error) {
    // Silently ignore or set 0 if network fails
    console.debug('Error al obtener resumen de notificaciones:', error)
  }
}

watch(() => route.path, () => {
  cargarResumenNotificaciones()
})

onMounted(() => {
  cargarResumenNotificaciones()
})

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.portal-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--color-bg-base, #f6f3ee);
  color: var(--color-text-main, #2b2520);
  font-family: var(--font-sans);
}

.portal-header {
  background: var(--color-bg-surface, #ffffff);
  border-bottom: 1px solid var(--color-border, #e4dacb);
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 10px rgba(43, 37, 32, 0.03);
}

.header-container {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0.85rem 1.75rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.brand-logo {
  width: 42px;
  height: 42px;
  background: var(--color-primary, #352f2a);
  color: var(--color-primary-text, #ffffff);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
}

.brand-info {
  display: flex;
  flex-direction: column;
}

.brand-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.brand-name {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-text-main, #2b2520);
  letter-spacing: -0.02em;
}

.badge-portal {
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  background: var(--color-bg-subtle, #f2ebe1);
  color: var(--color-accent-kicker, #9a7f60);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 4px;
  letter-spacing: 0.05em;
}

.brand-sub {
  font-size: 0.72rem;
  color: var(--color-text-muted, #6f6356);
}

.header-company {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.45rem 1rem;
  background: var(--color-bg-base, #f6f3ee);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: 8px;
}

.company-badge-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border-subtle, #ede5d8);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-accent-kicker, #9a7f60);
  font-size: 1rem;
}

.company-meta {
  display: flex;
  flex-direction: column;
}

.company-name {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text-main, #2b2520);
}

.company-ruc {
  font-size: 0.72rem;
  font-family: var(--font-mono);
  color: var(--color-text-muted, #6f6356);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.action-btn {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  border: 1px solid var(--color-border, #e4dacb);
  background: var(--color-bg-surface, #ffffff);
  color: var(--color-text-main, #2b2520);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background: var(--color-bg-subtle, #f2ebe1);
  border-color: var(--color-border-strong, #d5c8b6);
}

.notif-btn {
  position: relative;
  text-decoration: none;
}

.badge-unread {
  position: absolute;
  top: -4px;
  right: -4px;
  background: var(--color-danger, #b23b3b);
  color: #ffffff;
  font-size: 0.65rem;
  font-weight: 700;
  height: 18px;
  min-width: 18px;
  padding: 0 4px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--color-bg-surface, #ffffff);
}

.nav-badge {
  background: var(--color-danger, #b23b3b);
  color: #ffffff;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.1rem 0.45rem;
  border-radius: 10px;
  line-height: 1;
}

.logout-btn:hover {
  color: var(--color-danger-text, #9e2a2b);
  background: var(--color-danger-bg, #fdf0f0);
  border-color: var(--color-danger-border, #f6c8c8);
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.25rem 0.65rem;
  border-radius: 8px;
  border: 1px solid var(--color-border-subtle, #ede5d8);
}

.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--color-accent-kicker, #9a7f60);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.82rem;
  font-weight: 700;
}

.user-text {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 0.78rem;
  font-weight: 600;
}

.user-role {
  font-size: 0.68rem;
  color: var(--color-text-muted, #6f6356);
}

/* Sub-navegación */
.portal-nav {
  background: var(--color-bg-sidebar, #ede6db);
  border-top: 1px solid var(--color-border-subtle, #ede5d8);
}

.nav-container {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 1.75rem;
  display: flex;
  gap: 0.5rem;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.75rem 1.25rem;
  font-size: 0.84rem;
  font-weight: 500;
  color: var(--color-text-muted, #6f6356);
  text-decoration: none;
  border-bottom: 2px solid transparent;
  transition: all 0.2s ease;
}

.nav-item:hover {
  color: var(--color-text-main, #2b2520);
  background: rgba(255, 255, 255, 0.4);
}

.nav-item.active {
  color: var(--color-primary, #352f2a);
  font-weight: 700;
  border-bottom-color: var(--color-primary, #352f2a);
  background: var(--color-bg-surface, #ffffff);
}

/* Área de contenido */
.portal-main {
  flex: 1;
  padding: 2rem 1.75rem;
}

.main-container {
  max-width: 1440px;
  margin: 0 auto;
}

/* Footer */
.portal-footer {
  background: var(--color-bg-surface, #ffffff);
  border-top: 1px solid var(--color-border, #e4dacb);
  padding: 1.25rem 1.75rem;
  font-size: 0.75rem;
  color: var(--color-text-muted, #6f6356);
}

.footer-container {
  max-width: 1440px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
}

.footer-legal {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.footer-sep {
  opacity: 0.5;
}

.footer-contact {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-weight: 500;
}
</style>
