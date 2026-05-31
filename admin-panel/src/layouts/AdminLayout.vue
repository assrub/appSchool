<template>
  <v-layout>
    <v-navigation-drawer color="primary" rail expand-on-hover permanent>
      <v-list nav>
        <v-list-item prepend-icon="mdi-view-dashboard" title="Dashboard" to="/" exact />
        <v-list-item prepend-icon="mdi-bookshelf" title="Materias" to="/subjects" />
        <v-list-item prepend-icon="mdi-chart-bar" title="Progreso" to="/progress" />
        <v-list-item prepend-icon="mdi-script-text" title="Scripts" to="/script" />
        <v-list-item prepend-icon="mdi-logout" title="Salir" @click="logout" />
      </v-list>
    </v-navigation-drawer>

    <v-app-bar color="primary" elevation="1">
      <v-app-bar-title class="text-white">AppSchool Admin</v-app-bar-title>
      <v-spacer />
      <v-chip class="mr-4" color="white" variant="tonal">
        <v-icon start>mdi-account</v-icon>
        {{ username }}
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
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const username = computed(() => localStorage.getItem('username') || '')

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  router.push('/login')
}
</script>
