package co.iostream.apps.android.io_private.screens.misc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage

@Composable
fun ImageViewerScreen(
    imageUri: String,
    onDismiss: () -> Unit = {},
    onImageClick: () -> Unit = {},
) {
    Box {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() }
                .background(Color.DarkGray.copy(alpha = 0.6f))
        ) {
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
            )
//            Image(
//                painter = rememberAsyncImagePainter(
//                    ImageRequest.Builder(LocalContext.current).data(data = imageUrl).apply(block = fun ImageRequest.Builder.() {
//                        placeholder(R.drawable.baseline_broken_image_24)
//                    }).build()
//                ),
//                contentDescription = null,
//                modifier = Modifier.clickable { onImageClick() }
//            )
        }
    }
}