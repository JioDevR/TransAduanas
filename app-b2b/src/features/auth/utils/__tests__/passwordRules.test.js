import { describe, it, expect } from 'vitest'
import {
  PASSWORD_MIN_LENGTH,
  evaluatePasswordRules,
  isPasswordChangeValid
} from '../passwordRules'

describe('passwordRules', () => {
  it('define longitud mínima en 6 caracteres', () => {
    expect(PASSWORD_MIN_LENGTH).toBe(6)
  })

  it('marca todas las reglas en falso cuando el formulario está vacío', () => {
    const rules = evaluatePasswordRules({
      passwordActual: '',
      nuevaPassword: '',
      confirmarPassword: ''
    })
    expect(rules).toEqual({
      hasCurrentPassword: false,
      isLengthValid: false,
      isMatchValid: false,
      isDifferentFromCurrent: false
    })
    expect(isPasswordChangeValid({ passwordActual: '', nuevaPassword: '', confirmarPassword: '' })).toBe(false)
  })

  it('no marca isDifferentFromCurrent como válida si passwordActual está vacío', () => {
    const rules = evaluatePasswordRules({
      passwordActual: '',
      nuevaPassword: 'NuevaClave2026',
      confirmarPassword: 'NuevaClave2026'
    })
    expect(rules.isLengthValid).toBe(true)
    expect(rules.isMatchValid).toBe(true)
    expect(rules.hasCurrentPassword).toBe(false)
    expect(rules.isDifferentFromCurrent).toBe(false)
    expect(isPasswordChangeValid({
      passwordActual: '',
      nuevaPassword: 'NuevaClave2026',
      confirmarPassword: 'NuevaClave2026'
    })).toBe(false)
  })

  it('respeta espacios significativos sin recortar con trim', () => {
    const withSpaces = {
      passwordActual: 'temp123',
      nuevaPassword: ' clave 1 ',
      confirmarPassword: ' clave 1 '
    }
    expect(isPasswordChangeValid(withSpaces)).toBe(true)

    const mismatchTrailingSpace = {
      passwordActual: 'temp123',
      nuevaPassword: 'clave123',
      confirmarPassword: 'clave123 '
    }
    expect(evaluatePasswordRules(mismatchTrailingSpace).isMatchValid).toBe(false)
    expect(isPasswordChangeValid(mismatchTrailingSpace)).toBe(false)
  })

  it('rechaza nueva contraseña igual a la temporal o menor a 6 caracteres', () => {
    expect(evaluatePasswordRules({
      passwordActual: 'temp12',
      nuevaPassword: 'temp12',
      confirmarPassword: 'temp12'
    }).isDifferentFromCurrent).toBe(false)

    expect(evaluatePasswordRules({
      passwordActual: 'temp123',
      nuevaPassword: '12345',
      confirmarPassword: '12345'
    }).isLengthValid).toBe(false)
  })

  it('rechaza contraseñas compuestas únicamente por espacios en blanco', () => {
    const onlySpaces = {
      passwordActual: '   ',
      nuevaPassword: '      ',
      confirmarPassword: '      '
    }
    const rules = evaluatePasswordRules(onlySpaces)
    expect(rules.hasCurrentPassword).toBe(false)
    expect(rules.isLengthValid).toBe(false)
    expect(rules.isDifferentFromCurrent).toBe(false)
    expect(isPasswordChangeValid(onlySpaces)).toBe(false)
  })
})
