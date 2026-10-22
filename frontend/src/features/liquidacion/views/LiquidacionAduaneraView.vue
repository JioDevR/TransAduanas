<template>
  <div class="liquidador-view">
    <!-- 1. Toolbar de Acciones Warm Editorial (Sin Banners Invasivos) -->
    <div class="liquidacion-action-toolbar">
      <div class="toolbar-left-info">
        <span class="badge-kicker">GESTIÓN ADUANERA // LIQUIDACIÓN & DAM SUNAT</span>
        <h3 class="toolbar-title">Liquidación Aduanera, Clasificación y DAM</h3>
      </div>

      <div class="toolbar-actions-group">
        <span class="count-badge font-mono">{{ expedientesList.length }} DAM</span>

        <button
          type="button"
          class="btn-outline-warm"
          @click="loadExpedientes"
          :disabled="loadingExpedientes"
          title="Actualizar datos del servidor"
        >
          <i :class="['pi pi-sync', { 'pi-spin': loadingExpedientes }]"></i>
          <span>Actualizar</span>
        </button>

        <button
          type="button"
          class="btn-espresso"
          @click="switchTab('calculadora')"
        >
          <i class="pi pi-calculator"></i>
          <span>Calculadora CIF</span>
        </button>
      </div>
    </div>

    <!-- Pestañas de Navegación -->
    <div class="module-tabs-nav">
      <button
        type="button"
        class="tab-nav-btn"
        :class="{ active: activeTab === 'declaraciones' }"
        @click="switchTab('declaraciones')"
      >
        <i class="pi pi-file-edit"></i>
        <span>Expedientes & Declaraciones DAM</span>
        <span class="tab-counter font-mono">{{ expedientesList.length }}</span>
      </button>

      <button
        type="button"
        class="tab-nav-btn"
        :class="{ active: activeTab === 'calculadora' }"
        @click="switchTab('calculadora')"
      >
        <i class="pi pi-calculator"></i>
        <span>Calculadora de Tributos CIF</span>
      </button>

      <button
        type="button"
        class="tab-nav-btn"
        :class="{ active: activeTab === 'arancel' }"
        @click="switchTab('arancel')"
      >
        <i class="pi pi-book"></i>
        <span>Nomenclatura y Catálogo Nandina</span>
      </button>
    </div>

    <!-- ===================================================================== -->
    <!-- TAB 1: DECLARACIONES DAM Y EXPEDIENTES EN TRÁMITE                     -->
    <!-- ===================================================================== -->
    <div v-if="activeTab === 'declaraciones'" class="tab-container">
      <!-- Métricas Rápidas KPI (Warm Editorial) -->
      <div class="metrics-kpi-grid">
        <div class="kpi-card warning-card">
          <div class="kpi-card-header">
            <span class="kpi-num">01</span>
            <span class="kpi-label">Pendientes Clasificación</span>
            <i class="pi pi-clock kpi-icon warning"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono text-warning">{{ expedientesPendientesCount }}</span>
            <span class="kpi-badge warning">En Revisión</span>
          </div>
          <div class="kpi-footer-note">
            <span>Requieren clasificación Nandina</span>
          </div>
        </div>

        <div class="kpi-card pending-card">
          <div class="kpi-card-header">
            <span class="kpi-num">02</span>
            <span class="kpi-label">Canal Naranja / Rojo</span>
            <i class="pi pi-exclamation-triangle kpi-icon pending"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono text-pending">{{ expedientesCanalInspeccionCount }}</span>
            <span class="kpi-badge pending">Inspección</span>
          </div>
          <div class="kpi-footer-note">
            <span>Control documentario o físico</span>
          </div>
        </div>

        <div class="kpi-card success-card">
          <div class="kpi-card-header">
            <span class="kpi-num">03</span>
            <span class="kpi-label">Levante Conforme</span>
            <i class="pi pi-check-circle kpi-icon success"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono text-success">{{ expedientesLevanteCount }}</span>
            <span class="kpi-badge success">Autorizado</span>
          </div>
          <div class="kpi-footer-note">
            <span>Mercancía liberada por SUNAT</span>
          </div>
        </div>

        <div class="kpi-card accent-card">
          <div class="kpi-card-header">
            <span class="kpi-num">04</span>
            <span class="kpi-label">Total Expedientes DAM</span>
            <i class="pi pi-file-edit kpi-icon accent"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono">{{ expedientesList.length }}</span>
            <span class="kpi-badge accent">En Cartera</span>
          </div>
          <div class="kpi-footer-note">
            <span>Declaraciones en despacho aduanero</span>
          </div>
        </div>
      </div>

      <!-- Tabla de Expedientes para Liquidación -->
      <div class="content-table-card">
        <div class="card-header-bar">
          <div>
            <span class="badge-kicker">BANDEJA DE DESPACHO ADUANERO</span>
            <h3 class="section-title">Expedientes para Liquidación y Declaración DAM</h3>
          </div>

          <div class="header-actions">
            <select v-model="filterCanal" class="filter-select">
              <option value="">Todos los Canales</option>
              <option value="VERDE">Canal Verde</option>
              <option value="NARANJA">Canal Naranja</option>
              <option value="ROJO">Canal Rojo</option>
              <option value="PENDIENTE">Canal Pendiente</option>
            </select>

            <div class="search-input-wrap">
              <i class="pi pi-search search-icon"></i>
              <input
                v-model="searchQuery"
                type="text"
                class="search-input"
                placeholder="Buscar por código, DAM o cliente..."
              />
            </div>

            <button type="button" class="btn-refresh" @click="loadExpedientes" :disabled="loadingExpedientes">
              <i :class="['pi pi-sync', { 'pi-spin': loadingExpedientes }]"></i>
              <span>Actualizar</span>
            </button>
          </div>
        </div>

        <div class="table-container">
          <DataTable
            :value="filteredExpedientes"
            :loading="loadingExpedientes"
            responsiveLayout="scroll"
            class="custom-datatable"
            dataKey="idExpediente"
          >
            <template #empty>
              <div class="empty-state">
                <i class="pi pi-inbox empty-icon"></i>
                <p class="empty-title">No hay expedientes aduaneros</p>
                <p class="empty-desc">Los expedientes aperturados por Supervisión aparecerán aquí para formular la DAM.</p>
              </div>
            </template>

            <Column header="Expediente / Fecha">
              <template #body="{ data: exp }">
                <div class="code-cell">
                  <span class="font-mono font-bold">{{ exp.codigoExpediente }}</span>
                  <span class="text-xs text-muted">{{ formatDate(exp.fechaApertura) }}</span>
                </div>
              </template>
            </Column>

            <Column header="N° Declaración (DAM)">
              <template #body="{ data: exp }">
                <span v-if="exp.numeroDam" class="font-mono font-bold text-accent">{{ exp.numeroDam }}</span>
                <Tag v-else severity="warning" value="SIN NUMERAR" rounded class="tag-xs" />
              </template>
            </Column>

            <Column header="Cliente Importador">
              <template #body="{ data: exp }">
                <div class="client-cell">
                  <span class="client-name">{{ exp.clienteRazonSocial || 'Cliente SGA' }}</span>
                  <span class="client-sub font-mono">RUC: {{ exp.clienteNumeroDocumento || 'N/A' }}</span>
                </div>
              </template>
            </Column>

            <Column header="Mercancía / Bultos">
              <template #body="{ data: exp }">
                <div class="client-cell">
                  <span class="text-xs font-semibold">{{ exp.mercanciaNombre || exp.naveViaje || 'Carga General' }}</span>
                  <span class="client-sub font-mono">Cont: {{ exp.numeroContenedor || 'LCL / Suelta' }}</span>
                </div>
              </template>
            </Column>

            <Column header="Canal SUNAT">
              <template #body="{ data: exp }">
                <span :class="['canal-tag', getCanalClass(exp.canalSunat)]">
                  {{ exp.canalSunat || 'PENDIENTE' }}
                </span>
              </template>
            </Column>

            <Column header="Estado Trámite">
              <template #body="{ data: exp }">
                <Tag
                  :severity="getEstadoSeverity(exp.estadoExpediente)"
                  :value="formatEstado(exp.estadoExpediente)"
                  rounded
                  class="tag-xs"
                />
              </template>
            </Column>

            <Column header="Acciones" headerStyle="width: 130px; text-align: right;" bodyStyle="text-align: right;">
              <template #body="{ data: exp }">
                <div class="action-buttons-cell">
                  <button
                    type="button"
                    class="btn-row-action"
                    title="Liquidar Tributos en Calculadora"
                    @click="cargarEnCalculadora(exp)"
                  >
                    <i class="pi pi-calculator"></i>
                  </button>

                  <button
                    type="button"
                    class="btn-row-action"
                    title="Ver Documentos de Importación"
                    @click="openDocsModal(exp)"
                  >
                    <i class="pi pi-folder-open"></i>
                  </button>
                </div>
              </template>
            </Column>
          </DataTable>
        </div>
      </div>
    </div>

    <!-- ===================================================================== -->
    <!-- TAB 2: CALCULADORA ARANCELARIA Y LIQUIDACIÓN CIF                      -->
    <!-- ===================================================================== -->
    <div v-if="activeTab === 'calculadora'" class="tab-container">
      <div class="calc-wrapper-grid">
        <!-- Formulario de Entrada de Valores Aduaneros -->
        <div class="calc-card form-side">
          <div class="card-header-line">
            <i class="pi pi-sliders-h card-icon"></i>
            <div>
              <span class="badge-kicker">PARÁMETROS DE IMPORTACIÓN</span>
              <h4 class="card-title">Base Imponible y Derechos de Aduana</h4>
            </div>
          </div>

          <div class="calc-form-body">
            <div v-if="selectedExpediente" class="selected-exp-chip">
              <i class="pi pi-link"></i>
              <span>Expediente Vinculado: <strong>{{ selectedExpediente.codigoExpediente }}</strong> ({{ selectedExpediente.clienteRazonSocial }})</span>
              <button type="button" class="btn-clear-link" @click="selectedExpediente = null">
                <i class="pi pi-times"></i>
              </button>
            </div>

            <div class="form-grid-3">
              <div class="form-group">
                <label class="form-label">Valor FOB (USD) *</label>
                <input
                  v-model.number="calc.valorFob"
                  type="number"
                  step="0.01"
                  min="0"
                  class="form-input font-mono font-bold"
                  placeholder="0.00"
                />
              </div>

              <div class="form-group">
                <label class="form-label">Flete Internacional (USD) *</label>
                <input
                  v-model.number="calc.fleteInternacional"
                  type="number"
                  step="0.01"
                  min="0"
                  class="form-input font-mono"
                  placeholder="0.00"
                />
              </div>

              <div class="form-group">
                <label class="form-label">Seguro de Tabla (USD) *</label>
                <input
                  v-model.number="calc.seguroTabla"
                  type="number"
                  step="0.01"
                  min="0"
                  class="form-input font-mono"
                  placeholder="0.00"
                />
              </div>
            </div>

            <div class="form-grid-3">
              <div class="form-group">
                <label class="form-label">Tasa Ad-Valorem *</label>
                <select v-model.number="calc.tasaAdValorem" class="form-select font-mono font-bold">
                  <option :value="0">0% (Insumos / Bienes Capital)</option>
                  <option :value="6">6% (Bienes Intermedios)</option>
                  <option :value="11">11% (Bienes de Consumo Final)</option>
                </select>
              </div>

              <div class="form-group">
                <label class="form-label">Tasa ISC (Si aplica)</label>
                <select v-model.number="calc.tasaIsc" class="form-select font-mono">
                  <option :value="0">0% (Sin Impuesto Selectivo)</option>
                  <option :value="10">10% ISC</option>
                  <option :value="20">20% ISC</option>
                  <option :value="30">30% ISC (Licores / Tabaco)</option>
                </select>
              </div>

              <div class="form-group">
                <label class="form-label">Régimen Percepción SUNAT</label>
                <select v-model.number="calc.tasaPercepcion" class="form-select font-mono">
                  <option :value="3.5">3.5% (Importador Habitual)</option>
                  <option :value="5.0">5.0% (Bienes Usados)</option>
                  <option :value="10.0">10.0% (Primera Importación / No Habido)</option>
                  <option :value="0">0% (Exonerado)</option>
                </select>
              </div>
            </div>

            <div class="form-grid-2">
              <div class="form-group">
                <label class="form-label">Subpartida Nandina Clasificada</label>
                <input
                  v-model="calc.subpartidaNandina"
                  type="text"
                  class="form-input font-mono text-uppercase"
                  placeholder="Ej: 8471.30.00.00"
                  maxlength="15"
                />
              </div>

              <div class="form-group">
                <label class="form-label">Tipo de Cambio SUNAT (USD -> PEN)</label>
                <input
                  v-model.number="calc.tipoCambio"
                  type="number"
                  step="0.001"
                  min="1"
                  class="form-input font-mono font-bold"
                  placeholder="3.750"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Panel de Liquidación de Tributos (Resultados en Vivo) -->
        <div class="calc-card results-side">
          <div class="card-header-line">
            <i class="pi pi-receipt card-icon text-success"></i>
            <div>
              <span class="badge-kicker">LIQUIDACIÓN DE TRIBUTOS</span>
              <h4 class="card-title">Resumen de Liquidación de Derechos</h4>
            </div>
          </div>

          <div class="results-table">
            <div class="res-row">
              <span class="res-label">Valor FOB Total:</span>
              <span class="res-val font-mono">USD {{ formatNumber(calc.valorFob) }}</span>
            </div>
            <div class="res-row">
              <span class="res-label">Flete + Seguro Internacional:</span>
              <span class="res-val font-mono">USD {{ formatNumber(calc.fleteInternacional + calc.seguroTabla) }}</span>
            </div>
            <div class="res-row res-cif-box">
              <span class="res-label font-bold">VALOR CIF ADUANERO (Base Imponible):</span>
              <span class="res-val font-mono font-bold text-accent">USD {{ formatNumber(cifTotal) }}</span>
            </div>

            <div class="res-divider"></div>

            <div class="res-row">
              <span class="res-label">Ad-Valorem ({{ calc.tasaAdValorem }}%):</span>
              <span class="res-val font-mono">USD {{ formatNumber(adValoremMonto) }}</span>
            </div>
            <div v-if="calc.tasaIsc > 0" class="res-row">
              <span class="res-label">Impuesto Selectivo al Consumo ({{ calc.tasaIsc }}%):</span>
              <span class="res-val font-mono">USD {{ formatNumber(iscMonto) }}</span>
            </div>
            <div class="res-row">
              <span class="res-label">IGV Aduanero (16%):</span>
              <span class="res-val font-mono">USD {{ formatNumber(igvMonto) }}</span>
            </div>
            <div class="res-row">
              <span class="res-label">Impuesto Promoción Municipal - IPM (2%):</span>
              <span class="res-val font-mono">USD {{ formatNumber(ipmMonto) }}</span>
            </div>
            <div class="res-row">
              <span class="res-label">Percepción SUNAT ({{ calc.tasaPercepcion }}%):</span>
              <span class="res-val font-mono">USD {{ formatNumber(percepcionMonto) }}</span>
            </div>

            <div class="res-divider"></div>

            <div class="total-box-editorial">
              <div class="total-row-main">
                <span class="total-title">TOTAL DERECHOS Y TRIBUTOS SUNAT:</span>
                <span class="total-amount font-mono">USD {{ formatNumber(totalTributosUsd) }}</span>
              </div>
              <div class="total-row-sub">
                <span>Equivalente en Moneda Nacional (T/C {{ calc.tipoCambio }}):</span>
                <strong class="font-mono text-success">S/ {{ formatNumber(totalTributosPen) }}</strong>
              </div>
            </div>

            <div class="results-actions">
              <button type="button" class="btn-save-sim" @click="guardarEnExpediente">
                <i class="pi pi-check"></i>
                <span>Asentar Liquidación en DAM</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ===================================================================== -->
    <!-- TAB 3: NOMENCLATURA Y CATÁLOGO ARANCELARIO NANDINA                    -->
    <!-- ===================================================================== -->
    <div v-if="activeTab === 'arancel'" class="tab-container">
      <div class="content-table-card">
        <div class="card-header-bar">
          <div>
            <span class="badge-kicker">ARANCEL DE ADUANAS SUNAT</span>
            <h3 class="section-title">Compendio de Subpartidas Nacionales Comunes</h3>
          </div>
        </div>

        <div class="table-container">
          <DataTable :value="nandinaItems" responsiveLayout="scroll" class="custom-datatable">
            <Column field="codigo" header="Subpartida Nacional (Nandina)">
              <template #body="{ data: d }">
                <span class="font-mono font-bold">{{ d.codigo }}</span>
              </template>
            </Column>
            <Column field="descripcion" header="Descripción de la Mercancía" />
            <Column field="adValorem" header="Ad-Valorem">
              <template #body="{ data: d }">
                <Tag :severity="d.adValorem === 0 ? 'success' : 'info'" :value="d.adValorem + '%'" rounded class="tag-xs" />
              </template>
            </Column>
            <Column field="igv" header="IGV + IPM">
              <template #body>
                <span class="font-mono text-xs">18% (16% + 2%)</span>
              </template>
            </Column>
            <Column field="reglas" header="Unidad Física" />
            <Column header="Acción" headerStyle="width: 100px; text-align: center;" bodyStyle="text-align: center;">
              <template #body="{ data: d }">
                <button type="button" class="btn-row-action" title="Usar en Calculadora" @click="aplicarNandina(d)">
                  <i class="pi pi-arrow-right"></i>
                </button>
              </template>
            </Column>
          </DataTable>
        </div>
      </div>
    </div>

    <!-- Gestor Universal de Documentos -->
    <DocumentosManagerModal
      :visible="showDocsModal"
      :entidad-tipo="'EXPEDIENTE'"
      :id-entidad="selectedDocEntidadId"
      :entidad-id="selectedDocEntidadId"
      :codigo-entidad="selectedDocCodigo"
      :titulo-entidad="selectedDocTitulo"
      @close="showDocsModal = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import { useRoute, useRouter } from 'vue-router'
