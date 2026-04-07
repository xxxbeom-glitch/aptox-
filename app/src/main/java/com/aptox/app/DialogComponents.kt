package com.aptox.app

import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Figma 1214-4443: 팝업 라운드 12dp, 타이틀-본문 간격 16dp, 버튼 라운드 6dp */
private val DialogPopupCorner = RoundedCornerShape(12.dp)
private val DialogButtonCorner = RoundedCornerShape(6.dp)

/**
 * 필수 권한 안내 팝업 (Figma 1285-4277, MA-01)
 * - [AptoxConfirmDialog]와 동일 레이아웃 · 토큰
 * - 본문 2줄 + 단일 버튼 ([confirmButtonText], 기본 「닫기」)
 */
@Composable
fun AptoxRequiredPermissionDialog(
    onDismissRequest: () -> Unit,
    onCloseClick: () -> Unit = onDismissRequest,
    confirmButtonText: String = "닫기",
    modifier: Modifier = Modifier,
) {
    AptoxConfirmDialog(
        onDismissRequest = onDismissRequest,
        title = "필수 권한을 설정해주세요",
        subtitle = "필수 권한이 허용되지 않으면\n앱의 필수 기능을 사용하실 수 없어요",
        confirmButtonText = confirmButtonText,
        onConfirmClick = onCloseClick,
        modifier = modifier,
    )
}

/**
 * 확인(·취소) 팝업 (Figma 1214-4443)
 * - 제목 + 선택 부제 + 확인 1개 또는 확인+취소 2줄 버튼
 * - [subtitle]이 비어 있으면 본문 영역 생략
 * - [dismissButtonText]가 null이 아니면 [AptoxTwoLineButton] (확인 상단 · 취소 하단)
 */
@Composable
fun AptoxConfirmDialog(
    onDismissRequest: () -> Unit,
    title: String,
    subtitle: String = "",
    confirmButtonText: String = "확인",
    onConfirmClick: () -> Unit = { onDismissRequest() },
    dismissButtonText: String? = null,
    onDismissButtonClick: (() -> Unit)? = null,
    /** true면 확인 버튼을 경고색(삭제 등)으로 표시 */
    confirmButtonDestructive: Boolean = false,
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
        ),
    ) {
        AptoxConfirmDialogPanel(
            title = title,
            subtitle = subtitle,
            confirmButtonText = confirmButtonText,
            onConfirmClick = onConfirmClick,
            dismissButtonText = dismissButtonText,
            onDismissButtonClick = onDismissButtonClick ?: onDismissRequest,
            confirmButtonDestructive = confirmButtonDestructive,
            modifier = modifier,
        )
    }
}

@Composable
internal fun AptoxConfirmDialogPanel(
    title: String,
    subtitle: String,
    confirmButtonText: String,
    onConfirmClick: () -> Unit,
    dismissButtonText: String?,
    onDismissButtonClick: () -> Unit,
    confirmButtonDestructive: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .widthIn(max = 328.dp)
            .shadow(6.dp, DialogPopupCorner, false, Color.Black.copy(alpha = 0.06f))
            .clip(DialogPopupCorner)
            .background(AppColors.SurfaceBackgroundBackground)
            .padding(top = 32.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = title,
                    style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
                    textAlign = TextAlign.Center,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (dismissButtonText != null) {
                AptoxTwoLineButton(
                    primaryText = confirmButtonText,
                    ghostText = dismissButtonText,
                    onPrimaryClick = onConfirmClick,
                    onGhostClick = onDismissButtonClick,
                    shape = DialogButtonCorner,
                    primaryDestructive = confirmButtonDestructive,
                )
            } else if (confirmButtonDestructive) {
                Button(
                    onClick = onConfirmClick,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = DialogButtonCorner,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Red300,
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 16.dp),
                ) {
                    Text(
                        text = confirmButtonText,
                        style = AppTypography.ButtonLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                AptoxPrimaryButton(
                    text = confirmButtonText,
                    onClick = onConfirmClick,
                    shape = DialogButtonCorner,
                )
            }
        }
    }
}

/**
 * 스크롤 목록 + 닫기 (디버그 드롭다운 등). M3 AlertDialog 대체.
 */
