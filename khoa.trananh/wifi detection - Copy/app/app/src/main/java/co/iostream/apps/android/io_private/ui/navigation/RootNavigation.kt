package co.iostream.apps.android.io_private.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import co.iostream.apps.android.io_private.LocalNavController
import co.iostream.apps.android.io_private.screens.installer.InstallerScreen
import co.iostream.apps.android.io_private.screens.main.ExportScreen
import co.iostream.apps.android.io_private.screens.main.ExportViewModel
import co.iostream.apps.android.io_private.screens.main.FileManagerScreen
import co.iostream.apps.android.io_private.screens.main.FileManagerViewModel
import co.iostream.apps.android.io_private.screens.misc.ImageViewerScreen
import co.iostream.apps.android.io_private.screens.settings.LanguageScreen
import co.iostream.apps.android.io_private.screens.settings.PromotionScreen
import co.iostream.apps.android.io_private.screens.settings.SettingsScreen
import co.iostream.apps.android.io_private.screens.settings.WebviewScreen

object RootNavigatorGraph {
    const val Installer = "root_installer"
    const val Main = "root_processing"
    const val Settings = "root_settings"
}

object MainNavigatorGraph {
    const val FileManager = "main_manager"
    const val Exporter = "main_batch"
}

object SettingsNavigatorGraph {
    const val Settings = "settings_settings"
    const val Languages = "settings_language"
    const val Promotion = "settings_promotion"
}

object MiscNavigatorGraph {
    const val WebViewer = "misc_webviewer"
    const val MediaViewer = "misc_mediaviewer"
}

@Composable
fun RootNavigation(
) {
    val navController = LocalNavController.current

    val fileManagerViewModel: FileManagerViewModel = hiltViewModel()
    val exportViewModel: ExportViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = RootNavigatorGraph.Installer) {
        composable(route = RootNavigatorGraph.Installer) {
            InstallerScreen()
        }

        navigation(
            route = RootNavigatorGraph.Main, startDestination = MainNavigatorGraph.FileManager
        ) {
            composable(route = MainNavigatorGraph.FileManager) {
                FileManagerScreen(fileManagerViewModel)
            }
            composable(route = MainNavigatorGraph.Exporter) {
                ExportScreen(fileManagerViewModel, exportViewModel)
            }
        }

        navigation(
            route = RootNavigatorGraph.Settings, startDestination = SettingsNavigatorGraph.Settings
        ) {
            composable(route = SettingsNavigatorGraph.Settings) {
                SettingsScreen()
            }

            composable(route = SettingsNavigatorGraph.Promotion) {
                PromotionScreen()
            }

            composable(route = SettingsNavigatorGraph.Languages) {
                LanguageScreen()
            }
        }

        composable(
            route = "${MiscNavigatorGraph.MediaViewer}/{uri}", arguments = listOf(
                navArgument("uri") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString(
                "uri"
            ) ?: ""

            ImageViewerScreen(imageUri = uri)
        }

        composable(
            route = "${MiscNavigatorGraph.WebViewer}/{url}", arguments = listOf(
                navArgument("url") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString(
                "url"
            ) ?: ""

            WebviewScreen(url)
        }
    }
}