package com.example.aes

import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.aes.CryptographyUtils.Companion.encryptFile
import com.example.aes.ui.theme.AESTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException
import kotlin.concurrent.withLock
import kotlin.io.path.Path
import kotlin.io.path.exists
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
        // Android 11 (R) or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", this.packageName, null)
                intent.data = uri
                storageActivityResultLauncher.launch(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                storageActivityResultLauncher.launch(intent)
            }
        } else {
            // Below android 11
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
            // Android 11 (R) or above
            return Environment.isExternalStorageManager()
        } else {
            // Below android 11
            val write = ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val read = ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
            return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun encryptSelectedFile(uri: Uri) {
        val Token = "275010a649c4d5690f10dc49b9418456"
        val salt = Token.toByteArray(Charsets.UTF_8)

        val inputFilePath = Path(FileUtils.getPathFromUri(this, uri) ?: "")
        val outputFolderPath = "/storage/emulated/0/"
        val outputFilePath = Path(outputFolderPath, UUID.randomUUID().toString())
        val password = "your_password"

        val iteration = 2048
        val keyLength = 384

        encryptFile(
            inputFilePath = inputFilePath,
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
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkStoragePermissions()) requestForStoragePermissions()
        enableEdgeToEdge()
        val framePackPosition = mutableListOf<FilePack>()
        val filePackPosition = mutableListOf<FilePack>()
        setContent {
            AESTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Button(onClick = {
                            val currentMemory = Runtime.getRuntime().freeMemory()
                            val numberOfCores = Runtime.getRuntime().availableProcessors()

                            var memoryLogarithm = log(
                                ((currentMemory / numberOfCores).toDouble()), 2.0
                            ).toInt() - 4
                            if (memoryLogarithm < 1) memoryLogarithm = 1
                            val packSize = 64 * 1000
                            val filePath =
                                Path("/storage/emulated/0/Download/910aff08-6656-4e18-9e87-4bad06375da1.jpg")
                            val fileSize = filePath.fileSize()
                            val outputFile = filePath.toFile()
                            val outputFileSize = outputFile.length()
                            Log.d("DevTag", "Size of original file: $outputFileSize bytes")
                            val numberOfPack = ceil((fileSize / packSize).toDouble()).toInt()

                            for (i in 0 until numberOfPack) filePackPosition.add(
                                FilePack(i * packSize, packSize, false, false)
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

                               val password = "your_password"
                                val salt =
                                    "275010a649c4d5690f10dc49b9418456".toByteArray(Charsets.UTF_8)
                                val iteration = 2048
                                val keyLength = 384

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

                                            val encryptedData = CryptographyUtils.encrypt(
                                                data, password, salt, iteration, keyLength
                                            )

                                            val fos =
                                                FileOutputStream("/storage/emulated/0/Aaa/${pair.first}")
                                            fos.write(encryptedData)
                                            fos.flush()
                                            fos.close()

                                            reentrantLock.withLock {
                                                filePackPosition[pair.first].completed = true
                                                filePackPosition[pair.first].isTaken = true
                                            }
                                            Log.d(
                                                "DevTag",
                                                "Pack ${pair.first} encrypted with size ${encryptedData.size}"
                                            )
                                        } while (true)
                                    }
                                }
                                deferredResults.awaitAll()

                            }
                        }) {
                            Text(text = "Encrypt Parallel")
                        }

                        Button(onClick = {
                            GlobalScope.launch(Dispatchers.IO) {
                                val filePath =
                                    Path("/storage/emulated/0/Download/910aff08-6656-4e18-9e87-4bad06375da1.jpg")
                                val outputFilePath = Path("/storage/emulated/0/Aaa/0")

                                encryptSelectedFile(Uri.fromFile(filePath.toFile()))
                            }
                        }) {
                            Text(text = "Encrypt Big One")
                        }
                        Button(onClick = {
                            GlobalScope.launch(Dispatchers.IO) {
                                val outputFilePath = "/storage/emulated/0/Aaa/concatenated_file"
                                val file = File(outputFilePath)
                                val outputFile = File(outputFilePath)
                                if (!outputFile.exists()) {
                                    outputFile.createNewFile()
                                }

                                val fos = FileOutputStream(outputFile)

                                val sortedPacks = filePackPosition.sortedBy { it.off }
                                sortedPacks.forEach { pack ->
                                    val packFilePath = "/storage/emulated/0/Aaa/${filePackPosition.indexOf(pack)}"
                                    val packFile = File(packFilePath)
                                    if (packFile.exists()) {
                                        val packData = packFile.readBytes()
                                        val packSize = packData.size

                                        val frameBuffer = ByteBuffer.allocate(4 + packSize)
                                        frameBuffer.putInt(packSize)
                                        frameBuffer.put(packData)

                                        fos.write(frameBuffer.array())
                                    } else {
                                        Log.e("DevTag", "File not found: $packFilePath")
                                    }
                                }

                                fos.flush()
                                fos.close()
                                val size = file.length()
                                Log.d("DevTag", "All packs concatenated into $outputFilePath, and the size is : $size")
                            }
                        }) {
                            Text(text = "Concatenate Parallel")
                        }

                        Button(onClick = {
                            val inputFilePath = Path("/storage/emulated/0/Aaa/concatenated_file")
                            if (!inputFilePath.exists()) {
                                Log.d("DevTag", "File does not exist: $inputFilePath")
                            } else {
                                GlobalScope.launch(Dispatchers.IO) {
                                    val outputFilePath = Path("/storage/emulated/0/Aaa/decrypted_final_file")
                                    val password = "your_password"
                                    val salt = "275010a649c4d5690f10dc49b9418456".toByteArray(Charsets.UTF_8)
                                    val iteration = 2048
                                    val keyLength = 384

                                    val outputFile = outputFilePath.toFile()
                                    if (!outputFile.exists()) {
                                        outputFile.createNewFile()
                                    }

                                    val fis = FileInputStream(inputFilePath.toFile())

                                    val framePackPosition = mutableListOf<FilePack>()
                                    val buffer = ByteArray(4)

                                    // Read the entire file to determine frame pack positions
                                    while (true) {
                                        val bytesRead = fis.read(buffer)
                                        if (bytesRead != 4) break

                                        val frameSize = ByteBuffer.wrap(buffer).int
                                        if (frameSize <= 0) break

                                        framePackPosition.add(FilePack(fis.channel.position().toInt(), frameSize, false, false))
                                        fis.skip(frameSize.toLong())
                                    }
                                    fis.close()

                                    val reentrantLock = ReentrantLock()
                                    fun findUndoneFrame(): Pair<Int, FilePack?> {
                                        var index = -1
                                        var framePack: FilePack? = null

                                        reentrantLock.withLock {
                                            index = framePackPosition.indexOfFirst { !it.completed && !it.isTaken }
                                            if (index != -1) {
                                                framePack = framePackPosition[index]
                                                framePackPosition[index].isTaken = true
                                            }
                                        }

                                        return Pair(index, framePack)
                                    }

                                    val deferredResults = (0 until Runtime.getRuntime().availableProcessors()).map {
                                        GlobalScope.async {
                                            do {
                                                val pair = findUndoneFrame()
                                                if (pair.first == -1) break

                                                val pack = pair.second ?: break
                                                val frameData = ByteArray(pack.len + 4)

                                                val fis = FileInputStream(inputFilePath.toFile())
                                                fis.channel.position(pack.off.toLong() - 4)
                                                fis.read(frameData)
                                                fis.close()

                                                val frameSize = ByteBuffer.wrap(frameData, 0, 4).int
                                                val packData = frameData.copyOfRange(4, 4 + frameSize)

                                                val decryptedData: ByteArray? = try {
                                                    CryptographyUtils.decrypt(packData, password, salt, iteration, keyLength)
                                                } catch (e: Exception) {
                                                    Log.e("DecryptError", "Decryption failed: ${e.message}")
                                                    e.printStackTrace()
                                                    null
                                                }

                                                if (decryptedData != null) {
                                                    val fos = FileOutputStream(outputFile, true)
                                                    fos.write(decryptedData)
                                                    fos.flush()
                                                    fos.close()
                                                    Log.d("DecryptSuccess", "Pack ${pair.first} decrypted with size ${decryptedData.size}")
                                                }

                                                reentrantLock.withLock {
                                                    framePackPosition[pair.first].completed = true
                                                }
                                            } while (true)
                                        }
                                    }
                                    deferredResults.awaitAll()

                                    val outputFileSize = outputFile.length()
                                    Log.d("DevTag", "Size of decrypted file: $outputFileSize bytes")
                                    Log.d("DevTag", "Final decrypted file created at $outputFilePath")
                                }
                            }
                        }) {
                            Text(text = "Decrypt Parallel")
                        }


                    }
                }
            }
        }
    }
}

fun ByteArray.toHexString(): String {
    val hexArray = "0123456789ABCDEF".toCharArray()
    val hexChars = CharArray(this.size * 2)
    for (j in this.indices) {
        val v = this[j].toInt() and 0xFF
        hexChars[j * 2] = hexArray[v ushr 4]
        hexChars[j * 2 + 1] = hexArray[v and 0x0F]
    }
    return String(hexChars)
}

fun printHexView(data: ByteArray) {
    val hexString = StringBuilder(data.size * 2)
    for (byte in data) {
        val hex = Integer.toHexString(byte.toInt() and 0xFF)
        if (hex.length == 1) {
            hexString.append('0')
        }
        hexString.append(hex)
    }
    Log.d("HexView", hexString.toString())
}

