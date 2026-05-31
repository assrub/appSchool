<template>
  <div>
    <div class="d-flex align-center mb-6">
      <h1 class="text-h4">Materias</h1>
      <v-spacer />
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openDialog()">
        Nueva Materia
      </v-btn>
    </div>

    <v-card rounded="lg" elevation="2">
      <v-table>
        <thead>
          <tr>
            <th></th>
            <th>ID</th>
            <th>Nombre</th>
            <th>Color</th>
            <th>Activo</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="s in subjects" :key="s.id">
            <td><span class="text-h5">{{ s.icon }}</span></td>
            <td class="font-weight-medium">{{ s.id }}</td>
            <td>{{ s.name }}</td>
            <td>
              <v-chip :color="s.color" size="small" class="text-white">{{ s.color }}</v-chip>
            </td>
            <td>
              <v-chip :color="s.is_active ? 'green' : 'grey'" size="small" variant="tonal">
                {{ s.is_active ? 'Sí' : 'No' }}
              </v-chip>
            </td>
            <td>
              <v-btn icon="mdi-pencil" variant="text" size="small" color="primary" @click="openDialog(s)" />
              <v-btn icon="mdi-book-open-page-variant" variant="text" size="small" color="secondary" :to="`/subjects/${s.id}/topics`" />
              <v-btn icon="mdi-delete" variant="text" size="small" color="error" @click="confirmDelete(s)" />
            </td>
          </tr>
        </tbody>
      </v-table>
    </v-card>

    <!-- Dialog -->
    <v-dialog v-model="dialog" max-width="500">
      <v-card rounded="lg">
        <v-card-title>{{ editing ? 'Editar' : 'Nueva' }} Materia</v-card-title>
        <v-card-text>
          <v-text-field v-model="form.id" label="ID (ej: english, math)" :disabled="!!editing" variant="outlined" class="mb-2" />
          <v-text-field v-model="form.name" label="Nombre (ej: Inglés)" variant="outlined" class="mb-2" />
          <v-text-field v-model="form.icon" label="Ícono (emoji)" variant="outlined" class="mb-2" />
          <v-text-field v-model="form.color" label="Color HEX" variant="outlined" class="mb-2" />
          <v-text-field v-model.number="form.sort_order" label="Orden" type="number" variant="outlined" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="dialog = false">Cancelar</v-btn>
          <v-btn color="primary" :loading="saving" @click="saveSubject">{{ editing ? 'Guardar' : 'Crear' }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Delete confirm -->
    <v-dialog v-model="deleteDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title>¿Desactivar materia?</v-card-title>
        <v-card-text>La materia "{{ toDelete?.name }}" se desactivará. No se elimina permanentemente.</v-card-text>
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
import api from '../api/client'

const subjects = ref([])
const dialog = ref(false)
const deleteDialog = ref(false)
const editing = ref(null)
const saving = ref(false)
const toDelete = ref(null)

const form = ref({
  id: '',
  name: '',
  icon: '',
  color: '#4CAF50',
  sort_order: 0,
})

onMounted(fetchSubjects)

async function fetchSubjects() {
  const { data } = await api.get('/admin/subjects')
  subjects.value = data
}

function openDialog(subject = null) {
  editing.value = subject
  if (subject) {
    form.value = { ...subject }
  } else {
    form.value = { id: '', name: '', icon: '', color: '#4CAF50', sort_order: 0 }
  }
  dialog.value = true
}

async function saveSubject() {
  saving.value = true
  try {
    if (editing.value) {
      await api.put(`/admin/subjects/${editing.value.id}`, form.value)
    } else {
      await api.post('/admin/subjects', form.value)
    }
    dialog.value = false
    await fetchSubjects()
  } catch (e) {
    alert(e.response?.data?.detail || 'Error')
  } finally {
    saving.value = false
  }
}

function confirmDelete(subject) {
  toDelete.value = subject
  deleteDialog.value = true
}

async function doDelete() {
  await api.delete(`/admin/subjects/${toDelete.value.id}`)
  deleteDialog.value = false
  await fetchSubjects()
}
</script>
