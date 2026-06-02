<template>
  <v-layout>
    <v-navigation-drawer
      v-model="drawer"
      color="primary"
      :rail="rail"
      permanent
    >
      <div v-if="!rail" class="pa-4">
        <div class="text-h6 text-white font-weight-bold">AppSchool</div>
        <div class="text-caption text-grey-lighten-1">Admin Panel</div>
      </div>

      <div v-if="rail" class="pa-2 text-center">
        <v-icon icon="mdi-book-open-variant" color="white" size="28" />
      </div>

      <v-divider v-if="!rail" class="mb-2" />

      <v-list nav density="compact">
        <v-list-item
          prepend-icon="mdi-view-dashboard"
          title="Dashboard"
          to="/"
          exact
          rounded="lg"
          class="mb-1"
        />
        <v-list-item
          prepend-icon="mdi-bookshelf"
          title="Materias"
          to="/subjects"
          rounded="lg"
          class="mb-1"
        />
        <v-list-item
          prepend-icon="mdi-chart-bar"
          title="Progreso"
          to="/progress"
          rounded="lg"
          class="mb-1"
        />
        <v-list-item
          prepend-icon="mdi-account-group"
          title="Usuarios"
          to="/users"
          rounded="lg"
          class="mb-1"
        />
      </v-list>

      <v-divider v-if="!rail" />

      <v-list nav density="compact">
        <v-list-subheader v-if="!rail" class="text-caption text-grey-lighten-2">Herramientas</v-list-subheader>
        <v-list-item
          prepend-icon="mdi-wrench"
          title="Sistema"
          to="/system"
          rounded="lg"
          class="mb-1"
        />
      </v-list>

      <template #append>
        <v-divider />
        <v-list nav density="compact">
          <v-list-item
            prepend-icon="mdi-logout"
            title="Salir"
            @click="logout"
            rounded="lg"
          />
        </v-list>
      </template>
    </v-navigation-drawer>

    <v-app-bar color="white" elevation="1" density="compact">
      <v-btn
        icon="mdi-menu"
        variant="text"
        @click="toggleDrawer"
        class="ml-2"
      />

      <v-breadcrumbs :items="breadcrumbs" class="py-0">
        <template #divider>
          <v-icon icon="mdi-chevron-right" size="small" />
        </template>
        <template #title="{ item }">
          <router-link
            v-if="item.to"
            :to="item.to"
            class="text-decoration-none text-primary"
          >
            {{ item.title }}
          </router-link>
          <span v-else class="text-grey-darken-1">{{ item.title }}</span>
        </template>
      </v-breadcrumbs>

      <v-spacer />

      <v-chip class="mr-4" variant="tonal" color="primary" size="small">
        <v-icon start size="18">mdi-account</v-icon>
        {{ username }}
      </v-chip>
    </v-app-bar>

    <v-main style="min-height: 100vh; background: #f5f5f5">
      <v-container fluid class="pa-6">
        <router-view :key="$route.fullPath" />
      </v-container>
    </v-main>
  </v-layout>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const drawer = ref(true)
const rail = ref(true)
const username = computed(() => localStorage.getItem('username') || '')

function toggleDrawer() {
  rail.value = !rail.value
}

const breadcrumbs = computed(() => {
  const items = [{ title: 'Inicio', to: '/' }]

  const path = route.path

  if (path.startsWith('/subjects') && route.params.subjectId) {
    items.push({ title: 'Materias', to: '/subjects' })
    items.push({ title: 'Temas', disabled: true })
  } else if (path.startsWith('/topics') && route.params.topicId) {
    items.push({ title: 'Materias', to: '/subjects' })
    items.push({ title: 'Temas', disabled: true })
    items.push({ title: 'Unidades', disabled: true })
  } else if (path.startsWith('/units') && route.params.unitId) {
    items.push({ title: 'Materias', to: '/subjects' })
    items.push({ title: 'Temas', to: '/subjects' })
    items.push({ title: 'Unidades', disabled: true })
    items.push({ title: 'Bloques', disabled: true })
  } else if (path === '/subjects') {
    items.push({ title: 'Materias', disabled: true })
  } else if (path === '/progress') {
    items.push({ title: 'Materias', to: '/subjects' })
    items.push({ title: 'Progreso', disabled: true })
  } else if (path === '/users') {
    items.push({ title: 'Materias', to: '/subjects' })
    items.push({ title: 'Usuarios', disabled: true })
  } else if (path === '/script') {
    items.push({ title: 'Materias', to: '/subjects' })
    items.push({ title: 'Scripts', disabled: true })
  } else if (path.startsWith('/theory/')) {
    items.push({ title: 'Materias', to: '/subjects' })
    items.push({ title: 'Temas', to: '/subjects' })
    items.push({ title: 'Editor de Teoría', disabled: true })
  }

  return items
})

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  window.location.href = '/login'
}
</script>