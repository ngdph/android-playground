package com.example.xacthucvantay2

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import com.example.xacthucvantay2.ui.theme.Xacthucvantay2Theme
import com.plcoding.biometricauth.BiometricPromptManager

class MainActivity :  AppCompatActivity() {
    private val promptManager by lazy {
        BiometricPromptManager(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Xacthucvantay2Theme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color= Color.White
                ) {
                    val biometricResult by promptManager.promptResults.collectAsState(
                        initial = null
                    )
                    val enrollLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult(),
                        onResult = {
                            println("Activity result: $it")
                        }
                    )
                    LaunchedEffect(biometricResult) {
                        if(biometricResult is BiometricPromptManager.BiometricResult.AuthenticationNotSet) {
                            if(Build.VERSION.SDK_INT >= 30) {
                                val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                                    putExtra(
                                        Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                                        BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                                    )
                                }
                                enrollLauncher.launch(enrollIntent)
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Button(onClick = {
                            promptManager.showBiometricPrompt(
                                title = "XÁC THỰC",
                                description = ""
                            )
                        }) {
                            Text(text = "Xác thực")
                        }
                        biometricResult?.let { result ->
                            Text(
                                text = when(result) {
                                    is BiometricPromptManager.BiometricResult.AuthenticationError -> {
                                        //result.error
                                        "Lỗi xác thực"
                                    }
                                    BiometricPromptManager.BiometricResult.AuthenticationFailed -> {
                                        "Xác thực thất bại"
                                    }
                                    BiometricPromptManager.BiometricResult.AuthenticationNotSet -> {
                                        "Chưa đặt xác thực"
                                    }
                                    BiometricPromptManager.BiometricResult.AuthenticationSuccess -> {
                                        "Xác thực thành công"
                                    }
                                    BiometricPromptManager.BiometricResult.FeatureUnavailable -> {
                                        "Không có tính năng"
                                    }
                                    BiometricPromptManager.BiometricResult.HardwareUnavailable -> {
                                        "Hardware unavailable"
                                    }
                                }
                            )

                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

