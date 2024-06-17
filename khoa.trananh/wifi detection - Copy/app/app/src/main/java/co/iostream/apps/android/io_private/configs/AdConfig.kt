package co.iostream.apps.android.io_private.configs

import co.iostream.apps.android.io_private.BuildConfig

class AdConfig private constructor() {
    enum class SampleAdUnit(val id: String) {
        APP_OPEN("ca-app-pub-3940256099942544/3419835294"),
        BANNER("ca-app-pub-3940256099942544/6300978111"),
        INTERSTITIAL("ca-app-pub-3940256099942544/1033173712"),
        INTERSTITIAL_VIDEO("ca-app-pub-3940256099942544/8691691433"),
        REWARDED("ca-app-pub-3940256099942544/5224354917"),
        REWARDED_INTERSTITIAL("ca-app-pub-3940256099942544/5354046379"),
        NATIVE_ADVANCED("ca-app-pub-3940256099942544/2247696110"),
        NATIVE_ADVANCED_VIDEO("ca-app-pub-3940256099942544/1044960115")
    }

    enum class AdType {
        APP_OPEN,
        BANNER,
        INTERSTITIAL,
        INTERSTITIAL_VIDEO,
        REWARDED,
        REWARDED_INTERSTITIAL,
        NATIVE_ADVANCED,
        NATIVE_ADVANCED_VIDEO
    }

    companion object {
        private fun getAd(unitId: String, adType: AdType): String {
            if (!BuildConfig.DEBUG) return unitId

            return when (adType) {
                AdType.APP_OPEN -> SampleAdUnit.APP_OPEN.id
                AdType.BANNER -> SampleAdUnit.BANNER.id
                AdType.INTERSTITIAL -> SampleAdUnit.INTERSTITIAL.id
                AdType.INTERSTITIAL_VIDEO -> SampleAdUnit.INTERSTITIAL_VIDEO.id
                AdType.REWARDED -> SampleAdUnit.REWARDED.id
                AdType.REWARDED_INTERSTITIAL -> SampleAdUnit.REWARDED_INTERSTITIAL.id
                AdType.NATIVE_ADVANCED -> SampleAdUnit.NATIVE_ADVANCED.id
                AdType.NATIVE_ADVANCED_VIDEO -> SampleAdUnit.NATIVE_ADVANCED_VIDEO.id
            }
        }

        val MAIN_BOTTOM = getAd("ca-app-pub-6817535307616905/3922014629", AdType.BANNER)
        val BATCH_BOTTOM = getAd("ca-app-pub-6817535307616905/7669687949", AdType.BANNER)
    }
}