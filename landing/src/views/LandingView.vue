<template>
  <div class="landing">
    <!-- Barra superior -->
    <header class="topbar">
      <div class="topbar-inner">
        <a href="/" class="brand">
          <span class="brand-logo"><i class="pi pi-shield"></i></span>
          <span class="brand-name">{{ empresa.nombre }}</span>
        </a>

        <nav class="nav-links">
          <button type="button" class="nav-link" @click="irASeccion('modulos')">
            Módulos
          </button>
          <button type="button" class="nav-link" @click="irASeccion('flujo')">
            Flujo Operativo
          </button>
        </nav>

        <div class="topbar-actions">
          <button
            type="button"
            class="icon-btn"
            :title="isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'"
            :aria-label="isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'"
            @click="toggleTheme"
          >
            <i :class="isDark ? 'pi pi-sun' : 'pi pi-moon'"></i>
          </button>

          <a :href="portalClienteUrl" target="_blank" rel="noopener noreferrer" class="btn btn-ghost btn-sm">
            <i class="pi pi-user"></i>
            <span>Portal Clientes</span>
          </a>

          <a :href="sgaAppUrl" target="_blank" rel="noopener noreferrer" class="btn btn-primary btn-sm">
            <i class="pi pi-sign-in"></i>
            <span>Sistema SGA</span>
          </a>
        </div>
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
            <a :href="sgaAppUrl" target="_blank" rel="noopener noreferrer" class="btn btn-primary">
              <span>Ingresar al Sistema SGA</span>
              <i class="pi pi-arrow-right"></i>
            </a>
            <a :href="portalClienteUrl" target="_blank" rel="noopener noreferrer" class="btn btn-secondary">
              <i class="pi pi-building"></i>
              <span>Acceso Portal Clientes</span>
            </a>
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
              que le corresponde según su rol y permisos asignados.
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
            <p class="section-text">
              Garantizamos una trazabilidad integral y transparente en cada fase del despacho aduanero y logístico.
            </p>
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

      <!-- Portales del Ecosistema -->
      <section class="section">
        <div class="container">
          <header class="section-head">
            <span class="kicker">Ecosistema Digital</span>
            <h2 class="section-title">Dos accesos para cada necesidad</h2>
            <p class="section-text">
              Accede a la plataforma especializada según tu perfil dentro del proceso logístico.
            </p>
          </header>

          <div class="portals-grid">
            <!-- Tarjeta SGA -->
            <div class="portal-card">
              <div class="portal-badge">Personal Operativo</div>
              <h3 class="portal-title">Sistema de Gestión Aduanera (SGA)</h3>
              <p class="portal-desc">
                Módulo integral para personal interno: liquidaciones aduaneras, DAMs SUNAT, gestión de almacenes temporales, asignación de transporte y contabilidad.
              </p>
              <ul class="portal-features">
                <li><i class="pi pi-check-circle"></i> Trámite documentario SUNAT</li>
                <li><i class="pi pi-check-circle"></i> Control de patios y almacén</li>
                <li><i class="pi pi-check-circle"></i> Despacho y flota de transporte</li>
              </ul>
              <a :href="sgaAppUrl" target="_blank" rel="noopener noreferrer" class="btn btn-primary btn-portal">
                <span>Ingresar al SGA</span>
                <i class="pi pi-arrow-right"></i>
              </a>
            </div>

            <!-- Tarjeta Portal Clientes -->
            <div class="portal-card">
              <div class="portal-badge">Empresas & Consignatarios</div>
              <h3 class="portal-title">Portal B2B de Clientes</h3>
              <p class="portal-desc">
                Portal de autoservicio para empresas importadoras y exportadoras: seguimiento de expedientes en tiempo real, descarga de comprobantes y cotizaciones en línea.
              </p>
              <ul class="portal-features">
                <li><i class="pi pi-check-circle"></i> Tracking de carga en tiempo real</li>
                <li><i class="pi pi-check-circle"></i> Solicitud de cotizaciones online</li>
                <li><i class="pi pi-check-circle"></i> Repositorio de documentos DAM y BL</li>
              </ul>
              <a :href="portalClienteUrl" target="_blank" rel="noopener noreferrer" class="btn btn-secondary btn-portal">
                <span>Ingresar al Portal Clientes</span>
                <i class="pi pi-arrow-right"></i>
              </a>
            </div>
          </div>
        </div>
      </section>

      <!-- Cierre -->
      <section class="section section-cta">
        <div class="container">
          <div class="cta-band">
            <div>
              <h2 class="cta-title">¿Listo para optimizar tus operaciones?</h2>
              <p class="cta-text">Ingresa con tus credenciales asignadas o consulta con nuestro equipo comercial.</p>
            </div>
            <div class="cta-band-actions">
              <a :href="portalClienteUrl" target="_blank" rel="noopener noreferrer" class="btn btn-light">
                <span>Portal Clientes</span>
                <i class="pi pi-arrow-right"></i>
              </a>
              <a :href="sgaAppUrl" target="_blank" rel="noopener noreferrer" class="btn btn-light-outline">
                <span>Ingreso Personal SGA</span>
              </a>
            </div>
          </div>
        </div>
      </section>
    </main>

    <footer class="footer">
      <div class="container footer-inner">
        <span>{{ empresa.nombre }} &copy; {{ anio }} · Todos los derechos reservados</span>
        <span class="footer-note">
          <i class="pi pi-lock"></i> Acceso seguro, encriptado y auditado bajo estándares SUNAT / OEA
        </span>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { useTheme } from '@/composables/useTheme'

