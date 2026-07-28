// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.firebase.crashlytics") version "3.0.2" apply false
}

/** Gradle 기본 산출물은 app/build/outputs/ 고정. 찾기 쉽게 루트 dist/ 로만 복사한다. */
fun copyArtifactToDist(sourceDir: File, extension: String, destFileName: String) {
    val src = sourceDir.listFiles()?.find { it.isFile && it.name.endsWith(".$extension") }
        ?: error("산출물 없음: ${sourceDir.absolutePath} (*.$extension)")
    val distDir = file("dist").apply { mkdirs() }
    val dest = File(distDir, destFileName)
    src.copyTo(dest, overwrite = true)
    println("dist → ${dest.absolutePath}")
}

// Play Console AAB (devRelease) — 기존 단축 유지
tasks.register("aptox") {
    dependsOn(":app:bundleDevRelease")
    group = "aptox"
    description = "AAB (devRelease) → dist/aptox-dev-release.aab"
    doLast {
        copyArtifactToDist(
            file("app/build/outputs/bundle/devRelease"),
            "aab",
            "aptox-dev-release.aab",
        )
    }
}

// Play Console AAB (externalTestRelease, 디버그메뉴 숨김 — 스토어/내부테스트 권장)
tasks.register("aptoxPlay") {
    dependsOn(":app:bundleExternalTestRelease")
    group = "aptox"
    description = "AAB (externalTestRelease) → dist/aptox-play-release.aab"
    doLast {
        copyArtifactToDist(
            file("app/build/outputs/bundle/externalTestRelease"),
            "aab",
            "aptox-play-release.aab",
        )
    }
}

// 내부 설치용 디버그 APK
tasks.register("aptoxDebug") {
    dependsOn(":app:assembleDevDebug")
    group = "aptox"
    description = "디버그 APK → dist/aptox-dev-debug.apk"
    doLast {
        copyArtifactToDist(
            file("app/build/outputs/apk/dev/debug"),
            "apk",
            "aptox-dev-debug.apk",
        )
    }
}

// 외부/내부 테스트 설치용 APK (디버그메뉴 숨김)
tasks.register("aptoxTest") {
    dependsOn(":app:assembleExternalTestDebug")
    group = "aptox"
    description = "테스트 APK → dist/aptox-test-1.0.apk"
    doLast {
        copyArtifactToDist(
            file("app/build/outputs/apk/externalTest/debug"),
            "apk",
            "aptox-test-1.0.apk",
        )
    }
}
