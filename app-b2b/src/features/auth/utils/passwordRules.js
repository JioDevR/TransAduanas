export const PASSWORD_MIN_LENGTH = 6

/**
 * Evalua las reglas del cambio obligatorio de contraseña sin alterar los espacios ingresados.
 */
export function evaluatePasswordRules(form = {}) {
  const passwordActual = typeof form?.passwordActual === 'string' ? form.passwordActual : ''
  const nuevaPassword = typeof form?.nuevaPassword === 'string' ? form.nuevaPassword : ''
  const confirmarPassword = typeof form?.confirmarPassword === 'string' ? form.confirmarPassword : ''

  const hasCurrentPassword = passwordActual.trim().length > 0
  const isLengthValid =
    nuevaPassword.trim().length > 0 && nuevaPassword.length >= PASSWORD_MIN_LENGTH
  const isMatchValid = nuevaPassword.length > 0 && nuevaPassword === confirmarPassword
  const isDifferentFromCurrent =
    hasCurrentPassword && nuevaPassword.trim().length > 0 && passwordActual !== nuevaPassword

  return {
    hasCurrentPassword,
    isLengthValid,
    isMatchValid,
    isDifferentFromCurrent
  }
}

/**
 * Retorna true solo cuando todas las reglas de cambio de contraseña obligatorio se cumplen.
 */
export function isPasswordChangeValid(form = {}) {
  const rules = evaluatePasswordRules(form)
  return (
    rules.hasCurrentPassword &&
    rules.isLengthValid &&
    rules.isMatchValid &&
    rules.isDifferentFromCurrent
  )
}
