<script setup>
import { ref, computed, defineAsyncComponent, onErrorCaptured } from 'vue'
import Card from 'primevue/card'
import Button from 'primevue/button'
import SelectButton from 'primevue/selectbutton'
import Tag from 'primevue/tag'

onErrorCaptured((err) => {
  console.warn('[Showcase ErrorCaptured]', err.message)
  return false
})

const activeModule = ref('shared')
const openModalKey = ref(null)

// Mock props compartidos para que todos los componentes rendericen sin errores
const mockExpediente = {
  idExpediente: 101,
  codigoExpediente: 'EXP-2026-0101',
  numeroDam: '118-2026-10-004521',
  regimenAduanero: 'IMPORTACION',
  canalControl: 'NARANJA',
  estadoActual: 'EN_TRAMITE',
  clienteRazonSocial: 'Agroexportadora Lima SAC',
  clienteRuc: '20512345678',
  fechaApertura: '2026-11-10',
  pesoBrutoKg: 14250
}

const mockCotizacion = {
  idCotizacion: 55,
  codigoCotizacion: 'COT-2026-0055',
  idCliente: 1,
  regimenAduanero: 'IMPORTACION',
  modalidad: 'MARITIMO',
  totalCotizado: 2450.0,
  estadoCotizacion: 'APROBADA',
  items: []
}

const mockDespacho = {
  idDespacho: 12,
  codigoDespacho: 'DSP-2026-0012',
  estadoTransporte: 'EN_RUTA',
  placaVehiculo: 'ABC-123',
  conductorNombre: 'Carlos Mendoza'
}

const modules = [
  { id: 'shared', label: 'Shared (5)' },
  { id: 'admin', label: 'Admin (11)' },
  { id: 'aduanas', label: 'Aduanas (13)' },
  { id: 'agente', label: 'Agente (7)' },
  { id: 'almacen', label: 'Almacén (1)' },
  { id: 'comercial', label: 'Comercial (6)' },
  { id: 'finanzas', label: 'Finanzas (1)' },
  { id: 'transporte', label: 'Transporte (9)' },
  { id: 'portal', label: 'Portal B2B (5)' },
  { id: 'all', label: 'Todos (58)' }
]

