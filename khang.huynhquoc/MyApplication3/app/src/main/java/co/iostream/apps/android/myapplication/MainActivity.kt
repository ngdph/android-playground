package co.iostream.apps.android.myapplication

import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import co.iostream.apps.android.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.experimental.xor
import kotlin.io.path.Path
import kotlin.io.path.fileSize
import kotlin.io.path.pathString
import kotlin.math.ceil
import kotlin.math.log
import kotlin.math.pow


data class FilePack(val off: Int, val len: Int, var completed: Boolean, var isTaken: Boolean)

class MainActivity : ComponentActivity() {
    private val storagePermissionCode: Int = 23

    private val storageActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {}

    private fun requestForStoragePermissions() {
        //Android is 11 (R) or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent()
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", this.packageName, null)
                intent.setData(uri)
                storageActivityResultLauncher.launch(intent)
            } catch (e: java.lang.Exception) {
                val intent = Intent()
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                storageActivityResultLauncher.launch(intent)
            }
        } else {
            //Below android 11
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ), storagePermissionCode
            )
        }
    }

    private fun checkStoragePermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //Android is 11 (R) or above
            return Environment.isExternalStorageManager()
        } else {
            //Below android 11
            val write = ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val read = ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.READ_EXTERNAL_STORAGE
            )

            return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun xorFileOneBig(
        inputFile: File, outputFile: File, xorKey: ByteArray
    ) {
        FileInputStream(inputFile).use { inputStream ->
            if (!outputFile.exists())
                outputFile.createNewFile()

            FileOutputStream(outputFile).use { outputStream ->
                val buffer = ByteArray(2.0.pow(24).toInt())
                var bytesRead: Int
                var byteArrayIndex = 0
                var xorKeyIndex = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
//                    for (i in 0 until bytesRead) {
//                        byteArrayIndex++
//                    }
                    for (i in 0 until bytesRead) {
                        buffer[i] = buffer[i] xor xorKey[xorKeyIndex]
                        xorKeyIndex = (xorKeyIndex + 1) % xorKey.size
                    }
                    outputStream.write(buffer, 0, bytesRead)
                }
            }
        }
    }


    private fun xorFileParallel(
        Input: ByteArray , xorKey: ByteArray
    ) {
        val buffer = ByteArray(2.0.pow(23).toInt())
        var bytesRead: Int
        var byteArrayIndex = 0
        var xorKeyIndex = 0

        for (i in 0 until Input.size) {

            Input[i] = Input[i] xor xorKey[xorKeyIndex]
            xorKeyIndex = (xorKeyIndex + 1) % xorKey.size

        }
        try {
        } catch (e: Exception) {
            Log.d("DevTag", "e ${e.message}")
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkStoragePermissions()) requestForStoragePermissions()
        enableEdgeToEdge()

        val filePackPosition = mutableListOf<FilePack>()

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Button(onClick = {
                            val currentMemory = Runtime.getRuntime().freeMemory()
                            val numberOfCores = Runtime.getRuntime().availableProcessors()

                            var memoryLogarithm = log(
                                ((currentMemory / numberOfCores).toDouble()), 2.0
                            ).toInt() - 4
                            if (memoryLogarithm < 1) memoryLogarithm = 1
                            val packSize = 2.0.pow(24).toInt()

                            val filePath =
                                Path("/storage/emulated/obb/Free_Test_Data_1.1MB_TIFF.tif")
                            val fileSize = filePath.fileSize()

                            val numberOfPack = ceil((fileSize / packSize).toDouble()).toInt()

                            for (i in 0 until numberOfPack) filePackPosition.add(
                                FilePack(
                                    i * packSize, packSize, false, false
                                )
                            )
                            filePackPosition.add(
                                FilePack(
                                    numberOfPack * packSize,
                                    fileSize.toInt() - numberOfPack * packSize,
                                    false,
                                    false
                                )
                            )

                            val reentrantLock = ReentrantLock()

                            fun findUndonePair(): Pair<Int, FilePack?> {
                                var index = -1
                                var filePack: FilePack? = null

                                reentrantLock.withLock {
                                    index =
                                        filePackPosition.indexOfFirst { !it.completed && !it.isTaken }
                                    if (index != -1) {
                                        filePack = filePackPosition[index]
                                        filePackPosition[index].isTaken = true
                                    }
                                }

                                return Pair(index, filePack)
                            }

                            GlobalScope.launch(Dispatchers.IO) {
                                val start = System.nanoTime()

                                val deferredResults = (0 until numberOfCores).map {
                                    GlobalScope.async {
                                        do {
                                            val pair = findUndonePair()
                                            if (pair.first == -1) break

                                            val pack = pair.second ?: break

                                            val data = ByteArray(pack.len)

                                            val fis = FileInputStream(filePath.pathString)
                                            fis.skip(pack.off.toLong())
                                            fis.read(data, 0, pack.len)
                                            fis.close()

                                            val fos =
                                                FileOutputStream("/storage/emulated/0/filecuakhang${pair.first}")

                                            val stringValue = "12345"
                                            val byteArray = ByteArray(stringValue.length) { index ->
                                                stringValue[index].toString().toInt().toByte()
                                            }
                                            xorFileParallel(data, byteArray)
                                            fos.write(data)
                                            fos.flush()
                                            fos.close()

                                            reentrantLock.withLock {
                                                filePackPosition[pair.first].completed = true
                                                filePackPosition[pair.first].isTaken = true
                                            }
                                        } while (true)
                                    }
                                }
                                val results = deferredResults.awaitAll()

                                Log.d("DevTag", "e ${System.nanoTime() - start}")
                            }
                        }) {
                            Text(text = "Parallel")
                        }

                        Button(onClick = {
                            GlobalScope.launch(Dispatchers.IO) {
                                val filePath =
                                    Path("/storage/emulated/obb/Free_Test_Data_1.1MB_TIFF.tif")

                                val outputFilePath = Path("/storage/emulated/0/filecuakhangm")
                                val outputFile = outputFilePath.toFile()

                                val start = System.nanoTime()
                                val stringValue = "12345"
                                val byteArray = ByteArray(stringValue.length) { index ->
                                    stringValue[index].toString().toInt().toByte()
                                }
                                xorFileOneBig(filePath.toFile(), outputFile, byteArray)
                                Log.d("DevTag", "a ${System.nanoTime() - start}")
                            }
                        }) {
                            Text(text = "One big")
                        }
                    }
                }
            }
        }
    }
}
