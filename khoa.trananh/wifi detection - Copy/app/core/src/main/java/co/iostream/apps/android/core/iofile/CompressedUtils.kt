package co.iostream.apps.android.core.iofile

open class CompressedFormat(
    name: String,
    var container: String,
    type: FileUtils.Type,
    var extensions: Array<String>
) : FileFormat(name, type) {
    val extension: String?
        get() = extensions.firstOrNull()

    constructor(compressedFormat: CompressedFormat) : this(
        compressedFormat.name,
        compressedFormat.container,
        compressedFormat.type,
        compressedFormat.extensions.clone()
    ) {
        this.container = compressedFormat.container
        this.extensions = compressedFormat.extensions.clone()
    }
}

class CompressedUtils {
    enum class Family {
        Rar,
        Zip,
        SevenZip,
        Arj,
        Bz2,
        Cab,
        Gz,
        Iso,
        Jar,
        Lz,
        Lzh,
        Tar,
        Uue,
        Xy,
        Z,
        Zipx,
        Zst
    }

    companion object {
        val COMPRESSED_FORMATS: HashMap<Family, CompressedFormat> = hashMapOf(
            Family.Rar to CompressedFormat(
                "RAR",
                "rar",
                FileUtils.Type.Compressed,
                arrayOf(".rar")
            ),
            Family.Zip to CompressedFormat(
                "ZIP",
                "zip",
                FileUtils.Type.Compressed,
                arrayOf(".zip")
            ),
            Family.SevenZip to CompressedFormat(
                "SEVENZIP",
                "txt",
                FileUtils.Type.Compressed,
                arrayOf(".7z")
            ),
            Family.Arj to CompressedFormat(
                "ARJ",
                "arj",
                FileUtils.Type.Compressed,
                arrayOf(".arj")
            ),
            Family.Bz2 to CompressedFormat(
                "BZ2",
                "bz2",
                FileUtils.Type.Compressed,
                arrayOf(".bz2")
            ),
            Family.Cab to CompressedFormat(
                "CAB",
                "cab",
                FileUtils.Type.Compressed,
                arrayOf(".cab")
            ),
            Family.Gz to CompressedFormat("GZ", "gz", FileUtils.Type.Compressed, arrayOf(".gz")),
            Family.Iso to CompressedFormat(
                "ISO",
                "iso",
                FileUtils.Type.Compressed,
                arrayOf(".iso")
            ),
            Family.Jar to CompressedFormat(
                "JAR",
                "jar",
                FileUtils.Type.Compressed,
                arrayOf(".jar")
            ),
            Family.Lz to CompressedFormat("LZ", "lz", FileUtils.Type.Compressed, arrayOf(".lz")),
            Family.Lzh to CompressedFormat(
                "LZH",
                "lzh",
                FileUtils.Type.Compressed,
                arrayOf(".lzh", "lha")
            ),
            Family.Tar to CompressedFormat(
                "TAR",
                "tar",
                FileUtils.Type.Compressed,
                arrayOf(".tar")
            ),
            Family.Uue to CompressedFormat(
                "UUE",
                "uue",
                FileUtils.Type.Compressed,
                arrayOf(".uue")
            ),
            Family.Xy to CompressedFormat("XY", "xy", FileUtils.Type.Compressed, arrayOf(".xy")),
            Family.Z to CompressedFormat("Z", "z", FileUtils.Type.Compressed, arrayOf(".z")),
            Family.Zipx to CompressedFormat(
                "ZIPX",
                "zipx",
                FileUtils.Type.Compressed,
                arrayOf(".zipx")
            ),
            Family.Zst to CompressedFormat("ZST", "zst", FileUtils.Type.Compressed, arrayOf(".zsr"))
        )

        val TEXT_COMPRESSED_FORMATS: MutableMap<Family, CompressedFormat> = mutableMapOf()
        val CONTAINERS: MutableMap<String, String> = mutableMapOf()

        init {
            for ((key, value) in COMPRESSED_FORMATS) {
                if (value.type == FileUtils.Type.Compressed) {
                    TEXT_COMPRESSED_FORMATS[key] = value
                }

                for (extension in value.extensions) {
                    if (!CONTAINERS.containsKey(extension)) {
                        CONTAINERS[extension] = value.container
                    }
                }
            }
        }
    }
}