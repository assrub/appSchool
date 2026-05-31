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
      <v-col cols="8">
        <v-card rounded="lg" elevation="2">
          <v-tabs v-model="currentTab" color="primary" slider-color="primary">
            <v-tab v-for="(b, i) in blocks" :key="i" :value="i" class="text-lowercase">
              {{ b.title || `Sección ${i + 1}` }}
              <v-btn
                v-if="blocks.length > 1"
                icon="mdi-close"
                variant="text"
                size="x-small"
                class="ml-1"
                @click.stop="confirmRemoveBlock(i)"
              />
            </v-tab>
            <v-tab value="add" @click="addBlock">
              <v-icon>mdi-plus</v-icon>
            </v-tab>
          </v-tabs>

          <v-card-text v-if="currentBlock" class="pt-4">
            <v-text-field
              v-model="currentBlock.title"
              label="Título de la sección"
              variant="outlined"
              class="mb-4"
              density="comfortable"
              hide-details
              :placeholder="`Sección ${currentTab + 1}`"
            />

            <RichTextEditor v-model="currentBlock.html" />

            <v-divider class="my-4" />

            <div class="d-flex align-center mb-2">
              <v-icon size="20" class="mr-2">mdi-lightbulb-outline</v-icon>
              <span class="text-subtitle-2 font-weight-medium">Tips y consejos</span>
            </div>

            <div v-for="(tip, i) in currentBlock.tips" :key="i" class="mb-2">
              <v-text-field
                v-model="currentBlock.tips[i]"
                variant="outlined"
                density="compact"
                hide-details
                placeholder="Escribí un tip..."
                class="mb-1"
              >
                <template #prepend-inner>
                  <v-icon size="small" color="warning">mdi-alert</v-icon>
                </template>
                <template #append-inner>
                  <v-btn
                    icon="mdi-close"
                    variant="text"
                    size="x-small"
                    color="grey"
                    @click="removeTip(i)"
                  />
                </template>
              </v-text-field>
            </div>
            <v-btn
              variant="text"
              size="small"
              prepend-icon="mdi-plus"
              @click="addTip"
              class="mt-1"
            >
              Agregar tip
            </v-btn>
          </v-card-text>

          <v-card-text v-else class="text-center text-grey py-8">
            <v-icon size="48" class="mb-2">mdi-book-open-page-variant-outline</v-icon>
            <p>Agregá una sección de teoría con el botón + de arriba</p>
          </v-card-text>
        </v-card>

        <v-card v-if="type === 'topic'" rounded="lg" elevation="2" class="mt-4">
          <v-card-title class="d-flex align-center">
            <v-icon class="mr-2">mdi-video</v-icon>
            Videos
          </v-card-title>
          <v-card-text>
            <v-row v-for="(v, i) in videos" :key="i" class="mb-3" align="center">
              <v-col cols="5">
                <v-text-field
                  v-model="v.title"
                  label="Título"
                  variant="outlined"
                  density="compact"
                  hide-details
                />
              </v-col>
              <v-col cols="5">
                <v-text-field
                  v-model="v.url"
                  label="URL de YouTube"
                  variant="outlined"
                  density="compact"
                  hide-details
                  placeholder="https://youtube.com/..."
                />
              </v-col>
              <v-col cols="2">
                <v-btn
                  icon="mdi-delete"
                  variant="text"
                  color="error"
                  @click="removeVideo(i)"
                />
              </v-col>
            </v-row>
            <v-btn
              variant="tonal"
              prepend-icon="mdi-plus"
              @click="addVideo"
            >
              Agregar video
            </v-btn>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="4">
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
          Eliminar sección
        </v-card-title>
        <v-card-text>
          ¿Eliminar "<b>{{ blocks[blockToRemove]?.title || `Sección ${blockToRemove + 1}` }}</b>"?<br />
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
const currentTab = ref(0)
const blocks = ref([{ title: '', html: '', tips: [] }])
const videos = ref([])

const removeDialog = ref(false)
const blockToRemove = ref(0)
const lastSaved = ref(null)
let autosaveTimer = null

const currentBlock = computed(() => blocks.value[currentTab.value] || null)

const title = computed(() => type === 'topic' ? 'Teoría del tema' : 'Teoría de la unidad')

const backRoute = computed(() => {
  const sid = route.query.subjectId || route.query.topicId
  if (type === 'topic') return sid ? `/subjects/${sid}/topics` : null
  return route.query.topicId ? `/topics/${route.query.topicId}/units` : null
})

const backLabel = computed(() => type === 'topic' ? 'Temas' : 'Unidades')

const previewHtml = computed(() => {
  return blocks.value.map(b => {
    const title = b.title ? `<h2 style="color:#333;font-size:18px;margin-bottom:8px">${b.title}</h2>` : ''
    const tips = b.tips?.length
      ? `<div style="background:#FFF3E0;border-radius:8px;padding:8px 12px;margin-top:8px">${b.tips.map(t => `<div style="display:flex;align-items:center;gap:6px;margin-bottom:4px"><span style="font-size:14px">💡</span><span style="font-size:13px">${t}</span></div>`).join('')}</div>`
      : ''
    return `${title}${b.html || ''}${tips}`
  }).join('<hr style="margin:16px 0;border:none;border-top:1px dashed #ccc">')
})

function goBack() {
  if (backRoute.value) router.push(backRoute.value)
  else router.back()
}

function addBlock() {
  blocks.value.push({ title: '', html: '', tips: [] })
  currentTab.value = blocks.value.length - 1
}

function confirmRemoveBlock(i) {
  blockToRemove.value = i
  removeDialog.value = true
}

function doRemoveBlock() {
  blocks.value.splice(blockToRemove.value, 1)
  if (currentTab.value >= blocks.value.length) {
    currentTab.value = Math.max(0, blocks.value.length - 1)
  }
  removeDialog.value = false
}

function addTip() {
  if (!currentBlock.value.tips) currentBlock.value.tips = []
  currentBlock.value.tips.push('')
}

function removeTip(i) {
  currentBlock.value.tips.splice(i, 1)
}

function addVideo() {
  videos.value.push({ title: '', url: '', sort_order: videos.value.length })
}

function removeVideo(i) {
  videos.value.splice(i, 1)
}

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await api.get(`/admin/${type}s/${id}/theory`)
    if (data.blocks && data.blocks.length) {
      blocks.value = data.blocks.map(b => ({
        ...b,
        tips: b.tips || []
      }))
    } else if (data.text) {
      blocks.value = [{ title: '', html: data.text.replace(/\n/g, '<br>'), tips: [] }]
    } else {
      blocks.value = [{ title: '', html: '', tips: [] }]
    }

    if (type === 'topic') {
      try {
        const v = await api.get(`/admin/topics/${id}/videos`)
        videos.value = v.data || []
      } catch {
        videos.value = []
      }
    }

    currentTab.value = 0
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
    const payload = { blocks: blocks.value }
    await api.put(`/admin/${type}s/${id}/theory`, payload)

    if (type === 'topic' && videos.value.length) {
      for (const v of videos.value) {
        if (v.id) {
          await api.put(`/admin/videos/${v.id}`, v)
        } else if (v.title || v.url) {
          await api.post(`/admin/topics/${id}/videos`, v)
        }
      }
    }

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