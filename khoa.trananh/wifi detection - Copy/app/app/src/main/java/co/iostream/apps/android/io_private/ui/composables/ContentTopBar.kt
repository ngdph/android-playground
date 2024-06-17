package co.iostream.apps.android.io_private.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import co.iostream.apps.android.io_private.R
import co.iostream.apps.android.io_private.screens.main.FileManagerViewModel
import co.iostream.apps.android.io_private.ui.composables.buttons.IconTextButton
import co.iostream.apps.android.io_private.ui.navigation.MainNavigatorGraph


@Composable
private fun ContentTopBar(
    navController: NavHostController, mainViewModel: FileManagerViewModel
) {
    var isListView = true

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTextButton(
                painter = painterResource(id = R.drawable.baseline_arrow_forward_ios_24),
                text = "Export File",
                onClick = { navController.navigate(MainNavigatorGraph.Exporter) },
            )

            IconButton(
                onClick = { isListView = !isListView },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
            ) {
                if (isListView) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_view_list_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_view_module_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}