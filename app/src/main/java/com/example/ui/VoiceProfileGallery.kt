package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.VoiceProfile

@Composable
fun VoiceProfileGallery(
    profiles: List<VoiceProfile>,
    onSelectProfile: (VoiceProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(profiles) { profile ->
            VoiceProfileGalleryItem(
                profile = profile,
                onClick = { onSelectProfile(profile) }
            )
        }
    }
}

@Composable
fun VoiceProfileGalleryItem(
    profile: VoiceProfile,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(120.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = profile.name, style = MaterialTheme.typography.titleMedium)
            Text(text = profile.gender ?: "", style = MaterialTheme.typography.bodySmall)
        }
    }
}
