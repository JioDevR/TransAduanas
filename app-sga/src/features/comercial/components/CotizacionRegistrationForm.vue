<template>
  <div class="sga-cotizacion-wizard">
    <CotizacionFormHeader
      :current-step="currentStep"
      :is-edit-mode="isEditMode"
      :is-unassigned-claim="isUnassignedClaim"
      :codigo-cotizacion="form.codigoCotizacion"
      :submitting="submitting"
      :is-form-ready="isFormReady"
      :can-continue="canContinueCurrentStep"
      @cancel="$emit('cancel')"
      @next="goToNextStep"
      @submit="handleSubmit"
    />

    <Message v-if="isUnassignedClaim" severity="info" :closable="false">
      <strong>Solicitud B2B sin asignar:</strong>
      Revise los pasos 01–03 y liquide los costos en el paso 04 para asignarla a su cartera.
    </Message>

    <Message
      v-if="['REVISION', 'OBSERVADA', 'EN_REVISION'].includes(props.cotizacionToEdit?.estado)"
      severity="warn"
      :closable="false"
    >
      <strong>Observada por Supervisión Aduanera:</strong>
      {{ props.cotizacionToEdit?.motivoObservacion || 'Realice los ajustes requeridos antes de reenviar.' }}
    </Message>

    <Message v-if="errorMessage" severity="error" @close="errorMessage = ''">
      {{ errorMessage }}
    </Message>

    <CotizacionStepperNav
      v-model:current-step="currentStep"
      :form="form"
      :docs-count="isEditMode ? existingDocsCount : queuedDocs.length"
    />

    <div class="sga-split-layout">
      <div class="sga-split-main">
        <CotizacionStepCliente
          v-show="currentStep === 1"
          v-model:client-search-query="clientSearchQuery"
          v-model:show-quick-client-form="showQuickClientForm"
          :filtered-clients="filteredAvailableClients"
          :selected-client-object="selectedClientObject"
          :quick-client="quickClient"
          :saving-quick-client="savingQuickClient"
          :looking-up-client="lookingUpClient"
          :quick-client-error="quickClientError"
          @select-client="selectClient"
          @clear-client="clearSelectedClient"
          @lookup-client="handleLookupClientByDoc"
          @save-quick-client="handleSaveQuickClient"
        />

        <CotizacionStepRegimen
          v-show="currentStep === 2"
          :form="form"
          :regimen-options="regimenOptions"
          :modalidad-transporte="modalidadTransporte"
          v-model:pais-origen="paisOrigen"
          v-model:pais-destino="paisDestino"
          :paises-origen-disponibles="paisesOrigenDisponibles"
          :paises-destino-disponibles="paisesDestinoDisponibles"
          :puertos-origen-filtrados="puertosOrigenFiltrados"
          :puertos-destino-filtrados="puertosDestinoFiltrados"
          :selected-puerto-envio="selectedPuertoEnvio"
          :selected-puerto-destino="selectedPuertoDestino"
          :tipos-carga="tiposCarga"
          :selected-tipo-carga="selectedTipoCarga"
          :mercancias="mercancias"
          :selected-mercancia="selectedMercancia"
          :departamentos-list="departamentosList"
          :provincias-origen-list="provinciasOrigenList"
          :provincias-destino-list="provinciasDestinoList"
          :loading-catalogos="loadingCatalogos"
          :submitting="submitting"
          @select-regimen="handleSelectRegimen"
          @set-modalidad="setModalidad"
          @pais-origen-change="handlePaisOrigenChange"
          @pais-destino-change="handlePaisDestinoChange"
          @mercancia-change="handleMercanciaChange"
          @origen-departamento-change="onOrigenDepartamentoChange"
          @destino-departamento-change="onDestinoDepartamentoChange"
          @prev="goToPrevStep"
          @next="goToNextStep"
        />

        <CotizacionStepDocumentos
          v-show="currentStep === 3"
          v-model:queued-docs="queuedDocs"
          :is-unassigned-claim="isUnassignedClaim"
          :is-edit-mode="isEditMode"
          :cotizacion-id="props.cotizacionToEdit?.idCotizacion"
          :codigo-cotizacion="form.codigoCotizacion"
          :client-name="selectedClientObject?.razonSocial"
          :permiso-requerido="!!selectedMercancia?.requierePermiso"
          :entidad-reguladora="selectedMercancia?.entidadReguladora || ''"
          @prev="goToPrevStep"
          @next="goToNextStep"
        />

        <CotizacionStepCostos
          v-show="currentStep === 4"
          :form="form"
          :seguro-manual="seguroManual"
          :valor-cif="valorCif"
          :monto-ad-valorem="montoAdValorem"
          :monto-igv="montoIgv"
          :monto-percepcion="montoPercepcion"
          :format-number="formatNumber"
          :submitting="submitting"
          @seguro-manual-input="handleSeguroManualInput"
          @reset-seguro="resetearSeguroAutomatico"
          @prev="goToPrevStep"
        />
      </div>

      <CotizacionProformaPreview
        :form="form"
        :selected-client-object="selectedClientObject"
        :modalidad-transporte="modalidadTransporte"
        :pais-origen="paisOrigen"
        :pais-destino="paisDestino"
        :selected-puerto-envio="selectedPuertoEnvio"
        :selected-puerto-destino="selectedPuertoDestino"
        :selected-tipo-carga="selectedTipoCarga"
        :selected-mercancia="selectedMercancia"
        :docs-count="isEditMode ? existingDocsCount : queuedDocs.length"
        :get-regimen-label="getRegimenLabel"
        :format-number="formatNumber"
      />
    </div>
  </div>
