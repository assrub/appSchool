package com.appenglish

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.appenglish.data.remote.api.AuthInterceptor
import com.appenglish.data.repository.ProgressRepository
import com.appenglish.util.ApiConfig
import com.appenglish.websocket.WebSocketManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AppEnglishApp : Application(), Configuration.Provider {

    @Inject lateinit var progressRepository: ProgressRepository
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var webSocketManager: WebSocketManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            try { progressRepository.loadRemoteProgress() } catch (_: Exception) {}
        }
        setupWebSocket()
    }

    private fun setupWebSocket() {
        val userId = AuthInterceptor.userId ?: return
        val wsProtocol = if (ApiConfig.BASE_URL.startsWith("https")) "wss" else "ws"
        val baseUrl = ApiConfig.BASE_URL.removeSuffix("/api/v1/").removeSuffix("/api/v1").removePrefix("http://").removePrefix("https://")
        val wsUrl = "$wsProtocol://$baseUrl/ws/progress"

        webSocketManager.connect(wsUrl, userId)

        appScope.launch {
            webSocketManager.events.collect { event ->
                when (event.type) {
                    "progress_reset" -> {
                        when (event.scope) {
                            "all" -> {
                                progressRepository.resetAllProgress()
                                progressRepository.loadRemoteProgress()
                            }
                            "unit" -> {
                                if (event.topicId != null && event.unitId != null) {
                                    progressRepository.resetUnitProgress(event.topicId, event.unitId)
                                    progressRepository.loadRemoteProgress()
                                }
                            }
                            "block" -> {
                                if (event.topicId != null && event.unitId != null && event.blockIndex != null) {
                                    progressRepository.loadRemoteProgress()
                                }
                            }
                        }
                    }
                    "redo_marked" -> {
                        if (event.unitId != null) {
                            progressRepository.loadRemoteProgress()
                        }
                    }
                }
            }
        }
    }
}
