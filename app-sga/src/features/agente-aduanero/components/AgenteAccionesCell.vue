<template>
  <div class="actions-wrapper">
    <!-- ============================================================== -->
    <!-- ACCIONES ESPECÍFICAS DE EXPORTACIÓN                            -->
    <!-- ============================================================== -->
    <template v-if="isExport">
      <!-- 1. Pedir Recojo en Planta (Transporte Terrestre SGA) -->
      <button
        v-if="expediente.servicioTransporte && !expediente.idDespachoTransporte && (expediente.estadoExpediente === 'ASIGNADO' || expediente.estadoExpediente === 'EN_TRAMITE')"
        type="button"
        class="action-btn-outline approve"
        title="Solicitar Recojo de Carga en Planta del Exportador"
        @click="$emit('recojo-exportacion', expediente)"
      >
        <i class="pi pi-truck"></i>
        <span>Pedir Recojo</span>
      </button>

      <!-- 2. Confirmar Ingreso a Almacén / Zona Primaria -->
      <button
        v-if="expediente.estadoExpediente === 'ASIGNADO' || (expediente.estadoExpediente === 'EN_TRAMITE' && !expediente.fechaArriboAduana)"
        type="button"
        class="action-btn-outline"
        title="Confirmar Ingreso a Depósito Temporal / Zona Primaria (VGM)"
        @click="$emit('ingreso-origen', expediente)"
      >
        <i class="pi pi-inbox"></i>
        <span>Ingreso Almacén</span>
      </button>

      <!-- 3. Diligencia / Aforo Físico SUNAT (Canal Naranja/Rojo) -->
      <button
        v-if="(expediente.canalSunat === 'ROJO' || expediente.canalSunat === 'NARANJA' || expediente.estadoExpediente === 'INSPECCION') && expediente.estadoExpediente !== 'CERRADO' && expediente.estadoExpediente !== 'ANULADO'"
        type="button"
        class="action-btn-outline observe"
        title="Registrar Acta de Diligencia / Aforo SUNAT"
        @click="$emit('diligencia', expediente)"
      >
        <i class="pi pi-search-plus"></i>
        <span>Diligencia</span>
      </button>

      <!-- 4. Confirmar Embarque Internacional -->
      <button
        v-if="expediente.estadoExpediente === 'LEVANTE_AUTORIZADO' && !isEmbarcado"
        type="button"
        class="action-btn-outline approve"
        title="Confirmar Embarque a Bordo de la Nave"
        @click="$emit('embarque', expediente)"
      >
        <i class="pi pi-send"></i>
        <span>Confirmar Embarque</span>
      </button>

      <!-- 5. Confirmar Entrega en Destino Exterior -->
      <button
        v-if="expediente.estadoExpediente === 'LEVANTE_AUTORIZADO' && isEmbarcado && !isRecepcionado"
        type="button"
        class="action-btn-outline approve"
        title="Confirmar Recepción Conforme en Destino Exterior"
        @click="$emit('recepcion-exterior', expediente)"
      >
        <i class="pi pi-check-circle"></i>
        <span>Entrega Exterior</span>
      </button>

      <!-- 6. Actualizar Tránsito y Nave -->
      <button
        v-if="expediente.estadoExpediente !== 'CERRADO'"
        type="button"
        class="action-btn-outline"
        title="Actualizar Datos de Salida (Nave, Contenedor, Precinto)"
        @click="$emit('transito', expediente)"
      >
        <i class="pi pi-globe"></i>
        <span>Tránsito</span>
      </button>
    </template>

    <!-- ============================================================== -->
    <!-- ACCIONES ESPECÍFICAS DE IMPORTACIÓN                            -->
    <!-- ============================================================== -->
    <template v-else>
      <!-- 1. Actualizar Tránsito y Datos de Embarque -->
      <button
        v-if="expediente.estadoExpediente !== 'CERRADO'"
        type="button"
        class="action-btn-outline"
        title="Actualizar Datos de Embarque (Nave, Contenedor, Precinto, ETA)"
        @click="$emit('transito', expediente)"
      >
        <i class="pi pi-globe"></i>
        <span>Tránsito</span>
      </button>

      <!-- 2. Registrar Diligencia / Intervención de Campo (Canal Naranja/Rojo) -->
      <button
        v-if="(expediente.canalSunat === 'ROJO' || expediente.canalSunat === 'NARANJA' || expediente.estadoExpediente === 'INSPECCION') && expediente.estadoExpediente !== 'CERRADO' && expediente.estadoExpediente !== 'ANULADO'"
        type="button"
        class="action-btn-outline observe"
        title="Registrar Acta de Diligencia / Aforo Físico SUNAT"
        @click="$emit('diligencia', expediente)"
      >
        <i class="pi pi-search-plus"></i>
        <span>Diligencia</span>
      </button>

      <!-- 3. Solicitar Envío Terrestre a Destino (Transporte SGA con Levante) -->
      <button
        v-if="expediente.estadoExpediente === 'LEVANTE_AUTORIZADO' && expediente.servicioTransporte && !expediente.idDespachoTransporte"
        type="button"
        class="action-btn-outline approve"
        title="Solicitar Traslado Terrestre a Transporte y Flota"
        @click="$emit('solicitar-transporte', expediente)"
      >
        <i class="pi pi-send"></i>
        <span>Pedir Envío</span>
      </button>

      <!-- 4. Acta de Retiro Directo en Terminal (Transporte propio del Cliente) -->
      <button
        v-if="expediente.estadoExpediente === 'LEVANTE_AUTORIZADO' && !expediente.servicioTransporte"
        type="button"
        class="action-btn-outline"
        title="Registrar Acta de Retiro en Terminal por el Cliente"
        @click="$emit('retiro-directo', expediente)"
      >
        <i class="pi pi-building"></i>
        <span>Acta Retiro</span>
      </button>

      <!-- 5. Ingreso a Almacén / Balanza VGM -->
      <button
        v-if="expediente.estadoExpediente !== 'CERRADO'"
        type="button"
        class="action-btn-outline"
        title="Registrar Ingreso a Depósito Temporal / Balanza VGM"
        @click="$emit('ingreso-almacen', expediente)"
      >
        <i class="pi pi-box"></i>
        <span>Balanza VGM</span>
      </button>
    </template>

    <!-- ============================================================== -->
    <!-- COMUNES: Documentos Adjuntos y Detalle                         -->
    <!-- ============================================================== -->
    <button
      type="button"
      class="action-btn-icon"
      title="Gestionar Documentos Aduaneros (BL, DAM, Factura)"
      @click="$emit('documentos', expediente)"
    >
      <i class="pi pi-paperclip"></i>
    </button>

    <button
      type="button"
      class="action-btn-primary"
      title="Ver Ficha y Auditoría Completa del Expediente"
      @click="$emit('detalle', expediente)"
    >
      <i class="pi pi-eye"></i>
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  expediente: {
    type: Object,
    required: true
  }
})

