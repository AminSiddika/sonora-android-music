package dev.sonora.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.sonora.app.di.SonoraGraph

/**
 * The dependency graph is provided once at the top of the tree so screens and
 * view models do not have to thread it through every composable.
 */
val LocalSonoraGraph = staticCompositionLocalOf<SonoraGraph> {
    error("SonoraGraph was not provided by SonoraApp")
}

/** Creates a view model wired from the graph. */
@Composable
inline fun <reified VM : ViewModel> sonoraViewModel(
    key: String? = null,
    crossinline create: (SonoraGraph) -> VM,
): VM {
    val graph = LocalSonoraGraph.current
    return viewModel(
        key = key,
        factory = viewModelFactory {
            initializer { create(graph) }
        },
    )
}
