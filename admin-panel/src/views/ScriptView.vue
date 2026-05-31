<template>
  <div>
    <div class="d-flex align-center mb-4">
      <div>
        <h1 class="text-h4">Scripts</h1>
        <p class="text-grey text-body-2">Ejecutá scripts JSON para crear, editar o eliminar contenido de forma masiva.</p>
      </div>
      <v-spacer />
      <v-btn variant="text" size="small" @click="showHelp = !showHelp">
        {{ showHelp ? 'Ocultar ayuda' : 'Ver ayuda' }}
      </v-btn>
    </div>

    <v-expand-transition>
      <v-alert v-if="showHelp" type="info" variant="tonal" class="mb-4" density="compact">
        <b>Formato:</b> JSON con <code>"version": "1.0"</code> y array <code>"actions"</code>.<br />
        Acciones: <code>upsert_subject</code>, <code>upsert_topic</code>, <code>upsert_unit</code>, <code>delete_unit</code>, <code>delete_topic</code>, <code>delete_subject</code>.<br />
        <b>Tip:</b> Usá "Copiar template" para obtener la estructura exacta.
      </v-alert>
    </v-expand-transition>

    <v-card rounded="lg" elevation="2">
      <v-card-text>
        <div class="d-flex align-center mb-4 flex-wrap ga-2">
          <v-btn color="secondary" prepend-icon="mdi-content-copy" variant="tonal" size="small" @click="copyTemplate">
            Copiar template
          </v-btn>
          <v-btn color="success" prepend-icon="mdi-play" @click="executeScript" :loading="executing" size="small">
            Ejecutar
          </v-btn>
          <v-btn variant="outlined" prepend-icon="mdi-format-validation" @click="validateJson" :disabled="!script.trim()" size="small">
            Validar JSON
          </v-btn>
          <v-btn variant="text" prepend-icon="mdi-close" @click="clearAll" size="small" color="grey">
            Limpiar
          </v-btn>
        </div>

        <v-textarea
          v-model="script"
          label="Script JSON"
          variant="outlined"
          rows="18"
          placeholder='{ "version": "1.0", "actions": [ ... ] }'
          hide-details
          class="font-monospace"
          style="font-size: 13px"
          :error="jsonError !== null"
          :error-messages="jsonError"
        />

        <v-alert v-if="validationResult" type="warning" variant="tonal" class="mt-3" density="compact">
          <b>Validación:</b> {{ validationResult }}
        </v-alert>

        <v-card v-if="results.length" rounded="lg" variant="outlined" class="mt-4">
          <v-card-text>
            <div class="d-flex align-center mb-3">
              <span class="text-subtitle-1 font-weight-bold">Resultados ({{ results.length }} acciones)</span>
              <v-spacer />
              <v-btn variant="text" size="x-small" color="grey" @click="results = []">Limpiar</v-btn>
            </div>
            <div class="results-scroll" style="max-height: 300px; overflow-y: auto">
              <div v-for="r in results" :key="r.index" class="d-flex align-start py-2 px-1" :class="r.status === 'error' ? 'bg-red-lighten-5' : 'bg-green-lighten-5' + ' rounded mb-1'">
                <v-icon :color="r.status === 'error' ? 'error' : 'success'" size="18" class="mt-1 mr-2">
                  {{ r.status === 'error' ? 'mdi-close-circle' : 'mdi-check-circle' }}
                </v-icon>
                <div class="flex-grow-1">
                  <span class="font-weight-medium">#{{ r.index + 1 }} {{ r.action }}</span>
                  <span v-if="r.id" class="text-grey ml-1">({{ r.id }})</span>
                  <div v-if="r.error" class="text-error text-caption mt-1">{{ r.error }}</div>
                </div>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import { ref, inject } from 'vue'
import api from '../api/client'

const snackbar = inject('snackbar')

const script = ref('')
const executing = ref(false)
const results = ref([])
const showHelp = ref(false)
const jsonError = ref(null)
const validationResult = ref(null)

async function copyTemplate() {
  try {
    const { data } = await api.get('/admin/script/template')
    script.value = data.template
    jsonError.value = null
    validationResult.value = null
    snackbar.success('Template copiado')
  } catch (e) {
    snackbar.error('Error al obtener template')
  }
}

function validateJson() {
  validationResult.value = null
  jsonError.value = null
  if (!script.value.trim()) {
    validationResult.value = 'El campo está vacío'
    return
  }
  try {
    const parsed = JSON.parse(script.value)
    if (!parsed.version) {
      validationResult.value = 'Falta "version" en el root'
      return
    }
    if (!parsed.actions || !Array.isArray(parsed.actions)) {
      validationResult.value = 'Falta o es inválido el array "actions"'
      return
    }
    const issues = []
    parsed.actions.forEach((action, i) => {
      if (!action.action) issues.push(`Acción #${i + 1}: falta "action"`)
      else {
        if (action.action !== 'delete_subject' && action.action !== 'delete_topic' && action.action !== 'delete_unit') {
          if (!action.id) issues.push(`Acción #${i + 1} (${action.action}): falta "id"`)
        }
        if (action.action === 'upsert_topic') {
          if (!action.subject_id) issues.push(`Acción #${i + 1} (upsert_topic): falta "subject_id"`)
          if (action.name === undefined) issues.push(`Acción #${i + 1} (upsert_topic): falta "name"`)
        }
        if (action.action === 'upsert_unit') {
          if (!action.topic_id) issues.push(`Acción #${i + 1} (upsert_unit): falta "topic_id"`)
          if (action.name === undefined) issues.push(`Acción #${i + 1} (upsert_unit): usar "name" (no "title")`)
          if (action.exercises && !Array.isArray(action.exercises)) issues.push(`Acción #${i + 1}: "exercises" debe ser array`)
        }
        if (action.action === 'upsert_subject' && action.name === undefined) {
          issues.push(`Acción #${i + 1} (upsert_subject): falta "name"`)
        }
      }
    })
    if (issues.length) {
      validationResult.value = issues.join(' | ')
    } else {
      validationResult.value = `Válido! ${parsed.actions.length} acciones detectadas`
    }
  } catch (e) {
    jsonError.value = e.message
    validationResult.value = 'JSON inválido - ' + e.message
  }
}

async function executeScript() {
  if (!script.value.trim()) {
    snackbar.warning('Pegá un script JSON primero')
    return
  }

  let parsed
  try {
    parsed = JSON.parse(script.value)
  } catch (e) {
    snackbar.error('JSON inválido: ' + e.message)
    return
  }

  if (!parsed.actions || !Array.isArray(parsed.actions)) {
    snackbar.error('El JSON debe tener un array "actions"')
    return
  }

  executing.value = true
  results.value = []
  jsonError.value = null
  validationResult.value = null
  try {
    const { data } = await api.post('/admin/script', parsed)
    results.value = data.results || []
    snackbar.success('Script ejecutado')
  } catch (e) {
    const errMsg = e.response?.data?.detail || e.message
    results.value = [{ index: 0, status: 'error', action: 'script', error: errMsg }]
    snackbar.error('Error al ejecutar script')
  } finally {
    executing.value = false
  }
}

function clearAll() {
  script.value = ''
  results.value = []
  jsonError.value = null
  validationResult.value = null
}
</script>

<style scoped>
.results-scroll::-webkit-scrollbar { width: 6px; }
.results-scroll::-webkit-scrollbar-thumb { background: #ccc; border-radius: 3px; }
</style>