package com.aptox.app.usage

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * UsageStats → 로컬 DB → Firestore 백업.
 * 실행 주기: 최초 앱 실행 시 즉시 1회, 로그인 직후 즉시 1회, 이후 6시간마다.
 * 중복 방지: SQLite CONFLICT_REPLACE / REPLACE, Firestore set() 덮어쓰기.
 */
class UsageStatsSyncWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        UsageStatsFirestoreSync.sync(
            context.applicationContext,
            inputData.getBoolean(KEY_INITIAL_SYNC, false),
        ).fold(
            onSuccess = { Result.success() },
            onFailure = { Result.failure() },
        )
    }

    companion object {
        const val KEY_INITIAL_SYNC = "initial_sync"
    }
}
