package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CondoSettings
import com.example.ui.MainViewModel
import java.text.DecimalFormat

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentSettings by viewModel.settings.collectAsStateWithLifecycle()

    var condoName by remember(currentSettings) { mutableStateOf(currentSettings.condoName) }
    var waterRate by remember(currentSettings) { mutableStateOf(currentSettings.waterRatePerM3.toString()) }
    var sewagePercent by remember(currentSettings) { mutableStateOf(currentSettings.sewageRatePercent.toString()) }
    var fixedFee by remember(currentSettings) { mutableStateOf(currentSettings.fixedFeePerUnit.toString()) }
    var leakThreshold by remember(currentSettings) { mutableStateOf(currentSettings.leakThresholdM3.toString()) }

    val calculatedCombinedRate = remember(waterRate, sewagePercent) {
        val wr = waterRate.toDoubleOrNull() ?: 0.0
        val sp = sewagePercent.toDoubleOrNull() ?: 0.0
        wr * (1.0 + (sp / 100.0))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tarifas & Parâmetros do Condomínio",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Configure os valores de cobrança para o cálculo automático dos relatórios mensais.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = condoName,
                        onValueChange = { condoName = it },
                        label = { Text("Nome do Condomínio / Edifício") },
                        modifier = Modifier.fillMaxWidth().testTag("input_condo_name"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = waterRate,
                        onValueChange = { waterRate = it },
                        label = { Text("Tarifa de Água por m³ (R$)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_water_rate"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = { Text("Ex: 6.80 por metro cúbico consumido") }
                    )

                    OutlinedTextField(
                        value = sewagePercent,
                        onValueChange = { sewagePercent = it },
                        label = { Text("Taxa de Coleta de Esgoto (%)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_sewage_percent"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = { Text("Percentual sobre a tarifa de água (comum: 80% ou 100%)") }
                    )

                    OutlinedTextField(
                        value = fixedFee,
                        onValueChange = { fixedFee = it },
                        label = { Text("Taxa Fixa / Unidade (R$)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_fixed_fee"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = { Text("Custo fixo por hidrômetro/unidade (ou 0 se não houver)") }
                    )

                    OutlinedTextField(
                        value = leakThreshold,
                        onValueChange = { leakThreshold = it },
                        label = { Text("Limite para Alerta de Vazamento (m³)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_leak_threshold"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = { Text("Alerta quando o consumo do apartamento ultrapassar este valor") }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Combined Rate Preview
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tarifa Efetiva Combinada:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "R$ ${DecimalFormat("#,##0.00").format(calculatedCombinedRate)} / m³",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val updated = CondoSettings(
                                id = 1,
                                condoName = condoName.trim().ifBlank { "Condomínio" },
                                waterRatePerM3 = waterRate.toDoubleOrNull() ?: 6.80,
                                sewageRatePercent = sewagePercent.toDoubleOrNull() ?: 80.0,
                                fixedFeePerUnit = fixedFee.toDoubleOrNull() ?: 0.0,
                                leakThresholdM3 = leakThreshold.toDoubleOrNull() ?: 15.0
                            )
                            viewModel.saveSettings(updated)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_settings_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salvar Configurações")
                    }
                }
            }
        }

        // Gemini AI info card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tecnologia Gemini Vision OCR",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "O aplicativo processa fotos de hidrômetros analógicos e digitais, reconhecendo automaticamente os roletes pretos (metros cúbicos) e vermelhos (litros), além do número de série do aparelho. Você sempre pode ajustar qualquer dígito antes de confirmar a gravação.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Demo Data Reset Button
        item {
            OutlinedButton(
                onClick = { viewModel.resetToSampleData() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reload_sample_data_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recarregar Dados de Demonstração")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
