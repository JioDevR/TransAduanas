<template>
  <div class="landing">
    <!-- Barra superior -->
    <header class="topbar">
      <div class="topbar-inner">
        <router-link to="/" class="brand">
          <span class="brand-logo"><i class="pi pi-shield"></i></span>
          <span class="brand-name">{{ empresa.nombre }}</span>
        </router-link>

        <nav class="topbar-actions">
          <button
            type="button"
            class="icon-btn"
            :title="isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'"
            :aria-label="isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'"
            @click="toggleTheme"
          >
            <i :class="isDark ? 'pi pi-sun' : 'pi pi-moon'"></i>
          </button>
          <router-link :to="ctaPath" class="btn btn-primary btn-sm">
            <i class="pi pi-sign-in"></i>
            <span>{{ ctaLabel }}</span>
          </router-link>
        </nav>
      </div>
    </header>

    <main>
      <!-- Hero -->
      <section class="hero">
        <div class="container hero-inner">
          <span class="kicker">Plataforma integral</span>
          <h1 class="hero-title">
            Gestión aduanera y logística internacional, en un solo sistema.
          </h1>
          <p class="hero-text">
            {{ empresa.descripcion }}
          </p>

          <div class="hero-actions">
            <router-link :to="ctaPath" class="btn btn-primary">
              <span>{{ ctaLabel }}</span>
              <i class="pi pi-arrow-right"></i>
            </router-link>
            <button type="button" class="btn btn-ghost" @click="irASeccion('modulos')">
              Conocer los módulos
            </button>
          </div>

          <ul class="hero-facts">
            <li v-for="fact in facts" :key="fact.label">
              <strong>{{ fact.value }}</strong>
              <span>{{ fact.label }}</span>
            </li>
          </ul>
        </div>
      </section>

      <!-- Módulos -->
      <section id="modulos" class="section">
        <div class="container">
          <header class="section-head">
            <span class="kicker">Módulos del sistema</span>
            <h2 class="section-title">Cada área trabaja en su propio módulo</h2>
            <p class="section-text">
              Al iniciar sesión, el sistema lleva a cada usuario directamente al módulo
              que le corresponde según su rol.
            </p>
          </header>

          <div class="modules-grid">
            <article v-for="mod in modulos" :key="mod.titulo" class="module-card">
              <span class="module-icon"><i :class="mod.icono"></i></span>
              <h3 class="module-title">{{ mod.titulo }}</h3>
              <p class="module-text">{{ mod.descripcion }}</p>
            </article>
          </div>
        </div>
      </section>

      <!-- Flujo de trabajo -->
      <section id="flujo" class="section section-alt">
        <div class="container">
          <header class="section-head">
            <span class="kicker">Flujo de trabajo</span>
            <h2 class="section-title">Del primer contacto a la entrega</h2>
          </header>

          <ol class="steps">
            <li v-for="(paso, i) in pasos" :key="paso.titulo" class="step">
              <span class="step-number">{{ i + 1 }}</span>
              <h3 class="step-title">{{ paso.titulo }}</h3>
              <p class="step-text">{{ paso.descripcion }}</p>
            </li>
          </ol>
        </div>
      </section>

      <!-- Cierre -->
      <section class="section">
        <div class="container">
          <div class="cta-band">
            <div>
              <h2 class="cta-title">¿Listo para trabajar?</h2>
              <p class="cta-text">Ingresa con tu usuario institucional.</p>
            </div>
            <router-link :to="ctaPath" class="btn btn-light">
              <span>{{ ctaLabel }}</span>
              <i class="pi pi-arrow-right"></i>
            </router-link>
          </div>
        </div>
      </section>
    </main>

    <footer class="footer">
      <div class="container footer-inner">
        <span>{{ empresa.nombre }} &copy; {{ anio }}</span>
        <span class="footer-note">
          <i class="pi pi-lock"></i> Acceso restringido y auditado
        </span>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useTheme } from '@/shared/composables/useTheme'

const authStore = useAuthStore()
const { isDark, toggleTheme } = useTheme()

