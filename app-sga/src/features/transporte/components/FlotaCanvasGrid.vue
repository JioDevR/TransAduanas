<template>
  <div class="sga-canvas-wrapper">
    <div v-if="loading" class="sga-canvas-state">
      <i class="pi pi-spin pi-spinner sga-state-icon"></i>
      <span>Cargando parque automotor...</span>
    </div>

    <div v-else-if="!unidades || unidades.length === 0" class="sga-canvas-state">
      <i class="pi pi-truck sga-state-icon"></i>
      <p class="sga-state-title">No hay unidades registradas</p>
      <p class="sga-state-desc">Registre un tracto, plataforma, furgón o camión baranda para operar despachos.</p>
    </div>

    <div v-else class="sga-canvas-grid">
      <article
        v-for="u in unidades"
        :key="u.idUnidad"
        :class="['sga-canvas-card', { 'is-selected': selection?.idUnidad === u.idUnidad }]"
        @click="$emit('update:selection', u)"
        @dblclick="$emit('editar', u)"
      >
        <header class="sga-card-top">
          <span class="sga-card-code font-mono">{{ u.placaTracto }}</span>
          <Tag
            :value="u.estadoActivo ? formatEstadoFlota(u.estadoOperativo) : 'Inactiva'"
            :severity="u.estadoActivo ? getEstadoFlotaSeverity(u.estadoOperativo) : 'secondary'"
            rounded
          />
        </header>

        <div class="sga-card-client">
          <h4 class="sga-client-title" :title="u.marcaModelo">{{ u.marcaModelo }}</h4>
          <div class="sga-client-meta">
            <span>{{ formatTipoVehiculo(u.tipoVehiculo) }}</span>
            <span v-if="u.placaCarreta" class="font-mono">· Carreta {{ u.placaCarreta }}</span>
          </div>
        </div>

        <div class="sga-card-meta-grid">
          <div class="sga-meta-item">
            <span class="sga-meta-label">CAPACIDAD</span>
            <span class="sga-meta-val font-mono">{{ formatPesoKg(u.capacidadCargaKg) }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">ODÓMETRO</span>
            <span class="sga-meta-val font-mono">{{ formatKilometraje(u.kilometrajeActual) }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">SOAT</span>
            <span class="sga-meta-val font-mono">{{ formatDate(u.soatVencimiento) }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">CITV</span>
            <span class="sga-meta-val font-mono">{{ formatDate(u.citvVencimiento) }}</span>
          </div>
        </div>

        <footer class="sga-card-footer">
          <div class="sga-footer-left">
            <span class="sga-cell-sub font-mono">Año: {{ u.anoFabricacion || '—' }}</span>
          </div>
          <Button
            type="button"
            label="Editar"
            icon="pi pi-pencil"
            severity="secondary"
            outlined
            size="small"
            @click.stop="$emit('editar', u)"
          />
        </footer>
      </article>
    </div>
  </div>
</template>

<script setup>
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import {
  formatDate,
  formatTipoVehiculo,
  formatEstadoFlota,
  getEstadoFlotaSeverity,
  formatPesoKg,
  formatKilometraje
} from '../utils/transporteFormatters'

defineProps({
  unidades: { type: Array, default: () => [] },
  selection: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})

defineEmits(['update:selection', 'editar'])
</script>
