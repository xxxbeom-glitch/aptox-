package com.aptox.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.aptox.app.ui.components.AptoxToast
import com.google.firebase.analytics.FirebaseAnalytics
import androidx.compose.ui.res.painterResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import android.util.Log
import android.content.Context
import android.widget.Toast
import com.aptox.app.subscription.SubscriptionBillingController
import com.aptox.app.subscription.SubscriptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 스플래시 직후: 미완료 시 Figma 기기 권한 온보딩(1652) → 메인.
 * 기존 PermissionScreen·앱 소개·자가테스트·사용패턴 분석 체인은 비활성화됨.
 * [FirstRunFlowRepository] Figma 권한 온보딩 완료 플래그는 전용 DataStore(백업 제외)에만 저장.
 */
private suspend fun resolveStepAfterAuth(context: Context, firstRunRepo: FirstRunFlowRepository): SignUpStep {
    if (firstRunRepo.isPermissionFigma1652OnboardingCompleted()) return SignUpStep.MAIN
    return SignUpStep.PERMISSION_ONBOARDING_1652
}

@Composable
fun AptoxRootContent(
    pendingPauseFlowFromOverlay: PendingPauseFlowFromOverlay? = null,
    pendingOpenBottomSheetPackage: String? = null,
    onOpenBottomSheetConsumed: () -> Unit = {},
    pendingNavIndex: Int? = null,
    onNavIndexConsumed: () -> Unit = {},
) {
    if (!BuildConfig.SHOW_DEBUG_MENU || !BuildConfig.DEBUG) {
        SignUpFlowHost(
            pendingPauseFlowFromOverlay = pendingPauseFlowFromOverlay,
            pendingOpenBottomSheetPackage = pendingOpenBottomSheetPackage,
            onOpenBottomSheetConsumed = onOpenBottomSheetConsumed,
            pendingNavIndex = pendingNavIndex,
            onNavIndexConsumed = onNavIndexConsumed,
        )
        return
    }
    var showDebugMenu by remember { mutableStateOf(true) }
    if (showDebugMenu) {
        DebugFlowHost(
            onStartNormalFlow = { showDebugMenu = false },
            modifier = Modifier.fillMaxSize(),
            pendingPauseFlowFromOverlay = pendingPauseFlowFromOverlay,
            pendingOpenBottomSheetPackage = pendingOpenBottomSheetPackage,
            onOpenBottomSheetConsumed = onOpenBottomSheetConsumed,
            pendingNavIndex = pendingNavIndex,
            onNavIndexConsumed = onNavIndexConsumed,
        )
    } else {
        SignUpFlowHost(
            pendingPauseFlowFromOverlay = pendingPauseFlowFromOverlay,
            pendingOpenBottomSheetPackage = pendingOpenBottomSheetPackage,
            onOpenBottomSheetConsumed = onOpenBottomSheetConsumed,
            pendingNavIndex = pendingNavIndex,
            onNavIndexConsumed = onNavIndexConsumed,
        )
    }
}

/** 차단 오버레이에서 일시정지 클릭 시 1단계(제안)부터 시작하는 플로우용 데이터 */
data class PendingPauseFlowFromOverlay(
    val packageName: String,
    val appName: String,
    val blockUntilMs: Long,
)

enum class SignUpStep {
    SPLASH,
    /** Figma 1652 스타일 기기 권한 온보딩 — 스플래시 직후 1회 */
    PERMISSION_ONBOARDING_1652,
    PERMISSION,
    /** App intro after device permission screen; next → SelfTest Ver2 */
    APP_INTRO_ONBOARDING,
    LOGIN,
    EMAIL,
    PASSWORD,
    NAME_BIRTH_PHONE,
    VERIFICATION,
    COMPLETE,
    SELFTEST,
    SELFTEST_VER2,
    SELFTEST_LOADING,
    USAGE_PATTERN_ANALYSIS,
    ONBOARDING_START,
    SELFTEST_RESULT,
    ADD_APP,
    TIME_SPECIFIED,
    MAIN,
    // 비밀번호 재설정 RS-01 ~ RS-03
    PASSWORD_RESET_EMAIL,
    PASSWORD_RESET_CODE,
    PASSWORD_RESET_NEW,
}

