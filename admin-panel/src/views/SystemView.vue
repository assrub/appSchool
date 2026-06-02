<template>
  <div>
    <div class="d-flex align-center mb-4">
      <h1 class="text-h4">⚙️ Sistema</h1>
      <v-spacer />
      <v-chip v-if="wsConnected" color="success" variant="tonal" size="small">
        <v-icon start size="14">mdi-wifi</v-icon> En vivo
      </v-chip>
    </div>

    <v-tabs v-model="tab" color="primary">
      <v-tab value="logs">
        <v-icon start>mdi-text-box-search-outline</v-icon> Logs
      </v-tab>
      <v-tab value="db">
        <v-icon start>mdi-database</v-icon> DB Explorer
      </v-tab>
      <v-tab value="sql">
        <v-icon start>mdi-code-tags</v-icon> SQL Console
      </v-tab>
      <v-tab value="scripts">
        <v-icon start>mdi-script-text-outline</v-icon> Scripts
      </v-tab>
      <v-tab value="health">
        <v-icon start>mdi-heart-pulse</v-icon> Health
      </v-tab>
    </v-tabs>

    <v-window v-model="tab" class="mt-4">
      <!-- LOGS TAB -->
      <v-window-item value="logs">
        <v-card rounded="lg" elevation="2">
          <v-card-title class="d-flex align-center">
            <v-icon class="mr-2">mdi-console</v-icon>
            Logs del servidor
            <v-spacer />
            <v-select
              v-model="logContainer"
              :items="['api', 'admin', 'db']"
              variant="outlined"
              density="compact"
              hide-details
              class="mr-2"
              style="max-width: 120px"
            />
            <v-btn variant="tonal" prepend-icon="mdi-refresh" @click="fetchLogs" :loading="loadingLogs" size="small">
              Refrescar
            </v-btn>
          </v-card-title>
          <v-card-text>
            <v-alert v-if="logError" type="error" variant="tonal" closable class="mb-3">
              {{ logError }}
            </v-alert>
            <v-sheet rounded="lg" color="grey-darken-4" class="pa-4">
              <pre class="text-caption text-green-lighten-2" style="max-height: 500px; overflow-y: auto; white-space: pre-wrap; font-family: monospace; font-size: 12px;">
                <template v-if="logs.length">{{ logs.join('\n') }}</template>
                <span v-else class="text-grey">Sin logs. Hacé clic en Refrescar.</span>
              </pre>
            </v-sheet>
            <div class="text-caption text-grey mt-1">{{ logs.length }} líneas</div>
          </v-card-text>
        </v-card>
      </v-window-item>

      <!-- DB EXPLORER TAB -->
      <v-window-item value="db">
        <v-card rounded="lg" elevation="2">
          <v-card-title class="d-flex align-center">
            <v-icon class="mr-2">mdi-table</v-icon>
            Tablas de la base de datos
            <v-spacer />
            <v-btn variant="tonal" prepend-icon="mdi-refresh" @click="fetchTables" :loading="loadingTables" size="small">
              Refrescar
            </v-btn>
          </v-card-title>
          <v-card-text>
            <v-alert v-if="dbError" type="error" variant="tonal" closable class="mb-3">{{ dbError }}</v-alert>
            <v-list v-if="tables.length">
              <v-list-item
                v-for="t in tables"
                :key="t.name"
                @click="selectTable(t.name)"
                :color="selectedTable === t.name ? 'primary' : undefined"
              >
                <template #prepend>
                  <v-icon :color="selectedTable === t.name ? 'primary' : undefined">
                    {{ selectedTable === t.name ? 'mdi-table-arrow-down' : 'mdi-table' }}
                  </v-icon>
                </template>
                <v-list-item-title>{{ t.name }}</v-list-item-title>
                <v-list-item-subtitle>{{ t.rows }} filas</v-list-item-subtitle>
                <template #append>
                  <v-chip size="x-small" variant="tonal">{{ t.rows }} rows</v-chip>
                </template>
              </v-list-item>
            </v-list>
            <div v-else class="text-center text-grey py-4">
              <v-icon size="40" class="mb-2">mdi-database-off</v-icon>
              <p>No se pudieron cargar las tablas</p>
            </div>
          </v-card-text>
        </v-card>

        <v-card v-if="selectedTableColumns.length" rounded="lg" elevation="2" class="mt-4">
          <v-card-title class="d-flex align-center">
            <v-icon class="mr-2">mdi-table-eye</v-icon>
            {{ selectedTable }}
          </v-card-title>
          <v-card-text>
            <h4 class="text-subtitle-2 mb-2">Columnas</h4>
            <v-table density="compact">
              <thead>
                <tr>
                  <th>Columna</th>
                  <th>Tipo</th>
                  <th>Nullable</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="c in selectedTableColumns" :key="c.name">
                  <td><code>{{ c.name }}</code></td>
                  <td><code>{{ c.type }}</code></td>
                  <td>{{ c.nullable === 'YES' ? '✅' : '❌' }}</td>
                </tr>
              </tbody>
            </v-table>

            <h4 class="text-subtitle-2 mt-4 mb-2">Preview (primeros 20 registros)</h4>
            <v-table density="compact" v-if="selectedTablePreview.length">
              <thead>
                <tr>
                  <th v-for="c in selectedTableColumns" :key="c.name">{{ c.name }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, i) in selectedTablePreview" :key="i">
                  <td v-for="(val, j) in row" :key="j"><code>{{ val }}</code></td>
                </tr>
              </tbody>
            </v-table>
            <div v-else class="text-grey text-center py-2">Sin datos</div>
          </v-card-text>
        </v-card>
      </v-window-item>

      <!-- SQL CONSOLE TAB -->
      <v-window-item value="sql">
        ...
      </v-window-item>

      <!-- SCRIPTS TAB -->
      <v-window-item value="scripts">
        <ScriptsView />
      </v-window-item>

      <!-- HEALTH TAB -->
      <v-window-item value="health">
        <v-card rounded="lg" elevation="2">
          <v-card-title>
            <v-icon class="mr-2">mdi-information-outline</v-icon>
            Estado del sistema
          </v-card-title>
          <v-card-text>
            <v-row>
              <v-col cols="12" sm="6" md="3">
                <v-card variant="tonal" :color="health.api === 'ok' ? 'success' : 'error'">
                  <v-card-item>
                    <template #prepend><v-icon size="32">{{ health.api === 'ok' ? 'mdi-check-circle' : 'mdi-alert-circle' }}</v-icon></template>
                    <v-card-title>API</v-card-title>
                    <v-card-subtitle>{{ health.api === 'ok' ? 'Funcionando' : 'Error' }}</v-card-subtitle>
                  </v-card-item>
                </v-card>
              </v-col>
              <v-col cols="12" sm="6" md="3">
                <v-card variant="tonal" color="info">
                  <v-card-item>
                    <template #prepend><v-icon size="32">mdi-numeric</v-icon></template>
                    <v-card-title>Versión</v-card-title>
                    <v-card-subtitle>{{ health.version_name || '-' }} ({{ health.version_code || '-' }})</v-card-subtitle>
                  </v-card-item>
                </v-card>
              </v-col>
            </v-row>

            <v-row class="mt-4">
              <v-col cols="12">
                <h4 class="text-subtitle-1 mb-2">Quick Actions</h4>
                <v-btn variant="tonal" prepend-icon="mdi-refresh" @click="fetchHealth" :loading="loadingHealth" class="mr-2">
                  Refresh health
                </v-btn>
                <v-btn variant="tonal" prepend-icon="mdi-text-box-search-outline" @click="tab='logs'; fetchLogs()" class="mr-2">
                  Ver logs
                </v-btn>
                <v-btn variant="tonal" prepend-icon="mdi-database" @click="tab='db'; fetchTables()">
                  Ver DB
                </v-btn>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-window-item>
    </v-window>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import api from '../api/client'
