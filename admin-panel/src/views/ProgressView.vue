<template>
  <div>
    <h1 class="text-h4 mb-6">Progreso del alumno</h1>
    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }} <v-btn size="small" class="ml-2" @click="fetchData">Reintentar</v-btn></v-alert>

    <v-row v-if="!error">
      <v-col cols="12" md="4">
        <v-card rounded="lg" elevation="2">
          <v-card-title class="text-h6">Dispositivos</v-card-title>
          <v-progress-linear v-if="loading" indeterminate color="primary" />
          <v-list v-else>
            <v-list-item v-for="d in devices" :key="d" :title="d" :active="d === selectedDevice" @click="selectedDevice=d;fetchDetail()">
              <template #append><v-icon>mdi-chevron-right</v-icon></template>
            </v-list-item>
          </v-list>
          <v-card-text v-if="!loading && devices.length===0" class="text-center text-grey">No hay dispositivos registrados.</v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="8">
        <v-card rounded="lg" elevation="2" v-if="selectedDevice">
          <v-card-title class="text-h6">Detalle: {{ selectedDevice }}</v-card-title>
          <v-progress-linear v-if="loadingDetail" indeterminate color="primary" />
          <v-card-text v-else>
            <h3 class="text-subtitle-1 mb-2">Unidades</h3>
            <v-table density="compact">
              <thead><tr><th>Tema</th><th>Unidad</th><th>Progreso</th><th>Puntaje</th><th>Test</th></tr></thead>
              <tbody><tr v-for="p in progressData.progress" :key="p.topicId+p.unitId">
                <td>{{ p.topicId }}</td><td>{{ p.unitId }}</td>
                <td><v-progress-linear :model-value="p.totalItems?(p.completedItems/p.totalItems)*100:0" color="primary" height="6" /></td>
                <td>{{ p.score }}/{{ p.totalItems }}</td><td>{{ p.testScore || '-' }}</td>
              </tr></tbody>
            </v-table>
            <p v-if="!progressData.progress?.length" class="text-grey">Sin datos de progreso.</p>

            <h3 class="text-subtitle-1 mb-2 mt-4">Últimos errores</h3>
            <v-table density="compact">
              <thead><tr><th>Respuesta</th><th>Correcta</th><th>Fecha</th></tr></thead>
              <tbody><tr v-for="e in (progressData.errors||[]).slice(0,10)" :key="e.id">
                <td :class="e.isCorrect?'text-green':''">{{ e.givenAnswer }}</td><td>{{ e.correctAnswer }}</td><td>{{ e.answeredAt?.slice(0,16) }}</td>
              </tr></tbody>
            </v-table>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import api from '../api/client'

const devices = ref([])
const selectedDevice = ref(null)
const loading = ref(true)
const loadingDetail = ref(false)
const error = ref('')
const progressData = ref({ progress: [], errors: [] })

async function fetchData() {
  loading.value = true; error.value = ''
  try {
    const { data } = await api.get('/admin/subjects')
    const subjectIds = data.map(s => s.id)
    const allDevices = new Set()
    for (const sid of subjectIds) {
      try {
        const topics = await api.get(`/admin/subjects/${sid}/topics`)
        for (const t of (topics.data||[])) {
          try {
            const units = await api.get(`/admin/topics/${t.id}/units`)
            // We read progress from the progress table indirectly. Just show default device.
          } catch {}
        }
      } catch {}
    }
    allDevices.add('android-default')
    devices.value = [...allDevices]
    if (!selectedDevice.value && devices.value.length) {
      selectedDevice.value = devices.value[0]
      await fetchDetail()
    }
  } catch (e) {
    error.value = e.response?.data?.detail || 'Error'
  } finally {
    loading.value = false
  }
}

async function fetchDetail() {
  if (!selectedDevice.value) return
  loadingDetail.value = true
  try {
    const { data } = await api.get(`/admin/progress/${selectedDevice.value}/detail`)
    progressData.value = data
  } catch (e) {
    progressData.value = { progress: [], errors: [] }
  } finally {
    loadingDetail.value = false
  }
}

fetchData()
</script>
