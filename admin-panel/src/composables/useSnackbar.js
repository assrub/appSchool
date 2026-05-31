import { reactive, readonly } from 'vue'

const state = reactive({
  show: false,
  message: '',
  color: 'success',
  timeout: 4000
})

function show(message, color = 'success', timeout = 4000) {
  state.message = message
  state.color = color
  state.timeout = timeout
  state.show = true
}

function success(message, timeout = 4000) {
  show(message, 'success', timeout)
}

function error(message, timeout = 6000) {
  show(message, 'error', timeout)
}

function warning(message, timeout = 5000) {
  show(message, 'warning', timeout)
}

function info(message, timeout = 4000) {
  show(message, 'info', timeout)
}

export function useSnackbar() {
  return {
    state: readonly(state),
    success,
    error,
    warning,
    info,
    show
  }
}