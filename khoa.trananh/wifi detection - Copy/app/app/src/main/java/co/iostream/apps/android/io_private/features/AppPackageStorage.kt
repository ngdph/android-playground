package co.iostream.apps.android.io_private.features

import java.nio.file.Path


class AppPackageStorage {
    companion object {
        @Volatile
        private var instance: AppPackageStorage? = null

        fun getInstance(): AppPackageStorage {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = AppPackageStorage()
                    }
                }
            }
            return instance!!
        }
    }

    var appDir: Path? = null
    fun setAppStoragePath(path: Path) {
        appDir = path
    }
}