package co.iostream.apps.android.io_private

import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import co.iostream.apps.android.io_private.features.AppPackageStorage
import co.iostream.apps.android.io_private.ui.composables.core.CustomDialog
import co.iostream.apps.android.io_private.ui.composables.core.CustomDialogComposable
import co.iostream.apps.android.io_private.ui.navigation.RootNavigation
import co.iostream.apps.android.io_private.utils.AppDir
import kotlinx.coroutines.flow.first
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.pathString


val customDialog: CustomDialog = CustomDialog()

@Composable
fun MainApp() {
    val context = LocalContext.current

    LaunchedEffect(true) {
        try {
            val storagePathKey = stringPreferencesKey(name = "storagePath")
            val getStorageFlow = context.dataStore.getValueFlow(storagePathKey, String())

            val storagePathValue = getStorageFlow.first()
            var storagePath = Path(storagePathValue)

            if (!storagePath.exists()) {
                storagePath = Path(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path,
                    context.packageName
                )

                context.dataStore.edit { mutablePreferences ->
                    mutablePreferences[storagePathKey] = storagePath.pathString
                }
            } else storagePath = Path(storagePathValue)

            AppPackageStorage.getInstance().setAppStoragePath(storagePath)
            AppDir.initBase(storagePath.pathString)
        } catch (_: Exception) {
        }
    }

    RootNavigation()

// Global components
    CustomDialogComposable(customDialog)
}