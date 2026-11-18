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
          La mercancía seleccionada requiere autorización o visto bueno previo de
          <strong>{{ entidadReguladora || 'Entidad Reguladora' }}</strong>.
          Adjunte la resolución, permiso sanitario o registro correspondiente bajo la categoría
          <strong>Permiso Especial / Registro Regulatorio</strong>.
        </p>
      </div>
    </div>

    <!-- Zona de Carga de Nuevos Documentos (Dropzone) -->
    <div class="upload-section-card">
      <div class="upload-controls-row">
        <div class="control-item">
          <label class="control-label" for="portal-doc-cat">Categoría de Documento *</label>
          <select id="portal-doc-cat" v-model="formCategoria" class="select-input">
            <option v-for="cat in CATEGORIAS_DOCUMENTO" :key="cat.value" :value="cat.value">
              {{ cat.label }}
            </option>
          </select>
        </div>

        <div class="control-item flex-grow">
          <label class="control-label" for="portal-doc-desc">Descripción / Instrucciones (Opcional)</label>
          <input
            id="portal-doc-desc"
            v-model.trim="formDescripcion"
            type="text"
            class="text-input"
            placeholder="Ej. Factura comercial visada por proveedor, lista de empaque..."
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
      <div v-if="mode === 'deferred' && modelValue && modelValue.length > 0" class="queued-files-panel">
        <div class="queue-header">
          <span class="queue-title-badge">
            <i class="pi pi-paperclip"></i>
            Archivos listos para adjuntar a la solicitud ({{ modelValue.length }}):
          </span>
          <button type="button" class="btn-clear-queue" @click="clearDeferredQueue">
            Limpiar lista
          </button>
        </div>
        <div class="queued-list">
          <div v-for="(item, idx) in modelValue" :key="item.tempId || idx" class="queued-item">
            <div class="queued-item-left">
              <i :class="getFileIconClass(item.file?.name || item.name)"></i>
              <div class="queued-file-details">
                <span class="queued-name font-mono" :title="item.file?.name || item.name">
                  {{ item.file?.name || item.name }}
                </span>
                <span class="queued-meta">
                  {{ formatBytes(item.file?.size || item.size) }}
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
                title="Quitar archivo de la lista"
                aria-label="Quitar archivo de la lista"
                @click="removeDeferredFile(idx)"
              >
                <i class="pi pi-times"></i>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Modo Directo (Live): Lista de archivos en cola para subir a la API -->
      <div v-if="mode === 'live' && liveQueue.length > 0" class="queued-files-panel">
        <div class="queue-header">
          <span class="queue-title-badge">
            <i class="pi pi-cloud-upload"></i>
            Archivos listos para subir al expediente ({{ liveQueue.length }}):
          </span>
          <button type="button" class="btn-clear-queue" @click="liveQueue = []">
            Limpiar selección
          </button>
        </div>
        <div class="queued-list">
          <div v-for="(item, idx) in liveQueue" :key="idx" class="queued-item">
            <div class="queued-item-left">
              <i :class="getFileIconClass(item.file?.name || item.name)"></i>
              <div class="queued-file-details">
                <span class="queued-name font-mono" :title="item.file?.name || item.name">
                  {{ item.file?.name || item.name }}
                </span>
                <span class="queued-meta">
                  {{ formatBytes(item.file?.size || item.size) }}
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
                title="Quitar archivo"
                aria-label="Quitar archivo"
                @click="liveQueue.splice(idx, 1)"
              >
                <i class="pi pi-times"></i>
              </button>
            </div>
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

    <!-- Lista de Documentos Registrados en Servidor (Solo en modo Live) -->
    <div v-if="mode === 'live'" class="docs-list-section">
      <div class="list-section-header">
        <h3 class="list-title">
          <i class="pi pi-folder"></i>
          <span>Documentos Registrados en Repositorio ({{ documentos.length }})</span>
        </h3>
        <button
          type="button"
          class="btn-refresh-docs"
          :disabled="loading"
          @click="loadDocumentos"
          title="Actualizar listado de documentos"
        >
          <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
          <span>Actualizar</span>
        </button>
      </div>

      <!-- Estado de Carga -->
      <div v-if="loading" class="docs-loading-state">
        <i class="pi pi-spin pi-spinner"></i>
        <span>Consultando documentos adjuntos...</span>
      </div>

      <!-- Estado Vacío -->
      <div v-else-if="documentos.length === 0" class="docs-empty-state">
        <i class="pi pi-folder-open empty-icon"></i>
        <p class="empty-title">No hay documentos registrados para esta cotización</p>
        <p class="empty-desc">Use la zona de carga superior para adjuntar facturas proforma, listas de empaque o permisos sectoriales.</p>
      </div>

      <!-- Tabla de Documentos (SIN BOTÓN DE ELIMINAR) -->
      <div v-else class="docs-table-wrapper">
        <table class="portal-docs-table">
          <thead>
            <tr>
              <th>Documento / Nombre</th>
              <th>Categoría</th>
              <th>Descripción</th>
              <th>Fecha de Subida</th>
              <th class="text-right">Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="doc in documentos" :key="doc.idDocumento || doc.id">
              <td>
                <div class="doc-file-cell">
                  <div class="file-icon-box">
                    <i :class="getFileIconClass(doc.nombreOriginal || doc.nombreArchivo)"></i>
                  </div>
                  <div class="file-text-col">
                    <span class="file-name font-mono" :title="doc.nombreOriginal || doc.nombreArchivo">
                      {{ doc.nombreOriginal || doc.nombreArchivo || 'Documento Oficial' }}
                    </span>
                    <span class="file-meta">
                      {{ formatBytes(doc.tamanoBytes || doc.tamano) }}
                      <span v-if="doc.extension"> • {{ doc.extension.toUpperCase() }}</span>
                    </span>
                  </div>
                </div>
              </td>
              <td>
                <Tag
                  :value="getCategoriaLabel(doc.categoria)"
                  :severity="getCategoriaSeverity(doc.categoria)"
                  rounded
                />
              </td>
              <td>
                <span class="doc-desc-text">{{ doc.descripcion || '—' }}</span>
              </td>
              <td>
                <div class="date-cell">
                  <span class="date-sub font-mono">{{ formatDateTime(doc.fechaCreacion || doc.fechaSubida) }}</span>
                </div>
              </td>
              <td class="text-right">
                <div class="actions-wrapper">
                  <!-- Botón Ver (Inline / Tab) -->
                  <button
                    type="button"
                    class="action-btn-primary"
                    title="Previsualizar documento"
                    @click="handlePreview(doc)"
                  >
                    <i class="pi pi-eye"></i>
                    <span>Ver</span>
                  </button>

                  <!-- Botón Descargar -->
                  <button
                    type="button"
                    class="action-btn-outline"
                    title="Descargar archivo a su equipo"
                    @click="handleDownload(doc)"
                  >
                    <i class="pi pi-download"></i>
                    <span>Bajar</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import Tag from 'primevue/tag'
