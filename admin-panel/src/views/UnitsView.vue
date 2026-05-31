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
      <v-btn color="orange" variant="tonal" size="small" class="mr-2" @click="topicAction('reset-all')">
        <v-icon start>mdi-restart</v-icon> Resetear todo
      </v-btn>
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openDialog()">Nueva Unidad</v-btn>
    </div>

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4" closable @click:close="error = ''">
      {{ error }}
      <v-btn size="small" class="ml-2" @click="fetchData">Reintentar</v-btn>
    </v-alert>

    <v-row>
      <v-col cols="7">
        <v-card rounded="lg" elevation="2">
          <v-progress-linear v-if="loading" indeterminate color="primary" />
          <v-table v-else>
            <thead>
              <tr>
                <th>Ord.</th>
                <th>ID</th>
                <th>Título</th>
                <th>Input</th>
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
                  <v-chip :color="u.input_mode === 'tap' ? 'success' : 'warning'" size="small" variant="tonal">
                    {{ u.input_mode === 'tap' ? 'Tocar' : 'Escribir' }}
                  </v-chip>
                </td>
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
                  <v-tooltip text="Resetear progreso" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn icon="mdi-restart" v-bind="tp" variant="text" size="small" color="orange" @click="confirmReset(u)" />
                    </template>
                  </v-tooltip>
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
      <v-col cols="5" class="d-flex align-start justify-center">
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
              <v-select v-model="form.input_mode" label="Modo" :items="[{ title: 'Tocar', value: 'tap' }, { title: 'Escribir', value: 'type' }]" :error-messages="v.errors.input_mode" class="mb-2" />
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

    <v-dialog v-model="resetDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title>¿Resetear progreso?</v-card-title>
        <v-card-text>
          Se reseteará el progreso de "<b>{{ toReset?.title }}</b>" para <b>TODOS</b> los usuarios.
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="resetDialog = false">Cancelar</v-btn>
          <v-btn color="orange" @click="doReset">Resetear</v-btn>
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
const resetDialog = ref(false)
const editing = ref(null)
const saving = ref(false)
const toDelete = ref(null)
const toReset = ref(null)
const uploadType = ref('')

const form = ref({
  id: '',
  title: '',
  topic_id: topicId,
  exercise_type: 'fill-blank',
  input_mode: 'tap',
  explanation: '',
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
  const icon = form.value.id ? '✏️' : '📝'
  const title = form.value.title || 'Nombre de la unidad'
  const mode = form.value.input_mode === 'tap' ? 'Tocar' : 'Escribir'
  return `<div style="padding:8px 12px">
    <div style="display:flex;align-items:center;gap:10px;margin-bottom:8px">
      <div style="font-size:32px">${icon}</div>
      <div style="flex:1">
        <div style="font-weight:bold;font-size:16px">${title}</div>
        <div style="font-size:12px;color:#999">${mode}</div>
      </div>
    </div>
    <div style="height:6px;border-radius:3px;background:#e0e0e0;margin-bottom:4px">
      <div style="width:0%;height:100%;border-radius:3px;background:#4CAF50"></div>
    </div>
    <div style="font-size:11px;color:#999">0 / 0 items</div>
  </div>`
})

const listPreviewHtml = computed(() => {
  if (!items.value.length) return '<p style="color:#999;text-align:center;padding:20px">Sin unidades</p>'
  const unitIcons = ['✏️', '❌', '❓', '✅', '🔄', '📝']
  return items.value.map((u, i) => {
    const icon = unitIcons[i % unitIcons.length]
    const title = u.title || u.id
    const mode = u.input_mode === 'tap' ? 'Tocar' : 'Escribir'
    const locked = u.is_locked
    return `<div style="display:flex;align-items:center;gap:12px;padding:14px 12px;border-bottom:1px solid #f0f0f0;${locked ? 'opacity:0.5' : ''}">
      <div style="font-size:28px">${locked ? '🔒' : icon}</div>
      <div style="flex:1;min-width:0">
        <div style="font-weight:bold;font-size:14px;color:${locked ? '#999' : '#333'}">${title}</div>
        <div style="font-size:11px;color:#999">${mode}</div>
        <div style="height:6px;border-radius:3px;background:#e0e0e0;margin-top:6px;overflow:hidden">
          <div style="width:0%;height:100%;border-radius:3px;background:#4CAF50"></div>
        </div>
      </div>
      <div style="font-size:20px;color:${locked ? '#999' : '#4CAF50'}">${locked ? '🔒' : '→'}</div>
    </div>`
  }).join('')
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
  try {
    await api.put(`/admin/units/${unit.id}/${lock ? 'lock' : 'unlock'}`)
    snackbar.success(lock ? 'Unidad bloqueada' : 'Unidad desbloqueada')
    await fetchData()
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al cambiar estado')
  }
}

function confirmReset(u) {
  toReset.value = u
  resetDialog.value = true
}

async function doReset() {
  try {
    await api.delete(`/admin/progress/${topicId}/${toReset.value.id}/reset-all-users`)
    snackbar.success('Progreso reseteado')
    resetDialog.value = false
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al resetear')
  }
}

async function topicAction(action) {
  const labels = { 'lock-all': 'Bloquear', 'unlock-all': 'Desbloquear', 'reset-all': 'Resetear' }
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