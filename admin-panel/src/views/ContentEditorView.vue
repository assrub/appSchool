<template>
  <div class="content-editor">
    <v-navigation-drawer v-model="drawer" :rail="rail" permanent>
      <v-list nav density="compact">
        <v-list-item prepend-icon="mdi-view-dashboard" title="Dashboard" to="/" exact rounded="lg" class="mb-1" />
        <v-list-item prepend-icon="mdi-bookshelf" title="Materias" to="/subjects" rounded="lg" class="mb-1" />
        <v-list-item prepend-icon="mdi-chart-bar" title="Progreso" to="/progress" rounded="lg" class="mb-1" />
        <v-list-item prepend-icon="mdi-account-group" title="Usuarios" to="/users" rounded="lg" class="mb-1" />
      </v-list>

      <template #append>
        <v-divider />
        <v-btn icon variant="text" @click="rail = !rail" class="ma-2">
          <v-icon>{{ rail ? 'mdi-chevron-right' : 'mdi-chevron-left' }}</v-icon>
        </v-btn>
      </template>
    </v-navigation-drawer>

    <v-app-bar color="white" elevation="1">
      <v-breadcrumbs :items="breadcrumbs" class="py-0">
        <template #divider><v-icon icon="mdi-chevron-right" size="small" /></template>
        <template #title="{ item }">
          <span v-if="item.clickable" class="text-primary cursor-pointer" @click="navigateTo(item)">{{ item.title }}</span>
          <span v-else class="text-grey-darken-1">{{ item.title }}</span>
        </template>
      </v-breadcrumbs>
      <v-spacer />
      <v-chip variant="tonal" color="primary" size="small" class="mr-4">
        <v-icon start size="18">mdi-account</v-icon>
        {{ username }}
      </v-chip>
    </v-app-bar>

    <v-main>
      <v-container fluid class="pa-6">
        <v-row>
          <v-col cols="12" md="4" lg="3">
            <v-card rounded="lg" elevation="2">
              <v-card-title class="d-flex align-center">
                <v-icon class="mr-2">mdi-tree</v-icon>
                Contenido
                <v-spacer />
                <v-btn icon="mdi-plus" variant="text" size="small" @click="showAddDialog = true" />
              </v-card-title>

              <v-progress-linear v-if="loadingTree" indeterminate color="primary" />

              <v-treeview
                v-else
                :items="contentTree"
                item-children="children"
                activatable
                :active="[activeId]"
                @click:activator="onNodeClick"
                class="pa-2"
              >
                <template #prepend="{ item, open }">
                  <v-icon v-if="item.children?.length">
                    {{ open ? 'mdi-folder-open' : 'mdi-folder' }}
                  </v-icon>
                  <v-icon v-else>
                    {{ getIconForType(item.type) }}
                  </v-icon>
                </template>
              </v-treeview>
            </v-card>
          </v-col>

          <v-col cols="12" md="8" lg="9">
            <template v-if="selectedNode">
              <v-card rounded="lg" elevation="2">
                <v-card-title class="d-flex align-center">
                  <v-icon class="mr-2">{{ getIconForType(selectedNode.type) }}</v-icon>
                  Editar {{ selectedNode.type }}
                  <v-spacer />
                  <v-btn variant="text" color="grey" @click="selectedNode = null">
                    <v-icon>mdi-close</v-icon>
                  </v-btn>
                </v-card-title>

                <v-card-text>
                  <v-tabs v-model="editTab" color="primary">
                    <v-tab value="content">Contenido</v-tab>
                    <v-tab value="theory">Teoría</v-tab>
                    <v-tab value="preview">Vista previa</v-tab>
                  </v-tabs>

                  <v-window v-model="editTab" class="mt-4">
                    <v-window-item value="content">
                      <div v-if="selectedNode.type === 'subject'">
                        <v-text-field v-model="editForm.name" label="Nombre" variant="outlined" class="mb-3" />
                        <v-text-field v-model="editForm.icon" label="Icono (emoji)" variant="outlined" class="mb-3" />
                        <v-text-field v-model="editForm.color" label="Color" variant="outlined" class="mb-3" type="color" />
                        <v-text-field v-model.number="editForm.sort_order" label="Orden" variant="outlined" type="number" />
                      </div>

                      <div v-else-if="selectedNode.type === 'topic'">
                        <v-text-field v-model="editForm.name" label="Nombre" variant="outlined" class="mb-3" />
                        <v-text-field v-model="editForm.icon" label="Icono" variant="outlined" class="mb-3" />
                        <v-slider v-model="editForm.difficulty" label="Dificultad" :min="1" :max="5" thumb-label show-size class="mb-3" />
                      </div>

                      <div v-else-if="selectedNode.type === 'unit'">
                        <v-text-field v-model="editForm.title" label="Título" variant="outlined" class="mb-3" />
                        <v-select v-model="editForm.input_mode" label="Modo" :items="[{title:'Tocar',value:'tap'},{title:'Escribir',value:'type'}]" variant="outlined" class="mb-3" />
                        <v-switch v-model="editForm.is_locked" label="Bloqueada" color="warning" hide-details class="mb-3" />
                      </div>

                      <div v-else-if="selectedNode.type === 'block'">
                        <v-text-field v-model="editForm.title" label="Título" variant="outlined" class="mb-3" />
                        <v-switch v-model="editForm.shuffle" label="Mezclar ejercicios" color="warning" hide-details />
                      </div>

                      <div v-else-if="selectedNode.type === 'item'">
                        <v-select v-model="editForm.item_type" label="Tipo" :items="itemTypes" variant="outlined" class="mb-3" />
                        <v-textarea v-if="editForm.item_type === 'fill-blank'" v-model="editForm.sentence" label="Frase (usá ______)" variant="outlined" rows="3" class="mb-3" />
                        <v-text-field v-if="editForm.item_type !== 'reorder' && editForm.item_type !== 'matching'" v-model="editForm.answer" label="Respuesta" variant="outlined" class="mb-3" />
                      </div>

                      <div class="d-flex justify-end mt-4">
                        <v-btn color="primary" :loading="savingEdit" @click="saveNode">
                          <v-icon start>mdi-content-save</v-icon>
                          Guardar
                        </v-btn>
                      </div>
                    </v-window-item>

                    <v-window-item value="theory">
                      <div v-if="selectedNode.type === 'topic' || selectedNode.type === 'unit'">
                        <div class="d-flex align-center mb-4">
                          <v-btn color="primary" variant="tonal" prepend-icon="mdi-plus" @click="addTheoryBlock">
                            Agregar sección
                          </v-btn>
                        </div>

                        <div v-for="(block, i) in theoryBlocks" :key="i" class="mb-4">
                          <v-card variant="outlined" rounded="lg">
                            <v-card-text>
                              <v-text-field v-model="block.title" label="Título de sección" variant="outlined" density="compact" class="mb-3" />
                              <RichTextEditor v-model="block.html" />
                              <v-btn variant="text" color="error" size="small" @click="removeTheoryBlock(i)" class="mt-2">
                                <v-icon start>mdi-delete</v-icon>
                                Eliminar
                              </v-btn>
                            </v-card-text>
                          </v-card>
                        </div>

                        <div class="d-flex justify-end mt-4">
                          <v-btn color="primary" :loading="savingTheory" @click="saveTheory">
                            <v-icon start>mdi-content-save</v-icon>
                            Guardar teoría
                          </v-btn>
                        </div>
                      </div>
                      <div v-else class="text-center text-grey pa-8">
                        <v-icon size="48">mdi-book-open-page-variant-outline</v-icon>
                        <p>La teoría está disponible para temas y unidades</p>
                      </div>
                    </v-window-item>

                    <v-window-item value="preview">
                      <MobilePreview :html="previewHtml" />
                    </v-window-item>
                  </v-window>
                </v-card-text>
              </v-card>
            </template>

            <v-card v-else rounded="lg" elevation="2">
              <v-card-text class="text-center text-grey py-12">
                <v-icon size="64" class="mb-4">mdi-cursor-default-click-outline</v-icon>
                <p class="text-h6">Seleccioná un elemento para editar</p>
                <p class="text-body-2">Hacé click en materia, tema, unidad, bloque o ejercicio en el árbol de la izquierda</p>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </v-main>

    <v-dialog v-model="showAddDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title>Agregar contenido</v-card-title>
        <v-card-text>
          <v-select v-model="addType" label="Tipo" :items="addTypes" variant="outlined" class="mb-3" />
          <v-text-field v-model="addName" label="Nombre" variant="outlined" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="showAddDialog = false">Cancelar</v-btn>
          <v-btn color="primary" @click="addNode">Agregar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, inject } from 'vue'
