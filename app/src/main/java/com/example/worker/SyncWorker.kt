package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.network.TravelRepository
import android.util.Log

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = TravelRepository(applicationContext)
        return try {
            repository.syncData()
            Log.d("SyncWorker", "Data synced successfully in background")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Background sync failed", e)
            Result.retry()
        }
    }
}
