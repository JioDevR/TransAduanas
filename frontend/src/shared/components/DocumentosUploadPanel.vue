<template>
  <div class="docs-upload-panel">
    <!-- Banner de Alerta si la mercancía requiere permiso sectorial -->
    <div v-if="permisoRequerido" class="regulatory-docs-alert">
      <div class="alert-icon-box">
        <i class="pi pi-exclamation-triangle"></i>
      </div>
      <div class="alert-content">
        <strong class="alert-title">Documento Obligatorio Requerido:</strong>
        <p class="alert-desc">
          La mercancía seleccionada requiere autorización previa de <strong>{{ entidadReguladora || 'Entidad Reguladora' }}</strong>.
          Adjunte la resolución, permiso o visto bueno en la categoría <strong>Permiso Especial</strong>.
        </p>
      </div>
    </div>

    <!-- 1. Banner Superior Editorial (Opcional según hideHeaderBanner) -->
    <div v-if="!hideHeaderBanner" class="docs-header-card">
      <div class="header-left">
        <span class="badge-kicker">GESTIÓN DOCUMENTAL // ALMACÉN DIGITAL</span>
        <div class="entity-title-row">
          <span class="entity-code font-mono">{{ codigoEntidad || 'General' }}</span>
          <span class="entity-name">{{ tituloEntidad || 'Documentos del Sistema' }}</span>
        </div>
        <span class="header-sub">
          Suba y consulte comprobantes, certificados, facturas o papeles de sustento operativo.
        </span>
      </div>
      <div class="header-right">
        <div class="doc-counter-box">
          <span class="counter-num">{{ totalDocsCount }}</span>
          <span class="counter-label">{{ mode === 'deferred' ? 'Archivos en Cola' : 'Archivos Adjuntos' }}</span>
        </div>
      </div>
    </div>

    <!-- 2. Zona de Carga de Nuevos Documentos (Dropzone) -->
    <div class="upload-section-card">
      <div class="upload-controls-row">
        <div class="control-item">
          <label class="control-label" for="doc-cat">Categoría de Documento *</label>
          <select id="doc-cat" v-model="formCategoria" class="select-input">
            <option value="GENERAL">Documento General</option>
            <option value="FACTURA">Factura Comercial (Invoice)</option>
            <option value="PACKING_LIST">Lista de Empaque (Packing List)</option>
            <option value="TRANSPORTE_BL">Conocimiento de Embarque (B/L o Guía Aérea)</option>
            <option value="CERTIFICADO">Certificado de Origen / Calidad</option>
            <option value="PERMISO_ESPECIAL">Permiso Especial (DIGESA/SENASA/SUCAMEC)</option>
            <option value="IDENTIFICACION">Identificación / RUC / Ficha Técnica</option>
          </select>
        </div>

        <div class="control-item flex-grow">
          <label class="control-label" for="doc-desc">Descripción / Instrucciones (Opcional)</label>
          <input
            id="doc-desc"
            v-model.trim="formDescripcion"
            type="text"
            class="text-input"
            placeholder="Ej. Factura comercial visada por proveedor..."
            maxlength="200"
          />
        </div>
      </div>

      <!-- Dropzone Interactivo -->
      <div
        class="dropzone-area"
        :class="{ 'is-dragging': isDragging }"
        @dragover.prevent="isDragging = true"
        @dragleave.prevent="isDragging = false"
        @drop.prevent="handleFileDrop"
        @click="triggerFileInput"
      >
        <input
          ref="fileInputRef"
          type="file"
          multiple
          class="hidden-file-input"
          @change="handleFileSelect"
        />
        <div class="dropzone-content">
          <div class="dropzone-icon">
            <i class="pi pi-cloud-upload"></i>
          </div>
          <p class="dropzone-text">
            <strong>Haga clic para seleccionar archivos</strong> o arrástrelos aquí
          </p>
          <span class="dropzone-sub">
            Soporta PDF, imágenes (PNG, JPG), hojas de cálculo (XLSX, CSV) y documentos Word (hasta 25 MB c/u)
          </span>
        </div>
      </div>

      <!-- Modo Diferido: Lista de Archivos en Cola Listos para Registrar -->
      <div v-if="mode === 'deferred' && deferredQueue.length > 0" class="queued-files-panel">
        <div class="queue-header">
          <span class="queue-title-badge">
            <i class="pi pi-paperclip"></i>
            Archivos listos para adjuntar a la cotización ({{ deferredQueue.length }}):
          </span>
          <button type="button" class="btn-clear-queue" @click="clearDeferredQueue">
            Limpiar lista
          </button>
        </div>
        <div class="queued-list">
          <div v-for="(item, idx) in deferredQueue" :key="item.tempId" class="queued-item">
            <div class="queued-item-left">
              <i :class="getFileIconClass(item.file.name)"></i>
              <div class="queued-file-details">
                <span class="queued-name font-mono" :title="item.file.name">{{ item.file.name }}</span>
                <span class="queued-meta">
                  {{ formatBytes(item.file.size) }}
                  <span v-if="item.descripcion" class="meta-note">• {{ item.descripcion }}</span>
                </span>
              </div>
            </div>
            <div class="queued-item-right">
              <Tag
                :value="getCategoriaLabel(item.categoria)"
                :severity="getCategoriaSeverity(item.categoria)"
                rounded
              />
              <button
                type="button"
                class="btn-remove-queued"
                title="Eliminar de la lista"
                @click="removeDeferredFile(idx)"
              >
                <i class="pi pi-times"></i>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Modo Directo: Lista de archivos en cola para subir a la API -->
      <div v-if="mode === 'live' && liveQueue.length > 0" class="queued-files-panel">
        <div class="queue-header">
          <span>Archivos listos para subir ({{ liveQueue.length }}):</span>
          <button type="button" class="btn-clear-queue" @click="liveQueue = []">
            Limpiar selección
          </button>
        </div>
        <div class="queued-list">
          <div v-for="(file, idx) in liveQueue" :key="idx" class="queued-item">
            <div class="queued-item-left">
              <i :class="getFileIconClass(file.name)"></i>
              <span class="queued-name font-mono">{{ file.name }}</span>
              <span class="queued-size">({{ formatBytes(file.size) }})</span>
            </div>
            <button type="button" class="btn-remove-queued" @click="liveQueue.splice(idx, 1)">
              <i class="pi pi-times"></i>
            </button>
          </div>
        </div>

        <div class="upload-action-row">
          <button
            type="button"
            class="btn-upload-submit"
            :disabled="uploading"
            @click="submitLiveUpload"
          >
            <i v-if="uploading" class="pi pi-spin pi-spinner"></i>
            <i v-else class="pi pi-upload"></i>
            <span>{{ uploading ? 'Subiendo archivos...' : `Confirmar y Subir ${liveQueue.length} archivo(s)` }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 3. Lista de Documentos Guardados en el Servidor (Solo en modo Live o si hay entidadId) -->
    <div v-if="mode === 'live' || resolvedEntidadId" class="docs-list-section">
      <div class="list-section-header">
        <h3 class="list-title">Archivos Registrados en Repositorio</h3>
        <button type="button" class="btn-refresh-docs" :disabled="loading" @click="loadDocumentos">
          <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
          <span>Actualizar</span>
        </button>
      </div>

      <!-- Estado de Carga -->
      <div v-if="loading" class="docs-loading-state">
        <i class="pi pi-spin pi-spinner"></i>
        <span>Consultando repositorio de documentos...</span>
      </div>

      <!-- Estado Vacío -->
      <div v-else-if="documentos.length === 0" class="docs-empty-state">
        <i class="pi pi-folder-open empty-icon"></i>
        <p class="empty-title">No hay documentos registrados para esta operación</p>
        <p class="empty-desc">Use la zona superior para adjuntar facturas, guías, certificados o papeles aduaneros.</p>
      </div>

      <!-- Tabla de Documentos -->
      <div v-else class="table-container">
        <DataTable
          :value="documentos"
          responsiveLayout="scroll"
          class="custom-datatable"
          dataKey="idDocumento"
        >
          <Column header="Documento / Nombre">
            <template #body="{ data: d }">
              <div class="doc-file-cell">
                <div class="file-icon-box">
                  <i :class="getFileIconClass(d.nombreOriginal)"></i>
                </div>
                <div class="file-text-col">
                  <span class="file-name font-mono" :title="d.nombreOriginal">{{ d.nombreOriginal }}</span>
                  <span class="file-meta">{{ formatBytes(d.tamanoBytes) }} • {{ d.extension?.toUpperCase() }}</span>
                </div>
              </div>
            </template>
          </Column>

          <Column header="Categoría">
            <template #body="{ data: d }">
              <Tag
                :value="getCategoriaLabel(d.categoria)"
                :severity="getCategoriaSeverity(d.categoria)"
                rounded
              />
            </template>
          </Column>

          <Column header="Descripción / Notas">
            <template #body="{ data: d }">
              <span class="doc-desc-text">{{ d.descripcion || '—' }}</span>
            </template>
          </Column>

          <Column header="Subido por">
            <template #body="{ data: d }">
              <div class="user-cell">
                <span class="user-name">{{ d.usuarioCreadorNombre || 'Usuario del Sistema' }}</span>
                <span class="date-sub">{{ formatDateTime(d.fechaCreacion) }}</span>
              </div>
            </template>
          </Column>

          <Column header="Acciones" headerClass="text-right" bodyClass="text-right">
            <template #body="{ data: d }">
              <div class="actions-wrapper">
                <!-- Botón Previsualizar -->
                <button
                  v-if="canPreview(d.formatoMime)"
                  type="button"
                  class="action-btn-primary"
                  title="Previsualizar documento"
                  @click="previewFile(d)"
                >
                  <i class="pi pi-eye"></i>
                  <span>Ver</span>
                </button>

                <!-- Botón Descargar -->
                <button
                  type="button"
                  class="action-btn-outline"
                  title="Descargar archivo a su equipo"
                  @click="downloadFile(d)"
                >
                  <i class="pi pi-download"></i>
                  <span>Bajar</span>
                </button>

                <!-- Botón Eliminar -->
                <button
                  type="button"
                  class="action-btn-outline reject"
                  title="Eliminar documento del repositorio"
                  @click="confirmDelete(d)"
                >
                  <i class="pi pi-trash"></i>
                </button>
              </div>
            </template>
          </Column>
        </DataTable>
      </div>
    </div>

    <!-- 4. Diálogo de Previsualización Embebida -->
    <Dialog
      v-model:visible="showPreviewModal"
      modal
      :header="`Vista Previa: ${previewDoc?.nombreOriginal || ''}`"
      :style="{ width: '85vw', maxWidth: '1000px', height: '85vh' }"
      class="custom-sga-dialog preview-dialog"
    >
      <div v-if="previewDoc && previewBlobUrl" class="preview-container">
        <iframe
          v-if="previewDoc.formatoMime?.includes('pdf')"
          :src="previewBlobUrl"
          class="preview-iframe"
          title="Vista Previa PDF"
        ></iframe>
        <img
          v-else-if="previewDoc.formatoMime?.includes('image')"
          :src="previewBlobUrl"
          class="preview-image"
          alt="Vista Previa de Imagen"
        />
        <div v-else class="preview-unsupported">
          <i class="pi pi-file empty-icon"></i>
          <p>Este formato no admite previsualización directa en el navegador.</p>
          <button type="button" class="btn-outline-warm" @click="downloadFile(previewDoc)">
            <i class="pi pi-download"></i>
            <span>Descargar archivo para ver</span>
          </button>
        </div>
      </div>
      <div v-else class="preview-loading">
        <i class="pi pi-spin pi-spinner"></i>
        <span>Cargando previsualización...</span>
      </div>
      <template #footer>
        <div class="dialog-actions-row">
          <button type="button" class="btn-outline-warm" @click="downloadFile(previewDoc)">
            <i class="pi pi-download"></i>
            <span>Descargar</span>
          </button>
          <button type="button" class="btn-outline-warm" @click="closePreview">
            <span>Cerrar</span>
          </button>
        </div>
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted } from 'vue'
import Dialog from 'primevue/dialog'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import { documentoService } from '@/features/documentos/services/documentoService'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'

