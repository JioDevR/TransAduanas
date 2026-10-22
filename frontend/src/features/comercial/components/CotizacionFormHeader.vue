<template>
  <div class="form-header-bar">
    <div class="header-titles">
      <button
        type="button"
        class="btn-back-link"
        @click="$emit('cancel')"
        title="Regresar al panel de cotizaciones"
      >
        <i class="pi pi-arrow-left"></i>
        <span>Volver al Registro de Cotizaciones</span>
      </button>

      <div class="kicker-row">
        <span class="badge-kicker">PROPUESTAS ADUANERAS // EMISIÓN COMERCIAL</span>
        <span :class="['mode-pill', isEditMode ? 'mode-edit' : 'mode-create']">
          <i :class="isEditMode ? 'pi pi-pencil' : 'pi pi-plus'"></i>
          <span>{{ isEditMode ? 'MODO EDICIÓN' : 'MODO REGISTRO' }}</span>
        </span>
      </div>

      <h2 class="form-main-title">
        {{ isEditMode ? `Editar Cotización: ${codigoCotizacion}` : 'Emitir Nueva Cotización Comercial' }}
      </h2>
      <p class="form-main-desc">
        Estructure la propuesta comercial paso a paso: asigne el cliente o prospecto, especifique el régimen de aduanas y liquide los costos estimados de flete, honorarios y aranceles.
      </p>
    </div>

    <div class="header-actions">
      <button
        type="button"
        class="btn-outline-warm"
        @click="$emit('cancel')"
        :disabled="submitting"
      >
        <span>Cancelar</span>
      </button>
      <button
        type="button"
        class="btn-espresso"
        @click="$emit('submit')"
        :disabled="submitting || !isFormReady"
      >
        <i v-if="submitting" class="pi pi-spin pi-spinner"></i>
        <i v-else class="pi pi-check"></i>
        <span>{{ isEditMode ? 'Guardar Cambios' : 'Emitir Cotización' }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  isEditMode: {
    type: Boolean,
    default: false
  },
  codigoCotizacion: {
    type: String,
    default: ''
  },
  submitting: {
    type: Boolean,
    default: false
  },
  isFormReady: {
    type: Boolean,
    default: false
  }
})

defineEmits(['cancel', 'submit'])
</script>

<style scoped>
.form-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-5, 1.25rem);
  padding: var(--space-5, 1.25rem) var(--space-6, 1.5rem);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  box-shadow: var(--shadow-subtle);
  flex-wrap: wrap;
}

.header-titles {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  max-width: 820px;
}

.btn-back-link {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: none;
  border: none;
  color: var(--color-text-muted);
  font-size: 0.725rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  cursor: pointer;
  padding: 0;
  margin-bottom: 0.15rem;
  transition: color var(--transition-fast);
}

.btn-back-link:hover {
  color: var(--color-primary);
}

.kicker-row {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  flex-wrap: wrap;
}

.badge-kicker {
  font-size: 0.625rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  padding: 0.15rem 0.5rem;
  border-radius: var(--radius-xs, 0.25rem);
  border: 1px solid var(--color-border-subtle);
}

.mode-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.65rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  padding: 0.15rem 0.55rem;
  border-radius: var(--radius-pill);
}

.mode-create {
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border: 1px solid var(--color-info-border);
}

.mode-edit {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.form-main-title {
  font-family: var(--font-sans);
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--color-text-main);
  letter-spacing: -0.015em;
  line-height: 1.25;
  margin: 0.15rem 0 0;
}

.form-main-desc {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  line-height: 1.45;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.55rem 1.15rem;
  font-size: 0.84rem;
  font-weight: 600;
  border-radius: var(--radius-md, 0.5rem);
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-outline-warm:hover:not(:disabled) {
  border-color: var(--color-border-strong);
  background: var(--color-bg-subtle);
}

.btn-espresso {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.55rem 1.25rem;
  font-size: 0.84rem;
  font-weight: 600;
  border-radius: var(--radius-md, 0.5rem);
  border: none;
  background: var(--color-primary);
  color: var(--color-primary-text);
  cursor: pointer;
  transition: all var(--transition-fast);
  box-shadow: 0 2px 6px rgba(53, 47, 42, 0.2);
}

.btn-espresso:hover:not(:disabled) {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
}

.btn-espresso:disabled,
.btn-outline-warm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}
</style>
