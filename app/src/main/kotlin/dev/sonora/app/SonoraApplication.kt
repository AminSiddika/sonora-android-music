package dev.sonora.app

import android.app.Application
import dev.sonora.app.di.SonoraGraph

/**
 * Owns the small dependency graph for the whole process. Sonora uses a hand
 * written service locator instead of a DI framework: the graph has a dozen
 * nodes, so a framework would add build time without adding clarity.
 */
class SonoraApplication : Application() {

    lateinit var graph: SonoraGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = SonoraGraph(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        if (::graph.isInitialized) graph.player.release()
    }
}
