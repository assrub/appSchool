<template>
  <div>
    <div class="d-flex align-center mb-6"><h1 class="text-h4">Usuarios (Hijos)</h1><v-spacer /><v-btn color="primary" prepend-icon="mdi-plus" @click="openDialog()">Nuevo Usuario</v-btn></div>
    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }} <v-btn size="small" class="ml-2" @click="fetchData">Reintentar</v-btn></v-alert>
    <v-card rounded="lg" elevation="2"><v-progress-linear v-if="loading" indeterminate color="primary" />
      <v-table v-else><thead><tr><th>ID</th><th>Usuario</th><th>Nombre</th><th>Activo</th><th>Acciones</th></tr></thead>
        <tbody><tr v-for="u in items" :key="u.id"><td>{{ u.id }}</td><td>{{ u.username }}</td><td>{{ u.display_name }}</td><td><v-chip :color="u.is_active?'green':'grey'" size="small" variant="tonal">{{ u.is_active?'Sí':'No' }}</v-chip></td>
          <td><v-btn icon="mdi-pencil" variant="text" size="small" color="primary" @click="openDialog(u)" /><v-btn icon="mdi-delete" variant="text" size="small" color="error" @click="confirmDelete(u)" /></td></tr></tbody></v-table>
    </v-card>
    <v-dialog v-model="dialog" max-width="500"><v-card rounded="lg"><v-card-title>{{ editing?'Editar':'Nuevo' }} Usuario</v-card-title>
      <v-card-text><v-text-field v-model="form.username" label="Usuario" :disabled="!!editing" variant="outlined" class="mb-2" /><v-text-field v-model="form.display_name" label="Nombre" variant="outlined" class="mb-2" /><v-text-field v-model="form.password" label="Contraseña" type="password" variant="outlined" :hint="editing?'Dejar vacío para no cambiar':''" persistent-hint /></v-card-text>
      <v-card-actions><v-spacer /><v-btn variant="text" @click="dialog=false">Cancelar</v-btn><v-btn color="primary" :loading="saving" @click="save">{{ editing?'Guardar':'Crear' }}</v-btn></v-card-actions></v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'

const route = useRoute()
const items = ref([]); const loading = ref(true); const error = ref('')
const dialog = ref(false); const editing = ref(null); const saving = ref(false); const toDelete = ref(null)
const form = ref({ username:'', display_name:'', password:'' })

async function fetchData() { loading.value=true; error.value=''; try { const r=await api.get('/admin/users'); items.value=r.data } catch(e) { error.value=e.response?.data?.detail||'Error' } finally { loading.value=false } }
watch(() => route.params, fetchData, { immediate: true })

function openDialog(user=null) { editing.value=user; form.value=user?{...user,password:''}:{username:'',display_name:'',password:''}; dialog.value=true }
async function save() { saving.value=true; try { if(editing.value) await api.put(`/admin/users/${editing.value.id}`,form.value); else await api.post('/admin/users',form.value); dialog.value=false; await fetchData() } catch(e) { alert(e.response?.data?.detail||'Error') } finally { saving.value=false } }
function confirmDelete(u) { if(confirm('Eliminar usuario?')) doDelete(u) }
async function doDelete(u) { await api.delete(`/admin/users/${u.id}`); await fetchData() }
</script>
