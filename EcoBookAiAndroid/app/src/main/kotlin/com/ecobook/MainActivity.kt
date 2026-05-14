package com.ecobook

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ecobook.auth.SessionManager
import com.ecobook.fcm.NotificationInboxRepository
import com.ecobook.fcm.NotificationIntentRouter
import com.ecobook.fcm.NotificationNavigationManager
import com.ecobook.model.SessionDestination
import com.ecobook.navigation.NavGraph
import com.ecobook.ui.theme.EcoBookTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var notificationNavigationManager: NotificationNavigationManager

    @Inject
    lateinit var notificationInboxRepository: NotificationInboxRepository

    private var notificationPermissionRequestedThisSession = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Timber.i("Notification permission denied by the user.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeNotificationPermissionReadiness()
        routeNotificationIntent(intent)
        setContent {
            EcoBookTheme {
                NavGraph(notificationNavigationManager = notificationNavigationManager)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeNotificationIntent(intent)
    }

    private fun observeNotificationPermissionReadiness() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionManager.sessionState
                    .map { it.destination }
                    .distinctUntilChanged()
                    .collectLatest { destination ->
                        if (destination == SessionDestination.MAIN) {
                            requestNotificationPermissionIfNeeded()
                        }
                    }
            }
        }
    }

    private fun routeNotificationIntent(intent: Intent?) {
        val notification = NotificationIntentRouter.messageFromIntent(intent)
        notification?.let {
            notificationInboxRepository.record(it)
            notificationNavigationManager.queue(it.destination)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (notificationPermissionRequestedThisSession) {
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        notificationPermissionRequestedThisSession = true
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
