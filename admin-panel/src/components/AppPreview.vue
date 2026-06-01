<template>
  <div class="app-preview-container">
    <div class="phone-frame">
      <div class="phone-notch" />
      <div class="phone-screen">
        <div class="screen-header">
          <div class="header-back" @click="goBack" v-if="navStack.length > 1">←</div>
          <div class="header-title">{{ headerTitle }}</div>
        </div>
        <div class="screen-tabs" v-if="showTabs">
          <div class="tab" :class="{ active: selectedTab === 0 }" @click="selectedTab = 0">{{ tab1 }}</div>
          <div class="tab" :class="{ active: selectedTab === 1 }" @click="selectedTab = 1" v-if="showTab2">{{ tab2 }}</div>
        </div>
        <div class="screen-content" v-html="currentViewHtml" />
        <div class="bottom-nav">
          <div class="nav-item" :class="{ active: true }">
            <div class="nav-icon">🏠</div>
            <div class="nav-label">Inicio</div>
          </div>
          <div class="nav-item">
            <div class="nav-icon">📖</div>
            <div class="nav-label">Diccionario</div>
          </div>
          <div class="nav-item">
            <div class="nav-icon">📊</div>
            <div class="nav-label">Progreso</div>
          </div>
          <div class="nav-item">
            <div class="nav-icon">⚙️</div>
            <div class="nav-label">Ajustes</div>
          </div>
        </div>
      </div>
      <div class="phone-home" />
    </div>
    <div class="preview-nav-controls mt-2">
      <v-btn-group density="compact" variant="text">
        <v-btn size="x-small" color="primary" @click="goHome">🏠</v-btn>
        <v-btn size="x-small" color="primary" :disabled="navStack.length <= 1" @click="goBack">←</v-btn>
      </v-btn-group>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import api from '../api/client'

const navStack = ref([{ view: 'subjects', id: '' }])
const subjectsData = ref([])
const topicsData = ref([])
const unitsData = ref([])
const blocksData = ref([])
const selectedTab = ref(0)

const currentView = computed(() => navStack.value[navStack.value.length - 1])

const headerTitle = computed(() => {
  const v = currentView.value
  if (v.view === 'subjects') return 'AppEnglish'
  if (v.view === 'topics') return topicsData.value.find(t => t.id === v.id)?.name || 'Temas'
  if (v.view === 'units') return unitsData.value.find(u => u.id === v.id)?.title || unitsData.value[0]?.title || 'Unidades'
  if (v.view === 'blocks') return blocksData.value.find(b => b.id === v.id)?.title || blocksData.value[0]?.title || 'Bloques'
  return 'AppEnglish'
})

const showTabs = computed(() => {
  const v = currentView.value
  return v.view === 'units' || v.view === 'blocks'
})

const showTab2 = computed(() => {
  const v = currentView.value
  if (v.view === 'units') return unitsData.value.some(u => u.topicTheory)
  if (v.view === 'blocks') return blocksData.value.some(b => b.unitTheory)
  return false
})

const tab1 = computed(() => {
  const v = currentView.value
  if (v.view === 'units') return 'UNIDADES'
  if (v.view === 'blocks') return 'BLOQUES'
  return ''
})

const tab2 = computed(() => 'TEORÍA')

const currentViewHtml = computed(() => {
  const v = currentView.value
  if (v.view === 'subjects') return renderSubjects()
  if (v.view === 'topics') return renderTopics()
  if (v.view === 'units') return selectedTab.value === 0 ? renderUnits() : renderUnitsTheory()
  if (v.view === 'blocks') return selectedTab.value === 0 ? renderBlocks() : renderBlocksTheory()
  return '<div style="text-align:center;color:#999;padding:40px 20px">Cargando...</div>'
})

function renderSubjects() {
  if (!subjectsData.value.length) return '<div style="text-align:center;color:#999;padding:40px 20px;font-size:14px">No hay materias</div>'
  let html = '<div style="padding:16px 16px 8px 16px"><div style="font-size:20px;font-weight:bold;color:#4CAF50">MATERIAS</div></div>'
  html += subjectsData.value.map((s, i) => `
    <div class="card" data-idx="${i}" data-view="subjects" style="margin:0 12px 12px 12px">
      <div style="display:flex;align-items:center;padding:20px">
        <div style="font-size:28px;margin-right:16px">${s.icon||''}</div>
        <div style="flex:1;min-width:0">
          <div style="font-size:22px;font-weight:bold;color:#212121;line-height:1.2">${s.name}</div>
          <div style="font-size:14px;color:#757575;margin-top:4px">${s.topicsCount||0} temas</div>
        </div>
        <div style="font-size:24px;color:#4CAF50">→</div>
      </div>
    </div>
  `).join('')
  return html
}

