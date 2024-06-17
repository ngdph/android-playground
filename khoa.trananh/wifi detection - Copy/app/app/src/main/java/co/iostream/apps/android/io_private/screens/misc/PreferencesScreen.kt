package co.iostream.apps.android.io_private.screens.misc

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.navigation.NavController
import co.iostream.apps.android.core.iofile.FileUtils
import co.iostream.apps.android.io_private.LocalNavController
import co.iostream.apps.android.io_private.ui.navigation.RootNavigatorGraph
import co.iostream.apps.android.io_private.R
import co.iostream.apps.android.io_private.ui.composables.HeaderBar
import kotlinx.coroutines.launch

@Composable
fun PreferencesScreen() {
    val navController = LocalNavController.current

    Scaffold(
        content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                HeaderBar(
                    left = {
                        IconButton(
                            onClick = {navController.popBackStack()}
                        ) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                null,
                                modifier = Modifier.size(24.dp))
                        }
                    },
                    title = "Preferences",
                    right = {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                )
                ChangePasswordContent(navController)

            }
        },
    )
}

@Composable
private fun ChangePasswordContent(navController: NavController)
{
    Column(modifier = Modifier
        .padding(10.dp)
        .fillMaxWidth()
        .verticalScroll(rememberScrollState()),)
    {
        Text(
            text = "The configurations are intended for the purpose of restricting private, sensitive media from being displayed in public environments, these features do not support high-level media security.",
            maxLines = 4,
            fontSize = 14.sp,
            modifier = Modifier
                .fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
        )
        Divider(thickness = 1.dp, modifier = Modifier.padding(top = 15.dp, bottom = 15.dp))

        Text(
            text = "Change Password",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "This application will be required to enter a password to open.",
            fontSize = 14.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(modifier = Modifier.height(8.dp))
        SimpleOutlinedPasswordFieldSample1("Current Password")

        Spacer(modifier = Modifier.padding(3.dp))
        SimpleOutlinedPasswordFieldSample1("New Password")

        Spacer(modifier = Modifier.padding(3.dp))
        SimpleOutlinedPasswordFieldSample1("Retype the password to confirm")

        Spacer(modifier = Modifier.padding(10.dp))

        Text(
            text = "*",
            fontSize = 20.sp,
            modifier = Modifier
                .padding(top = 3.dp , bottom = 3.dp),
        )
        Button(
            onClick = { navController.navigate(RootNavigatorGraph.Main) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "Apply", fontSize = 20.sp)
        }

        Divider(thickness = 1.dp, modifier = Modifier.padding(top = 15.dp))
    }
}



@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SimpleOutlinedPasswordFieldSample1(tilte: String) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var password by rememberSaveable { mutableStateOf("") }
    var passwordHidden by rememberSaveable { mutableStateOf(true) }
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        shape = RoundedCornerShape(5.dp),
        label = {
            Text(
                text = tilte,
                style = MaterialTheme.typography.labelMedium,
            ) },
        visualTransformation =
        if (passwordHidden) PasswordVisualTransformation() else VisualTransformation.None,
        //  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Password
        ),
        trailingIcon = {
            IconButton(onClick = { passwordHidden = !passwordHidden }) {
                val visibilityIcon =
                    if (passwordHidden) R.drawable.baseline_visibility_24 else R.drawable.baseline_visibility_off_24
                val description = if (passwordHidden) "Show password" else "Hide password"
                Icon(painterResource(id = visibilityIcon), contentDescription = description)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        keyboardActions = KeyboardActions(
            onDone = {
                keyboardController?.hide()
                // do something here
            }
        )
    )
}

