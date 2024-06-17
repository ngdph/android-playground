package co.iostream.apps.android.io_private.models

import co.iostream.apps.android.core.IOSize
import co.iostream.apps.android.core.iofile.FileUtils
import co.iostream.apps.android.data.configs.IO_APP_ID
import co.iostream.apps.android.io_private.utils.BitUtils
import com.google.gson.Gson
import kotlinx.serialization.Serializable
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString
import kotlin.math.max


@Serializable
class FooterExtra {
    var Dimension: IOSize<Int>? = null

    var Size: Long = 0
    var CreationTime: Long = 0
    var LastWriteTime: Long = 0
    var IsThumbnailEncrypted: Boolean = false

    fun toJSON(): String = Gson().toJson(this)

    companion object {
        fun isEmpty(extra: FooterExtra): Boolean = extra.Dimension == null && extra.Size == 0L
    }
}

@Serializable
class FooterMetadata {
    var Version: Int = 1
    var Platform: String = "Android"
    var IsFile: Boolean = false
    var FileType: FileUtils.Type = FileUtils.Type.Unknown
    var OriginalName: String = String()
    var ThumbnailSize: Int = 0
    var Extras: String = String()

    constructor(fileType: FileUtils.Type, originalName: String, extra: FooterExtra) {
        IsFile = fileType != FileUtils.Type.Directory
        FileType = fileType
        OriginalName = originalName
        Extras = extra.toJSON()
    }
}

class MetadataProxy {
    var Version: Int = 1
    var Platform: String = "Android"
    var IsFile: Boolean = false
    var FileType: Int = 0
    var OriginalName: String = String()
    var ThumbnailSize: Int = 0
    var Extras: String = String()
}

@Serializable
class FileFooter {
    companion object {
        const val SIGNATURE = IO_APP_ID

        fun create(): FileFooter = FileFooter()

        fun create(byteArray: ByteArray): FileFooter? {
            val footer = FileFooter()
            if (footer.load(byteArray)) return footer
            return null
        }

        fun create(path: String): FileFooter? {
            val footer = FileFooter()
            if (footer.load(path)) return footer
            return null
        }

        fun isLockedFile(byteArray: ByteArray): Boolean = create(byteArray) != null

        fun isLockedFile(path: String): Boolean = create(path) != null

        fun appendToFile(filePath: Path, metadata: FooterMetadata, thumbnailBytes: ByteArray) {
            metadata.ThumbnailSize = thumbnailBytes.size

            val fos = FileOutputStream(filePath.toFile(), true)
            val footerMetadataBuffer = Gson().toJson(metadata).toByteArray(Charsets.UTF_8)

            if (thumbnailBytes.isNotEmpty()) fos.write(thumbnailBytes)

            try {
                val footerMetadataSize = ByteArray(4)
                BitUtils.write4BytesToBuffer(footerMetadataSize, 0, footerMetadataBuffer.size)

                fos.write(footerMetadataBuffer)
                fos.write(footerMetadataSize)
                fos.write(IO_APP_ID.toByteArray(Charsets.UTF_8))

                fos.flush()
            } catch (e: Exception) {
                throw e
            } finally {
                fos.close()
            }
        }
    }

    var signature = SIGNATURE.toByteArray(Charsets.UTF_8)
    var metaSize: Int = 0
    var metadata: FooterMetadata? = null
        get
        private set
    private var extra: FooterExtra? = null
        get
        private set

    private var metaSizeBuffer: ByteArray = byteArrayOf()
    private var footerMetadataBuffer: ByteArray = byteArrayOf()
    var footerThumbnailBuffer: ByteArray = byteArrayOf()

    private fun loadSignature(raf: RandomAccessFile): Boolean {
        try {
            raf.seek(0L.coerceAtLeast(raf.length() - signature.size))
            raf.read(signature, 0, signature.size)

            return signature.isNotEmpty() && signature.toString(Charsets.UTF_8) == SIGNATURE
        } catch (e: Exception) {
            return false
        }
    }

    private fun loadSignature(byteArray: ByteArray): Boolean {
        try {
            signature = ByteArray(SIGNATURE.length)
            byteArray.copyInto(signature, 0, byteArray.size - signature.size, byteArray.size)

            return signature.isNotEmpty() && signature.toString(Charsets.UTF_8) == SIGNATURE
        } catch (e: Exception) {
            return false
        }
    }

