<template>
  <div>
    <div class="d-flex align-center mb-6"><div><v-btn variant="text" prepend-icon="mdi-arrow-left" :to="`/units/${unitId}/blocks`" class="mb-2">Bloques</v-btn><h1 class="text-h4">Ejercicios</h1><p class="text-grey">{{ blockTitle }}</p></div><v-spacer /><v-btn color="primary" prepend-icon="mdi-plus" @click="openDialog()">Nuevo</v-btn></div>
    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }} <v-btn size="small" class="ml-2" @click="fetchData">Reintentar</v-btn></v-alert>
    <v-card rounded="lg" elevation="2"><v-progress-linear v-if="loading" indeterminate color="primary" />
      <v-table v-else><thead><tr><th>Tipo</th><th>Input</th><th>Contenido</th><th>Respuesta</th><th></th></tr></thead>
        <tbody><tr v-for="i in items" :key="i.id">
          <td><v-chip size="x-small" color="primary" variant="tonal">{{ i.item_type }}</v-chip></td>
          <td><v-chip v-if="i.input_mode" :color="i.input_mode==='tap'?'green':'orange'" size="x-small" variant="tonal">{{ i.input_mode==='tap'?'Tap':'Type' }}</v-chip><span v-else class="text-caption">hereda</span></td>
          <td style="max-width:250px" class="text-truncate">{{ i.sentence||i.question||'-' }}</td>
          <td>{{ i.answer||'-' }}</td>
          <td><v-btn icon="mdi-pencil" variant="text" size="x-small" color="primary" @click="openDialog(i)" /><v-btn icon="mdi-delete" variant="text" size="x-small" color="error" @click="confirmDelete(i)" /></td>
        </tr></tbody></v-table>
      <v-card-text v-if="!loading && items.length===0" class="text-center text-grey">No hay ejercicios.</v-card-text>
    </v-card>

    <v-dialog v-model="dialog" max-width="900"><v-card rounded="lg"><v-card-title>{{ editing?'Editar':'Nuevo' }} Ejercicio</v-card-title>
      <v-card-text>
        <v-row>
          <v-col cols="7">
        <v-select v-model="form.item_type" label="Tipo" :items="itemTypes" variant="outlined" class="mb-3" />
        <v-select v-model="form.input_mode" label="Modo" :items="inputModes" variant="outlined" class="mb-3" clearable hint="Vacío = hereda de la unidad" persistent-hint />
        <template v-if="form.item_type==='fill-blank'">
          <v-alert variant="text" color="info" density="compact" class="mb-3" icon="mdi-information">💡 <strong>Guía:</strong> Usá <code>______</code> para marcar dónde va el blank. Podés poner varias respuestas correctas (ej: "I am" y "I'm"). Si el modo es Tap, definí qué opciones ve el nene.</v-alert>
          <v-text-field v-model="form.sentence" label="Frase (usá ______)" variant="outlined" class="mb-3" />

          <v-label class="font-weight-bold mb-1">Respuesta/s correcta/s</v-label>
          <v-text-field v-model="form.answer" label="Principal" variant="outlined" density="compact" class="mb-2" hide-details />
          <div class="mb-3"><div class="text-caption">Alternativas (opcional — otras respuestas válidas)</div><div v-for="(a,i) in answersList" :key="i" class="d-flex align-center mb-1"><v-text-field v-model="answersList[i]" variant="outlined" density="compact" hide-details class="mr-1" /><v-btn icon="mdi-close" variant="text" size="x-small" color="error" @click="answersList.splice(i,1)" /></div><v-btn size="x-small" variant="outlined" class="mt-1" @click="answersList.push('')">+ alternativa</v-btn></div>

          <v-label class="font-weight-bold mb-1">Opciones visibles (modo Tap)</v-label>
          <div class="mb-3"><div class="text-caption">Botones que el nene ve. Si dejás vacío, se generan automáticas.</div><div v-for="(o,i) in optionsList" :key="i" class="d-flex align-center mb-1"><v-text-field v-model="optionsList[i]" variant="outlined" density="compact" hide-details class="mr-1" /><v-btn icon="mdi-close" variant="text" size="x-small" color="error" @click="optionsList.splice(i,1)" /></div><v-btn size="x-small" variant="outlined" class="mt-1" @click="optionsList.push('')">+ opción</v-btn></div>

          <v-text-field v-model="form.hint" label="Pista (opcional)" variant="outlined" />
        </template>
        <template v-else-if="form.item_type==='multiple-choice'">
          <v-text-field v-model="form.question" label="Pregunta" variant="outlined" class="mb-2" />
          <div v-for="(o,i) in formOptions" :key="i" class="d-flex align-center mb-2"><v-text-field v-model="formOptions[i]" :label="'Opción '+(i+1)" variant="outlined" density="compact" hide-details class="flex-grow-1 mr-2" /><v-btn icon="mdi-delete" variant="text" size="small" color="error" @click="formOptions.splice(i,1)" /></div><v-btn variant="outlined" size="small" prepend-icon="mdi-plus" @click="formOptions.push('')" class="mb-2">Opción</v-btn>
          <v-text-field v-model="form.answer" label="Correcta" variant="outlined" />
        </template>
        <template v-else-if="form.item_type==='reorder'"><v-textarea v-model="wordsText" label="Palabras (una por línea)" variant="outlined" rows="4" class="mb-2" /><v-text-field v-model="correctOrderText" label="Orden correcto" variant="outlined" class="mb-2" /><v-text-field v-model="form.hint" label="Pista" variant="outlined" /></template>
        <template v-else-if="form.item_type==='listening'"><v-text-field v-model="form.sentence" label="Frase" variant="outlined" class="mb-2" /><v-text-field v-model="form.audio_url" label="URL audio" variant="outlined" class="mb-2" /><v-text-field v-model="form.answer" label="Respuesta" variant="outlined" /></template>
        <template v-else-if="form.item_type==='matching'"><div v-for="(p,i) in formPairs" :key="i" class="d-flex align-center mb-2"><v-text-field v-model="formPairs[i].left" label="Izq" variant="outlined" density="compact" hide-details class="mr-2" /><v-icon>mdi-arrow-right</v-icon><v-text-field v-model="formPairs[i].right" label="Der" variant="outlined" density="compact" hide-details class="ml-2" /><v-btn icon="mdi-delete" variant="text" size="small" color="error" @click="formPairs.splice(i,1)" /></div><v-btn variant="outlined" size="small" prepend-icon="mdi-plus" @click="formPairs.push({left:'',right:''})">Par</v-btn></template>
        <template v-else-if="form.item_type==='true-false'"><v-text-field v-model="form.sentence" label="Frase" variant="outlined" class="mb-2" /><v-switch v-model="form.is_correct_boolean" label="¿Es correcta?" color="primary" /><v-text-field v-if="!form.is_correct_boolean" v-model="form.answer" label="Corrección" variant="outlined" /></template>
          </v-col>
          <v-col cols="5" class="d-flex align-center justify-center">
            <MobilePreview :html="previewExerciseHtml" />
          </v-col>
        </v-row>
      </v-card-text>
      <v-card-actions><v-spacer /><v-btn variant="text" @click="dialog=false">Cancelar</v-btn><v-btn color="primary" :loading="saving" @click="save">{{ editing?'Guardar':'Crear' }}</v-btn></v-card-actions></v-card>
    </v-dialog>
    <v-dialog v-model="deleteDialog" max-width="400"><v-card rounded="lg"><v-card-title>¿Eliminar?</v-card-title><v-card-actions><v-spacer /><v-btn variant="text" @click="deleteDialog=false">Cancelar</v-btn><v-btn color="error" @click="doDelete">Eliminar</v-btn></v-card-actions></v-card></v-dialog>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'
