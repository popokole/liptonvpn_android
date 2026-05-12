package com.lipton.vpn

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        viewModel.setPermissionLauncher(vpnPermissionLauncher)
        viewModel.bindService(this)
        handleDeepLink(intent)

        setContent {
            val state by viewModel.state.collectAsState()
            LiptonTheme(appTheme = state.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        state = state,
                        viewModel = viewModel,
                        activity = this,
                    )
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
        val url = "https://${uri.host}${uri.path}"
        lifecycleScope.launch { viewModel.addSubscription(url) }
    }

    override fun onDestroy() {
        viewModel.unbindService(this)
        super.onDestroy()
    }
}
