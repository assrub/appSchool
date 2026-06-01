<template>
  <div class="editor-wrapper" v-if="editor">
    <!-- Toolbar -->
    <div class="toolbar">
      <!-- Font Family -->
      <v-menu :close-on-content-click="false">
        <template #activator="{ props: menuProps }">
          <v-btn v-bind="menuProps" variant="text" size="small" class="toolbar-btn" title="Fuente">
            <v-icon size="18">mdi-format-font</v-icon>
            <v-icon size="14">mdi-menu-down</v-icon>
          </v-btn>
        </template>
        <v-list density="compact" style="min-width: 180px">
          <v-list-item
            v-for="font in fontFamilies"
            :key="font.value"
            :active="editor.isActive('textStyle', { fontFamily: font.value })"
            @click="editor.chain().focus().setFontFamily(font.value).run()"
            density="compact"
          >
            <v-list-item-title :style="{ fontFamily: font.value }">{{ font.label }}</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>

      <!-- Font Size -->
      <v-menu :close-on-content-click="false">
        <template #activator="{ props: menuProps }">
          <v-btn v-bind="menuProps" variant="text" size="small" class="toolbar-btn" title="Tamaño">
            <v-icon size="18">mdi-format-size</v-icon>
            <v-icon size="14">mdi-menu-down</v-icon>
          </v-btn>
        </template>
        <v-list density="compact">
          <v-list-item
            v-for="size in fontSizes"
            :key="size"
            :active="editor.isActive('textStyle', { fontSize: size + 'px' })"
            @click="editor.chain().focus().setFontSize(size + 'px').run()"
            density="compact"
          >
            <v-list-item-title>{{ size }}px</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>

      <v-divider vertical class="mx-1" />

      <!-- Text Formatting -->
      <v-btn-group density="compact" variant="text">
        <v-tooltip text="Negrita (Ctrl+B)" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" :color="editor.isActive('bold')?'primary':''" @click="editor.chain().focus().toggleBold().run()">
              <v-icon size="18">mdi-format-bold</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
        <v-tooltip text="Cursiva (Ctrl+I)" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" :color="editor.isActive('italic')?'primary':''" @click="editor.chain().focus().toggleItalic().run()">
              <v-icon size="18">mdi-format-italic</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
        <v-tooltip text="Subrayado (Ctrl+U)" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" :color="editor.isActive('underline')?'primary':''" @click="editor.chain().focus().toggleUnderline().run()">
              <v-icon size="18">mdi-format-underline</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
        <v-tooltip text="Tachado" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" :color="editor.isActive('strike')?'primary':''" @click="editor.chain().focus().toggleStrike().run()">
              <v-icon size="18">mdi-format-strikethrough-variant</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
      </v-btn-group>

      <v-divider vertical class="mx-1" />

      <!-- Text Alignment -->
      <v-btn-group density="compact" variant="text">
        <v-tooltip text="Alinear izquierda" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" :color="editor.isActive({ textAlign: 'left' })?'primary':''" @click="editor.chain().focus().setTextAlign('left').run()">
              <v-icon size="18">mdi-format-align-left</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
        <v-tooltip text="Centrar" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" :color="editor.isActive({ textAlign: 'center' })?'primary':''" @click="editor.chain().focus().setTextAlign('center').run()">
              <v-icon size="18">mdi-format-align-center</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
        <v-tooltip text="Alinear derecha" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" :color="editor.isActive({ textAlign: 'right' })?'primary':''" @click="editor.chain().focus().setTextAlign('right').run()">
              <v-icon size="18">mdi-format-align-right</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
        <v-tooltip text="Justificar" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" :color="editor.isActive({ textAlign: 'justify' })?'primary':''" @click="editor.chain().focus().setTextAlign('justify').run()">
              <v-icon size="18">mdi-format-align-justify</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
      </v-btn-group>

      <v-divider vertical class="mx-1" />

      <!-- Headings -->
      <v-btn-group density="compact" variant="text">
        <v-tooltip text="Título 1" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" :color="editor.isActive('heading', {level:1})?'primary':''" @click="editor.chain().focus().toggleHeading({level:1}).run()">
              <v-icon size="18">mdi-format-header-1</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
        <v-tooltip text="Título 2" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" :color="editor.isActive('heading', {level:2})?'primary':''" @click="editor.chain().focus().toggleHeading({level:2}).run()">
              <v-icon size="18">mdi-format-header-2</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
        <v-tooltip text="Título 3" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" :color="editor.isActive('heading', {level:3})?'primary':''" @click="editor.chain().focus().toggleHeading({level:3}).run()">
              <v-icon size="18">mdi-format-header-3</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
      </v-btn-group>

      <v-divider vertical class="mx-1" />

      <!-- Lists & Quote -->
      <v-btn-group density="compact" variant="text">
        <v-tooltip text="Lista con viñetas" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" :color="editor.isActive('bulletList')?'primary':''" @click="editor.chain().focus().toggleBulletList().run()">
              <v-icon size="18">mdi-format-list-bulleted</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
        <v-tooltip text="Lista numerada" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" :color="editor.isActive('orderedList')?'primary':''" @click="editor.chain().focus().toggleOrderedList().run()">
              <v-icon size="18">mdi-format-list-numbered</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
        <v-tooltip text="Cita textual" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" :color="editor.isActive('blockquote')?'primary':''" @click="editor.chain().focus().toggleBlockquote().run()">
              <v-icon size="18">mdi-format-quote-close</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
      </v-btn-group>

      <v-divider vertical class="mx-1" />

      <!-- Colors -->
      <v-btn-group density="compact" variant="text">
        <v-tooltip text="Color de texto" location="bottom">
          <template #activator="{ props: tp }">
            <v-menu :close-on-content-click="false" location="bottom">
              <template #activator="{ props: menuProps }">
                <v-btn v-bind="{...tp, ...menuProps}" size="small" :color="editor.isActive('textStyle')?'primary':''">
                  <v-icon size="18">mdi-palette</v-icon>
                </v-btn>
              </template>
              <v-color-picker v-model="textColor" @update:model-value="setTextColor" hide-inputs width="280" />
            </v-menu>
          </template>
        </v-tooltip>
        <v-tooltip text="Resaltar texto" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" :color="editor.isActive('highlight')?'primary':''" @click="editor.chain().focus().toggleHighlight().run()">
              <v-icon size="18">mdi-highlighter</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
      </v-btn-group>

      <v-divider vertical class="mx-1" />

      <!-- Insert -->
      <v-btn-group density="compact" variant="text">
        <v-tooltip text="Insertar tabla" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" @click="showTableDialog = true">
              <v-icon size="18">mdi-table-plus</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
        <v-tooltip text="Subir imagen" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" @click="triggerImageUpload">
              <v-icon size="18">mdi-image</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
        <v-tooltip text="Insertar imagen desde URL" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" @click="insertImageByUrl">
              <v-icon size="18">mdi-link-image</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
        <v-tooltip text="Insertar video de YouTube" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" @click="insertVideo">
              <v-icon size="18">mdi-youtube</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
        <v-tooltip text="Insertar link" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" :color="editor.isActive('link')?'primary':''" @click="toggleLink">
              <v-icon size="18">mdi-link</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
        <v-tooltip text="Emojis" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" @click="showEmojiPicker = !showEmojiPicker">
              <v-icon size="18">mdi-emoticon</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
      </v-btn-group>

      <v-divider vertical class="mx-1" />

      <!-- Undo/Redo -->
      <v-btn-group density="compact" variant="text">
        <v-tooltip text="Deshacer (Ctrl+Z)" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" @click="editor.chain().focus().undo().run()">
              <v-icon size="18">mdi-undo</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
        <v-tooltip text="Rehacer (Ctrl+Y)" location="bottom">
          <template #activator="{ props: tp }">
            <v-btn v-bind="tp" size="small" @click="editor.chain().focus().redo().run()">
              <v-icon size="18">mdi-redo</v-icon>
            </v-btn>
          </template>
        </v-tooltip>
      </v-btn-group>
    </div>

    <!-- Emoji Picker -->
    <v-expand-transition>
      <div v-if="showEmojiPicker" class="emoji-picker">
        <div class="emoji-grid">
          <button
            v-for="emoji in emojis"
            :key="emoji"
            class="emoji-btn"
            @click="insertEmoji(emoji)"
          >
            {{ emoji }}
          </button>
        </div>
      </div>
    </v-expand-transition>

    <!-- Drop Zone Overlay -->
    <div
      v-if="isDraggingOver"
      class="drop-overlay"
      @dragover.prevent="onDragOver"
      @dragleave="onDragLeave"
      @drop.prevent="onDrop"
    >
      <div class="drop-message">
        <v-icon size="48" color="primary">mdi-cloud-upload</v-icon>
        <p class="text-h6 mt-2">Soltá la imagen aquí</p>
      </div>
    </div>

    <!-- Editor Content -->
    <div
      class="editor-content"
      @dragover.prevent="onDragOver"
      @dragleave="onDragLeave"
      @drop.prevent="onDrop"
    >
      <editor-content :editor="editor" />
    </div>

    <!-- Hidden file input -->
    <input
      ref="imageInput"
      type="file"
      accept="image/*"
      style="display: none"
      @change="onImageSelected"
    />

    <!-- Table Dialog -->
    <v-dialog v-model="showTableDialog" max-width="400">
      <v-card>
        <v-card-title class="text-h6 pa-4">Insertar Tabla</v-card-title>
        <v-card-text class="pa-4 pt-0">
          <v-row>
            <v-col cols="6">
              <v-text-field
                v-model.number="tableRows"
                label="Filas"
                type="number"
                min="1"
                max="20"
                variant="outlined"
                density="compact"
              />
            </v-col>
            <v-col cols="6">
              <v-text-field
                v-model.number="tableCols"
                label="Columnas"
                type="number"
                min="1"
                max="10"
                variant="outlined"
                density="compact"
              />
            </v-col>
          </v-row>
          <v-checkbox
            v-model="tableWithHeader"
            label="Incluir fila de encabezado"
            density="compact"
          />
          <v-select
            v-model="tableStyle"
            label="Estilo"
            :items="[
              { title: 'Simple con bordes', value: 'bordered' },
              { title: 'Zebra (filas alternadas)', value: 'zebra' },
              { title: 'Sin bordes', value: 'plain' }
            ]"
            variant="outlined"
            density="compact"
          />
        </v-card-text>
        <v-card-actions class="pa-4 pt-0">
          <v-spacer />
          <v-btn variant="text" @click="showTableDialog = false">Cancelar</v-btn>
          <v-btn color="primary" variant="flat" @click="insertCustomTable">Insertar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Image URL Dialog -->
    <v-dialog v-model="showImageUrlDialog" max-width="400">
      <v-card>
        <v-card-title class="text-h6 pa-4">Insertar Imagen</v-card-title>
        <v-card-text class="pa-4 pt-0">
          <v-text-field
            v-model="imageUrl"
            label="URL de la imagen"
            placeholder="https://..."
            variant="outlined"
            density="compact"
          />
          <v-text-field
            v-model="imageAlt"
            label="Texto alternativo (descripción)"
            placeholder="Descripción de la imagen"
            variant="outlined"
            density="compact"
            class="mt-2"
          />
        </v-card-text>
        <v-card-actions class="pa-4 pt-0">
          <v-spacer />
          <v-btn variant="text" @click="showImageUrlDialog = false">Cancelar</v-btn>
          <v-btn color="primary" variant="flat" @click="insertImageFromUrl" :disabled="!imageUrl">Insertar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Link Dialog -->
    <v-dialog v-model="showLinkDialog" max-width="400">
      <v-card>
        <v-card-title class="text-h6 pa-4">Insertar Link</v-card-title>
        <v-card-text class="pa-4 pt-0">
          <v-text-field
            v-model="linkUrl"
            label="URL"
            placeholder="https://..."
            variant="outlined"
            density="compact"
          />
        </v-card-text>
        <v-card-actions class="pa-4 pt-0">
          <v-btn v-if="editor.isActive('link')" variant="text" color="error" @click="removeLink">Quitar link</v-btn>
          <v-spacer />
          <v-btn variant="text" @click="showLinkDialog = false">Cancelar</v-btn>
          <v-btn color="primary" variant="flat" @click="applyLink" :disabled="!linkUrl">Aplicar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Video Dialog -->
    <v-dialog v-model="showVideoDialog" max-width="400">
      <v-card>
        <v-card-title class="text-h6 pa-4">Insertar Video de YouTube</v-card-title>
        <v-card-text class="pa-4 pt-0">
          <v-text-field
            v-model="videoUrl"
            label="URL de YouTube"
            placeholder="https://www.youtube.com/watch?v=..."
            variant="outlined"
            density="compact"
          />
        </v-card-text>
        <v-card-actions class="pa-4 pt-0">
          <v-spacer />
          <v-btn variant="text" @click="showVideoDialog = false">Cancelar</v-btn>
          <v-btn color="primary" variant="flat" @click="insertVideoFromUrl" :disabled="!videoUrl">Insertar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import { Underline } from '@tiptap/extension-underline'
