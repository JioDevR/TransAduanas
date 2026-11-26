<template>
  <div class="agente-informe-view fade-in">
    <!-- Cabecera contextual (sgaUI: título único en .sga-topbar) -->
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
        <template v-if="!acc.isExport">
          <Button
            v-if="acc.puedeVerificacionOrigen"
            type="button"
            label="Verificación Origen"
            icon="pi pi-check-square"
            severity="info"
            outlined
            size="small"
            @click="$emit('verificacion-origen', expediente)"
          />
          <Button
            v-if="acc.puedeSalidaOrigen"
            type="button"
            label="Salida Origen"
            icon="pi pi-send"
            size="small"
            @click="$emit('salida-origen', expediente)"
          />
          <Button
            v-if="acc.puedeConfirmarArribo"
            type="button"
            label="Confirmar Arribo"
            icon="pi pi-compass"
            severity="success"
            size="small"
            @click="$emit('arribo-destino', expediente)"
          />
          <Button
            v-if="acc.puedeCuadrillaCitaRojo"
            type="button"
            label="Cuadrilla / Cita Rojo"
            icon="pi pi-users"
            severity="danger"
            outlined
            size="small"
            @click="$emit('cuadrilla-rojo', expediente)"
          />
          <Button
            v-if="acc.puedeFinalizarRojo"
            type="button"
            label="Finalizar Revisión Rojo"
            icon="pi pi-check-circle"
            severity="danger"
            size="small"
            @click="$emit('finalizar-rojo', expediente)"
          />
          <Button
            v-if="acc.puedeEnviarSunatNaranja"
            type="button"
            label="Enviar a SUNAT (Naranja)"
            icon="pi pi-cloud-upload"
            severity="warn"
            size="small"
            @click="$emit('enviar-sunat', expediente)"
          />
          <Button
            v-if="acc.puedeEntregadoAlmacenVerde"
            type="button"
            label="Entregado Almacén (Verde)"
            icon="pi pi-check-square"
            severity="success"
            outlined
            size="small"
            @click="$emit('entregado-almacen-verde', expediente)"
          />
          <Button
            v-if="acc.puedeDiligencia"
            type="button"
            label="Registrar Diligencia"
            icon="pi pi-search-plus"
            severity="warn"
            outlined
            size="small"
            @click="$emit('diligencia', expediente)"
          />
          <Button
            v-if="acc.puedeSolicitarTransporte"
            type="button"
            label="Solicitar Flota"
            icon="pi pi-truck"
            severity="success"
            size="small"
            @click="$emit('solicitar-transporte', expediente)"
          />
          <Button
            v-if="acc.puedeRetiroDirecto"
            type="button"
            label="Acta Retiro Directo"
            icon="pi pi-check"
            severity="secondary"
            outlined
            size="small"
            @click="$emit('retiro-directo', expediente)"
          />
          <Button
            v-if="acc.puedeInformeConformidad"
            type="button"
            label="Informe Conformidad"
            icon="pi pi-file-check"
            severity="info"
            size="small"
            @click="$emit('informe-conformidad', expediente)"
          />
          <Button
            v-if="acc.tieneInformeConformidad"
            type="button"
            label="PDF Conformidad"
            icon="pi pi-file-pdf"
            severity="secondary"
            outlined
            size="small"
            @click="$emit('descargar-informe', expediente)"
          />
          <Button
            v-if="acc.puedeIngresoAlmacen"
            type="button"
            label="Balanza VGM"
            icon="pi pi-box"
            severity="secondary"
            outlined
            size="small"
            @click="$emit('ingreso-almacen', expediente)"
          />
        </template>

        <template v-else>
          <Button
            v-if="acc.puedeRecojoExportacion"
            type="button"
            label="Solicitar Recojo"
            icon="pi pi-truck"
            size="small"
            @click="$emit('recojo-exportacion', expediente)"
          />
          <Button
            v-if="acc.puedeIngresoOrigen"
            type="button"
            label="Ingreso Almacén Central"
            icon="pi pi-inbox"
            severity="secondary"
            outlined
            size="small"
            @click="$emit('ingreso-origen', expediente)"
          />
          <Button
            v-if="acc.puedeMarcarRevisionAduana"
            type="button"
            label="Revisión Aduana"
            icon="pi pi-search"
            severity="info"
            outlined
            size="small"
            @click="$emit('marcar-estado', { expediente, codigoEstado: 'REVISION_ADUANA', titulo: 'Revisión Aduana' })"
          />
          <Button
            v-if="acc.puedeMarcarEsperaTransito"
            type="button"
            label="Espera de Tránsito"
            icon="pi pi-clock"
            severity="warn"
            outlined
            size="small"
            @click="$emit('marcar-estado', { expediente, codigoEstado: 'ESPERA_TRANSITO', titulo: 'En Espera de Tránsito' })"
          />
          <Button
            v-if="acc.puedeDiligencia"
            type="button"
            label="Registrar Diligencia"
            icon="pi pi-search-plus"
            severity="warn"
            size="small"
            @click="$emit('diligencia', expediente)"
          />
          <Button
            v-if="acc.puedeEmbarque"
            type="button"
            label="Vuelo / Embarque Confirmado"
            icon="pi pi-send"
            severity="success"
            size="small"
            @click="$emit('embarque', expediente)"
          />
          <Button
            v-else-if="acc.puedeRecepcionExterior"
            type="button"
            label="Conformidad Destino"
            icon="pi pi-check-circle"
            severity="success"
            size="small"
            @click="$emit('recepcion-exterior', expediente)"
          />
        </template>

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
          v-if="acc.puedeFinalizarExpediente"
          type="button"
          label="Finalizar Expediente"
          icon="pi pi-lock"
          severity="success"
          size="small"
          @click="$emit('finalizar-expediente', expediente)"
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
          type="button"
          label="Documentos"
          icon="pi pi-paperclip"
          severity="secondary"
          outlined
          size="small"
          @click="$emit('documentos', expediente)"
        />
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

    <!-- Stepper horizontal de trazabilidad (sga-stepper-bar) -->
    <div v-if="expediente" class="sga-stepper-bar informe-stepper-6">
      <div
        v-for="step in trazabilidadSteps"
        :key="step.num"
        :class="['sga-stepper-item', 'is-static', { 'is-completed': step.done, 'is-active': step.active }]"
      >
        <span class="sga-step-circle">
          <i v-if="step.done" class="pi pi-check"></i>
          <span v-else>{{ step.num }}</span>
        </span>
        <span class="sga-step-meta">
          <span class="sga-step-kicker">{{ step.kicker }}</span>
          <span class="sga-step-title">{{ step.title }}</span>
        </span>
      </div>
    </div>

    <!-- Workspace Split-Screen (sga-split-layout) -->
    <div v-if="expediente" class="sga-split-layout">
      <div class="sga-split-main">
        <AgenteInformeFichaCards :expediente="expediente" />

        <!-- Bitácora Cronológica de Estados y Tiempos -->
        <section class="sga-card-surface" style="margin-top: 1rem;">
          <div class="sga-table-toolbar">
            <div>
              <span class="sga-kicker">MARCACIÓN DE ESTADOS Y TIEMPOS</span>
              <h3 class="sga-chart-title">Historial Operativo del Expediente ({{ (expediente.eventos || []).length }})</h3>
            </div>
            <Button
              v-if="!acc.isCerrado"
              type="button"
              label="Registrar Hito Manual"
              icon="pi pi-plus"
              size="small"
              outlined
              @click="$emit('marcar-estado-custom', expediente)"
            />
          </div>

          <div v-if="!expediente.eventos || expediente.eventos.length === 0" class="sga-cell-sub">
            Aún no se registran eventos estructurados; utilice los botones superiores para marcar estados y tiempos.
          </div>

          <ul v-else class="eventos-list">
            <li v-for="ev in expediente.eventos" :key="ev.idEvento" class="evento-item">
              <div>
                <strong>{{ ev.titulo }}</strong>
                <div class="sga-cell-sub">{{ ev.descripcion || ev.codigoEstado }}</div>
              </div>
              <div class="evento-meta">
                <span class="font-mono">{{ formatDateTimeSafe(ev.fechaEvento) }}</span>
                <span class="sga-cell-sub">{{ ev.usuarioNombre || 'Agente' }}</span>
              </div>
            </li>
          </ul>
        </section>
      </div>

      <div class="sga-split-aside">
        <AgenteInformeAside :expediente="expediente" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import AgenteInformeFichaCards from './informe/AgenteInformeFichaCards.vue'
