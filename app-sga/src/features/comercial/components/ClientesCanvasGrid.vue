<template>
  <div class="sga-canvas-wrapper">
    <div v-if="loading" class="sga-canvas-state">
      <i class="pi pi-spin pi-spinner sga-state-icon"></i>
      <span>Cargando clientes y prospectos...</span>
    </div>

    <div v-else-if="!clientes || clientes.length === 0" class="sga-canvas-state">
      <i class="pi pi-building sga-state-icon"></i>
      <p class="sga-state-title">No hay clientes encontrados</p>
      <p class="sga-state-desc">Registre su primer cliente o prospecto para empezar a cotizar.</p>
    </div>

    <div v-else class="sga-canvas-grid">
      <article
        v-for="cl in clientes"
        :key="cl.idCliente"
        :class="['sga-canvas-card', { 'is-selected': selection?.idCliente === cl.idCliente }]"
        @click="$emit('update:selection', cl)"
        @dblclick="$emit('edit-item', cl)"
      >
        <header class="sga-card-top">
          <span class="sga-card-code font-mono">{{ cl.tipoDocumento }}: {{ cl.numeroDocumento }}</span>
          <Tag
            :value="cl.esProspecto ? 'Prospecto' : 'Cliente Efectivo'"
            :severity="cl.esProspecto ? 'warn' : 'success'"
            rounded
          />
        </header>

        <div class="sga-card-client">
          <h4 class="sga-client-title" :title="cl.razonSocial">{{ cl.razonSocial }}</h4>
          <div class="sga-client-meta">
            <span v-if="cl.contactoRepresentante" class="sga-client-doc">Rep: {{ cl.contactoRepresentante }}</span>
            <Tag
              :value="clienteTieneUsuarioPortal(cl) ? 'Portal B2B' : 'Presencial'"
              :severity="clienteTieneUsuarioPortal(cl) ? 'info' : 'secondary'"
              class="sga-mini-tag"
            />
          </div>
        </div>

        <div class="sga-card-meta-grid">
          <div class="sga-meta-item">
            <span class="sga-meta-label">CORREO</span>
            <span class="sga-meta-val" :title="cl.correoContacto">{{ cl.correoContacto || '—' }}</span>
          </div>

          <div class="sga-meta-item">
            <span class="sga-meta-label">TELÉFONO</span>
            <span class="sga-meta-val font-mono">{{ cl.telefonoContacto || '—' }}</span>
          </div>

          <div class="sga-meta-item full-span">
            <span class="sga-meta-label">DIRECCIÓN FISCAL</span>
            <span class="sga-meta-val" :title="cl.direccionFiscal">{{ cl.direccionFiscal || '—' }}</span>
          </div>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import Tag from 'primevue/tag'

defineProps({
  clientes: { type: Array, default: () => [] },
  selection: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  clienteTieneUsuarioPortal: { type: Function, required: true }
})

defineEmits(['update:selection', 'edit-item'])
</script>
