<template>
  <div>
    <div class="d-flex align-center mb-6">
      <div>
        <v-btn variant="text" prepend-icon="mdi-arrow-left" :to="`/units/${unitId}/blocks`" class="mb-2">Bloques</v-btn>
        <h1 class="text-h4">Ejercicios del bloque</h1>
        <p class="text-grey">{{ blockTitle }}</p>
      </div>
      <v-spacer />
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openDialog()">
        Nuevo Ejercicio
      </v-btn>
    </div>

    <v-card rounded="lg" elevation="2">
      <v-table>
        <thead>
          <tr>
            <th>#</th>
            <th>Tipo</th>
            <th>Contenido</th>
            <th>Respuesta</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="i in items" :key="i.id">
            <td>{{ i.id }}</td>
            <td>
              <v-chip size="small" color="primary" variant="tonal">{{ i.item_type }}</v-chip>
            </td>
            <td class="text-truncate" style="max-width:300px">
              {{ i.sentence || i.question || (i.words && i.words.join(' ')) || '-' }}
            </td>
            <td>{{ i.answer || (i.correct_order && i.correct_order.join(' ')) || '-' }}</td>
            <td>
              <v-btn icon="mdi-pencil" variant="text" size="small" color="primary" @click="openDialog(i)" />
              <v-btn icon="mdi-delete" variant="text" size="small" color="error" @click="confirmDelete(i)" />
            </td>
          </tr>
        </tbody>
      </v-table>
      <v-card-text v-if="items.length === 0" class="text-center text-grey">
        No hay ejercicios. Creá uno.
      </v-card-text>
    </v-card>

    <!-- Dialog -->
    <v-dialog v-model="dialog" max-width="600">
      <v-card rounded="lg">
        <v-card-title>{{ editing ? 'Editar' : 'Nuevo' }} Ejercicio</v-card-title>
        <v-card-text>
          <v-select v-model="form.item_type" label="Tipo de ejercicio" :items="itemTypes" variant="outlined" class="mb-3" />

          <!-- fill-blank -->
          <template v-if="form.item_type === 'fill-blank'">
            <v-text-field v-model="form.sentence" label="Frase (usá ______ para el blank)" variant="outlined" class="mb-2" />
            <v-text-field v-model="form.answer" label="Respuesta correcta" variant="outlined" class="mb-2" />
            <v-text-field v-model="form.hint" label="Pista (opcional)" variant="outlined" />
          </template>

          <!-- multiple-choice -->
          <template v-else-if="form.item_type === 'multiple-choice'">
            <v-text-field v-model="form.question" label="Pregunta" variant="outlined" class="mb-2" />
            <div v-for="(opt, idx) in formOptions" :key="idx" class="d-flex align-center mb-2">
              <v-text-field v-model="formOptions[idx]" :label="`Opción ${idx + 1}`" variant="outlined" density="compact" hide-details class="flex-grow-1 mr-2" />
              <v-btn icon="mdi-delete" variant="text" size="small" color="error" @click="formOptions.splice(idx, 1)" />
            </div>
            <v-btn variant="outlined" size="small" prepend-icon="mdi-plus" @click="formOptions.push('')" class="mb-2">
              Agregar opción
            </v-btn>
            <v-text-field v-model="form.answer" label="Respuesta correcta (debe coincidir con una opción)" variant="outlined" />
          </template>

          <!-- reorder -->
          <template v-else-if="form.item_type === 'reorder'">
            <v-textarea v-model="wordsText" label="Palabras (una por línea)" variant="outlined" rows="4" class="mb-2" />
            <v-text-field v-model="correctOrderText" label="Orden correcto (separado por espacios)" variant="outlined" class="mb-2" />
            <v-text-field v-model="form.hint" label="Pista (opcional)" variant="outlined" />
          </template>

          <!-- listening -->
          <template v-else-if="form.item_type === 'listening'">
            <v-text-field v-model="form.sentence" label="Frase que escucha el nene" variant="outlined" class="mb-2" />
            <v-text-field v-model="form.audio_url" label="URL del audio" variant="outlined" class="mb-2" />
            <v-text-field v-model="form.answer" label="Respuesta esperada" variant="outlined" />
          </template>

          <!-- matching -->
          <template v-else-if="form.item_type === 'matching'">
            <div v-for="(p, idx) in formPairs" :key="idx" class="d-flex align-center mb-2">
              <v-text-field v-model="formPairs[idx].left" label="Izquierda" variant="outlined" density="compact" hide-details class="mr-2" />
              <v-icon>mdi-arrow-right</v-icon>
              <v-text-field v-model="formPairs[idx].right" label="Derecha" variant="outlined" density="compact" hide-details class="ml-2" />
              <v-btn icon="mdi-delete" variant="text" size="small" color="error" @click="formPairs.splice(idx, 1)" />
            </div>
            <v-btn variant="outlined" size="small" prepend-icon="mdi-plus" @click="formPairs.push({ left: '', right: '' })">
              Agregar par
            </v-btn>
          </template>

          <!-- true-false -->
          <template v-else-if="form.item_type === 'true-false'">
            <v-text-field v-model="form.sentence" label="Frase" variant="outlined" class="mb-2" />
            <v-switch v-model="form.is_correct_boolean" label="¿Es correcta la frase?" color="primary" class="mb-2" />
            <v-text-field v-if="!form.is_correct_boolean" v-model="form.answer" label="Corrección (frase correcta)" variant="outlined" />
          </template>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="dialog = false">Cancelar</v-btn>
          <v-btn color="primary" :loading="saving" @click="saveItem">{{ editing ? 'Guardar' : 'Crear' }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="deleteDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title>¿Eliminar ejercicio?</v-card-title>
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
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'

const route = useRoute()
const blockId = route.params.blockId
const unitId = ref('')
const blockTitle = ref('')
const items = ref([])
const dialog = ref(false)
const deleteDialog = ref(false)
const editing = ref(null)
const saving = ref(false)
const toDelete = ref(null)

const itemTypes = ['fill-blank', 'multiple-choice', 'reorder', 'listening', 'matching', 'true-false']

const form = ref({
  block_id: Number(blockId),
  item_type: 'fill-blank',
  sentence: '', answer: '', hint: '',
  question: '',
  options: null, words: null, correct_order: null,
  audio_url: '',
  pairs: null,
  is_correct_boolean: null,
  sort_order: 0,
})

const formOptions = ref(['', '', ''])
const formPairs = ref([{ left: '', right: '' }, { left: '', right: '' }])
const wordsText = ref('')
const correctOrderText = ref('')

watch(() => form.value.item_type, () => {
  formOptions.value = ['', '', '']
  formPairs.value = [{ left: '', right: '' }, { left: '', right: '' }]
  wordsText.value = ''
  correctOrderText.value = ''
})

onMounted(async () => {
  const { data } = await api.get(`/admin/blocks/${blockId}/items`)
  items.value = data
  try {
    const b = await api.get(`/admin/blocks/${blockId}`)
    blockTitle.value = b.data.title || `Bloque #${blockId}`
    unitId.value = b.data.unit_id
  } catch { blockTitle.value = `Bloque #${blockId}` }
})

function openDialog(item = null) {
  editing.value = item
  if (item) {
    form.value = {
      block_id: Number(blockId),
      item_type: item.item_type,
      sentence: item.sentence || '', answer: item.answer || '',
      hint: item.hint || '', question: item.question || '',
      options: item.options, words: item.words, correct_order: item.correct_order,
      audio_url: item.audio_url || '',
      pairs: item.pairs, is_correct_boolean: item.is_correct_boolean,
      sort_order: item.sort_order || 0,
    }
    formOptions.value = item.options || ['', '', '']
    formPairs.value = item.pairs || [{ left: '', right: '' }]
    wordsText.value = (item.words || []).join('\n')
    correctOrderText.value = (item.correct_order || []).join(' ')
  } else {
    form.value = {
      block_id: Number(blockId), item_type: 'fill-blank',
      sentence: '', answer: '', hint: '', question: '',
      options: null, words: null, correct_order: null,
      audio_url: '', pairs: null, is_correct_boolean: null, sort_order: 0,
    }
    formOptions.value = ['', '', '']
    formPairs.value = [{ left: '', right: '' }]
    wordsText.value = ''
    correctOrderText.value = ''
  }
  dialog.value = true
}

async function saveItem() {
  saving.value = true
  try {
    const payload = { ...form.value, block_id: Number(blockId) }

    if (payload.item_type === 'multiple-choice') {
      payload.options = formOptions.value.filter(o => o.trim())
    }
    if (payload.item_type === 'reorder') {
      payload.words = wordsText.value.split('\n').map(w => w.trim()).filter(w => w)
      payload.correct_order = correctOrderText.value.split(' ').filter(w => w)
    }
    if (payload.item_type === 'matching') {
      payload.pairs = formPairs.value.filter(p => p.left.trim() || p.right.trim())
    }

    if (editing.value) await api.put(`/admin/items/${editing.value.id}`, payload)
    else await api.post('/admin/items', payload)

    dialog.value = false
    const { data } = await api.get(`/admin/blocks/${blockId}/items`)
    items.value = data
  } catch (e) { alert(e.response?.data?.detail || 'Error') }
  finally { saving.value = false }
}

function confirmDelete(item) { toDelete.value = item; deleteDialog.value = true }

async function doDelete() {
  await api.delete(`/admin/items/${toDelete.value.id}`)
  deleteDialog.value = false
  const { data } = await api.get(`/admin/blocks/${blockId}/items`)
  items.value = data
}
</script>
