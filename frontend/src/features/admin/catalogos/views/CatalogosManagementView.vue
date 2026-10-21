<template>
  <div class="catalogos-management-view">

    <!-- Vista Completa: Formulario Maestro de Puerto / Terminal -->
    <PuertoRegistrationForm
      v-if="showPuertoForm"
      :puerto-to-edit="selectedPuerto"
      @cancel="showPuertoForm = false"
      @saved="onPuertoSaved"
    />

    <!-- Vista Completa: Formulario Maestro de Tipo de Carga -->
    <TipoCargaRegistrationForm
      v-else-if="showTipoCargaForm"
      :tipo-to-edit="selectedTipo"
      @cancel="showTipoCargaForm = false"
      @saved="onTipoSaved"
    />

    <!-- Vista Principal: Directorio y Tablas Maestras de Catálogos Auxiliares -->
    <div v-else class="catalogo-master-container">

      <!-- 1. Hero Banner Card con Estilo Editorial Cálido -->
      <div class="hero-banner-card">
        <div class="hero-content">
          <span class="badge-kicker">CATÁLOGOS AUXILIARES // TABLAS MAESTRAS ADUANERAS</span>
          <h2 class="hero-title">Puertos, Terminales y Tipos de Carga</h2>
          <p class="hero-desc">
            Mantenimiento de tablas maestras del sistema aduanero. Registre y supervise puertos autorizados de arribo/zarpe y especificaciones técnicas de contenedores.
          </p>
        </div>
        <div class="hero-watermark">
          <span>SGA</span>
        </div>
      </div>

      <!-- 2. Tarjetas de Métricas Rápidas (01, 02, 03, 04) -->
      <div class="metrics-grid">
        <div class="metric-card">
          <div class="metric-top">
            <span class="metric-idx">01</span>
            <span class="metric-label">Puertos Totales</span>
          </div>
          <div class="metric-val">{{ puertos.length }}</div>
      </div>

      <div class="metric-card">
        <div class="metric-top">
          <span class="metric-idx">02</span>
          <span class="metric-label">Terminales Marítimos</span>
        </div>
        <div class="metric-val">{{ maritimePortsCount }}</div>
      </div>

      <div class="metric-card">
        <div class="metric-top">
          <span class="metric-idx">03</span>
          <span class="metric-label">Aéreos & Terrestres</span>
        </div>
        <div class="metric-val">{{ otherPortsCount }}</div>
      </div>

      <div class="metric-card">
        <div class="metric-top">
          <span class="metric-idx">04</span>
          <span class="metric-label">Tipos de Carga</span>
        </div>
        <div class="metric-val">{{ tiposCarga.length }}</div>
      </div>
    </div>

    <!-- 3. Selector de Pestañas de Catálogo Auxiliar -->
    <div class="catalog-tabs-bar">
      <div class="catalog-tabs">
        <button
          type="button"
          :class="['catalog-tab-btn', { active: activeTab === 'puertos' }]"
          @click="activeTab = 'puertos'"
        >
          <i class="pi pi-compass"></i>
          <span>Puertos & Terminales Aduaneros</span>
          <span class="tab-count-badge">{{ puertos.length }}</span>
        </button>

        <button
          type="button"
          :class="['catalog-tab-btn', { active: activeTab === 'tipos-carga' }]"
          @click="activeTab = 'tipos-carga'"
        >
          <i class="pi pi-box"></i>
          <span>Tipos de Carga & Contenedor</span>
          <span class="tab-count-badge">{{ tiposCarga.length }}</span>
        </button>

        <button
          type="button"
          :class="['catalog-tab-btn', { active: activeTab === 'mercancias' }]"
          @click="activeTab = 'mercancias'"
        >
          <i class="pi pi-tags"></i>
          <span>Mercancías & Partidas Arancelarias</span>
          <span class="tab-count-badge">{{ mercancias.length }}</span>
        </button>
      </div>
    </div>

    <!-- ========================================================================= -->
    <!-- TAB 1: PUERTOS Y TERMINALES ADUANEROS -->
    <!-- ========================================================================= -->
    <section v-if="activeTab === 'puertos'" class="catalog-content-section">
      <!-- Toolbar de Controles y Búsqueda -->
      <div class="section-toolbar">
        <div class="toolbar-left">
          <div class="search-box">
            <i class="pi pi-search search-icon"></i>
            <input
              v-model.trim="puertosSearch"
              type="text"
              placeholder="Buscar por código, nombre o país..."
              class="search-input"
            />
            <button v-if="puertosSearch" class="clear-search-btn" @click="puertosSearch = ''">
              <i class="pi pi-times"></i>
            </button>
          </div>

          <div class="filter-dropdown">
            <select v-model="puertosTypeFilter" class="filter-select">
              <option value="ALL">Todos los Tipos</option>
              <option value="MARITIMO">Marítimo</option>
              <option value="AEREO">Aéreo</option>
              <option value="TERRESTRE">Terrestre</option>
              <option value="FLUVIAL">Fluvial</option>
            </select>
          </div>

          <div class="filter-dropdown">
            <select v-model="puertosStatusFilter" class="filter-select">
              <option value="ALL">Todos los Estados</option>
              <option value="ACTIVO">Solo Activos</option>
              <option value="INACTIVO">Solo Inactivos</option>
            </select>
          </div>
        </div>

        <div class="toolbar-right">
          <button
            type="button"
            class="btn-refresh"
            @click="loadPuertos"
            :disabled="loadingPuertos"
            title="Recargar datos"
          >
            <i :class="['pi pi-sync', { 'pi-spin': loadingPuertos }]"></i>
            <span>Actualizar</span>
          </button>

          <button
            type="button"
            class="btn-refresh"
            @click="handleExportPdf"
            :disabled="exportingPdf"
            title="Exportar reporte oficial de puertos en PDF con JasperReports"
          >
            <i :class="['pi', exportingPdf ? 'pi-spin pi-spinner' : 'pi-file-pdf']"></i>
            <span>Ver Reporte (PDF)</span>
          </button>

          <button
            type="button"
            class="btn-primary-action"
            @click="openCreatePuertoModal"
          >
            <i class="pi pi-plus"></i>
            <span>+ Nuevo Puerto</span>
          </button>
        </div>
      </div>

      <!-- Tabla de Puertos con PrimeVue DataTable -->
      <div class="data-table-container">
        <DataTable
          :value="filteredPuertos"
          :loading="loadingPuertos"
          responsiveLayout="scroll"
          class="custom-datatable"
          dataKey="idPuerto"
        >
          <template #empty>
            <div class="empty-state">
              <i class="pi pi-inbox empty-icon"></i>
              <p class="empty-title">No se encontraron puertos registrados</p>
              <p class="empty-desc">Pruebe ajustando los filtros de búsqueda o registre un nuevo puerto.</p>
            </div>
          </template>
          <template #loading>
            <div class="loading-state">
              <i class="pi pi-spin pi-spinner"></i>
              <span>Cargando catálogo de puertos aduaneros...</span>
            </div>
          </template>

          <Column header="CÓDIGO PUERTO" style="width: 140px;">
            <template #body="{ data: puerto }">
              <span class="code-badge">{{ puerto.codigoPuerto }}</span>
            </template>
          </Column>

          <Column header="NOMBRE DEL PUERTO / TERMINAL">
            <template #body="{ data: puerto }">
              <div class="port-name-cell">
                <span class="port-name">{{ puerto.nombre }}</span>
                <span class="port-id-sub">ID: #{{ puerto.idPuerto }}</span>
              </div>
            </template>
          </Column>

          <Column header="PAÍS" style="width: 150px;">
            <template #body="{ data: puerto }">
              <span class="country-pill">{{ puerto.pais }}</span>
            </template>
          </Column>

          <Column header="TIPO" style="width: 150px;">
            <template #body="{ data: puerto }">
              <Tag
                :class="['type-badge', getPortTypeClass(puerto.tipo)]"
                rounded
              >
                <template #icon>
                  <i :class="getPortTypeIcon(puerto.tipo)"></i>
                </template>
                <span>{{ puerto.tipo }}</span>
              </Tag>
            </template>
          </Column>

          <Column header="ESTADO" style="width: 130px; text-align: center;" headerClass="text-center" bodyClass="text-center">
            <template #body="{ data: puerto }">
              <Tag
                :value="puerto.estado"
                :severity="puerto.estado === 'ACTIVO' ? 'success' : 'danger'"
                rounded
              />
            </template>
          </Column>

          <Column header="ACCIONES" style="width: 180px; text-align: right;" headerClass="text-right" bodyClass="text-right">
            <template #body="{ data: puerto }">
              <div class="actions-cell">
                <button
                  type="button"
                  class="action-btn edit-btn"
                  @click="openEditPuertoModal(puerto)"
                  title="Editar información del puerto"
                >
                  <i class="pi pi-pencil"></i>
                  <span>Editar</span>
                </button>

                <button
                  v-if="puerto.estado === 'ACTIVO'"
                  type="button"
                  class="action-btn deactivate-btn"
                  @click="handleDeactivatePuerto(puerto)"
                  title="Desactivar puerto"
                >
                  <i class="pi pi-ban"></i>
                </button>

                <button
                  v-else
                  type="button"
                  class="action-btn reactivate-btn"
                  @click="handleReactivatePuerto(puerto)"
                  title="Reactivar puerto"
                >
                  <i class="pi pi-check-circle"></i>
                </button>
              </div>
            </template>
          </Column>
        </DataTable>
      </div>
    </section>

    <!-- ========================================================================= -->
    <!-- TAB 2: TIPOS DE CARGA Y CONTENEDOR -->
    <!-- ========================================================================= -->
    <section v-else-if="activeTab === 'tipos-carga'" class="catalog-content-section">
      <!-- Toolbar de Controles y Búsqueda -->
      <div class="section-toolbar">
        <div class="toolbar-left">
          <div class="search-box">
            <i class="pi pi-search search-icon"></i>
            <input
              v-model.trim="tiposSearch"
              type="text"
              placeholder="Buscar por código o descripción de carga..."
              class="search-input"
            />
            <button v-if="tiposSearch" class="clear-search-btn" @click="tiposSearch = ''">
              <i class="pi pi-times"></i>
            </button>
          </div>

          <div class="filter-dropdown">
            <select v-model="tiposStatusFilter" class="filter-select">
              <option value="ALL">Todos los Estados</option>
              <option value="ACTIVO">Solo Activos</option>
              <option value="INACTIVO">Solo Inactivos</option>
            </select>
          </div>
        </div>

        <div class="toolbar-right">
          <button
            type="button"
            class="btn-refresh"
            @click="loadTiposCarga"
            :disabled="loadingTipos"
            title="Recargar datos"
          >
            <i :class="['pi pi-sync', { 'pi-spin': loadingTipos }]"></i>
            <span>Actualizar</span>
          </button>

          <button
            type="button"
            class="btn-primary-action"
            @click="openCreateTipoModal"
          >
            <i class="pi pi-plus"></i>
            <span>+ Nuevo Tipo de Carga</span>
          </button>
        </div>
      </div>

      <!-- Tabla de Tipos de Carga con PrimeVue DataTable -->
      <div class="data-table-container">
        <DataTable
          :value="filteredTiposCarga"
          :loading="loadingTipos"
          responsiveLayout="scroll"
          class="custom-datatable"
          dataKey="idTipoCarga"
        >
          <template #empty>
            <div class="empty-state">
              <i class="pi pi-inbox empty-icon"></i>
              <p class="empty-title">No se encontraron tipos de carga registrados</p>
              <p class="empty-desc">Pruebe ajustando los filtros o registre un nuevo tipo de carga.</p>
            </div>
          </template>
          <template #loading>
            <div class="loading-state">
              <i class="pi pi-spin pi-spinner"></i>
              <span>Cargando catálogo de tipos de carga...</span>
            </div>
          </template>

          <Column header="CÓDIGO CARGA" style="width: 170px;">
            <template #body="{ data: tipo }">
              <span class="code-badge">{{ tipo.codigo }}</span>
            </template>
          </Column>

          <Column header="DESCRIPCIÓN DE MERCANCÍA / CARGA">
            <template #body="{ data: tipo }">
              <div class="port-name-cell">
                <span class="port-name">{{ tipo.nombre }}</span>
                <span class="port-id-sub">Clasificación Técnica #{{ tipo.idTipoCarga }}</span>
              </div>
            </template>
          </Column>

          <Column header="CONTENEDOR SUGERIDO" style="width: 180px;">
            <template #body="{ data: tipo }">
              <span class="container-pill">{{ tipo.tipoContenedorSugerido || 'Estándar / N/A' }}</span>
            </template>
          </Column>

          <Column header="TARA SUGERIDA" style="width: 150px; text-align: right;" headerClass="text-right" bodyClass="text-right">
            <template #body="{ data: tipo }">
              <span class="tara-val">{{ formatNumber(tipo.taraSugeridaKg) }} kg</span>
            </template>
          </Column>

          <Column header="ESTADO" style="width: 130px; text-align: center;" headerClass="text-center" bodyClass="text-center">
            <template #body="{ data: tipo }">
              <Tag
                :value="tipo.estado"
                :severity="tipo.estado === 'ACTIVO' ? 'success' : 'danger'"
                rounded
              />
            </template>
          </Column>

          <Column header="ACCIONES" style="width: 180px; text-align: right;" headerClass="text-right" bodyClass="text-right">
            <template #body="{ data: tipo }">
              <div class="actions-cell">
                <button
                  type="button"
                  class="action-btn edit-btn"
                  @click="openEditTipoModal(tipo)"
                  title="Editar especificaciones de carga"
                >
                  <i class="pi pi-pencil"></i>
                  <span>Editar</span>
                </button>

                <button
                  v-if="tipo.estado === 'ACTIVO'"
                  type="button"
                  class="action-btn deactivate-btn"
                  @click="handleDeactivateTipo(tipo)"
                  title="Desactivar tipo de carga"
                >
                  <i class="pi pi-ban"></i>
                </button>

                <button
                  v-else
                  type="button"
                  class="action-btn reactivate-btn"
                  @click="handleReactivateTipo(tipo)"
                  title="Reactivar tipo de carga"
                >
                  <i class="pi pi-check-circle"></i>
                </button>
              </div>
            </template>
          </Column>
        </DataTable>
      </div>
    </section>

    <!-- ========================================================================= -->
    <!-- TAB 3: MERCANCÍAS Y PARTIDAS ARANCELARIAS -->
    <!-- ========================================================================= -->
    <section v-if="activeTab === 'mercancias'" class="catalog-content-section">
      <div class="section-controls-header">
        <div class="controls-kicker-group">
          <span class="badge-kicker">NOMENCLATURA & RESTRICCIONES VUCE</span>
          <h3 class="section-heading">Catálogo de Mercancías y Subpartidas</h3>
        </div>

        <div class="controls-toolbar">
          <div class="search-input-wrapper">
            <i class="pi pi-search search-icon"></i>
            <input
              v-model="mercanciasSearch"
              type="text"
              placeholder="Buscar por código, descripción o categoría..."
              class="toolbar-search-input"
            />
          </div>

          <select v-model="mercanciasCategoriaFilter" class="toolbar-select">
            <option value="ALL">Todas las Categorías</option>
            <option value="FARMACOS">Fármacos y Medicamentos</option>
            <option value="ALIMENTOS">Alimentos y Perecibles</option>
            <option value="TECNOLOGIA">Tecnología y Electrónica</option>
            <option value="TEXTILES">Textiles y Calzado</option>
            <option value="MAQUINARIA">Maquinaria y Repuestos</option>
            <option value="QUIMICOS">Sustancias Químicas</option>
            <option value="GENERAL">Carga General Miscelánea</option>
          </select>

          <select v-model="mercanciasStatusFilter" class="toolbar-select">
            <option value="ALL">Todos los Estados</option>
            <option value="ACTIVO">Solo Activos</option>
            <option value="INACTIVO">Solo Inactivos</option>
          </select>

          <button
            type="button"
            class="btn-primary-action"
            @click="openCreateMercanciaModal"
          >
            <i class="pi pi-plus"></i>
            <span>+ Nueva Mercancía</span>
          </button>
        </div>
      </div>

      <div class="table-card-container">
        <DataTable
          :value="filteredMercancias"
          :loading="loadingMercancias"
          responsiveLayout="scroll"
          class="custom-datatable"
          dataKey="idMercancia"
        >
          <template #empty>
            <div class="empty-state">
              <i class="pi pi-tags empty-icon"></i>
              <p class="empty-title">No se encontraron mercancías registradas</p>
              <p class="empty-desc">Agregue subpartidas arancelarias para la clasificación en cotizaciones y DAM.</p>
            </div>
          </template>

          <Column field="codigo" header="Código / Partida">
            <template #body="{ data: m }">
              <div class="type-badge-cell">
                <span class="type-code-pill font-mono">{{ m.codigo }}</span>
              </div>
            </template>
          </Column>

          <Column field="nombre" header="Descripción de Mercancía">
            <template #body="{ data: m }">
              <div class="name-desc-cell">
                <strong class="port-name">{{ m.nombre }}</strong>
              </div>
            </template>
          </Column>

          <Column field="categoria" header="Categoría">
            <template #body="{ data: m }">
              <span class="country-text">{{ m.categoria }}</span>
            </template>
          </Column>

          <Column header="Control VUCE">
            <template #body="{ data: m }">
              <Tag
                v-if="m.requierePermiso"
                :value="`Restringido (${m.entidadReguladora || 'VUCE'})`"
                severity="warn"
                rounded
              />
              <Tag
                v-else
                value="Libre Comercialización"
                severity="success"
                rounded
              />
            </template>
          </Column>

          <Column field="arancelBasePct" header="Arancel Base">
            <template #body="{ data: m }">
              <span class="arancel-pct-text font-bold">{{ Number(m.arancelBasePct || 0).toFixed(2) }}%</span>
            </template>
          </Column>

          <Column field="estado" header="Estado">
            <template #body="{ data: m }">
              <Tag
                :value="m.estado"
                :severity="m.estado === 'ACTIVO' ? 'success' : 'danger'"
                rounded
              />
            </template>
          </Column>

          <Column header="Acciones" headerClass="text-right" bodyClass="text-right">
            <template #body="{ data: m }">
              <div class="actions-wrapper">
                <button
                  type="button"
                  class="action-btn edit-btn"
                  @click="openEditMercanciaModal(m)"
                  title="Editar mercancía"
                >
                  <i class="pi pi-pencil"></i>
                </button>

                <button
                  v-if="m.estado === 'ACTIVO'"
                  type="button"
                  class="action-btn deactivate-btn"
                  @click="handleDeactivateMercancia(m)"
                  title="Desactivar mercancía"
                >
                  <i class="pi pi-ban"></i>
                </button>

                <button
                  v-else
                  type="button"
                  class="action-btn reactivate-btn"
                  @click="handleReactivateMercancia(m)"
                  title="Reactivar mercancía"
                >
                  <i class="pi pi-check-circle"></i>
                </button>
              </div>
            </template>
          </Column>
        </DataTable>
      </div>
    </section>

    <!-- Modal Formulario de Mercancía -->
    <MercanciaFormModal
      :visible="showMercanciaModal"
      :mercancia-to-edit="selectedMercancia"
      @close="showMercanciaModal = false"
      @saved="onMercanciaSaved"
    />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import catalogoService from '../services/catalogoService'
