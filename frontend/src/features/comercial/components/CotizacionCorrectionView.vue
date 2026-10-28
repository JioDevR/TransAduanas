<template>
  <div class="cotizacion-correction-view fade-in">
    <!-- 1. Header Fijo de Control y Acciones -->
    <header class="correction-header-bar">
      <div class="header-left">
        <button type="button" class="btn-back-action" @click="$emit('cancel')">
          <i class="pi pi-arrow-left"></i>
          <span>Volver</span>
        </button>
        <div class="header-titles">
          <div class="badges-row">
            <span class="badge-code">{{ form.codigoCotizacion || 'COT-SIN-CODIGO' }}</span>
            <span class="badge-status-observed">
              <i class="pi pi-exclamation-circle"></i>
              <span>OBSERVADA / EN REVISIÓN</span>
            </span>
          </div>
          <h2 class="header-main-title">Corrección de Propuesta Comercial</h2>
          <p class="header-subtitle">
            Ajuste los parámetros técnicos y económicos observados por Supervisión Aduanera en una sola vista.
          </p>
        </div>
      </div>

      <div class="header-right-actions">
        <button
          type="button"
          class="btn-cancel-flat"
          :disabled="submitting"
          @click="$emit('cancel')"
        >
          Cancelar
        </button>
        <button
          type="button"
          class="btn-submit-correction"
          :disabled="submitting"
          @click="handleSubmitCorrection"
        >
          <i v-if="submitting" class="pi pi-spin pi-spinner"></i>
          <i v-else class="pi pi-send"></i>
          <span>{{ submitting ? 'Guardando...' : 'Guardar y Reenviar a Supervisión' }}</span>
        </button>
      </div>
    </header>

    <!-- Banner de Error si la validación falla -->
    <div v-if="validationError" class="error-banner">
      <i class="pi pi-exclamation-triangle"></i>
      <div class="error-content">
        <strong>Error de validación:</strong>
        <span>{{ validationError }}</span>
      </div>
      <button type="button" class="error-close-btn" @click="validationError = ''">
        <i class="pi pi-times"></i>
      </button>
    </div>

    <!-- 2. Banner Destacado de Observación Técnica -->
    <section class="observation-master-banner">
      <div class="obs-banner-icon-col">
        <div class="obs-icon-circle">
          <i class="pi pi-exclamation-triangle"></i>
        </div>
      </div>
      <div class="obs-banner-content">
        <div class="obs-kicker-row">
          <span class="obs-kicker">OBSERVACIÓN TÉCNICA DE SUPERVISIÓN ADUANERA</span>
          <span class="obs-cliente-tag">{{ form.clienteRazonSocial || 'Cliente no especificado' }}</span>
        </div>
        <h3 class="obs-title">Motivo de Devolución para Subsanación:</h3>
        <p class="obs-reason-text">
          "{{ motivoObservacion || 'Se requieren correcciones y precisiones técnicas antes de la aprobación definitiva.' }}"
        </p>
        <div class="obs-instruction-hint">
          <i class="pi pi-info-circle"></i>
          <span>Modifique los campos correspondientes a continuación. Al presionar "Guardar y Reenviar", la cotización regresará automáticamente a la bandeja de Supervisión Aduanera como <strong>ENVIADA</strong>.</span>
        </div>
      </div>
    </section>

    <!-- 3. Formulario Unificado en Pantalla Continua (2 Columnas) -->
    <div class="correction-main-grid">
      <!-- ======================================================== -->
      <!-- COLUMNA 1: LOGÍSTICA, RUTAS Y PARÁMETROS ADUANEROS       -->
      <!-- ======================================================== -->
      <div class="grid-column">
        <!-- Tarjeta A: Identificación de Cliente y Régimen -->
        <div class="correction-card">
          <div class="card-header">
            <div class="card-icon-tag"><i class="pi pi-id-card"></i></div>
            <div>
              <h4 class="card-title">1. Cliente y Régimen Aduanero</h4>
              <p class="card-subtitle">Datos de la empresa importadora/exportadora y tratamiento legal</p>
            </div>
          </div>

          <div class="card-body">
            <!-- Info de Cliente -->
            <div class="client-summary-box">
              <div class="client-meta">
                <span class="client-label">Cliente Asignado:</span>
                <strong class="client-name">{{ form.clienteRazonSocial || 'Sin Razón Social' }}</strong>
                <span class="client-doc">Doc: {{ form.clienteNumeroDocumento || 'S/D' }}</span>
              </div>
              <div v-if="clientes && clientes.length > 0" class="field-group">
                <label class="form-label-sub">Reasignar Cliente (Opcional):</label>
                <Select
                  v-model="form.idCliente"
                  :options="clientes"
                  option-label="razonSocial"
                  option-value="idCliente"
                  filter
                  placeholder="Seleccionar cliente registrado"
                  class="w-full sga-select"
                  @change="handleClientReassign"
                />
              </div>
            </div>

            <!-- Régimen Aduanero -->
            <div class="field-group mt-3">
              <label class="form-label">Régimen Aduanero SUNAT *</label>
              <div class="regimen-selector-pills">
                <button
                  type="button"
                  :class="['pill-opt', { active: form.regimenAduanero === 'IMPORTACION_DEFINITIVA' }]"
                  @click="handleSelectRegimen('IMPORTACION_DEFINITIVA')"
                >
                  <i class="pi pi-download"></i>
                  <span>Importación Definitiva (REG-10)</span>
                </button>
                <button
                  type="button"
                  :class="['pill-opt', { active: form.regimenAduanero === 'EXPORTACION_DEFINITIVA' }]"
                  @click="handleSelectRegimen('EXPORTACION_DEFINITIVA')"
                >
                  <i class="pi pi-upload"></i>
                  <span>Exportación Definitiva (REG-40)</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Tarjeta B: Modalidad de Transporte y Terminales en Cascada -->
        <div class="correction-card">
          <div class="card-header">
            <div class="card-icon-tag"><i class="pi pi-compass"></i></div>
            <div>
              <h4 class="card-title">2. Modalidad y Ruta Internacional</h4>
              <p class="card-subtitle">Terminales de origen y arribo con filtrado reactivo por país</p>
            </div>
          </div>

          <div class="card-body">
            <!-- Botones segmentados de Modalidad -->
            <div class="field-group">
              <label class="form-label">Modalidad de Transporte *</label>
              <div class="segmented-mode-selector">
                <button
                  type="button"
                  :class="['mode-btn', { active: modalidadTransporte === 'MARITIMO' }]"
                  @click="setModalidad('MARITIMO')"
                >
                  <i class="pi pi-compass"></i>
                  <span>Carga Marítima (FCL / LCL)</span>
                </button>
                <button
                  type="button"
                  :class="['mode-btn', { active: modalidadTransporte === 'AEREO' }]"
                  @click="setModalidad('AEREO')"
                >
                  <i class="pi pi-send"></i>
                  <span>Carga Aérea (Air Cargo)</span>
                </button>
              </div>
            </div>

            <!-- Cascada Origen: País -> Puerto/Aeropuerto -->
            <div class="two-fields-row">
              <div class="field-group">
                <label class="form-label">País de Origen / Envío</label>
                <Select
                  v-model="paisOrigen"
                  :options="paisesOrigenDisponibles"
                  filter
                  placeholder="Seleccione país origen"
                  class="w-full sga-select"
                  @change="handlePaisOrigenChange"
                />
              </div>

              <div class="field-group">
                <label class="form-label">Terminal / Puerto de Origen *</label>
                <Select
                  v-model="form.idPuertoEnvio"
                  :options="puertosOrigenFiltrados"
                  option-label="nombre"
                  option-value="idPuerto"
                  filter
                  :disabled="!paisOrigen"
                  placeholder="Seleccione terminal"
                  class="w-full sga-select"
                >
                  <template #option="{ option }">
                    <div class="port-option-row">
                      <span class="port-code">{{ option.codigoPuerto }}</span>
                      <span class="port-name">{{ option.nombre }}</span>
                      <span class="port-type">{{ option.tipo }}</span>
                    </div>
                  </template>
                </Select>
              </div>
            </div>

            <!-- Cascada Destino: País -> Puerto/Aeropuerto -->
            <div class="two-fields-row">
              <div class="field-group">
                <label class="form-label">País de Destino / Llegada</label>
                <Select
                  v-model="paisDestino"
                  :options="paisesDestinoDisponibles"
                  filter
                  placeholder="Seleccione país destino"
                  class="w-full sga-select"
                  @change="handlePaisDestinoChange"
                />
              </div>

              <div class="field-group">
                <label class="form-label">Terminal / Puerto de Destino *</label>
                <Select
                  v-model="form.idPuertoDestino"
                  :options="puertosDestinoFiltrados"
                  option-label="nombre"
                  option-value="idPuerto"
                  filter
                  :disabled="!paisDestino"
                  placeholder="Seleccione terminal"
                  class="w-full sga-select"
                >
                  <template #option="{ option }">
                    <div class="port-option-row">
                      <span class="port-code">{{ option.codigoPuerto }}</span>
                      <span class="port-name">{{ option.nombre }}</span>
                      <span class="port-type">{{ option.tipo }}</span>
                    </div>
                  </template>
                </Select>
              </div>
            </div>

            <!-- Tipo de Carga y Mercancía -->
            <div class="two-fields-row mt-2">
              <div class="field-group">
                <label class="form-label">Tipo de Carga</label>
                <Select
                  v-model="form.idTipoCarga"
                  :options="tiposCarga"
                  option-label="nombre"
                  option-value="idTipoCarga"
                  filter
                  placeholder="Tipo de carga"
                  class="w-full sga-select"
                />
              </div>

              <div class="field-group">
                <label class="form-label">Clasificación Arancelaria / Mercancía</label>
                <Select
                  v-model="form.idMercancia"
                  :options="mercancias"
                  option-label="nombre"
                  option-value="idMercancia"
                  filter
                  placeholder="Seleccione mercancía"
                  class="w-full sga-select"
                  @change="handleMercanciaChange"
                >
                  <template #option="{ option }">
                    <div class="merc-option-row">
                      <strong>{{ option.nombre }}</strong>
                      <span v-if="option.arancelBasePct !== undefined" class="merc-arancel">
                        Arancel: {{ option.arancelBasePct }}%
                      </span>
                    </div>
                  </template>
                </Select>
              </div>
            </div>

            <!-- Días de Validez y Días Libres -->
            <div class="two-fields-row mt-2">
              <div class="field-group">
                <label class="form-label">Días de Validez de Oferta</label>
                <input
                  v-model.number="form.validezDias"
                  type="number"
                  min="1"
                  max="90"
                  class="sga-input"
                />
              </div>

              <div class="field-group">
                <label class="form-label">Transporte Interno Terrestre</label>
                <div class="checkbox-box">
                  <input
                    id="chk-transporte"
                    v-model="form.servicioTransporte"
                    type="checkbox"
                    class="sga-checkbox"
                  />
                  <label for="chk-transporte" class="chk-label">Incluir flete terrestre local</label>
                </div>
              </div>
            </div>

            <!-- Si incluye transporte interno, mostrar costo -->
            <div v-if="form.servicioTransporte" class="field-group mt-2">
              <label class="form-label">Costo Transporte Interno (USD)</label>
              <input
                v-model.number="form.costoTransporteInterno"
                type="number"
                step="0.01"
                min="0"
                class="sga-input"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- ======================================================== -->
      <!-- COLUMNA 2: ESTRUCTURA ECONÓMICA Y LIQUIDACIÓN TRIBUTARIA  -->
      <!-- ======================================================== -->
      <div class="grid-column">
        <div class="correction-card economics-card">
          <div class="card-header">
            <div class="card-icon-tag"><i class="pi pi-dollar"></i></div>
            <div>
              <h4 class="card-title">3. Valoración Económica y Tributos</h4>
              <p class="card-subtitle">Ajuste de montos en USD con liquidación SUNAT en tiempo real</p>
            </div>
          </div>

          <div class="card-body">
            <!-- Entradas Económicas Principales -->
            <div class="economic-inputs-grid">
              <div class="field-group">
                <label class="form-label">Valor Mercancía (FOB) USD *</label>
                <div class="input-money-wrapper">
                  <span class="currency-prefix">$</span>
                  <input
                    v-model.number="form.valorFob"
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="0.00"
                    class="sga-input money-input"
                  />
                </div>
              </div>

              <div class="field-group">
                <label class="form-label">Flete Internacional Estimado (USD)</label>
                <div class="input-money-wrapper">
                  <span class="currency-prefix">$</span>
                  <input
                    v-model.number="form.montoFleteEstimado"
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="0.00"
                    class="sga-input money-input"
                  />
                </div>
              </div>

              <div class="field-group">
                <div class="label-with-toggle">
                  <label class="form-label">Seguro de Transporte (USD)</label>
                  <button
                    v-if="form.regimenAduanero !== 'EXPORTACION_DEFINITIVA'"
                    type="button"
                    class="btn-toggle-seguro"
                    @click="toggleSeguroManual"
                  >
                    {{ seguroManual ? 'Usar Tabla SUNAT' : 'Editar Manual' }}
                  </button>
                </div>
                <div class="input-money-wrapper">
                  <span class="currency-prefix">$</span>
                  <input
                    v-model.number="form.montoSeguroEstimado"
                    type="number"
                    step="0.01"
                    min="0"
                    :readonly="!seguroManual && form.regimenAduanero !== 'EXPORTACION_DEFINITIVA'"
                    class="sga-input money-input"
                  />
                </div>
                <span class="input-caption">
                  {{ seguroManual ? 'Modo de cálculo manual' : 'Calculado según tabla de seguros SUNAT' }}
                </span>
              </div>

              <div class="field-group">
                <label class="form-label">Comisión Agenciamiento / Honorarios (USD)</label>
                <div class="input-money-wrapper">
                  <span class="currency-prefix">$</span>
                  <input
                    v-model.number="form.montoHonorariosEstimado"
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="0.00"
                    class="sga-input money-input"
                  />
                </div>
              </div>
            </div>

            <!-- Tasas Arancelarias -->
            <div v-if="form.regimenAduanero !== 'EXPORTACION_DEFINITIVA'" class="two-fields-row mt-3">
              <div class="field-group">
                <label class="form-label">Tasa Ad-Valórem (%)</label>
                <Select
                  v-model="form.tasaAdValorem"
                  :options="tasasAdValoremOptions"
                  option-label="label"
                  option-value="value"
                  class="w-full sga-select"
                />
              </div>

              <div class="field-group">
                <label class="form-label">Tasa Percepción SUNAT (%)</label>
                <Select
                  v-model="form.tasaPercepcion"
                  :options="tasasPercepcionOptions"
                  option-label="label"
                  option-value="value"
                  class="w-full sga-select"
                />
              </div>
            </div>

            <!-- Tarjeta de Liquidación y Desglose en Vivo -->
            <div class="liquidation-summary-box mt-4">
              <div class="liq-header">
                <span class="liq-kicker">LIQUIDACIÓN ESTIMADA ADUANERA</span>
                <strong class="liq-title">Cálculo Reactivo SUNAT</strong>
              </div>

              <div class="liq-rows-list">
                <div class="liq-row">
                  <span class="liq-label">Valor CIF Calculado (FOB + Flete + Seguro):</span>
                  <strong class="liq-val">${{ formatMoney(valorCif) }} USD</strong>
                </div>

                <template v-if="form.regimenAduanero !== 'EXPORTACION_DEFINITIVA'">
                  <div class="liq-row">
                    <span class="liq-label">Ad-Valórem (Arancel {{ form.tasaAdValorem }}%):</span>
                    <span class="liq-val">${{ formatMoney(montoAdValorem) }} USD</span>
                  </div>

                  <div class="liq-row">
                    <span class="liq-label">IGV (16%) + IPM (2%) = 18%:</span>
                    <span class="liq-val">${{ formatMoney(montoIgv) }} USD</span>
                  </div>

                  <div class="liq-row">
                    <span class="liq-label">Percepción SUNAT ({{ form.tasaPercepcion }}%):</span>
                    <span class="liq-val">${{ formatMoney(montoPercepcion) }} USD</span>
                  </div>

                  <div class="liq-row liq-subtotal-tributos">
                    <span class="liq-label">Total Tributos de Importación:</span>
                    <strong class="liq-val">${{ formatMoney(form.montoTributosEstimado) }} USD</strong>
                  </div>
                </template>
                <div v-else class="liq-row export-exempt-badge">
                  <i class="pi pi-check-circle"></i>
                  <span>Régimen de Exportación Definitiva: Inafecto de Tributos Arancelarios</span>
                </div>
              </div>

              <div class="liq-grand-total">
                <div class="total-label-box">
                  <span class="total-kicker">IMPORTE TOTAL DE LA PROPUESTA</span>
                  <span class="total-caption">(Incluye tributos, flete, logística y honorarios)</span>
                </div>
                <div class="total-amount-box">
                  <span class="total-currency">USD</span>
                  <span class="total-number">${{ formatMoney(form.totalCotizado) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ======================================================== -->
    <!-- SECCIÓN ANCHA INFERIOR: DOCUMENTACIÓN Y SUBSANACIÓN      -->
    <!-- ======================================================== -->
    <div class="correction-bottom-grid">
      <!-- Tarjeta C: Documentación Sustentatoria -->
      <div class="correction-card">
        <div class="card-header">
          <div class="card-icon-tag"><i class="pi pi-paperclip"></i></div>
          <div>
            <h4 class="card-title">4. Documentación y Expediente Digital</h4>
            <p class="card-subtitle">Visualice los adjuntos actuales y cargue nuevos sustentos solicitados</p>
          </div>
        </div>

        <div class="card-body">
          <!-- Documentos Existentes -->
          <div class="docs-section-title">Documentos Adjuntos Actuales:</div>
          <div v-if="loadingDocs" class="docs-loading">
            <i class="pi pi-spin pi-spinner"></i>
            <span>Cargando expediente digital...</span>
          </div>
          <div v-else-if="existingDocs.length === 0" class="docs-empty-state">
            <i class="pi pi-folder-open"></i>
            <span>No hay documentos adjuntos previos registrados para esta cotización.</span>
          </div>
          <div v-else class="existing-docs-list">
            <div v-for="doc in existingDocs" :key="doc.idDocumento" class="existing-doc-chip">
              <i class="pi pi-file-pdf doc-icon"></i>
              <div class="doc-info">
                <strong class="doc-name">{{ doc.nombreOriginal || doc.nombreArchivo }}</strong>
                <span class="doc-meta">{{ doc.categoria || 'SUSTENTO' }} • {{ doc.tamanoBytes ? (doc.tamanoBytes / 1024).toFixed(1) + ' KB' : '' }}</span>
              </div>
              <button
                type="button"
                class="btn-doc-download"
                title="Descargar archivo"
                @click="handleDownloadDoc(doc)"
              >
                <i class="pi pi-download"></i>
              </button>
            </div>
          </div>

          <!-- Cola de Nuevos Documentos a Subir -->
          <div class="docs-section-title mt-4">Adjuntar Nuevos Documentos para Subsanación:</div>
          <div class="doc-upload-dropzone">
            <input
              id="file-upload-correction"
              type="file"
              multiple
              class="hidden-file-input"
              @change="handleFilesSelected"
            />
            <label for="file-upload-correction" class="upload-dropzone-label">
              <i class="pi pi-cloud-upload upload-icon"></i>
              <span class="upload-main-text">Haga clic o arrastre aquí archivos PDF, XML o imágenes</span>
              <span class="upload-sub-text">Factura Comercial, Packing List, BL / Guía Aérea rectificada</span>
            </label>
          </div>

          <!-- Lista de archivos en cola -->
          <div v-if="queuedDocs.length > 0" class="queued-docs-list mt-3">
            <div v-for="(qDoc, idx) in queuedDocs" :key="idx" class="queued-doc-item">
              <i class="pi pi-file"></i>
              <span class="qdoc-name">{{ qDoc.file.name }}</span>
              <Select
                v-model="qDoc.categoria"
                :options="categoriasDocOptions"
                class="qdoc-category-select"
              />
              <button type="button" class="btn-remove-qdoc" @click="removeQueuedDoc(idx)">
                <i class="pi pi-trash"></i>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Tarjeta D: Nota de Subsanación Comercial -->
      <div class="correction-card">
        <div class="card-header">
          <div class="card-icon-tag"><i class="pi pi-comment"></i></div>
          <div>
            <h4 class="card-title">5. Nota de Subsanación Comercial</h4>
            <p class="card-subtitle">Detalle al Supervisor Aduanero las modificaciones y correcciones efectuadas</p>
          </div>
        </div>

        <div class="card-body">
          <div class="field-group">
            <label class="form-label">Explicación de las Correcciones Realizadas *</label>
            <textarea
              v-model="notaSubsanacion"
              rows="5"
              placeholder="Ejemplo: Se corrigió el flete marítimo según cotización de naviera, se actualizó la subpartida arancelaria al 6% y se adjuntó el Packing List firmado por el consignatario."
              class="sga-textarea"
            ></textarea>
            <span class="input-caption">
              Esta nota se añadirá al historial de la cotización para que el supervisor valide rápidamente los puntos subsanados.
            </span>
          </div>

          <div class="subsanacion-actions-footer mt-4">
            <button
              type="button"
              class="btn-cancel-flat"
              :disabled="submitting"
              @click="$emit('cancel')"
            >
              Cancelar
            </button>
            <button
              type="button"
              class="btn-submit-correction"
              :disabled="submitting"
              @click="handleSubmitCorrection"
            >
              <i v-if="submitting" class="pi pi-spin pi-spinner"></i>
              <i v-else class="pi pi-check-circle"></i>
              <span>{{ submitting ? 'Guardando y reingresando...' : 'Confirmar y Reenviar a Supervisión' }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, toRef } from 'vue'
import Select from 'primevue/select'
import { useCotizacionCorrection } from '../composables/useCotizacionCorrection'
import { catalogoService } from '@/features/admin/catalogos/services/catalogoService'
import { documentoService } from '@/features/documentos/services/documentoService'
import { formatMoney } from '../composables/useCotizacionCalculator'

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

// Catálogos
const puertos = ref([])
const tiposCarga = ref([])
const mercancias = ref([])
const loadingCatalogos = ref(false)
const submitting = ref(false)
const validationError = ref('')

// Documentación
const existingDocs = ref([])
const loadingDocs = ref(false)
const queuedDocs = ref([])

// Opciones arancelarias fijas
const tasasAdValoremOptions = [
  { label: '0% (Exonerado / TLC)', value: 0 },
  { label: '6% (Tasa Estándar)', value: 6 },
  { label: '11% (Bienes Sensibles)', value: 11 }
]

const tasasPercepcionOptions = [
  { label: '0% (Exportación / Inafecto)', value: 0 },
  { label: '3.5% (Importador Habitual)', value: 3.5 },
  { label: '5.0% (Bienes Usados)', value: 5.0 },
  { label: '10.0% (Primera Importación)', value: 10.0 }
]

const categoriasDocOptions = [
  'FACTURA_COMERCIAL',
  'PACKING_LIST',
  'DOCUMENTO_TRANSPORTE',
  'CERTIFICADO_ORIGEN',
  'SUSTENTO_ADUANERO',
  'OTRO'
]

// Instanciar composable de corrección
const cotizacionRef = toRef(props, 'cotizacion')
const {
  form,
  motivoObservacion,
  notaSubsanacion,
  seguroManual,
  modalidadTransporte,
  paisOrigen,
  paisDestino,
  paisesOrigenDisponibles,
  paisesDestinoDisponibles,
  puertosOrigenFiltrados,
  puertosDestinoFiltrados,
  setModalidad,
  aplicarReglasRegimen,
  valorCif,
  montoAdValorem,
  montoIgv,
  montoPercepcion,
  recalcularLiquidacion,
  validateCorrection,
  buildSubmissionPayload
} = useCotizacionCorrection({
  cotizacion: cotizacionRef,
  puertos
})

// Desenvolver respuestas de API
const unwrapList = (res) => {
  if (!res) return []
  if (Array.isArray(res)) return res
  if (Array.isArray(res.data)) return res.data
  return []
}

// Cargar catálogos auxiliares
const loadCatalogos = async () => {
  loadingCatalogos.value = true
  try {
    const [puertosRes, tiposRes, mercanciasRes] = await Promise.allSettled([
      catalogoService.getPuertos(true),
      catalogoService.getTiposCarga(true),
      catalogoService.getMercancias(true)
    ])

    if (puertosRes.status === 'fulfilled') {
      puertos.value = unwrapList(puertosRes.value)
    }
    if (tiposRes.status === 'fulfilled') {
      tiposCarga.value = unwrapList(tiposRes.value)
    }
    if (mercanciasRes.status === 'fulfilled') {
      mercancias.value = unwrapList(mercanciasRes.value)
    }
  } catch (err) {
    console.error('Error cargando catálogos en CotizacionCorrectionView:', err)
  } finally {
    loadingCatalogos.value = false
  }
}

// Cargar documentos existentes
const loadExistingDocuments = async () => {
  if (!props.cotizacion?.idCotizacion) return
  loadingDocs.value = true
  try {
    const docs = await documentoService.getDocumentos({
      entidadTipo: 'COTIZACION',
      idCotizacion: props.cotizacion.idCotizacion
    })
    existingDocs.value = Array.isArray(docs) ? docs : unwrapList(docs)
  } catch (err) {
    console.error('Error al cargar documentos adjuntos:', err)
    existingDocs.value = []
  } finally {
    loadingDocs.value = false
  }
}

const handleDownloadDoc = async (doc) => {
  try {
    const blob = await documentoService.download(doc.idDocumento)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = doc.nombreOriginal || doc.nombreArchivo || 'documento.pdf'
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
  } catch (err) {
    console.error('Error al descargar archivo:', err)
  }
}

// Manejadores de cascada
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
  if (form.value.idMercancia) {
    const found = mercancias.value.find((m) => m.idMercancia === form.value.idMercancia)
    if (found && found.arancelBasePct !== undefined && found.arancelBasePct !== null) {
      const arancel = Number(found.arancelBasePct)
      if ([0, 6, 11].includes(arancel)) {
        form.value.tasaAdValorem = arancel
      }
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

const toggleSeguroManual = () => {
  seguroManual.value = !seguroManual.value
  recalcularLiquidacion()
}

// Manejo de carga de archivos en cola
const handleFilesSelected = (event) => {
  const files = Array.from(event.target.files || [])
  for (const f of files) {
    queuedDocs.value.push({
      file: f,
      categoria: 'SUSTENTO_ADUANERO',
      descripcion: ''
    })
  }
  event.target.value = ''
}

const removeQueuedDoc = (idx) => {
  queuedDocs.value.splice(idx, 1)
}

// Envío de la corrección
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
    documentos: queuedDocs.value,
    done: () => {
      submitting.value = false
    }
  })
}

onMounted(() => {
  loadCatalogos()
  loadExistingDocuments()
})
</script>

<style scoped>
.cotizacion-correction-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  padding-bottom: var(--space-10, 2.5rem);
  width: 100%;
}

