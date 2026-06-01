<template>
  <div>
    <div class="d-flex align-center mb-6">
      <h1 class="text-h4">Materias</h1>
      <v-spacer />
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openDialog()">Nueva Materia</v-btn>
      <v-btn variant="tonal" prepend-icon="mdi-export" class="ml-2" @click="showExportDialog = true">Exportar</v-btn>
      <v-btn variant="tonal" prepend-icon="mdi-import" class="ml-2" @click="triggerImport">Importar</v-btn>
      <input ref="fileInput" type="file" accept=".json" style="display:none" @change="handleImport" />
    </div>

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4" closable @click:close="error = ''">
      {{ error }}
      <v-btn size="small" class="ml-2" @click="fetchData">Reintentar</v-btn>
    </v-alert>

    <v-row>
      <v-col cols="12" md="9">
        <v-card rounded="lg" elevation="2">
          <div class="d-flex align-center pa-2 border-b">
            <span class="text-overline">Materias</span>
          </div>
          <v-progress-linear v-if="loading" indeterminate color="primary" />
          <v-table v-else>
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
              <tr v-for="s in items" :key="s.id">
                <td><span class="text-h5">{{ s.icon }}</span></td>
                <td class="font-weight-medium">{{ s.id }}</td>
                <td>{{ s.name }}</td>
                <td>
                  <v-chip :color="s.color" size="small" class="text-white">
                    {{ s.color }}
                  </v-chip>
                </td>
                <td>
                  <v-chip :color="s.is_active?'success':'grey'" size="small" variant="tonal">
                    {{ s.is_active?'Sí':'No' }}
                  </v-chip>
                </td>
                <td>
                  <v-tooltip text="Editar materia" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn icon="mdi-pencil" v-bind="tp" variant="text" size="small" color="primary" @click="openDialog(s)" />
                    </template>
                  </v-tooltip>
                  <v-tooltip text="Ver temas" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn icon="mdi-book-open-page-variant" v-bind="tp" variant="text" size="small" color="secondary" :to="`/subjects/${s.id}/topics`" />
                    </template>
                  </v-tooltip>
                  <v-tooltip text="Asignar usuarios" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn icon="mdi-account-group" v-bind="tp" variant="text" size="small" color="info" @click="openUserDialog(s)" />
                    </template>
                  </v-tooltip>
                  <v-tooltip text="Desactivar materia" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn icon="mdi-delete" v-bind="tp" variant="text" size="small" color="error" @click="confirmDelete(s)" />
                    </template>
                  </v-tooltip>
                  <v-tooltip text="Eliminar permanentemente" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn icon="mdi-delete-forever" v-bind="tp" variant="text" size="small" color="deep-orange" @click="confirmHardDelete(s)" />
                    </template>
                  </v-tooltip>
                </td>
              </tr>
            </tbody>
          </v-table>
        </v-card>
      </v-col>
      <v-col cols="12" md="3">
        <MobilePreview :html="listPreviewHtml" />
      </v-col>
    </v-row>

    <v-dialog v-model="dialog" max-width="500">
      <v-card rounded="lg">
        <v-card-title>{{ editing ? 'Editar' : 'Nueva' }} Materia</v-card-title>
        <v-card-text>
          <v-text-field
            v-model="form.id"
            label="ID"
            :disabled="!!editing"
            :error-messages="v.errors.id"
            class="mb-2"
          />
          <v-text-field
            v-model="form.name"
            label="Nombre"
            :error-messages="v.errors.name"
            class="mb-2"
          />
          <v-text-field
            v-model="form.icon"
            label="Ícono (emoji)"
            :error-messages="v.errors.icon"
            class="mb-2"
          />
          <v-text-field
            v-model="form.color"
            label="Color HEX"
            :error-messages="v.errors.color"
            class="mb-2"
          />
          <v-text-field
            v-model.number="form.sort_order"
            label="Orden"
            type="number"
            :error-messages="v.errors.sort_order"
          />
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
          ¿Estás seguro de eliminar "<b>{{ toHardDelete?.name }}</b>"?<br />
          Se borrarán TODOS los temas, unidades, ejercicios y progreso.
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="hardDeleteDialog = false">Cancelar</v-btn>
          <v-btn color="deep-orange" @click="doHardDelete">Eliminar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="userDialog" max-width="500">
      <v-card rounded="lg">
        <v-card-title>Asignar usuarios a "{{ selectedSubject?.name }}"</v-card-title>
        <v-card-text>
          <v-text-field v-model="userSearch" prepend-inner-icon="mdi-magnify" label="Buscar usuarios" class="mb-4" hide-details />
          <div v-for="u in filteredUsers" :key="u.id" class="mb-2">
            <v-checkbox v-model="selectedUsers" :value="u.id" :label="`${u.display_name} (${u.username})`" hide-details />
          </div>
          <div v-if="filteredUsers.length === 0" class="text-center text-grey pa-4">
            No se encontraron usuarios
          </div>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="userDialog = false">Cancelar</v-btn>
          <v-btn color="primary" :loading="savingUsers" @click="saveUserAssignment">Guardar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="showExportDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title>Exportar materia</v-card-title>
        <v-card-text>
          <v-select
            v-model="exportId"
            :items="items"
            item-title="name"
            item-value="id"
            label="Seleccionar materia"
            prepend-icon="mdi-bookshelf"
          />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="showExportDialog = false">Cancelar</v-btn>
          <v-btn color="primary" :loading="exporting" @click="doExport">Exportar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, computed, inject, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'
