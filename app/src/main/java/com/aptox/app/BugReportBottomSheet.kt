package com.aptox.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

private const val BUG_REPORT_MAX_LENGTH = 1000

/**
 * 버그 신고 바텀시트 (Figma 1022-3968)
 * - 제목: "버그 신고" (HeadingH1)
 * - 텍스트 입력 필드 (높이 174dp, FormInputBgDefault 배경, 1000자 제한)
 * - 등록 버튼 (AptoxPrimaryButton) / 취소 버튼 (AptoxGhostButton)
 */
@Composable
fun BugReportBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }

    BaseBottomSheet(
        title = "버그 신고",
        onDismissRequest = onDismiss,
        onPrimaryClick = { onSubmit(text) },
        primaryButtonText = "등록",
        secondaryButtonText = "취소",
        onSecondaryClick = onDismiss,
        primaryButtonEnabled = text.isNotBlank(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = text,
                onValueChange = { newValue ->
                    if (newValue.length <= BUG_REPORT_MAX_LENGTH) {
                        text = newValue
                    }
                },
            modifier = Modifier
                .fillMaxWidth()
                .height(174.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AppColors.FormInputBgDefault)
                .padding(20.dp),
            textStyle = AppTypography.BodyMedium.copy(color = AppColors.FormTextValue),
                decorationBox = { innerTextField ->
                    if (text.isEmpty()) {
                        Text(
                            text = "어떤 상황에서 발생했나요? \n재현 방법이 있다면 함께 적어주세요 (1000자 이내)",
                            style = AppTypography.BodyMedium.copy(color = AppColors.FormTextPlaceholder),
                        )
                    }
                    innerTextField()
                },
            )
        }
    }
}
