<template>
  <section class="ti-reports-grid">
    <!-- 1. Personal Interno -->
    <article class="sga-card-surface ti-report-card">
      <div class="ti-report-head">
        <div>
          <span class="sga-kicker">DIRECTORIO INTERNO · PDF</span>
          <h3 class="ti-report-title">Personal Interno SGA</h3>
        </div>
        <i class="pi pi-users ti-report-icon"></i>
      </div>
      <p class="sga-text-muted ti-report-desc">
        Listado institucional de operadores con nombre completo, correo, teléfono, cargo, sede y estado de cuenta.
      </p>
      <div class="ti-report-options">
        <label class="ti-check-opt">
          <Checkbox v-model="soloActivosPersonal" binary />
          <span>Solo cuentas activas</span>
        </label>
      </div>
      <div class="ti-report-controls">
        <Button
          label="Ver PDF"
          icon="pi pi-file-pdf"
          size="small"
          :loading="loadingKey === 'personal-view'"
          @click="handlePersonalPdf('view')"
        />
        <Button
          icon="pi pi-download"
          severity="secondary"
          outlined
          size="small"
          title="Descargar PDF"
          :loading="loadingKey === 'personal-download'"
          @click="handlePersonalPdf('download')"
        />
      </div>
    </article>

    <!-- 2. Usuarios Portal B2B -->
    <article class="sga-card-surface ti-report-card">
      <div class="ti-report-head">
        <div>
          <span class="sga-kicker">ACCESOS EXTERNOS · PDF</span>
          <h3 class="ti-report-title">Cuentas Portal B2B</h3>
        </div>
        <i class="pi pi-id-card ti-report-icon"></i>
      </div>
      <p class="sga-text-muted ti-report-desc">
        Cuentas corporativas de clientes B2B vinculadas con razón social, RUC, representante, correo y estado.
      </p>
      <div class="ti-report-options">
        <label class="ti-check-opt">
          <Checkbox v-model="soloActivosPortal" binary />
          <span>Solo cuentas activas</span>
        </label>
      </div>
      <div class="ti-report-controls">
        <Button
          label="Ver PDF"
          icon="pi pi-file-pdf"
          size="small"
          :loading="loadingKey === 'portal-view'"
          @click="handlePortalPdf('view')"
        />
        <Button
          icon="pi pi-download"
          severity="secondary"
          outlined
          size="small"
          title="Descargar PDF"
          :loading="loadingKey === 'portal-download'"
          @click="handlePortalPdf('download')"
        />
      </div>
    </article>

    <!-- 3. Bitácora de Auditoría TI -->
    <article class="sga-card-surface ti-report-card">
      <div class="ti-report-head">
        <div>
          <span class="sga-kicker">SEGURIDAD Y TRAZABILIDAD · PDF</span>
          <h3 class="ti-report-title">Bitácora de Auditoría TI</h3>
        </div>
        <i class="pi pi-shield ti-report-icon"></i>
      </div>
      <p class="sga-text-muted ti-report-desc">
        Informe auditable de eventos de seguridad, accesos y cambios administrativos con filtros propios.
      </p>
      <div class="ti-report-filters">
        <Select
          v-model="audDias"
          :options="audPeriodoOptions"
          optionLabel="label"
          optionValue="value"
          size="small"
          fluid
        />
        <Select
          v-model="audModulo"
          :options="moduloOptions"
          optionLabel="label"
          optionValue="value"
          size="small"
          fluid
        />
        <Select
          v-model="audResultado"
          :options="resultadoOptions"
          optionLabel="label"
          optionValue="value"
          size="small"
          fluid
        />
      </div>
      <div class="ti-report-controls">
        <Button
          label="Ver PDF"
          icon="pi pi-file-pdf"
          size="small"
          :loading="loadingKey === 'auditoria-view'"
          @click="handleAuditoriaPdf('view')"
        />
        <Button
          icon="pi pi-download"
          severity="secondary"
          outlined
          size="small"
          title="Descargar PDF"
          :loading="loadingKey === 'auditoria-download'"
          @click="handleAuditoriaPdf('download')"
        />
      </div>
    </article>

    <!-- 4. Informe Técnico de Sistema & Almacenamiento -->
    <article class="sga-card-surface ti-report-card">
      <div class="ti-report-head">
        <div>
          <span class="sga-kicker">INFRAESTRUCTURA · PDF</span>
          <h3 class="ti-report-title">Estado del Sistema y Storage</h3>
        </div>
        <i class="pi pi-server ti-report-icon"></i>
      </div>
      <p class="sga-text-muted ti-report-desc">
        Snapshot técnico con estado de BD, memoria JVM, uso de disco, cuentas activas e integridad documental por módulo.
      </p>
      <div class="ti-report-options">
        <Select
          v-model="sysDias"
          :options="sysPeriodoOptions"
          optionLabel="label"
          optionValue="value"
          size="small"
          fluid
        />
      </div>
      <div class="ti-report-controls">
        <Button
          label="Ver PDF"
          icon="pi pi-file-pdf"
          size="small"
          :loading="loadingKey === 'sistema-view'"
          @click="handleSistemaPdf('view')"
        />
        <Button
          icon="pi pi-download"
          severity="secondary"
          outlined
          size="small"
          title="Descargar PDF"
          :loading="loadingKey === 'sistema-download'"
          @click="handleSistemaPdf('download')"
        />
      </div>
    </article>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import Select from 'primevue/select'