import api from '../api/client'
import MobilePreview from '../components/MobilePreview.vue'
import RichTextEditor from '../components/RichTextEditor.vue'

const snackbar = inject('snackbar')

const drawer = ref(true)
const rail = ref(false)
const username = computed(() => localStorage.getItem('username') || '')

const loadingTree = ref(true)
const contentTree = ref([])
const activeId = ref(null)
const selectedNode = ref(null)
const editTab = ref('content')
const savingEdit = ref(false)
const savingTheory = ref(false)

const editForm = ref({})
const theoryBlocks = ref([])

const showAddDialog = ref(false)
const addType = ref('subject')
const addName = ref('')

const itemTypes = [
  { title: 'Completar vacío', value: 'fill-blank' },
  { title: 'Opción múltiple', value: 'multiple-choice' },
  { title: 'Ordenar', value: 'reorder' },
  { title: 'Escuchar', value: 'listening' },
  { title: 'Emparejar', value: 'matching' },
  { title: 'Verdadero/Falso', value: 'true-false' }
]

const addTypes = computed(() => {
  const types = [{ title: 'Materia', value: 'subject' }]
  if (selectedNode.value?.type === 'subject') {
    types.push({ title: 'Tema', value: 'topic' })
  }
  if (selectedNode.value?.type === 'topic') {
    types.push({ title: 'Unidad', value: 'unit' })
  }
  return types
})

