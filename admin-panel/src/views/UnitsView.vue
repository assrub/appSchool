<template>
  <div>
    <div class="d-flex align-center mb-6">
      <div>
        <v-btn variant="text" prepend-icon="mdi-arrow-left" :to="`/subjects/${subjectId}/topics`" class="mb-2">Temas</v-btn>
        <h1 class="text-h4">Unidades de {{ topicName }}</h1>
      </div>
      <v-spacer />
      <v-btn color="warning" variant="tonal" size="small" class="mr-2" @click="topicAction('lock-all')">
        <v-icon start>mdi-lock</v-icon> Bloquear todo
      </v-btn>
      <v-btn color="success" variant="tonal" size="small" class="mr-2" @click="topicAction('unlock-all')">
        <v-icon start>mdi-lock-open</v-icon> Desbloquear todo
      </v-btn>
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openDialog()">Nueva Unidad</v-btn>
    </div>

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4" closable @click:close="error = ''">
      {{ error }}
      <v-btn size="small" class="ml-2" @click="fetchData">Reintentar</v-btn>
    </v-alert>

    <v-row>
      <v-col cols="12" md="9">
        <v-card rounded="lg" elevation="2">
          <div class="d-flex align-center pa-2 border-b">
            <span class="text-overline">Unidades</span>
          </div>
          <v-progress-linear v-if="loading" indeterminate color="primary" />
          <v-table v-else>
            <thead>
              <tr>
                <th>Ord.</th>
                <th>ID</th>
                <th>Título</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(u, idx) in items" :key="u.id">
                <td>
                  <v-btn icon="mdi-chevron-up" variant="text" size="x-small" :disabled="idx === 0" @click="moveItem(idx, -1)" />
                  <v-btn icon="mdi-chevron-down" variant="text" size="x-small" :disabled="idx === items.length - 1" @click="moveItem(idx, 1)" />
                </td>
                <td class="font-weight-medium">{{ u.id }}</td>
                <td>{{ u.title }}</td>
                <td>
                  <v-chip :color="u.is_locked ? 'grey' : 'success'" size="small" variant="tonal">
                    <v-icon start size="14">{{ u.is_locked ? 'mdi-lock' : 'mdi-lock-open' }}</v-icon>
                    {{ u.is_locked ? 'Bloqueada' : 'Activa' }}
                  </v-chip>
                </td>
                <td>
                  <v-tooltip text="Editar unidad" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn icon="mdi-pencil" v-bind="tp" variant="text" size="small" color="primary" @click="openDialog(u)" />
                    </template>
                  </v-tooltip>
                  <v-tooltip text="Ver bloques" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn icon="mdi-list-box-outline" v-bind="tp" variant="text" size="small" color="secondary" :to="`/units/${u.id}/blocks`" />
                    </template>
                  </v-tooltip>
                  <v-tooltip text="Editar teoría" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn icon="mdi-book-open-page-variant" v-bind="tp" variant="text" size="small" color="info" :to="{ path: `/theory/unit/${u.id}`, query: { topicId } }" />
                    </template>
                  </v-tooltip>
                  <v-switch v-model="u.is_locked" color="warning" hide-details density="compact" inline @change="toggleLock(u, $event)" />
                  <v-tooltip text="Eliminar" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn icon="mdi-delete" v-bind="tp" variant="text" size="small" color="error" @click="confirmDelete(u)" />
                    </template>
                  </v-tooltip>
                </td>
              </tr>
            </tbody>
          </v-table>
          <v-card-text v-if="!loading && items.length === 0" class="text-center text-grey pa-8">
            No hay unidades. ¡Crea la primera!
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="3">
        <MobilePreview :html="listPreviewHtml" />
      </v-col>
    </v-row>

    <v-dialog v-model="dialog" max-width="850">
      <v-card rounded="lg">
        <v-card-title>{{ editing ? 'Editar' : 'Nueva' }} Unidad</v-card-title>
        <v-card-text>
          <v-row>
            <v-col cols="7">
              <v-text-field v-model="form.id" label="ID" :disabled="!!editing" :error-messages="v.errors.id" class="mb-2" />
              <v-text-field v-model="form.title" label="Título" :error-messages="v.errors.title" class="mb-2" />
              <v-text-field v-model="form.icon" label="Ícono (emoji)" :error-messages="v.errors.icon" class="mb-2" />
              <v-textarea v-model="form.explanation" label="Explicación" variant="outlined" rows="2" class="mb-3" />
              <div class="text-caption mb-1">Sonido de acierto</div>
              <div class="d-flex align-center mb-2">
                <v-text-field v-model="form.sound_correct_url" placeholder="URL o subir..." variant="outlined" density="compact" hide-details class="mr-1" />
                <v-btn icon="mdi-play" variant="text" size="small" color="success" @click="previewSound(form.sound_correct_url)" :disabled="!form.sound_correct_url" />
                <v-btn icon="mdi-upload" variant="text" size="small" color="primary" @click="triggerUpload('correct')" />
                <v-btn icon="mdi-close" variant="text" size="small" color="grey" @click="form.sound_correct_url = ''" />
              </div>
              <div class="text-caption mb-1">Sonido de error</div>
              <div class="d-flex align-center">
                <v-text-field v-model="form.sound_incorrect_url" placeholder="URL o subir..." variant="outlined" density="compact" hide-details class="mr-1" />
                <v-btn icon="mdi-play" variant="text" size="small" color="success" @click="previewSound(form.sound_incorrect_url)" :disabled="!form.sound_incorrect_url" />
                <v-btn icon="mdi-upload" variant="text" size="small" color="primary" @click="triggerUpload('incorrect')" />
                <v-btn icon="mdi-close" variant="text" size="small" color="grey" @click="form.sound_incorrect_url = ''" />
              </div>
            </v-col>
            <v-col cols="5" class="d-flex align-center justify-center">
              <MobilePreview :html="previewHtml" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="dialog = false">Cancelar</v-btn>
          <v-btn color="primary" :loading="saving" @click="save">{{ editing ? 'Guardar' : 'Crear' }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="deleteDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title>¿Eliminar unidad?</v-card-title>
        <v-card-text>"{{ toDelete?.title }}" se eliminará con sus bloques y ejercicios.</v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="deleteDialog = false">Cancelar</v-btn>
          <v-btn color="error" @click="doDelete">Eliminar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted, inject } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'
