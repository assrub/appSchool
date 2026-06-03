<template>
  <div>
    <div class="d-flex align-center mb-6">
      <h1 class="text-h4">Progreso de Estudiantes</h1>
      <v-spacer />
      <v-chip :color="wsConnected ? 'success' : 'grey'" variant="tonal" size="small" class="mr-3">
        <v-icon start size="14">{{ wsConnected ? 'mdi-wifi' : 'mdi-wifi-off' }}</v-icon>
        {{ wsConnected ? 'En vivo' : 'Desconectado' }}
      </v-chip>
      <v-btn variant="tonal" prepend-icon="mdi-refresh" @click="fetchUsers" :loading="loadingUsers">
        Actualizar
      </v-btn>
    </div>

    <v-row>
      <v-col cols="12" md="4">
        <v-card rounded="lg" elevation="2">
          <v-card-title class="d-flex align-center">
            <v-icon class="mr-2">mdi-account-group</v-icon>
            Estudiantes
            <v-spacer />
            <v-chip size="small" variant="tonal">{{ filteredUsers.length }}</v-chip>
          </v-card-title>

          <div class="px-4 pb-2">
            <v-text-field
              v-model="searchQuery"
              prepend-inner-icon="mdi-magnify"
              label="Buscar estudiante"
              variant="outlined"
              density="compact"
              hide-details
              clearable
            />
          </div>

          <v-progress-linear v-if="loadingUsers" indeterminate color="primary" />

          <v-list v-else-if="filteredUsers.length" class="py-0">
            <v-list-item
              v-for="u in filteredUsers"
              :key="u.userId"
              :active="selectedUser?.userId === u.userId"
              @click="selectUser(u)"
              class="py-3"
            >
              <template #prepend>
                <v-avatar :color="u.percentComplete >= 100 ? 'success' : 'primary'" size="40" class="mr-3">
                  <span class="text-white text-caption">{{ u.displayName?.charAt(0) || '?' }}</span>
                </v-avatar>
              </template>

              <v-list-item-title class="font-weight-medium">
                {{ u.displayName || u.username }}
              </v-list-item-title>
              <v-list-item-subtitle>
                {{ u.username }}
              </v-list-item-subtitle>

              <template #append>
                <div class="text-right">
                  <div class="text-h6 font-weight-bold" :class="u.percentComplete >= 100 ? 'text-success' : 'text-primary'">
                    {{ u.percentComplete }}%
                  </div>
                  <div class="text-caption text-grey">
                    {{ u.completedUnits }}/{{ u.totalUnits }} unidades
                  </div>
                </div>
              </template>
            </v-list-item>
          </v-list>

          <v-card-text v-else class="text-center text-grey pa-8">
            <v-icon size="48" class="mb-2">mdi-account-off</v-icon>
            <p v-if="searchQuery">No se encontraron estudiantes</p>
            <p v-else>No hay usuarios registrados</p>
            <p class="text-caption mt-2">Creá usuarios en la sección "Usuarios"</p>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="8">
        <template v-if="selectedUser">
          <v-card rounded="lg" elevation="2" class="mb-4">
            <v-card-title class="d-flex align-center">
              <v-avatar :color="selectedUser.percentComplete >= 100 ? 'success' : 'primary'" size="40" class="mr-3">
                <span class="text-white">{{ selectedUser.displayName?.charAt(0) || '?' }}</span>
              </v-avatar>
              <div>
                <div class="text-h6">{{ selectedUser.displayName }}</div>
                <div class="text-caption text-grey">{{ selectedUser.username }}</div>
              </div>
              <v-spacer />
              <v-menu>
                <template #activator="{ props: menuProps }">
                  <v-btn icon="mdi-dots-vertical" variant="text" v-bind="menuProps" />
                </template>
                <v-list>
                  <v-list-item prepend-icon="mdi-restart" @click="confirmResetAll">
                    <v-list-item-title>Resetear todo el progreso</v-list-item-title>
                  </v-list-item>
                  <v-list-item prepend-icon="mdi-export" @click="exportProgress">
                    <v-list-item-title>Exportar progreso</v-list-item-title>
                  </v-list-item>
                </v-list>
              </v-menu>
            </v-card-title>

            <v-card-text>
              <v-row>
                <v-col cols="4">
                  <div class="text-center">
                    <div class="text-h4 font-weight-bold text-primary">{{ selectedUser.percentComplete }}%</div>
                    <div class="text-caption text-grey">Progreso total</div>
                    <v-progress-linear
                      :model-value="selectedUser.percentComplete"
                      color="primary"
                      height="8"
                      class="mt-2 rounded"
                    />
                  </div>
                </v-col>
                <v-col cols="4">
                  <div class="text-center">
                    <div class="text-h4 font-weight-bold text-success">{{ selectedUser.completedUnits }}</div>
                    <div class="text-caption text-grey">Unidades completadas</div>
                  </div>
                </v-col>
                <v-col cols="4">
                  <div class="text-center">
                    <div class="text-h4 font-weight-bold">{{ selectedUser.totalUnits }}</div>
                    <div class="text-caption text-grey">Total unidades</div>
                  </div>
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>

          <v-card v-if="loadingDetail" rounded="lg" elevation="2">
            <v-progress-linear indeterminate color="primary" />
            <v-card-text class="text-center py-8">Cargando detalles...</v-card-text>
          </v-card>

          <template v-else>
            <v-card v-if="weaknesses.length" rounded="lg" elevation="2" class="mb-4">
              <v-card-title class="d-flex align-center">
                <v-icon class="mr-2" color="warning">mdi-alert-circle</v-icon>
                Áreas que necesitan atención
                <v-chip size="small" variant="tonal" color="warning" class="ml-2">{{ weaknesses.length }}</v-chip>
              </v-card-title>
              <v-card-text>
                <WeaknessCard
                  v-for="w in weaknesses"
                  :key="w.unitId"
                  :weakness="w"
                  @redo="confirmRedoUnitById(w.unitId, w.topicId)"
                  @reset="confirmResetUnit(w)"
                />
              </v-card-text>
            </v-card>

            <v-card rounded="lg" elevation="2" class="mb-4">
              <v-card-title class="d-flex align-center">
                <v-icon class="mr-2" color="primary">mdi-format-list-bulleted</v-icon>
                Detalle por unidad
              </v-card-title>

              <v-card-text v-if="!progressData.topics?.length" class="text-center text-grey py-6">
                <v-icon size="48" class="mb-2">mdi-clipboard-text-off</v-icon>
                <p>Sin datos de progreso registrados</p>
              </v-card-text>

              <div v-else>
                <div v-for="topic in progressData.topics" :key="topic.topicId" class="pa-4" style="border-bottom: 1px solid #e0e0e0;">
                  <h3 class="text-subtitle-1 font-weight-bold mb-2 text-primary">
                    <v-icon class="mr-1">mdi-book-open-page-variant</v-icon>
                    {{ topic.topicName }}
                  </h3>

                  <div v-for="u in topic.units" :key="u.unitId" class="ml-2 mb-3 pa-3 rounded" :style="{ background: u.status === 'mastered' ? '#F3E5F5' : u.completed ? '#E8F5E9' : '#FAFAFA' }">
                    <div class="d-flex align-center mb-1">
                      <div class="flex-grow-1">
                        <div class="text-body-2 font-weight-medium">{{ u.title || u.unitId }}</div>
                        <div class="text-caption text-grey">
                          {{ u.completedItems }}/{{ u.totalItems }} ejercicios
                          <template v-if="u.status === 'mastered'"> 🏆 Dominado</template>
                          <template v-else-if="u.completed"> ✅ Completado</template>
                          <template v-else-if="u.status === 'in_progress'"> 📖 En progreso</template>
                        </div>
                      </div>
                      <v-chip v-if="u.testScore != null" :color="u.testScore >= 14 ? 'success' : 'warning'" size="x-small" variant="tonal" class="mr-1">
                        Test {{ u.testScore }}/20
                      </v-chip>
                      <v-btn icon="mdi-restart" variant="text" size="x-small" color="orange" @click="confirmRedoUnit(u)" />
                      <v-btn icon="mdi-delete" variant="text" size="x-small" color="error" @click="confirmResetUnitById(u)" />
                    </div>

                    <div v-if="u.itemsAttempted > 0" class="d-flex align-center mt-2 mb-2" style="gap: 12px;">
                      <v-chip size="x-small" :color="u.accuracy >= 70 ? 'success' : u.accuracy >= 50 ? 'warning' : 'error'" variant="tonal">
                        Precisión: {{ Math.round(u.accuracy) }}%
                      </v-chip>
                      <v-chip size="x-small" :color="u.mastery >= 90 ? 'purple' : u.mastery >= 70 ? 'success' : u.mastery >= 50 ? 'warning' : 'error'" variant="tonal">
                        Dominio: {{ Math.round(u.mastery) }}%
                      </v-chip>
                      <v-chip size="x-small" color="info" variant="tonal">
                        Intentados: {{ u.itemsAttempted }}/{{ u.totalItems }}
                      </v-chip>
                      <v-chip v-if="u.timeSpentSeconds > 0" size="x-small" color="grey" variant="tonal">
                        Tiempo: {{ formatDuration(u.timeSpentSeconds) }}
                      </v-chip>
                    </div>

                    <div v-for="b in u.blocks" :key="b.blockIndex" class="d-flex align-center ml-4 py-1">
                      <v-icon size="14" :color="b.completed ? 'success' : 'grey'" class="mr-1">mdi-circle-small</v-icon>
                      <span class="text-caption flex-grow-1">{{ b.title || 'Bloque ' + (b.blockIndex + 1) }}</span>
                      <span v-if="b.completed" class="text-caption">
                        <span class="text-success">✅ {{ b.score }}</span>
                        <span v-if="b.wrongCount > 0" class="text-error ml-1">❌ {{ b.wrongCount }}</span>
                        <span v-else class="text-success ml-1">✨ perfecto</span>
                      </span>
                      <span v-else class="text-caption text-grey">{{ b.score }}/{{ b.totalItems }}</span>
                      <v-btn icon="mdi-restart" variant="text" size="x-small" color="orange" class="ml-1" @click="confirmResetBlock(u, b.blockIndex)" />
                    </div>
                  </div>
                </div>
              </div>
            </v-card>

            <v-card v-if="timelineEvents.length" rounded="lg" elevation="2" class="mb-4">
              <v-card-title class="d-flex align-center">
                <v-icon class="mr-2" color="info">mdi-timeline-clock</v-icon>
                Actividad reciente
                <v-spacer />
                <v-chip-group v-model="timelineFilter" mandatory selected-class="text-primary">
                  <v-chip size="small" value="all">Todo</v-chip>
                  <v-chip size="small" value="reset">Resets</v-chip>
                  <v-chip size="small" value="sync">Syncs</v-chip>
                </v-chip-group>
              </v-card-title>
              <v-card-text>
                <TimelineEvent
                  v-for="event in filteredTimeline"
                  :key="event.id"
                  :event="event"
                />
              </v-card-text>
            </v-card>

            <v-card v-if="progressData.errors?.length" rounded="lg" elevation="2" class="mb-4">
              <v-card-title class="d-flex align-center">
                <v-icon class="mr-2" color="error">mdi-alert-circle</v-icon>
                Últimos errores ({{ progressData.errors.length }})
              </v-card-title>

              <v-table density="compact">
                <thead>
                  <tr>
                    <th>Respuesta dada</th>
                    <th>Respuesta correcta</th>
                    <th>Fecha</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="e in progressData.errors.slice(0, 15)" :key="e.id">
                    <td :class="e.isCorrect ? 'text-success' : 'text-error'">
                      {{ e.givenAnswer }}
                    </td>
                    <td>{{ e.correctAnswer }}</td>
                    <td class="text-caption text-grey">{{ formatDate(e.answeredAt) }}</td>
                  </tr>
                </tbody>
              </v-table>
            </v-card>

            <v-card v-if="analytics" rounded="lg" elevation="2">
              <v-card-title class="d-flex align-center">
                <v-icon class="mr-2" color="primary">mdi-chart-bar</v-icon>
                Análisis de rendimiento
              </v-card-title>

              <v-card-text>
                <v-row>
                  <v-col cols="4">
                    <v-sheet rounded="lg" color="grey-lighten-4" class="pa-3 text-center">
                      <div class="text-h3 font-weight-bold" :class="analytics.overallAccuracy >= 70 ? 'text-success' : 'text-warning'">
                        {{ analytics.overallAccuracy || 0 }}%
                      </div>
                      <div class="text-caption font-weight-medium">Precisión global</div>
                      <div class="text-caption text-grey">{{ analytics.totalCorrect || 0 }} / {{ analytics.totalAnswered || 0 }} respuestas</div>
                    </v-sheet>
                  </v-col>
                  <v-col cols="4">
                    <v-sheet rounded="lg" color="grey-lighten-4" class="pa-3 text-center">
                      <div class="text-h3 font-weight-bold text-warning">{{ analytics.weakUnits?.length || 0 }}</div>
                      <div class="text-caption font-weight-medium">Unidades débiles</div>
                      <div class="text-caption text-grey">Requieren práctica</div>
                    </v-sheet>
                  </v-col>
                  <v-col cols="4">
                    <v-sheet rounded="lg" color="grey-lighten-4" class="pa-3 text-center">
                      <div class="text-h3 font-weight-bold text-success">{{ analytics.strongUnits?.length || 0 }}</div>
                      <div class="text-caption font-weight-medium">Unidades fuertes</div>
                      <div class="text-caption text-grey">Bien dominadas</div>
                    </v-sheet>
                  </v-col>
                </v-row>

                <div v-if="evolutionChart" class="mt-4">
                  <h4 class="text-subtitle-1 font-weight-medium mb-2">
                    <v-icon color="primary" class="mr-1">mdi-chart-line</v-icon>
                    Evolución por unidad
                  </h4>
                  <div style="height: 300px;">
                    <Line :data="evolutionChart" :options="chartOptions" />
                  </div>
                </div>

                <div v-if="analytics.weakUnits?.length" class="mt-4">
                  <h4 class="text-subtitle-1 font-weight-medium mb-2">
                    <v-icon color="warning" class="mr-1">mdi-alert-circle</v-icon>
                    Unidades que necesitan refuerzo
                  </h4>
                  <div v-for="w in analytics.weakUnits" :key="w.unitId" class="d-flex align-center py-2 px-3 mb-2 rounded" style="background:#FFF3E0">
                    <div class="flex-grow-1">
                      <div class="text-body-2 font-weight-medium">{{ w.unitId }}</div>
                      <div class="text-caption text-grey">
                        <span class="text-success">✅ {{ w.correct }}</span>
                        <span class="text-error ml-2">❌ {{ w.wrong }}</span>
                        <span class="ml-2">({{ w.errorRate }}% error)</span>
                      </div>
                    </div>
                    <v-btn size="small" variant="tonal" color="orange" @click="confirmRedoUnitById(w.unitId, w.topicId)">
                      <v-icon start size="16">mdi-restart</v-icon> Rehacer
                    </v-btn>
                  </div>
                </div>

                <div v-if="analytics.commonMistakes?.length" class="mt-4">
                  <h4 class="text-subtitle-1 font-weight-medium mb-2">
                    <v-icon color="error" class="mr-1">mdi-alert-rhombus</v-icon>
                    Errores más repetidos
                  </h4>
                  <div v-for="m in analytics.commonMistakes" :key="m.givenAnswer" class="d-flex align-center py-2 px-3 mb-1 rounded" style="background:#FFEBEE">
                    <div class="flex-grow-1 d-flex align-center">
                      <v-chip size="small" color="error" variant="tonal" class="mr-2">
                        "{{ m.givenAnswer }}"
                      </v-chip>
                      <span class="text-body-2 mx-1">→</span>
                      <v-chip size="small" color="success" variant="tonal" class="mr-2">
                        "{{ m.correctAnswer }}"
                      </v-chip>
                      <span class="text-caption text-grey">{{ m.count }} vez{{ m.count !== 1 ? 'es' : '' }}</span>
                    </div>
                    <v-progress-linear
                      :model-value="(m.count / analytics.commonMistakes[0].count) * 100"
                      color="error"
                      height="4"
                      rounded
                      class="ml-2"
                      style="max-width: 80px"
                    />
                  </div>
                </div>

                <div v-if="analytics.hardestExercises?.length" class="mt-4">
                  <h4 class="text-subtitle-1 font-weight-medium mb-2">
                    <v-icon color="deep-orange" class="mr-1">mdi-fire</v-icon>
                    Ejercicios más difíciles
                  </h4>
                  <div v-for="(ex, i) in analytics.hardestExercises" :key="i" class="d-flex align-center py-2 px-3 mb-2 rounded" style="background:#FFF3E0">
                    <div class="flex-grow-1">
                      <div class="text-body-2 font-weight-medium">{{ ex.correctAnswer }}</div>
                      <div class="text-caption">
                        <span class="text-error">❌ {{ ex.failedAttempts }}/{{ ex.totalAttempts }} incorrectos</span>
                        <span v-if="ex.commonWrongAnswers?.length" class="ml-2 text-grey">
                          — Respuestas comunes:
                          <v-chip v-for="(wa, wi) in ex.commonWrongAnswers" :key="wi" size="x-small" color="error" variant="tonal" class="mr-1">
                            "{{ wa }}" ({{ ex.commonWrongCounts?.[wi] || '?' }})
                          </v-chip>
                        </span>
                      </div>
                    </div>
                    <v-chip :color="ex.failedAttempts > ex.totalAttempts / 2 ? 'error' : 'warning'" size="x-small" variant="tonal">
                      {{ ex.failedAttempts > ex.totalAttempts / 2 ? 'Crítico' : 'Difícil' }}
                    </v-chip>
                  </div>
                </div>

                <div v-if="analytics.retryImprovement?.length" class="mt-4">
                  <h4 class="text-subtitle-1 font-weight-medium mb-2">
                    <v-icon color="info" class="mr-1">mdi-trending-up</v-icon>
                    Evolución por ejercicio
                  </h4>
                  <div v-for="(r, i) in analytics.retryImprovement" :key="i" class="d-flex align-center py-2 px-3 mb-1 rounded" :style="{ background: r.eventuallyCorrect ? '#E8F5E9' : '#FFEBEE' }">
                    <div class="flex-grow-1">
                      <div class="text-body-2 font-weight-medium">{{ r.correctAnswer }}</div>
                      <div class="text-caption">
                        {{ r.totalAttempts }} intento{{ r.totalAttempts !== 1 ? 's' : '' }}
                        <span class="ml-2 text-error">❌ {{ r.consecutiveWrongAtStart }} fallo{{ r.consecutiveWrongAtStart !== 1 ? 's' : '' }} al inicio</span>
                        <span v-if="r.eventuallyCorrect" class="ml-2 text-success">✅ Lo logró después</span>
                        <span v-else class="ml-2 text-error">❌ Sigue fallando</span>
                      </div>
                    </div>
                    <v-icon :color="r.eventuallyCorrect ? 'success' : 'error'">
                      {{ r.eventuallyCorrect ? 'mdi-check-circle' : 'mdi-close-circle' }}
                    </v-icon>
                  </div>
                </div>
              </v-card-text>
            </v-card>
          </template>
        </template>

        <v-card v-else rounded="lg" elevation="2">
          <v-card-text class="text-center text-grey py-12">
            <v-icon size="64" class="mb-4">mdi-account-search</v-icon>
            <p class="text-h6">Seleccioná un estudiante</p>
            <p class="text-body-2">Hacé click en un estudiante de la lista para ver su progreso</p>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-dialog v-model="resetDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title class="text-error">
          <v-icon class="mr-2">mdi-alert</v-icon>
          Resetear progreso
        </v-card-title>
        <v-card-text>
          ¿Resetear TODO el progreso de <b>{{ selectedUser?.displayName }}</b>?<br />
          Esta acción no se puede deshacer.
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="resetDialog = false">Cancelar</v-btn>
          <v-btn color="error" :loading="resetting" @click="doResetAll">Resetear</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="redoDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title class="text-warning">
          <v-icon class="mr-2">mdi-restart</v-icon>
          Marcar para rehacer
        </v-card-title>
        <v-card-text>
          ¿Marcar "<b>{{ unitToRedo }}</b>" para que el estudiante la rehaga?<br />
          La unidad aparecerá como no completada.
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="redoDialog = false">Cancelar</v-btn>
          <v-btn color="warning" :loading="redoing" @click="doRedoUnit">Marcar</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="resetUnitDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title class="text-error">
          <v-icon class="mr-2">mdi-delete</v-icon>
          Resetear unidad
        </v-card-title>
        <v-card-text>
          ¿Resetear la unidad "<b>{{ unitToReset }}</b>" de <b>{{ selectedUser?.displayName }}</b>?<br />
          Se borrará todo el progreso de esta unidad.
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="resetUnitDialog = false">Cancelar</v-btn>
          <v-btn color="error" :loading="resettingUnit" @click="doResetUnit">Resetear</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="resetBlockDialog" max-width="400">
      <v-card rounded="lg">
        <v-card-title class="text-error">
          <v-icon class="mr-2">mdi-delete</v-icon>
          Resetear bloque
        </v-card-title>
        <v-card-text>
          ¿Resetear el bloque "<b>{{ blockToResetTitle }}</b>" de "<b>{{ unitToReset }}</b>"?<br />
          El estudiante deberá rehacer este bloque.
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="resetBlockDialog = false">Cancelar</v-btn>
          <v-btn color="error" :loading="resettingBlock" @click="doResetBlock">Resetear</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, inject, watch } from 'vue'
