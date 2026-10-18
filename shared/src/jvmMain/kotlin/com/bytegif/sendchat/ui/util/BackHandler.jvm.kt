package com.bytegif.sendchat.ui.util

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op for Desktop JVM
}
