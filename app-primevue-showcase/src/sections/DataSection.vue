<script setup>
import { ref } from 'vue'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Paginator from 'primevue/paginator'
import OrderList from 'primevue/orderlist'
import PickList from 'primevue/picklist'
import Tree from 'primevue/tree'
import Timeline from 'primevue/timeline'
import OrganizationChart from 'primevue/organizationchart'
import InputText from 'primevue/inputtext'

const search = ref('')
const rows = ref([
  { code: 'EXP-001', client: 'Agroexportadora Lima', regime: 'Exportación', amount: 4850, status: 'LEVANTE' },
  { code: 'EXP-002', client: 'Minera Andina SAC', regime: 'Importación', amount: 12300, status: 'CANAL ROJO' },
  { code: 'EXP-003', client: 'Textiles Pacífico', regime: 'Importación', amount: 3120, status: 'CANAL VERDE' },
  { code: 'EXP-004', client: 'Tech Imports Perú', regime: 'Importación', amount: 8900, status: 'EN TRÁNSITO' }
])

const orderItems = ref([
  { id: 1, name: '1. Cotización Comercial' },
  { id: 2, name: '2. Validación Financiera' },
  { id: 3, name: '3. Numeración DAM' },
  { id: 4, name: '4. Levante Autorizado' }
])

const pickItems = ref([
  [
    { id: 1, name: 'Factura Comercial' },
    { id: 2, name: 'Packing List' },
    { id: 3, name: 'Bill of Lading (BL)' }
  ],
  [{ id: 4, name: 'Ticket Balanza VGM' }]
])

const treeNodes = ref([
  {
    key: '0',
    label: 'Expediente EXP-2026-001',
    icon: 'pi pi-folder',
    children: [
      { key: '0-0', label: 'Factura.pdf', icon: 'pi pi-file-pdf' },
      { key: '0-1', label: 'BL_Callao.pdf', icon: 'pi pi-file-pdf' }
    ]
  },
  {
    key: '1',
    label: 'Liquidación Tributaria',
    icon: 'pi pi-folder',
    children: [{ key: '1-0', label: 'DAM_40.xml', icon: 'pi pi-file' }]
  }
])

const timelineEvents = [
  { status: 'Cotización aprobada', date: '15/11/2026 09:00', color: '#3b82f6' },
  { status: 'Pago validado', date: '15/11/2026 14:30', color: '#10b981' },
  { status: 'Canal Verde SUNAT', date: '16/11/2026 11:15', color: '#22c55e' }
]

const orgData = ref({
  key: '0',
  label: 'Gerencia General',
  children: [
    { key: '0-0', label: 'Comercial & Finanzas' },
    { key: '0-1', label: 'Aduanas & Almacén' },
    { key: '0-2', label: 'Transporte Flota' }
  ]
})

const first = ref(0)
</script>

<template>
  <div>
    <h2 class="section-title">Datos, Tablas y Listas</h2>
    <p class="section-desc">DataTable, Paginator, OrderList, PickList, Tree, Timeline y OrganizationChart.</p>

    <div class="demo-box">
      <Card>
        <template #title>DataTable (ordenamiento, selección y Paginator)</template>
        <template #content>
          <div class="demo-row" style="margin-bottom: 0.75rem">
            <InputText v-model="search" placeholder="Filtrar..." />
          </div>
          <DataTable :value="rows.filter(r => r.client.toLowerCase().includes(search.toLowerCase()) || r.code.toLowerCase().includes(search.toLowerCase()))" sortMode="multiple" stripedRows>
            <Column field="code" header="Código" sortable />
            <Column field="client" header="Cliente" sortable />
            <Column field="regime" header="Régimen" sortable />
            <Column field="amount" header="Monto USD" sortable />
            <Column field="status" header="Estado">
              <template #body="{ data }">
                <Tag :value="data.status" :severity="data.status === 'CANAL ROJO' ? 'danger' : 'success'" rounded />
              </template>
            </Column>
          </DataTable>
          <Paginator v-model:first="first" :rows="10" :totalRecords="120" />
        </template>
      </Card>

      <div class="showcase-grid">
        <Card>
          <template #title>OrderList</template>
          <template #content>
            <OrderList v-model="orderItems" dataKey="id" breakpoint="575px">
              <template #option="{ option }">{{ option.name }}</template>
            </OrderList>
          </template>
        </Card>

        <Card>
          <template #title>Tree & Timeline</template>
          <template #content>
            <Tree :value="treeNodes" selectionMode="single" />
            <Timeline :value="timelineEvents" style="margin-top: 1rem">
              <template #content="slotProps">
                <strong>{{ slotProps.item.status }}</strong>
                <div style="font-size: 0.8rem; opacity: 0.7">{{ slotProps.item.date }}</div>
              </template>
            </Timeline>
          </template>
        </Card>
      </div>

      <Card>
        <template #title>PickList</template>
        <template #content>
          <PickList v-model="pickItems" dataKey="id" breakpoint="960px">
            <template #option="{ option }">{{ option.name }}</template>
          </PickList>
        </template>
      </Card>

      <Card>
        <template #title>OrganizationChart</template>
        <template #content>
          <OrganizationChart :value="orgData">
            <template #default="slotProps">
              <span>{{ slotProps.node.label }}</span>
            </template>
          </OrganizationChart>
        </template>
      </Card>
    </div>
  </div>
</template>
