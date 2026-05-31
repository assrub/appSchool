<template>
  <div>
    <h1 class="text-h4 mb-6">Dashboard</h1>
    <v-row><v-col cols="7">

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
      {{ error }}
      <v-btn size="small" class="ml-2" @click="fetchData">Reintentar</v-btn>
    </v-alert>

    <v-row v-if="!error">
      <v-col cols="12" sm="6" md="3">
        <v-card rounded="lg" elevation="2">
          <v-card-item>
            <template #prepend>
              <v-progress-circular v-if="loading" indeterminate size="40" color="primary" />
              <v-icon v-else color="primary" size="40">mdi-bookshelf</v-icon>
            </template>
            <v-card-title class="text-h5">{{ loading ? '...' : subjects.length }}</v-card-title>
            <v-card-subtitle>Materias</v-card-subtitle>
          </v-card-item>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="3">
        <v-card rounded="lg" elevation="2">
          <v-card-item>
            <template #prepend>
              <v-icon color="secondary" size="40">mdi-book-open-page-variant</v-icon>
            </template>
            <v-card-title class="text-h5">{{ totalTopics }}</v-card-title>
            <v-card-subtitle>Temas</v-card-subtitle>
          </v-card-item>
        </v-card>
      </v-col>
    </v-row>

    <v-card v-if="!error" class="mt-6" rounded="lg" elevation="2">
      <v-card-title class="text-h6">Materias activas</v-card-title>
      <v-progress-linear v-if="loading" indeterminate color="primary" />
      <v-list v-else>
        <v-list-item v-for="s in subjects" :key="s.id" :title="`${s.icon} ${s.name}`" :subtitle="`ID: ${s.id}`" :to="`/subjects/${s.id}/topics`">
          <template #append><v-icon>mdi-chevron-right</v-icon></template>
        </v-list-item>
      </v-list>
      <v-card-text v-if="!loading && subjects.length === 0" class="text-center text-grey">
        No hay materias todavía. Creá una en "Materias".
      </v-card-text>
    </v-card>
      </v-col>
      <v-col cols="5" class="d-flex align-start"><AppPreview /></v-col>
    </v-row>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'
import AppPreview from '../components/AppPreview.vue'

const route = useRoute()
const subjects = ref([])
const loading = ref(true)
const error = ref('')
const totalTopics = computed(() => subjects.value.reduce((acc, s) => acc + (s.topicsCount || 0), 0))

async function fetchData() {
  loading.value = true; error.value = ''
  try {
    const subjRes = await api.get('/admin/subjects')
    subjects.value = subjRes.data || []
  } catch (e) {
    error.value = e.response?.data?.detail || 'Error al cargar'
  } finally {
    loading.value = false
  }
}

watch(() => route.params, fetchData, { immediate: true })
</script>
