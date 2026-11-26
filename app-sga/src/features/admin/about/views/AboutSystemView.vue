<template>
  <div class="about-system-view fade-in">
    <header class="sga-dash-header">
      <div class="sga-dash-meta">
        <Tag value="SGA - TransAduanas · Build 1.4" severity="primary" rounded />
        <Tag :value="`API v${estado.appVersion || '0.5.1'} · ${estado.perfilActivo || 'default'}`" severity="info" rounded />
      </div>

      <div class="sga-dash-actions">
        <Button
          label="Sincronizar"
          icon="pi pi-sync"
          severity="secondary"
          outlined
          size="small"
          :loading="loading"
          @click="loadEstado"
        />
      </div>
    </header>

    <section class="sga-charts-grid">
      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">IDENTIDAD DEL SISTEMA</span>
            <h3 class="sga-chart-title">Plataforma y Build General</h3>
          </div>
          <i class="pi pi-box sga-kpi-icon"></i>
        </div>

        <div class="sga-card-meta-grid">
          <div class="sga-meta-item">
            <span class="sga-meta-label">PLATAFORMA</span>
            <span class="sga-meta-val">SGA - TransAduanas</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">BUILD GENERAL</span>
            <span class="sga-meta-val">Build 1.4</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">VERSIÓN API</span>
            <span class="sga-meta-val">v{{ estado.appVersion || '0.5.1' }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">SERVICIO BACKEND</span>
            <span class="sga-meta-val">{{ estado.appNombre || 'sga-backend' }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">PERFIL ACTIVO</span>
            <span class="sga-meta-val">{{ estado.perfilActivo || 'default' }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">UPTIME</span>
            <span class="sga-meta-val">{{ formatUptime(estado.uptimeSegundos) }}</span>
          </div>
        </div>
      </article>

      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">ENTORNO DE EJECUCIÓN EN VIVO</span>
            <h3 class="sga-chart-title">Servidor, Runtime y Recursos</h3>
          </div>
          <i class="pi pi-server sga-kpi-icon"></i>
        </div>

        <div class="sga-card-meta-grid">
          <div class="sga-meta-item">
            <span class="sga-meta-label">JAVA RUNTIME</span>
            <span class="sga-meta-val">Java {{ estado.javaVersion || '21' }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">SISTEMA OPERATIVO</span>
            <span class="sga-meta-val">{{ estado.osNombre || 'Linux' }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">MEMORIA JVM HEAP</span>
            <span class="sga-meta-val">
              {{ estado.jvmHeapUsadoMb ?? 0 }} / {{ estado.jvmHeapMaximoMb ?? 0 }} MB ({{ estado.jvmHeapPorcentaje ?? 0 }}%)
            </span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">CAPACIDAD DE DISCO</span>
            <span class="sga-meta-val">
              {{ estado.discoEspacioLibreMb ?? 0 }} MB libres / {{ estado.discoEspacioTotalMb ?? 0 }} MB
            </span>
          </div>
          <div class="sga-meta-item full-span">
            <span class="sga-meta-label">ALMACENAMIENTO DOCUMENTAL</span>
            <span class="sga-meta-val">{{ estado.storageDirectorio || './uploads/documentos' }}</span>
          </div>
        </div>
      </article>

      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">ARQUITECTURA Y PERSISTENCIA</span>
            <h3 class="sga-chart-title">Stack Tecnológico Verificado</h3>
          </div>
          <i class="pi pi-shield sga-kpi-icon"></i>
        </div>

        <div class="sga-card-meta-grid">
          <div class="sga-meta-item">
            <span class="sga-meta-label">MOTOR BD</span>
            <span class="sga-meta-val">{{ estado.bdMotor || 'MySQL' }} {{ estado.bdVersion || '' }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">ESTADO BD</span>
            <span class="sga-meta-val" :class="estado.bdConectada ? 'sga-text-success' : 'sga-text-warn'">
              {{ estado.bdConectada ? `Conectada (${estado.bdLatenciaMs} ms)` : 'Sin verificar' }}
            </span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">FRONTEND</span>
            <span class="sga-meta-val">Vue 3.5 + Vite 8 + PrimeVue 4 (sgaUI)</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">BACKEND</span>
            <span class="sga-meta-val">Spring Boot 4.1 + Spring Data JPA</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">SEGURIDAD</span>
            <span class="sga-meta-val">Spring Security + JWT Stateless</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">MOTOR DE REPORTES</span>
            <span class="sga-meta-val">Thymeleaf + OpenHTMLtoPDF</span>
          </div>
        </div>
      </article>

      <article class="sga-card-surface sga-chart-card">
        <div class="sga-chart-head">
          <div>
            <span class="sga-kicker">ALCANCE FUNCIONAL</span>
            <h3 class="sga-chart-title">Ecosistema SGA - TransAduanas</h3>
          </div>
          <i class="pi pi-sitemap sga-kpi-icon"></i>
        </div>

        <div class="sga-card-meta-grid">
          <div class="sga-meta-item full-span">
            <span class="sga-meta-label">APLICACIONES INTEGRADAS</span>
            <span class="sga-meta-val">Intranet SGA · Portal B2B Clientes · Landing Corporativa</span>
          </div>
          <div class="sga-meta-item full-span">
            <span class="sga-meta-label">MÓDULOS OPERATIVOS</span>
            <span class="sga-meta-val">Comercial, Gestión Aduanera, Agente Aduanero, Transporte y Flota, Finanzas</span>
          </div>
          <div class="sga-meta-item full-span">
            <span class="sga-meta-label">GOBIERNO Y ADMINISTRACIÓN TI</span>
            <span class="sga-meta-val">Gerencia General (Sedes y Catálogos) · Administración TI (Usuarios, B2B, Registro TI, Storage)</span>
          </div>
          <div class="sga-meta-item full-span">
            <span class="sga-meta-label">ORGANIZACIÓN</span>
            <span class="sga-meta-val">TransAduanas del Perú S.A.C. · RUC 20601234567</span>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { sistemaService } from '../../services/sistemaService'
import { formatUptime } from '../../composables/useAdminDashboard'

const loading = ref(false)
const estado = ref({})

const loadEstado = async () => {
  loading.value = true
  try {
    estado.value = await sistemaService.getEstado()
  } catch {
    estado.value = {}
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadEstado()
})
</script>

<style scoped>
.about-system-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  font-variant-numeric: tabular-nums;
}

.about-system-view :deep(.sga-meta-val) {
  white-space: normal;
}
</style>
