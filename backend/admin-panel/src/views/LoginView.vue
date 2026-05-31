<template>
  <v-container class="fill-height" fluid>
    <v-row justify="center" align="center">
      <v-col cols="12" sm="6" md="4">
        <v-card elevation="8" class="pa-6" rounded="lg">
          <v-card-title class="text-center text-primary text-h4 mb-4">
            AppSchool
          </v-card-title>
          <v-card-subtitle class="text-center mb-6">
            Panel de Administración
          </v-card-subtitle>

          <v-text-field
            v-model="user"
            label="Usuario"
            prepend-inner-icon="mdi-account"
            variant="outlined"
            @keyup.enter="doLogin"
          />
          <v-text-field
            v-model="pass"
            label="Contraseña"
            type="password"
            prepend-inner-icon="mdi-lock"
            variant="outlined"
            class="mt-2"
            @keyup.enter="doLogin"
          />

          <v-alert v-if="error" type="error" variant="tonal" class="mt-2" density="compact">
            {{ error }}
          </v-alert>

          <v-btn
            block
            color="primary"
            size="large"
            class="mt-4"
            :loading="loading"
            @click="doLogin"
          >
            Ingresar
          </v-btn>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const user = ref('')
const pass = ref('')
const error = ref('')
const loading = ref(false)

async function doLogin() {
  if (!user.value || !pass.value) return
  loading.value = true
  error.value = ''
  try {
    await auth.login(user.value, pass.value)
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.detail || 'Error de autenticación'
  } finally {
    loading.value = false
  }
}
</script>
