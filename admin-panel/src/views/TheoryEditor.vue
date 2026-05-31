<template>
  <div>
    <div class="d-flex align-center mb-6">
      <div>
        <v-btn variant="text" prepend-icon="mdi-arrow-left" @click="goBack" class="mb-2">{{ backLabel }}</v-btn>
        <h1 class="text-h4">{{ title }}</h1>
      </div>
      <v-spacer />
    </div>

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }} <v-btn size="small" class="ml-2" @click="fetchData">Reintentar</v-btn></v-alert>

    <v-card rounded="lg" elevation="2">
      <v-progress-linear v-if="loading" indeterminate color="primary" />
      <v-card-text v-else>
        <!-- Tabs -->
        <v-tabs v-model="tab" color="primary" class="mb-4">
          <v-tab value="text">📝 Texto</v-tab>
          <v-tab value="sections">📑 Secciones</v-tab>
          <v-tab value="table">📊 Tabla</v-tab>
          <v-tab value="tips">💡 Tips</v-tab>
          <v-tab value="videos">🎬 Videos</v-tab>
        </v-tabs>

        <v-window v-model="tab">
          <!-- Texto -->
          <v-window-item value="text">
            <v-textarea v-model="form.text" label="Texto principal" variant="outlined" rows="6" />
          </v-window-item>

          <!-- Secciones -->
          <v-window-item value="sections">
            <div class="d-flex align-center mb-2">
              <v-spacer />
              <v-btn size="small" variant="outlined" prepend-icon="mdi-plus" @click="addSection">Agregar sección</v-btn>
            </div>
            <v-card v-for="(s, i) in form.sections" :key="i" variant="outlined" class="mb-3 pa-3">
              <div class="d-flex">
                <v-text-field v-model="s.title" label="Título de la sección" variant="outlined" density="compact" class="mr-2" hide-details />
                <v-btn icon="mdi-delete" variant="text" size="small" color="error" @click="form.sections.splice(i,1)" />
              </div>
              <v-textarea v-model="s.text" label="Contenido de la sección" variant="outlined" density="compact" rows="2" class="mt-2" hide-details />
              <div class="d-flex align-center mt-2 flex-wrap">
                <v-text-field v-for="(ex, ei) in (s.examples||[])" :key="ei" v-model="s.examples[ei]" label="Ejemplo" variant="outlined" density="compact" hide-details style="max-width:250px" class="mr-2 mb-1" />
                <v-btn size="x-small" variant="text" icon="mdi-plus" @click="if(!s.examples)s.examples=[];s.examples.push('')" />
              </div>
            </v-card>
            <v-card-text v-if="!form.sections.length" class="text-center text-grey">No hay secciones. Agregá una.</v-card-text>
          </v-window-item>

          <!-- Tabla -->
          <v-window-item value="table">
            <div class="d-flex align-center mb-2">
              <v-btn size="small" variant="outlined" @click="addTableColumn">+ Columna</v-btn>
              <v-btn size="small" variant="outlined" class="ml-2" @click="addTableRow">+ Fila</v-btn>
            </div>
            <v-table v-if="form.table_headers?.length" density="compact">
              <thead><tr><th v-for="(h,hi) in form.table_headers" :key="hi"><v-text-field v-model="form.table_headers[hi]" variant="plain" density="compact" hide-details /></th></tr></thead>
              <tbody><tr v-for="(row,ri) in (form.table_rows||[])" :key="ri"><td v-for="(cell,ci) in (row||[])" :key="ci"><v-text-field v-model="form.table_rows[ri][ci]" variant="plain" density="compact" hide-details /></td></tr></tbody>
            </v-table>
            <v-card-text v-else class="text-center text-grey">No hay tabla. Agregá columnas y filas.</v-card-text>
          </v-window-item>

          <!-- Tips -->
          <v-window-item value="tips">
            <div v-for="(t, i) in (form.tips||[])" :key="i" class="d-flex align-center mb-2">
              <v-text-field v-model="t.emoji" label="Emoji" variant="outlined" density="compact" style="max-width:80px" hide-details class="mr-2" />
              <v-text-field v-model="t.text" label="Tip" variant="outlined" density="compact" hide-details class="mr-2" />
              <v-btn icon="mdi-delete" variant="text" size="small" color="error" @click="form.tips.splice(i,1)" />
            </div>
            <v-btn size="small" variant="outlined" @click="if(!form.tips)form.tips=[];form.tips.push({emoji:'',text:''})">+ Tip</v-btn>
            <v-card-text v-if="!form.tips?.length" class="text-center text-grey">No hay tips. Agregá uno.</v-card-text>
          </v-window-item>

          <!-- Videos -->
          <v-window-item value="videos">
            <div class="d-flex align-center mb-2">
              <v-spacer />
              <v-btn size="small" variant="outlined" prepend-icon="mdi-plus" @click="showVideoForm=true; editingVideo=null; videoForm={title:'',url:'',description:''}">Agregar video</v-btn>
            </div>
            <v-card v-for="v in videos" :key="v.id" variant="outlined" class="mb-3 pa-3">
              <div class="d-flex">
                <div class="flex-grow-1">
                  <div class="font-weight-bold">{{ v.title }}</div>
                  <div class="text-caption text-grey">{{ v.url }}</div>
                </div>
                <v-btn icon="mdi-pencil" variant="text" size="small" color="primary" @click="editingVideo=v; videoForm={...v}; showVideoForm=true" />
                <v-btn icon="mdi-delete" variant="text" size="small" color="error" @click="deleteVideo(v)" />
              </div>
            </v-card>
            <v-card-text v-if="!videos.length" class="text-center text-grey">No hay videos. Agregá uno.</v-card-text>

            <v-dialog v-model="showVideoForm" max-width="500">
              <v-card rounded="lg"><v-card-title>{{ editingVideo?'Editar':'Nuevo' }} Video</v-card-title>
                <v-card-text>
                  <v-text-field v-model="videoForm.title" label="Título" variant="outlined" class="mb-2" />
                  <v-text-field v-model="videoForm.url" label="URL (YouTube/Vimeo)" variant="outlined" class="mb-2" />
                  <v-textarea v-model="videoForm.description" label="Descripción" variant="outlined" rows="2" />
                </v-card-text>
                <v-card-actions><v-spacer /><v-btn variant="text" @click="showVideoForm=false">Cancelar</v-btn><v-btn color="primary" @click="saveVideo">{{ editingVideo?'Guardar':'Agregar' }}</v-btn></v-card-actions>
              </v-card>
            </v-dialog>
          </v-window-item>
        </v-window>

        <v-btn block color="primary" class="mt-4" :loading="saving" @click="save">💾 Guardar teoría</v-btn>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api/client'

