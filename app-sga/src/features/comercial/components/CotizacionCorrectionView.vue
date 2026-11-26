<template>
  <div class="sga-correction-view fade-in">
    <!-- Cabecera directa sgaUI -->
    <header class="sga-form-header">
      <div class="sga-correction-header-left">
        <Button
          type="button"
          label="Volver"
          icon="pi pi-arrow-left"
          severity="secondary"
          text
          size="small"
          :disabled="submitting"
          @click="$emit('cancel')"
        />

        <h2 class="sga-form-title">
          Corregir Propuesta
          <span class="font-mono sga-correction-code">
            · {{ form.codigoCotizacion || 'COT-SIN-CODIGO' }}
          </span>
        </h2>

        <Tag value="Observada" severity="warn" icon="pi pi-exclamation-circle" rounded />
      </div>

      <div class="sga-form-actions">
        <Button
          type="button"
          label="Cancelar"
          severity="secondary"
          outlined
          size="small"
          :disabled="submitting"
          @click="$emit('cancel')"
        />
        <Button
          type="button"
          label="Guardar y Reenviar a Supervisión"
          icon="pi pi-send"
          size="small"
          :loading="submitting"
          @click="handleSubmitCorrection"
        />
      </div>
    </header>

    <!-- Error de validación -->
    <Message v-if="validationError" severity="error" @close="validationError = ''">
      {{ validationError }}
    </Message>

    <!-- Banner de Observación Técnica de Supervisión Aduanera -->
    <div class="sga-banner is-warn">
      <i class="pi pi-exclamation-triangle sga-obs-banner-icon"></i>
      <div class="sga-obs-banner-body">
        <span class="sga-micro-label">OBSERVACIÓN TÉCNICA DE SUPERVISIÓN ADUANERA</span>
        <p class="sga-obs-reason-text">
          {{ motivoObservacion || 'Se requieren correcciones y precisiones técnicas antes de la aprobación definitiva.' }}
        </p>
        <span class="sga-obs-hint">
          Al guardar los cambios, la cotización volverá automáticamente a Supervisión Aduanera para su reevaluación.
        </span>
      </div>
    </div>

    <!-- Layout Split-Screen sgaUI -->
    <div class="sga-split-layout">
      <div class="sga-split-main">
        <!-- 1. Cliente y Nota de Subsanación -->
        <CotizacionCorrectionSubsanacionCard
          v-model:nota-subsanacion="notaSubsanacion"
          :form="form"
          :clientes="clientes"
          :submitting="submitting"
          @client-reassign="handleClientReassign"
        />

        <!-- 2. Régimen, Modalidad, Puertos, Mercancía, Transporte Local y Vigencia -->
        <CotizacionStepRegimen
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
        />

        <!-- 3. Valoración Económica y Tributos SUNAT -->
        <CotizacionStepCostos
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
        />

        <!-- 4. Documentación Sustentatoria (Modo Live) -->
        <section class="sga-card-surface sga-docs-card">
          <header class="sga-docs-card-head">
            <h3 class="sga-docs-card-title">Documentación y Expediente Digital</h3>
            <p class="sga-docs-card-desc">
              Revise los adjuntos actuales y cargue o reemplace sustentos para la subsanación.
            </p>
          </header>

          <DocumentosUploadPanel
            mode="live"
            entidad-tipo="COTIZACION"
            :entidad-id="form.idCotizacion"
            :codigo-entidad="form.codigoCotizacion || 'COT'"
            :titulo-entidad="selectedClientObject?.razonSocial || 'Propuesta Comercial'"
            :permiso-requerido="!!selectedMercancia?.requierePermiso"
            :entidad-reguladora="selectedMercancia?.entidadReguladora || ''"
            :hide-header-banner="true"
            @docs-loaded="docsCount = $event"
          />
        </section>
      </div>

      <!-- Proforma Sticky en Vivo -->
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
        :docs-count="docsCount"
        :get-regimen-label="getRegimenLabel"
        :format-number="formatNumber"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, toRef } from 'vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Message from 'primevue/message'
import DocumentosUploadPanel from '@/shared/components/DocumentosUploadPanel.vue'
import CotizacionCorrectionSubsanacionCard from './CotizacionCorrectionSubsanacionCard.vue'
import CotizacionStepRegimen from './wizard/CotizacionStepRegimen.vue'
import CotizacionStepCostos from './wizard/CotizacionStepCostos.vue'
import CotizacionProformaPreview from './CotizacionProformaPreview.vue'
import { useCotizacionCorrection } from '../composables/useCotizacionCorrection'
import { catalogoService } from '@/features/admin/catalogos/services/catalogoService'
import { getDepartamentosPeru, getProvinciasPorDepartamento } from '../data/peruUbigeo'
import {
  mapPuertosCatalogo,
  mapTiposCargaCatalogo,
  mapMercanciasCatalogo
} from '../utils/cotizacionCatalogos'

