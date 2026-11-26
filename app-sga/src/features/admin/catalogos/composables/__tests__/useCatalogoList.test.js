import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'

vi.stubGlobal('localStorage', {
  getItem: vi.fn(() => null),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
})

const mockShowSuccess = vi.fn()
const mockShowError = vi.fn()
vi.mock('@/shared/composables/useNotification', () => ({
  useNotification: () => ({
    showSuccess: mockShowSuccess,
    showError: mockShowError
  })
}))

const mockConfirmAction = vi.fn()
vi.mock('@/shared/composables/useConfirmation', () => ({
  useConfirmation: () => ({
    confirmAction: (...args) => mockConfirmAction(...args)
  })
}))

const mockOpenBlob = vi.fn()
vi.mock('@/shared/utils/download', () => ({
  openBlobInNewTab: (...args) => mockOpenBlob(...args)
}))

import { useCatalogoList } from '../useCatalogoList'
import { useNavigationStore } from '@/core/stores/navigationStore'

describe('useCatalogoList', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  const createSubject = (overrides = {}) => {
    const loadFn = vi.fn().mockResolvedValue([
      { id: 1, codigo: 'B-02', nombre: 'Beta', estado: 'ACTIVO' },
      { id: 2, codigo: 'A-01', nombre: 'Alfa', estado: 'INACTIVO' }
    ])
    const deleteFn = vi.fn().mockResolvedValue({})
    const reactivateFn = vi.fn().mockResolvedValue({})
    const pdfFn = vi.fn().mockResolvedValue(new Blob(['pdf']))

    const composable = useCatalogoList({
      entityLabel: 'Registro',
      getId: (item) => item.id,
      getCode: (item) => item.codigo,
      activeStatus: 'ACTIVO',
      loadFn,
      deleteFn,
      reactivateFn,
      pdfFn,
      initialFilters: { estado: '', search: '', sortBy: 'CODIGO_ASC' },
      filterItem: (item, f) => {
        if (f.estado && item.estado !== f.estado) return false
        if (f.search) {
          const q = f.search.toLowerCase()
          return item.codigo.toLowerCase().includes(q) || item.nombre.toLowerCase().includes(q)
        }
        return true
      },
      sortItems: (list, sortBy) => {
        if (sortBy === 'CODIGO_ASC') return [...list].sort((a, b) => a.codigo.localeCompare(b.codigo))
        return list
      },
      ...overrides
    })

    return { ...composable, loadFn, deleteFn, reactivateFn, pdfFn }
  }

  it('carga registros, aplica filtros con toggle KPI y ordena resultados', async () => {
    const { loadItems, items, filteredItems, filters, toggleFilter, resetFilters } = createSubject()
    await loadItems()

    expect(items.value).toHaveLength(2)
    expect(filteredItems.value.map((x) => x.codigo)).toEqual(['A-01', 'B-02'])

    toggleFilter('estado', 'ACTIVO')
    expect(filters.estado).toBe('ACTIVO')
    expect(filteredItems.value).toHaveLength(1)

    // Segundo clic limpia el filtro
    toggleFilter('estado', 'ACTIVO')
    expect(filters.estado).toBe('')
    expect(filteredItems.value).toHaveLength(2)

    filters.search = 'beta'
    expect(filteredItems.value).toHaveLength(1)
    resetFilters()
    expect(filters.search).toBe('')
    expect(filteredItems.value).toHaveLength(2)
  })

  it('cierra formulario abierto cuando cambia navigationStore.navTick y muestra toast en onSaved', async () => {
    const nav = useNavigationStore()
    const { showForm, openCreate, onSaved, editingItem, loadItems } = createSubject()

    openCreate()
    expect(showForm.value).toBe(true)
    expect(editingItem.value).toBeNull()

    nav.navTick++
    await nextTick()
    expect(showForm.value).toBe(false)

    openCreate()
    onSaved({ isEdit: false })
    expect(showForm.value).toBe(false)
    expect(mockShowSuccess).toHaveBeenCalled()
    expect(loadItems).toBeDefined()
  })

  it('solicita confirmación al desactivar, reactiva directo y exporta PDF', async () => {
    const { items, loadItems, toggleEstado, exportPdf, deleteFn, reactivateFn, pdfFn } = createSubject()
    await loadItems()

    mockConfirmAction.mockResolvedValueOnce(false)
    await toggleEstado(items.value[0])
    expect(deleteFn).not.toHaveBeenCalled()

    mockConfirmAction.mockResolvedValueOnce(true)
    await toggleEstado(items.value[0])
    expect(deleteFn).toHaveBeenCalledWith(1)
    expect(mockShowSuccess).toHaveBeenCalled()

    await toggleEstado(items.value[1])
    expect(reactivateFn).toHaveBeenCalledWith(2)

    await exportPdf()
    expect(pdfFn).toHaveBeenCalled()
    expect(mockOpenBlob).toHaveBeenCalled()
  })

  it('muestra showError cuando falla la carga inicial', async () => {
    const loadFn = vi.fn().mockRejectedValueOnce(new Error('Network error'))
    const { loadItems } = createSubject({ loadFn })
    await loadItems()
    expect(mockShowError).toHaveBeenCalled()
  })
})
