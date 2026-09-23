package com.portfolio.iptvplayer.presentation.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * Replaces the Leanback GuidedStepSupportFragment. Same three ways to load
 * a real playlist as before (URL, local file picker, app-folder file) —
 * only the widget toolkit changed, not the underlying logic, which all
 * lives in SettingsViewModel and the use cases it wraps.
 */
@Composable
fun SettingsScreen(onDone: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val focusRequester = androidx.compose.runtime.remember { FocusRequester() }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importFile(it.toString()) }
    }

    BackHandler(onBack = onDone)

    LaunchedEffect(state.done) {
        if (state.done) onDone()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp)
    ) {
        Text("Playlist source", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Paste an M3U URL, pick a local file, or load one pushed into the app's folder via adb.",
            color = Color.Gray
        )
        Spacer(Modifier.height(24.dp))

        BasicTextField(
            value = state.urlInput,
            onValueChange = viewModel::onUrlChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                .padding(12.dp)
        )
        Spacer(Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = viewModel::saveUrl, modifier = Modifier.fillMaxWidth()) {
                Text("Save URL & reload")
            }
            Button(
                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Pick a local .m3u file")
            }
            Button(onClick = viewModel::importAppFolder, modifier = Modifier.fillMaxWidth()) {
                Text("Load IPTV.m3u from app folder")
            }
            Button(onClick = viewModel::clearPlaylist, modifier = Modifier.fillMaxWidth()) {
                Text("Clear (use bundled test streams)")
            }
        }

        state.message?.let { message ->
            Spacer(Modifier.height(16.dp))
            Text(message, color = Color.Yellow)
        }
    }
}