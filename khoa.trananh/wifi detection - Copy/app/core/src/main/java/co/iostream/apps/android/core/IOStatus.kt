package co.iostream.apps.android.core

import kotlinx.serialization.Serializable
import co.iostream.apps.android.core.ioutils.ZUtils

enum class StatusType {
    Init, Ready,  //

    Loading, LoadingOne, LoadingAll,

    Loaded, LoadFailed, LoadCanceled,

    LoadPausing, LoadPaused,

    LoadStopping, LoadStopped,  //

    ProcessInQueue, ProcessStart,

    Processing, ProcessingOne, ProcessingAll,

    Processed, ProcessFailed, ProcessCanceled,

    ProcessPausing, ProcessPaused,

    ProcessStopping, ProcessStopped,

    Extra0, Extra1, Extra2, Extra3
}

@Serializable
class IOStatus<T : Enum<T>>() : IOItem() {
    companion object {
        val SL = mapOf(
            StatusType.Init to "",
            StatusType.Ready to "",

            StatusType.Loading to "",
            StatusType.LoadingOne to "",
            StatusType.LoadingAll to "",

            StatusType.Loaded to "",
            StatusType.LoadFailed to "",
            StatusType.LoadCanceled to "",

            StatusType.LoadPausing to "",
            StatusType.LoadPaused to "",

            StatusType.LoadStopping to "",
            StatusType.LoadStopped to "",

            StatusType.ProcessInQueue to "",
            StatusType.ProcessStart to "",

            StatusType.Processing to "",
            StatusType.ProcessingOne to "",
            StatusType.ProcessingAll to "",

            StatusType.Processed to "",
            StatusType.ProcessFailed to "",
            StatusType.ProcessCanceled to "",

            StatusType.ProcessPausing to "",
            StatusType.ProcessPaused to "",

            StatusType.ProcessStopping to "",
            StatusType.ProcessStopped to "",

            StatusType.Extra0 to "",
            StatusType.Extra1 to "",
            StatusType.Extra2 to "",
            StatusType.Extra3 to "",
        )

        inline fun <reified E : Enum<E>> initialize(noinline action: () -> Unit): IOStatus<E> {
            val status = IOStatus<E>()
            val genericTypes = E::class.java.enumConstants?.map { it.name }

            var statuses = listOf<StatusType>()
            if (genericTypes != null) {
                for (genericType in genericTypes) {
                    val statusType = StatusType.entries.find { it.name == genericType }
                    if (statusType != null) {
                        statuses = statuses.plus(statusType)
                    } else {
                        throw Exception(
                            "Out of range: $genericType [${
                                StatusType.entries.joinToString(", ")
                            }]."
                        )
                    }
                }
            }

            status._STATUSES = statuses.toTypedArray()
            status._statusType = status._STATUSES[0]
            status._prevStatusType = status._STATUSES[0]

            return status
        }
    }

    var _STATUSES: Array<StatusType> = emptyArray()
    var _statusType: StatusType? = null
    var _prevStatusType: StatusType? = null

    val isFirstSet: Boolean = _statusType == _prevStatusType

    fun set(genericType: T) {
        _prevStatusType = _statusType
        _statusType = ZUtils.changeEnumType<StatusType>(genericType.name)
    }

    fun any(vararg genericTypes: T): Boolean {
        return genericTypes.map { it.toString() }.contains(_statusType.toString())
    }

    fun not(vararg genericTypes: T): Boolean {
        return !any(*genericTypes)
    }

    fun prevAny(vararg genericTypes: T): Boolean {
        return genericTypes.map { it.toString() }.contains(_prevStatusType.toString())
    }

    fun prevNot(vararg genericTypes: T): Boolean {
        return !prevAny(*genericTypes)
    }
}