import mercanciaService from '../services/mercanciaService'
import PuertoRegistrationForm from '../components/PuertoRegistrationForm.vue'
import TipoCargaRegistrationForm from '../components/TipoCargaRegistrationForm.vue'
import MercanciaFormModal from '../components/MercanciaFormModal.vue'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import apiClient from '@/core/api/apiClient'

const { showSuccess, showError, showInfo } = useNotification()
const { confirmAction } = useConfirmation()

// Estado General
const activeTab = ref('puertos') // 'puertos' | 'tipos-carga'
const exportingPdf = ref(false)

const handleExportPdf = async () => {
  exportingPdf.value = true
  try {
    const blobData = await catalogoService.downloadPuertosPdf()
    const url = window.URL.createObjectURL(new Blob([blobData], { type: 'application/pdf' }))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `reporte-puertos-aduaneros-${new Date().toISOString().slice(0, 10)}.pdf`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (err) {
    console.error('Error al descargar reporte de puertos:', err)
    showError('No se pudo generar el reporte oficial de puertos.')
  } finally {
    exportingPdf.value = false
  }
}

// Estado Puertos
const puertos = ref([])
const loadingPuertos = ref(false)
const puertosSearch = ref('')
const puertosTypeFilter = ref('ALL')
const puertosStatusFilter = ref('ALL')

const showPuertoForm = ref(false)
const selectedPuerto = ref(null)

// Estado Tipos de Carga
const tiposCarga = ref([])
const loadingTipos = ref(false)
const tiposSearch = ref('')
const tiposStatusFilter = ref('ALL')

const showTipoCargaForm = ref(false)
const selectedTipo = ref(null)

const showToast = (message, type = 'success') => {
  if (type === 'error') {
    showError(message)
  } else if (type === 'info') {
    showInfo(message)
  } else {
    showSuccess(message)
  }
}

// Métricas Computadas
const maritimePortsCount = computed(() => {
  return puertos.value.filter((p) => p.tipo === 'MARITIMO' && p.estado === 'ACTIVO').length
})

const otherPortsCount = computed(() => {
  return puertos.value.filter((p) => p.tipo !== 'MARITIMO').length
})

// Filtrado de Puertos
const filteredPuertos = computed(() => {
  return puertos.value.filter((p) => {
    const matchesSearch =
      !puertosSearch.value ||
      p.codigoPuerto?.toLowerCase().includes(puertosSearch.value.toLowerCase()) ||
      p.nombre?.toLowerCase().includes(puertosSearch.value.toLowerCase()) ||
      p.pais?.toLowerCase().includes(puertosSearch.value.toLowerCase())

    const matchesType =
      puertosTypeFilter.value === 'ALL' || p.tipo === puertosTypeFilter.value

    const matchesStatus =
      puertosStatusFilter.value === 'ALL' || p.estado === puertosStatusFilter.value

    return matchesSearch && matchesType && matchesStatus
  })
})

// Filtrado de Tipos de Carga
const filteredTiposCarga = computed(() => {
  return tiposCarga.value.filter((t) => {
    const matchesSearch =
      !tiposSearch.value ||
      t.codigo?.toLowerCase().includes(tiposSearch.value.toLowerCase()) ||
      t.nombre?.toLowerCase().includes(tiposSearch.value.toLowerCase()) ||
      t.tipoContenedorSugerido?.toLowerCase().includes(tiposSearch.value.toLowerCase())

    const matchesStatus =
      tiposStatusFilter.value === 'ALL' || t.estado === tiposStatusFilter.value

    return matchesSearch && matchesStatus
  })
})

// Cargar Puertos
const loadPuertos = async () => {
  loadingPuertos.value = true
  try {
    const res = await catalogoService.getPuertos(false)
    puertos.value = res.data || []
  } catch (error) {
    console.error('Error cargando puertos:', error)
    showToast('Error al conectar con el catálogo de puertos', 'error')
  } finally {
    loadingPuertos.value = false
  }
}

// Cargar Tipos de Carga
const loadTiposCarga = async () => {
  loadingTipos.value = true
  try {
    const res = await catalogoService.getTiposCarga(false)
    tiposCarga.value = res.data || []
  } catch (error) {
    console.error('Error cargando tipos de carga:', error)
    showToast('Error al conectar con el catálogo de tipos de carga', 'error')
  } finally {
    loadingTipos.value = false
  }
}

// Helpers Visuales
const getPortTypeClass = (tipo) => {
  const map = {
    MARITIMO: 'type-maritimo',
    AEREO: 'type-aereo',
    TERRESTRE: 'type-terrestre',
    FLUVIAL: 'type-fluvial'
  }
  return map[tipo] || 'type-maritimo'
}

const getPortTypeIcon = (tipo) => {
  const map = {
    MARITIMO: 'pi pi-compass',
    AEREO: 'pi pi-send',
    TERRESTRE: 'pi pi-map-marker',
    FLUVIAL: 'pi pi-compass'
  }
  return map[tipo] || 'pi pi-compass'
}

const formatNumber = (num) => {
  if (num === null || num === undefined) return '0.00'
  return Number(num).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// Acciones Puertos
const openCreatePuertoModal = () => {
  selectedPuerto.value = null
  showPuertoForm.value = true
}

const openEditPuertoModal = (puerto) => {
  selectedPuerto.value = { ...puerto }
  showPuertoForm.value = true
}

const onPuertoSaved = (payload) => {
  showPuertoForm.value = false
  showToast(payload.isEdit ? 'Puerto actualizado exitosamente' : 'Nuevo puerto registrado en el catálogo')
  loadPuertos()
}

const handleDeactivatePuerto = async (puerto) => {
  const confirmed = await confirmAction({
    message: `¿Desea marcar el puerto ${puerto.codigoPuerto} — ${puerto.nombre} como inactivo?`,
    header: 'Confirmar Desactivación de Puerto',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Desactivar',
    acceptClass: 'p-button-danger'
  })
  if (!confirmed) return

  try {
    await catalogoService.deletePuerto(puerto.idPuerto)
    showToast(`Puerto ${puerto.codigoPuerto} marcado como inactivo`)
    loadPuertos()
  } catch (error) {
    console.error('Error desactivando puerto:', error)
    showToast('No se pudo desactivar el puerto', 'error')
  }
}

const handleReactivatePuerto = async (puerto) => {
  try {
    await catalogoService.reactivarPuerto(puerto.idPuerto)
    showToast(`Puerto ${puerto.codigoPuerto} reactivado exitosamente`)
    loadPuertos()
  } catch (error) {
    console.error('Error reactivando puerto:', error)
    showToast('No se pudo reactivar el puerto', 'error')
  }
}

// Acciones Tipos de Carga
const openCreateTipoModal = () => {
  selectedTipo.value = null
  showTipoCargaForm.value = true
}

const openEditTipoModal = (tipo) => {
  selectedTipo.value = { ...tipo }
  showTipoCargaForm.value = true
}

const onTipoSaved = (payload) => {
  showTipoCargaForm.value = false
  showToast(payload.isEdit ? 'Tipo de carga actualizado exitosamente' : 'Nuevo tipo de carga registrado')
  loadTiposCarga()
}

const handleDeactivateTipo = async (tipo) => {
  const confirmed = await confirmAction({
    message: `¿Desea marcar el tipo de carga ${tipo.codigo} como inactivo?`,
    header: 'Confirmar Desactivación de Tipo de Carga',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Desactivar',
    acceptClass: 'p-button-danger'
  })
  if (!confirmed) return

  try {
    await catalogoService.deleteTipoCarga(tipo.idTipoCarga)
    showToast(`Tipo de carga ${tipo.codigo} marcado como inactivo`)
    loadTiposCarga()
  } catch (error) {
    console.error('Error desactivando tipo de carga:', error)
    showToast('No se pudo desactivar el tipo de carga', 'error')
  }
}

const handleReactivateTipo = async (tipo) => {
  try {
    await catalogoService.reactivarTipoCarga(tipo.idTipoCarga)
    showToast(`Tipo de carga ${tipo.codigo} reactivado exitosamente`)
    loadTiposCarga()
  } catch (error) {
    console.error('Error reactivando tipo de carga:', error)
    showToast('No se pudo reactivar el tipo de carga', 'error')
  }
}

// Estado Mercancías
const mercancias = ref([])
const loadingMercancias = ref(false)
const mercanciasSearch = ref('')
const mercanciasCategoriaFilter = ref('ALL')
const mercanciasStatusFilter = ref('ALL')
const showMercanciaModal = ref(false)
const selectedMercancia = ref(null)

const filteredMercancias = computed(() => {
  return mercancias.value.filter((m) => {
    const q = mercanciasSearch.value.toLowerCase().trim()
    const matchesSearch =
      !q ||
      m.codigo?.toLowerCase().includes(q) ||
      m.nombre?.toLowerCase().includes(q) ||
      m.categoria?.toLowerCase().includes(q)

    const matchesCategory =
      mercanciasCategoriaFilter.value === 'ALL' || m.categoria === mercanciasCategoriaFilter.value

    const matchesStatus =
      mercanciasStatusFilter.value === 'ALL' || m.estado === mercanciasStatusFilter.value

    return matchesSearch && matchesCategory && matchesStatus
  })
})

const loadMercancias = async () => {
  loadingMercancias.value = true
  try {
    const res = await mercanciaService.getMercancias()
    mercancias.value = res || []
  } catch (error) {
    console.error('Error cargando mercancías:', error)
    showToast('Error al conectar con el catálogo de mercancías', 'error')
  } finally {
    loadingMercancias.value = false
  }
}

const openCreateMercanciaModal = () => {
  selectedMercancia.value = null
  showMercanciaModal.value = true
}

const openEditMercanciaModal = (mercancia) => {
  selectedMercancia.value = { ...mercancia }
  showMercanciaModal.value = true
}

const onMercanciaSaved = () => {
  showMercanciaModal.value = false
  showToast('Mercancía guardada exitosamente')
  loadMercancias()
}

const handleDeactivateMercancia = async (mercancia) => {
  const confirmed = await confirmAction({
    message: `¿Desea desactivar la mercancía ${mercancia.codigo} — ${mercancia.nombre}?`,
    header: 'Confirmar Desactivación de Mercancía',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Desactivar',
    acceptClass: 'p-button-danger'
  })
  if (!confirmed) return

  try {
    await mercanciaService.deleteMercancia(mercancia.idMercancia)
    showToast(`Mercancía ${mercancia.codigo} desactivada`)
    loadMercancias()
  } catch (error) {
    console.error('Error desactivando mercancía:', error)
    showToast('No se pudo desactivar la mercancía', 'error')
  }
}

const handleReactivateMercancia = async (mercancia) => {
  try {
    await mercanciaService.reactivarMercancia(mercancia.idMercancia)
    showToast(`Mercancía ${mercancia.codigo} reactivada exitosamente`)
    loadMercancias()
  } catch (error) {
    console.error('Error reactivando mercancía:', error)
    showToast('No se pudo reactivar la mercancía', 'error')
  }
}

onMounted(() => {
  loadPuertos()
  loadTiposCarga()
  loadMercancias()
})
</script>

<style scoped>
.catalogos-management-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* 1. Hero Banner Card */
.hero-banner-card {
  position: relative;
  background: linear-gradient(135deg, #1c1917 0%, #292524 60%, #453406 100%);
  border: 1px solid rgba(234, 179, 8, 0.25);
  border-radius: var(--radius-lg, 12px);
  padding: 1.75rem 2rem;
  color: #fafaf9;
  overflow: hidden;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.hero-content {
  max-width: 780px;
  position: relative;
  z-index: 2;
}

.badge-kicker {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: #eab308;
  display: inline-block;
  margin-bottom: 0.5rem;
  text-transform: uppercase;
}

.hero-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 0.5rem;
  line-height: 1.25;
}

.hero-desc {
  font-size: 0.875rem;
  line-height: 1.5;
  color: #d6d3d1;
  margin: 0;
}

.hero-watermark {
  position: absolute;
  right: 1.5rem;
  top: 50%;
  transform: translateY(-50%);
  font-size: 7rem;
  font-weight: 900;
  color: rgba(255, 255, 255, 0.03);
  letter-spacing: 0.05em;
  user-select: none;
  pointer-events: none;
}

/* 2. Metrics Grid */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.25rem;
}

.metric-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1.15rem 1.35rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: var(--shadow-subtle);
}

.metric-top {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  margin-bottom: 0.5rem;
}

.metric-idx {
  font-size: 0.725rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.metric-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-subtle);
  text-transform: capitalize;
}

