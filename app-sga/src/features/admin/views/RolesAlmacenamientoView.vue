<template>
  <div class="roles-storage-view fade-in">
    <!-- 1. Barra de Estado y Acciones sgaUI (Título único en Topbar) -->
    <header class="sga-dash-header">
      <div class="sga-dash-meta">
        <Tag :value="`${rolesMatriz.length} roles institucionales`" severity="secondary" />
      </div>

      <div class="sga-dash-actions">
        <Button
          label="Actualizar"
          icon="pi pi-sync"
          severity="secondary"
          outlined
          size="small"
          :loading="loading"
          @click="loadData"
        />
      </div>
    </header>

    <!-- 2. KPIs de Almacenamiento & JVM (sgaUI) -->
    <section class="sga-kpis-grid">
      <article class="sga-kpi-card" :class="estado.storageDisponible ? 'is-success' : 'is-danger'">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">01</span>
          <span class="sga-kpi-label">Repositorio Documental</span>
          <i class="pi pi-folder-open sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value is-compact">
            {{ estado.storageDisponible ? 'DISPONIBLE' : 'NO DISPONIBLE' }}
          </span>
          <span class="sga-kpi-pill" :class="estado.storageDisponible ? 'is-success' : 'is-danger'">
            {{ formatBytes(estado.storageTamanoBytes) }}
          </span>
        </div>
        <p class="sga-kpi-foot sga-mono">{{ estado.storageDirectorio || './uploads/documentos' }}</p>
      </article>

      <article class="sga-kpi-card">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">02</span>
          <span class="sga-kpi-label">Archivos vs Base de Datos</span>
          <i class="pi pi-file sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ estado.storageArchivosDisco ?? 0 }}</span>
          <span class="sga-kpi-pill">{{ estado.storageDocumentosRegistradosBd ?? 0 }} en BD</span>
        </div>
        <p class="sga-kpi-foot">Documentos físicos indexados en disco local</p>
      </article>

      <article class="sga-kpi-card">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">03</span>
          <span class="sga-kpi-label">Espacio en Disco</span>
          <i class="pi pi-server sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value is-compact">{{ estado.discoEspacioLibreMb ?? 0 }} MB</span>
          <span class="sga-kpi-pill">de {{ estado.discoEspacioTotalMb ?? 0 }} MB</span>
        </div>
        <p class="sga-kpi-foot">Capacidad disponible en volumen del servidor</p>
      </article>

      <article class="sga-kpi-card">
        <div class="sga-kpi-head">
          <span class="sga-kpi-index">04</span>
          <span class="sga-kpi-label">Memoria JVM Heap</span>
          <i class="pi pi-microchip-ai sga-kpi-icon"></i>
        </div>
        <div class="sga-kpi-stat">
          <span class="sga-kpi-value">{{ estado.jvmHeapPorcentaje ?? 0 }}%</span>
          <span class="sga-kpi-pill">{{ estado.jvmHeapUsadoMb ?? 0 }}/{{ estado.jvmHeapMaximoMb ?? 0 }} MB</span>
        </div>
        <p class="sga-kpi-foot">Consumo actual de máquina virtual Java</p>
      </article>
    </section>

    <!-- 3. Matriz de Roles y Permisos (SgaListShell) -->
    <SgaListShell v-model:viewMode="viewMode" title="Matriz de Roles del SGA">
      <template #filters>
        <span class="sga-status-note sga-text-muted">
          Alcance funcional, conmutación de módulos y distribución de cuentas por rol.
        </span>
      </template>

      <div v-if="viewMode === 'table'">
        <DataTable
          :value="rolesMatriz"
          :loading="loading"
          size="small"
          stripedRows
          responsiveLayout="scroll"
          dataKey="codigo"
        >
          <Column header="Rol / Código">
            <template #body="{ data }">
              <div class="role-cell-stack">
                <strong>{{ data.etiqueta }}</strong>
                <code class="sga-text-muted">{{ data.codigo }}</code>
              </div>
            </template>
          </Column>

          <Column header="Descripción Operativa">
            <template #body="{ data }">
              <span>{{ data.descripcion }}</span>
            </template>
          </Column>

          <Column header="Módulos Habilitados">
            <template #body="{ data }">
              <div class="tags-wrap">
                <Tag
                  v-for="mod in data.modulosPermitidos"
                  :key="mod"
                  :value="mod"
                  severity="secondary"
                />
              </div>
            </template>
          </Column>

          <Column header="Capacidades Clave">
            <template #body="{ data }">
              <div class="tags-wrap">
                <Tag
                  v-for="cap in data.capacidades"
                  :key="cap"
                  :value="cap"
                  severity="info"
                />
              </div>
            </template>
          </Column>

          <Column header="Conmuta" style="width: 100px">
            <template #body="{ data }">
              <Tag
                :value="data.conmutaModulos ? 'Sí' : 'Fijo'"
                :severity="data.conmutaModulos ? 'warn' : 'secondary'"
                rounded
              />
            </template>
          </Column>

          <Column header="Usuarios" style="width: 120px">
            <template #body="{ data }">
              <div class="role-cell-stack">
                <span class="sga-mono sga-text-success">{{ data.usuariosActivos }} activos</span>
                <small class="sga-mono sga-text-muted">{{ data.usuariosInactivos }} baja</small>
              </div>
            </template>
          </Column>
        </DataTable>
      </div>

      <div v-else class="sga-canvas-grid">
        <article v-for="r in rolesMatriz" :key="r.codigo" class="sga-canvas-card">
          <div class="sga-card-top">
            <span class="sga-card-code">{{ r.etiqueta }}</span>
            <Tag
              :value="`${r.usuariosActivos} activos`"
              :severity="r.usuariosActivos > 0 ? 'success' : 'secondary'"
              rounded
            />
          </div>
          <p class="sga-text-muted role-desc">{{ r.descripcion }}</p>
          <div class="tags-wrap">
            <Tag v-for="mod in r.modulosPermitidos" :key="mod" :value="mod" severity="secondary" />
          </div>
          <div class="sga-card-footer">
            <code class="sga-text-muted">{{ r.codigo }}</code>
            <Tag :value="r.conmutaModulos ? 'Multimódulo' : 'Módulo único'" :severity="r.conmutaModulos ? 'warn' : 'secondary'" />
          </div>
        </article>
      </div>
    </SgaListShell>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Tag from 'primevue/tag'
import SgaListShell from '@/shared/components/SgaListShell.vue'
import { useNotification } from '@/shared/composables/useNotification'
import { sistemaService } from '../services/sistemaService'
import { formatBytes } from '../composables/useAdminDashboard'

const { showError } = useNotification()

const loading = ref(false)
const viewMode = ref('table')
const estado = ref({})
const rolesMatriz = ref([])

const loadData = async () => {
  loading.value = true
  try {
    const [est, matriz] = await Promise.all([
      sistemaService.getEstado(),
      sistemaService.getRolesMatriz()
    ])
    estado.value = est || {}
    rolesMatriz.value = matriz || []
  } catch (err) {
    showError(err.response?.data?.message || 'Error al cargar matriz de roles y estado de almacenamiento')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.roles-storage-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.role-cell-stack {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.tags-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
}

.role-desc {
  margin: 0;
  font-size: 0.82rem;
}
</style>