</template>

<script setup>
import Message from 'primevue/message'
import CotizacionFormHeader from './CotizacionFormHeader.vue'
import CotizacionStepperNav from './CotizacionStepperNav.vue'
import CotizacionProformaPreview from './CotizacionProformaPreview.vue'
import CotizacionStepCliente from './wizard/CotizacionStepCliente.vue'
import CotizacionStepRegimen from './wizard/CotizacionStepRegimen.vue'
import CotizacionStepDocumentos from './wizard/CotizacionStepDocumentos.vue'
import CotizacionStepCostos from './wizard/CotizacionStepCostos.vue'
import { useCotizacionForm } from '../composables/useCotizacionForm'

const props = defineProps({
  cotizacionToEdit: {
    type: Object,
    default: null
  },
  clientes: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['cancel', 'saved', 'client-created'])

const {
  currentStep,
  submitting,
  errorMessage,
  loadingCatalogos,
  queuedDocs,
  existingDocsCount,
  seguroManual,
  tiposCarga,
  mercancias,
  clientSearchQuery,
  filteredAvailableClients,
  showQuickClientForm,
  savingQuickClient,
  lookingUpClient,
  quickClientError,
  quickClient,
  regimenOptions,
  form,
  isEditMode,
  isUnassignedClaim,
  departamentosList,
  provinciasOrigenList,
  provinciasDestinoList,
  onOrigenDepartamentoChange,
  onDestinoDepartamentoChange,
  selectedClientObject,
  selectedMercancia,
  selectedPuertoEnvio,
  selectedPuertoDestino,
  selectedTipoCarga,
  modalidadTransporte,
  paisOrigen,
  paisDestino,
  paisesOrigenDisponibles,
  paisesDestinoDisponibles,
  puertosOrigenFiltrados,
  puertosDestinoFiltrados,
  setModalidad,
  handleSelectRegimen,
  handlePaisOrigenChange,
  handlePaisDestinoChange,
  selectClient,
  clearSelectedClient,
  handleLookupClientByDoc,
  handleSaveQuickClient,
  valorCif,
  montoAdValorem,
  montoIgv,
  montoPercepcion,
  handleMercanciaChange,
  handleSeguroManualInput,
  resetearSeguroAutomatico,
  formatNumber,
  getRegimenLabel,
  isFormReady,
  canContinueCurrentStep,
  goToNextStep,
  goToPrevStep,
  handleSubmit,
  setSubmitting
} = useCotizacionForm(props, emit)

defineExpose({
  submitting,
  setSubmitting
})
</script>

<style scoped>
.sga-cotizacion-wizard {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
</style>
