package com.example.rateme

import androidx.compose.runtime.Composable

@Composable
actual fun CommonBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS doesn't have a hardware back button. 
    // Navigation is usually handled via UI buttons or swipe gestures 
    // which are managed by the Navigation component itself.
}
