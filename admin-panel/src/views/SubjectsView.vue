<template>
  <div>
    <div class="d-flex align-center mb-6">
      <h1 class="text-h4">Materias</h1>
      <v-spacer />
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openDialog()">Nueva Materia</v-btn>
    </div>

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }} <v-btn size="small" class="ml-2" @click="fetchData">Reintentar</v-btn></v-alert>

    <v-row>
      <v-col cols="7">
        <v-card rounded="lg" elevation="2">
          <v-progress-linear v-if="loading" indeterminate color="primary" />
          <v-table v-else>
        <thead><tr><th></th><th>ID</th><th>Nombre</th><th>Color</th><th>Activo</th><th>Acciones</th></tr></thead>
        <tbody>
          <tr v-for="s in items" :key="s.id">
            <td><span class="text-h5">{{ s.icon }}</span></td>
            <td class="font-weight-medium">{{ s.id }}</td><td>{{ s.name }}</td>
            <td><v-chip :color="s.color" size="small" class="text-white">{{ s.color }}</v-chip></td>
            <td><v-chip :color="s.is_active?'green':'grey'" size="small" variant="tonal">{{ s.is_active?'Sí':'No' }}</v-chip></td>
            <td>
              <v-tooltip text="Editar materia" location="top"><template #activator="{ props: tp }"><v-btn icon="mdi-pencil" v-bind="tp" variant="text" size="small" color="primary" @click="openDialog(s)" /></template></v-tooltip>
              <v-tooltip text="Ver temas" location="top"><template #activator="{ props: tp }"><v-btn icon="mdi-book-open-page-variant" v-bind="tp" variant="text" size="small" color="secondary" :to="`/subjects/${s.id}/topics`" /></template></v-tooltip>
              <v-tooltip text="Desactivar materia" location="top"><template #activator="{ props: tp }"><v-btn icon="mdi-delete" v-bind="tp" variant="text" size="small" color="error" @click="confirmDelete(s)" /></template></v-tooltip>
              <v-tooltip text="Eliminar permanentemente" location="top"><template #activator="{ props: tp }"><v-btn icon="mdi-delete-forever" v-bind="tp" variant="text" size="small" color="deep-orange" @click="confirmHardDelete(s)" /></template></v-tooltip>
            </td>
          </tr>
        </tbody>
      </v-table>
    </v-card>
      </v-col>
      <v-col cols="5" class="d-flex align-start justify-center">
        <MobilePreview :html="listPreviewHtml" />
      </v-col>
    </v-row>

    <v-dialog v-model="dialog" max-width="500">
      <v-card rounded="lg"><v-card-title>{{ editing ? 'Editar' : 'Nueva' }} Materia</v-card-title>
        <v-card-text>
          <v-text-field v-model="form.id" label="ID" :disabled="!!editing" variant="outlined" class="mb-2" />
          <v-text-field v-model="form.name" label="Nombre" variant="outlined" class="mb-2" />
          <v-text-field v-model="form.icon" label="Ícono (emoji)" variant="outlined" class="mb-2" />
          <v-text-field v-model="form.color" label="Color HEX" variant="outlined" class="mb-2" />
          <v-text-field v-model.number="form.sort_order" label="Orden" type="number" variant="outlined" />
        </v-card-text>
        <v-card-actions><v-spacer /><v-btn variant="text" @click="dialog=false">Cancelar</v-btn><v-btn color="primary" :loading="saving" @click="save">{{ editing?'Guardar':'Crear' }}</v-btn></v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="deleteDialog" max-width="400"><v-card rounded="lg"><v-card-title>¿Desactivar?</v-card-title><v-card-text>"{{ toDelete?.name }}" se desactivará.</v-card-text><v-card-actions><v-spacer /><v-btn variant="text" @click="deleteDialog=false">Cancelar</v-btn><v-btn color="error" @click="doDelete">Desactivar</v-btn></v-card-actions></v-card></v-dialog>
    <v-dialog v-model="hardDeleteDialog" max-width="400"><v-card rounded="lg"><v-card-title>⚠️ Eliminar permanentemente</v-card-title><v-card-text>¿Estás seguro de eliminar "<b>{{ toHardDelete?.name }}</b>"? Se borrarán TODOS los temas, unidades, ejercicios y progreso.</v-card-text><v-card-actions><v-spacer /><v-btn variant="text" @click="hardDeleteDialog=false">Cancelar</v-btn><v-btn color="deep-orange" @click="doHardDelete">Eliminar permanentemente</v-btn></v-card-actions></v-card></v-dialog>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'
import MobilePreview from '../components/MobilePreview.vue'
import AppPreview from '../components/AppPreview.vue'

const route = useRoute()
const items = ref([]); const loading = ref(true); const error = ref('')
const dialog = ref(false); const deleteDialog = ref(false); const hardDeleteDialog = ref(false); const editing = ref(null); const saving = ref(false); const toDelete = ref(null); const toHardDelete = ref(null)
const form = ref({ id:'', name:'', icon:'', color:'#4CAF50', sort_order:0 })

const listPreviewHtml = computed(() => {
  if (!items.value.length) return '<p style="color:#999;text-align:center;padding:20px">Sin materias</p>'
  return items.value.map(s => {
    return `<div style="display:flex;align-items:center;gap:12px;padding:16px 14px;border-bottom:1px solid #f0f0f0">
      <div style="font-size:32px">${s.icon || '📚'}</div>
      <div style="flex:1;min-width:0">
        <div style="font-weight:bold;font-size:16px;color:#333">${s.name}</div>
        <div style="font-size:12px;color:#999;margin-top:2px">${(s.topicsCount||0)} temas</div>
      </div>
      <div style="font-size:18px;color:#4CAF50">→</div>
    </div>`
  }).join('')
})

async function fetchData() { loading.value=true; error.value=''; try { const r=await api.get('/admin/subjects'); items.value=r.data } catch(e) { error.value=e.response?.data?.detail||'Error' } finally { loading.value=false } }
watch(() => route.params, fetchData, { immediate: true })

function openDialog(item=null) { editing.value=item; form.value=item?{...item}:{id:'',name:'',icon:'',color:'#4CAF50',sort_order:0}; dialog.value=true }
async function save() { saving.value=true; try { if(editing.value) await api.put(`/admin/subjects/${editing.value.id}`,form.value); else await api.post('/admin/subjects',form.value); dialog.value=false; await fetchData() } catch(e) { alert(e.response?.data?.detail||'Error') } finally { saving.value=false } }
function confirmDelete(s) { toDelete.value=s; deleteDialog.value=true }
async function doDelete() { await api.delete(`/admin/subjects/${toDelete.value.id}`); deleteDialog.value=false; await fetchData() }
function confirmHardDelete(s) { toHardDelete.value = s; hardDeleteDialog.value = true }
async function doHardDelete() { await api.delete(`/admin/subjects/${toHardDelete.value.id}/hard`); hardDeleteDialog.value=false; await fetchData() }
</script>