import { gestionAduaneraService } from '@/features/roles-modules/services/gestionAduaneraService'
import DocumentosManagerModal from '@/shared/components/DocumentosManagerModal.vue'
import { useNotification } from '@/shared/composables/useNotification'
import { useNavigationStore } from '@/core/stores/navigationStore'

const route = useRoute()
const router = useRouter()
const { showSuccess, showError, showInfo } = useNotification()
const navigationStore = useNavigationStore()

const activeTab = ref('declaraciones')
const expedientesList = ref([])
const loadingExpedientes = ref(false)
const filterCanal = ref('')
const searchQuery = ref('')
const selectedExpediente = ref(null)

// Gestor de Documentos
const showDocsModal = ref(false)
const selectedDocEntidadId = ref(null)
const selectedDocCodigo = ref('')
const selectedDocTitulo = ref('')

// Calculadora
const calc = ref({
  valorFob: 25000,
  fleteInternacional: 1800,
  seguroTabla: 350,
  tasaAdValorem: 6,
  tasaIsc: 0,
  tasaPercepcion: 3.5,
  subpartidaNandina: '8471.30.00.00',
  tipoCambio: 3.750
})

// Catálogo de Referencia Nandina
const nandinaItems = ref([
  { codigo: '8471.30.00.00', descripcion: 'Máquinas automáticas para tratamiento o procesamiento de datos (Laptops/PCs)', adValorem: 0, reglas: 'Unidad (U)' },
  { codigo: '8517.13.00.00', descripcion: 'Teléfonos inteligentes (Smartphones) y teléfonos para redes celulares', adValorem: 0, reglas: 'Unidad (U)' },
  { codigo: '8704.22.90.00', descripcion: 'Vehículos automóviles para transporte de mercancías de peso total con carga máxima > 5t pero <= 20t', adValorem: 6, reglas: 'Unidad (U)' },
  { codigo: '7214.20.00.00', descripcion: 'Barras de hierro o acero sin alear con muescas o cordones para construcción', adValorem: 6, reglas: 'Kilogramos (KG)' },
  { codigo: '6109.10.00.31', descripcion: 'T-shirts y camisetas de punto de algodón para hombres o niños', adValorem: 11, reglas: 'Unidades (U)' },
  { codigo: '6403.99.90.00', descripcion: 'Calzado con suela de caucho, plástico y parte superior de cuero natural', adValorem: 11, reglas: 'Pares (PAR)' },
  { codigo: '2208.30.00.00', descripcion: 'Whisky y bebidas espirituosas destiladas', adValorem: 6, reglas: 'Litros (L)' }
])

