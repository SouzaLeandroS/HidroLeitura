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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Apartment
import com.example.domain.MonthlyReportCalculator
import com.example.ui.MainViewModel
import com.example.ui.ScreenTab
import java.text.DecimalFormat

@Composable
fun ApartmentsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val apartments by viewModel.apartments.collectAsStateWithLifecycle()
    val allReadings by viewModel.allReadings.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var apartmentToEdit by remember { mutableStateOf<Apartment?>(null) }
    var apartmentToDelete by remember { mutableStateOf<Apartment?>(null) }
    var apartmentHistoryToShow by remember { mutableStateOf<Apartment?>(null) }

    val filteredApartments = remember(apartments, searchQuery) {
        if (searchQuery.isBlank()) apartments
        else apartments.filter {
            it.block.contains(searchQuery, ignoreCase = true) ||
            it.unitNumber.contains(searchQuery, ignoreCase = true) ||
            it.residentName.contains(searchQuery, ignoreCase = true) ||
            it.meterSerialNumber.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("apartments_screen")
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                label = { Text("Buscar por bloco, apartamento ou morador") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Buscar")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("apartment_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Summary counters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredApartments.size} unidades cadastradas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val readCount = apartments.count { apt ->
                    allReadings.any { it.apartmentId == apt.id && it.referenceMonth == selectedMonth }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "$readCount / ${apartments.size} lidos este mês",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredApartments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma unidade habitacional encontrada.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredApartments, key = { it.id }) { apt ->
                        val readThisMonth = allReadings.any { it.apartmentId == apt.id && it.referenceMonth == selectedMonth }

                        ApartmentCardItem(
                            apartment = apt,
                            isReadThisMonth = readThisMonth,
                            onReadClicked = {
                                viewModel.selectApartment(apt)
                                viewModel.selectTab(ScreenTab.LEITURA)
                            },
                            onEditClicked = { apartmentToEdit = apt },
                            onDeleteClicked = { apartmentToDelete = apt },
                            onHistoryClicked = { apartmentHistoryToShow = apt }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Floating Action Button to add unit
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_apartment_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Adicionar Apartamento")
        }
    }

    // Add / Edit Apartment Dialog
    if (showAddDialog || apartmentToEdit != null) {
        val target = apartmentToEdit
        ApartmentFormDialog(
            initialApartment = target,
            onDismiss = {
                showAddDialog = false
                apartmentToEdit = null
            },
            onSave = { savedApt ->
                viewModel.saveApartment(savedApt)
                showAddDialog = false
                apartmentToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    apartmentToDelete?.let { apt ->
        AlertDialog(
            onDismissRequest = { apartmentToDelete = null },
            title = { Text("Excluir Unidade") },
            text = { Text("Tem certeza que deseja excluir ${apt.displayTitle}? Todas as leituras e relatórios desta unidade serão removidos.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteApartment(apt)
                        apartmentToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_btn")
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { apartmentToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Apartment History Full Screen Dialog
    apartmentHistoryToShow?.let { apt ->
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { apartmentHistoryToShow = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                ApartmentHistoryScreen(
                    apartment = apt,
                    viewModel = viewModel,
                    onBack = { apartmentHistoryToShow = null },
                    onNewReading = {
                        apartmentHistoryToShow = null
                        viewModel.selectApartment(apt)
                        viewModel.selectTab(ScreenTab.LEITURA)
                    }
                )
            }
        }
    }
}

@Composable
private fun ApartmentCardItem(
    apartment: Apartment,
    isReadThisMonth: Boolean,
    onReadClicked: () -> Unit,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onHistoryClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("apt_card_${apartment.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = apartment.displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = apartment.residentName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isReadThisMonth) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = if (isReadThisMonth) "Leitura OK" else "Pendente",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isReadThisMonth) Color(0xFF2E7D32) else Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (apartment.meterSerialNumber.isNotBlank()) {
                        Text(
                            text = "Hidrômetro: ${apartment.meterSerialNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (apartment.residentPhone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = apartment.residentPhone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onHistoryClicked, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.History, contentDescription = "Histórico", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEditClicked, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDeleteClicked, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onReadClicked,
                modifier = Modifier.fillMaxWidth().testTag("read_meter_btn_${apartment.id}"),
                shape = RoundedCornerShape(10.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = if (isReadThisMonth) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isReadThisMonth) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isReadThisMonth) "Ver / Releitura do Hidrômetro" else "Fazer Leitura Agora")
            }
        }
    }
}

@Composable
private fun ApartmentFormDialog(
    initialApartment: Apartment?,
    onDismiss: () -> Unit,
    onSave: (Apartment) -> Unit
) {
    var block by remember { mutableStateOf(initialApartment?.block ?: "Bloco A") }
    var unitNumber by remember { mutableStateOf(initialApartment?.unitNumber ?: "") }
    var residentName by remember { mutableStateOf(initialApartment?.residentName ?: "") }
    var residentPhone by remember { mutableStateOf(initialApartment?.residentPhone ?: "") }
    var meterSerialNumber by remember { mutableStateOf(initialApartment?.meterSerialNumber ?: "") }
    var initialM3 by remember { mutableStateOf(initialApartment?.initialReadingM3?.toString() ?: "0.0") }
    var notes by remember { mutableStateOf(initialApartment?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialApartment == null) "Nova Unidade Habitacional" else "Editar Unidade") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = block,
                        onValueChange = { block = it },
                        label = { Text("Bloco / Torre") },
                        modifier = Modifier.fillMaxWidth().testTag("input_apt_block"),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = unitNumber,
                        onValueChange = { unitNumber = it },
                        label = { Text("Número do Apto") },
                        modifier = Modifier.fillMaxWidth().testTag("input_apt_number"),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = residentName,
                        onValueChange = { residentName = it },
                        label = { Text("Nome do Morador") },
                        modifier = Modifier.fillMaxWidth().testTag("input_resident_name"),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = residentPhone,
                        onValueChange = { residentPhone = it },
                        label = { Text("Telefone / WhatsApp") },
                        modifier = Modifier.fillMaxWidth().testTag("input_resident_phone"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }
                item {
                    OutlinedTextField(
                        value = meterSerialNumber,
                        onValueChange = { meterSerialNumber = it },
                        label = { Text("Nº Série do Hidrômetro") },
                        modifier = Modifier.fillMaxWidth().testTag("input_meter_serial"),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = initialM3,
                        onValueChange = { initialM3 = it },
                        label = { Text("Leitura Inicial (m³)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_initial_m3"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Observações") },
                        modifier = Modifier.fillMaxWidth().testTag("input_apt_notes"),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val apt = Apartment(
                        id = initialApartment?.id ?: 0L,
                        block = block.trim(),
                        unitNumber = unitNumber.trim(),
                        residentName = residentName.trim().ifBlank { "Morador" },
                        residentPhone = residentPhone.trim(),
                        meterSerialNumber = meterSerialNumber.trim(),
                        initialReadingM3 = initialM3.toDoubleOrNull() ?: 0.0,
                        notes = notes.trim()
                    )
                    onSave(apt)
                },
                enabled = unitNumber.isNotBlank(),
                modifier = Modifier.testTag("save_apartment_btn")
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
