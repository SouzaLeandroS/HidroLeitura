package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Apartment
import com.example.ui.MainViewModel
import com.example.ui.ScreenTab
import java.text.DecimalFormat

@Composable
fun ApartmentSelectionScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onApartmentSelected: ((Apartment) -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    val apartments by viewModel.apartments.collectAsStateWithLifecycle()
    val allReadings by viewModel.allReadings.collectAsStateWithLifecycle()
    val selectedApartment by viewModel.selectedApartment.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var filterPendingOnly by remember { mutableStateOf(false) }

    val filteredList = remember(apartments, searchQuery, filterPendingOnly, allReadings, selectedMonth) {
        apartments.filter { apt ->
            val matchesQuery = searchQuery.isBlank() ||
                apt.block.contains(searchQuery, ignoreCase = true) ||
                apt.unitNumber.contains(searchQuery, ignoreCase = true) ||
                apt.residentName.contains(searchQuery, ignoreCase = true) ||
                apt.meterSerialNumber.contains(searchQuery, ignoreCase = true)

            if (!matchesQuery) return@filter false

            if (filterPendingOnly) {
                val isRead = allReadings.any { it.apartmentId == apt.id && it.referenceMonth == selectedMonth }
                !isRead
            } else {
                true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("apartment_selection_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Header with title and optional back arrow
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("selection_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar"
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Column {
                Text(
                    text = "Selecionar Apartamento",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Escolha a unidade para realizar a leitura do hidrômetro",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar apto, bloco ou morador...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Buscar")
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("selection_search_input"),
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter chips: All vs Only Pending
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !filterPendingOnly,
                onClick = { filterPendingOnly = false },
                label = { Text("Todos (${apartments.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.testTag("filter_all_apartments_chip")
            )

            val pendingCount = apartments.count { apt ->
                allReadings.none { it.apartmentId == apt.id && it.referenceMonth == selectedMonth }
            }

            FilterChip(
                selected = filterPendingOnly,
                onClick = { filterPendingOnly = true },
                label = { Text("Pendentes de Leitura ($pendingCount)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier.testTag("filter_pending_apartments_chip")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // LazyColumn displaying apartments
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (filterPendingOnly) "Nenhum apartamento pendente de leitura neste mês!" else "Nenhuma unidade habitacional encontrada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("apartments_selection_lazy_column"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { apt ->
                    val isSelected = selectedApartment?.id == apt.id
                    val isReadThisMonth = allReadings.any {
                        it.apartmentId == apt.id && it.referenceMonth == selectedMonth
                    }

                    val latestReading = allReadings
                        .filter { it.apartmentId == apt.id }
                        .maxByOrNull { it.referenceMonth }

                    ApartmentSelectionItem(
                        apartment = apt,
                        isSelected = isSelected,
                        isReadThisMonth = isReadThisMonth,
                        latestReadingM3 = latestReading?.readingM3 ?: apt.initialReadingM3,
                        onSelect = {
                            viewModel.selectApartment(apt)
                            onApartmentSelected?.invoke(apt)
                            if (onBack != null) {
                                onBack()
                            } else {
                                viewModel.selectTab(ScreenTab.LEITURA)
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ApartmentSelectionItem(
    apartment: Apartment,
    isSelected: Boolean,
    isReadThisMonth: Boolean,
    latestReadingM3: Double,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelect() }
            .testTag("select_apt_item_${apartment.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio Button / Selection Indicator
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                modifier = Modifier.testTag("radio_apt_${apartment.id}")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Unit details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = apartment.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isReadThisMonth) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                    ) {
                        Text(
                            text = if (isReadThisMonth) "Leitura OK" else "Pendente",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isReadThisMonth) Color(0xFF2E7D32) else Color(0xFFE65100),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Morador: ${apartment.residentName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (apartment.meterSerialNumber.isNotBlank()) {
                    Text(
                        text = "Nº Hidrômetro: ${apartment.meterSerialNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Última leitura: ${DecimalFormat("#,##0.00").format(latestReadingM3)} m³",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action button to start reading immediately
            Button(
                onClick = onSelect,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("action_select_apt_${apartment.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isSelected) "Ler" else "Selecionar",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