import { TextStyle } from '@tiptap/extension-text-style'
import { FontFamily } from '@tiptap/extension-font-family'
import { Color } from '@tiptap/extension-color'
import { Highlight } from '@tiptap/extension-highlight'
import { Link } from '@tiptap/extension-link'
import { Image } from '@tiptap/extension-image'
import { Table } from '@tiptap/extension-table'
import { TableHeader } from '@tiptap/extension-table-header'
import { TableRow } from '@tiptap/extension-table-row'
import { TableCell } from '@tiptap/extension-table-cell'
import { TextAlign } from '@tiptap/extension-text-align'
import { Youtube } from '@tiptap/extension-youtube'
import api from '../api/client'

const props = defineProps({ modelValue: { type: String, default: '' } })
const emit = defineEmits(['update:modelValue'])

const imageInput = ref(null)
const textColor = ref('#000000')
const isDraggingOver = ref(false)
const showEmojiPicker = ref(false)
const showTableDialog = ref(false)
const showImageUrlDialog = ref(false)
const showLinkDialog = ref(false)
const showVideoDialog = ref(false)

const tableRows = ref(3)
const tableCols = ref(3)
const tableWithHeader = ref(true)
const tableStyle = ref('bordered')

const imageUrl = ref('')
const imageAlt = ref('')
const linkUrl = ref('')
const videoUrl = ref('')

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
  '🎯', '💡', '📚', '✅', '❌', '⚠️', '💪', '🎉', '🔥', '⭐',
  '👑', '👉', '🚢', '🔄', '📝', '📖', '🎓', '🏠', '🌟', '✨',
  '👀', '🤔', '💭', '🗣️', '👂', '📋', '📊', '🔢', '🔤', '✏️',
  '🧠', '💯', '👍', '👎', '❤️', '🧡', '💛', '💚', '💙', '💜',
  '🌈', '☀️', '🌙', '⭐', '🌸', '🌺', '🍎', '🥕', '⚽', '🎸',
]

