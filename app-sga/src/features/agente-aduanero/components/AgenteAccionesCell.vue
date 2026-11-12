<template>
  <div class="actions-wrapper">
    <!-- ============================================================== -->
    <!-- CASO 1: PESTAÑA EXPEDIENTES / TODOS -> SOLO VER INFORME Y DOCS -->
    <!-- ============================================================== -->
    <template v-if="isTabExpedientes">
      <button
        type="button"
        class="action-btn-outline"
        title="Bóveda de Documentos Digitales (BL, DAM, Factura)"
        @click="$emit('documentos', expediente)"
      >
        <i class="pi pi-paperclip"></i>
        <span>Docs</span>
      </button>

      <button
        type="button"
        class="action-btn-primary"
        title="Ver informe completo y auditoría del expediente"
        @click="$emit('detalle', expediente)"
      >
        <i class="pi pi-file-edit"></i>
        <span>Ver Informe</span>
      </button>
    </template>

    <!-- ============================================================== -->
    <!-- CASO 2: PESTAÑA TRÁNSITO / ARRIBO -> SOLO HITOS DE TRÁNSITO    -->
    <!-- ============================================================== -->
    <template v-else-if="activeTab === 'transito'">
      <!-- Exportación en Tránsito -->
      <template v-if="isExport">
        <button
          v-if="expediente.servicioTransporte && !expediente.idDespachoTransporte && (expediente.estadoExpediente === 'ASIGNADO' || expediente.estadoExpediente === 'EN_TRAMITE')"
          type="button"
          class="action-btn-outline approve"
          title="Solicitar Recojo de Carga en Planta"
          @click="$emit('recojo-exportacion', expediente)"
        >
          <i class="pi pi-truck"></i>
          <span>Pedir Recojo</span>
        </button>

        <button
          v-if="expediente.estadoExpediente === 'ASIGNADO' || (expediente.estadoExpediente === 'EN_TRAMITE' && !expediente.fechaArriboAduana)"
          type="button"
          class="action-btn-outline"
          title="Confirmar Ingreso a Depósito Temporal (VGM)"
          @click="$emit('ingreso-origen', expediente)"
        >
          <i class="pi pi-inbox"></i>
          <span>Ingreso Almacén</span>
        </button>

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

      <!-- Importación en Tránsito -->
      <template v-else>
        <!-- 1. Salida de Origen -->
        <button
          v-if="!hasZarpeConfirmado && !isCerrado"
          type="button"
          class="action-btn-outline approve"
          title="Confirmar Salida Real de Origen (Nave, Contenedor, Precinto, Zarpe)"
          @click="$emit('salida-origen', expediente)"
        >
          <i class="pi pi-send"></i>
          <span>Salida Origen</span>
        </button>

        <!-- 2. Arribo a Destino -->
        <button
          v-if="hasZarpeConfirmado && !hasArribo && !isCerrado"
          type="button"
          class="action-btn-outline approve"
          title="Confirmar Arribo de Nave/Carga a Puerto de Destino"
          @click="$emit('arribo-destino', expediente)"
        >
          <i class="pi pi-compass"></i>
          <span>Confirmar Arribo</span>
        </button>

        <!-- 3. Actualizar Tránsito -->
        <button
          v-if="hasZarpeConfirmado && !isCerrado"
          type="button"
          class="action-btn-outline"
          title="Actualizar Datos de Embarque (Nave, Contenedor, Precinto, ETA)"
          @click="$emit('transito', expediente)"
        >
          <i class="pi pi-globe"></i>
          <span>Tránsito</span>
        </button>

        <!-- Indicador si ya arribó -->
        <span
          v-if="hasArribo && !hasCanal && !isCerrado"
          class="status-pill-waiting"
          title="Mercancía arribada. Esperando que el Supervisor asigne Canal SUNAT"
        >
          <i class="pi pi-clock"></i>
          <span>Esperando Canal</span>
        </span>
      </template>

      <!-- Botón Ver Informe -->
      <button
        type="button"
        class="action-btn-primary"
        title="Ver informe completo"
        @click="$emit('detalle', expediente)"
      >
        <i class="pi pi-file-edit"></i>
        <span>Informe</span>
      </button>
    </template>

    <!-- ============================================================== -->
    <!-- CASO 3: PESTAÑA INSPECCIÓN / AFORO -> SOLO DILIGENCIA SUNAT    -->
    <!-- ============================================================== -->
    <template v-else-if="activeTab === 'inspeccion'">
      <button
        v-if="!isCerrado"
        type="button"
        class="action-btn-outline observe"
        title="Registrar Acta de Diligencia / Aforo SUNAT"
        @click="$emit('diligencia', expediente)"
      >
        <i class="pi pi-search-plus"></i>
        <span>Diligencia</span>
      </button>

      <button
        type="button"
        class="action-btn-icon"
        title="Bóveda de Documentos"
        @click="$emit('documentos', expediente)"
      >
        <i class="pi pi-paperclip"></i>
      </button>

      <button
        type="button"
        class="action-btn-primary"
        title="Ver informe completo"
        @click="$emit('detalle', expediente)"
      >
        <i class="pi pi-file-edit"></i>
        <span>Informe</span>
      </button>
    </template>

    <!-- ============================================================== -->
    <!-- CASO 4: PESTAÑA LEVANTE & DESPACHO -> SOLO RETIRO Y TRANSPORTE -->
    <!-- ============================================================== -->
    <template v-else-if="activeTab === 'levante'">
      <template v-if="!isExport">
        <button
          v-if="expediente.servicioTransporte && !expediente.idDespachoTransporte"
          type="button"
          class="action-btn-outline approve"
          title="Solicitar Traslado Terrestre a Transporte y Flota"
          @click="$emit('solicitar-transporte', expediente)"
        >
          <i class="pi pi-truck"></i>
          <span>Pedir Envío</span>
        </button>

        <button
          v-if="!expediente.servicioTransporte"
          type="button"
          class="action-btn-outline"
          title="Registrar Acta de Retiro en Terminal por el Cliente"
          @click="$emit('retiro-directo', expediente)"
        >
          <i class="pi pi-check"></i>
          <span>Retiro Directo</span>
        </button>
      </template>

      <template v-else>
        <button
          v-if="!isEmbarcado"
          type="button"
          class="action-btn-outline approve"
          title="Confirmar Embarque Internacional"
          @click="$emit('embarque', expediente)"
        >
          <i class="pi pi-send"></i>
          <span>Confirmar Embarque</span>
        </button>

        <button
          v-else-if="!isRecepcionado"
          type="button"
          class="action-btn-outline approve"
          title="Confirmar Recepción Conforme en Destino Exterior"
          @click="$emit('recepcion-exterior', expediente)"
        >
          <i class="pi pi-check-circle"></i>
          <span>Entrega Exterior</span>
        </button>
      </template>

      <button
        type="button"
        class="action-btn-icon"
        title="Bóveda de Documentos"
        @click="$emit('documentos', expediente)"
      >
        <i class="pi pi-paperclip"></i>
      </button>

      <button
        type="button"
        class="action-btn-primary"
        title="Ver informe completo"
        @click="$emit('detalle', expediente)"
      >
        <i class="pi pi-file-edit"></i>
        <span>Informe</span>
      </button>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  expediente: {
    type: Object,
    required: true
  },
  activeTab: {
    type: String,
    default: 'todos'
  }
})

