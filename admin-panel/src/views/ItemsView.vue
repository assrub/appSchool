<template>
  <div>
    <div class="d-flex align-center mb-6">
      <div>
        <v-btn variant="text" prepend-icon="mdi-arrow-left" :to="`/units/${unitId}/blocks`" class="mb-2">
          Bloques
        </v-btn>
        <h1 class="text-h4">Ejercicios</h1>
        <p class="text-grey">{{ blockTitle }}</p>
      </div>
      <v-spacer />
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openDialog()">Nuevo</v-btn>
    </div>

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4" closable @click:close="error = ''">
      {{ error }}
      <v-btn size="small" class="ml-2" @click="fetchData">Reintentar</v-btn>
    </v-alert>

    <v-card rounded="lg" elevation="2">
      <v-progress-linear v-if="loading" indeterminate color="primary" />
      <v-table v-else>
        <thead>
          <tr>
            <th>#</th>
            <th>Tipo</th>
            <th>Input</th>
            <th>Contenido</th>
            <th>Resp.</th>
            <th>Ops</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(i, idx) in items" :key="i.id">
            <td>
              <v-btn
                icon="mdi-chevron-up"
                variant="text"
                size="x-small"
                :disabled="idx === 0"
                @click="moveItem(idx, -1)"
              />
              <v-btn
                icon="mdi-chevron-down"
                variant="text"
                size="x-small"
                :disabled="idx === items.length - 1"
                @click="moveItem(idx, 1)"
              />
            </td>
            <td>
              <v-chip size="x-small" color="primary" variant="tonal">
                {{ i.item_type }}
              </v-chip>
            </td>
            <td>
              <v-chip
                v-if="i.input_mode"
                :color="i.input_mode === 'tap' ? 'success' : 'warning'"
                size="x-small"
                variant="tonal"
              >
                {{ i.input_mode === 'tap' ? 'Tap' : 'Type' }}
              </v-chip>
              <span v-else class="text-caption text-grey">hereda</span>
            </td>
            <td style="max-width: 200px" class="text-truncate">
              {{ i.sentence || i.question || '-' }}
            </td>
            <td>
              {{ i.answer || '-' }}
              <span v-if="i.answers?.length" class="text-caption text-success"> +{{ i.answers.length }}</span>
            </td>
            <td>
              <span v-if="i.options?.length" class="text-caption">{{ i.options.length }} ops</span>
              <span v-else class="text-caption text-grey">-</span>
            </td>
            <td>
              <v-btn
                icon="mdi-pencil"
                variant="text"
                size="x-small"
                color="primary"
                @click="openDialog(i)"
              />
              <v-btn
                icon="mdi-delete"
                variant="text"
                size="x-small"
                color="error"
                @click="confirmDelete(i)"
              />
            </td>
          </tr>
        </tbody>
      </v-table>
      <v-card-text v-if="!loading && items.length === 0" class="text-center text-grey pa-8">
        No hay ejercicios. ¡Crea el primero!
      </v-card-text>
    </v-card>

    <v-dialog v-model="dialog" max-width="900">
      <v-card rounded="lg">
        <v-card-title>{{ editing ? 'Editar' : 'Nuevo' }} Ejercicio</v-card-title>
        <v-card-text>
          <v-row>
            <v-col cols="7">
              <v-select
                v-model="form.item_type"
                label="Tipo"
                :items="itemTypes"
                variant="outlined"
                class="mb-3"
              />
              <v-select
                v-model="form.input_mode"
                label="Modo"
                :items="inputModes"
                variant="outlined"
                class="mb-3"
                clearable
                hint="Vacío = hereda de la unidad"
                persistent-hint
              />

              <template v-if="form.item_type === 'fill-blank'">
                <v-text-field
                  v-model="form.sentence"
                  label="Frase (usá ______ para el espacio)"
                  :error-messages="v.errors.sentence"
                  variant="outlined"
                  class="mb-3"
                />
                <div class="text-caption mb-1">Respuesta correcta</div>
                <v-text-field
                  v-model="form.answer"
                  label="Principal"
                  :error-messages="v.errors.answer"
                  variant="outlined"
                  density="compact"
                  class="mb-2"
                />
                <div class="mb-3">
                  <div class="text-caption">Alternativas (otras respuestas válidas)</div>
                  <div v-for="(a, i) in answersList" :key="i" class="d-flex align-center mb-1">
                    <v-text-field
                      v-model="answersList[i]"
                      variant="outlined"
                      density="compact"
                      hide-details
                      class="mr-1"
                    />
                    <v-btn
                      icon="mdi-close"
                      variant="text"
                      size="x-small"
                      color="error"
                      @click="answersList.splice(i, 1)"
                    />
                  </div>
                  <v-btn
                    size="x-small"
                    variant="outlined"
                    class="mt-1"
                    @click="answersList.push('')"
                  >
                    + Alternativa
                  </v-btn>
                </div>

                <div class="mb-3">
                  <div class="text-caption">Opciones visibles (modo Tap). Si están vacías, se generan automáticamente.</div>
                  <div v-for="(o, i) in optionsList" :key="i" class="d-flex align-center mb-1">
                    <v-text-field
                      v-model="optionsList[i]"
                      variant="outlined"
                      density="compact"
                      hide-details
                      class="mr-1"
                    />
                    <v-btn
                      icon="mdi-close"
                      variant="text"
                      size="x-small"
                      color="error"
                      @click="optionsList.splice(i, 1)"
                    />
                  </div>
                  <v-btn size="x-small" variant="outlined" class="mt-1" @click="optionsList.push('')">
                    + Opción
                  </v-btn>
                </div>

                <v-text-field v-model="form.hint" label="Pista (opcional)" variant="outlined" />
              </template>

              <template v-else-if="form.item_type === 'multiple-choice'">
                <v-text-field
                  v-model="form.question"
                  label="Pregunta"
                  :error-messages="v.errors.question"
                  variant="outlined"
                  class="mb-2"
                />
                <div v-for="(o, i) in formOptions" :key="i" class="d-flex align-center mb-2">
                  <v-text-field
                    v-model="formOptions[i]"
                    :label="'Opción ' + (i + 1)"
                    variant="outlined"
                    density="compact"
                    hide-details
                    class="flex-grow-1 mr-2"
                  />
                  <v-btn
                    icon="mdi-delete"
                    variant="text"
                    size="small"
                    color="error"
                    @click="formOptions.splice(i, 1)"
                  />
                </div>
                <v-btn variant="outlined" size="small" prepend-icon="mdi-plus" @click="formOptions.push('')" class="mb-2">
                  Opción
                </v-btn>
                <v-text-field
                  v-model="form.answer"
                  label="Correcta"
                  :error-messages="v.errors.answer"
                  variant="outlined"
                />
              </template>

              <template v-else-if="form.item_type === 'reorder'">
                <v-textarea
                  v-model="wordsText"
                  label="Palabras (una por línea)"
                  variant="outlined"
                  rows="4"
                  class="mb-2"
                />
                <v-text-field
                  v-model="correctOrderText"
                  label="Orden correcto (separado por espacios)"
                  variant="outlined"
                  class="mb-2"
                />
                <v-text-field v-model="form.hint" label="Pista" variant="outlined" />
              </template>

              <template v-else-if="form.item_type === 'listening'">
                <v-text-field v-model="form.sentence" label="Frase" variant="outlined" class="mb-2" />
                <v-text-field v-model="form.audio_url" label="URL audio" variant="outlined" class="mb-2" />
                <v-text-field v-model="form.answer" label="Respuesta" variant="outlined" />
              </template>

              <template v-else-if="form.item_type === 'matching'">
                <div v-for="(p, i) in formPairs" :key="i" class="d-flex align-center mb-2">
                  <v-text-field
                    v-model="formPairs[i].left"
                    label="Izq"
                    variant="outlined"
                    density="compact"
                    hide-details
                    class="mr-2"
                  />
                  <v-icon>mdi-arrow-right</v-icon>
                  <v-text-field
                    v-model="formPairs[i].right"
                    label="Der"
                    variant="outlined"
                    density="compact"
                    hide-details
                    class="ml-2"
                  />
                  <v-btn
                    icon="mdi-delete"
                    variant="text"
                    size="small"
                    color="error"
                    @click="formPairs.splice(i, 1)"
                  />
                </div>
                <v-btn variant="outlined" size="small" prepend-icon="mdi-plus" @click="formPairs.push({ left: '', right: '' })">
                  Par
                </v-btn>
              </template>

              <template v-else-if="form.item_type === 'true-false'">
                <v-text-field v-model="form.sentence" label="Frase" variant="outlined" class="mb-2" />
                <v-switch v-model="form.is_correct_boolean" label="¿Es correcta?" color="primary" hide-details />
                <v-text-field
                  v-if="!form.is_correct_boolean"
                  v-model="form.answer"
                  label="Corrección"
                  variant="outlined"
                />
              </template>
            </v-col>
            <v-col cols="5" class="d-flex align-center justify-center">
              <MobilePreview :html="previewExerciseHtml" />
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="dialog = false">Cancelar</v-btn>
          <v-btn color="primary" :loading="saving" @click="save">
            {{ editing ? 'Guardar' : 'Crear' }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="deleteDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title>¿Eliminar ejercicio?</v-card-title>
        <v-card-text>Esta acción no se puede deshacer.</v-card-text>
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

const blockId = route.params.blockId
const unitId = ref('')
const blockTitle = ref('')
const items = ref([])
const loading = ref(true)
const error = ref('')
const dialog = ref(false)
const deleteDialog = ref(false)
const editing = ref(null)
const saving = ref(false)
const toDelete = ref(null)

const itemTypes = [
  { title: 'Completar vacío (fill-blank)', value: 'fill-blank' },
  { title: 'Opción múltiple (multiple-choice)', value: 'multiple-choice' },
  { title: 'Ordenar (reorder)', value: 'reorder' },
  { title: 'Escuchar (listening)', value: 'listening' },
  { title: 'Emparejar (matching)', value: 'matching' },
  { title: 'Verdadero/Falso (true-false)', value: 'true-false' }
]
const inputModes = [
  { title: 'Tocar (tap)', value: 'tap' },
  { title: 'Escribir (type)', value: 'type' }
]
const answersList = ref([])
const optionsList = ref([])
const formOptions = ref(['', '', ''])
const formPairs = ref([{ left: '', right: '' }, { left: '', right: '' }])
const wordsText = ref('')
const correctOrderText = ref('')

const form = ref({
  block_id: Number(blockId),
  item_type: 'fill-blank',
  sentence: '',
  answer: '',
  answers: null,
  hint: '',
  question: '',
  options: null,
  words: null,
  correct_order: null,
  input_mode: null,
  audio_url: '',
  pairs: null,
  is_correct_boolean: null,
  sort_order: 0
})

const v = useValidate({
  sentence: [],
  answer: [],
  question: []
})

const validationRules = computed(() => ({
  sentence: form.value.item_type === 'fill-blank' || form.value.item_type === 'listening' || form.value.item_type === 'true-false' ? { required: true } : {},
  answer: form.value.item_type === 'fill-blank' || form.value.item_type === 'multiple-choice' || form.value.item_type === 'listening' ? { required: true } : {},
  question: form.value.item_type === 'multiple-choice' ? { required: true } : {}
}))

const previewExerciseHtml = computed(() => {
  const t = form.value.item_type
  const bottomNav = '<div style="display:flex;justify-content:space-around;background:white;border-top:1px solid #e0e0e0;padding:8px 0 6px 0;flex-shrink:0"><div style="text-align:center;flex:1"><div style="font-size:18px">🏠</div><div style="font-size:10px;color:#757575;margin-top:2px">Inicio</div></div><div style="text-align:center;flex:1"><div style="font-size:18px">📖</div><div style="font-size:10px;color:#757575;margin-top:2px">Diccionario</div></div><div style="text-align:center;flex:1"><div style="font-size:18px">📊</div><div style="font-size:10px;color:#757575;margin-top:2px">Progreso</div></div><div style="text-align:center;flex:1"><div style="font-size:18px">⚙️</div><div style="font-size:10px;color:#757575;margin-top:2px">Ajustes</div></div></div>'
  if (t === 'fill-blank') {
    const s = form.value.sentence || '...'
    const opts = optionsList.value.filter(o => o.trim())
    const mode = form.value.input_mode || 'tap'
    const blankHtml = s.replace(/_+/g, '<span style="border-bottom:2px dashed #999;padding:2px 12px;margin:0 4px;display:inline-block;font-size:22px">______</span>')
    if (mode === 'type') {
      return `<div style="display:flex;flex-direction:column;height:100%"><div style="flex:1;overflow-y:auto;padding:12px;background:#f5f5f5"><div style="margin:0 8px 12px 8px;background:white;border-radius:16px;padding:24px;box-shadow:0 4px 12px rgba(0,0,0,0.15)"><p style="font-size:22px;text-align:center;margin:0 0 20px 0;font-weight:bold">${blankHtml}</p><div style="border:2px solid #e0e0e0;border-radius:12px;padding:18px;text-align:center;color:#999;font-size:18px">Escribí tu respuesta...</div><div style="text-align:center;margin-top:16px"><span style="background:#4CAF50;color:white;padding:14px 36px;border-radius:12px;font-size:18px;font-weight:bold;display:inline-block">Corregir</span></div></div></div>${bottomNav}</div>`
    }
    const btns = opts.length ? opts : ['am', 'is', 'are']
    return `<div style="display:flex;flex-direction:column;height:100%"><div style="flex:1;overflow-y:auto;padding:12px;background:#f5f5f5"><div style="margin:0 8px 12px 8px;background:white;border-radius:16px;padding:24px;box-shadow:0 4px 12px rgba(0,0,0,0.15)"><p style="font-size:22px;text-align:center;margin:0 0 20px 0;font-weight:bold">${blankHtml}</p><div style="display:flex;gap:12px;justify-content:center;flex-wrap:wrap">${btns.map(o => `<span style="background:#f5f5f5;border-radius:12px;padding:14px 22px;font-weight:bold;font-size:20px;box-shadow:0 2px 6px rgba(0,0,0,0.1)">${o}</span>`).join('')}</div></div></div>${bottomNav}</div>`
  }
  if (t === 'multiple-choice') {
    const q = form.value.question || 'Pregunta...'
    const opts = formOptions.value.filter(o => o.trim())
    if (opts.length === 0) opts.push('Opción 1', 'Opción 2', 'Opción 3')
    return `<div style="display:flex;flex-direction:column;height:100%"><div style="flex:1;overflow-y:auto;padding:12px;background:#f5f5f5"><div style="margin:0 8px 12px 8px;background:white;border-radius:16px;padding:24px;box-shadow:0 4px 12px rgba(0,0,0,0.15)"><p style="font-size:18px;font-weight:bold;text-align:center;margin:0 0 20px 0">${q}</p><div>${opts.map((o, i) => `<div style="border:2px solid #e0e0e0;border-radius:12px;padding:16px;margin-bottom:10px;font-size:16px;text-align:center;cursor:pointer">${String.fromCharCode(65 + i)}) ${o}</div>`).join('')}</div></div></div>${bottomNav}</div>`
  }
  if (t === 'reorder') {
    const words = wordsText.value ? wordsText.value.split('\n').filter(w => w.trim()) : ['palabra1', 'palabra2', 'palabra3']
    return `<div style="display:flex;flex-direction:column;height:100%"><div style="flex:1;overflow-y:auto;padding:12px;background:#f5f5f5"><div style="margin:0 8px 12px 8px;background:white;border-radius:16px;padding:24px;box-shadow:0 4px 12px rgba(0,0,0,0.15)"><p style="font-size:14px;text-align:center;color:#999;margin:0 0 16px 0">Ordená las palabras:</p><div style="display:flex;gap:10px;flex-wrap:wrap;justify-content:center;margin:8px 0">${words.map(w => `<span style="background:#e3f2fd;border-radius:12px;padding:12px 18px;font-weight:bold;font-size:18px">${w.trim()}</span>`).join('')}</div><div style="border:2px dashed #4CAF50;border-radius:12px;padding:14px;min-height:50px;text-align:center;color:#999;margin-top:12px;font-size:15px">Soltá acá</div></div></div>${bottomNav}</div>`
  }
  if (t === 'listening') {
    return `<div style="display:flex;flex-direction:column;height:100%"><div style="flex:1;overflow-y:auto;padding:12px;background:#f5f5f5"><div style="margin:0 8px 12px 8px;background:white;border-radius:16px;padding:28px;box-shadow:0 4px 12px rgba(0,0,0,0.15);text-align:center"><span style="font-size:60px;display:block">🔊</span><p style="color:#999;margin:12px 0 20px 0;font-size:15px">Escuchá y escribí</p><div style="border:2px solid #e0e0e0;border-radius:12px;padding:16px;min-height:40px;color:#ccc;font-size:16px">Escribí tu respuesta...</div></div></div>${bottomNav}</div>`
  }
  if (t === 'matching') {
    const pairs = formPairs.value.filter(p => p.left.trim() || p.right.trim())
    const left = pairs.map(p => p.left || '?')
    const right = pairs.map(p => p.right || '?').sort(() => Math.random() - 0.5)
    return `<div style="display:flex;flex-direction:column;height:100%"><div style="flex:1;overflow-y:auto;padding:12px;background:#f5f5f5"><div style="margin:0 8px 12px 8px;background:white;border-radius:16px;padding:24px;box-shadow:0 4px 12px rgba(0,0,0,0.15)"><div style="display:flex;gap:20px;justify-content:center;align-items:center">${[0, 1, 2].map(i => `<div style="text-align:center"><div style="background:#e8f5e9;border-radius:12px;padding:14px 18px;margin-bottom:8px;font-weight:bold;font-size:16px">${left[i] || '?'}</div><div style="font-size:28px;color:#999;margin:4px 0">↔</div><div style="background:#e3f2fd;border-radius:12px;padding:14px 18px;font-weight:bold;font-size:16px">${right[i] || '?'}</div></div>`).join('')}</div></div></div>${bottomNav}</div>`
  }
  if (t === 'true-false') {
    const s = form.value.sentence || 'Frase...'
    const correct = form.value.is_correct_boolean ? '✅ Correcta' : '❌ Incorrecta'
    return `<div style="display:flex;flex-direction:column;height:100%"><div style="flex:1;overflow-y:auto;padding:12px;background:#f5f5f5"><div style="margin:0 8px 12px 8px;background:white;border-radius:16px;padding:28px;box-shadow:0 4px 12px rgba(0,0,0,0.15)"><p style="font-size:20px;text-align:center;font-weight:bold;margin:0 0 20px 0">${s}</p><p style="text-align:center;font-size:24px;margin:0">${correct}</p></div></div>${bottomNav}</div>`
  }
  return `<div style="display:flex;flex-direction:column;height:100%"><div style="flex:1;display:flex;align-items:center;justify-content:center;text-align:center;color:#999;font-size:14px">Seleccioná un tipo de ejercicio</div>${bottomNav}</div>`
})

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const r = await api.get(`/admin/blocks/${blockId}/items`)
    items.value = r.data
    const b = await api.get(`/admin/blocks/${blockId}`)
    blockTitle.value = b.data.title || 'Bloque'
    unitId.value = b.data.unit_id
  } catch (e) {
    error.value = e.response?.data?.detail || 'Error al cargar'
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

function openDialog(item = null) {
  editing.value = item
  v.clearAllErrors()
  if (item) {
    form.value = { ...item }
    answersList.value = item.answers || []
    optionsList.value = item.options || []
    formOptions.value = item.options || ['', '', '']
    formPairs.value = item.pairs || [{ left: '', right: '' }]
    wordsText.value = (item.words || []).join('\n')
    correctOrderText.value = (item.correct_order || []).join(' ')
  } else {
    form.value = {
      block_id: Number(blockId),
      item_type: 'fill-blank',
      sentence: '',
      answer: '',
      answers: null,
      hint: '',
      question: '',
      options: null,
      words: null,
      correct_order: null,
      input_mode: null,
      audio_url: '',
      pairs: null,
      is_correct_boolean: null,
      sort_order: 0
    }
    answersList.value = []
    optionsList.value = []
    formOptions.value = ['', '', '']
    formPairs.value = [{ left: '', right: '' }]
    wordsText.value = ''
    correctOrderText.value = ''
  }
  dialog.value = true
}

function validateForm() {
  return v.validateAll(form.value, validationRules.value)
}

async function save() {
  if (!validateForm()) return

  saving.value = true
  try {
    const p = { ...form.value, block_id: Number(blockId) }
    const a = answersList.value.filter(x => x.trim())
    p.answers = a.length > 0 ? a : null

    if (p.item_type === 'fill-blank') {
      const o = optionsList.value.filter(x => x.trim())
      p.options = o.length > 0 ? o : null
    }
    if (p.item_type === 'multiple-choice') p.options = formOptions.value.filter(o => o.trim())
    if (p.item_type === 'reorder') {
      p.words = wordsText.value.split('\n').map(w => w.trim()).filter(w => w)
      p.correct_order = correctOrderText.value.split(' ').filter(w => w)
    }
    if (p.item_type === 'matching') p.pairs = formPairs.value.filter(x => x.left.trim() || x.right.trim())

    if (editing.value) {
      await api.put(`/admin/items/${editing.value.id}`, p)
      snackbar.success('Ejercicio actualizado')
    } else {
      await api.post('/admin/items', p)
      snackbar.success('Ejercicio creado')
    }
    dialog.value = false
    await fetchData()
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al guardar')
  } finally {
    saving.value = false
  }
}

function confirmDelete(i) {
  toDelete.value = i
  deleteDialog.value = true
}

async function doDelete() {
  try {
    await api.delete(`/admin/items/${toDelete.value.id}`)
    snackbar.success('Ejercicio eliminado')
    deleteDialog.value = false
    await fetchData()
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al eliminar')
  }
}

async function moveItem(idx, dir) {
  const a = items.value[idx]
  const b = items.value[idx + dir]
  const tmp = a.sort_order
  a.sort_order = b.sort_order
  b.sort_order = tmp
  try {
    await api.put('/admin/items-reorder', {
      items: items.value.map(x => ({ id: x.id, sort_order: x.sort_order }))
    })
    await fetchData()
  } catch (e) {
    snackbar.error('Error al reordenar')
  }
}
</script>