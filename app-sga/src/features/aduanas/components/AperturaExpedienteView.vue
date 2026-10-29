<template>
  <div class="apertura-expediente-view fade-in">
    <!-- Header Bar de Apertura Formal -->
    <div class="apertura-header-bar">
      <div class="header-titles">
        <div class="kicker-row">
          <span class="badge-kicker">CONTROL ADUANERO // FORMALIZACIÓN OPERATIVA</span>
          <span class="flow-pill">
            <i class="pi pi-check-circle"></i>
            <span>FASE 02: APERTURA DE EXPEDIENTE</span>
          </span>
        </div>
        <h1 class="header-main-title">Apertura y Formalización de Expediente Aduanero</h1>
        <p class="header-desc">
          Formalice las propuestas comerciales aprobadas con confirmación de abono financiero, asignando al agente especialista e iniciando la numeración y diligencia ante SUNAT.
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

    <form @submit.prevent="handleSubmit" class="apertura-form-layout">
      <!-- 1. Bloque: Selección de Cotización Origen -->
      <section class="section-card">
        <div class="card-section-header">
          <div class="section-idx">01</div>
          <div>
            <h3 class="section-title">Cotización Comercial Aprobada (Origen)</h3>
            <p class="section-sub">Seleccione la cotización con pago verificado en Finanzas para aperturar su expediente.</p>
          </div>
          <span class="badge-counter" v-if="cotizacionesDisponibles.length > 0">
            {{ cotizacionesDisponibles.length }} listas para expediente
          </span>
        </div>

        <div class="card-section-body">
          <div v-if="cotizacionesDisponibles.length === 0" class="empty-cotizaciones-alert">
            <i class="pi pi-info-circle alert-icon"></i>
            <div>
              <strong>No hay cotizaciones pendientes de apertura con pago aprobado</strong>
              <p>
                Para aperturar un expediente formal, la cotización debe estar en estado <em>APROBADA</em> y contar con al menos un abono validado por el área de Finanzas.
              </p>
            </div>
          </div>

          <div v-else class="picker-container">
            <div class="picker-select-row">
              <label for="cot-select" class="picker-label">
                <i class="pi pi-file-edit"></i>
                <span>Seleccionar Propuesta Comercial:</span>
              </label>
              <select
                id="cot-select"
                v-model="selectedCotizacionId"
                class="select-input select-highlight"
                required
                @change="onCotizacionSelect"
              >
                <option :value="null">-- Seleccione una cotización comercial aprobada --</option>
                <option
                  v-for="c in cotizacionesDisponibles"
                  :key="c.idCotizacion"
                  :value="c.idCotizacion"
                >
                  {{ c.codigoCotizacion }} — {{ c.clienteRazonSocial || 'Cliente s/n' }} | ${{ Number(c.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}
                </option>
              </select>
            </div>

            <!-- Ficha Resumen de la Cotización Activa -->
            <div v-if="activeCotizacion" class="cotizacion-summary-panel">
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
                <option :value="authStore.user?.idUsuario || 1">
                  {{ authStore.userFullName || 'Agente Aduanero Actual' }} ({{ authStore.user?.username || 'Usuario Actual' }})
                </option>
                <option v-for="u in agentesList" :key="u.idUsuario" :value="u.idUsuario">
                  {{ u.nombres }} {{ u.apellidos }} ({{ u.username }})
                </option>
              </select>
            </div>
            <span class="field-hint">Especialista que gestionará la numeración de DAM, aforo e inspecciones.</span>
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
            <p class="section-sub">Fechas estimadas de zarpe/arribo y datos de contenedor o nave si están disponibles.</p>
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
            <label for="exp-contenedor">N° Contenedor (Si aplica)</label>
            <input
              id="exp-contenedor"
              v-model.trim="form.numeroContenedor"
              type="text"
              placeholder="Ej. MSCU-928472-1"
              class="text-input font-mono uppercase"
              maxlength="30"
            />
          </div>

          <div class="field-item">
            <label for="exp-precinto">N° Precinto Aduanero / Botella</label>
            <input
              id="exp-precinto"
              v-model.trim="form.precintoAduanero"
              type="text"
              placeholder="Ej. ML-PE-992147"
              class="text-input font-mono uppercase"
              maxlength="50"
            />
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
          <span class="required-note">* Campos obligatorios para la formalización del expediente.</span>
        </div>
        <div class="actions-right">
          <button
            type="button"
            class="btn-outline-warm"
            @click="resetForm"
            :disabled="loading"
          >
            <span>Limpiar Formulario</span>
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
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { expedienteService } from '../services/expedienteService'
import { gestionAduaneraService } from '@/features/aduanas/services/gestionAduaneraService'
import { userService } from '@/features/admin/users/services/userService'
import { useAuthStore } from '@/features/auth/stores/authStore'

const props = defineProps({
  cotizacionPreseleccionada: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['created', 'cancel'])

const authStore = useAuthStore()

const loading = ref(false)
const errorMessage = ref('')
const agentesList = ref([])
const cotizacionesDisponibles = ref([])
const selectedCotizacionId = ref(null)

const defaultForm = () => ({
  idCotizacion: null,
  idAgenteAduanero: authStore.user?.idUsuario || 1,
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

const activeCotizacion = computed(() => {
  if (props.cotizacionPreseleccionada) return props.cotizacionPreseleccionada
  if (selectedCotizacionId.value) {
    return cotizacionesDisponibles.value.find(
      (c) => c.idCotizacion === selectedCotizacionId.value
    ) || null
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

const onCotizacionSelect = () => {
  if (selectedCotizacionId.value) {
    form.value.idCotizacion = selectedCotizacionId.value
  } else {
    form.value.idCotizacion = null
  }
}

const resetForm = () => {
  form.value = defaultForm()
  if (!props.cotizacionPreseleccionada && cotizacionesDisponibles.value.length > 0) {
    selectedCotizacionId.value = cotizacionesDisponibles.value[0].idCotizacion
    form.value.idCotizacion = selectedCotizacionId.value
  } else if (!props.cotizacionPreseleccionada) {
    selectedCotizacionId.value = null
  }
  errorMessage.value = ''
}

const loadAgentes = async () => {
  try {
    const users = await userService.getUsers(true)
    agentesList.value = (users || []).filter(
      (u) => u.idUsuario !== authStore.user?.idUsuario
    )
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
    } else if (cotizacionesDisponibles.value.length > 0) {
      selectedCotizacionId.value = cotizacionesDisponibles.value[0].idCotizacion
      form.value.idCotizacion = cotizacionesDisponibles.value[0].idCotizacion
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
  color: var(--color-accent-kicker, #9a7f60);
  background: var(--color-bg-subtle, #f5f2eb);
  padding: 0.25rem 0.625rem;
  border-radius: var(--radius-sm, 4px);
}

.flow-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.6875rem;
  font-weight: 700;
  color: #15803d;
  background: #dcfce7;
  padding: 0.25rem 0.625rem;
  border-radius: var(--radius-sm, 4px);
}

.header-main-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0 0 0.375rem 0;
  letter-spacing: -0.02em;
}

.header-desc {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  margin: 0;
  max-width: 850px;
  line-height: 1.5;
}

/* Error Banner */
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

/* Sections */
.apertura-form-layout {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.section-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.5rem 1.75rem;
  box-shadow: var(--shadow-subtle);
}

.card-section-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 1rem;
  margin-bottom: 1.25rem;
}

.section-idx {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  background: var(--color-accent-kicker, #9a7f60);
  color: #fff;
  font-weight: 800;
  font-size: 0.875rem;
}

.section-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
}

.section-sub {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin: 0.125rem 0 0 0;
}

.badge-counter {
  margin-left: auto;
  font-size: 0.75rem;
  font-weight: 700;
  color: #1e40af;
  background: #dbeafe;
  padding: 0.25rem 0.75rem;
  border-radius: var(--radius-full, 9999px);
}

/* Alert */
.empty-cotizaciones-alert {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  background: var(--color-warning-bg, #fdf5e6);
  border: 1px solid var(--color-warning-border, #f8dab3);
  padding: 1.25rem;
  border-radius: var(--radius-md, 8px);
  color: var(--color-warning-text, #8c5b16);
}

.empty-cotizaciones-alert .alert-icon {
  font-size: 1.5rem;
  margin-top: 0.125rem;
}

.empty-cotizaciones-alert p {
  margin: 0.25rem 0 0 0;
  font-size: 0.8125rem;
  color: inherit;
  line-height: 1.4;
}

/* Picker */
.picker-container {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.picker-select-row {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.picker-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.select-highlight {
  font-size: 0.9375rem;
  font-weight: 600;
  border-color: var(--color-accent-kicker);
}

/* Cotización Summary Panel */
.cotizacion-summary-panel {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.25rem;
  background: var(--color-bg-subtle, #f5f2eb);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  padding: 1.25rem 1.5rem;
}

.summary-col {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.summary-label {
  font-size: 0.6875rem;
  font-weight: 800;
  color: var(--color-text-subtle);
  letter-spacing: 0.05em;
}

.summary-val {
  font-size: 0.9375rem;
  color: var(--color-text-main);
}

.summary-sub {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.text-accent {
  color: var(--color-accent-copper, #c2410c);
}

.text-success {
  color: #15803d;
}

.regimen-badge {
  display: inline-block;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-accent-copper);
  background: #fff;
  border: 1px solid var(--color-border);
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
}

.ports-flow {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  color: var(--color-text-main);
}

.port-chip {
  background: #fff;
  border: 1px solid var(--color-border);
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-weight: 600;
}

/* Form Grids */
.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1.25rem;
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
  font-size: 0.75rem;
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

.input-with-icon .select-input {
  padding-left: 2.25rem;
}

.select-input,
.text-input {
  width: 100%;
  height: 2.5rem;
  padding: 0 0.875rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  font-family: inherit;
  font-size: 0.875rem;
  transition: border-color 0.15s ease;
}

.select-input:focus,
.text-input:focus,
.textarea-input:focus {
  outline: none;
  border-color: var(--color-accent-kicker);
}

.textarea-input {
  width: 100%;
  padding: 0.625rem 0.875rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  font-family: inherit;
  font-size: 0.875rem;
  resize: vertical;
}

.uppercase {
  text-transform: uppercase;
}

/* Actions Bar */
.form-actions-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.25rem 1.75rem;
  box-shadow: var(--shadow-subtle);
}

.required-note {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  font-style: italic;
}

.actions-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

/* Botones SGA */
.btn-espresso {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: var(--color-primary-espresso, #2b231d);
  color: #fff;
  border: 1px solid var(--color-primary-espresso, #2b231d);
  padding: 0.625rem 1.25rem;
  border-radius: var(--radius-md, 8px);
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-espresso:hover:not(:disabled) {
  background: var(--color-accent-copper, #c2410c);
  border-color: var(--color-accent-copper, #c2410c);
}

.btn-espresso:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: transparent;
  color: var(--color-text-main);
  border: 1px solid var(--color-border);
  padding: 0.625rem 1.25rem;
  border-radius: var(--radius-md, 8px);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-outline-warm:hover:not(:disabled) {
  border-color: var(--color-accent-kicker);
  color: var(--color-accent-kicker);
}

.fade-in {
  animation: fadeIn 0.2s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
