package co.iostream.apps.android.io_private.ui.composables.inputs

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import co.iostream.apps.android.io_private.R
import co.iostream.apps.android.io_private.ui.theme.ThemeDarker

@Composable
fun InputPasswordBox(
    value: String, enabled: Boolean, width: Int = 60, onChangeCallback: (value: String) -> Unit
) {
    var isVisiblePassword by rememberSaveable { mutableStateOf(false) }
    val config = LocalConfiguration.current

    val screenWidth = config.screenWidthDp.dp

    TextField(
        value = value,
        onValueChange = { if (enabled) onChangeCallback(it.trim()) },
        label = { Text(stringResource(R.string.password), color = ThemeDarker) },
        singleLine = true,
        placeholder = { Text(stringResource(R.string.password)) },
        visualTransformation = if (isVisiblePassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            val image = if (isVisiblePassword) R.drawable.baseline_visibility_24
            else R.drawable.baseline_visibility_off_24

            val description =
                if (isVisiblePassword) stringResource(R.string.hide_password) else stringResource(R.string.show_password)

            IconButton(onClick = { isVisiblePassword = !isVisiblePassword }) {
                Icon(painter = painterResource(image), description)
            }
        },
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .defaultMinSize(
                minWidth = 0.dp, minHeight = 0.dp
            )
            .widthIn(min = 0.dp, max = (screenWidth * width / 100)),
    )
}