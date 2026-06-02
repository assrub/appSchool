package com.appenglish

import android.app.Application
import com.appenglish.data.repository.ProgressRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AppEnglishApp : Application() {

    @Inject lateinit var progressRepository: ProgressRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            try { progressRepository.loadRemoteProgress() } catch (_: Exception) {}
        }
    }
}
