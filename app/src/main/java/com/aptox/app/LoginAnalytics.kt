package com.aptox.app

import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics

/** Google Credential Manager 기반 로그인 취소/실패 Firebase Analytics 이벤트 */
object LoginAnalytics {
    fun isGoogleLoginCancelled(e: Throwable): Boolean {
        // AuthRepository가 사용자 취소일 때만 쓰는 고정 문구 (설정 오류성 Cancellation은 제외)
        val msg = e.message.orEmpty()
        return msg == "구글 로그인이 취소되었습니다." || msg == "구글 재인증이 취소되었습니다."
    }

    fun logLoginCancelled(analytics: FirebaseAnalytics, method: String, screen: String) {
        analytics.logEvent(
            "login_cancelled",
            bundleOf(
                "method" to method,
                "screen" to screen,
            ),
        )
    }

    fun logLoginFailed(analytics: FirebaseAnalytics, method: String, error: String) {
        val safe = error.take(100)
        analytics.logEvent(
            "login_failed",
            bundleOf(
                "method" to method,
                "error" to safe,
            ),
        )
    }
}