function renderTopics() {
  if (!topicsData.value.length) return '<div style="text-align:center;color:#999;padding:40px 20px;font-size:14px">No hay temas</div>'
  let html = '<div style="padding:16px 16px 8px 16px"><div style="font-size:20px;font-weight:bold;color:#4CAF50">TEMAS</div></div>'
  html += topicsData.value.map((t, i) => {
    const stars = '★'.repeat(t.difficulty || 1) + '☆'.repeat(5 - (t.difficulty || 1))
    return `<div class="card" data-idx="${i}" data-view="topics" style="margin:0 12px 12px 12px">
      <div style="display:flex;align-items:center;padding:16px">
        <div style="font-size:28px;margin-right:14px">${t.icon||''}</div>
        <div style="flex:1;min-width:0">
          <div style="font-size:18px;font-weight:bold;color:#212121;line-height:1.2">${t.name}</div>
          <div style="font-size:12px;color:#FFA726;margin-top:4px">${stars}</div>
          <div style="height:6px;border-radius:3px;background:#e0e0e0;margin-top:6px;overflow:hidden">
            <div style="width:${t.progress?.percentComplete||0}%;height:100%;border-radius:3px;background:#4CAF50"></div>
          </div>
          <div style="font-size:11px;color:#757575;margin-top:4px">${t.completedUnits||0}/${t.totalUnits||0} unidades</div>
        </div>
        ${t.isLocked ? '<div style="font-size:20px">🔒</div>' : '<div style="font-size:24px;color:#4CAF50">→</div>'}
      </div>
    </div>`
  }).join('')
  return html
}

function renderUnits() {
  if (!unitsData.value.length) return '<div style="text-align:center;color:#999;padding:40px 20px;font-size:14px">No hay unidades</div>'
  let html = '<div style="padding:8px 0"></div>'
  html += unitsData.value.map((u, i) => {
    const locked = u.is_locked
    return `<div class="card" data-idx="${i}" data-view="units" style="margin:0 12px 12px 12px;${locked ? 'opacity:0.5' : ''}">
      <div style="display:flex;align-items:center;padding:18px">
        <div style="font-size:28px;margin-right:14px">${locked ? '🔒' : (u.icon||'')}</div>
        <div style="flex:1;min-width:0">
          <div style="font-size:17px;font-weight:bold;color:${locked ? '#999' : '#212121'};line-height:1.2">${u.title||u.id}</div>
          <div style="font-size:12px;color:#757575;margin-top:2px">${u.exerciseType||(u.input_mode==='tap'?'Tocar':'Escribir')}</div>
          <div style="height:6px;border-radius:3px;background:#e0e0e0;margin-top:8px;overflow:hidden">
            <div style="width:${u.percent||0}%;height:100%;border-radius:3px;background:#4CAF50"></div>
          </div>
          <div style="font-size:12px;color:#757575;margin-top:4px">${u.completedItems||0} / ${u.totalItems||0} items</div>
        </div>
        <div style="font-size:24px;color:${locked ? '#999' : '#4CAF50'}">${locked ? '🔒' : '→'}</div>
      </div>
    </div>`
  }).join('')
  return html
}

function renderUnitsTheory() {
  return '<div style="padding:16px;text-align:center;color:#999;font-size:14px">Teoría de la unidad</div>'
}

function renderBlocks() {
  if (!blocksData.value.length) return '<div style="text-align:center;color:#999;padding:40px 20px;font-size:14px">No hay bloques</div>'
  let html = '<div style="padding:8px 0"></div>'
  html += blocksData.value.map((b, i) => `
    <div class="card" data-idx="${i}" data-view="blocks" style="margin:0 12px 12px 12px">
      <div style="display:flex;align-items:center;padding:16px">
        <div style="flex:1;min-width:0">
          <div style="font-size:17px;font-weight:bold;color:#4CAF50">📋 ${b.title}</div>
          <div style="font-size:13px;color:#757575;margin-top:4px">${b.items?.length||0} ejercicios</div>
        </div>
        <div style="font-size:24px;color:#4CAF50">→</div>
      </div>
    </div>
  `).join('')
  return html
}