const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit.configure({
      heading: { levels: [1, 2, 3] },
      table: false,
    }),
    Underline,
    TextStyle,
    FontFamily,
    Color,
    Highlight,
    Link.configure({ openOnClick: false, HTMLAttributes: { target: '_blank', rel: 'noopener noreferrer' } }),
    Image.configure({ inline: false, allowBase64: true }),
    Table.configure({ resizable: true }),
    TableHeader,
    TableRow,
    TableCell,
    TextAlign.configure({ types: ['heading', 'paragraph'] }),
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

function setTextColor(color) {
  editor.value?.chain().focus().setColor(color).run()
}

function insertEmoji(emoji) {
  editor.value?.chain().focus().insertContent(emoji).run()
  showEmojiPicker.value = false
}

function insertTable() {
  editor.value?.chain().focus().insertTable({ rows: tableRows.value, cols: tableCols.value, withHeaderRow: tableWithHeader.value }).run()
  showTableDialog.value = false
}

function insertCustomTable() {
  editor.value?.chain().focus().insertTable({
    rows: tableRows.value,
    cols: tableCols.value,
    withHeaderRow: tableWithHeader.value
  }).run()
  showTableDialog.value = false
}

function triggerImageUpload() {
  imageInput.value?.click()
}

async function onImageSelected(event) {
  const file = event.target.files?.[0]
  if (file) {
    await uploadAndInsertImage(file)
  }
  event.target.value = ''
}

