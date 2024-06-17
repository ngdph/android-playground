package co.iostream.apps.android.io_private.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.iostream.apps.android.io_private.ui.theme.Foreground2

@Composable
fun HintView(
    hintIconPainter: Painter,
    title: String,
    subTitle: String
){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    )
    {
        Icon(
            painter = hintIconPainter,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Foreground2
        )
        Text(
            text = title,
            style = TextStyle(
                color = MaterialTheme.colorScheme.outline,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 15.dp)
        )
        Text(
            text = subTitle,
            style = TextStyle(
                color = Foreground2,
                fontSize = 14.sp,
                textAlign = TextAlign.Center),
            maxLines = 2,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}