import MobilePreview from '../components/MobilePreview.vue'
import { useValidate } from '../composables/useValidation'

const route = useRoute()
const snackbar = inject('snackbar')

const topicId = route.params.topicId
const subjectId = ref('')
const topicName = ref('')
const items = ref([])
const loading = ref(true)
const error = ref('')
const dialog = ref(false)
const deleteDialog = ref(false)
const editing = ref(null)
const saving = ref(false)
const toDelete = ref(null)
const uploadType = ref('')

const form = ref({
  id: '',
  title: '',
  topic_id: topicId,
  exercise_type: 'fill-blank',
  input_mode: 'tap',
  explanation: '',
  icon: '',
  sound_correct_url: '',
  sound_incorrect_url: ''
})

const v = useValidate({
  id: [],
  title: [],
  input_mode: []
})

const validationRules = {
  id: { required: true, minLength: 2, maxLength: 50, pattern: /^[a-zA-Z0-9_-]+$/, patternMessage: 'Solo letras, números, guiones' },
  title: { required: true, minLength: 2, maxLength: 100 },
  input_mode: { required: true }
}

const previewHtml = computed(() => {
  const icon = form.value.icon || ''
  const title = form.value.title || 'Nombre de la unidad'
  const mode = form.value.input_mode === 'tap' ? 'Tocar' : 'Escribir'
  const header = '<div style="background:#4CAF50;color:white;padding:12px 16px;font-weight:bold;font-size:14px;flex-shrink:0;display:flex;align-items:center"><div style="flex:1">Unidad</div></div>'
  const bottomNav = '<div style="display:flex;justify-content:space-around;background:white;border-top:1px solid #e0e0e0;padding:8px 0 6px 0;flex-shrink:0"><div style="text-align:center;flex:1"><div style="font-size:18px">🏠</div><div style="font-size:10px;color:#757575;margin-top:2px">Inicio</div></div><div style="text-align:center;flex:1"><div style="font-size:18px">📖</div><div style="font-size:10px;color:#757575;margin-top:2px">Diccionario</div></div><div style="text-align:center;flex:1"><div style="font-size:18px">📊</div><div style="font-size:10px;color:#757575;margin-top:2px">Progreso</div></div><div style="text-align:center;flex:1"><div style="font-size:18px">⚙️</div><div style="font-size:10px;color:#757575;margin-top:2px">Ajustes</div></div></div>'

  return `<div style="display:flex;flex-direction:column;height:100%">${header}<div style="flex:1;overflow-y:auto;background:#f5f5f5;padding:12px"><div style="margin:0 8px;background:white;border-radius:16px;box-shadow:0 1px 2px rgba(0,0,0,0.3), 0 1px 3px 1px rgba(0,0,0,0.15)"><div style="display:flex;align-items:center;padding:20px"><div style="font-size:28px;margin-right:16px;flex-shrink:0">${icon}</div><div style="flex:1;min-width:0"><div style="font-size:20px;font-weight:bold;color:#212121;line-height:1.2">${title}</div><div style="font-size:14px;color:#757575;margin-top:4px">${mode}</div><div style="height:6px;border-radius:3px;background:#e0e0e0;margin-top:8px;overflow:hidden"><div style="width:0%;height:100%;border-radius:3px;background:#4CAF50"></div></div><div style="font-size:12px;color:#757575;margin-top:4px">0 / 0 items</div></div><div style="font-size:24px;color:#4CAF50;flex-shrink:0">→</div></div></div></div>${bottomNav}</div>`
})