import MobilePreview from '../components/MobilePreview.vue'

const route = useRoute()
const blockId = route.params.blockId
const unitId = ref(''); const blockTitle = ref('')
const items = ref([]); const loading = ref(true); const error = ref('')
const dialog = ref(false); const deleteDialog = ref(false); const editing = ref(null); const saving = ref(false); const toDelete = ref(null)

const itemTypes = ['fill-blank','multiple-choice','reorder','listening','matching','true-false']
const inputModes = [{title:'🖐️ Tap',value:'tap'},{title:'⌨️ Type',value:'type'}]
const answersList = ref([])
const optionsList = ref([])
const formOptions = ref(['','',''])
const formPairs = ref([{left:'',right:''},{left:'',right:''}])
const wordsText = ref('')
const correctOrderText = ref('')
const form = ref({block_id:Number(blockId),item_type:'fill-blank',sentence:'',answer:'',answers:null,hint:'',question:'',options:null,words:null,correct_order:null,input_mode:null,audio_url:'',pairs:null,is_correct_boolean:null,sort_order:0})
const previewExerciseHtml = computed(() => {
  const t = form.value.item_type
  if (t === 'fill-blank') {
    const s = form.value.sentence || '...'
    const opts = optionsList.value.filter(o => o.trim())
    const btns = opts.length ? opts : ['am', 'is', 'are']
    return `<p style="font-size:18px;text-align:center">${s.replace(/_+/g, '<span style="border-bottom:2px dashed #999;padding:2px 12px;margin:0 4px">______</span>')}</p><div style="display:flex;gap:8px;justify-content:center;margin-top:12px">${btns.map(o => `<span style="background:#f5f5f5;border-radius:8px;padding:8px 16px;font-weight:bold;font-size:16px">${o}</span>`).join('')}</div>`
  }
  if (t === 'multiple-choice') {
    const q = form.value.question || 'Pregunta...'
    const opts = formOptions.value.filter(o => o.trim())
    if (opts.length === 0) opts.push('Opción 1', 'Opción 2', 'Opción 3')
    return `<p style="font-size:16px;font-weight:bold;text-align:center">${q}</p><div style="margin-top:12px">${opts.map((o, i) => `<div style="border:1px solid #ddd;border-radius:8px;padding:10px;margin-bottom:6px;font-size:15px;cursor:pointer">${String.fromCharCode(65+i)}) ${o}</div>`).join('')}</div>`
  }
  if (t === 'reorder') {
    const words = wordsText.value ? wordsText.value.split('\n').filter(w => w.trim()) : ['palabra1', 'palabra2', 'palabra3']
    return `<p style="font-size:14px;text-align:center;color:#999">Ordená las palabras:</p><div style="display:flex;gap:8px;flex-wrap:wrap;justify-content:center;margin:8px 0">${words.map(w => `<span style="background:#e3f2fd;border-radius:8px;padding:8px 14px;font-weight:bold;font-size:16px">${w.trim()}</span>`).join('')}</div><div style="border:2px dashed #4CAF50;border-radius:8px;padding:8px;min-height:36px;text-align:center;color:#999;margin-top:8px">Soltá acá</div>`
  }
  if (t === 'listening') {
    return `<div style="text-align:center;padding:20px"><span style="font-size:48px">🔊</span><p style="color:#999;margin-top:8px">Escuchá y escribí</p><div style="border:1px solid #ddd;border-radius:8px;padding:10px;margin-top:12px;min-height:30px;color:#ccc">Escribí tu respuesta...</div></div>`
  }
  if (t === 'matching') {
    const pairs = formPairs.value.filter(p => p.left.trim() || p.right.trim())
    const left = pairs.map(p => p.left || '?')
    const right = pairs.map(p => p.right || '?').sort(() => Math.random() - 0.5)
    return `<div style="display:flex;gap:16px;justify-content:center">${[0,1,2].map(i => `<div><div style="background:#e8f5e9;border-radius:8px;padding:8px 14px;margin-bottom:4px;font-weight:bold;text-align:center;font-size:15px">${left[i]||'?'}</div><div style="background:#e3f2fd;border-radius:8px;padding:8px 14px;font-weight:bold;text-align:center;font-size:15px">${right[i]||'?'}</div></div>`).join('<div style="font-size:24px;line-height:60px">↔</div>')}</div>`
  }
  if (t === 'true-false') {
    const s = form.value.sentence || 'Frase...'
    const correct = form.value.is_correct_boolean ? '✅ Correcta' : '❌ Incorrecta'
    return `<p style="font-size:17px;text-align:center;font-weight:bold">${s}</p><p style="text-align:center;font-size:20px;margin-top:12px">${correct}</p>`
  }
  return `<p style="text-align:center;color:#999">Seleccioná un tipo de ejercicio</p>`
})
async function fetchData() { loading.value=true; error.value=''; try { const r=await api.get(`/admin/blocks/${blockId}/items`); items.value=r.data; const b=await api.get(`/admin/blocks/${blockId}`); blockTitle.value=b.data.title||'Bloque'; unitId.value=b.data.unit_id } catch(e) { error.value=e.response?.data?.detail||'Error' } finally { loading.value=false } }
watch(() => route.params, fetchData, { immediate: true })