import api from '../api/client'
import { useWebSocket } from '../composables/useWebSocket'
import { Line } from 'vue-chartjs'
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, Filler } from 'chart.js'
import WeaknessCard from '../components/WeaknessCard.vue'
import TimelineEvent from '../components/TimelineEvent.vue'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, Filler)

const snackbar = inject('snackbar')

const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
const wsPort = window.location.port || (window.location.protocol === 'https:' ? '443' : '80')
const wsUrl = `${wsProtocol}//${window.location.hostname}:${wsPort}/ws/progress`
const { connected: wsConnected, lastMessage: wsMessage } = useWebSocket(wsUrl)

watch(wsMessage, (msg) => {
  if (!msg) return
  if (msg.type === 'progress_synced' || msg.type === 'answers_recorded' || msg.type === 'progress_reset') {
    fetchUsers()
    if (selectedUser.value) selectUser(selectedUser.value)
  }
})

const users = ref([])
const selectedUser = ref(null)
const loadingUsers = ref(true)
const loadingDetail = ref(false)
const searchQuery = ref('')
const progressData = ref({ progress: [], errors: [], sessions: [] })
const analytics = ref(null)
const weaknesses = ref([])
const timelineEvents = ref([])
const evolutionData = ref({})
const timelineFilter = ref('all')

