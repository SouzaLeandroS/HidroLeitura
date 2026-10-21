package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.Apartment
import com.example.data.model.MeterReading
import com.example.domain.MonthlyReportCalculator
import com.example.domain.ReportExporter
import com.example.ui.MainViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ApartmentHistoryScreen(
    apartment: Apartment,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onNewReading: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val allReadings by viewModel.allReadings.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var isAscendingOrder by remember { mutableStateOf(false) }
    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }

    val currencyFormat = remember { DecimalFormat("R$ #,##0.00") }
    val m3Format = remember { DecimalFormat("#,##0.000") }
    val litersFormat = remember { DecimalFormat("#,##0") }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")) }

    // Filter readings for this specific apartment
    val aptReadings = remember(allReadings, apartment.id, isAscendingOrder) {
        val filtered = allReadings.filter { it.apartmentId == apartment.id }
        if (isAscendingOrder) {
            filtered.sortedWith(compareBy({ it.referenceMonth }, { it.readingDate }))
        } else {
            filtered.sortedWith(compareByDescending<MeterReading> { it.referenceMonth }.thenByDescending { it.readingDate })
        }
    }

    // Historical calculations
    val totalReadingsCount = aptReadings.size
    val totalConsumptionAccumulated = remember(allReadings, apartment) {
        val sortedAsc = allReadings.filter { it.apartmentId == apartment.id }.sortedBy { it.referenceMonth }
        if (sortedAsc.isEmpty()) 0.0
        else {
            val minReading = sortedAsc.first().readingM3
            val maxReading = sortedAsc.last().readingM3
            (maxReading - minReading).coerceAtLeast(0.0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("apartment_history_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Top Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("history_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar"
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Histórico de Leituras",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = apartment.displayTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                onClick = { isAscendingOrder = !isAscendingOrder },
                modifier = Modifier.testTag("toggle_sort_order_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Alternar Ordem Cronológica",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Apartment Summary Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("apartment_history_summary_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Morador: ${apartment.residentName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (apartment.meterSerialNumber.isNotBlank()) {
                            Text(
                                text = "Nº Hidrômetro: ${apartment.meterSerialNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "$totalReadingsCount leituras",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Base Inicial: ${m3Format.format(apartment.initialReadingM3)} m³",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Total Consumido: ${m3Format.format(totalConsumptionAccumulated)} m³",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chronological order indicator & filter chip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isAscendingOrder) "Ordem: Mais antigas ➔ Mais recentes" else "Ordem: Mais recentes ➔ Mais antigas",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (onNewReading != null) {
                Text(
                    text = "Nova Leitura",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onNewReading() }
                        .padding(4.dp)
                        .testTag("history_new_reading_trigger")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chronological readings LazyColumn
        if (aptReadings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhuma leitura registrada para esta unidade.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (onNewReading != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNewReading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("history_first_reading_btn")
                        ) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fazer Primeira Leitura")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("chronological_readings_lazy_column"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(aptReadings, key = { _, item -> item.id }) { index, reading ->
                    // Calculate consumption delta relative to the immediately preceding chronologic reading
                    val previousReading = allReadings
                        .filter { it.apartmentId == apartment.id && it.referenceMonth < reading.referenceMonth }
                        .maxByOrNull { it.referenceMonth }

                    val baseM3 = previousReading?.readingM3 ?: apartment.initialReadingM3
                    val consumptionM3 = (reading.readingM3 - baseM3).coerceAtLeast(0.0)
                    val consumptionLiters = consumptionM3 * 1000.0

                    val waterCost = consumptionM3 * settings.waterRatePerM3
                    val totalCost = waterCost * (1.0 + settings.sewageRatePercent / 100.0) + settings.fixedFeePerUnit

                    ChronologicalReadingCard(
                        reading = reading,
                        previousM3 = baseM3,
                        consumptionM3 = consumptionM3,
                        consumptionLiters = consumptionLiters,
                        estimatedCost = totalCost,
                        formattedDate = dateFormat.format(Date(reading.readingDate)),
                        onPhotoClick = { reading.photoPath?.let { selectedPhotoUri = it } },
                        onShare = {
                            val rep = MonthlyReportCalculator.generateApartmentReport(
                                apartment = apartment,
                                referenceMonth = reading.referenceMonth,
                                allReadings = allReadings,
                                settings = settings
                            )
                            val text = ReportExporter.formatApartmentReceipt(rep, settings)
                            ReportExporter.shareText(
                                context,
                                text,
                                "Comprovante - ${apartment.displayTitle} (${reading.referenceMonth})"
                            )
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Photo Dialog if user tapped on photo thumbnail
    selectedPhotoUri?.let { uriStr ->
        Dialog(onDismissRequest = { selectedPhotoUri = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Foto do Hidrômetro",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { selectedPhotoUri = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    AsyncImage(
                        model = Uri.parse(uriStr),
                        contentDescription = "Foto da Leitura",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
private fun ChronologicalReadingCard(
    reading: MeterReading,
    previousM3: Double,
    consumptionM3: Double,
    consumptionLiters: Double,
    estimatedCost: Double,
    formattedDate: String,
    onPhotoClick: () -> Unit,
    onShare: () -> Unit
) {
    val m3Format = remember { DecimalFormat("#,##0.000") }
    val litersFormat = remember { DecimalFormat("#,##0") }
    val currencyFormat = remember { DecimalFormat("R$ #,##0.00") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reading_item_${reading.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Month tag & Share button
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = MonthlyReportCalculator.formatReferenceMonth(reading.referenceMonth),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(32.dp).testTag("share_reading_btn_${reading.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Compartilhar Comprovante",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "Medido em: $formattedDate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Reading dial display summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LEITURA ATUAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${m3Format.format(reading.readingM3)} m³",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Anterior: ${m3Format.format(previousM3)} m³",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "CONSUMO DO MÊS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF0288D1),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "+${m3Format.format(consumptionM3)} m³",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0288D1)
                    )
                    Text(
                        text = "${litersFormat.format(consumptionLiters)} Litros • ${currencyFormat.format(estimatedCost)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Photo preview and notes (if available)
            if (reading.photoPath != null || reading.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (reading.photoPath != null) {
                        AsyncImage(
                            model = Uri.parse(reading.photoPath),
                            contentDescription = "Foto comprovante",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .clickable { onPhotoClick() }
                                .testTag("photo_thumb_${reading.id}")
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    if (reading.notes.isNotBlank()) {
                        Text(
                            text = "Obs: ${reading.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}