/* 1. Header Bar */
.correction-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-6, 1.5rem);
  padding: var(--space-5, 1.25rem) var(--space-6, 1.5rem);
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e2e8f0);
  border-radius: var(--radius-xl, 1rem);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  flex-wrap: wrap;
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-4, 1rem);
}

.btn-back-action {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1rem;
  background: var(--color-bg-muted, #f8fafc);
  border: 1px solid var(--color-border, #cbd5e1);
  border-radius: 0.5rem;
  color: var(--color-text, #1e293b);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-back-action:hover {
  background: #f1f5f9;
  border-color: #94a3b8;
}

.badges-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}

.badge-code {
  font-family: var(--font-mono, monospace);
  font-size: 0.85rem;
  font-weight: 700;
  padding: 0.2rem 0.55rem;
  background: #3b2a1a;
  color: #f7d399;
  border-radius: 0.35rem;
  letter-spacing: 0.04em;
}

.badge-status-observed {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.2rem 0.55rem;
  background: #fffbeb;
  color: #b45309;
  border: 1px solid #fde68a;
  border-radius: 0.35rem;
}

.header-main-title {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 800;
  color: var(--color-text-emphasis, #0f172a);
}

.header-subtitle {
  margin: 0.15rem 0 0 0;
  font-size: 0.88rem;
  color: var(--color-text-muted, #64748b);
}

.header-right-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.btn-cancel-flat {
  padding: 0.65rem 1.2rem;
  background: transparent;
  border: 1px solid var(--color-border, #cbd5e1);
  border-radius: 0.6rem;
  color: var(--color-text, #334155);
  font-weight: 600;
  cursor: pointer;
}

.btn-cancel-flat:hover {
  background: #f8fafc;
}

.btn-submit-correction {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1.4rem;
  background: linear-gradient(135deg, #43302b 0%, #241411 100%);
  border: 1px solid #d4af37;
  color: #fff8eb;
  border-radius: 0.6rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(36, 20, 17, 0.25);
  transition: all 0.2s ease;
}

.btn-submit-correction:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(36, 20, 17, 0.35);
}

.btn-submit-correction:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Error Banner */
.error-banner {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.85rem 1.25rem;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
  border-radius: 0.75rem;
}

.error-content {
  flex: 1;
  font-size: 0.9rem;
}

.error-close-btn {
  background: transparent;
  border: none;
  color: #991b1b;
  cursor: pointer;
}

/* 2. Banner de Observación */
.observation-master-banner {
  display: flex;
  align-items: flex-start;
  gap: 1.25rem;
  padding: 1.25rem 1.5rem;
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  border: 2px solid #f59e0b;
  border-radius: 1rem;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.12);
}

.obs-icon-circle {
  width: 2.75rem;
  height: 2.75rem;
  border-radius: 50%;
  background: #f59e0b;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.3rem;
  box-shadow: 0 2px 6px rgba(245, 158, 11, 0.3);
}

.obs-banner-content {
  flex: 1;
}

.obs-kicker-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.35rem;
  flex-wrap: wrap;
}

.obs-kicker {
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: #b45309;
}

.obs-cliente-tag {
  font-size: 0.8rem;
  font-weight: 700;
  padding: 0.15rem 0.6rem;
  background: #ffffff;
  border: 1px solid #fcd34d;
  color: #92400e;
  border-radius: 1rem;
}

.obs-title {
  margin: 0 0 0.4rem 0;
  font-size: 1.05rem;
  font-weight: 800;
  color: #78350f;
}

.obs-reason-text {
  margin: 0 0 0.65rem 0;
  font-size: 1rem;
  line-height: 1.5;
  color: #451a03;
  background: rgba(255, 255, 255, 0.7);
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  border-left: 4px solid #b45309;
  font-style: italic;
}

.obs-instruction-hint {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-size: 0.82rem;
  color: #92400e;
}

/* 3. Grid Principal (2 Columnas) */
.correction-main-grid {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: var(--space-6, 1.5rem);
}

@media (max-width: 1024px) {
  .correction-main-grid {
    grid-template-columns: 1fr;
  }
}

.grid-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
}

/* Tarjetas */
.correction-card {
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e2e8f0);
  border-radius: var(--radius-xl, 1rem);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 1rem 1.25rem;
  background: #fafaf9;
  border-bottom: 1px solid #f1f5f9;
}

.card-icon-tag {
  width: 2.2rem;
  height: 2.2rem;
  border-radius: 0.5rem;
  background: #3b2a1a;
  color: #e6c894;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.05rem;
}

.card-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: #1e293b;
}

.card-subtitle {
  margin: 0.1rem 0 0 0;
  font-size: 0.78rem;
  color: #64748b;
}

.card-body {
  padding: 1.25rem;
}

/* Client summary */
.client-summary-box {
  padding: 0.85rem 1rem;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 0.75rem;
}

.client-meta {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
  font-size: 0.9rem;
}

.client-label {
  color: #64748b;
  font-size: 0.82rem;
}

.client-name {
  color: #0f172a;
}

.client-doc {
  font-size: 0.78rem;
  background: #e2e8f0;
  padding: 0.1rem 0.45rem;
  border-radius: 0.35rem;
  color: #475569;
}

/* Campos */
.field-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.form-label {
  font-size: 0.82rem;
  font-weight: 700;
  color: #334155;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.form-label-sub {
  font-size: 0.78rem;
  font-weight: 600;
  color: #64748b;
}

.two-fields-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

@media (max-width: 640px) {
  .two-fields-row {
    grid-template-columns: 1fr;
  }
}

.sga-input,
.sga-textarea {
  width: 100%;
  padding: 0.65rem 0.85rem;
  border: 1px solid #cbd5e1;
  border-radius: 0.5rem;
  font-size: 0.92rem;
  color: #0f172a;
  background: #ffffff;
  outline: none;
  transition: border-color 0.2s;
}

.sga-input:focus,
.sga-textarea:focus {
  border-color: #3b2a1a;
  box-shadow: 0 0 0 2px rgba(59, 42, 26, 0.15);
}

.sga-textarea {
  resize: vertical;
}

/* Regimen Selector Pills */
.regimen-selector-pills {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.6rem;
}

.pill-opt {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  padding: 0.7rem 0.85rem;
  border: 1px solid #cbd5e1;
  border-radius: 0.6rem;
  background: #ffffff;
  color: #475569;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.pill-opt.active {
  background: #3b2a1a;
  border-color: #3b2a1a;
  color: #f7d399;
}

/* Segmented Mode Selector */
.segmented-mode-selector {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
  background: #f1f5f9;
  padding: 0.35rem;
  border-radius: 0.65rem;
}

.mode-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.65rem 0.75rem;
  border: none;
  border-radius: 0.5rem;
  background: transparent;
  color: #64748b;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.mode-btn.active {
  background: #ffffff;
  color: #0f172a;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

/* Ports Option Row */
.port-option-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
}