// Datos editables de la empresa (cambiar aquí el nombre o la descripción)
const empresa = {
  nombre: 'TransAduanas',
  descripcion:
    'Digitaliza y sincroniza las operaciones comerciales, contables, aduaneras, de almacén y de transporte de carga, con trazabilidad documental y control por roles.'
}

const anio = new Date().getFullYear()

const facts = [
  { value: '8', label: 'módulos operativos' },
  { value: 'SUNAT · MTC', label: 'normativa aplicada' },
  { value: '100%', label: 'trazabilidad documental' }
]

const modulos = [
  {
    icono: 'pi pi-server',
    titulo: 'Administración TI',
    descripcion: 'Usuarios, roles y bitácora de actividad del sistema.'
  },
  {
    icono: 'pi pi-chart-line',
    titulo: 'Gerencia General',
    descripcion: 'Dashboard ejecutivo, sedes, catálogos, flota y conductores.'
  },
  {
    icono: 'pi pi-briefcase',
    titulo: 'Recepción Comercial',
    descripcion: 'Clientes, prospectos y cotizaciones comerciales.'
  },
  {
    icono: 'pi pi-file-edit',
    titulo: 'Gestión Aduanera',
    descripcion: 'Expedientes, declaraciones DAM, clasificación arancelaria y tributos.'
  },
  {
    icono: 'pi pi-box',
    titulo: 'Agente Aduanero',
    descripcion: 'Ingreso a almacén, seguimiento de expedientes y despacho.'
  },
  {
    icono: 'pi pi-truck',
    titulo: 'Transporte y Flota',
    descripcion: 'Despachos, unidades de transporte y conductores.'
  },
  {
    icono: 'pi pi-wallet',
    titulo: 'Finanzas',
    descripcion: 'Validación de pagos, liquidaciones y conciliación.'
  },
  {
    icono: 'pi pi-verified',
    titulo: 'Legal y Auditoría',
    descripcion: 'Revisión y auditoría de expedientes.'
  }
]

const pasos = [
  {
    titulo: 'Cotización',
    descripcion: 'Comercial registra al cliente y emite la cotización del servicio.'
  },
  {
    titulo: 'Pago validado',
    descripcion: 'Finanzas valida el pago o anticipo antes de abrir el expediente.'
  },
  {
    titulo: 'Expediente aduanero',
    descripcion: 'Se gestiona la DAM, el canal de control SUNAT y el levante.'
  },
  {
    titulo: 'Transporte y entrega',
    descripcion: 'Se asignan recursos, se despacha la carga y se confirma la entrega.'
  }
]

// Si ya hay sesión activa, el botón lleva al módulo del usuario; si no, al login
const ctaPath = computed(() => (authStore.isAuthenticated ? authStore.homePath : '/login'))
const ctaLabel = computed(() =>
  authStore.isAuthenticated ? 'Ir a mi módulo' : 'Ingresar al sistema'
)

const irASeccion = (id) => {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<style scoped>
.landing {
  min-height: 100vh;
  background: var(--color-bg-base);
  color: var(--color-text-main);
  font-family: var(--font-sans);
}

.container {
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 var(--space-6);
}

/* ---------- Barra superior ---------- */
.topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  background: var(--color-bg-base);
  border-bottom: 1px solid var(--color-border);
}

.topbar-inner {
  max-width: 1120px;
  margin: 0 auto;
  padding: var(--space-3) var(--space-6);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: var(--space-3);
  text-decoration: none;
  color: var(--color-text-main);
}

.brand-logo {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--color-primary);
  color: #efe7dd;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
}

