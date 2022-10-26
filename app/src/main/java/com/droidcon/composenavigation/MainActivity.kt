package com.droidcon.composenavigation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.droidcon.composenavigation.ui.theme.ComposeNavigationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var screenToShow: @Composable () -> Unit by remember { mutableStateOf({ Greeting("Nick!") }) }

            ComposeNavigationTheme {
                // A surface container using the 'background' color from the theme
                Scaffold(
                    topBar = {
                        Button(onClick = { screenToShow = { ShowSiren("123") } }) {
                            Text("Show Siren!")
                        }
                    }
                ) {
                    screenToShow()
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Composable
fun ShowSiren(name: String) {
    Text(text = "Here's the Siren!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ComposeNavigationTheme {
        Greeting("Android")
    }
}