const resetDialog = ref(false)
const resetting = ref(false)

const redoDialog = ref(false)
const unitToRedo = ref('')
const unitToRedoTopic = ref('')
const redoing = ref(false)

const resetUnitDialog = ref(false)
const unitToReset = ref('')
const unitToResetTopic = ref('')
const resettingUnit = ref(false)

const resetBlockDialog = ref(false)
const blockToResetIndex = ref(null)
const blockToResetTitle = ref('')
const resettingBlock = ref(false)

const filteredUsers = computed(() => {
  if (!searchQuery.value) return users.value
  const q = searchQuery.value.toLowerCase()
  return users.value.filter(u =>
    (u.displayName || '').toLowerCase().includes(q) ||
    (u.username || '').toLowerCase().includes(q)
  )
})

const filteredTimeline = computed(() => {
  if (timelineFilter.value === 'all') return timelineEvents.value
  return timelineEvents.value.filter(e => e.eventType === timelineFilter.value)
})

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { position: 'top' },
    tooltip: { mode: 'index', intersect: false }
  },
  scales: {
    y: { beginAtZero: true, max: 100, title: { display: true, text: 'Precisión (%)' } }
  }
}

const evolutionChart = computed(() => {
  const data = evolutionData.value
  if (!data || Object.keys(data).length === 0) return null

  const entries = Object.values(data).filter(d => d.attempts > 1)
  if (entries.length === 0) return null

  const labels = entries.map(d => d.unitId)
  const firstAccuracies = entries.map(d => d.firstAccuracy)
  const currentAccuracies = entries.map(d => d.currentAccuracy)

  return {
    labels,
    datasets: [
      {
        label: 'Primera vez',
        data: firstAccuracies,
        borderColor: '#FF9800',
        backgroundColor: 'rgba(255, 152, 0, 0.1)',
        fill: true,
        tension: 0.3
      },
      {
        label: 'Actual',
        data: currentAccuracies,
        borderColor: '#4CAF50',
        backgroundColor: 'rgba(76, 175, 80, 0.1)',
        fill: true,
        tension: 0.3
      }
    ]
  }
})

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('es-AR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function formatDuration(seconds) {
  if (!seconds || seconds <= 0) return '-'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) return `${hours}h ${minutes}min`
  if (minutes > 0) return `${minutes}min`
  return `${seconds}s`
}

