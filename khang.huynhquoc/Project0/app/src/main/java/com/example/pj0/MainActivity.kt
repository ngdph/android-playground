package com.example.pj0
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            Greeting("Khang")
            GreetingText("Hello World","hi")

        }
    }
}

@Composable
fun Greeting(name:String) {
    Column(modifier = Modifier.padding(start = 120.dp)) {
        Spacer(modifier = Modifier.height(400.dp))
        Text(" Happy\n Birthday \n $name!",
            style = TextStyle(
                fontSize = 50.sp, // Thay đổi kích thước văn bản ở đây
                lineHeight = 100.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                //textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
fun GreetingText(message: String,from:String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        fontSize = 100.sp,
        lineHeight = 116.sp,
    )
    Text(
        text=from,
        fontSize = 100.sp ,
        lineHeight = 200.sp,
    )
}





