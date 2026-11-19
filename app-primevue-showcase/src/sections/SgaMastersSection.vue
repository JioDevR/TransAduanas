<script setup>
import { ref, computed } from 'vue'
import Card from 'primevue/card'
import Button from 'primevue/button'
import SelectButton from 'primevue/selectbutton'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import { useToast } from 'primevue/usetoast'

import SgaFormShell from '../sga-masters/SgaFormShell.vue'
import SgaFormBlock from '../sga-masters/SgaFormBlock.vue'
import SgaSummaryCard from '../sga-masters/SgaSummaryCard.vue'
import SgaWorkflowModal from '../sga-masters/SgaWorkflowModal.vue'
import SgaStepperNav from '../sga-masters/SgaStepperNav.vue'
import SgaDataTableCard from '../sga-masters/SgaDataTableCard.vue'
import SgaKpiStrip from '../sga-masters/SgaKpiStrip.vue'

const toast = useToast()

// 1. Variante SgaKpiStrip
const kpiVariant = ref('aduanas')
const activeKpi = ref(null)

const kpiItems = computed(() =>
  kpiVariant.value === 'aduanas'
    ? [
        { key: 'tramite', label: 'En Trámite', value: 24, icon: 'pi pi-compass', severity: 'info', hint: 'Expedientes activos' },
        { key: 'rojo', label: 'Canal Rojo', value: 5, icon: 'pi pi-exclamation-triangle', severity: 'danger', hint: 'Aforo físico pendiente' },
        { key: 'levante', label: 'Levante Autorizado', value: 18, icon: 'pi pi-check-circle', severity: 'success', hint: 'Listos para retiro' },
        { key: 'cerrados', label: 'Cerrados Mes', value: 62, icon: 'pi pi-lock', severity: 'neutral', hint: 'Legajo archivado' }
      ]
    : [
        { key: 'pendiente', label: 'Por Asignar', value: 4, icon: 'pi pi-clock', severity: 'warn', hint: 'Sin tracto/chofer' },
        { key: 'ruta', label: 'En Ruta', value: 7, icon: 'pi pi-truck', severity: 'info', hint: 'GPS activo Callao-Lima' },
        { key: 'entregado', label: 'Entregados Hoy', value: 11, icon: 'pi pi-verified', severity: 'success', hint: 'GRE-T conformes' }
      ]
)

// 2. Variante SgaStepperNav
const stepperVariant = ref('comercial')
const currentStep = ref(2)

const stepperSteps = computed(() =>
  stepperVariant.value === 'comercial'
    ? [
        { id: 1, title: 'Cliente Solicitante', completed: true },
        { id: 2, title: 'Régimen & Plazos', completed: false },
        { id: 3, title: 'Documentación', completed: false },
        { id: 4, title: 'Liquidación & Proforma', completed: false }
      ]
    : [
        { id: 1, title: 'Régimen & Logística', completed: true },
        { id: 2, title: 'Documentación', completed: false },
        { id: 3, title: 'Valoración & Envío', completed: false }
      ]
)

// 3. Variante SgaFormShell
const formVariant = ref('puerto')
const formEditMode = ref(false)
const formData = ref({
  codigo: 'PECLL',
  nombre: 'Puerto del Callao - Muelle Sur',
  modalidad: 'MARITIMO',
  pais: 'Perú',
  departamento: 'Callao',
  correo: 'operaciones@sga.pe'
})

const formConfig = computed(() => {
  if (formVariant.value === 'puerto') {
    return {
      kicker: 'CATÁLOGO MAESTRO ADUANERO',
      backLabel: 'Volver al Catálogo de Puertos',
      title: formEditMode.value ? `Editar Puerto: ${formData.value.codigo}` : 'Registrar Nuevo Puerto o Terminal',
      desc: 'Variante de SgaFormShell para Puertos (reemplaza el cascarón de PuertoRegistrationForm).',
      submitLabel: formEditMode.value ? 'Guardar Cambios' : 'Registrar Puerto',
      summaryTitle: 'Ficha Portuaria',
      summaryItems: [
        { label: 'Código UN/LOCODE', value: formData.value.codigo, mono: true },
        { label: 'Terminal', value: formData.value.nombre },
        { label: 'Modalidad', value: formData.value.modalidad },
        { label: 'País', value: formData.value.pais }
      ]
    }
  }
  return {
    kicker: 'INFRAESTRUCTURA & SEDES',
    backLabel: 'Volver al Directorio de Sedes',
    title: formEditMode.value ? `Editar Sede: ${formData.value.codigo}` : 'Registrar Nueva Sede Operativa',
    desc: 'Variante de SgaFormShell para Sedes (reemplaza el cascarón de SedeRegistrationForm).',
    submitLabel: formEditMode.value ? 'Guardar Cambios' : 'Registrar Sede',
    summaryTitle: 'Ficha de Sede',
    summaryItems: [
      { label: 'Código TI', value: formData.value.codigo, mono: true },
      { label: 'Instalación', value: formData.value.nombre },
      { label: 'Jurisdicción', value: formData.value.departamento },
      { label: 'Contacto', value: formData.value.correo }
    ]
  }
})