async function fetchUsers() {
  loadingUsers.value = true
  try {
    const { data } = await api.get('/admin/users/progress-summary')
    users.value = data || []
    if (data?.length && !selectedUser.value) {
      selectUser(data[0])
    }
  } catch (e) {
    snackbar.error('Error al cargar usuarios')
  } finally {
    loadingUsers.value = false
  }
}

async function selectUser(user) {
  selectedUser.value = user
  loadingDetail.value = true
  analytics.value = null
  weaknesses.value = []
  timelineEvents.value = []
  evolutionData.value = {}

  try {
    const { data } = await api.get(`/admin/progress/${user.userId}/detail`)
    progressData.value = data
  } catch {
    progressData.value = { topics: [], errors: [], sessions: [] }
  } finally {
    loadingDetail.value = false
  }

  try {
    const { data } = await api.get(`/admin/progress/${user.userId}/analytics`)
    analytics.value = data
    weaknesses.value = data.weaknesses || []
  } catch {
    analytics.value = null
  }

  try {
    const { data } = await api.get(`/admin/progress/${user.userId}/timeline`)
    timelineEvents.value = data.events || []
    evolutionData.value = data.evolution || {}
  } catch {
    timelineEvents.value = []
  }
}

function confirmRedoUnit(p) {
  unitToRedo.value = p.unitId
  unitToRedoTopic.value = p.topicId
  redoDialog.value = true
}