.port-code {
  font-family: monospace;
  font-weight: 700;
  font-size: 0.8rem;
  background: #f1f5f9;
  padding: 0.1rem 0.35rem;
  border-radius: 0.25rem;
}

.port-name {
  flex: 1;
}

.port-type {
  font-size: 0.72rem;
  color: #64748b;
}

/* Mercancía Option Row */
.merc-option-row {
  display: flex;
  justify-content: space-between;
  width: 100%;
}

.merc-arancel {
  font-size: 0.75rem;
  color: #059669;
  font-weight: 700;
}

/* Money Input */
.economic-inputs-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.85rem;
}

.input-money-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.currency-prefix {
  position: absolute;
  left: 0.85rem;
  font-weight: 700;
  color: #64748b;
}

.money-input {
  padding-left: 2rem;
  font-family: var(--font-mono, monospace);
  font-weight: 700;
  font-size: 1rem;
}

.label-with-toggle {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.btn-toggle-seguro {
  background: none;
  border: none;
  color: #2563eb;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  text-decoration: underline;
}

.input-caption {
  font-size: 0.74rem;
  color: #64748b;
}

.checkbox-box {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 0;
}

.chk-label {
  font-size: 0.85rem;
  color: #334155;
  cursor: pointer;
}

/* Liquidación Box */
.liquidation-summary-box {
  background: #fafaf9;
  border: 1px solid #e7e5e4;
  border-radius: 0.85rem;
  padding: 1.25rem;
}

.liq-header {
  border-bottom: 1px solid #e7e5e4;
  padding-bottom: 0.6rem;
  margin-bottom: 0.75rem;
}

.liq-kicker {
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: #78716c;
}

.liq-title {
  display: block;
  font-size: 1.05rem;
  color: #1c1917;
}

.liq-rows-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.liq-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.88rem;
  color: #44403c;
}

