package com.lipton.vpn

import android.Manifest
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.lipton.vpn.service.LiptonNotificationHelper
import com.lipton.vpn.ui.MainScreen
import com.lipton.vpn.ui.OnboardingScreen
import com.lipton.vpn.ui.theme.LiptonTheme
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

        splash.setKeepOnScreenCondition { viewModel.state.value.loading }
        splash.setOnExitAnimationListener { view ->
            val anim = ObjectAnimator.ofFloat(view.view, View.ALPHA, 1f, 0f)
            anim.duration = 380L
            anim.interpolator = DecelerateInterpolator()
            anim.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: android.animation.Animator) { view.remove() }
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
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!state.loading && state.isFirstLaunch) {
                        OnboardingScreen(onFinish = { viewModel.dismissFirstLaunch() })
                    } else {
                        MainScreen(
                            state = state,
                            viewModel = viewModel,
                            activity = this,
                        )
                    }
                }
            }
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
    }

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
        lifecycleScope.launch { viewModel.addSubscription(url) }
    }

    override fun onDestroy() {
        viewModel.unbindService(this)
        super.onDestroy()
    }
}