const breadcrumbs = computed(() => {
  const items = [{ title: 'Inicio', clickable: true, to: '/' }]
  if (!selectedNode.value) return items

  const parts = []
  let current = selectedNode.value
  while (current) {
    parts.unshift(current.name || current.title || current.id)
    current = contentTree.value.find(n => n.id === current.parentId)
  }
  parts.forEach(p => items.push({ title: p, clickable: false }))
  return items
})

function getIconForType(type) {
  const icons = {
    subject: 'mdi-bookshelf',
    topic: 'mdi-book-open-page-variant',
    unit: 'mdi-format-list-numbered',
    block: 'mdi-view-grid-outline',
    item: 'mdi-help-circle-outline'
  }
  return icons[type] || 'mdi-folder'
}

const previewHtml = computed(() => {
  if (!selectedNode.value) return '<p>Seleccioná un contenido</p>'
  return `<div style="padding:16px">
    <h2 style="font-size:20px;font-weight:bold;margin-bottom:12px">${selectedNode.value.name || selectedNode.value.title || 'Sin título'}</h2>
    <p style="color:#666">${selectedNode.value.type}</p>
  </div>`
})

async function loadContentTree() {
  loadingTree.value = true
  try {
    const { data } = await api.get('/admin/subjects')
    contentTree.value = data.map(subject => ({
      id: subject.id,
      name: `${subject.icon} ${subject.name}`,
      type: 'subject',
      children: (subject.topics || []).map(topic => ({
        id: topic.id,
        name: topic.name,
        type: 'topic',
        parentId: subject.id,
        children: (topic.units || []).map(unit => ({
          id: unit.id,
          name: unit.title,
          type: 'unit',
          parentId: topic.id,
          children: (unit.blocks || []).map(block => ({
            id: block.id,
            name: block.title,
            type: 'block',
            parentId: unit.id,
            children: (block.items || []).map(item => ({
              id: item.id,
              name: item.sentence?.substring(0, 30) || item.answer || 'Sin descripción',
              type: 'item',
              parentId: block.id
            }))
          }))
        }))
      }))
    }))
  } catch (e) {
    snackbar.error('Error al cargar contenido')
  } finally {
    loadingTree.value = false
  }
}

function onNodeClick(node) {
  activeId.value = node.id
  selectedNode.value = node
  editTab.value = 'content'
  editForm.value = { ...node }

  if (node.type === 'topic' || node.type === 'unit') {
    loadTheory(node)
  }
}

async function loadTheory(node) {
  try {
    const { data } = await api.get(`/admin/${node.type}s/${node.id}/theory`)
    theoryBlocks.value = data.blocks?.length ? data.blocks : [{ title: '', html: '' }]
  } catch {
    theoryBlocks.value = [{ title: '', html: '' }]
  }
}

async function saveNode() {
  savingEdit.value = true
  try {
    const type = selectedNode.value.type
    const id = selectedNode.value.id
    await api.put(`/admin/${type}s/${id}`, editForm.value)
    snackbar.success('Guardado correctamente')
    await loadContentTree()
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al guardar')
  } finally {
    savingEdit.value = false
  }
}

async function saveTheory() {
  savingTheory.value = true
  try {
    const type = selectedNode.value.type
    const id = selectedNode.value.id
    await api.put(`/admin/${type}s/${id}/theory`, { blocks: theoryBlocks.value })
    snackbar.success('Teoría guardada')
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al guardar')
  } finally {
    savingTheory.value = false
  }
}

function addTheoryBlock() {
  theoryBlocks.value.push({ title: '', html: '' })
}

function removeTheoryBlock(i) {
  theoryBlocks.value.splice(i, 1)
}

async function addNode() {
  if (!addName.value) return
  try {
    const payload = addType.value === 'subject'
      ? { id: addName.value.toLowerCase().replace(/\s+/g, '-'), name: addName.value }
      : { name: addName.value }

    await api.post(`/admin/${addType.value}s`, payload)
    snackbar.success(`${addType.value} creado`)
    showAddDialog.value = false
    addName.value = ''
    await loadContentTree()
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al crear')
  }
}

function navigateTo(item) {
}

onMounted(loadContentTree)
</script>

<style scoped>
.content-editor {
  min-height: 100vh;
}
.cursor-pointer {
  cursor: pointer;
}
</style>