defineEmits([
  'salida-origen',
  'arribo-destino',
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

const isTabExpedientes = computed(() => {
  return props.activeTab === 'todos' || props.activeTab === 'expedientes' || !props.activeTab
})

const isExport = computed(() => {
  return (props.expediente?.regimenAduanero || '').toUpperCase().includes('EXPORT')
})

const hasZarpeConfirmado = computed(() => {
  const obs = props.expediente?.observaciones || ''
  if (obs.includes('SALIDA ORIGEN CONFIRMADA')) return true
  if (hasArribo.value || hasCanal.value) return true
  if (props.expediente?.estadoExpediente === 'ASIGNADO') return false
  return props.expediente?.estadoExpediente !== 'ASIGNADO'
})

const hasArribo = computed(() => {
  return Boolean(props.expediente?.fechaArriboAduana)
})

const hasCanal = computed(() => {
  return props.expediente?.canalSunat && props.expediente?.canalSunat !== 'PENDIENTE'
})

const isCerrado = computed(() => {
  return props.expediente?.estadoExpediente === 'CERRADO' || props.expediente?.estadoExpediente === 'ANULADO'
})

const isEmbarcado = computed(() => {
  const obs = props.expediente?.observaciones || ''
  return obs.includes('EMBARCADO')
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
  padding: 0.35rem 0.65rem;
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  border-radius: var(--radius-sm, 6px);
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  cursor: pointer;
  white-space: nowrap;
  transition: all var(--transition-fast, 0.15s ease);
}

.action-btn-outline:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.action-btn-outline.approve {
  border-color: var(--color-success-border);
  color: var(--color-success-text);
  background: var(--color-success-bg);
}

.action-btn-outline.approve:hover {
  filter: brightness(0.95);
}

.action-btn-outline.observe {
  border-color: var(--color-warning-border);
  color: var(--color-warning-text);
  background: var(--color-warning-bg);
}

.action-btn-outline.observe:hover {
  filter: brightness(0.95);
}

.status-pill-waiting {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.25rem 0.5rem;
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 600;
  border-radius: 4px;
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px dashed var(--color-warning-border);
  white-space: nowrap;
}

.action-btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: var(--radius-sm, 6px);
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.action-btn-icon:hover {
  color: var(--color-text-main);
  border-color: var(--color-border-strong);
  background: var(--color-bg-subtle);
}

.action-btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-sm, 6px);
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: var(--color-primary-text);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.action-btn-primary:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}
</style>
