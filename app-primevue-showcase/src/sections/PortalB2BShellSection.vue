<template>
  <section class="section-block">
    <h2 class="section-title">Portal B2B — Propuesta Shell Unificado (sgaUI)</h2>
    <p class="section-desc">
      Prototipo en vivo del cascarón de <code>app-sga</code> (<code>sgaUI/sga-shell.css</code>) adaptado a las
      4 opciones reales de <code>app-portal-cliente</code>: <strong>Mis Cotizaciones</strong>,
      <strong>Expedientes &amp; Tracking</strong>, <strong>Cobros y Pagos</strong> y <strong>Notificaciones</strong>,
      usando componentes nativos de PrimeVue 4 y tokens <em>Warm Editorial Pro</em>.
    </p>

    <div class="shell-preview-frame">
      <PortalShellLayout
        v-model:activeKey="activePortalView"
        :unread-count="unreadCount"
        :is-dark="isDark"
        @toggle-theme="emit('toggle-theme')"
        @logout="handleLogoutDemo"
      >
        <div class="portal-demo-stack">
          <!-- 1. MIS COTIZACIONES -->
          <template v-if="activePortalView === 'cotizaciones'">
            <div class="portal-view-bar">
              <p class="portal-view-sub">
                Consulte sus cotizaciones de importación y exportación, desglose de costos y aprobación de servicios.
              </p>
              <Button
                label="Solicitar Nueva Cotización"
                icon="pi pi-plus"
                size="small"
                @click="notifyDemo('Nueva Cotización', 'Abre el stepper PortalCotizacionForm')"
              />
            </div>

            <SgaKpiStrip
              :items="cotizacionesKpis"
              :active-key="kpiFilter"
              @select="(k) => (kpiFilter = k)"
            />

            <Card>
              <template #content>
                <div class="portal-filters-row">
                  <IconField class="portal-search-field">
                    <InputIcon class="pi pi-search" />
                    <InputText
                      v-model="searchCotizacion"
                      placeholder="Buscar por código, mercancía o puerto..."
                      fluid
                    />
                  </IconField>
                  <Select
                    v-model="estadoFilter"
                    :options="estadoOptions"
                    optionLabel="label"
                    optionValue="value"
                    placeholder="Todos los estados"
                    class="portal-filter-select"
                  />
                </div>

                <DataTable :value="filteredCotizaciones" stripedRows size="small">
                  <Column field="codigo" header="Código">
                    <template #body="{ data }">
                      <code class="portal-code">{{ data.codigo }}</code>
                    </template>
                  </Column>
                  <Column field="regimen" header="Régimen" />
                  <Column field="mercancia" header="Mercancía" />
                  <Column field="puerto" header="Puerto / Ruta" />
                  <Column field="totalUsd" header="Total (USD)">
                    <template #body="{ data }">
                      <span class="portal-num">$ {{ data.totalUsd.toFixed(2) }}</span>
                    </template>
                  </Column>
                  <Column field="estado" header="Estado">
                    <template #body="{ data }">
                      <Tag :value="data.estado" :severity="cotizacionSeverity(data.estado)" />
                    </template>
                  </Column>
                  <Column header="Acciones" style="width: 110px">
                    <template #body="{ data }">
                      <Button
                        label="Detalle"
                        icon="pi pi-eye"
                        size="small"
                        variant="text"
                        @click="notifyDemo('Detalle', `Cotización ${data.codigo}`)"
                      />
                    </template>
                  </Column>
                </DataTable>
              </template>
            </Card>
          </template>

          <!-- 2. EXPEDIENTES & TRACKING DAM -->
          <template v-else-if="activePortalView === 'expedientes'">
            <div class="portal-view-bar">
              <p class="portal-view-sub">
                Supervise sus operaciones en curso, canal SUNAT asignado y bóveda documental oficial.
              </p>
              <Button
                label="Actualizar Tablero"
                icon="pi pi-refresh"
                severity="secondary"
                variant="outlined"
                size="small"
                @click="notifyDemo('Tablero actualizado', 'Tracking sincronizado con SUNAT')"
              />
            </div>

            <SgaKpiStrip :items="expedientesKpis" />

            <Card>
              <template #content>
                <DataTable :value="expedientesRows" stripedRows size="small">
                  <Column field="codigo" header="Expediente">
                    <template #body="{ data }">
                      <code class="portal-code">{{ data.codigo }}</code>
                    </template>
                  </Column>
                  <Column field="dam" header="N° DAM">
                    <template #body="{ data }">
                      <span class="portal-num">{{ data.dam }}</span>
                    </template>
                  </Column>
                  <Column field="regimen" header="Régimen" />
                  <Column field="canal" header="Canal SUNAT">
                    <template #body="{ data }">
                      <Tag :value="data.canal" :severity="canalSeverity(data.canal)" />
                    </template>
                  </Column>
                  <Column field="etapa" header="Etapa Actual" />
                  <Column header="Bóveda" style="width: 120px">
                    <template #body="{ data }">
                      <Button
                        label="Documentos"
                        icon="pi pi-folder-open"
                        size="small"
                        variant="outlined"
                        @click="notifyDemo('Bóveda Documental', `Archivos de ${data.codigo}`)"
                      />
                    </template>
                  </Column>
                </DataTable>
              </template>
            </Card>
          </template>

          <!-- 3. COBROS Y PAGOS -->
          <template v-else-if="activePortalView === 'pagos'">
            <div class="portal-view-bar">
              <p class="portal-view-sub">
                Estado de cuenta empresarial en USD, registro de abonos bancarios y subsanación de observaciones.
              </p>
              <Button
                label="Registrar Comprobante ($ USD)"
                icon="pi pi-plus-circle"
                size="small"
                @click="notifyDemo('Registrar Pago', 'Navega a /pagos/registrar')"
              />
            </div>

            <Message severity="warn" :closable="false">
              Atención: Tiene 1 abono bancario con observación de Finanzas. Regularice el comprobante para continuar el despacho.
            </Message>

            <SgaKpiStrip :items="pagosKpis" />

            <Card>
              <template #content>
                <DataTable :value="pagosRows" stripedRows size="small">
                  <Column field="referencia" header="Cotización / Exp.">
                    <template #body="{ data }">
                      <code class="portal-code">{{ data.referencia }}</code>
                    </template>
                  </Column>
                  <Column field="concepto" header="Concepto" />
                  <Column field="montoUsd" header="Monto (USD)">
                    <template #body="{ data }">
                      <span class="portal-num">$ {{ data.montoUsd.toFixed(2) }}</span>
                    </template>
                  </Column>
                  <Column field="estado" header="Estado">
                    <template #body="{ data }">
                      <Tag :value="data.estado" :severity="pagoSeverity(data.estado)" />
                    </template>
                  </Column>
                  <Column header="Acción" style="width: 130px">
                    <template #body="{ data }">
                      <Button
                        :label="data.estado === 'OBSERVADO' ? 'Subsanar' : 'Ver Voucher'"
                        :severity="data.estado === 'OBSERVADO' ? 'warn' : 'secondary'"
                        size="small"
                        variant="outlined"
                        @click="notifyDemo(data.estado === 'OBSERVADO' ? 'Subsanar Abono' : 'Comprobante', data.referencia)"
                      />
                    </template>
                  </Column>
                </DataTable>
              </template>
            </Card>
          </template>

          <!-- 4. NOTIFICACIONES -->
          <template v-else>
            <div class="portal-view-bar">
              <p class="portal-view-sub">
                Avisos oficiales, cotizaciones aprobadas y trazabilidad aduanera en tiempo real.
              </p>
              <Button
                label="Marcar todas como leídas"
                icon="pi pi-check-double"
                size="small"
                :disabled="unreadCount === 0"
                @click="markAllRead"
              />
            </div>

            <Card>
              <template #content>
                <DataTable :value="notificacionesRows" stripedRows size="small">
                  <Column field="titulo" header="Aviso" />
                  <Column field="categoria" header="Categoría">
                    <template #body="{ data }">
                      <Tag :value="data.categoria" severity="secondary" />
                    </template>
                  </Column>
                  <Column field="fecha" header="Fecha">
                    <template #body="{ data }">
                      <span class="portal-num">{{ data.fecha }}</span>
                    </template>
                  </Column>
                  <Column field="leida" header="Estado" style="width: 120px">
                    <template #body="{ data }">
                      <Tag
                        :value="data.leida ? 'Leída' : 'No leída'"
                        :severity="data.leida ? 'secondary' : 'warn'"
                      />
                    </template>
                  </Column>
                </DataTable>
              </template>
            </Card>
          </template>
        </div>
      </PortalShellLayout>
    </div>
  </section>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import PortalShellLayout from '../sga-components/portal-cliente/PortalShellLayout.vue'
