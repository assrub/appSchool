<template>
  <div>
    <h1 class="text-h4 mb-6">Progreso</h1>

    <v-row>
      <v-col cols="12" md="4">
        <v-card rounded="lg" elevation="2">
          <v-card-title class="text-h6">Usuarios</v-card-title>
          <v-progress-linear v-if="loadingUsers" indeterminate color="primary" />
          <v-list v-else>
            <v-list-item v-for="u in users" :key="u.userId" :title="u.displayName" :subtitle="`${u.username} · ${u.percentComplete}% (${u.completedUnits}/${u.totalUnits})`" :active="selectedUser?.userId === u.userId" @click="selectUser(u)">
              <template #prepend><v-icon :color="u.percentComplete>=100?'success':'primary'">{{ u.percentComplete>=100?'mdi-check-circle':'mdi-account' }}</v-icon></template>
              <template #append><v-icon>mdi-chevron-right</v-icon></template>
            </v-list-item>
          </v-list>
          <v-card-text v-if="!loadingUsers && users.length===0" class="text-center text-grey">
        <p v-if="errorUsers" class="text-error mb-2">Error: {{ errorUsers }}</p>
        <p>No hay usuarios. Creá uno en "Usuarios".</p>
        <p class="text-caption mt-2">Si ya creaste usuarios, asegurate de haber hecho <code>git pull && docker compose up -d --build</code> en el VPS.</p>
      </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="8">
        <v-card v-if="selectedUser" rounded="lg" elevation="2">
          <v-card-title class="d-flex align-center">
            📊 {{ selectedUser.displayName }}
            <v-spacer />
            <v-btn size="small" variant="tonal" color="orange" @click="resetAll">🔄 Resetear todo</v-btn>
          </v-card-title>
          <v-progress-linear v-if="loadingDetail" indeterminate color="primary" />
          <v-card-text v-else>
            <v-table density="compact">
              <thead><tr><th>Tema</th><th>Unidad</th><th>Progreso</th><th>Puntaje</th><th>Test</th></tr></thead>
              <tbody>
                <tr v-for="p in progressData.progress" :key="p.topicId+p.unitId">
                  <td>{{ p.topicId }}</td><td>{{ p.unitId }}</td>
                  <td>
                    <v-progress-linear :model-value="p.totalItems?(p.completedItems/p.totalItems)*100:0" :color="p.completed?'success':'primary'" height="6" />
                    <span class="text-caption">{{ p.completedItems }}/{{ p.totalItems }}</span>
                  </td>
                  <td>{{ p.score }}/{{ p.totalItems }}</td>
                  <td>
                    <v-chip v-if="p.testScore!=null" :color="p.testScore>=14?'success':'warning'" size="x-small">{{ p.testScore }}/20</v-chip>
                    <span v-else class="text-grey">-</span>
                  </td>
                </tr>
              </tbody>
            </v-table>
            <p v-if="!progressData.progress?.length" class="text-grey text-center py-4">Sin datos de progreso.</p>

            <h3 class="text-subtitle-1 mb-2 mt-4">Últimos errores</h3>
            <v-table density="compact" v-if="progressData.errors?.length">
              <thead><tr><th>Respuesta</th><th>Correcta</th><th>Fecha</th></tr></thead>
              <tbody><tr v-for="e in progressData.errors.slice(0,10)" :key="e.id">
                <td :class="e.isCorrect?'text-green':''">{{ e.givenAnswer }}</td><td>{{ e.correctAnswer }}</td><td>{{ e.answeredAt?.slice(0,16) }}</td>
              </tr></tbody>
            </v-table>
            <p v-else class="text-grey text-center py-4">Sin errores registrados.</p>
          </v-card-text>
        </v-card>
        <v-card v-else rounded="lg"><v-card-text class="text-center text-grey py-8">Seleccioná un usuario para ver su progreso.</v-card-text></v-card>
      </v-col>
    </v-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import api from '../api/client'

const users = ref([])
const selectedUser = ref(null)
const loadingUsers = ref(true)
const loadingDetail = ref(false)
const errorUsers = ref('')
const progressData = ref({ progress: [], errors: [], sessions: [] })

async function fetchUsers() {
  loadingUsers.value = true
  errorUsers.value = ''
  try {
    const { data } = await api.get('/admin/users/progress-summary')
    users.value = data || []
    if (data.length && !selectedUser.value) selectUser(data[0])
  } catch (e) {
    users.value = []
    errorUsers.value = e.response?.data?.detail || e.message || 'Error de conexión'
  } finally {
    loadingUsers.value = false
  }
}

async function selectUser(user) {
  selectedUser.value = user
  loadingDetail.value = true
  try {
    const { data } = await api.get(`/admin/progress/${user.userId}/detail`)
    progressData.value = data
  } catch (e) {
    progressData.value = { progress: [], errors: [], sessions: [] }
  } finally {
    loadingDetail.value = false
  }
}

async function resetAll() {
  if (!confirm(`¿Resetear TODO el progreso de ${selectedUser.value.displayName}?`)) return
  // We need to go unit by unit — but for simplicity, reset all progress rows for this user
  for (const p of progressData.value.progress) {
    try {
      await api.delete(`/admin/progress/${selectedUser.value.userId}/${p.topicId}/${p.unitId}`)
    } catch {}
  }
  await selectUser(selectedUser.value)
  alert('Progreso reseteado')
}

fetchUsers()
</script>
