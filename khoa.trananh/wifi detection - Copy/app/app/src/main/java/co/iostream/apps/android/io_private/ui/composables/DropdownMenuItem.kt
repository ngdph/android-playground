package co.iostream.apps.android.io_private.ui.composables

import android.content.Context
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import co.iostream.apps.android.io_private.customDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DialogItem(
    val context: Context,
    val titleResIdInt: Int,
    val subtitleResIdInt: Int,
    val coroutineScope: CoroutineScope,
    val onConfirmCallback: suspend () -> Unit = {}
) {
    val onClick = {
        if (!customDialog.getState()) {
            customDialog.title = context.getString(titleResIdInt)
            customDialog.subTitle = context.getString(subtitleResIdInt)
            customDialog.onConfirmCallback = {
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        onConfirmCallback
                    }
                }
            }
        }

        customDialog.enable(!customDialog.getState())
    }
}

data class MenuItem(
    var onClick: () -> Unit = {}, var iconResId: Int, var title: String
) {
    constructor(dialogItem: DialogItem, iconResId: Int, title: String) : this(
        dialogItem.onClick, iconResId, title
    )
}

@Composable
fun CustomDropdownMenuItem(
    item: MenuItem
) {
    DropdownMenuItem(onClick = item.onClick, text = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = item.iconResId),
                contentDescription = null,
                modifier = Modifier.padding(end = 10.dp)
            )
            Text(text = item.title)
        }
    })
}