<template>
  <div class="legal-view">
    <!-- Hero Banner Card Editorial -->
    <HeroBannerCard
      kicker="LEGAL Y AUDITORÍA // ASUNTOS CONTENCIOSO-TRIBUTARIOS & SUNAT"
      title="Legal y Auditoría - Recursos de Reclamación y Dictámenes"
      description="Patrocinio y defensa jurídica en despachos aduaneros observados, contingencias de duda razonable de valor, inspecciones de Canal Rojo y apelaciones ante el Tribunal Fiscal."
    />

    <!-- Pestañas de Navegación -->
    <div class="module-tabs-nav">
      <button
        type="button"
        class="tab-nav-btn"
        :class="{ active: activeTab === 'observados' }"
        @click="switchTab('observados')"
      >
        <i class="pi pi-exclamation-circle"></i>
        <span>Expedientes en Canal Rojo & Observados</span>
        <span class="tab-counter-danger">{{ expedientesRojosList.length }}</span>
      </button>

      <button
        type="button"
        class="tab-nav-btn"
        :class="{ active: activeTab === 'recursos' }"
        @click="switchTab('recursos')"
      >
        <i class="pi pi-briefcase"></i>
        <span>Recursos de Reclamación & Dictámenes</span>
        <span class="tab-counter">{{ recursosList.length }}</span>
      </button>

      <button
        type="button"
        class="tab-nav-btn"
        :class="{ active: activeTab === 'normativa' }"
        @click="switchTab('normativa')"
      >
        <i class="pi pi-book"></i>
        <span>Compendio Normativo & Plazos MTC/SUNAT</span>
      </button>
    </div>

    <!-- ===================================================================== -->
    <!-- TAB 1: EXPEDIENTES OBSERVADOS Y CANAL ROJO                            -->
    <!-- ===================================================================== -->
    <div v-if="activeTab === 'observados'" class="tab-container">
      <!-- Métricas Legales -->
      <div class="metrics-grid">
        <MetricCard index="01" label="En Canal Rojo (Físico)" :value="expedientesRojosList.length" severity="danger" highlight />
        <MetricCard index="02" label="Duda Razonable de Valor" :value="dudaRazonableCount" severity="warning" />
        <MetricCard index="03" label="Dictámenes Redactados" :value="recursosList.length" severity="success" />
        <MetricCard index="04" label="Total en Fiscalización" :value="expedientesTotalAuditoria" severity="accent" />
      </div>

      <!-- Tabla de Expedientes en Contingencia -->
      <div class="content-table-card">
        <div class="card-header-bar">
          <div>
            <span class="badge-kicker">INCIDENCIAS TRIBUTARIO-ADUANERAS</span>
            <h3 class="section-title">Bandeja de Expedientes con Inspección o Ajuste de Valor</h3>
          </div>

          <div class="header-actions">
            <div class="search-input-wrap">
              <i class="pi pi-search search-icon"></i>
              <input
                v-model="searchQuery"
                type="text"
                class="search-input"
                placeholder="Buscar por expediente, DAM o cliente..."
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
                <i class="pi pi-shield empty-icon text-success"></i>
                <p class="empty-title">Sin contingencias jurídicas activas</p>
                <p class="empty-desc">Todos los expedientes aduaneros se encuentran en curso regular sin incidencias en canal rojo.</p>
              </div>
            </template>

            <Column header="Expediente / DAM">
              <template #body="{ data: exp }">
                <div class="code-cell">
                  <span class="font-mono font-bold">{{ exp.codigoExpediente }}</span>
                  <span v-if="exp.numeroDam" class="text-xs font-mono text-accent">DAM: {{ exp.numeroDam }}</span>
                  <span v-else class="text-xs text-muted">Sin numerar</span>
                </div>
              </template>
            </Column>

            <Column header="Cliente / RUC">
              <template #body="{ data: exp }">
                <div class="client-cell">
                  <span class="client-name">{{ exp.clienteRazonSocial }}</span>
                  <span class="client-sub font-mono">RUC: {{ exp.clienteNumeroDocumento }}</span>
                </div>
              </template>
            </Column>

            <Column header="Canal SUNAT">
              <template #body="{ data: exp }">
                <span :class="['canal-tag', getCanalClass(exp.canalSunat)]">
                  CANAL {{ exp.canalSunat }}
                </span>
              </template>
            </Column>

            <Column header="Detalle de Inspección / Notificación">
              <template #body="{ data: exp }">
                <span class="text-xs font-semibold text-danger">
                  {{ exp.detalleInspeccion || exp.observaciones || 'Reconocimiento físico de bultos programado en depósito temporal.' }}
                </span>
              </template>
            </Column>

            <Column header="Fecha Diligencia">
              <template #body="{ data: exp }">
                <span class="font-mono text-xs">{{ formatDate(exp.fechaAsignacionCanal || exp.fechaApertura) }}</span>
              </template>
            </Column>

            <Column header="Acciones Jurídicas" headerStyle="width: 140px; text-align: right;" bodyStyle="text-align: right;">
              <template #body="{ data: exp }">
                <div class="action-buttons-cell">
                  <button
                    type="button"
                    class="btn-row-action"
                    title="Formular Dictamen / Recurso"
                    @click="openNuevoRecursoModal(exp)"
                  >
                    <i class="pi pi-file-edit"></i>
                  </button>

                  <button
                    type="button"
                    class="btn-row-action"
                    title="Ver Expediente Digital"
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
    <!-- TAB 2: RECURSOS DE RECLAMACIÓN Y DICTÁMENES FORMULADOS                 -->
    <!-- ===================================================================== -->
    <div v-if="activeTab === 'recursos'" class="tab-container">
      <div class="content-table-card">
        <div class="card-header-bar">
          <div>
            <span class="badge-kicker">DEFENSA LEGAL & PROCEDIMIENTOS</span>
            <h3 class="section-title">Escritos, Impugnaciones y Dictámenes Legales</h3>
          </div>

          <button type="button" class="btn-primary-add" @click="openNuevoRecursoModal(null)">
            <i class="pi pi-plus"></i>
            <span>+ Redactar Dictamen Legal</span>
          </button>
        </div>

        <div class="table-container">
          <DataTable
            :value="recursosList"
            responsiveLayout="scroll"
            class="custom-datatable"
            dataKey="idRecurso"
          >
            <template #empty>
              <div class="empty-state">
                <i class="pi pi-briefcase empty-icon"></i>
                <p class="empty-title">No hay recursos formulados</p>
                <p class="empty-desc">Redacte un recurso de reclamación o apelación frente a observaciones aduaneras.</p>
              </div>
            </template>

            <Column header="N° Expediente Legal">
              <template #body="{ data: r }">
                <span class="font-mono font-bold">{{ r.numeroExpedienteLegal }}</span>
              </template>
            </Column>

            <Column header="Tipo de Recurso">
              <template #body="{ data: r }">
                <Tag :severity="getTipoRecursoSeverity(r.tipo)" :value="r.tipo" rounded class="tag-xs" />
              </template>
            </Column>

            <Column header="Expediente SGA">
              <template #body="{ data: r }">
                <span class="font-mono font-bold text-accent">{{ r.codigoExpediente }}</span>
              </template>
            </Column>

            <Column header="Materia Contenciosa">
              <template #body="{ data: r }">
                <div class="client-cell">
                  <span class="text-xs font-semibold">{{ r.materia }}</span>
                  <span class="client-sub">{{ r.autoridad }}</span>
                </div>
              </template>
            </Column>

            <Column header="Plazo Límite">
              <template #body="{ data: r }">
                <span class="font-mono text-xs text-danger font-bold">{{ formatDate(r.fechaVencimientoPlazo) }}</span>
              </template>
            </Column>

            <Column header="Estado">
              <template #body="{ data: r }">
                <Tag :severity="r.estado === 'ADMITIDO' ? 'success' : 'warning'" :value="r.estado" rounded class="tag-xs" />
              </template>
            </Column>

            <Column header="Documento" headerStyle="width: 100px; text-align: center;" bodyStyle="text-align: center;">
              <template #body="{ data: r }">
                <button
                  type="button"
                  class="btn-row-action"
                  title="Ver Escrito y Anexos"
                  @click="openRecursoDocs(r)"
                >
                  <i class="pi pi-file-pdf"></i>
                </button>
              </template>
            </Column>
          </DataTable>
        </div>
      </div>
    </div>

    <!-- ===================================================================== -->
    <!-- TAB 3: COMPENDIO NORMATIVO Y PLAZOS PERENTORIOS                       -->
    <!-- ===================================================================== -->
    <div v-if="activeTab === 'normativa'" class="tab-container">
      <div class="normativa-grid">
        <div class="normativa-card">
          <div class="normativa-header">
            <i class="pi pi-clock text-warning"></i>
            <h4 class="normativa-title">Plazos Perentorios de Impugnación (D.L. 1053)</h4>
          </div>
          <ul class="normativa-list">
            <li><strong>Recurso de Reclamación:</strong> 20 días hábiles contados a partir del día siguiente de notificada la Resolución de Determinación o Multa.</li>
            <li><strong>Duda Razonable de Valor:</strong> 5 días hábiles (prorrogables por 10 días) para presentar sustento de precios de transferencia y facturación internacional.</li>
            <li><strong>Recurso de Apelación (Tribunal Fiscal):</strong> 15 días hábiles computados desde la notificación del pronunciamiento denegatorio de SUNAT.</li>
            <li><strong>Queja ante el Tribunal Fiscal:</strong> Interponible en cualquier estado del procedimiento en caso de defectos de tramitación o vulneración del debido proceso.</li>
          </ul>
        </div>

        <div class="normativa-card">
          <div class="normativa-header">
            <i class="pi pi-shield text-accent"></i>
            <h4 class="normativa-title">Tabla de Infracciones y Sanciones Aduaneras</h4>
          </div>
          <ul class="normativa-list">
            <li><strong>Infracción Art. 192 inc. a):</strong> Declarar datos incorrectos en la DAM que incidan en la determinación de tributos (Sanción: Multa equivalente al doble de los tributos dejados de pagar).</li>
            <li><strong>Infracción Art. 192 inc. c):</strong> No presentar la documentación complementaria requerida en Canal Rojo/Naranja dentro del plazo legal (Sanción: 0.1 UIT).</li>
            <li><strong>Régimen de Gradualidad (R.S. 021-2020):</strong> Rebaja del 95% si se subsana voluntariamente antes de cualquier requerimiento de SUNAT.</li>
          </ul>
        </div>
      </div>
    </div>

    <!-- MODAL: REDACTAR DICTAMEN / RECURSO LEGAL -->
    <Dialog
      v-model:visible="showModalRecurso"
      modal
      header="FORMULACIÓN DE RECURSO JURÍDICO // ASESORÍA LEGAL"
      :style="{ width: '640px' }"
      class="custom-modal-editorial"
    >
      <form @submit.prevent="guardarRecurso" class="modal-form">
        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label">Tipo de Acción Legal *</label>
            <select v-model="formRecurso.tipo" class="form-select font-bold" required>
              <option value="RECLAMACION">Recurso de Reclamación (SUNAT)</option>
              <option value="APELACION">Recurso de Apelación (Tribunal Fiscal)</option>
              <option value="DUDA_RAZONABLE">Sustento de Duda Razonable de Valor</option>
              <option value="DICTAMEN_PREVENTIVO">Dictamen Jurídico Preventivo</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">Autoridad Destinataria *</label>
            <input
              v-model="formRecurso.autoridad"
              type="text"
              class="form-input"
              placeholder="Ej: Intendencia de Aduana Marítima del Callao"
              required
            />
          </div>
        </div>

        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label">Expediente SGA Asociado *</label>
            <select v-model="formRecurso.codigoExpediente" class="form-select font-mono" required>
              <option value="">Seleccione Expediente...</option>
              <option v-for="exp in expedientesList" :key="exp.idExpediente" :value="exp.codigoExpediente">
                {{ exp.codigoExpediente }} - {{ exp.clienteRazonSocial }}
              </option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">Fecha Límite de Presentación *</label>
            <input
              v-model="formRecurso.fechaVencimientoPlazo"
              type="date"
              class="form-input font-mono"
              required
            />
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">Materia Jurídica Contenciosa *</label>
          <input
            v-model="formRecurso.materia"
            type="text"
            class="form-input"
            placeholder="Ej: Impugnación de Liquidación de Oficio por Valoración CIF"
            required
          />
        </div>

        <div class="form-group">
          <label class="form-label">Fundamentos de Hecho y de Derecho</label>
          <textarea
            v-model="formRecurso.fundamentos"
            class="form-textarea"
            rows="4"
            placeholder="Argumentación técnico-jurídica, aplicación del Acuerdo del Valor OMC, análisis de factura comercial y medios probatorios..."
            required
          ></textarea>
        </div>

        <div class="modal-footer-actions">
          <button type="button" class="btn-cancel" @click="showModalRecurso = false">
            Cancelar
          </button>
          <button type="submit" class="btn-save">
            <i class="pi pi-check"></i>
            <span>Emitir Dictamen y Radicar</span>
          </button>
        </div>
      </form>
    </Dialog>

    <!-- Gestor de Documentos -->
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
import Dialog from 'primevue/dialog'
import { gestionAduaneraService } from '@/features/roles-modules/services/gestionAduaneraService'
import DocumentosManagerModal from '@/shared/components/DocumentosManagerModal.vue'
import { useNotification } from '@/shared/composables/useNotification'
import { useNavigationStore } from '@/core/stores/navigationStore'

