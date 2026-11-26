<template>
  <section class="sga-card-surface sga-tracking-card">
    <header class="sga-tracking-card-head">
      <div class="sga-tracking-card-title">
        <span class="sga-tracking-card-icon"><i class="pi pi-history"></i></span>
        <div>
          <span class="sga-kicker">BITÁCORA EN TIEMPO REAL</span>
          <h3 class="sga-tracking-card-heading">Historial Cronológico de Seguimiento</h3>
        </div>
      </div>

      <Tag
        :value="`${orderedEvents.length} evento(s)`"
        severity="secondary"
        class="sga-mono"
        rounded
      />
    </header>

    <div v-if="orderedEvents.length === 0" class="sga-inline-empty">
      <i class="pi pi-clock"></i>
      <span>
        Aún no se registran eventos detallados en la bitácora operativa. Conforme el agente aduanero avance con el despacho, los hitos aparecerán aquí en orden cronológico.
      </span>
    </div>

    <ol v-else class="sga-timeline-feed">
      <li
        v-for="(ev, idx) in orderedEvents"
        :key="ev.idEvento || `${ev.codigoEstado}-${idx}`"
        :class="['sga-timeline-event', { 'is-latest': idx === 0 }]"
      >
        <div class="sga-timeline-rail" aria-hidden="true">
          <span :class="['sga-timeline-node', `is-${ev.toneMeta.tone}`]">
            <i :class="ev.toneMeta.icon"></i>
          </span>
        </div>

        <div class="sga-timeline-content">
          <div class="sga-timeline-top">
            <strong class="sga-timeline-title">{{ ev.titulo }}</strong>
            <span class="sga-timeline-time">{{ formatDateTime(ev.fechaEvento) }}</span>
          </div>
          <p v-if="ev.descripcion" class="sga-timeline-desc">{{ ev.descripcion }}</p>
          <span class="sga-timeline-actor">
            <i class="pi pi-user"></i>
            <span>{{ ev.usuarioNombre || 'Agente SGA' }}</span>
          </span>
        </div>
      </li>
    </ol>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import Tag from 'primevue/tag'
import { formatDateTime } from '@/shared/utils/formatters'
import { sortTrackingEvents } from '../../utils/expedienteTrackingUtils'

const props = defineProps({
  eventos: { type: Array, default: () => [] }
})

const orderedEvents = computed(() => sortTrackingEvents(props.eventos))
</script>