const props = defineProps({
  mode: {
    type: String,
    default: 'live', // 'live' (directo al API) | 'deferred' (en cola en memoria)
    validator: (v) => ['live', 'deferred'].includes(v)
  },
  modelValue: {
    type: Array,
    default: () => []
  },
  entidadTipo: {
    type: String,
    default: 'COTIZACION'
  },
  idEntidad: {
    type: Number,
    default: null
  },
  entidadId: {
    type: Number,
    default: null
  },
  codigoEntidad: {
    type: String,
    default: ''
  },
  tituloEntidad: {
    type: String,
    default: ''
  },
  hideHeaderBanner: {
    type: Boolean,
    default: false
  },
  permisoRequerido: {
    type: Boolean,
    default: false
  },
  entidadReguladora: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'updated'])

const authStore = useAuthStore()
const { showSuccess, showError, showInfo } = useNotification()
const { confirmAction } = useConfirmation()

const resolvedEntidadId = computed(() => {
  return props.entidadId !== null && props.entidadId !== undefined ? props.entidadId : props.idEntidad
})

const loading = ref(false)
const uploading = ref(false)
const documentos = ref([])

// Formulario de Carga
const formCategoria = ref(props.permisoRequerido ? 'PERMISO_ESPECIAL' : 'GENERAL')
const formDescripcion = ref('')
const isDragging = ref(false)
const fileInputRef = ref(null)

