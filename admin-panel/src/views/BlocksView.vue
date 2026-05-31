<template>
  <div>
    <div class="d-flex align-center mb-6">
      <div>
        <v-btn variant="text" prepend-icon="mdi-arrow-left" :to="`/topics/${topicId}/units`" class="mb-2">
          Unidades
        </v-btn>
        <h1 class="text-h4">Bloques de {{ unitTitle }}</h1>
      </div>
      <v-spacer />
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openDialog()">Nuevo Bloque</v-btn>
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
                <th>#</th>
                <th>Título</th>
                <th>🔀</th>
                <th>Ejercicios</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(b, idx) in items" :key="b.id">
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
                <td>{{ b.id }}</td>
                <td>{{ b.title }}</td>
                <td>
                  <v-chip v-if="b.shuffle" size="small" color="warning" variant="tonal">
                    Mezclado
                  </v-chip>
                </td>
                <td>
                  <v-btn
                    variant="text"
                    color="secondary"
                    size="small"
                    :to="`/blocks/${b.id}/items`"
                  >
                    Ver ejercicios
                  </v-btn>
                </td>
                <td>
                  <v-tooltip text="Editar" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn
                        icon="mdi-pencil"
                        v-bind="tp"
                        variant="text"
                        size="small"
                        color="primary"
                        @click="openDialog(b)"
                      />
                    </template>
                  </v-tooltip>
                  <v-tooltip text="Eliminar" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn
                        icon="mdi-delete"
                        v-bind="tp"
                        variant="text"
                        size="small"
                        color="error"
                        @click="confirmDelete(b)"
                      />
                    </template>
                  </v-tooltip>
                </td>
              </tr>
            </tbody>
          </v-table>
          <v-card-text v-if="!loading && items.length === 0" class="text-center text-grey pa-8">
            No hay bloques. ¡Crea el primero!
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="5" class="d-flex align-start">
        <AppPreview />
      </v-col>
    </v-row>

    <v-dialog v-model="dialog" max-width="500">
      <v-card rounded="lg">
        <v-card-title>{{ editing ? 'Editar' : 'Nuevo' }} Bloque</v-card-title>
        <v-card-text>
          <v-text-field
            v-model="form.title"
            label="Título"
            :error-messages="v.errors.title"
            class="mb-2"
          />
          <v-switch
            v-model="form.shuffle"
            label="Mezclar ejercicios (aleatorio)"
            color="warning"
            hide-details
          />
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
        <v-card-title>¿Eliminar bloque?</v-card-title>
        <v-card-text>
          Se eliminará "<b>{{ toDelete?.title }}</b>" con todos sus ejercicios.
        </v-card-text>
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
import { ref, watch, onMounted, inject } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'
import AppPreview from '../components/AppPreview.vue'
import { useValidate } from '../composables/useValidation'

const route = useRoute()
const snackbar = inject('snackbar')

const unitId = route.params.unitId
const topicId = ref('')
const unitTitle = ref('')
const items = ref([])
const loading = ref(true)
const error = ref('')
const dialog = ref(false)
const deleteDialog = ref(false)
const editing = ref(null)
const saving = ref(false)
const toDelete = ref(null)

const form = ref({ title: '', unit_id: unitId, shuffle: false, sort_order: 0 })

const v = useValidate({ title: [] })
const validationRules = {
  title: { required: true, minLength: 2, maxLength: 100 }
}

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const r = await api.get(`/admin/units/${unitId}/blocks`)
    items.value = r.data
    const u = await api.get(`/admin/units/${unitId}`)
    unitTitle.value = u.data.title || unitId
    topicId.value = u.data.topic_id
  } catch (e) {
    error.value = e.response?.data?.detail || 'Error al cargar'
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

function openDialog(block = null) {
  editing.value = block
  v.clearAllErrors()
  form.value = block
    ? { ...block, unit_id: unitId }
    : { title: '', unit_id: unitId, shuffle: false, sort_order: 0 }
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
      await api.put(`/admin/blocks/${editing.value.id}`, form.value)
      snackbar.success('Bloque actualizado')
    } else {
      await api.post('/admin/blocks', form.value)
      snackbar.success('Bloque creado')
    }
    dialog.value = false
    await fetchData()
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al guardar')
  } finally {
    saving.value = false
  }
}

function confirmDelete(b) {
  toDelete.value = b
  deleteDialog.value = true
}

async function doDelete() {
  try {
    await api.delete(`/admin/blocks/${toDelete.value.id}`)
    snackbar.success('Bloque eliminado')
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
    await api.put('/admin/blocks-reorder', {
      items: items.value.map(x => ({ id: x.id, sort_order: x.sort_order }))
    })
    await fetchData()
  } catch (e) {
    snackbar.error('Error al reordenar')
  }
}
</script>