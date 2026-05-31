<template>
  <div class="editor-container" v-if="editor">
    <div class="toolbar">
      <v-btn-group density="compact" variant="text">
        <v-tooltip text="Negrita (Ctrl+B)" location="bottom"><template #activator="{ props: tp }"><v-btn icon="mdi-format-bold" v-bind="tp" size="small" :color="editor.isActive('bold')?'primary':''" @click="editor.chain().focus().toggleBold().run()" /></template></v-tooltip>
        <v-tooltip text="Cursiva (Ctrl+I)" location="bottom"><template #activator="{ props: tp }"><v-btn icon="mdi-format-italic" v-bind="tp" size="small" :color="editor.isActive('italic')?'primary':''" @click="editor.chain().focus().toggleItalic().run()" /></template></v-tooltip>
        <v-tooltip text="Subrayado (Ctrl+U)" location="bottom"><template #activator="{ props: tp }"><v-btn icon="mdi-format-underline" v-bind="tp" size="small" :color="editor.isActive('underline')?'primary':''" @click="editor.chain().focus().toggleUnderline().run()" /></template></v-tooltip>
        <v-tooltip text="Tachado" location="bottom"><template #activator="{ props: tp }"><v-btn icon="mdi-format-strikethrough-variant" v-bind="tp" size="small" :color="editor.isActive('strike')?'primary':''" @click="editor.chain().focus().toggleStrike().run()" /></template></v-tooltip>
      </v-btn-group>
      <v-divider vertical class="mx-1" />
      <v-btn-group density="compact" variant="text">
        <v-tooltip text="Título grande" location="bottom"><template #activator="{ props: tp }"><v-btn icon="mdi-format-header-1" v-bind="tp" size="small" :color="editor.isActive('heading',{level:1})?'primary':''" @click="editor.chain().focus().toggleHeading({level:1}).run()" /></template></v-tooltip>
        <v-tooltip text="Título mediano" location="bottom"><template #activator="{ props: tp }"><v-btn icon="mdi-format-header-2" v-bind="tp" size="small" :color="editor.isActive('heading',{level:2})?'primary':''" @click="editor.chain().focus().toggleHeading({level:2}).run()" /></template></v-tooltip>
        <v-tooltip text="Título chico" location="bottom"><template #activator="{ props: tp }"><v-btn icon="mdi-format-header-3" v-bind="tp" size="small" :color="editor.isActive('heading',{level:3})?'primary':''" @click="editor.chain().focus().toggleHeading({level:3}).run()" /></template></v-tooltip>
      </v-btn-group>
      <v-divider vertical class="mx-1" />
      <v-btn-group density="compact" variant="text">
        <v-tooltip text="Lista con viñetas" location="bottom"><template #activator="{ props: tp }"><v-btn icon="mdi-format-list-bulleted" v-bind="tp" size="small" :color="editor.isActive('bulletList')?'primary':''" @click="editor.chain().focus().toggleBulletList().run()" /></template></v-tooltip>
        <v-tooltip text="Lista numerada" location="bottom"><template #activator="{ props: tp }"><v-btn icon="mdi-format-list-numbered" v-bind="tp" size="small" :color="editor.isActive('orderedList')?'primary':''" @click="editor.chain().focus().toggleOrderedList().run()" /></template></v-tooltip>
        <v-tooltip text="Cita textual" location="bottom"><template #activator="{ props: tp }"><v-btn icon="mdi-format-quote-close" v-bind="tp" size="small" :color="editor.isActive('blockquote')?'primary':''" @click="editor.chain().focus().toggleBlockquote().run()" /></template></v-tooltip>
      </v-btn-group>
      <v-divider vertical class="mx-1" />
      <v-btn-group density="compact" variant="text">
        <v-tooltip text="Color de texto" location="bottom"><template #activator="{ props: tp }"><v-menu :close-on-content-click="false"><template #activator="{ props }"><v-btn icon="mdi-palette" size="small" v-bind="{...tp,...props}" /></template><v-color-picker v-model="selectedColor" @update:model-value="setColor" hide-inputs width="200" /></v-menu></template></v-tooltip>
        <v-tooltip text="Resaltar texto" location="bottom"><template #activator="{ props: tp }"><v-btn icon="mdi-highlighter" v-bind="tp" size="small" :color="editor.isActive('highlight')?'primary':''" @click="editor.chain().focus().toggleHighlight().run()" /></template></v-tooltip>
      </v-btn-group>
      <v-divider vertical class="mx-1" />
      <v-btn-group density="compact" variant="text">
        <v-tooltip text="Insertar tabla 3x3" location="bottom"><template #activator="{ props: tp }"><v-btn icon="mdi-table-plus" v-bind="tp" size="small" @click="insertTable" /></template></v-tooltip>
        <v-tooltip text="Insertar imagen" location="bottom"><template #activator="{ props: tp }"><v-btn icon="mdi-image" v-bind="tp" size="small" @click="insertImage" /></template></v-tooltip>
        <v-tooltip text="Insertar video de YouTube" location="bottom"><template #activator="{ props: tp }"><v-btn icon="mdi-youtube" v-bind="tp" size="small" @click="insertVideo" /></template></v-tooltip>
        <v-tooltip text="Insertar link" location="bottom"><template #activator="{ props: tp }"><v-btn icon="mdi-link" v-bind="tp" size="small" :color="editor.isActive('link')?'primary':''" @click="toggleLink" /></template></v-tooltip>
      </v-btn-group>
      <v-divider vertical class="mx-1" />
      <v-btn-group density="compact" variant="text">
        <v-tooltip text="Deshacer (Ctrl+Z)" location="bottom"><template #activator="{ props: tp }"><v-btn icon="mdi-undo" v-bind="tp" size="small" @click="editor.chain().focus().undo().run()" /></template></v-tooltip>
        <v-tooltip text="Rehacer (Ctrl+Y)" location="bottom"><template #activator="{ props: tp }"><v-btn icon="mdi-redo" v-bind="tp" size="small" @click="editor.chain().focus().redo().run()" /></template></v-tooltip>
      </v-btn-group>
    </div>
    <editor-content :editor="editor" class="editor-content" />
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import { Underline } from '@tiptap/extension-underline'
import { TextStyle } from '@tiptap/extension-text-style'
import { Color } from '@tiptap/extension-color'
import { Highlight } from '@tiptap/extension-highlight'
import { Link } from '@tiptap/extension-link'
import { Image } from '@tiptap/extension-image'
import { Table } from '@tiptap/extension-table'
import { TableHeader } from '@tiptap/extension-table-header'
import { TableRow } from '@tiptap/extension-table-row'
import { TableCell } from '@tiptap/extension-table-cell'
import { Youtube } from '@tiptap/extension-youtube'

