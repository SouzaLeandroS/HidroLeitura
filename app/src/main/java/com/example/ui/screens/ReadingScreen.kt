package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Apartment
import com.example.domain.MonthlyReportCalculator
import com.example.ui.MainViewModel
import com.example.ui.components.WaterMeterCameraCapture
import com.example.ui.components.WaterMeterDialView
import java.text.DecimalFormat

@Composable
fun ReadingScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val apartments by viewModel.apartments.collectAsStateWithLifecycle()
    val allReadings by viewModel.allReadings.collectAsStateWithLifecycle()
    val selectedApartment by viewModel.selectedApartment.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val draftBlack by viewModel.draftBlackDigits.collectAsStateWithLifecycle()
    val draftRed by viewModel.draftRedDigits.collectAsStateWithLifecycle()
    val draftPhotoUri by viewModel.draftPhotoUri.collectAsStateWithLifecycle()
    val draftNotes by viewModel.draftNotes.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val lastAiResult by viewModel.lastAiResult.collectAsStateWithLifecycle()

    var showPhotoDialog by remember { mutableStateOf(false) }
    var showCameraCapture by remember { mutableStateOf(false) }
    var showApartmentSelectionModal by remember { mutableStateOf(false) }
    var showApartmentHistoryModal by remember { mutableStateOf(false) }

    // Android Zero-permission photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.processImageUri(it) }
    }

    // Default select first apartment if none selected
    if (selectedApartment == null && apartments.isNotEmpty()) {
        viewModel.selectApartment(apartments.first())
    }

    val currentTotalM3 by remember(draftBlack, draftRed) {
        derivedStateOf {
            val b = draftBlack.toIntOrNull() ?: 0
            val rStr = draftRed.ifBlank { "00" }
            val r = rStr.toIntOrNull() ?: 0
            val divisor = Math.pow(10.0, rStr.length.toDouble())
            b + (r / divisor)
        }
    }

    val currentApt = selectedApartment
    val previousReading = remember(currentApt, allReadings, selectedMonth) {
        currentApt?.let { apt ->
            allReadings
                .filter { it.apartmentId == apt.id && it.referenceMonth < selectedMonth }
                .maxByOrNull { it.referenceMonth }
        }
    }

    val baseM3 = previousReading?.readingM3 ?: (currentApt?.initialReadingM3 ?: 0.0)
    val calculatedConsumptionM3 = (currentTotalM3 - baseM3).coerceAtLeast(0.0)
    val calculatedConsumptionLiters = calculatedConsumptionM3 * 1000.0

    val isAlreadyRead = remember(currentApt, allReadings, selectedMonth) {
        currentApt != null && allReadings.any { it.apartmentId == currentApt.id && it.referenceMonth == selectedMonth }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("reading_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Apartment Selection Ribbon
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selecione o Apartamento",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    androidx.compose.material3.TextButton(
                        onClick = { showApartmentSelectionModal = true },
                        modifier = Modifier.testTag("open_apt_selection_list_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ver Lista", fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(apartments, key = { it.id }) { apt ->
                        val readThisMonth = allReadings.any { it.apartmentId == apt.id && it.referenceMonth == selectedMonth }
                        val isSelected = selectedApartment?.id == apt.id

                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectApartment(apt) },
                            label = { Text(apt.displayTitle) },
                            leadingIcon = {
                                if (readThisMonth) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Lido",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFA000))
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("apt_chip_${apt.id}")
                        )
                    }
                }
            }
        }

        // 2. Selected Apartment Card Info
        if (currentApt != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("apartment_info_card"),
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
                                    text = currentApt.displayTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Morador: ${currentApt.residentName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isAlreadyRead) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isAlreadyRead) Color(0xFF81C784) else Color(0xFFFFB74D)
                                    )
                                ) {
                                    Text(
                                        text = if (isAlreadyRead) "Já Lido" else "Pendente",
                                        color = if (isAlreadyRead) Color(0xFF2E7D32) else Color(0xFFE65100),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                IconButton(
                                    onClick = { showApartmentHistoryModal = true },
                                    modifier = Modifier.size(32.dp).testTag("open_apt_history_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "Ver Histórico de Leituras",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (currentApt.meterSerialNumber.isNotBlank()) {
                                Text(
                                    text = "🔢 Hidrômetro: ${currentApt.meterSerialNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Última Leitura: ${DecimalFormat("#,##0.00").format(baseM3)} m³",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 3. Photo Capture & AI Trigger Buttons
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("camera_action_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "IA",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reconhecimento por Foto (IA)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showCameraCapture = true },
                            modifier = Modifier.weight(1.2f).testTag("open_live_camera_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Câmera ao Vivo",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Câmera")
                        }

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("pick_photo_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Galeria de Fotos",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Galeria")
                        }

                        OutlinedButton(
                            onClick = { viewModel.processSampleMeterImage() },
                            modifier = Modifier.weight(1f).testTag("sample_test_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Testar Hidrômetro Demo",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Demo", maxLines = 1)
                        }
                    }

                    // Analyzing Spinner
                    AnimatedVisibility(visible = isAnalyzing) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Analisando mostrador com Gemini Vision...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Photo Thumbnail Preview
                    if (draftPhotoUri != null && !isAnalyzing) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = draftPhotoUri,
                                contentDescription = "Foto capturada do hidrômetro",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showPhotoDialog = true }
                                    .testTag("photo_preview_thumb")
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Foto do Hidrômetro Vinculada",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                lastAiResult?.let { result ->
                                    Text(
                                        text = result.explanation,
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
        }

        // 4. Interactive Water Meter Dial View (Black & Red Rollers)
        item {
            WaterMeterDialView(
                blackDigits = draftBlack,
                redDigits = draftRed,
                onDigitsChanged = { black, red ->
                    viewModel.setDraftDigits(black, red)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 5. Consumption Preview & Calculation
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("consumption_preview_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CONSUMO CALCULADO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${DecimalFormat("#,##0.000").format(calculatedConsumptionM3)} m³",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "EM LITROS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${DecimalFormat("#,##0").format(calculatedConsumptionLiters)} L",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0288D1)
                        )
                    }
                }
            }
        }

        // 6. Notes field
        item {
            OutlinedTextField(
                value = draftNotes,
                onValueChange = { viewModel.setDraftNotes(it) },
                label = { Text("Observações (opcional)") },
                placeholder = { Text("Ex: Relógio embaçado, lacre íntegro...") },
                modifier = Modifier.fillMaxWidth().testTag("reading_notes_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // 7. Save Confirmation Button
        item {
            Button(
                onClick = { viewModel.saveCurrentReading() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_reading_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAlreadyRead) "Atualizar Leitura do Mês" else "Salvar Leitura do Hidrômetro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Photo Dialog Zoom
    if (showPhotoDialog && draftPhotoUri != null) {
        Dialog(onDismissRequest = { showPhotoDialog = false }) {
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
                            text = "Foto do Mostrador",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showPhotoDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AsyncImage(
                        model = draftPhotoUri,
                        contentDescription = "Foto ampliada do relógio",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }

    // Live Camera Capture Component
    if (showCameraCapture) {
        WaterMeterCameraCapture(
            onPhotoCaptured = { capturedUri ->
                showCameraCapture = false
                viewModel.processImageUri(capturedUri)
            },
            onDismiss = { showCameraCapture = false }
        )
    }

    // Apartment Selection LazyColumn Modal
    if (showApartmentSelectionModal) {
        Dialog(
            onDismissRequest = { showApartmentSelectionModal = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                ApartmentSelectionScreen(
                    viewModel = viewModel,
                    onApartmentSelected = {
                        showApartmentSelectionModal = false
                    },
                    onBack = { showApartmentSelectionModal = false }
                )
            }
        }
    }

    // Apartment History Chronological Modal
    if (showApartmentHistoryModal && currentApt != null) {
        Dialog(
            onDismissRequest = { showApartmentHistoryModal = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                ApartmentHistoryScreen(
                    apartment = currentApt,
                    viewModel = viewModel,
                    onBack = { showApartmentHistoryModal = false },
                    onNewReading = {
                        showApartmentHistoryModal = false
                    }
                )
            }
        }
    }
}
