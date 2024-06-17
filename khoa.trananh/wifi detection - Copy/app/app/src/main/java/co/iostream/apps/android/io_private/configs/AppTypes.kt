package co.iostream.apps.android.io_private.configs

import co.iostream.apps.android.io_private.R

class AppTypes {
    companion object {
        val DOCS: Map<String, String> = mapOf()

        val FILES: Map<FileType, Int> = mapOf(
            FileType.All to R.string.core_all,
            FileType.Video to R.string.core_video,
            FileType.Audio to R.string.core_audio,
            FileType.Image to R.string.core_image,
        )

        val EXPORTS: Map<ExportType, Int> = mapOf(
            ExportType.Export to R.string.export,
            ExportType.ExportUnlockedCopy to R.string.export_unlocked_copy,
            ExportType.ExportLockedCopy to R.string.export_locked_copy,
        )
        val SORT_TYPES: Map<SortType, Int> = mapOf(
            SortType.AZ to R.string.a2z,
            SortType.ZA to R.string.z2a,
            SortType.Newest to R.string.newest,
            SortType.Oldest to R.string.oldest
        )
    }

    class FileTypeRecord(var isSupported: Boolean, var extensions: Array<String>)

    enum class FileType {
        All, Video, Audio, Image, Document, Pdf,
    }
    enum class SortType {
        AZ, ZA, Newest, Oldest,
    }

    enum class ExportType {
        Export, ExportUnlockedCopy, ExportLockedCopy,
    }

    enum class PasswordType {
        NotSet, Text, PIN,
    }
}