package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("הקלד טקסט לסינתזה קולית:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        
        OutlinedTextField(
            value = inputPhrase,
            onValueChange = onInputPhraseChange,
            placeholder = { Text("הזן טקסט כאן...") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
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

        Button(
            onClick = {
                if (inputPhrase.isNotBlank()) {
                    viewModel.synthesizeText(
                        text = inputPhrase,
                        profile = profile,
                        vibeModifier = selectedVibe
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