function openDialog(item=null) { editing.value=item
  if(item){ form.value={...item}; answersList.value=item.answers||[]; optionsList.value=item.options||[]; formOptions.value=item.options||['','','']; formPairs.value=item.pairs||[{left:'',right:''}]; wordsText.value=(item.words||[]).join('\n'); correctOrderText.value=(item.correct_order||[]).join(' ') }
  else { form.value={block_id:Number(blockId),item_type:'fill-blank',sentence:'',answer:'',answers:null,hint:'',question:'',options:null,words:null,correct_order:null,input_mode:null,audio_url:'',pairs:null,is_correct_boolean:null,sort_order:0}; answersList.value=[]; optionsList.value=[]; formOptions.value=['','','']; formPairs.value=[{left:'',right:''}]; wordsText.value=''; correctOrderText.value='' }
  dialog.value=true
}

async function save() { saving.value=true; try { const p={...form.value,block_id:Number(blockId)}; const a=answersList.value.filter(x=>x.trim()); p.answers=a.length>0?a:null; if(p.item_type==='fill-blank'){ const o=optionsList.value.filter(x=>x.trim()); p.options=o.length>0?o:null } if(p.item_type==='multiple-choice') p.options=formOptions.value.filter(o=>o.trim()); if(p.item_type==='reorder'){ p.words=wordsText.value.split('\n').map(w=>w.trim()).filter(w=>w); p.correct_order=correctOrderText.value.split(' ').filter(w=>w) } if(p.item_type==='matching') p.pairs=formPairs.value.filter(x=>x.left.trim()||x.right.trim()); if(editing.value) await api.put(`/admin/items/${editing.value.id}`,p); else await api.post('/admin/items',p); dialog.value=false; await fetchData() } catch(e) { alert(e.response?.data?.detail||'Error') } finally { saving.value=false } }
function confirmDelete(i) { toDelete.value=i; deleteDialog.value=true }
async function doDelete() { await api.delete(`/admin/items/${toDelete.value.id}`); deleteDialog.value=false; await fetchData() }
</script>
