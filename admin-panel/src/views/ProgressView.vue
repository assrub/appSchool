<template>
  <div>
    <div class="d-flex align-center mb-6">
      <h1 class="text-h4">Progreso de Estudiantes</h1>
      <v-spacer />
      <v-chip :color="wsConnected ? 'success' : 'grey'" variant="tonal" size="small" class="mr-3">
        <v-icon start size="14">{{ wsConnected ? 'mdi-wifi' : 'mdi-wifi-off' }}</v-icon>
        {{ wsConnected ? 'En vivo' : 'Desconectado' }}
      </v-chip>
      <v-btn variant="tonal" prepend-icon="mdi-refresh" @click="fetchUsers" :loading="loadingUsers">
        Actualizar
      </v-btn>
    </div>

    <v-row>
      <v-col cols="12" md="4">
        <v-card rounded="lg" elevation="2">
          <v-card-title class="d-flex align-center">
            <v-icon class="mr-2">mdi-account-group</v-icon>
            Estudiantes
            <v-spacer />
            <v-chip size="small" variant="tonal">{{ filteredUsers.length }}</v-chip>
          </v-card-title>

          <div class="px-4 pb-2">
            <v-text-field
              v-model="searchQuery"
              prepend-inner-icon="mdi-magnify"
              label="Buscar estudiante"
              variant="outlined"
              density="compact"
              hide-details
              clearable
            />
          </div>

          <v-progress-linear v-if="loadingUsers" indeterminate color="primary" />

          <v-list v-else-if="filteredUsers.length" class="py-0">
            <v-list-item
              v-for="u in filteredUsers"
              :key="u.userId"
              :active="selectedUser?.userId === u.userId"
              @click="selectUser(u)"
              class="py-3"
            >
              <template #prepend>
                <v-avatar :color="u.percentComplete >= 100 ? 'success' : 'primary'" size="40" class="mr-3">
                  <span class="text-white text-caption">{{ u.displayName?.charAt(0) || '?' }}</span>
                </v-avatar>
              </template>

              <v-list-item-title class="font-weight-medium">
                {{ u.displayName || u.username }}
              </v-list-item-title>
              <v-list-item-subtitle>
                {{ u.username }}
              </v-list-item-subtitle>

              <template #append>
                <div class="text-right">
                  <div class="text-h6 font-weight-bold" :class="u.percentComplete >= 100 ? 'text-success' : 'text-primary'">
                    {{ u.percentComplete }}%
                  </div>
                  <div class="text-caption text-grey">
                    {{ u.completedUnits }}/{{ u.totalUnits }} unidades
                  </div>
                </div>
              </template>
            </v-list-item>
          </v-list>

          <v-card-text v-else class="text-center text-grey pa-8">
            <v-icon size="48" class="mb-2">mdi-account-off</v-icon>
            <p v-if="searchQuery">No se encontraron estudiantes</p>
            <p v-else>No hay usuarios registrados</p>
            <p class="text-caption mt-2">Creá usuarios en la sección "Usuarios"</p>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="8">
        <template v-if="selectedUser">
          <v-card rounded="lg" elevation="2" class="mb-4">
            <v-card-title class="d-flex align-center">
              <v-avatar :color="selectedUser.percentComplete >= 100 ? 'success' : 'primary'" size="40" class="mr-3">
                <span class="text-white">{{ selectedUser.displayName?.charAt(0) || '?' }}</span>
              </v-avatar>
              <div>
                <div class="text-h6">{{ selectedUser.displayName }}</div>
                <div class="text-caption text-grey">{{ selectedUser.username }}</div>
              </div>
              <v-spacer />
              <v-menu>
                <template #activator="{ props: menuProps }">
                  <v-btn icon="mdi-dots-vertical" variant="text" v-bind="menuProps" />
                </template>
                <v-list>
                  <v-list-item prepend-icon="mdi-restart" @click="confirmResetAll">
                    <v-list-item-title>Resetear todo el progreso</v-list-item-title>
                  </v-list-item>
                  <v-list-item prepend-icon="mdi-export" @click="exportProgress">
                    <v-list-item-title>Exportar progreso</v-list-item-title>
                  </v-list-item>
                </v-list>
              </v-menu>
            </v-card-title>

            <v-card-text>
              <v-row>
                <v-col cols="4">
                  <div class="text-center">
                    <div class="text-h4 font-weight-bold text-primary">{{ selectedUser.percentComplete }}%</div>
                    <div class="text-caption text-grey">Progreso total</div>
                    <v-progress-linear
                      :model-value="selectedUser.percentComplete"
                      color="primary"
                      height="8"
                      class="mt-2 rounded"
                    />
                  </div>
                </v-col>
                <v-col cols="4">
                  <div class="text-center">
                    <div class="text-h4 font-weight-bold text-success">{{ selectedUser.completedUnits }}</div>
                    <div class="text-caption text-grey">Unidades completadas</div>
                  </div>
                </v-col>
                <v-col cols="4">
                  <div class="text-center">
                    <div class="text-h4 font-weight-bold">{{ selectedUser.totalUnits }}</div>
                    <div class="text-caption text-grey">Total unidades</div>
                  </div>
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>

          <v-card v-if="loadingDetail" rounded="lg" elevation="2">
            <v-progress-linear indeterminate color="primary" />
            <v-card-text class="text-center py-8">Cargando detalles...</v-card-text>
          </v-card>

          <template v-else>
            <v-card rounded="lg" elevation="2" class="mb-4">
              <v-card-title class="d-flex align-center">
                <v-icon class="mr-2">mdi-format-list-bulleted</v-icon>
                Detalle por unidad
              </v-card-title>

              <v-table v-if="progressData.progress?.length">
                <thead>
                  <tr>
                    <th>Tema</th>
                    <th>Unidad</th>
                    <th>Progreso</th>
                    <th>Puntaje</th>
                    <th>Test</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="p in progressData.progress" :key="p.topicId + p.unitId">
                    <td class="font-weight-medium">{{ p.topicId }}</td>
                    <td>{{ p.unitId }}</td>
                    <td style="min-width: 120px">
                      <v-progress-linear
                        :model-value="p.totalItems ? (p.completedItems / p.totalItems) * 100 : 0"
                        :color="p.completed ? 'success' : 'primary'"
                        height="8"
                        class="rounded"
                      />
                      <div class="text-caption text-grey mt-1">{{ p.completedItems }}/{{ p.totalItems }} items</div>
                    </td>
                    <td>
                      <v-chip
                        :color="p.score >= (p.totalItems * 0.7) ? 'success' : 'warning'"
                        size="small"
                        variant="tonal"
                      >
                        {{ p.score }}/{{ p.totalItems }}
                      </v-chip>
                    </td>
                    <td>
                      <v-chip
                        v-if="p.testScore != null"
                        :color="p.testScore >= 14 ? 'success' : 'warning'"
                        size="small"
                        variant="tonal"
                      >
                        {{ p.testScore }}/20
                      </v-chip>
                      <span v-else class="text-grey">-</span>
                    </td>
                    <td>
                      <v-btn
                        icon="mdi-restart"
                        variant="text"
                        size="small"
                        color="orange"
                        @click="confirmRedoUnit(p)"
                      />
                    </td>
                  </tr>
                </tbody>
              </v-table>
              <v-card-text v-else class="text-center text-grey py-6">
                <v-icon size="48" class="mb-2">mdi-clipboard-text-off</v-icon>
                <p>Sin datos de progreso registrados</p>
              </v-card-text>
            </v-card>

            <v-card v-if="progressData.errors?.length" rounded="lg" elevation="2" class="mb-4">
              <v-card-title class="d-flex align-center">
                <v-icon class="mr-2" color="error">mdi-alert-circle</v-icon>
                Últimos errores ({{ progressData.errors.length }})
              </v-card-title>

              <v-table density="compact">
                <thead>
                  <tr>
                    <th>Respuesta dada</th>
                    <th>Respuesta correcta</th>
                    <th>Fecha</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="e in progressData.errors.slice(0, 15)" :key="e.id">
                    <td :class="e.isCorrect ? 'text-success' : 'text-error'">
                      {{ e.givenAnswer }}
                    </td>
                    <td>{{ e.correctAnswer }}</td>
                    <td class="text-caption text-grey">{{ formatDate(e.answeredAt) }}</td>
                  </tr>
                </tbody>
              </v-table>
            </v-card>

            <v-card v-if="analytics" rounded="lg" elevation="2">
              <v-card-title class="d-flex align-center">
                <v-icon class="mr-2" color="primary">mdi-chart-bar</v-icon>
                Análisis
              </v-card-title>

              <v-card-text>
                <v-row>
                  <v-col cols="6">
                    <v-sheet rounded="lg" color="grey-lighten-4" class="pa-4 text-center">
                      <div class="text-h4 font-weight-bold text-primary">{{ analytics.overallAccuracy || 0 }}%</div>
                      <div class="text-caption">Precisión global</div>
                      <div class="text-caption text-grey mt-1">
                        {{ analytics.totalCorrect || 0 }} correctas de {{ analytics.totalAnswered || 0 }}
                      </div>
                    </v-sheet>
                  </v-col>
                  <v-col cols="6">
                    <v-sheet rounded="lg" color="grey-lighten-4" class="pa-4 text-center">
                      <div class="text-h4 font-weight-bold text-warning">{{ analytics.weakUnits?.length || 0 }}</div>
                      <div class="text-caption">Unidades problemáticas</div>
                      <div class="text-caption text-grey mt-1">Requieren práctica adicional</div>
                    </v-sheet>
                  </v-col>
                </v-row>

                <div v-if="analytics.weakUnits?.length" class="mt-4">
                  <h4 class="text-subtitle-1 font-weight-medium mb-2">Unidades que necesitan refuerzo:</h4>
                  <div v-for="w in analytics.weakUnits" :key="w.unitId" class="d-flex align-center py-2 px-3 mb-2 rounded" style="background:#FFF3E0">
                    <v-icon color="warning" size="20" class="mr-2">mdi-alert-circle</v-icon>
                    <div class="flex-grow-1">
                      <div class="text-body-2 font-weight-medium">{{ w.unitId }}</div>
                      <div class="text-caption text-grey">{{ w.errorRate }}% errores ({{ w.totalErrors }} fallos)</div>
                    </div>
                    <v-btn size="small" variant="tonal" color="orange" @click="confirmRedoUnitById(w.unitId)">
                      <v-icon start size="16">mdi-restart</v-icon>
                      Rehacer
                    </v-btn>
                  </div>
                </div>

                <div v-if="analytics.commonMistakes?.length" class="mt-4">
                  <h4 class="text-subtitle-1 font-weight-medium mb-2">Errores comunes:</h4>
                  <div v-for="m in analytics.commonMistakes.slice(0, 5)" :key="m.givenAnswer" class="py-1">
                    <v-chip size="small" color="error" variant="tonal" class="mr-2">
                      "{{ m.givenAnswer }}"
                    </v-chip>
                    <span class="text-caption">debería ser</span>
                    <v-chip size="small" color="success" variant="tonal" class="ml-2">
                      "{{ m.correctAnswer }}"
                    </v-chip>
                    <span class="text-caption text-grey ml-2">({{ m.count }} veces)</span>
                  </div>
                </div>
              </v-card-text>
            </v-card>
          </template>
        </template>

        <v-card v-else rounded="lg" elevation="2">
          <v-card-text class="text-center text-grey py-12">
            <v-icon size="64" class="mb-4">mdi-account-search</v-icon>
            <p class="text-h6">Seleccioná un estudiante</p>
            <p class="text-body-2">Hacé click en un estudiante de la lista para ver su progreso</p>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-dialog v-model="resetDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title class="text-error">
          <v-icon class="mr-2">mdi-alert</v-icon>
          Resetear progreso
        </v-card-title>
        <v-card-text>
          ¿Resetear TODO el progreso de <b>{{ selectedUser?.displayName }}</b>?<br />
          Esta acción no se puede deshacer.
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="resetDialog = false">Cancelar</v-btn>
          <v-btn color="error" :loading="resetting" @click="doResetAll">Resetear</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="redoDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title class="text-warning">
          <v-icon class="mr-2">mdi-restart</v-icon>
          Marcar para rehacer
        </v-card-title>
        <v-card-text>
          ¿Marcar "<b>{{ unitToRedo }}</b>" para que el estudiante la rehega?<br />
          La unidad aparecerá como no completada.
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="redoDialog = false">Cancelar</v-btn>
          <v-btn color="warning" :loading="redoing" @click="doRedoUnit">Marcar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, inject, watch } from 'vue'
