package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
//import androidx.compose.material.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                AndroidAliens()
            }
        }
    }
}

@Composable
fun AndroidAlien(
    color: Color,
    modifier: Modifier = Modifier
) {
    Image(
        modifier = modifier,
        painter = painterResource(id = R.drawable.android_alien),
        contentDescription = "This is an alien", // Provide proper content description here
        colorFilter = ColorFilter.tint(color = color)
    )
    //Text("This is an alien")
}

@Composable
fun AndroidAliens() {
    Column {
        AndroidAlien(
            color = Color.Red,
            modifier = Modifier
                .size(100.dp)
                .padding(4.dp)
        )
        AndroidAlien(
            color = Color.Black,
            modifier = Modifier
                .size(100.dp)
                .padding(4.dp)
        )
    }
}

@Preview
@Composable
fun PreviewAndroidAliens() {
    MyApplicationTheme {
        AndroidAliens()
    }
}
