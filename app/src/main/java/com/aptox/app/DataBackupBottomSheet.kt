package com.aptox.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 설정 > 데이터 백업 바텀시트 (Figma 1694-5891 기본 / 1694-5959 유료구독자)
 * - 무료: Primary "기기에 데이터 백업하기" + Ghost "백업 데이터 복원하기" ([BaseBottomSheet] 2라인 = [AptoxTwoLineButton] 가이드와 동일)
 * - 유료(구독): Primary "지금 백업하기" + Ghost "백업 데이터 복원하기"
 */
enum class DataBackupSheetVariant {
    /** 데이터백업_기본 */
    Free,
    /** 데이터백업_유료구독자 */
    Premium,
}

@Composable
fun DataBackupBottomSheet(
    variant: DataBackupSheetVariant,
    onDismissRequest: () -> Unit,
    totalSizeText: String,
    lastBackupText: String,
    onPrimaryClick: () -> Unit = onDismissRequest,
    /** 무료 시트 하단 Ghost — 기본은 시트만 닫기, 복원 플로우 연결 시 교체 */
    onRestoreClick: () -> Unit = onDismissRequest,
) {
    val primaryText = when (variant) {
        DataBackupSheetVariant.Free -> "기기에 데이터 백업하기"
        DataBackupSheetVariant.Premium -> "지금 백업하기"
    }
    BaseBottomSheet(
        title = "데이터 백업",
        onDismissRequest = onDismissRequest,
        onPrimaryClick = onPrimaryClick,
        primaryButtonText = primaryText,
        secondaryButtonText = "백업 데이터 복원하기",
        onSecondaryClick = onRestoreClick,
    ) {
        DataBackupSummaryCard(
            totalSizeText = totalSizeText,
            lastBackupText = lastBackupText,
        )
    }
}

@Composable
private fun DataBackupSummaryCard(
    totalSizeText: String,
    lastBackupText: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.SurfaceBackgroundInfoBox, RoundedCornerShape(12.dp))
            .border(0.5.dp, AppColors.BorderInfoBox, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DataBackupSummaryRow(label = "전체용량", value = totalSizeText)
        DataBackupSummaryRow(label = "마지막 백업 일시", value = lastBackupText)
    }
}

@Composable
private fun DataBackupSummaryRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
        )
        Text(
            text = value,
            style = AppTypography.BodyBold.copy(color = AppColors.TextHighlight),
            textAlign = TextAlign.End,
        )
    }
}
