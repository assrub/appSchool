<template>
  <div class="app-preview-container">
    <MobilePreview :html="currentViewHtml" />
    <div class="mt-2 text-center">
      <v-btn-group density="compact" variant="text">
        <v-btn size="x-small" :color="navStack.length===0?'primary':'grey'" @click="goHome">🏠</v-btn>
        <v-btn size="x-small" color="primary" @click="goBack" :disabled="navStack.length<=1">←</v-btn>
      </v-btn-group>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import api from '../api/client'
import MobilePreview from './MobilePreview.vue'

const navStack = ref([{ view: 'subjects', id: null }])
const subjectsData = ref([])
const topicsData = ref([])
const unitsData = ref([])
const blocksData = ref([])

const currentView = computed(() => navStack.value[navStack.value.length - 1])
const currentViewHtml = computed(() => buildHtml())

function buildHtml() {
  const v = currentView.value
  if (v.view === 'subjects') return renderSubjects()
  if (v.view === 'topics') return renderTopics()
  if (v.view === 'units') return renderUnits()
  if (v.view === 'blocks') return renderBlocks()
  return '<p style="text-align:center;color:#999;padding:20px">Cargando...</p>'
}

function renderSubjects() {
  if (!subjectsData.value.length) return '<p style="text-align:center;color:#999;padding:20px">No hay materias</p>'
  return subjectsData.value.map(s => `
    <div style="display:flex;align-items:center;gap:16px;padding:16px 14px;border-bottom:1px solid #f0f0f0;cursor:pointer" onclick="document.dispatchEvent(new CustomEvent('appnav',{detail:{view:'topics',subjectId:'${s.id}',subjectName:'${s.name}'}}))">
      <div style="font-size:32px">${s.icon||'📚'}</div>
      <div style="flex:1;min-width:0">
        <div style="font-weight:bold;font-size:16px;color:#333">${s.name}</div>
        <div style="font-size:12px;color:#999;margin-top:2px">${s.topicsCount||0} temas</div>
      </div>
      <div style="font-size:18px;color:#4CAF50">→</div>
    </div>
  `).join('')
}

function renderTopics() {
  if (!topicsData.value.length) return '<p style="text-align:center;color:#999;padding:20px">No hay temas</p>'
  return topicsData.value.map(t => `
    <div style="display:flex;align-items:center;gap:12px;padding:14px;border-bottom:1px solid #f0f0f0;cursor:pointer" onclick="document.dispatchEvent(new CustomEvent('appnav',{detail:{view:'units',topicId:'${t.id}',topicName:'${t.name}'}}))">
      <div style="font-size:28px">${t.icon||'📝'}</div>
      <div style="flex:1;min-width:0">
        <div style="font-weight:bold;font-size:14px;color:#333">${t.name}</div>
        <div style="font-size:11px;color:#999">Dificultad: ${'★'.repeat(t.difficulty||1)}</div>
        <div style="height:6px;border-radius:3px;background:#e0e0e0;margin-top:6px"><div style="width:0%;height:100%;border-radius:3px;background:#4CAF50"></div></div>
      </div>
      <div style="font-size:18px;color:#4CAF50">→</div>
    </div>
  `).join('')
}

function renderUnits() {
  if (!unitsData.value.length) return '<p style="text-align:center;color:#999;padding:20px">No hay unidades</p>'
  const icons = ['✏️','❌','❓','✅']
  return unitsData.value.map((u,i) => `
    <div style="display:flex;align-items:center;gap:12px;padding:14px;border-bottom:1px solid #f0f0f0;cursor:pointer${u.is_locked?'opacity:0.5':''}" onclick="document.dispatchEvent(new CustomEvent('appnav',{detail:{view:'blocks',unitId:'${u.id}',unitTitle:'${u.title}'}}))">
      <div style="font-size:28px">${u.is_locked?'🔒':icons[i%icons.length]}</div>
      <div style="flex:1;min-width:0">
        <div style="font-weight:bold;font-size:14px;color:${u.is_locked?'#999':'#333'}">${u.title||u.id}</div>
        <div style="font-size:11px;color:#999">${u.input_mode==='tap'?'🖐️ Tap':'⌨️ Type'}</div>
        <div style="height:6px;border-radius:3px;background:#e0e0e0;margin-top:6px"><div style="width:0%;height:100%;border-radius:3px;background:#4CAF50"></div></div>
      </div>
      <div style="font-size:18px;color:${u.is_locked?'#999':'#4CAF50'}">${u.is_locked?'🔒':'→'}</div>
    </div>
  `).join('')
}

function renderBlocks() {
  if (!blocksData.value.length) return '<p style="text-align:center;color:#999;padding:20px">No hay bloques</p>'
  return blocksData.value.map(b => `
    <div style="padding:16px 14px;border-bottom:1px solid #f0f0f0">
      <div style="font-size:14px;color:#4CAF50;font-weight:bold;margin-bottom:4px">${b.icon||'📋'} ${b.title}</div>
      <div style="font-size:12px;color:#999">${b.shuffle?'🔀 Mezclado | ':''}Ejercicios disponibles</div>
    </div>
  `).join('')
}

function goHome() {
  navStack.value = [{ view: 'subjects', id: null }]
  loadSubjects()
}

function goBack() {
  if (navStack.value.length > 1) navStack.value.pop()
}

async function loadSubjects() {
  const { data } = await api.get('/content/subjects')
  subjectsData.value = data.subjects || []
}

async function loadTopics(subjectId) {
  const { data } = await api.get(`/admin/subjects/${subjectId}/topics`)
  topicsData.value = data || []
}

async function loadUnits(topicId) {
  const { data } = await api.get(`/admin/topics/${topicId}/units`)
  unitsData.value = data || []
}

async function loadBlocks(unitId) {
  const { data } = await api.get(`/admin/units/${unitId}/blocks`)
  blocksData.value = data || []
}

// Listen for clicks from the HTML
document.addEventListener('appnav', async (e) => {
  const { view, subjectId, topicId, unitId } = e.detail
  if (view === 'topics') { await loadTopics(subjectId); navStack.value.push({ view, id: subjectId }) }
  if (view === 'units') { await loadUnits(topicId); navStack.value.push({ view, id: topicId }) }
  if (view === 'blocks') { await loadBlocks(unitId); navStack.value.push({ view, id: unitId }) }
})

loadSubjects()
</script>
