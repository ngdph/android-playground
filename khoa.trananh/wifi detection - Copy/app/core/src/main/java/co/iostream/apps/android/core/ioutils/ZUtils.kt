package co.iostream.apps.android.core.ioutils

import androidx.annotation.Nullable
import kotlin.reflect.full.isSubclassOf

class ZUtils {
    companion object {
        inline fun <reified T> isEnum(): Boolean = T::class.isSubclassOf(Enum::class)

        inline fun <reified T : Enum<T>> changeEnumType(value: Any?): T? {
            return try {
                if (T::class.java.isEnum) {
                    val res = enumValueOf<T>(value as String)
                    if (enumValues<T>().contains(res)) res else null
                } else {
                    if (T::class.isSubclassOf(Nullable::class)) {
                        value?.let { it as T }
                    } else {
                        value as T
                    }
                }
            } catch (e: Exception) {
                null
            }
        }

        inline fun <reified T> changeType(value: Any?): T? {
            return try {
                if (T::class.isSubclassOf(Nullable::class)) {
                    value?.let { it as T }
                } else {
                    value as T
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}