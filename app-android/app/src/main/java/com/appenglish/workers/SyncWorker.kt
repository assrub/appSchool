package com.appenglish.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.appenglish.data.local.dao.PendingSyncDao
import com.appenglish.data.remote.api.ProgressApi
import com.appenglish.data.remote.dto.AnswerBatchRequest
import com.appenglish.data.remote.dto.AnswerEntryDto
import com.appenglish.data.remote.dto.BlockProgressEntryDto
import com.appenglish.data.remote.dto.ProgressEntryDto
import com.appenglish.data.remote.dto.ProgressSyncRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val pendingSyncDao: PendingSyncDao,
    private val progressApi: ProgressApi,
    private val gson: Gson
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Clean up failed entries that exceeded max retries
        pendingSyncDao.deleteFailed()

        val pendingSyncs = pendingSyncDao.getAllPending()
        if (pendingSyncs.isEmpty()) return Result.success()

        var allSuccess = true

        for (sync in pendingSyncs) {
            try {
                when (sync.syncType) {
                    "progress" -> {
                        val type = object : TypeToken<ProgressSyncRequest>() {}.type
                        val request: ProgressSyncRequest = gson.fromJson(sync.payload, type)
                        progressApi.syncProgress(request)
                    }
                    "answers" -> {
                        val type = object : TypeToken<AnswerBatchRequest>() {}.type
                        val request: AnswerBatchRequest = gson.fromJson(sync.payload, type)
                        progressApi.recordAnswers(request)
                    }
                }
                // Success - delete from pending
                pendingSyncDao.delete(sync.id)
            } catch (e: Exception) {
                // Failed - increment retry count
                pendingSyncDao.incrementRetry(sync.id)
                allSuccess = false
            }
        }

        return if (allSuccess) Result.success() else Result.retry()
    }

    companion object {
        private const val WORK_NAME = "sync_pending_progress"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, androidx.work.ExistingWorkPolicy.KEEP, workRequest)
        }
    }
}