class MainActivity : ComponentActivity() {

    /**
     * 알림 미허용 시 앱이 다시 보일 때마다(최소 한 번 onStop 경과 후) 시스템 권한 팝업 시도.
     * 권한 다이얼로그는 보통 onStop 없이 onPause만 일어나므로, 연속 onResume으로 즉시 재요청되지 않음.
     */
    private var shouldAttemptPostNotificationsOnNextResume = true

    private val requestPostNotificationsOnAppOpenLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /** 앱 제한 오버레이에서 일시정지 클릭 후 1단계(제안)부터 시작할 플로우 데이터 (packageName, appName, blockUntilMs) */
    private val pendingPauseFlowState = mutableStateOf<PendingPauseFlowFromOverlay?>(null)
    var pendingPauseFlowFromOverlay: PendingPauseFlowFromOverlay?
        get() = pendingPauseFlowState.value
        set(value) { pendingPauseFlowState.value = value }

    /** 시스템 알림/오버레이에서 카운트 중지·시작 버튼 탭 시 열 바텀시트의 packageName */
    private val pendingOpenBottomSheetState = mutableStateOf<String?>(null)
    var pendingOpenBottomSheetPackage: String?
        get() = pendingOpenBottomSheetState.value
        set(value) { pendingOpenBottomSheetState.value = value }

    /** 주간 리포트/목표 달성 알림 탭 시 열 탭 인덱스 (1=챌린지, 2=통계) */
    private val pendingNavIndexState = mutableStateOf<Int?>(null)
    var pendingNavIndex: Int?
        get() = pendingNavIndexState.value
        set(value) { pendingNavIndexState.value = value }

    fun clearPendingPauseFlowFromOverlay() {
        pendingPauseFlowState.value = null
    }

    fun clearPendingOpenBottomSheet() {
        pendingOpenBottomSheetState.value = null
    }

    fun clearPendingNavIndex() {
        pendingNavIndexState.value = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingPauseFlowState.value = extractPauseFlowFromIntent(intent)
        pendingOpenBottomSheetState.value = intent.getStringExtra(AppMonitorService.EXTRA_OPEN_BOTTOM_SHEET)
        if (intent.hasExtra(EXTRA_NAV_INDEX)) {
            pendingNavIndexState.value = intent.getIntExtra(EXTRA_NAV_INDEX, 0).takeIf { it in 1..3 }
        }
    }

    override fun onStart() {
        super.onStart()
        AptoxApplication.startAppMonitorIfNeeded(applicationContext, clearForegroundPkg = pendingOpenBottomSheetPackage != null)
    }

