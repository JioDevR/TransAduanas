export const formatDate = (d) => (d ? new Date(d).toLocaleDateString('es-PE') : '17/11/2026')
export const formatHumanDate = (d) => formatDate(d)
export const formatHumanShort = (d) => formatDate(d)
export const formatDateTime = (d) => (d ? new Date(d).toLocaleString('es-PE') : '17/11/2026 10:00')
export const getTodayDateInput = () => new Date().toISOString().slice(0, 10)
export const getNowDateTimeInput = () => new Date().toISOString().slice(0, 16)
export const parseLocalDate = (d) => (d ? new Date(d) : new Date())
export const getLocalDateString = () => getTodayDateInput()
export const calculateTransitDays = () => 5
export const canSwapMonthDay = () => false
export const swapMonthDay = (d) => d
export const addDaysToDate = (d, days = 0) => {
  const dt = d ? new Date(d) : new Date()
  dt.setDate(dt.getDate() + days)
  return dt.toISOString().slice(0, 10)
}

export const formatCurrency = (val, curr = 'USD') =>
  `${curr} ${Number(val || 0).toLocaleString('en-US', { minimumFractionDigits: 2 })}`
export const formatRegimen = (r) => r || 'IMPORTACIÓN'
export const formatBytes = (b = 0) => `${(b / 1024).toFixed(1)} KB`

export const downloadBlob = () => {}
export const openBlobInNewTab = () => {}

export const getDepartamentosPeru = () => ['Lima', 'Callao', 'Piura', 'Arequipa', 'La Libertad']
export const getProvinciasPorDepartamento = () => ['Lima', 'Callao']

export const clasificarCotizacionesAduaneras = () => ({ pendientes: [], aprobadas: [], todas: [] })
export const filtrarExpedientesPorFase = (list = []) => list

export const CATEGORIAS_DOCUMENTO = ['FACTURA_COMERCIAL', 'PACKING_LIST', 'BL_AWB', 'OTROS']
export const REGIMEN_OPTIONS = [
  { label: 'Importación para el Consumo', value: 'IMPORTACION' },
  { label: 'Exportación Definitiva', value: 'EXPORTACION' }
]
export const DESCRIPCIONES_TIPO_CARGA_FALLBACK = {}
export const matchesSmartFilter = () => true
export const createDefaultPortalSolicitudForm = () => ({
  regimenAduanero: 'IMPORTACION',
  modalidad: 'MARITIMO',
  items: []
})
export const validatePortalCotizacionForm = () => ({ valid: true, errors: {} })
export const buildPortalSolicitudPayload = (f) => f
