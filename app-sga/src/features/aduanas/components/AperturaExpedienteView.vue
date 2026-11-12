<template>
  <div class="apertura-expediente-view fade-in">
    <!-- Header Bar de Apertura Formal -->
    <div class="apertura-header-bar">
      <div class="header-titles">
        <div class="kicker-row">
          <span class="badge-kicker">CONTROL ADUANERO // FORMALIZACIÓN OPERATIVA</span>
          <span class="flow-pill">
            <i class="pi pi-check-circle"></i>
            <span>FASE OPERATIVA: APERTURA DE EXPEDIENTE</span>
          </span>
        </div>
        <h1 class="header-main-title">Apertura y Formalización de Expediente Aduanero</h1>
        <p class="header-desc">
          Formalice las propuestas comerciales con visto bueno aduanero y abono verificado en Finanzas, asignando al agente especialista e iniciando la numeración y diligencia ante SUNAT.
        </p>
      </div>

      <div class="header-actions">
        <button
          type="button"
          class="btn-outline-warm"
          @click="loadInitialData"
          :disabled="loading"
          title="Refrescar cotizaciones aprobadas del servidor"
        >
          <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
          <span>Refrescar Lista</span>
        </button>
      </div>
    </div>

    <!-- Feedback de Error Global -->
    <div v-if="errorMessage" class="error-banner">
      <i class="pi pi-exclamation-triangle"></i>
      <span>{{ errorMessage }}</span>
    </div>

    <!-- ============================================================== -->
    <!-- ESTADO A: COLA GUIADA DE COTIZACIONES LISTAS PARA FORMALIZAR    -->
    <!-- ============================================================== -->
    <div v-if="!activeCotizacion" class="apertura-queue-container">
      <div class="queue-header-row">
        <div class="queue-title-wrap">
          <span class="badge-kicker">COLA DE TRABAJO PENDIENTE</span>
          <h3 class="queue-title">Propuestas Comerciales Listas para Aperturar Expediente</h3>
          <p class="queue-subtitle">
            Cotizaciones aprobadas técnicamente que cuentan con abono verificado por el área de Finanzas. Seleccione una propuesta para formalizar su expediente.
          </p>
        </div>
        <span class="badge-counter" v-if="cotizacionesDisponibles.length > 0">
          {{ cotizacionesDisponibles.length }} listas para formalizar
        </span>
      </div>

      <!-- Alerta cuando no hay pendientes -->
      <div v-if="cotizacionesDisponibles.length === 0" class="empty-cotizaciones-alert">
        <i class="pi pi-check-circle alert-icon text-success"></i>
        <div>
          <strong>No hay cotizaciones pendientes de apertura con pago aprobado</strong>
          <p>
            Todas las propuestas comerciales aprobadas ya cuentan con su expediente formalizado, o bien están en espera de validación de pago en el módulo de Finanzas.
          </p>
        </div>
      </div>

      <!-- Cuadrícula de Tarjetas de Cotizaciones Listas -->
      <div v-else class="queue-cards-grid">
        <div
          v-for="c in cotizacionesDisponibles"
          :key="c.idCotizacion"
          class="cotizacion-card-box"
        >
          <div class="card-box-header">
            <span class="doc-code font-mono">{{ c.codigoCotizacion }}</span>
            <span class="regimen-tag">{{ formatRegimen(c.regimenAduanero) }}</span>
          </div>

          <div class="card-box-body">
            <div class="client-info">
              <strong class="client-name">{{ c.clienteRazonSocial || 'Cliente no asignado' }}</strong>
              <span class="client-doc font-mono">RUC/Doc: {{ c.clienteNumeroDocumento }}</span>
            </div>

            <div class="ports-flow-box">
              <span class="port-chip">{{ c.puertoEnvioNombre || 'Origen' }}</span>
              <i class="pi pi-arrow-right"></i>
              <span class="port-chip">{{ c.puertoDestinoNombre || 'Destino' }}</span>
            </div>

            <div class="financial-chips-row">
              <div class="fin-col">
                <span class="fin-label">TOTAL COTIZADO</span>
                <span class="fin-val font-mono">${{ Number(c.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}</span>
              </div>
              <div class="fin-col highlight">
                <span class="fin-label">ABONO FINANZAS</span>
                <span class="fin-val font-mono text-success">
                  <i class="pi pi-check-circle"></i>
                  ${{ Number(c.totalAbonado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}
                </span>
              </div>
            </div>
          </div>

          <div class="card-box-footer">
            <button
              type="button"
              class="btn-formalizar-card"
              @click="selectCotizacionParaApertura(c)"
            >
              <i class="pi pi-bolt"></i>
              <span>Formalizar y Aperturar Expediente</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ============================================================== -->
    <!-- ESTADO B: ASISTENTE DE FORMALIZACIÓN (FORMULARIO CONTEXTUAL)   -->
    <!-- ============================================================== -->
    <div v-else class="apertura-form-flow">
      <!-- Barra de Retorno y Contexto Activo -->
      <div class="form-back-nav">
        <button
          type="button"
          class="btn-back-link"
          @click="clearSelection"
          title="Regresar a la lista de cotizaciones listas"
        >
          <i class="pi pi-arrow-left"></i>
          <span>← Elegir otra cotización</span>
        </button>
        <span class="active-badge font-mono">Formalizando: {{ activeCotizacion.codigoCotizacion }}</span>
      </div>

      <form @submit.prevent="handleSubmit" class="apertura-form-layout">
        <!-- 1. Bloque: Resumen de la Cotización Origen -->
        <section class="section-card">
          <div class="card-section-header">
            <div class="section-idx">01</div>
            <div>
              <h3 class="section-title">Cotización Comercial Aprobada (Origen)</h3>
              <p class="section-sub">Información técnica y financiera validada en Finanzas y Aduanas.</p>
            </div>
          </div>

          <div class="card-section-body">
            <div class="cotizacion-summary-panel">
              <div class="summary-col">
                <span class="summary-label">CLIENTE IMPORTADOR/EXPORTADOR</span>
                <strong class="summary-val">{{ activeCotizacion.clienteRazonSocial || '—' }}</strong>
                <span class="summary-sub">RUC/Doc: {{ activeCotizacion.clienteNumeroDocumento }}</span>
              </div>

              <div class="summary-col">
                <span class="summary-label">RÉGIMEN ADUANERO</span>
                <span class="regimen-badge">{{ formatRegimen(activeCotizacion.regimenAduanero) }}</span>
              </div>

              <div class="summary-col">
                <span class="summary-label">MODALIDAD & PUERTOS</span>
                <div class="ports-flow">
                  <span class="port-chip">{{ activeCotizacion.puertoEnvioNombre || 'Origen' }}</span>
                  <i class="pi pi-arrow-right"></i>
                  <span class="port-chip">{{ activeCotizacion.puertoDestinoNombre || 'Destino' }}</span>
                </div>
              </div>

              <div class="summary-col">
                <span class="summary-label">TOTAL COTIZADO</span>
                <strong class="summary-val text-accent">
                  ${{ Number(activeCotizacion.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }} USD
                </strong>
              </div>

              <div class="summary-col">
                <span class="summary-label">ABONO FINANZAS</span>
                <strong class="summary-val text-success">
                  <i class="pi pi-check-circle"></i>
                  ${{ Number(activeCotizacion.totalAbonado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}
                </strong>
                <span class="summary-sub text-success">Pago Verificado</span>
              </div>
            </div>
          </div>
        </section>

        <!-- 2. Bloque: Agente Aduanero y Contactos -->
        <section class="section-card">
          <div class="card-section-header">
            <div class="section-idx">02</div>
            <div>
              <h3 class="section-title">Especialistas Asignados & Contacto en el Exterior</h3>
              <p class="section-sub">Agente aduanero que liderará el despacho ante SUNAT y enlace con embarcador/proveedor.</p>
            </div>
          </div>

          <div class="card-section-body form-grid">
            <div class="field-item full-width">
              <label for="exp-agente">Agente Aduanero Responsable *</label>
              <div class="input-with-icon">
                <i class="pi pi-user-edit field-icon"></i>
                <select id="exp-agente" v-model="form.idAgenteAduanero" class="select-input" required>
                  <option :value="null" disabled>-- Seleccione un Agente Aduanero Acreditado --</option>
                  <option v-for="u in agentesList" :key="u.idUsuario" :value="u.idUsuario">
                    {{ u.nombres }} {{ u.apellidos }} ({{ u.codigoSunat ? u.codigoSunat + ' / ' : '' }}@{{ u.username }})
                  </option>
                </select>
              </div>
              <span class="field-hint">Especialista acreditado (Rol: AGENTE_ADUANERO) que gestionará la numeración de DAM, aforo e inspecciones.</span>
            </div>

            <div class="field-item">
              <label for="exp-contacto-nombre">Contacto en el Exterior (Proveedor / Embarcador)</label>
              <input
                id="exp-contacto-nombre"
                v-model.trim="form.contactoExteriorNombre"
                type="text"
                placeholder="Ej. Shanghai Logistics Co. / Hans Gruber"
                class="text-input"
                maxlength="150"
              />
            </div>

            <div class="field-item">
              <label for="exp-contacto-tel">Teléfono / WhatsApp Internacional</label>
              <input
                id="exp-contacto-tel"
                v-model.trim="form.contactoExteriorTelefono"
                type="text"
                placeholder="Ej. +86 21 5555 1234"
                class="text-input font-mono"
                maxlength="30"
              />
            </div>

            <div class="field-item full-width">
              <label for="exp-contacto-correo">Correo Electrónico de Contacto</label>
              <input
                id="exp-contacto-correo"
                v-model.trim="form.contactoExteriorCorreo"
                type="email"
                placeholder="Ej. operations@shanghailogistics.com"
                class="text-input font-mono"
                maxlength="100"
              />
            </div>
          </div>
        </section>

        <!-- 3. Bloque: Logística de Tránsito Internacional & Manifiesto -->
        <section class="section-card">
          <div class="card-section-header">
            <div class="section-idx">03</div>
            <div>
              <h3 class="section-title">Tránsito Internacional & Datos de Manifiesto</h3>
              <p class="section-sub">Fechas estimadas y datos de contenedor o precinto (pueden completarse o rectificarse durante el tránsito).</p>
            </div>
          </div>

          <div class="card-section-body form-grid">
            <div class="field-item">
              <label for="exp-etd">Fecha Estimada de Zarpe / Salida (ETD)</label>
              <input
                id="exp-etd"
                v-model="form.fechaZarpeEtd"
                type="date"
                class="text-input"
              />
            </div>

            <div class="field-item">
              <label for="exp-eta">Fecha Estimada de Llegada / Arribo (ETA)</label>
              <input
                id="exp-eta"
                v-model="form.fechaLlegadaEta"
                type="date"
                class="text-input"
              />
            </div>

            <div class="field-item">
              <label for="exp-nave">Nave / Embarcación / N° Vuelo</label>
              <input
                id="exp-nave"
                v-model.trim="form.naveViaje"
                type="text"
                placeholder="Ej. MSC Gülsün / Vuelo Latam Cargo LA-770"
                class="text-input"
                maxlength="100"
              />
            </div>

            <div class="field-item">
              <label for="exp-contenedor">N° Contenedor (Opcional en apertura)</label>
              <input
                id="exp-contenedor"
                v-model.trim="form.numeroContenedor"
                type="text"
                placeholder="Ej. MSCU-928472-1"
                class="text-input font-mono uppercase"
                maxlength="30"
              />
              <span class="field-hint">Actualizable en tránsito o llegada a depósito.</span>
            </div>

            <div class="field-item">
              <label for="exp-precinto">N° Precinto Aduanero (Opcional en apertura)</label>
              <input
                id="exp-precinto"
                v-model.trim="form.precintoAduanero"
                type="text"
                placeholder="Ej. ML-PE-992147"
                class="text-input font-mono uppercase"
                maxlength="50"
              />
              <span class="field-hint">Actualizable en tránsito o llegada a depósito.</span>
            </div>

            <div class="field-item full-width">
              <label for="exp-obs">Instrucciones Operativas / Observaciones Iniciales</label>
              <textarea
                id="exp-obs"
                v-model.trim="form.observaciones"
                rows="3"
                placeholder="Anotaciones sobre depósito temporal asignado, requerimiento de inspección previa o prioridad de despacho..."
                class="textarea-input"
              ></textarea>
            </div>
          </div>
        </section>

        <!-- Barra de Acciones y Formalización -->
        <div class="form-actions-bar">
          <div class="actions-left">
            <span class="required-note">* Agente aduanero responsable obligatorio.</span>
          </div>
          <div class="actions-right">
            <button
              type="button"
              class="btn-outline-warm"
              @click="clearSelection"
              :disabled="loading"
            >
              <span>Cancelar</span>
            </button>

            <button
              type="submit"
              class="btn-espresso"
              :disabled="loading || !activeCotizacion"
            >
              <i :class="['pi', loading ? 'pi-spin pi-spinner' : 'pi-folder-open']"></i>
              <span>{{ loading ? 'Aperturando Expediente...' : 'Formalizar y Aperturar Expediente' }}</span>
            </button>
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { gestionAduaneraService } from '@/features/aduanas/services/gestionAduaneraService'
import { expedienteService } from '@/features/aduanas/services/expedienteService'
import { userService } from '@/features/admin/users/services/userService'

const props = defineProps({
  cotizacionPreseleccionada: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['created', 'cancel', 'clear-preselection'])

const authStore = useAuthStore()

const loading = ref(false)
const errorMessage = ref('')
const cotizacionesDisponibles = ref([])
const agentesList = ref([])
const selectedCotizacionId = ref(null)

const isCurrentUserAgente = computed(() => {
  return authStore.userRole === 'AGENTE_ADUANERO' || authStore.user?.rol === 'AGENTE_ADUANERO'
})

const defaultForm = () => ({
  idCotizacion: null,
  idAgenteAduanero: isCurrentUserAgente.value ? authStore.user?.idUsuario : null,
  contactoExteriorNombre: '',
  contactoExteriorTelefono: '',
  contactoExteriorCorreo: '',
  fechaZarpeEtd: '',
  fechaLlegadaEta: '',
  numeroContenedor: '',
  precintoAduanero: '',
  naveViaje: '',
  observaciones: ''
})

const form = ref(defaultForm())

// Cotización activa seleccionada
const activeCotizacion = computed(() => {
  if (selectedCotizacionId.value) {
    const found = cotizacionesDisponibles.value.find(
      (c) => c.idCotizacion === selectedCotizacionId.value
    )
    if (found) return found
    if (props.cotizacionPreseleccionada && props.cotizacionPreseleccionada.idCotizacion === selectedCotizacionId.value) {
      return props.cotizacionPreseleccionada
    }
  }
  return null
})

const formatRegimen = (reg) => {
  if (!reg) return 'IMPORTACIÓN DEFINITIVA'
  const labels = {
    IMPORTACION_DEFINITIVA: 'Importación Definitiva (10)',
    EXPORTACION_DEFINITIVA: 'Exportación Definitiva (40)',
    DEPOSITO_ADUANERO: 'Depósito Aduanero (70)',
    TRANSITO_ADUANERO: 'Tránsito Aduanero (80)',
    ADMISION_TEMPORAL: 'Admisión Temporal (20)'
  }
  return labels[reg] || reg
}

const selectCotizacionParaApertura = (cotizacion) => {
  if (!cotizacion) return
  selectedCotizacionId.value = cotizacion.idCotizacion
  form.value.idCotizacion = cotizacion.idCotizacion
}

const clearSelection = () => {
  selectedCotizacionId.value = null
  form.value.idCotizacion = null
  emit('clear-preselection')
}

const loadAgentes = async () => {
  try {
    const agentes = await userService.getAgentesAduaneros()
    agentesList.value = agentes || []

    if (isCurrentUserAgente.value && authStore.user?.idUsuario) {
      form.value.idAgenteAduanero = authStore.user.idUsuario
    } else if (agentesList.value.length > 0 && !form.value.idAgenteAduanero) {
      form.value.idAgenteAduanero = agentesList.value[0].idUsuario
    }
  } catch (e) {
    console.error('Error cargando agentes aduaneros:', e)
  }
}

const loadInitialData = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const [cots] = await Promise.all([
      gestionAduaneraService.getCotizaciones(),
      loadAgentes()
    ])

    // Filtrar cotizaciones listas: Aprobadas, sin expediente, con pago validado
    const cotsList = cots || []
    cotizacionesDisponibles.value = cotsList.filter(
      (c) => c.estado === 'APROBADA' && !c.tieneExpediente && c.tienePagoAprobado
    )

    if (props.cotizacionPreseleccionada) {
      selectedCotizacionId.value = props.cotizacionPreseleccionada.idCotizacion
      form.value.idCotizacion = props.cotizacionPreseleccionada.idCotizacion
    } else {
      selectedCotizacionId.value = null
      form.value.idCotizacion = null
    }
  } catch (err) {
    console.error('Error cargando cotizaciones para apertura:', err)
    errorMessage.value = 'No se pudieron cargar las cotizaciones disponibles desde el servidor.'
  } finally {
    loading.value = false
  }
}

watch(
  () => props.cotizacionPreseleccionada,
  (newVal) => {
    if (newVal) {
      selectedCotizacionId.value = newVal.idCotizacion
      form.value.idCotizacion = newVal.idCotizacion
    } else {
      selectedCotizacionId.value = null
      form.value.idCotizacion = null
    }
  },
  { immediate: true }
)

onMounted(() => {
  loadInitialData()
})

const handleSubmit = async () => {
  errorMessage.value = ''
  const cotId = form.value.idCotizacion || activeCotizacion.value?.idCotizacion
  if (!cotId || !form.value.idAgenteAduanero) {
    errorMessage.value = 'Debe seleccionar una cotización válida y el agente aduanero responsable.'
    return
  }

  loading.value = true
  try {
    const payload = {
      idCotizacion: cotId,
      idAgenteAduanero: form.value.idAgenteAduanero,
      contactoExteriorNombre: form.value.contactoExteriorNombre || null,
      contactoExteriorTelefono: form.value.contactoExteriorTelefono || null,
      contactoExteriorCorreo: form.value.contactoExteriorCorreo || null,
      fechaZarpeEtd: form.value.fechaZarpeEtd || null,
      fechaLlegadaEta: form.value.fechaLlegadaEta || null,
      numeroContenedor: form.value.numeroContenedor || null,
      precintoAduanero: form.value.precintoAduanero || null,
      naveViaje: form.value.naveViaje || null,
      observaciones: form.value.observaciones || null
    }

    const creado = await expedienteService.createExpediente(payload)
    emit('created', creado)
  } catch (err) {
    console.error('Error formalizando expediente aduanero:', err)
    errorMessage.value = err.response?.data?.message || 'Error al formalizar el expediente aduanero.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.apertura-expediente-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding-bottom: 3rem;
}

/* Header Bar */
.apertura-header-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.5rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.5rem 1.75rem;
  box-shadow: var(--shadow-subtle);
}

.kicker-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
}

.badge-kicker {
  font-size: 0.6875rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker, #8a4b26);
  background: var(--color-bg-subtle, #f5f2eb);
  padding: 0.25rem 0.625rem;
  border-radius: var(--radius-sm, 4px);
  text-transform: uppercase;
}

.flow-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.6875rem;
  font-weight: 700;
  color: #15803d;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
}

.header-main-title {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-text-main);
  letter-spacing: -0.02em;
  margin: 0 0 0.5rem 0;
}

.header-desc {
  font-size: 0.875rem;
  color: var(--color-text-subtle);
  line-height: 1.5;
  margin: 0;
  max-width: 850px;
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  border-radius: var(--radius-sm, 4px);
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-outline-warm:hover:not(:disabled) {
  border-color: var(--color-accent-copper, #c27847);
  color: var(--color-accent-copper, #c27847);
}

.error-banner {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #b91c1c;
  padding: 0.875rem 1.25rem;
  border-radius: var(--radius-md, 8px);
  font-size: 0.875rem;
  font-weight: 600;
}

/* ============================================================== */
/* ESTADO A: COLA DE COTIZACIONES LISTAS                         */
/* ============================================================== */
.apertura-queue-container {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.queue-header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--color-border);
}

.queue-title {
  margin: 0.25rem 0 0.25rem 0;
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--color-text-main);
}

.queue-subtitle {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--color-text-subtle);
}

.badge-counter {
  font-size: 0.75rem;
  font-weight: 800;
  background: rgba(138, 75, 38, 0.1);
  color: var(--color-accent-kicker, #8a4b26);
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  white-space: nowrap;
}

.empty-cotizaciones-alert {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 2rem;
  background: var(--color-bg-surface);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-main);
}

.empty-cotizaciones-alert .alert-icon {
  font-size: 1.75rem;
}

.queue-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 1.25rem;
}

.cotizacion-card-box {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: var(--shadow-sm);
  transition: all 0.2s ease;
}

.cotizacion-card-box:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--color-accent-copper, #c27847);
}

.card-box-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

.doc-code {
  font-size: 0.9375rem;
  font-weight: 800;
  color: var(--color-accent-copper, #c27847);
}

.regimen-tag {
  font-size: 0.6875rem;
  font-weight: 700;
  background: var(--color-bg-subtle);
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  color: var(--color-text-subtle);
}

.card-box-body {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
}

.client-info {
  display: flex;
  flex-direction: column;
}

.client-name {
  font-size: 0.9375rem;
  color: var(--color-text-main);
  line-height: 1.3;
}

.client-doc {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.ports-flow-box {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.port-chip {
  background: var(--color-bg-subtle);
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-weight: 600;
}

.financial-chips-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-bg-subtle);
  padding: 0.5rem 0.75rem;
  border-radius: var(--radius-sm);
  margin-top: 0.25rem;
}

.fin-col {
  display: flex;
  flex-direction: column;
}

.fin-label {
  font-size: 0.625rem;
  font-weight: 800;
  color: var(--color-text-muted);
}

.fin-val {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.fin-val.text-success {
  color: #16a34a !important;
}

.btn-formalizar-card {
  width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.65rem 1rem;
  background: var(--color-accent-kicker, #8a4b26);
  color: #ffffff;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 0.8125rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-formalizar-card:hover {
  background: #6d3a1d;
}

/* ============================================================== */
/* ESTADO B: ASISTENTE DE FORMALIZACIÓN                          */
/* ============================================================== */
.form-back-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  padding: 0.75rem 1.25rem;
  border-radius: var(--radius-md);
}

.btn-back-link {
  background: none;
  border: none;
  color: var(--color-accent-kicker, #8a4b26);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.25rem 0;
}

.btn-back-link:hover {
  text-decoration: underline;
}

.active-badge {
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-accent-copper, #c27847);
}

.apertura-form-layout {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  margin-top: 1rem;
}

.section-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  box-shadow: var(--shadow-subtle);
  overflow: hidden;
}

.card-section-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.25rem 1.5rem;
  background: var(--color-bg-subtle, #faf7f2);
  border-bottom: 1px solid var(--color-border);
}

.section-idx {
  font-family: monospace;
  font-size: 1rem;
  font-weight: 800;
  color: var(--color-accent-kicker, #8a4b26);
  background: #ffffff;
  border: 1px solid var(--color-border);
  width: 2.25rem;
  height: 2.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
}

.section-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
}

.section-sub {
  font-size: 0.8125rem;
  color: var(--color-text-subtle);
  margin: 0.15rem 0 0 0;
}

.card-section-body {
  padding: 1.5rem;
}

.cotizacion-summary-panel {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1.25rem;
  background: var(--color-bg-subtle, #f9f8f5);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  padding: 1.25rem;
}

.summary-col {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.summary-label {
  font-size: 0.6875rem;
  font-weight: 800;
  color: var(--color-text-muted);
  letter-spacing: 0.05em;
}

.summary-val {
  font-size: 0.9375rem;
  color: var(--color-text-main);
}

.summary-sub {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.regimen-badge {
  display: inline-block;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-accent-kicker, #8a4b26);
  background: rgba(138, 75, 38, 0.1);
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
}

.ports-flow {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.75rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.25rem;
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.field-item.full-width {
  grid-column: 1 / -1;
}

.field-item label {
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.field-hint {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
}

.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.input-with-icon .field-icon {
  position: absolute;
  left: 0.875rem;
  color: var(--color-text-muted);
  pointer-events: none;
}

.select-input,
.text-input,
.textarea-input {
  width: 100%;
  padding: 0.65rem 0.875rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 4px);
  background: #ffffff;
  color: var(--color-text-main);
  font-size: 0.875rem;
  transition: all 0.2s ease;
}

.input-with-icon select {
  padding-left: 2.5rem;
}

.select-input:focus,
.text-input:focus,
.textarea-input:focus {
  outline: none;
  border-color: var(--color-accent-copper, #c27847);
  box-shadow: 0 0 0 3px rgba(194, 120, 71, 0.15);
}

.font-mono {
  font-family: monospace;
}

.uppercase {
  text-transform: uppercase;
}

.form-actions-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  box-shadow: var(--shadow-subtle);
}

.required-note {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.actions-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.btn-espresso {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1.25rem;
  background: var(--color-accent-kicker, #8a4b26);
  color: #ffffff;
  border: none;
  border-radius: var(--radius-sm, 4px);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-espresso:hover:not(:disabled) {
  background: #6d3a1d;
}

.btn-espresso:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