// Cálculos dinámicos
const cifTotal = computed(() => {
  return Number(calc.value.valorFob || 0) + Number(calc.value.fleteInternacional || 0) + Number(calc.value.seguroTabla || 0)
})

const adValoremMonto = computed(() => {
  return cifTotal.value * (Number(calc.value.tasaAdValorem || 0) / 100)
})

const iscMonto = computed(() => {
  const baseIsc = cifTotal.value + adValoremMonto.value
  return baseIsc * (Number(calc.value.tasaIsc || 0) / 100)
})

const baseIgv = computed(() => {
  return cifTotal.value + adValoremMonto.value + iscMonto.value
})

const igvMonto = computed(() => {
  return baseIgv.value * 0.16
})

const ipmMonto = computed(() => {
  return baseIgv.value * 0.02
})

const basePercepcion = computed(() => {
  return cifTotal.value + adValoremMonto.value + iscMonto.value + igvMonto.value + ipmMonto.value
})

const percepcionMonto = computed(() => {
  return basePercepcion.value * (Number(calc.value.tasaPercepcion || 0) / 100)
})

const totalTributosUsd = computed(() => {
  return adValoremMonto.value + iscMonto.value + igvMonto.value + ipmMonto.value + percepcionMonto.value
})

const totalTributosPen = computed(() => {
  return totalTributosUsd.value * Number(calc.value.tipoCambio || 1)
})

