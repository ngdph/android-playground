package co.iostream.apps.android.io_private.utils

import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Path
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
            password: String, salt: ByteArray, interation: Int, keyLength: Int, mode: Int
        ): Cipher {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val keySpec = PBEKeySpec(password.toCharArray(), salt, interation, keyLength)

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
            inputBytes: ByteArray,
            password: String,
            salt: ByteArray,
            interation: Int,
            keyLength: Int
        ): ByteArray {
            val cipher = getCipher(password, salt, interation, keyLength, Cipher.ENCRYPT_MODE)
            return cipher.doFinal(inputBytes)
        }

        fun decrypt(
            inputBytes: ByteArray,
            password: String,
            salt: ByteArray,
            interation: Int,
            keyLength: Int
        ): ByteArray {
            val cipher = getCipher(password, salt, interation, keyLength, Cipher.DECRYPT_MODE)
            return cipher.doFinal(inputBytes)
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
                fis = FileInputStream(inputFilePath.toFile())
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
    }

}