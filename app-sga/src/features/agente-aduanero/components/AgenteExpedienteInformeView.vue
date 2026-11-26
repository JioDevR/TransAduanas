<template>
  <div class="agente-informe-view fade-in">
    <!-- Cabecera contextual limpia (sgaUI: título único en .sga-topbar) -->
    <header class="sga-form-header no-print">
      <div class="informe-header-left">
        <Button
          type="button"
          label="Volver"
          icon="pi pi-arrow-left"
          severity="secondary"
          text
          size="small"
          @click="$emit('back')"
        />

        <span v-if="expediente" class="informe-code font-mono">
          {{ expediente.codigoExpediente }}
        </span>

        <div v-if="expediente" class="informe-badges">
          <Tag
            :value="formatRegimenShort(expediente.regimenAduanero)"
            :severity="acc.isExport ? 'success' : 'info'"
            rounded
          />
          <Tag
            :value="formatEstadoAgente(expediente.estadoExpediente)"
            :severity="getExpedienteSeverity(expediente.estadoExpediente)"
            rounded
          />
          <Tag
            :value="getCanalLabel(expediente)"
            :severity="getCanalSeverity(expediente.canalSunat)"
            rounded
          />
        </div>
      </div>

      <div v-if="expediente" class="sga-form-actions">
        <template v-if="readonly">
          <Button
            v-if="!acc.isCerrado"
            type="button"
            label="Ir a Gestionar"
            icon="pi pi-sliders-h"
            size="small"
            @click="$emit('switch-to-manage', expediente)"
          />
        </template>

        <template v-else>
          <SelectButton
            v-model="activeDetailTab"
            :options="detailTabOptions"
            optionLabel="label"
            optionValue="value"
            :allowEmpty="false"
            size="small"
            aria-label="Cambiar vista del expediente"
          >
            <template #option="{ option }">
              <i :class="option.icon"></i>
              <span>{{ option.label }}</span>
            </template>
          </SelectButton>

          <Select
            v-if="switcherOptions.length > 1"
            :modelValue="expediente.idExpediente"
            :options="switcherOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            class="informe-exp-switcher"
            aria-label="Cambiar expediente abierto"
            @update:modelValue="handleSwitchExpediente"
          />
          <Button
            v-if="acc.puedeEditarTransito"
            type="button"
            label="Editar Tránsito"
            icon="pi pi-globe"
            severity="secondary"
            outlined
            size="small"
            @click="$emit('transito', expediente)"
          />
          <Button
            v-if="!acc.isCerrado"
            type="button"
            label="Marcar Estado"
            icon="pi pi-stopwatch"
            severity="secondary"
            outlined
            size="small"
            @click="$emit('marcar-estado-custom', expediente)"
          />
          <Button
            type="button"
            label="Documentos"
            icon="pi pi-paperclip"
            severity="secondary"
            outlined
            size="small"
            @click="$emit('documentos', expediente)"
          />
        </template>

        <Button
          type="button"
          icon="pi pi-print"
          severity="secondary"
          outlined
          size="small"
          title="Imprimir informe del despacho"
          aria-label="Imprimir"
          @click="imprimirInforme"
        />
      </div>
    </header>

    <!-- Workspace Split-Screen (sgaUI): separa Ficha del Expediente vs Flujo Operativo -->
    <div v-if="expediente" class="sga-split-layout">
      <div class="sga-split-main">
        <!-- VISTA A: FLUJO OPERATIVO DE HITOS (solo en Gestión cuando se selecciona esa vista) -->
        <AgenteHitosWorkflowCard
          v-if="!readonly && activeDetailTab === 'flujo'"
          class="no-print"
          :expediente="expediente"
          :readonly="false"
          @trigger-action="handleWorkflowAction"
        />

        <!-- VISTA B: FICHA E INFORMACIÓN DEL EXPEDIENTE + BITÁCORA -->
        <template v-else>
          <AgenteInformeFichaCards :expediente="expediente" />

          <!-- Bitácora Cronológica de Estados y Tiempos (sga-tracking.css) -->
          <section class="sga-card-surface sga-tracking-card">
            <div class="sga-tracking-card-head">
              <div class="sga-tracking-card-title">
                <span class="sga-tracking-card-icon"><i class="pi pi-history"></i></span>
                <div>
                  <span class="sga-kicker">MARCACIÓN DE ESTADOS Y TIEMPOS</span>
                  <h3 class="sga-tracking-card-heading">
                    Historial Operativo del Expediente ({{ orderedEventos.length }})
                  </h3>
                </div>
              </div>
              <Button
                v-if="!readonly && !acc.isCerrado"
                type="button"
                label="Registrar Hito Manual"
                icon="pi pi-plus"
                size="small"
                outlined
                class="no-print"
                @click="$emit('marcar-estado-custom', expediente)"
              />
            </div>

            <div v-if="orderedEventos.length === 0" class="sga-inline-empty">
              <i class="pi pi-clock"></i>
              <span>
                Aún no se registran eventos estructurados; {{ readonly ? 'aparecerán aquí conforme avance la operación.' : 'ejecute los hitos operativos para alimentar la bitácora y el tracking B2B.' }}
              </span>
            </div>

            <ol v-else class="sga-timeline-feed">
              <li
                v-for="(ev, idx) in orderedEventos"
                :key="ev.idEvento || `${ev.codigoEstado}-${idx}`"
                :class="['sga-timeline-event', { 'is-latest': idx === 0 }]"
              >
                <div class="sga-timeline-rail" aria-hidden="true">
                  <span class="sga-timeline-node is-info">
                    <i class="pi pi-clock"></i>
                  </span>
                </div>
                <div class="sga-timeline-content">
                  <div class="sga-timeline-top">
                    <strong class="sga-timeline-title">{{ ev.titulo }}</strong>
                    <span class="sga-timeline-time">{{ formatDateTimeSafe(ev.fechaEvento) }}</span>
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
      </div>

      <div class="sga-split-aside">
        <AgenteInformeAside :expediente="expediente" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import Button from 'primevue/button'
