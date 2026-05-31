<template>
  <div>
    <div class="d-flex align-center mb-6"><div><v-btn variant="text" prepend-icon="mdi-arrow-left" :to="`/subjects/${subjectId}/topics`" class="mb-2">Temas</v-btn><h1 class="text-h4">Unidades de {{ topicName }}</h1></div><v-spacer />
      <v-btn color="warning" variant="tonal" size="small" class="mr-2" @click="topicAction('lock-all')">🔒 Bloquear todo</v-btn>
      <v-btn color="success" variant="tonal" size="small" class="mr-2" @click="topicAction('unlock-all')">🔓 Desbloquear todo</v-btn>
      <v-btn color="orange" variant="tonal" size="small" class="mr-2" @click="topicAction('reset-all')">🔄 Resetear todo</v-btn>
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openDialog()">Nueva Unidad</v-btn>
    </div>

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }} <v-btn size="small" class="ml-2" @click="fetchData">Reintentar</v-btn></v-alert>

    <v-row>
      <v-col cols="7">
        <v-card rounded="lg" elevation="2"><v-progress-linear v-if="loading" indeterminate color="primary" />
          <v-table v-else><thead><tr><th>Ord.</th><th>ID</th><th>Título</th><th>Input</th><th>Estado</th><th>Acciones</th></tr></thead>
        <tbody><tr v-for="(u,idx) in items" :key="u.id">
          <td><v-btn icon="mdi-chevron-up" variant="text" size="x-small" :disabled="idx===0" @click="moveItem(idx,-1)" /><v-btn icon="mdi-chevron-down" variant="text" size="x-small" :disabled="idx===items.length-1" @click="moveItem(idx,1)" /></td>
          <td class="font-weight-medium">{{ u.id }}</td><td>{{ u.title }}</td><td><v-chip :color="u.input_mode==='tap'?'green':'orange'" size="small" variant="tonal">{{ u.input_mode==='tap'?'Tocar':'Escribir' }}</v-chip></td><td><v-chip :color="u.is_locked?'grey':'green'" size="small" variant="tonal">{{ u.is_locked?'🔒':'🔓' }}</v-chip></td>
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
      </v-col>
      <v-col cols="5" class="d-flex align-start justify-center">
        <MobilePreview :html="listPreviewHtml" />
      </v-col>
    </v-row>

    <v-dialog v-model="dialog" max-width="850"><v-card rounded="lg"><v-card-title>{{ editing?'Editar':'Nueva' }} Unidad</v-card-title>
      <v-card-text>
        <v-row>
          <v-col cols="7">
            <v-text-field v-model="form.id" label="ID" :disabled="!!editing" variant="outlined" class="mb-2" />
            <v-text-field v-model="form.title" label="Título" variant="outlined" class="mb-2" />
            <v-select v-model="form.input_mode" label="Modo" :items="[{title:'🖐️ Tap',value:'tap'},{title:'⌨️ Type',value:'type'}]" variant="outlined" class="mb-2" />
            <v-textarea v-model="form.explanation" label="Explicación" variant="outlined" rows="2" class="mb-3" />
            <v-label class="mb-1">🔊 Sonido de acierto</v-label>
            <div class="d-flex align-center mb-2"><v-text-field v-model="form.sound_correct_url" placeholder="URL o subir..." variant="outlined" density="compact" hide-details class="mr-1" /><v-btn icon="mdi-play" variant="text" size="small" color="success" @click="previewSound(form.sound_correct_url)" /><v-btn icon="mdi-upload" variant="text" size="small" color="primary" @click="triggerUpload('correct')" /><v-btn icon="mdi-close" variant="text" size="small" color="grey" @click="form.sound_correct_url=''" /></div>
            <v-label class="mb-1">🔊 Sonido de error</v-label>
            <div class="d-flex align-center"><v-text-field v-model="form.sound_incorrect_url" placeholder="URL o subir..." variant="outlined" density="compact" hide-details class="mr-1" /><v-btn icon="mdi-play" variant="text" size="small" color="success" @click="previewSound(form.sound_incorrect_url)" /><v-btn icon="mdi-upload" variant="text" size="small" color="primary" @click="triggerUpload('incorrect')" /><v-btn icon="mdi-close" variant="text" size="small" color="grey" @click="form.sound_incorrect_url=''" /></div>
          </v-col>
          <v-col cols="5" class="d-flex align-center justify-center">
            <MobilePreview :html="previewHtml" />
          </v-col>
        </v-row>
      </v-card-text>
      <v-card-actions><v-spacer /><v-btn variant="text" @click="dialog=false">Cancelar</v-btn><v-btn color="primary" :loading="saving" @click="save">{{ editing?'Guardar':'Crear' }}</v-btn></v-card-actions></v-card>
    </v-dialog>
    <v-dialog v-model="deleteDialog" max-width="400"><v-card rounded="lg"><v-card-title>¿Eliminar?</v-card-title><v-card-text>"{{ toDelete?.title }}" se eliminará con sus bloques y ejercicios.</v-card-text><v-card-actions><v-spacer /><v-btn variant="text" @click="deleteDialog=false">Cancelar</v-btn><v-btn color="error" @click="doDelete">Eliminar</v-btn></v-card-actions></v-card></v-dialog>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'
import MobilePreview from '../components/MobilePreview.vue'

