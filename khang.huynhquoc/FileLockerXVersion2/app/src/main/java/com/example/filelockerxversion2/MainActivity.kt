package com.example.filelockerxversion2

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.filelockerxversion2.CryptographyUtils.Companion.encryptFile
import com.example.filelockerxversion2.ui.theme.FileLockerXVersion2Theme
import java.nio.file.Paths
import java.util.UUID
import kotlin.io.path.Path

//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContent {
//            FileLockerXVersion2Theme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}

class MainActivity : ComponentActivity() {
    private var selectedFileUri by mutableStateOf<Uri?>(null)

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    finishAndRemoveTask()
                }
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val getPermissionIntent = Intent()
                getPermissionIntent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startForResult.launch(getPermissionIntent)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1111
                )
            }
        }

        enableEdgeToEdge()
        setContent {
            FileLockerXVersion2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                pickFile()
                            }, modifier = Modifier.padding(8.dp)
                        ) {
                            Text("Choose File to Encrypt")
                        }

                        selectedFileUri?.let { uri ->
                            Button(
                                onClick = {
                                    encryptSelectedFile(uri)
                                }, modifier = Modifier.padding(8.dp)
                            ) {
                                Text("Encrypt Selected File")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*"  // Chỉ định loại file cần chọn (ở đây là tất cả các loại)
        startActivityForResult.launch(intent)
    }

    private val startActivityForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                data?.data?.let { uri ->
                    selectedFileUri = uri
                    Toast.makeText(this, "File selected: $uri", Toast.LENGTH_SHORT).show()
                }
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun encryptSelectedFile(uri: Uri) {
        val Token = "275010a649c4d5690f10dc49b9418456"
        val salt = Token.toByteArray(Charsets.UTF_8)

        val inputFilePath = Path(FileUtils.getPathFromUri(this, uri) ?: "")
        val outputFolderPath = "/storage/emulated/0/"
        val outputFilePath = Path(
            outputFolderPath, UUID.randomUUID().toString()
        )// Specify your desired output file path here
        val password = "your_password"

        val iteration = 2048 // Example iteration count
        val keyLength = 384 // Example key length

        val startTime = System.currentTimeMillis()
        encryptFile(inputFilePath = inputFilePath,
            outputFilePath = outputFilePath,
            password = password,
            salt = salt,
            iteration = iteration,
            keyLength = keyLength,
            progressCallback = { progress ->
                // Update UI with progress if needed
            },
            errorCallback = { e ->
                // Handle error if needed
                Toast.makeText(this, "Encryption failed: ${e.message}", Toast.LENGTH_SHORT).show()
            })




        val endTime = System.currentTimeMillis()
        val elapsedTime = endTime - startTime
        val seconds = elapsedTime / 1000
        val milliseconds = elapsedTime % 1000

        Toast.makeText(this, "Encryption time: ${seconds}s ${milliseconds}ms", Toast.LENGTH_SHORT)
            .show()
        println("Encryption time: ${seconds}s ${milliseconds}ms")
    }
}

