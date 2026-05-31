<template>
  <div>
    <div class="d-flex align-center mb-6">
      <div>
        <v-btn variant="text" prepend-icon="mdi-arrow-left" to="/subjects" class="mb-2">Materias</v-btn>
        <h1 class="text-h4">Temas de {{ subjectName }}</h1>
      </div>
      <v-spacer />
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openDialog()">
        Nuevo Tema
      </v-btn>
    </div>

    <v-card rounded="lg" elevation="2">
      <v-table>
        <thead>
          <tr>
            <th></th>
            <th>ID</th>
            <th>Nombre</th>
            <th>Dificultad</th>
            <th>Activo</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in topics" :key="t.id">
            <td><span class="text-h5">{{ t.icon }}</span></td>
            <td class="font-weight-medium">{{ t.id }}</td>
            <td>{{ t.name }}</td>
            <td>
              <v-rating :model-value="t.difficulty" length="5" size="20" readonly density="compact" color="amber" />
            </td>
            <td>
              <v-chip :color="t.is_active ? 'green' : 'grey'" size="small" variant="tonal">
                {{ t.is_active ? 'Sí' : 'No' }}
              </v-chip>
            </td>
            <td>
              <v-btn icon="mdi-pencil" variant="text" size="small" color="primary" @click="openDialog(t)" />
              <v-btn icon="mdi-delete" variant="text" size="small" color="error" @click="confirmDelete(t)" />
            </td>
          </tr>
        </tbody>
      </v-table>
      <v-card-text v-if="topics.length === 0" class="text-center text-grey">
        No hay temas todavía. Creá uno.
      </v-card-text>
    </v-card>

    <!-- Dialog -->
    <v-dialog v-model="dialog" max-width="500">
      <v-card rounded="lg">
        <v-card-title>{{ editing ? 'Editar' : 'Nuevo' }} Tema</v-card-title>
        <v-card-text>
          <v-text-field v-model="form.id" label="ID (ej: verb-to-be)" :disabled="!!editing" variant="outlined" class="mb-2" />
          <v-text-field v-model="form.name" label="Nombre (ej: Verbo To Be)" variant="outlined" class="mb-2" />
          <v-text-field v-model="form.icon" label="Ícono (emoji)" variant="outlined" class="mb-2" />
          <v-text-field v-model.number="form.difficulty" label="Dificultad (1-5)" type="number" min="1" max="5" variant="outlined" class="mb-2" />
          <v-text-field v-model.number="form.sort_order" label="Orden" type="number" variant="outlined" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="dialog = false">Cancelar</v-btn>
          <v-btn color="primary" :loading="saving" @click="saveTopic">{{ editing ? 'Guardar' : 'Crear' }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Delete confirm -->
    <v-dialog v-model="deleteDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title>¿Desactivar tema?</v-card-title>
        <v-card-text>El tema "{{ toDelete?.name }}" se desactivará.</v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="deleteDialog = false">Cancelar</v-btn>
          <v-btn color="error" @click="doDelete">Desactivar</v-btn>
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
const subjectId = route.params.subjectId
const subjectName = ref('')
const topics = ref([])
const dialog = ref(false)
const deleteDialog = ref(false)
const editing = ref(null)
const saving = ref(false)
const toDelete = ref(null)

const form = ref({
  id: '',
  subject_id: subjectId,
  name: '',
  icon: '',
  difficulty: 1,
  sort_order: 0,
})

onMounted(async () => {
  await fetchTopics()
  const { data } = await api.get('/admin/subjects')
  const s = data.find(s => s.id === subjectId)
  if (s) subjectName.value = s.name
})

async function fetchTopics() {
  const { data } = await api.get(`/admin/subjects/${subjectId}/topics`)
  topics.value = data
}

function openDialog(topic = null) {
  editing.value = topic
  if (topic) {
    form.value = { ...topic, subject_id: subjectId }
  } else {
    form.value = { id: '', subject_id: subjectId, name: '', icon: '', difficulty: 1, sort_order: 0 }
  }
  dialog.value = true
}

async function saveTopic() {
  saving.value = true
  try {
    if (editing.value) {
      await api.put(`/admin/topics/${editing.value.id}`, form.value)
    } else {
      await api.post('/admin/topics', form.value)
    }
    dialog.value = false
    await fetchTopics()
  } catch (e) {
    alert(e.response?.data?.detail || 'Error')
  } finally {
    saving.value = false
  }
}

function confirmDelete(topic) {
  toDelete.value = topic
  deleteDialog.value = true
}

async function doDelete() {
  await api.delete(`/admin/topics/${toDelete.value.id}`)
  deleteDialog.value = false
  await fetchTopics()
}
</script>
