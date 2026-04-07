package com.aptox.app.backup

/**
 * [LocalBackupImporter] 결과.
 * @param failedSectionLabels 사용자에게 노출할 한글 라벨 ([LocalBackupFormat.SectionLabels])
 */
data class LocalBackupRestoreResult(
    val success: Boolean,
    val failedSectionLabels: List<String>,
    val fatalMessage: String? = null,
)