async function uploadAndInsertImage(file) {
  try {
    const formData = new FormData()
    formData.append('file', file)

    const { data } = await api.post('/admin/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    editor.value?.chain().focus().setImage({ src: data.url, alt: imageAlt.value || '' }).run()
    imageAlt.value = ''
  } catch (e) {
    console.error('Error uploading image:', e)
    alert('Error al subir la imagen. Verificá que el archivo sea válido y no supere 5MB.')
  }
}

function insertImageByUrl() {
  showImageUrlDialog.value = true
  imageUrl.value = ''
  imageAlt.value = ''
}

function insertImageFromUrl() {
  if (imageUrl.value) {
    editor.value?.chain().focus().setImage({ src: imageUrl.value, alt: imageAlt.value || '' }).run()
    showImageUrlDialog.value = false
    imageUrl.value = ''
    imageAlt.value = ''
  }
}

function onDragOver(event) {
  if (event.dataTransfer?.types.includes('Files')) {
    isDraggingOver.value = true
  }
}

function onDragLeave() {
  isDraggingOver.value = false
}

async function onDrop(event) {
  isDraggingOver.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file && file.type.startsWith('image/')) {
    await uploadAndInsertImage(file)
  }
}

function toggleLink() {
  if (editor.value?.isActive('link')) {
    linkUrl.value = editor.value.getAttributes('link').href || ''
  } else {
    linkUrl.value = ''
  }
  showLinkDialog.value = true
}

function applyLink() {
  if (linkUrl.value) {
    editor.value?.chain().focus().setLink({ href: linkUrl.value }).run()
  }
  showLinkDialog.value = false
  linkUrl.value = ''
}

function removeLink() {
  editor.value?.chain().focus().unsetLink().run()
  showLinkDialog.value = false
  linkUrl.value = ''
}

function insertVideo() {
  showVideoDialog.value = true
  videoUrl.value = ''
}

function insertVideoFromUrl() {
  if (videoUrl.value) {
    editor.value?.chain().focus().setYoutubeVideo({ src: videoUrl.value, width: 640, height: 360 }).run()
    showVideoDialog.value = false
    videoUrl.value = ''
  }
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

.toolbar-btn {
  text-transform: none !important;
}

.editor-content {
  padding: 16px;
  min-height: 300px;
  max-height: 500px;
  overflow-y: auto;
}

.editor-content :deep(p) { margin: 0 0 8px 0; }
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

.emoji-picker {
  border-bottom: 1px solid #e0e0e0;
  padding: 12px;
  background: #fafafa;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 4px;
}

.emoji-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.emoji-btn:hover {
  background: #e0e0e0;
}

.drop-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.95);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.drop-message {
  text-align: center;
  color: #4CAF50;
}
</style>