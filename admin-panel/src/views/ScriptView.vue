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
        <b>Tip:</b> Usá "Copiar todo" para obtener el template JSON y la guía IA para crear teorías.
      </v-alert>
    </v-expand-transition>

    <v-card rounded="lg" elevation="2">
      <v-tabs v-model="tab" color="primary" align-tabs="start">
        <v-tab value="template">
          <v-icon start>mdi-code-json</v-icon>
          Template JSON
        </v-tab>
        <v-tab value="ai-guide">
          <v-icon start>mdi-robot</v-icon>
          Guía IA para Teorías
        </v-tab>
      </v-tabs>

      <v-divider />

      <v-card-text>
        <div class="d-flex align-center mb-4 flex-wrap ga-2">
          <v-btn color="primary" prepend-icon="mdi-content-copy" variant="flat" size="small" @click="copyAll" :loading="loading">
            <v-icon start size="18">mdi-content-copy</v-icon>
            Copiar todo (Template + Guía IA)
          </v-btn>
           <v-btn color="success" prepend-icon="mdi-play" @click="confirmExecute" :loading="executing" size="small">
            Ejecutar script
          </v-btn>
          <v-btn variant="outlined" prepend-icon="mdi-format-validation" @click="validateJson" :disabled="!script.trim()" size="small">
            Validar
          </v-btn>
          <v-btn variant="text" prepend-icon="mdi-close" @click="clearAll" size="small" color="grey">
            Limpiar
          </v-btn>
        </div>

        <v-window v-model="tab">
          <v-window-item value="template">
            <v-textarea
              v-model="script"
              label="Script JSON"
              variant="outlined"
              rows="20"
              placeholder='{ "version": "1.0", "actions": [ ... ] }'
              hide-details
              class="font-monospace"
              style="font-size: 13px"
              :error="jsonError !== null"
              :error-messages="jsonError"
            />
          </v-window-item>

          <v-window-item value="ai-guide">
            <v-alert type="info" variant="tonal" class="mb-4">
              <b>Esta guía</b> la podés usar para darle contexto a una IA (Claude, ChatGPT, etc.) cuando quieras que te ayude a crear teorías para topics, unidades y bloques. Copiala junto con el template JSON.
            </v-alert>
            <v-textarea
              v-model="aiGuide"
              label="Guía IA para crear teorías"
              variant="outlined"
              rows="25"
              hide-details
              class="font-monospace"
              style="font-size: 12px; background: #f8f9fa;"
              readonly
              auto-grow
            />
          </v-window-item>
        </v-window>

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
              <div v-for="r in results" :key="r.index" class="d-flex align-start py-2 px-1 rounded mb-1" :class="r.status === 'error' ? 'bg-red-lighten-5' : 'bg-green-lighten-5'">
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

    <v-dialog v-model="confirmDialog" max-width="500">
      <v-card rounded="lg">
        <v-card-title class="text-warning">
          <v-icon class="mr-2">mdi-alert</v-icon>
          ¿Ejecutar script?
        </v-card-title>
        <v-card-text>
          <p>Se ejecutarán <b>{{ actionCount }}</b> acciones que pueden <b>crear, modificar o eliminar</b> contenido.</p>
          <v-chip v-if="actionCount" size="small" color="warning" variant="tonal" class="mt-2">
            Revisá bien el JSON antes de ejecutar
          </v-chip>
          <div v-if="actionSummary.length" class="mt-3">
            <div class="text-caption text-grey mb-1">Resumen:</div>
            <div v-for="s in actionSummary" :key="s.label" class="text-caption">
              • {{ s.label }}: <b>{{ s.count }}</b>
            </div>
          </div>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="confirmDialog = false">Cancelar</v-btn>
          <v-btn color="success" @click="confirmedExecute">Ejecutar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, computed, inject } from 'vue'
import api from '../api/client'

const snackbar = inject('snackbar')

const tab = ref('template')
const script = ref('')
const aiGuide = ref('')
const loading = ref(false)
const executing = ref(false)
const results = ref([])
const showHelp = ref(false)
const jsonError = ref(null)
const validationResult = ref(null)
const confirmDialog = ref(false)
const pendingScript = ref(null)

const actionCount = computed(() => pendingScript.value?.actions?.length || 0)

const actionSummary = computed(() => {
  if (!pendingScript.value?.actions) return []
  const counts = {}
  pendingScript.value.actions.forEach(a => {
    const action = a.action || 'unknown'
    counts[action] = (counts[action] || 0) + 1
  })
  return Object.entries(counts).map(([label, count]) => ({ label, count }))
})

async function copyAll() {
  loading.value = true
  try {
    const { data } = await api.get('/admin/script/template')
    script.value = data.template
    aiGuide.value = data.aiGuide
    jsonError.value = null
    validationResult.value = null

    const fullContent = `===========================================
# TEMPLATE JSON - AppSchool
===========================================
${data.template}

===========================================
# GUÍA IA PARA CREAR TEORÍAS - AppSchool
===========================================
Copiá esta guía y usala como contexto cuando le pidas a una IA que te ayude a crear contenido teórico para la app.

${data.aiGuide}
`

    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(fullContent)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = fullContent
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    snackbar.success('Template JSON + Guía IA copiados al portapapeles')
  } catch (e) {
    snackbar.error('Error al obtener template: ' + (e.message || 'Error desconocido'))
  } finally {
    loading.value = false
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
          if (action.title === undefined) issues.push(`Acción #${i + 1} (upsert_unit): falta "title"`)
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

function confirmExecute() {
  if (!script.value.trim()) {
    snackbar.warning('Pegá un script JSON primero')
    return
  }
  try {
    pendingScript.value = JSON.parse(script.value)
    if (!pendingScript.value.actions || !Array.isArray(pendingScript.value.actions)) {
      snackbar.error('El JSON debe tener un array "actions"')
      return
    }
    validateJson()
    confirmDialog.value = true
  } catch (e) {
    snackbar.error('JSON inválido: ' + e.message)
  }
}

async function confirmedExecute() {
  confirmDialog.value = false
  if (!pendingScript.value) return
  await executeScript()
}

async function executeScript() {
  const parsed = pendingScript.value
  if (!parsed) return

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
    pendingScript.value = null
  }
}

function clearAll() {
  script.value = ''
  results.value = []
  jsonError.value = null
  validationResult.value = null
  pendingScript.value = null
}
</script>

<style scoped>
.results-scroll::-webkit-scrollbar { width: 6px; }
.results-scroll::-webkit-scrollbar-thumb { background: #ccc; border-radius: 3px; }
</style>