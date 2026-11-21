<template>
  <div class="sga-cotizacion-wizard fade-in">
    <PortalCotizacionFormHeader
      :current-step="currentStep"
      :submitting="submitting"
      :is-form-ready="isFormReady"
      @cancel="$emit('cancel')"
      @next="currentStep = Math.min(3, currentStep + 1)"
      @submit="handleSubmit"
    />

    <PortalCotizacionStepperNav
      :current-step="currentStep"
      :form="form"
      :docs-count="queuedDocs.length"
      @update:current-step="currentStep = $event"
    />

    <div class="sga-split-layout">
      <div class="sga-split-main">
        <PortalStepRegimenRuta
          v-show="currentStep === 1"
          :form="form"
          :regimen-options="REGIMEN_OPTIONS"
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
          :departamentos-peru="departamentosPeru"
          :provincias-origen="provinciasOrigen"
          :provincias-destino="provinciasDestino"
          :loading-catalogos="loadingCatalogos"
          :submitting="submitting"
          @select-regimen="handleSelectRegimen"
          @set-modalidad="setModalidad"
          @next="currentStep = 2"
        />

        <section v-show="currentStep === 2" class="sga-card-surface docs-step-card">
          <PortalDocumentosUploadPanel
            mode="deferred"
            v-model="queuedDocs"
            :permiso-requerido="selectedMercancia?.requierePermiso || false"
            :entidad-reguladora="selectedMercancia?.entidadReguladora || ''"
          />

          <div class="step-footer-actions">
            <Button
              type="button"
              label="Anterior: Régimen & Ruta"
              icon="pi pi-arrow-left"
              severity="secondary"
              outlined
              size="small"
              @click="currentStep = 1"
            />
            <Button
              type="button"
              label="Siguiente: Valores Declarados"
              icon="pi pi-arrow-right"
              iconPos="right"
              size="small"
              @click="currentStep = 3"
            />
          </div>
        </section>

        <PortalStepValoresDeclarados
          v-show="currentStep === 3"
          :form="form"
          :submitting="submitting"
          :is-form-ready="isFormReady"
          @prev="currentStep = 2"
          @submit="handleSubmit"
        />
      </div>

      <PortalCotizacionProformaPreview
        :form="form"
        :empresa="empresaData"
        :selected-puerto-envio="selectedPuertoEnvio"
        :selected-puerto-destino="selectedPuertoDestino"
        :selected-tipo-carga="selectedTipoCarga"
        :selected-mercancia="selectedMercancia"
        :docs-count="queuedDocs.length"
      />
    </div>
  </div>
</template>

<script setup>
import Button from 'primevue/button'
import { usePortalCotizacionForm } from '../composables/usePortalCotizacionForm'
import PortalCotizacionFormHeader from './PortalCotizacionFormHeader.vue'
import PortalCotizacionStepperNav from './PortalCotizacionStepperNav.vue'
import PortalDocumentosUploadPanel from './PortalDocumentosUploadPanel.vue'
import PortalCotizacionProformaPreview from './PortalCotizacionProformaPreview.vue'
import PortalStepRegimenRuta from './wizard/PortalStepRegimenRuta.vue'
import PortalStepValoresDeclarados from './wizard/PortalStepValoresDeclarados.vue'

const emit = defineEmits(['cancel', 'saved'])

const {
  currentStep,
  submitting,
  loadingCatalogos,
  queuedDocs,
  form,
  REGIMEN_OPTIONS,
  tiposCarga,
  mercancias,
  modalidadTransporte,
  paisOrigen,
  paisDestino,
  paisesOrigenDisponibles,
  paisesDestinoDisponibles,
  puertosOrigenFiltrados,
  puertosDestinoFiltrados,
  departamentosPeru,
  provinciasOrigen,
  provinciasDestino,
  selectedPuertoEnvio,
  selectedPuertoDestino,
  selectedTipoCarga,
  selectedMercancia,
  empresaData,
  isFormReady,
  setModalidad,
  handleSelectRegimen,
  handleSubmit
} = usePortalCotizacionForm(emit)

defineExpose({
  form,
  currentStep,
  queuedDocs,
  handleSubmit,
  isFormReady
})
</script>

<style scoped>
.sga-cotizacion-wizard {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  width: 100%;
  font-family: var(--font-sans);
}

.docs-step-card {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.step-footer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border-subtle);
}
</style>
