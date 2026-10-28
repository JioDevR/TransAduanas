import { ref, computed } from 'vue'

export function useModalidadPuertos({ puertos, regimen, form }) {
  // Modalidad activa: 'MARITIMO' (por defecto) o 'AEREO'
  const modalidadTransporte = ref('MARITIMO')

  // Países seleccionados para filtro
  const paisOrigen = ref('')
  const paisDestino = ref('')

  const matchesModalidad = (puerto, modalidad) => {
    if (!puerto || !puerto.tipo) return false
    const t = puerto.tipo.toUpperCase()
    if (modalidad === 'AEREO') {
      return t === 'AEREO'
    }
    return t === 'MARITIMO' || t === 'FLUVIAL'
  }

  // Lista de todos los terminales que coinciden con la modalidad activa
  const puertosPorModalidad = computed(() => {
    const list = Array.isArray(puertos?.value) ? puertos.value : []
    return list.filter((p) => matchesModalidad(p, modalidadTransporte.value))
  })

  // Lista única de países para la modalidad activa
  const paisesDisponibles = computed(() => {
    const set = new Set()
    puertosPorModalidad.value.forEach((p) => {
      if (p.pais) set.add(p.pais)
    })
    return Array.from(set).sort((a, b) => a.localeCompare(b))
  })

  const paisesOrigenDisponibles = computed(() => paisesDisponibles.value)
  const paisesDestinoDisponibles = computed(() => paisesDisponibles.value)

  // Terminales filtrados por país y modalidad activa
  const puertosOrigenFiltrados = computed(() => {
    if (!paisOrigen.value) return []
    return puertosPorModalidad.value.filter((p) => p.pais === paisOrigen.value)
  })

  const puertosDestinoFiltrados = computed(() => {
    if (!paisDestino.value) return []
    return puertosPorModalidad.value.filter((p) => p.pais === paisDestino.value)
  })

  // Reglas automáticas según Régimen Aduanero
  const aplicarReglasRegimen = (regimenActual = regimen?.value) => {
    if (regimenActual === 'IMPORTACION_DEFINITIVA') {
      paisDestino.value = 'Perú'
    } else if (regimenActual === 'EXPORTACION_DEFINITIVA') {
      paisOrigen.value = 'Perú'
    }
  }

  // Cambiar modalidad con saneamiento de puertos incompatibles
  const setModalidad = (nuevaModalidad) => {
    if (modalidadTransporte.value === nuevaModalidad) return
    modalidadTransporte.value = nuevaModalidad

    const list = Array.isArray(puertos?.value) ? puertos.value : []
    
    // Verificar origen
    if (form?.value?.idPuertoEnvio) {
      const pEnvio = list.find((p) => p.idPuerto === form.value.idPuertoEnvio)
      if (!matchesModalidad(pEnvio, nuevaModalidad)) {
        form.value.idPuertoEnvio = null
      }
    }

    // Verificar destino
    if (form?.value?.idPuertoDestino) {
      const pDestino = list.find((p) => p.idPuerto === form.value.idPuertoDestino)
      if (!matchesModalidad(pDestino, nuevaModalidad)) {
        form.value.idPuertoDestino = null
      }
    }

    // Si los países seleccionados no tienen terminales en la nueva modalidad, limpiar
    const paisesValidos = new Set(
      list.filter((p) => matchesModalidad(p, nuevaModalidad)).map((p) => p.pais)
    )

    if (paisOrigen.value && !paisesValidos.has(paisOrigen.value)) {
      paisOrigen.value = ''
    }
    if (paisDestino.value && !paisesValidos.has(paisDestino.value)) {
      paisDestino.value = ''
    }

    // Reaplicar default según régimen
    aplicarReglasRegimen()
  }

  // Infiere la modalidad y países a partir de los IDs asignados (modo edición)
  const inferirDesdePuertos = () => {
    const list = Array.isArray(puertos?.value) ? puertos.value : []
    const pEnvio = list.find((p) => p.idPuerto === form?.value?.idPuertoEnvio)
    const pDestino = list.find((p) => p.idPuerto === form?.value?.idPuertoDestino)

    const pRef = pEnvio || pDestino
    if (pRef) {
      modalidadTransporte.value = pRef.tipo === 'AEREO' ? 'AEREO' : 'MARITIMO'
    }

    if (pEnvio?.pais) {
      paisOrigen.value = pEnvio.pais
    }
    if (pDestino?.pais) {
      paisDestino.value = pDestino.pais
    }

    // Si falta alguno, aplicar reglas de régimen
    if (!paisDestino.value && regimen?.value === 'IMPORTACION_DEFINITIVA') {
      paisDestino.value = 'Perú'
    }
    if (!paisOrigen.value && regimen?.value === 'EXPORTACION_DEFINITIVA') {
      paisOrigen.value = 'Perú'
    }
  }

  return {
    modalidadTransporte,
    paisOrigen,
    paisDestino,
    paisesDisponibles,
    paisesOrigenDisponibles,
    paisesDestinoDisponibles,
    puertosOrigenFiltrados,
    puertosDestinoFiltrados,
    setModalidad,
    aplicarReglasRegimen,
    inferirDesdePuertos
  }
}