function confirmRedoUnitById(unitId, topicId) {
  unitToRedo.value = unitId
  unitToRedoTopic.value = topicId
  redoDialog.value = true
}

async function doRedoUnit() {
  redoing.value = true
  try {
    await api.post(`/admin/progress/${selectedUser.value.userId}/${unitToRedoTopic.value}/${unitToRedo.value}/redo`)
    snackbar.success('Unidad marcada para rehacer')
    redoDialog.value = false
    await selectUser(selectedUser.value)
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al marcar unidad')
  } finally {
    redoing.value = false
  }
}

function confirmResetAll() {
  resetDialog.value = true
}

async function doResetAll() {
  resetting.value = true
  try {
    await api.delete(`/admin/progress/${selectedUser.value.userId}/reset-all`)
    snackbar.success('Progreso reseteado')
    resetDialog.value = false
    await selectUser(selectedUser.value)
  } catch (e) {
    snackbar.error('Error al resetear')
  } finally {
    resetting.value = false
  }
}

function confirmResetUnitById(u) {
  unitToReset.value = u.unitId || u.title
  unitToResetTopic.value = u.topicId
  resetUnitDialog.value = true
}

function confirmResetUnit(w) {
  unitToReset.value = w.unitId
  unitToResetTopic.value = w.topicId
  resetUnitDialog.value = true
}

