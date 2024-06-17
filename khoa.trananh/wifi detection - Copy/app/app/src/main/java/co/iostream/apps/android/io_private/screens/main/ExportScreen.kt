package co.iostream.apps.android.io_private.screens.main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.iostream.apps.android.core.iofile.FileSystemItem
import co.iostream.apps.android.core.iofile.FileUtils
import co.iostream.apps.android.io_private.LocalNavController
import co.iostream.apps.android.io_private.R
import co.iostream.apps.android.io_private.configs.AdConfig
import co.iostream.apps.android.io_private.configs.AppTypes
import co.iostream.apps.android.io_private.customDialog
import co.iostream.apps.android.io_private.models.CypherFileItem
import co.iostream.apps.android.io_private.ui.composables.BannerAdView
import co.iostream.apps.android.io_private.ui.composables.CustomDropdownMenuItem
import co.iostream.apps.android.io_private.ui.composables.DialogItem
import co.iostream.apps.android.io_private.ui.composables.HeaderBar
import co.iostream.apps.android.io_private.ui.composables.HintView
import co.iostream.apps.android.io_private.ui.composables.MenuItem
import co.iostream.apps.android.io_private.ui.theme.Foreground2
import co.iostream.apps.android.io_private.ui.theme.Theme
import co.iostream.apps.android.io_private.ui.theme.seed
import co.iostream.apps.android.io_private.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.pathString

@Composable
fun ExportScreen(fileManagerViewModel: FileManagerViewModel, batchViewModel: ExportViewModel) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()

    val status = batchViewModel.status.collectAsState()
    val selectMode = batchViewModel.selectMode.collectAsState()

    val isProcessing = batchViewModel.any(
        status.value,
        ExportViewModel.StatusType.Loading,
        ExportViewModel.StatusType.Processing,
        ExportViewModel.StatusType.Pausing,
        ExportViewModel.StatusType.Stopping
    )

    LaunchedEffect(true) {
        batchViewModel.refreshGlobalVariables(fileManagerViewModel)
        coroutineScope.launch {
            batchViewModel.loadFiles(fileManagerViewModel.items.value.filter { it.isSelected })
        }
    }

    DisposableEffect(true) {
        onDispose {
            batchViewModel.reset()
        }
    }

    BackHandler(true) {
        if (selectMode.value) {
            batchViewModel.setSelectMode(false)
        } else if (!isProcessing) {
            batchViewModel.removeMany(ExportViewModel.RemoveMode.All)
            navController.popBackStack()
        }
    }

    Scaffold(
        content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                ContentTopBar(batchViewModel)
                ItemListBox(batchViewModel)
            }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.Transparent
            ) {
                ContentBottomBar(exportViewModel = batchViewModel)
            }
        },
    )
}

@Composable
private fun ContentTopBar(exportViewModel: ExportViewModel) {
    val navController = LocalNavController.current

    val status = exportViewModel.status.collectAsState()

    var showSetting by rememberSaveable { mutableStateOf(false) }
    val processing = exportViewModel.any(
        status.value,
        ExportViewModel.StatusType.Loading,
        ExportViewModel.StatusType.Pausing,
        ExportViewModel.StatusType.Processing,
        ExportViewModel.StatusType.Stopping,
    )

    val title = stringResource(id = R.string.export)

    HeaderBar(left = {
        TextButton(onClick = {
            navController.popBackStack()
        }) {
            Icon(
                Icons.Default.ArrowBack, null, modifier = Modifier.size(24.dp)
            )
        }
    }, title = title, right = {
        IconButton(
            onClick = {
                if (!processing) {
                    showSetting = !showSetting
                }
            }, colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_settings_24),
                null,
                modifier = Modifier.size(24.dp)
            )
        }
    })

    if (showSetting) {
        ExportConfigurationDialog(exportViewModel, {
            showSetting = false
        }, { showSetting = false })
    }
}

