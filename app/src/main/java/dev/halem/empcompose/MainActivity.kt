package dev.halem.empcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import dev.halem.emp.Emp
import dev.halem.empcompose.ui.theme.EmpComposeTheme

class MainActivity : ComponentActivity() {

    private lateinit var emp: Emp.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        emp = Emp.Builder(this).build()

        setContent {
            EmpApp()
        }
    }

    @Composable
    fun EmpApp() {
        EmpComposeTheme {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(color = Color.Black)
                    .fillMaxSize()
            ) {
                EmpPlayer()
            }
        }
    }

    @Composable
    fun EmpPlayer() {

        val url =
            "https://shls-mbc3-prod-dub.shahid.net/out/v1/d5bbe570e1514d3d9a142657d33d85e6/index.m3u8"

        LaunchedEffect(emp.player) {
            emp.apply {
                mediaSource(url)
            }
        }

        AndroidView(factory = { emp.playerView })

    }

    override fun onResume() {
        super.onResume()
        emp.resume()
    }

    override fun onStop() {
        super.onStop()
        emp.release()
    }
}