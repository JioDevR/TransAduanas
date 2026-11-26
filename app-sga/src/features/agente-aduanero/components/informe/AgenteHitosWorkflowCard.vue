<template>
  <section class="sga-card-surface hitos-workflow-card">
    <header class="hitos-head">
      <div>
        <span class="sga-kicker">CONTROL DE HITOS &amp; SINCRONIZACIÓN B2B</span>
        <h3 class="sga-chart-title">Flujo Operativo del Despacho</h3>
      </div>
      <Tag
        v-if="readonly"
        value="Solo Lectura"
        icon="pi pi-eye"
        severity="secondary"
        class="sga-mini-tag"
        rounded
      />
      <Tag
        v-else
        value="Reflejo en vivo Portal B2B"
        icon="pi pi-sync"
        severity="info"
        class="sga-mini-tag"
        rounded
      />
    </header>

    <!-- Banner de Siguiente Hito Requerido -->
    <div
      :class="[
        'sga-banner next-hito-banner',
        nextAction.isCerrado
          ? 'is-success'
          : (nextAction.isWaiting ? 'is-warn' : 'is-info')
      ]"
    >
      <i :class="[nextAction.icon || 'pi pi-flag', 'next-hito-icon']"></i>
      <div class="next-hito-body">
        <span class="sga-micro-label">
          {{ nextAction.isCerrado ? 'CICLO COMPLETADO' : (nextAction.isWaiting ? 'EN ESPERA DE GESTIÓN ADUANERA' : 'SIGUIENTE HITO REQUERIDO') }}
        </span>
        <strong class="next-hito-title">{{ nextAction.hitoTitle }}</strong>
        <p class="next-hito-desc">
          {{ nextAction.waitingText || nextAction.b2bEffect }}
        </p>
      </div>
      <Button
        v-if="!readonly && nextAction.event"
        type="button"
        :label="nextAction.label"
        :icon="nextAction.icon"
        :severity="nextAction.severity === 'primary' ? undefined : nextAction.severity"
        size="small"
        @click="triggerAction(nextAction)"
      />
    </div>

    <!-- Lista Secuencial de Hitos -->
    <ol class="hitos-checklist">
      <li
        v-for="hito in hitos"
        :key="hito.id"
        :class="['hito-row', `is-${hito.status}`]"
      >
        <div class="hito-rail" aria-hidden="true">
          <span class="hito-circle">
            <i v-if="hito.status === 'completed'" class="pi pi-check"></i>
            <i v-else-if="hito.status === 'waiting'" class="pi pi-clock"></i>
            <span v-else>{{ hito.num }}</span>
          </span>
        </div>

        <div class="hito-content">
          <div class="hito-top">
            <div class="hito-title-group">
              <span class="hito-kicker">{{ hito.kicker }}</span>
              <h4 class="hito-title">{{ hito.title }}</h4>
            </div>

            <div class="hito-status-meta">
              <span v-if="hito.date" class="hito-date font-mono">
                {{ formatDateSafe(hito.date) }}
              </span>
              <Tag
                :value="statusLabel(hito.status)"
                :severity="statusSeverity(hito.status)"
                class="sga-mini-tag"
                rounded
              />
            </div>
          </div>

          <p class="hito-desc">{{ hito.description }}</p>

          <div class="hito-footer">
            <span class="hito-b2b-hint">
              <i class="pi pi-eye"></i>
              <span>{{ hito.b2bEffect }}</span>
            </span>

            <div
              v-if="!readonly && (hito.action || (hito.secondaryActions && hito.secondaryActions.length > 0))"
              class="hito-actions"
            >
              <Button
                v-if="hito.action"
                type="button"
                :label="hito.action.label"
                :icon="hito.action.icon"
                :severity="hito.action.severity === 'primary' ? undefined : hito.action.severity"
                size="small"
                @click="triggerAction(hito.action)"
              />
              <Button
                v-for="sec in hito.secondaryActions"
                :key="sec.event + (sec.label || '')"
                type="button"
                :label="sec.label"
                :icon="sec.icon"
                :severity="sec.severity || 'secondary'"
                outlined
                size="small"
                @click="triggerAction(sec)"
              />
            </div>
          </div>
        </div>
      </li>
    </ol>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import {
  buildAgenteChecklistHitos,
  getSiguienteAccionAgente
} from '../../utils/agenteWorkflowUtils'

