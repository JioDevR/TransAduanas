<template>
  <div class="about-system-view">
    <!-- 1. Hero Banner Card Editorial Cálido -->
    <HeroBannerCard
      title="Sistema de Gestión Aduanera (SGA)"
      kicker="INGENIERÍA DE SOFTWARE // FICHA TÉCNICA"
      description="Plataforma centralizada para la supervisión y control de despachos aduaneros, liquidación arancelaria, emisión de cotizaciones comerciales, fiscalización territorial y generación de órdenes de servicio operativo."
    >
      <template #extra>
        <div class="build-version-pill">
          <span class="pulse-indicator"></span>
          <span>Developer preview Alpha 0.01r2</span>
        </div>
      </template>
    </HeroBannerCard>

    <!-- 2. Métricas de la Compilación (01, 02, 03, 04) -->
    <div class="metrics-grid">
      <MetricCard index="01" label="Versión del Sistema" value="Alpha 0.01r2" severity="accent" />
      <MetricCard index="02" label="Canal de Distribución" value="Dev Preview" />
      <MetricCard index="03" label="Stack Tecnológico" value="Java 21 + Vue 3" severity="success" />
      <MetricCard index="04" label="Estado de Entorno" value="Activo / Local" severity="warning" />
    </div>

    <!-- 3. Grilla de Información Técnica y Arquitectura -->
    <div class="info-grid">
      <!-- Tarjeta 1: Ficha Técnica de la Compilación -->
      <div class="info-card">
        <div class="info-card-header">
          <div class="header-icon-wrap">
            <i class="pi pi-code"></i>
          </div>
          <div>
            <span class="card-kicker">INFORMACIÓN DE COMPILACIÓN</span>
            <h3 class="card-title">Metadatos de la Versión</h3>
          </div>
        </div>

        <div class="info-card-body">
          <div class="spec-row">
            <span class="spec-label">Identificador de Build:</span>
            <span class="spec-value mono">SGA-BUILD-2026-0.5.0-ALPHA</span>
          </div>
          <div class="spec-row">
            <span class="spec-label">Etiqueta de Versión:</span>
            <span class="spec-value highlight">Developer preview Alpha 0.5.0</span>
          </div>
          <div class="spec-row">
            <span class="spec-label">Entorno de Ejecución:</span>
            <span class="spec-value">OpenJDK 64-Bit Server VM 21.0.12 (Linux)</span>
          </div>
          <div class="spec-row">
            <span class="spec-label">Motor Frontend:</span>
            <span class="spec-value">Vite 8.3 + Vue 3.5 (Composition API)</span>
          </div>
          <div class="spec-row">
            <span class="spec-label">Framework Backend:</span>
            <span class="spec-value">Spring Boot 4.1.1 (Java EE / Jakarta)</span>
          </div>
          <div class="spec-row">
            <span class="spec-label">Capa de Persistencia:</span>
            <span class="spec-value">Spring Data JPA + Hibernate 6.x</span>
          </div>
          <div class="spec-row">
            <span class="spec-label">Base de Datos:</span>
            <span class="spec-value">MySQL 8.4 Enterprise (Puerto 3306)</span>
          </div>
          <div class="spec-row">
            <span class="spec-label">Librería de Componentes:</span>
            <span class="spec-value">PrimeVue v4 (Preset Oficial SGA)</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import HeroBannerCard from '@/shared/components/HeroBannerCard.vue'
import MetricCard from '@/shared/components/MetricCard.vue'
</script>

<style scoped>
.about-system-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  padding-bottom: var(--space-10, 2.5rem);
}

/* 1. Hero Banner Editorial Cálido */
.hero-banner-card {
  position: relative;
  background: linear-gradient(135deg, #1c1917 0%, #292524 60%, #453406 100%);
  border: 1px solid rgba(234, 179, 8, 0.25);
  border-radius: var(--radius-lg, 12px);
  padding: 1.75rem 2rem;
  color: #fafaf9;
  overflow: hidden;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.hero-content {
  position: relative;
  z-index: 2;
  max-width: 820px;
}

.hero-top-row {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  flex-wrap: wrap;
  margin-bottom: var(--space-2, 0.5rem);
}

.badge-kicker {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #eab308;
}

.build-version-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.2rem 0.65rem;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 9999px;
  font-family: var(--font-mono, monospace);
  font-size: 0.6875rem;
  font-weight: 700;
  color: #fafaf9;
}

.pulse-indicator {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  animation: pulse-dot 1.8s infinite;
}

@keyframes pulse-dot {
  0% { transform: scale(0.95); opacity: 0.8; }
  50% { transform: scale(1.4); opacity: 1; }
  100% { transform: scale(0.95); opacity: 0.8; }
}

.hero-title {
  font-size: 1.625rem;
  font-weight: 700;
  color: #ffffff;
  line-height: 1.25;
  margin: 0 0 0.5rem 0;
}

.hero-desc {
  font-size: 0.875rem;
  color: #d6d3d1;
  line-height: 1.5;
  margin: 0;
}

.hero-watermark {
  position: absolute;
  right: 1.5rem;
  top: 50%;
  transform: translateY(-50%);
  font-size: 7rem;
  font-weight: 900;
  color: rgba(255, 255, 255, 0.03);
  pointer-events: none;
  user-select: none;
}

/* 2. Métricas de la Compilación */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4, 1rem);
}

@media (max-width: 900px) {
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 500px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }
}

.metric-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: var(--space-2, 0.5rem);
}

.metric-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.metric-idx {
  font-family: var(--font-mono, monospace);
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-subtle);
}

.metric-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-muted);
}

.metric-val {
  font-family: var(--font-display, serif);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text-main);
  line-height: 1.2;
}

.text-accent {
  color: var(--color-accent-kicker);
}

.text-success {
  color: var(--color-success-text, #16a34a);
}

.text-warning {
  color: var(--color-warning-text, #d97706);
}

/* 3. Grilla de Información */
.info-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: var(--space-5, 1.25rem);
}

.info-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
}

.info-card-header {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  background: var(--color-bg-subtle);
  border-bottom: 1px solid var(--color-border-subtle);
}

.header-icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md, 8px);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-main);
  font-size: 1rem;
}

.card-kicker {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--color-text-subtle);
  display: block;
}

.card-title {
  font-size: 1.0625rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
}

.info-card-body {
  padding: var(--space-5, 1.25rem);
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-3, 0.75rem) var(--space-6, 1.5rem);
}

@media (max-width: 768px) {
  .info-card-body {
    grid-template-columns: 1fr;
  }
}

.spec-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: var(--space-2, 0.5rem);
  border-bottom: 1px solid var(--color-border-subtle);
  font-size: 0.8125rem;
}

.spec-label {
  color: var(--color-text-muted);
  font-weight: 500;
}

.spec-value {
  color: var(--color-text-main);
  font-weight: 600;
  text-align: right;
}

.spec-value.mono {
  font-family: var(--font-mono, monospace);
  font-size: 0.75rem;
}

.spec-value.highlight {
  color: var(--color-accent-kicker);
  font-weight: 700;
}
</style>
