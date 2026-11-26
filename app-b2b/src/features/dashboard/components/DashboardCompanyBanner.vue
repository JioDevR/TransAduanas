<template>
  <section class="sga-card-surface company-banner" aria-label="Datos de la empresa">
    <div class="company-identity">
      <div class="company-emblem" aria-hidden="true">
        <i class="pi pi-building"></i>
      </div>

      <div class="company-main-info">
        <div class="company-kicker-row">
          <span class="sga-kicker">CUENTA EMPRESARIAL B2B</span>
          <span class="ruc-pill sga-mono">RUC {{ empresa.ruc }}</span>
        </div>
        <h2 class="company-name">{{ empresa.razonSocial }}</h2>
        <p class="company-address">
          <i class="pi pi-map-marker"></i>
          <span>{{ empresa.direccionFiscal }}</span>
        </p>
      </div>
    </div>

    <div class="company-contact-grid">
      <div class="contact-item">
        <span class="sga-kicker">REPRESENTANTE</span>
        <strong class="contact-value">{{ empresa.representante }}</strong>
        <span class="contact-sub">{{ empresa.cargo }}</span>
      </div>

      <div class="contact-item">
        <span class="sga-kicker">CORREO CORPORATIVO</span>
        <strong class="contact-value">{{ empresa.correoContacto }}</strong>
        <span class="contact-sub">Notificaciones y comprobantes</span>
      </div>

      <div class="contact-item">
        <span class="sga-kicker">TELÉFONO DE CONTACTO</span>
        <strong class="contact-value sga-mono">{{ empresa.telefonoContacto }}</strong>
        <span class="contact-sub">Canal operativo</span>
      </div>
    </div>
  </section>
</template>

<script setup>
defineProps({
  empresa: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.company-banner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
}

.company-identity {
  display: flex;
  align-items: center;
  gap: 1rem;
  min-width: 260px;
  flex: 1 1 320px;
}

.company-emblem {
  width: 50px;
  height: 50px;
  border-radius: var(--radius-lg, 12px);
  background: var(--color-bg-hero);
  border: 1px solid var(--color-border);
  color: var(--color-primary-espresso);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1.35rem;
  flex-shrink: 0;
}

.company-main-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 0;
}

.company-kicker-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.ruc-pill {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.1rem 0.55rem;
  border-radius: var(--radius-pill);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
}

.company-name {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-text-main);
  line-height: 1.25;
}

.company-address {
  margin: 0;
  display: inline-flex;
  align-items: center;
  gap: 0.38rem;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.company-address i {
  font-size: 0.78rem;
  color: var(--color-accent-kicker);
}

.company-contact-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(150px, 1fr));
  gap: 1rem 1.5rem;
  padding-left: 1.5rem;
  border-left: 1px solid var(--color-border-subtle);
  flex: 1 1 460px;
}

.contact-item {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  min-width: 0;
}

.contact-value {
  font-size: 0.84rem;
  font-weight: 600;
  color: var(--color-text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contact-sub {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

@media (max-width: 1040px) {
  .company-contact-grid {
    padding-left: 0;
    padding-top: 1rem;
    border-left: none;
    border-top: 1px solid var(--color-border-subtle);
    flex-basis: 100%;
  }
}

@media (max-width: 640px) {
  .company-contact-grid {
    grid-template-columns: 1fr;
    gap: 0.85rem;
  }
}
</style>