const route = useRoute()
const router = useRouter()

const id = route.params.id
const type = route.params.type
const saving = ref(false)
const loading = ref(true)
const error = ref('')
const tab = ref('text')
const videos = ref([])
const showVideoForm = ref(false)
const editingVideo = ref(null)
const videoForm = ref({ title: '', url: '', description: '' })

const title = computed(() => type === 'topic' ? 'Teoría del tema' : 'Teoría de la unidad')

const backRoute = computed(() => {
  const subjectId = route.query.subjectId || route.query.topicId
  if (type === 'topic') {
    return subjectId ? `/subjects/${subjectId}/topics` : null
  }
  const topicId = route.query.topicId
  return topicId ? `/topics/${topicId}/units` : null
})

const backLabel = computed(() => {
  const label = type === 'topic' ? 'Temas' : 'Unidades'
  return `< ${label}`
})

function goBack() {
  if (backRoute.value) router.push(backRoute.value)
  else router.back()
}

const form = ref({ text: '', sections: [], table_headers: [], table_rows: [], tips: [] })

function addSection() { form.value.sections.push({ title: '', text: '', examples: [] }) }
function addTableColumn() { if(!form.value.table_headers) form.value.table_headers=['']; else form.value.table_headers.push(''); (form.value.table_rows||[]).forEach(r => r.push('')) }
function addTableRow() { if(!form.value.table_rows) form.value.table_rows=[]; form.value.table_rows.push((form.value.table_headers||[]).map(()=>'')) }

async function fetchData() {
  loading.value = true; error.value = ''
  try {
    const [theoryRes, videosRes] = await Promise.all([
      api.get(`/admin/${type}s/${id}/theory`),
      api.get(`/admin/topics/${id}/videos`),
    ])
    const d = theoryRes.data
    form.value = { text: d.text||'', sections: d.sections||[], table_headers: d.table_headers||[], table_rows: d.table_rows||[], tips: d.tips||[] }
    videos.value = videosRes.data || []
  } catch (e) {
    error.value = e.response?.data?.detail || 'Error al cargar'
  } finally {
    loading.value = false
  }
}

watch(() => route.params, fetchData, { immediate: true })

async function save() {
  saving.value = true
  try { await api.put(`/admin/${type}s/${id}/theory`, form.value); alert('Guardado') }
  catch (e) { alert('Error: '+(e.response?.data?.detail||e.message)) }
  finally { saving.value = false }
}

async function saveVideo() {
  if (!videoForm.value.title || !videoForm.value.url) return
  try {
    if (editingVideo.value) {
      await api.put(`/admin/videos/${editingVideo.value.id}`, videoForm.value)
    } else {
      await api.post(`/admin/topics/${id}/videos`, videoForm.value)
    }
    showVideoForm.value = false
    const { data } = await api.get(`/admin/topics/${id}/videos`)
    videos.value = data || []
  } catch (e) { alert('Error al guardar video') }
}

async function deleteVideo(v) {
  if (!confirm('Eliminar video?')) return
  await api.delete(`/admin/videos/${v.id}`)
  const { data } = await api.get(`/admin/topics/${id}/videos`)
  videos.value = data || []
}
</script>
