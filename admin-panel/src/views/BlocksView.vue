<template>
  <div>
    <div class="d-flex align-center mb-6">
      <div>
        <v-btn variant="text" prepend-icon="mdi-arrow-left" :to="`/topics/${topicId}/units`" class="mb-2">
          Unidades
        </v-btn>
        <h1 class="text-h4">Bloques de {{ unitTitle }}</h1>
      </div>
      <v-spacer />
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openDialog()">Nuevo Bloque</v-btn>
    </div>

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4" closable @click:close="error = ''">
      {{ error }}
      <v-btn size="small" class="ml-2" @click="fetchData">Reintentar</v-btn>
    </v-alert>

    <v-row>
      <v-col cols="12" md="9">
        <v-card rounded="lg" elevation="2">
          <v-progress-linear v-if="loading" indeterminate color="primary" />
          <v-table v-else>
            <thead>
              <tr>
                <th>Ord.</th>
                <th>#</th>
                <th>Título</th>
                <th>🔀</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(b, idx) in items" :key="b.id">
                <td>
                  <v-btn icon="mdi-chevron-up" variant="text" size="x-small" :disabled="idx === 0" @click="moveItem(idx, -1)" />
                  <v-btn icon="mdi-chevron-down" variant="text" size="x-small" :disabled="idx === items.length - 1" @click="moveItem(idx, 1)" />
                </td>
                <td>{{ b.id }}</td>
                <td>{{ b.title }}</td>
                <td>
                  <v-chip v-if="b.shuffle" size="small" color="warning" variant="tonal">
                    Mezclado
                  </v-chip>
                </td>
                <td>
                  <v-tooltip text="Ver ejercicios" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn icon="mdi-dumbbell" v-bind="tp" variant="text" size="small" color="secondary" :to="`/blocks/${b.id}/items`" />
                    </template>
                  </v-tooltip>
                  <v-tooltip text="Editar bloque" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn icon="mdi-pencil" v-bind="tp" variant="text" size="small" color="primary" @click="openDialog(b)" />
                    </template>
                  </v-tooltip>
                  <v-tooltip text="Editar teoría del bloque" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn icon="mdi-book-open-page-variant" v-bind="tp" variant="text" size="small" color="info" :to="{ path: `/theory/block/${b.id}`, query: { unitId } }" />
                    </template>
                  </v-tooltip>
                  <v-tooltip text="Desactivar bloque" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn icon="mdi-delete" v-bind="tp" variant="text" size="small" color="error" @click="confirmDelete(b)" />
                    </template>
                  </v-tooltip>
                  <v-tooltip text="Eliminar permanentemente" location="top">
                    <template #activator="{ props: tp }">
                      <v-btn icon="mdi-delete-forever" v-bind="tp" variant="text" size="small" color="deep-orange" @click="confirmHardDelete(b)" />
                    </template>
                  </v-tooltip>
                </td>
              </tr>
            </tbody>
          </v-table>
          <v-card-text v-if="!loading && items.length === 0" class="text-center text-grey pa-8">
            No hay bloques. ¡Crea el primero!
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="3">
        <MobilePreview :html="blocksPreviewHtml" />
      </v-col>
    </v-row>

    <v-dialog v-model="dialog" max-width="500">
      <v-card rounded="lg">
        <v-card-title>{{ editing ? 'Editar' : 'Nuevo' }} Bloque</v-card-title>
        <v-card-text>
          <v-text-field
            v-model="form.title"
            label="Título"
            :error-messages="v.errors.title"
            class="mb-2"
          />
          <v-switch
            v-model="form.shuffle"
            label="Mezclar ejercicios (aleatorio)"
            color="warning"
            hide-details
          />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="dialog = false">Cancelar</v-btn>
          <v-btn color="primary" :loading="saving" @click="save">
            {{ editing ? 'Guardar' : 'Crear' }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="deleteDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title>¿Eliminar bloque?</v-card-title>
        <v-card-text>
          Se eliminará "<b>{{ toDelete?.title }}</b>" con todos sus ejercicios.
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="deleteDialog = false">Cancelar</v-btn>
          <v-btn color="error" @click="doDelete">Eliminar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="hardDeleteDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title class="text-error">
          <v-icon class="mr-2">mdi-alert</v-icon>
          Eliminar permanentemente
        </v-card-title>
        <v-card-text>
          ¿Estás seguro de eliminar "<b>{{ toHardDelete?.title }}</b>"?<br />
          Se borrarán TODOS los ejercicios y progreso asociados.
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="hardDeleteDialog = false">Cancelar</v-btn>
          <v-btn color="deep-orange" @click="doHardDelete">Eliminar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, inject } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/client'
import MobilePreview from '../components/MobilePreview.vue'
import { useValidate } from '../composables/useValidation'

const route = useRoute()
const snackbar = inject('snackbar')

const unitId = route.params.unitId
const topicId = ref('')
const unitTitle = ref('')
const items = ref([])
const loading = ref(true)
const error = ref('')
const dialog = ref(false)
const deleteDialog = ref(false)
const hardDeleteDialog = ref(false)
const editing = ref(null)
const saving = ref(false)
const toDelete = ref(null)
const toHardDelete = ref(null)

const form = ref({ title: '', unit_id: unitId, shuffle: false, sort_order: 0 })

