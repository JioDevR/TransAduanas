<template>
  <div class="ti-tab-stack">
    <section v-if="isAdminTi" class="sga-card-surface sga-filters-card">
      <div class="sga-filters-grid">
        <div class="sga-filter-field">
          <label class="sga-field-label">Ventana de Actividad</label>
          <Select
            :modelValue="filterDias"
            :options="diasOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
            @update:modelValue="$emit('update:filterDias', $event)"
            @change="$emit('filter-change')"
          />
        </div>

        <div class="sga-filter-field">
          <label class="sga-field-label">Módulo Auditado</label>
          <Select
            :modelValue="filterModulo"
            :options="moduloOptions"
            optionLabel="label"
            optionValue="value"
            size="small"
            fluid
            @update:modelValue="$emit('update:filterModulo', $event)"
            @change="$emit('filter-change')"
          />
        </div>

        <div v-if="hasActiveFilters" class="sga-filter-reset">
          <Button
            label="Restablecer"
            icon="pi pi-filter-slash"
            severity="secondary"
            text
            size="small"
            @click="$emit('reset-filters')"
          />
        </div>
      </div>
    </section>

    <section class="sga-charts-grid">
      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">CONTROL DE ACCESOS</span>
            <h3 class="sga-chart-title">Operadores por Rol Institucional</h3>
          </div>
          <Button
            label="Gestionar"
            icon="pi pi-arrow-right"
            iconPos="right"
            severity="secondary"
            text
            size="small"
            @click="$emit('navigate', 'users')"
          />
        </div>
        <div class="sga-chart-box sga-doughnut-wrap">
          <Chart
            v-if="rolesChartData.labels.length > 0"
            type="doughnut"
            :data="rolesChartData"
            :options="doughnutOptions"
            class="sga-chart-canvas"
          />
          <div v-else class="sga-empty-state">
            <i class="pi pi-users sga-empty-icon"></i>
            <p class="sga-empty-desc">Sin operadores registrados</p>
          </div>
        </div>
      </article>

      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">SEGURIDAD &amp; AUDITORÍA</span>
            <h3 class="sga-chart-title">Eventos por Día ({{ filterDias }} días)</h3>
          </div>
          <Button
            v-if="isAdminTi"
            label="Ver Bitácora"
            icon="pi pi-arrow-right"
            iconPos="right"
            severity="secondary"
            text
            size="small"
            @click="$emit('navigate', 'auditoria')"
          />
        </div>
        <div class="sga-chart-box">
          <Chart
            v-if="actividadChartData.labels.length > 0"
            type="bar"
            :data="actividadChartData"
            :options="barOptions"
            class="sga-chart-canvas"
          />
          <div v-else class="sga-empty-state">
            <i class="pi pi-chart-bar sga-empty-icon"></i>
            <p class="sga-empty-desc">Sin actividad registrada en el período</p>
          </div>
        </div>
      </article>
    </section>

    <section class="sga-charts-grid">
      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">TRAZABILIDAD EN VIVO</span>
            <h3 class="sga-chart-title">Últimos Eventos de Auditoría</h3>
          </div>
          <Button
            v-if="isAdminTi"
            label="Explorar todo"
            severity="secondary"
            outlined
            size="small"
            @click="$emit('navigate', 'auditoria')"
          />
        </div>

        <DataTable
          :value="eventosRecientes"
          size="small"
          stripedRows
          responsiveLayout="scroll"
        >
          <template #empty>
            <div class="sga-empty-state">
              <i class="pi pi-history sga-empty-icon"></i>
              <p class="sga-empty-desc">Aún no hay eventos registrados.</p>
            </div>
          </template>
          <Column header="Fecha / Actor">
            <template #body="{ data }">
              <div class="ti-cell-stack">
                <strong>{{ data.usuarioActor }}</strong>
                <small class="sga-text-muted">{{ formatFecha(data.fecha) }}</small>
              </div>
            </template>
          </Column>
          <Column header="Acción">
            <template #body="{ data }">
              <div class="ti-cell-stack">
                <span>{{ data.accion }}</span>
                <small class="sga-text-muted">{{ data.detalle || data.modulo }}</small>
              </div>
            </template>
          </Column>
          <Column header="Estado" style="width: 100px">
            <template #body="{ data }">
              <Tag
                :value="data.resultado"
                :severity="data.resultado === 'EXITO' ? 'success' : 'danger'"
                rounded
              />
            </template>
          </Column>
        </DataTable>
      </article>

      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">DIAGNÓSTICO DEL SERVIDOR</span>
            <h3 class="sga-chart-title">Infraestructura Verificada</h3>
          </div>
          <span class="sga-kpi-pill">Sync {{ lastCheckedTime }}</span>
        </div>

        <div class="ti-infra-list">
          <div class="ti-infra-row">
            <div>
              <strong>Motor Base de Datos</strong>
              <p class="sga-text-muted">{{ sistema.bdMotor || '—' }} {{ sistema.bdVersion || '' }}</p>
            </div>
            <Tag
              :value="sistema.bdConectada ? `UP · ${sistema.bdLatenciaMs} ms` : 'DOWN'"
              :severity="sistema.bdConectada ? 'success' : 'danger'"
              rounded
            />
          </div>

          <div class="ti-infra-row">
            <div>
              <strong>Almacenamiento Local</strong>
              <p class="sga-text-muted">{{ sistema.storageDirectorio || './uploads/documentos' }}</p>
            </div>
            <Tag
              :value="sistema.storageDisponible ? `${sistema.storageArchivosDisco ?? 0} archivos` : 'NO DISP.'"
              :severity="sistema.storageDisponible ? 'success' : 'warn'"
              rounded
            />
          </div>

          <div class="ti-infra-row">
            <div>
              <strong>Runtime &amp; Versión API</strong>
              <p class="sga-text-muted">Java {{ sistema.javaVersion || '21' }} · {{ sistema.osNombre || 'Linux' }}</p>
            </div>
            <Tag :value="`v${sistema.appVersion || '0.5.1'} (${sistema.perfilActivo || 'default'})`" severity="info" rounded />
          </div>

          <div class="ti-infra-row">
            <div>
              <strong>Memoria JVM &amp; Disco</strong>
              <p class="sga-text-muted">
                Heap {{ sistema.jvmHeapUsadoMb ?? 0 }}/{{ sistema.jvmHeapMaximoMb ?? 0 }} MB ·
                Disco libre {{ sistema.discoEspacioLibreMb ?? 0 }}/{{ sistema.discoEspacioTotalMb ?? 0 }} MB
              </p>
            </div>
            <Button
              label="Ficha Técnica"
              icon="pi pi-info-circle"
              severity="secondary"
              outlined
              size="small"
              @click="$emit('navigate', 'about')"
            />
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import Button from 'primevue/button'
import Chart from 'primevue/chart'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Select from 'primevue/select'
import Tag from 'primevue/tag'

defineProps({
  isAdminTi: { type: Boolean, default: false },
  filterDias: { type: Number, default: 7 },
  filterModulo: { type: String, default: 'TODOS' },
  diasOptions: { type: Array, default: () => [] },
  moduloOptions: { type: Array, default: () => [] },
  hasActiveFilters: { type: Boolean, default: false },
  rolesChartData: { type: Object, required: true },
  actividadChartData: { type: Object, required: true },
  doughnutOptions: { type: Object, required: true },
  barOptions: { type: Object, required: true },
  eventosRecientes: { type: Array, default: () => [] },
  sistema: { type: Object, default: () => ({}) },
  lastCheckedTime: { type: String, default: '—' }
})

defineEmits(['update:filterDias', 'update:filterModulo', 'filter-change', 'reset-filters', 'navigate'])

const formatFecha = (iso) => {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('es-PE', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.ti-tab-stack {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.ti-cell-stack {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.ti-infra-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.ti-infra-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  padding: 0.65rem 0.85rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md, 8px);
}

.ti-infra-row p {
  margin: 0.15rem 0 0;
  font-size: 0.78rem;
}
</style>
