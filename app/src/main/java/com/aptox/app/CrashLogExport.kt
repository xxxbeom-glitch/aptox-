package com.aptox.app

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 크래시 로그를 다른 앱(파일, 메일, 드라이브 등)으로보내기.
 * [FileProvider] + ACTION_SEND.
 */
object CrashLogExport {

    private const val EXPORT_CACHE_SUBDIR = "crash_exports"

    fun fileProviderAuthority(context: Context): String =
        "${context.packageName}.crashlogfileprovider"

    /** 내부 crash_logs 아래의 로그 파일만 허용 */
    fun isCrashLogFile(context: Context, file: File): Boolean {
        if (!file.isFile || !file.exists()) return false
        return try {
            val base = File(context.filesDir, CrashLogRepository.CRASH_LOG_DIR_NAME).canonicalFile
            val parent = file.canonicalFile.parentFile ?: return false
            parent == base &&
                file.name.startsWith("crash_") &&
                file.name.endsWith(".txt")
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 단일 로그 텍스트 파일 공유.
     * @return 성공 시 true (Chooser 표시)
     */
    fun shareSingleLog(context: Context, file: File): Boolean {
        if (!isCrashLogFile(context, file)) return false
        return try {
            val uri = FileProvider.getUriForFile(context, fileProviderAuthority(context), file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Aptox 크래시 로그: ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "크래시 로그보내기").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 모든 로그를 ZIP으로 묶어 캐시에 만든 뒤 공유.
     * @return ZIP 생성 및 Chooser 표시 성공 시 true
     */
    suspend fun shareAllLogsAsZip(context: Context): Boolean {
        val files = withContext(Dispatchers.IO) { CrashLogRepository.listLogFiles(context) }
        if (files.isEmpty()) return false
        val zipFile = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, EXPORT_CACHE_SUBDIR).apply { mkdirs() }
            // 이전 zip 정리 (용량)
            dir.listFiles()?.filter { it.extension.equals("zip", true) }?.forEach { it.delete() }
            val out = File(dir, "aptox_crash_logs_${System.currentTimeMillis()}.zip")
            ZipOutputStream(FileOutputStream(out)).use { zos ->
                for (f in files) {
                    if (!isCrashLogFile(context, f)) continue
                    zos.putNextEntry(ZipEntry(f.name))
                    FileInputStream(f).use { input -> input.copyTo(zos) }
                    zos.closeEntry()
                }
            }
            out
        }
        if (!zipFile.exists() || zipFile.length() == 0L) return false
        return try {
            val uri = FileProvider.getUriForFile(context, fileProviderAuthority(context), zipFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, zipFile.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "크래시 로그 ZIP보내기").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            false
        }
    }
}
