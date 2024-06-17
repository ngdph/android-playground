package co.iostream.apps.android.data.helper

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import kotlin.io.path.Path

class JsonUtils private constructor() {
    companion object {
        inline fun <reified T> encode(data: T): String {
            return Json.encodeToString(data)
        }

        inline fun <reified T> decode(str: String): T {
            return Json.decodeFromString(str)
        }

        inline fun <reified T> save(items: T, path: String) {
            val jsonData = Json.encodeToString(items).toByteArray(Charsets.UTF_8)
            Files.write(Path(path), jsonData)
        }

        inline fun <reified T> load(path: String): T? {
            if (path.isEmpty() || !Files.exists(Path(path))) return null

            return try {
                val jsonData = Files.readAllBytes(Path(path)).toString(Charsets.UTF_8)
                Json.decodeFromString<T>(jsonData)
            } catch (e: Exception) {
                null
            }
        }
    }
}