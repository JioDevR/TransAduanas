<template>
  <Dialog
    :visible="visible"
    modal
    :closable="false"
    :dismissable-mask="closeOnBackdrop && !busy"
    :style="{ width: maxWidth, maxWidth: '95vw' }"
    class="base-modal-dialog"
    :pt="{
      root: { class: 'base-modal__window' },
      header: { class: 'base-modal__header' },
      content: { class: 'base-modal__content-wrap' }
    }"
    @update:visible="(val) => { if (!val) requestClose() }"
  >
    <template #header>
      <div class="base-modal__header-left">
        <div v-if="icon" class="base-modal__icon">
          <i :class="icon"></i>
        </div>
        <div>
          <h2 :id="titleId" class="base-modal__title">{{ title }}</h2>
          <p v-if="subtitle" class="base-modal__subtitle">{{ subtitle }}</p>
        </div>
      </div>
      <button
        type="button"
        class="base-modal__close"
        title="Cerrar"
        :disabled="busy"
        @click="requestClose"
      >
        <i class="pi pi-times"></i>
      </button>
    </template>

    <!-- Alerta de error -->
    <div v-if="errorMessage" class="base-modal__alert" role="alert">
      <i class="pi pi-exclamation-triangle"></i>
      <span>{{ errorMessage }}</span>
    </div>

    <!-- Contenido (normalmente un <form class="form-body">) -->
    <slot />
  </Dialog>
</template>

<script setup>
/**
 * Contenedor modal reutilizable del SGA basado en PrimeVue Dialog.
 * Provee: backdrop blur, encabezado (icono/título/subtítulo), botón cerrar,
 * alerta de error, cierre con ESC y estilos compartidos de formulario
 * (.form-body, .form-grid, .field-item, .text-input, .modal-actions, etc.).
 */
import { useId } from 'vue'
import Dialog from 'primevue/dialog'

const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  icon: { type: String, default: '' },
  maxWidth: { type: String, default: '640px' },
  errorMessage: { type: String, default: '' },
  // Mientras es true, el modal no se puede cerrar (ej. guardando)
  busy: { type: Boolean, default: false },
  closeOnBackdrop: { type: Boolean, default: true }
})

const emit = defineEmits(['close'])

const titleId = useId()

const requestClose = () => {
  if (!props.busy) emit('close')
}
</script>

<style>
/* Estilos integrados con PrimeVue Dialog y variables semánticas SGA */
.base-modal-dialog.p-dialog {
  padding: 0 !important;
  background: var(--color-bg-surface) !important;
  border: 1px solid var(--color-border) !important;
  border-radius: var(--radius-xl) !important;
  box-shadow: var(--shadow-elevated) !important;
  overflow: hidden;
}

.base-modal-dialog .p-dialog-header {
  padding: 0 !important;
  border: none !important;
  background: transparent !important;
}

.base-modal-dialog .p-dialog-content {
  padding: 0 !important;
  background: var(--color-bg-surface) !important;
}

/* ---------- Encabezado ---------- */
.base-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 1.5rem 1.75rem 1.25rem;
  border-bottom: 1px solid var(--color-border-subtle);
  background: var(--color-bg-subtle);
}

.base-modal__header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.base-modal__icon {
  width: 44px;
  height: 44px;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-size: 1.2rem;
  flex-shrink: 0;
}

.base-modal__title {
  font-size: 1.2rem;
  font-weight: 700;
  margin: 0;
  color: var(--color-text-main);
}

.base-modal__subtitle {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  margin: 0.2rem 0 0;
}

.base-modal__close {
  background: transparent;
  border: none;
  color: var(--color-text-subtle);
  font-size: 1.1rem;
  cursor: pointer;
  padding: 0.5rem;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.base-modal__close:hover:not(:disabled) {
  color: var(--color-text-main);
  background: var(--color-bg-muted);
}

.base-modal__close:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ---------- Alerta ---------- */
.base-modal__alert {
  margin: 1.25rem 1.75rem 0;
  padding: 0.75rem 1rem;
  background: var(--color-danger-bg);
  border: 1px solid var(--color-danger-border);
  border-radius: var(--radius-sm);
  color: var(--color-danger-text);
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

/* ---------- Formulario compartido ---------- */
.base-modal-dialog .form-body {
  padding: 1.5rem 1.75rem;
}

.base-modal-dialog .form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.25rem;
}

.base-modal-dialog .full-width {
  grid-column: span 2;
}

.base-modal-dialog .field-item {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.base-modal-dialog .field-item label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.base-modal-dialog .text-input,
.base-modal-dialog .select-input,
.base-modal-dialog .textarea-input {
  width: 100%;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.65rem 0.85rem;
  color: var(--color-text-main);
  font-size: 0.875rem;
  font-family: inherit;
  outline: none;
  transition: all var(--transition-fast);
}

.base-modal-dialog .text-input:focus,
.base-modal-dialog .select-input:focus,
.base-modal-dialog .textarea-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(214, 180, 118, 0.15);
}

.base-modal-dialog .text-input:disabled,
.base-modal-dialog .select-input:disabled,
.base-modal-dialog .textarea-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: var(--color-bg-muted);
}

.base-modal-dialog .field-hint {
  font-size: 0.6875rem;
  color: var(--color-text-subtle);
}

/* ---------- Acciones ---------- */
.base-modal-dialog .modal-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.75rem;
  padding-top: 1.25rem;
  border-top: 1px solid var(--color-border-subtle);
}

.base-modal-dialog .btn-modal-cancel {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  padding: 0.6rem 1.25rem;
  border-radius: var(--radius-md);
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.base-modal-dialog .btn-modal-cancel:hover:not(:disabled) {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.base-modal-dialog .btn-modal-save {
  background: var(--color-primary);
  border: none;
  color: var(--color-primary-text);
  padding: 0.6rem 1.4rem;
  border-radius: var(--radius-md);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  box-shadow: 0 2px 8px rgba(53, 47, 42, 0.2);
  transition: all var(--transition-fast);
}

.base-modal-dialog .btn-modal-save:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.base-modal-dialog .btn-modal-save:disabled,
.base-modal-dialog .btn-modal-cancel:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .base-modal-dialog .form-grid {
    grid-template-columns: 1fr;
  }

  .base-modal-dialog .full-width {
    grid-column: span 1;
  }
}
</style>
