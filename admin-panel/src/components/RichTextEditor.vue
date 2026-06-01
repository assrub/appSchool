<template>
  <div class="editor-wrapper" v-if="editor">
    <div class="toolbar">
      <v-tooltip text="Fuente (familia)" location="bottom">
        <template #activator="{ props: tp }">
          <v-menu :close-on-content-click="false">
            <template #activator="{ props: menuProps }">
              <v-btn v-bind="{...tp, ...menuProps}" variant="text" size="small" class="toolbar-btn">
                <v-icon size="18">mdi-format-font</v-icon>
                <v-icon size="14">mdi-menu-down</v-icon>
              </v-btn>
            </template>
            <v-list density="compact" style="min-width: 180px">
              <v-list-item v-for="font in fontFamilies" :key="font.value" :active="editor.isActive('textStyle', { fontFamily: font.value })" @click="editor.chain().focus().setFontFamily(font.value).run()" density="compact">
                <v-list-item-title :style="{ fontFamily: font.value }">{{ font.label }}</v-list-item-title>
              </v-list-item>
            </v-list>
          </v-menu>
        </template>
      </v-tooltip>

      <v-tooltip text="Tamaño de fuente" location="bottom">
        <template #activator="{ props: tp }">
          <v-menu :close-on-content-click="false">
            <template #activator="{ props: menuProps }">
              <v-btn v-bind="{...tp, ...menuProps}" variant="text" size="small" class="toolbar-btn">
                <v-icon size="18">mdi-format-size</v-icon>
                <v-icon size="14">mdi-menu-down</v-icon>
              </v-btn>
            </template>
            <v-list density="compact">
              <v-list-item v-for="size in fontSizes" :key="size" :active="editor.isActive('textStyle', { fontSize: size + 'px' })" @click="editor.chain().focus().setFontSize(size + 'px').run()" density="compact">
                <v-list-item-title>{{ size }}px</v-list-item-title>
              </v-list-item>
            </v-list>
          </v-menu>
        </template>
      </v-tooltip>

      <v-divider vertical class="mx-1" />

      <v-btn-group density="compact" variant="text">
        <v-tooltip text="Negrita (Ctrl+B)" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" :color="editor.isActive('bold')?'primary':''" @click="editor.chain().focus().toggleBold().run()"><v-icon size="18">mdi-format-bold</v-icon></v-btn></template></v-tooltip>
        <v-tooltip text="Cursiva (Ctrl+I)" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" :color="editor.isActive('italic')?'primary':''" @click="editor.chain().focus().toggleItalic().run()"><v-icon size="18">mdi-format-italic</v-icon></v-btn></template></v-tooltip>
        <v-tooltip text="Subrayado (Ctrl+U)" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" :color="editor.isActive('underline')?'primary':''" @click="editor.chain().focus().toggleUnderline().run()"><v-icon size="18">mdi-format-underline</v-icon></v-btn></template></v-tooltip>
        <v-tooltip text="Tachado" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" :color="editor.isActive('strike')?'primary':''" @click="editor.chain().focus().toggleStrike().run()"><v-icon size="18">mdi-format-strikethrough-variant</v-icon></v-btn></template></v-tooltip>
      </v-btn-group>

      <v-divider vertical class="mx-1" />

      <v-btn-group density="compact" variant="text">
        <v-tooltip text="Alinear izquierda" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" :color="editor.isActive({ textAlign: 'left' })?'primary':''" @click="editor.chain().focus().setTextAlign('left').run()"><v-icon size="18">mdi-format-align-left</v-icon></v-btn></template></v-tooltip>
        <v-tooltip text="Centrar" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" :color="editor.isActive({ textAlign: 'center' })?'primary':''" @click="editor.chain().focus().setTextAlign('center').run()"><v-icon size="18">mdi-format-align-center</v-icon></v-btn></template></v-tooltip>
        <v-tooltip text="Alinear derecha" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" :color="editor.isActive({ textAlign: 'right' })?'primary':''" @click="editor.chain().focus().setTextAlign('right').run()"><v-icon size="18">mdi-format-align-right</v-icon></v-btn></template></v-tooltip>
        <v-tooltip text="Justificar" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" :color="editor.isActive({ textAlign: 'justify' })?'primary':''" @click="editor.chain().focus().setTextAlign('justify').run()"><v-icon size="18">mdi-format-align-justify</v-icon></v-btn></template></v-tooltip>
      </v-btn-group>

      <v-divider vertical class="mx-1" />

      <v-btn-group density="compact" variant="text">
        <v-tooltip text="Título grande (H1)" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" :color="editor.isActive('heading', {level:1})?'primary':''" @click="editor.chain().focus().toggleHeading({level:1}).run()"><v-icon size="18">mdi-format-header-1</v-icon></v-btn></template></v-tooltip>
        <v-tooltip text="Título mediano (H2)" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" :color="editor.isActive('heading', {level:2})?'primary':''" @click="editor.chain().focus().toggleHeading({level:2}).run()"><v-icon size="18">mdi-format-header-2</v-icon></v-btn></template></v-tooltip>
        <v-tooltip text="Título chico (H3)" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" :color="editor.isActive('heading', {level:3})?'primary':''" @click="editor.chain().focus().toggleHeading({level:3}).run()"><v-icon size="18">mdi-format-header-3</v-icon></v-btn></template></v-tooltip>
      </v-btn-group>

      <v-divider vertical class="mx-1" />

      <v-btn-group density="compact" variant="text">
        <v-tooltip text="Lista con viñetas" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" :color="editor.isActive('bulletList')?'primary':''" @click="editor.chain().focus().toggleBulletList().run()"><v-icon size="18">mdi-format-list-bulleted</v-icon></v-btn></template></v-tooltip>
        <v-tooltip text="Lista numerada" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" :color="editor.isActive('orderedList')?'primary':''" @click="editor.chain().focus().toggleOrderedList().run()"><v-icon size="18">mdi-format-list-numbered</v-icon></v-btn></template></v-tooltip>
        <v-tooltip text="Cita textual" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" :color="editor.isActive('blockquote')?'primary':''" @click="editor.chain().focus().toggleBlockquote().run()"><v-icon size="18">mdi-format-quote-close</v-icon></v-btn></template></v-tooltip>
      </v-btn-group>

      <v-divider vertical class="mx-1" />

      <v-btn-group density="compact" variant="text">
        <v-tooltip text="Color de texto" location="bottom"><template #activator="{ props: tp }"><v-menu :close-on-content-click="false" location="bottom"><template #activator="{ props: menuProps }"><v-btn v-bind="{...tp, ...menuProps}" size="small"><v-icon size="18">mdi-palette</v-icon></v-btn></template><v-color-picker v-model="textColor" @update:model-value="setTextColor" hide-inputs width="280" /></v-menu></template></v-tooltip>
        <v-tooltip text="Resaltar texto" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" :color="editor.isActive('highlight')?'primary':''" @click="editor.chain().focus().toggleHighlight().run()"><v-icon size="18">mdi-highlighter</v-icon></v-btn></template></v-tooltip>
      </v-btn-group>

      <v-divider vertical class="mx-1" />

      <v-btn-group density="compact" variant="text">
        <v-tooltip text="Insertar tabla" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" @click="showTableDialog = true"><v-icon size="18">mdi-table-plus</v-icon></v-btn></template></v-tooltip>
        <v-tooltip text="Subir imagen desde PC" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" @click="triggerImageUpload"><v-icon size="18">mdi-image</v-icon></v-btn></template></v-tooltip>
        <v-tooltip text="Insertar imagen desde URL" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" @click="showImageUrlDialog = true"><v-icon size="18">mdi-link-variant</v-icon></v-btn></template></v-tooltip>
        <v-tooltip text="Subir video o YouTube" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" @click="showVideoDialog = true"><v-icon size="18">mdi-video-plus</v-icon></v-btn></template></v-tooltip>
        <v-tooltip text="Insertar link" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" :color="editor.isActive('link')?'primary':''" @click="toggleLink"><v-icon size="18">mdi-link</v-icon></v-btn></template></v-tooltip>
        <v-tooltip text="Emojis" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" @click="showEmojiDialog = true"><v-icon size="18">mdi-emoticon</v-icon></v-btn></template></v-tooltip>
        <v-tooltip text="Tips y consejos" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" @click="showTipsDialog = true"><v-icon size="18">mdi-lightbulb-outline</v-icon></v-btn></template></v-tooltip>
      </v-btn-group>

      <v-divider vertical class="mx-1" />

      <v-btn-group density="compact" variant="text">
        <v-tooltip text="Deshacer (Ctrl+Z)" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" @click="editor.chain().focus().undo().run()"><v-icon size="18">mdi-undo</v-icon></v-btn></template></v-tooltip>
        <v-tooltip text="Rehacer (Ctrl+Y)" location="bottom"><template #activator="{ props: tp }"><v-btn v-bind="tp" size="small" @click="editor.chain().focus().redo().run()"><v-icon size="18">mdi-redo</v-icon></v-btn></template></v-tooltip>
      </v-btn-group>
    </div>

    <div class="editor-content" @dragover.prevent="onDragOver" @dragleave="onDragLeave" @drop.prevent="onDrop">
      <editor-content :editor="editor" />
    </div>

    <div v-if="isDraggingOver" class="drop-overlay" @dragover.prevent @dragleave="onDragLeave" @drop.prevent="onDrop">
      <div class="drop-message">
        <v-icon size="48" color="primary">mdi-cloud-upload</v-icon>
        <p class="text-h6 mt-2">Soltá imagen o video aquí</p>
      </div>
    </div>

    <input ref="fileInput" type="file" accept="image/*,video/mp4,video/mov,video/webm" style="display:none" @change="onFileSelected" />

    <!-- Tabla Dialog -->
    <v-dialog v-model="showTableDialog" max-width="400">
      <v-card>
        <v-card-title class="text-h6 pa-4">Insertar Tabla</v-card-title>
        <v-card-text class="pa-4 pt-0">
          <v-row>
            <v-col cols="6"><v-text-field v-model.number="tableRows" label="Filas" type="number" min="1" max="20" variant="outlined" density="compact" /></v-col>
            <v-col cols="6"><v-text-field v-model.number="tableCols" label="Columnas" type="number" min="1" max="10" variant="outlined" density="compact" /></v-col>
          </v-row>
          <v-checkbox v-model="tableWithHeader" label="Incluir fila de encabezado" density="compact" />
        </v-card-text>
        <v-card-actions class="pa-4 pt-0">
          <v-spacer />
          <v-btn variant="text" @click="showTableDialog = false">Cancelar</v-btn>
          <v-btn color="primary" variant="flat" @click="insertCustomTable">Insertar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Imagen URL Dialog -->
    <v-dialog v-model="showImageUrlDialog" max-width="400">
      <v-card>
        <v-card-title class="text-h6 pa-4">Insertar Imagen</v-card-title>
        <v-card-text class="pa-4 pt-0">
          <v-text-field v-model="imageUrl" label="URL de la imagen" placeholder="https://..." variant="outlined" density="compact" />
          <v-text-field v-model="imageAlt" label="Descripción" placeholder="Texto alternativo" variant="outlined" density="compact" class="mt-2" />
        </v-card-text>
        <v-card-actions class="pa-4 pt-0">
          <v-spacer />
          <v-btn variant="text" @click="showImageUrlDialog = false">Cancelar</v-btn>
          <v-btn color="primary" variant="flat" @click="insertImageFromUrl" :disabled="!imageUrl">Insertar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Video / Link Dialog -->
    <v-dialog v-model="showVideoDialog" max-width="500">
      <v-card>
        <v-card-title class="text-h6 pa-4">Insertar Video</v-card-title>
        <v-card-text class="pa-4 pt-0">
          <v-tabs v-model="videoTab" color="primary" align-tabs="start">
            <v-tab value="upload"><v-icon start>mdi-upload</v-icon>Subir archivo</v-tab>
            <v-tab value="youtube"><v-icon start>mdi-youtube</v-icon>YouTube</v-tab>
          </v-tabs>
          <v-divider class="mb-3" />
          <v-window v-model="videoTab">
            <v-window-item value="upload">
              <v-btn color="primary" variant="tonal" prepend-icon="mdi-file-video" @click="$refs.fileInputVideo?.click()" block class="py-6" style="border: 2px dashed #ccc; border-radius: 12px;">
                {{ videoFile ? videoFile.name : 'Hacé clic para seleccionar un video .mp4' }}
              </v-btn>
              <v-progress-linear v-if="uploadingVideo" indeterminate color="primary" class="mt-2" />
            </v-window-item>
            <v-window-item value="youtube">
              <v-text-field v-model="youtubeUrl" label="URL de YouTube" placeholder="https://www.youtube.com/watch?v=..." variant="outlined" density="compact" />
            </v-window-item>
          </v-window>
          <input ref="fileInputVideo" type="file" accept="video/mp4,video/mov,video/webm" style="display:none" @change="onVideoFileSelected" />
        </v-card-text>
        <v-card-actions class="pa-4 pt-0">
          <v-spacer />
          <v-btn variant="text" @click="showVideoDialog = false">Cancelar</v-btn>
          <v-btn color="primary" variant="flat" :disabled="(!youtubeUrl && !videoFile) || uploadingVideo" @click="insertVideoContent">Insertar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Link Dialog -->
    <v-dialog v-model="showLinkDialog" max-width="400">
      <v-card>
        <v-card-title class="text-h6 pa-4">Insertar Link</v-card-title>
        <v-card-text class="pa-4 pt-0">
          <v-text-field v-model="linkUrl" label="URL" placeholder="https://..." variant="outlined" density="compact" />
        </v-card-text>
        <v-card-actions class="pa-4 pt-0">
          <v-btn v-if="editor.isActive('link')" variant="text" color="error" @click="removeLink">Quitar link</v-btn>
          <v-spacer />
          <v-btn variant="text" @click="showLinkDialog = false">Cancelar</v-btn>
          <v-btn color="primary" variant="flat" @click="applyLink" :disabled="!linkUrl">Aplicar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Emoji Dialog (modal) -->
    <v-dialog v-model="showEmojiDialog" max-width="420">
      <v-card>
        <v-card-title class="text-h6 pa-4 d-flex align-center">
          <v-icon class="mr-2">mdi-emoticon</v-icon> Emojis
        </v-card-title>
        <v-card-text>
          <div class="emoji-grid">
            <button v-for="emoji in emojis" :key="emoji" class="emoji-btn" @click="insertEmoji(emoji)">{{ emoji }}</button>
          </div>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="showEmojiDialog = false">Cerrar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Tips Dialog -->
    <v-dialog v-model="showTipsDialog" max-width="500">
      <v-card>
        <v-card-title class="text-h6 pa-4 d-flex align-center">
          <v-icon class="mr-2" color="warning">mdi-lightbulb-outline</v-icon> Tips y consejos
        </v-card-title>
        <v-card-text class="pa-4 pt-0">
          <p class="text-body-2 text-grey mb-3">Los tips aparecen al final de la teoría.</p>
          <div v-for="(tip, i) in localTips" :key="i" class="d-flex align-center mb-2">
            <v-text-field v-model="localTips[i]" variant="outlined" density="compact" hide-details placeholder="Escribí un tip..." class="flex-grow-1">
              <template #prepend-inner><v-icon size="small" color="warning">mdi-lightbulb</v-icon></template>
            </v-text-field>
            <v-btn icon="mdi-close" variant="text" size="small" color="grey" @click="localTips.splice(i, 1)" class="ml-1" />
          </div>
          <v-btn variant="text" size="small" prepend-icon="mdi-plus" @click="localTips.push('')" class="mt-1">
            Agregar tip
          </v-btn>
        </v-card-text>
        <v-card-actions class="pa-4 pt-0">
          <v-spacer />
          <v-btn variant="text" @click="showTipsDialog = false">Cerrar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import TextStyle from '@tiptap/extension-text-style'
