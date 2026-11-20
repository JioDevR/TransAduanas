<template>
  <div class="about-system-view fade-in">
    <header class="sga-dash-header">
      <div>
        <h2 class="section-title">Ficha Técnica del Sistema (SGA)</h2>
      </div>

      <div class="sga-dash-actions">
        <Tag :value="`v${estado.appVersion || '0.5.1'} · ${estado.perfilActivo || 'default'}`" severity="info" rounded />
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
            <span class="sga-kicker">ENTORNO DE EJECUCIÓN EN VIVO</span>
            <h3 class="sga-chart-title">Metadatos de Servidor y Build</h3>
          </div>
          <i class="pi pi-code sga-kpi-icon"></i>
        </div>

        <div class="sga-card-meta-grid">
          <div class="sga-meta-item">
            <span class="sga-meta-label">APLICACIÓN</span>
            <span class="sga-meta-val sga-mono">{{ estado.appNombre || 'sga-backend' }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">VERSIÓN API</span>
            <span class="sga-meta-val sga-mono">{{ estado.appVersion || '0.5.1' }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">JAVA RUNTIME</span>
            <span class="sga-meta-val sga-mono">Java {{ estado.javaVersion || '21' }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">SISTEMA OPERATIVO</span>
            <span class="sga-meta-val">{{ estado.osNombre || 'Linux' }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">PERFIL ACTIVO</span>
            <span class="sga-meta-val sga-mono">{{ estado.perfilActivo || 'default' }}</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">UPTIME</span>
            <span class="sga-meta-val sga-mono">{{ formatUptime(estado.uptimeSegundos) }}</span>
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
              {{ estado.bdConectada ? `Conectada (${estado.bdLatenciaMs}ms)` : 'Sin verificar' }}
            </span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">FRONTEND</span>
            <span class="sga-meta-val">Vue 3.5 + PrimeVue 4 (sgaUI)</span>
          </div>
          <div class="sga-meta-item">
            <span class="sga-meta-label">SEGURIDAD</span>
            <span class="sga-meta-val">Spring Security + JWT Stateless</span>
          </div>
          <div class="sga-meta-item full-span">
            <span class="sga-meta-label">ALMACENAMIENTO DOCUMENTAL</span>
            <span class="sga-meta-val sga-mono">{{ estado.storageDirectorio || './uploads/documentos' }}</span>
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
}
</style>
