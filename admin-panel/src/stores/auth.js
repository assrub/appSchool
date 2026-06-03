import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const isAuthenticated = computed(() => !!token.value)

  async function login(user, pass) {
    const { default: api } = await import('../api/client')
    const { data } = await api.post('/auth/login', { username: user, password: pass })
    token.value = data.access_token
    username.value = data.username
    localStorage.setItem('token', data.access_token)
    localStorage.setItem('username', data.username)
  }

  function logout() {
    token.value = ''
    username.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('username')
  }

  function clearInvalidToken() {
    token.value = ''
    username.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('username')
  }

  return { token, username, isAuthenticated, login, logout, clearInvalidToken }
})