// Cola en modo diferido (para cotización en creación)
const deferredQueue = ref([...props.modelValue])

// Cola en modo live (para subir en bloque al servidor)
const liveQueue = ref([])

// Previsualización
const showPreviewModal = ref(false)
const previewDoc = ref(null)
const previewBlobUrl = ref(null)

const totalDocsCount = computed(() => {
  if (props.mode === 'deferred') {
    return deferredQueue.value.length
  }
  return documentos.value.length
})

// Si cambia permisoRequerido, sugerir PERMISO_ESPECIAL
watch(
  () => props.permisoRequerido,
  (val) => {
    if (val && formCategoria.value === 'GENERAL') {
      formCategoria.value = 'PERMISO_ESPECIAL'
    }
  }
)

watch(
  () => props.modelValue,
  (newVal) => {
    if (props.mode === 'deferred') {
      deferredQueue.value = newVal ? [...newVal] : []
    }
  },
  { deep: true }
)

const triggerFileInput = () => {
  fileInputRef.value?.click()
}

const handleFileSelect = (e) => {
  const selected = Array.from(e.target.files || [])
  addFilesToQueue(selected)
  e.target.value = ''
}

const handleFileDrop = (e) => {
  isDragging.value = false
  const dropped = Array.from(e.dataTransfer.files || [])
  addFilesToQueue(dropped)
}