@Composable
fun AptoxOptionsListDialog(
    onDismissRequest: () -> Unit,
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    closeButtonText: String = "닫기",
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            modifier = modifier
                .widthIn(max = 328.dp)
                .shadow(6.dp, DialogPopupCorner, false, Color.Black.copy(alpha = 0.06f))
                .clip(DialogPopupCorner)
                .background(AppColors.SurfaceBackgroundBackground)
                .padding(top = 32.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = title,
                    style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    options.forEachIndexed { index, opt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    onSelect(index)
                                    onDismissRequest()
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = opt,
                                style = AppTypography.BodyMedium.copy(
                                    color = if (index == selectedIndex) AppColors.Primary400 else AppColors.TextPrimary,
                                ),
                            )
                        }
                    }
                }
                AptoxGhostButton(
                    text = closeButtonText,
                    onClick = onDismissRequest,
                    shape = DialogButtonCorner,
                )
            }
        }
    }
}

/**
 * [ComponentActivity]에서 시스템 [AlertDialog] 대신 앱톡스 확인 팝업 (제목만 또는 제목+부제, 단일 확인).
 * 구독 완료·탈퇴 감사 등 Compose 트리 밖에서 사용.
 */
fun ComponentActivity.showAptoxConfirmDialog(
    title: String,
    subtitle: String = "",
    confirmButtonText: String = "확인",
    cancelable: Boolean = true,
    onDismiss: () -> Unit = {},
    onConfirm: () -> Unit,
) {
    val dialog = android.app.Dialog(this)
    dialog.window?.apply {
        setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
    }
    dialog.setCancelable(cancelable)
    dialog.setCanceledOnTouchOutside(cancelable)
    val composeView = ComposeView(this).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            AptoxTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .then(
                            if (cancelable) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    dialog.dismiss()
                                    onDismiss()
                                }
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { /* consume */ },
                    ) {
                        AptoxConfirmDialogPanel(
                            title = title,
                            subtitle = subtitle,
                            confirmButtonText = confirmButtonText,
                            onConfirmClick = {
                                dialog.dismiss()
                                onConfirm()
                            },
                            dismissButtonText = null,
                            onDismissButtonClick = {},
                            confirmButtonDestructive = false,
                        )
                    }
                }
            }
        }
    }
    dialog.setContentView(composeView)
    dialog.setOnDismissListener { onDismiss() }
    dialog.show()
}

/**
 * 다이얼로그 팝업 가이드 (Figma 310:2725, 1214-4443 스타일 통일)
 * - 아이콘/이미지 + 제목 + 부제 + 날짜 + 2줄 버튼
 * - 팝업 12dp 라운드, 버튼 6dp 라운드, 타이틀-부제 16dp
 */
@Composable
fun AptoxGuideDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    date: String? = null,
    primaryButtonText: String = "계속 진행",
    secondaryButtonText: String = "돌아가기",
    onPrimaryClick: () -> Unit = { onDismissRequest() },
    onSecondaryClick: () -> Unit = { onDismissRequest() },
    icon: @Composable (() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            modifier = modifier
                .widthIn(max = 328.dp)
                .shadow(6.dp, DialogPopupCorner, false, Color.Black.copy(alpha = 0.06f))
                .clip(DialogPopupCorner)
                .background(AppColors.SurfaceBackgroundBackground)
                .padding(top = 32.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // 아이콘 + 텍스트 블록
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // 아이콘 영역 (72dp) - 연보라 플레이스홀더 또는 커스텀
                    if (icon != null) {
                        icon()
                    } else {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(AppColors.Primary100),
                        )
                    }
                    // 제목 + 부제 (타이틀-본문 간격 16dp)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = title,
                            style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
                            textAlign = TextAlign.Center,
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    if (date != null) {
                        Text(
                            text = date,
                            style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                AptoxTwoLineButton(
                    primaryText = primaryButtonText,
                    ghostText = secondaryButtonText,
                    onPrimaryClick = onPrimaryClick,
                    onGhostClick = onSecondaryClick,
                    shape = DialogButtonCorner,
                )
            }
        }
    }
}
