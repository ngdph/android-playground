package co.iostream.apps.android.io_private.models

import co.iostream.apps.android.core.iofile.FileSystemItem
import co.iostream.apps.android.core.iofile.FileUtils
import co.iostream.apps.android.data.entities.FileEntity
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString

@Serializable
class CypherFileItem() : FileSystemItem() {
    enum class FileInfoType {
        Input, Encrypted, Original
    }

    enum class FileItemType {
        Normal, EncodedFile, EncodedFolder
    }

    enum class FeatureType {
        Encode, Decode
    }

    private var _originalInfo: String = String()
    var originalInfo: Path?
        get() = if (_originalInfo.isNotEmpty()) Path(_originalInfo) else null
        set(value) {
            _originalInfo = value?.pathString ?: String()
        }

    private var _encryptedInfo: String = String()
    var encryptedInfo: Path?
        get() = if (_encryptedInfo.isNotEmpty()) Path(_encryptedInfo) else null
        set(value) {
            _encryptedInfo = value?.pathString ?: String()
        }

    private var _exportedInfo: String = String()
    var exportedInfo: Path?
        get() = if (_exportedInfo.isNotEmpty()) Path(_exportedInfo) else null
        set(value) {
            _exportedInfo = value?.pathString ?: String()
        }

    var recoveredFileOrFolderPath = String()

    var footer: FileFooter? = null
        get
        protected set

    var itemType: FileItemType = FileItemType.Normal
        get
        protected set

    var isCorrupted = false
        get
        protected set

    var originalExtension: String = String()
        get
        protected set

    var isAvailable: Boolean = true

    protected var _thumbnailPath: String = String()
    var thumbnailPath: String
        get() = _thumbnailPath
        set(value) {
            _thumbnailPath = value
        }

    var cacheImagePath: String = String()

    constructor(path: String) : this() {
        _inputInfo = path
    }

    constructor(entity: FileEntity) : this() {
        _inputInfo = entity.path
        _encryptedInfo = entity.path

        _fileType = FileUtils.Type.fromInt(entity.fileType)
    }

    companion object {
        fun create(inputPath: String): CypherFileItem {
            val item = CypherFileItem(inputPath)

            if (item.tryLoadFooter(inputPath, false)) {
                item.encryptedInfo = Path(inputPath)
                item.originalInfo = Path(item.footer!!.metadata!!.OriginalName)
            } else {
                item.encryptedInfo = null
                item.originalInfo = Path(inputPath)
            }

            return item
        }

        fun create(entity: FileEntity): CypherFileItem {
            val item = CypherFileItem(entity)

            if (item.tryLoadFooter(FileInfoType.Encrypted, false)) item.originalInfo =
                Path(item.footer!!.metadata!!.OriginalName)

            item.updateCorruptedStatus()

            return item
        }
    }

    fun updateAndGetIsAvailable(available: Boolean): Boolean {
        isAvailable = available
        return isAvailable
    }

    fun tryLoadFooter(path: String, reload: Boolean): Boolean {
        if (footer != null && !reload) return true

        val footer = FileFooter.create(path)
        if (footer != null) {
            this.footer = footer
            _fileType = footer.metadata!!.FileType
        }

        return this.footer != null
    }

    fun tryLoadFooter(fileInfoType: FileInfoType, reload: Boolean): Boolean {
        if (footer != null && !reload) return true

        val fileInfo = getFileInfoByType(fileInfoType) ?: return false

        val loadedFooter = FileFooter.create(fileInfo.pathString)
        if (loadedFooter != null) {
            footer = loadedFooter
            originalInfo = Path(loadedFooter.metadata!!.OriginalName)
            _fileType = loadedFooter.metadata!!.FileType
        }

        return footer != null
    }

    fun getFileInfoByType(fileInfoType: FileInfoType): Path? {
        return when (fileInfoType) {
            FileInfoType.Input -> inputInfo
            FileInfoType.Original -> originalInfo
            FileInfoType.Encrypted -> encryptedInfo
            else -> null
        }
    }

    fun setCorruptedStatus(corrupted: Boolean) {
        isCorrupted = corrupted
    }

    fun updateCorruptedStatus() {
        isCorrupted = footer == null
    }

    fun moveOutputPathToInputPath() {
        inputInfo = outputInfo?.let { Path(it.pathString) }
        loadFileTypeAndOriginalExtension()
    }

    fun loadFileTypeAndOriginalExtension() {
        _fileType = FileUtils.getTypeByExtension(inputInfo!!.extension)
        itemType = FileItemType.Normal
        originalExtension = String()

        if (inputInfo!!.isRegularFile()) {
            val footer = FileFooter.create(inputInfo!!.pathString)
            if (footer?.metadata != null) {
                _fileType = FileUtils.getTypeByExtension(footer.metadata!!.OriginalName)

                if (footer.metadata!!.Version >= 2) {
                    itemType =
                        if (footer.metadata!!.FileType != FileUtils.Type.Directory) FileItemType.EncodedFile
                        else FileItemType.EncodedFolder
                } else {
                    itemType = if (footer.metadata!!.IsFile) FileItemType.EncodedFile
                    else FileItemType.EncodedFolder
                }

                originalExtension = Path(footer.metadata!!.OriginalName).extension
            } else {
                originalExtension = inputInfo!!.extension
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun clone(): CypherFileItem {
        val binaryData = ProtoBuf.encodeToByteArray(serializer(), this)
        return ProtoBuf.decodeFromByteArray(serializer(), binaryData)
    }
}
