<template>
  <div class="d-flex align-start py-2">
    <v-avatar :color="avatarColor" size="32" class="mr-3 mt-1" style="flex-shrink: 0;">
      <v-icon size="16" color="white">{{ avatarIcon }}</v-icon>
    </v-avatar>
    <div class="flex-grow-1">
      <div class="text-body-2">
        <span class="font-weight-medium">{{ eventText }}</span>
      </div>
      <div class="text-caption text-grey">
        {{ event.topicName }}
        <template v-if="event.unitName"> → {{ event.unitName }}</template>
        <template v-if="event.blockIndex != null"> → Bloque {{ event.blockIndex + 1 }}</template>
        <span class="ml-2">{{ formatTime(event.createdAt) }}</span>
      </div>
      <div v-if="event.eventData?.score != null" class="d-flex mt-1" style="gap: 6px;">
        <v-chip size="x-small" variant="tonal" :color="event.eventData.score > 0 ? 'success' : 'grey'">
          Score: {{ event.eventData.score }}
        </v-chip>
        <v-chip v-if="event.eventData.accuracy != null" size="x-small" variant="tonal" :color="event.eventData.accuracy >= 70 ? 'success' : 'warning'">
          {{ Math.round(event.eventData.accuracy) }}%
        </v-chip>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  event: { type: Object, required: true }
})

const avatarColor = computed(() => {
  switch (props.event.eventType) {
    case 'sync': return 'primary'
    case 'reset': return 'error'
    case 'redo_marked': return 'warning'
    case 'completed': return 'success'
    default: return 'grey'
  }
})

const avatarIcon = computed(() => {
  switch (props.event.eventType) {
    case 'sync': return 'mdi-sync'
    case 'reset': return 'mdi-restart'
    case 'redo_marked': return 'mdi-bookmark'
    case 'completed': return 'mdi-check'
    default: return 'mdi-information'
  }
})

const eventText = computed(() => {
  switch (props.event.eventType) {
    case 'sync': return 'Sincronizó progreso'
    case 'reset': return 'Progreso reseteado'
    case 'redo_marked': return 'Marcado para rehacer'
    case 'completed': return 'Completado'
    default: return props.event.eventType
  }
})

function formatTime(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('es-AR', {
    day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit'
  })
}
</script>