.metric-val {
  font-size: 1.85rem;
  font-weight: 800;
  letter-spacing: -0.03em;
  color: var(--color-text-main);
  line-height: 1;
}

/* 3. Catalog Tabs Bar */
.catalog-tabs-bar {
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 0.5rem;
}

.catalog-tabs {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.catalog-tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.65rem 1.15rem;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-pill);
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.catalog-tab-btn:hover {
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
}

.catalog-tab-btn.active {
  background: var(--color-primary);
  color: var(--color-primary-text);
  box-shadow: 0 2px 6px rgba(53, 47, 42, 0.2);
}

.tab-count-badge {
  font-size: 0.7rem;
  font-weight: 800;
  background: var(--color-bg-subtle);
  color: inherit;
  padding: 0.1rem 0.45rem;
  border-radius: 10px;
}

.catalog-tab-btn.active .tab-count-badge {
  background: rgba(0, 0, 0, 0.2);
  color: var(--color-primary-text);
}

/* 4. Toolbar */
.section-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  margin-top: 0.25rem;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
  min-width: 320px;
}

.search-icon {
  position: absolute;
  left: 0.85rem;
  color: var(--color-text-subtle);
  font-size: 0.85rem;
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 0.55rem 2rem 0.55rem 2.25rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 0.825rem;
  color: var(--color-text-main);
  outline: none;
  transition: all var(--transition-fast);
}

