package co.iostream.apps.android.data.helper

import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
import java.util.*

class LocaleUtils private constructor() {
    companion object {
        fun getAvailableLocales(): List<Locale> {
            var locales = emptyList<Locale>()
            val llc = ConfigurationCompat.getLocales(Resources.getSystem().configuration)

            for (i in 0 until llc.size()) {
                llc.get(i)?.let { locales = locales.plus(it) }
            }

            return locales
        }
    }
}