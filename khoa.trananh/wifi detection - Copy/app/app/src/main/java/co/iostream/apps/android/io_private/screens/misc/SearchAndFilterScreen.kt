package co.iostream.apps.android.io_private.screens.misc

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import co.iostream.apps.android.io_private.LocalNavController
import co.iostream.apps.android.io_private.R.drawable
import co.iostream.apps.android.io_private.configs.AdConfig
import co.iostream.apps.android.io_private.ui.composables.BannerAdView
import co.iostream.apps.android.io_private.ui.composables.HeaderBar
import co.iostream.apps.android.io_private.ui.composables.buttons.IconTextButton
import co.iostream.apps.android.io_private.screens.main.FileManagerViewModel

@Composable
fun SearchAndFilterScreen(
) {
    val navController = LocalNavController.current
    val mainViewModel: FileManagerViewModel = hiltViewModel()

    DisposableEffect(Unit) {
        onDispose {}
    }

    BackHandler(true) {
        navController.popBackStack()
    }

    Scaffold(
        content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                ContentTopBar(navController, mainViewModel)
                SubContentTopBar(navController, mainViewModel)
                ListContainerBox(navController, mainViewModel)
            }
        },
        bottomBar = {
            BottomAppBar(containerColor = Color.Transparent) {}
        },
    )
}

@Composable
private fun ContentTopBar(navController: NavHostController, mainViewModel: FileManagerViewModel) {
    val title = "Tìm trong IO Private"

    HeaderBar(
        left = {
            TextButton(
                onClick = {
                    navController.popBackStack()
                },
            ) {
                Icon(
                    Icons.Default.ArrowBack, null, modifier = Modifier.size(24.dp),
                )
            }
        },
        title = title,
        right = {
            Spacer(modifier = Modifier.size(48.dp))
        },
    )
}

@Composable
private fun SubContentTopBar(navController: NavHostController, mainViewModel: FileManagerViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTextButton(
                painter = painterResource(id = drawable.baseline_arrow_forward_ios_24),
                text = "Loại",
                onClick = {},
            )

            IconTextButton(
                painter = painterResource(id = drawable.baseline_arrow_forward_ios_24),
                text = "Thời điểm sửa đổi",
                onClick = {},
            )

            Surface {}
        }
    }
}

@Composable
private fun ListTopBar(navController: NavHostController, mainViewModel: FileManagerViewModel) {
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListContainerBox(navController: NavHostController, mainViewModel: FileManagerViewModel) {
}

@Composable
private fun ContentBottomBar(mainViewModel: FileManagerViewModel) {
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
