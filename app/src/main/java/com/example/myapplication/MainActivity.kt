package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.fragment.app.Fragment
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.navigation.compose.*
import kotlinx.coroutines.delay
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.R
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.gson.annotations.SerializedName
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.*

class PureComposeBridgeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(this.activity as Context).apply {
        setContent {
            MainScreen()
        }
    }
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(FrameLayout(this).apply {
            id = R.id.fragment_container_view_tag
        })

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_view_tag, PureComposeBridgeFragment())
            .commitNow()
    }
}

@Composable
private fun MainScreen() {
    val navController = rememberNavController()
    var sirenShouldBeOn by rememberSaveable { mutableStateOf(false) }

    MyApplicationTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { navController.navigateSingle("greeting") }) {
                        Text("Greeting")
                    }
                    Button(onClick = { navController.navigateSingle("configure") }) {
                        Text("Configure")
                    }
                    Button(onClick = {
                        navController.navigateSingle(
                            "show_siren/$sirenShouldBeOn",
                            _restoreState = false
                        )
                    }) {
                        Text("Show Siren")
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
                        Configure(
                            sirenShouldBeOn = sirenShouldBeOn,
                            onSirenShouldBeOn = { _sirenShouldBeOn ->
                                sirenShouldBeOn = _sirenShouldBeOn
                            }
                        )
                    }
                    composable(
                        route = "show_siren/{shouldBeOn}",
                        arguments = listOf(navArgument("shouldBeOn") {
                            type = NavType.BoolType
                        })
                    ) {
                        val vm: ShowSirenViewModel = viewModel()
                        ShowSiren(it.arguments?.getBoolean("shouldBeOn") ?: false)
                    }
                }
            }
        }
    }
}

fun NavHostController.navigateSingle(route: String, _restoreState: Boolean = true) =
    this.navigate(route) {
        restoreState = _restoreState
        popUpTo("greeting") {
            saveState = true
        }
        launchSingleTop = true
    }

@Composable
fun Greeting() {
    Column {
        Text(text = "Greetings! Check out my cool siren!")
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
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

class ShowSirenViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    init {
        val shouldBeOn = savedStateHandle.get<Boolean>("shouldBeOn") ?: false
        if (shouldBeOn) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    particleInterface.turnOnRedSiren().execute()
                    delay(500)
                    particleInterface.turnOffRedSiren().execute()
                }
            }
        }
    }
}

@Composable
fun ShowSiren(shouldBeOn: Boolean = false) {
    Column {
        Text(text = "This is the 'Show Siren' Screen (${if (shouldBeOn) "on" else "off"})")

        RedSirenVideoPlayer(shouldBeOn)
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
fun RedSirenVideoPlayer(shouldBeOn: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(400.dp)
            .width(400.dp),
    ) {

        val context = LocalContext.current

        val exoPlayer = remember {
            ExoPlayer.Builder(context)
                .build()
                .apply {

                    // use the following if you are presenting and have
                    // network access and want to use the camera
//                  val rtspMediaSource: MediaSource = RtspMediaSource.Factory()
//                      .createMediaSource(MediaItem.fromUri("rtsp://ndipatri:ndipatri@192.168.148.129:554/stream1"))
//                  setMediaSource(rtspMediaSource)
                    //

                    // use the following if you are presenting and DO NOT have
                    // network access
                    val defaultDataSourceFactory = DefaultDataSource.Factory(context)
                    val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(
                        context,
                        defaultDataSourceFactory
                    )
                    val fileSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(if (shouldBeOn) "file:///android_asset/sirenOn.ts" else "file:///android_asset/sirenOff.ts"))
                    setMediaSource(fileSource)
                    //

                    addAnalyticsListener(EventLogger(null, "exo"))
                    prepare()
                }
        }

        exoPlayer.playWhenReady = true
        //exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF

        DisposableEffect(
            AndroidView(factory = {
                PlayerView(context).apply {
                    hideController()
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
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
