<template>
  <div class="liquidador-view">
    <!-- Hero Banner Card Editorial -->
    <HeroBannerCard
      kicker="GESTIÓN ADUANERA // LIQUIDACIÓN & DAM SUNAT"
      title="Liquidación Aduanera - Clasificación Arancelaria y DAM"
      description="Cálculo de base imponible CIF aduanera, liquidación de derechos arancelarios Ad-Valorem, IGV (16%), IPM (2%), ISC y percepciones SUNAT para la formulación de la DAM."
    />

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
        <span class="tab-counter">{{ expedientesList.length }}</span>
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
      <!-- Métricas Rápidas -->
      <div class="metrics-grid">
        <MetricCard index="01" label="Pendientes Clasificación" :value="expedientesPendientesCount" severity="warning" highlight />
        <MetricCard index="02" label="Canal Naranja / Rojo" :value="expedientesCanalInspeccionCount" severity="pending" />
        <MetricCard index="03" label="Con Levante Conforme" :value="expedientesLevanteCount" severity="success" />
        <MetricCard index="04" label="Total Expedientes DAM" :value="expedientesList.length" severity="accent" />
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
import HeroBannerCard from '@/shared/components/HeroBannerCard.vue'
import MetricCard from '@/shared/components/MetricCard.vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import { gestionAduaneraService } from '@/features/roles-modules/services/gestionAduaneraService'
import DocumentosManagerModal from '@/shared/components/DocumentosManagerModal.vue'
import { useNotification } from '@/shared/composables/useNotification'
import { useNavigationStore } from '@/core/stores/navigationStore'

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
  navigationStore.setGestionAduaneraSubOption(subMap[tab] || tab)
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
  () => [navigationStore.gestionAduaneraSubOption, navigationStore.navTick],
  ([sub]) => {
    if (sub && ['declaraciones', 'clasificacion', 'tributos'].includes(sub)) {
      activeTab.value = sub === 'clasificacion' ? 'arancel' : sub === 'tributos' ? 'calculadora' : 'declaraciones'
    }
  },
  { immediate: true }
)

onMounted(() => {
  loadExpedientes()
})
</script>

