<template>
  <v-layout>
    <v-navigation-drawer color="primary" rail expand-on-hover permanent>
      <v-list nav>
        <v-list-item prepend-icon="mdi-view-dashboard" title="Dashboard" to="/" exact />
        <v-list-item prepend-icon="mdi-bookshelf" title="Materias" to="/subjects" />
        <v-list-item prepend-icon="mdi-logout" title="Salir" @click="logout" />
      </v-list>
    </v-navigation-drawer>

    <v-app-bar color="primary" elevation="1">
      <v-app-bar-title class="text-white">AppSchool Admin</v-app-bar-title>
      <v-spacer />
      <v-chip class="mr-4" color="white" variant="tonal">
        <v-icon start>mdi-account</v-icon>
        {{ auth.username }}
      </v-chip>
    </v-app-bar>

    <v-main style="min-height: 100vh; background: #f5f5f5">
      <v-container fluid class="pa-6">
        <router-view :key="$route.fullPath" />
      </v-container>
    </v-main>
  </v-layout>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import api from '../api/client'

const auth = useAuthStore()
const router = useRouter()

import { onMounted } from 'vue'
onMounted(async () => {
  if (!auth.isAuthenticated) { router.push('/login'); return }
  try { await api.post('/auth/refresh') } catch { auth.logout(); router.push('/login') }
})

function logout() {
  auth.logout()
  router.push('/login')
}
</script>
