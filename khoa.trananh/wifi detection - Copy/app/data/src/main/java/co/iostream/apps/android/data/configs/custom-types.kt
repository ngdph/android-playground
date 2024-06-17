package co.iostream.apps.android.data.configs

enum class FileItemType {
    FILE, DIRECTORY
}

enum class OutputExtension {
    ORIGINAL, AI, BIN, CPP, DLL, LIB, OBJ, PSD, RAW
}

val OUTPUT_EXTENSION = mapOf(
    OutputExtension.ORIGINAL to "original",
    OutputExtension.AI to "ai",
    OutputExtension.BIN to "bin",
    OutputExtension.CPP to "cpp",
    OutputExtension.DLL to "dll",
    OutputExtension.LIB to "lib",
    OutputExtension.OBJ to "obj",
    OutputExtension.PSD to "psd",
    OutputExtension.RAW to "raw"
)