import FontFamily from '@tiptap/extension-font-family'
import Color from '@tiptap/extension-color'
import Highlight from '@tiptap/extension-highlight'
import Link from '@tiptap/extension-link'
import Image from '@tiptap/extension-image'
import Table from '@tiptap/extension-table'
import TableHeader from '@tiptap/extension-table-header'
import TableRow from '@tiptap/extension-table-row'
import TableCell from '@tiptap/extension-table-cell'
import TextAlign from '@tiptap/extension-text-align'
import Youtube from '@tiptap/extension-youtube'
import api from '../api/client'

const props = defineProps({
  modelValue: { type: String, default: '' },
  globalTips: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue', 'update:globalTips'])

// ── Dialogs ──
const showTableDialog = ref(false)
const showImageUrlDialog = ref(false)
const showVideoDialog = ref(false)
const showLinkDialog = ref(false)
const showEmojiDialog = ref(false)
const showTipsDialog = ref(false)
const videoTab = ref('upload')

// ── File handling ──
const fileInput = ref(null)
const fileInputVideo = ref(null)
const imageUrl = ref('')
const imageAlt = ref('')
const linkUrl = ref('')
const youtubeUrl = ref('')
const videoFile = ref(null)
const uploadingVideo = ref(false)
const isDraggingOver = ref(false)
const textColor = ref('#000000')

// ── Table ──
const tableRows = ref(3)
const tableCols = ref(3)
const tableWithHeader = ref(true)

// ── Tips ──
const localTips = ref([...props.globalTips])

watch(() => props.globalTips, (v) => { localTips.value = [...v] }, { deep: true })

// ── Fonts ──
const fontFamilies = [
  { label: 'Arial', value: 'Arial, sans-serif' },
  { label: 'Georgia', value: 'Georgia, serif' },
  { label: 'Times New Roman', value: 'Times New Roman, serif' },
  { label: 'Courier New', value: 'Courier New, monospace' },
  { label: 'Verdana', value: 'Verdana, sans-serif' },
  { label: 'Trebuchet MS', value: 'Trebuchet MS, sans-serif' },
  { label: 'Impact', value: 'Impact, sans-serif' },
  { label: 'Comic Sans MS', value: 'Comic Sans MS, cursive' },
]
const fontSizes = [12, 14, 16, 18, 20, 22, 24, 28, 32, 36, 48]
const emojis = [
  '🎯','💡','📚','✅','❌','⚠️','💪','🎉','🔥','⭐',
  '👑','👉','🚢','🔄','📝','📖','🎓','🏠','🌟','✨',
  '👀','🤔','💭','🗣️','👂','📋','📊','🔢','🔤','✏️',
  '🧠','💯','👍','👎','❤️','🧡','💛','💚','💙','💜',
  '🌈','☀️','🌙','⭐','🌸','🌺','🍎','🥕','⚽','🎸',
]

const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit.configure({ heading: { levels: [1,2,3] }, table: false }),
    Underline, TextStyle, FontFamily, Color, Highlight,
    Link.configure({ openOnClick: false, HTMLAttributes: { target:'_blank', rel:'noopener noreferrer' } }),
    Image.configure({ inline: false, allowBase64: true }),
    Table.configure({ resizable: true }), TableHeader, TableRow, TableCell,
    TextAlign.configure({ types: ['heading','paragraph'] }),
    Youtube.configure({ controls: false, nocookie: true }),
  ],
  onUpdate: () => {
    const html = editor.value?.getHTML() || ''
    emit('update:modelValue', html)
  },
})

