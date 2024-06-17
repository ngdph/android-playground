package co.iostream.apps.android.io_private.screens.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import co.iostream.apps.android.core.iofile.FileSystemItem
import co.iostream.apps.android.core.iofile.FileUtils
import co.iostream.apps.android.io_private.LocalNavController
import co.iostream.apps.android.io_private.R
import co.iostream.apps.android.io_private.configs.AppTypes
import co.iostream.apps.android.io_private.customDialog
import co.iostream.apps.android.io_private.dataStore
import co.iostream.apps.android.io_private.features.Share
import co.iostream.apps.android.io_private.findActivity
import co.iostream.apps.android.io_private.models.CypherFileItem
import co.iostream.apps.android.io_private.ui.composables.CustomDropdownMenuItem
import co.iostream.apps.android.io_private.ui.composables.DialogItem
import co.iostream.apps.android.io_private.ui.composables.FabIcon
import co.iostream.apps.android.io_private.ui.composables.FabOption
import co.iostream.apps.android.io_private.ui.composables.HintView
import co.iostream.apps.android.io_private.ui.composables.MenuItem
import co.iostream.apps.android.io_private.ui.composables.MultiFabItem
import co.iostream.apps.android.io_private.ui.composables.MultiFloatingActionButton
import co.iostream.apps.android.io_private.ui.navigation.MainNavigatorGraph
import co.iostream.apps.android.io_private.ui.navigation.bottomNavItems
import co.iostream.apps.android.io_private.ui.theme.Foreground2
import co.iostream.apps.android.io_private.ui.theme.Theme
import co.iostream.apps.android.io_private.ui.theme.seed
import co.iostream.apps.android.io_private.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
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
fun FileManagerScreen(mainViewModel: FileManagerViewModel) {
    val context = LocalContext.current

    val addFilesVisible = mainViewModel.addFilesVisible.collectAsState()
    val isSelecting = mainViewModel.isSelecting.collectAsState()

    val isInitialized = mainViewModel.isInitialized.collectAsState()
    if (!isInitialized.value) {
        LaunchedEffect(Unit) {
            mainViewModel.updateStatus(FileManagerViewModel.StatusType.Loaded)
            mainViewModel.initialize(context)

            mainViewModel.setInitialized(true)
        }
    }

    BackHandler(true) {
        if (isSelecting.value) {
            mainViewModel.setSelectMode(false)
        } else {
            val activity = context.findActivity()
            activity.run {
                this.finish()
            }
        }
    }

    Scaffold(
        topBar = {
            ContentTopBar(mainViewModel)
        },
        content = { innerPadding ->
            ContentBox(mainViewModel, innerPadding)
        },
        bottomBar = {
            ContentBottomBar()
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            if (addFilesVisible.value) {
                BottomFloatButton(mainViewModel)
            }
        },
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentTopBar(mainViewModel: FileManagerViewModel) {
    val navController = LocalNavController.current
    val status = mainViewModel.status.collectAsState()

    CenterAlignedTopAppBar(colors = topAppBarColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ), title = {
        Text(text = stringResource(id = R.string.app_name))
    }, navigationIcon = { }, actions = {
        var showSetting by remember { mutableStateOf(false) }
        val processing = mainViewModel.any(
            status.value,
            FileManagerViewModel.StatusType.Loading,
            FileManagerViewModel.StatusType.Processing
        )

        IconButton(
            onClick = {
                if (!processing) {
                    showSetting = !showSetting
                }
            }, colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.more_horiz),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }

        DropdownMenu(expanded = showSetting, onDismissRequest = { showSetting = false }) {
            val menuItems = listOf(
                MenuItem(title = stringResource(id = R.string.export),
                    iconResId = R.drawable.baseline_lock_24,
                    onClick = {
                        navController.navigate(MainNavigatorGraph.Exporter)
                    }),
            )

            menuItems.forEach { item ->
                CustomDropdownMenuItem(item = item)
            }
        }
    })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContentBox(mainViewModel: FileManagerViewModel, innerPadding: PaddingValues) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val items = mainViewModel.items.collectAsState()
    val status = mainViewModel.status.collectAsState()
    val isSelecting = mainViewModel.isSelecting.collectAsState()

    val selectedItemsCount = items.value.filter { it.isSelected }

    Column(modifier = Modifier.padding(innerPadding)) {
        if (items.value.isEmpty()) {
            EmptyHintView()
        } else {
            Column {
                Crossfade(targetState = isSelecting.value, label = "") {
                    if (it) {
                        SelectModeView(
                            mainViewModel = mainViewModel,
                            selectedItemsCount = selectedItemsCount.size,
                            coroutineScope = coroutineScope,
                            context = context
                        )
                    } else {
                        DefaultModeView(mainViewModel = mainViewModel, itemsCount = items.value.count(), context = context, coroutineScope = coroutineScope, status = status.value)
                    }
                }
                ItemListView(isSelecting = isSelecting.value, mainViewModel = mainViewModel, status = status.value, context = context, coroutineScope = coroutineScope)
            }
        }
    }
}

