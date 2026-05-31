<template>
  <div>
    <h1 class="text-h4 mb-6">Dashboard</h1>

    <v-row>
      <v-col cols="12" sm="6" md="3">
        <v-card rounded="lg" elevation="2">
          <v-card-item>
            <template #prepend>
              <v-icon color="primary" size="40">mdi-bookshelf</v-icon>
            </template>
            <v-card-title class="text-h5">{{ subjects.length }}</v-card-title>
            <v-card-subtitle>Materias</v-card-subtitle>
          </v-card-item>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="3">
        <v-card rounded="lg" elevation="2">
          <v-card-item>
            <template #prepend>
              <v-icon color="secondary" size="40">mdi-book-open-page-variant</v-icon>
            </template>
            <v-card-title class="text-h5">{{ totalTopics }}</v-card-title>
            <v-card-subtitle>Temas</v-card-subtitle>
          </v-card-item>
        </v-card>
      </v-col>
    </v-row>

    <v-card class="mt-6" rounded="lg" elevation="2">
      <v-card-title class="text-h6">Materias activas</v-card-title>
      <v-list>
        <v-list-item
          v-for="s in subjects"
          :key="s.id"
          :title="s.name"
          :subtitle="`ID: ${s.id}`"
          :to="`/subjects/${s.id}/topics`"
        >
          <template #prepend>
            <span class="text-h5">{{ s.icon }}</span>
          </template>
          <template #append>
            <v-icon>mdi-chevron-right</v-icon>
          </template>
        </v-list-item>
      </v-list>
      <v-card-text v-if="subjects.length === 0" class="text-center text-grey">
        No hay materias todavía. Creá una en "Materias".
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api/client'

const subjects = ref([])

const totalTopics = computed(() => {
  return subjects.value.reduce((acc, s) => acc + (s.topicsCount || 0), 0)
})

onMounted(async () => {
  const { data } = await api.get('/content/subjects')
  subjects.value = data.subjects || []
})
</script>
