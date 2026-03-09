// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

// cole: APK 빌드만 수행 (기기 미연결 시 installDebug 실패 방지)
tasks.register("cole") {
    dependsOn(":app:assembleDebug")
    group = "cole"
    description = "APK 빌드. 기기 연결 후 설치하려면 installDebug 실행"
}