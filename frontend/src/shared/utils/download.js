/**
 * Utilidad compartida para descarga de archivos Blob (PDF JasperReports, Excel, etc.)
 * Centraliza la creación y limpieza del objeto URL y el enlace simulado en el DOM.
 *
 * @param {Blob|ArrayBuffer} data - Contenido binario del archivo
 * @param {string} filename - Nombre con el que se guardará el archivo en el cliente
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

export default downloadBlob
