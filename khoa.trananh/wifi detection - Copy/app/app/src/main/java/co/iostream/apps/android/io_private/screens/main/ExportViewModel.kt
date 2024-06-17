package co.iostream.apps.android.io_private.screens.main

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.iostream.apps.android.core.iofile.FileSystemItem
import co.iostream.apps.android.core.iofile.FileUtils
import co.iostream.apps.android.data.repositories.FileRepository
import co.iostream.apps.android.io_private.R
import co.iostream.apps.android.io_private.configs.AppTypes
import co.iostream.apps.android.io_private.dataStore
import co.iostream.apps.android.io_private.features.Share
import co.iostream.apps.android.io_private.models.CypherFileItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okio.withLock
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.pathString

@HiltViewModel
class ExportViewModel @Inject constructor(private val fileRepository: FileRepository) :
    ViewModel() {
    enum class StatusType {
        Init, Loading, Loaded, LoadFailed, Processing, Processed, Failed, Pausing, Paused, Stopping, Stopped
    }

    enum class UpdateItemsFlag {
        Add, Update, Delete
    }

    enum class RemoveMode {
        All, Selected, NonExisted
    }

    private var _argv: Share.Argv? = null
    private var _fileManagerViewModel: FileManagerViewModel? = null

    private val _items = mutableStateListOf<CypherFileItem>()
    val items = MutableStateFlow(_items)

    private var _featureType = MutableStateFlow(CypherFileItem.FeatureType.Encode)
    val featureType = _featureType.asStateFlow()

    private var _status = MutableStateFlow(StatusType.Init)
    val status = _status.asStateFlow()

    private var _isSelecting = MutableStateFlow(false)
    val isSelecting = _isSelecting.asStateFlow()

    private var _exportButtonEnabled = MutableStateFlow(true)
    val exportButtonEnabled = _exportButtonEnabled.asStateFlow()

    private var _keepOriginal = MutableStateFlow(false)
    val keepOriginal = _keepOriginal.asStateFlow()

    fun setKeepOriginal(value: Boolean) {
        _keepOriginal.value = value
    }

    private var _overwriteExisting = MutableStateFlow(false)
    val overwriteExisting = _overwriteExisting.asStateFlow()

    fun setOverwriteExisting(value: Boolean) {
        _overwriteExisting.value = value
    }

    private var _exportType = MutableStateFlow(AppTypes.ExportType.Export)
    val exportType = _exportType.asStateFlow()

    fun setExportType(value: AppTypes.ExportType) {
        _exportType.value = value
    }

    private var _selectMode = MutableStateFlow(false)
    val selectMode = _selectMode.asStateFlow()

    fun refreshGlobalVariables(mainViewModel: FileManagerViewModel) {
        _fileManagerViewModel = mainViewModel
    }

//    fun applyFilter(userString:String) {
//        _items.clear()
//
//        if (userString.isEmpty()) {
//            _items.addAll(_sourceItems)
//            return
//        }
//
//        for (item in _sourceItems) {
//            if(item.inputInfo.name.contains(userString, ignoreCase = true)) {
//                val newItem : FileItem = item
//                _items.add(newItem)
//            }
//        }
//    }

    private fun enableInputControls(value: Boolean) {
        _exportButtonEnabled.value = value
    }

    private fun enableOutputControl(value: Boolean) {
    }

    fun setSelectMode(value: Boolean) {
        _selectMode.value = value
    }

    private fun updateStatus(value: StatusType) {
        val prevStatus = _status.value
        _status.value = value

        when (value) {
            StatusType.Init -> {
                enableInputControls(true)
                enableOutputControl(false)
            }

            StatusType.Loading -> {
                enableInputControls(false)
                enableOutputControl(false)
            }

            StatusType.Loaded -> {
                enableInputControls(true)
                enableOutputControl(_items.size > 0)
            }

            StatusType.LoadFailed -> {
                enableInputControls(true)
                enableOutputControl(_items.size > 0)
            }

            StatusType.Processing -> {
                enableInputControls(false)
                enableOutputControl(false)

                for (index in 0 until _items.size) {
                    val updatedItem = _items[index]
                    updatedItem.isEnabled = false
                    updateOne(updatedItem)
                }
            }

            StatusType.Processed -> {
                _items.removeIf { it.status.any(FileSystemItem.S.Processed) }
//                _fileManagerViewModel?.loadFiles(_items.toList())

                enableInputControls(true)
                enableOutputControl(_items.size > 0)

                for (index in 0 until _items.size) {
                    val updatedItem = _items[index]
                    updatedItem.isEnabled = true
                    updateOne(updatedItem)
                }
            }

            StatusType.Pausing -> {
                enableInputControls(false)
                enableOutputControl(false)
            }

            StatusType.Paused -> {
                enableInputControls(false)
                enableOutputControl(_items.size > 0)
            }

            StatusType.Stopping -> {
                if (prevStatus == StatusType.Paused) {
                    enableInputControls(true)
                    enableOutputControl(_items.size > 0)
                } else {
                    enableInputControls(false)
                    enableOutputControl(false)
                }
            }

            StatusType.Stopped -> {
                _items.removeIf { it.status.any(FileSystemItem.S.Processed) }
//                _fileManagerViewModel?.loadFiles(_items.toList())

                enableInputControls(true)
                enableOutputControl(_items.size > 0)

                for (index in 0 until _items.size) {
                    val updatedItem = _items[index]
                    updatedItem.isEnabled = true
                    updateOne(updatedItem)
                }
            }

            StatusType.Failed -> {
                enableInputControls(true)
                enableOutputControl(_items.size > 0)

                for (index in 0 until _items.size) {
                    val updatedItem = _items[index]
                    updatedItem.isEnabled = true
                    updateOne(updatedItem)
                }
            }
        }
    }

    suspend fun pressProcessAll(context: Context) {
        val passwordValueFlow = context.dataStore.data.map { preferences ->
            val passwordValueKey = stringPreferencesKey("passwordValue")
            preferences[passwordValueKey]
        }

        passwordValueFlow.collect {
            it?.let {
                val argv =
                    Share.Argv(it, _keepOriginal.value, _overwriteExisting.value, _exportType.value)
                processAll(context, argv)
            }
        }
    }

    fun <T> any(value: T, vararg values: StatusType): Boolean {
        return values.any { it == value }
    }

    fun loadFiles(addedItems: List<CypherFileItem>) {
        updateStatus(StatusType.Loading)

        _items.clear()
        _items.addAll(addedItems)

        updateStatus(StatusType.Loaded)
    }

    fun updateOne(item: CypherFileItem) {
        val clonedItem = item.clone()

        val itemIndex =
            _items.indexOfFirst { it.inputInfo!!.pathString == item.inputInfo!!.pathString }
        if (itemIndex > -1) _items[itemIndex] = clonedItem

        val availableIndex =
            _items.indexOfFirst { it.inputInfo!!.pathString == item.inputInfo!!.pathString }
        if (availableIndex > -1) _items[availableIndex] = clonedItem
    }

    fun removeMany(mode: RemoveMode) {
        when (mode) {
            RemoveMode.All -> {
                _items.removeIf { true }
                _items.removeIf { true }
            }

            RemoveMode.Selected -> {
                _items.removeIf { it.isSelected }
                _items.removeIf { it.isSelected }
            }

            RemoveMode.NonExisted -> {
                _items.removeIf { !it.inputInfo!!.exists() }
                _items.removeIf { !it.inputInfo!!.exists() }
            }
        }
    }

    fun removeSelected() {
        _items.removeIf { it.isSelected }
    }

    suspend fun deleteSelectedPermanently() {
        val selectedItems = _items.filter { it.isSelected }

        for (item in selectedItems) {
            deletePermanently(item)
        }
    }

    fun selectItem(item: CypherFileItem) {
        if (_selectMode.value) {
            item.isSelected = !item.isSelected
            updateOne(item)

            if (!item.isSelected) {
                val remainingSelectedItems = items.value.filter { it.isSelected }
                if (remainingSelectedItems.isEmpty()) {
                    setSelectMode(false)
                }
            }
        } else {
            setSelectMode(true)
            item.isSelected = true
            updateOne(item)
        }
    }

    fun removeOne(item: CypherFileItem) {
        viewModelScope.launch(Dispatchers.IO) {
            _items.remove(item)
            fileRepository.deleteByPath(item.inputInfo!!.pathString)

            _fileManagerViewModel?.removeOne(item)
        }
    }

    fun deletePermanently(item: CypherFileItem): Boolean {
        val itemPath = Path(item.inputInfo!!.pathString)

        if (!Files.exists(itemPath)) return true

        var result = false

        viewModelScope.launch(Dispatchers.IO) {
            if (Files.isDirectory(itemPath)) {
                result = FileUtils.deleteRecursively(itemPath.toString())
            } else if (Files.isRegularFile(itemPath)) {
                result = Files.deleteIfExists(itemPath)
            }
        }

        if (result) _items.remove(item)

        return result
    }

    private suspend fun processAll(context: Context, argv: Share.Argv) {
        if (any(
                _status.value, StatusType.Loading, StatusType.Processing
            ) || _items.size == 0
        ) return

        updateStatus(StatusType.Processing)
        enableInputControls(false)

        val locker = ReentrantLock()

        for (i in _items) {
            i.updateAndGetIsAvailable(false)
            i.status.set(FileSystemItem.S.ProcessInQueue)
        }

        fun endCallback(endStatus: StatusType) {
            viewModelScope.launch(Dispatchers.Main) {
                _status.value = endStatus
                enableInputControls(true)
            }
        }

        fun itemCallback(item: CypherFileItem, itemStatus: FileSystemItem.S, message: String) {
            viewModelScope.launch(Dispatchers.Main) {
                locker.withLock {
                    item.setMessage(message)
                    item.status.set(itemStatus)

                    if (item.status.any(FileSystemItem.S.Processed)) {
                        item.setMessage(context.getString(R.string.process_done))
                        item.updateAndGetIsAvailable(true)

                        if (argv.exportType == AppTypes.ExportType.Export) {
                            _fileManagerViewModel?.let { fileManagerViewModel ->
                                val fileManagerItem =
                                    fileManagerViewModel.items.value.first { it.inputInfo!!.pathString == item.inputInfo!!.pathString }
                                fileManagerViewModel.removeOne(fileManagerItem)
                            }
                        }
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                for (index in 0 until _items.size) {
                    val item = _items[index]

                    if (item.status.not(FileSystemItem.S.ProcessInQueue)) continue

                    itemCallback(item, FileSystemItem.S.Processing, String())

                    var error = String()

                    if (argv.password.isEmpty()) error =
                        context.getString(R.string.no_password_provided)

                    if (error.isNotEmpty()) {
                        itemCallback(item, FileSystemItem.S.ProcessFailed, error)
                    } else {
                        try {
                            var outputFileOrFolderPath = Path(item.footer!!.metadata!!.OriginalName)

                            if (!argv.overwriteExisting) {
                                outputFileOrFolderPath =
                                    if (item.fileType == FileUtils.Type.Directory) {
                                        Path(
                                            FileUtils.nextAvailableFolderNameAdvanced(
                                                outputFileOrFolderPath.toString()
                                            )
                                        )
                                    } else {
                                        Path(
                                            FileUtils.nextAvailableFileNameAdvanced(
                                                outputFileOrFolderPath.toString()
                                            )
                                        )
                                    }
                            }

                            if (argv.exportType == AppTypes.ExportType.ExportLockedCopy) {
                                Files.copy(item.inputInfo!!, outputFileOrFolderPath)
                            } else {
                                Share.decodeOne(item, argv)
                                Files.move(
                                    Path(item.recoveredFileOrFolderPath),
                                    outputFileOrFolderPath,
                                    StandardCopyOption.ATOMIC_MOVE
                                )

                                if (argv.exportType == AppTypes.ExportType.Export) {
                                    fileRepository.deleteByPath(item.inputInfo!!.pathString)
                                    FileUtils.deleteFileOrDirectory(item.inputInfo!!)
                                }
                            }

                            item.exportedInfo = outputFileOrFolderPath

                            itemCallback(item, FileSystemItem.S.Processed, String())
                        } catch (e: IOException) {
                            itemCallback(
                                item,
                                FileSystemItem.S.ProcessFailed,
                                "File or folder does not exist."
                            )
                        } catch (e: Exception) {
                            itemCallback(
                                item, FileSystemItem.S.ProcessFailed, "Password incorrect."
                            )
                        }
                    }
                }

                endCallback(StatusType.Processed)
            } catch (e: Exception) {
                endCallback(StatusType.Failed)
            }
        }
    }

    fun reset() {
        _argv = null
        _items.clear()
        _items.clear()
        _featureType.value = CypherFileItem.FeatureType.Encode
        _status.value = StatusType.Init
    }

    init {
        _status.value = StatusType.Init
        enableInputControls(true)
        enableOutputControl(false)
    }
}