<template>
  <div>
    <div class="d-flex align-center mb-6">
      <div>
        <v-btn variant="text" prepend-icon="mdi-arrow-left" :to="`/subjects/${topicId}/topics`" class="mb-2">
          Temas
        </v-btn>
        <h1 class="text-h4">Unidades de {{ topicName }}</h1>
      </div>
      <v-spacer />
      <v-btn color="warning" variant="tonal" size="small" class="mr-2" @click="topicAction('lock-all')">
        🔒 Bloquear todo
      </v-btn>
      <v-btn color="success" variant="tonal" size="small" class="mr-2" @click="topicAction('unlock-all')">
        🔓 Desbloquear todo
      </v-btn>
      <v-btn color="orange" variant="tonal" size="small" class="mr-2" @click="topicAction('reset-all')">
        🔄 Resetear todo
      </v-btn>
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openDialog()">
        Nueva Unidad
      </v-btn>
    </div>

    <v-card rounded="lg" elevation="2">
      <v-table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Título</th>
            <th>Tipo</th>
            <th>Input</th>
            <th>Estado</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in units" :key="u.id">
            <td class="font-weight-medium">{{ u.id }}</td>
            <td>{{ u.title }}</td>
            <td>
              <v-chip size="small" color="primary" variant="tonal">{{ u.exercise_type }}</v-chip>
            </td>
            <td>
              <v-chip :color="u.input_mode === 'tap' ? 'green' : 'orange'" size="small" variant="tonal">
                {{ u.input_mode === 'tap' ? 'Tocar' : 'Escribir' }}
              </v-chip>
            </td>
            <td>
              <v-chip :color="u.is_locked ? 'grey' : 'green'" size="small" variant="tonal">
                {{ u.is_locked ? '🔒 Bloqueado' : '🔓 Abierto' }}
              </v-chip>
            </td>
            <td>
              <v-tooltip text="Editar unidad" location="top"><template #activator="{ props: tp }"><v-btn icon="mdi-pencil" v-bind="tp" variant="text" size="small" color="primary" @click="openDialog(u)" /></template></v-tooltip>
              <v-tooltip text="Ver bloques de ejercicios" location="top"><template #activator="{ props: tp }"><v-btn icon="mdi-list-box-outline" v-bind="tp" variant="text" size="small" color="secondary" :to="`/units/${u.id}/blocks`" /></template></v-tooltip>
              <v-tooltip text="Editar teoría de esta unidad" location="top"><template #activator="{ props: tp }"><v-btn icon="mdi-book-open-page-variant" v-bind="tp" variant="text" size="small" color="info" :to="`/theory/unit/${u.id}`" /></template></v-tooltip>
              <v-tooltip :text="u.is_locked?'Desbloquear':'Bloquear'" location="top"><template #activator="{ props: tp }"><v-btn v-if="!u.is_locked" icon="mdi-lock" v-bind="tp" variant="text" size="small" color="warning" @click="toggleLock(u, true)" /><v-btn v-else icon="mdi-lock-open" v-bind="tp" variant="text" size="small" color="success" @click="toggleLock(u, false)" /></template></v-tooltip>
              <v-tooltip text="Resetear progreso (el nene empieza de cero)" location="top"><template #activator="{ props: tp }"><v-btn icon="mdi-restart" v-bind="tp" variant="text" size="small" color="orange" @click="confirmReset(u)" /></template></v-tooltip>
              <v-tooltip text="Eliminar unidad" location="top"><template #activator="{ props: tp }"><v-btn icon="mdi-delete" v-bind="tp" variant="text" size="small" color="error" @click="confirmDelete(u)" /></template></v-tooltip>
            </td>
          </tr>
        </tbody>
      </v-table>
      <v-card-text v-if="units.length === 0" class="text-center text-grey">
        No hay unidades. Creá una.
      </v-card-text>
    </v-card>

    <!-- Dialog -->
    <v-dialog v-model="dialog" max-width="500">
      <v-card rounded="lg">
        <v-card-title>{{ editing ? 'Editar' : 'Nueva' }} Unidad</v-card-title>
        <v-card-text>
          <v-text-field v-model="form.id" label="ID (ej: affirmative)" :disabled="!!editing" variant="outlined" class="mb-2" />
          <v-text-field v-model="form.title" label="Título (ej: Afirmativo)" variant="outlined" class="mb-2" />
          <v-select v-model="form.exercise_type" label="Tipo de ejercicio" :items="exerciseTypes" variant="outlined" class="mb-2" />
          <v-select v-model="form.input_mode" label="Modo de respuesta" :items="['tap','type']" variant="outlined" class="mb-2">
            <template #item="{ item, props }">
              <v-list-item v-bind="props" :title="item.title" :subtitle="item.title === 'tap' ? 'Tocar opciones' : 'Escribir respuesta'" />
            </template>
          </v-select>
          <v-textarea v-model="form.explanation" label="Explicación" variant="outlined" rows="2" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="dialog = false">Cancelar</v-btn>
          <v-btn color="primary" :loading="saving" @click="saveUnit">{{ editing ? 'Guardar' : 'Crear' }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Delete confirm -->
    <v-dialog v-model="deleteDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title>¿Eliminar unidad?</v-card-title>
        <v-card-text>La unidad "{{ toDelete?.title }}" se eliminará permanentemente junto con sus bloques y ejercicios.</v-card-text>
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
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'

const route = useRoute()
const topicId = route.params.topicId
const topicName = ref('')
const units = ref([])
const dialog = ref(false)
const deleteDialog = ref(false)
const editing = ref(null)
const saving = ref(false)
const toDelete = ref(null)

const exerciseTypes = [
  'fill-blank',
  'multiple-choice',
  'reorder',
  'listening',
  'matching',
  'true-false',
]

const form = ref({ id: '', title: '', topic_id: topicId, exercise_type: 'fill-blank', input_mode: 'tap', explanation: '' })

onMounted(async () => {
  const [u, t] = await Promise.all([
    api.get(`/admin/topics/${topicId}/units`),
    api.get(`/admin/topics/${topicId}`).catch(() => ({ data: {} })),
  ])
  units.value = u.data
  try { topicName.value = t.data.name || topicId } catch { topicName.value = topicId }
})

function openDialog(unit = null) {
  editing.value = unit
  form.value = unit ? { ...unit, topic_id: topicId } : { id: '', title: '', topic_id: topicId, exercise_type: 'fill-blank', input_mode: 'tap', explanation: '' }
  dialog.value = true
}

async function saveUnit() {
  saving.value = true
  try {
    if (editing.value) await api.put(`/admin/units/${editing.value.id}`, form.value)
    else await api.post('/admin/units', form.value)
    dialog.value = false
    const { data } = await api.get(`/admin/topics/${topicId}/units`)
    units.value = data
  } catch (e) { alert(e.response?.data?.detail || 'Error') }
  finally { saving.value = false }
}

function confirmDelete(unit) { toDelete.value = unit; deleteDialog.value = true }

async function toggleLock(unit, lock) {
  try {
    const endpoint = lock ? `/admin/units/${unit.id}/lock` : `/admin/units/${unit.id}/unlock`
    await api.put(endpoint)
    const { data } = await api.get(`/admin/topics/${topicId}/units`)
    units.value = data
  } catch (e) { alert(e.response?.data?.detail || 'Error') }
}

async function confirmReset(unit) {
  if (!confirm(`¿Resetear progreso de "${unit.title}"? El nene la hará de cero.`)) return
  await api.delete(`/admin/progress/${deviceId}/${topicId}/${unit.id}`)
  alert('Progreso reseteado')
}

const deviceId = 'android-default'

async function topicAction(action) {
  const labels = { 'lock-all': 'Bloquear', 'unlock-all': 'Desbloquear', 'reset-all': 'Resetear' }
  if (!confirm(`¿${labels[action]} todas las unidades?`)) return
  await api.post(`/admin/topics/${topicId}/${action}`)
  const { data } = await api.get(`/admin/topics/${topicId}/units`)
  units.value = data
  alert('Acción completada')
}

async function doDelete() {
  await api.delete(`/admin/units/${toDelete.value.id}`)
  deleteDialog.value = false
  const { data } = await api.get(`/admin/topics/${topicId}/units`)
  units.value = data
}
</script>
