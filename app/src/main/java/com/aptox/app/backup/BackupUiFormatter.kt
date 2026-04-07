package com.aptox.app.backup

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupUiFormatter {

    /** 예: `26.3.3  19:00` (연월일 한 자리 허용, 시간 앞 공백 2칸) */
    fun formatLastBackupInstant(epochMs: Long): String {
        val fmt = SimpleDateFormat("yy.M.d  HH:mm", Locale.KOREA)
        return fmt.format(Date(epochMs))
    }
}
