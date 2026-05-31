<template>
  <div>
    <div class="d-flex align-center mb-6">
      <div>
        <v-btn variant="text" prepend-icon="mdi-arrow-left" @click="goBack" class="mb-2">{{ backLabel }}</v-btn>
        <h1 class="text-h4">{{ title }}</h1>
      </div>
      <v-spacer />
      <v-btn color="primary" prepend-icon="mdi-content-save" :loading="saving" @click="save">Guardar</v-btn>
    </div>

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{ error }} <v-btn size="small" class="ml-2" @click="fetchData">Reintentar</v-btn></v-alert>

    <v-row v-if="!loading">
      <v-col cols="8">
        <v-card rounded="lg" elevation="2">
          <v-toolbar color="transparent" density="compact">
            <v-tabs v-model="currentTab" color="primary">
              <v-tab v-for="(b,i) in blocks" :key="i" :value="i">
                {{ b.title || 'Bloque '+(i+1) }}
                <v-btn v-if="blocks.length>1" icon="mdi-close" variant="text" size="x-small" class="ml-1" @click.stop="removeBlock(i)" />
              </v-tab>
              <v-tab value="new" @click="addBlock"><v-icon>mdi-plus</v-icon></v-tab>
            </v-tabs>
          </v-toolbar>
          <v-card-text>
            <v-text-field v-if="currentBlock" v-model="currentBlock.title" label="Título del bloque" variant="outlined" class="mb-3" density="compact" hide-details />
            <RichTextEditor v-if="currentBlock" v-model="currentBlock.html" />
            <v-card-text v-else class="text-center text-grey py-8">
              Agregá un bloque de teoría con el botón <v-icon>mdi-plus</v-icon> de arriba.
            </v-card-text>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="4">
        <MobilePreview :html="previewHtml" />
      </v-col>
    </v-row>

    <v-card v-else rounded="lg"><v-progress-linear indeterminate color="primary" /><v-card-text class="text-center py-8 text-grey">Cargando...</v-card-text></v-card>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api/client'
import RichTextEditor from '../components/RichTextEditor.vue'
import MobilePreview from '../components/MobilePreview.vue'

const route = useRoute()
const router = useRouter()

const id = route.params.id
const type = route.params.type
const saving = ref(false)
const loading = ref(true)
const error = ref('')
const currentTab = ref(0)
const blocks = ref([{ title: '', html: '' }])

const currentBlock = computed(() => blocks.value[currentTab.value] || null)

const title = computed(() => type === 'topic' ? 'Teoría del tema' : 'Teoría de la unidad')

const backRoute = computed(() => {
  const sid = route.query.subjectId || route.query.topicId
  if (type === 'topic') return sid ? `/subjects/${sid}/topics` : null
  return route.query.topicId ? `/topics/${route.query.topicId}/units` : null
})

const backLabel = computed(() => type === 'topic' ? '< Temas' : '< Unidades')

const previewHtml = computed(() => {
  return blocks.value.map(b => {
    const title = b.title ? `<h2>${b.title}</h2>` : ''
    return `${title}${b.html || ''}`
  }).join('<hr style="margin:12px 0;border:none;border-top:1px dashed #ccc">')
})

function goBack() {
  if (backRoute.value) router.push(backRoute.value)
  else router.back()
}

function addBlock() {
  blocks.value.push({ title: '', html: '' })
  currentTab.value = blocks.value.length - 1
}

function removeBlock(i) {
  blocks.value.splice(i, 1)
  if (currentTab.value >= blocks.value.length) currentTab.value = Math.max(0, blocks.value.length - 1)
}

async function fetchData() {
  loading.value = true; error.value = ''
  try {
    const { data } = await api.get(`/admin/${type}s/${id}/theory`)
    if (data.blocks && data.blocks.length) {
      blocks.value = data.blocks
    } else if (data.text) {
      blocks.value = [{ title: '', html: data.text.replace(/\n/g, '<br>') }]
    } else {
      blocks.value = [{ title: '', html: '' }]
    }
    currentTab.value = 0
  } catch (e) {
    error.value = e.response?.data?.detail || 'Error al cargar'
  } finally {
    loading.value = false
  }
}

watch(() => route.params, fetchData, { immediate: true })

async function save() {
  saving.value = true
  try {
    const payload = { blocks: blocks.value }
    await api.put(`/admin/${type}s/${id}/theory`, payload)
    alert('Guardado')
  } catch (e) {
    alert('Error: ' + (e.response?.data?.detail || e.message))
  } finally {
    saving.value = false
  }
}
</script>
