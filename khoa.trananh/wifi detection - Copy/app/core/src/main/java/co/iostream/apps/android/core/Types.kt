package co.iostream.apps.android.core

import android.util.Size
import kotlinx.serialization.Serializable

@Serializable
class IOSize<T : Comparable<T>> {
    var w: T? = null
    var h: T? = null

    constructor()

    constructor(w: T, h: T) {
        this.w = w
        this.h = h
    }

    fun set(w: T, h: T) {
        this.w = w
        this.h = h
    }
}
