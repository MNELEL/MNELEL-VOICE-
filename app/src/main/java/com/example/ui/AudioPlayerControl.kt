package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AudioPlayerControl(
    isPlaying: Boolean,
    progress: Float,
    elapsedText: String,
    durationText: String,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Menu else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
                IconButton(onClick = onStop) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop"
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(text = elapsedText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                
                Slider(
                    value = progress,
                    onValueChange = onSeek,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                
                Text(text = durationText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