import MobilePreview from '../components/MobilePreview.vue'
import { useValidate } from '../composables/useValidation'

const route = useRoute()
const snackbar = inject('snackbar')

const items = ref([])
const loading = ref(true)
const error = ref('')
const dialog = ref(false)
const deleteDialog = ref(false)
const hardDeleteDialog = ref(false)
const userDialog = ref(false)
const editing = ref(null)
const saving = ref(false)
const savingUsers = ref(false)
const toDelete = ref(null)
const toHardDelete = ref(null)
const selectedSubject = ref(null)
const selectedUsers = ref([])
const allUsers = ref([])
const userSearch = ref('')
const showExportDialog = ref(false)
const exportId = ref(null)
const exporting = ref(false)
const fileInput = ref(null)

const form = ref({ id: '', name: '', icon: '', color: '#4CAF50', sort_order: 0 })

const v = useValidate({
  id: [],
  name: [],
  icon: [],
  color: [],
  sort_order: []
})

const validationRules = {
  id: { required: true, minLength: 2, maxLength: 50, pattern: /^[a-zA-Z0-9_-]+$/, patternMessage: 'Solo letras, números, guiones y guiones bajos' },
  name: { required: true, minLength: 2, maxLength: 100 },
  icon: { maxLength: 10 },
  color: { color: true },
  sort_order: { numeric: true, min: 0 }
}

const filteredUsers = computed(() => {
  if (!userSearch.value) return allUsers.value
  const search = userSearch.value.toLowerCase()
  return allUsers.value.filter(u =>
    u.display_name?.toLowerCase().includes(search) ||
    u.username?.toLowerCase().includes(search)
  )
})