const { showSuccess, showError, showInfo } = useNotification()
const navigationStore = useNavigationStore()

const activeTab = ref('observados')
const expedientesList = ref([])
const loadingExpedientes = ref(false)
const searchQuery = ref('')

// Gestor de Documentos
const showDocsModal = ref(false)
const selectedDocEntidadId = ref(null)
const selectedDocCodigo = ref('')
const selectedDocTitulo = ref('')

// Modal y Formulario de Recurso
const showModalRecurso = ref(false)
const formRecurso = ref({
  tipo: 'RECLAMACION',
  autoridad: 'Intendencia de Aduana Marítima del Callao',
  codigoExpediente: '',
  fechaVencimientoPlazo: '',
  materia: '',
  fundamentos: ''
})

// Lista de Recursos Simulada en Memoria (persiste en la sesión)
const recursosList = ref([
  {
    idRecurso: 1,
    numeroExpedienteLegal: 'REC-2026-0012',
    tipo: 'RECLAMACION',
    codigoExpediente: 'EXP-2026-0001',
    autoridad: 'Intendencia de Aduana Marítima del Callao',
    materia: 'Impugnación de Liquidación de Oficio por Duda Razonable de Flete',
    fechaVencimientoPlazo: '2026-11-15',
    estado: 'ADMITIDO'
  },
  {
    idRecurso: 2,
    numeroExpedienteLegal: 'REC-2026-0015',
    tipo: 'DUDA_RAZONABLE',
    codigoExpediente: 'EXP-2026-0002',
    autoridad: 'División de Gestión del Despacho Aduanero',
    materia: 'Sustento de Valor de Transacción y Transferencias Bancarias',
    fechaVencimientoPlazo: '2026-11-08',
    estado: 'EN_TRAMITE'
  }
])

