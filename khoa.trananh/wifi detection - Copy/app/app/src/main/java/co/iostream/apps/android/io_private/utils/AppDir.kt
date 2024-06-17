package co.iostream.apps.android.io_private.utils

import co.iostream.apps.android.core.iofile.FileUtils
import okio.withLock
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.io.path.Path
import kotlin.io.path.name

class AppDir {
    enum class Type {
        BaseFolder, LocalFolder, TemporaryFolder
    }

    companion object {
        private val _locker = ReentrantLock(true)

        private var _init = false
        private var _appBaseDir = String()

        fun initBase(baseDir: String) {
            _locker.withLock {
                if (_init) throw Exception("Could not init _appBaseDir more than once.")

                _appBaseDir = baseDir
                _init = true
            }
        }

        fun getAndCreate(create: Boolean, dirType: Type, vararg subDirs: String): Path {
            _locker.withLock {
                if (!_init) throw Exception("_appBaseDir has not been initialized.")
            }

            var path: Path

            path = when (dirType) {
                Type.BaseFolder -> Path(_appBaseDir)
                else -> {
                    val folder = when (dirType) {
                        Type.LocalFolder -> Path(_appBaseDir, "Local")
                        Type.TemporaryFolder -> Path(_appBaseDir, "Temp")
                        else -> Path(_appBaseDir)
                    }

                    Path(_appBaseDir, folder.name)
                }
            }

            for (i in subDirs) path = Path(path.toString(), i)

            if (create) {
                _locker.withLock {
                    FileUtils.createDirectoryIfNotExist(path.toString())
                }
            }

            return path
        }

        fun get(type: Type, vararg subDirs: String) =
            getAndCreate(true, type, *subDirs)      // Get AppDir

        fun pGet(type: Type, vararg subDirs: String) =
            getAndCreate(true, type, *subDirs)    // Get Package Dir

        fun getFilePath(type: Type, vararg segments: String): Path {
            return Path(
                getAndCreate(
                    true, type, *segments.take(segments.size - 1).toTypedArray()
                ).toString(), segments.last()
            )
        }

        fun pGetFilePath(type: Type, vararg segments: String): Path {
            return Path(
                getAndCreate(
                    true, type, *segments.take(segments.size - 1).toTypedArray()
                ).toString(), segments.last()
            )
        }
    }
}