import api from '../api/client'
import { useWebSocket } from '../composables/useWebSocket'

const snackbar = inject('snackbar')

const wsUrl = `ws://${window.location.hostname}:8000/ws/progress`
const { connected: wsConnected, lastMessage: wsMessage } = useWebSocket(wsUrl)

watch(wsMessage, (msg) => {
  if (!msg) return
  if (msg.type === 'progress_synced' || msg.type === 'answers_recorded') {
    fetchUsers()
    if (selectedUser.value) selectUser(selectedUser.value)
  }
})

const users = ref([])
const selectedUser = ref(null)
const loadingUsers = ref(true)
const loadingDetail = ref(false)
const searchQuery = ref('')
const progressData = ref({ progress: [], errors: [], sessions: [] })
const analytics = ref(null)

const resetDialog = ref(false)
const resetting = ref(false)

const redoDialog = ref(false)
const unitToRedo = ref('')
const redoing = ref(false)

const filteredUsers = computed(() => {
  if (!searchQuery.value) return users.value
  const q = searchQuery.value.toLowerCase()
  return users.value.filter(u =>
    (u.displayName || '').toLowerCase().includes(q) ||
    (u.username || '').toLowerCase().includes(q)
  )
})

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('es-AR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function fetchUsers() {
  loadingUsers.value = true
  try {
    const { data } = await api.get('/admin/users/progress-summary')
    users.value = data || []
    if (data?.length && !selectedUser.value) {
      selectUser(data[0])
    }
  } catch (e) {
    snackbar.error('Error al cargar usuarios')
  } finally {
    loadingUsers.value = false
  }
}