// 4. Variante SgaWorkflowModal
const modalVariant = ref(null)
const modalObs = ref('')
const modalRefCode = ref('118-2026-10-004521')

const modalConfig = computed(() => {
  if (modalVariant.value === 'canal') {
    return {
      title: 'Asignar Canal SUNAT y DAM',
      subtitle: 'Expediente EXP-2026-0101 — Variante CanalLevanteModal',
      icon: 'pi pi-compass',
      bannerSeverity: 'warn',
      bannerTitle: 'Control Aduanero SUNAT',
      bannerText: 'Asigne la numeración DAM y el canal de control correspondiente.',
      submitLabel: 'Registrar Canal SUNAT'
    }
  }
  if (modalVariant.value === 'embarque') {
    return {
      title: 'Confirmar Embarque Internacional',
      subtitle: 'Expediente EXP-2026-0101 — Variante ConfirmarEmbarqueModal',
      icon: 'pi pi-send',
      bannerSeverity: 'info',
      bannerTitle: 'Zarpe de Nave',
      bannerText: 'Certifica la estiba a bordo y salida internacional de la carga.',
      submitLabel: 'Registrar Embarque'
    }
  }
  return {
    title: 'Registrar Acta de Retiro Directo',
    subtitle: 'Expediente EXP-2026-0101 — Variante RetiroDirectoModal',
    icon: 'pi pi-building',
    bannerSeverity: 'success',
    bannerTitle: 'Retiro en Terminal',
    bannerText: 'El cliente retira la carga directamente con su unidad externa.',
    submitLabel: 'Registrar Acta de Retiro'
  }
})

const handleModalSubmit = () => {
  toast.add({
    severity: 'success',
    summary: 'Variante ejecutada',
    detail: `${modalConfig.value.title} (${modalRefCode.value})`,
    life: 3000
  })
  modalVariant.value = null
}

// 5. Variante SgaDataTableCard
const tableVariant = ref('usuarios')
const tableSearch = ref('')
const tableFilter = ref('')

const usuariosRows = [
  { codigo: 'USR-01', nombre: 'Ana Torres', detalle: 'SUPERVISOR_ADUANERO', estado: 'ACTIVO' },
  { codigo: 'USR-02', nombre: 'Carlos Mendoza', detalle: 'AGENTE_ADUANERO', estado: 'ACTIVO' },
  { codigo: 'USR-03', nombre: 'Lucía Paredes', detalle: 'AGENTE_COMERCIAL', estado: 'INACTIVO' }
]

const despachosRows = [
  { codigo: 'DSP-2026-0012', nombre: 'Agroexportadora Lima SAC', detalle: 'Tracto ABC-123 • Callao → Ate', estado: 'EN_RUTA' },
  { codigo: 'DSP-2026-0013', nombre: 'Textiles Andinos EIRL', detalle: 'Por asignar • DPW → Lurín', estado: 'PENDIENTE' }
]

const filteredRows = computed(() => {
  const base = tableVariant.value === 'usuarios' ? usuariosRows : despachosRows
  return base.filter((r) => {
    const q = tableSearch.value.trim().toLowerCase()
    const matchQ = !q || r.codigo.toLowerCase().includes(q) || r.nombre.toLowerCase().includes(q)
    const matchF = !tableFilter.value || r.estado === tableFilter.value
    return matchQ && matchF
  })
})
</script>

