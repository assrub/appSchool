<template>
  <div>
    <div class="d-flex align-center mb-6">
      <div>
        <v-btn variant="text" prepend-icon="mdi-arrow-left" to="/subjects" class="mb-2">Materias</v-btn>
        <h1 class="text-h4">Temas de {{ subjectName }}</h1>
      </div>
      <v-spacer />
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openDialog()">Nuevo Tema</v-btn>
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
                <th>Orden</th>
                <th></th>
                <th>ID</th>
                <th>Nombre</th>
                <th>Dificultad</th>
                <th>Activo</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(t, idx) in items" :key="t.id">
                <td>
                  <v-btn icon="mdi-chevron-up" variant="text" size="x-small" :disabled="idx === 0" @click="moveItem(idx, -1)" />
                  <v-btn icon="mdi-chevron-down" variant="text" size="x-small" :disabled="idx === items.length - 1" @click="moveItem(idx, 1)" />
                </td>
                <td><span class="text-h5">{{ t.icon }}</span></td>
                <td class="font-weight-medium">{{ t.id }}</td>
                <td>{{ t.name }}</td>
                <td>
                  <v-rating :model-value="t.difficulty" length="5" size="20" readonly density="compact" color="amber" />
                </td>
                <td>
                  <v-chip :color="t.is_active ? 'success' : 'grey'" size="small" variant="tonal">
                    {{ t.is_active ? 'Sí' : 'No' }}
                  </v-chip>
                </td>
                <td>
                  <v-tooltip text="Ver unidades" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn icon="mdi-dumbbell" v-bind="tp" variant="text" size="small" color="warning" :to="`/topics/${t.id}/units`" />
                    </template>
                  </v-tooltip>
                  <v-tooltip text="Editar teoría" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn icon="mdi-book-open-page-variant" v-bind="tp" variant="text" size="small" color="info" :to="{ path: `/theory/topic/${t.id}`, query: { subjectId } }" />
                    </template>
                  </v-tooltip>
                  <v-tooltip text="Editar tema" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn icon="mdi-pencil" v-bind="tp" variant="text" size="small" color="primary" @click="openDialog(t)" />
                    </template>
                  </v-tooltip>
                  <v-tooltip text="Desactivar" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn icon="mdi-delete" v-bind="tp" variant="text" size="small" color="error" @click="confirmDelete(t)" />
                    </template>
                  </v-tooltip>
                  <v-tooltip text="Eliminar permanentemente" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn icon="mdi-delete-forever" v-bind="tp" variant="text" size="small" color="deep-orange" @click="confirmHardDelete(t)" />
                    </template>
                  </v-tooltip>
                </td>
              </tr>
            </tbody>
          </v-table>
          <v-card-text v-if="!loading && items.length === 0" class="text-center text-grey pa-8">
            No hay temas. ¡Crea el primero!
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="3">
        <AppPreview />
      </v-col>
    </v-row>

    <v-dialog v-model="dialog" max-width="500">
      <v-card rounded="lg">
        <v-card-title>{{ editing ? 'Editar' : 'Nuevo' }} Tema</v-card-title>
        <v-card-text>
          <v-text-field v-model="form.id" label="ID" :disabled="!!editing" :error-messages="v.errors.id" class="mb-2" />
          <v-text-field v-model="form.name" label="Nombre" :error-messages="v.errors.name" class="mb-2" />
          <v-text-field v-model="form.icon" label="Ícono (emoji)" :error-messages="v.errors.icon" class="mb-2" />
          <v-slider v-model="form.difficulty" label="Dificultad" :min="1" :max="5" :step="1" thumb-label show-size>
            <template #append>
              <v-chip color="amber" size="small">{{ form.difficulty }}</v-chip>
            </template>
          </v-slider>
          <v-text-field v-model.number="form.sort_order" label="Orden" type="number" :error-messages="v.errors.sort_order" />
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
        <v-card-title>¿Desactivar?</v-card-title>
        <v-card-text>"{{ toDelete?.name }}" se desactivará.</v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="deleteDialog = false">Cancelar</v-btn>
          <v-btn color="error" @click="doDelete">Desactivar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="hardDeleteDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title class="text-error">
          <v-icon class="mr-2">mdi-alert</v-icon>
          Eliminar permanentemente
        </v-card-title>
        <v-card-text>
          ¿Estás seguro de eliminar "<b>{{ toHardDelete?.name }}</b>"?<br /><br />
          Se borrarán <b>TODOS</b> los datos: unidades, bloques, ejercicios y progreso.
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="hardDeleteDialog = false">Cancelar</v-btn>
          <v-btn color="deep-orange" @click="doHardDelete">Eliminar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, inject } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'
import AppPreview from '../components/AppPreview.vue'
import { useValidate } from '../composables/useValidation'

const route = useRoute()
const snackbar = inject('snackbar')
const subjectId = route.params.subjectId
const subjectName = ref('')
const items = ref([])
const loading = ref(true)
const error = ref('')
const dialog = ref(false)
const deleteDialog = ref(false)
const hardDeleteDialog = ref(false)
const editing = ref(null)
const saving = ref(false)
const toDelete = ref(null)
const toHardDelete = ref(null)

const form = ref({ id: '', subject_id: subjectId, name: '', icon: '', difficulty: 1, sort_order: 0 })

const v = useValidate({
  id: [],
  name: [],
  icon: [],
  difficulty: [],
  sort_order: []
})

const validationRules = {
  id: { required: true, minLength: 2, maxLength: 50, pattern: /^[a-zA-Z0-9_-]+$/, patternMessage: 'Solo letras, números, guiones' },
  name: { required: true, minLength: 2, maxLength: 100 },
  icon: { maxLength: 10 },
  difficulty: { required: true, numeric: true, min: 1, max: 5 },
  sort_order: { numeric: true, min: 0 }
}

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const [r, s] = await Promise.all([
      api.get(`/admin/subjects/${subjectId}/topics`),
      api.get('/admin/subjects')
    ])
    items.value = r.data
    const sub = s.data.find(x => x.id === subjectId)
    if (sub) subjectName.value = sub.name
  } catch (e) {
    error.value = e.response?.data?.detail || 'Error al cargar'
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

function openDialog(topic = null) {
  editing.value = topic
  v.clearAllErrors()
  form.value = topic ? { ...topic, subject_id: subjectId } : { id: '', subject_id: subjectId, name: '', icon: '', difficulty: 1, sort_order: 0 }
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
      await api.put(`/admin/topics/${editing.value.id}`, form.value)
      snackbar.success('Tema actualizado correctamente')
    } else {
      await api.post('/admin/topics', form.value)
      snackbar.success('Tema creado correctamente')
    }
    dialog.value = false
    await fetchData()
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al guardar')
  } finally {
    saving.value = false
  }
}

function confirmDelete(t) {
  toDelete.value = t
  deleteDialog.value = true
}

async function doDelete() {
  try {
    await api.delete(`/admin/topics/${toDelete.value.id}`)
    snackbar.success('Tema desactivado')
    deleteDialog.value = false
    await fetchData()
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al desactivar')
  }
}

function confirmHardDelete(t) {
  toHardDelete.value = t
  hardDeleteDialog.value = true
}

async function doHardDelete() {
  try {
    await api.delete(`/admin/topics/${toHardDelete.value.id}/hard`)
    snackbar.success('Tema eliminado permanentemente')
    hardDeleteDialog.value = false
    await fetchData()
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al eliminar')
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
    await api.put('/admin/topics-reorder', { items: list })
    await fetchData()
  } catch (e) {
    snackbar.error('Error al reordenar')
  }
}
</script>