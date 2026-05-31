<template>
  <div>
    <div class="d-flex align-center mb-6"><div><v-btn variant="text" prepend-icon="mdi-arrow-left" :to="`/subjects/${subjectId}/topics`" class="mb-2">Temas</v-btn><h1 class="text-h4">Unidades de {{ topicName }}</h1></div><v-spacer />
      <v-btn color="warning" variant="tonal" size="small" class="mr-2" @click="topicAction('lock-all')">🔒 Bloquear todo</v-btn>
      <v-btn color="success" variant="tonal" size="small" class="mr-2" @click="topicAction('unlock-all')">🔓 Desbloquear todo</v-btn>
      <v-btn color="orange" variant="tonal" size="small" class="mr-2" @click="topicAction('reset-all')">🔄 Resetear todo</v-btn>
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openDialog()">Nueva Unidad</v-btn>
    </div>

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }} <v-btn size="small" class="ml-2" @click="fetchData">Reintentar</v-btn></v-alert>

    <v-card rounded="lg" elevation="2"><v-progress-linear v-if="loading" indeterminate color="primary" />
      <v-table v-else><thead><tr><th>ID</th><th>Título</th><th>Tipo</th><th>Input</th><th>Estado</th><th>Acciones</th></tr></thead>
        <tbody><tr v-for="u in items" :key="u.id"><td class="font-weight-medium">{{ u.id }}</td><td>{{ u.title }}</td><td><v-chip size="small" color="primary" variant="tonal">{{ u.exercise_type }}</v-chip></td><td><v-chip :color="u.input_mode==='tap'?'green':'orange'" size="small" variant="tonal">{{ u.input_mode==='tap'?'Tocar':'Escribir' }}</v-chip></td><td><v-chip :color="u.is_locked?'grey':'green'" size="small" variant="tonal">{{ u.is_locked?'🔒 Bloqueado':'🔓 Abierto' }}</v-chip></td>
            <td>
              <v-tooltip text="Editar unidad" location="top"><template #activator="{ props: tp }"><v-btn icon="mdi-pencil" v-bind="tp" variant="text" size="small" color="primary" @click="openDialog(u)" /></template></v-tooltip>
              <v-tooltip text="Ver bloques" location="top"><template #activator="{ props: tp }"><v-btn icon="mdi-list-box-outline" v-bind="tp" variant="text" size="small" color="secondary" :to="`/units/${u.id}/blocks`" /></template></v-tooltip>
              <v-tooltip text="Editar teoría" location="top"><template #activator="{ props: tp }"><v-btn icon="mdi-book-open-page-variant" v-bind="tp" variant="text" size="small" color="info" :to="{ path: `/theory/unit/${u.id}`, query: { topicId } }" /></template></v-tooltip>
              <v-tooltip :text="u.is_locked?'Desbloquear':'Bloquear'" location="top"><template #activator="{ props: tp }"><v-btn v-if="!u.is_locked" icon="mdi-lock" v-bind="tp" variant="text" size="small" color="warning" @click="toggleLock(u,true)" /><v-btn v-else icon="mdi-lock-open" v-bind="tp" variant="text" size="small" color="success" @click="toggleLock(u,false)" /></template></v-tooltip>
              <v-tooltip text="Resetear progreso" location="top"><template #activator="{ props: tp }"><v-btn icon="mdi-restart" v-bind="tp" variant="text" size="small" color="orange" @click="confirmReset(u)" /></template></v-tooltip>
              <v-tooltip text="Eliminar" location="top"><template #activator="{ props: tp }"><v-btn icon="mdi-delete" v-bind="tp" variant="text" size="small" color="error" @click="confirmDelete(u)" /></template></v-tooltip>
            </td></tr></tbody></v-table>
      <v-card-text v-if="!loading && items.length===0" class="text-center text-grey">No hay unidades.</v-card-text>
    </v-card>

    <v-dialog v-model="dialog" max-width="500"><v-card rounded="lg"><v-card-title>{{ editing?'Editar':'Nueva' }} Unidad</v-card-title>
      <v-card-text><v-text-field v-model="form.id" label="ID" :disabled="!!editing" variant="outlined" class="mb-2" /><v-text-field v-model="form.title" label="Título" variant="outlined" class="mb-2" /><v-select v-model="form.input_mode" label="Modo por defecto" :items="[{title:'🖐️ Tap',value:'tap'},{title:'⌨️ Type',value:'type'}]" variant="outlined" class="mb-2" /><v-textarea v-model="form.explanation" label="Explicación" variant="outlined" rows="2" class="mb-3" />
        <v-label class="mb-1">🔊 Sonido de acierto</v-label>
        <div class="d-flex align-center mb-2"><v-text-field v-model="form.sound_correct_url" placeholder="URL o subir archivo..." variant="outlined" density="compact" hide-details class="mr-1" /><v-btn icon="mdi-play" variant="text" size="small" color="success" @click="previewSound(form.sound_correct_url)" /><v-btn icon="mdi-upload" variant="text" size="small" color="primary" @click="triggerUpload('correct')" /><v-btn icon="mdi-close" variant="text" size="small" color="grey" @click="form.sound_correct_url=''" /></div>
        <v-label class="mb-1">🔊 Sonido de error</v-label>
        <div class="d-flex align-center"><v-text-field v-model="form.sound_incorrect_url" placeholder="URL o subir archivo..." variant="outlined" density="compact" hide-details class="mr-1" /><v-btn icon="mdi-play" variant="text" size="small" color="success" @click="previewSound(form.sound_incorrect_url)" /><v-btn icon="mdi-upload" variant="text" size="small" color="primary" @click="triggerUpload('incorrect')" /><v-btn icon="mdi-close" variant="text" size="small" color="grey" @click="form.sound_incorrect_url=''" /></div>
      </v-card-text>
      <v-card-actions><v-spacer /><v-btn variant="text" @click="dialog=false">Cancelar</v-btn><v-btn color="primary" :loading="saving" @click="save">{{ editing?'Guardar':'Crear' }}</v-btn></v-card-actions></v-card>
    </v-dialog>
    <v-dialog v-model="deleteDialog" max-width="400"><v-card rounded="lg"><v-card-title>¿Eliminar?</v-card-title><v-card-text>"{{ toDelete?.title }}" se eliminará con sus bloques y ejercicios.</v-card-text><v-card-actions><v-spacer /><v-btn variant="text" @click="deleteDialog=false">Cancelar</v-btn><v-btn color="error" @click="doDelete">Eliminar</v-btn></v-card-actions></v-card></v-dialog>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'

