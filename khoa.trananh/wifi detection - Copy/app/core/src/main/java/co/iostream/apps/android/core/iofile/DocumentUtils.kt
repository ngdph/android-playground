package co.iostream.apps.android.core.iofile

open class DocumentFormat(
    name: String,
    var container: String,
    type: FileUtils.Type,
    var extensions: Array<String>
) : FileFormat(name, type) {
    val extension: String?
        get() = extensions.firstOrNull()

    constructor(documentFormat: DocumentFormat) : this(
        documentFormat.name,
        documentFormat.container,
        documentFormat.type,
        documentFormat.extensions.clone()
    ) {
        this.container = documentFormat.container
        this.extensions = documentFormat.extensions.clone()
    }
}

class DocumentUtils {
    enum class Family {
        // Text
        T_Doc,
        T_Pdf,
        T_Txt,
        T_Rtf,
        T_Odt,
        T_Pages,

        // Presentation
        P_Ppt,
        P_Key,
        P_Odp,

        // Sheet
        S_Xls,
        S_Ods,

        // Ebook
        EB_Epub,
        EB_Mobi,
        EB_Azw,
        EB_Fb2
    }

    companion object {
        val DOCUMENT_FORMATS: HashMap<Family, DocumentFormat> = hashMapOf(
            // Text
            Family.T_Doc to DocumentFormat(
                "DOC",
                "doc",
                FileUtils.Type.Document,
                arrayOf(".doc", ".docx", ".docm")
            ),
            Family.T_Pdf to DocumentFormat("PDF", "pdf", FileUtils.Type.Document, arrayOf(".pdf")),
            Family.T_Txt to DocumentFormat(
                "TXT",
                "txt",
                FileUtils.Type.Document,
                arrayOf(".txt", ".text", ".tex")
            ),
            Family.T_Rtf to DocumentFormat("RTF", "rtf", FileUtils.Type.Document, arrayOf(".rtf")),
            Family.T_Odt to DocumentFormat("ODT", "odt", FileUtils.Type.Document, arrayOf(".odt")),
            Family.T_Pages to DocumentFormat(
                "pages",
                "pages",
                FileUtils.Type.Document,
                arrayOf(".pages")
            ),

            // Presentation
            Family.P_Ppt to DocumentFormat(
                "PPT",
                "ppt",
                FileUtils.Type.Document,
                arrayOf(".ppt", ".pptx", ".pptm")
            ),
            Family.P_Key to DocumentFormat("KEY", "key", FileUtils.Type.Document, arrayOf(".key")),
            Family.P_Odp to DocumentFormat("ODP", "odp", FileUtils.Type.Document, arrayOf(".odp")),

            // Sheet
            Family.S_Xls to DocumentFormat(
                "XLS",
                "xls",
                FileUtils.Type.Document,
                arrayOf(".xls", ".xlsx", ".xlsm")
            ),
            Family.S_Ods to DocumentFormat("ODS", "ods", FileUtils.Type.Document, arrayOf(".ods")),

            // Ebook
            Family.EB_Epub to DocumentFormat(
                "EPUB",
                "epub",
                FileUtils.Type.Document,
                arrayOf(".epub")
            ),
            Family.EB_Mobi to DocumentFormat(
                "MOBI",
                "mobi",
                FileUtils.Type.Document,
                arrayOf(".mobi")
            ),
            Family.EB_Azw to DocumentFormat(
                "AZW",
                "azw",
                FileUtils.Type.Document,
                arrayOf(".azw", ".azw3")
            ),
            Family.EB_Fb2 to DocumentFormat("FB2", "fb2", FileUtils.Type.Document, arrayOf(".fb2"))
        )

        val TEXT_DOCUMENT_FORMATS: MutableMap<Family, DocumentFormat> = mutableMapOf()
        val CONTAINERS: MutableMap<String, String> = mutableMapOf()

        init {
            for ((key, value) in DOCUMENT_FORMATS) {
                if (value.type == FileUtils.Type.Document) {
                    TEXT_DOCUMENT_FORMATS[key] = value
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
