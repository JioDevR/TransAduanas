<template>
  <div class="sga-canvas-wrapper">
    <div v-if="loading" class="sga-canvas-state">
      <i class="pi pi-spin pi-spinner sga-state-icon"></i>
      <span>Cargando padrón de conductores...</span>
    </div>

    <div v-else-if="!conductores || conductores.length === 0" class="sga-canvas-state">
      <i class="pi pi-users sga-state-icon"></i>
      <p class="sga-state-title">No hay conductores registrados</p>
      <p class="sga-state-desc">Registre choferes profesionales con licencia MTC vigente.</p>
    </div>

    <div v-else class="sga-canvas-grid">
      <article
        v-for="c in conductores"
        :key="c.idConductor"
        :class="['sga-canvas-card', { 'is-selected': selection?.idConductor === c.idConductor }]"
        @click="$emit('update:selection', c)"
        @dblclick="$emit('editar', c)"
      >
        <header class="sga-card-top">
          <span class="sga-card-code font-mono">{{ c.numeroLicencia }}</span>
          <Tag
            :value="c.estadoActivo ? formatEstadoConductor(c.estadoOperativo) : 'Inactivo'"
            :severity="c.estadoActivo ? getEstadoConductorSeverity(c.estadoOperativo) : 'secondary'"
            rounded
          />
        </header>

        <div class="sga-card-client">
          <h4 class="sga-client-title">{{ c.nombreCompleto || `${c.nombres} ${c.apellidos}` }}</h4>
          <div class="sga-client-meta">
            <span class="font-mono">DNI: {{ c.numeroDocumento || '—' }}</span>
            <span>· Cat. {{ c.tipoLicencia }}</span>
          </div>
        </div>

        <div class="sga-card-meta-grid">
          <div class="sga-meta-item">
            <span class="sga-meta-label">VENC. LICENCIA</span>
            <span class="sga-meta-val font-mono">{{ formatDate(c.fechaVencimientoLicencia) }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">TELÉFONO</span>
            <span class="sga-meta-val font-mono">{{ c.telefono || '—' }}</span>
          </div>
        </div>

        <footer class="sga-card-footer">
          <div class="sga-footer-left">
            <span class="sga-cell-sub">{{ c.observaciones ? 'Con notas técnicas' : 'Padrón MTC' }}</span>
          </div>
          <Button
            type="button"
            label="Editar"
            icon="pi pi-pencil"
            severity="secondary"
            outlined
            size="small"
            @click.stop="$emit('editar', c)"
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
  formatEstadoConductor,
  getEstadoConductorSeverity
} from '../utils/transporteFormatters'

defineProps({
  conductores: { type: Array, default: () => [] },
  selection: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})

defineEmits(['update:selection', 'editar'])
</script>
