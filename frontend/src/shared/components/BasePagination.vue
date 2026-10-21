<template>
  <div v-if="totalItems > 0" class="base-pagination">
    <span class="page-info">
      Mostrando {{ startIndex }} a {{ endIndex }} de {{ totalItems }} {{ itemLabel }}
    </span>
    <div class="pagination-controls">
      <button
        type="button"
        class="page-nav-btn"
        :disabled="currentPage <= 1 || disabled"
        title="Página anterior"
        @click="handlePrev"
      >
        <i class="pi pi-chevron-left"></i>
      </button>

      <span class="current-page-tag">
        Página {{ currentPage }} de {{ totalPages }}
      </span>

      <button
        type="button"
        class="page-nav-btn"
        :disabled="currentPage >= totalPages || disabled"
        title="Página siguiente"
        @click="handleNext"
      >
        <i class="pi pi-chevron-right"></i>
      </button>
    </div>
  </div>
</template>

<script setup>
/**
 * Componente genérico de paginación con estética cálida editorial de SGA.
 */
const props = defineProps({
  currentPage: {
    type: Number,
    required: true
  },
  totalPages: {
    type: Number,
    required: true
  },
  totalItems: {
    type: Number,
    default: 0
  },
  startIndex: {
    type: Number,
    default: 0
  },
  endIndex: {
    type: Number,
    default: 0
  },
  itemLabel: {
    type: String,
    default: 'registros'
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:currentPage', 'prev', 'next'])

const handlePrev = () => {
  if (props.currentPage > 1 && !props.disabled) {
    emit('update:currentPage', props.currentPage - 1)
    emit('prev')
  }
}

const handleNext = () => {
  if (props.currentPage < props.totalPages && !props.disabled) {
    emit('update:currentPage', props.currentPage + 1)
    emit('next')
  }
}
</script>

<style scoped>
.base-pagination {
  padding: 1rem 1.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid var(--color-border-subtle);
  background: var(--color-bg-subtle);
  flex-wrap: wrap;
  gap: 0.75rem;
}

.page-info {
  font-size: 0.785rem;
  color: var(--color-text-muted);
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.current-page-tag {
  font-size: 0.785rem;
  color: var(--color-text-main);
  font-weight: 600;
}

.page-nav-btn {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.page-nav-btn:hover:not(:disabled) {
  background: var(--color-bg-muted);
  border-color: var(--color-border-strong);
}

.page-nav-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
