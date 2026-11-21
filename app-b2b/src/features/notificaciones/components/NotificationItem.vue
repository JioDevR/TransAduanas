<template>
  <article
    class="notif-item"
    :class="{ 'is-unread': !notification.leido, 'is-clickable': hasAction }"
    :tabindex="hasAction ? 0 : undefined"
    :role="hasAction ? 'button' : undefined"
    @click="handleItemClick"
    @keydown.enter.prevent="handleItemClick"
    @keydown.space.prevent="handleItemClick"
  >
    <div class="notif-icon-wrap" :class="meta.color" aria-hidden="true">
      <i :class="meta.icon"></i>
    </div>

    <div class="notif-body">
      <div class="notif-meta-row">
        <span class="notif-category-pill" :class="meta.color">{{ meta.label }}</span>
        <span class="notif-time font-mono" :title="exactDate">{{ relativeTime }}</span>
        <span
          v-if="!notification.leido"
          class="notif-unread-dot"
          title="No leída"
          aria-label="Notificación no leída"
        ></span>
      </div>

      <h4 class="notif-title">{{ notification.titulo }}</h4>
      <p class="notif-message" :class="{ 'is-expanded': expanded }">
        {{ notification.mensaje }}
      </p>

      <div class="notif-actions-row">
        <span v-if="normalizedLink" class="notif-link-hint">
          <span>Ver detalle</span>
          <i class="pi pi-arrow-right"></i>
        </span>
        <button
          v-else-if="isLongMessage"
          type="button"
          class="notif-inline-btn"
          @click.stop="expanded = !expanded"
        >
          {{ expanded ? 'Ver menos' : 'Leer completo' }}
        </button>

        <button
          v-if="!notification.leido"
          type="button"
          class="notif-mark-btn"
          title="Marcar como leída"
          aria-label="Marcar como leída"
          @click.stop="emit('mark-read', notification)"
        >
          <i class="pi pi-check"></i>
          <span>Marcar leída</span>
        </button>
      </div>
    </div>
  </article>
</template>

<script setup>
import { ref, computed } from 'vue'
import { formatDateTime } from '@/shared/utils/formatters'
import {
  getNotificationMeta,
  normalizeNotificationLink,
  formatRelativeTime
} from '../utils/notificationMeta'

const props = defineProps({
  notification: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['select', 'mark-read'])

const expanded = ref(false)

const meta = computed(() => getNotificationMeta(props.notification.tipo))
const normalizedLink = computed(() => normalizeNotificationLink(props.notification.enlace))
const relativeTime = computed(() => formatRelativeTime(props.notification.fechaCreacion))
const exactDate = computed(() => formatDateTime(props.notification.fechaCreacion))
const isLongMessage = computed(() => (props.notification.mensaje?.length || 0) > 105)
const hasAction = computed(() => Boolean(normalizedLink.value) || !props.notification.leido)

function handleItemClick() {
  if (normalizedLink.value) {
    emit('select', { notification: props.notification, link: normalizedLink.value })
    return
  }
  if (!props.notification.leido) {
    emit('mark-read', props.notification)
  } else if (isLongMessage.value) {
    expanded.value = !expanded.value
  }
}
</script>

<style scoped>
.notif-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.85rem 1rem;
  border-bottom: 1px solid var(--color-border-subtle);
  background: var(--color-bg-surface);
  transition: background-color var(--transition-fast);
  position: relative;
}

.notif-item:last-child {
  border-bottom: none;
}

.notif-item.is-clickable {
  cursor: pointer;
}

.notif-item:hover {
  background: var(--color-bg-subtle);
}

.notif-item:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: -2px;
}

.notif-item.is-unread {
  background: color-mix(in srgb, var(--color-primary) 5%, var(--color-bg-surface));
}

.notif-item.is-unread:hover {
  background: color-mix(in srgb, var(--color-primary) 9%, var(--color-bg-surface));
}

.notif-icon-wrap {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-lg, 0.75rem);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.92rem;
  flex-shrink: 0;
  margin-top: 0.1rem;
}

.notif-icon-wrap.neutral {
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
}

.notif-icon-wrap.success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
}

.notif-icon-wrap.accent {
  background: var(--color-bg-muted);
  color: var(--color-accent-kicker);
}

.notif-icon-wrap.info {
  background: var(--color-info-bg);
  color: var(--color-info-text);
}

.notif-icon-wrap.warning {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.notif-icon-wrap.danger {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
}

.notif-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.28rem;
}

.notif-meta-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.notif-category-pill {
  font-size: 0.625rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 0.14rem 0.5rem;
  border-radius: var(--radius-pill);
}

.notif-category-pill.neutral {
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
}

.notif-category-pill.success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
}

.notif-category-pill.accent {
  background: var(--color-bg-muted);
  color: var(--color-accent-kicker);
}

.notif-category-pill.info {
  background: var(--color-info-bg);
  color: var(--color-info-text);
}

.notif-category-pill.warning {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.notif-category-pill.danger {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
}

.notif-time {
  font-size: 0.6875rem;
  color: var(--color-text-subtle);
  margin-left: auto;
  white-space: nowrap;
}

.notif-unread-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-primary);
  flex-shrink: 0;
}

.notif-title {
  margin: 0;
  font-size: 0.825rem;
  font-weight: 600;
  line-height: 1.3;
  color: var(--color-text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notif-item.is-unread .notif-title {
  font-weight: 700;
}

.notif-message {
  margin: 0;
  font-size: 0.775rem;
  line-height: 1.42;
  color: var(--color-text-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notif-message.is-expanded {
  display: block;
  -webkit-line-clamp: unset;
}

.notif-actions-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-top: 0.15rem;
  min-height: 20px;
}

.notif-link-hint {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--color-primary);
}

.notif-link-hint i {
  font-size: 0.65rem;
  transition: transform var(--transition-fast);
}

.notif-item.is-clickable:hover .notif-link-hint i {
  transform: translateX(2px);
}

.notif-inline-btn,
.notif-mark-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  border: none;
  background: transparent;
  padding: 0.18rem 0.45rem;
  border-radius: var(--radius-pill);
  font-size: 0.71rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color var(--transition-fast), color var(--transition-fast);
}

.notif-inline-btn {
  color: var(--color-text-muted);
}

.notif-inline-btn:hover {
  background: var(--color-bg-muted);
  color: var(--color-text-main);
}

.notif-mark-btn {
  color: var(--color-text-muted);
  margin-left: auto;
}

.notif-mark-btn:hover {
  background: var(--color-bg-muted);
  color: var(--color-primary);
}

.notif-mark-btn i {
  font-size: 0.66rem;
}
</style>
