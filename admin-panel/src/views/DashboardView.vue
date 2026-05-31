<template>
  <div>
    <h1 class="text-h4 mb-6">Dashboard</h1>

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4" closable @click:close="error = ''">
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
            <v-card-title class="text-h5">{{ loading ? '...' : totalTopics }}</v-card-title>
            <v-card-subtitle>Temas</v-card-subtitle>
          </v-card-item>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="3">
        <v-card rounded="lg" elevation="2">
          <v-card-item>
            <template #prepend>
              <v-icon color="success" size="40">mdi-account-group</v-icon>
            </template>
            <v-card-title class="text-h5">{{ loading ? '...' : totalUsers }}</v-card-title>
            <v-card-subtitle>Usuarios</v-card-subtitle>
          </v-card-item>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="3">
        <v-card rounded="lg" elevation="2">
          <v-card-item>
            <template #prepend>
              <v-icon color="warning" size="40">mdi-lock-open</v-icon>
            </template>
            <v-card-title class="text-h5">{{ loading ? '...' : activeUnits }}</v-card-title>
            <v-card-subtitle>Unidades activas</v-card-subtitle>
          </v-card-item>
        </v-card>
      </v-col>
    </v-row>

    <v-row class="mt-6" v-if="!error">
      <v-col cols="12" md="7">
        <v-card rounded="lg" elevation="2">
          <v-card-title class="d-flex align-center">
            <v-icon class="mr-2">mdi-bookshelf</v-icon>
            Materias activas
          </v-card-title>

          <v-progress-linear v-if="loading" indeterminate color="primary" />

          <v-list v-else-if="subjects.length">
            <v-list-item
              v-for="s in subjects"
              :key="s.id"
              :title="`${s.icon} ${s.name}`"
              :subtitle="`${s.topicsCount || 0} temas`"
              :to="`/subjects/${s.id}/topics`"
            >
              <template #prepend>
                <v-avatar color="primary" size="40">
                  <span class="text-white">{{ s.icon }}</span>
                </v-avatar>
              </template>
              <template #append>
                <v-chip size="small" variant="tonal" :color="s.is_active ? 'success' : 'grey'">
                  {{ s.is_active ? 'Activa' : 'Inactiva' }}
                </v-chip>
                <v-icon class="ml-2">mdi-chevron-right</v-icon>
              </template>
            </v-list-item>
          </v-list>

          <v-card-text v-else class="text-center text-grey pa-8">
            <v-icon size="48" class="mb-2">mdi-bookshelf</v-icon>
            <p>No hay materias todavía</p>
            <v-btn color="primary" variant="tonal" class="mt-2" to="/subjects">
              <v-icon start>mdi-plus</v-icon>
              Crear materia
            </v-btn>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="5" class="d-flex align-start">
        <AppPreview />
      </v-col>
    </v-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'
import AppPreview from '../components/AppPreview.vue'

const route = useRoute()
const subjects = ref([])
const users = ref([])
const loading = ref(true)
const error = ref('')

const totalTopics = computed(() => subjects.value.reduce((acc, s) => acc + (s.topicsCount || 0), 0))
const totalUsers = computed(() => users.value.length)
const activeUnits = computed(() => subjects.value.reduce((acc, s) => {
  return acc + (s.topicsCount || 0) * 3
}, 0))

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const [subjRes, usersRes] = await Promise.all([
      api.get('/admin/subjects'),
      api.get('/admin/users')
    ])
    subjects.value = subjRes.data || []
    users.value = usersRes.data || []
  } catch (e) {
    error.value = e.response?.data?.detail || 'Error al cargar'
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>