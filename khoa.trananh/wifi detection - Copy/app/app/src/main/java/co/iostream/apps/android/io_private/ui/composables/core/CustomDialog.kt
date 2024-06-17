package co.iostream.apps.android.io_private.ui.composables.core

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import co.iostream.apps.android.io_private.R

interface ICustomDialog {
    fun getState(): Boolean
    fun enable(value: Boolean)
    fun confirm()
    fun cancel()
    fun dismiss()
}

class CustomDialog : ICustomDialog {
    private var _visible = MutableStateFlow(false)
    val visible = _visible.asStateFlow()

    var title: String = ""
    var subTitle: String = ""
    var onConfirmCallback: () -> Unit = {}
    var onCancelCallback: () -> Unit = {}
    var onDismissCallback: () -> Unit = {}

    override fun getState(): Boolean = visible.value

    override fun enable(value: Boolean) {
        _visible.value = value
    }

    override fun confirm() {
        onConfirmCallback()
        _visible.value = false
    }

    override fun cancel() {
        onCancelCallback()
        _visible.value = false
    }

    override fun dismiss() {
        onDismissCallback()
        _visible.value = false
    }
}

@Composable
fun CustomDialogComposable(dialogControl: CustomDialog) {
    val visible by co.iostream.apps.android.io_private.customDialog.visible.collectAsState()

    if (visible) {
        AlertDialog(
            modifier = Modifier.fillMaxWidth(0.84f),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { dialogControl.dismiss() },
            title = {
                    Column {
                        Text(
                            text = dialogControl.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            fontSize = 14.sp,
                            text = dialogControl.subTitle,
                        )
                    }
            },
            confirmButton = {
                        TextButton(
                            onClick = { dialogControl.confirm() },
                        ) {
                            Text(stringResource(id = R.string.okay))
                        }
            },
            dismissButton = {
                        TextButton(
                            onClick = { dialogControl.cancel() },
                        ) {
                            Text(stringResource(id = R.string.cancel))
                        }
            }
//            Surface(
//                modifier = Modifier.wrapContentSize(),
//                shape = MaterialTheme.shapes.small,
//            ) {
//                Column(modifier = Modifier.padding(24.dp)) {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
//                    ) {
//                    }
//                }
//            }
        )
    }
}