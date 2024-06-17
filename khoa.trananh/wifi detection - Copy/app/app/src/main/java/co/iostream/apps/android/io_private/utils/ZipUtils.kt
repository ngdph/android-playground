package co.iostream.apps.android.io_private.utils

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.progress.ProgressMonitor
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path


class ZipUtils private constructor() {
    companion object {
        fun createFromDirectory(
            sourceDirectoryName: String,
            destinationArchiveFileName: String,
            progressCallback: (progress: Int) -> Unit = {}
        ) {
            if (!Files.isDirectory(Path(sourceDirectoryName))) throw Exception()

            var zipFile: ZipFile? = null
            val progressMonitor: ProgressMonitor?

            try {
                zipFile = ZipFile(destinationArchiveFileName)
                progressMonitor = zipFile.progressMonitor

                zipFile.bufferSize = DEFAULT_BUFFER_SIZE
                zipFile.isRunInThread = true

                for (file in File(sourceDirectoryName).listFiles()!!) {
                    val zipParameters = ZipParameters()
                    zipParameters.compressionMethod = CompressionMethod.STORE
                    zipParameters.compressionLevel = CompressionLevel.NO_COMPRESSION

                    if (file.isFile) {
                        zipParameters.entrySize = file.length()
                        zipParameters.fileNameInZip = file.name
                        zipFile.addFile(file, zipParameters)
                    } else {
                        zipFile.addFolder(file, zipParameters)
                    }

                    while (!progressMonitor.state.equals(ProgressMonitor.State.READY)) {
                        progressCallback(progressMonitor.percentDone)
                        Thread.sleep(100)
                    }
                }

            } catch (e: Exception) {
                throw e
            } finally {
                zipFile?.close()
            }
        }

        fun extractToDirectory(
            sourceArchiveFileName: String,
            destinationDirectoryName: String,
            overwriteFiles: Boolean = false
        ) {
            var zipFile: ZipFile? = null
            try {
                zipFile = ZipFile(sourceArchiveFileName)
                zipFile.bufferSize = DEFAULT_BUFFER_SIZE
                zipFile.extractAll(destinationDirectoryName)
            } catch (e: Exception) {
                throw e
            } finally {
                zipFile?.close()
            }
        }

        fun extractToDirectory(
            sourceArchiveFileName: Path,
            destinationDirectoryName: String,
            overwriteFiles: Boolean = false
        ) {
            var zipFile: ZipFile? = null
            try {
                zipFile = ZipFile(sourceArchiveFileName.toFile())
                zipFile.bufferSize = DEFAULT_BUFFER_SIZE
                zipFile.extractAll(destinationDirectoryName)
            } catch (e: Exception) {
                throw e
            } finally {
                zipFile?.close()
            }
        }
    }
}