<template>
  <div class="about-system-view">
    <!-- 1. Toolbar de Acciones Warm Editorial (Sin Banners Invasivos) -->
    <div class="about-action-toolbar">
      <div class="toolbar-left-info">
        <span class="badge-kicker">INGENIERÍA DE SOFTWARE // FICHA TÉCNICA</span>
        <h3 class="toolbar-title">Sistema de Gestión Aduanera (SGA)</h3>
      </div>

      <div class="toolbar-actions-group">
        <div class="build-version-pill">
          <span class="pulse-indicator"></span>
          <span class="font-mono">Alpha 0.5.0 · Dev Preview</span>
        </div>
      </div>
    </div>

    <!-- 2. Grilla de Información Técnica y Arquitectura -->
    <div class="info-grid">
      <!-- Tarjeta 1: Ficha Técnica de la Compilación -->
      <div class="info-card">
        <div class="info-card-header">
          <div class="header-icon-wrap">
            <i class="pi pi-code"></i>
          </div>
          <div>
            <span class="card-kicker">INFORMACIÓN DE COMPILACIÓN</span>
            <h4 class="card-title">Metadatos de la Versión</h4>
          </div>
        </div>

        <div class="info-card-body">
          <div class="spec-row">
            <span class="spec-label">Identificador de Build:</span>
            <span class="spec-value font-mono">SGA-BUILD-2026-0.5.0-ALPHA</span>
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

      <!-- Tarjeta 2: Arquitectura y Servicios Core -->
      <div class="info-card">
        <div class="info-card-header">
          <div class="header-icon-wrap">
            <i class="pi pi-shield"></i>
          </div>
          <div>
            <span class="card-kicker">SEGURIDAD & MOTOR OPERATIVO</span>
            <h4 class="card-title">Arquitectura y Gobernanza Técnica</h4>
          </div>
        </div>

        <div class="info-card-body">
          <div class="spec-row">
            <span class="spec-label">Modelo de Autenticación:</span>
            <span class="spec-value font-mono">Stateless JWT Bearer Token</span>
          </div>
          <div class="spec-row">
            <span class="spec-label">Algoritmo Criptográfico:</span>
            <span class="spec-value font-mono">HMAC-SHA256 (256-bit Key)</span>
          </div>
          <div class="spec-row">
            <span class="spec-label">Vigencia de Sesión:</span>
            <span class="spec-value font-mono">24 Horas continuas</span>
          </div>
          <div class="spec-row">
            <span class="spec-label">Generador de Documentos:</span>
            <span class="spec-value">JasperReports v7.0.8 (Compilador JDT)</span>
          </div>
          <div class="spec-row">
            <span class="spec-label">Pool de Conexiones:</span>
            <span class="spec-value font-mono">HikariCP (Max: 10, Min-Idle: 5)</span>
          </div>
          <div class="spec-row">
            <span class="spec-label">Gobernanza de Módulos:</span>
            <span class="spec-value">8 Dominios Segregados por Rol</span>
          </div>
          <div class="spec-row">
            <span class="spec-label">Sistema de Diseño:</span>
            <span class="spec-value highlight">Warm Editorial 2.0 (Arena / Espresso)</span>
          </div>
          <div class="spec-row">
            <span class="spec-label">Soporte de Temas:</span>
            <span class="spec-value">Modo Claro & Modo Oscuro (Titanio Carbón)</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// Componente de Ficha Técnica Acerca de SGA
</script>

<style scoped>
.about-system-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  padding-bottom: var(--space-10, 2.5rem);
  width: 100%;
}

/* ==========================================================================
   1. TOOLBAR DE ACCIONES WARM EDITORIAL
   ========================================================================== */
.about-action-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-4, 1rem);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  box-shadow: var(--shadow-subtle);
}

.toolbar-left-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.badge-kicker {
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-accent-kicker);
}

.toolbar-title {
  font-size: var(--font-size-xl, 1.25rem);
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
  letter-spacing: -0.015em;
}

.toolbar-actions-group {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.build-version-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.35rem 0.85rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-pill, 9999px);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: var(--color-text-main);
}

.pulse-indicator {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #16a34a;
  box-shadow: 0 0 6px rgba(22, 163, 74, 0.5);
  animation: pulse-dot 1.8s infinite;
}

@keyframes pulse-dot {
  0% { transform: scale(0.95); opacity: 0.8; }
  50% { transform: scale(1.3); opacity: 1; }
  100% { transform: scale(0.95); opacity: 0.8; }
}

/* ==========================================================================
   2. GRILLA DE INFORMACIÓN TÉCNICA
   ========================================================================== */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-5, 1.25rem);
}

@media (max-width: 900px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}

.info-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  overflow: hidden;
  box-shadow: var(--shadow-card);
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
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
  display: block;
  text-transform: uppercase;
}

.card-title {
  font-size: var(--font-size-md, 0.9375rem);
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
}

.info-card-body {
  padding: var(--space-5, 1.25rem);
  display: flex;
  flex-direction: column;
  gap: var(--space-3, 0.75rem);
}

.spec-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: var(--space-2, 0.5rem);
  border-bottom: 1px solid var(--color-border-subtle);
  font-size: var(--font-size-sm, 0.8125rem);
}

.spec-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
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

.spec-value.font-mono {
  font-family: var(--font-mono, monospace);
  font-size: var(--font-size-xs, 0.75rem);
}

.spec-value.highlight {
  color: var(--color-accent-kicker);
  font-weight: 700;
}
</style>
