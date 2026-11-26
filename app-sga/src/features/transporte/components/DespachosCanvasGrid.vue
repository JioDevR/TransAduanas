<template>
  <div class="sga-canvas-wrapper">
    <div v-if="loading" class="sga-canvas-state">
      <i class="pi pi-spin pi-spinner sga-state-icon"></i>
      <span>Cargando despachos de transporte...</span>
    </div>

    <div v-else-if="!despachos || despachos.length === 0" class="sga-canvas-state">
      <i class="pi pi-truck sga-state-icon"></i>
      <p class="sga-state-title">No hay despachos para mostrar</p>
      <p class="sga-state-desc">Programe un nuevo despacho terrestre o ajuste los filtros de búsqueda.</p>
    </div>

    <div v-else class="sga-canvas-grid">
      <article
        v-for="d in despachos"
        :key="d.idDespachoTransporte"
        :class="['sga-canvas-card', { 'is-selected': selection?.idDespachoTransporte === d.idDespachoTransporte }]"
        @click="$emit('update:selection', d)"
        @dblclick="$emit('detalle', d)"
      >
        <header class="sga-card-top">
          <span class="sga-card-code font-mono">{{ d.codigoTracking }}</span>
          <Tag
            :value="formatEstadoDespacho(d.estadoTransporte)"
            :severity="getEstadoDespachoSeverity(d.estadoTransporte)"
            rounded
          />
        </header>

        <div class="sga-card-client">
          <h4 class="sga-client-title" :title="d.clienteRazonSocial">
            {{ d.clienteRazonSocial || 'Cliente no asignado' }}
          </h4>
          <div class="sga-client-meta">
            <span class="sga-client-doc font-mono">{{ d.codigoExpediente || 'EXP-SGA' }}</span>
            <span class="sga-client-doc font-mono">· {{ formatTipoTraslado(d.tipoTraslado) }}</span>
          </div>
        </div>

        <div class="sga-card-meta-grid">
          <div class="sga-meta-item">
            <span class="sga-meta-label">UNIDAD</span>
            <span class="sga-meta-val font-mono">{{ d.unidadPlacaTracto || 'Sin asignar' }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">CONDUCTOR</span>
            <span class="sga-meta-val">{{ d.conductorNombre || 'Sin asignar' }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">DESTINO</span>
            <span class="sga-meta-val">{{ d.distritoLlegada || '—' }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">PROGRAMACIÓN</span>
            <span class="sga-meta-val font-mono">{{ formatDate(d.fechaProgramada) }}</span>
          </div>
        </div>

        <footer class="sga-card-footer">
          <div class="sga-footer-left">
            <span class="sga-cell-sub font-mono">GRE-T: {{ d.guiaRemisionTransporte || 'Pendiente' }}</span>
          </div>
          <Button
            v-if="d.estadoTransporte === 'PENDIENTE_ASIGNACION'"
            type="button"
            label="Asignar"
            icon="pi pi-user-plus"
            severity="warn"
            size="small"
            @click.stop="$emit('asignar', d)"
          />
          <Button
            v-else-if="d.estadoTransporte === 'PROGRAMADO'"
            type="button"
            label="Iniciar Ruta"
            icon="pi pi-send"
            size="small"
            @click.stop="$emit('iniciar-ruta', d)"
          />
          <Button
            v-else-if="d.estadoTransporte === 'EN_RUTA'"
            type="button"
            label="Entregar"
            icon="pi pi-check"
            severity="success"
            size="small"
            @click.stop="$emit('entregar', d)"
          />
          <Button
            v-else
            type="button"
            label="Detalle"
            icon="pi pi-eye"
            severity="secondary"
            outlined
            size="small"
            @click.stop="$emit('detalle', d)"
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
  formatEstadoDespacho,
  getEstadoDespachoSeverity,
  formatTipoTraslado
} from '../utils/transporteFormatters'

defineProps({
  despachos: { type: Array, default: () => [] },
  selection: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})

defineEmits(['update:selection', 'detalle', 'asignar', 'iniciar-ruta', 'entregar'])
</script>
