<template>
  <section class="section-block">
    <h2 class="section-title">Shell Principal y Componentes Base (app-sga)</h2>
    <p class="section-desc">
      Vista separada con los componentes estructurales fijos de <code>app-sga</code>:
      <strong>AppLayout</strong> (<code>AppSidebar</code> + Topbar + Menú de usuario/tema) y componentes compartidos
      (<code>BaseModal</code>, <code>BasePagination</code>, <code>DocumentosUploadPanel</code>).
    </p>

    <!-- Selector de rol simulado para ver qué permanece fijo y qué cambia -->
    <div class="role-simulator-bar">
      <div class="role-sim-left">
        <span class="role-sim-label">Simular Rol Activo:</span>
        <Select
          v-model="selectedRole"
          :options="ROLE_OPTIONS"
          optionLabel="label"
          optionValue="value"
          class="role-sim-select"
          @change="onRoleChange"
        />
      </div>
      <div class="role-sim-tags">
        <Tag
          :value="authStore.canSwitchModules ? 'Conmuta módulos (Admin/Gerencia)' : 'Módulo anclado al rol'"
          :severity="authStore.canSwitchModules ? 'success' : 'info'"
        />
      </div>
    </div>

    <!-- Marco embebido para previsualizar AppLayout completo sin ocupar toda la pantalla -->
    <div class="shell-preview-frame">
      <AppLayout>
        <div class="demo-inner-content">
          <div class="demo-info-banner">
            <div>
              <strong>Área de Contenido (<code>&lt;router-view /&gt;</code>)</strong>
              <p>
                Módulo activo: <code>{{ navigationStore.currentModule }}</code> &bull;
                Rol actual: <code>{{ authStore.userRole }}</code>
              </p>
            </div>
            <Button label="Abrir BaseModal" icon="pi pi-window-maximize" size="small" @click="modalOpen = true" />
          </div>

          <div class="demo-shared-grid">
            <div class="demo-shared-card">
              <h3>BasePagination</h3>
              <p class="demo-muted">Paginación compartida entre tablas de módulos.</p>
              <BasePagination
                :current-page="page"
                :total-pages="5"
                :total-items="48"
                :page-size="10"
                @page-change="(p) => (page = p)"
              />
            </div>

            <div class="demo-shared-card">
              <h3>DocumentosUploadPanel</h3>
              <p class="demo-muted">Panel transversal de carga documental.</p>
              <DocumentosUploadPanel :id-expediente="101" :readonly="false" />
            </div>
          </div>
        </div>
      </AppLayout>
    </div>

    <BaseModal v-model="modalOpen" title="Ejemplo de BaseModal" subtitle="Componente base transversal">
      <p>Este modal es compartido por los distintos módulos de <code>app-sga</code>.</p>
      <template #footer>
        <Button label="Cerrar" severity="secondary" @click="modalOpen = false" />
      </template>
    </BaseModal>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Button from 'primevue/button'
import AppLayout from '../sga-components/shared/AppLayout.vue'
import BaseModal from '../sga-components/shared/BaseModal.vue'
import BasePagination from '../sga-components/shared/BasePagination.vue'
import DocumentosUploadPanel from '../sga-components/shared/DocumentosUploadPanel.vue'
import { ROLE_OPTIONS } from '../sga-components/roles.js'
import { useAuthStore, useNavigationStore } from '../sga-mocks/storesAndServices.js'

const authStore = useAuthStore()
const navigationStore = useNavigationStore()

const selectedRole = ref(authStore.userRole)
const page = ref(1)
const modalOpen = ref(false)

const onRoleChange = () => {
  authStore.setDemoRole(selectedRole.value)
}
</script>

<style scoped>
.role-simulator-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.85rem 1.1rem;
  margin-bottom: 1rem;
  background: var(--p-surface-0);
  border: 1px solid var(--p-surface-200);
  border-radius: 12px;
}

.role-sim-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.role-sim-label {
  font-weight: 600;
  font-size: 0.9rem;
}

.role-sim-select {
  min-width: 240px;
}

.shell-preview-frame {
  border: 1px solid var(--p-surface-300);
  border-radius: 14px;
  overflow: hidden;
  height: 640px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  position: relative;
}

/* Ajustes para contener AppLayout (que normalmente usa 100vh y sidebar fijo) dentro del marco */
.shell-preview-frame :deep(.app-layout) {
  min-height: 100%;
  height: 100%;
}

.shell-preview-frame :deep(.app-sidebar) {
  position: relative;
  height: 100%;
  min-height: 100%;
}

.shell-preview-frame :deep(.main-container) {
  margin-left: 0 !important;
  height: 100%;
  overflow-y: auto;
}

.demo-inner-content {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.demo-info-banner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.25rem;
  border-radius: 12px;
  background: var(--p-surface-0);
  border: 1px solid var(--p-surface-200);
}

.demo-info-banner p {
  margin: 0.25rem 0 0;
  font-size: 0.85rem;
  color: var(--p-text-muted-color);
}

.demo-shared-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1rem;
}

.demo-shared-card {
  padding: 1.1rem;
  border-radius: 12px;
  background: var(--p-surface-0);
  border: 1px solid var(--p-surface-200);
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.demo-shared-card h3 {
  margin: 0;
  font-size: 1rem;
}

.demo-muted {
  margin: 0;
  font-size: 0.825rem;
  color: var(--p-text-muted-color);
}
</style>