// Expedientes en Canal Rojo o con observaciones
const expedientesRojosList = computed(() => {
  return expedientesList.value.filter(e => e.canalSunat === 'ROJO' || e.detalleInspeccion)
})

const dudaRazonableCount = computed(() => 2)
const expedientesTotalAuditoria = computed(() => expedientesRojosList.value.length + recursosList.value.length)

const filteredExpedientes = computed(() => {
  let list = expedientesRojosList.value
  // Si no hay rojos registrados, mostrar todos los expedientes que tengan canal o inspección
  if (list.length === 0) {
    list = expedientesList.value.filter(e => e.canalSunat === 'NARANJA' || e.canalSunat === 'ROJO' || e.canalSunat === 'VERDE')
  }

  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase().trim()
    list = list.filter(e => {
      const cod = (e.codigoExpediente || '').toLowerCase()
      const dam = (e.numeroDam || '').toLowerCase()
      const cli = (e.clienteRazonSocial || '').toLowerCase()
      return cod.includes(q) || dam.includes(q) || cli.includes(q)
    })
  }
  return list
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
  navigationStore.setLegalAuditoriaSubOption(tab === 'observados' ? 'expedientes' : tab === 'recursos' ? 'auditorias' : 'expedientes')
}

const openNuevoRecursoModal = (exp) => {
  const hoy = new Date()
  const plazo = new Date()
  plazo.setDate(hoy.getDate() + 20)

  formRecurso.value = {
    tipo: 'RECLAMACION',
    autoridad: 'Intendencia de Aduana Marítima del Callao',
    codigoExpediente: exp ? exp.codigoExpediente : (expedientesList.value[0]?.codigoExpediente || ''),
    fechaVencimientoPlazo: plazo.toISOString().substring(0, 10),
    materia: exp ? `Impugnación de Canal ${exp.canalSunat} / Diligencia DAM ${exp.numeroDam || exp.codigoExpediente}` : 'Recurso de Reclamación Aduanero',
    fundamentos: 'Se formula impugnación formal acreditando el correcto valor de transacción conforme al Artículo 1 del Acuerdo de Valor de la OMC.'
  }
  showModalRecurso.value = true
}

