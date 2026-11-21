<template>
  <div class="ti-tab-stack">
    <div class="sga-banner" :class="hasIntegrityAlerts ? 'is-warn' : 'is-success'">
      <i :class="hasIntegrityAlerts ? 'pi pi-exclamation-triangle' : 'pi pi-check-circle'"></i>
      <div class="ti-banner-content">
        <div>
          <strong>
            {{ hasIntegrityAlerts ? `Se detectaron ${integrityAlertsCount} inconsistencias Disco ↔ BD` : 'Repositorio documental sincronizado' }}
          </strong>
          <div>
            Directorio activo: {{ storage.directorioRaiz || sistema.storageDirectorio || './uploads/documentos' }} ·
            Faltantes: {{ storage.archivosFaltantesEnDisco ?? 0 }} · Huérfanos: {{ storage.archivosHuerfanosEnDisco ?? 0 }}
          </div>
        </div>
        <Button
          label="Gestionar Almacenamiento"
          icon="pi pi-folder-open"
          severity="secondary"
          outlined
          size="small"
          @click="$emit('navigate', 'almacenamiento')"
        />
      </div>
    </div>

    <section class="sga-card-surface ti-storage-summary">
      <div class="ti-storage-stat">
        <span class="sga-meta-label">ESTADO REPOSITORIO</span>
        <strong class="ti-stat-val">{{ storage.disponible ? 'Disponible' : 'No disponible' }}</strong>
        <small class="sga-text-muted">{{ formatBytes(storage.storageTamanoBytes) }} en disco</small>
      </div>
      <div class="ti-storage-stat">
        <span class="sga-meta-label">VOLUMEN DEL SERVIDOR</span>
        <strong class="ti-stat-val">{{ storage.discoOcupacionPct ?? 0 }}% usado</strong>
        <small class="sga-text-muted">
          {{ formatMb(storage.discoUsadoMb) }} / {{ formatMb(storage.discoTotalMb) }} ({{ formatMb(storage.discoLibreMb) }} libres)
        </small>
      </div>
      <div class="ti-storage-stat">
        <span class="sga-meta-label">DOCUMENTOS EN BD</span>
        <strong class="ti-stat-val">{{ storage.registrosBdActivos ?? 0 }} activos</strong>
        <small class="sga-text-muted">
          {{ storage.archivosEnDisco ?? 0 }} en disco · {{ storage.registrosBdInactivos ?? 0 }} en baja lógica
        </small>
      </div>
      <div class="ti-storage-stat">
        <span class="sga-meta-label">INTEGRIDAD DISCO ↔ BD</span>
        <strong class="ti-stat-val">{{ storage.archivosSincronizados ?? 0 }} sincronizados</strong>
        <small class="sga-text-muted">
          Faltantes: {{ storage.archivosFaltantesEnDisco ?? 0 }} · Huérfanos: {{ storage.archivosHuerfanosEnDisco ?? 0 }}
        </small>
      </div>
    </section>

    <section class="sga-charts-grid">
      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">DISTRIBUCIÓN POR ENTIDAD</span>
            <h3 class="sga-chart-title">Volumen Documental por Módulo</h3>
          </div>
          <Button
            label="Ver Archivos"
            icon="pi pi-folder-open"
            severity="secondary"
            text
            size="small"
            @click="$emit('navigate', 'almacenamiento')"
          />
        </div>
        <div class="sga-chart-box">
          <Chart
            v-if="storageEntidadChartData.labels.length > 0"
            type="bar"
            :data="storageEntidadChartData"
            :options="barOptions"
            class="sga-chart-canvas"
          />
          <div v-else class="sga-empty-state">
            <i class="pi pi-chart-bar sga-empty-icon"></i>
            <p class="sga-empty-desc">Aún no hay documentos activos registrados para graficar.</p>
          </div>
        </div>
      </article>

      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">FORMATOS ALMACENADOS</span>
            <h3 class="sga-chart-title">Consumo por Extensión de Archivo</h3>
          </div>
          <span class="sga-text-muted">{{ formatBytes(storage.storageTamanoBytes) }}</span>
        </div>
        <div class="sga-chart-box sga-doughnut-wrap">
          <Chart
            v-if="storageExtensionChartData.labels.length > 0"
            type="doughnut"
            :data="storageExtensionChartData"
            :options="doughnutOptions"
            class="sga-chart-canvas"
          />
          <div v-else class="sga-empty-state">
            <i class="pi pi-chart-pie sga-empty-icon"></i>
            <p class="sga-empty-desc">Sin archivos en repositorio para analizar extensiones.</p>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import Button from 'primevue/button'
import Chart from 'primevue/chart'
import { formatBytes, formatMb } from '../../composables/useAdminDashboard'

defineProps({
  storage: { type: Object, default: () => ({}) },
  sistema: { type: Object, default: () => ({}) },
  hasIntegrityAlerts: { type: Boolean, default: false },
  integrityAlertsCount: { type: Number, default: 0 },
  storageEntidadChartData: { type: Object, required: true },
  storageExtensionChartData: { type: Object, required: true },
  barOptions: { type: Object, required: true },
  doughnutOptions: { type: Object, required: true }
})

defineEmits(['navigate'])
</script>

<style scoped>
.ti-tab-stack {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.ti-banner-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.ti-storage-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 1rem;
  font-variant-numeric: tabular-nums;
}

.ti-storage-stat {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  padding: 0.75rem 0.9rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md, 8px);
}

.ti-stat-val {
  font-size: 1.05rem;
  color: var(--color-text-main);
}
</style>
