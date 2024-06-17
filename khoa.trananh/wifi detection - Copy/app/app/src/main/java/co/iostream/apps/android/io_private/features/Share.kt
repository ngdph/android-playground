package co.iostream.apps.android.io_private.features

import android.os.Environment
import co.iostream.apps.android.core.iofile.FileUtils
import co.iostream.apps.android.io_private.configs.AppTypes
import co.iostream.apps.android.io_private.configs.Constants
import co.iostream.apps.android.io_private.models.CypherFileItem
import co.iostream.apps.android.io_private.models.FileFooter
import co.iostream.apps.android.io_private.models.FooterExtra
import co.iostream.apps.android.io_private.models.FooterMetadata
import co.iostream.apps.android.io_private.utils.AppDir
import co.iostream.apps.android.io_private.utils.CryptographyUtils
import co.iostream.apps.android.io_private.utils.ZipUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.pathString


class Share {
    data class Argv(
        val password: String, val keepOriginal: Boolean, val overwriteExisting: Boolean, val exportType: AppTypes.ExportType
    )

    companion object {
        fun encodeOne(item: CypherFileItem, argv: Argv) {
            if (Environment.MEDIA_MOUNTED != Environment.getExternalStorageState()) throw Exception()
            if (!item.inputInfo!!.exists()) throw Exception("File not exist.")

            val tempFolderPath = AppDir.get(AppDir.Type.TemporaryFolder)

            val outputZipFilePath = Path(tempFolderPath.pathString, UUID.randomUUID().toString())
            val outputTempFilePath = Path(tempFolderPath.pathString, UUID.randomUUID().toString())

            val encryptedFolderPath =
                Path(AppDir.get(AppDir.Type.LocalFolder).pathString, "Encrypted")
            FileUtils.createDirectoryIfNotExist(encryptedFolderPath)

            try {
                if (item.inputInfo!!.isRegularFile()) CryptographyUtils.encryptFile(
                    item.inputInfo!!,
                    outputTempFilePath,
                    argv.password,
                    Constants.Salt,
                    Constants.Iterations,
                    Constants.KeyLength
                )
                else {
                    ZipUtils.createFromDirectory(
                        item.inputInfo!!.pathString, outputZipFilePath.toString()
                    )

                    CryptographyUtils.encryptFile(
                        outputZipFilePath,
                        outputTempFilePath,
                        argv.password,
                        Constants.Salt,
                        Constants.Iterations,
                        Constants.KeyLength
                    )
                }

                val inputAttributes =
                    Files.readAttributes(item.inputInfo, BasicFileAttributes::class.java)

                val footerExtra = FooterExtra()
                footerExtra.Size = outputTempFilePath.fileSize()
                footerExtra.CreationTime = inputAttributes.creationTime().toMillis()
                footerExtra.LastWriteTime = inputAttributes.lastModifiedTime().toMillis()

                val footerMetadata = FooterMetadata(
                    if (item.inputInfo!!.isRegularFile()) FileUtils.Type.Unknown else FileUtils.Type.Directory,
                    item.inputInfo!!.pathString,
                    FooterExtra()
                )

                FileFooter.appendToFile(outputTempFilePath, footerMetadata, byteArrayOf())

                val outputFilePath =
                    Path(encryptedFolderPath.toString(), UUID.randomUUID().toString())
                Files.move(
                    outputTempFilePath,
                    outputFilePath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )

                item.inputInfo = outputFilePath
                item.encryptedInfo = outputFilePath
                item.originalInfo = Path(footerMetadata.OriginalName)

                item.tryLoadFooter(CypherFileItem.FileInfoType.Encrypted, true)

                FileUtils.deleteFileOrDirectory(footerMetadata.OriginalName)
            } catch (e: Exception) {
                throw e
            } finally {
                FileUtils.deleteFileOrDirectory(outputTempFilePath)
                FileUtils.deleteFileOrDirectory(outputZipFilePath)
            }
        }

        suspend fun decodeOne(item: CypherFileItem, argv: Argv) {
            if (Environment.MEDIA_MOUNTED != Environment.getExternalStorageState()) throw Exception()
            if (!item.encryptedInfo!!.exists() || item.footer == null) throw Exception("File not exist.")

            val tempFolderPath = AppDir.get(AppDir.Type.TemporaryFolder)

            val outputRawTempFilePath =
                Path(tempFolderPath.toString(), UUID.randomUUID().toString())
            val outputTempFilePath = Path(tempFolderPath.toString(), UUID.randomUUID().toString())

            try {
                withContext(Dispatchers.IO) {
                    Files.copy(
                        item.encryptedInfo,
                        outputRawTempFilePath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES
                    )

                    FileUtils.removeLastBytesFromFile(
                        outputRawTempFilePath, item.footer!!.getSize()
                    )
                    CryptographyUtils.decryptFile(
                        outputRawTempFilePath,
                        outputTempFilePath,
                        argv.password,
                        Constants.Salt,
                        Constants.Iterations,
                        Constants.KeyLength
                    )

                    item.recoveredFileOrFolderPath = Path(
                        tempFolderPath.toString(), Path(item.footer!!.metadata!!.OriginalName).name
                    ).toString()

                    if (item.footer!!.metadata!!.FileType != FileUtils.Type.Directory) Files.copy(
                        outputTempFilePath,
                        Path(item.recoveredFileOrFolderPath),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES
                    )
                    else ZipUtils.extractToDirectory(
                        outputTempFilePath, item.recoveredFileOrFolderPath, true
                    )
                }
            } catch (e: Exception) {
                throw e
            } finally {
                withContext(Dispatchers.IO) {
                    FileUtils.deleteFileOrDirectory(outputRawTempFilePath)
                    FileUtils.deleteFileOrDirectory(outputTempFilePath)
                }
            }
        }

        fun <T> phases(array: List<T>): List<List<List<T>>> {
            val phases = mutableListOf<List<List<T>>>()

            var phase = array.take(1).chunked(1)
            if (phase.isNotEmpty()) phases.add(phase)

            phase = array.drop(1).take(3).chunked(3)
            if (phase.isNotEmpty()) phases.add(phase)

            phase = array.drop(4).take(28).chunked(28)
            if (phase.isNotEmpty()) phases.add(phase)

            phase = array.drop(32).chunked(128)
            if (phase.isNotEmpty()) phases.add(phase)

            return phases
        }

        fun sleepByPhase(phase: Int) {
            if (phase == 0) Thread.sleep(100)
            else if (phase == 1) Thread.sleep(50)
        }
    }
}