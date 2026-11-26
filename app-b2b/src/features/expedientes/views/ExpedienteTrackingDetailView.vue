<template>
  <div class="sga-tracking-view fade-in">
    <!-- Cabecera contextual sgaUI (Título único vive en .sga-topbar) -->
    <header class="sga-form-header">
      <div class="sga-tracking-header-left">
        <Button
          type="button"
          label="Volver"
          icon="pi pi-arrow-left"
          severity="secondary"
          outlined
          size="small"
          @click="volver"
        />

        <span v-if="expediente" class="sga-tracking-code-pill">
          <span>{{ expediente.codigoExpediente }}</span>
          <button
            type="button"
            class="sga-copy-btn"
            title="Copiar código de expediente"
            aria-label="Copiar código de expediente"
            @click="copiarCodigo(expediente.codigoExpediente, 'Expediente')"
          >
            <i class="pi pi-copy"></i>
          </button>
        </span>

        <Tag
          v-if="expediente"
          :value="isExportExpediente(expediente) ? 'Exportación' : 'Importación'"
          severity="secondary"
          rounded
        />
      </div>

      <div v-if="expediente" class="sga-form-actions">
        <Button
          v-if="expediente.idCotizacion"
          type="button"
          label="Ver Cotización"
          icon="pi pi-file"
          severity="secondary"
          outlined
          size="small"
          @click="irACotizacion(expediente.idCotizacion)"
        />
        <Button
          v-if="expediente.estadoExpediente !== 'CERRADO' && expediente.estadoExpediente !== 'ANULADO'"
          type="button"
          label="Completar Datos"
          icon="pi pi-pencil"
          severity="secondary"
          outlined
          size="small"
          @click="abrirEdicion"
        />
        <Button
          type="button"
          label="Actualizar"
          icon="pi pi-sync"
          size="small"
          :loading="refreshing"
          @click="cargarDetalle({ silent: true })"
        />
      </div>
    </header>

    <!-- Estado de carga -->
    <div v-if="loading" class="sga-card-surface tracking-state-card">
      <i class="pi pi-spin pi-spinner tracking-state-icon"></i>
      <p>Cargando seguimiento en tiempo real del expediente...</p>
    </div>

    <!-- Estado no encontrado / error -->
    <div v-else-if="!expediente" class="sga-card-surface tracking-state-card">
      <i class="pi pi-exclamation-circle tracking-state-icon is-danger"></i>
      <h3>No se encontró el expediente</h3>
      <p>
        Es posible que el código ingresado no exista o no pertenezca a su cuenta empresarial.
      </p>
      <Button type="button" label="Volver a Expedientes" size="small" @click="volver" />
    </div>

    <!-- Contenido Tracking Estándar sgaUI -->
    <template v-else>
      <TrackingHeroCard
        :expediente="expediente"
        @copy-code="copiarCodigo"
      />

      <div class="sga-split-layout">
        <div class="sga-split-main">
          <TrackingTimelineFeed :eventos="expediente.eventos" />

          <TrackingOperacionGrid
            :expediente="expediente"
            @edit-client-data="abrirEdicion"
            @preview-informe="handleVerInformeConformidad"
            @download-informe="handleDescargarInformeConformidad"
            @copy-code="copiarCodigo"
          />
        </div>

        <TrackingResumenAside
          :expediente="expediente"
          :documentos="documentos"
          :loading-docs="loadingDocs"
          @preview-doc="handlePrevisualizar"
          @download-doc="handleDescargar"
          @open-cotizacion="irACotizacion"
        />
      </div>
    </template>

    <!-- Modal de Edición de Datos Operativos del Cliente -->
    <ExpedienteSimpleModal
      :visible="showEditModal"
      :expediente="expediente"
      :saving="savingSimple"
      @close="showEditModal = false"
      @submit="handleSubmitDatos"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import TrackingHeroCard from '../components/tracking/TrackingHeroCard.vue'
import TrackingTimelineFeed from '../components/tracking/TrackingTimelineFeed.vue'
import TrackingOperacionGrid from '../components/tracking/TrackingOperacionGrid.vue'
import TrackingResumenAside from '../components/tracking/TrackingResumenAside.vue'
import ExpedienteSimpleModal from '../components/ExpedienteSimpleModal.vue'
import { useExpedienteTrackingDetail } from '../composables/useExpedienteTrackingDetail'
import { isExportExpediente } from '../utils/expedienteTrackingUtils'

const router = useRouter()
const route = useRoute()

const {
  expediente,
  documentos,
  loading,
  loadingDocs,
  refreshing,
  savingSimple,
  cargarDetalle,
  guardarDatosCliente,
  handleDescargar,
  handlePrevisualizar,
  handleVerInformeConformidad,
  handleDescargarInformeConformidad,
  copiarCodigo
} = useExpedienteTrackingDetail(() => route.params.id)

const showEditModal = ref(false)

function volver() {
  router.push('/expedientes')
}

function irACotizacion(idCotizacion) {
  if (!idCotizacion) return
  router.push(`/cotizaciones/${idCotizacion}`)
}

function abrirEdicion() {
  if (!expediente.value) return
  showEditModal.value = true
}

async function handleSubmitDatos(formPayload) {
  const res = await guardarDatosCliente(formPayload)
  if (res) {
    showEditModal.value = false
  }
}

watch(
  () => route.params.id,
  (newId, oldId) => {
    if (newId && newId !== oldId) {
      cargarDetalle()
    }
  }
)

onMounted(() => {
  cargarDetalle()
})
</script>

<style scoped>
.tracking-state-card {
  padding: 3rem 1.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 0.65rem;
  color: var(--color-text-muted);
}

.tracking-state-card h3 {
  margin: 0;
  color: var(--color-text-main);
}

.tracking-state-card p {
  margin: 0;
  font-size: 0.85rem;
}

.tracking-state-icon {
  font-size: 2rem;
  color: var(--color-accent-kicker);
}

.tracking-state-icon.is-danger {
  color: var(--color-danger-text);
}
</style>