watch(() => props.modelValue, (val) => {
  if (editor.value && val !== editor.value.getHTML()) {
    editor.value.commands.setContent(val, false)
  }
})

// ── Text Color ──
function setTextColor(color) { editor.value?.chain().focus().setColor(color).run() }

// ── Emojis ──
function insertEmoji(emoji) {
  editor.value?.chain().focus().insertContent(emoji).run()
  showEmojiDialog.value = false
}

// ── Table ──
function insertCustomTable() {
  editor.value?.chain().focus().insertTable({ rows: tableRows.value, cols: tableCols.value, withHeaderRow: tableWithHeader.value }).run()
  showTableDialog.value = false
}

// ── Images ──
function triggerImageUpload() { fileInput.value?.click() }

async function onFileSelected(event) {
  const file = event.target.files?.[0]
  if (file) await uploadAndInsertFile(file)
  event.target.value = ''
}

async function uploadAndInsertFile(file) {
  try {
    const formData = new FormData()
    formData.append('file', file)

    if (file.type.startsWith('image/')) {
      const { data } = await api.post('/admin/upload/image', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
      editor.value?.chain().focus().setImage({ src: data.url }).run()
    } else {
      const { data } = await api.post('/admin/upload/video', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
      editor.value?.chain().focus().setYoutubeVideo({ src: data.url, width: 640, height: 360 }).run()
    }
  } catch (e) {
    alert('Error al subir el archivo: ' + (e.response?.data?.detail || e.message))
  }
}

function insertImageFromUrl() {
  if (imageUrl.value) {
    editor.value?.chain().focus().setImage({ src: imageUrl.value, alt: imageAlt.value || '' }).run()
    showImageUrlDialog.value = false
    imageUrl.value = ''
    imageAlt.value = ''
  }
}

// ── Video ──
function onVideoFileSelected(event) {
  videoFile.value = event.target.files?.[0] || null
}

async function insertVideoContent() {
  if (videoFile.value) {
    await uploadAndInsertFile(videoFile.value)
    videoFile.value = null
  } else if (youtubeUrl.value) {
    editor.value?.chain().focus().setYoutubeVideo({ src: youtubeUrl.value, width: 640, height: 360 }).run()
    youtubeUrl.value = ''
  }
  showVideoDialog.value = false
}

// ── Drag & drop ──
function onDragOver(event) {
  if (event.dataTransfer?.types.includes('Files')) isDraggingOver.value = true
}

function onDragLeave() { isDraggingOver.value = false }

async function onDrop(event) {
  isDraggingOver.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) await uploadAndInsertFile(file)
}

// ── Link ──
function toggleLink() {
  if (editor.value?.isActive('link')) linkUrl.value = editor.value.getAttributes('link').href || ''
  else linkUrl.value = ''
  showLinkDialog.value = true
}

function applyLink() {
  if (linkUrl.value) editor.value?.chain().focus().setLink({ href: linkUrl.value }).run()
  showLinkDialog.value = false; linkUrl.value = ''
}

function removeLink() {
  editor.value?.chain().focus().unsetLink().run()
  showLinkDialog.value = false; linkUrl.value = ''
}

// ── Tips ──
function saveTips() {
  emit('update:globalTips', [...localTips.value])
  showTipsDialog.value = false
}

onBeforeUnmount(() => editor.value?.destroy())
</script>

<style scoped>
.editor-wrapper {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  background: white;
}

.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
  padding: 8px;
  border-bottom: 1px solid #e0e0e0;
  background: #fafafa;
}

