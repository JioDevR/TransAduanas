<template>
  <section class="sga-step-card">
    <!-- Barra superior: Buscador + Botón Registro Rápido -->
    <div class="sga-client-toolbar">
      <IconField class="sga-search-field">
        <InputIcon class="pi pi-search" />
        <InputText
          v-model.trim="searchQuery"
          placeholder="Buscar cliente por RUC, DNI o razón social..."
          fluid
          size="small"
        />
      </IconField>

      <Button
        type="button"
        :label="showQuickClientForm ? 'Cerrar Registro' : 'Crear Nuevo Cliente'"
        :icon="showQuickClientForm ? 'pi pi-times' : 'pi pi-user-plus'"
        severity="secondary"
        outlined
        size="small"
        @click="$emit('update:showQuickClientForm', !showQuickClientForm)"
      />
    </div>

    <!-- Panel Inline: Registro de Cliente Comercial -->
    <div v-if="showQuickClientForm" class="sga-quick-client-panel">
      <div class="sga-quick-panel-title">
        <i class="pi pi-user-plus"></i>
        <span>Registro de Cliente Comercial</span>
      </div>

      <Message v-if="quickClientError" severity="error" :closable="false" class="sga-inline-msg">
        {{ quickClientError }}
      </Message>

      <!-- Fila 1: 3 columnas (Tipo Doc, Número Doc, Razón Social) -->
      <div class="sga-form-grid cols-3">
        <div class="sga-field">
          <label class="sga-field-label">TIPO DOCUMENTO *</label>
          <Select
            v-model="quickClient.tipoDocumento"
            :options="docTypeOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
          />
        </div>

        <div class="sga-field">
          <label class="sga-field-label">NÚMERO DE DOCUMENTO *</label>
          <IconField>
            <InputText
              v-model.trim="quickClient.numeroDocumento"
              placeholder="Ej. 20601234567"
              maxlength="15"
              size="small"
              fluid
              @keydown.enter.prevent="$emit('lookup-client')"
            />
            <InputIcon
              :class="lookingUpClient ? 'pi pi-spin pi-spinner' : 'pi pi-search'"
              class="sga-icon-clickable"
              title="Buscar en directorio de clientes"
              @click="$emit('lookup-client')"
            />
          </IconField>
        </div>

        <div class="sga-field">
          <label class="sga-field-label">RAZÓN SOCIAL O NOMBRE COMPLETO *</label>
          <InputText
            v-model.trim="quickClient.razonSocial"
            placeholder="Ej. COMERCIALIZADORA DEL PACIFICO S.A.C."
            size="small"
            fluid
          />
        </div>
      </div>

      <!-- Fila 2: 2 columnas (Dirección Fiscal, Representante Comercial) -->
      <div class="sga-form-grid cols-2">
        <div class="sga-field">
          <label class="sga-field-label">DIRECCIÓN FISCAL</label>
          <InputText
            v-model.trim="quickClient.direccionFiscal"
            placeholder="Ej. Av. Elmer Faucett 2823, Callao - Lima"
            size="small"
            fluid
          />
        </div>

        <div class="sga-field">
          <label class="sga-field-label">REPRESENTANTE COMERCIAL</label>
          <InputText
            v-model.trim="quickClient.contactoRepresentante"
            placeholder="Ej. Carlos Mendoza — Asesor Comercial Senior"
            size="small"
            fluid
          />
        </div>
      </div>

      <!-- Fila 3: 2 columnas (Correo de Contacto, Teléfono Móvil) -->
      <div class="sga-form-grid cols-2">
        <div class="sga-field">
          <label class="sga-field-label">CORREO DE CONTACTO *</label>
          <InputText
            v-model.trim="quickClient.correoContacto"
            type="email"
            placeholder="Ej. importaciones@empresa.com"
            size="small"
            fluid
          />
        </div>

        <div class="sga-field">
          <label class="sga-field-label">TELÉFONO MÓVIL</label>
          <InputText
            v-model.trim="quickClient.telefonoContacto"
            placeholder="Ej. +51 999 888 777"
            size="small"
            fluid
          />
        </div>
      </div>

      <div class="sga-quick-panel-actions">
        <Button
          type="button"
          label="Cancelar"
          severity="secondary"
          outlined
          size="small"
          :disabled="savingQuickClient"
          @click="$emit('update:showQuickClientForm', false)"
        />
        <Button
          type="button"
          label="Registrar"
          icon="pi pi-check"
          size="small"
          :loading="savingQuickClient"
          @click="$emit('save-quick-client')"
        />
      </div>
    </div>

    <!-- Vista cuando NO está abierto el registro rápido -->
    <template v-else>
      <!-- Tarjeta del cliente ya seleccionado -->
      <div v-if="selectedClientObject" class="sga-selected-client-banner">
        <div class="sga-selected-info">
          <div class="sga-selected-meta">
            <span class="sga-doc-chip font-mono">{{ selectedClientObject.tipoDocumento }}: {{ selectedClientObject.numeroDocumento }}</span>
            <Tag
              :value="selectedClientObject.esProspecto ? 'Prospecto' : 'Cliente'"
              :severity="selectedClientObject.esProspecto ? 'warn' : 'success'"
              rounded
            />
          </div>
          <h4 class="sga-selected-name">{{ selectedClientObject.razonSocial }}</h4>
          <span class="sga-selected-sub">
            {{ selectedClientObject.correoContacto }}
            <template v-if="selectedClientObject.telefonoContacto"> · {{ selectedClientObject.telefonoContacto }}</template>
          </span>
        </div>

        <Button
          type="button"
          label="Cambiar"
          icon="pi pi-refresh"
          severity="secondary"
          outlined
          size="small"
          @click="$emit('clear-client')"
        />
      </div>

      <!-- Directorio rápido de clientes -->
      <div v-if="!selectedClientObject || searchQuery" class="sga-clients-directory">
        <div v-if="filteredClients.length === 0" class="sga-empty-clients">
          <p class="sga-empty-text">No se encontraron clientes para "{{ searchQuery }}".</p>
          <Button
            type="button"
            label="Registrar nuevo cliente"
            icon="pi pi-user-plus"
            severity="secondary"
            outlined
            size="small"
            @click="$emit('update:showQuickClientForm', true)"
          />
        </div>

        <div v-else class="sga-clients-grid">
          <button
            v-for="client in filteredClients.slice(0, 12)"
            :key="client.idCliente"
            type="button"
            :class="['sga-client-card', { 'is-selected': selectedClientObject?.idCliente === client.idCliente }]"
            @click="$emit('select-client', client)"
          >
            <div class="sga-client-card-top">
              <span class="sga-doc-chip font-mono">{{ client.tipoDocumento }}: {{ client.numeroDocumento }}</span>
              <span :class="['sga-type-dot', client.esProspecto ? 'is-prospect' : 'is-client']">
                {{ client.esProspecto ? 'Prospecto' : 'Cliente' }}
              </span>
            </div>
            <span class="sga-client-card-name">{{ client.razonSocial }}</span>
            <span class="sga-client-card-email">{{ client.correoContacto }}</span>
          </button>
        </div>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import Tag from 'primevue/tag'