.liq-subtotal-tributos {
  margin-top: 0.4rem;
  padding-top: 0.5rem;
  border-top: 1px dashed #d6d3d1;
  color: #1c1917;
}

.export-exempt-badge {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.6rem 0.85rem;
  background: #ecfdf5;
  color: #065f46;
  border-radius: 0.5rem;
  font-weight: 600;
  font-size: 0.82rem;
}

.liq-grand-total {
  margin-top: 1rem;
  padding: 1rem 1.25rem;
  background: linear-gradient(135deg, #2b1f1d 0%, #170d0b 100%);
  border-radius: 0.75rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #ffffff;
}

.total-kicker {
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.06em;
  color: #d4af37;
  display: block;
}

.total-caption {
  font-size: 0.72rem;
  color: #a8a29e;
}

.total-currency {
  font-size: 0.85rem;
  color: #d4af37;
  font-weight: 700;
  margin-right: 0.35rem;
}

.total-number {
  font-size: 1.55rem;
  font-weight: 900;
  color: #ffffff;
  font-family: var(--font-mono, monospace);
}

/* 4. Bottom Grid (Documentos y Subsanación) */
.correction-bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-6, 1.5rem);
}

@media (max-width: 1024px) {
  .correction-bottom-grid {
    grid-template-columns: 1fr;
  }
}

