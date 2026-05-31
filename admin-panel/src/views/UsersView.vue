<template>
  <div>
    <div class="d-flex align-center mb-6">
      <h1 class="text-h4">Usuarios (Hijos)</h1>
      <v-spacer />
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openDialog()">Nuevo Usuario</v-btn>
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
            <th>ID</th>
            <th>Usuario</th>
            <th>Nombre</th>
            <th>Estado</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in items" :key="u.id">
            <td class="text-caption text-grey">{{ u.id }}</td>
            <td class="font-weight-medium">{{ u.username }}</td>
            <td>{{ u.display_name }}</td>
            <td>
              <v-chip
                :color="u.is_active ? 'success' : 'grey'"
                size="small"
                variant="tonal"
              >
                {{ u.is_active ? 'Activo' : 'Inactivo' }}
              </v-chip>
            </td>
            <td>
              <v-btn
                icon="mdi-pencil"
                variant="text"
                size="small"
                color="primary"
                @click="openDialog(u)"
              />
              <v-btn
                icon="mdi-delete"
                variant="text"
                size="small"
                color="error"
                @click="confirmDelete(u)"
              />
            </td>
          </tr>
        </tbody>
      </v-table>

      <v-card-text v-if="!loading && items.length === 0" class="text-center text-grey pa-8">
        <v-icon size="48" class="mb-2">mdi-account-off</v-icon>
        <p>No hay usuarios registrados</p>
        <v-btn color="primary" variant="tonal" class="mt-2" @click="openDialog()">
          <v-icon start>mdi-plus</v-icon>
          Crear primer usuario
        </v-btn>
      </v-card-text>
    </v-card>

    <v-dialog v-model="dialog" max-width="500">
      <v-card rounded="lg">
        <v-card-title>{{ editing ? 'Editar' : 'Nuevo' }} Usuario</v-card-title>
        <v-card-text>
          <v-text-field
            v-model="form.username"
            label="Usuario"
            :disabled="!!editing"
            :error-messages="v.errors.username"
            variant="outlined"
            class="mb-3"
          />
          <v-text-field
            v-model="form.display_name"
            label="Nombre (para mostrar)"
            :error-messages="v.errors.display_name"
            variant="outlined"
            class="mb-3"
          />
          <v-text-field
            v-model="form.password"
            label="Contraseña"
            type="password"
            variant="outlined"
            :hint="editing ? 'Dejar vacío para no cambiar' : ''"
            persistent-hint
            :error-messages="v.errors.password"
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
        <v-card-title class="text-error">
          <v-icon class="mr-2">mdi-alert</v-icon>
          Eliminar usuario
        </v-card-title>
        <v-card-text>
          ¿Eliminar al usuario "<b>{{ toDelete?.username }}</b>"?<br />
          Esta acción no se puede deshacer.
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
import { ref, onMounted, inject } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'
import { useValidate } from '../composables/useValidation'

const route = useRoute()
const snackbar = inject('snackbar')

const items = ref([])
const loading = ref(true)
const error = ref('')
const dialog = ref(false)
const deleteDialog = ref(false)
const editing = ref(null)
const saving = ref(false)
const toDelete = ref(null)

const form = ref({ username: '', display_name: '', password: '' })

const v = useValidate({
  username: [],
  display_name: [],
  password: []
})

const validationRules = {
  username: { required: true, minLength: 3, maxLength: 50 },
  display_name: { required: true, minLength: 2, maxLength: 100 },
  password: editing => editing ? {} : { required: true, minLength: 4 }
}

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const r = await api.get('/admin/users')
    items.value = r.data || []
  } catch (e) {
    error.value = e.response?.data?.detail || 'Error al cargar'
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

function openDialog(user = null) {
  editing.value = user
  v.clearAllErrors()
  form.value = user
    ? { ...user, password: '' }
    : { username: '', display_name: '', password: '' }
  dialog.value = true
}

function validateForm() {
  const rules = {
    username: { required: true, minLength: 3, maxLength: 50 },
    display_name: { required: true, minLength: 2, maxLength: 100 }
  }
  if (!editing.value) {
    rules.password = { required: true, minLength: 4 }
  }
  return v.validateAll(form.value, rules)
}

async function save() {
  if (!validateForm()) return

  saving.value = true
  try {
    const payload = { ...form.value }
    if (!payload.password) delete payload.password

    if (editing.value) {
      await api.put(`/admin/users/${editing.value.id}`, payload)
      snackbar.success('Usuario actualizado')
    } else {
      await api.post('/admin/users', payload)
      snackbar.success('Usuario creado')
    }
    dialog.value = false
    await fetchData()
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al guardar')
  } finally {
    saving.value = false
  }
}

function confirmDelete(u) {
  toDelete.value = u
  deleteDialog.value = true
}

async function doDelete() {
  try {
    await api.delete(`/admin/users/${toDelete.value.id}`)
    snackbar.success('Usuario eliminado')
    deleteDialog.value = false
    await fetchData()
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al eliminar')
  }
}
</script>