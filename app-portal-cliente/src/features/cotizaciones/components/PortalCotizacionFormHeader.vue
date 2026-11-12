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
        <span>Volver a Mis Cotizaciones</span>
      </button>

      <div class="kicker-row">
        <span class="badge-kicker">PORTAL B2B // SOLICITUD DE COTIZACIÓN</span>
        <span class="mode-pill mode-create">
          <i class="pi pi-file-edit"></i>
          <span>NUEVA SOLICITUD</span>
        </span>
      </div>

      <h2 class="form-main-title">
        Solicitar Nueva Cotización Aduanera
      </h2>
      <p class="form-main-desc">
        Estructure su solicitud logística paso a paso: especifique el régimen y la ruta aduanera, adjunte la documentación técnica de sustento y declare los valores comerciales estimados para su liquidación oficial.
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
        <i v-else class="pi pi-send"></i>
        <span>{{ submitting ? 'Enviando Solicitud...' : 'Enviar Solicitud a Evaluación' }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
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
/* Los estilos base (.form-header-bar, .btn-espresso, .btn-outline-warm, .mode-pill.mode-create)
   provienen de design-system.css y style.css. */
.mode-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}
</style>
