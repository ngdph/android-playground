package com.example.nghich1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.nghich1.ui.theme.Nghich1Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Nghich1Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("huy")
                    Multiplestyle()
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name",
        modifier = modifier,
        color = Color.Red,
        fontSize = 50.sp,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight(weight = 200),
        textAlign = TextAlign.Center,
        fontFamily = FontFamily.Cursive,
        textDecoration = TextDecoration.Underline,
        maxLines = 1
    )
}

@Composable
fun Multiplestyle() {
    Text(text = buildAnnotatedString {
        withStyle(style = SpanStyle(color = Color.Blue)){
            append("H")
        }
        append("ello")
        withStyle(style = SpanStyle(color = Color.Green)){
            append("H")
        }
        append("uy")
    })
}
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Nghich1Theme {
        Greeting("huy")
        Multiplestyle()
    }
}

