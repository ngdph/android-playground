package co.iostream.apps.android.core.iofile

open class MediaFormat(family: MediaUtils.Family, name: String, var container: String, type: FileUtils.Type, var extensions: Array<String>) : FileFormat(name, type) {
    var family: MediaUtils.Family = family
        private set

    val extension: String?
        get() = extensions.firstOrNull()

    constructor(format: MediaFormat) : this(format.family, format.name, format.container, format.type, format.extensions.clone()) {
        this.family = format.family
        this.container = format.container
        this.extensions = format.extensions.clone()
    }
}


class MediaUtils {
    enum class Family {
        // Video
        V_3gp,
        V_Asf,
        V_Avi,
        V_Dat,
        V_Flv,
        V_Hevc,
        V_M2ts,
        V_M4v,
        V_Mkv,
        V_Mov,
        V_Mp4,
        V_Mxf,
        V_Vob,
        V_Ogg,
        V_Ogv,
        V_Rm,
        V_Swf,
        V_Ts,
        V_Mpeg,
        V_Webm,
        V_Wmv,

        // Audio
        // Lossy
        A_Aac,
        A_Ac3,
        A_Amr,
        A_Mp2,
        A_Mp3,
        A_Ogg,
        A_Opus,
        A_Oga,
        A_Spx,
        // Lossless
        A_Alac,
        //A_Cda,
        A_Flac,
        A_M4a,
        A_M4b,
        A_M4p,
        A_M4r,
        A_Mlp,
        A_Tta,
        A_Ra,
        A_Voc,
        A_Wv,
        // Uncompressed
        A_Aif,
        A_Aifc,
        A_Aiff,
        A_Au,
        A_Dsd,
        A_Dsf,
        A_Dts,
        A_Pcm,
        A_Wav,
        // Container
        A_Caf,
        //
        A_Weba,
        A_Wma,

        A_Midi
    }

