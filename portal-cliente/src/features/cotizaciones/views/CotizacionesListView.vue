<template>
  <div class="cotizaciones-view">
    <!-- Header de Sección -->
    <div class="view-header">
      <div class="header-titles">
        <span class="view-kicker">MÓDULO COMERCIAL B2B</span>
        <h1 class="view-title">Mis Cotizaciones Logísticas</h1>
        <p class="view-subtitle">Consulte sus cotizaciones de importación y exportación, desglose de costos y aprobación de servicios.</p>
      </div>
      <div class="header-actions">
        <button class="btn-primary" @click="openSolicitudModal">
          <i class="pi pi-plus"></i>
          <span>Solicitar Nueva Cotización</span>
        </button>
      </div>
    </div>

    <!-- Tarjetas Métricas KPI con numeración 01..04 Warm Editorial -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-idx">01</span>
          <div class="kpi-icon-box">
            <i class="pi pi-file"></i>
          </div>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">TOTAL COTIZACIONES</span>
          <span class="kpi-value font-mono">{{ cotizaciones.length }}</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-idx">02</span>
          <div class="kpi-icon-box success">
            <i class="pi pi-check-circle"></i>
          </div>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">APROBADAS PARA DESPACHO</span>
          <span class="kpi-value font-mono">{{ cotizacionesAprobadas }}</span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-idx">03</span>
          <div class="kpi-icon-box warning">
            <i class="pi pi-clock"></i>
          </div>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">EN EVALUACIÓN TÉCNICA</span>
          <span class="kpi-value font-mono">{{ cotizacionesPendientes }}</span>
        </div>
      </div>

      <div class="kpi-card highlighted">
        <div class="kpi-top">
          <span class="kpi-idx">04</span>
          <div class="kpi-icon-box accent">
            <i class="pi pi-dollar"></i>
          </div>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">VALOR TOTAL COTIZADO</span>
          <span class="kpi-value font-mono">$ {{ formatCurrency(totalMontoCotizado) }}</span>
        </div>
      </div>
    </div>

    <!-- Barra de Filtros -->
    <div class="filters-bar">
      <div class="search-box">
        <i class="pi pi-search"></i>
        <input 
          v-model="filtroTexto" 
          type="text" 
          placeholder="Buscar por código, mercancía o puerto..." 
          class="search-input"
        />
      </div>

      <div class="filter-group">
        <label>Régimen:</label>
        <select v-model="filtroRegimen" class="custom-select">
          <option value="">Todos los regímenes</option>
          <option value="IMPORTACION_DEFINITIVA">Importación Definitiva</option>
          <option value="EXPORTACION_DEFINITIVA">Exportación Definitiva</option>
        </select>
      </div>

      <div class="filter-group">
        <label>Estado:</label>
        <select v-model="filtroEstado" class="custom-select">
          <option value="">Todos los estados</option>
          <option value="APROBADA">Aprobadas</option>
          <option value="PENDIENTE">En Evaluación</option>
          <option value="OBSERVADA">Observadas</option>
        </select>
      </div>
    </div>

    <!-- Tabla de Cotizaciones -->
    <div class="table-card">
      <div v-if="loading" class="loading-state">
        <i class="pi pi-spin pi-spinner"></i>
        <span>Cargando cotizaciones de su empresa...</span>
      </div>

      <div v-else-if="cotizacionesFiltradas.length === 0" class="empty-state">
        <i class="pi pi-folder-open"></i>
        <h3>No se encontraron cotizaciones</h3>
        <p>No hay registros que coincidan con los filtros seleccionados o aún no ha solicitado cotizaciones.</p>
      </div>

      <div v-else class="table-responsive">
        <table class="data-table">
          <thead>
            <tr>
              <th>CÓDIGO</th>
              <th>MERCANCÍA & CARGA</th>
              <th>RÉGIMEN</th>
              <th>RUTA LOGÍSTICA</th>
              <th class="text-right">VALOR FOB</th>
              <th class="text-right">TOTAL ESTIMADO</th>
              <th class="text-center">ESTADO</th>
              <th class="text-center">ESTADO PAGO</th>
              <th class="text-center">ACCIONES</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in paginatedCotizaciones" :key="c.idCotizacion">
              <td class="font-mono font-bold">{{ c.codigoCotizacion }}</td>
              <td>
                <div class="item-name">{{ c.nombreMercancia || 'Mercancía General' }}</div>
                <div class="item-sub font-mono">{{ c.codigoMercancia || 'Sin partida' }} • {{ c.nombreTipoCarga || 'General' }}</div>
              </td>
              <td>
                <span class="regimen-tag" :class="c.regimenAduanero?.toLowerCase()">
                  {{ formatRegimen(c.regimenAduanero) }}
                </span>
              </td>
              <td>
                <div class="route-box">
                  <span>{{ c.nombrePuertoEnvio || 'Origen' }}</span>
                  <i class="pi pi-arrow-right"></i>
                  <span>{{ c.nombrePuertoDestino || 'Destino' }}</span>
                </div>
              </td>
              <td class="text-right font-mono">$ {{ formatCurrency(c.valorFob) }}</td>
              <td class="text-right font-mono font-bold highlight-cell">$ {{ formatCurrency(c.totalCotizado) }}</td>
              <td class="text-center">
                <span class="status-badge" :class="getStatusClass(c.estado)">
                  {{ c.estado }}
                </span>
              </td>
              <td class="text-center">
                <span class="pago-badge" :class="getPagoClass(c.estadoPago)">
                  {{ formatPago(c.estadoPago) }}
                </span>
              </td>
              <td class="text-center">
                <button class="btn-table-action" @click="verDetalle(c)" title="Ver Desglose Completo">
                  <i class="pi pi-eye"></i>
                  <span>Detalle</span>
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Paginador Compartido -->
        <BasePagination 
          v-if="totalItems > pageSize"
          v-model:currentPage="currentPage"
          :total-pages="totalPages"
          :total-items="totalItems"
          :start-index="startIndex"
          :end-index="endIndex"
          item-label="cotizaciones"
        />
      </div>
    </div>

    <!-- Modal de Detalle de Cotización -->
    <BaseModal
      v-model:visible="modalDetalleVisible"
      :title="cotizacionSeleccionada?.codigoCotizacion || 'Detalle de Cotización'"
      subtitle="DETALLE DE COTIZACIÓN COMERCIAL"
      icon="pi pi-file"
      max-width="680px"
      @close="modalDetalleVisible = false"
    >
      <div class="modal-body" v-if="cotizacionSeleccionada">
        <!-- Información General -->
        <div class="detail-section">
          <h4 class="section-title">Información de la Operación</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">Mercancía:</span>
              <span class="detail-val">{{ cotizacionSeleccionada.nombreMercancia || 'Mercancía General' }} ({{ cotizacionSeleccionada.codigoMercancia || '-' }})</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Tipo de Carga:</span>
              <span class="detail-val">{{ cotizacionSeleccionada.nombreTipoCarga || 'General' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Régimen Aduanero:</span>
              <span class="detail-val">{{ formatRegimen(cotizacionSeleccionada.regimenAduanero) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Ruta Puerto:</span>
              <span class="detail-val">{{ cotizacionSeleccionada.nombrePuertoEnvio || 'Origen' }} &rarr; {{ cotizacionSeleccionada.nombrePuertoDestino || 'Destino' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Transporte Local:</span>
              <span class="detail-val">{{ cotizacionSeleccionada.servicioTransporte ? 'Sí (Incluido a almacén)' : 'No incluido' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">Validez Comercial:</span>
              <span class="detail-val">{{ cotizacionSeleccionada.validezDias }} días (Emitida: {{ cotizacionSeleccionada.fechaEmision }})</span>
            </div>
          </div>
        </div>

        <!-- Desglose Económico y Liquidación Estimada -->
        <div class="detail-section">
          <h4 class="section-title">Desglose de Costos y Tributos (USD)</h4>
          <div class="cost-table">
            <div class="cost-row">
              <span>Valor Mercancía (FOB estimado):</span>
              <span class="font-mono">$ {{ formatCurrency(cotizacionSeleccionada.valorFob) }}</span>
            </div>
            <div class="cost-row">
              <span>Flete Internacional Estimado:</span>
              <span class="font-mono">$ {{ formatCurrency(cotizacionSeleccionada.montoFlete) }}</span>
            </div>
            <div class="cost-row">
              <span>Seguro de Transporte Estimado:</span>
              <span class="font-mono">$ {{ formatCurrency(cotizacionSeleccionada.montoSeguroEstimado) }}</span>
            </div>
            <div class="cost-row">
              <span>Honorarios de Agenciamiento Aduanero:</span>
              <span class="font-mono">$ {{ formatCurrency(cotizacionSeleccionada.montoHonorarios) }}</span>
            </div>
            <div class="cost-row highlight">
              <span>Tributos Aduaneros Estimados (Aranceles + IGV):</span>
              <span class="font-mono">$ {{ formatCurrency(cotizacionSeleccionada.montoTributos) }}</span>
            </div>
            <div class="cost-row total-row">
              <span>TOTAL ESTIMADO DEL SERVICIO:</span>
              <span class="font-mono total-amount">$ {{ formatCurrency(cotizacionSeleccionada.totalCotizado) }}</span>
            </div>
          </div>
        </div>

        <!-- Estado de Pagos y Anticipos -->
        <div class="detail-section">
          <h4 class="section-title">Control Financiero de Anticipos</h4>
          <div class="financial-status-box">
            <div class="fin-col">
              <span class="fin-label">Total Cotizado:</span>
              <span class="fin-val font-mono">$ {{ formatCurrency(cotizacionSeleccionada.totalCotizado) }}</span>
            </div>
            <div class="fin-col">
              <span class="fin-label">Total Abonado:</span>
              <span class="fin-val font-mono success">$ {{ formatCurrency(cotizacionSeleccionada.totalAbonado) }}</span>
            </div>
            <div class="fin-col">
              <span class="fin-label">Saldo Pendiente:</span>
              <span class="fin-val font-mono warning">$ {{ formatCurrency(cotizacionSeleccionada.saldoPendiente) }}</span>
            </div>
          </div>
        </div>

        <!-- Banner de Observación Técnica de Aduanas -->
        <div v-if="cotizacionSeleccionada.estado === 'OBSERVADA'" class="alert-box-observation">
          <div class="alert-obs-header">
            <i class="pi pi-exclamation-triangle"></i>
            <strong>Observación Técnica de Aduanas Emitida</strong>
          </div>
          <p class="alert-obs-message">
            {{ cotizacionSeleccionada.motivoObservacion || 'Se requieren correcciones técnicas o precisiones documentarias en la propuesta comercial.' }}
          </p>
          <div class="alert-obs-footer">
            <i class="pi pi-info-circle"></i>
            <span>Su asesor comercial asignado se encuentra coordinando con usted la subsanación de estos requisitos antes del reingreso a supervisión.</span>
          </div>
        </div>

        <!-- Observaciones -->
        <div v-if="cotizacionSeleccionada.observaciones" class="observations-box">
          <i class="pi pi-info-circle"></i>
          <span>{{ cotizacionSeleccionada.observaciones }}</span>
        </div>
      </div>

      <template #footer>
        <div class="modal-footer">
          <button class="btn-secondary" @click="modalDetalleVisible = false">
            Cerrar
          </button>
          <button 
            v-if="cotizacionSeleccionada?.estado === 'APROBADA'" 
            class="btn-primary" 
            @click="handleAceptarCotizacion(cotizacionSeleccionada)"
            :disabled="aceptando || cotizacionSeleccionada?.aceptadaPorCliente"
          >
            <i :class="aceptando ? 'pi pi-spin pi-spinner' : (cotizacionSeleccionada?.aceptadaPorCliente ? 'pi pi-check-circle' : 'pi pi-check')"></i>
            <span>{{ cotizacionSeleccionada?.aceptadaPorCliente ? 'Cotización Ya Autorizada' : 'Aceptar y Autorizar Despacho' }}</span>
          </button>
        </div>
      </template>
    </BaseModal>

    <!-- Modal de Solicitar Nueva Cotización -->
    <BaseModal
      v-model:visible="modalSolicitudVisible"
      title="Solicitar Cotización de Agenciamiento"
      subtitle="NUEVA OPERACIÓN"
      icon="pi pi-file"
      max-width="680px"
      :busy="enviandoSolicitud"
      @close="modalSolicitudVisible = false"
    >
      <form @submit.prevent="enviarSolicitud" class="modal-body">
        <div class="form-grid">
          <div class="form-field">
            <label>Régimen Aduanero *</label>
            <select v-model="solicitudForm.regimenAduanero" required class="custom-select">
              <option value="IMPORTACION_DEFINITIVA">Importación Definitiva</option>
              <option value="EXPORTACION_DEFINITIVA">Exportación Definitiva</option>
            </select>
          </div>

          <div class="form-field">
            <label>Modalidad de Transporte *</label>
            <select v-model="modalidadSeleccionada" class="custom-select" @change="onModalidadChange">
              <option value="MARITIMO">Marítimo</option>
              <option value="AEREO">Aéreo</option>
              <option value="TERRESTRE">Terrestre</option>
            </select>
          </div>

          <div class="form-field">
            <label>Tipo de Carga:</label>
            <select v-model="solicitudForm.idTipoCarga" class="custom-select">
              <option :value="null">Seleccione tipo de carga...</option>
              <option v-for="tc in tiposCarga" :key="tc.idTipoCarga" :value="tc.idTipoCarga">
                {{ tc.nombre }}
              </option>
            </select>
          </div>

          <div class="form-field">
            <label>Mercancía / Partida:</label>
            <select v-model="solicitudForm.idMercancia" class="custom-select">
              <option :value="null">Seleccione mercancía...</option>
              <option v-for="m in mercancias" :key="m.idMercancia" :value="m.idMercancia">
                {{ m.codigo }} - {{ m.nombre }}
              </option>
            </select>
          </div>

          <div class="form-field">
            <label>Puerto de Embarque (Origen):</label>
            <select v-model="solicitudForm.idPuertoEnvio" class="custom-select">
              <option :value="null">Seleccione puerto de salida...</option>
              <option v-for="p in puertosFiltrados" :key="'env-' + p.idPuerto" :value="p.idPuerto">
                {{ p.nombre }} ({{ p.pais }})
              </option>
            </select>
          </div>

          <div class="form-field">
            <label>Puerto de Llegada (Destino):</label>
            <select v-model="solicitudForm.idPuertoDestino" class="custom-select">
              <option :value="null">Seleccione puerto de arribo...</option>
              <option v-for="p in puertosFiltrados" :key="'dest-' + p.idPuerto" :value="p.idPuerto">
                {{ p.nombre }} ({{ p.pais }})
              </option>
            </select>
          </div>

          <div class="form-field full">
            <label>Valor FOB Estimado (USD) *</label>
            <input 
              v-model.number="solicitudForm.valorFob" 
              type="number" 
              step="0.01" 
              min="1" 
              placeholder="Ej. 25000.00" 
              required 
              class="custom-input font-mono"
            />
          </div>

          <div class="form-field full">
            <label class="checkbox-label">
              <input type="checkbox" v-model="solicitudForm.servicioTransporte" />
              <span>Requiero servicio de transporte terrestre hasta almacén / planta</span>
            </label>
          </div>

          <div class="form-field full">
            <label>Detalle de la Carga u Observaciones:</label>
            <textarea 
              v-model="solicitudForm.observaciones" 
              rows="3" 
              placeholder="Describa el tipo de mercancía, volumen en bultos o requerimientos especiales..."
              class="custom-textarea"
            ></textarea>
          </div>
        </div>

        <div class="modal-footer" style="padding: 1rem 0 0 0; margin-top: 1rem;">
          <button type="button" class="btn-secondary" @click="modalSolicitudVisible = false" :disabled="enviandoSolicitud">
            Cancelar
          </button>
          <button type="submit" class="btn-primary" :disabled="enviandoSolicitud">
            <i v-if="enviandoSolicitud" class="pi pi-spin pi-spinner"></i>
            <span v-else>Enviar Solicitud a Evaluación</span>
          </button>
        </div>
      </form>
    </BaseModal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { portalCotizacionService } from '../services/portalCotizacionService'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import BasePagination from '@/shared/components/BasePagination.vue'
import BaseModal from '@/shared/components/BaseModal.vue'

const { showSuccess, showError, showWarn } = useNotification()
const { confirmAction } = useConfirmation()

const cotizaciones = ref([])
const loading = ref(true)
const filtroTexto = ref('')
const filtroRegimen = ref('')
const filtroEstado = ref('')

// Catálogos dinámicos
const puertos = ref([])
const mercancias = ref([])
const tiposCarga = ref([])
const modalidadSeleccionada = ref('MARITIMO')

// Paginación
const currentPage = ref(1)
const pageSize = ref(10)

const modalDetalleVisible = ref(false)
const cotizacionSeleccionada = ref(null)
const aceptando = ref(false)

const modalSolicitudVisible = ref(false)
const enviandoSolicitud = ref(false)
const solicitudForm = ref({
  regimenAduanero: 'IMPORTACION_DEFINITIVA',
  valorFob: null,
  idMercancia: null,
  idTipoCarga: null,
  idPuertoEnvio: null,
  idPuertoDestino: null,
  servicioTransporte: true,
  observaciones: ''
})

const puertosFiltrados = computed(() => {
  if (!modalidadSeleccionada.value) return puertos.value
  return puertos.value.filter(p => !p.tipo || p.tipo.toUpperCase() === modalidadSeleccionada.value)
})

function onModalidadChange() {
  solicitudForm.value.idPuertoEnvio = null
  solicitudForm.value.idPuertoDestino = null
}

const cotizacionesFiltradas = computed(() => {
  return cotizaciones.value.filter(c => {
    const matchRegimen = !filtroRegimen.value || c.regimenAduanero === filtroRegimen.value
    const matchEstado = !filtroEstado.value || c.estado === filtroEstado.value
    const query = filtroTexto.value.toLowerCase().trim()
    const matchTexto = !query 
      || c.codigoCotizacion?.toLowerCase().includes(query)
      || c.nombreMercancia?.toLowerCase().includes(query)
      || c.nombrePuertoDestino?.toLowerCase().includes(query)

    return matchRegimen && matchEstado && matchTexto
  })
})

const totalItems = computed(() => cotizacionesFiltradas.value.length)
const totalPages = computed(() => Math.ceil(totalItems.value / pageSize.value) || 1)
const startIndex = computed(() => (totalItems.value === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1))
const endIndex = computed(() => Math.min(currentPage.value * pageSize.value, totalItems.value))

const paginatedCotizaciones = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return cotizacionesFiltradas.value.slice(start, start + pageSize.value)
})

watch([filtroTexto, filtroRegimen, filtroEstado], () => {
  currentPage.value = 1
})

const cotizacionesAprobadas = computed(() => {
  return cotizaciones.value.filter(c => c.estado === 'APROBADA').length
})

const cotizacionesPendientes = computed(() => {
  return cotizaciones.value.filter(c => c.estado === 'PENDIENTE').length
})

const totalMontoCotizado = computed(() => {
  return cotizaciones.value.reduce((acc, c) => acc + (Number(c.totalCotizado) || 0), 0)
})

onMounted(async () => {
  await Promise.all([
    cargarCotizaciones(),
    cargarCatalogos()
  ])
})

async function cargarCatalogos() {
  try {
    const [pList, mList, tcList] = await Promise.all([
      portalCotizacionService.obtenerPuertos().catch(() => []),
      portalCotizacionService.obtenerMercancias().catch(() => []),
      portalCotizacionService.obtenerTiposCarga().catch(() => [])
    ])
    puertos.value = pList
    mercancias.value = mList
    tiposCarga.value = tcList
  } catch (err) {
    console.debug('Error al cargar catálogos:', err)
  }
}

async function cargarCotizaciones() {
  loading.value = true
  try {
    const data = await portalCotizacionService.listarCotizaciones()
    cotizaciones.value = data || []
  } catch (err) {
    showError(err.response?.data?.message || 'Error al cargar cotizaciones')
  } finally {
    loading.value = false
  }
}

function formatCurrency(val) {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatRegimen(reg) {
  if (!reg) return ''
  return reg.replace('_', ' ')
}

function formatPago(pago) {
  if (!pago) return 'SIN PAGO'
  return pago.replace('_', ' ')
}

function getStatusClass(st) {
  switch (st) {
    case 'APROBADA': return 'success'
    case 'PENDIENTE': return 'warning'
    case 'OBSERVADA': return 'danger'
    case 'RECHAZADA': return 'danger'
    default: return 'neutral'
  }
}

function getPagoClass(pago) {
  switch (pago) {
    case 'PAGADO_TOTAL': return 'success'
    case 'PAGADO_PARCIAL': return 'info'
    default: return 'neutral'
  }
}

function verDetalle(c) {
  cotizacionSeleccionada.value = c
  modalDetalleVisible.value = true
}

async function handleAceptarCotizacion(c) {
  if (!c) return
  if (c.aceptadaPorCliente) {
    showWarn('Esta cotización ya fue aceptada y autorizada previamente.')
    return
  }

  const confirmed = await confirmAction({
    header: 'Aceptar y Autorizar Despacho',
    message: `¿Está seguro de autorizar la cotización ${c.codigoCotizacion} por un monto de $ ${formatCurrency(c.totalCotizado)} USD? Al aceptar, se iniciará el despacho aduanero.`,
    icon: 'pi pi-check-circle',
    acceptLabel: 'Autorizar Despacho',
    rejectLabel: 'Cancelar',
    acceptClass: 'p-button-success'
  })

  if (!confirmed) return

  aceptando.value = true
  try {
    const res = await portalCotizacionService.aceptarCotizacion(c.idCotizacion)
    if (res) {
      modalDetalleVisible.value = false
      showSuccess(`Cotización ${c.codigoCotizacion} aceptada con éxito. Se ha generado la orden de despacho.`, 'Operación Autorizada', 4000)
      await cargarCotizaciones()
    }
  } catch (err) {
    showError(err.response?.data?.message || 'Error al aceptar cotización')
  } finally {
    aceptando.value = false
  }
}

function openSolicitudModal() {
  solicitudForm.value = {
    regimenAduanero: 'IMPORTACION_DEFINITIVA',
    valorFob: null,
    idMercancia: null,
    idTipoCarga: null,
    idPuertoEnvio: null,
    idPuertoDestino: null,
    servicioTransporte: true,
    observaciones: ''
  }
  modalidadSeleccionada.value = 'MARITIMO'
  modalSolicitudVisible.value = true
}

async function enviarSolicitud() {
  enviandoSolicitud.value = true
  try {
    const res = await portalCotizacionService.solicitarCotizacion(solicitudForm.value)
    if (res) {
      modalSolicitudVisible.value = false
      showSuccess('Solicitud de cotización remitida con éxito a evaluación técnica.', 'Solicitud Enviada')
      await cargarCotizaciones()
    }
  } catch (err) {
    showError(err.response?.data?.message || 'Error al enviar solicitud')
  } finally {
    enviandoSolicitud.value = false
  }
}
</script>

<style scoped>
.cotizaciones-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  width: 100%;
}

/* Header */
.view-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.view-kicker {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
}

.view-title {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-text-main);
  margin: 0.2rem 0;
  letter-spacing: -0.02em;
}

.view-subtitle {
  font-size: 0.85rem;
  color: var(--color-text-muted);
  margin: 0;
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1.15rem;
  background: var(--color-primary);
  color: var(--color-primary-text);
  border: none;
  border-radius: 6px;
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.btn-primary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn-secondary {
  padding: 0.65rem 1.15rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  border-radius: 6px;
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-secondary:hover:not(:disabled) {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

/* KPI Grid */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

.kpi-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 0.75rem;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.kpi-card:hover {
  transform: translateY(-2px);
  border-color: var(--color-border-strong);
}

.kpi-card.highlighted {
  border-color: var(--color-accent-kicker);
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.kpi-idx {
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: 6px;
  padding: 0.15rem 0.45rem;
  letter-spacing: 0.02em;
  font-family: var(--font-mono);
}

.kpi-icon-box {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  color: var(--color-primary);
}

.kpi-icon-box.success {
  background: var(--color-success-bg);
  border-color: var(--color-success-border);
  color: var(--color-success-text);
}

.kpi-icon-box.warning {
  background: var(--color-warning-bg);
  border-color: var(--color-warning-border);
  color: var(--color-warning-text);
}

.kpi-icon-box.accent {
  background: var(--color-bg-subtle);
  color: var(--color-accent-kicker);
}

.kpi-content {
  display: flex;
  flex-direction: column;
}

.kpi-label {
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--color-text-subtle);
}

.kpi-value {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-text-main);
  margin-top: 0.15rem;
}

/* Filtros */
.filters-bar {
  display: flex;
  align-items: center;
  gap: 1rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 0.85rem 1rem;
  flex-wrap: wrap;
}

.search-box {
  flex: 1;
  min-width: 250px;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 0.45rem 0.75rem;
  background: var(--color-bg-base);
}

.search-box i {
  color: var(--color-text-subtle);
}

.search-box input {
  border: none;
  background: transparent;
  width: 100%;
  font-size: 0.82rem;
  color: var(--color-text-main);
  outline: none;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.custom-select {
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 0.45rem 0.65rem;
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  font-size: 0.82rem;
  outline: none;
}

/* Tabla */
.table-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  overflow: hidden;
}

.table-responsive {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 0.82rem;
}

.data-table th {
  background: var(--color-bg-subtle);
  padding: 0.75rem 1rem;
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-text-muted);
  letter-spacing: 0.05em;
  border-bottom: 1px solid var(--color-border);
}

.data-table td {
  padding: 0.85rem 1rem;
  border-bottom: 1px solid var(--color-border-subtle);
  color: var(--color-text-main);
}

.data-table tr:hover {
  background: var(--color-bg-subtle);
}

.item-name {
  font-weight: 600;
}

.item-sub {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

.route-box {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.78rem;
}

.route-box i {
  font-size: 0.65rem;
  color: var(--color-accent-kicker);
}

.highlight-cell {
  color: var(--color-primary);
}

.regimen-tag {
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.2rem 0.45rem;
  border-radius: 4px;
}

.regimen-tag.importacion_definitiva {
  background: var(--color-info-bg, var(--color-bg-subtle));
  color: var(--color-info-text, var(--color-primary));
  border: 1px solid var(--color-border-subtle);
}

.regimen-tag.exportacion_definitiva {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.status-badge {
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.2rem 0.5rem;
  border-radius: 12px;
  display: inline-block;
}

.status-badge.success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.status-badge.warning {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.status-badge.danger {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border: 1px solid var(--color-danger-border);
}

.status-badge.neutral {
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
}

.pago-badge {
  font-size: 0.72rem;
  font-weight: 600;
}

.pago-badge.success {
  color: var(--color-success-text);
}

.pago-badge.info {
  color: var(--color-info-text, var(--color-primary));
}

.pago-badge.neutral {
  color: var(--color-text-subtle);
}

.btn-table-action {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.65rem;
  background: var(--color-bg-base);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-main);
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-table-action:hover {
  background: var(--color-primary);
  color: var(--color-primary-text);
  border-color: var(--color-primary);
}

/* Modales */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1.5rem;
}

.modal-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  width: 100%;
  max-width: 680px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: var(--shadow-elevated, 0 10px 30px rgba(0, 0, 0, 0.25));
}

.modal-header {
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-bg-subtle);
}

.modal-kicker {
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
}

.modal-title {
  margin: 0.15rem 0 0 0;
  font-size: 1.25rem;
  color: var(--color-text-main);
}

.close-btn {
  background: transparent;
  border: none;
  font-size: 1.1rem;
  cursor: pointer;
  color: var(--color-text-subtle);
  padding: 0.35rem;
  border-radius: 4px;
}

.close-btn:hover {
  color: var(--color-text-main);
  background: var(--color-bg-muted);
}

.modal-body {
  padding: 1.5rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.section-title {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-primary);
  margin: 0 0 0.65rem 0;
  border-bottom: 1px solid var(--color-border-subtle);
  padding-bottom: 0.35rem;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.75rem;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.detail-label {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

.detail-val {
  font-size: 0.82rem;
  font-weight: 600;
}

.cost-table {
  background: var(--color-bg-base);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
}

.cost-row {
  display: flex;
  justify-content: space-between;
  padding: 0.55rem 0.85rem;
  border-bottom: 1px solid var(--color-border-subtle);
  font-size: 0.82rem;
}

.cost-row.highlight {
  background: var(--color-warning-bg);
  font-weight: 600;
}

.cost-row.total-row {
  background: var(--color-bg-subtle);
  font-weight: 700;
  border-bottom: none;
}

.total-amount {
  color: var(--color-primary);
}

.financial-status-box {
  display: flex;
  justify-content: space-between;
  background: var(--color-bg-base);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 0.75rem 1rem;
}

.fin-col {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.fin-label {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

.fin-val {
  font-size: 0.95rem;
  font-weight: 700;
}

.fin-val.success {
  color: var(--color-success-text);
}

.fin-val.warning {
  color: var(--color-warning-text);
}

.observations-box {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 0.85rem;
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border);
  border-radius: 6px;
  font-size: 0.78rem;
  color: var(--color-warning-text);
}

.modal-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--color-border);
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  background: var(--color-bg-subtle);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.form-field.full {
  grid-column: span 2;
}

.form-field label {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.custom-textarea {
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 0.65rem;
  font-family: inherit;
  font-size: 0.82rem;
  outline: none;
  background: var(--color-bg-surface);
  color: var(--color-text-main);
}

.custom-textarea:focus,
.custom-input:focus {
  border-color: var(--color-primary);
}

.custom-input {
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 0.55rem;
  font-size: 0.85rem;
  outline: none;
  background: var(--color-bg-surface);
  color: var(--color-text-main);
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
}

.loading-state, .empty-state {
  padding: 3rem 1.5rem;
  text-align: center;
  color: var(--color-text-muted);
}

.empty-state i {
  font-size: 2.5rem;
  margin-bottom: 0.5rem;
  opacity: 0.4;
}

.empty-state h3 {
  margin: 0.2rem 0;
  color: var(--color-text-main);
}

.empty-state p {
  margin: 0;
  font-size: 0.82rem;
}

.alert-box-observation {
  background: var(--color-danger-bg);
  border: 1px solid var(--color-danger-border);
  border-radius: 8px;
  padding: 0.85rem 1rem;
  margin-bottom: 0.5rem;
}

.alert-obs-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--color-danger-text);
  font-size: 0.875rem;
  margin-bottom: 0.4rem;
}

.alert-obs-message {
  font-size: 0.8125rem;
  color: var(--color-text-main);
  line-height: 1.45;
  margin: 0 0 0.5rem 0;
}

.alert-obs-footer {
  display: flex;
  align-items: flex-start;
  gap: 0.4rem;
  font-size: 0.75rem;
  color: var(--color-text-muted);
  border-top: 1px dashed var(--color-danger-border);
  padding-top: 0.4rem;
}

@media (max-width: 1024px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
  .form-grid {
    grid-template-columns: 1fr;
  }
  .form-field.full {
    grid-column: span 1;
  }
}
</style>