    override fun onResume() {
        super.onResume()
        AptoxApplication.startAppMonitorIfNeeded(applicationContext, clearForegroundPkg = pendingOpenBottomSheetPackage != null)
        if (shouldAttemptPostNotificationsOnNextResume) {
            shouldAttemptPostNotificationsOnNextResume = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPostNotificationsOnAppOpenLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("AptoxMain", "onPause")
    }

    override fun onStop() {
        super.onStop()
        shouldAttemptPostNotificationsOnNextResume = true
        Log.d("AptoxMain", "onStop")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }
        pendingPauseFlowState.value = extractPauseFlowFromIntent(intent)
        pendingOpenBottomSheetState.value = intent.getStringExtra(AppMonitorService.EXTRA_OPEN_BOTTOM_SHEET)
        if (intent.hasExtra(EXTRA_NAV_INDEX)) {
            pendingNavIndexState.value = intent.getIntExtra(EXTRA_NAV_INDEX, 0).takeIf { it in 1..3 }
        }
        setContent {
            AptoxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColors.SurfaceBackgroundBackground,
                ) {
                    if (BuildConfig.SHOW_DEBUG_MENU && BuildConfig.DEBUG) {
                        AptoxRootContent(
                            pendingPauseFlowFromOverlay = pendingPauseFlowFromOverlay,
                            pendingOpenBottomSheetPackage = pendingOpenBottomSheetPackage,
                            onOpenBottomSheetConsumed = ::clearPendingOpenBottomSheet,
                            pendingNavIndex = pendingNavIndex,
                            onNavIndexConsumed = ::clearPendingNavIndex,
                        )
                    } else {
                        SignUpFlowHost(
                            pendingPauseFlowFromOverlay = pendingPauseFlowFromOverlay,
                            pendingOpenBottomSheetPackage = pendingOpenBottomSheetPackage,
                            onOpenBottomSheetConsumed = ::clearPendingOpenBottomSheet,
                            pendingNavIndex = pendingNavIndex,
                            onNavIndexConsumed = ::clearPendingNavIndex,
                        )
                    }
                }
            }
        }
    }

    companion object {
        /** 주간 리포트/목표 달성 알림 탭 시 열 탭 (1=챌린지, 2=통계) */
        const val EXTRA_NAV_INDEX = "com.aptox.app.EXTRA_NAV_INDEX"
        /** 홈 위젯(무료) 추가·탭 후 구독 바텀시트 */
        const val EXTRA_OPEN_SUBSCRIPTION_FROM_WIDGET = "com.aptox.app.EXTRA_OPEN_SUBSCRIPTION_FROM_WIDGET"
    }

    private fun extractPauseFlowFromIntent(i: Intent?): PendingPauseFlowFromOverlay? {
        if (i?.action != BlockDialogActivity.ACTION_PAUSE_FLOW_FROM_OVERLAY) return null
        val pkg = i.getStringExtra(BlockDialogActivity.EXTRA_PACKAGE_NAME)
        val name = i.getStringExtra(BlockDialogActivity.EXTRA_APP_NAME)
        val blockUntilMs = i.getLongExtra(BlockDialogActivity.EXTRA_BLOCK_UNTIL_MS, 0L)
        return if (pkg != null && name != null) PendingPauseFlowFromOverlay(pkg, name, blockUntilMs) else null
    }
}

