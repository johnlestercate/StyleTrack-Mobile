package com.styletrack.customer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.styletrack.customer.data.SessionState
import com.styletrack.customer.notify.NotificationWorker
import com.styletrack.customer.notify.Notifier
import com.styletrack.customer.ui.AppNav
import com.styletrack.customer.ui.AuthScreen
import com.styletrack.customer.ui.LoadingBox
import com.styletrack.customer.ui.LocalRepo
import com.styletrack.customer.ui.StyleTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as StyleTrackApp
        setContent {
            StyleTrackTheme {
                CompositionLocalProvider(LocalRepo provides app.repo) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Root(app)
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun Root(app: StyleTrackApp) {
    val context = LocalContext.current
    val session by app.session.state.collectAsState()

    LaunchedEffect(Unit) { app.repo.restoreSession() }

    when (session) {
        SessionState.Starting -> LoadingBox()
        SessionState.SignedOut -> {
            LaunchedEffect(Unit) { NotificationWorker.stop(context) }
            AuthScreen()
        }
        is SessionState.SignedIn -> {
            LaunchedEffect(Unit) { NotificationWorker.start(context) }
            AskForNotificationPermission()
            AppNav()
        }
    }
}

/** Android 13+ needs the customer's OK before booking updates can show as phone notifications. */
@androidx.compose.runtime.Composable
private fun AskForNotificationPermission() {
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (!asked && Build.VERSION.SDK_INT >= 33 && !Notifier.hasPermission(context)) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