<style scoped>
.liquidador-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.module-tabs-nav {
  display: flex;
  gap: 0.5rem;
  border-bottom: 2px solid var(--color-border, #e2d9cd);
  padding-bottom: 0.25rem;
  flex-wrap: wrap;
}
.tab-nav-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1rem;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -0.35rem;
  font-size: 0.85rem;
  font-weight: 700;
  color: #7a7065;
  cursor: pointer;
}
.tab-nav-btn.active {
  color: var(--color-accent, #9a7f60);
  border-bottom-color: var(--color-accent, #9a7f60);
}
.tab-counter {
  background: #ede6db;
  color: #352f2a;
  font-size: 0.7rem;
  padding: 0.1rem 0.45rem;
  border-radius: 12px;
  font-family: var(--font-mono, monospace);
}

.tab-container {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.content-table-card {
  background: var(--color-bg-card, #ede6db);
  border: 1px solid var(--color-border, #e2d9cd);
  border-radius: 6px;
  padding: 1.25rem;
}

.card-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.badge-kicker {
  font-family: var(--font-mono, monospace);
  font-size: 0.65rem;
  letter-spacing: 0.08em;
  color: var(--color-accent, #9a7f60);
  font-weight: 700;
  display: block;
}

.section-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-text-primary, #352f2a);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.filter-select {
  padding: 0.45rem 0.65rem;
  border: 1px solid var(--color-border, #e2d9cd);
  background: #ffffff;
  border-radius: 4px;
  font-size: 0.8rem;
}

.search-input-wrap {
  position: relative;
}
.search-icon {
  position: absolute;
  left: 0.6rem;
  top: 50%;
  transform: translateY(-50%);
  color: #8c827a;
  font-size: 0.8rem;
}
.search-input {
  padding: 0.45rem 0.65rem 0.45rem 1.8rem;
  border: 1px solid var(--color-border, #e2d9cd);
  background: #ffffff;
  border-radius: 4px;
  font-size: 0.8rem;
  width: 220px;
}

.btn-refresh {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.45rem 0.75rem;
  background: transparent;
  border: 1px solid var(--color-border, #e2d9cd);
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
}

.table-container {
  background: #ffffff;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid var(--color-border, #e2d9cd);
}

.code-cell, .client-cell {
  display: flex;
  flex-direction: column;
}
.client-name {
  font-weight: 700;
  font-size: 0.825rem;
}
.client-sub {
  font-size: 0.7rem;
  color: #6b635b;
}

.canal-tag {
  font-size: 0.75rem;
  font-weight: 800;
  padding: 0.2rem 0.55rem;
  border-radius: 4px;
  font-family: var(--font-mono, monospace);
}
.canal-verde { background: #eef8f3; color: #027a48; border: 1px solid #a6dbbe; }
.canal-naranja { background: #fdf5ea; color: #b54708; border: 1px solid #e8c697; }
.canal-rojo { background: #faebeb; color: #b42318; border: 1px solid #e89797; }
.canal-pendiente { background: #f6f3ee; color: #7a7065; border: 1px solid #e2d9cd; }

.action-buttons-cell {
  display: inline-flex;
  gap: 0.35rem;
}
.btn-row-action {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  border: 1px solid var(--color-border, #e2d9cd);
  background: transparent;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
}
.btn-row-action:hover {
  background: #f6f3ee;
}
.tag-xs {
  font-size: 0.675rem;
  padding: 0.15rem 0.4rem;
}

/* Grilla de Calculadora */
.calc-wrapper-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
  gap: 1.25rem;
}

.calc-card {
  background: var(--color-bg-card, #ede6db);
  border: 1px solid var(--color-border, #e2d9cd);
  border-radius: 6px;
  padding: 1.25rem;
}

.card-header-line {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  border-bottom: 1px solid #e2d9cd;
  padding-bottom: 0.75rem;
  margin-bottom: 1rem;
}
.card-icon {
  font-size: 1.5rem;
  color: var(--color-accent, #9a7f60);
}
.card-title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-text-primary, #352f2a);
}

.selected-exp-chip {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: #ffffff;
  padding: 0.5rem 0.75rem;
  border-radius: 4px;
  border: 1px solid #e2d9cd;
  font-size: 0.8rem;
  margin-bottom: 1rem;
}
.btn-clear-link {
  margin-left: auto;
  background: transparent;
  border: none;
  cursor: pointer;
  color: #8c827a;
}

.calc-form-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}
.form-grid-3 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 0.75rem;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.form-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-primary, #352f2a);
}
.form-input, .form-select {
  padding: 0.45rem 0.65rem;
  border: 1px solid var(--color-border, #e2d9cd);
  background: #ffffff;
  border-radius: 4px;
  font-size: 0.825rem;
}

/* Resultados */
.results-table {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}
.res-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.825rem;
  padding: 0.2rem 0;
}
.res-label {
  color: #554f46;
}
.res-val {
  color: #352f2a;
}
.res-cif-box {
  background: #ffffff;
  padding: 0.5rem 0.75rem;
  border-radius: 4px;
  border: 1px solid #e2d9cd;
}
.res-divider {
  height: 1px;
  background: #e2d9cd;
  margin: 0.25rem 0;
}

.total-box-editorial {
  background: #352f2a;
  color: #ffffff;
  padding: 1rem;
  border-radius: 6px;
  margin-top: 0.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.total-row-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.total-title {
  font-size: 0.85rem;
  font-weight: 800;
  letter-spacing: 0.05em;
  color: #e2d9cd;
}
.total-amount {
  font-size: 1.35rem;
  font-weight: 800;
  color: #d6b476;
}
.total-row-sub {
  display: flex;
  justify-content: space-between;
  font-size: 0.775rem;
  color: #a89f91;
  border-top: 1px solid #4a423b;
  padding-top: 0.35rem;
}

.results-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.75rem;
}
.btn-save-sim {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.55rem 1.15rem;
  background: var(--color-accent, #9a7f60);
  border: none;
  border-radius: 4px;
  color: #ffffff;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
}
.empty-state {
  text-align: center;
  padding: 2.5rem;
}
.empty-icon {
  font-size: 2rem;
  color: #8c827a;
  margin-bottom: 0.5rem;
}
</style>