const props = defineProps({
  cotizacion: {
    type: Object,
    required: true
  },
  clientes: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['cancel', 'saved'])

const puertos = ref([])
const tiposCarga = ref([])
const mercancias = ref([])
const loadingCatalogos = ref(false)
const submitting = ref(false)
const validationError = ref('')
const docsCount = ref(0)

const {
  form,
  motivoObservacion,
  notaSubsanacion,
  seguroManual,
  regimenOptions,
  modalidadTransporte,
  paisOrigen,
  paisDestino,
  paisesOrigenDisponibles,
  paisesDestinoDisponibles,
  puertosOrigenFiltrados,
  puertosDestinoFiltrados,
  selectedClientObject,
  selectedPuertoEnvio,
  selectedPuertoDestino,
  selectedTipoCarga,
  selectedMercancia,
  setModalidad,
  aplicarReglasRegimen,
  handleSeguroManualInput,
  resetearSeguroAutomatico,
  valorCif,
  montoAdValorem,
  montoIgv,
  montoPercepcion,
  recalcularLiquidacion,
  validateCorrection,
  buildSubmissionPayload,
  getRegimenLabel,
  formatNumber
} = useCotizacionCorrection({
  cotizacion: toRef(props, 'cotizacion'),
  puertos,
  tiposCarga,
  mercancias,
  clientes: toRef(props, 'clientes')
})

const departamentosList = computed(() => getDepartamentosPeru())
const provinciasOrigenList = computed(() =>
  getProvinciasPorDepartamento(form.value.origenDepartamento, form.value.origenProvincia)
)
const provinciasDestinoList = computed(() =>
  getProvinciasPorDepartamento(form.value.destinoDepartamento, form.value.destinoProvincia)
)

const onOrigenDepartamentoChange = () => {
  const provs = getProvinciasPorDepartamento(form.value.origenDepartamento)
  form.value.origenProvincia = provs.length > 0 ? provs[0] : ''
}

const onDestinoDepartamentoChange = () => {
  const provs = getProvinciasPorDepartamento(form.value.destinoDepartamento)
  form.value.destinoProvincia = provs.length > 0 ? provs[0] : ''
}

const loadCatalogos = async () => {
  loadingCatalogos.value = true
  try {
    const [puertosRes, tiposRes, mercanciasRes] = await Promise.allSettled([
      catalogoService.getPuertos(true),
      catalogoService.getTiposCarga(true),
      catalogoService.getMercancias(true)
    ])

    if (puertosRes.status === 'fulfilled') {
      puertos.value = mapPuertosCatalogo(puertosRes.value)
    }
    if (tiposRes.status === 'fulfilled') {
      tiposCarga.value = mapTiposCargaCatalogo(tiposRes.value)
    }
    if (mercanciasRes.status === 'fulfilled') {
      mercancias.value = mapMercanciasCatalogo(mercanciasRes.value)
    }
  } catch (err) {
    console.error('Error cargando catálogos en CotizacionCorrectionView:', err)
  } finally {
    loadingCatalogos.value = false
  }
}

const handleSelectRegimen = (regimen) => {
  form.value.regimenAduanero = regimen
  aplicarReglasRegimen(regimen)
  recalcularLiquidacion()
}

const handlePaisOrigenChange = () => {
  if (form.value.idPuertoEnvio) {
    const valid = puertosOrigenFiltrados.value.some((p) => p.idPuerto === form.value.idPuertoEnvio)
    if (!valid) form.value.idPuertoEnvio = null
  }
}

const handlePaisDestinoChange = () => {
  if (form.value.idPuertoDestino) {
    const valid = puertosDestinoFiltrados.value.some((p) => p.idPuerto === form.value.idPuertoDestino)
    if (!valid) form.value.idPuertoDestino = null
  }
}

const handleMercanciaChange = () => {
  if (selectedMercancia.value?.arancelBasePct !== undefined && selectedMercancia.value?.arancelBasePct !== null) {
    const arancel = Number(selectedMercancia.value.arancelBasePct)
    if ([0, 6, 11].includes(arancel)) {
      form.value.tasaAdValorem = arancel
    }
    recalcularLiquidacion()
  }
}

const handleClientReassign = () => {
  if (form.value.idCliente && props.clientes) {
    const c = props.clientes.find((cl) => cl.idCliente === form.value.idCliente)
    if (c) {
      form.value.clienteRazonSocial = c.razonSocial || ''
      form.value.clienteNumeroDocumento = c.numeroDocumento || ''
    }
  }
}

const handleSubmitCorrection = () => {
  validationError.value = ''
  const valResult = validateCorrection()
  if (!valResult.valid) {
    validationError.value = valResult.error
    window.scrollTo({ top: 0, behavior: 'smooth' })
    return
  }

  submitting.value = true
  const payload = buildSubmissionPayload()

  emit('saved', {
    isEdit: true,
    cotizacionId: form.value.idCotizacion,
    payload,
    documentos: [],
    done: () => {
      submitting.value = false
    }
  })
}

onMounted(() => {
  loadCatalogos()
})
</script>

<style scoped>
.sga-correction-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  width: 100%;
}

.sga-correction-header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.sga-correction-code {
  color: var(--p-primary-color);
}

.sga-obs-banner-icon {
  margin-top: 0.2rem;
  flex-shrink: 0;
}

.sga-obs-banner-body {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 0;
}

.sga-obs-reason-text {
  margin: 0;
  font-size: 0.9375rem;
  font-weight: 600;
  line-height: 1.45;
  white-space: pre-line;
}

.sga-obs-hint {
  font-size: 0.78rem;
  opacity: 0.9;
}

.sga-docs-card {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.sga-docs-card-head {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--color-border);
}

.sga-docs-card-title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 600;
  letter-spacing: -0.015em;
  color: var(--color-text-main);
}

.sga-docs-card-desc {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--color-text-subtle);
}
</style>
