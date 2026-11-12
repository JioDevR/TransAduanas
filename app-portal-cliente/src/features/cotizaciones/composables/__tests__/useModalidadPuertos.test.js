import { describe, it, expect } from 'vitest'
import { ref } from 'vue'
import { useModalidadPuertos } from '../useModalidadPuertos'

describe('useModalidadPuertos', () => {
  const mockPuertos = [
    { idPuerto: 1, codigoPuerto: 'PECAL-DPW', nombre: 'Callao Muelle Sur', pais: 'Perú', tipo: 'MARITIMO' },
    { idPuerto: 2, codigoPuerto: 'PECAL-APM', nombre: 'Callao Muelle Norte', pais: 'Perú', tipo: 'MARITIMO' },
    { idPuerto: 10, codigoPuerto: 'PEYUR', nombre: 'Puerto Yurimaguas', pais: 'Perú', tipo: 'FLUVIAL' },
    { idPuerto: 11, codigoPuerto: 'PEAQP', nombre: 'Aeropuerto Internacional Jorge Chávez', pais: 'Perú', tipo: 'AEREO' },
    { idPuerto: 12, codigoPuerto: 'CNSHA', nombre: 'Puerto de Shanghái', pais: 'China', tipo: 'MARITIMO' },
    { idPuerto: 40, codigoPuerto: 'CNPVG', nombre: 'Aeropuerto Internacional Shanghái-Pudong', pais: 'China', tipo: 'AEREO' },
    { idPuerto: 20, codigoPuerto: 'USLAX', nombre: 'Puerto de Los Ángeles', pais: 'Estados Unidos', tipo: 'MARITIMO' }
  ]

  it('filtra países disponibles según la modalidad activa', () => {
    const puertos = ref(mockPuertos)
    const regimen = ref('IMPORTACION_DEFINITIVA')
    const form = ref({ idPuertoEnvio: null, idPuertoDestino: null })
    const { modalidadTransporte, paisesOrigenDisponibles } = useModalidadPuertos({ puertos, regimen, form })

    modalidadTransporte.value = 'MARITIMO'
    expect(paisesOrigenDisponibles.value).toContain('China')
    expect(paisesOrigenDisponibles.value).toContain('Estados Unidos')
    expect(paisesOrigenDisponibles.value).toContain('Perú')

    modalidadTransporte.value = 'AEREO'
    expect(paisesOrigenDisponibles.value).toContain('China')
    expect(paisesOrigenDisponibles.value).toContain('Perú')
    expect(paisesOrigenDisponibles.value).not.toContain('Estados Unidos')
  })

  it('filtra terminales por modalidad y país seleccionado', () => {
    const puertos = ref(mockPuertos)
    const regimen = ref('IMPORTACION_DEFINITIVA')
    const form = ref({ idPuertoEnvio: null, idPuertoDestino: null })
    const { modalidadTransporte, paisOrigen, puertosOrigenFiltrados } = useModalidadPuertos({ puertos, regimen, form })

    modalidadTransporte.value = 'MARITIMO'
    paisOrigen.value = 'China'
    expect(puertosOrigenFiltrados.value).toHaveLength(1)
    expect(puertosOrigenFiltrados.value[0].codigoPuerto).toBe('CNSHA')

    modalidadTransporte.value = 'AEREO'
    paisOrigen.value = 'China'
    expect(puertosOrigenFiltrados.value).toHaveLength(1)
    expect(puertosOrigenFiltrados.value[0].codigoPuerto).toBe('CNPVG')
  })

  it('aplica por defecto Perú como destino en Importación y como origen en Exportación', () => {
    const puertos = ref(mockPuertos)
    const regimen = ref('IMPORTACION_DEFINITIVA')
    const form = ref({ idPuertoEnvio: null, idPuertoDestino: null })
    const { paisOrigen, paisDestino, aplicarReglasRegimen } = useModalidadPuertos({ puertos, regimen, form })

    aplicarReglasRegimen('IMPORTACION_DEFINITIVA')
    expect(paisDestino.value).toBe('Perú')

    aplicarReglasRegimen('EXPORTACION_DEFINITIVA')
    expect(paisOrigen.value).toBe('Perú')
  })

  it('limpia terminales incompatibles al cambiar de modalidad', () => {
    const puertos = ref(mockPuertos)
    const regimen = ref('IMPORTACION_DEFINITIVA')
    const form = ref({ idPuertoEnvio: 12, idPuertoDestino: 1 }) // Shanghái y Callao (Marítimos)
    const { setModalidad } = useModalidadPuertos({ puertos, regimen, form })

    setModalidad('AEREO')
    expect(form.value.idPuertoEnvio).toBeNull()
    expect(form.value.idPuertoDestino).toBeNull()
  })

  it('infiere modalidad y países al recibir una cotización existente', () => {
    const puertos = ref(mockPuertos)
    const regimen = ref('IMPORTACION_DEFINITIVA')
    const form = ref({ idPuertoEnvio: 40, idPuertoDestino: 11 }) // Pudong y Jorge Chávez (Aéreos)
    const { modalidadTransporte, paisOrigen, paisDestino, inferirDesdePuertos } = useModalidadPuertos({ puertos, regimen, form })

    inferirDesdePuertos()
    expect(modalidadTransporte.value).toBe('AEREO')
    expect(paisOrigen.value).toBe('China')
    expect(paisDestino.value).toBe('Perú')
  })
})
