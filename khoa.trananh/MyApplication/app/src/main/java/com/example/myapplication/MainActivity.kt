package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                AndroidAliensColumns()
                AndroidAliensRows()
                Greeting("Android")
            }
        }
    }
}
@Composable
fun Greeting(name: String) {
    Surface(color = Color.Magenta) {
        Text(text = "Hi, my name is $name!", Modifier.padding(5.dp))
    }
}
@Composable
fun ReGreeting() {
    Greeting(name = "Anh Khoa")
}
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        Greeting("Meghan")
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
fun AndroidAliensColumns() {
    Column (
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        AndroidAlien(
            color = Color.Red,
            modifier = Modifier
                .size(100.dp)
                .padding(4.dp)
        )
        Spacer(Modifier.size(16.dp))
        AndroidAlien(
            color = Color.Black,
            modifier = Modifier.size(100.dp).padding(4.dp).background(Color.Blue)
            // can invoke many consecutive methods()
        )
    }
}
@Composable
fun AndroidAliensRows() {
    Row (
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top
    ){
        AndroidAlien(
            color = Color.Green,
            modifier = Modifier
                .size(100.dp)
                .padding(4.dp)
        )
        //Spacer(Modifier.size(16.dp)) ???
        Spacer(modifier = Modifier.size(16.dp))
        AndroidAlien(
            color = Color.Yellow,
            modifier = Modifier
                .size(100.dp)
                .padding(4.dp)
        )
    }
}

/*@Preview
@Composable
fun PreviewAndroidAliens() {
    MyApplicationTheme {
        AndroidAliensColumns()
        AndroidAliensRows()
    }
}*/
