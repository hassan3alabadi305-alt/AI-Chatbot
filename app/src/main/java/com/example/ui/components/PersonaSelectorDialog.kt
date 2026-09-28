package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Persona

@Composable
fun PersonaSelectorDialog(
    currentPersonaId: String,
    onPersonaSelected: (Persona) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPersona by remember {
        mutableStateOf(Persona.DEFAULT_PERSONAS.find { it.id == currentPersonaId } ?: Persona.DEFAULT_PERSONA)
    }

    var customPromptText by remember { mutableStateOf(selectedPersona.systemPrompt) }
    var isCustomMode by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "اختيار شخصية البوت (Bot Persona)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "اختر نمط الإجابة والتعليمات المخصصة للمساعد",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(Persona.DEFAULT_PERSONAS) { persona ->
                        val isSelected = persona.id == selectedPersona.id && !isCustomMode
                        PersonaCardItem(
                            persona = persona,
                            isSelected = isSelected,
                            onSelect = {
                                selectedPersona = persona
                                customPromptText = persona.systemPrompt
                                isCustomMode = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isCustomMode) {
                    OutlinedTextField(
                        value = customPromptText,
                        onValueChange = { customPromptText = it },
                        label = { Text("تعليمات مخصصة (System Prompt)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    TextButton(
                        onClick = { isCustomMode = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("تعديل التعليمات مخصصاً ✏️")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalPersona = if (isCustomMode) {
                                selectedPersona.copy(
                                    id = "custom",
                                    name = "Custom Persona",
                                    nameAr = "شخصية مخصصة",
                                    systemPrompt = customPromptText
                                )
                            } else selectedPersona

                            onPersonaSelected(finalPersona)
                            onDismiss()
                        }
                    ) {
                        Text("تأكيد الشخصية")
                    }
                }
            }
        }
    }
}

@Composable
fun PersonaCardItem(
    persona: Persona,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val icon = getPersonaIcon(persona.iconName)
    val color = Color(persona.colorHex)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) color else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = persona.nameAr,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = persona.nameAr,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = persona.descriptionAr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun getPersonaIcon(iconName: String): ImageVector {
    return when (iconName) {
        "Code" -> Icons.Default.Code
        "AutoAwesome" -> Icons.Default.AutoAwesome
        "Translate" -> Icons.Default.Translate
        "Psychology" -> Icons.Default.Psychology
        else -> Icons.Default.SmartToy
    }
}