    companion object {
        val MEDIA_FORMATS: HashMap<Family, MediaFormat> = hashMapOf(
            // Video
            Family.V_3gp to MediaFormat(Family.V_3gp, "3GP", "3gp", FileUtils.Type.Video, arrayOf(".3gp", ".3g2", ".3gpp", ".3gp2", ".3gpp2")),
            Family.V_Asf to MediaFormat(Family.V_Asf, "ASF", "asf", FileUtils.Type.Video, arrayOf(".asf")),
            Family.V_Avi to MediaFormat(Family.V_Avi, "AVI", "avi", FileUtils.Type.Video, arrayOf(".avi")),
            Family.V_Dat to MediaFormat(Family.V_Dat, "DAT", "dat", FileUtils.Type.Video, arrayOf(".dat")),
            Family.V_Flv to MediaFormat(Family.V_Flv, "Flash Video", "flv", FileUtils.Type.Video, arrayOf(".f4v", ".flv")),
            Family.V_Hevc to MediaFormat(Family.V_Hevc, "HEVC", "hevc", FileUtils.Type.Video, arrayOf(".hevc")),
            Family.V_M2ts to MediaFormat(Family.V_M2ts, "M2TS", "m2ts", FileUtils.Type.Video, arrayOf(".m2ts", ".mts")),
            Family.V_M4v to MediaFormat(Family.V_M4v, "M4V", "m4v", FileUtils.Type.Video, arrayOf(".m4v")),
            Family.V_Mkv to MediaFormat(Family.V_Mkv, "MKV", "mkv", FileUtils.Type.Video, arrayOf(".mkv")),
            Family.V_Mov to MediaFormat(Family.V_Mov, "Movie", "mov", FileUtils.Type.Video, arrayOf(".mov", ".qt")),
            Family.V_Mp4 to MediaFormat(Family.V_Mp4, "MP4", "mp4", FileUtils.Type.Video, arrayOf(".mp4", ".m4a", ".m4p", ".m4b", ".m4r")),
            Family.V_Mpeg to MediaFormat(Family.V_Mpeg, "MPEG", "mpeg", FileUtils.Type.Video, arrayOf(".mpeg", ".mpg")),
            Family.V_Mxf to MediaFormat(Family.V_Mxf, "MXF", "mxf", FileUtils.Type.Video, arrayOf(".mxf")),
            Family.V_Ogg to MediaFormat(Family.V_Ogg, "OGG", "ogg", FileUtils.Type.Video, arrayOf(".ogg", ".ogv", ".oga", ".ogx", ".ogm", ".spx", ".opus")),
            Family.V_Ogv to MediaFormat(Family.V_Ogv, "OGV", "ogv", FileUtils.Type.Video, arrayOf(".ogv")),
            Family.V_Rm to MediaFormat(Family.V_Rm, "RealMedia", "rm", FileUtils.Type.Video, arrayOf(".rm" , ".ra" , ".rmvb" , ".ram")),
            Family.V_Swf to MediaFormat(Family.V_Swf, "ShockWave Flash", "swf", FileUtils.Type.Video, arrayOf(".swf")),
            Family.V_Ts to MediaFormat(Family.V_Ts, "TS", "ts", FileUtils.Type.Video, arrayOf(".ts", ".tsv", ".tsa", ".m2t")),
            Family.V_Vob to MediaFormat(Family.V_Vob, "DVD Video Object File", "vob", FileUtils.Type.Video, arrayOf(".vob")),
            Family.V_Webm to MediaFormat(Family.V_Webm, "WEBM", "webm", FileUtils.Type.Video, arrayOf(".webm")),
            Family.V_Wmv to MediaFormat(Family.V_Wmv, "WMV", "wmv", FileUtils.Type.Video, arrayOf(".wmv", ".wm")),

            // Audio
            Family.A_Aac to MediaFormat(Family.A_Aac, "AAC", "aac", FileUtils.Type.Audio, arrayOf(".aac")),
            Family.A_Ac3 to MediaFormat(Family.A_Ac3, "AC3", "ac3", FileUtils.Type.Audio, arrayOf(".ac3")),
            Family.A_Aiff to MediaFormat(Family.A_Aiff, "AIFF", "aiff", FileUtils.Type.Audio, arrayOf(".aiff", ".aifc", ".aif")),
            Family.A_Amr to MediaFormat(Family.A_Amr, "AMR", "amr", FileUtils.Type.Audio, arrayOf(".amr")),
            Family.A_Au to MediaFormat(Family.A_Au, "AU", "au", FileUtils.Type.Audio, arrayOf(".au")),
            Family.A_Caf to MediaFormat(Family.A_Caf, "CAF", "caf", FileUtils.Type.Audio, arrayOf(".caf")),
            Family.A_Dsd to MediaFormat(Family.A_Dsd, "DSD Audio", "dsd", FileUtils.Type.Audio, arrayOf(".dff", ".dsf")),
            Family.A_Dts to MediaFormat(Family.A_Dts, "DTS", "dts", FileUtils.Type.Audio, arrayOf(".dts")),
            Family.A_Flac to MediaFormat(Family.A_Flac, "FLAC", "flac", FileUtils.Type.Audio, arrayOf(".flac")),
            Family.A_M4a to MediaFormat(Family.A_M4a, "M4A", "m4a", FileUtils.Type.Audio, arrayOf(".m4a")),
            Family.A_M4b to MediaFormat(Family.A_M4b, "M4B", "m4b", FileUtils.Type.Audio, arrayOf(".m4b")),
            Family.A_M4p to MediaFormat(Family.A_M4p, "M4P", "m4p", FileUtils.Type.Audio, arrayOf(".m4p")),
            Family.A_M4r to MediaFormat(Family.A_M4r, "M4R", "m4r", FileUtils.Type.Audio, arrayOf(".m4r")),
            Family.A_Midi to MediaFormat(Family.A_Midi, "Musical Instrument Digital Interface", "midi", FileUtils.Type.Audio, arrayOf(".mid", ".midi")),
            Family.A_Mlp to MediaFormat(Family.A_Mlp, "Meridian Lossless Packing", "mlp", FileUtils.Type.Audio, arrayOf(".mlp")),
            Family.A_Mp2 to MediaFormat(Family.A_Mp2, "MP2", "mp2", FileUtils.Type.Audio, arrayOf(".mp2")),
            Family.A_Mp3 to MediaFormat(Family.A_Mp3, "MP3", "mp3", FileUtils.Type.Audio, arrayOf(".mp3")),
            Family.A_Ogg to MediaFormat(Family.A_Ogg, "OGG", "ogg", FileUtils.Type.Audio, arrayOf(".ogg", ".oga", ".opus", ".spx")),
            Family.A_Opus to MediaFormat(Family.A_Opus, "OPUS", "opus", FileUtils.Type.Audio, arrayOf(".opus")),
            Family.A_Tta to MediaFormat(Family.A_Tta, "TTA", "tta", FileUtils.Type.Audio, arrayOf(".tta")),
            Family.A_Voc to MediaFormat(Family.A_Voc, "VOC", "voc", FileUtils.Type.Audio, arrayOf(".voc")),
            Family.A_Wav to MediaFormat(Family.A_Wav, "WAV", "wav", FileUtils.Type.Audio, arrayOf(".wav")),
            Family.A_Weba to MediaFormat(Family.A_Weba, "WEBA", "weba", FileUtils.Type.Audio, arrayOf(".weba")),
            Family.A_Wma to MediaFormat(Family.A_Wma, "WMA", "wma", FileUtils.Type.Audio, arrayOf(".wma")),
            Family.A_Wv to MediaFormat(Family.A_Wv, "WV", "wv", FileUtils.Type.Audio, arrayOf(".wv"))
        )

        val VIDEO_FORMATS: MutableMap<Family, MediaFormat> = mutableMapOf()
        val AUDIO_FORMATS: MutableMap<Family, MediaFormat> = mutableMapOf()
        val CONTAINERS: MutableMap<String, String> = mutableMapOf()

        init {
            for ((key, value) in MEDIA_FORMATS) {
                if (value.type == FileUtils.Type.Video) {
                    VIDEO_FORMATS[key] = value
                } else if (value.type == FileUtils.Type.Audio) {
                    AUDIO_FORMATS[key] = value
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