// Métricas de Expedientes
const expedientesPendientesCount = computed(() =>
  expedientesList.value.filter(e => !e.numeroDam).length
)
const expedientesCanalInspeccionCount = computed(() =>
  expedientesList.value.filter(e => e.canalSunat === 'NARANJA' || e.canalSunat === 'ROJO').length
)
const expedientesLevanteCount = computed(() =>
  expedientesList.value.filter(e => e.estadoExpediente === 'LEVANTE_AUTORIZADO' || e.estadoExpediente === 'CERRADO').length
)

const filteredExpedientes = computed(() => {
  return expedientesList.value.filter(e => {
    if (filterCanal.value && e.canalSunat !== filterCanal.value) return false
    if (searchQuery.value) {
      const q = searchQuery.value.toLowerCase().trim()
      const cod = (e.codigoExpediente || '').toLowerCase()
      const dam = (e.numeroDam || '').toLowerCase()
      const cli = (e.clienteRazonSocial || '').toLowerCase()
      if (!cod.includes(q) && !dam.includes(q) && !cli.includes(q)) return false
    }
    return true
  })
})

const loadExpedientes = async () => {
  loadingExpedientes.value = true
  try {
    const data = await gestionAduaneraService.getExpedientes({ soloActivos: false })
    expedientesList.value = data || []
  } catch (err) {
    showError('Error al recuperar expedientes aduaneros')
  } finally {
    loadingExpedientes.value = false
  }
}

