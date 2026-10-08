package com.styletrack.customer.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.styletrack.customer.StyleTrackApp
import com.styletrack.customer.data.ApiException
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/**
 * Checks for new booking updates in the background and shows them as phone notifications.
 * Android allows background checks at most every 15 minutes, so alerts can arrive a little late; instant
 * delivery needs Firebase Cloud Messaging (see the README).
 */
class NotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as StyleTrackApp
        if (app.tokens.token == null) return Result.success()
        return try {
            val items = app.repo.notifications() // newest first
            val newest = items.maxOfOrNull { it.id } ?: 0L
            val last = app.tokens.lastNotifiedId
            if (last < 0) {
                // first run after signing in: remember where we are, don't announce old items
                app.tokens.lastNotifiedId = newest
                return Result.success()
            }
            items.filter { !it.seen && it.id > last }
                .sortedBy { it.id }
                .forEach { Notifier.show(applicationContext, it) }
            if (newest > last) app.tokens.lastNotifiedId = newest
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            if (e.code == 401 || e.code == 403) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC = "styletrack-notification-check"
        private const val NOW = "styletrack-notification-check-now"

        fun start(context: Context) {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val manager = WorkManager.getInstance(context)
            manager.enqueueUniqueWork(
                NOW, ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<NotificationWorker>().setConstraints(constraints).build(),
            )
            manager.enqueueUniquePeriodicWork(
                PERIODIC, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES).setConstraints(constraints).build(),
            )
        }

        fun stop(context: Context) {
            val manager = WorkManager.getInstance(context)
            manager.cancelUniqueWork(PERIODIC)
            manager.cancelUniqueWork(NOW)
        }
    }
}
