import { describe, it, expect } from 'vitest'
import {
  SEDE_TIPO_LABELS,
  PUERTO_TIPO_LABELS,
  formatSedeTipo,
  getSedeEstadoSeverity,
  formatPuertoTipo,
  getPuertoTipoIcon,
  getCatalogoEstadoSeverity,
  classifyTipoCargaRegimen,
  getTipoCargaRegimenLabel,
  getTipoCargaRegimenSeverity,
  mergeNormalizedCategories,
  getPermisoVuceSeverity
} from '../gerenciaFormatters'

describe('gerenciaFormatters', () => {
  it('devuelve etiquetas y severidades PrimeVue 4 para Sedes (sin usar warning)', () => {
    expect(SEDE_TIPO_LABELS.SEDE_CENTRAL).toBeTruthy()
    expect(formatSedeTipo('SEDE_CENTRAL')).toBe('Sede Central')
    expect(formatSedeTipo('TERMINAL')).toBe('Terminal / Almacén')
    expect(getSedeEstadoSeverity('OPERATIVO')).toBe('success')
    expect(getSedeEstadoSeverity('MANTENIMIENTO')).toBe('warn')
    expect(getSedeEstadoSeverity('INACTIVO')).toBe('danger')
  })

  it('devuelve etiquetas e iconos para Puertos y severidades de estado de catálogos', () => {
    expect(PUERTO_TIPO_LABELS.MARITIMO).toBe('Marítimo')
    expect(formatPuertoTipo('AEREO')).toBe('Aéreo')
    expect(getPuertoTipoIcon('AEREO')).toBe('pi pi-send')
    expect(getCatalogoEstadoSeverity('ACTIVO')).toBe('success')
    expect(getCatalogoEstadoSeverity('INACTIVO')).toBe('secondary')
  })

  it('clasifica Tipos de Carga por prefijo de código real (IMP / EXP / GEN / ESP)', () => {
    expect(classifyTipoCargaRegimen('IMP-FCL-40')).toBe('IMP')
    expect(classifyTipoCargaRegimen('exp_reefer')).toBe('EXP')
    expect(classifyTipoCargaRegimen('GEN-SUELTA')).toBe('GEN')
    expect(classifyTipoCargaRegimen('IMO-CLASE3')).toBe('ESP')
    expect(getTipoCargaRegimenLabel('IMP-CONT')).toBe('Importación')
    expect(getTipoCargaRegimenSeverity('EXP-AIR')).toBe('success')
    expect(getTipoCargaRegimenSeverity('IMO-1')).toBe('warn')
  })

  it('normaliza y deduplica categorías de mercancía en UPPERCASE y severidad VUCE', () => {
    const merged = mergeNormalizedCategories(
      ['TECNOLOGIA', 'farmacia', '  '],
      ['Tecnologia', 'Farmacia', 'Quimicos']
    )
    expect(merged).toEqual(['FARMACIA', 'QUIMICOS', 'TECNOLOGIA'])
    expect(getPermisoVuceSeverity(true)).toBe('warn')
    expect(getPermisoVuceSeverity(false)).toBe('success')
  })
})
