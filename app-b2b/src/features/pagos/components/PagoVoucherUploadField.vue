<template>
  <div class="sga-form-field">
    <label :for="inputId" class="sga-form-label">{{ label }}</label>

    <div
      :class="['voucher-dropzone', { 'is-dragging': isDragging, 'has-file': Boolean(modelValue) }]"
      @dragover.prevent="isDragging = true"
      @dragleave.prevent="isDragging = false"
      @drop.prevent="handleDrop"
    >
      <input
        :id="inputId"
        ref="fileInputRef"
        type="file"
        accept="application/pdf,image/png,image/jpeg,image/webp"
        class="file-input-hidden"
        @change="handleSelect"
      />

      <label v-if="!modelValue" :for="inputId" class="dropzone-prompt">
        <span class="dropzone-icon"><i class="pi pi-upload"></i></span>
        <div class="dropzone-copy">
          <strong>{{ promptTitle }}</strong>
          <span>Formatos permitidos: PDF, PNG, JPG, WEBP (Hasta 10 MB)</span>
        </div>
      </label>

      <div v-else class="file-preview-row">
        <div class="file-meta">
          <i
            :class="[
              'pi',
              modelValue.type?.includes('pdf') ? 'pi-file-pdf is-pdf' : 'pi-image is-img'
            ]"
          ></i>
          <div class="file-texts">
            <strong class="sga-mono file-name">{{ modelValue.name }}</strong>
            <span class="sga-mono file-size">{{ formatBytes(modelValue.size) }}</span>
          </div>
        </div>

        <Button
          type="button"
          icon="pi pi-times"
          severity="secondary"
          text
          rounded
          size="small"
          aria-label="Quitar archivo"
          title="Quitar archivo"
          @click="clearFile"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import Button from 'primevue/button'
import { useNotification } from '@/shared/composables/useNotification'
import { formatBytes, validarArchivoVoucher } from '@/shared/utils/formatters'

defineProps({
  modelValue: { type: Object, default: null },
  inputId: { type: String, default: 'voucher-file-input' },
  label: { type: String, default: 'Voucher Bancario (PDF o Imagen) *' },
  promptTitle: { type: String, default: 'Haga clic para adjuntar comprobante o arrastre el archivo aquí' }
})

const emit = defineEmits(['update:modelValue'])

const { showWarn } = useNotification()
const isDragging = ref(false)
const fileInputRef = ref(null)

function handleSelect(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (validarArchivoVoucher(file, showWarn)) {
    emit('update:modelValue', file)
  } else {
    e.target.value = ''
  }
}

function handleDrop(e) {
  isDragging.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file && validarArchivoVoucher(file, showWarn)) {
    emit('update:modelValue', file)
  }
}

function clearFile() {
  emit('update:modelValue', null)
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}
</script>

<style scoped>
.voucher-dropzone {
  position: relative;
  border: 1.5px dashed var(--color-border-strong);
  border-radius: var(--radius-lg, 12px);
  background: var(--color-bg-subtle);
  padding: 1rem 1.15rem;
  transition: border-color var(--transition-fast), background-color var(--transition-fast);
}

.voucher-dropzone.is-dragging {
  border-color: var(--color-primary);
  background: var(--color-bg-muted);
}

.voucher-dropzone.has-file {
  border-style: solid;
  border-color: var(--color-success-border);
  background: var(--color-success-bg);
}

.file-input-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.dropzone-prompt {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  cursor: pointer;
}

.dropzone-icon {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-md);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dropzone-copy {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.dropzone-copy strong {
  font-size: 0.82rem;
  color: var(--color-text-main);
}

.dropzone-copy span {
  font-size: 0.74rem;
  color: var(--color-text-muted);
}

.file-preview-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.file-meta {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  min-width: 0;
}

.file-meta i {
  font-size: 1.3rem;
}

.file-meta i.is-pdf {
  color: var(--color-danger-text);
}

.file-meta i.is-img {
  color: var(--color-primary);
}

.file-texts {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.file-name {
  font-size: 0.82rem;
  color: var(--color-text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}
</style>
