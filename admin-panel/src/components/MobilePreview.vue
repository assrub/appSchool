<template>
  <div class="phone-preview">
    <div class="phone-frame">
      <div class="phone-notch" />
      <div class="phone-screen">
        <div class="screen-header">
          <div class="header-back" v-if="showBack">←</div>
          <div class="header-title">{{ headerTitle }}</div>
        </div>
        <div class="screen-tabs" v-if="showTabs">
          <div class="tab" :class="{ active: selectedTab === 0 }" @click="selectedTab = 0">{{ tab1 }}</div>
          <div class="tab" :class="{ active: selectedTab === 1 }" @click="selectedTab = 1" v-if="showTab2">{{ tab2 }}</div>
        </div>
        <div class="screen-content" v-html="contentHtml" />
        <div class="bottom-nav">
          <div class="nav-item" :class="{ active: currentTab === 'home' }">
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
    <div class="text-caption text-center mt-2 text-grey">Vista previa en celular</div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  screen: { type: String, default: 'subjects' },
  data: { type: Object, default: () => ({}) }
})

const selectedTab = ref(0)
const currentTab = ref('home')

const showBack = computed(() => props.screen !== 'subjects')
const headerTitle = computed(() => {
  if (props.screen === 'subjects') return 'AppEnglish'
  if (props.screen === 'topics') return props.data.subjectName || 'Temas'
  if (props.screen === 'units') return props.data.topicName || 'Unidades'
  if (props.screen === 'blocks') return props.data.unitName || 'Bloques'
  return 'AppEnglish'
})

const showTabs = computed(() => {
  if (props.screen === 'subjects') return false
  if (props.screen === 'topics') return false
  return props.data.hasTheory || false
})

const showTab2 = computed(() => props.data.hasTheory)

const tab1 = computed(() => {
  if (props.screen === 'units') return 'UNIDADES'
  if (props.screen === 'blocks') return 'BLOQUES'
  return ''
})

const tab2 = computed(() => 'TEORÍA')

const contentHtml = computed(() => {
  if (props.screen === 'subjects') return renderSubjects()
  if (props.screen === 'topics') return renderTopics()
  if (props.screen === 'units') return selectedTab.value === 0 ? renderUnits() : renderTheory(props.data.topicTheory)
  if (props.screen === 'blocks') return selectedTab.value === 0 ? renderBlocks() : renderTheory(props.data.unitTheory)
  return '<div style="text-align:center;color:#999;padding:40px">Pantalla no reconocida</div>'
})

function renderSubjects() {
  if (!props.data.subjects?.length) return '<div style="text-align:center;color:#999;padding:40px 20px;font-size:14px">No hay materias disponibles</div>'
  let html = '<div style="padding:16px 16px 8px 16px"><div style="font-size:20px;font-weight:bold;color:#4CAF50">MATERIAS</div></div>'
  html += props.data.subjects.map(s => `
    <div class="card" style="margin:0 12px 12px 12px">
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
  if (!props.data.topics?.length) return '<div style="text-align:center;color:#999;padding:40px 20px;font-size:14px">No hay temas disponibles</div>'
  let html = '<div style="padding:16px 16px 8px 16px"><div style="font-size:20px;font-weight:bold;color:#4CAF50">TEMAS</div></div>'
  html += props.data.topics.map(t => {
    const stars = '★'.repeat(t.difficulty || 1) + '☆'.repeat(5 - (t.difficulty || 1))
    return `<div class="card" style="margin:0 12px 12px 12px">
      <div style="display:flex;align-items:center;padding:16px">
        <div style="font-size:28px;margin-right:14px">${t.icon||''}</div>
        <div style="flex:1;min-width:0">
          <div style="font-size:18px;font-weight:bold;color:#212121;line-height:1.2">${t.name}</div>
          <div style="font-size:12px;color:#FFA726;margin-top:4px">${stars}</div>
          <div style="height:6px;border-radius:3px;background:#e0e0e0;margin-top:6px;overflow:hidden">
            <div style="width:${t.percentComplete||0}%;height:100%;border-radius:3px;background:#4CAF50"></div>
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
  if (!props.data.units?.length) return '<div style="text-align:center;color:#999;padding:40px 20px;font-size:14px">No hay unidades</div>'
  let html = '<div style="padding:8px 0"></div>'
  html += props.data.units.map(u => {
    const locked = u.is_locked
    return `<div class="card" style="margin:0 12px 12px 12px;${locked ? 'opacity:0.5' : ''}">
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

function renderBlocks() {
  if (!props.data.blocks?.length) return '<div style="text-align:center;color:#999;padding:40px 20px;font-size:14px">No hay bloques</div>'
  let html = '<div style="padding:8px 0"></div>'
  html += props.data.blocks.map(b => `
    <div class="card" style="margin:0 12px 12px 12px">
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

function renderTheory(theory) {
  if (!theory) return '<div style="text-align:center;color:#999;padding:40px 20px;font-size:14px">No hay teoría disponible</div>'
  let html = '<div style="padding:12px">'
  if (theory.blocks?.length) {
    theory.blocks.forEach(b => {
      if (b.title) html += `<div style="font-size:16px;font-weight:bold;color:#4CAF50;margin-bottom:8px">${b.title}</div>`
      const text = b.html ? b.html.replace(/<[^>]*>/g, '') : ''
      if (text) html += `<div style="font-size:14px;color:#333;margin-bottom:16px;line-height:1.5">${text}</div>`
    })
  } else if (theory.text) {
    html += `<div style="font-size:14px;color:#333;line-height:1.5">${theory.text}</div>`
  }
  html += '</div>'
  return html
}
</script>

<style scoped>
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
.screen-header {
  background: #4CAF50; color: white;
  padding: 12px 8px 12px 8px; font-weight: bold; font-size: 14px;
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