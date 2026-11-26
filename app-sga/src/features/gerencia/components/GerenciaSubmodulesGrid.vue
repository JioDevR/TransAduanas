<template>
  <section class="gerencia-directory">
    <div class="gerencia-section-head">
      <div>
        <span class="sga-kicker">SUB-OPCIONES DE GERENCIA &amp; ENLACES</span>
        <h3 class="sga-chart-title">Directorio Ejecutivo de Catálogos y Supervisión</h3>
      </div>
    </div>

    <div class="sga-canvas-grid gerencia-modules-grid">
      <article
        v-for="card in cards"
        :key="card.key"
        class="sga-card-surface sga-canvas-card gerencia-module-card"
        @click="$emit('navigate', card.key)"
      >
        <div class="gerencia-card-top">
          <Tag :value="card.index" severity="secondary" class="sga-mono" rounded />
          <Tag :value="card.badge" :severity="card.severity" rounded />
        </div>

        <div class="gerencia-card-body">
          <div class="gerencia-card-title-row">
            <i :class="[card.icon, 'gerencia-card-icon']"></i>
            <h4 class="gerencia-card-title">{{ card.title }}</h4>
          </div>
          <p class="gerencia-card-desc">{{ card.desc }}</p>
        </div>

        <div class="gerencia-card-metrics">
          <div class="gerencia-stat-kv">
            <span>{{ card.stat1Label }}</span>
            <strong class="sga-mono">{{ card.stat1Val }}</strong>
          </div>
          <div class="gerencia-stat-kv">
            <span>{{ card.stat2Label }}</span>
            <strong class="sga-mono">{{ card.stat2Val }}</strong>
          </div>
        </div>

        <div class="gerencia-card-actions" @click.stop>
          <Button
            :label="card.cta"
            icon="pi pi-arrow-right"
            iconPos="right"
            size="small"
            text
            @click="$emit('navigate', card.key)"
          />
          <Button
            v-if="card.pdfKey"
            icon="pi pi-file-pdf"
            label="PDF"
            size="small"
            severity="secondary"
            outlined
            :loading="exportingType === card.pdfKey"
            @click="$emit('export-pdf', card.pdfKey)"
          />
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'

const props = defineProps({
  metrics: { type: Object, required: true },
  exportingType: { type: [String, null], default: null }
})

defineEmits(['navigate', 'export-pdf'])

const cards = computed(() => [
  {
    key: 'sedes',
    pdfKey: 'sedes',
    index: '02',
    badge: 'Territorial',
    severity: 'info',
    icon: 'pi pi-building',
    title: 'Sedes & Agencias',
    desc: 'Sucursales operativas, agencias de aduana y puntos de enlace en todo el país.',
    stat1Label: 'Registradas',
    stat1Val: props.metrics.sedesCount,
    stat2Label: 'Operativas',
    stat2Val: props.metrics.sedesOperativasCount,
    cta: 'Gestionar Sedes'
  },
  {
    key: 'puertos',
    pdfKey: 'puertos',
    index: '03',
    badge: 'Nodos POL/POD',
    severity: 'contrast',
    icon: 'pi pi-compass',
    title: 'Puertos & Terminales',
    desc: 'Catálogo maestro de puertos marítimos, aeropuertos, terminales terrestres y fluviales.',
    stat1Label: 'Puertos',
    stat1Val: props.metrics.puertosCount,
    stat2Label: 'Activos',
    stat2Val: props.metrics.puertosActivosCount,
    cta: 'Gestionar Puertos'
  },
  {
    key: 'tipos-carga',
    pdfKey: 'tipos-carga',
    index: '04',
    badge: 'Clasificación',
    severity: 'success',
    icon: 'pi pi-box',
    title: 'Tipos de Carga',
    desc: 'Tipificación aduanera estandarizada para importación, exportación y cargas especiales.',
    stat1Label: 'Catálogo',
    stat1Val: props.metrics.cargasCount,
    stat2Label: 'Activos',
    stat2Val: props.metrics.cargasActivasCount,
    cta: 'Gestionar Tipos'
  },
  {
    key: 'mercancias',
    pdfKey: 'mercancias',
    index: '05',
    badge: 'Arancel & VUCE',
    severity: 'warn',
    icon: 'pi pi-tags',
    title: 'Mercancías & Partidas',
    desc: 'Subpartidas nacionales, permisos sanitarios/VUCE y tasas arancelarias Ad-Valorem.',
    stat1Label: 'Partidas',
    stat1Val: props.metrics.mercanciasCount,
    stat2Label: 'Con VUCE',
    stat2Val: props.metrics.mercanciasVuceCount,
    cta: 'Gestionar Partidas'
  }
])
</script>

<style scoped>
.gerencia-directory {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.gerencia-section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
}

.gerencia-modules-grid {
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 275px), 1fr));
}

.gerencia-module-card {
  cursor: pointer;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 0.9rem;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.gerencia-module-card:hover {
  transform: translateY(-2px);
}

.gerencia-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.gerencia-card-title-row {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  margin-bottom: 0.35rem;
}

.gerencia-card-icon {
  color: var(--sga-accent, #8c5a3c);
  font-size: 1rem;
}

.gerencia-card-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: var(--sga-text-primary);
}

.gerencia-card-desc {
  margin: 0;
  font-size: 0.84rem;
  line-height: 1.45;
  color: var(--sga-text-muted);
}

.gerencia-card-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.6rem;
  padding: 0.65rem 0.75rem;
  border-radius: var(--sga-radius-md, 10px);
  background: var(--sga-bg-subtle);
}

.gerencia-stat-kv {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  font-size: 0.76rem;
  color: var(--sga-text-muted);
}

.gerencia-stat-kv strong {
  font-size: 0.95rem;
  color: var(--sga-text-primary);
}

.gerencia-card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  border-top: 1px solid var(--sga-border-light);
  padding-top: 0.65rem;
}
</style>
