package co.iostream.apps.android.io_private.screens.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import co.iostream.apps.android.data.helper.LocaleUtils
import co.iostream.apps.android.io_private.LocalNavController
import co.iostream.apps.android.io_private.R
import co.iostream.apps.android.io_private.screens.main.FileManagerViewModel
import co.iostream.apps.android.io_private.ui.composables.HeaderBar
import co.iostream.apps.android.io_private.ui.navigation.MiscNavigatorGraph
import co.iostream.apps.android.io_private.ui.navigation.SettingsNavigatorGraph
import co.iostream.apps.android.io_private.ui.navigation.bottomNavItems
import co.iostream.apps.android.io_private.ui.theme.Foreground2
import co.iostream.apps.android.io_private.utils.AppUtils
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Year


private data class SettingItem(val icon: Int, val title: String, val url: String)

data class FeaturedItem(val icon: Int, val name: String, val description: String)

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val mainViewModel: FileManagerViewModel = hiltViewModel()

    val addFilesVisible = mainViewModel.addFilesVisible.collectAsState()

    val miscItems = listOf(
        SettingItem(
            title = stringResource(id = R.string.rate_app),
            icon = R.drawable.baseline_thumb_up_24,
            url = "market://details?id=${context.packageName}"
        ),
        SettingItem(
            title = stringResource(id = R.string.help_and_support),
            icon = R.drawable.baseline_mode_comment_24,
            url = "https://www.iostream.co/contact"
        ),
        SettingItem(
            title = stringResource(id = R.string.how_to_use),
            icon = R.drawable.baseline_question_mark_24,
            url = "https://www.iostream.co/io/how-to-use-file-locker-x-7uy38"
        ),
    )

    val promotionItems = listOf(
        SettingItem(
            title = stringResource(id = R.string.more_apps),
            icon = R.drawable.baseline_workspaces_24,
            url = "https://www.iostream.co/apps"
        )
    )

    val privacyItems = listOf(
        SettingItem(
            title = stringResource(id = R.string.privacy),
            icon = R.drawable.baseline_privacy_tip_24,
            url = "https://www.iostream.co/io/io-apps-privacy-policy-D13wF2"
        )
    )

    Scaffold(
        content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                HeaderBar(
                    left = {
                        Spacer(modifier = Modifier.size(48.dp))
                    },
                    title = stringResource(id = R.string.settings),
                    right = {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                )
                SettingContent(
                    context = context,
                    navController = navController,
                    privacyItems = privacyItems,
                    promotionItems = promotionItems,
                    miscItems = miscItems
                )
            }
        },
        bottomBar = {
            BottomAppBar(containerColor = Color.Transparent) {
                ContentBottomBar()
            }
        },
    )
}

@Composable
private fun SettingContent(
    context: Context,
    navController: NavHostController,
    privacyItems: List<SettingItem>,
    promotionItems: List<SettingItem>,
    miscItems: List<SettingItem>,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .scrollable(state = scrollState, orientation = Orientation.Vertical)
    ) {
        val availableLanguages = LocaleUtils.getAvailableLocales().map { it.language }
        val availableSupportedLanguages = AppUtils.getSupportedLanguages()
        val supportedLanguages =
            availableLanguages.intersect(availableSupportedLanguages.toSet())

        AppBanner(
            appName = stringResource(id = R.string.app_name),
            context = context
        )

        Divider(thickness = 1.dp)

        Column(modifier = Modifier.weight(1f)) {
            if (supportedLanguages.isNotEmpty()) {
                SettingRowNotIcon(SettingsNavigatorGraph.Languages, navController)
                Divider(thickness = 1.dp)
            }

            val settingItems: List<SettingItem> = miscItems + promotionItems + privacyItems
            settingItems.forEach { item ->
                SettingRow(context = context, navController, item = item)
            }
        }
    }
}

@Composable
private fun AppBanner(
    appName: String,
    context: Context
) {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(0.dp))
            .background(Color(0x11ffffff))
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val vector = ImageVector.vectorResource(id = R.drawable.icon)
            val painter = rememberVectorPainter(image = vector)

            Canvas(
                modifier = Modifier.size(55.dp)
            ) {
                with(painter) {
                    draw(
                        size = Size(width = 55.dp.toPx(), height = 55.dp.toPx())
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(start = 10.dp)
            ) {
                Text(
                    text = appName,
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                )

                Text(
                    text = "Version ${packageInfo.versionName}",
                    style = TextStyle(color = Foreground2, fontSize = 10.sp),
                    modifier = Modifier.absolutePadding(bottom = 2.dp)
                )

                Row {
                    val intent = remember {
                        Intent(
                            Intent.ACTION_VIEW, Uri.parse("https://www.iostream.co/")
                        )
                    }
                    Text(
                        text = "©${stringResource(id = R.string.company_full_name)}, ${Year.now().value}",
                        style = TextStyle(fontSize = 12.sp),
                        modifier = Modifier.clickable { context.startActivity(intent) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    context: Context,
    navController: NavHostController,
    item: SettingItem,
    onClick: (() -> Unit)? = null
) {
    val intent = remember {
        Intent(
            Intent.ACTION_VIEW, Uri.parse(item.url)
        )
    }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(0.dp))
            .clickable(indication = null,
                interactionSource = interactionSource,
                onClick = {
                    onClick ?: navController.navigate(
                        "${MiscNavigatorGraph.WebViewer}/${
                            URLEncoder.encode(
                                item.url,
                                StandardCharsets.UTF_8.toString()
                            )
                        }"
                    )
                })
            .padding(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(item.icon),
                "icon",
                modifier = Modifier.size(28.dp)
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 15.dp)
            ) {
                Text(
                    text = item.title,
                    style = TextStyle(fontSize = 14.sp)
                )

                Icon(
                    painter = painterResource(R.drawable.baseline_arrow_forward_ios_24),
                    "icon",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingRowNotIcon(
    route: String,
    navController: NavHostController,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(0.dp))
            .clickable(indication = null,
                interactionSource = interactionSource,
                onClick = { navController.navigate(route) })
            .padding(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(6f)) {
                Text(
                    text = stringResource(id = R.string.language),
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            }

            Column {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_arrow_forward_ios_24),
                    "icon",
                    modifier = Modifier.size(20.dp)
                )
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

                NavigationBarItem(
                    selected = selected,
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