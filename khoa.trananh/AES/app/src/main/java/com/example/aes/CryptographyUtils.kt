package com.example.aes

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Path
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.io.path.fileSize

class CryptographyUtils private constructor() {
    companion object {
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

        fun encrypt(
            inputBytes: ByteArray, password: String, salt: ByteArray, iteration: Int, keyLength: Int
        ): ByteArray {
            val cipher = getCipher(password, salt, iteration, keyLength, Cipher.ENCRYPT_MODE)
            val results = cipher.doFinal(inputBytes)

            return results
        }

        fun encrypt(
            inputString: String, password: String, salt: ByteArray, iteration: Int, keyLength: Int
        ): ByteArray {
            val cipher = getCipher(password, salt, iteration, keyLength, Cipher.ENCRYPT_MODE)
            val results = cipher.doFinal(inputString.toByteArray())

            return results
        }


        fun decrypt(
            input: ByteArray, password: String, salt: ByteArray, iteration: Int, keyLength: Int
        ): ByteArray {
            val cipher = getCipher(password, salt, iteration, keyLength, Cipher.DECRYPT_MODE)
            val results = cipher.doFinal(input)

            return results
        }


        fun decrypt(
            inputString: String, password: String, salt: ByteArray, iteration: Int, keyLength: Int
        ): ByteArray {
            val cipher = getCipher(password, salt, iteration, keyLength, Cipher.DECRYPT_MODE)
            val results = cipher.doFinal(inputString.toByteArray())

            return results
        }

        @OptIn(ExperimentalEncodingApi::class)
        fun encryptToB64(
            inputString: String, password: String, salt: ByteArray, iteration: Int, keyLength: Int
        ): String {
            val cipher = getCipher(password, salt, iteration, keyLength, Cipher.ENCRYPT_MODE)
            val results = cipher.doFinal(inputString.toByteArray())

            val result: String = Base64.encode(results)
            return result
        }

        @OptIn(ExperimentalEncodingApi::class)
        fun decryptFromB64(
            inputString: String, password: String, salt: ByteArray, iteration: Int, keyLength: Int
        ): String {
            val encryptedBytes = Base64.decode(inputString)

            val cipher = getCipher(password, salt, iteration, keyLength, Cipher.DECRYPT_MODE)

            val decrypted = cipher.doFinal(encryptedBytes)
            val result = String(decrypted)

            return result
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun encryptFile(
            inputFilePath: Path,
            outputFilePath: Path,
            password: String, salt: ByteArray, iteration: Int, keyLength: Int,
            progressCallback: ((progress: Double) -> Unit) = {},
            errorCallback: ((e: Exception) -> Unit) = {},
        ) {
            val cipher = getCipher(password, salt, iteration, keyLength, Cipher.ENCRYPT_MODE)

            var fis: FileInputStream? = null
            var cos: CipherOutputStream? = null

            try {
                val inputFile = inputFilePath.toFile()
                val inputSize = inputFile.length()
                fis = FileInputStream(inputFile)
                cos = CipherOutputStream(FileOutputStream(outputFilePath.toFile()), cipher)

                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var read = fis.read(buffer)

                val total = inputFilePath.fileSize()
                var done = 0L

                while (read > -1) {
                    cos.write(buffer, 0, read)
                    read = fis.read(buffer)

                    if (read > -1) {
                        done += read
                        progressCallback(done.toDouble() / total)
                    }
                }

                cos.flush()
            } catch (e: Exception) {
                errorCallback(e)

                throw CryptographyException()
            } finally {
                fis?.close()
                cos?.close()
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun decryptFile(
            inputFilePath: Path,
            outputFilePath: Path,
            password: String, salt: ByteArray, iteration: Int, keyLength: Int,
            progressCallback: ((progress: Double) -> Unit) = {},
            errorCallback: ((e: Exception) -> Unit) = {},
        ) {
            val cipher = getCipher(password, salt, iteration, keyLength, Cipher.DECRYPT_MODE)

            var cis: CipherInputStream? = null
            var fos: FileOutputStream? = null

            try {
                cis = CipherInputStream(FileInputStream(inputFilePath.toFile()), cipher)
                fos = FileOutputStream(outputFilePath.toFile())

                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var read = cis.read(buffer)


                val total = inputFilePath.fileSize()
                var done = 0L

                while (read > -1) {
                    fos.write(buffer, 0, read)
                    read = cis.read(buffer)

                    if (read > -1) {
                        done += read
                        progressCallback(done.toDouble() / total)
                    }
                }
            } catch (e: Exception) {
                errorCallback(e)

                throw CryptographyException()
            } finally {
                cis?.close()
                fos?.close()
            }
        }

        fun decryptBuffer(
            encryptedBuffer: ByteArray,
            password: String,
            salt: ByteArray,
            iteration: Int,
            keyLength: Int,
        ): ByteArray {
            val cipher = getCipher(password, salt, iteration, keyLength, Cipher.DECRYPT_MODE)

            var cis: CipherInputStream? = null
            var outputStream: ByteArrayOutputStream? = null

            return try {
                cis = CipherInputStream(ByteArrayInputStream(encryptedBuffer), cipher)
                outputStream = ByteArrayOutputStream()

                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var read = cis.read(buffer)

                val total = encryptedBuffer.size.toLong()
                var done = 0L

                while (read > -1) {
                    outputStream.write(buffer, 0, read)
                    read = cis.read(buffer)

                    if (read > -1) {
                        done += read
                        //progressCallback(done.toDouble() / total)
                    }
                }

                outputStream.toByteArray()
            } catch (e: Exception) {
                //errorCallback(e)
                throw CryptographyException()
            } finally {
                cis?.close()
                outputStream?.close()
            }
        }

    }

}

class CryptographyException: Exception()