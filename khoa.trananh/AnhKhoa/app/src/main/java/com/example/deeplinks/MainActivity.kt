package com.example.deeplinks

import android.provider.Settings
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.deeplinks.ui.theme.DeeplinksTheme
import android.content.Context
import android.net.wifi.WifiManager


class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            DeeplinksTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        var appName by remember { mutableStateOf("") }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            when(viewModel.shortcutType) {
                                ShortcutType.STATIC -> {
                                    Text("Static feature")
                                }
                                ShortcutType.DYNAMIC -> {
                                    Text("Opening AnhKhoa's Github")
                                    openApp("github.com/AnhKhoa2174")
                                }
                                ShortcutType.PINNED -> {
                                    Text("Opening pinned shortcut")
                                }
                                null -> Text ("This app is developed by Khoa")
                            }
                            Button(
                                onClick = ::addDynamicShortcut
                            ) {
                                Text("Click this to open Github")
                            }
                            Button(
                                onClick = ::addPinnedShortcut
                            ) {
                                Text("Click this to open Youtube")
                            }
                            Button(
                                onClick = ::addPinnedKhoaShortcut
                            ) {
                                Text("Click this to open Khoa's Github")
                            }
                            OutlinedTextField(
                                value = appName,
                                onValueChange = { appName = it },
                                label = { Text("App Name") }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                openApp(appName)
                            }) {
                                Text(text = "Open App")
                            }
                        }
                    }
                }
            }

        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun addPinnedShortcut() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val shortcutManager = getSystemService<ShortcutManager>()!!
        if(shortcutManager.isRequestPinShortcutSupported) {
            val shortcut = ShortcutInfo.Builder(applicationContext, "pinned_youtube")
                .setShortLabel("Open Youtube")
                .setLongLabel("This will open Youtube")
                .setIcon(
                    Icon.createWithResource(
                        applicationContext, R.drawable.youtube
                    ))
                .setIntent(
                    Intent(applicationContext, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("shortcut_id", "pinned_youtube")
                    }
                )
                .build()

            val callbackIntent = shortcutManager.createShortcutResultIntent(shortcut)
            val successPendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                0,
                callbackIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
            shortcutManager.requestPinShortcut(shortcut, successPendingIntent.intentSender)
        }
    }

    private fun addPinnedKhoaShortcut() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val shortcutManager = getSystemService<ShortcutManager>()!!
        if(shortcutManager.isRequestPinShortcutSupported) {
            val shortcut = ShortcutInfo.Builder(applicationContext, "pinned_github")
                .setShortLabel("Open Khoa's Github")
                .setLongLabel("This will open Anh Khoa's Github")
                .setIcon(
                    Icon.createWithResource(
                        applicationContext, R.drawable.github
                    ))
                .setIntent(
                    Intent(applicationContext, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("shortcut_id", "pinned_github")
                    }
                )
                .build()

            val callbackIntent = shortcutManager.createShortcutResultIntent(shortcut)
            val successPendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                0,
                callbackIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
            shortcutManager.requestPinShortcut(shortcut, successPendingIntent.intentSender)
        }
    }

    private fun openApp(appName: String) {
        val intent = packageManager.getLaunchIntentForPackage(appName)
        if (intent != null) {
            startActivity(intent)
        } else {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://$appName")
                }
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addDynamicShortcut() {
        val shortcut = ShortcutInfoCompat.Builder(applicationContext, "dynamic")
            .setShortLabel("Open AnhKhoa's Github")
            .setLongLabel("Clicking this will open Anh Khoa's Github Account")
            .setIcon(
                IconCompat.createWithResource(
                    applicationContext, R.drawable.github
                ))
            .setIntent(
                Intent(applicationContext, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("shortcut_id", "dynamic")
                }
            )
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(applicationContext, shortcut)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            when(intent.getStringExtra("shortcut_id")) {
                "static" -> viewModel.onShortcutClicked(ShortcutType.STATIC)
                "dynamic" -> viewModel.onShortcutClicked(ShortcutType.DYNAMIC)
                "pinned_youtube" -> openApp("youtube.com")
                "pinned_github" -> openApp("github.com/AnhKhoa2174")
            }
        }
    }
}