import Message from 'primevue/message'

const props = defineProps({
  clientSearchQuery: {
    type: String,
    default: ''
  },
  filteredClients: {
    type: Array,
    default: () => []
  },
  selectedClientObject: {
    type: Object,
    default: null
  },
  showQuickClientForm: {
    type: Boolean,
    default: false
  },
  quickClient: {
    type: Object,
    required: true
  },
  savingQuickClient: {
    type: Boolean,
    default: false
  },
  lookingUpClient: {
    type: Boolean,
    default: false
  },
  quickClientError: {
    type: String,
    default: ''
  }
})

const emit = defineEmits([
  'update:clientSearchQuery',
  'update:showQuickClientForm',
  'select-client',
  'clear-client',
  'lookup-client',
  'save-quick-client'
])

const searchQuery = computed({
  get: () => props.clientSearchQuery,
  set: (val) => emit('update:clientSearchQuery', val)
})

const docTypeOptions = [
  { label: 'RUC', value: 'RUC' },
  { label: 'DNI', value: 'DNI' },
  { label: 'Carnet Extranjería', value: 'CE' },
  { label: 'Pasaporte', value: 'PASAPORTE' }
]
</script>

<style scoped>
.sga-step-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--sga-card-radius, 14px);
  padding: var(--sga-card-padding-y, 1.75rem) var(--sga-card-padding-x, 2rem);
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.sga-client-toolbar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.sga-search-field {
  flex: 1;
}

.sga-quick-client-panel {
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: var(--sga-subpanel-padding, 1.25rem 1.35rem);
  display: flex;
  flex-direction: column;
  gap: 1rem;
  background: var(--color-bg-base);
}

.sga-quick-panel-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.sga-quick-panel-title i {
  color: var(--color-primary);
}

.sga-form-grid {
  display: grid;
  gap: 1rem;
}

.sga-form-grid.cols-3 {
  grid-template-columns: minmax(0, 0.85fr) minmax(0, 1fr) minmax(0, 1.35fr);
}

.sga-form-grid.cols-2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.sga-field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  min-width: 0;
}

.sga-field-label {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--color-text-muted);
}

.sga-icon-clickable {
  cursor: pointer;
}

.sga-quick-panel-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.625rem;
  padding-top: 0.25rem;
}

.sga-selected-client-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.25rem;
  border: 1px solid var(--color-primary);
  border-radius: 12px;
  background: var(--color-bg-base);
}

.sga-selected-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.sga-selected-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.sga-selected-name {
  margin: 0;
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.sga-selected-sub {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.sga-clients-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 240px), 1fr));
  gap: 0.75rem;
}

.sga-client-card {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: 0.9rem 1rem;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  background: var(--color-bg-base);
  text-align: left;
  cursor: pointer;
  min-width: 0;
  overflow: hidden;
  transition: border-color var(--transition-fast), background-color var(--transition-fast);
}

.sga-client-card:hover {
  border-color: var(--color-primary);
}

.sga-client-card.is-selected {
  border-color: var(--color-primary);
  background: var(--color-bg-subtle);
}

.sga-client-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}

.sga-doc-chip {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.sga-type-dot {
  font-size: 0.6875rem;
  font-weight: 600;
  color: var(--color-text-subtle);
}

.sga-type-dot.is-client {
  color: var(--color-success-text);
}

.sga-type-dot.is-prospect {
  color: var(--color-warning-text);
}

.sga-client-card-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sga-client-card-email {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sga-empty-clients {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 2rem 1rem;
  border: 1px dashed var(--color-border);
  border-radius: 12px;
}

.sga-empty-text {
  margin: 0;
  font-size: 0.875rem;
  color: var(--color-text-muted);
}

.font-mono {
  font-family: var(--font-mono);
}

@media (max-width: 900px) {
  .sga-client-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .sga-form-grid.cols-3,
  .sga-form-grid.cols-2 {
    grid-template-columns: 1fr;
  }
}
</style>
