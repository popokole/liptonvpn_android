package com.lipton.vpn

import android.Manifest
import android.animation.ObjectAnimator
import android.app.Activity
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
import com.lipton.vpn.ui.MainScreen
import com.lipton.vpn.ui.OnboardingScreen
import com.lipton.vpn.ui.theme.LiptonTheme
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

        setContent {
            val state by viewModel.state.collectAsState()
            LiptonTheme {
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
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri: Uri = intent?.data ?: return
        if (uri.scheme != "liptonvpn") return
        if (uri.host != "sub.popokole.online") return
        // Восстанавливаем HTTPS URL с путём и query-параметрами
        val url = uri.toString().replaceFirst("liptonvpn://", "https://")
        lifecycleScope.launch { viewModel.addSubscription(url) }
    }

    override fun onDestroy() {
        viewModel.unbindService(this)
        super.onDestroy()
    }
}
