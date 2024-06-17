package co.iostream.apps.android.core.iofile

import kotlinx.serialization.Serializable
import co.iostream.apps.android.core.IOItem
import co.iostream.apps.android.core.IOStatus
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString

@Serializable
open class FileSystemItem() : IOItem() {
    enum class S {
        Ready,

        ProcessInQueue, Processing,

        Processed, ProcessFailed,

        ProcessPaused, ProcessStopped,
    }

    protected var _inputInfo: String = String()
    var inputInfo: Path?
        get() = if (_inputInfo.isNotEmpty()) Path(_inputInfo) else null
        set(value) {
            _inputInfo = value?.pathString ?: String()
        }

    protected var _outputInfo: String = String()
    var outputInfo: Path?
        get() = if (_outputInfo.isNotEmpty()) Path(_outputInfo) else null
        set(value) {
            _outputInfo = value?.pathString ?: String()
        }

    var status: IOStatus<S> = IOStatus.initialize<S> {  }

    protected var _fileType: FileUtils.Type = FileUtils.Type.Unknown
    var fileType: FileUtils.Type
        get() = _fileType
        set(value) {
            _fileType = value
        }

    protected var _messageText: String = String()
    var MessageText: String
        get() = _messageText
        set(value) {
            _messageText = value
        }

    fun setFileType(fileType: FileUtils.Type, notify: Boolean) {
        _fileType = fileType
    }

    fun setMessage(error: String) {
        _messageText = error
    }

    constructor(path: String) : this() {
        _inputInfo = path
        _fileType = FileUtils.getType(path)
    }
}