const guardarRecurso = () => {
  const count = recursosList.value.length + 1
  const nuevo = {
    idRecurso: count + 10,
    numeroExpedienteLegal: `REC-2026-${String(count + 15).padStart(4, '0')}`,
    tipo: formRecurso.value.tipo,
    codigoExpediente: formRecurso.value.codigoExpediente,
    autoridad: formRecurso.value.autoridad,
    materia: formRecurso.value.materia,
    fechaVencimientoPlazo: formRecurso.value.fechaVencimientoPlazo,
    estado: 'EN_TRAMITE'
  }

  recursosList.value.unshift(nuevo)
  showSuccess(`Recurso jurídico ${nuevo.numeroExpedienteLegal} radicado correctamente`)
  showModalRecurso.value = false
  activeTab.value = 'recursos'
}

const openDocsModal = (exp) => {
  selectedDocEntidadId.value = exp.idExpediente
  selectedDocCodigo.value = exp.codigoExpediente
  selectedDocTitulo.value = `Expediente Jurídico: ${exp.codigoExpediente}`
  showDocsModal.value = true
}

const openRecursoDocs = (r) => {
  const exp = expedientesList.value.find(e => e.codigoExpediente === r.codigoExpediente)
  if (exp) {
    openDocsModal(exp)
  } else {
    showInfo(`Mostrando repositorio documental para recurso ${r.numeroExpedienteLegal}`)
  }
}