const { isDark, toggleTheme } = useTheme()

// URLs configurables a través de variables de entorno con fallback por defecto
const sgaAppUrl = import.meta.env.VITE_SGA_APP_URL || 'http://localhost:5180'
const portalClienteUrl = import.meta.env.VITE_PORTAL_CLIENTE_URL || 'http://localhost:5174'

// Datos de la empresa
const empresa = {
  nombre: 'TransAduanas',
  descripcion:
    'Digitaliza y sincroniza las operaciones comerciales, contables, aduaneras, de almacén y de transporte de carga, con trazabilidad documental de extremo a extremo y control por roles.'
}

const anio = new Date().getFullYear()

const facts = [
  { value: '8', label: 'módulos operativos integrados' },
  { value: 'SUNAT · MTC', label: 'normativa y validaciones oficiales' },
  { value: '100%', label: 'trazabilidad documental y física' }
]

const modulos = [
  {
    icono: 'pi pi-server',
    titulo: 'Administración TI',
    descripcion: 'Gestión de usuarios, perfiles de seguridad y bitácora de auditoría del sistema.'
  },
  {
    icono: 'pi pi-chart-line',
    titulo: 'Gerencia General',
    descripcion: 'Dashboard ejecutivo, métricas operativas de sedes, catálogos y flota de transporte.'
  },
  {
    icono: 'pi pi-briefcase',
    titulo: 'Recepción Comercial',
    descripcion: 'Gestión de prospectos, clientes corporativos y cálculo de cotizaciones aduaneras.'
  },
  {
    icono: 'pi pi-file-edit',
    titulo: 'Gestión Aduanera',
    descripcion: 'Apertura de expedientes, declaraciones DAM, subpartidas arancelarias y liquidación de tributos.'
  },
  {
    icono: 'pi pi-box',
    titulo: 'Agente Aduanero',
    descripcion: 'Control de depósito temporal, verificación de aforo (Verde, Naranja, Rojo) y autorización de levante.'
  },
  {
    icono: 'pi pi-truck',
    titulo: 'Transporte y Flota',
    descripcion: 'Planificación de rutas, programación de despachos, asignación de unidades y choferes con SOAT/CITV.'
  },
  {
    icono: 'pi pi-wallet',
    titulo: 'Finanzas y Cobranzas',
    descripcion: 'Validación bancaria de comprobantes de pago, liquidaciones de gastos portuarios y facturación.'
  },
  {
    icono: 'pi pi-verified',
    titulo: 'Legal y Auditoría',
    descripcion: 'Conformidad legal, revisión preventiva de inconsistencias documentales y cumplimiento normativo.'
  }
]

const pasos = [
  {
    titulo: 'Cotización Comercial',
    descripcion: 'El agente comercial registra los datos de la carga y genera la cotización personalizada con cálculo de tributos.'
  },
  {
    titulo: 'Validación Financiera',
    descripcion: 'Finanzas comprueba y valida el adelanto bancario o pago inicial antes de habilitar el expediente operativo.'
  },
  {
    titulo: 'Trámite Aduanero SUNAT',
    descripcion: 'Se numera la DAM, se asigna el canal aduanero (Verde/Naranja/Rojo) y se gestiona el levante autorizado.'
  },
  {
    titulo: 'Transporte y Entrega',
    descripcion: 'Se asigna la unidad y conductor, se genera la guía de remisión y se realiza la entrega conforme en almacén de destino.'
  }
]

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
  max-width: 1160px;
  margin: 0 auto;
  padding: 0 var(--space-6);
}

