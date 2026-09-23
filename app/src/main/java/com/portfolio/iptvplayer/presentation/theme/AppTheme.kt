package com.portfolio.iptvplayer.presentation.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme

/**
 * androidx.tv.material3.MaterialTheme (not the plain Compose Material3 one)
 * — it's tuned for TV: larger default focus/touch targets and dark-first
 * color defaults that suit a living-room screen viewed from a distance.
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}