<template>
  <div class="masters-showcase">
    <h2 class="section-title">Componentes Maestros SGA &amp; Variantes (PrimeVue 4 + sga-theme)</h2>
    <p class="section-desc">
      Prototipo de arquitectura maestro-variante en <code>src/sga-masters/</code>: un solo componente maestro con estructura, tokens y PrimeVue 4, generando variantes por props y slots.
    </p>

    <!-- 1. SgaKpiStrip -->
    <Card class="master-section-card">
      <template #title>
        <div class="master-title-row">
          <span>1. SgaKpiStrip.vue — Tira KPI Interactiva</span>
          <SelectButton
            v-model="kpiVariant"
            :options="[
              { label: 'Variante Aduanas', value: 'aduanas' },
              { label: 'Variante Transporte', value: 'transporte' }
            ]"
            optionLabel="label"
            optionValue="value"
            :allowEmpty="false"
            size="small"
            @update:modelValue="activeKpi = null"
          />
        </div>
      </template>
      <template #content>
        <SgaKpiStrip :items="kpiItems" :activeKey="activeKpi" @select="activeKpi = $event" />
      </template>
    </Card>

    <!-- 2. SgaStepperNav -->
    <Card class="master-section-card">
      <template #title>
        <div class="master-title-row">
          <span>2. SgaStepperNav.vue — Stepper de Cotización</span>
          <SelectButton
            v-model="stepperVariant"
            :options="[
              { label: 'Comercial Interno (4 pasos)', value: 'comercial' },
              { label: 'Portal B2B (3 pasos)', value: 'portal' }
            ]"
            optionLabel="label"
            optionValue="value"
            :allowEmpty="false"
            size="small"
            @update:modelValue="currentStep = 1"
          />
        </div>
      </template>
      <template #content>
        <SgaStepperNav v-model="currentStep" :steps="stepperSteps" />
      </template>
    </Card>

    <!-- 3. SgaWorkflowModal -->
    <Card class="master-section-card">
      <template #title>3. SgaWorkflowModal.vue — Maestro para Modales Operativos</template>
      <template #content>
        <div class="demo-row">
          <Button label="Variante Canal SUNAT" icon="pi pi-compass" size="small" @click="modalVariant = 'canal'" />
          <Button label="Variante Embarque Exportación" icon="pi pi-send" severity="secondary" outlined size="small" @click="modalVariant = 'embarque'" />
          <Button label="Variante Retiro Directo" icon="pi pi-building" severity="secondary" outlined size="small" @click="modalVariant = 'retiro'" />
        </div>

        <SgaWorkflowModal
          :visible="Boolean(modalVariant)"
          :title="modalConfig.title"
          :subtitle="modalConfig.subtitle"
          :icon="modalConfig.icon"
          :bannerSeverity="modalConfig.bannerSeverity"
          :bannerTitle="modalConfig.bannerTitle"
          :bannerText="modalConfig.bannerText"
          :submitLabel="modalConfig.submitLabel"
          @close="modalVariant = null"
          @submit="handleModalSubmit"
        >
          <div class="field-col">
            <label>Referencia / Código DAM</label>
            <InputText v-model="modalRefCode" class="font-mono" fluid />
          </div>
          <div class="field-col">
            <label>Observaciones Operativas</label>
            <Textarea v-model="modalObs" rows="2" autoResize placeholder="Detalle de diligencia o entrega..." fluid />
          </div>
        </SgaWorkflowModal>
      </template>
    </Card>

    <!-- 4. SgaDataTableCard -->
    <Card class="master-section-card">
      <template #title>
        <div class="master-title-row">
          <span>4. SgaDataTableCard.vue — Tabla Maestra con Filtros</span>
          <SelectButton
            v-model="tableVariant"
            :options="[
              { label: 'Variante Usuarios', value: 'usuarios' },
              { label: 'Variante Despachos', value: 'despachos' }
            ]"
            optionLabel="label"
            optionValue="value"
            :allowEmpty="false"
            size="small"
            @update:modelValue="tableFilter = ''"
          />
        </div>
      </template>
      <template #content>
        <SgaDataTableCard
          :rows="filteredRows"
          v-model:search="tableSearch"
          :searchPlaceholder="tableVariant === 'usuarios' ? 'Buscar usuario...' : 'Buscar despacho o cliente...'"
        >
          <template #filters>
            <Select
              v-model="tableFilter"
              :options="tableVariant === 'usuarios'
                ? [{ label: 'Todos', value: '' }, { label: 'ACTIVO', value: 'ACTIVO' }, { label: 'INACTIVO', value: 'INACTIVO' }]
                : [{ label: 'Todos', value: '' }, { label: 'EN_RUTA', value: 'EN_RUTA' }, { label: 'PENDIENTE', value: 'PENDIENTE' }]"
              optionLabel="label"
              optionValue="value"
              size="small"
              placeholder="Estado"
            />
          </template>
          <template #columns>
            <Column field="codigo" header="Código" sortable>
              <template #body="{ data }">
                <code class="font-mono">{{ data.codigo }}</code>
              </template>
            </Column>
            <Column field="nombre" :header="tableVariant === 'usuarios' ? 'Colaborador' : 'Cliente'" sortable />
            <Column field="detalle" :header="tableVariant === 'usuarios' ? 'Rol' : 'Ruta / Unidad'" />
            <Column field="estado" header="Estado">
              <template #body="{ data }">
                <Tag
                  :value="data.estado"
                  :severity="data.estado === 'ACTIVO' || data.estado === 'EN_RUTA' ? 'success' : 'warn'"
                  rounded
                />
              </template>
            </Column>
          </template>
        </SgaDataTableCard>
      </template>
    </Card>

    <!-- 5. SgaFormShell + SgaFormBlock + SgaSummaryCard -->
    <Card class="master-section-card">
      <template #title>
        <div class="master-title-row">
          <span>5. SgaFormShell.vue — Formulario Maestro de 2 Columnas</span>
          <div class="demo-row">
            <SelectButton
              v-model="formVariant"
              :options="[
                { label: 'Variante Puerto', value: 'puerto' },
                { label: 'Variante Sede', value: 'sede' }
              ]"
              optionLabel="label"
              optionValue="value"
              :allowEmpty="false"
              size="small"
            />
            <Button
              :label="formEditMode ? 'Ver Modo Registro' : 'Ver Modo Edición'"
              severity="secondary"
              outlined
              size="small"
              @click="formEditMode = !formEditMode"
            />
          </div>
        </div>
      </template>
      <template #content>
        <SgaFormShell
          :kicker="formConfig.kicker"
          :backLabel="formConfig.backLabel"
          :title="formConfig.title"
          :description="formConfig.desc"
          :isEditMode="formEditMode"
          :submitLabel="formConfig.submitLabel"
          @submit="toast.add({ severity: 'success', summary: 'Guardado', detail: formConfig.title, life: 2500 })"
          @cancel="toast.add({ severity: 'info', summary: 'Cancelado', detail: 'Acción cancelar del maestro', life: 2000 })"
        >
          <SgaFormBlock index="01" title="Identificación" subtitle="Código único y denominación principal">
            <div class="master-fields-grid">
              <div class="field-col">
                <label>Código</label>
                <InputText v-model="formData.codigo" class="font-mono" fluid />
              </div>
              <div class="field-col">
                <label>Nombre Oficial</label>
                <InputText v-model="formData.nombre" fluid />
              </div>
            </div>
          </SgaFormBlock>

          <SgaFormBlock index="02" title="Parámetros Operativos" subtitle="Campos específicos según la variante seleccionada">
            <div v-if="formVariant === 'puerto'" class="master-fields-grid">
              <div class="field-col">
                <label>Modalidad</label>
                <Select v-model="formData.modalidad" :options="['MARITIMO', 'AEREO', 'TERRESTRE']" fluid />
              </div>
              <div class="field-col">
                <label>País</label>
                <InputText v-model="formData.pais" fluid />
              </div>
            </div>
            <div v-else class="master-fields-grid">
              <div class="field-col">
                <label>Departamento / Jurisdicción</label>
                <InputText v-model="formData.departamento" fluid />
              </div>
              <div class="field-col">
                <label>Correo Operativo</label>
                <InputText v-model="formData.correo" fluid />
              </div>
            </div>
          </SgaFormBlock>

          <template #sidebar>
            <SgaSummaryCard :title="formConfig.summaryTitle" :items="formConfig.summaryItems" />
          </template>
        </SgaFormShell>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.masters-showcase {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.master-section-card {
  margin-bottom: 0.25rem;
}

.master-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.master-fields-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.field-col {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.field-col label {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text-muted);
}

@media (max-width: 640px) {
  .master-fields-grid {
    grid-template-columns: 1fr;
  }
}
</style>
