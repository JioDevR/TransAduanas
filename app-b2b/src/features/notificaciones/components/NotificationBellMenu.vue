<template>
  <div class="notif-bell-wrapper">
    <Button
      type="button"
      icon="pi pi-bell"
      severity="secondary"
      variant="text"
      rounded
      :badge="notificationStore.badgeLabel"
      badgeSeverity="danger"
      aria-label="Centro de Notificaciones"
      aria-haspopup="dialog"
      :aria-expanded="isOpen"
      title="Notificaciones"
      @click="toggleMenu"
    />

    <Popover
      ref="popoverRef"
      class="notif-popover-root"
      :pt="{
        root: { style: 'border-radius: 1.25rem; overflow: hidden;' },
        content: { style: 'padding: 0; border-radius: 1.25rem; overflow: hidden;' }
      }"
      @show="handleShow"
      @hide="handleHide"
    >
      <section class="notif-dropdown" aria-label="Notificaciones recientes">
        <!-- Header fijo del menú desplegable -->
        <header class="notif-dropdown-header">
          <div class="notif-header-title-group">
            <h3 class="notif-header-title">Notificaciones</h3>
            <span
              v-if="notificationStore.unreadCount > 0"
              class="notif-header-badge font-mono"
            >
              {{ notificationStore.unreadCount }}
            </span>
          </div>

          <div class="notif-header-actions">
            <button
              type="button"
              class="notif-icon-action"
              :disabled="notificationStore.loading"
              title="Actualizar notificaciones"
              aria-label="Actualizar notificaciones"
              @click="refreshNotifications"
            >
              <i :class="notificationStore.loading ? 'pi pi-spin pi-spinner' : 'pi pi-refresh'"></i>
            </button>

            <button
              v-if="notificationStore.unreadCount > 0"
              type="button"
              class="notif-mark-all-btn"
              :disabled="notificationStore.loadingAction || notificationStore.loading"
              @click="handleMarkAllRead"
            >
              <i class="pi pi-check-double"></i>
              <span>Marcar todas</span>
            </button>
          </div>
        </header>

        <!-- Selector rápido: Todas / No leídas -->
        <div class="notif-filter-tabs" role="tablist" aria-label="Filtrar notificaciones">
          <button
            type="button"
            role="tab"
            :aria-selected="notificationStore.activeFilter === 'ALL'"
            class="notif-tab-btn"
            :class="{ active: notificationStore.activeFilter === 'ALL' }"
            @click="setFilter('ALL')"
          >
            <span>Todas</span>
            <span class="notif-tab-count font-mono">{{ notificationStore.items.length }}</span>
          </button>

          <button
            type="button"
            role="tab"
            :aria-selected="notificationStore.activeFilter === 'UNREAD'"
            class="notif-tab-btn"
            :class="{ active: notificationStore.activeFilter === 'UNREAD' }"
            @click="setFilter('UNREAD')"
          >
            <span>No leídas</span>
            <span class="notif-tab-count font-mono">{{ notificationStore.unreadCount }}</span>
          </button>
        </div>

        <!-- Lista scrolleable de notificaciones -->
        <div class="notif-dropdown-body">
          <div
            v-if="notificationStore.loading && !notificationStore.hasLoaded"
            class="notif-state-box"
          >
            <i class="pi pi-spin pi-spinner notif-state-icon"></i>
            <p class="notif-state-text">Cargando notificaciones...</p>
          </div>

          <div
            v-else-if="notificationStore.error && notificationStore.items.length === 0"
            class="notif-state-box"
          >
            <i class="pi pi-exclamation-circle notif-state-icon is-error"></i>
            <p class="notif-state-title">No pudimos cargar tus avisos</p>
            <p class="notif-state-text">{{ notificationStore.error }}</p>
            <button type="button" class="notif-retry-btn" @click="refreshNotifications">
              Reintentar
            </button>
          </div>

          <div
            v-else-if="visibleItems.length === 0"
            class="notif-state-box"
          >
            <div class="notif-empty-circle">
              <i class="pi pi-inbox"></i>
            </div>
            <p class="notif-state-title">
              {{
                notificationStore.activeFilter === 'UNREAD'
                  ? 'Estás al día'
                  : 'Sin notificaciones'
              }}
            </p>
            <p class="notif-state-text">
              {{
                notificationStore.activeFilter === 'UNREAD'
                  ? 'No tienes notificaciones pendientes por leer.'
                  : 'Los avisos de cotizaciones, despachos y pagos aparecerán aquí.'
              }}
            </p>
          </div>

          <div v-else class="notif-list">
            <NotificationItem
              v-for="item in visibleItems"
              :key="item.idNotificacion"
              :notification="item"
              @select="handleSelectItem"
              @mark-read="handleMarkOneRead"
            />
          </div>
        </div>

        <!-- Pie compacto con carga progresiva si existen muchos avisos -->
        <footer v-if="hasMore || notificationStore.items.length > 0" class="notif-dropdown-footer">
          <button
            v-if="hasMore"
            type="button"
            class="notif-load-more-btn"
            @click="visibleLimit += 15"
          >
            Mostrar más ({{ remainingCount }} restantes)
          </button>
          <span v-else class="notif-footer-caption">
            Mostrando {{ visibleItems.length }} de {{ notificationStore.filteredItems.length }}
          </span>
        </footer>
      </section>
    </Popover>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import Popover from 'primevue/popover'
import { useNotification } from '@/shared/composables/useNotification'
import { useNotificationStore } from '../stores/notificationStore'
import NotificationItem from './NotificationItem.vue'

const INITIAL_VISIBLE_LIMIT = 15

const emit = defineEmits(['open'])

const router = useRouter()
const notificationStore = useNotificationStore()
const { showError } = useNotification()

