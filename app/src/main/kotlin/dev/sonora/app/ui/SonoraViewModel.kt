package dev.sonora.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.sonora.app.di.SonoraGraph

/**
 * Creates a view model wired from the application graph.
 *
 * The graph is an explicit parameter rather than a CompositionLocal: the app
 * shell creates its player view model before any provider could wrap the
 * content, so relying on a composition local crashed the app on launch.
 * Passing it in makes that ordering mistake impossible.
 */
@Composable
inline fun <reified VM : ViewModel> sonoraViewModel(
    graph: SonoraGraph,
    key: String? = null,
    crossinline create: (SonoraGraph) -> VM,
): VM = viewModel(
    key = key,
    factory = viewModelFactory {
        initializer { create(graph) }
    },
)