import { useNotification } from '@/shared/composables/useNotification'
import { downloadBlob, openBlobInNewTab } from '@/shared/utils/download'
import { userService } from '../../users/services/userService'
import { portalUserService } from '../../portal-users/services/portalUserService'
import { auditoriaService } from '../../services/auditoriaService'
import { sistemaService } from '../../services/sistemaService'

const props = defineProps({
  filterDias: { type: Number, default: 7 },
  filterModulo: { type: String, default: 'TODOS' }
})

const { showError } = useNotification()
const loadingKey = ref('')

const soloActivosPersonal = ref(true)
const soloActivosPortal = ref(true)
const audDias = ref(props.filterDias || 7)
const audModulo = ref(props.filterModulo || 'TODOS')
const audResultado = ref('ALL')
const sysDias = ref(props.filterDias || 7)

const audPeriodoOptions = [
  { label: 'Últimos 7 días', value: 7 },
  { label: 'Últimos 14 días', value: 14 },
  { label: 'Últimos 30 días', value: 30 },
  { label: 'Todo el histórico', value: 0 }
]

const sysPeriodoOptions = [
  { label: 'Ventana: 7 días', value: 7 },
  { label: 'Ventana: 14 días', value: 14 },
  { label: 'Ventana: 30 días', value: 30 }
]

const moduloOptions = [
  { label: 'Todos los módulos', value: 'TODOS' },
  { label: 'Seguridad TI', value: 'SEGURIDAD_TI' },
  { label: 'Comercial', value: 'COMERCIAL' },
  { label: 'Aduanas', value: 'ADUANAS' },
  { label: 'Finanzas', value: 'FINANZAS' },
  { label: 'Almacén', value: 'ALMACEN' },
  { label: 'Transporte', value: 'TRANSPORTE' },
  { label: 'Portal B2B', value: 'PORTAL_B2B' },
  { label: 'Documentos', value: 'DOCUMENTOS' },
  { label: 'Maestros', value: 'MAESTROS' }
]

const resultadoOptions = [
  { label: 'Todos los resultados', value: 'ALL' },
  { label: 'Solo éxitos', value: 'EXITO' },
  { label: 'Solo fallos / alertas', value: 'FALLO' }
]

const buildLocalDateDesde = (dias) => {
  if (!dias || dias <= 0) return undefined
  const d = new Date()
  d.setDate(d.getDate() - dias + 1)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const deliverPdf = (blob, filename, mode) => {
  if (mode === 'download') {
    downloadBlob(blob, filename, 'application/pdf')
  } else {
    openBlobInNewTab(blob, 'application/pdf')
  }
}

const handlePersonalPdf = async (mode) => {
  if (loadingKey.value) return
  loadingKey.value = `personal-${mode}`
  try {
    const blob = await userService.downloadPdf(soloActivosPersonal.value)
    const name = soloActivosPersonal.value ? 'reporte-usuarios-activos.pdf' : 'reporte-usuarios-listado.pdf'
    deliverPdf(blob, name, mode)
  } catch {
    showError('No se pudo generar el reporte PDF de personal interno.')
  } finally {
    loadingKey.value = ''
  }
}

const handlePortalPdf = async (mode) => {
  if (loadingKey.value) return
  loadingKey.value = `portal-${mode}`
  try {
    const blob = await portalUserService.downloadPdf(soloActivosPortal.value)
    const name = soloActivosPortal.value ? 'reporte-usuarios-portal-activos.pdf' : 'reporte-usuarios-portal-listado.pdf'
    deliverPdf(blob, name, mode)
  } catch {
    showError('No se pudo generar el reporte PDF de cuentas Portal B2B.')
  } finally {
    loadingKey.value = ''
  }
}

const handleAuditoriaPdf = async (mode) => {
  if (loadingKey.value) return
  loadingKey.value = `auditoria-${mode}`
  const params = {
    modulo: audModulo.value && audModulo.value !== 'TODOS' ? audModulo.value : undefined,
    resultado: audResultado.value && audResultado.value !== 'ALL' ? audResultado.value : undefined,
    fechaDesde: buildLocalDateDesde(audDias.value)
  }
  try {
    const blob = await auditoriaService.downloadPdf(params)
    deliverPdf(blob, 'reporte-auditoria-ti.pdf', mode)
  } catch {
    showError('No se pudo generar el reporte PDF de auditoría TI.')
  } finally {
    loadingKey.value = ''
  }
}

const handleSistemaPdf = async (mode) => {
  if (loadingKey.value) return
  loadingKey.value = `sistema-${mode}`
  try {
    const blob = await sistemaService.downloadPdf(sysDias.value)
    deliverPdf(blob, 'reporte-estado-sistema-ti.pdf', mode)
  } catch {
    showError('No se pudo generar el informe técnico PDF de estado del sistema.')
  } finally {
    loadingKey.value = ''
  }
}
</script>

<style scoped>
.ti-reports-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.1rem;
}

.ti-report-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 0.85rem;
  padding: var(--sga-subpanel-padding, 1.25rem 1.35rem);
}

.ti-report-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
}

.ti-report-title {
  margin: 0.2rem 0 0;
  font-size: 1.05rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.ti-report-icon {
  font-size: 1.25rem;
  color: var(--color-primary);
  opacity: 0.85;
}

.ti-report-desc {
  margin: 0;
  font-size: 0.83rem;
  line-height: 1.45;
}

.ti-report-options {
  display: flex;
  align-items: center;
  min-height: 34px;
}

.ti-report-filters {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: 0.5rem;
}

.ti-report-controls {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.5rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border-subtle);
}

.ti-check-opt {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  font-size: 0.82rem;
  color: var(--color-text-main);
  cursor: pointer;
}
</style>