.editor-content {
  padding: 16px;
  min-height: 250px;
}

.editor-content :deep(p) { margin: 0 0 8px 0; line-height: 1.6; }
.editor-content :deep(h1) { font-size: 1.8em; margin: 0 0 12px 0; }
.editor-content :deep(h2) { font-size: 1.4em; margin: 0 0 10px 0; }
.editor-content :deep(h3) { font-size: 1.2em; margin: 0 0 8px 0; }
.editor-content :deep(table) { border-collapse: collapse; width: 100%; margin: 8px 0; }
.editor-content :deep(th), .editor-content :deep(td) { border: 1px solid #ccc; padding: 8px; text-align: left; }
.editor-content :deep(th) { background: #f5f5f5; font-weight: bold; }
.editor-content :deep(blockquote) { border-left: 3px solid #ccc; margin: 8px 0; padding: 4px 12px; color: #666; }
.editor-content :deep(img) { max-width: 100%; border-radius: 8px; }
.editor-content :deep(.youtube-video) { margin: 8px 0; }
.editor-content :deep(a) { color: #4CAF50; }
.editor-content :deep(ul) { list-style-type: disc; padding-left: 20px; margin: 8px 0; }
.editor-content :deep(ol) { list-style-type: decimal; padding-left: 20px; margin: 8px 0; }

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 4px;
}

.emoji-btn {
  width: 38px; height: 38px;
  border: none; background: transparent;
  border-radius: 8px; cursor: pointer;
  font-size: 20px;
  display: flex; align-items: center; justify-content: center;
}
.emoji-btn:hover { background: #e0e0e0; }

.drop-overlay {
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(255,255,255,.95);
  display: flex; align-items: center; justify-content: center; z-index: 10;
}
.drop-message { text-align: center; color: #4CAF50; }
</style>