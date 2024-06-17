package co.iostream.apps.android.io_private.utils

import java.io.File

class AppUtils {
    companion object {
        private var filesDir: File? = null
        private var historyFile: File? = null

        fun getAppDir() = filesDir

        fun setAppDir(dir: File) {
            filesDir = dir
        }

        fun getSupportedLanguages(): List<String> {
            return listOf("vi", "en", "ja")
        }

        fun getHistoryFile() = historyFile

        fun setHistoryFile(file: File) {
            historyFile = file
        }
    }
}
