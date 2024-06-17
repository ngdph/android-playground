package co.iostream.apps.android.io_private.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import co.iostream.apps.android.io_private.R
import co.iostream.apps.android.io_private.configs.AdConfig

@Composable
fun BannerAdView(adUnitId: String = AdConfig.SampleAdUnit.BANNER.id) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.advertisement),
            modifier = Modifier.padding(4.dp, 4.dp)
        )

        Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(4.dp))

        Row(modifier = Modifier.height(AdSize.BANNER.height.dp)) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                factory = { context ->
                    AdView(context).apply {
                        this.setAdSize(AdSize.BANNER)
                        this.adUnitId = adUnitId
                        this.loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
    }
}