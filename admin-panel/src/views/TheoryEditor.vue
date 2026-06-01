<template>
  <div>
    <div class="d-flex align-center mb-4">
      <div>
        <v-btn variant="text" prepend-icon="mdi-arrow-left" @click="goBack" class="mb-1">{{ backLabel }}</v-btn>
        <h1 class="text-h5">{{ title }}</h1>
      </div>
      <v-spacer />
      <v-chip v-if="lastSaved" size="small" variant="tonal" color="success" class="mr-2">
        <v-icon start size="14">mdi-check</v-icon>
        Guardado {{ lastSaved }}
      </v-chip>
      <v-btn color="primary" prepend-icon="mdi-content-save" :loading="saving" @click="saveManual">Guardar</v-btn>
    </div>

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4" closable @click:close="error = ''">
      {{ error }}
      <v-btn size="small" class="ml-2" @click="fetchData">Reintentar</v-btn>
    </v-alert>

    <v-row v-if="!loading">
      <v-col cols="12" md="8">
        <v-card rounded="lg" elevation="2">
          <div class="pa-3 border-b d-flex align-center">
            <span class="text-overline mr-4">Bloques de teoría ({{ blocks.length }})</span>
            <v-btn size="x-small" variant="tonal" prepend-icon="mdi-plus" color="success" @click="addBlock">
              Agregar bloque
            </v-btn>
          </div>

          <div v-if="blocks.length === 0" class="text-center text-grey py-8">
            <v-icon size="48" class="mb-2">mdi-book-open-page-variant-outline</v-icon>
            <p>Agregá un bloque de teoría</p>
          </div>

          <v-expansion-panels v-model="openPanels" multiple variant="accordion">
            <v-expansion-panel v-for="(block, i) in blocks" :key="i">
              <v-expansion-panel-title>
                <div class="d-flex align-center flex-grow-1">
                  <span class="text-grey text-caption mr-2">#{{ i + 1 }}</span>
                  <v-text-field
                    v-model="block.title"
                    variant="plain"
                    density="compact"
                    hide-details
                    :placeholder="`Título sección ${i + 1}`"
                    class="flex-grow-1"
                    style="max-width: 300px"
                  />
                  <v-chip size="x-small" variant="tonal" class="ml-2">
                    {{ block.html ? block.html.replace(/<[^>]*>/g, '').length : 0 }} chars
                  </v-chip>
                  <v-btn icon="mdi-close" variant="text" size="x-small" color="grey" @click.stop="confirmRemoveBlock(i)" class="ml-1" />
                </div>
              </v-expansion-panel-title>
              <v-expansion-panel-text>
                <div class="pt-2">
                  <RichTextEditor v-model="block.html" :globalTips="globalTips" @update:globalTips="v => globalTips = v" />
                </div>
              </v-expansion-panel-text>
            </v-expansion-panel>
          </v-expansion-panels>
        </v-card>
      </v-col>

      <v-col cols="12" md="4">
        <div class="sticky-top" style="top: 80px">
          <h3 class="text-subtitle-1 font-weight-medium mb-2">Vista previa</h3>
          <MobilePreview :html="previewHtml" />
        </div>
      </v-col>
    </v-row>

    <v-card v-else rounded="lg">
      <v-progress-linear indeterminate color="primary" />
      <v-card-text class="text-center py-8 text-grey">Cargando...</v-card-text>
    </v-card>

    <v-dialog v-model="removeDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title class="text-error">
          <v-icon class="mr-2">mdi-alert</v-icon>
          Eliminar bloque
        </v-card-title>
        <v-card-text>
          ¿Eliminar el bloque "<b>{{ blocks[blockToRemove]?.title || `Bloque ${blockToRemove + 1}` }}</b>"?<br />
          El contenido se perderá.
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="removeDialog = false">Cancelar</v-btn>
          <v-btn color="error" @click="doRemoveBlock">Eliminar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, inject } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api/client'
import RichTextEditor from '../components/RichTextEditor.vue'
import MobilePreview from '../components/MobilePreview.vue'

const route = useRoute()
const router = useRouter()
const snackbar = inject('snackbar')

const id = route.params.id
const type = route.params.type

const saving = ref(false)
const loading = ref(true)
const error = ref('')
const openPanels = ref([])
const blocks = ref([{ title: '', html: '', tips: [] }])
const globalTips = ref([])

const removeDialog = ref(false)
const blockToRemove = ref(0)
const lastSaved = ref(null)
let autosaveTimer = null

const currentBlock = computed(() => blocks.value[openPanels.value[0]] || null)

const title = computed(() => {
  if (type === 'topic') return 'Teoría del tema'
  if (type === 'unit') return 'Teoría de la unidad'
  return 'Teoría del bloque'
})

const backRoute = computed(() => {
  if (type === 'topic') {
    const sid = route.query.subjectId || route.query.topicId
    return sid ? `/subjects/${sid}/topics` : null
  }
  if (type === 'unit') {
    return route.query.topicId ? `/topics/${route.query.topicId}/units` : null
  }
  return route.query.unitId ? `/units/${route.query.unitId}/blocks` : null
})