const listPreviewHtml = computed(() => {
  const header = '<div style="background:#4CAF50;color:white;padding:12px 16px;font-weight:bold;font-size:14px;flex-shrink:0;display:flex;align-items:center"><div style="flex:1">AppEnglish</div></div>'
  const bottomNav = '<div style="display:flex;justify-content:space-around;background:white;border-top:1px solid #e0e0e0;padding:8px 0 6px 0;flex-shrink:0"><div style="text-align:center;flex:1"><div style="font-size:18px">🏠</div><div style="font-size:10px;color:#4CAF50;margin-top:2px">Inicio</div></div><div style="text-align:center;flex:1"><div style="font-size:18px">📖</div><div style="font-size:10px;color:#757575;margin-top:2px">Diccionario</div></div><div style="text-align:center;flex:1"><div style="font-size:18px">📊</div><div style="font-size:10px;color:#757575;margin-top:2px">Progreso</div></div><div style="text-align:center;flex:1"><div style="font-size:18px">⚙️</div><div style="font-size:10px;color:#757575;margin-top:2px">Ajustes</div></div></div>'

  if (!items.value.length) {
    return `<div style="display:flex;flex-direction:column;height:100%">${header}<div style="flex:1;display:flex;align-items:center;justify-content:center;background:#f5f5f5;color:#757575;font-size:14px;padding:20px">No hay materias</div>${bottomNav}</div>`
  }

  let cards = '<div style="padding:16px 16px 8px 16px"><div style="font-size:22px;font-weight:bold;color:#4CAF50;line-height:1.2">MATERIAS</div></div>'
  cards += items.value.map(s => `
    <div style="margin:0 12px 16px 12px;background:white;border-radius:16px;box-shadow:0 1px 2px rgba(0,0,0,0.3), 0 1px 3px 1px rgba(0,0,0,0.15)">
      <div style="display:flex;align-items:center;padding:20px">
        <div style="font-size:28px;margin-right:16px;flex-shrink:0">${s.icon||''}</div>
        <div style="flex:1;min-width:0">
          <div style="font-size:22px;font-weight:bold;color:#212121;line-height:1.2">${s.name}</div>
          <div style="font-size:14px;color:#757575;margin-top:4px">${s.topicsCount||0} temas</div>
        </div>
        <div style="font-size:24px;color:#4CAF50;flex-shrink:0">→</div>
      </div>
    </div>
  `).join('')

  return `<div style="display:flex;flex-direction:column;height:100%">${header}<div style="flex:1;overflow-y:auto;background:#f5f5f5">${cards}</div>${bottomNav}</div>`
})

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const r = await api.get('/admin/subjects')
    items.value = r.data
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
  form.value = item ? { ...item } : { id: '', name: '', icon: '', color: '#4CAF50', sort_order: 0 }
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
      await api.put(`/admin/subjects/${editing.value.id}`, form.value)
      snackbar.success('Materia actualizada correctamente')
    } else {
      await api.post('/admin/subjects', form.value)
      snackbar.success('Materia creada correctamente')
    }
    dialog.value = false
    await fetchData()
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al guardar')
  } finally {
    saving.value = false
  }
}

function confirmDelete(s) {
  toDelete.value = s
  deleteDialog.value = true
}

async function doDelete() {
  try {
    await api.delete(`/admin/subjects/${toDelete.value.id}`)
    snackbar.success('Materia desactivada')
    deleteDialog.value = false
    await fetchData()
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al desactivar')
  }
}

function confirmHardDelete(s) {
  toHardDelete.value = s
  hardDeleteDialog.value = true
}

async function doHardDelete() {
  try {
    await api.delete(`/admin/subjects/${toHardDelete.value.id}/hard`)
    snackbar.success('Materia eliminada permanentemente')
    hardDeleteDialog.value = false
    await fetchData()
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al eliminar')
  }
}

async function openUserDialog(s) {
  selectedSubject.value = s
  userSearch.value = ''
  try {
    const { data } = await api.get('/admin/users')
    allUsers.value = data
  } catch {
    allUsers.value = []
  }
  try {
    const { data } = await api.get(`/admin/subjects/${s.id}/users`)
    selectedUsers.value = data || []
  } catch {
    selectedUsers.value = []
  }
  userDialog.value = true
}

async function saveUserAssignment() {
  savingUsers.value = true
  try {
    await api.put(`/admin/subjects/${selectedSubject.value.id}/users`, { user_ids: selectedUsers.value })
    snackbar.success('Usuarios asignados correctamente')
    userDialog.value = false
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al asignar')
  } finally {
    savingUsers.value = false
  }
}

function triggerImport() {
  fileInput.value?.click()
}

async function handleImport(e) {
  const f = e.target.files[0]
  if (!f) return

  try {
    const text = await f.text()
    const data = JSON.parse(text)
    await api.post('/admin/import', data)
    snackbar.success('Materia importada correctamente')
    await fetchData()
  } catch (err) {
    snackbar.error(err.response?.data?.detail || 'Error al importar')
  }

  e.target.value = ''
}

async function doExport() {
  if (!exportId.value) return

  exporting.value = true
  try {
    const { data } = await api.get(`/admin/export/${exportId.value}`)
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${exportId.value}.json`
    a.click()
    URL.revokeObjectURL(url)
    snackbar.success('Exportación iniciada')
    showExportDialog.value = false
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al exportar')
  } finally {
    exporting.value = false
  }
}
</script>