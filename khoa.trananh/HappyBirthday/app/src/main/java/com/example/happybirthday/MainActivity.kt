package com.example.happybirthday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.happybirthday.ui.theme.HappyBirthdayTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HappyBirthdayTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen()
                }
            }
        }
    }
}

@Composable
fun Greeting(message: String, from: String, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Text(
            style = TextStyle(color = Color.Black),
            text = "The greatest $message",
            fontSize = 80.sp,
            lineHeight = 90.sp,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Cursive
        )
        Text(
            text = from,
            fontSize = 25.sp,
            fontFamily = FontFamily.Cursive,
            modifier = Modifier
                .padding(16.dp)
                .align(alignment = Alignment.End)
        )
    }
}

@Composable
fun HomeScreen() {
    Column(modifier = Modifier.padding(24.dp)) {
        GreetingText()
        Spacer(modifier = Modifier.height(14.dp))
        PainterCompose()
        Spacer(modifier = Modifier.height(14.dp))
        Greeting("Uchiha", " who sacrificed himself")
        Spacer(modifier = Modifier.height(14.dp))
        OnClickButton()
    }
}

@Composable
fun GreetingText() {
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.app_name),
            color = Color.Red,
            fontSize = 45.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Cursive,
            //maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PainterCompose() {
    Image(
        painter = painterResource(id = R.drawable.itachi),
        contentDescription = null,
        modifier = Modifier
            .clip(RectangleShape) // First clip to circle
            .border(
                BorderStroke(width = 20.dp, color = Color.Unspecified),
                shape = RectangleShape
            ) // Then apply border
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(8.dp)
            )
    )
}
@Composable
fun OnClickButton() {
    Button(onClick ={},
        colors = ButtonDefaults.buttonColors(Color.Black)) {
        Icon( Icons.Default.Search, contentDescription = null)
        Text(" For more details about the legend ...")
    }
}

//@Composable
//fun VectorCompose() {
//    Image(imageVector = Icons.Filled.Person, contentDescription = null )
//}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HappyBirthdayTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            HomeScreen()
        }
    }
}
