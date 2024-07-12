package com.example.aes

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
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import kotlinx.coroutines.CompletableDeferred
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.HashMap
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

        fun joinPath(base: String, subs: List<String>): Path {
            var basePath = Path(base)

            for (sub in subs) {
                basePath = Path(basePath.toString(), sub)
            }

            return basePath
        }

        @RequiresApi(Build.VERSION_CODES.O)
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

        @RequiresApi(Build.VERSION_CODES.O)
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
                                "image", "images" -> {
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

        @RequiresApi(Build.VERSION_CODES.O)
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
