package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WaterMeterDialView(
    blackDigits: String,
    redDigits: String,
    onDigitsChanged: (black: String, red: String) -> Unit,
    modifier: Modifier = Modifier,
    isEditable: Boolean = true
) {
    var isManualInputMode by remember { mutableStateOf(false) }

    // Ensure 5 black digits and 2-3 red digits
    val paddedBlack = blackDigits.padStart(5, '0').takeLast(5)
    val paddedRed = (if (redDigits.isBlank()) "00" else redDigits).take(3)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .testTag("water_meter_dial_view"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Dial title & Quick edit toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0288D1))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mostrador do Hidrômetro",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isEditable) {
                    IconButton(
                        onClick = { isManualInputMode = !isManualInputMode },
                        modifier = Modifier.size(32.dp).testTag("toggle_manual_edit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Dígitos",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Realistic Counter Box
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF212121), Color(0xFF121212), Color(0xFF2C2C2C))
                        )
                    )
                    .border(2.dp, Color(0xFF616161), RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Black Rollers (m³)
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        paddedBlack.forEachIndexed { index, char ->
                            RollerDigit(
                                digit = char,
                                isRed = false,
                                onIncrement = {
                                    val currentNum = paddedBlack.toIntOrNull() ?: 0
                                    val factor = Math.pow(10.0, (4 - index).toDouble()).toInt()
                                    val newNum = currentNum + factor
                                    onDigitsChanged(newNum.toString().padStart(5, '0'), paddedRed)
                                },
                                onDecrement = {
                                    val currentNum = paddedBlack.toIntOrNull() ?: 0
                                    val factor = Math.pow(10.0, (4 - index).toDouble()).toInt()
                                    val newNum = maxOf(0, currentNum - factor)
                                    onDigitsChanged(newNum.toString().padStart(5, '0'), paddedRed)
                                },
                                isEditable = isEditable && !isManualInputMode
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(36.dp)
                            .background(Color(0xFF757575))
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    // Red Rollers (liters / fractions)
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        paddedRed.forEachIndexed { index, char ->
                            RollerDigit(
                                digit = char,
                                isRed = true,
                                onIncrement = {
                                    val currentRed = paddedRed.toIntOrNull() ?: 0
                                    val factor = Math.pow(10.0, (paddedRed.length - 1 - index).toDouble()).toInt()
                                    val newRed = (currentRed + factor) % 1000
                                    onDigitsChanged(paddedBlack, newRed.toString().padStart(paddedRed.length, '0'))
                                },
                                onDecrement = {
                                    val currentRed = paddedRed.toIntOrNull() ?: 0
                                    val factor = Math.pow(10.0, (paddedRed.length - 1 - index).toDouble()).toInt()
                                    val newRed = maxOf(0, currentRed - factor)
                                    onDigitsChanged(paddedBlack, newRed.toString().padStart(paddedRed.length, '0'))
                                },
                                isEditable = isEditable && !isManualInputMode
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "m³",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    text = "⬛ Pretos = m³ inteiros",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "🟥 Vermelhos = Litros",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.Medium
                )
            }

            // Manual Edit Fields if user toggled manual mode
            AnimatedVisibility(visible = isManualInputMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = blackDigits,
                            onValueChange = { newVal ->
                                val filtered = newVal.filter { it.isDigit() }
                                onDigitsChanged(filtered, redDigits)
                            },
                            label = { Text("Pretos (m³)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_black_digits"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = redDigits,
                            onValueChange = { newVal ->
                                val filtered = newVal.filter { it.isDigit() }
                                onDigitsChanged(blackDigits, filtered)
                            },
                            label = { Text("Vermelhos (L)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_red_digits"),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RollerDigit(
    digit: Char,
    isRed: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    isEditable: Boolean
) {
    val bgColor = if (isRed) Color(0xFFB71C1C) else Color(0xFF000000)
    val textColor = Color.White

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 1.dp)
    ) {
        if (isEditable) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { onIncrement() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Aumentar",
                    tint = Color.LightGray,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .width(28.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(bgColor)
                .border(1.dp, if (isRed) Color(0xFFFF5252) else Color(0xFF424242), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = digit.toString(),
                color = textColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        if (isEditable) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { onDecrement() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Diminuir",
                    tint = Color.LightGray,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