import { portalCotizacionService } from '../services/portalCotizacionService'
import { downloadBlob, openBlobInNewTab } from '@/shared/utils/download'
import { useNotification } from '@/shared/composables/useNotification'
import { formatBytes, formatDateTime } from '@/shared/utils/formatters'
import { CATEGORIAS_DOCUMENTO } from '../utils/portalCotizacionFormUtils'

const props = defineProps({
  mode: {
    type: String,
    default: 'deferred',
    validator: (v) => ['deferred', 'live'].includes(v)
  },
  modelValue: {
    type: Array,
    default: () => []
  },
  idCotizacion: {
    type: [Number, String],
    default: null
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

const emit = defineEmits(['update:modelValue', 'uploaded'])

const { showSuccess, showError, showInfo } = useNotification()

const loading = ref(false)
const uploading = ref(false)
const documentos = ref([])

const formCategoria = ref(props.permisoRequerido ? 'PERMISO_ESPECIAL' : 'GENERAL')
const formDescripcion = ref('')
const isDragging = ref(false)
const fileInputRef = ref(null)

// Cola temporal para modo Live
const liveQueue = ref([])

watch(
  () => props.permisoRequerido,
  (val) => {
    if (val && formCategoria.value === 'GENERAL') {
      formCategoria.value = 'PERMISO_ESPECIAL'
    }
  }
)

watch(
  () => props.idCotizacion,
  (newId) => {
    if (props.mode === 'live' && newId) {
      loadDocumentos()
    }
  }
)

onMounted(() => {
  if (props.mode === 'live' && props.idCotizacion) {
    loadDocumentos()
  }
})

function triggerFileInput() {
  fileInputRef.value?.click()
}

function handleFileSelect(e) {
  const selected = Array.from(e.target.files || [])
  addFiles(selected)
  e.target.value = ''
}

function handleFileDrop(e) {
  isDragging.value = false
  const dropped = Array.from(e.dataTransfer.files || [])
  addFiles(dropped)
}

function addFiles(files) {
  const validFiles = files.filter((f) => f.size <= 25 * 1024 * 1024)
  if (validFiles.length < files.length) {
    showError('Algunos archivos superan el límite de 25 MB y fueron omitidos.')
  }

  if (validFiles.length === 0) return

  if (props.mode === 'deferred') {
    const newItems = validFiles.map((file) => ({
      tempId: `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
      file,
      categoria: formCategoria.value,
      descripcion: formDescripcion.value || ''
    }))
    const updated = [...(props.modelValue || []), ...newItems]
    emit('update:modelValue', updated)
    formDescripcion.value = ''
    showInfo(`${validFiles.length} archivo(s) agregado(s) a la documentación.`)
  } else {
    const newItems = validFiles.map((file) => ({
      file,
      categoria: formCategoria.value,
      descripcion: formDescripcion.value || ''
    }))
    liveQueue.value.push(...newItems)
    formDescripcion.value = ''
    showInfo(`${validFiles.length} archivo(s) agregado(s) a la cola de subida.`)
  }
}

function removeDeferredFile(idx) {
  const updated = [...(props.modelValue || [])]
  updated.splice(idx, 1)
  emit('update:modelValue', updated)
}

function clearDeferredQueue() {
  emit('update:modelValue', [])
}

async function submitLiveUpload() {
  if (!props.idCotizacion || liveQueue.value.length === 0) return

  uploading.value = true
  try {
    const result = await portalCotizacionService.subirDocumentosCotizacion(
      props.idCotizacion,
      liveQueue.value
    )

    if (result.uploaded && result.uploaded.length > 0) {
      showSuccess(`Se subieron ${result.uploaded.length} documento(s) correctamente.`)
    }
    if (result.failed && result.failed.length > 0) {
      showError(`No se pudieron subir ${result.failed.length} archivo(s).`)
    }

    liveQueue.value = []
    await loadDocumentos()
    emit('uploaded', result.uploaded || [])
  } catch (err) {
    showError(err.response?.data?.message || 'Error al subir los documentos a la cotización.')
  } finally {
    uploading.value = false
  }
}

async function loadDocumentos() {
  if (!props.idCotizacion) return
  loading.value = true
  try {
    const res = await portalCotizacionService.listarDocumentosCotizacion(props.idCotizacion)
    documentos.value = res || []
  } catch (err) {
    console.debug('Error al cargar documentos de la cotización:', err)
    showError('No se pudieron recuperar los documentos adjuntos.')
  } finally {
    loading.value = false
  }
}

async function handlePreview(doc) {
  try {
    const blob = await portalCotizacionService.previsualizarDocumento(doc.idDocumento || doc.id)
    openBlobInNewTab(blob, doc.formatoMime || doc.mimeType || 'application/pdf')
  } catch (err) {
    showError(err.response?.data?.message || 'Error al previsualizar el documento en el navegador.')
  }
}

async function handleDownload(doc) {
  try {
    const blob = await portalCotizacionService.descargarDocumento(doc.idDocumento || doc.id)
    downloadBlob(
      blob,
      doc.nombreOriginal || doc.nombreArchivo || `documento-${doc.idDocumento || doc.id}.pdf`,
      doc.formatoMime || doc.mimeType || 'application/pdf'
    )
    showSuccess(`Descargando ${doc.nombreOriginal || doc.nombreArchivo || 'documento'}`)
  } catch (err) {
    showError(err.response?.data?.message || 'Error al descargar el documento del servidor.')
  }
}

function getCategoriaLabel(catVal) {
  const found = CATEGORIAS_DOCUMENTO.find((c) => c.value === catVal)
  return found ? found.label : catVal || 'General'
}

function getCategoriaSeverity(catVal) {
  switch (catVal) {
    case 'PERMISO_ESPECIAL':
      return 'warn'
    case 'FACTURA':
      return 'info'
    case 'TRANSPORTE_BL':
      return 'success'
    case 'CERTIFICADO':
      return 'secondary'
    case 'PACKING_LIST':
      return 'secondary'
    case 'IDENTIFICACION':
      return 'contrast'
    default:
      return 'secondary'
  }
}

function getFileIconClass(filename = '') {
  if (!filename) return 'pi pi-file'
  const ext = String(filename).split('.').pop()?.toLowerCase()
  if (ext === 'pdf') return 'pi pi-file-pdf'
  if (['png', 'jpg', 'jpeg', 'webp', 'svg'].includes(ext)) return 'pi pi-image'
  if (['xlsx', 'xls', 'csv'].includes(ext)) return 'pi pi-file-excel'
  if (['doc', 'docx'].includes(ext)) return 'pi pi-file-word'
  return 'pi pi-file'
}
</script>

<style scoped>
.docs-upload-panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-4, 1rem);
}

.regulatory-docs-alert {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3, 0.75rem);
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border);
  border-radius: var(--radius-lg, 0.75rem);
  padding: 1rem 1.25rem;
  color: var(--color-warning-text);
}

.alert-icon-box {
  font-size: 1.25rem;
  flex-shrink: 0;
  margin-top: 0.1rem;
}

.alert-content {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.alert-title {
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 700;
}

.alert-desc {
  font-size: var(--font-size-xs, 0.75rem);
  line-height: 1.45;
  margin: 0;
}

.upload-section-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-4, 1rem);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  padding: 1.25rem;
}

.upload-controls-row {
  display: flex;
  align-items: flex-end;
  gap: var(--space-3, 0.75rem);
  flex-wrap: wrap;
}

.control-item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  min-width: 220px;
}

.control-item.flex-grow {
  flex: 1;
}

.control-label {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: var(--color-text-main);
}

.select-input,
.text-input {
  padding: 0.55rem 0.75rem;
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-text-main);
  background-color: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
  outline: none;
  transition: border-color var(--transition-fast);
}

.select-input {
  padding-right: 2.25rem;
}

.select-input:focus,
.text-input:focus {
  border-color: var(--color-primary);
}

.dropzone-area {
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px dashed var(--color-border-strong);
  border-radius: var(--radius-lg, 0.75rem);
  padding: 1.75rem 1.25rem;
  background: var(--color-bg-subtle);
  cursor: pointer;
  transition: all var(--transition-fast);
  text-align: center;
}

.dropzone-area:hover,
.dropzone-area.is-dragging {
  border-color: var(--color-primary);
  background: var(--color-bg-muted);
}

.hidden-file-input {
  display: none;
}

.dropzone-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.4rem;
}

.dropzone-icon {
  font-size: 2rem;
  color: var(--color-primary);
}

.dropzone-text {
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-text-main);
  margin: 0;
}

.dropzone-sub {
  font-size: var(--font-size-2xs, 0.6875rem);
  color: var(--color-text-muted);
}

.queued-files-panel {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding-top: 0.5rem;
  border-top: 1px solid var(--color-border-subtle);
}

.queue-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.queue-title-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: var(--color-accent-kicker);
}

.btn-clear-queue {
  background: none;
  border: none;
  color: var(--color-text-subtle);
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 600;
  cursor: pointer;
  padding: 0.2rem 0.4rem;
  transition: color var(--transition-fast);
}

.btn-clear-queue:hover {
  color: var(--color-danger);
}

.queued-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.queued-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.55rem 0.85rem;
  background: var(--color-bg-base);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
}

.queued-item-left {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  min-width: 0;
  flex: 1;
}

.queued-item-left i {
  font-size: 1.25rem;
  color: var(--color-primary);
  flex-shrink: 0;
}

.queued-file-details {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
  min-width: 0;
}

.queued-name {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: var(--color-text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.queued-meta {
  font-size: var(--font-size-2xs, 0.6875rem);
  color: var(--color-text-muted);
}

.meta-note {
  font-style: italic;
  color: var(--color-text-subtle);
}

.queued-item-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

.btn-remove-queued {
  background: none;
  border: none;
  color: var(--color-text-muted);
  font-size: 0.8rem;
  cursor: pointer;
  padding: 0.25rem;
  border-radius: var(--radius-xs, 0.25rem);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.btn-remove-queued:hover {
  color: var(--color-danger);
  background: var(--color-danger-bg);
}

.upload-action-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.5rem;
}

.btn-upload-submit {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.55rem 1.25rem;
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 600;
  border-radius: var(--radius-md, 0.5rem);
  border: none;
  background: var(--color-primary);
  color: var(--color-primary-text);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-upload-submit:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.btn-upload-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Documentos Registrados */
.docs-list-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  padding: 1.25rem;
}

.list-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.list-title {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
}

.list-title i {
  color: var(--color-accent-kicker);
}

.btn-refresh-docs {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: none;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 0.25rem);
  padding: 0.3rem 0.65rem;
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 600;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-refresh-docs:hover:not(:disabled) {
  border-color: var(--color-border-strong);
  color: var(--color-text-main);
}

.docs-loading-state,
.docs-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 2.5rem 1.5rem;
  background: var(--color-bg-subtle);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
  text-align: center;
  color: var(--color-text-muted);
}

.empty-icon {
  font-size: 2.25rem;
  opacity: 0.4;
}

.empty-title {
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
}

.empty-desc {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
  margin: 0;
}

.docs-table-wrapper {
  overflow-x: auto;
}

.portal-docs-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--font-size-xs, 0.75rem);
}

.portal-docs-table th {
  text-align: left;
  padding: 0.65rem 0.85rem;
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--color-text-muted);
  background: var(--color-bg-subtle);
  border-bottom: 1px solid var(--color-border);
}

.portal-docs-table td {
  padding: 0.75rem 0.85rem;
  border-bottom: 1px solid var(--color-border-subtle);
  color: var(--color-text-main);
  vertical-align: middle;
}

.portal-docs-table tr:hover td {
  background: var(--color-bg-subtle);
}

.doc-file-cell {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.file-icon-box i {
  font-size: 1.4rem;
  color: var(--color-primary);
}

.file-text-col {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.file-name {
  font-weight: 700;
  color: var(--color-text-main);
}

.file-meta {
  font-size: var(--font-size-2xs, 0.6875rem);
  color: var(--color-text-muted);
}

.doc-desc-text {
  color: var(--color-text-muted);
  font-style: normal;
}

.date-sub {
  font-size: var(--font-size-2xs, 0.6875rem);
  color: var(--color-text-subtle);
}

.actions-wrapper {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.action-btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.75rem;
  background: var(--color-primary);
  color: var(--color-primary-text);
  border: none;
  border-radius: var(--radius-sm, 0.25rem);
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-btn-primary:hover {
  background: var(--color-primary-hover);
}

.action-btn-outline {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.75rem;
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 0.25rem);
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-btn-outline:hover {
  border-color: var(--color-border-strong);
  background: var(--color-bg-subtle);
}

.text-right {
  text-align: right;
}
</style>