const props = defineProps({ modelValue: { type: String, default: '' } })
const emit = defineEmits(['update:modelValue'])
const selectedColor = ref('#000000')

const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit.configure({ heading: { levels: [1,2,3] } }),
    Underline, TextStyle, Color, Highlight,
    Link.configure({ openOnClick: false }),
    Image.configure({ inline: false }),
    Table.configure({ resizable: true }),
    TableHeader, TableRow, TableCell,
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

function setColor(color) {
  editor.value?.chain().focus().setColor(color).run()
}

function insertTable() {
  editor.value?.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()
}

function insertImage() {
  const url = prompt('URL de la imagen:')
  if (url) editor.value?.chain().focus().setImage({ src: url }).run()
}

function insertVideo() {
  const url = prompt('URL del video de YouTube:')
  if (url) editor.value?.chain().focus().setYoutubeVideo({ src: url, width: 640, height: 360 }).run()
}

function toggleLink() {
  if (editor.value?.isActive('link')) {
    editor.value.chain().focus().unsetLink().run()
  } else {
    const url = prompt('URL del link:')
    if (url) editor.value?.chain().focus().setLink({ href: url }).run()
  }
}

onBeforeUnmount(() => editor.value?.destroy())
</script>

<style scoped>
.editor-container { border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; }
.toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 2px; padding: 8px; border-bottom: 1px solid #e0e0e0; background: #fafafa; }
.editor-content { padding: 16px; min-height: 300px; max-height: 500px; overflow-y: auto; }
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
</style>