const route = useRoute()
const topicId = route.params.topicId; const subjectId = ref(''); const topicName = ref('')
const items = ref([]); const loading = ref(true); const error = ref('')
const dialog = ref(false); const deleteDialog = ref(false); const editing = ref(null); const saving = ref(false); const toDelete = ref(null)
const form = ref({ id:'', title:'', topic_id:topicId, exercise_type:'fill-blank', input_mode:'tap', explanation:'', sound_correct_url:'', sound_incorrect_url:'' })
const uploadType = ref('')

async function fetchData() { loading.value=true; error.value=''; try { const [u,t]=await Promise.all([api.get(`/admin/topics/${topicId}/units`),api.get(`/admin/topics/${topicId}`)]); items.value=u.data; topicName.value=t.data.name||topicId; subjectId.value=t.data.subject_id } catch(e) { error.value=e.response?.data?.detail||'Error' } finally { loading.value=false } }
watch(() => route.params, fetchData, { immediate: true })

function openDialog(unit=null) { editing.value=unit; form.value=unit?{...unit,topic_id:topicId}:{id:'',title:'',topic_id:topicId,exercise_type:'fill-blank',input_mode:'tap',explanation:'',sound_correct_url:'',sound_incorrect_url:''}; dialog.value=true }
async function save() { saving.value=true; try { if(editing.value) await api.put(`/admin/units/${editing.value.id}`,form.value); else await api.post('/admin/units',form.value); dialog.value=false; await fetchData() } catch(e) { alert(e.response?.data?.detail||'Error') } finally { saving.value=false } }
function triggerUpload(type) { uploadType.value=type; const el=document.createElement('input'); el.type='file'; el.accept='audio/*'; el.onchange=async(e)=>{ const f=e.target.files[0]; if(!f) return; const fd=new FormData(); fd.append('file',f); const {data}=await api.post('/admin/upload',fd,{headers:{'Content-Type':'multipart/form-data'}}); if(type==='correct') form.value.sound_correct_url=data.url; else form.value.sound_incorrect_url=data.url }; el.click() }
function previewSound(url) { if(!url) return; const a=new Audio(url); a.play() }
function confirmDelete(u) { toDelete.value=u; deleteDialog.value=true }
async function doDelete() { await api.delete(`/admin/units/${toDelete.value.id}`); deleteDialog.value=false; await fetchData() }
async function toggleLock(unit,lock) { try { await api.put(`/admin/units/${unit.id}/${lock?'lock':'unlock'}`); await fetchData() } catch(e) { alert(e.response?.data?.detail||'Error') } }
async function confirmReset(unit) { if(!confirm(`¿Resetear progreso de "${unit.title}"?`)) return; await api.delete(`/admin/progress/android-default/${topicId}/${unit.id}`); alert('Reseteado') }
async function topicAction(action) { const labels={'lock-all':'Bloquear','unlock-all':'Desbloquear','reset-all':'Resetear'}; if(!confirm(`¿${labels[action]} todas las unidades?`)) return; await api.post(`/admin/topics/${topicId}/${action}`); await fetchData(); alert('Hecho') }
</script>