const catalog = [
  // SHARED (5)
  { key: 'AppSidebar', mod: 'shared', comp: defineAsyncComponent(() => import('../sga-components/shared/AppSidebar.vue')), props: {} },
  { key: 'BaseModal', mod: 'shared', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/shared/BaseModal.vue')), props: { title: 'BaseModal SGA', subtitle: 'Modal reutilizable', icon: 'pi pi-box' } },
  { key: 'BasePagination', mod: 'shared', comp: defineAsyncComponent(() => import('../sga-components/shared/BasePagination.vue')), props: { currentPage: 2, totalPages: 8, totalItems: 76, startIndex: 11, endIndex: 20 } },
  { key: 'DocumentosManagerModal', mod: 'shared', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/shared/DocumentosManagerModal.vue')), props: { entidadTipo: 'EXPEDIENTE', entidadId: 101 } },
  { key: 'DocumentosUploadPanel', mod: 'shared', comp: defineAsyncComponent(() => import('../sga-components/shared/DocumentosUploadPanel.vue')), props: { entidadTipo: 'EXPEDIENTE', entidadId: 101 } },

  // ADMIN (11)
  { key: 'MercanciaFormModal', mod: 'admin', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/admin/catalogos/MercanciaFormModal.vue')), props: {} },
  { key: 'PuertoRegistrationForm', mod: 'admin', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/admin/catalogos/PuertoRegistrationForm.vue')), props: {} },
  { key: 'TipoCargaRegistrationForm', mod: 'admin', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/admin/catalogos/TipoCargaRegistrationForm.vue')), props: {} },
  { key: 'PortalUserAuditModal', mod: 'admin', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/admin/portal-users/PortalUserAuditModal.vue')), props: { user: { id: 1, username: 'cliente.demo', razonSocial: 'Demo SAC' } } },
  { key: 'PortalUserForm', mod: 'admin', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/admin/portal-users/PortalUserForm.vue')), props: {} },
  { key: 'PortalUserResetPasswordModal', mod: 'admin', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/admin/portal-users/PortalUserResetPasswordModal.vue')), props: { user: { id: 1, username: 'cliente.demo' } } },
  { key: 'PortalUserTable', mod: 'admin', comp: defineAsyncComponent(() => import('../sga-components/admin/portal-users/PortalUserTable.vue')), props: { users: [{ idUsuarioCliente: 1, username: 'demo.b2b', razonSocial: 'Importadora Demo SAC', activo: true }] } },
  { key: 'SedeRegistrationForm', mod: 'admin', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/admin/sedes/SedeRegistrationForm.vue')), props: {} },
  { key: 'UserRegistrationForm', mod: 'admin', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/admin/users/UserRegistrationForm.vue')), props: {} },
  { key: 'UserRoleBadge', mod: 'admin', comp: defineAsyncComponent(() => import('../sga-components/admin/users/UserRoleBadge.vue')), props: { roleName: 'SUPERVISOR_ADUANERO' } },
  { key: 'UserTable', mod: 'admin', comp: defineAsyncComponent(() => import('../sga-components/admin/users/UserTable.vue')), props: { users: [{ idUsuario: 1, nombre: 'Ana', apellidos: 'Torres', correo: 'ana@sga.pe', rol: 'AGENTE_ADUANERO', activo: true }] } },

  // ADUANAS (13)
  { key: 'ActualizarTransitoModal', mod: 'aduanas', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/aduanas/ActualizarTransitoModal.vue')), props: { expediente: mockExpediente } },
  { key: 'AperturaExpedienteView', mod: 'aduanas', comp: defineAsyncComponent(() => import('../sga-components/aduanas/AperturaExpedienteView.vue')), props: {} },
  { key: 'AuditoriaExpedienteModal', mod: 'aduanas', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/aduanas/AuditoriaExpedienteModal.vue')), props: { expediente: mockExpediente } },
  { key: 'CanalLevanteModal', mod: 'aduanas', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/aduanas/CanalLevanteModal.vue')), props: { expediente: mockExpediente } },
  { key: 'CierreExpedienteModal', mod: 'aduanas', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/aduanas/CierreExpedienteModal.vue')), props: { expediente: mockExpediente } },
  { key: 'ConfirmarEmbarqueExportacionModal', mod: 'aduanas', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/aduanas/ConfirmarEmbarqueExportacionModal.vue')), props: { expediente: mockExpediente } },
  { key: 'ConfirmarIngresoOrigenModal', mod: 'aduanas', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/aduanas/ConfirmarIngresoOrigenModal.vue')), props: { expediente: mockExpediente } },
  { key: 'DashboardAduaneroPanel', mod: 'aduanas', comp: defineAsyncComponent(() => import('../sga-components/aduanas/DashboardAduaneroPanel.vue')), props: { expedientes: [mockExpediente] } },
  { key: 'DiligenciaCampoModal', mod: 'aduanas', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/aduanas/DiligenciaCampoModal.vue')), props: { expediente: mockExpediente } },
  { key: 'RecepcionDestinoExteriorModal', mod: 'aduanas', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/aduanas/RecepcionDestinoExteriorModal.vue')), props: { expediente: mockExpediente } },
  { key: 'RetiroDirectoModal', mod: 'aduanas', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/aduanas/RetiroDirectoModal.vue')), props: { expediente: mockExpediente } },
  { key: 'SolicitudRecojoExportacionModal', mod: 'aduanas', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/aduanas/SolicitudRecojoExportacionModal.vue')), props: { expediente: mockExpediente } },
  { key: 'SolicitudTransporteModal', mod: 'aduanas', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/aduanas/SolicitudTransporteModal.vue')), props: { expediente: mockExpediente } },

  // AGENTE ADUANERO (7)
  { key: 'AgenteAccionesCell', mod: 'agente', comp: defineAsyncComponent(() => import('../sga-components/agente-aduanero/AgenteAccionesCell.vue')), props: { expediente: mockExpediente } },
  { key: 'AgenteDashboardPanel', mod: 'agente', comp: defineAsyncComponent(() => import('../sga-components/agente-aduanero/AgenteDashboardPanel.vue')), props: { expedientes: [mockExpediente] } },
  { key: 'AgenteExpedienteInformeView', mod: 'agente', comp: defineAsyncComponent(() => import('../sga-components/agente-aduanero/AgenteExpedienteInformeView.vue')), props: { expediente: mockExpediente } },
  { key: 'AgenteExpedientesCanvas', mod: 'agente', comp: defineAsyncComponent(() => import('../sga-components/agente-aduanero/AgenteExpedientesCanvas.vue')), props: { expedientes: [mockExpediente] } },
  { key: 'AgenteExpedientesTable', mod: 'agente', comp: defineAsyncComponent(() => import('../sga-components/agente-aduanero/AgenteExpedientesTable.vue')), props: { expedientes: [mockExpediente] } },
  { key: 'AgenteHeaderToolbar', mod: 'agente', comp: defineAsyncComponent(() => import('../sga-components/agente-aduanero/AgenteHeaderToolbar.vue')), props: { viewMode: 'canvas', agentesList: [] } },
  { key: 'AgenteOperacionTransitoView', mod: 'agente', comp: defineAsyncComponent(() => import('../sga-components/agente-aduanero/AgenteOperacionTransitoView.vue')), props: { expediente: mockExpediente } },

  // ALMACEN (1)
  { key: 'IngresoAlmacenModal', mod: 'almacen', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/almacen/IngresoAlmacenModal.vue')), props: { expediente: mockExpediente } },

  // COMERCIAL (6)
  { key: 'ClienteRegistrationForm', mod: 'comercial', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/comercial/ClienteRegistrationForm.vue')), props: {} },
  { key: 'CotizacionCorrectionView', mod: 'comercial', comp: defineAsyncComponent(() => import('../sga-components/comercial/CotizacionCorrectionView.vue')), props: { cotizacion: mockCotizacion } },
  { key: 'CotizacionFormHeader', mod: 'comercial', comp: defineAsyncComponent(() => import('../sga-components/comercial/CotizacionFormHeader.vue')), props: { currentStep: 1 } },
  { key: 'CotizacionProformaPreview', mod: 'comercial', comp: defineAsyncComponent(() => import('../sga-components/comercial/CotizacionProformaPreview.vue')), props: { form: mockCotizacion } },
  { key: 'CotizacionRegistrationForm', mod: 'comercial', comp: defineAsyncComponent(() => import('../sga-components/comercial/CotizacionRegistrationForm.vue')), props: {} },
  { key: 'CotizacionStepperNav', mod: 'comercial', comp: defineAsyncComponent(() => import('../sga-components/comercial/CotizacionStepperNav.vue')), props: { currentStep: 2, form: mockCotizacion, docsCount: 2 } },

  // FINANZAS (1)
  { key: 'RegistroPagoFullView', mod: 'finanzas', comp: defineAsyncComponent(() => import('../sga-components/finanzas/RegistroPagoFullView.vue')), props: { cotizacion: mockCotizacion } },

  // TRANSPORTE (9)
  { key: 'AsignarRecursosModal', mod: 'transporte', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/transporte/AsignarRecursosModal.vue')), props: { despacho: mockDespacho } },
  { key: 'CabinaRutaConductorPanel', mod: 'transporte', comp: defineAsyncComponent(() => import('../sga-components/transporte/CabinaRutaConductorPanel.vue')), props: { despachos: [mockDespacho] } },
  { key: 'ConductoresManagementPanel', mod: 'transporte', comp: defineAsyncComponent(() => import('../sga-components/transporte/ConductoresManagementPanel.vue')), props: {} },
  { key: 'ConfirmarEntregaModal', mod: 'transporte', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/transporte/ConfirmarEntregaModal.vue')), props: { despacho: mockDespacho } },
  { key: 'CrearDespachoModal', mod: 'transporte', isModal: true, comp: defineAsyncComponent(() => import('../sga-components/transporte/CrearDespachoModal.vue')), props: {} },
  { key: 'FlotaManagementPanel', mod: 'transporte', comp: defineAsyncComponent(() => import('../sga-components/transporte/FlotaManagementPanel.vue')), props: {} },
  { key: 'TransporteDashboardPanel', mod: 'transporte', comp: defineAsyncComponent(() => import('../sga-components/transporte/TransporteDashboardPanel.vue')), props: { despachos: [mockDespacho] } },
  { key: 'TransporteDespachosTable', mod: 'transporte', comp: defineAsyncComponent(() => import('../sga-components/transporte/TransporteDespachosTable.vue')), props: { despachos: [mockDespacho] } },
  { key: 'TransporteHeaderToolbar', mod: 'transporte', comp: defineAsyncComponent(() => import('../sga-components/transporte/TransporteHeaderToolbar.vue')), props: { enRutaCount: 7, disponiblesCount: 14, loading: false } },

  // PORTAL CLIENTE B2B (5)
  { key: 'PortalCotizacionForm', mod: 'portal', comp: defineAsyncComponent(() => import('../sga-components/portal-cliente/PortalCotizacionForm.vue')), props: {} },
  { key: 'PortalCotizacionFormHeader', mod: 'portal', comp: defineAsyncComponent(() => import('../sga-components/portal-cliente/PortalCotizacionFormHeader.vue')), props: { currentStep: 1 } },
  { key: 'PortalCotizacionProformaPreview', mod: 'portal', comp: defineAsyncComponent(() => import('../sga-components/portal-cliente/PortalCotizacionProformaPreview.vue')), props: { form: mockCotizacion } },
  { key: 'PortalCotizacionStepperNav', mod: 'portal', comp: defineAsyncComponent(() => import('../sga-components/portal-cliente/PortalCotizacionStepperNav.vue')), props: { currentStep: 2, form: mockCotizacion, docsCount: 1 } },
  { key: 'PortalDocumentosUploadPanel', mod: 'portal', comp: defineAsyncComponent(() => import('../sga-components/portal-cliente/PortalDocumentosUploadPanel.vue')), props: { documentos: [] } }
]

const visibleItems = computed(() =>
  activeModule.value === 'all'
    ? catalog
    : catalog.filter((item) => item.mod === activeModule.value)
)
</script>

<template>
  <div>
    <h2 class="section-title">Vista Integrada de los 58 Componentes SGA & Portal B2B</h2>
    <p class="section-desc">
      Cada componente está montado y renderizado en vivo tal cual viene del proyecto principal (con datos mock). Selecciona un módulo para inspeccionarlos:
    </p>

    <div style="margin-bottom: 1.25rem; overflow-x: auto">
      <SelectButton
        v-model="activeModule"
        :options="modules"
        optionLabel="label"
        optionValue="id"
        :allowEmpty="false"
        size="small"
      />
    </div>

    <div class="demo-box">
      <Card v-for="item in visibleItems" :key="item.key">
        <template #title>
          <div class="demo-row" style="justify-content: space-between; width: 100%">
            <span>{{ item.key }}.vue</span>
            <Tag :value="item.isModal ? 'Modal / Dialog' : item.mod" :severity="item.isModal ? 'warn' : 'secondary'" />
          </div>
        </template>
        <template #content>
          <template v-if="item.isModal">
            <Button
              :label="`Abrir ${item.key}`"
              icon="pi pi-external-link"
              size="small"
              @click="openModalKey = item.key"
            />
            <component
              :is="item.comp"
              v-if="openModalKey === item.key"
              v-bind="item.props"
              :visible="true"
              @close="openModalKey = null"
              @update:visible="(v) => { if (!v) openModalKey = null }"
            />
          </template>
          <template v-else>
            <component :is="item.comp" v-bind="item.props" />
          </template>
        </template>
      </Card>
    </div>
  </div>
</template>