const props = defineProps({
  expediente: { type: Object, required: true },
  readonly: { type: Boolean, default: false }
})

const emit = defineEmits(['trigger-action'])

const hitos = computed(() => buildAgenteChecklistHitos(props.expediente))
const nextAction = computed(() => getSiguienteAccionAgente(props.expediente))

const triggerAction = (actionObj) => {
  if (!actionObj?.event) return
  emit('trigger-action', {
    event: actionObj.event,
    payload: actionObj.payload || null,
    expediente: props.expediente
  })
}

const statusLabel = (status) => {
  if (status === 'completed') return 'Completado'
  if (status === 'current') return 'En curso'
  if (status === 'waiting') return 'En espera'
  return 'Pendiente'
}

const statusSeverity = (status) => {
  if (status === 'completed') return 'success'
  if (status === 'current') return 'info'
  if (status === 'waiting') return 'warn'
  return 'secondary'
}

const formatDateSafe = (val) => {
  if (!val) return ''
  return String(val).replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.hitos-workflow-card {
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
}

.hitos-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.next-hito-banner {
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
}

.next-hito-icon {
  font-size: 1.15rem;
  flex-shrink: 0;
}

.next-hito-body {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  flex: 1 1 260px;
  min-width: 0;
}

.next-hito-title {
  font-size: 0.92rem;
  font-weight: 700;
}

.next-hito-desc {
  margin: 0;
  font-size: 0.78rem;
  opacity: 0.9;
}

.hitos-checklist {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.hito-row {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  gap: 0.85rem;
  padding-bottom: 0.95rem;
}

.hito-row:last-child {
  padding-bottom: 0;
}

.hito-rail {
  position: relative;
  display: flex;
  justify-content: center;
}

.hito-row:not(:last-child) .hito-rail::after {
  content: '';
  position: absolute;
  top: 30px;
  bottom: -4px;
  width: 2px;
  background: var(--color-border);
}

.hito-row.is-completed:not(:last-child) .hito-rail::after {
  background: var(--color-success-text);
}

.hito-circle {
  width: 30px;
  height: 30px;
  border-radius: 9999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-mono);
  font-size: 0.75rem;
  font-weight: 700;
  background: var(--color-bg-subtle);
  border: 2px solid var(--color-border-strong);
  color: var(--color-text-subtle);
  position: relative;
  z-index: 1;
}

.hito-row.is-completed .hito-circle {
  background: var(--color-success-bg);
  border-color: var(--color-success-text);
  color: var(--color-success-text);
}

.hito-row.is-current .hito-circle {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-primary-text);
  box-shadow: 0 0 0 4px rgba(212, 130, 82, 0.2);
}

.hito-row.is-waiting .hito-circle {
  background: var(--color-warning-bg);
  border-color: var(--color-warning-border);
  color: var(--color-warning-text);
}

.hito-content {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  padding: 0.75rem 0.95rem;
  border-radius: var(--radius-md, 8px);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  min-width: 0;
}

.hito-row.is-current .hito-content {
  background: var(--color-bg-surface);
  border-color: var(--color-primary);
}

.hito-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.hito-title-group {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
  min-width: 0;
}

.hito-kicker {
  font-size: 0.625rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--color-accent-kicker);
}

.hito-title {
  margin: 0;
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.hito-status-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.hito-date {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

.hito-desc {
  margin: 0;
  font-size: 0.8rem;
  line-height: 1.45;
  color: var(--color-text-muted);
}

.hito-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
  padding-top: 0.35rem;
  border-top: 1px dashed var(--color-border-subtle);
}

.hito-b2b-hint {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.73rem;
  color: var(--color-text-subtle);
}

.hito-actions {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  flex-wrap: wrap;
}
</style>
