package com.cole.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun BlockOverlayTestScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("차단 오버레이 테스트")
        Spacer(modifier = Modifier.height(16.dp))
        ColePrimaryButton(
            text = "오버레이 띄우기",
            onClick = {
                val intent = android.content.Intent(context, BlockOverlayService::class.java).apply {
                    putExtra(BlockOverlayService.EXTRA_PACKAGE_NAME, "com.instagram.android")
                    putExtra(BlockOverlayService.EXTRA_BLOCK_UNTIL_MS, System.currentTimeMillis() + (3 * 60 + 30) * 60_000L) // 3시간 30분
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                onBack() // 오버레이 띄운 후 테스트 화면 닫기 → 디버그 메뉴로. 오버레이만 보이게
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(24.dp))
        ColeGhostButton(text = "돌아가기", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}