import ScriptsView from './ScriptsView.vue'

const tab = ref('logs')
const wsConnected = ref(false)

// Logs
const logs = ref([])
const logContainer = ref('api')
const loadingLogs = ref(false)
const logError = ref('')

async function fetchLogs() {
  loadingLogs.value = true
  logError.value = ''
  try {
    const { data } = await api.get(`/admin/system/logs?container=${logContainer.value}&lines=100`)
    logs.value = data.logs || []
  } catch (e) {
    logError.value = e.response?.data?.detail || 'Error al cargar logs'
    logs.value = []
  } finally {
    loadingLogs.value = false
  }
}

// DB Explorer
const tables = ref([])
const loadingTables = ref(false)
const dbError = ref('')
const selectedTable = ref('')
const selectedTableColumns = ref([])
const selectedTablePreview = ref([])

async function fetchTables() {
  loadingTables.value = true
  dbError.value = ''
  try {
    const { data } = await api.get('/admin/system/db-tables')
    tables.value = data.tables || []
  } catch (e) {
    dbError.value = e.response?.data?.detail || 'Error al cargar tablas'
  } finally {
    loadingTables.value = false
  }
}

async function selectTable(name) {
  selectedTable.value = name
  try {
    const { data } = await api.get(`/admin/system/db-table/${name}`)
    selectedTableColumns.value = data.columns || []
    selectedTablePreview.value = data.preview || []
  } catch {
    selectedTableColumns.value = []
    selectedTablePreview.value = []
  }
}

// SQL Console
const sqlQuery = ref('SELECT * FROM progress LIMIT 10')
const sqlRows = ref([])
const sqlColumns = ref([])
const sqlError = ref('')
const loadingQuery = ref(false)
const sqlRowCount = ref(null)
const sqlElapsed = ref(null)

async function executeQuery() {
  if (!sqlQuery.value.trim()) return
  loadingQuery.value = true
  sqlError.value = ''
  sqlRows.value = []
  sqlColumns.value = []
  sqlRowCount.value = null
  sqlElapsed.value = null
  try {
    const { data } = await api.post('/admin/system/db-query', { query: sqlQuery.value }, {
      headers: { 'Content-Type': 'application/json' }
    })
    sqlColumns.value = data.columns || []
    sqlRows.value = data.rows || []
    sqlRowCount.value = data.rowCount || 0
    sqlElapsed.value = data.elapsed || 0
  } catch (e) {
    sqlError.value = e.response?.data?.detail || 'Error al ejecutar query'
  } finally {
    loadingQuery.value = false
  }
}

// Health
const health = ref({})
const loadingHealth = ref(false)
let logInterval = null

async function fetchHealth() {
  loadingHealth.value = true
  try {
    const { data } = await api.get('/admin/system/health')
    health.value = data
  } catch {
    health.value = { api: 'error' }
  } finally {
    loadingHealth.value = false
  }
}

onMounted(() => {
  fetchHealth()
  fetchLogs()
  logInterval = setInterval(fetchLogs, 5000)
})

onUnmounted(() => {
  if (logInterval) clearInterval(logInterval)
})
</script>