@Composable
private fun ListTopBar(viewModel: ExportViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .height(52.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        val items = viewModel.items.collectAsState()
        val selectMode = viewModel.selectMode.collectAsState()
        val status = viewModel.status.collectAsState()
        val selectedItemsCount = items.value.filter { it.isSelected }

        Crossfade(targetState = selectMode.value, label = "") {
            when (it) {
                true -> {
                    var showSetting by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            viewModel.setSelectMode(false)
                        }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Theme
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${selectedItemsCount.size} ${stringResource(id = if (selectedItemsCount.size > 1) R.string.items else R.string.item)}",
                                style = TextStyle(color = Theme)
                            )
                            IconButton(onClick = {
                                if (!customDialog.getState()) {
                                    customDialog.title =
                                        context.getString(R.string.remove_selected_items_from_list_title)
                                    customDialog.subTitle =
                                        context.getString(R.string.remove_selected_items_from_list_subtitle)
                                    customDialog.onConfirmCallback = {
                                        viewModel.removeSelected()
                                    }
                                }

                                customDialog.enable(!customDialog.getState())
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_playlist_remove_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = Theme
                                )
                            }
                            IconButton(
                                onClick = { showSetting = !showSetting },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = Theme
                                )
                            }

                            DropdownMenu(
                                expanded = showSetting,
                                onDismissRequest = { showSetting = false }) {
                                val menuItems = listOf(
                                    MenuItem(
                                        title = stringResource(id = R.string.delete_permanently),
                                        iconResId = R.drawable.baseline_delete_24,
                                        dialogItem = DialogItem(context = context,
                                            titleResIdInt = R.string.delete_permanently_title,
                                            subtitleResIdInt = R.string.delete_permanently_subtitle,
                                            coroutineScope = coroutineScope,
                                            onConfirmCallback = { viewModel.deleteSelectedPermanently() }),
                                    ),
                                )

                                menuItems.forEach { item ->
                                    CustomDropdownMenuItem(item = item)
                                }
                            }
                        }
                    }
                }

                false -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "${items.value.count()} ${stringResource(id = if (items.value.count() > 1) R.string.items else R.string.item)}")

                            if (!viewModel.any(
                                    status.value,
                                    ExportViewModel.StatusType.Loading,
                                    ExportViewModel.StatusType.Pausing,
                                    ExportViewModel.StatusType.Processing,
                                    ExportViewModel.StatusType.Stopping
                                )
                            ) {
                                IconButton(onClick = {
                                    if (!customDialog.getState()) {
                                        customDialog.title =
                                            context.getString(R.string.remove_all_of_items_title)
                                        customDialog.subTitle =
                                            context.getString(R.string.remove_all_of_items_subtitle)
                                        customDialog.onConfirmCallback = {
                                            coroutineScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    viewModel.removeMany(ExportViewModel.RemoveMode.All)
                                                }
                                            }
                                        }
                                    }

                                    customDialog.enable(!customDialog.getState())
                                }) {
                                    Icon(
                                        painter = painterResource(R.drawable.baseline_playlist_remove_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            } else {
                                IconButton(onClick = {}) {
                                    Icon(
                                        painter = painterResource(R.drawable.baseline_playlist_remove_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = Color.Transparent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemListBox(exportViewModel: ExportViewModel) {
    val context = LocalContext.current

    val selectMode = exportViewModel.selectMode.collectAsState()
    val featureType = exportViewModel.featureType.collectAsState()
    val status = exportViewModel.status.collectAsState()
    val isSelecting = exportViewModel.isSelecting.collectAsState()
    val items = exportViewModel.items.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    if (items.value.isEmpty()) {
        val hintIcon = when (featureType.value) {
            CypherFileItem.FeatureType.Encode -> painterResource(R.drawable.baseline_lock_24)
            CypherFileItem.FeatureType.Decode -> painterResource(R.drawable.baseline_lock_open_24)
        }
        val title = stringResource(id = R.string.no_items_here)
        val subtitle = stringResource(id = R.string.add_files_and_folders)

        HintView(
            hintIconPainter = hintIcon, title = title, subTitle = subtitle
        )
    } else {
        Column {
            ListTopBar(exportViewModel)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                itemsIndexed(
                    items.value,
                    key = { _, item -> item.inputInfo!!.pathString }) { index, item ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (index == items.value.size - 1) 96.dp else 0.dp)
                            .background(if (isSelecting.value && item.isSelected) seed else Color.Transparent)
                            .combinedClickable(onClick = {}, onLongClick = {
                                exportViewModel.setSelectMode(true)
                                item.isSelected = true
                                exportViewModel.updateOne(item)
                            }), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            IconButton(modifier = Modifier.height(32.dp), onClick = {
                                if (!item.isEnabled || exportViewModel.any(
                                        status.value,
                                        ExportViewModel.StatusType.Processing,
                                        ExportViewModel.StatusType.Loading
                                    )
                                ) return@IconButton

                                exportViewModel.selectItem(item)
                            }) {
                                item.inputInfo?.let {
                                    Icon(
                                        painter = if (isSelecting.value && item.isSelected) painterResource(
                                            R.drawable.baseline_check_circle_24
                                        ) else if (it.isRegularFile() && item.itemType != CypherFileItem.FileItemType.EncodedFolder) painterResource(
                                            R.drawable.baseline_insert_drive_file_24
                                        ) else painterResource(R.drawable.baseline_folder_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = if (item.isSelected && isSelecting.value) Theme else LocalContentColor.current
                                    )
                                }

                                when (item.fileType) {
                                    FileUtils.Type.Image -> {
                                        Icon(
                                            painter = painterResource(R.drawable.baseline_image_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = if (item.isSelected && isSelecting.value) Theme else LocalContentColor.current
                                        )
                                    }

                                    FileUtils.Type.Video -> {
                                        Icon(
                                            painter = painterResource(R.drawable.baseline_video_file_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = if (item.isSelected && isSelecting.value) Theme else LocalContentColor.current
                                        )
                                    }

                                    FileUtils.Type.Audio -> {
                                        Icon(
                                            painter = painterResource(R.drawable.baseline_audio_file_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = if (item.isSelected && isSelecting.value) Theme else LocalContentColor.current
                                        )
                                    }

                                    FileUtils.Type.Document -> {
                                        Icon(
                                            painter = painterResource(R.drawable.baseline_feed_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = if (item.isSelected && isSelecting.value) Theme else LocalContentColor.current
                                        )
                                    }

                                    FileUtils.Type.Compressed -> {
                                        Icon(
                                            painter = painterResource(co.iostream.apps.android.core.R.drawable.baseline_compress_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = if (item.isSelected && isSelecting.value) Theme else LocalContentColor.current
                                        )
                                    }

                                    else -> {}
                                }
                            }
                        }

                        Column {
                            Row {
                                Column(
                                    modifier = Modifier
                                        .weight(1F)
                                        .height(72.dp),
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = item.originalInfo!!.name,
                                        style = TextStyle(
                                            color = Foreground2,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )

                                    Row {
                                        if (FileUtils.getExtension(item.originalInfo!!.extension)
                                                .isNotEmpty()
                                        ) {
                                            Text(
                                                text = ".${FileUtils.getExtension(item.originalInfo!!.name)}, ",
                                                style = TextStyle(color = Foreground2)
                                            )
                                        }

                                        item.inputInfo?.let {
                                            if (it.exists()) {
                                                Text(
                                                    text = "${
                                                        FileUtils.getReadableFileSize(
                                                            it.fileSize()
                                                        )
                                                    }, ", style = TextStyle(
                                                        color = Foreground2, fontSize = 12.sp
                                                    )
                                                )
                                                Text(
                                                    text = DateUtils.formatRelative(
                                                        context, it.getLastModifiedTime().toMillis()
                                                    ), style = TextStyle(
                                                        color = Foreground2, fontSize = 12.sp
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = FileUtils.getBriefOfPath(item.originalInfo!!.parent.pathString),
                                        style = TextStyle(color = Foreground2, fontSize = 12.sp),
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Surface(
                                        modifier = Modifier
                                            .height(7.dp)
                                            .offset(0.dp, 4.dp)
                                    ) {

                                        if (status.value == ExportViewModel.StatusType.Processing && item.status.any(
                                                FileSystemItem.S.Processing
                                            )
                                        ) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(color = Theme)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.height(72.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    var isMenuOpened by remember { mutableStateOf(false) }

                                    if (item.isEnabled && !isSelecting.value) {
                                        IconButton(onClick = { isMenuOpened = !isMenuOpened }) {
                                            Icon(
                                                Icons.Default.MoreVert,
                                                contentDescription = null,
                                            )
                                        }
                                    }

                                    DropdownMenu(expanded = isMenuOpened,
                                        onDismissRequest = { isMenuOpened = false }) {
                                        val menuItems = listOf(
                                            MenuItem(
                                                title = stringResource(id = R.string.remove_from_list),
                                                iconResId = R.drawable.baseline_playlist_remove_24,
                                                onClick = {
                                                    coroutineScope.launch {
                                                        exportViewModel.removeOne(item)
                                                        isMenuOpened = false
                                                    }
                                                },
                                            ),
                                            MenuItem(
                                                title = stringResource(id = R.string.delete_permanently),
                                                iconResId = R.drawable.baseline_delete_24,
                                                dialogItem = DialogItem(context = context,
                                                    titleResIdInt = R.string.delete_permanently_title,
                                                    subtitleResIdInt = R.string.delete_permanently_subtitle,
                                                    coroutineScope = coroutineScope,
                                                    onConfirmCallback = {
                                                        exportViewModel.deletePermanently(
                                                            item
                                                        )
                                                    }),
                                            ),
                                        )

                                        menuItems.forEach { item ->
                                            CustomDropdownMenuItem(item = item)
                                        }
                                    }
                                }
                            }

                            Divider(thickness = (1).dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentBottomBar(exportViewModel: ExportViewModel) {
    val context = LocalContext.current
    val config = LocalConfiguration.current

    val coroutineScope = rememberCoroutineScope()

    val exportButtonEnabled by exportViewModel.exportButtonEnabled.collectAsState()
    val screenWidth = config.screenWidthDp.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height((56 + 60 + screenWidth.value * 5 / 100).dp),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Spacer(modifier = Modifier.width(screenWidth * 5 / 100))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(modifier = Modifier
                    .height(56.dp)
                    .fillMaxWidth(),
                    enabled = exportButtonEnabled,
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Gray,
                        disabledContentColor = Color.White,
                    ),
                    onClick = {
                        coroutineScope.launch {
                            exportViewModel.pressProcessAll(context)
                            Toast.makeText(
                                context,
                                context.getString(R.string.process_done),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) {
                    Text(
                        text = stringResource(id = R.string.export)
                    )
                }
            }

            Spacer(modifier = Modifier.width(screenWidth * 5 / 100))
        }

//        BannerAds()
    }
}

@Composable
private fun BannerAds() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(top = 4.dp)
            .padding(bottom = 4.dp)
    ) {
        BannerAdView(adUnitId = AdConfig.BATCH_BOTTOM)
    }
}

@Composable
private fun StorageLocationContent()
{
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    fun addFileFromUri(uri: Uri?, isTree: Boolean) {
        if (uri == null)
            return

        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )

        val finalUri =
            if (!isTree)
                uri
            else
                DocumentsContract.buildDocumentUriUsingTree(
                    uri,
                    DocumentsContract.getTreeDocumentId(uri)
                )

        FileUtils.getPathFromUri(context, finalUri)?.let { path ->
            coroutineScope.launch {
//                mainViewModel.addOne(FileItem(path))
//                TODO
            }
        }
    }

    val directoryPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                addFileFromUri(result.data?.data, true)
            }
        }
    Column(modifier = Modifier
        .padding(top =10.dp)
        .fillMaxWidth()
        .verticalScroll(rememberScrollState()),)
    {
        Text(
            text = "Storage location",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            style = MaterialTheme.typography.headlineSmall
        )

        TextButton(onClick = {
            var intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent = Intent.createChooser(intent, "Add a folder")
            directoryPickerLauncher.launch(intent)
        }) {
            Icon(painterResource(id = R.drawable.baseline_folder_24),contentDescription = "Folder Icon")
            Spacer(modifier = Modifier.width(8.dp))
            Text("All files will be stored in here.")
        }
    }
}
@Composable
fun ExportConfigurationDialog(
    exportViewModel: ExportViewModel, onDismissRequest: () -> Unit, onConfirmation: () -> Unit
) {
    val context = LocalContext.current

    val overwriteExisting by exportViewModel.overwriteExisting.collectAsState()
    val exportType by exportViewModel.exportType.collectAsState()

    AlertDialog(onDismissRequest = {
        onDismissRequest()
    }, text = {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = overwriteExisting,
                    onCheckedChange = { exportViewModel.setOverwriteExisting(it) })
                Text(text = context.getString(R.string.overwrite_existing))
            }

            StorageLocationContent()

            AppTypes.EXPORTS.forEach {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = (it.key == exportType),
                        onClick = { exportViewModel.setExportType(it.key) })
                    Text(text = context.getString(it.value))
                }
            }
        }
    }, confirmButton = {
        TextButton(onClick = {
            onConfirmation()
        }) {
            Text("Confirm")
        }
    }, dismissButton = {
        TextButton(onClick = {
            onDismissRequest()
        }) {
            Text("Dismiss")
        }
    })
}