.brand-name {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  letter-spacing: -0.02em;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.icon-btn {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.icon-btn:hover {
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
}

/* ---------- Botones ---------- */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: 0.75rem 1.4rem;
  border-radius: var(--radius-md);
  border: 1px solid transparent;
  font-family: inherit;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  text-decoration: none;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-sm {
  padding: 0.5rem 1rem;
  font-size: var(--font-size-sm);
}

.btn-primary {
  background: var(--color-primary);
  color: var(--color-primary-text);
}

.btn-primary:hover {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
}

.btn-ghost {
  background: transparent;
  border-color: var(--color-border-strong);
  color: var(--color-text-main);
}

.btn-ghost:hover {
  background: var(--color-bg-subtle);
}

.btn-light {
  background: #f6f3ee;
  color: #2b2520;
}

.btn-light:hover {
  background: #ffffff;
  transform: translateY(-1px);
}

/* ---------- Textos comunes ---------- */
.kicker {
  display: inline-block;
  font-size: var(--font-size-2xs);
  font-weight: var(--font-weight-bold);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-accent-kicker);
}

/* ---------- Hero ---------- */
.hero {
  background: var(--color-bg-hero);
  border-bottom: 1px solid var(--color-border);
  padding: var(--space-16) 0;
}

.hero-inner {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-5);
}

.hero-title {
  margin: 0;
  max-width: 760px;
  font-size: clamp(2rem, 4.5vw, 3.25rem);
  line-height: 1.1;
  font-weight: var(--font-weight-extrabold);
  letter-spacing: -0.03em;
}

.hero-text {
  margin: 0;
  max-width: 620px;
  font-size: var(--font-size-lg);
  line-height: 1.6;
  color: var(--color-text-muted);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-top: var(--space-2);
}

.hero-facts {
  list-style: none;
  margin: var(--space-6) 0 0;
  padding: var(--space-5) 0 0;
  border-top: 1px solid var(--color-border-strong);
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-8);
}

.hero-facts li {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.hero-facts strong {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
}

.hero-facts span {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

/* ---------- Secciones ---------- */
.section {
  padding: var(--space-16) 0;
}

.section-alt {
  background: var(--color-bg-subtle);
  border-top: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
}

.section-head {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin-bottom: var(--space-8);
  max-width: 640px;
}

.section-title {
  margin: 0;
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  letter-spacing: -0.02em;
}

.section-text {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.6;
}

/* ---------- Módulos ---------- */
.modules-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: var(--space-4);
}

.module-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  box-shadow: var(--shadow-subtle);
  transition: all var(--transition-normal);
}

.module-card:hover {
  box-shadow: var(--shadow-card);
  transform: translateY(-2px);
}

.module-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  color: var(--color-accent-kicker);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  margin-bottom: var(--space-4);
}

.module-title {
  margin: 0 0 var(--space-2);
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
}

.module-text {
  margin: 0;
  font-size: var(--font-size-sm);
  line-height: 1.55;
  color: var(--color-text-muted);
}

/* ---------- Flujo ---------- */
.steps {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: var(--space-6);
}

.step-number {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-pill);
  background: var(--color-primary);
  color: var(--color-primary-text);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-mono);
  font-weight: var(--font-weight-bold);
  font-size: var(--font-size-sm);
  margin-bottom: var(--space-3);
}

.step-title {
  margin: 0 0 var(--space-2);
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
}

.step-text {
  margin: 0;
  font-size: var(--font-size-sm);
  line-height: 1.55;
  color: var(--color-text-muted);
}

/* ---------- Banda de cierre ---------- */
.cta-band {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--space-5);
  padding: var(--space-8);
  border-radius: var(--radius-xl);
  background: #352f2a;
  color: #f6f3ee;
}

.cta-title {
  margin: 0 0 0.25rem;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
}

.cta-text {
  margin: 0;
  color: #cfc0ab;
  font-size: var(--font-size-base);
}

/* ---------- Pie ---------- */
.footer {
  border-top: 1px solid var(--color-border);
  padding: var(--space-6) 0;
  font-size: var(--font-size-xs);
  color: var(--color-text-subtle);
}

.footer-inner {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: var(--space-3);
}

.footer-note {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

@media (max-width: 640px) {
  .hero,
  .section {
    padding: var(--space-12) 0;
  }
  .brand-name {
    font-size: var(--font-size-md);
  }
  .cta-band {
    padding: var(--space-6);
  }
}
</style>