import Select from 'primevue/select'
import SelectButton from 'primevue/selectbutton'
import Tag from 'primevue/tag'
import AgenteHitosWorkflowCard from './informe/AgenteHitosWorkflowCard.vue'
import AgenteInformeFichaCards from './informe/AgenteInformeFichaCards.vue'
import AgenteInformeAside from './informe/AgenteInformeAside.vue'
import { getAccionesPermitidasAgente } from '../utils/agenteWorkflowUtils'
import {
  formatRegimenShort,
  formatEstadoAgente,
  getExpedienteSeverity,
  getCanalLabel,
  getCanalSeverity
} from '../utils/agenteFormatters'

const props = defineProps({
  expediente: { type: Object, default: null },
  readonly: { type: Boolean, default: false },
  openExpedientes: { type: Array, default: () => [] }
})

const emit = defineEmits([
  'back',
  'switch-to-manage',
  'select-expediente',
  'salida-origen',
  'arribo-destino',
  'transito',
  'diligencia',
  'solicitar-transporte',
  'retiro-directo',
  'ingreso-almacen',
  'recojo-exportacion',
  'ingreso-origen',
  'embarque',
  'recepcion-exterior',
  'verificacion-origen',
  'cuadrilla-rojo',
  'finalizar-rojo',
  'enviar-sunat',
  'entregado-almacen-verde',
  'informe-conformidad',
  'descargar-informe',
  'marcar-estado',
  'marcar-estado-custom',
  'finalizar-expediente',
  'documentos'
])

const activeDetailTab = ref(props.readonly ? 'ficha' : 'flujo')

const detailTabOptions = [
  { label: 'Flujo Operativo', value: 'flujo', icon: 'pi pi-sliders-h' },
  { label: 'Ficha del Expediente', value: 'ficha', icon: 'pi pi-file' }
]

watch(
  () => props.readonly,
  (isReadonly) => {
    activeDetailTab.value = isReadonly ? 'ficha' : 'flujo'
  }
)

const formatDateTimeSafe = (val) => {
  if (!val) return '—'
  return String(val).replace('T', ' ').slice(0, 16)
}

const acc = computed(() => getAccionesPermitidasAgente(props.expediente || {}))

const switcherOptions = computed(() =>
  (props.openExpedientes || []).map((e) => ({
    value: e.idExpediente,
    label: `${e.codigoExpediente} — ${e.clienteRazonSocial || 'Cliente'}`
  }))
)

const handleSwitchExpediente = (id) => {
  const found = (props.openExpedientes || []).find((e) => e.idExpediente === id)
  if (found) emit('select-expediente', found)
}

const orderedEventos = computed(() => {
  const list = props.expediente?.eventos || []
  return [...list].sort((a, b) => {
    const ta = a?.fechaEvento ? new Date(a.fechaEvento).getTime() : 0
    const tb = b?.fechaEvento ? new Date(b.fechaEvento).getTime() : 0
    if (tb !== ta) return tb - ta
    return (Number(b?.idEvento) || 0) - (Number(a?.idEvento) || 0)
  })
})

const handleWorkflowAction = ({ event, payload, expediente }) => {
  if (props.readonly || !event) return
  if (event === 'marcar-estado' && payload) {
    emit('marcar-estado', { expediente, ...payload })
    return
  }
  emit(event, expediente)
}

const imprimirInforme = () => {
  window.print()
}
</script>

<style scoped>
.agente-informe-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  width: 100%;
}

.informe-header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.informe-code {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-primary);
}

.informe-badges {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.informe-exp-switcher {
  min-width: 240px;
  max-width: 320px;
}

@media print {
  .no-print {
    display: none !important;
  }
}
</style>