@Composable
private fun EmptyHintView() {
    HintView(
        hintIconPainter = painterResource(R.drawable.baseline_upload_file),
        title = stringResource(id = R.string.no_items_here),
        subTitle = stringResource(id = R.string.add_files_and_folders)
    )
}

@Composable
private fun SelectModeView(
    mainViewModel: FileManagerViewModel,
    selectedItemsCount: Int,
    coroutineScope: CoroutineScope,
    context: Context
) {
    var showSetting by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { mainViewModel.setSelectMode(false) }) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Theme
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$selectedItemsCount ${stringResource(id = if (selectedItemsCount == 1) R.string.item else R.string.items)}",
                style = TextStyle(color = Theme)
            )
            IconButton(onClick = {
                if (!customDialog.getState()) {
                    customDialog.title = context.getString(R.string.remove_selected_items_from_list_title)
                    customDialog.subTitle = context.getString(R.string.remove_selected_items_from_list_subtitle)
                    customDialog.onConfirmCallback = {
                        coroutineScope.launch {
                            mainViewModel.removeMany(FileManagerViewModel.RemoveMode.Selected)
                        }
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

            DropdownMenu(expanded = showSetting, onDismissRequest = { showSetting = false }) {
                val menuItems = listOf(
                    MenuItem(
                        title = stringResource(id = R.string.select_all),
                        iconResId = R.drawable.baseline_content_cut_24,
                        dialogItem = DialogItem(context = context,
                            titleResIdInt = R.string.select_all_title,
                            subtitleResIdInt = R.string.select_all_subtitle,
                            coroutineScope = coroutineScope,
                            onConfirmCallback = {
                                // Add select all logic here
                            })
                    ),
                    MenuItem(
                        title = stringResource(id = R.string.delete_permanently),
                        iconResId = R.drawable.baseline_delete_24,
                        dialogItem = DialogItem(context = context,
                            titleResIdInt = R.string.delete_permanently_title,
                            subtitleResIdInt = R.string.delete_permanently_subtitle,
                            coroutineScope = coroutineScope,
                            onConfirmCallback = { mainViewModel.deleteSelectedPermanently() })
                    ),
                )

                menuItems.forEach { item ->
                    CustomDropdownMenuItem(item = item)
                }
            }
        }
    }
}

@Composable
private fun DefaultModeView(
    mainViewModel: FileManagerViewModel,
    itemsCount: Int,
    context: Context,
    coroutineScope: CoroutineScope,
    status: FileManagerViewModel.StatusType
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$itemsCount ${stringResource(id = if (itemsCount == 1) R.string.item else R.string.items)}")

            IconButton(onClick = {
                if (!customDialog.getState()) {
                    customDialog.title = context.getString(R.string.remove_all_of_items_title)
                    customDialog.subTitle = context.getString(R.string.remove_all_of_items_subtitle)
                    customDialog.onConfirmCallback = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                mainViewModel.removeMany(FileManagerViewModel.RemoveMode.All)
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
                    tint = if (!mainViewModel.any(status, FileManagerViewModel.StatusType.Loading, FileManagerViewModel.StatusType.Processing)) LocalContentColor.current else Color.Transparent
                )
            }
        }
    }
}

