package dev.sonora.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sonora.app.data.ThemeMode
import dev.sonora.app.ui.SonoraApp
import dev.sonora.app.ui.theme.SonoraTheme

/**
 * Single activity host. Theme and reduced-motion preferences are observed here
 * so the whole UI reacts immediately when they change.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val graph = (application as SonoraApplication).graph

        setContent {
            val themeMode by graph.preferences.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val reduceMotion by graph.preferences.reduceMotion
                .collectAsStateWithLifecycle(initialValue = false)

            SonoraTheme(
                themeMode = themeMode,
                reduceMotionPreference = reduceMotion,
            ) {
                SonoraApp(graph = graph)
            }
        }
    }
}