.search-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(214, 180, 118, 0.2);
}

.clear-search-btn {
  position: absolute;
  right: 0.5rem;
  background: transparent;
  border: none;
  color: var(--color-text-subtle);
  cursor: pointer;
  padding: 0.2rem;
}

.filter-select {
  padding: 0.55rem 0.85rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 0.825rem;
  color: var(--color-text-main);
  outline: none;
  cursor: pointer;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.btn-refresh {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.55rem 0.95rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 0.825rem;
  font-weight: 600;
  color: var(--color-text-main);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-refresh:hover:not(:disabled) {
  background: var(--color-bg-subtle);
}

.btn-primary-action {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.55rem 1.15rem;
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-sm);
  font-size: 0.825rem;
  font-weight: 700;
  color: var(--color-primary-text);
  cursor: pointer;
  transition: all var(--transition-fast);
  box-shadow: var(--shadow-subtle);
}

.btn-primary-action:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

/* 5. Tabla Editorial */
.data-table-container {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow-x: auto;
  box-shadow: var(--shadow-subtle);
}

.editorial-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 0.825rem;
}

.editorial-table th {
  background: var(--color-bg-subtle);
  color: var(--color-accent-kicker);
  font-size: 0.6875rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  padding: 0.85rem 1.25rem;
  border-bottom: 1px solid var(--color-border);
  text-transform: uppercase;
  white-space: nowrap;
}

