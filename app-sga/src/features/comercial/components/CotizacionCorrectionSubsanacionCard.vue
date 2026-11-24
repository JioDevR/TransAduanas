<template>
  <section class="sga-card-surface sga-subsanacion-card">
    <div class="sga-client-summary-row">
      <div class="sga-client-info">
        <span class="sga-form-label">Cliente Asignado</span>
        <strong class="sga-client-title">{{ form.clienteRazonSocial || 'Sin Razón Social' }}</strong>
        <span class="sga-client-doc font-mono">
          Doc: {{ form.clienteNumeroDocumento || 'S/D' }}
        </span>
      </div>

      <div v-if="clientes && clientes.length > 0" class="sga-form-field sga-client-reassign">
        <label for="corr-cliente-select" class="sga-form-label">Reasignar Cliente (Opcional)</label>
        <Select
          inputId="corr-cliente-select"
          v-model="form.idCliente"
          :options="clientes"
          optionLabel="razonSocial"
          optionValue="idCliente"
          filter
          placeholder="Seleccionar cliente registrado"
          size="small"
          fluid
          :disabled="submitting"
          @change="$emit('client-reassign')"
        />
      </div>
    </div>

    <div class="sga-form-field">
      <label for="corr-nota-subsanacion" class="sga-form-label">Nota de Subsanación Comercial</label>
      <Textarea
        id="corr-nota-subsanacion"
        v-model="notaModel"
        rows="3"
        fluid
        :disabled="submitting"
        placeholder="Detalle al Supervisor Aduanero las correcciones efectuadas (ej. ajuste de flete, partida NANDINA o documentos actualizados)..."
      />
      <small class="sga-field-hint">
        Esta nota se añadirá al historial de la cotización para agilizar la reevaluación técnica.
      </small>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'

const props = defineProps({
  form: { type: Object, required: true },
  clientes: { type: Array, default: () => [] },
  notaSubsanacion: { type: String, default: '' },
  submitting: { type: Boolean, default: false }
})

const emit = defineEmits(['update:notaSubsanacion', 'client-reassign'])

const notaModel = computed({
  get: () => props.notaSubsanacion,
  set: (val) => emit('update:notaSubsanacion', val)
})
</script>

<style scoped>
.sga-subsanacion-card {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.sga-client-summary-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(240px, 320px);
  gap: 1rem 1.25rem;
  align-items: start;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--color-border);
}

.sga-client-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 0;
}

.sga-client-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.sga-client-doc {
  font-size: 0.78rem;
  color: var(--color-text-subtle);
}

.sga-field-hint {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

@media (max-width: 900px) {
  .sga-client-summary-row {
    grid-template-columns: 1fr;
  }
}
</style>
