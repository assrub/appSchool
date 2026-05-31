import { reactive, computed, ref } from 'vue'

export function useValidate(initialErrors = {}) {
  const errors = reactive({ ...initialErrors })

  function validate(fieldName, value, rules = {}) {
    const messages = []

    if (rules.required && (!value || (typeof value === 'string' && value.trim() === ''))) {
      messages.push('Este campo es requerido')
    }

    if (rules.minLength && value && value.length < rules.minLength) {
      messages.push(`Mínimo ${rules.minLength} caracteres`)
    }

    if (rules.maxLength && value && value.length > rules.maxLength) {
      messages.push(`Máximo ${rules.maxLength} caracteres`)
    }

    if (rules.pattern && value && !rules.pattern.test(value)) {
      messages.push(rules.patternMessage || 'Formato inválido')
    }

    if (rules.color && value && !/^#[0-9A-Fa-f]{6}$/.test(value)) {
      messages.push('Color debe ser formato HEX válido (#4CAF50)')
    }

    if (rules.email && value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
      messages.push('Email inválido')
    }

    if (rules.numeric && value && isNaN(Number(value))) {
      messages.push('Debe ser un número')
    }

    if (rules.min !== undefined && value !== undefined && Number(value) < rules.min) {
      messages.push(`Valor mínimo: ${rules.min}`)
    }

    if (rules.max !== undefined && value !== undefined && Number(value) > rules.max) {
      messages.push(`Valor máximo: ${rules.max}`)
    }

    if (messages.length > 0) {
      errors[fieldName] = messages
    } else {
      delete errors[fieldName]
    }

    return messages.length === 0
  }

  function validateAll(formData, validationRules) {
    let isValid = true
    for (const [fieldName, rules] of Object.entries(validationRules)) {
      const fieldValue = formData[fieldName]
      if (!validate(fieldName, fieldValue, rules)) {
        isValid = false
      }
    }
    return isValid
  }

  function clearError(fieldName) {
    delete errors[fieldName]
  }

  function clearAllErrors() {
    Object.keys(errors).forEach(key => delete errors[key])
  }

  function hasErrors() {
    return Object.keys(errors).length > 0
  }

  function getError(fieldName) {
    return computed(() => errors[fieldName]?.[0] || '')
  }

  function getAllErrors(fieldName) {
    return computed(() => errors[fieldName] || [])
  }

  return {
    errors,
    validate,
    validateAll,
    clearError,
    clearAllErrors,
    hasErrors,
    getError,
    getAllErrors
  }
}

export function useLoading() {
  const isLoading = ref(false)
  const error = ref(null)

  async function wrap(promise, options = {}) {
    const { onSuccess, onError } = options
    isLoading.value = true
    error.value = null

    try {
      const result = await promise
      if (onSuccess) onSuccess(result)
      return result
    } catch (err) {
      error.value = err.response?.data?.detail || err.message || 'Ocurrió un error'
      if (onError) onError(err)
      throw err
    } finally {
      isLoading.value = false
    }
  }

  return { isLoading, error, wrap }
}

export function useConfirm() {
  const isDialogOpen = ref(false)
  const confirmData = ref(null)

  function openConfirm(options) {
    return new Promise((resolve) => {
      confirmData.value = {
        title: options.title || '¿Confirmar?',
        message: options.message || '',
        confirmText: options.confirmText || 'Confirmar',
        cancelText: options.cancelText || 'Cancelar',
        color: options.color || 'primary',
        onConfirm: () => {
          isDialogOpen.value = false
          resolve(true)
        },
        onCancel: () => {
          isDialogOpen.value = false
          resolve(false)
        }
      }
      isDialogOpen.value = true
    })
  }

  return { isDialogOpen, confirmData, openConfirm }
}

export default { useValidate, useLoading, useConfirm }