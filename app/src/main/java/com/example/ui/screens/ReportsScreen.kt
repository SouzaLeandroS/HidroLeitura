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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.ApartmentMonthlyReport
import com.example.domain.MonthlyReportCalculator
import com.example.domain.ReportExporter
import com.example.ui.MainViewModel
import com.example.ui.ReportFilter
import com.example.ui.components.StatCard
import java.text.DecimalFormat

@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val summary by viewModel.monthlySummary.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val recordedMonths by viewModel.recordedMonths.collectAsStateWithLifecycle()
    val reportFilter by viewModel.reportFilter.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var monthDropdownExpanded by remember { mutableStateOf(false) }
    var selectedPhotoReport by remember { mutableStateOf<ApartmentMonthlyReport?>(null) }

    val currencyFormat = remember { DecimalFormat("R$ #,##0.00") }
    val m3Format = remember { DecimalFormat("#,##0.000") }
    val litersFormat = remember { DecimalFormat("#,##0") }

    val displayedReports = remember(summary, reportFilter) {
        val list = summary?.apartmentReports ?: emptyList()
        when (reportFilter) {
            ReportFilter.TODOS -> list
            ReportFilter.PENDENTES -> list.filter { it.isPending }
            ReportFilter.ALERTAS -> list.filter { it.isLeakAlert }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("reports_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Month Selector & Full Report Export Row
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clickable { monthDropdownExpanded = true }
                            .testTag("select_month_dropdown_trigger")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = MonthlyReportCalculator.formatReferenceMonth(selectedMonth),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = monthDropdownExpanded,
                        onDismissRequest = { monthDropdownExpanded = false }
                    ) {
                        recordedMonths.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(MonthlyReportCalculator.formatReferenceMonth(m)) },
                                onClick = {
                                    viewModel.selectMonth(m)
                                    monthDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                summary?.let { s ->
                    OutlinedButton(
                        onClick = {
                            val reportText = ReportExporter.formatCondoSummaryText(s, settings)
                            ReportExporter.shareText(
                                context,
                                reportText,
                                "Relatório Geral Água - ${s.monthFormatted}"
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("export_full_report_btn")
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Compartilhar Geral",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar Geral", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // 2. Global Condo Stats Cards
        summary?.let { s ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("condo_stats_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = settings.condoName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Fechamento de Leitura • ${s.monthFormatted}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (s.leakAlertCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFEBEE)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.WaterDamage,
                                            contentDescription = null,
                                            tint = Color(0xFFD32F2F),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${s.leakAlertCount} alerta(s)",
                                            color = Color(0xFFD32F2F),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Metric grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(
                                title = "CONSUMO TOTAL",
                                value = "${m3Format.format(s.totalConsumptionM3)} m³",
                                subtitle = "${litersFormat.format(s.totalConsumptionM3 * 1000)} Litros",
                                icon = Icons.Default.WaterDrop,
                                iconColor = Color(0xFF0288D1),
                                modifier = Modifier.weight(1f),
                                testTag = "stat_total_m3"
                            )
                            StatCard(
                                title = "TOTAL A RATEAR",
                                value = currencyFormat.format(s.totalCost),
                                subtitle = "Média: ${m3Format.format(s.averageConsumptionM3)} m³/apto",
                                icon = Icons.Default.DateRange,
                                iconColor = Color(0xFF2E7D32),
                                modifier = Modifier.weight(1f),
                                testTag = "stat_total_cost"
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Reading Progress Bar
                        val progress = if (s.totalApartments > 0) s.readApartments.toFloat() / s.totalApartments.toFloat() else 0f
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Progresso das Leituras",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${s.readApartments} de ${s.totalApartments} apartamentos lidos (${(progress * 100).toInt()}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 3. Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = reportFilter == ReportFilter.TODOS,
                    onClick = { viewModel.setReportFilter(ReportFilter.TODOS) },
                    label = { Text("Todas as Unidades") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("filter_all_units")
                )
                FilterChip(
                    selected = reportFilter == ReportFilter.PENDENTES,
                    onClick = { viewModel.setReportFilter(ReportFilter.PENDENTES) },
                    label = { Text("Pendentes") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("filter_pending_units")
                )
                FilterChip(
                    selected = reportFilter == ReportFilter.ALERTAS,
                    onClick = { viewModel.setReportFilter(ReportFilter.ALERTAS) },
                    label = { Text("Possível Vazamento") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.testTag("filter_leak_alerts")
                )
            }
        }

        // 4. Per-Unit Detailed Report List
        if (displayedReports.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum relatório encontrado para o filtro selecionado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(displayedReports, key = { it.apartment.id }) { report ->
                ApartmentReportCard(
                    report = report,
                    settings = settings,
                    onShareReceipt = {
                        val receiptText = ReportExporter.formatApartmentReceipt(report, settings)
                        ReportExporter.shareText(
                            context,
                            receiptText,
                            "Comprovante Consumo Água - ${report.apartment.displayTitle}"
                        )
                    },
                    onViewPhoto = { selectedPhotoReport = report }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Photo Dialog View
    selectedPhotoReport?.let { rep ->
        val uriStr = rep.currentReading?.photoPath
        if (uriStr != null) {
            Dialog(onDismissRequest = { selectedPhotoReport = null }) {
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
                                text = "Foto: ${rep.apartment.displayTitle}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { selectedPhotoReport = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        AsyncImage(
                            model = Uri.parse(uriStr),
                            contentDescription = "Foto da leitura do hidrômetro",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Leitura registrada: ${m3Format.format(rep.currentReading.readingM3)} m³",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApartmentReportCard(
    report: ApartmentMonthlyReport,
    settings: com.example.data.model.CondoSettings,
    onShareReceipt: () -> Unit,
    onViewPhoto: () -> Unit
) {
    val currencyFormat = remember { DecimalFormat("R$ #,##0.00") }
    val m3Format = remember { DecimalFormat("#,##0.000") }
    val litersFormat = remember { DecimalFormat("#,##0") }
    val apt = report.apartment

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_card_${apt.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Apartment Name & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = apt.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Morador: ${apt.residentName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (report.isPending) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFF3E0)
                    ) {
                        Text(
                            text = "Pendente",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (report.isLeakAlert) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                    ) {
                        Text(
                            text = if (report.isLeakAlert) "⚠️ Alto Consumo" else "Normal",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (report.isLeakAlert) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Leak Warning Alert Banner if applicable
            if (report.isLeakAlert && !report.isPending) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF3CD),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE082)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Consumo acima de ${settings.leakThresholdM3} m³ ou aumento súbito! Verificar possíveis vazamentos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF795548),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (report.isPending) {
                Text(
                    text = "Ainda não foi realizada a leitura fotográfica deste apartamento no mês.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Read Numbers Box
                val prevM3 = report.previousReading?.readingM3 ?: apt.initialReadingM3
                val currM3 = report.currentReading?.readingM3 ?: 0.0

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
                            text = "Leitura Anterior: ${m3Format.format(prevM3)} m³",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Leitura Atual:    ${m3Format.format(currM3)} m³",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${m3Format.format(report.consumptionM3)} m³",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0288D1)
                        )
                        Text(
                            text = "${litersFormat.format(report.consumptionLiters)} Litros",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Month comparison badge
                report.percentageChange?.let { pct ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (pct >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (pct >= 0) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val signal = if (pct >= 0) "+" else ""
                        Text(
                            text = "$signal${DecimalFormat("#0.0").format(pct)}% em relação ao mês anterior",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (pct >= 0) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Financial Breakdown
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Água (${m3Format.format(report.consumptionM3)} m³)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = currencyFormat.format(report.waterCost),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Esgoto (${DecimalFormat("#0").format(settings.sewageRatePercent)}%)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = currencyFormat.format(report.sewageCost),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (report.fixedFee > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Taxa Fixa", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = currencyFormat.format(report.fixedFee),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TOTAL DA UNIDADE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currencyFormat.format(report.totalCost),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (report.currentReading?.photoPath != null) {
                    OutlinedButton(
                        onClick = onViewPhoto,
                        modifier = Modifier.weight(1f).testTag("view_photo_btn_${apt.id}"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Ver Foto", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Button(
                    onClick = onShareReceipt,
                    modifier = Modifier.weight(1.3f).testTag("share_receipt_btn_${apt.id}"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Enviar Comprovante",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enviar Comprovante", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
    }
}