const addFilesToQueue = (files) => {
  const validFiles = files.filter((f) => f.size <= 25 * 1024 * 1024)
  if (validFiles.length < files.length) {
    showError('Algunos archivos superan el límite de 25 MB y fueron omitidos.')
  }

  if (props.mode === 'deferred') {
    validFiles.forEach((file) => {
      deferredQueue.value.push({
        tempId: `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
        file,
        categoria: formCategoria.value,
        descripcion: formDescripcion.value || ''
      })
    })
    formDescripcion.value = ''
    emit('update:modelValue', deferredQueue.value)
    showInfo(`${validFiles.length} archivo(s) agregado(s) a la documentación de la propuesta.`)
  } else {
    liveQueue.value.push(...validFiles)
  }
}

const removeDeferredFile = (index) => {
  deferredQueue.value.splice(index, 1)
  emit('update:modelValue', deferredQueue.value)
}

const clearDeferredQueue = () => {
  deferredQueue.value = []
  emit('update:modelValue', [])
}

const submitLiveUpload = async () => {
  if (liveQueue.value.length === 0) return

  uploading.value = true
  try {
    await documentoService.upload(liveQueue.value, {
      categoria: formCategoria.value,
      descripcion: formDescripcion.value,
      entidadTipo: props.entidadTipo,
      idEntidad: resolvedEntidadId.value,
      idUsuario: authStore.user?.idUsuario || 1
    })

    showSuccess(`¡${liveQueue.value.length} archivo(s) subido(s) con éxito!`)
    liveQueue.value = []
    formDescripcion.value = ''
    await loadDocumentos()
    emit('updated')
  } catch (err) {
    showError(err.response?.data?.message || 'Error al subir los documentos.')
  } finally {
    uploading.value = false
  }
}

const loadDocumentos = async () => {
  if (!resolvedEntidadId.value) return
  loading.value = true
  try {
    const params = {
      entidadTipo: props.entidadTipo
    }
    if (props.entidadTipo === 'COTIZACION') {
      params.idCotizacion = resolvedEntidadId.value
    } else if (props.entidadTipo === 'EXPEDIENTE') {
      params.idExpediente = resolvedEntidadId.value
    } else if (props.entidadTipo === 'CLIENTE') {
      params.idCliente = resolvedEntidadId.value
    }

    const res = await documentoService.getDocumentos(params)
    documentos.value = res || []
  } catch {
    showError('No se pudieron recuperar los documentos adjuntos.')
  } finally {
    loading.value = false
  }
}

const downloadFile = async (doc) => {
  try {
    await documentoService.downloadDocumento(doc.idDocumento, doc.nombreOriginal)
  } catch {
    showError('Error al descargar el archivo.')
  }
}

const canPreview = (mime) => {
  if (!mime) return false
  return mime.includes('pdf') || mime.includes('image')
}

const previewFile = async (doc) => {
  previewDoc.value = doc
  previewBlobUrl.value = null
  showPreviewModal.value = true

  try {
    const res = await documentoService.getPreviewBlobUrl(doc.idDocumento)
    previewBlobUrl.value = res.url
  } catch {
    showError('No se pudo generar la previsualización del documento.')
    showPreviewModal.value = false
  }
}

const closePreview = () => {
  if (previewBlobUrl.value) {
    window.URL.revokeObjectURL(previewBlobUrl.value)
    previewBlobUrl.value = null
  }
  showPreviewModal.value = false
  previewDoc.value = null
}

const confirmDelete = async (doc) => {
  const confirmed = await confirmAction(
    `¿Desea eliminar el archivo "${doc.nombreOriginal}" del repositorio?`,
    'Eliminar Documento'
  )
  if (!confirmed) return

  try {
    await documentoService.deleteDocumento(doc.idDocumento)
    showInfo(`Archivo "${doc.nombreOriginal}" eliminado.`)
    await loadDocumentos()
    emit('updated')
  } catch {
    showError('No se pudo eliminar el documento.')
  }
}

watch(
  () => resolvedEntidadId.value,
  (val) => {
    if (val && props.mode === 'live') {
      loadDocumentos()
    }
  },
  { immediate: true }
)

onMounted(() => {
  if (props.mode === 'live' && resolvedEntidadId.value) {
    loadDocumentos()
  }
})

// Helpers
const getFileIconClass = (filename) => {
  if (!filename) return 'pi pi-file'
  const ext = filename.split('.').pop()?.toLowerCase()
  switch (ext) {
    case 'pdf':
      return 'pi pi-file-pdf text-danger'
    case 'xls':
    case 'xlsx':
    case 'csv':
      return 'pi pi-file-excel text-success'
    case 'doc':
    case 'docx':
      return 'pi pi-file-word text-info'
    case 'png':
    case 'jpg':
    case 'jpeg':
    case 'webp':
      return 'pi pi-image text-accent'
    case 'zip':
    case 'rar':
      return 'pi pi-box'
    default:
      return 'pi pi-file'
  }
}

const getCategoriaLabel = (cat) => {
  const map = {
    GENERAL: 'General',
    FACTURA: 'Factura Comercial',
    PACKING_LIST: 'Packing List',
    TRANSPORTE_BL: 'B/L / AWB',
    CERTIFICADO: 'Certificado de Origen',
    PERMISO_ESPECIAL: 'Permiso Especial',
    IDENTIFICACION: 'Identificación / RUC'
  }
  return map[cat] || cat
}

const getCategoriaSeverity = (cat) => {
  switch (cat) {
    case 'FACTURA':
      return 'success'
    case 'TRANSPORTE_BL':
      return 'info'
    case 'PERMISO_ESPECIAL':
      return 'warn'
    case 'CERTIFICADO':
      return 'contrast'
    default:
      return 'secondary'
  }
}

const formatBytes = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

const formatDateTime = (val) => {
  if (!val) return '—'
  try {
    const d = new Date(val)
    if (isNaN(d.getTime())) return val
    return d.toLocaleDateString('es-PE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return val
  }
}

defineExpose({
  loadDocumentos,
  clearDeferredQueue
})
</script>

<style scoped>
.docs-upload-panel {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.regulatory-docs-alert {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  background: var(--color-warning-subtle, #fef8ec);
  border: 1px solid var(--color-warning, #d97706);
  border-left: 5px solid var(--color-warning, #d97706);
  border-radius: var(--radius-md, 8px);
  padding: 1rem 1.25rem;
}

.alert-icon-box {
  color: var(--color-warning, #d97706);
  font-size: 1.5rem;
  margin-top: 0.1rem;
}

.alert-content {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.alert-title {
  font-size: 0.875rem;
  color: var(--color-text-main);
  font-weight: 700;
}

.alert-desc {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin: 0;
  line-height: 1.4;
}

.docs-header-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1.25rem;
  padding: 1.25rem 1.5rem;
  background: var(--color-bg-subtle, #f5f2eb);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  flex-wrap: wrap;
}

.badge-kicker {
  font-size: 0.6875rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker, #9a7f60);
  display: block;
  margin-bottom: 0.25rem;
}

.entity-title-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.entity-code {
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--color-text-main);
}

.entity-name {
  font-size: 0.9375rem;
  color: var(--color-text-muted);
}

.header-sub {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  display: block;
  margin-top: 0.25rem;
}

.doc-counter-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  padding: 0.5rem 1rem;
}

.counter-num {
  font-family: var(--font-mono, monospace);
  font-size: 1.375rem;
  font-weight: 800;
  color: var(--color-accent-kicker, #c2410c);
}

.counter-label {
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-text-subtle);
  letter-spacing: 0.05em;
}

.upload-section-card {
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  padding: 1rem 1.25rem;
}

.upload-controls-row {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.control-item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.control-item.flex-grow {
  flex: 1;
  min-width: 250px;
}

.control-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.text-input {
  padding: 0.55rem 0.85rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 6px);
  background-color: var(--color-bg-surface);
  color: var(--color-text-main);
  font-size: 0.875rem;
  font-family: inherit;
  transition: border-color var(--transition-fast, 0.2s);
}

.text-input:focus {
  outline: none;
  border-color: var(--color-primary);
}

.dropzone-area {
  border: 2px dashed var(--color-border-strong, #c4bcab);
  background: var(--color-bg-subtle, #f9f7f2);
  border-radius: var(--radius-md, 8px);
  padding: 1.5rem 1rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dropzone-area:hover,
.dropzone-area.is-dragging {
  border-color: var(--color-primary, #352f2a);
  background: var(--color-bg-muted, #ede6db);
}

.hidden-file-input {
  display: none;
}

.dropzone-icon {
  font-size: 2.25rem;
  color: var(--color-accent-kicker, #9a7f60);
  margin-bottom: 0.5rem;
}

.dropzone-text {
  font-size: 0.875rem;
  color: var(--color-text-main);
  margin: 0;
}

.dropzone-sub {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  display: block;
  margin-top: 0.35rem;
}

.queued-files-panel {
  margin-top: 1rem;
  background: var(--color-bg-subtle, #f5f2eb);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  padding: 0.75rem 1rem;
}

.queue-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin-bottom: 0.5rem;
}

.queue-title-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.btn-clear-queue {
  background: none;
  border: none;
  color: var(--color-danger-text, #9e2a2b);
  font-size: 0.75rem;
  cursor: pointer;
}

.btn-clear-queue:hover {
  text-decoration: underline;
}

.queued-list {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  max-height: 220px;
  overflow-y: auto;
}

.queued-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 6px);
  padding: 0.45rem 0.75rem;
  font-size: 0.8125rem;
  gap: 0.75rem;
}

.queued-item-left {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  overflow: hidden;
  flex: 1;
}

.queued-file-details {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.queued-name {
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 380px;
  color: var(--color-text-main);
}

.queued-meta {
  color: var(--color-text-subtle);
  font-size: 0.75rem;
}

.meta-note {
  font-style: italic;
  color: var(--color-text-muted);
}

.queued-item-right {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-shrink: 0;
}

.btn-remove-queued {
  background: none;
  border: none;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: 0.2rem;
  display: flex;
  align-items: center;
}

.btn-remove-queued:hover {
  color: var(--color-danger-text, #9e2a2b);
}

.upload-action-row {
  margin-top: 0.75rem;
  display: flex;
  justify-content: flex-end;
}

.btn-upload-submit {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1.25rem;
  background: var(--color-primary, #352f2a);
  color: #fff;
  border: none;
  border-radius: var(--radius-md, 8px);
  font-weight: 700;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-upload-submit:hover:not(:disabled) {
  background: var(--color-primary-hover, #484039);
  transform: translateY(-1px);
}

.docs-list-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.list-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.list-title {
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
}

.btn-refresh-docs {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: none;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 6px);
  padding: 0.35rem 0.6rem;
  font-size: 0.75rem;
  color: var(--color-text-main);
  cursor: pointer;
}

.docs-loading-state,
.docs-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2.5rem 1rem;
  background: var(--color-bg-surface);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md, 8px);
  text-align: center;
  gap: 0.5rem;
  color: var(--color-text-muted);
}

.empty-icon {
  font-size: 2.25rem;
  color: var(--color-text-subtle);
}

.empty-title {
  font-weight: 700;
  margin: 0;
  color: var(--color-text-main);
}

.empty-desc {
  font-size: 0.8125rem;
  margin: 0;
}

.doc-file-cell {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.file-icon-box {
  width: 2.25rem;
  height: 2.25rem;
  border-radius: var(--radius-sm, 6px);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.125rem;
  flex-shrink: 0;
}

.file-text-col {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.file-name {
  font-weight: 700;
  font-size: 0.8125rem;
  color: var(--color-text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 220px;
}

.file-meta {
  font-size: 0.6875rem;
  color: var(--color-text-subtle);
}

.doc-desc-text {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.user-cell {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.date-sub {
  font-size: 0.6875rem;
  color: var(--color-text-subtle);
}

.preview-container {
  width: 100%;
  height: 65vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #2b2b2b;
  border-radius: var(--radius-md, 8px);
  overflow: hidden;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.preview-unsupported,
.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  color: #fff;
}

.dialog-actions-row {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.actions-wrapper {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.5rem;
}

.action-btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.65rem;
  border-radius: var(--radius-sm, 6px);
  background: var(--color-primary, #352f2a);
  color: #fff;
  border: none;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
}

.action-btn-outline {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.65rem;
  border-radius: var(--radius-sm, 6px);
  background: transparent;
  color: var(--color-text-main);
  border: 1px solid var(--color-border);
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
}

.action-btn-outline.reject {
  color: var(--color-danger-text, #9e2a2b);
  border-color: var(--color-danger-border, #f5c2c7);
}

.action-btn-outline.reject:hover {
  background: var(--color-danger-subtle, #f8d7da);
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 1rem;
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  border-radius: var(--radius-md, 8px);
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-outline-warm:hover {
  background: var(--color-bg-subtle);
}

.text-danger {
  color: var(--color-danger-text, #9e2a2b);
}

.text-success {
  color: var(--color-success-text, #1e6638);
}

.text-info {
  color: var(--color-info-text, #21598f);
}

.text-accent {
  color: var(--color-accent-copper, #c2410c);
}
</style>