function renderBlocksTheory() {
  return '<div style="padding:16px;text-align:center;color:#999;font-size:14px">Teoría del bloque</div>'
}

function goHome() {
  navStack.value = [{ view: 'subjects', id: '' }]
  selectedTab.value = 0
}
function goBack() {
  if (navStack.value.length > 1) {
    navStack.value.pop()
    selectedTab.value = 0
  }
}

async function loadSubjects() {
  try {
    const { data } = await api.get('/content/subjects')
    subjectsData.value = data.subjects || []
  } catch (e) { subjectsData.value = [] }
}

watch(navStack, async () => {
  const v = currentView.value
  if (v.view === 'subjects') await loadSubjects()
  if (v.view === 'topics' && v.id) {
    try {
      const { data } = await api.get(`/admin/subjects/${v.id}/topics`)
      topicsData.value = data || []
    } catch (e) { topicsData.value = [] }
  }
  if (v.view === 'units' && v.id) {
    try {
      const { data } = await api.get(`/admin/topics/${v.id}/units`)
      unitsData.value = data || []
    } catch (e) { unitsData.value = [] }
  }
  if (v.view === 'blocks' && v.id) {
    try {
      const { data } = await api.get(`/admin/units/${v.id}/blocks`)
      blocksData.value = data || []
    } catch (e) { blocksData.value = [] }
  }
  selectedTab.value = 0
}, { immediate: true, deep: true })

function handleCardClick(e) {
  const card = e.target.closest('.card[data-view]')
  if (!card) return
  const view = card.dataset.view
  const idx = parseInt(card.dataset.idx || '0')
  const v = currentView.value
  if (view === 'subjects' && v.view === 'subjects') {
    const s = subjectsData.value[idx]
    if (s) navStack.value.push({ view: 'topics', id: s.id })
  }
  if (view === 'topics' && v.view === 'topics') {
    const t = topicsData.value[idx]
    if (t) navStack.value.push({ view: 'units', id: t.id })
  }
  if (view === 'units' && v.view === 'units') {
    const u = unitsData.value[idx]
    if (u) navStack.value.push({ view: 'blocks', id: u.id })
  }
}

onMounted(() => {
  document.addEventListener('click', handleCardClick)
})
</script>

<style scoped>
.app-preview-container { display: flex; flex-direction: column; align-items: flex-start; width: fit-content; }
.phone-frame {
  max-width: 240px; width: 240px; height: 480px;
  border: 3px solid #333; border-radius: 24px;
  background: #fff; overflow: hidden;
  position: relative;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
  display: flex; flex-direction: column;
}
.phone-notch {
  width: 80px; height: 16px;
  background: #333; border-radius: 0 0 12px 12px;
  margin: 0 auto; flex-shrink: 0;
}
.phone-screen { flex: 1; overflow-y: auto; display: flex; flex-direction: column; }
.screen-header {
  background: #4CAF50; color: white;
  padding: 12px 8px; font-weight: bold; font-size: 14px;
  display: flex; align-items: center; flex-shrink: 0;
}
.header-back { font-size: 18px; margin-right: 8px; cursor: pointer; }
.header-title { flex: 1; text-align: center; margin-right: 24px; }
.screen-tabs {
  display: flex; background: white; border-bottom: 1px solid #e0e0e0; flex-shrink: 0;
}
.tab {
  flex: 1; text-align: center; padding: 10px 0; font-size: 12px; font-weight: 500;
  color: #757575; cursor: pointer; border-bottom: 2px solid transparent;
}
.tab.active { color: #4CAF50; font-weight: bold; border-bottom-color: #4CAF50; }
.screen-content { flex: 1; overflow-y: auto; background: #f5f5f5; }
.card {
  background: white; border-radius: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.15); cursor: pointer;
}
.bottom-nav {
  display: flex; justify-content: space-around;
  background: white; border-top: 1px solid #e0e0e0; padding: 8px 0; flex-shrink: 0;
}
.nav-item { text-align: center; flex: 1; }
.nav-icon { font-size: 20px; }
.nav-label { font-size: 10px; color: #757575; margin-top: 2px; }
.nav-item.active .nav-label { color: #4CAF50; }
.phone-home {
  width: 30px; height: 4px; background: #999; border-radius: 2px;
  margin: 8px auto 0; flex-shrink: 0;
}
</style>