const route = useRoute()
const topicId = route.params.topicId; const subjectId = ref(''); const topicName = ref('')
const items = ref([]); const loading = ref(true); const error = ref('')
const dialog = ref(false); const deleteDialog = ref(false); const editing = ref(null); const saving = ref(false); const toDelete = ref(null)
const form = ref({ id:'', title:'', topic_id:topicId, exercise_type:'fill-blank', input_mode:'tap', explanation:'', sound_correct_url:'', sound_incorrect_url:'' })
const uploadType = ref('')
const viewMode = ref('table')
const previewHtml = computed(() => {
  const icon = form.value.id ? '✏️' : '📝'
  const title = form.value.title || 'Nombre de la unidad'
  const mode = form.value.input_mode === 'tap' ? '🖐️ Tap' : '⌨️ Type'
  const pct = 0
  return `<div style="padding:8px 12px">
    <div style="display:flex;align-items:center;gap:10px;margin-bottom:8px">
      <div style="font-size:32px">${icon}</div>
      <div style="flex:1">
        <div style="font-weight:bold;font-size:16px">${title}</div>
        <div style="font-size:12px;color:#999">${mode}</div>
      </div>
    </div>
    <div style="height:6px;border-radius:3px;background:#e0e0e0;margin-bottom:4px">
      <div style="width:${pct}%;height:100%;border-radius:3px;background:#4CAF50"></div>
    </div>
    <div style="font-size:11px;color:#999">0 / 0 items</div>
  </div>`
})

const listPreviewHtml = computed(() => {
  if (!items.value.length) return '<p style="color:#999;text-align:center;padding:20px">Sin unidades</p>'
  const unitIcons = ['✏️','❌','❓','✅','🔄','📝']
  return items.value.map((u, i) => {
    const icon = unitIcons[i % unitIcons.length]
    const title = u.title || u.id
    const mode = u.input_mode === 'tap' ? '🖐️ Tap' : '⌨️ Type'
    const locked = u.is_locked
    return `<div style="display:flex;align-items:center;gap:12px;padding:14px 12px;border-bottom:1px solid #f0f0f0;${locked?'opacity:0.5':''}">
      <div style="font-size:28px">${locked?'🔒':icon}</div>
      <div style="flex:1;min-width:0">
        <div style="font-weight:bold;font-size:14px;color:${locked?'#999':'#333'}">${title}</div>
        <div style="font-size:11px;color:#999">${mode}</div>
        <div style="height:6px;border-radius:3px;background:#e0e0e0;margin-top:6px;overflow:hidden">
          <div style="width:0%;height:100%;border-radius:3px;background:#4CAF50"></div>
        </div>
      </div>
      <div style="font-size:20px;color:${locked?'#999':'#4CAF50'}">${locked?'🔒':'→'}</div>
    </div>`
  }).join('')
})

async function fetchData() { loading.value=true; error.value=''; try { const [u,t]=await Promise.all([api.get(`/admin/topics/${topicId}/units`),api.get(`/admin/topics/${topicId}`)]); items.value=u.data; topicName.value=t.data.name||topicId; subjectId.value=t.data.subject_id } catch(e) { error.value=e.response?.data?.detail||'Error' } finally { loading.value=false } }
watch(() => route.params, fetchData, { immediate: true })

function openDialog(unit=null) { editing.value=unit; form.value=unit?{...unit,topic_id:topicId}:{id:'',title:'',topic_id:topicId,exercise_type:'fill-blank',input_mode:'tap',explanation:'',sound_correct_url:'',sound_incorrect_url:''}; dialog.value=true }
async function save() { saving.value=true; try { if(editing.value) await api.put(`/admin/units/${editing.value.id}`,form.value); else await api.post('/admin/units',form.value); dialog.value=false; await fetchData() } catch(e) { alert(e.response?.data?.detail||'Error') } finally { saving.value=false } }
function triggerUpload(type) { uploadType.value=type; const el=document.createElement('input'); el.type='file'; el.accept='audio/*'; el.onchange=async(e)=>{ const f=e.target.files[0]; if(!f) return; const fd=new FormData(); fd.append('file',f); const {data}=await api.post('/admin/upload',fd,{headers:{'Content-Type':'multipart/form-data'}}); if(type==='correct') form.value.sound_correct_url=data.url; else form.value.sound_incorrect_url=data.url }; el.click() }
function previewSound(url) { if(!url) return; const a=new Audio(url); a.play() }
function confirmDelete(u) { toDelete.value=u; deleteDialog.value=true }
async function doDelete() { await api.delete(`/admin/units/${toDelete.value.id}`); deleteDialog.value=false; await fetchData() }
async function toggleLock(unit,lock) { try { await api.put(`/admin/units/${unit.id}/${lock?'lock':'unlock'}`); await fetchData() } catch(e) { alert(e.response?.data?.detail||'Error') } }
async function confirmReset(unit) { if(!confirm(`¿Resetear progreso de "${unit.title}" para TODOS los usuarios?`)) return; await api.delete(`/admin/progress/${topicId}/${unit.id}/reset-all-users`); alert('Reseteado para todos los usuarios') }
async function topicAction(action) { const labels={'lock-all':'Bloquear','unlock-all':'Desbloquear','reset-all':'Resetear'}; if(!confirm(`¿${labels[action]} todas las unidades?`)) return; await api.post(`/admin/topics/${topicId}/${action}`); await fetchData(); alert('Hecho') }
async function moveItem(idx, dir) {
  const a = items.value[idx]; const b = items.value[idx+dir]
  const temp = a.sort_order; a.sort_order = b.sort_order; b.sort_order = temp
  const list = items.value.map(x=>({id:x.id,sort_order:x.sort_order}))
  try {
    await api.put('/admin/units-reorder', { items: list })
    await fetchData()
  } catch (e) {
    alert('Error al reordenar: ' + (e.response?.data?.detail || e.message))
  }
}
</script>