import AgenteInformeAside from './informe/AgenteInformeAside.vue'
import {
  getAccionesPermitidasAgente,
  hasZarpeConfirmado,
  hasArriboConfirmado,
  hasCanalAsignado,
  isDiligenciaCompletada,
  isLevanteAutorizado
} from '../utils/agenteWorkflowUtils'
import {
  formatRegimenShort,
  formatEstadoAgente,
  getExpedienteSeverity,
  getCanalLabel,
  getCanalSeverity
} from '../utils/agenteFormatters'

const props = defineProps({
  expediente: { type: Object, default: null }
})

defineEmits([
  'back',
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

const formatDateTimeSafe = (val) => {
  if (!val) return '—'
  return String(val).replace('T', ' ').slice(0, 16)
}

const acc = computed(() => getAccionesPermitidasAgente(props.expediente || {}))

const trazabilidadSteps = computed(() => {
  const exp = props.expediente
  if (!exp) return []

  const zarpe = hasZarpeConfirmado(exp)
  const arribo = hasArriboConfirmado(exp)
  const canal = hasCanalAsignado(exp)
  const diligencia = isDiligenciaCompletada(exp)
  const levante = isLevanteAutorizado(exp)

  return [
    { num: '01', kicker: 'ASIGNACIÓN', title: 'Asignado', done: true, active: !zarpe },
    { num: '02', kicker: 'ORIGEN', title: acc.value.isExport ? 'En Tránsito' : 'Salida Origen', done: zarpe, active: !zarpe },
    { num: '03', kicker: 'ADUANA', title: acc.value.isExport ? 'Zona Primaria' : 'Arribo Destino', done: arribo, active: zarpe && !arribo },
    { num: '04', kicker: 'SUNAT', title: canal ? `Canal ${exp.canalSunat}` : 'Canal SUNAT', done: canal, active: arribo && !canal },
    { num: '05', kicker: 'AFORO', title: exp.canalSunat === 'VERDE' ? 'Exento (Verde)' : 'Diligencia', done: diligencia, active: canal && !diligencia },
    { num: '06', kicker: 'LIBERACIÓN', title: 'Levante', done: levante, active: diligencia && !levante }
  ]
})

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

.informe-stepper-6 {
  grid-template-columns: repeat(6, minmax(115px, 1fr));
}

.sga-stepper-item.is-static {
  cursor: default;
}

.sga-stepper-item.is-static:hover {
  background: transparent;
}

.eventos-list {
  list-style: none;
  margin: 0.75rem 0 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.evento-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  padding: 0.65rem 0.8rem;
  border-radius: 8px;
  border: 1px solid var(--color-border);
  background: var(--color-bg-subtle);
}

.evento-meta {
  text-align: right;
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  font-size: 0.78rem;
  white-space: nowrap;
}

@media (max-width: 1100px) {
  .informe-stepper-6 {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .informe-stepper-6 {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media print {
  .no-print {
    display: none !important;
  }
}
</style>
