/**
 * Utilidad compartida para descarga y visualización de archivos binarios Blob (PDFs de expedientes,
 * constancias DAM, comprobantes de pago, liquidaciones y cotizaciones).
 * Centraliza la creación y limpieza del objeto URL y la descarga / vista previa en el navegador.
 */

/**
 * Descarga directamente un archivo binario Blob forzando el diálogo de descarga del navegador.
 *
 * @param {Blob|ArrayBuffer} data - Contenido binario del archivo
 * @param {string} filename - Nombre con el que se guardará el archivo
 * @param {string} mimeType - Tipo MIME del archivo (default: 'application/pdf')
 */
export function downloadBlob(data, filename, mimeType = 'application/pdf') {
  if (!data) return
  const blob = data instanceof Blob ? data : new Blob([data], { type: mimeType })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', filename)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

/**
 * Abre un archivo binario Blob en una nueva pestaña del navegador para visualización inmediata
 * (visor nativo PDF o render de imagen del navegador).
 *
 * @param {Blob|ArrayBuffer} data - Contenido binario del archivo
 * @param {string} mimeType - Tipo MIME del archivo (default: 'application/pdf')
 */
export function openBlobInNewTab(data, mimeType = 'application/pdf') {
  if (!data) return
  const blob = data instanceof Blob ? data : new Blob([data], { type: mimeType })
  const url = window.URL.createObjectURL(blob)
  window.open(url, '_blank')
}

export default {
  downloadBlob,
  openBlobInNewTab
}
