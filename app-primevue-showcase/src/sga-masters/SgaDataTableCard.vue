<template>
  <Card class="sga-datatable-card">
    <template #content>
      <div class="table-toolbar">
        <IconField class="search-field">
          <InputIcon class="pi pi-search" />
          <InputText
            :modelValue="search"
            :placeholder="searchPlaceholder"
            size="small"
            fluid
            @update:modelValue="$emit('update:search', $event)"
          />
        </IconField>

        <div class="filters-row">
          <slot name="filters" />
        </div>
      </div>

      <DataTable
        :value="rows"
        :paginator="paginator"
        :rows="rowsPerPage"
        :loading="loading"
        stripedRows
        removableSort
        size="small"
        responsiveLayout="scroll"
      >
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-inbox"></i>
            <span>{{ emptyMessage }}</span>
          </div>
        </template>
        <slot name="columns" />
      </DataTable>
    </template>
  </Card>
</template>

<script setup>
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import InputText from 'primevue/inputtext'

defineProps({
  rows: { type: Array, default: () => [] },
  search: { type: String, default: '' },
  searchPlaceholder: { type: String, default: 'Buscar registros...' },
  emptyMessage: { type: String, default: 'No se encontraron registros.' },
  paginator: { type: Boolean, default: true },
  rowsPerPage: { type: Number, default: 5 },
  loading: { type: Boolean, default: false }
})

defineEmits(['update:search'])
</script>

<style scoped>
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.search-field {
  min-width: 260px;
  flex: 1;
  max-width: 380px;
}

.filters-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 2rem 1rem;
  color: var(--color-text-muted);
}
</style>
