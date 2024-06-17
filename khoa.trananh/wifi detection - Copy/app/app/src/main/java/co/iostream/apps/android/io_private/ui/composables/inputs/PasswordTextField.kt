package co.iostream.apps.android.io_private.ui.composables.inputs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import co.iostream.apps.android.io_private.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PasswordTextField(
    label: String, password: MutableState<String>, isHidden: MutableState<Boolean>
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = password.value,
        onValueChange = { password.value = it },
        shape = RoundedCornerShape(5.dp),
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
            )
        },
        visualTransformation = if (isHidden.value) PasswordVisualTransformation() else VisualTransformation.None,
        //  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done, keyboardType = KeyboardType.Password
        ),
//        colors = OutlinedTextFieldDefaults.colors(
//            focusedBorderColor = MaterialTheme.colorScheme.primary,
//            unfocusedBorderColor = MaterialTheme.colorScheme.primary,
//        ),
        trailingIcon = {
            IconButton(onClick = { isHidden.value = !isHidden.value }) {
                val visibilityIcon =
                    if (isHidden.value) R.drawable.baseline_visibility_24 else R.drawable.baseline_visibility_off_24
                // Please provide localized description for accessibility services
                val description = if (isHidden.value) "Show password" else "Hide password"
                Icon(painterResource(id = visibilityIcon), contentDescription = description)
            }
        },
        modifier = Modifier.fillMaxWidth(0.8f),
        keyboardActions = KeyboardActions(onDone = {
            keyboardController?.hide()
            // do something here
        })
    )
}