const v = useValidate({ title: [] })
const validationRules = {
  title: { required: true, minLength: 2, maxLength: 100 }
}

const blocksPreviewHtml = computed(() => {
  const header = `<div style="background:#4CAF50;color:white;padding:12px 8px;font-weight:bold;font-size:14px;flex-shrink:0;display:flex;align-items:center"><div style="font-size:18px;margin-right:8px">←</div><div style="flex:1;text-align:center;margin-right:24px">${unitTitle.value || 'Bloques'}</div></div>`
  const tabs = '<div style="display:flex;background:white;border-bottom:1px solid #e0e0e0;flex-shrink:0"><div style="flex:1;text-align:center;padding:12px 0;font-size:10px;font-weight:bold;color:#4CAF50;border-bottom:2px solid #4CAF50">BLOQUES</div></div>'
  const bottomNav = '<div style="display:flex;justify-content:space-around;background:white;border-top:1px solid #e0e0e0;padding:8px 0 6px 0;flex-shrink:0"><div style="text-align:center;flex:1"><div style="font-size:18px">🏠</div><div style="font-size:10px;color:#4CAF50;margin-top:2px">Inicio</div></div><div style="text-align:center;flex:1"><div style="font-size:18px">📖</div><div style="font-size:10px;color:#757575;margin-top:2px">Diccionario</div></div><div style="text-align:center;flex:1"><div style="font-size:18px">📊</div><div style="font-size:10px;color:#757575;margin-top:2px">Progreso</div></div><div style="text-align:center;flex:1"><div style="font-size:18px">⚙️</div><div style="font-size:10px;color:#757575;margin-top:2px">Ajustes</div></div></div>'

  if (!items.value.length) {
    return `<div style="display:flex;flex-direction:column;height:100%">${header}${tabs}<div style="flex:1;display:flex;align-items:center;justify-content:center;background:#f5f5f5;color:#757575;font-size:14px;padding:20px">No hay bloques</div>${bottomNav}</div>`
  }

  let cards = '<div style="padding:8px 0"></div>'
  cards += items.value.map(b => `
    <div style="margin:0 12px 12px 12px;background:white;border-radius:16px;box-shadow:0 1px 2px rgba(0,0,0,0.3), 0 1px 3px 1px rgba(0,0,0,0.15)">
      <div style="display:flex;align-items:center;padding:16px">
        <div style="flex:1;min-width:0">
          <div style="font-size:17px;font-weight:600;color:#4CAF50;line-height:1.2">📋 ${b.title}</div>
          <div style="font-size:13px;color:#757575;margin-top:4px">${b.items_count||0} ejercicios</div>
        </div>
        <div style="font-size:24px;color:#4CAF50;flex-shrink:0">→</div>
      </div>
    </div>
  `).join('')

  return `<div style="display:flex;flex-direction:column;height:100%">${header}${tabs}<div style="flex:1;overflow-y:auto;background:#f5f5f5">${cards}</div>${bottomNav}</div>`
})

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const r = await api.get(`/admin/units/${unitId}/blocks`)
    items.value = r.data
    const u = await api.get(`/admin/units/${unitId}`)
    unitTitle.value = u.data.title || unitId
    topicId.value = u.data.topic_id
  } catch (e) {
    error.value = e.response?.data?.detail || 'Error al cargar'
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

function openDialog(block = null) {
  editing.value = block
  v.clearAllErrors()
  form.value = block
    ? { ...block, unit_id: unitId }
    : { title: '', unit_id: unitId, shuffle: false, sort_order: 0 }
  dialog.value = true
}

function validateForm() {
  return v.validateAll(form.value, validationRules)
}

async function save() {
  if (!validateForm()) return

  saving.value = true
  try {
    if (editing.value) {
      await api.put(`/admin/blocks/${editing.value.id}`, form.value)
      snackbar.success('Bloque actualizado')
    } else {
      await api.post('/admin/blocks', form.value)
      snackbar.success('Bloque creado')
    }
    dialog.value = false
    await fetchData()
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al guardar')
  } finally {
    saving.value = false
  }
}

function confirmDelete(b) {
  toDelete.value = b
  deleteDialog.value = true
}

async function doDelete() {
  try {
    await api.delete(`/admin/blocks/${toDelete.value.id}`)
    snackbar.success('Bloque eliminado')
    deleteDialog.value = false
    await fetchData()
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al eliminar')
  }
}

function confirmHardDelete(b) {
  toHardDelete.value = b
  hardDeleteDialog.value = true
}

async function doHardDelete() {
  try {
    await api.delete(`/admin/blocks/${toHardDelete.value.id}/hard`)
    snackbar.success('Bloque eliminado permanentemente')
    hardDeleteDialog.value = false
    await fetchData()
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al eliminar')
  }
}

async function moveItem(idx, dir) {
  const a = items.value[idx]
  const b = items.value[idx + dir]
  const tmp = a.sort_order
  a.sort_order = b.sort_order
  b.sort_order = tmp
  try {
    await api.put('/admin/blocks-reorder', {
      items: items.value.map(x => ({ id: x.id, sort_order: x.sort_order }))
    })
    await fetchData()
  } catch (e) {
    snackbar.error('Error al reordenar')
  }
}
</script>