const popoverRef = ref(null)
const isOpen = ref(false)
const visibleLimit = ref(INITIAL_VISIBLE_LIMIT)

const visibleItems = computed(() => {
  return notificationStore.filteredItems.slice(0, visibleLimit.value)
})

const hasMore = computed(() => {
  return notificationStore.filteredItems.length > visibleLimit.value
})

const remainingCount = computed(() => {
  return Math.max(0, notificationStore.filteredItems.length - visibleLimit.value)
})

function toggleMenu(event) {
  popoverRef.value?.toggle(event)
}

function hide() {
  popoverRef.value?.hide()
}

async function handleShow() {
  isOpen.value = true
  visibleLimit.value = INITIAL_VISIBLE_LIMIT
  emit('open')
  try {
    await notificationStore.fetchNotifications({ silent: notificationStore.hasLoaded })
  } catch {
    // El estado de error ya se refleja dentro del desplegable si no hay caché previa
  }
}

function handleHide() {
  isOpen.value = false
}

function setFilter(filter) {
  notificationStore.setFilter(filter)
  visibleLimit.value = INITIAL_VISIBLE_LIMIT
}

async function refreshNotifications() {
  try {
    await notificationStore.fetchNotifications({ silent: false })
  } catch {
    showError('No se pudieron actualizar las notificaciones')
  }
}

async function handleMarkOneRead(notification) {
  const result = await notificationStore.markAsRead(notification.idNotificacion)
  if (!result.success) {
    showError(result.message)
  }
}

async function handleMarkAllRead() {
  const result = await notificationStore.markAllAsRead()
  if (!result.success) {
    showError(result.message)
  }
}

async function handleSelectItem({ notification, link }) {
  if (!notification.leido) {
    await handleMarkOneRead(notification)
  }
  hide()
  if (link) {
    router.push(link)
  }
}

defineExpose({ hide })
</script>

<style scoped>
.notif-bell-wrapper {
  display: inline-flex;
  align-items: center;
}

.notif-dropdown {
  width: min(390px, calc(100vw - 1.5rem));
  display: flex;
  flex-direction: column;
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  border-radius: var(--radius-2xl, 1.25rem);
  overflow: hidden;
}

.notif-dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.85rem 1rem;
  border-bottom: 1px solid var(--color-border-subtle);
  background: var(--color-bg-surface);
}

.notif-header-title-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.notif-header-title {
  margin: 0;
  font-size: 0.94rem;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--color-text-main);
}

.notif-header-badge {
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 0.1rem 0.45rem;
  border-radius: var(--radius-pill);
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border: 1px solid var(--color-danger-border);
}

.notif-header-actions {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.notif-icon-action {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-pill);
  border: 1px solid transparent;
  background: transparent;
  color: var(--color-text-muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 0.8rem;
  transition: background-color var(--transition-fast), color var(--transition-fast);
}

.notif-icon-action:hover:not(:disabled) {
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
}

.notif-icon-action:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.notif-mark-all-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.28rem 0.68rem;
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-border);
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color var(--transition-fast), border-color var(--transition-fast);
}

.notif-mark-all-btn:hover:not(:disabled) {
  background: var(--color-bg-muted);
  border-color: var(--color-border-strong);
}

.notif-mark-all-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.notif-filter-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.35rem;
  padding: 0.5rem 0.75rem;
  background: var(--color-bg-subtle);
  border-bottom: 1px solid var(--color-border-subtle);
}

.notif-tab-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  padding: 0.38rem 0.65rem;
  border-radius: var(--radius-pill);
  border: 1px solid transparent;
  background: transparent;
  color: var(--color-text-muted);
  font-size: 0.76rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.notif-tab-btn:hover {
  color: var(--color-text-main);
}

.notif-tab-btn.active {
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  border-color: var(--color-border);
  box-shadow: var(--shadow-subtle);
}

.notif-tab-count {
  font-size: 0.68rem;
  padding: 0.05rem 0.35rem;
  border-radius: var(--radius-pill);
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
}

.notif-tab-btn.active .notif-tab-count {
  color: var(--color-text-main);
}

.notif-dropdown-body {
  max-height: 390px;
  overflow-y: auto;
  background: var(--color-bg-surface);
}

.notif-list {
  display: flex;
  flex-direction: column;
}

.notif-state-box {
  padding: 2.25rem 1.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 0.45rem;
}

.notif-empty-circle {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--color-bg-subtle);
  color: var(--color-accent-kicker);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  margin-bottom: 0.2rem;
}

.notif-state-icon {
  font-size: 1.35rem;
  color: var(--color-text-muted);
}

.notif-state-icon.is-error {
  color: var(--color-danger-text);
}

.notif-state-title {
  margin: 0;
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.notif-state-text {
  margin: 0;
  font-size: 0.78rem;
  line-height: 1.4;
  color: var(--color-text-muted);
  max-width: 280px;
}

.notif-retry-btn {
  margin-top: 0.4rem;
  padding: 0.38rem 0.95rem;
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-border);
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
  font-size: 0.76rem;
  font-weight: 600;
  cursor: pointer;
}

.notif-retry-btn:hover {
  background: var(--color-bg-muted);
}

.notif-dropdown-footer {
  padding: 0.55rem 1rem;
  border-top: 1px solid var(--color-border-subtle);
  background: var(--color-bg-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
}

.notif-load-more-btn {
  border: none;
  background: transparent;
  color: var(--color-primary);
  font-size: 0.76rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0.25rem 0.65rem;
  border-radius: var(--radius-pill);
}

.notif-load-more-btn:hover {
  background: var(--color-bg-muted);
}

.notif-footer-caption {
  font-size: 0.7rem;
  color: var(--color-text-subtle);
}
</style>
