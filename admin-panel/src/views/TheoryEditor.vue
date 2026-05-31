<template>
  <div>
    <div class="d-flex align-center mb-6">
      <div>
        <v-btn variant="text" :to="backRoute" class="mb-2">{{ backLabel }}</v-btn>
        <h1 class="text-h4">{{ title }}</h1>
      </div>
      <v-spacer />
    </div>

    <v-card rounded="lg" elevation="2">
      <v-card-text>
        <v-textarea v-model="form.text" label="Texto principal" variant="outlined" rows="3" class="mb-4" />

        <div class="d-flex align-center mb-2">
          <h3 class="text-h6">Secciones</h3>
          <v-spacer />
          <v-btn size="small" variant="outlined" prepend-icon="mdi-plus" @click="addSection">Agregar</v-btn>
        </div>
        <v-card v-for="(s, i) in form.sections" :key="i" variant="outlined" class="mb-3 pa-3">
          <div class="d-flex">
            <v-text-field v-model="s.title" label="Título" variant="outlined" density="compact" class="mr-2" hide-details />
            <v-btn icon="mdi-delete" variant="text" size="small" color="error" @click="form.sections.splice(i,1)" />
          </div>
          <v-textarea v-model="s.text" label="Texto" variant="outlined" density="compact" rows="2" class="mt-2" hide-details />
          <div class="d-flex align-center mt-2">
            <v-text-field v-for="(ex, ei) in (s.examples||[])" :key="ei" v-model="s.examples[ei]" label="Ejemplo" variant="outlined" density="compact" hide-details class="mr-2" />
            <v-btn size="x-small" variant="text" icon="mdi-plus" @click="if(!s.examples)s.examples=[];s.examples.push('')" />
          </div>
        </v-card>

        <h3 class="text-h6 mt-4 mb-2">Tabla resumen</h3>
        <div class="d-flex align-center mb-2">
          <v-btn size="small" variant="outlined" @click="addTableColumn">+ Columna</v-btn>
          <v-btn size="small" variant="outlined" class="ml-2" @click="addTableRow">+ Fila</v-btn>
        </div>
        <v-table v-if="form.table_headers?.length" density="compact">
          <thead>
            <tr>
              <th v-for="(h,hi) in form.table_headers" :key="hi">
                <v-text-field v-model="form.table_headers[hi]" variant="plain" density="compact" hide-details />
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, ri) in (form.table_rows||[])" :key="ri">
              <td v-for="(cell, ci) in (row||[])" :key="ci">
                <v-text-field v-model="form.table_rows[ri][ci]" variant="plain" density="compact" hide-details />
              </td>
            </tr>
          </tbody>
        </v-table>

        <h3 class="text-h6 mt-4 mb-2">Tips</h3>
        <div v-for="(t, i) in (form.tips||[])" :key="i" class="d-flex align-center mb-2">
          <v-text-field v-model="t.emoji" label="Emoji" variant="outlined" density="compact" style="max-width:80px" hide-details class="mr-2" />
          <v-text-field v-model="t.text" label="Tip" variant="outlined" density="compact" hide-details class="mr-2" />
          <v-btn icon="mdi-delete" variant="text" size="small" color="error" @click="form.tips.splice(i,1)" />
        </div>
        <v-btn size="small" variant="outlined" @click="if(!form.tips)form.tips=[];form.tips.push({emoji:'',text:''})">+ Tip</v-btn>

        <v-btn block color="primary" class="mt-6" :loading="saving" @click="save">Guardar teoría</v-btn>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'

const route = useRoute()
const props = defineProps({ type: String, id: String })

const id = route.params.id || props.id
const type = route.params.type || props.type
const saving = ref(false)
const title = computed(() => type === 'topic' ? 'Teoría del tema' : 'Teoría de la unidad')
const backRoute = computed(() => type === 'topic' ? `/subjects/${route.query.subjectId}/topics` : `/topics/${route.query.topicId}/units`)
const backLabel = computed(() => type === 'topic' ? 'Temas' : 'Unidades')
const endpoint = computed(() => `/admin/${type}s/${id}/theory`)

const form = ref({ text: '', sections: [], table_headers: [], table_rows: [], tips: [] })

function addSection() { form.value.sections.push({ title: '', text: '', examples: [] }) }
function addTableColumn() { if(!form.value.table_headers) form.value.table_headers=['']; else form.value.table_headers.push(''); (form.value.table_rows||[]).forEach(r => r.push('')) }
function addTableRow() { if(!form.value.table_rows) form.value.table_rows=[]; form.value.table_rows.push((form.value.table_headers||[]).map(()=>'')) }

onMounted(async () => {
  try {
    const { data } = await api.get(endpoint.value)
    form.value = { text: data.text||'', sections: data.sections||[], table_headers: data.table_headers||[], table_rows: data.table_rows||[], tips: data.tips||[] }
  } catch {}
})

async function save() {
  saving.value = true
  try { await api.put(endpoint.value, form.value); alert('Guardado') }
  catch (e) { alert('Error: '+(e.response?.data?.detail||e.message)) }
  finally { saving.value = false }
}
</script>
