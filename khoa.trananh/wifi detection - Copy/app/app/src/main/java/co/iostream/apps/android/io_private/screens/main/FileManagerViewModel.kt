package co.iostream.apps.android.io_private.screens.main

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.iostream.apps.android.core.iofile.FileSystemItem
import co.iostream.apps.android.core.iofile.FileUtils
import co.iostream.apps.android.data.entities.FileEntity
import co.iostream.apps.android.data.repositories.FileRepository
import co.iostream.apps.android.io_private.configs.AppTypes
import co.iostream.apps.android.io_private.configs.Constants
import co.iostream.apps.android.io_private.features.Share
import co.iostream.apps.android.io_private.models.CypherFileItem
import co.iostream.apps.android.io_private.models.FileFooter
import co.iostream.apps.android.io_private.utils.AppDir
import co.iostream.apps.android.io_private.utils.CryptographyException
import co.iostream.apps.android.io_private.utils.CryptographyUtils
import co.iostream.apps.android.io_private.utils.ZipUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.withLock
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.pathString

@HiltViewModel
class FileManagerViewModel @Inject constructor(private val fileRepository: FileRepository) :
    ViewModel() {
    enum class StatusType {
        Init, Loading, Loaded, LoadFailed, Processing, Processed, ProcessFailed
    }

    enum class RemoveMode {
        All, Selected, NonExisted
    }

    private var _status = MutableStateFlow(StatusType.Init)
    val status = _status.asStateFlow()

    private val itemsLocker = ReentrantLock()

    private val _sourceItems = mutableStateListOf<CypherFileItem>()

    private val _items = mutableStateListOf<CypherFileItem>()
    val items = MutableStateFlow(_items)

    private var _searchKeyword = MutableStateFlow(String())
    val searchKeyword = _searchKeyword.asStateFlow()

    private val _sortOrder = MutableStateFlow(AppTypes.SortType.AZ)
    val sortOrder = _sortOrder.asStateFlow()

    private val _pendingLockedItems = mutableStateListOf<CypherFileItem>()
    val pendingLockedItems = MutableStateFlow(_pendingLockedItems)

    private val _currentItem = MutableStateFlow<CypherFileItem?>(null)
    val currentItem = _currentItem.asStateFlow()

    private var _addFilesVisible = MutableStateFlow(true)
    val addFilesVisible = _addFilesVisible.asStateFlow()

    private var _isSelecting = MutableStateFlow(false)
    val isSelecting = _isSelecting.asStateFlow()

    private var _isInitialized = MutableStateFlow(false)
    val isInitialized = _isInitialized.asStateFlow()

    fun setInitialized(value: Boolean)
    {
        _isInitialized.value = value
    }

    fun <T> any(value: T, vararg values: StatusType): Boolean {
        return values.any { it == value }
    }

    fun applyFilter() {
        val filteredItems = mutableListOf<CypherFileItem>()

        if (_searchKeyword.value.isEmpty()) {
            filteredItems.addAll(_sourceItems)
        } else {
            for (item in _sourceItems) {
                item.inputInfo?.let { inputInfo ->
                    if (inputInfo.name.contains(_searchKeyword.value, ignoreCase = true)) {
                        filteredItems.add(item)
                    }
                }
            }
        }

        when (_sortOrder.value) {
            AppTypes.SortType.AZ -> {
                filteredItems.sortBy {
                    it.inputInfo?.name
                }
            }

            AppTypes.SortType.ZA -> {
                filteredItems.sortByDescending { it.inputInfo?.name }
            }

            AppTypes.SortType.Oldest -> {
                filteredItems.sortBy {
                    Files.readAttributes(it.inputInfo, BasicFileAttributes::class.java)
                        .creationTime().toMillis()
                }
            }

            AppTypes.SortType.Newest -> {
                filteredItems.sortByDescending {
                    Files.readAttributes(it.inputInfo, BasicFileAttributes::class.java)
                        .creationTime().toMillis()
                }
            }
        }

        _items.clear()
        _items.addAll(filteredItems)
    }


    private fun enableInputControls(value: Boolean) {
        _addFilesVisible.value = value
    }

    private fun enableOutputControl(value: Boolean) {
    }

    fun updateStatus(value: StatusType) {
        _status.value = value

        when (_status.value) {
            StatusType.Init -> {
                enableInputControls(true)
                enableOutputControl(false)
            }

            StatusType.Loaded -> {
                enableInputControls(true)
                enableOutputControl(_currentItem.value != null)

                if (_currentItem.value != null) {
                    _currentItem.value!!.isEnabled = true
                    updateFileItem(_currentItem.value)
                }
            }

            StatusType.Processing -> {
                enableInputControls(false)
                enableOutputControl(false)

                if (_currentItem.value != null) {
                    _currentItem.value!!.isEnabled = false
                    updateFileItem(_currentItem.value)
                }
            }

            StatusType.Processed -> {
                enableInputControls(true)
                enableOutputControl(true)

                if (_currentItem.value != null) {
                    _currentItem.value!!.isEnabled = true
                    updateFileItem(_currentItem.value)
                }
            }

            StatusType.ProcessFailed -> {
                enableInputControls(true)
                enableOutputControl(true)

                if (_currentItem.value != null) {
                    _currentItem.value!!.isEnabled = true
                    updateFileItem(_currentItem.value)
                }
            }

            else -> {}
        }
    }

    fun setSearchKeyword(value: String) {
        _searchKeyword.value = value
    }

    fun setSortOrder(value: AppTypes.SortType) {
        _sortOrder.value = value
    }

    fun setSelectMode(value: Boolean) {
        _isSelecting.value = value

        if (!value) {
            for (index in _sourceItems.indices) {
                val i = _sourceItems[index]
                i.isSelected = false
                updateOne(i)
            }
        }
    }

    suspend fun addOne(
        newItem: CypherFileItem
    ) {
        val existingItem =
            _sourceItems.find { it.inputInfo!!.pathString == newItem.inputInfo!!.pathString }
        if (existingItem != null) {
            updateOne(existingItem)
            return
        }

        newItem.tryLoadFooter(newItem.inputInfo!!.pathString, true)

        if (newItem.inputInfo!!.pathString.isNotEmpty()) {
            _sourceItems.add(newItem)
            applyFilter()
            withContext(Dispatchers.IO) {
                fileRepository.insert(FileEntity(newItem.inputInfo!!.pathString))
            }
        }
    }

    suspend fun initialize(context: Context) {
        withContext(Dispatchers.IO) {
            loadFiles(fileRepository.getAll())
        }
    }

    fun loadFiles(entities: List<FileEntity>) {
        if (entities.isEmpty()) return

        updateStatus(StatusType.Loading)

        var hasCorrupted = false

        val locker = ReentrantLock(true)

        var coreCount = Runtime.getRuntime().availableProcessors()
        var pathCount = entities.size

        fun endProgress(action: () -> Unit) {
            viewModelScope.launch(Dispatchers.Main) { action() }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val phases = Share.phases(entities.toList())

                for ((i, phase) in phases.withIndex()) {
                    for (pack in phase) {
                        val items = mutableListOf<CypherFileItem>()

                        for (itemsPerProcess in pack.chunked(coreCount)) {
                            itemsPerProcess.parallelStream().forEach { path ->
                                try {
                                    val item = CypherFileItem.create(path)
                                    item.status.set(FileSystemItem.S.Ready)

                                    locker.withLock {
                                        items.add(item)
                                    }
                                } catch (e: Exception) {
                                    hasCorrupted = true
                                }
                            }
                        }

                        items.sortByDescending { item -> item.inputInfo!!.name }

                        endProgress {
                            locker.withLock {
                                _sourceItems.addAll(items)
                            }
                        }

                        Share.sleepByPhase(i)
                    }
                }

                endProgress {
                    updateStatus(StatusType.Loaded)
                }
            } catch (e: Exception) {
                endProgress {
                    updateStatus(StatusType.LoadFailed)
                }
            }
        }
    }

    fun addFiles(
        paths: Array<String>,
        argv: Share.Argv,
        packageAction: ((items: List<CypherFileItem>) -> Unit)?
    ) {
        if (paths.isEmpty()) {
            updateStatus(StatusType.Loaded)
            return
        }

        updateStatus(StatusType.Loading)

        var hasCorrupted = false

        var locker = ReentrantLock(true)

        var coreCount = Runtime.getRuntime().availableProcessors()
        var uriCount = paths.size

        _pendingLockedItems.clear()

        fun endProgress(action: () -> Unit) {
            viewModelScope.launch(Dispatchers.Main) { action() }
        }

        fun progress(s: StatusType) {
            viewModelScope.launch(Dispatchers.Main) {
                if (_pendingLockedItems.size > 0) {
                }

                if (s == StatusType.Loaded) {
                    if (hasCorrupted) {

                    }
                }

                _pendingLockedItems.clear()
                updateStatus(s)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val phases = Share.phases(paths.toList())

                for ((i, phase) in phases.withIndex()) {
                    for (pack in phase) {
                        val items = mutableListOf<CypherFileItem>()

                        for (itemsPerProcess in pack.chunked(coreCount)) {
                            itemsPerProcess.parallelStream().forEach { path ->
                                var item: CypherFileItem? = null

                                try {
                                    item = CypherFileItem(path)

                                    if (item.footer == null) {
                                        item.setFileType(
                                            if (item.inputInfo!!.isRegularFile()) FileUtils.getTypeByExtension(
                                                item.inputInfo!!.extension
                                            ) else FileUtils.Type.Directory, false
                                        )
                                        Share.encodeOne(item, argv)
                                    } else {
                                        val tempFilePaths = mutableListOf<Path>()

                                        val outputRawRecoveringTempFilePath = AppDir.getFilePath(
                                            AppDir.Type.TemporaryFolder, "${UUID.randomUUID()}"
                                        )
                                        val outputRecoveringTempFilePath = AppDir.getFilePath(
                                            AppDir.Type.TemporaryFolder, "${UUID.randomUUID()}"
                                        )

                                        tempFilePaths.addAll(
                                            mutableListOf(
                                                outputRawRecoveringTempFilePath,
                                                outputRecoveringTempFilePath
                                            )
                                        )

                                        File(item.inputInfo!!.pathString).copyTo(
                                            outputRawRecoveringTempFilePath.toFile(), true
                                        )

                                        CryptographyUtils.decryptFile(
                                            outputRawRecoveringTempFilePath,
                                            outputRecoveringTempFilePath,
                                            argv.password,
                                            Constants.Salt,
                                            Constants.Iterations,
                                            Constants.KeyLength
                                        )

                                        item.setFileType(item.footer!!.metadata!!.FileType, false)
                                        item.originalInfo =
                                            Path(item.footer!!.metadata!!.OriginalName)

                                        item.recoveredFileOrFolderPath = AppDir.getFilePath(
                                            AppDir.Type.TemporaryFolder,
                                            File(item.footer!!.metadata!!.OriginalName).name
                                        ).toString()

                                        if (item.footer!!.metadata!!.IsFile) outputRecoveringTempFilePath.toFile()
                                            .copyTo(File(item.recoveredFileOrFolderPath), true)
                                        else ZipUtils.extractToDirectory(
                                            outputRecoveringTempFilePath.toString(),
                                            item.recoveredFileOrFolderPath,
                                            true
                                        )

                                        FileUtils.deleteFileOrDirectory(
                                            outputRawRecoveringTempFilePath.toString()
                                        )
                                        FileUtils.deleteFileOrDirectory(outputRecoveringTempFilePath.toString())

                                        //

                                        val outputEncryptingTempFilePath = AppDir.getFilePath(
                                            AppDir.Type.TemporaryFolder, "${UUID.randomUUID()}.tmp"
                                        )
                                        val outputZipFilePath = AppDir.getFilePath(
                                            AppDir.Type.TemporaryFolder, "${UUID.randomUUID()}.tmp"
                                        )

                                        tempFilePaths.addAll(
                                            listOf(
                                                outputEncryptingTempFilePath, outputZipFilePath
                                            )
                                        )

                                        if (item.fileType == FileUtils.Type.Directory) {
                                            ZipUtils.createFromDirectory(
                                                item.recoveredFileOrFolderPath,
                                                outputZipFilePath.toString()
                                            )
                                            CryptographyUtils.encryptFile(outputZipFilePath,
                                                outputRecoveringTempFilePath,
                                                argv.password,
                                                Constants.Salt,
                                                Constants.Iterations,
                                                Constants.KeyLength,
                                                {}) {}

                                            FileUtils.deleteFileOrDirectory(outputZipFilePath.toString())
                                        } else CryptographyUtils.encryptFile(Path(item.recoveredFileOrFolderPath),
                                            outputRecoveringTempFilePath,
                                            argv.password,
                                            Constants.Salt,
                                            Constants.Iterations,
                                            Constants.KeyLength,
                                            {}) {}

                                        FileFooter.appendToFile(
                                            outputRecoveringTempFilePath,
                                            item.footer!!.metadata!!,
                                            item.footer!!.footerThumbnailBuffer
                                        )

                                        //

                                        val outputFilePath = AppDir.getFilePath(
                                            AppDir.Type.LocalFolder,
                                            "Encrypted",
                                            "${UUID.randomUUID()}"
                                        )
                                        outputRecoveringTempFilePath.toFile()
                                            .copyTo(outputFilePath.toFile(), true)

                                        item.inputInfo = outputFilePath
                                        item.encryptedInfo = outputFilePath
                                        item.tryLoadFooter(
                                            CypherFileItem.FileInfoType.Encrypted, true
                                        )

                                        FileUtils.deleteFileOrDirectory(item.originalInfo!!.pathString)
                                        FileUtils.deleteFileOrDirectory(outputRecoveringTempFilePath.toString())
                                        FileUtils.deleteFileOrDirectory(outputZipFilePath.toString())

                                        item.updateCorruptedStatus()
                                    }

                                    this.launch(Dispatchers.IO) {
                                        fileRepository.insert(
                                            FileEntity(item.inputInfo!!.pathString)
                                        )
                                    }

                                    item.status.set(FileSystemItem.S.Ready)

                                    if (FileUtils.isFilePath(item.encryptedInfo!!.pathString) == true) {
                                        locker.withLock {
                                            items.add(item)
                                        }
                                    }
                                } catch (e: Exception) {
                                    item?.status?.set(FileSystemItem.S.ProcessFailed)

                                    if (e is CryptographyException) {
                                        locker.withLock {
                                            _pendingLockedItems.add(item!!)
                                        }
                                    } else hasCorrupted = true
                                }
                            }
                        }

                        endProgress {
                            locker.withLock {
                                packageAction?.let { it(items) }
                                _sourceItems.addAll(items)
                            }
                        }

                        Share.sleepByPhase(i)
                    }
                }
                progress(StatusType.Loaded)
                applyFilter()
            } catch (e: Exception) {
                progress(StatusType.LoadFailed)
            }
        }
    }

    fun updateOne(file: CypherFileItem) {
        val index = _sourceItems.indexOfFirst { it.inputInfo!!.pathString == file.inputInfo!!.pathString }

        if (index > -1) {
            _sourceItems[index] = file.clone()
            applyFilter()
        }
    }

    suspend fun removeOne(file: CypherFileItem) {
        withContext(Dispatchers.IO) {
            fileRepository.deleteByPath(file.inputInfo!!.pathString)
            _sourceItems.remove(file)
        }
    }

    suspend fun removeMany(mode: RemoveMode) {
        when (mode) {
            RemoveMode.All -> {
                _sourceItems.clear()
                withContext(Dispatchers.IO) {
                    fileRepository.deleteAll()
                }
            }

            RemoveMode.Selected -> {
                val filteredItems = _sourceItems.filter { item -> item.isSelected }
                withContext(Dispatchers.IO) {
                    filteredItems.forEach { item -> fileRepository.deleteByPath(item.inputInfo!!.pathString) }
                }
                _sourceItems.removeAll(filteredItems)
            }

            RemoveMode.NonExisted -> {
                val filteredItems = _sourceItems.filter { item -> !item.inputInfo!!.exists() }
                withContext(Dispatchers.IO) {
                    filteredItems.forEach { item -> fileRepository.deleteByPath(item.inputInfo!!.pathString) }
                }
                _sourceItems.removeAll(filteredItems)
            }
        }
    }

    fun updateFileItem(updatedItem: CypherFileItem?) {
        _currentItem.value = updatedItem
        if (updatedItem != null) {
            _currentItem.value = updatedItem.clone()
            updateOne(updatedItem)
        }
    }

    suspend fun deleteSelectedPermanently() {
        val selectedItems = _sourceItems.filter { it.isSelected }

        for (item in selectedItems) {
            deletePermanently(item)
        }
    }

    fun selectItem(item: CypherFileItem) {
        if (_isSelecting.value) {
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

    suspend fun deletePermanently(item: CypherFileItem): Boolean {
        val itemPath = item.inputInfo

        if (!Files.exists(itemPath)) return true

        var result = false

        withContext(Dispatchers.IO) {
            if (Files.isDirectory(itemPath)) {
                result = FileUtils.deleteRecursively(itemPath.toString())
            } else if (Files.isRegularFile(itemPath)) {
                result = Files.deleteIfExists(itemPath)
            }
        }

        if (result) removeOne(item)

        return result
    }

    suspend fun processFile(_argv: Share.Argv): Boolean {
        if (_currentItem.value == null) return false

        updateStatus(StatusType.Processing)

        if (_argv!!.password.isEmpty()) {
            updateStatus(StatusType.ProcessFailed)
            return false
        }

        val fileIndex =
            _sourceItems.indexOfFirst { it.inputInfo!!.pathString == _currentItem.value!!.inputInfo!!.pathString }
        if (fileIndex == -1) {
            updateStatus(StatusType.ProcessFailed)
            return false
        }

        val file = _sourceItems[fileIndex]

        try {
            file.status.set(FileSystemItem.S.Processing)
            updateFileItem(file)

            if (file.itemType == CypherFileItem.FileItemType.Normal) Share.encodeOne(file, _argv)
            else Share.decodeOne(file, _argv)

            file.status.set(FileSystemItem.S.Processed)
            file.moveOutputPathToInputPath()

            updateStatus(StatusType.Processed)

            return true
        } catch (e: Exception) {
            file.status.set(FileSystemItem.S.ProcessFailed)
            updateStatus(StatusType.ProcessFailed)

            return false
        } finally {
            file.isEnabled = true
            updateOne(file)
            updateFileItem(file)
        }
    }

    init {
        updateStatus(StatusType.Loaded)
    }
}