async function doResetUnit() {
  resettingUnit.value = true
  try {
    await api.delete(`/admin/progress/${selectedUser.value.userId}/${unitToResetTopic.value}/${unitToReset.value}`)
    snackbar.success('Unidad reseteada')
    resetUnitDialog.value = false
    await selectUser(selectedUser.value)
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al resetear unidad')
  } finally {
    resettingUnit.value = false
  }
}

function confirmResetBlock(u, blockIndex) {
  unitToReset.value = u.unitId || u.title
  unitToResetTopic.value = u.topicId
  blockToResetIndex.value = blockIndex
  blockToResetTitle.value = u.blocks?.find(b => b.blockIndex === blockIndex)?.title || `Bloque ${blockIndex + 1}`
  resetBlockDialog.value = true
}

async function doResetBlock() {
  resettingBlock.value = true
  try {
    await api.delete(`/admin/progress/${selectedUser.value.userId}/${unitToResetTopic.value}/${unitToReset.value}/block/${blockToResetIndex.value}`)
    snackbar.success('Bloque reseteado')
    resetBlockDialog.value = false
    await selectUser(selectedUser.value)
  } catch (e) {
    snackbar.error(e.response?.data?.detail || 'Error al resetear bloque')
  } finally {
    resettingBlock.value = false
  }
}

function exportProgress() {
  if (!selectedUser.value) return
  const data = {
    user: selectedUser.value,
    progress: progressData.value.progress,
    errors: progressData.value.errors,
    analytics: analytics.value,
    weaknesses: weaknesses.value,
    timeline: timelineEvents.value,
    evolution: evolutionData.value
  }
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `progress_${selectedUser.value.username}_${Date.now()}.json`
  a.click()
  URL.revokeObjectURL(url)
  snackbar.success('Progreso exportado')
}

onMounted(fetchUsers)
</script>
