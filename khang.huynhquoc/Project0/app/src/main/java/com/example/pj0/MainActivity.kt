package com.example.pj0
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.pj0.ui.theme.Pj0Theme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Pj0Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ){
                    HomeScreen("Gvardiol")
                }
                Spacer(modifier = Modifier.height(10.dp))
                ScreenImage(contentScale = ContentScale.Fit)
            }
        }
    }
}

@Composable
fun HomeScreen(name:String) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Center
    ) {
        MyButton()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        )
        {
            GreetingText(name)

        }
    }
    Column(
        //verticalArrangement = Arrangement.Bottom
        modifier = Modifier
            .fillMaxHeight()
            .padding(top = 700.dp),
        //verticalArrangement = Arrangement.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center
        ) {
            AVT()
        }
    }

}

val custom1: TextStyle
    get() = TextStyle(
        fontSize = 30.sp,
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Serif,
        lineHeight = 50.sp,
        color = Color.Blue,
        textAlign = TextAlign.Start,
        textDecoration = TextDecoration.Underline
        )

@Composable
fun GreetingText(message: String) {
    Text(" The best\n CB \n $message!",
        style = custom1
    )
}
@Composable
fun ScreenImage(contentScale : ContentScale){
    Surface(
        modifier = Modifier
            .border(
                BorderStroke(
                    5.dp,
                    color = Color.DarkGray
                ),
                shape = RectangleShape
            )
            .clip(shape = RectangleShape)

    )
    {
        Image(
            painterResource(id = R.drawable.gvardiol),
            contentDescription = "Idol",
            //modifier = Modifier.size(300.dp),
            contentScale = contentScale
        )
    }
}

@Composable
fun AVT(){
    Surface(
        modifier = Modifier
            .border(
                BorderStroke(
                    5.dp,
                    color = Color.Black
                ),
                shape = CircleShape
            )
            .clip(shape = CircleShape)

    )
    {
        Image(
            painterResource(id = R.drawable.logo),
            contentDescription = "Mancity",
            modifier = Modifier.size(150.dp)
        )
    }
}

@Composable
fun MyButton(){
    Button(
        onClick = {},
        //modifier = Modifier.width(50.dp).height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black,
            contentColor = Color.Yellow,
            disabledContentColor = Color.White,
            disabledContainerColor = Color.Cyan
        )
    ){
        Icon(Icons.Default.Warning,"")
        Text("Click me",
            fontSize = 30.sp,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            lineHeight = 50.sp,
        )
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Pj0Theme {
        HomeScreen("Khang")
    }
}

@Composable
fun MyApp() {
    // Tạo NavController ở cấp cao trong cấu trúc phân cấp composable
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        //composable("home") { HomeScreen(navController) }
        //composable("details") { DetailsScreen(navController) }
    }
}


