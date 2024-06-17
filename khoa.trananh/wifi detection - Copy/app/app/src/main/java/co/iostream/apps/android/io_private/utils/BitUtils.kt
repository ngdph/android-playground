package co.iostream.apps.android.io_private.utils

import java.io.RandomAccessFile

class BitUtils private constructor() {
    companion object {

        fun littleEndianConversion(bytes: ByteArray): Int {
            var result = 0
            for (i in bytes.indices) {
                result = result or (bytes[i].toInt() shl 8 * i)
            }

            return result
        }

        fun removeLatestBytesFromFile(filePath: String, size: Long) {
            var raf: RandomAccessFile? = null

            try {
                raf = RandomAccessFile(filePath, "rwd")
                raf.seek(raf.length() - size)
                raf.setLength(raf.length() - size)
            } catch (e: Exception) {
                println(e.message)
            } finally {
                raf?.close()
            }
        }

        fun write4BytesToBuffer(buffer: ByteArray, offset: Int, data: Int) {
            buffer[offset + 0] = (data shr 0).toByte()
            buffer[offset + 1] = (data shr 8).toByte()
            buffer[offset + 2] = (data shr 16).toByte()
            buffer[offset + 3] = (data shr 24).toByte()
        }

        fun read4BytesFromBuffer(buffer: ByteArray, offset: Int): Int {
            return (buffer[offset + 3].toInt() shl 24) or
                    (buffer[offset + 2].toInt() and 0xff shl 16) or
                    (buffer[offset + 1].toInt() and 0xff shl 8) or
                    (buffer[offset + 0].toInt() and 0xff)
        }
    }
}