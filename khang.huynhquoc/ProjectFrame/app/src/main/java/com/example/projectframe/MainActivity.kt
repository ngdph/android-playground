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
import com.example.projectframe.Frame
import com.example.projectframe.ui.theme.Constants
import com.example.projectframe.ui.theme.ProjectFrameTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
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
                val buffer = ByteArray(2.0.pow(23).toInt())
                //val buffer = ByteArray(64)
                var bytesRead: Int
                var byteArrayIndex = 0
                var xorKeyIndex = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
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


    private fun getCipher(
        password: String, salt: ByteArray, iteration: Int, keyLength: Int, mode: Int
    ): Cipher {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val keySpec = PBEKeySpec(password.toCharArray(), salt, iteration, keyLength)

        val derivedKey = factory.generateSecret(keySpec).encoded

        val key = ByteArray(32)
        val iv = ByteArray(16)

        System.arraycopy(derivedKey, 0, key, 0, key.size)
        System.arraycopy(derivedKey, key.size, iv, 0, iv.size)

        val secretKeySpec = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(mode, secretKeySpec, ivSpec)

        return cipher
    }


    private fun encrypt(
        input: ByteArray, password: String, salt: ByteArray, iteration: Int, keyLength: Int
    ): ByteArray {
        val cipher = getCipher(password, salt, iteration, keyLength, Cipher.ENCRYPT_MODE)
        val results = cipher.doFinal(input)

        return results
    }
    private fun decrypt(
        input: ByteArray, password: String, salt: ByteArray, iteration: Int, keyLength: Int
    ): ByteArray {
        val cipher = getCipher(password, salt, iteration, keyLength, Cipher.DECRYPT_MODE)
        val results = cipher.doFinal(input)

        return results
    }





    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkStoragePermissions()) requestForStoragePermissions()
        enableEdgeToEdge()
        var EncryptedPackSize = 0
        val filePackPosition = mutableListOf<FilePack>()
        val EncryptedfilePackPosition = mutableListOf<FilePack>()
        setContent {
            ProjectFrameTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Button(onClick = {
                            val currentMemory = Runtime.getRuntime().freeMemory()
                            val numberOfCores = Runtime.getRuntime().availableProcessors()

                            var memoryLogarithm = log(
                                ((currentMemory / numberOfCores).toDouble()), 2.0
                            ).toInt() - 4
                            if (memoryLogarithm < 1) memoryLogarithm = 1
//                            val packSize = 2.0.pow(24).toInt()
                            val packSize = 102400.toInt()

                            val filePath =
                                    Path("/storage/emulated/obb/910aff08-6656-4e18-9e87-4bad06375da1.jpg")

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

                                val outputStream = ByteArrayOutputStream()

                                val deferredResults = (0 until numberOfCores).map {
                                    GlobalScope.async {
                                        do {
                                            val pair = findUndonePair()
                                            if (pair.first == -1) break

                                            val pack = pair.second ?: break

                                            var data = ByteArray(pack.len)

                                            val fis = FileInputStream(filePath.pathString)
                                            fis.skip(pack.off.toLong())
                                            fis.read(data, 0, pack.len)
                                            fis.close()

                                            val fos =
                                                FileOutputStream("/storage/emulated/0/fileMINI_ENCRYPTED${pair.first}")

                                            val password = "12345"

                                            val newdata = encrypt(
                                                data,
                                                password,
                                                Constants.Salt,
                                                Constants.Iterations,
                                                Constants.KeyLength
                                            )
                                            if(EncryptedPackSize==0 ){
                                                EncryptedPackSize = newdata.size
                                            }

                                            val sizeBytes = ByteBuffer.allocate(4).putInt(newdata.size).array()
                                            Log.d("MyTag", "Kích thước của data: ${data.size}") //
                                            Log.d("MyTag", "Kích thước của newdata: ${newdata.size}")

                                            fos.write(sizeBytes)
                                            fos.write(newdata)
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
                                val outputFile = File("/storage/emulated/0/fileBIG_ENCRYPTED")
                                val fosOutput = FileOutputStream(outputFile)

                                for (i in 0 until numberOfPack+1) {
                                    val inputFile = File("/storage/emulated/0/fileMINI_ENCRYPTED$i")
                                    val fisInput = FileInputStream(inputFile)

                                    val buffer = ByteArray(2.0.pow(25).toInt())
                                    var bytesRead: Int
                                    while (fisInput.read(buffer).also { bytesRead = it } != -1) {
                                        fosOutput.write(buffer, 0, bytesRead)
                                    }
                                    fisInput.close()
                                    inputFile.delete()
                                }
                                fosOutput.flush()
                                fosOutput.close()


                                Log.d("DevTag", "e ${System.nanoTime() - start}")
                            }
                        }
                        ) {
                            Text(text = "Encrypt Parallel")
                        }

                        Button(onClick = {
                            val currentMemory = Runtime.getRuntime().freeMemory()
                            val numberOfCores = Runtime.getRuntime().availableProcessors()

                            var memoryLogarithm = log(
                                ((currentMemory / numberOfCores).toDouble()), 2.0
                            ).toInt() - 4
                            if (memoryLogarithm < 1) memoryLogarithm = 1
                            val packSize = EncryptedPackSize + 4

                            val filePath =
                                Path("/storage/emulated/0/fileBIG_ENCRYPTED")
                            val fileSize = filePath.fileSize()

                            val numberOfPack = ceil((fileSize / packSize).toDouble()).toInt()

                            for (i in 0 until numberOfPack) EncryptedfilePackPosition.add(
                                FilePack(
                                    i * packSize, packSize, false, false
                                )
                            )
                            EncryptedfilePackPosition.add(
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
                                        EncryptedfilePackPosition.indexOfFirst { !it.completed && !it.isTaken }
                                    if (index != -1) {
                                        filePack = EncryptedfilePackPosition[index]
                                        EncryptedfilePackPosition[index].isTaken = true
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

                                            val fis2 = FileInputStream(filePath.pathString)

                                            var encryptedData = ByteArray(pack.len-4)
                                            fis2.skip(pack.off.toLong() + 4)
                                            fis2.read(encryptedData, 0, pack.len-4)
                                            fis2.close()

                                            val fos2 =
                                                FileOutputStream("/storage/emulated/0/fileMINI_DECRYPTED${pair.first}")

                                            val password = "12345"

                                            val newdata2 = decrypt(
                                                encryptedData,
                                                password,
                                                Constants.Salt,
                                                Constants.Iterations,
                                                Constants.KeyLength
                                            )

                                            fos2.write(newdata2)
                                            fos2.flush()
                                            fos2.close()

                                            reentrantLock.withLock {
                                                EncryptedfilePackPosition[pair.first].completed = true
                                                EncryptedfilePackPosition[pair.first].isTaken = true
                                            }
                                        } while (true)
                                    }
                                }

                                val results = deferredResults.awaitAll()
                                val outputFile = File("/storage/emulated/0/fileBIG_DECRYPTED")
                                val fosOutput = FileOutputStream(outputFile)

                                for (i in 0 until numberOfPack+1) {
                                    val inputFile = File("/storage/emulated/0/fileMINI_DECRYPTED$i")
                                    val fisInput = FileInputStream(inputFile)

                                    val buffer = ByteArray(2.0.pow(25).toInt())
                                    var bytesRead: Int
                                    while (fisInput.read(buffer).also { bytesRead = it } != -1) {
                                        fosOutput.write(buffer, 0, bytesRead)
                                    }
                                    fisInput.close()
                                    inputFile.delete()
                                }
                                fosOutput.flush()
                                fosOutput.close()


                                Log.d("DevTag", "e ${System.nanoTime() - start}")
                            }
                        }
                        ) {
                            Text(text = "Decrypt Parallel")
                        }

                        Button(onClick = {
                            GlobalScope.launch(Dispatchers.IO) {
                                val filePath =
                                    Path("/storage/emulated/obb/chapter1_th2.pt")

                                val outputFilePath = Path("/storage/emulated/0/fileBIG_ENCRYPTED")
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
                            Text(text = "Encrypt One big")
                        }
                    }
                }
            }
        }
    }
}
