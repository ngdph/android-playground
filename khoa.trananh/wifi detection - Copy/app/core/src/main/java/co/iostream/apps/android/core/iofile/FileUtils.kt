package co.iostream.apps.android.core.iofile

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Size
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.CompletableDeferred
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.DecimalFormat
import java.util.Locale
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

open class FileFormat(val name: String, val type: FileUtils.Type) {
    var isSupported: Boolean = false
    var extra: Any? = null
}

class FileUtils {
    enum class Type {
        Unknown, Image, Video, Audio, Compressed, Document, Directory;

        companion object {
            fun fromInt(value: Int) = entries.first { it.ordinal == value }
        }
    }

    companion object {
        val EXTENSIONS: HashMap<Type, Array<String>> = hashMapOf(
            Type.Image to ImageUtils.IMAGE_FORMATS.flatMap { it.value.extensions.toList() }
                .distinct().toTypedArray(),
            Type.Video to MediaUtils.VIDEO_FORMATS.flatMap { it.value.extensions.toList() }
                .distinct().toTypedArray(),
            Type.Audio to MediaUtils.AUDIO_FORMATS.flatMap { it.value.extensions.toList() }
                .distinct().toTypedArray(),
            Type.Compressed to CompressedUtils.COMPRESSED_FORMATS.flatMap { it.value.extensions.toList() }
                .distinct().toTypedArray(),
            Type.Document to DocumentUtils.DOCUMENT_FORMATS.flatMap { it.value.extensions.toList() }
                .distinct().toTypedArray(),
        )

        fun createFormat(format: ImageUtils.Family, isSupported: Boolean, extra: Any? = null) =
            ImageFormat(ImageUtils.IMAGE_FORMATS[format]!!).apply {
                this.isSupported = isSupported
                this.extra = extra
            }

        fun createFormat(format: MediaUtils.Family, isSupported: Boolean, extra: Any? = null) =
            MediaFormat(MediaUtils.MEDIA_FORMATS[format]!!).apply {
                this.isSupported = isSupported
                this.extra = extra
            }

        fun createFormat(format: DocumentUtils.Family, isSupported: Boolean, extra: Any? = null) =
            DocumentFormat(DocumentUtils.DOCUMENT_FORMATS[format]!!).apply {
                this.isSupported = isSupported
                this.extra = extra
            }

        fun createFormat(format: CompressedUtils.Family, isSupported: Boolean, extra: Any? = null) =
            CompressedFormat(CompressedUtils.COMPRESSED_FORMATS[format]!!).apply {
                this.isSupported = isSupported
                this.extra = extra
            }

        fun createFormatPair(format: ImageUtils.Family, isSupported: Boolean, extra: Any? = null) =
            Pair(format, ImageFormat(ImageUtils.IMAGE_FORMATS[format]!!).apply {
                this.isSupported = isSupported
                this.extra = extra
            })

        fun createFormatPair(format: MediaUtils.Family, isSupported: Boolean, extra: Any? = null) =
            Pair(format, MediaFormat(MediaUtils.MEDIA_FORMATS[format]!!).apply {
                this.isSupported = isSupported
                this.extra = extra
            })

        fun createFormatPair(
            format: DocumentUtils.Family, isSupported: Boolean, extra: Any? = null
        ) = Pair(format, DocumentFormat(DocumentUtils.DOCUMENT_FORMATS[format]!!).apply {
            this.isSupported = isSupported
            this.extra = extra
        })

        fun createFormatPair(
            format: CompressedUtils.Family, isSupported: Boolean, extra: Any? = null
        ) = Pair(format, CompressedFormat(CompressedUtils.COMPRESSED_FORMATS[format]!!).apply {
            this.isSupported = isSupported
            this.extra = extra
        })

        fun isType(extension: String, type: Type) =
            EXTENSIONS[type]!!.contains(extension.lowercase(Locale.ROOT))

        fun getType(path: String): Type {
            if (!isExistFileOrDirectory(path)) throw IOException()

            if (Files.isDirectory(Paths.get(path))) return Type.Directory

            val ext = File(path).extension.lowercase(Locale.ROOT)

            for ((key, value) in EXTENSIONS) if (value.contains(ext)) return key

            return Type.Unknown
        }

        fun getTypeByExtension(extension: String): Type {
            val ext = extension.lowercase(Locale.ROOT)

            EXTENSIONS.forEach { entry ->
                if (entry.value.contains(ext)) return entry.key
            }

            return Type.Unknown
        }

        fun isFilePath(path: String): Boolean? {
            return if (isExistFileOrDirectory(path)) !Files.isDirectory(Paths.get(path)) else null
        }

        fun isDirectoryPath(path: String): Boolean? {
            return if (isExistFileOrDirectory(path)) Files.isDirectory(Paths.get(path)) else null
        }

        fun getFileSize(filePath: String): Long {
            return if (isFilePath(filePath) == true) File(filePath).length() else -1
        }

        fun isExistFileOrDirectory(path: String) = Files.exists(Paths.get(path))

        fun isEmptyDirectory(path: String) = Files.list(Paths.get(path)).count() == 0L

        fun createFreshDirectory(path: String) {
            deleteFileOrDirectory(path)
            createDirectoryIfNotExist(path)
        }

        fun createDirectoryIfNotExist(path: String) {
            if (!Files.exists(Path(path))) Files.createDirectory(Path(path))
        }

        fun createDirectoryIfNotExist(path: Path) {
            if (!path.exists()) Files.createDirectories(path)
        }

        fun copyDirectory(sourceDir: String, destinationDir: String, recursive: Boolean) {
            val directory = File(sourceDir)

            if (!directory.exists()) throw Exception("Source directory not found: ${directory.absolutePath}")

            val directories = directory.listFiles { file -> file.isDirectory }

            File(destinationDir).mkdirs()

            for (file in directory.listFiles { file -> file.isFile }!!) file.copyTo(
                File(
                    destinationDir, file.name
                ), true
            )

            if (recursive) {
                for (dir in directories!!) copyDirectory(
                    dir.path, "$destinationDir/${dir.name}", true
                )
            }
        }

        @OptIn(ExperimentalPathApi::class)
        fun deleteFileOrDirectory(path: String) {
            try {
                if (path.isBlank()) return

                val isFile = isFilePath(path)
                if (isFile != null) {
                    try {
                        if (isFile) Path(path).deleteIfExists()
                        else {
                            emptyDirectory(path)
                            Path(path).deleteRecursively()
                        }
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
            }
        }

        @OptIn(ExperimentalPathApi::class)
        fun deleteFileOrDirectory(path: Path) {
            try {
                if (!path.exists()) return

                try {
                    if (path.isRegularFile()) path.deleteIfExists()
                    else {
                        emptyDirectory(path)
                        path.deleteRecursively()
                    }
                } catch (_: Exception) {
                }
            } catch (_: Exception) {
            }
        }

        fun emptyDirectory(path: String) {
            val info = File(path)
            for (i in info.listFiles()!!) i.delete()
        }

        fun emptyDirectory(path: Path) {
            val info = path.toFile()
            for (i in info.listFiles()!!) i.delete()
        }

        fun loadMemoryStreamFromFile(filePath: String): ByteArrayOutputStream {
            val fileStream = FileInputStream(filePath)

            val memoryStream = ByteArrayOutputStream()
            fileStream.copyTo(memoryStream)
            return memoryStream
        }

        fun saveMemoryStreamToFile(filePath: String, memoryStream: ByteArrayOutputStream) {
            val fileStream = FileOutputStream(filePath)
            memoryStream.writeTo(fileStream)
        }

        fun removeLastBytesFromFile(path: String, size: Long) {
            RandomAccessFile(
                path, "rw"
            ).setLength(0.coerceAtLeast((File(path).length() - size).toInt()).toLong())
        }

        fun removeLastBytesFromFile(path: Path, size: Long) {
            RandomAccessFile(path.toFile(), "rw").setLength(
                0.coerceAtLeast(
                    (path.toFile().length() - size).toInt()
                ).toLong()
            )
        }

        fun removeAndGetLastBytesFromFile(path: String, size: Long): ByteArray {
            val bytes = ByteArray(size.toInt())

            val file = RandomAccessFile(path, "rw")
            file.seek(file.length() - size)
            file.read(bytes)
            file.setLength(maxOf(0, file.length() - size))
            file.close()

            return bytes
        }

        fun removeAndGetLastBytesFromFile(path: Path, size: Long): ByteArray {
            val bytes = ByteArray(size.toInt())

            val file = RandomAccessFile(path.toFile(), "rw")
            file.seek(file.length() - size)
            file.read(bytes)
            file.setLength(maxOf(0, file.length() - size))
            file.close()

            return bytes
        }

        private const val mimeTypeAudio = "audio/*"
        private const val mimeTypeText = "text/*"
        private const val mimeTypeImage = "image/*"
        private const val mimeTypeVideo = "video/*"
        private const val mimeTypeApp = "application/*"

        private const val hiddenPrefix = "."

        fun getReadableFileSize(_size: Long): String {
            val dec = DecimalFormat("###.##")
            var prefix = ""
            var size = _size

            if (size < 0) {
                prefix = "-"
                size = -size
            }

            val sizes = listOf("B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")
            var len = size.toDouble()
            var order = 0

            while (len >= 1024 && order < sizes.size - 1) {
                len /= 1024.0
                order++
            }
            return prefix + dec.format(len) + sizes[order]
        }

        fun getThumbnail(
            context: Context, uri: Uri, mimeType: String, size: Size
        ): Bitmap? {
            if (!isMediaUri(uri)) {
                return null
            }
            var bm: Bitmap? = null
            val resolver = context.contentResolver
            var cursor: Cursor? = null

            try {
                cursor = resolver.query(uri, null, null, null, null)
                if (cursor!!.moveToFirst()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        bm = resolver.loadThumbnail(uri, size, null)
                    } else {
                        val id = cursor.getLong(0)

                        if (mimeType.contains("video")) {
                            bm = MediaStore.Video.Thumbnails.getThumbnail(
                                resolver, id, MediaStore.Video.Thumbnails.MINI_KIND, null
                            )
                        } else if (mimeType.contains("image")) {
                            bm = MediaStore.Images.Thumbnails.getThumbnail(
                                resolver, id, MediaStore.Images.Thumbnails.MINI_KIND, null
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                cursor?.close()
            }
            return bm
        }

        var sComparator: Comparator<File>? =
            Comparator<File> { f1, f2 -> // Sort alphabetically by lower case, which is much cleaner
                f1.name.lowercase(Locale.getDefault()).compareTo(
                    f2.name.lowercase(Locale.getDefault())
                )
            }

        var sFileFilter: FileFilter = FileFilter { file ->
            val fileName: String = file.name
            // Return files only (not directories) and skip hidden files
            file.isFile && !fileName.startsWith(hiddenPrefix)
        }

        var sDirFilter: FileFilter = FileFilter { file ->
            val fileName: String = file.name
            // Return directories only and skip hidden directories
            file.isDirectory && !fileName.startsWith(hiddenPrefix)
        }

        fun createGetContentIntent(): Intent {
            // Implicitly allow the user to select a particular kind of data
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            // The MIME data type filter
            intent.type = "*/*"
            // Only return URIs that can be opened with ContentResolver
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            return intent
        }

        fun getMimeType(file: File): String {
            val extension = MimeTypeMap.getFileExtensionFromUrl(
                file.path
            )
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                ?: "*/*"  // "application/octet-stream"
        }

        fun getExtension(strPath: String): String {
            val extension = Path(strPath).extension

            if (extension.isNullOrBlank()) return String()

            return ".$extension"
        }

        fun nextAvailableFolderNameAdvanced(path: String): String {
            val numberPattern = " (%d)"
            if (!Files.exists(Path(path))) return path

            return getNextFolderNameAdvanced(path + numberPattern)
        }

        private fun getNextFolderNameAdvanced(pattern: String): String {
            val tmp = String.format(pattern, 1)
            if (tmp == pattern) throw Exception("The pattern must include an index place-holder")

            if (!Files.exists(Path(tmp))) return tmp

            var min = 1
            var max = 2 // min is inclusive, max is exclusive/untested

            while (Files.exists(Path(String.format(pattern, max)))) {
                min = max
                max *= 2
            }

            while (max != min + 1) {
                val pivot = (max + min) / 2
                if (Files.exists(Path(String.format(pattern, pivot)))) min = pivot
                else max = pivot
            }

            return String.format(pattern, max)
        }

        //

        fun nextAvailableFileNameAdvanced(path: String): String {
            val numberPattern = " (%d)"

            if (!Files.exists(Path(path))) return path

            if (Path(path).extension != "") return getNextFileNameAdvanced(
                StringBuilder(path).insert(
                    path.lastIndexOf("." + Path(path).extension), numberPattern
                ).toString()
            )

            return getNextFileNameAdvanced(path + numberPattern)
        }

        private fun getNextFileNameAdvanced(pattern: String): String {
            val tmp = String.format(pattern, 1)
            if (tmp == pattern) throw Exception("The pattern must include an index place-holder")

            if (!Files.exists(Path(tmp))) return tmp

            var min = 1
            var max = 2 // min is inclusive, max is exclusive/untested

            while (Files.exists(Path(String.format(pattern, max)))) {
                min = max
                max *= 2
            }

            while (max != min + 1) {
                val pivot = (max + min) / 2

                if (Files.exists(Path(String.format(pattern, pivot)))) min = pivot
                else max = pivot
            }

            return String.format(pattern, max)
        }

        fun joinPath(base: String, subs: List<String>): Path {
            var basePath = Path(base)

            for (sub in subs) {
                basePath = Path(basePath.toString(), sub)
            }

            return basePath
        }

        fun getBriefOfPath(pathStr: String): String {
            val path = Path(pathStr)
            val exStoragePath = Environment.getExternalStorageDirectory().toPath()
            val relativePath = exStoragePath.relativize(path)

            val split = relativePath.toString().split(File.separator)
            if (split.size <= 2) return "~" + File.separator + relativePath.toString()

            return "~" + listOf(split[0], "...", split[2]).joinToString(File.separator)
        }

        suspend fun getMediaUriFromPath(context: Context, path: String): Uri? {
            val file = File(path)
            val mimeType = getMimeType(file)

            val deferred = CompletableDeferred<Uri?>()

            MediaScannerConnection.scanFile(
                context, arrayOf(file.absolutePath), arrayOf(mimeType)
            ) { _, uri ->
                deferred.complete(uri)
            }

            return deferred.await()
        }

        fun getFinalUri(context: Context, uri: Uri, isTree: Boolean): Uri {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            val finalUri = if (!isTree) uri
            else DocumentsContract.buildDocumentUriUsingTree(
                uri, DocumentsContract.getTreeDocumentId(uri)
            )

            return finalUri
        }

        suspend fun getDocumentUriFromPath(context: Context, path: String): Uri? {
            val file = File(path)
            val mimeType = getMimeType(file)
            val deferred = CompletableDeferred<Uri?>()
            MediaScannerConnection.scanFile(
                context, arrayOf(file.absolutePath), arrayOf(mimeType)
            ) { _, uri ->
                deferred.complete(uri)
            }

            val mediaUri = deferred.await() ?: return null
            return MediaStore.getDocumentUri(context, mediaUri)
        }

        fun getFileProviderUri(context: Context, path: String): Uri? {
            return try {
                FileProvider.getUriForFile(context, context.packageName + ".provider", File(path))
            } catch (e: Exception) {
                null
            }
        }

        fun isFile(context: Context, uri: Uri): Boolean {
            var isFile = true

            val projection = arrayOf(
                MediaStore.Files.FileColumns.MIME_TYPE, DocumentsContract.Document.COLUMN_MIME_TYPE
            )

            try {
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val mimeType =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE))
                                ?: cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE))
                        if (mimeType != null && mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                            isFile = false
                        }
                    }
                }
            } catch (e: Exception) {
                val treeUri = DocumentsContract.buildDocumentUriUsingTree(
                    uri, DocumentsContract.getTreeDocumentId(uri)
                )

                context.contentResolver.query(treeUri, projection, null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val mimeType =
                                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE))
                                    ?: cursor.getString(
                                        cursor.getColumnIndexOrThrow(
                                            DocumentsContract.Document.COLUMN_MIME_TYPE
                                        )
                                    )
                            if (mimeType != null && mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                                isFile = false
                            }
                        }
                    }
            }

            return isFile
        }

        fun getPathFromUri(context: Context, uri: Uri): String? {
            when {
                DocumentsContract.isDocumentUri(context, uri) -> {
                    when {
                        isExternalStorageDocument(uri) -> {
                            val docId = DocumentsContract.getDocumentId(uri)
                            val split = docId.split(":").toTypedArray()
                            val type = split[0]
                            return if ("primary".equals(type, ignoreCase = true)) {
                                if (split.size > 1) {
                                    Environment.getExternalStorageDirectory()
                                        .toString() + File.separator + split[1]
                                } else {
                                    Environment.getExternalStorageDirectory()
                                        .toString() + File.separator
                                }
                            } else {
                                "storage" + File.separator + docId.replace(":", File.separator)
                            }
                        }

                        isDownloadsDocument(uri) -> {
                            var id = DocumentsContract.getDocumentId(uri)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && id.startsWith("msf:")) {
                                val split: List<String> = id.split(":")
                                val selection = "_id=?"
                                val selectionArgs = arrayOf(split[1])
                                return getDataColumn(
                                    context,
                                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                    selection,
                                    selectionArgs
                                )
                            }

                            if (id.startsWith("raw:")) {
                                id = id.replaceFirst("raw:".toRegex(), "")
                                val file = File(id)
                                if (file.exists()) return id
                            }

                            val contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/my_downloads"),
                                java.lang.Long.valueOf(id)
                            )

                            return getDataColumn(context, contentUri, null, null)
                        }

                        isMediaDocument(uri) -> {
                            val docId = DocumentsContract.getDocumentId(uri)
                            val split = docId.split(":").toTypedArray()
                            val type = split[0]
                            val contentUri: Uri?
                            when (type) {
                                "image" -> {
                                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                }

                                "video" -> {
                                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                }

                                "audio" -> {
                                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                                }

                                else -> {
                                    //non-media files i.e documents and other files
                                    contentUri = MediaStore.Files.getContentUri("external")
                                    val selection = "_id=?"
                                    val selectionArgs = arrayOf(Environment.DIRECTORY_DOCUMENTS)
                                    return getMediaDocumentPath(
                                        context, contentUri, selection, selectionArgs
                                    )
                                }
                            }
                            val selection = "_id=?"
                            val selectionArgs = arrayOf(split[1])
                            return getDataColumn(context, contentUri, selection, selectionArgs)
                        }

                        isGoogleDriveUri(uri) -> {
                            return getDriveFilePath(uri, context)
                        }
                    }
                }

                isMediaUri(uri) -> {
                    val splits = uri.toString().split(File.separator)

                    val docId = splits[splits.size - 1]
                    val type = splits[splits.size - 3]
                    val contentUri: Uri?
                    when (type) {
                        "image" -> {
                            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        }

                        "video" -> {
                            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        }

                        "audio" -> {
                            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        }

                        else -> {
                            contentUri = MediaStore.Files.getContentUri(splits[splits.size - 4])
                            val selection = "_id=?"
                            val selectionArgs = arrayOf(Environment.DIRECTORY_DOCUMENTS)
                            return getMediaDocumentPath(
                                context, contentUri, selection, selectionArgs
                            )
                        }
                    }
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(docId)
                    return getDataColumn(context, contentUri, selection, selectionArgs)
                }

                "content".equals(uri.scheme, ignoreCase = true) -> {
                    return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(
                        context, uri, null, null
                    )
                }

                "file".equals(uri.scheme, ignoreCase = true) -> {
                    return uri.path
                }
            }
            return null
        }

        private fun getDataColumn(
            context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?
        ): String? {
            var cursor: Cursor? = null
            val column = "_data"
            val projection = arrayOf(column)
            try {
                if (uri == null) return null
                cursor =
                    context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(index)
                }
            } finally {
                cursor?.close()
            }
            return null
        }

        private fun getMediaDocumentPath(
            context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?
        ): String? {
            var cursor: Cursor? = null
            val column = MediaStore.Files.FileColumns.DATA
            val projection = arrayOf(column)
            try {
                if (uri == null) return null
                cursor =
                    context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                val f = cursor?.moveToFirst()
                if (cursor != null && f == true) {
                    val index = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(index)
                }
            } finally {
                cursor?.close()
            }
            return null
        }

        private fun getFilePath(context: Context, uri: Uri?): String? {
            var cursor: Cursor? = null
            val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
            try {
                if (uri == null) return null
                cursor = context.contentResolver.query(
                    uri, projection, null, null, null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    return cursor.getString(index)
                }
            } finally {
                cursor?.close()
            }
            return null
        }

        private fun getDriveFilePath(uri: Uri, context: Context): String? {
            val returnCursor = context.contentResolver.query(uri, null, null, null, null)
            val nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE)
            returnCursor.moveToFirst()
            val name = returnCursor.getString(nameIndex)
            returnCursor.getLong(sizeIndex).toString()
            val file = File(context.cacheDir, name)
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val outputStream = FileOutputStream(file)
                var read: Int
                val maxBufferSize = 1 * 1024 * 1024
                val bytesAvailable = inputStream!!.available()

                val bufferSize = bytesAvailable.coerceAtMost(maxBufferSize)
                val buffers = ByteArray(bufferSize)
                while (inputStream.read(buffers).also { read = it } != -1) {
                    outputStream.write(buffers, 0, read)
                }
                inputStream.close()
                outputStream.close()
                returnCursor.close()
            } catch (e: Exception) {
                println(e.message)
            }
            return file.path
        }

        fun isMediaUri(uri: Uri?): Boolean {
            return "media".equals(uri!!.authority, ignoreCase = true)
        }

        private fun isExternalStorageDocument(uri: Uri): Boolean {
            return "com.android.externalstorage.documents" == uri.authority
        }

        private fun isDownloadsDocument(uri: Uri): Boolean {
            return "com.android.providers.downloads.documents" == uri.authority
        }

        private fun isMediaDocument(uri: Uri): Boolean {
            return "com.android.providers.media.documents" == uri.authority
        }

        private fun isGooglePhotosUri(uri: Uri): Boolean {
            return "com.google.android.apps.photos.content" == uri.authority
        }

        private fun isGoogleDriveUri(uri: Uri): Boolean {
            return "com.google.android.apps.docs.storage" == uri.authority || "com.google.android.apps.docs.storage.legacy" == uri.authority
        }

        fun deleteRecursively(pathStr: String): Boolean {
            val path = Path(pathStr)

            if (!Files.exists(path)) return true

            return try {
                Files.walk(path).use { walk ->
                    walk.sorted(Comparator.reverseOrder()).map { it.toFile() }
                        .forEach { it.delete() }
                }
                true
            } catch (e: Exception) {
                println(e.message)
                false
            }
        }
    }
}