const listPreviewHtml = computed(() => {
  const header = `<div style="background:#4CAF50;color:white;padding:12px 8px;font-weight:bold;font-size:14px;flex-shrink:0;display:flex;align-items:center"><div style="font-size:18px;margin-right:8px">←</div><div style="flex:1;text-align:center;margin-right:24px">${topicName.value || 'Unidades'}</div></div>`
  const tabs = '<div style="display:flex;background:white;border-bottom:1px solid #e0e0e0;flex-shrink:0"><div style="flex:1;text-align:center;padding:12px 0;font-size:10px;font-weight:bold;color:#4CAF50;border-bottom:2px solid #4CAF50">UNIDADES</div><div style="flex:1;text-align:center;padding:12px 0;font-size:10px;font-weight:500;color:#757575">TEORÍA</div></div>'
  const bottomNav = '<div style="display:flex;justify-content:space-around;background:white;border-top:1px solid #e0e0e0;padding:8px 0 6px 0;flex-shrink:0"><div style="text-align:center;flex:1"><div style="font-size:18px">🏠</div><div style="font-size:10px;color:#4CAF50;margin-top:2px">Inicio</div></div><div style="text-align:center;flex:1"><div style="font-size:18px">📖</div><div style="font-size:10px;color:#757575;margin-top:2px">Diccionario</div></div><div style="text-align:center;flex:1"><div style="font-size:18px">📊</div><div style="font-size:10px;color:#757575;margin-top:2px">Progreso</div></div><div style="text-align:center;flex:1"><div style="font-size:18px">⚙️</div><div style="font-size:10px;color:#757575;margin-top:2px">Ajustes</div></div></div>'

  if (!items.value.length) {
    return `<div style="display:flex;flex-direction:column;height:100%">${header}${tabs}<div style="flex:1;display:flex;align-items:center;justify-content:center;background:#f5f5f5;color:#757575;font-size:14px;padding:20px">No hay unidades</div>${bottomNav}</div>`
  }

  let cards = '<div style="padding:8px 0"></div>'
  cards += items.value.map(u => {
    const icon = u.icon || ''
    const locked = u.is_locked
    const mode = u.input_mode === 'tap' ? 'Tocar' : 'Escribir'
    return `<div style="margin:0 12px 12px 12px;background:white;border-radius:16px;box-shadow:0 1px 2px rgba(0,0,0,0.3), 0 1px 3px 1px rgba(0,0,0,0.15);${locked ? 'opacity:0.5' : ''}">
      <div style="display:flex;align-items:center;padding:18px">
        <div style="font-size:28px;margin-right:14px;flex-shrink:0">${locked ? '🔒' : icon}</div>
        <div style="flex:1;min-width:0">
          <div style="font-size:17px;font-weight:bold;color:${locked ? '#999' : '#212121'};line-height:1.2">${u.title||u.id}</div>
          <div style="font-size:12px;color:#757575;margin-top:2px">${mode}</div>
          <div style="height:6px;border-radius:3px;background:#e0e0e0;margin-top:8px;overflow:hidden">
            <div style="width:0%;height:100%;border-radius:3px;background:#4CAF50"></div>
          </div>
<div style="font-size:12px;color:#757575;margin-top:4px">${u.items_count||0} items</div>
        </div>
        <div style="font-size:24px;color:${locked ? '#999' : '#4CAF50'};flex-shrink:0">${locked ? '🔒' : '→'}</div>
      </div>
    </div>`
  }).join('')

  return `<div style="display:flex;flex-direction:column;height:100%">${header}${tabs}<div style="flex:1;overflow-y:auto;background:#f5f5f5">${cards}</div>${bottomNav}</div>`
})

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const [u, t] = await Promise.all([
      api.get(`/admin/topics/${topicId}/units`),
      api.get(`/admin/topics/${topicId}`)
    ])
    items.value = u.data
    topicName.value = t.data.name || topicId
    subjectId.value = t.data.subject_id
  } catch (e) {
    error.value = e.response?.data?.detail || 'Error al cargar'
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

function openDialog(unit = null) {
  editing.value = unit
  v.clearAllErrors()
  form.value = unit ? { ...unit, topic_id: topicId } : {
    id: '',
    title: '',
    topic_id: topicId,
    exercise_type: 'fill-blank',
    input_mode: 'tap',
    explanation: '',
    icon: '',
    sound_correct_url: '',
    sound_incorrect_url: ''
  }
  dialog.value = true
}

function validateForm() {
  return v.validateAll(form.value, validationRules)
}

async function save() {
  if (!validateForm()) return

  saving.value = true
  try {
    if (editing.value) {
      await api.put(`/admin/units/${editing.value.id}`, form.value)
      snackbar.success('Unidad actualizada correctamente')
    } else {
      await api.post('/admin/units', form.value)
      snackbar.success('Unidad creada correctamente')
    }
    dialog.value = false
    await fetchData()
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al guardar')
  } finally {
    saving.value = false
  }
}

function triggerUpload(type) {
  uploadType.value = type
  const el = document.createElement('input')
  el.type = 'file'
  el.accept = 'audio/*'
  el.onchange = async (e) => {
    const f = e.target.files[0]
    if (!f) return
    const fd = new FormData()
    fd.append('file', f)
    try {
      const { data } = await api.post('/admin/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
      if (type === 'correct') form.value.sound_correct_url = data.url
      else form.value.sound_incorrect_url = data.url
      snackbar.success('Archivo subido')
    } catch (e) {
      snackbar.error('Error al subir archivo')
    }
  }
  el.click()
}

function previewSound(url) {
  if (!url) return
  const a = new Audio(url)
  a.play()
}

function confirmDelete(u) {
  toDelete.value = u
  deleteDialog.value = true
}

async function doDelete() {
  try {
    await api.delete(`/admin/units/${toDelete.value.id}`)
    snackbar.success('Unidad eliminada')
    deleteDialog.value = false
    await fetchData()
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al eliminar')
  }
}

async function toggleLock(unit, lock) {
  const labels = { 'lock-all': 'Bloquear', 'unlock-all': 'Desbloquear' }
  try {
    await api.post(`/admin/topics/${topicId}/${action}`)
    snackbar.success(`${labels[action]} completado`)
    await fetchData()
  } catch (e) {
    snackbar.error(e.response?.data?.detail || `Error al ${labels[action].toLowerCase()}`)
  }
}

async function moveItem(idx, dir) {
  const a = items.value[idx]
  const b = items.value[idx + dir]
  const temp = a.sort_order
  a.sort_order = b.sort_order
  b.sort_order = temp
  const list = items.value.map(x => ({ id: x.id, sort_order: x.sort_order }))
  try {
    await api.put('/admin/units-reorder', { items: list })
    await fetchData()
  } catch (e) {
    snackbar.error('Error al reordenar')
  }
}
</script>