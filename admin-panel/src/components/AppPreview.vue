<template>
  <div class="app-preview-container">
    <MobilePreview :html="currentViewHtml" />
    <div class="mt-2 text-center">
      <v-btn-group density="compact" variant="text">
        <v-btn size="x-small" :color="navStack.length===1?'primary':'grey'" @click="goHome">🏠 Inicio</v-btn>
        <v-btn size="x-small" color="primary" :disabled="navStack.length<=1" @click="goBack">← Volver</v-btn>
      </v-btn-group>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import api from '../api/client'
import MobilePreview from './MobilePreview.vue'

const navStack = ref([{ view: 'subjects', id: '' }])
const subjectsData = ref([])
const topicsData = ref([])
const unitsData = ref([])
const blocksData = ref([])
const previewKey = ref(0)

const currentView = computed(() => navStack.value[navStack.value.length - 1])

const currentViewHtml = computed(() => {
  const v = currentView.value
  if (v.view === 'subjects') return renderSubjects()
  if (v.view === 'topics') return renderTopics()
  if (v.view === 'units') return renderUnits()
  if (v.view === 'blocks') return renderBlocks()
  return '<div class="pp-empty">Cargando...</div>'
})

function renderSubjects() {
  if (!subjectsData.value.length) return '<div class="pp-empty">No hay materias</div>'
  return subjectsData.value.map((s, i) => `
    <div class="pp-card" data-idx="${i}" data-view="subjects">
      <div class="pp-icon">${s.icon||'📚'}</div>
      <div class="pp-body"><div class="pp-title">${s.name}</div><div class="pp-sub">${s.topicsCount||0} temas</div></div>
      <div class="pp-arrow">→</div>
    </div>
  `).join('')
}

function renderTopics() {
  if (!topicsData.value.length) return '<div class="pp-empty">No hay temas</div>'
  return topicsData.value.map((t, i) => `
    <div class="pp-card" data-idx="${i}" data-view="topics">
      <div class="pp-icon">${t.icon||'📝'}</div>
      <div class="pp-body">
        <div class="pp-title">${t.name}</div>
        <div class="pp-sub">Dificultad: ${'★'.repeat(t.difficulty||1)}</div>
        <div class="pp-bar"><div class="pp-bar-fill" style="width:${(t.progress?.percentComplete||0)}%"></div></div>
      </div>
      <div class="pp-arrow">→</div>
    </div>
  `).join('')
}

function renderUnits() {
  if (!unitsData.value.length) return '<div class="pp-empty">No hay unidades</div>'
  const icons = ['✏️','❌','❓','✅']
  return unitsData.value.map((u, i) => {
    const locked = u.is_locked
    return `<div class="pp-card" data-idx="${i}" data-view="units">
      <div class="pp-icon">${locked?'🔒':icons[i%icons.length]}</div>
      <div class="pp-body">
        <div class="pp-title" style="color:${locked?'#999':'#333'}">${u.title||u.id}</div>
        <div class="pp-sub">${u.input_mode==='tap'?'🖐️ Tap':'⌨️ Type'}</div>
        <div class="pp-bar"><div class="pp-bar-fill" style="width:0%"></div></div>
      </div>
      <div class="pp-arrow" style="color:${locked?'#999':'#4CAF50'}">${locked?'🔒':'→'}</div>
    </div>`
  }).join('')
}

function renderBlocks() {
  if (!blocksData.value.length) return '<div class="pp-empty">No hay bloques</div>'
  return blocksData.value.map(b => `
    <div class="pp-card" data-view="blocks">
      <div class="pp-body"><div class="pp-title" style="color:#4CAF50">📋 ${b.title}</div></div>
    </div>
  `).join('')
}

function goHome() { navStack.value = [{ view: 'subjects', id: '' }] }
function goBack() { if (navStack.value.length > 1) navStack.value.pop() }

async function loadSubjects() { const { data } = await api.get('/content/subjects'); subjectsData.value = data.subjects || [] }

watch(navStack, async () => {
  const v = currentView.value
  if (v.view === 'subjects') await loadSubjects()
  if (v.view === 'topics' && v.id) { const { data } = await api.get(`/admin/subjects/${v.id}/topics`); topicsData.value = data || [] }
  if (v.view === 'units' && v.id) { const { data } = await api.get(`/admin/topics/${v.id}/units`); unitsData.value = data || [] }
  if (v.view === 'blocks' && v.id) { const { data } = await api.get(`/admin/units/${v.id}/blocks`); blocksData.value = data || [] }
}, { immediate: true, deep: true })

// Handle clicks on rendered HTML
function handleCardClick(e) {
  const card = e.target.closest('.pp-card')
  if (!card) return
  const view = card.dataset.view
  const idx = parseInt(card.dataset.idx || '0')
  const v = currentView.value
  if (view === 'subjects' && v.view === 'subjects') { const s = subjectsData.value[idx]; if (s) navStack.value.push({ view: 'topics', id: s.id }) }
  if (view === 'topics' && v.view === 'topics') { const t = topicsData.value[idx]; if (t) navStack.value.push({ view: 'units', id: t.id }) }
  if (view === 'units' && v.view === 'units') { const u = unitsData.value[idx]; if (u) navStack.value.push({ view: 'blocks', id: u.id }) }
}

onMounted(() => {
  document.addEventListener('click', handleCardClick)
})
</script>

<style scoped>
.app-preview-container { display: flex; flex-direction: column; align-items: center; }
.pp-card { display: flex; align-items: center; gap: 12px; padding: 14px 12px; border-bottom: 1px solid #f0f0f0; cursor: pointer; }
.pp-icon { font-size: 28px; flex-shrink: 0; }
.pp-body { flex: 1; min-width: 0; }
.pp-title { font-weight: bold; font-size: 14px; color: #333; }
.pp-sub { font-size: 11px; color: #999; margin-top: 2px; }
.pp-bar { height: 6px; border-radius: 3px; background: #e0e0e0; margin-top: 4px; overflow: hidden; }
.pp-bar-fill { height: 100%; border-radius: 3px; background: #4CAF50; }
.pp-arrow { font-size: 18px; color: #4CAF50; flex-shrink: 0; }
.pp-empty { text-align: center; color: #999; padding: 20px; font-size: 14px; }
</style>
