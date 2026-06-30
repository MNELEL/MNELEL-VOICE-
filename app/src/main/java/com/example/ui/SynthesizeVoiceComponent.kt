package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.VoiceClonerViewModel
import com.example.data.VoiceProfile
import com.example.ui.theme.LightPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynthesizeVoiceComponent(
    viewModel: VoiceClonerViewModel,
    profile: VoiceProfile,
    inputPhrase: String,
    onInputPhraseChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSynthesize: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val isSynthesizing by viewModel.isSynthesizing.collectAsState()
    val isNoiseReductionEnabled by viewModel.isNoiseReductionEnabled.collectAsState()
    
    var fileSynthesisProgress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(isSynthesizing) {
        if (isSynthesizing && inputPhrase.length > 50) {
            fileSynthesisProgress = 0f
            while (fileSynthesisProgress < 0.95f && isSynthesizing) {
                kotlinx.coroutines.delay(100)
                fileSynthesisProgress += 0.02f
            }
        } else {
            fileSynthesisProgress = 0f
        }
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                    onInputPhraseChange(reader.readText())
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "שגיאה בקריאת הקובץ", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("הקלד טקסט או העלה קובץ לסינתזה:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            IconButton(
                onClick = { launcher.launch("text/plain") },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Upload Text File",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        OutlinedTextField(
            value = inputPhrase,
            onValueChange = onInputPhraseChange,
            placeholder = { Text("הזן טקסט או העלה קובץ טקסט שלם...") },
            modifier = Modifier.fillMaxWidth().height(140.dp),
            shape = RoundedCornerShape(12.dp)
        )


        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = isNoiseReductionEnabled,
                onCheckedChange = { viewModel.toggleNoiseReduction() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("הפעל סינון רעשי רקע (DSP)", fontSize = 14.sp)
        }

        var expanded by remember { mutableStateOf(false) }
        val vibeOptions = listOf("מקורי", "שמח", "מקצועי", "רגוע", "עצוב", "אנרגטי")
        var selectedVibe by remember { mutableStateOf(vibeOptions[0]) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                readOnly = true,
                value = selectedVibe,
                onValueChange = { },
                label = { Text("טון רגשי") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                vibeOptions.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            selectedVibe = selectionOption
                            expanded = false
                        }
                    )
                }
            }
        }

        var accentExpanded by remember { mutableStateOf(false) }
        val accentOptions = listOf(
            "Standard" to "רגיל (Standard)",
            "Russian" to "רוסי (Russian)",
            "Moroccan" to "מרוקאי (Moroccan)",
            "Yemeni" to "תימני (Yemeni)",
            "American" to "אמריקאי (American)",
            "British" to "בריטי (British)"
        )
        var selectedAccentPair by remember { mutableStateOf(accentOptions[0]) }

        ExposedDropdownMenuBox(
            expanded = accentExpanded,
            onExpandedChange = { accentExpanded = !accentExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                readOnly = true,
                value = selectedAccentPair.second,
                onValueChange = { },
                label = { Text("מבטא קול") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = accentExpanded)
                },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = accentExpanded,
                onDismissRequest = { accentExpanded = false }
            ) {
                accentOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.second) },
                        onClick = {
                            selectedAccentPair = option
                            accentExpanded = false
                        }
                    )
                }
            }
        }

        if (isSynthesizing && inputPhrase.length > 50) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("מסנתז אודיו מתוך טקסט (קובץ ארוך)...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = fileSynthesisProgress,
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        Button(
            onClick = {
                if (inputPhrase.isNotBlank()) {
                    viewModel.synthesizeText(
                        text = inputPhrase,
                        profile = profile,
                        vibeModifier = selectedVibe,
                        accent = selectedAccentPair.first
                    )
                    onSynthesize(inputPhrase)
                } else {
                    android.widget.Toast.makeText(context, "נא להזין טקסט", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !isSynthesizing,
            colors = ButtonDefaults.buttonColors(containerColor = LightPrimary),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isSynthesizing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("התחל סינתזה", fontWeight = FontWeight.Bold)
            }
        }
    }
}