@Composable
fun SignUpFlowHost(
    pendingPauseFlowFromOverlay: PendingPauseFlowFromOverlay? = null,
    pendingOpenBottomSheetPackage: String? = null,
    onOpenBottomSheetConsumed: () -> Unit = {},
    pendingNavIndex: Int? = null,
    onNavIndexConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val goToMain = pendingPauseFlowFromOverlay != null || pendingOpenBottomSheetPackage != null || pendingNavIndex != null
    var step by remember {
        mutableStateOf(if (goToMain) SignUpStep.MAIN else SignUpStep.SPLASH)
    }
    LaunchedEffect(pendingPauseFlowFromOverlay, pendingOpenBottomSheetPackage, pendingNavIndex) {
        if (goToMain) step = SignUpStep.MAIN
    }
    var selfTestAnswers by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var selfTestUserName by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }
    var loginLoading by remember { mutableStateOf(false) }
    var loginCancelToast by remember { mutableStateOf<String?>(null) }
    var loginCancelToastKey by remember { mutableStateOf(0) }
    var autoOpenPackage by remember { mutableStateOf<String?>(null) }
    var prefilledApp by remember { mutableStateOf<com.aptox.app.model.SelectedAppInfo?>(null) }
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }
    val firstRunRepo = remember { FirstRunFlowRepository(context) }
    val firebaseAnalytics = remember {
        FirebaseAnalytics.getInstance(context.applicationContext)
    }

    val postNotificationsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* MAIN으로 이미 전환됨; 결과는 무시 */ }

    BackHandler(
        enabled = step == SignUpStep.ADD_APP || step == SignUpStep.TIME_SPECIFIED,
    ) {
        prefilledApp = null
        step = SignUpStep.MAIN
    }

    val completePermissionOnboarding: () -> Unit = {
        scope.launch {
            firstRunRepo.setPermissionFigma1652OnboardingCompleted(true)
            val appCtx = context.applicationContext
            UsageStatsInitialSync.flushPendingInitialSyncIfNeeded(appCtx)
            UsageStatsInitialSync.enqueueInitial7DayIfPermitted(appCtx)
            (appCtx as? AptoxApplication)?.applicationScope?.launch(Dispatchers.Default) {
                runCatching {
                    AppSelectableAppsCache.set(AppSelectableAppsLoader.load(appCtx))
                }
            }
            step = SignUpStep.MAIN
        }
    }

    // 현재 화면 렌더링
    when (step) {
        SignUpStep.SPLASH -> SplashScreen(
            onFinish = {
                scope.launch {
                    step = resolveStepAfterAuth(context, firstRunRepo)
                }
            },
        )
        SignUpStep.PERMISSION_ONBOARDING_1652 -> DebugPermissionUsageAccessOnboarding1652Screen(
            onBack = completePermissionOnboarding,
            onCloseSkipPermissions = completePermissionOnboarding,
            onStartAptox = completePermissionOnboarding,
        )
        // 아래 온보딩·구 Permission 분기는 숨김 — 진입 시 즉시 MAIN (잘못된 step 방지)
        SignUpStep.PERMISSION -> {
            LaunchedEffect(Unit) { step = SignUpStep.MAIN }
            Box(modifier = Modifier.fillMaxSize())
        }
        SignUpStep.APP_INTRO_ONBOARDING -> {
            LaunchedEffect(Unit) { step = SignUpStep.MAIN }
            Box(modifier = Modifier.fillMaxSize())
        }
        SignUpStep.LOGIN -> Box(modifier = Modifier.fillMaxSize()) {
            SplashLoginScreen(
                initialButtonsVisible = true,
                onGoogleLoginClick = {
                    loginError = null
                    loginLoading = true
                    scope.launch {
                        authRepository.signInWithGoogle(context)
                            .onSuccess {
                                loginLoading = false
                                scope.launch {
                                    step = resolveStepAfterAuth(context, firstRunRepo)
                                }
                            }
                            .onFailure { e ->
                                loginLoading = false
                                if (LoginAnalytics.isGoogleLoginCancelled(e)) {
                                    LoginAnalytics.logLoginCancelled(
                                        firebaseAnalytics,
                                        "google",
                                        "signup_login",
                                    )
                                    loginCancelToastKey++
                                    loginCancelToast = "로그인을 취소했습니다"
                                } else {
                                    LoginAnalytics.logLoginFailed(
                                        firebaseAnalytics,
                                        "google",
                                        e.message ?: "구글 로그인에 실패했어요",
                                    )
                                    loginError = e.message ?: "구글 로그인에 실패했어요"
                                }
                            }
                    }
                },
                errorMessage = loginError,
                onClearError = { loginError = null },
                isLoading = loginLoading,
            )
            AptoxToast(
                message = loginCancelToast ?: "",
                visible = loginCancelToast != null,
                onDismiss = { loginCancelToast = null },
                replayKey = loginCancelToastKey,
            )
        }
        // 비활성화: 회원가입 플로우 (EMAIL, PASSWORD, NAME_BIRTH_PHONE, VERIFICATION, COMPLETE)
        SignUpStep.EMAIL,
        SignUpStep.PASSWORD,
        SignUpStep.NAME_BIRTH_PHONE,
        SignUpStep.VERIFICATION,
        SignUpStep.COMPLETE -> {
            Box(modifier = Modifier.fillMaxSize()) {
                LaunchedEffect(Unit) { step = SignUpStep.LOGIN }
            }
        }
        SignUpStep.SELFTEST -> {
            LaunchedEffect(Unit) { step = SignUpStep.MAIN }
            Box(modifier = Modifier.fillMaxSize())
        }
        SignUpStep.SELFTEST_VER2 -> {
            LaunchedEffect(Unit) { step = SignUpStep.MAIN }
            Box(modifier = Modifier.fillMaxSize())
        }
        SignUpStep.SELFTEST_LOADING -> {
            LaunchedEffect(Unit) { step = SignUpStep.MAIN }
            Box(modifier = Modifier.fillMaxSize())
        }
        SignUpStep.USAGE_PATTERN_ANALYSIS -> {
            LaunchedEffect(Unit) { step = SignUpStep.MAIN }
            Box(modifier = Modifier.fillMaxSize())
        }
        SignUpStep.ONBOARDING_START -> {
            LaunchedEffect(Unit) { step = SignUpStep.MAIN }
            Box(modifier = Modifier.fillMaxSize())
        }
        SignUpStep.SELFTEST_RESULT -> {
            LaunchedEffect(Unit) { step = SignUpStep.MAIN }
            Box(modifier = Modifier.fillMaxSize())
        }
        SignUpStep.ADD_APP -> {
            var showAddAppPremiumSheet by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxSize()) {
                AddAppFlowHost(
                    onComplete = {
                        prefilledApp = null
                        step = SignUpStep.MAIN
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            postNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onBackFromFirst = {
                        prefilledApp = null
                        step = SignUpStep.MAIN
                    },
                    initialPrefilledApp = prefilledApp,
                    onRequestPremiumSubscription = { showAddAppPremiumSheet = true },
                )
                if (showAddAppPremiumSheet) {
                    val act = context as? ComponentActivity
                    SubscriptionBottomSheet(
                        onDismissRequest = { showAddAppPremiumSheet = false },
                        onSubscribe = { tier ->
                            if (SubscriptionManager.isSubscribed(context.applicationContext)) {
                                Toast.makeText(context, "이미 구독 중입니다", Toast.LENGTH_SHORT).show()
                            } else {
                                act?.let { SubscriptionBillingController.launchSubscriptionFlow(it, tier) }
                            }
                        },
                    )
                }
            }
        }
        SignUpStep.TIME_SPECIFIED -> TimeSpecifiedFlowHost(
            onComplete = {
                prefilledApp = null
                step = SignUpStep.MAIN
            },
            onBackFromFirst = {
                prefilledApp = null
                step = SignUpStep.MAIN
            },
            initialPrefilledApp = prefilledApp,
        )
        SignUpStep.MAIN -> MainFlowHost(
            onAddAppClick = { app ->
                prefilledApp = app
                step = SignUpStep.ADD_APP
            },
            onTimeSpecifiedClick = { app ->
                prefilledApp = app
                step = SignUpStep.TIME_SPECIFIED
            },
            onLogout = { step = SignUpStep.LOGIN },
            initialPauseFlowFromOverlay = pendingPauseFlowFromOverlay,
            onPauseFlowConsumed = { activity?.clearPendingPauseFlowFromOverlay() },
            initialAutoOpenPackage = autoOpenPackage ?: pendingOpenBottomSheetPackage,
            onAutoOpenConsumed = {
                autoOpenPackage = null
                onOpenBottomSheetConsumed()
            },
            initialNavIndex = pendingNavIndex,
            onNavIndexConsumed = onNavIndexConsumed,
        )
        // 비활성화: 비밀번호 찾기 플로우
        SignUpStep.PASSWORD_RESET_EMAIL,
        SignUpStep.PASSWORD_RESET_CODE,
        SignUpStep.PASSWORD_RESET_NEW -> {
            Box(modifier = Modifier.fillMaxSize()) {
                LaunchedEffect(Unit) { step = SignUpStep.LOGIN }
            }
        }
    }
}