    private fun loadMetaSize(raf: RandomAccessFile): Boolean {
        try {
            metaSizeBuffer = ByteArray(Int.SIZE_BYTES)

            raf.seek(max(0L, raf.length() - signature.size - metaSizeBuffer.size))
            val byteRead = raf.read(metaSizeBuffer, 0, metaSizeBuffer.size)

            if (byteRead != metaSizeBuffer.size) return false

            val byteBuffer = ByteBuffer.wrap(metaSizeBuffer)
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
            metaSize = byteBuffer.int

            return metaSize > 0
        } catch (e: Exception) {
            return false
        }
    }

    private fun loadMetaSize(byteArray: ByteArray): Boolean {
        try {
            metaSizeBuffer = ByteArray(Int.SIZE_BYTES)

            byteArray.copyInto(
                metaSizeBuffer,
                0,
                byteArray.size - signature.size - metaSizeBuffer.size,
                byteArray.size - signature.size
            )

            val byteBuffer = ByteBuffer.wrap(metaSizeBuffer)
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
            metaSize = byteBuffer.int

            return metaSize > 0
        } catch (e: Exception) {
            return false
        }
    }

    private fun loadMetadata(raf: RandomAccessFile): Boolean {
        try {
            footerMetadataBuffer = ByteArray(metaSize)

            raf.seek(
                max(
                    0L,
                    raf.length() - signature.size - metaSizeBuffer.size - footerMetadataBuffer.size
                )
            )
            val byteRead = raf.read(footerMetadataBuffer, 0, footerMetadataBuffer.size)
            if (byteRead != footerMetadataBuffer.size) return false

            val footerMetadataStr = footerMetadataBuffer.toString(Charsets.UTF_8)

            metadata = Gson().fromJson(footerMetadataStr, FooterMetadata::class.java)

            return metadata != null
        } catch (e: Exception) {
            return false
        }
    }

    private fun loadMetadata(byteArray: ByteArray): Boolean {
        try {
            footerMetadataBuffer = ByteArray(metaSize)

            byteArray.copyInto(
                footerMetadataBuffer,
                0,
                byteArray.size - signature.size - metaSizeBuffer.size - footerMetadataBuffer.size,
                byteArray.size - signature.size - metaSizeBuffer.size
            )

            val footerMetadataStr = footerMetadataBuffer.toString(Charsets.UTF_8)

            val metadataProxy = Gson().fromJson(footerMetadataStr, MetadataProxy::class.java)
            metadata = Gson().fromJson(footerMetadataStr, FooterMetadata::class.java)

            if (metadata != null && metadataProxy != null) {
                metadata!!.FileType = FileUtils.Type.fromInt(metadataProxy.FileType)
                metadata!!.IsFile = metadata!!.FileType != FileUtils.Type.Directory
            }

            return metadata != null
        } catch (e: Exception) {
            return false
        }
    }

    private fun loadThumbnail(raf: RandomAccessFile): Boolean {
        if (metadata == null) return false

        if (metadata!!.ThumbnailSize == 0) return true
        footerThumbnailBuffer = ByteArray(metadata!!.ThumbnailSize)

        try {
            raf.seek(raf.length() - signature.size - metaSizeBuffer.size - footerMetadataBuffer.size - footerThumbnailBuffer.size)

            val byteRead = raf.read(footerThumbnailBuffer, 0, footerThumbnailBuffer.size)
            return byteRead == footerThumbnailBuffer.size
        } catch (e: Exception) {
            return false
        }
    }

    private fun loadThumbnail(byteArray: ByteArray): Boolean {
        if (metadata == null) return false

        if (metadata!!.ThumbnailSize == 0) return true
        footerThumbnailBuffer = ByteArray(metadata!!.ThumbnailSize)

        try {
            byteArray.copyInto(
                footerThumbnailBuffer,
                0,
                byteArray.size - signature.size - metaSizeBuffer.size - footerMetadataBuffer.size - footerThumbnailBuffer.size,
                byteArray.size - signature.size - metaSizeBuffer.size - footerMetadataBuffer.size
            )

            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun load(filePath: String): Boolean {
        val path = Path(filePath)

        if (!path.exists() || path.isDirectory()) return false

        val raf = RandomAccessFile(path.pathString, "r")

        if (!loadSignature(raf)) return false
        if (!loadMetaSize(raf)) return false
        if (!loadMetadata(raf)) return false
        if (!loadThumbnail(raf)) return false

        return true
    }

    fun load(byteArray: ByteArray): Boolean {
        if (byteArray.isEmpty()) return false

        if (!loadSignature(byteArray)) return false
        if (!loadMetaSize(byteArray)) return false
        if (!loadMetadata(byteArray)) return false
        if (!loadThumbnail(byteArray)) return false

        return true
    }

    fun getSize(): Long =
        (signature.size + metaSizeBuffer.size + footerMetadataBuffer.size + footerThumbnailBuffer.size).toLong()
}