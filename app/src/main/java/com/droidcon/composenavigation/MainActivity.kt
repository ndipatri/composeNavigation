package com.droidcon.composenavigation

import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.droidcon.composenavigation.ui.theme.ComposeNavigationTheme
import com.google.gson.annotations.SerializedName
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            var sirenShouldBeOn by rememberSaveable { mutableStateOf(false) }

            ComposeNavigationTheme {
                Surface(
                    color = MaterialTheme.colors.background
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { navController.navigateSingle("greeting") }) {
                                Text("Greeting")
                            }
                            Button(onClick = { navController.navigateSingle("configure") }) {
                                Text("Configure")
                            }
                            Button(onClick = { navController.navigateSingle("show_siren/$sirenShouldBeOn", shouldRestore = false) }) {
                                Text("Show Siren (${if (sirenShouldBeOn) "on" else "off"})")
                            }
                        }
                        NavHost(
                            navController = navController,
                            startDestination = "greeting"
                        ) {
                            composable(route = "greeting") {
                                Greeting()
                            }
                            composable(route = "configure") {
                                Configure(sirenShouldBeOn = sirenShouldBeOn,
                                          onSirenShouldBeOn = { _sirenShouldBeOn -> sirenShouldBeOn = _sirenShouldBeOn })

                            }
                            composable(route = "show_siren/{shouldBeOn}",
                                arguments = listOf(navArgument("shouldBeOn") {
                                    type = NavType.BoolType
                                })
                            ) {
                                ShowSiren(it.arguments?.getBoolean("shouldBeOn") ?: false)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun NavHostController.navigateSingle(route: String, shouldRestore: Boolean = true) =
    this.navigate(route) {
        popUpTo("greeting") {
            saveState = true
        }
        launchSingleTop = true
        restoreState = shouldRestore
    }


@Composable
fun Greeting() {
    Column {
        Text(text = "Greetings! Check out my cool siren!")
    }
}

@Composable
fun Configure(sirenShouldBeOn: Boolean, onSirenShouldBeOn: (Boolean) -> Unit) {
    Column {
        Text(text = "This is the 'Configure' Screen")

        Row {
            Text(text = "Siren should be on!")
            Checkbox(
                checked = sirenShouldBeOn,
                onCheckedChange = { checked -> onSirenShouldBeOn(checked) })
        }
    }
}


@Composable
fun ShowSiren(shouldBeOn: Boolean) {
    Column {
        Text(text = "This is the 'Show Siren' Screen (${if (shouldBeOn) "on" else "off"})")

        LiveRedSirenVideoPlayer()

        LaunchedEffect(Unit) {
            if (shouldBeOn) {
                withContext(Dispatchers.IO) {
                    particleInterface.turnOnRedSiren().execute()
                    delay(500)
                    particleInterface.turnOffRedSiren().execute()
                }
            }
        }
    }
}


val particleInterface: ParticleRESTInterface by lazy {

    val okHttpClient = OkHttpClient()

    Retrofit.Builder().apply {
        baseUrl("https://api.particle.io/")
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
    }
        .build()
        .create(ParticleRESTInterface::class.java)
}


interface ParticleRESTInterface {
    @GET("/v1/devices?access_token=$particleToken")
    suspend fun getDevices(): List<SimpleDevice>

    @FormUrlEncoded
    @POST("/v1/devices/{deviceId}/sirenOn")
    fun turnOnRedSiren(
        @Path("deviceId") deviceId: String = "e00fce6829afbb59f9e1a1fb",
        @Field("arg") updateState: String = "",
        @Field("access_token") accessToken: String = particleToken
    ): Call<Unit>

    @FormUrlEncoded
    @POST("/v1/devices/{deviceId}/sirenOff")
    fun turnOffRedSiren(
        @Path("deviceId") deviceId: String = "e00fce6829afbb59f9e1a1fb",
        @Field("arg") updateState: String = "",
        @Field("access_token") accessToken: String = particleToken
    ): Call<Unit>
}

const val particleToken = "78116e05f59f44d8142c22a86216d8103aa7bdec"


@Composable
@OptIn(UnstableApi::class)
fun LiveRedSirenVideoPlayer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(175.dp)
            .width(300.dp),
    ) {

        val context = LocalContext.current

        val exoPlayer = remember {
            ExoPlayer.Builder(context)
                .build()
                .apply {

                    val rtspMediaSource: MediaSource = RtspMediaSource.Factory()
                        .createMediaSource(MediaItem.fromUri("rtsp://ndipatri:ndipatri@10.0.0.21:554/stream1"))

                    setMediaSource(rtspMediaSource)
                    addAnalyticsListener(EventLogger(null, "exo"))
                    prepare()
                }
        }

        exoPlayer.playWhenReady = true
        //exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE

        DisposableEffect(
            AndroidView(factory = {
                PlayerView(context).apply {
                    hideController()
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }
            })
        ) {
            onDispose { exoPlayer.release() }
        }
    }
}

data class SimpleDevice(
    val id: String,
    val name: String,
    val cellular: Boolean,
    val imei: String,
    @SerializedName("last_iccid") val lastIccid: String,
    @SerializedName("current_build_target") val currentBuild: String,
    @SerializedName("default_build_target") val defaultBuild: String,
    @SerializedName("connected") val isConnected: Boolean,
    @SerializedName("platform_id") val platformId: Int,
    @SerializedName("product_id") val productId: Int,
    @SerializedName("last_ip_address") val ipAddress: String,
    @SerializedName("status") val status: String,
    @SerializedName("last_heard") val lastHeard: Date
)