.editorial-table td {
  padding: 0.95rem 1.25rem;
  border-bottom: 1px solid var(--color-border-subtle);
  vertical-align: middle;
  color: var(--color-text-main);
}

.editorial-table tbody tr:hover {
  background: var(--color-bg-subtle);
}

.code-badge {
  display: inline-block;
  padding: 0.2rem 0.5rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  color: var(--color-accent-kicker);
  font-weight: 800;
  font-size: 0.775rem;
  border-radius: 4px;
  letter-spacing: 0.04em;
}

.port-name-cell {
  display: flex;
  flex-direction: column;
}

.port-name {
  font-weight: 700;
  color: var(--color-text-main);
}

.port-id-sub {
  font-size: 0.7rem;
  color: var(--color-text-subtle);
}

.country-pill {
  display: inline-block;
  padding: 0.15rem 0.5rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.container-pill {
  display: inline-block;
  padding: 0.15rem 0.5rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.tara-val {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-main);
}

.type-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.2rem 0.6rem;
  border-radius: var(--radius-pill);
  font-size: 0.725rem;
  font-weight: 700;
  text-transform: uppercase;
  border: 1px solid transparent;
}

.type-badge.type-maritimo {
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border-color: var(--color-info-border);
}

.type-badge.type-aereo {
  background: #fdf2f8;
  color: #9d174d;
  border-color: #fbcfe8;
}