const switchTab = (tab) => {
  activeTab.value = tab
  const subMap = {
    declaraciones: 'declaraciones',
    arancel: 'clasificacion',
    calculadora: 'tributos'
  }
  const subOption = subMap[tab] || tab
  navigationStore.setGestionAduaneraSubOption(subOption)
  if (route.query.tab !== subOption) {
    router.replace({ query: { ...route.query, tab: subOption } })
  }
}

const cargarEnCalculadora = (exp) => {
  selectedExpediente.value = exp
  activeTab.value = 'calculadora'
  showInfo(`Expediente ${exp.codigoExpediente} cargado en la calculadora arancelaria`)
}

const aplicarNandina = (item) => {
  calc.value.subpartidaNandina = item.codigo
  calc.value.tasaAdValorem = item.adValorem
  activeTab.value = 'calculadora'
  showSuccess(`Subpartida ${item.codigo} seleccionada (${item.adValorem}% Ad-Valorem)`)
}

const guardarEnExpediente = () => {
  if (selectedExpediente.value) {
    showSuccess(`Liquidación calculada para DAM ${selectedExpediente.value.numeroDam || selectedExpediente.value.codigoExpediente}: USD ${formatNumber(totalTributosUsd.value)}`)
  } else {
    showSuccess(`Liquidación arancelaria precalculada: USD ${formatNumber(totalTributosUsd.value)} / S/ ${formatNumber(totalTributosPen.value)}`)
  }
}

const openDocsModal = (exp) => {
  selectedDocEntidadId.value = exp.idExpediente
  selectedDocCodigo.value = exp.codigoExpediente
  selectedDocTitulo.value = `Documentos Aduaneros DAM: ${exp.codigoExpediente}`
  showDocsModal.value = true
}

