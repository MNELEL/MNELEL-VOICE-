package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.VoiceClonerViewModel
import com.example.data.VoiceProfile

@Composable
fun SynthesizeVoiceQueueComponent(
    viewModel: VoiceClonerViewModel,
    profile: VoiceProfile,
    modifier: Modifier = Modifier
) {
    val textQueue by viewModel.textQueue.collectAsState()
    var inputPhrase by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = inputPhrase,
            onValueChange = { inputPhrase = it },
            placeholder = { Text("הזן טקסט לתור...") },
            modifier = Modifier.fillMaxWidth()
        )
        Row {
            Button(onClick = {
                if (inputPhrase.isNotBlank()) {
                    viewModel.addToQueue(inputPhrase)
                    inputPhrase = ""
                }
            }) {
                Text("הוסף לתור")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { viewModel.playQueue(profile) }) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text("נגן תור")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { viewModel.clearTextQueue() }) {
                Text("נקה")
            }
        }
        LazyColumn(modifier = Modifier.height(200.dp)) {
            itemsIndexed(textQueue) { index, text ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.removeFromQueue(index) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove")
                    }
                }
            }
        }
    }
}
