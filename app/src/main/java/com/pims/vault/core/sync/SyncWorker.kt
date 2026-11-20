package com.pims.vault.core.sync

import android.content.Context
import androidx.room.InvalidationTracker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import com.pims.vault.core.logging.VaultLogger
import com.pims.vault.data.local.database.PimsDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncWorkerEntryPoint {
    fun syncQueueManager(): SyncQueueManager
    fun cloudSyncAccess(): CloudSyncAccess
    fun syncWorkScheduler(): SyncWorkScheduler
}

class SyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val services = EntryPointAccessors.fromApplication(applicationContext, SyncWorkerEntryPoint::class.java)
        val uid = services.cloudSyncAccess().currentAccount() ?: return Result.success()
        val outcome = services.syncQueueManager().processPendingQueue(uid)
        return if (outcome.shouldRetry && services.cloudSyncAccess().currentAccount() == uid) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}

@Singleton
class SyncWorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val access: CloudSyncAccess,
    private val database: Provider<PimsDatabase>
) {
    private val networkRequired = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val work get() = WorkManager.getInstance(context)

    val pendingSweep: Flow<Boolean> get() = work.getWorkInfosForUniqueWorkFlow(NOW).map { entries ->
        entries.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED }
    }.distinctUntilChanged()

    fun start() {
        scope.launch {
            val mutations = object : InvalidationTracker.Observer(SWEEP_TABLES) {
                override fun onInvalidated(tables: Set<String>) = requestSync()
            }
            var registered = false
            access.authorizedAccount.collect { uid ->
                try {
                    if (uid == null) {
                        if (registered) {
                            database.get().invalidationTracker.removeObserver(mutations)
                            registered = false
                        }
                        work.cancelUniqueWork(NOW)
                        work.cancelUniqueWork(PERIODIC)
                    } else {
                        if (!registered) {
                            database.get().invalidationTracker.addObserver(mutations)
                            registered = true
                        }
                        work.enqueueUniquePeriodicWork(
                            PERIODIC,
                            ExistingPeriodicWorkPolicy.KEEP,
                            PeriodicWorkRequestBuilder<SyncWorker>(60, TimeUnit.MINUTES)
                                .setInitialDelay(60, TimeUnit.MINUTES)
                                .setConstraints(networkRequired)
                                .build()
                        )
                        requestSync()
                    }
                } catch (_: IllegalStateException) {
                    VaultLogger.w("CloudSync", "Background scheduling is unavailable")
                }
            }
        }
    }

    fun requestSync() {
        if (access.currentAccount() == null) return
        try {
            work.enqueueUniqueWork(
                NOW,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(networkRequired)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build()
            )
        } catch (_: IllegalStateException) {
            VaultLogger.w("CloudSync", "Background scheduling is unavailable")
        }
    }

    private companion object {
        const val NOW = "account-cloud-sync"
        const val PERIODIC = "account-cloud-sync-periodic"

        val SWEEP_TABLES = arrayOf(
            "persons", "contact_methods", "addresses", "relationships", "social_accounts",
            "education_records", "employment_records", "medical_profiles", "medical_records",
            "relationship_notes", "relationship_note_attachments", "documents", "document_versions",
            "document_deletions", "vault_items", "vault_proofs", "vault_deletions"
        )
    }
}
