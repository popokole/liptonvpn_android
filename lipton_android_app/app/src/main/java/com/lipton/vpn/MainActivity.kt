package com.lipton.vpn

import android.Manifest
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.lipton.vpn.service.LiptonNotificationHelper
import com.lipton.vpn.ui.MainScreen
import com.lipton.vpn.ui.auth.AuthFlow
import com.lipton.vpn.ui.auth.LoginSuccessScreen
import com.lipton.vpn.ui.auth.authStepFor
import com.lipton.vpn.ui.theme.LiptonTheme
import com.lipton.vpn.ui.theme.ThemeRevealHost
import com.lipton.vpn.ui.theme.isDark
import com.lipton.vpn.worker.ExpiryCheckWorker
import com.lipton.vpn.worker.LogCleanupWorker
import com.lipton.vpn.worker.TrafficCheckWorker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val serverId = viewModel.state.value.activeServerId
                ?: viewModel.state.value.subscriptions.flatMap { it.servers }.firstOrNull()?.id
                ?: return@registerForActivityResult
            viewModel.connect(this, serverId)
        }
    }

    // Android 13+ требует явного запроса разрешения на уведомления
    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* результат не требует обработки */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        CrashLogger.install(this)
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Свечение уходит под статус-бар и навигацию; цвет иконок баров выставляет LiptonTheme.
        applySystemBars()

        splash.setKeepOnScreenCondition { viewModel.state.value.loading }
        splash.setOnExitAnimationListener { view ->
            // Сплэш (Android 12+) при выходе возвращает цвета баров из темы окна — ставим свои снова.
            applySystemBars()
            val anim = ObjectAnimator.ofFloat(view.view, View.ALPHA, 1f, 0f)
            anim.duration = 380L
            anim.interpolator = DecelerateInterpolator()
            anim.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: android.animation.Animator) {
                    view.remove()
                    applySystemBars()
                }
            })
            anim.start()
        }

        // Запрашиваем разрешение на уведомления (нужно для VPN foreground notification)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        viewModel.setPermissionLauncher(vpnPermissionLauncher)
        viewModel.bindService(this)
        handleDeepLink(intent)
        handleShortcut(intent)

        // Schedule background workers
        LiptonNotificationHelper.ensureChannels(this)
        TrafficCheckWorker.schedule(this)
        ExpiryCheckWorker.schedule(this)
        LogCleanupWorker.schedule(this)

        // Check What's New
        viewModel.checkWhatsNew(BuildConfig.VERSION_NAME)

        // Check clipboard for subscription URL
        checkClipboard()

        setContent {
            val state by viewModel.state.collectAsState()
            LiptonTheme(appTheme = state.themeMode) {
                ThemeRevealHost(currentTheme = state.themeMode, onApply = { viewModel.setThemeMode(it) }) {
                    Box(Modifier.fillMaxSize().background(LiptonTheme.colors.bg)) {
                        // Корень: онбординг и вход → «Вы вошли» → приложение; гость — приложение без аккаунта.
                        val root = when {
                            state.loading -> Root.MAIN
                            state.isAuthed && state.loginSuccess -> Root.SUCCESS
                            state.isAuthed -> Root.MAIN
                            state.guest != null && state.authEntry == null -> Root.MAIN
                            else -> Root.AUTH
                        }
                        AnimatedContent(
                            targetState = root,
                            transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(220)) },
                            label = "root",
                        ) { r ->
                            when (r) {
                                Root.AUTH -> AuthFlow(
                                    state = state,
                                    vm = viewModel,
                                    activity = this@MainActivity,
                                    start = authStepFor(state.authEntry),
                                    onClose = if (state.guest != null) ({ viewModel.openAuth(null) }) else null,
                                )
                                Root.SUCCESS -> LoginSuccessScreen(state = state, onContinue = { viewModel.dismissLoginSuccess() })
                                Root.MAIN -> MainScreen(
                                    state = state,
                                    viewModel = viewModel,
                                    activity = this@MainActivity,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /** Прозрачные бары (edge-to-edge) и цвет их иконок по текущей теме приложения. */
    private fun applySystemBars() {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        val systemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val dark = viewModel.state.value.themeMode.isDark(systemDark)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
        handleShortcut(intent)
        checkClipboard()
    }

    override fun onResume() {
        super.onResume()
        checkClipboard()
        // Таймеры не идут, пока телефон спит: проверяем сроки пробного доступа при возврате.
        viewModel.checkTrialDeadlines()
    }

    private enum class Root { AUTH, SUCCESS, MAIN }

    private fun checkClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val text = cm.primaryClip?.getItemAt(0)?.text?.toString()
        viewModel.checkClipboard(this, text)
    }

    private fun handleShortcut(intent: Intent?) {
        if (intent?.action != "com.lipton.vpn.SHORTCUT_TOGGLE") return
        viewModel.handleConnectToggle(this)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri: Uri = intent?.data ?: return
        if (uri.scheme != "liptonvpn") return
        val url = when (uri.host) {
            "add" -> uri.path?.removePrefix("/") ?: return  // liptonvpn://add/https://...
            else  -> uri.toString().replaceFirst("liptonvpn://", "https://")  // legacy
        }
        if (url.isBlank()) return
        lifecycleScope.launch {
            try { viewModel.addSubscription(url) }
            catch (e: Exception) { viewModel.showError(e.message) }
        }
    }

    override fun onDestroy() {
        viewModel.unbindService(this)
        super.onDestroy()
    }
}