@Composable
private fun ItemListView(
    isSelecting: Boolean,
    mainViewModel: FileManagerViewModel,
    status: FileManagerViewModel.StatusType,
    context: Context,
    coroutineScope: CoroutineScope
) {
    val items = mainViewModel.items.collectAsState()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        itemsIndexed(
            items = items.value,
        ) { index, item ->
            ItemRow(
                item = item,
                isSelecting = isSelecting,
                isLastItem = index == items.value.size - 1,
                mainViewModel = mainViewModel,
                status = status,
                context = context,
                coroutineScope = coroutineScope
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemRow(
    item: CypherFileItem,
    isSelecting: Boolean,
    isLastItem: Boolean,
    mainViewModel: FileManagerViewModel,
    status: FileManagerViewModel.StatusType,
    context: Context,
    coroutineScope: CoroutineScope
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLastItem) 96.dp else 0.dp)
            .background(if (isSelecting && item.isSelected) seed else Color.Transparent)
            .combinedClickable(onClick = {}, onLongClick = {
                mainViewModel.setSelectMode(true)
                item.isSelected = true
                mainViewModel.updateOne(item)
            }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ItemIconButton(item = item, isSelecting = isSelecting, mainViewModel = mainViewModel, status = status)
        ItemDetails(item = item, context = context)
        ItemMoreOptions(item = item, isSelecting = isSelecting, mainViewModel = mainViewModel, coroutineScope = coroutineScope)
    }
}

@Composable
private fun ItemIconButton(
    item: CypherFileItem,
    isSelecting: Boolean,
    mainViewModel: FileManagerViewModel,
    status: FileManagerViewModel.StatusType
) {
    Box {
        IconButton(modifier = Modifier.height(32.dp), onClick = {
            if (!item.isEnabled || mainViewModel.any(status, FileManagerViewModel.StatusType.Processing, FileManagerViewModel.StatusType.Loading)) return@IconButton

            mainViewModel.selectItem(item)
        }) {
            item.inputInfo?.let {
                Icon(
                    painter = if (isSelecting && item.isSelected) painterResource(R.drawable.baseline_check_circle_24) else if (it.isRegularFile() && item.itemType != CypherFileItem.FileItemType.EncodedFolder) painterResource(R.drawable.baseline_insert_drive_file_24) else painterResource(R.drawable.baseline_folder_24),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (item.isSelected && isSelecting) Theme else LocalContentColor.current
                )
            }

            when (item.fileType) {
                FileUtils.Type.Image -> {
                    Icon(
                        painter = painterResource(R.drawable.baseline_image_24),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (item.isSelected && isSelecting) Theme else LocalContentColor.current
                    )
                }

                FileUtils.Type.Video -> {
                    Icon(
                        painter = painterResource(R.drawable.baseline_video_file_24),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (item.isSelected && isSelecting) Theme else LocalContentColor.current
                    )
                }

                FileUtils.Type.Audio -> {
                    Icon(
                        painter = painterResource(R.drawable.baseline_audio_file_24),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (item.isSelected && isSelecting) Theme else LocalContentColor.current
                    )
                }

                FileUtils.Type.Document -> {
                    Icon(
                        painter = painterResource(R.drawable.baseline_feed_24),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (item.isSelected && isSelecting) Theme else LocalContentColor.current
                    )
                }

                FileUtils.Type.Compressed -> {
                    Icon(
                        painter = painterResource(co.iostream.apps.android.core.R.drawable.baseline_compress_24),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (item.isSelected && isSelecting) Theme else LocalContentColor.current
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun ItemDetails(item: CypherFileItem, context: Context) {
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
                    if (FileUtils.getExtension(item.originalInfo!!.extension).isNotEmpty()) {
                        Text(
                            text = ".${FileUtils.getExtension(item.originalInfo!!.name)}, ",
                            style = TextStyle(color = Foreground2)
                        )
                    }

                    item.inputInfo?.let {
                        if (it.exists()) {
                            Text(
                                text = "${FileUtils.getReadableFileSize(it.fileSize())}, ", style = TextStyle(
                                    color = Foreground2, fontSize = 12.sp
                                )
                            )
                            Text(
                                text = DateUtils.formatRelative(context, it.getLastModifiedTime().toMillis()), style = TextStyle(
                                    color = Foreground2, fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                Text(
                    text = FileUtils.getBriefOfPath(item.originalInfo!!.parent.pathString),
                    style = TextStyle(
                        color = Foreground2, fontSize = 12.sp
                    ),
                    overflow = TextOverflow.Ellipsis
                )

                Surface(
                    modifier = Modifier
                        .height(7.dp)
                        .offset(0.dp, 4.dp)
                ) {
                    if (item.status.any(FileSystemItem.S.Processing)) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(color = Theme)
                    }
                }
            }
        }
        Divider(thickness = 1.dp)
    }
}

@Composable
private fun ItemMoreOptions(
    item: CypherFileItem,
    isSelecting: Boolean,
    mainViewModel: FileManagerViewModel,
    coroutineScope: CoroutineScope
) {
    Row(
        modifier = Modifier.height(72.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var isMenuOpened by remember { mutableStateOf(false) }

        if (item.isEnabled && !isSelecting) {
            IconButton(onClick = { isMenuOpened = !isMenuOpened }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null,
                )
            }
        }

        DropdownMenu(expanded = isMenuOpened, onDismissRequest = { isMenuOpened = false }) {
            val menuItems = listOf(
                MenuItem(
                    title = stringResource(id = R.string.remove_from_list),
                    iconResId = R.drawable.baseline_playlist_remove_24,
                    onClick = {
                        coroutineScope.launch {
                            mainViewModel.removeOne(item)
                            isMenuOpened = false
                        }
                    },
                ),
                MenuItem(
                    title = stringResource(id = R.string.delete_permanently),
                    iconResId = R.drawable.baseline_delete_24,
                    dialogItem = DialogItem(context = LocalContext.current,
                        titleResIdInt = R.string.delete_permanently_title,
                        subtitleResIdInt = R.string.delete_permanently_subtitle,
                        coroutineScope = coroutineScope,
                        onConfirmCallback = {
                            mainViewModel.deletePermanently(item)
                        })
                ),
            )

            menuItems.forEach { item ->
                CustomDropdownMenuItem(item = item)
            }
        }
    }
}

@Composable
private fun ContentBottomBar() {
    val navController = LocalNavController.current

    val customModifier = Modifier
        .fillMaxWidth()
        .background(
            color = Color.Transparent
        )
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .drawWithCache {
            val path = Path()
            path.addRect(
                Rect(
                    topLeft = Offset.Zero, bottomRight = Offset(size.width, size.height)
                )
            )
            onDrawWithContent {
                clipPath(path) {
                    this@onDrawWithContent.drawContent()
                }
                val dotSize = 90f
                drawCircle(
                    Color.Black, radius = dotSize, center = Offset(
                        x = size.width / 2, y = 0f
                    ), blendMode = BlendMode.Clear
                )
            }
        }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    BottomAppBar(containerColor = Color.Transparent) {
        NavigationBar(
            modifier = customModifier.height(60.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            bottomNavItems.forEach { item ->
                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                NavigationBarItem(selected = selected,
                    onClick = { navController.navigate(item.route) },
                    label = {},
                    icon = {
                        Icon(
                            painterResource(id = item.icon),
                            contentDescription = "${item.name} Icon",
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun BottomFloatButton(mainViewModel: FileManagerViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    suspend fun addFilesOrFolder(filePaths: Array<String>) {
        val passwordValueKey = stringPreferencesKey("passwordValue")
        val passwordValueFlow =
            context.dataStore.data.map { preferences -> preferences[passwordValueKey] ?: String() }

        passwordValueFlow.collect {
            mainViewModel.addFiles(
                filePaths, Share.Argv(it, false, false, AppTypes.ExportType.Export), null
            )
        }
    }

    val filePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uris = mutableListOf<Uri>()

                if (result.data?.clipData != null) {
                    val count = result.data?.clipData?.itemCount ?: 0

                    for (i in 0 until count) result.data?.clipData?.getItemAt(i)?.uri?.let {
                        uris.add(
                            FileUtils.getFinalUri(
                                context, it, false
                            )
                        )
                    }

                } else result.data?.data?.let { FileUtils.getFinalUri(context, it, false) }
                    ?.let { uris.add(it) }

                val itemPaths =
                    uris.mapNotNull { FileUtils.getPathFromUri(context, it) }.toTypedArray()

                coroutineScope.launch {
                    addFilesOrFolder(itemPaths)
                }
            }
        }

    val directoryPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    val uris = mutableListOf<Uri>()
                    uris.add(FileUtils.getFinalUri(context, uri, true))

                    val itemPaths =
                        uris.mapNotNull { FileUtils.getPathFromUri(context, it) }.toTypedArray()

                    coroutineScope.launch {
                        addFilesOrFolder(itemPaths)
                    }
                }
            }
        }

    Column(
        modifier = Modifier.offset(y = 56.dp)
    ) {
        MultiFloatingActionButton(
            fabIcon = FabIcon(
                iconRes = R.drawable.baseline_add_24,
                iconResAfterRotate = R.drawable.baseline_add_24,
                iconRotate = 135f
            ),
            fabOption = FabOption(
                iconTint = Color.White,
                showLabels = true,
                backgroundTint = Theme,
            ),
            itemsMultiFab = listOf(
                MultiFabItem(
                    tag = "AddFiles",
                    icon = R.drawable.baseline_insert_drive_file_24,
                    label = stringResource(id = R.string.add_files),
                ),
                MultiFabItem(
                    tag = "AddAFolder",
                    icon = R.drawable.baseline_folder_24,
                    label = stringResource(id = R.string.add_a_folder),
                ),
            ),
            onFabItemClicked = {
                if (it.tag == "AddFiles") {
                    var intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    intent.putExtra(
                        DocumentsContract.EXTRA_INITIAL_URI,
                        Environment.getExternalStorageDirectory().toUri()
                    )
                    intent.type = "*/*"
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent = Intent.createChooser(intent, "Add files")
                    filePickerLauncher.launch(intent)
                } else {
                    var intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.addCategory(Intent.CATEGORY_DEFAULT)
                    intent = Intent.createChooser(intent, "Add a folder")
                    directoryPickerLauncher.launch(intent)
                }
            },
            fabTitle = "MultiFloatActionButton", showFabTitle = false,
        )
    }
}