import SgaKpiStrip from '../sga-masters/SgaKpiStrip.vue'

defineProps({
  isDark: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['toggle-theme'])

const toast = useToast()
const activePortalView = ref('cotizaciones')
const searchCotizacion = ref('')
const estadoFilter = ref('')
const kpiFilter = ref(null)

const estadoOptions = [
  { label: 'Todos los estados', value: '' },
  { label: 'Aprobadas', value: 'APROBADA' },
  { label: 'En Evaluación', value: 'PENDIENTE' },
  { label: 'Observadas', value: 'OBSERVADA' }
]

const cotizacionesRows = ref([
  { codigo: 'COT-2026-0142', regimen: 'Importación Definitiva', mercancia: 'Monitores OLED 4K', puerto: 'Callao (PECAL)', totalUsd: 3840.5, estado: 'APROBADA' },
  { codigo: 'COT-2026-0149', regimen: 'Importación Definitiva', mercancia: 'Compresores Inverter', puerto: 'Callao (PECAL)', totalUsd: 2190.0, estado: 'PENDIENTE' },
  { codigo: 'COT-2026-0153', regimen: 'Exportación Definitiva', mercancia: 'Repuestos Industriales', puerto: 'Paita (PEPAI)', totalUsd: 1620.75, estado: 'OBSERVADA' }
])

const cotizacionesKpis = computed(() => [
  { key: 'ALL', label: 'Total Cotizaciones', value: cotizacionesRows.value.length, icon: 'pi pi-file', severity: 'neutral' },
  { key: 'APROBADA', label: 'Aprobadas', value: 1, icon: 'pi pi-check-circle', severity: 'success' },
  { key: 'PENDIENTE', label: 'En Evaluación', value: 1, icon: 'pi pi-clock', severity: 'warn' },
  { key: 'TOTAL', label: 'Valor Cotizado', value: '$ 7,651.25', icon: 'pi pi-dollar', severity: 'info' }
])

const filteredCotizaciones = computed(() => {
  const q = searchCotizacion.value.trim().toLowerCase()
  const activeEstado = kpiFilter.value && ['APROBADA', 'PENDIENTE'].includes(kpiFilter.value)
    ? kpiFilter.value
    : estadoFilter.value

  return cotizacionesRows.value.filter((row) => {
    const matchesText = !q || [row.codigo, row.mercancia, row.puerto].some((v) => v.toLowerCase().includes(q))
    const matchesEstado = !activeEstado || row.estado === activeEstado
    return matchesText && matchesEstado
  })
})

const expedientesKpis = [
  { key: 'total', label: 'Total Operaciones', value: 8, icon: 'pi pi-folder', severity: 'neutral' },
  { key: 'abiertos', label: 'Abiertos en Trámite', value: 3, icon: 'pi pi-sync', severity: 'info' },
  { key: 'cerrados', label: 'Finalizados', value: 5, icon: 'pi pi-check-circle', severity: 'success' },
  { key: 'levante', label: 'Con Levante', value: 2, icon: 'pi pi-shield', severity: 'warn' }
]

const expedientesRows = [
  { codigo: 'EXP-2026-0088', dam: '118-2026-10-041290', regimen: 'Importación', canal: 'VERDE', etapa: 'Levante Concedido' },
  { codigo: 'EXP-2026-0091', dam: '118-2026-10-042811', regimen: 'Importación', canal: 'NARANJA', etapa: 'Revisión Documentaria' },
  { codigo: 'EXP-2026-0094', dam: '118-2026-40-010552', regimen: 'Exportación', canal: 'ROJO', etapa: 'Aforo Físico Programado' }
]

const pagosKpis = [
  { key: 'cotizado', label: 'Total Contratado', value: '$ 7,651.25', icon: 'pi pi-file', severity: 'neutral' },
  { key: 'abonado', label: 'Total Amortizado', value: '$ 4,200.00', icon: 'pi pi-check-circle', severity: 'success' },
  { key: 'saldo', label: 'Saldo Pendiente', value: '$ 3,451.25', icon: 'pi pi-wallet', severity: 'warn' },
  { key: 'vouchers', label: 'Vouchers', value: 3, icon: 'pi pi-receipt', severity: 'info' }
]

const pagosRows = [
  { referencia: 'COT-2026-0142', concepto: 'Anticipo Agenciamiento & Flete', montoUsd: 2400.0, estado: 'CONCILIADO' },
  { referencia: 'COT-2026-0149', concepto: 'Derechos Aduaneros DAM', montoUsd: 1800.0, estado: 'EN REVISIÓN' },
  { referencia: 'COT-2026-0153', concepto: 'Abono Operación Exportación', montoUsd: 850.0, estado: 'OBSERVADO' }
]

const notificacionesRows = ref([
  { id: 1, titulo: 'Cotización COT-2026-0142 aprobada por Gerencia Comercial', categoria: 'COTIZACIÓN', fecha: '18/11/2026 10:15', leida: false },
  { id: 2, titulo: 'Canal NARANJA asignado a DAM 118-2026-10-042811', categoria: 'DAM', fecha: '18/11/2026 09:40', leida: false },
  { id: 3, titulo: 'Abono observado: adjuntar voucher legible para COT-2026-0153', categoria: 'PAGO', fecha: '17/11/2026 17:05', leida: true }
])

const unreadCount = computed(() => notificacionesRows.value.filter((n) => !n.leida).length)

const markAllRead = () => {
  notificacionesRows.value = notificacionesRows.value.map((n) => ({ ...n, leida: true }))
  toast.add({ severity: 'success', summary: 'Notificaciones', detail: 'Todas marcadas como leídas', life: 2200 })
}

const cotizacionSeverity = (estado) => ({ APROBADA: 'success', PENDIENTE: 'warn', OBSERVADA: 'danger' }[estado] || 'secondary')
const canalSeverity = (canal) => ({ VERDE: 'success', NARANJA: 'warn', ROJO: 'danger' }[canal] || 'secondary')
const pagoSeverity = (estado) => ({ CONCILIADO: 'success', 'EN REVISIÓN': 'info', OBSERVADO: 'warn' }[estado] || 'secondary')

const notifyDemo = (summary, detail) => {
  toast.add({ severity: 'info', summary, detail, life: 2200 })
}

const handleLogoutDemo = () => {
  toast.add({ severity: 'warn', summary: 'Sesión B2B', detail: 'Acción de cierre de sesión simulada en showcase', life: 2200 })
}
</script>

<style scoped>
.shell-preview-frame {
  border: 1px solid var(--p-surface-300);
  border-radius: 14px;
  overflow: hidden;
  height: 680px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  position: relative;
}

.shell-preview-frame :deep(.sga-shell) {
  min-height: 100%;
  height: 100%;
}

.shell-preview-frame :deep(.sga-sidebar) {
  height: 100%;
}

.shell-preview-frame :deep(.sga-main) {
  height: 100%;
  overflow-y: auto;
}

.portal-demo-stack {
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
}

.portal-view-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.portal-view-sub {
  margin: 0;
  font-size: 0.875rem;
  color: var(--color-text-muted);
}

.portal-filters-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.portal-search-field {
  flex: 1;
  min-width: 240px;
}

.portal-filter-select {
  min-width: 190px;
}

.portal-code,
.portal-num {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 0.84rem;
}
</style>
