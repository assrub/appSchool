<template>
  <v-app>
    <router-view />
    <v-snackbar
      v-model="snackbar.state.value.show"
      :color="snackbar.state.value.color"
      :timeout="snackbar.state.value.timeout"
      location="top right"
      rounded="lg"
      elevation="4"
    >
      <div class="d-flex align-center">
        <v-icon v-if="snackbar.state.value.color === 'error'" class="mr-2" size="20">mdi-alert-circle</v-icon>
        <v-icon v-else-if="snackbar.state.value.color === 'success'" class="mr-2" size="20">mdi-check-circle</v-icon>
        <v-icon v-else-if="snackbar.state.value.color === 'warning'" class="mr-2" size="20">mdi-alert</v-icon>
        <v-icon v-else class="mr-2" size="20">mdi-information</v-icon>
        <span class="text-body-2">{{ snackbar.state.value.message }}</span>
      </div>
      <template #actions>
        <v-btn icon variant="text" size="small" @click="snackbar.state.value.show = false">
          <v-icon size="18">mdi-close</v-icon>
        </v-btn>
      </template>
    </v-snackbar>
  </v-app>
</template>

<script setup>
import { provide } from 'vue'
import { useSnackbar } from './composables/useSnackbar'

const snackbar = useSnackbar()

provide('snackbar', snackbar)
</script>