const formatNumber = (val) => {
  return Number(val || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const formatDate = (val) => {
  if (!val) return '—'
  try {
    return new Date(val).toLocaleDateString('es-PE', { year: 'numeric', month: '2-digit', day: '2-digit' })
  } catch {
    return val
  }
}

const formatEstado = (est) => {
  const map = {
    ASIGNADO: 'Asignado a Agente',
    CANAL_ASIGNADO: 'DAM Numerada',
    INSPECCION: 'En Reconocimiento',
    LEVANTE_AUTORIZADO: 'Levante Autorizado',
    CERRADO: 'Trámite Concluido'
  }
  return map[est] || est || 'En Trámite'
}

const getEstadoSeverity = (est) => {
  switch (est) {
    case 'LEVANTE_AUTORIZADO':
    case 'CERRADO': return 'success'
    case 'CANAL_ASIGNADO': return 'info'
    case 'INSPECCION': return 'warning'
    default: return 'secondary'
  }
}

const getCanalClass = (canal) => {
  switch (canal) {
    case 'VERDE': return 'canal-verde'
    case 'NARANJA': return 'canal-naranja'
    case 'ROJO': return 'canal-rojo'
    default: return 'canal-pendiente'
  }
}

watch(
  () => route.query.tab,
  (newTab) => {
    if (newTab && ['declaraciones', 'clasificacion', 'tributos'].includes(newTab)) {
      activeTab.value = newTab === 'clasificacion' ? 'arancel' : newTab === 'tributos' ? 'calculadora' : 'declaraciones'
      if (navigationStore.gestionAduaneraSubOption !== newTab) {
        navigationStore.setGestionAduaneraSubOption(newTab)
      }
    }
  },
  { immediate: true }
)

watch(
  () => [navigationStore.gestionAduaneraSubOption, navigationStore.navTick],
  ([sub]) => {
    if (sub && ['declaraciones', 'clasificacion', 'tributos'].includes(sub)) {
      const target = sub === 'clasificacion' ? 'arancel' : sub === 'tributos' ? 'calculadora' : 'declaraciones'
      if (activeTab.value !== target) {
        activeTab.value = target
      }
    }
  }
)

onMounted(() => {
  loadExpedientes()
})
</script>

<style scoped>
.liquidador-view {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--space-5, 1.25rem);
  font-family: var(--font-sans, 'Inter', sans-serif);
}

/* 1. Toolbar de Acciones Warm Editorial */
.liquidacion-action-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--space-4, 1rem);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  background-color: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-lg, 0.75rem);
  box-shadow: var(--shadow-subtle, 0 1px 3px rgba(45, 35, 25, 0.04));
}

.toolbar-left-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-1, 0.25rem);
}

.badge-kicker {
  font-family: var(--font-mono, 'JetBrains Mono', monospace);
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker, #9a7f60);
  text-transform: uppercase;
}

