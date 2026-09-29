package com.ishaq.visionlens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ishaq.visionlens.ui.screens.CameraScreen
import com.ishaq.visionlens.ui.screens.PermissionScreen
import com.ishaq.visionlens.ui.theme.VisionLensTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Always light status/nav bar icons: the app is dark-themed.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            VisionLensTheme {
                var granted by remember { mutableStateOf(hasCameraPermission()) }
                var permanentlyDenied by remember { mutableStateOf(false) }

                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { isGranted ->
                    granted = isGranted
                    // Android stops showing the dialog after repeated denials; send the user to Settings instead.
                    permanentlyDenied = !isGranted && !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
                }

                // Re-check when returning from system Settings.
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = hasCameraPermission() }

                Crossfade(targetState = granted, label = "screen") { hasPermission ->
                    if (hasPermission) {
                        CameraScreen()
                    } else {
                        PermissionScreen(
                            permanentlyDenied = permanentlyDenied,
                            onRequestPermission = { launcher.launch(Manifest.permission.CAMERA) },
                            onOpenSettings = ::openAppSettings,
                        )
                    }
                }
            }
        }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun openAppSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
        )
    }
}
