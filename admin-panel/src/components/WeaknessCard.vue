<template>
  <v-card rounded="lg" :color="cardColor" variant="tonal" class="mb-3">
    <v-card-text class="pa-4">
      <div class="d-flex align-center mb-2">
        <v-icon :color="iconColor" class="mr-2">{{ icon }}</v-icon>
        <div class="text-body-1 font-weight-bold flex-grow-1">{{ weakness.unitId }}</div>
        <v-chip :color="trendColor" size="x-small" variant="flat">{{ trendLabel }}</v-chip>
      </div>
      <div class="text-body-2 text-grey-darken-2 mb-2">{{ weakness.description }}</div>
      <div class="d-flex align-center" style="gap: 8px;">
        <v-chip size="x-small" variant="tonal" color="info">
          Intentos: {{ weakness.resetCount + 1 }}
        </v-chip>
        <v-chip v-if="weakness.firstAttemptAccuracy != null" size="x-small" variant="tonal" :color="weakness.firstAttemptAccuracy >= 70 ? 'success' : 'warning'">
          Antes: {{ Math.round(weakness.firstAttemptAccuracy) }}%
        </v-chip>
        <v-chip v-if="weakness.currentAccuracy != null" size="x-small" variant="tonal" :color="weakness.currentAccuracy >= 70 ? 'success' : weakness.currentAccuracy >= 50 ? 'warning' : 'error'">
          Ahora: {{ Math.round(weakness.currentAccuracy) }}%
        </v-chip>
      </div>
      <div class="text-caption text-grey-darken-1 mt-2 font-italic">{{ weakness.recommendation }}</div>
      <div class="d-flex mt-3" style="gap: 8px;">
        <v-btn size="small" variant="tonal" color="orange" @click="$emit('redo', weakness)">
          <v-icon start size="16">mdi-restart</v-icon> Rehacer
        </v-btn>
        <v-btn size="small" variant="tonal" color="error" @click="$emit('reset', weakness)">
          <v-icon start size="16">mdi-delete</v-icon> Resetear
        </v-btn>
      </div>
    </v-card-text>
  </v-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  weakness: { type: Object, required: true }
})

defineEmits(['redo', 'reset'])

const cardColor = computed(() => {
  if (props.weakness.trend === 'declining') return 'error'
  if (props.weakness.trend === 'improving') return 'success'
  return 'warning'
})

const iconColor = computed(() => {
  if (props.weakness.trend === 'declining') return 'error'
  if (props.weakness.trend === 'improving') return 'success'
  return 'warning'
})

const icon = computed(() => {
  if (props.weakness.trend === 'declining') return 'mdi-trending-down'
  if (props.weakness.trend === 'improving') return 'mdi-trending-up'
  return 'mdi-alert-circle'
})

const trendColor = computed(() => {
  if (props.weakness.trend === 'declining') return 'error'
  if (props.weakness.trend === 'improving') return 'success'
  return 'warning'
})

const trendLabel = computed(() => {
  if (props.weakness.trend === 'declining') return 'Empeorando'
  if (props.weakness.trend === 'improving') return 'Mejorando'
  return 'Estancado'
})
</script>
