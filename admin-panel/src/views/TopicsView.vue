<template>
  <div>
    <div class="d-flex align-center mb-6"><div><v-btn variant="text" prepend-icon="mdi-arrow-left" to="/subjects" class="mb-2">Materias</v-btn><h1 class="text-h4">Temas de {{ subjectName }}</h1></div><v-spacer /><v-btn color="primary" prepend-icon="mdi-plus" @click="openDialog()">Nuevo Tema</v-btn></div>
    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }} <v-btn size="small" class="ml-2" @click="fetchData">Reintentar</v-btn></v-alert>
    <v-card rounded="lg" elevation="2"><v-progress-linear v-if="loading" indeterminate color="primary" />
      <v-table v-else><thead><tr><th>Orden</th><th></th><th>ID</th><th>Nombre</th><th>Dificultad</th><th>Activo</th><th>Acciones</th></tr></thead>
        <tbody><tr v-for="(t,idx) in items" :key="t.id">
          <td><v-btn icon="mdi-chevron-up" variant="text" size="x-small" :disabled="idx===0" @click="moveItem(idx,-1)" /><v-btn icon="mdi-chevron-down" variant="text" size="x-small" :disabled="idx===items.length-1" @click="moveItem(idx,1)" /></td>
          <td><span class="text-h5">{{ t.icon }}</span></td><td class="font-weight-medium">{{ t.id }}</td><td>{{ t.name }}</td><td><v-rating :model-value="t.difficulty" length="5" size="20" readonly density="compact" color="amber" /></td><td><v-chip :color="t.is_active?'green':'grey'" size="small" variant="tonal">{{ t.is_active?'Sí':'No' }}</v-chip></td>
            <td>
              <v-tooltip text="Ver unidades" location="top"><template #activator="{ props: tp }"><v-btn icon="mdi-dumbbell" v-bind="tp" variant="text" size="small" color="warning" :to="`/topics/${t.id}/units`" /></template></v-tooltip>
              <v-tooltip text="Editar teoría" location="top"><template #activator="{ props: tp }"><v-btn icon="mdi-book-open-page-variant" v-bind="tp" variant="text" size="small" color="info" :to="{ path: `/theory/topic/${t.id}`, query: { subjectId } }" /></template></v-tooltip>
              <v-tooltip text="Editar tema" location="top"><template #activator="{ props: tp }"><v-btn icon="mdi-pencil" v-bind="tp" variant="text" size="small" color="primary" @click="openDialog(t)" /></template></v-tooltip>
              <v-tooltip text="Desactivar" location="top"><template #activator="{ props: tp }"><v-btn icon="mdi-delete" v-bind="tp" variant="text" size="small" color="error" @click="confirmDelete(t)" /></template></v-tooltip>
              <v-tooltip text="Eliminar permanentemente" location="top"><template #activator="{ props: tp }"><v-btn icon="mdi-delete-forever" v-bind="tp" variant="text" size="small" color="deep-orange" @click="confirmHardDelete(t)" /></template></v-tooltip>
            </td></tr></tbody></v-table>
      <v-card-text v-if="!loading && items.length===0" class="text-center text-grey">No hay temas.</v-card-text>
    </v-card>

    <v-dialog v-model="dialog" max-width="500"><v-card rounded="lg"><v-card-title>{{ editing?'Editar':'Nuevo' }} Tema</v-card-title>
      <v-card-text><v-text-field v-model="form.id" label="ID" :disabled="!!editing" variant="outlined" class="mb-2" /><v-text-field v-model="form.name" label="Nombre" variant="outlined" class="mb-2" /><v-text-field v-model="form.icon" label="Ícono" variant="outlined" class="mb-2" /><v-text-field v-model.number="form.difficulty" label="Dificultad (1-5)" type="number" min="1" max="5" variant="outlined" class="mb-2" /><v-text-field v-model.number="form.sort_order" label="Orden" type="number" variant="outlined" /></v-card-text>
      <v-card-actions><v-spacer /><v-btn variant="text" @click="dialog=false">Cancelar</v-btn><v-btn color="primary" :loading="saving" @click="save">{{ editing?'Guardar':'Crear' }}</v-btn></v-card-actions></v-card>
    </v-dialog>
    <v-dialog v-model="deleteDialog" max-width="400"><v-card rounded="lg"><v-card-title>¿Desactivar?</v-card-title><v-card-text>"{{ toDelete?.name }}" se desactivará.</v-card-text><v-card-actions><v-spacer /><v-btn variant="text" @click="deleteDialog=false">Cancelar</v-btn><v-btn color="error" @click="doDelete">Desactivar</v-btn></v-card-actions></v-card></v-dialog>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'

const route = useRoute()
const subjectId = route.params.subjectId
const subjectName = ref('')
const items = ref([]); const loading = ref(true); const error = ref('')
const dialog = ref(false); const deleteDialog = ref(false); const editing = ref(null); const saving = ref(false); const toDelete = ref(null)
const form = ref({ id:'', subject_id:subjectId, name:'', icon:'', difficulty:1, sort_order:0 })

async function fetchData() { loading.value=true; error.value=''; try { const [r,s]=await Promise.all([api.get(`/admin/subjects/${subjectId}/topics`),api.get('/admin/subjects')]); items.value=r.data; const sub=s.data.find(x=>x.id===subjectId); if(sub) subjectName.value=sub.name } catch(e) { error.value=e.response?.data?.detail||'Error' } finally { loading.value=false } }
watch(() => route.params, fetchData, { immediate: true })

function openDialog(topic=null) { editing.value=topic; form.value=topic?{...topic,subject_id:subjectId}:{id:'',subject_id:subjectId,name:'',icon:'',difficulty:1,sort_order:0}; dialog.value=true }
async function save() { saving.value=true; try { if(editing.value) await api.put(`/admin/topics/${editing.value.id}`,form.value); else await api.post('/admin/topics',form.value); dialog.value=false; await fetchData() } catch(e) { alert(e.response?.data?.detail||'Error') } finally { saving.value=false } }
function confirmDelete(t) { toDelete.value=t; deleteDialog.value=true }
async function doDelete() { await api.delete(`/admin/topics/${toDelete.value.id}`); deleteDialog.value=false; await fetchData() }
function confirmHardDelete(t) { if(confirm(`¿Eliminar "${t.name}" PERMANENTEMENTE?`)) doHardDelete(t) }
async function doHardDelete(t) { await api.delete(`/admin/topics/${t.id}/hard`); await fetchData() }
async function moveItem(idx, dir) {
  const a = items.value[idx]; const b = items.value[idx+dir]
  const temp = a.sort_order; a.sort_order = b.sort_order; b.sort_order = temp
  const list = items.value.map(x=>({id:x.id,sort_order:x.sort_order}))
  await api.put('/admin/topics/reorder', { items: list })
  await fetchData()
}
</script>