defineEmits([
  'transito',
  'diligencia',
  'solicitar-transporte',
  'retiro-directo',
  'ingreso-almacen',
  'recojo-exportacion',
  'ingreso-origen',
  'embarque',
  'recepcion-exterior',
  'documentos',
  'detalle'
])

const isExport = computed(() => {
  return (props.expediente?.regimenAduanero || '').toUpperCase().includes('EXPORT')
})

const isEmbarcado = computed(() => {
  const obs = props.expediente?.observaciones || ''
  return obs.includes('EMBARQUE CONFIRMADO')
})

const isRecepcionado = computed(() => {
  const obs = props.expediente?.observaciones || ''
  return obs.includes('RECEPCIÓN DESTINO EXTERIOR')
})
</script>

<style scoped>
.actions-wrapper {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.375rem;
  flex-wrap: wrap;
}

.action-btn-outline {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.3rem 0.6rem;
  font-size: 0.6875rem;
  font-weight: 700;
  border-radius: 4px;
  border: 1px solid var(--color-border, #444138);
  background: var(--color-bg-base, #1e1e1b);
  color: var(--color-text-main, #f6f2ea);
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
}

.action-btn-outline:hover {
  background: var(--color-bg-subtle, #2e2d27);
  border-color: var(--color-border-strong, #5c564a);
}

.action-btn-outline.approve {
  border-color: var(--color-success-border, #2d5738);
  color: var(--color-success-text, #7ee79d);
  background: var(--color-success-bg, #1c2b1f);
}

.action-btn-outline.approve:hover {
  background: rgba(16, 185, 129, 0.25);
  border-color: #4ade80;
}

.action-btn-outline.observe {
  border-color: var(--color-warning-border, #695222);
  color: var(--color-warning-text, #fcd34d);
  background: var(--color-warning-bg, #342914);
}

.action-btn-outline.observe:hover {
  background: rgba(245, 158, 11, 0.25);
  border-color: #f59e0b;
}

.action-btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 4px;
  border: 1px solid var(--color-border, #444138);
  background: var(--color-bg-base, #1e1e1b);
  color: var(--color-text-muted, #c4bcab);
  cursor: pointer;
  transition: all 0.15s ease;
}

.action-btn-icon:hover {
  color: var(--color-primary, #d6b476);
  border-color: var(--color-primary, #d6b476);
  background: var(--color-bg-subtle, #2e2d27);
}

.action-btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 4px;
  border: 1px solid var(--color-primary, #d6b476);
  background: var(--color-primary, #d6b476);
  color: var(--color-primary-text, #1d1b15);
  cursor: pointer;
  transition: all 0.15s ease;
}

.action-btn-primary:hover {
  background: var(--color-primary-hover, #e5c78f);
  border-color: var(--color-primary-hover, #e5c78f);
}
</style>
