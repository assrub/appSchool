<template>
  <div class="app-preview-container">
    <div class="phone-frame">
      <div class="phone-notch" />
      <div class="phone-screen">
        <div class="app-header">
          <span>AppEnglish</span>
        </div>
        <div class="app-content" v-html="currentViewHtml" />
        <div class="bottom-nav" v-if="showBottomNav">
          <div class="nav-item active">
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
        <v-btn size="x-small" :color="navStack.length===1?'primary':'grey'" @click="goHome">🏠</v-btn>
        <v-btn size="x-small" color="primary" :disabled="navStack.length<=1" @click="goBack">←</v-btn>
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

const showBottomNav = computed(() => true)

const currentView = computed(() => navStack.value[navStack.value.length - 1])

const currentViewHtml = computed(() => {
  const v = currentView.value
  if (v.view === 'subjects') return renderSubjects()
  if (v.view === 'topics') return renderTopics()
  if (v.view === 'units') return renderUnits()
  if (v.view === 'blocks') return renderBlocks()
  return '<div style="text-align:center;color:#999;padding:40px 20px;font-size:14px">Cargando...</div>'
})

function renderSubjects() {
  if (!subjectsData.value.length) return '<div style="text-align:center;color:#999;padding:40px 20px;font-size:14px">No hay materias</div>'
  let html = '<div style="padding:16px 16px 8px 16px"><div style="font-size:20px;font-weight:bold;color:#4CAF50">MATERIAS</div></div>'
  html += subjectsData.value.map(s => `
    <div class="subject-card" data-idx="${subjectsData.value.indexOf(s)}" data-view="subjects">
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
  html += topicsData.value.map(t => {
    const stars = '★'.repeat(t.difficulty || 1) + '☆'.repeat(5 - (t.difficulty || 1))
    return `<div class="topic-card" data-idx="${topicsData.value.indexOf(t)}" data-view="topics">
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
  let html = '<div style="padding:16px 16px 8px 16px"><div style="font-size:20px;font-weight:bold;color:#4CAF50">UNIDADES</div></div>'
  html += unitsData.value.map(u => {
    const locked = u.is_locked
    const icon = u.icon || ''
    return `<div class="unit-card" data-idx="${unitsData.value.indexOf(u)}" data-view="units" style="${locked ? 'opacity:0.5' : ''}">
      <div style="display:flex;align-items:center;padding:18px">
        <div style="font-size:28px;margin-right:14px">${locked ? '🔒' : icon}</div>
        <div style="flex:1;min-width:0">
          <div style="font-size:17px;font-weight:bold;color:${locked ? '#999' : '#212121'};line-height:1.2">${u.title||u.id}</div>
          <div style="font-size:12px;color:#757575;margin-top:2px">${u.exerciseType||(u.input_mode==='tap'?'Tocar':'Escribir')}</div>
          <div style="height:6px;border-radius:3px;background:#e0e0e0;margin-top:8px;overflow:hidden">
            <div style="width:${u.percent||0}%;height:100%;border-radius:3px;background:#4CAF50"></div>
          </div>
          <div style="font-size:11px;color:#757575;margin-top:4px">${u.completedItems||0} / ${u.totalItems||0} items</div>
        </div>
        <div style="font-size:24px;color:${locked ? '#999' : '#4CAF50'}">${locked ? '🔒' : '→'}</div>
      </div>
    </div>`
  }).join('')
  return html
}

function renderBlocks() {
  if (!blocksData.value.length) return '<div style="text-align:center;color:#999;padding:40px 20px;font-size:14px">No hay bloques</div>'
  return blocksData.value.map(b => `
    <div class="block-card" data-view="blocks">
      <div style="display:flex;align-items:center;padding:16px">
        <div style="flex:1;min-width:0">
          <div style="font-size:17px;font-weight:bold;color:#4CAF50">📋 ${b.title}</div>
          <div style="font-size:13px;color:#757575;margin-top:4px">${b.items?.length||0} ejercicios</div>
        </div>
        <div style="font-size:24px;color:#4CAF50">→</div>
      </div>
    </div>
  `).join('')
}

function goHome() { navStack.value = [{ view: 'subjects', id: '' }] }
function goBack() { if (navStack.value.length > 1) navStack.value.pop() }

async function loadSubjects() {
  try {
    const { data } = await api.get('/content/subjects')
    subjectsData.value = data.subjects || []
  } catch (e) {
    subjectsData.value = []
  }
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
}, { immediate: true, deep: true })

function handleCardClick(e) {
  const card = e.target.closest('[data-view]')
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
.app-preview-container { display: flex; flex-direction: column; align-items: flex-start; width: fit-content; }
.phone-preview { display: inline-flex; flex-direction: column; align-items: flex-start; flex-shrink: 0; }
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
.app-header {
  background: #4CAF50; color: white;
  padding: 12px 16px; font-weight: bold; font-size: 16px;
  text-align: center; flex-shrink: 0;
}
.app-content { flex: 1; overflow-y: auto; background: #f5f5f5; }
.bottom-nav {
  display: flex; justify-content: space-around;
  background: white; border-top: 1px solid #e0e0e0;
  padding: 8px 0; flex-shrink: 0;
}
.nav-item { text-align: center; flex: 1; }
.nav-icon { font-size: 20px; }
.nav-label { font-size: 10px; color: #757575; margin-top: 2px; }
.nav-item.active .nav-label { color: #4CAF50; }
.subject-card, .topic-card, .unit-card, .block-card {
  margin: 0 12px 12px 12px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  cursor: pointer;
}
</style>