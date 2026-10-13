package dev.sonora.app.ui

/** Shared loading lifecycle used by screens that fetch from a provider. */
enum class LoadStatus {
    IDLE,
    LOADING,
    CONTENT,
    EMPTY,
    ERROR,
}
