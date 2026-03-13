package com.aptox.app

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val TopTextsGap = 8.dp
private val TextToAnimationGap = 10.dp
private val AnimationToProgressGap = 10.dp  // Figma: 애니메이션 ~ 진행 단계
private val ProgressStepGap = 4.dp

/** 진행 단계 opacity: 대기/완료=20%, 진행중=100% */
private fun stepOpacity(state: Int, isFirstStep: Boolean = false) = when {
    isFirstStep && state == 0 -> 1f   // step1 "확인 중" = 진행중
    isFirstStep && state == 1 -> 0.2f // step1 "확인 완료" = 완료
    state == 1 -> 1f                  // step2,3 진행중
    else -> 0.2f                       // 대기 또는 완료
}

/**
 * ST-09: 자가테스트 결과 로딩 화면
 * - 잠시만 기다려주세요 / 평소 사용하는~/ 애니메이션 / 3줄 진행 단계 UI
 * - 각 단계 1.4초, 3번째 완료 시 체크 애니메이션 → onFinish
 */
@Composable
fun SelfTestLoadingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var step1 by remember { mutableIntStateOf(0) } // 0=중, 1=완료
    var step2 by remember { mutableIntStateOf(0) } // 0=대기, 1=진행중, 2=완료
    var step3 by remember { mutableIntStateOf(0) } // 0=대기, 1=진행중, 2=완료
    var triggerCheck by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        step1 = 0
        step2 = 0
        step3 = 0
        val preload = AppDataPreloadRepository(context)

        // Step 1: 설치된 앱 확인
        val installedApps = withContext(Dispatchers.IO) { preload.loadInstalledApps() }
        step1 = 1
        step2 = 1

        // Step 2: 스크린 타임 분석 (UsageStats → DB 동기화)
        withContext(Dispatchers.IO) { preload.syncUsageStatsToDb() }
        step2 = 2
        step3 = 1

        // Step 3: AI 카테고리 분류 및 캐시 저장
        withContext(Dispatchers.IO) { preload.classifyAndCacheApps(installedApps) }
        step3 = 2
        triggerCheck = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 328.dp)
                .padding(horizontal = 24.dp)
                .offset(y = (-32).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "잠시만 기다려주세요",
                style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(TopTextsGap))
            Text(
                text = "평소 사용하는 앱들의\n사용시간을 분석 중이에요",
                style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(TextToAnimationGap))
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                LoadingToCheckAnimation(
                    triggerCheck = triggerCheck,
                    onComplete = onFinish,
                )
            }
            Spacer(modifier = Modifier.height(AnimationToProgressGap))
            Column(
                modifier = Modifier.widthIn(max = 270.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ProgressStepGap),
            ) {
                ProgressStepText(
                    text = if (step1 == 0) "설치된 앱 확인 중" else "설치된 앱 확인 완료",
                    opacity = stepOpacity(step1, isFirstStep = true),
                )
                ProgressStepText(
                    text = when (step2) {
                        0 -> "스크린 타임 분석 대기"
                        1 -> "스크린 타임 분석 진행 중"
                        else -> "스크린 타임 분석 완료"
                    },
                    opacity = stepOpacity(step2),
                )
                ProgressStepText(
                    text = when (step3) {
                        0 -> "설치된 앱 카테고리 분류 대기"
                        1 -> "설치된 앱 카테고리 분류 진행 중"
                        else -> "설치된 앱 카테고리 분류 완료"
                    },
                    opacity = stepOpacity(step3),
                )
            }
        }
    }
}

@Composable
private fun ProgressStepText(
    text: String,
    opacity: Float,
    modifier: Modifier = Modifier,
) {
    val animOpacity by animateFloatAsState(
        targetValue = opacity,
        animationSpec = tween(300),
        label = "stepOpacity",
    )
    Text(
        text = text,
        style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .alpha(animOpacity),
    )
}