/* ---------- Barra superior ---------- */
.topbar {
  position: sticky;
  top: 0;
  z-index: 50;
  background: var(--color-bg-base);
  border-bottom: 1px solid var(--color-border);
  backdrop-filter: blur(12px);
}

.topbar-inner {
  max-width: 1160px;
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
  width: 38px;
  height: 38px;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: var(--color-primary-text);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1.15rem;
  box-shadow: var(--shadow-subtle);
}

.brand-name {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  letter-spacing: -0.02em;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.nav-link {
  background: none;
  border: none;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.nav-link:hover {
  color: var(--color-text-main);
  background: var(--color-bg-subtle);
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.icon-btn {
  width: 38px;
  height: 38px;
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
  border-color: var(--color-border-strong);
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
  padding: 0.45rem 0.95rem;
  font-size: var(--font-size-sm);
}

.btn-primary {
  background: var(--color-primary);
  color: var(--color-primary-text);
}

.btn-primary:hover {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-card);
}

.btn-secondary {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border-strong);
  color: var(--color-text-main);
}

.btn-secondary:hover {
  background: var(--color-bg-subtle);
  transform: translateY(-1px);
  box-shadow: var(--shadow-card);
}

.btn-ghost {
  background: transparent;
  border-color: var(--color-border);
  color: var(--color-text-main);
}

.btn-ghost:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.btn-light {
  background: #f6f3ee;
  color: #2b2520;
}

.btn-light:hover {
  background: #ffffff;
  transform: translateY(-1px);
}

.btn-light-outline {
  background: transparent;
  border: 1px solid rgba(246, 243, 238, 0.4);
  color: #f6f3ee;
}

.btn-light-outline:hover {
  background: rgba(246, 243, 238, 0.1);
  border-color: #f6f3ee;
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
  position: relative;
  overflow: hidden;
}

.hero-inner {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-5);
}

.hero-title {
  margin: 0;
  max-width: 820px;
  font-size: clamp(2.2rem, 5vw, 3.5rem);
  line-height: 1.12;
  font-weight: var(--font-weight-extrabold);
  letter-spacing: -0.03em;
}

.hero-text {
  margin: 0;
  max-width: 650px;
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
  max-width: 680px;
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
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
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
  border-color: var(--color-border-strong);
  transform: translateY(-2px);
}

.module-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  color: var(--color-accent-kicker);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
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
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: var(--space-6);
}

.step {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  box-shadow: var(--shadow-subtle);
}

.step-number {
  width: 36px;
  height: 36px;
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

/* ---------- Portales ---------- */
.portals-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: var(--space-6);
}

.portal-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--space-8);
  box-shadow: var(--shadow-subtle);
  display: flex;
  flex-direction: column;
  transition: all var(--transition-normal);
}

.portal-card:hover {
  box-shadow: var(--shadow-card);
  border-color: var(--color-border-strong);
  transform: translateY(-2px);
}

.portal-badge {
  align-self: flex-start;
  padding: var(--space-1) var(--space-3);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  color: var(--color-accent-kicker);
  border-radius: var(--radius-pill);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  margin-bottom: var(--space-4);
}

.portal-title {
  margin: 0 0 var(--space-3);
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
}

.portal-desc {
  margin: 0 0 var(--space-5);
  font-size: var(--font-size-base);
  color: var(--color-text-muted);
  line-height: 1.6;
}

.portal-features {
  list-style: none;
  margin: 0 0 var(--space-8);
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  flex-grow: 1;
}

.portal-features li {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-sm);
  color: var(--color-text-main);
}

.portal-features i {
  color: var(--color-accent-kicker);
}

.btn-portal {
  width: 100%;
}

/* ---------- Banda de cierre ---------- */
.section-cta {
  padding-top: 0;
}

.cta-band {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--space-5);
  padding: var(--space-10);
  border-radius: var(--radius-xl);
  background: #352f2a;
  color: #f6f3ee;
  box-shadow: var(--shadow-elevated);
}

.cta-title {
  margin: 0 0 0.35rem;
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
}

.cta-text {
  margin: 0;
  color: #cfc0ab;
  font-size: var(--font-size-base);
}

.cta-band-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
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
  align-items: center;
  gap: var(--space-3);
}

.footer-note {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

@media (max-width: 860px) {
  .nav-links {
    display: none;
  }
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
  .topbar-actions .btn {
    padding: 0.4rem 0.6rem;
    font-size: var(--font-size-xs);
  }
  .topbar-actions .btn span {
    display: none;
  }
}
</style>