async function selectUser(user) {
  selectedUser.value = user
  loadingDetail.value = true
  analytics.value = null

  try {
    const { data } = await api.get(`/admin/progress/${user.userId}/detail`)
    progressData.value = data
  } catch {
    progressData.value = { progress: [], errors: [], sessions: [] }
  } finally {
    loadingDetail.value = false
  }

  try {
    const { data } = await api.get(`/admin/progress/${user.userId}/analytics`)
    analytics.value = data
  } catch {
    analytics.value = null
  }
}

function confirmRedoUnit(p) {
  unitToRedo.value = p.unitId
  redoDialog.value = true
}

function confirmRedoUnitById(unitId) {
  unitToRedo.value = unitId
  redoDialog.value = true
}

async function doRedoUnit() {
  redoing.value = true
  try {
    await api.post(`/admin/progress/${selectedUser.value.userId}/${unitToRedo.value}/redo`)
    snackbar.success('Unidad marcada para rehacer')
    redoDialog.value = false
    await selectUser(selectedUser.value)
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al marcar unidad')
  } finally {
    redoing.value = false
  }
}

function confirmResetAll() {
  resetDialog.value = true
}

async function doResetAll() {
  resetting.value = true
  try {
    for (const p of progressData.value.progress) {
      try {
        await api.delete(`/admin/progress/${selectedUser.value.userId}/${p.topicId}/${p.unitId}`)
      } catch {}
    }
    snackbar.success('Progreso reseteado')
    resetDialog.value = false
    await selectUser(selectedUser.value)
  } catch (e) {
    snackbar.error('Error al resetear')
  } finally {
    resetting.value = false
  }
}

function exportProgress() {
  if (!selectedUser.value) return
  const data = {
    user: selectedUser.value,
    progress: progressData.value.progress,
    errors: progressData.value.errors,
    analytics: analytics.value
  }
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `progress_${selectedUser.value.username}_${Date.now()}.json`
  a.click()
  URL.revokeObjectURL(url)
  snackbar.success('Progreso exportado')
}

onMounted(fetchUsers)
</script>