<template>
  <div class="sga-canvas-wrapper">
    <div v-if="loading" class="sga-canvas-state">
      <i class="pi pi-spin pi-spinner sga-state-icon"></i>
      <span>Cargando expedientes aduaneros...</span>
    </div>

    <div v-else-if="!expedientes || expedientes.length === 0" class="sga-canvas-state">
      <i class="pi pi-box sga-state-icon"></i>
      <p class="sga-state-title">No se encontraron expedientes</p>
      <p class="sga-state-desc">
        Sus cotizaciones aprobadas con pago validado aparecerán aquí en cuanto se aperture la carpeta aduanera.
      </p>
    </div>

    <div v-else class="sga-canvas-grid">
      <article
        v-for="exp in expedientes"
        :key="exp.idExpediente"
        class="sga-canvas-card"
        tabindex="0"
        role="button"
        @click="$emit('open-detail', exp)"
        @keydown.enter.prevent="$emit('open-detail', exp)"
        @keydown.space.prevent="$emit('open-detail', exp)"
      >
        <header class="sga-card-top">
          <span class="sga-card-code sga-mono">{{ exp.codigoExpediente }}</span>
          <Tag
            v-if="exp.estadoExpediente === 'CERRADO'"
            value="CERRADO"
            severity="success"
            rounded
          />
          <Tag
            v-else
            :value="getCanalMeta(exp.canalSunat).label"
            :severity="getCanalMeta(exp.canalSunat).severity"
            rounded
          />
        </header>

        <div class="sga-card-client">
          <h4 class="sga-client-title" :title="exp.nombreMercancia">
            {{ exp.nombreMercancia || 'Carga General' }}
          </h4>
          <div class="sga-client-meta">
            <Tag
              :value="isExportExpediente(exp) ? 'Exportación' : 'Importación'"
              severity="secondary"
              class="sga-mini-tag"
            />
            <Tag
              :value="getExpedientePhase(exp).label"
              :severity="getExpedientePhase(exp).severity"
              class="sga-mini-tag"
            />
          </div>
        </div>

        <div class="sga-card-meta-grid">
          <div class="sga-meta-item">
            <span class="sga-meta-label">DAM SUNAT</span>
            <span class="sga-meta-val sga-mono">{{ exp.numeroDam || 'En trámite' }}</span>
          </div>

          <div class="sga-meta-item">
            <span class="sga-meta-label">APERTURA</span>
            <span class="sga-meta-val sga-mono">{{ formatDate(exp.fechaApertura) }}</span>
          </div>

          <div v-if="exp.naveViaje" class="sga-meta-item">
            <span class="sga-meta-label">NAVE / VIAJE</span>
            <span class="sga-meta-val" :title="exp.naveViaje">{{ exp.naveViaje }}</span>
          </div>

          <div v-if="exp.numeroContenedor" class="sga-meta-item">
            <span class="sga-meta-label">CONTENEDOR</span>
            <span class="sga-meta-val sga-mono">{{ exp.numeroContenedor }}</span>
          </div>
        </div>

        <div
          v-if="exp.tieneAlmacen && exp.diasRestantesAlmacen != null && exp.estadoExpediente !== 'CERRADO'"
          :class="['sga-banner exp-almacen-banner', getAlmacenAlertClass(exp.diasRestantesAlmacen)]"
        >
          <i class="pi pi-clock"></i>
          <span>{{ exp.diasRestantesAlmacen }} días libres de sobreestadía</span>
        </div>

        <footer class="sga-card-footer">
          <div class="sga-footer-left">
            <span class="sga-kicker">
              {{ exp.estadoExpediente === 'CERRADO' ? 'CICLO' : 'AGENTE' }}
            </span>
            <span class="footer-sub sga-mono">
              {{
                exp.estadoExpediente === 'CERRADO'
                  ? (calculateLeadTime(exp) || 'Archivo conforme')
                  : (exp.agenteAduaneroNombre || 'Por asignar')
              }}
            </span>
          </div>

          <Button
            type="button"
            :label="exp.estadoExpediente === 'CERRADO' ? 'Historial' : 'Ver Tracking'"
            icon="pi pi-arrow-right"
            iconPos="right"
            severity="secondary"
            outlined
            size="small"
            @click.stop="$emit('open-detail', exp)"
          />
        </footer>
      </article>
    </div>
  </div>
</template>

<script setup>
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { formatDate } from '@/shared/utils/formatters'
import {
  isExportExpediente,
  getExpedientePhase,
  getCanalMeta,
  getAlmacenAlertClass,
  calculateLeadTime
} from '../utils/expedienteTrackingUtils'

defineProps({
  expedientes: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

defineEmits(['open-detail'])
</script>

<style scoped>
.exp-almacen-banner {
  padding: 0.45rem 0.7rem;
  font-size: 0.75rem;
  font-weight: 600;
  align-items: center;
}

.footer-sub {
  font-size: 0.76rem;
  font-weight: 600;
  color: var(--color-text-muted);
}
</style>