.docs-section-title {
  font-size: 0.82rem;
  font-weight: 700;
  color: #475569;
  margin-bottom: 0.5rem;
}

.existing-docs-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.existing-doc-chip {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.65rem 0.85rem;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 0.5rem;
}

.doc-icon {
  font-size: 1.4rem;
  color: #dc2626;
}

.doc-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.doc-name {
  font-size: 0.88rem;
  color: #0f172a;
}

.doc-meta {
  font-size: 0.72rem;
  color: #64748b;
}

.btn-doc-download {
  background: transparent;
  border: 1px solid #cbd5e1;
  border-radius: 0.35rem;
  padding: 0.35rem 0.6rem;
  color: #334155;
  cursor: pointer;
}

.btn-doc-download:hover {
  background: #f1f5f9;
}

.docs-empty-state {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 0.5rem;
  font-size: 0.85rem;
  color: #64748b;
}

.doc-upload-dropzone {
  position: relative;
  border: 2px dashed #cbd5e1;
  border-radius: 0.75rem;
  padding: 1.25rem 1rem;
  text-align: center;
  background: #fafafa;
  transition: border-color 0.2s;
}

.hidden-file-input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

.upload-dropzone-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.35rem;
  cursor: pointer;
}

.upload-icon {
  font-size: 1.8rem;
  color: #3b2a1a;
}

.upload-main-text {
  font-size: 0.88rem;
  font-weight: 700;
  color: #1e293b;
}

.upload-sub-text {
  font-size: 0.76rem;
  color: #64748b;
}

.queued-docs-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.queued-doc-item {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.5rem 0.75rem;
  background: #f1f5f9;
  border-radius: 0.5rem;
  font-size: 0.85rem;
}

.qdoc-name {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.qdoc-category-select {
  width: 180px;
  font-size: 0.8rem;
}

.btn-remove-qdoc {
  background: none;
  border: none;
  color: #ef4444;
  cursor: pointer;
}

.subsanacion-actions-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.mt-2 { margin-top: 0.5rem; }
.mt-3 { margin-top: 0.75rem; }
.mt-4 { margin-top: 1rem; }
.w-full { width: 100%; }
.fade-in {
  animation: fadeIn 0.25s ease-in-out;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