.toolbar-title {
  margin: 0;
  font-size: var(--font-size-xl, 1.25rem);
  font-weight: 700;
  color: var(--color-text-main, #2b2520);
  letter-spacing: -0.015em;
}

.toolbar-actions-group {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  flex-wrap: wrap;
}

.count-badge {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  padding: 0.35rem 0.65rem;
  background-color: var(--color-bg-subtle, #f2ebe1);
  color: var(--color-text-muted, #6f6356);
  border: 1px solid var(--color-border-subtle, #ede5d8);
  border-radius: var(--radius-pill, 9999px);
}

/* Botones Estándar Warm Editorial */
.btn-espresso {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  padding: 0.55rem 1rem;
  background-color: var(--color-primary, #352f2a);
  color: var(--color-primary-text, #ffffff) !important;
  border: 1px solid var(--color-primary, #352f2a);
  border-radius: var(--radius-md, 0.5rem);
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease-in-out;
  box-shadow: var(--shadow-subtle, 0 1px 3px rgba(45, 35, 25, 0.04));
}

.btn-espresso:hover:not(:disabled) {
  background-color: var(--color-primary-hover, #484039);
  border-color: var(--color-primary-hover, #484039);
  transform: translateY(-1px);
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  padding: 0.55rem 0.9rem;
  background-color: var(--color-bg-surface, #ffffff);
  color: var(--color-text-main, #2b2520);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-md, 0.5rem);
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease-in-out;
}

.btn-outline-warm:hover:not(:disabled) {
  background-color: var(--color-bg-subtle, #f2ebe1);
  border-color: var(--color-border-strong, #d5c8b6);
  color: var(--color-text-main, #2b2520);
}

/* Pestañas de Navegación */
.module-tabs-nav {
  display: flex;
  gap: var(--space-2, 0.5rem);
  border-bottom: 2px solid var(--color-border-subtle, #ede5d8);
  padding-bottom: var(--space-1, 0.25rem);
  flex-wrap: wrap;
}

.tab-nav-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  padding: 0.65rem 1rem;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -0.35rem;
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 700;
  color: var(--color-text-muted, #6f6356);
  cursor: pointer;
  transition: all 0.15s ease;
}

.tab-nav-btn:hover {
  color: var(--color-text-main, #2b2520);
}

.tab-nav-btn.active {
  color: var(--color-primary, #352f2a);
  border-bottom-color: var(--color-accent-kicker, #9a7f60);
}

.tab-counter {
  background-color: var(--color-bg-subtle, #f2ebe1);
  color: var(--color-text-main, #2b2520);
  font-size: var(--font-size-xs, 0.75rem);
  padding: 0.1rem 0.45rem;
  border-radius: var(--radius-pill, 9999px);
  border: 1px solid var(--color-border-subtle, #ede5d8);
}

.tab-container {
  display: flex;
  flex-direction: column;
  gap: var(--space-5, 1.25rem);
}

/* 2. Grid de Métricas KPI Técnicas */
.metrics-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4, 1rem);
}

.kpi-card {
  background-color: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-lg, 0.75rem);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
  transition: all 0.2s ease-in-out;
  box-shadow: var(--shadow-subtle, 0 1px 3px rgba(45, 35, 25, 0.04));
}

.kpi-card:hover {
  border-color: var(--color-border-strong, #d5c8b6);
  transform: translateY(-2px);
  box-shadow: var(--shadow-card, 0 4px 16px -2px rgba(50, 40, 30, 0.04));
}

.kpi-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kpi-num {
  font-family: var(--font-mono, 'JetBrains Mono', monospace);
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  color: var(--color-text-subtle, #998c7e);
}

.kpi-label {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 600;
  color: var(--color-text-muted, #6f6356);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-right: auto;
  margin-left: 0.5rem;
}

.kpi-icon {
  font-size: 1.1rem;
  color: var(--color-text-subtle, #998c7e);
}
.kpi-icon.warning { color: var(--color-warning-text, #8c5b16); }
.kpi-icon.pending { color: var(--color-info-text, #21598f); }
.kpi-icon.success { color: var(--color-success-text, #1e6638); }
.kpi-icon.accent { color: var(--color-accent-kicker, #9a7f60); }

.kpi-main-stat {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-2, 0.5rem);
  margin-top: 0.15rem;
}

.kpi-value {
  font-size: var(--font-size-3xl, 1.875rem);
  font-weight: 800;
  line-height: 1;
  color: var(--color-text-main, #2b2520);
}

.kpi-badge {
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-pill, 9999px);
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.kpi-badge.success {
  background-color: var(--color-success-bg, #eaf6ed);
  color: var(--color-success-text, #1e6638);
  border: 1px solid var(--color-success-border, #bce5c6);
}

.kpi-badge.warning {
  background-color: var(--color-warning-bg, #fdf5e6);
  color: var(--color-warning-text, #8c5b16);
  border: 1px solid var(--color-warning-border, #f8dab3);
}

.kpi-badge.pending {
  background-color: var(--color-info-bg, #eef4fb);
  color: var(--color-info-text, #21598f);
  border: 1px solid var(--color-info-border, #cadff4);
}

.kpi-badge.accent {
  background-color: var(--color-bg-subtle, #f2ebe1);
  color: var(--color-accent-kicker, #9a7f60);
  border: 1px solid var(--color-border, #e4dacb);
}

.kpi-footer-note {
  font-size: var(--font-size-2xs, 0.6875rem);
  color: var(--color-text-subtle, #998c7e);
  border-top: 1px dashed var(--color-border-subtle, #ede5d8);
  padding-top: 0.4rem;
  margin-top: 0.2rem;
}

/* 3. Contenedores de Tabla y Secciones */
.content-table-card {
  background-color: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-lg, 0.75rem);
  padding: var(--space-5, 1.25rem);
  box-shadow: var(--shadow-subtle, 0 1px 3px rgba(45, 35, 25, 0.04));
}

.card-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4, 1rem);
  flex-wrap: wrap;
  gap: var(--space-3, 0.75rem);
}

.section-title {
  margin: 0;
  font-size: var(--font-size-lg, 1.125rem);
  font-weight: 700;
  color: var(--color-text-main, #2b2520);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  flex-wrap: wrap;
}

/* Selectores Nativos Sin Repetición de Flechas */
.filter-select, .form-select {
  appearance: none !important;
  -webkit-appearance: none !important;
  -moz-appearance: none !important;
  font-family: var(--font-sans, 'Inter', sans-serif) !important;
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-text-main, #2b2520) !important;
  background-color: var(--color-bg-surface, #ffffff) !important;
  border: 1px solid var(--color-border, #e4dacb) !important;
  border-radius: var(--radius-md, 0.5rem) !important;
  padding: 0.55rem 2.25rem 0.55rem 0.85rem !important;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%236f6356' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E") !important;
  background-repeat: no-repeat !important;
  background-position: right 0.75rem center !important;
  background-size: 1rem 1rem !important;
  cursor: pointer;
}

.search-input-wrap {
  position: relative;
}
.search-icon {
  position: absolute;
  left: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-text-subtle, #998c7e);
  font-size: 0.85rem;
}
.search-input, .form-input {
  font-family: var(--font-sans, 'Inter', sans-serif);
  font-size: var(--font-size-sm, 0.8125rem);
  padding: 0.55rem 0.85rem 0.55rem 2.25rem;
  border: 1px solid var(--color-border, #e4dacb);
  background-color: var(--color-bg-surface, #ffffff);
  color: var(--color-text-main, #2b2520);
  border-radius: var(--radius-md, 0.5rem);
  width: 260px;
  outline: none;
  transition: border-color 0.15s ease;
}
.search-input:focus, .form-input:focus, .filter-select:focus, .form-select:focus {
  border-color: var(--color-border-strong, #d5c8b6);
  box-shadow: 0 0 0 3px rgba(53, 47, 42, 0.08);
}

.table-container {
  background-color: var(--color-bg-surface, #ffffff);
  border-radius: var(--radius-md, 0.5rem);
  overflow: hidden;
  border: 1px solid var(--color-border, #e4dacb);
}

.client-cell {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}
.client-name {
  font-weight: 700;
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-text-main, #2b2520);
}
.client-sub {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted, #6f6356);
}

.canal-tag {
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 800;
  padding: 0.2rem 0.6rem;
  border-radius: var(--radius-pill, 9999px);
  font-family: var(--font-mono, 'JetBrains Mono', monospace);
  display: inline-block;
}
.canal-verde {
  background-color: var(--color-success-bg, #eaf6ed);
  color: var(--color-success-text, #1e6638);
  border: 1px solid var(--color-success-border, #bce5c6);
}
.canal-naranja {
  background-color: var(--color-warning-bg, #fdf5e6);
  color: var(--color-warning-text, #8c5b16);
  border: 1px solid var(--color-warning-border, #f8dab3);
}
.canal-rojo {
  background-color: var(--color-danger-bg, #fdf0f0);
  color: var(--color-danger-text, #9e2a2b);
  border: 1px solid var(--color-danger-border, #f6c8c8);
}
.canal-pendiente {
  background-color: var(--color-bg-subtle, #f2ebe1);
  color: var(--color-text-muted, #6f6356);
  border: 1px solid var(--color-border-subtle, #ede5d8);
}

.action-buttons-cell {
  display: inline-flex;
  gap: 0.35rem;
}
.btn-row-action {
  width: 30px;
  height: 30px;
  border-radius: var(--radius-sm, 0.375rem);
  border: 1px solid var(--color-border, #e4dacb);
  background-color: var(--color-bg-surface, #ffffff);
  color: var(--color-text-main, #2b2520);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  transition: all 0.15s ease;
}
.btn-row-action:hover {
  background-color: var(--color-bg-subtle, #f2ebe1);
  border-color: var(--color-border-strong, #d5c8b6);
}
.tag-xs {
  font-size: 0.6875rem;
  padding: 0.15rem 0.45rem;
}

/* Grilla de Calculadora */
.calc-wrapper-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
  gap: var(--space-5, 1.25rem);
}

.calc-card {
  background-color: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-lg, 0.75rem);
  padding: var(--space-5, 1.25rem);
  box-shadow: var(--shadow-subtle, 0 1px 3px rgba(45, 35, 25, 0.04));
}

.card-header-line {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  border-bottom: 1px solid var(--color-border-subtle, #ede5d8);
  padding-bottom: var(--space-3, 0.75rem);
  margin-bottom: var(--space-4, 1rem);
}
.card-icon {
  font-size: 1.5rem;
  color: var(--color-accent-kicker, #9a7f60);
}
.card-title {
  margin: 0;
  font-size: var(--font-size-md, 0.9375rem);
  font-weight: 700;
  color: var(--color-text-main, #2b2520);
}

.selected-exp-chip {
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  background-color: var(--color-bg-subtle, #f2ebe1);
  padding: 0.5rem 0.75rem;
  border-radius: var(--radius-md, 0.5rem);
  border: 1px solid var(--color-border, #e4dacb);
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-main, #2b2520);
  margin-bottom: var(--space-4, 1rem);
}
.btn-clear-link {
  margin-left: auto;
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--color-text-subtle, #998c7e);
}

.calc-form-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-4, 1rem);
}
.form-grid-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-3, 0.75rem);
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-1, 0.25rem);
}
.form-label {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 600;
  color: var(--color-text-muted, #6f6356);
}

/* Resultados */
.results-table {
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
}
.res-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--font-size-sm, 0.8125rem);
  padding: 0.2rem 0;
}
.res-label {
  color: var(--color-text-muted, #6f6356);
}
.res-val {
  color: var(--color-text-main, #2b2520);
}
.res-cif-box {
  background-color: var(--color-bg-subtle, #f2ebe1);
  padding: 0.65rem 0.85rem;
  border-radius: var(--radius-md, 0.5rem);
  border: 1px solid var(--color-border, #e4dacb);
}
.res-divider {
  height: 1px;
  background-color: var(--color-border-subtle, #ede5d8);
  margin: var(--space-1, 0.25rem) 0;
}

.total-box-editorial {
  background-color: var(--color-primary, #352f2a);
  color: var(--color-primary-text, #ffffff);
  padding: var(--space-4, 1rem);
  border-radius: var(--radius-lg, 0.75rem);
  margin-top: var(--space-3, 0.75rem);
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
  box-shadow: var(--shadow-subtle, 0 1px 3px rgba(45, 35, 25, 0.04));
}
.total-row-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.total-title {
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 800;
  letter-spacing: 0.05em;
  color: var(--color-bg-subtle, #f2ebe1);
}
.total-amount {
  font-size: 1.4rem;
  font-weight: 800;
  color: #d6b476;
}
.total-row-sub {
  display: flex;
  justify-content: space-between;
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-subtle, #998c7e);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding-top: var(--space-2, 0.5rem);
}

.results-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--space-3, 0.75rem);
}

.empty-state {
  text-align: center;
  padding: 2.5rem;
}
.empty-icon {
  font-size: 2rem;
  color: var(--color-text-subtle, #998c7e);
  margin-bottom: 0.5rem;
}

/* Modo Oscuro (.dark-theme) */
:global(.dark-theme) .liquidador-view .filter-select,
:global(.dark-theme) .liquidador-view .form-select {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23d6b476' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E") !important;
}

:global(.dark-theme) .liquidador-view .total-box-editorial {
  background-color: var(--color-bg-surface, #282723);
  border: 1px solid var(--color-border, #444138);
}

:global(.dark-theme) .liquidador-view .btn-espresso {
  background-color: var(--color-primary, #d6b476);
  color: var(--color-primary-text, #1d1b15) !important;
  border-color: var(--color-primary, #d6b476);
}

@media (max-width: 900px) {
  .metrics-kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .form-grid-3 {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 600px) {
  .metrics-kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>