const backLabel = computed(() => {
  if (type === 'topic') return 'Temas'
  if (type === 'unit') return 'Unidades'
  return 'Bloques'
})

const previewHtml = computed(() => {
  const header = '<div style="background:#4CAF50;color:white;padding:12px 8px;font-weight:bold;font-size:14px;flex-shrink:0;display:flex;align-items:center"><div style="font-size:18px;margin-right:8px">←</div><div style="flex:1;text-align:center;margin-right:24px">Teoría</div></div>'
  const bottomNav = '<div style="display:flex;justify-content:space-around;background:white;border-top:1px solid #e0e0e0;padding:8px 0 6px 0;flex-shrink:0"><div style="text-align:center;flex:1"><div style="font-size:18px">🏠</div><div style="font-size:10px;color:#757575;margin-top:2px">Inicio</div></div><div style="text-align:center;flex:1"><div style="font-size:18px">📖</div><div style="font-size:10px;color:#757575;margin-top:2px">Diccionario</div></div><div style="text-align:center;flex:1"><div style="font-size:18px">📊</div><div style="font-size:10px;color:#757575;margin-top:2px">Progreso</div></div><div style="text-align:center;flex:1"><div style="font-size:18px">⚙️</div><div style="font-size:10px;color:#757575;margin-top:2px">Ajustes</div></div></div>'

  if (!blocks.value.length) {
    return `<div style="display:flex;flex-direction:column;height:100%">${header}<div style="flex:1;display:flex;align-items:center;justify-content:center;background:#f5f5f5;color:#757575;font-size:14px;padding:20px">Agregá un bloque de teoría</div>${bottomNav}</div>`
  }

  let content = blocks.value.map(b => {
    const title = b.title ? `<div style="font-size:18px;font-weight:bold;color:#4CAF50;margin-bottom:12px;padding-top:8px">${b.title}</div>` : ''
    const text = b.html ? b.html.replace(/<[^>]*>/g, '') : ''
    return `<div style="margin:0 12px 16px 12px;background:white;border-radius:16px;padding:20px;box-shadow:0 1px 2px rgba(0,0,0,0.3), 0 1px 3px 1px rgba(0,0,0,0.15)">${title}<div style="font-size:15px;color:#333;line-height:1.6">${text || '<span style="color:#999">Sin contenido</span>'}</div></div>`
  }).join('<div style="height:8px"></div>')

  return `<div style="display:flex;flex-direction:column;height:100%">${header}<div style="flex:1;overflow-y:auto;background:#f5f5f5;padding:12px 0">${content}</div>${bottomNav}</div>`
})

function goBack() {
  if (backRoute.value) router.push(backRoute.value)
  else router.back()
}

function addBlock() {
  blocks.value.push({ title: '', html: '', tips: [] })
  openPanels.value = [blocks.value.length - 1]
}

function confirmRemoveBlock(i) {
  blockToRemove.value = i
  removeDialog.value = true
}

function doRemoveBlock() {
  blocks.value.splice(blockToRemove.value, 1)
  if (blocks.value.length === 0) {
    blocks.value.push({ title: '', html: '', tips: [] })
  }
  openPanels.value = [Math.min(blockToRemove.value, blocks.value.length - 1)]
  removeDialog.value = false
}

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const endpoint = type === 'block' ? `/admin/blocks/${id}/theory` : `/admin/${type}s/${id}/theory`
    const { data } = await api.get(endpoint)
    if (data.blocks && data.blocks.length) {
      blocks.value = data.blocks.map(b => ({
        ...b,
        tips: b.tips || []
      }))
      globalTips.value = data.tips || []
      openPanels.value = [0]
    } else if (data.text) {
      blocks.value = [{ title: '', html: data.text.replace(/\n/g, '<br>'), tips: [] }]
      globalTips.value = []
      openPanels.value = [0]
    } else {
      blocks.value = [{ title: '', html: '', tips: [] }]
      globalTips.value = []
      openPanels.value = []
    }
  } catch (e) {
    error.value = e.response?.data?.detail || 'Error al cargar'
  } finally {
    loading.value = false
  }
}

function scheduleAutosave() {
  if (autosaveTimer) clearTimeout(autosaveTimer)
  autosaveTimer = setTimeout(() => {
    save(true)
  }, 30000)
}

async function save(silent = false) {
  if (!silent) saving.value = true
  try {
    const payload = { blocks: blocks.value, tips: globalTips.value }
    const endpoint = type === 'block' ? `/admin/blocks/${id}/theory` : `/admin/${type}s/${id}/theory`
    await api.put(endpoint, payload)

    lastSaved.value = new Date().toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' })
    if (!silent) snackbar.success('Guardado correctamente')
  } catch (e) {
    if (!silent) snackbar.error(e.response?.data?.detail || 'Error al guardar')
  } finally {
    if (!silent) saving.value = false
  }
}

async function saveManual() {
  await save(false)
}

onMounted(() => {
  fetchData()
  scheduleAutosave()
})

onUnmounted(() => {
  if (autosaveTimer) clearTimeout(autosaveTimer)
})

watch(blocks, () => {
  scheduleAutosave()
}, { deep: true })

watch(() => route.params, fetchData, { immediate: true })
</script>