:global(.dark-theme) .type-badge.type-aereo {
  background: #371427;
  color: #f472b6;
  border-color: #5c1d3f;
}

.type-badge.type-terrestre {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border-color: var(--color-warning-border);
}

.type-badge.type-fluvial {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border-color: var(--color-success-border);
}

.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-pill);
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
}

.status-chip.active {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.status-chip.active .chip-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status-chip.inactive {
  background: var(--color-bg-muted);
  color: var(--color-text-subtle);
  border: 1px solid var(--color-border);
}

.status-chip.inactive .chip-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.actions-cell {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

.action-btn {
  padding: 0.35rem 0.65rem;
  border-radius: var(--radius-sm);
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  transition: all var(--transition-fast);
}

.edit-btn {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
}

.edit-btn:hover {
  background: var(--color-bg-surface);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.deactivate-btn {
  background: var(--color-danger-bg);
  border: 1px solid var(--color-danger-border);
  color: var(--color-danger-text);
  padding: 0.35rem 0.5rem;
}

.deactivate-btn:hover {
  filter: brightness(1.1);
}

.reactivate-btn {
  background: var(--color-success-bg);
  border: 1px solid var(--color-success-border);
  color: var(--color-success-text);
  padding: 0.35rem 0.5rem;
}

.reactivate-btn:hover {
  filter: brightness(1.1);
}

.state-cell {
  padding: 3rem 1rem !important;
  text-align: center;
  color: var(--color-text-subtle);
  font-size: 0.85rem;
}

.state-icon {
  display: block;
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
  color: var(--color-accent-kicker);
}

@media (max-width: 1024px) {
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }
  .section-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .search-box {
    min-width: 100%;
  }
  .toolbar-left,
  .toolbar-right {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
