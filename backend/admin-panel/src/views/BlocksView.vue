<template>
  <div>
    <div class="d-flex align-center mb-6">
      <div>
        <v-btn variant="text" prepend-icon="mdi-arrow-left" :to="`/subjects/${subjectId}/topics`" class="mb-2">Temas</v-btn>
        <h1 class="text-h4">Bloques de {{ unitTitle }}</h1>
      </div>
      <v-spacer />
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openDialog()">
        Nuevo Bloque
      </v-btn>
    </div>

    <v-card rounded="lg" elevation="2">
      <v-table>
        <thead>
          <tr>
            <th>#</th>
            <th>Título</th>
            <th>Ejercicios</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="b in blocks" :key="b.id">
            <td>{{ b.id }}</td>
            <td>{{ b.title }}</td>
            <td>
              <v-btn variant="text" color="secondary" size="small" :to="`/blocks/${b.id}/items`">
                Ver ejercicios
              </v-btn>
            </td>
            <td>
              <v-btn icon="mdi-pencil" variant="text" size="small" color="primary" @click="openDialog(b)" />
              <v-btn icon="mdi-delete" variant="text" size="small" color="error" @click="confirmDelete(b)" />
            </td>
          </tr>
        </tbody>
      </v-table>
      <v-card-text v-if="blocks.length === 0" class="text-center text-grey">
        No hay bloques. Creá uno.
      </v-card-text>
    </v-card>

    <v-dialog v-model="dialog" max-width="500">
      <v-card rounded="lg">
        <v-card-title>{{ editing ? 'Editar' : 'Nuevo' }} Bloque</v-card-title>
        <v-card-text>
          <v-text-field v-model="form.title" label="Título (ej: PASO 1: Pronombres)" variant="outlined" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="dialog = false">Cancelar</v-btn>
          <v-btn color="primary" :loading="saving" @click="saveBlock">{{ editing ? 'Guardar' : 'Crear' }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="deleteDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title>¿Eliminar bloque?</v-card-title>
        <v-card-text>El bloque se eliminará junto con sus ejercicios.</v-card-text>
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
const unitId = route.params.unitId
const subjectId = ref('')
const unitTitle = ref('')
const blocks = ref([])
const dialog = ref(false)
const deleteDialog = ref(false)
const editing = ref(null)
const saving = ref(false)
const toDelete = ref(null)
const form = ref({ title: '', unit_id: unitId, sort_order: 0 })

onMounted(async () => {
  const { data } = await api.get(`/admin/units/${unitId}/blocks`)
  blocks.value = data
  try {
    const u = await api.get(`/admin/units/${unitId}`)
    unitTitle.value = u.data.title || unitId
    const t = await api.get(`/admin/topics/${u.data.topic_id}`).catch(() => ({ data: {} }))
    subjectId.value = u.data.topic_id
  } catch { unitTitle.value = unitId }
})

function openDialog(block = null) {
  editing.value = block
  form.value = block ? { ...block, unit_id: unitId } : { title: '', unit_id: unitId, sort_order: 0 }
  dialog.value = true
}

async function saveBlock() {
  saving.value = true
  try {
    if (editing.value) await api.put(`/admin/blocks/${editing.value.id}`, form.value)
    else await api.post('/admin/blocks', form.value)
    dialog.value = false
    const { data } = await api.get(`/admin/units/${unitId}/blocks`)
    blocks.value = data
  } catch (e) { alert(e.response?.data?.detail || 'Error') }
  finally { saving.value = false }
}

function confirmDelete(block) { toDelete.value = block; deleteDialog.value = true }

async function doDelete() {
  await api.delete(`/admin/blocks/${toDelete.value.id}`)
  deleteDialog.value = false
  const { data } = await api.get(`/admin/units/${unitId}/blocks`)
  blocks.value = data
}
</script>
