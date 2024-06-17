package co.iostream.apps.android.io_private.screens.installer

import android.os.Environment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import co.iostream.apps.android.core.iofile.FileUtils
import co.iostream.apps.android.io_private.LocalNavController
import co.iostream.apps.android.io_private.R
import co.iostream.apps.android.io_private.configs.AppTypes
import co.iostream.apps.android.io_private.configs.Constants
import co.iostream.apps.android.io_private.customDialog
import co.iostream.apps.android.io_private.dataStore
import co.iostream.apps.android.io_private.ui.composables.inputs.PasswordTextField
import co.iostream.apps.android.io_private.ui.navigation.RootNavigatorGraph
import co.iostream.apps.android.io_private.utils.CryptographyUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory


enum class InstallStep {
    Greetings, SetPassword, Login, Done;

    companion object {
        fun fromInt(value: Int) = FileUtils.Type.entries.first { it.ordinal == value }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InstallerScreen() {
    val context = LocalContext.current
    val navController = LocalNavController.current

    var installProgress by rememberSaveable { mutableStateOf(InstallStep.Greetings) }

    val totalPages = rememberSaveable { mutableIntStateOf(3) }
    val pagerState = rememberPagerState(initialPage = installProgress.ordinal,
        initialPageOffsetFraction = 0f,
        pageCount = { totalPages.intValue })

    LaunchedEffect(installProgress) {
        if (installProgress == InstallStep.Done) navController.navigate(RootNavigatorGraph.Main)
        else pagerState.scrollToPage(installProgress.ordinal, 0f)
    }

    LaunchedEffect(true) {
        val passwordValueKey = stringPreferencesKey("passwordValue")
        val passwordTypeKey = stringPreferencesKey("passwordType")

        val passwordTypeFlow = context.dataStore.data.map { preferences ->
            preferences[passwordTypeKey] ?: String()
        }
        val passwordValueFlow = context.dataStore.data.map { preferences ->
            preferences[passwordValueKey] ?: String()
        }

        val combinedFlows = combine(
            passwordTypeFlow, passwordValueFlow
        ) { combinedFlows -> combinedFlows }

        CoroutineScope(Dispatchers.Main).launch {
            combinedFlows.collect { (passwordType, passwordValue) ->
                val appStoragePath = Path(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                        .toString(), context.packageName
                )
                appStoragePath.createDirectories()

                if (appStoragePath.isDirectory()) {
                    installProgress = InstallStep.Greetings

                    if (passwordType.isNotEmpty() && passwordValue.isNotEmpty()) installProgress =
                        InstallStep.Login
                }

                pagerState.scrollToPage(installProgress.ordinal, 0f)
            }
        }
    }

    Scaffold(
        topBar = {},
        content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HorizontalPager(state = pagerState,
                            userScrollEnabled = false,
                            modifier = Modifier.weight(1f),
                            pageContent = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    when (it) {
                                        0 -> GreetingScreen(pagerState)
                                        1 -> SetPasswordScreen(pagerState)
                                        2 -> LoginScreen() {
                                            installProgress = InstallStep.Done
                                        }
                                    }
                                }
                            })

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .align(Alignment.CenterHorizontally)
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .padding(bottom = 8.dp), horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(pagerState.pageCount) { iteration ->
                                val color =
                                    if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
                                Box(
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = { },
        modifier = Modifier.fillMaxSize(),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GreetingScreen(pagerState: PagerState) {
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Greetings",
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
        )

        Text(
            text = "Set password for open app",
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
        )

        TextButton(onClick = { coroutineScope.launch { pagerState.scrollToPage(InstallStep.SetPassword.ordinal) } }) {
            Text(text = "Get Started")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SetPasswordScreen(pagerState: PagerState) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val password = rememberSaveable { mutableStateOf(String()) }
    val passwordHidden = rememberSaveable { mutableStateOf(true) }

    val passwordConfirm = rememberSaveable { mutableStateOf(String()) }
    val passwordConfirmHidden = rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Set password for open app",
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(modifier = Modifier.height(8.dp))
        PasswordTextField(stringResource(R.string.password), password, passwordHidden)

        Spacer(modifier = Modifier.padding(3.dp))
        PasswordTextField(
            stringResource(R.string.confirm_password), passwordConfirm, passwordConfirmHidden
        )

        Spacer(modifier = Modifier.padding(10.dp))

        Button(
            onClick = {
                if (password.value.isEmpty()) {
                    customDialog.title = context.getString(R.string.no_password_provided)
                    customDialog.enable(true)
                } else if (password.value != passwordConfirm.value) {
                    customDialog.title = context.getString((R.string.password_not_matched))
                    customDialog.enable(true)
                } else {
                    coroutineScope.launch {
                        context.dataStore.edit { preferences ->
                            val passwordTypeKey = stringPreferencesKey("passwordType")
                            val passwordValueKey = stringPreferencesKey("passwordValue")

                            val encryptedPassword = CryptographyUtils.encryptToB64(
                                password.value,
                                Constants.Token,
                                Constants.Salt,
                                Constants.Iterations,
                                Constants.KeyLength
                            )

                            preferences[passwordTypeKey] = AppTypes.PasswordType.Text.toString()
                            preferences[passwordValueKey] = encryptedPassword
                        }

                        pagerState.scrollToPage(InstallStep.Login.ordinal, 0f)
                    }
                }
            }, modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(50.dp)
        ) {
            Text(text = stringResource(id = R.string.confirm))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LoginScreen(successCallback: () -> Unit) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val password = rememberSaveable { mutableStateOf(String()) }
    val isHidden = rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PasswordTextField(stringResource(R.string.password), password, isHidden)

        Spacer(modifier = Modifier.padding(10.dp))

        Button(
            onClick = {
                val passwordValueKey = stringPreferencesKey("passwordValue")
                val passwordTypeKey = stringPreferencesKey("passwordType")

                val passwordTypeFlow = context.dataStore.data.map { preferences ->
                    preferences[passwordTypeKey] ?: String()
                }
                val passwordValueFlow = context.dataStore.data.map { preferences ->
                    preferences[passwordValueKey] ?: String()
                }

                val combinedFlows = combine(
                    passwordTypeFlow, passwordValueFlow
                ) { combinedFlows -> combinedFlows }

                coroutineScope.launch {
                    combinedFlows.collect { (_, passwordValue) ->
                        val decryptedStoredPassword = CryptographyUtils.decryptFromB64(
                            passwordValue,
                            Constants.Token,
                            Constants.Salt,
                            Constants.Iterations,
                            Constants.KeyLength
                        )

                        if (decryptedStoredPassword == password.value) successCallback()
                    }
                }
            }, modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(50.dp)
        ) {
            Text(stringResource(R.string.unlock))
        }
    }
}
