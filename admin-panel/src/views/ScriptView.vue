<template>
  <div>
    <h1 class="text-h4 mb-2">Scripts</h1>
    <p class="text-grey mb-6">Ejecutá scripts JSON para crear, editar o eliminar contenido de forma masiva.</p>

    <v-card rounded="lg" elevation="2" class="mb-6">
      <v-card-text>
        <div class="d-flex align-center mb-4">
          <v-btn color="secondary" prepend-icon="mdi-content-copy" variant="tonal" @click="copyTemplate" class="mr-3">
            Copiar template para IA
          </v-btn>
          <v-btn color="success" prepend-icon="mdi-check-circle" @click="executeScript" :loading="executing" class="mr-3">
            Ejecutar script
          </v-btn>
          <v-spacer />
          <v-btn variant="text" size="small" @click="showHelp = !showHelp">
            {{ showHelp ? 'Ocultar ayuda' : 'Ver ayuda' }}
          </v-btn>
        </div>

        <v-expand-transition>
          <v-alert v-if="showHelp" type="info" variant="tonal" class="mb-4">
            <b>Formato:</b> JSON con <code>"version": "1.0"</code> y array <code>"actions"</code>.
            Cada acción tiene un <code>"action"</code> (upsert_subject, upsert_topic, upsert_unit, delete_unit, delete_topic, delete_subject).
          </v-alert>
        </v-expand-transition>

        <v-textarea
          v-model="script"
          label="Script JSON"
          variant="outlined"
          rows="16"
          placeholder='{"version":"1.0","actions":[...]}'
          hide-details
        />

        <v-card v-if="results.length" rounded="lg" variant="outlined" class="mt-4 pa-4">
          <div class="text-subtitle-1 font-weight-bold mb-2">Resultados ({{ results.length }} acciones)</div>
          <div v-for="r in results" :key="r.index" class="d-flex align-center py-1" :class="r.status === 'error' ? 'text-error' : 'text-success'">
            <v-icon :color="r.status === 'error' ? 'error' : 'success'" size="20" class="mr-2">
              {{ r.status === 'error' ? 'mdi-alert-circle' : 'mdi-check-circle' }}
            </v-icon>
            <span class="font-weight-medium mr-2">#{{ r.index }}</span>
            <span>{{ r.action }}</span>
            <span v-if="r.id" class="text-grey ml-1">({{ r.id }})</span>
            <span v-if="r.error" class="text-error ml-2">— {{ r.error }}</span>
          </div>
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

async function copyTemplate() {
  try {
    const { data } = await api.get('/admin/script/template')
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(data.template)
    } else {
      const el = document.createElement('textarea')
      el.value = data.template
      document.body.appendChild(el)
      el.select()
      document.execCommand('copy')
      document.body.removeChild(el)
    }
    snackbar.success('Template copiado al portapapeles')
  } catch (e) {
    snackbar.error('Error al copiar template')
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
  } catch {
    snackbar.error('JSON inválido. Revisá la sintaxis.')
    return
  }

  if (!parsed.actions || !Array.isArray(parsed.actions)) {
    snackbar.error('El JSON debe tener un array "actions"')
    return
  }

  executing.value = true
  results.value = []
  try {
    const { data } = await api.post('/admin/script', parsed)
    results.value = data.results || []
    snackbar.success('Script ejecutado')
  } catch (e) {
    results.value = [{ index: 0, status: 'error', action: 'script', error: e.response?.data?.detail || e.message }]
    snackbar.error('Error al ejecutar script')
  } finally {
    executing.value = false
  }
}
</script>