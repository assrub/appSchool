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
              <v-icon color="info" size="40">mdi-account-check</v-icon>
            </template>
            <v-card-title class="text-h5">{{ loading ? '...' : metrics.activeToday }}</v-card-title>
            <v-card-subtitle>Activos hoy</v-card-subtitle>
          </v-card-item>
        </v-card>
      </v-col>
    </v-row>

    <v-row v-if="!error">
      <v-col cols="12" sm="4">
        <v-card rounded="lg" elevation="2">
          <v-card-item>
            <template #prepend>
              <v-icon color="success" size="36">mdi-check-circle</v-icon>
            </template>
            <v-card-title class="text-h5">{{ loading ? '...' : metrics.completedUnits }}</v-card-title>
            <v-card-subtitle>Unidades completadas</v-card-subtitle>
          </v-card-item>
        </v-card>
      </v-col>

      <v-col cols="12" sm="4">
        <v-card rounded="lg" elevation="2">
          <v-card-item>
            <template #prepend>
              <v-icon color="warning" size="36">mdi-chart-line</v-icon>
            </template>
            <v-card-title class="text-h5">{{ loading ? '...' : `${metrics.avgCompletion}%` }}</v-card-title>
            <v-card-subtitle>Completitud promedio</v-card-subtitle>
          </v-card-item>
        </v-card>
      </v-col>

      <v-col cols="12" sm="4">
        <v-card rounded="lg" elevation="2">
          <v-card-item>
            <template #prepend>
              <v-icon color="primary" size="36">mdi-format-list-checks</v-icon>
            </template>
            <v-card-title class="text-h5">{{ loading ? '...' : metrics.totalProgress }}</v-card-title>
            <v-card-subtitle>Registros de progreso</v-card-subtitle>
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
        <MobilePreview :html="dashboardPreviewHtml" />
      </v-col>
    </v-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'
import MobilePreview from '../components/MobilePreview.vue'

const route = useRoute()
const subjects = ref([])
const users = ref([])
const loading = ref(true)
const error = ref('')
const metrics = ref({ totalUsers: 0, totalProgress: 0, completedUnits: 0, activeToday: 0, avgCompletion: 0 })

const totalTopics = computed(() => subjects.value.reduce((acc, s) => acc + (s.topicsCount || 0), 0))
const totalUsers = computed(() => users.value.length)
const activeUnits = computed(() => subjects.value.reduce((acc, s) => {
  return acc + (s.topicsCount || 0) * 3
}, 0))

const dashboardPreviewHtml = computed(() => {
  const header = '<div style="background:#4CAF50;color:white;padding:12px 16px;font-weight:bold;font-size:14px;flex-shrink:0;display:flex;align-items:center"><div style="flex:1">AppEnglish</div></div>'
  const bottomNav = '<div style="display:flex;justify-content:space-around;background:white;border-top:1px solid #e0e0e0;padding:8px 0 6px 0;flex-shrink:0"><div style="text-align:center;flex:1"><div style="font-size:18px">🏠</div><div style="font-size:10px;color:#4CAF50;margin-top:2px">Inicio</div></div><div style="text-align:center;flex:1"><div style="font-size:18px">📖</div><div style="font-size:10px;color:#757575;margin-top:2px">Diccionario</div></div><div style="text-align:center;flex:1"><div style="font-size:18px">📊</div><div style="font-size:10px;color:#757575;margin-top:2px">Progreso</div></div><div style="text-align:center;flex:1"><div style="font-size:18px">⚙️</div><div style="font-size:10px;color:#757575;margin-top:2px">Ajustes</div></div></div>'

  if (!subjects.value.length) {
    return `<div style="display:flex;flex-direction:column;height:100%">${header}<div style="flex:1;display:flex;align-items:center;justify-content:center;background:#f5f5f5;color:#757575;font-size:14px;padding:20px">No hay materias</div>${bottomNav}</div>`
  }

  let cards = '<div style="padding:16px 16px 8px 16px"><div style="font-size:22px;font-weight:bold;color:#4CAF50;line-height:1.2">MATERIAS</div></div>'
  cards += subjects.value.map(s => `
    <div style="margin:0 12px 16px 12px;background:white;border-radius:16px;box-shadow:0 1px 2px rgba(0,0,0,0.3), 0 1px 3px 1px rgba(0,0,0,0.15)">
      <div style="display:flex;align-items:center;padding:20px">
        <div style="font-size:28px;margin-right:16px;flex-shrink:0">${s.icon||''}</div>
        <div style="flex:1;min-width:0">
          <div style="font-size:22px;font-weight:bold;color:#212121;line-height:1.2">${s.name}</div>
          <div style="font-size:14px;color:#757575;margin-top:4px">${s.topics_count||s.topicsCount||0} temas</div>
        </div>
        <div style="font-size:24px;color:#4CAF50;flex-shrink:0">→</div>
      </div>
    </div>
  `).join('')

  return `<div style="display:flex;flex-direction:column;height:100%">${header}<div style="flex:1;overflow-y:auto;background:#f5f5f5">${cards}</div>${bottomNav}</div>`
})

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    let subjData = []
    try {
      const subjRes = await api.get('/admin/subjects')
      subjData = subjRes.data || []
    } catch {
      const pubRes = await api.get('/api/v1/content/subjects')
      subjData = (pubRes.data?.subjects || []).map(s => ({ ...s, topicsCount: s.topicsCount || s.topics?.length || 0 }))
    }
    subjects.value = subjData

    const [usersRes, metricsRes] = await Promise.all([
      api.get('/admin/users').catch(() => ({ data: [] })),
      api.get('/admin/progress/dashboard-metrics').catch(() => ({ data: { totalUsers: 0, totalProgress: 0, completedUnits: 0, activeToday: 0, avgCompletion: 0 } }))
    ])
    users.value = usersRes.data || []
    metrics.value = metricsRes.data || { totalUsers: 0, totalProgress: 0, completedUnits: 0, activeToday: 0, avgCompletion: 0 }
  } catch (e) {
    if (subjects.value.length === 0) {
      error.value = e.response?.data?.detail || 'Error al cargar'
    }
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>