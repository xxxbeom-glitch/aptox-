package com.aptox.app

import android.app.Application
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 크래시/미처리 예외 로그를 앱 내부 저장소(filesDir)에 파일로 저장·조회.
 */
object CrashLogRepository {

    private const val TAG = "CrashLogRepository"
    internal const val CRASH_LOG_DIR_NAME = "crash_logs"
    private const val FILE_PREFIX = "crash_"
    private const val FILE_SUFFIX = ".txt"

    private fun logDir(context: android.content.Context): File =
        File(context.filesDir, CRASH_LOG_DIR_NAME).apply { mkdirs() }

    /**
     * [Thread.UncaughtExceptionHandler]에서 호출. 기본 핸들러 호출 전에 동기 저장.
     */
    fun saveUncaughtException(application: Application, thread: Thread, throwable: Throwable) {
        try {
            val dir = logDir(application)
            val ts = System.currentTimeMillis()
            val file = File(dir, "$FILE_PREFIX$ts$FILE_SUFFIX")
            val tz = TimeZone.getDefault()
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS XXX", Locale.US).apply {
                timeZone = tz
            }
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            file.bufferedWriter(Charsets.UTF_8).use { w ->
                w.appendLine("===== Aptox Crash =====")
                w.appendLine("Time: ${fmt.format(Date(ts))}")
                w.appendLine("Thread: ${thread.name}")
                w.appendLine("Message: ${throwable.javaClass.name}: ${throwable.message}")
                w.appendLine()
                w.append(sw.toString())
                w.appendLine("===== End =====")
            }
            Log.e(TAG, "크래시 로그 저장: ${file.name}", throwable)
        } catch (e: Exception) {
            Log.e(TAG, "크래시 로그 저장 실패", e)
        }
    }

    fun listLogFiles(context: android.content.Context): List<File> {
        val dir = logDir(context)
        return dir.listFiles { f ->
            f.isFile && f.name.startsWith(FILE_PREFIX) && f.name.endsWith(FILE_SUFFIX)
        }?.sortedByDescending { it.name } ?: emptyList()
    }

    fun readLogText(file: File): String =
        runCatching { file.readText(Charsets.UTF_8) }.getOrElse { it.message ?: "(읽기 실패)" }

    fun clearAll(context: android.content.Context) {
        listLogFiles(context).forEach { runCatching { it.delete() } }
    }

    /** 목록용: 파일에서 Time / Message 한 줄씩 파싱 (실패 시 파일명) */
    fun summarizeForList(file: File): Pair<String, String> {
        return runCatching {
            file.bufferedReader(Charsets.UTF_8).use { reader ->
                var time = ""
                var message = ""
                repeat(20) {
                    val line = reader.readLine() ?: return@repeat
                    when {
                        line.startsWith("Time:") -> time = line.removePrefix("Time:").trim()
                        line.startsWith("Message:") -> message = line.removePrefix("Message:").trim()
                    }
                }
                val title = time.ifBlank { file.nameWithoutExtension }
                val sub = message.ifBlank { "스택트레이스 확인" }
                title to sub
            }
        }.getOrElse { file.nameWithoutExtension to "(파싱 실패)" }
    }
}

/**
 * 기존 [Thread.UncaughtExceptionHandler]를 체인하여 로그 저장 후 기본 동작 유지.
 */
class AptoxUncaughtExceptionHandler(
    private val application: Application,
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            CrashLogRepository.saveUncaughtException(application, t, e)
        } catch (_: Throwable) {
            // 저장 실패해도 프로세스 종료 흐름은 유지
        }
        defaultHandler?.uncaughtException(t, e)
    }
}