const formatDate = (val) => {
  if (!val) return '—'
  try {
    return new Date(val).toLocaleDateString('es-PE', { year: 'numeric', month: '2-digit', day: '2-digit' })
  } catch {
    return val
  }
}

const getCanalClass = (canal) => {
  switch (canal) {
    case 'ROJO': return 'canal-rojo'
    case 'NARANJA': return 'canal-naranja'
    case 'VERDE': return 'canal-verde'
    default: return 'canal-pendiente'
  }
}

const getTipoRecursoSeverity = (tipo) => {
  switch (tipo) {
    case 'RECLAMACION': return 'danger'
    case 'APELACION': return 'warning'
    case 'DUDA_RAZONABLE': return 'info'
    default: return 'secondary'
  }
}

watch(
  () => [navigationStore.legalAuditoriaSubOption, navigationStore.navTick],
  ([sub]) => {
    if (sub && ['expedientes', 'auditorias'].includes(sub)) {
      activeTab.value = sub === 'auditorias' ? 'recursos' : 'observados'
    }
  },
  { immediate: true }
)

onMounted(() => {
  loadExpedientes()
})
</script>

<style scoped>
.legal-view {
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
.tab-counter-danger {
  background: #faebeb;
  color: #b42318;
  border: 1px solid #e89797;
  font-size: 0.7rem;
  padding: 0.1rem 0.45rem;
  border-radius: 12px;
  font-family: var(--font-mono, monospace);
  font-weight: 700;
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

.btn-primary-add {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.45rem 0.85rem;
  background: var(--color-accent, #9a7f60);
  border: none;
  border-radius: 4px;
  color: #ffffff;
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
.canal-rojo { background: #faebeb; color: #b42318; border: 1px solid #e89797; }
.canal-naranja { background: #fdf5ea; color: #b54708; border: 1px solid #e8c697; }
.canal-verde { background: #eef8f3; color: #027a48; border: 1px solid #a6dbbe; }
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
.empty-state {
  text-align: center;
  padding: 2.5rem;
}
.empty-icon {
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

/* Normativa */
.normativa-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.25rem;
}
.normativa-card {
  background: var(--color-bg-card, #ede6db);
  border: 1px solid var(--color-border, #e2d9cd);
  border-radius: 6px;
  padding: 1.25rem;
}
.normativa-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  border-bottom: 1px solid #e2d9cd;
  padding-bottom: 0.75rem;
  margin-bottom: 0.75rem;
}
.normativa-title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--color-text-primary, #352f2a);
}
.normativa-list {
  padding-left: 1.2rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  font-size: 0.8rem;
  color: #4a423b;
  margin: 0;
}

/* Modal Form */
.modal-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
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
.form-input, .form-select, .form-textarea {
  padding: 0.45rem 0.65rem;
  border: 1px solid var(--color-border, #e2d9cd);
  background: #ffffff;
  border-radius: 4px;
  font-size: 0.825rem;
}
.modal-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  border-top: 1px solid var(--color-border, #e2d9cd);
  padding-top: 0.75rem;
}
.btn-cancel {
  padding: 0.45rem 0.85rem;
  background: transparent;
  border: 1px solid var(--color-border, #e2d9cd);
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
}
.btn-save {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.45rem 1rem;
  background: var(--color-accent, #9a7f60);
  border: none;
  border-radius: 4px;
  color: #ffffff;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
}
</style>
