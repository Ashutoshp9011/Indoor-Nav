package com.ashutosh.corridor360.capture

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Simple grid preview of frames captured so far in the current session.
 * Opened from the gallery icon on CorridorCaptureScreen; read-only for now
 * (no delete/retake — add if you want per-frame retake later).
 */
@Composable
fun CapturedFramesGallery(
    framePaths: List<String>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1C1C1E)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Captured Frames (${framePaths.size})",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (framePaths.isEmpty()) {
                    Text(
                        text = "No frames captured yet",
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(24.dp)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.heightIn(max = 400.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(framePaths) { path ->
                            FrameThumbnail(path)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FrameThumbnail(path: String) {
    // Decoded off the main thread at reduced resolution (inSampleSize=4) — these are
    // burst frames, avoid both main-thread jank and OOM across many frames.
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = path) {
        value = withContext(Dispatchers.IO) {
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            BitmapFactory.decodeFile(File(path).absolutePath, opts)
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))) {
                // Still decoding, or decode failed / file missing — leave a blank dark tile rather than crash
            }
        }
    }
}
