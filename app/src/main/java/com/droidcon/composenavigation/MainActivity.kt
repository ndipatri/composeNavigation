package com.droidcon.composenavigation

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.droidcon.composenavigation.ui.theme.ComposeNavigationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var screenToShow: @Composable () -> Unit by remember { mutableStateOf({ Greeting("Nick!") }) }

            ComposeNavigationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column {
                        Button(onClick = { screenToShow = { StartSiren("") } }) {
                            Text("Start Siren!")
                        }
                        Button(onClick = { screenToShow = { SeeSiren(LocalContext.current.applicationContext) } }) {
                            Text("See the Siren!")
                        }

                        screenToShow()
                    }
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
fun StartSiren(name: String) {
    Text(text = "Starting the Siren...")
}

@Composable
fun SeeSiren(context: Context) {
    AndroidView(factory = {
        WebView(context).apply {
            webViewClient = WebViewClient()

            loadUrl("https://cnn.com")
        }
    })
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ComposeNavigationTheme {
        Greeting("Android")
    }
}