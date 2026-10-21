package com.example.ui

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.ai.GeminiMeterReader
import com.example.data.ai.MeterRecognitionResult
import com.example.data.local.AppDatabase
import com.example.data.model.Apartment
import com.example.data.model.CondoSettings
import com.example.data.model.MeterReading
import com.example.data.repository.WaterMeterRepository
import com.example.domain.CondoMonthlySummary
import com.example.domain.MonthlyReportCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ScreenTab(val title: String) {
    LEITURA("Leitura"),
    APARTAMENTOS("Unidades"),
    RELATORIOS("Relatórios"),
    CONFIGURACOES("Ajustes")
}

enum class ReportFilter {
    TODOS,
    PENDENTES,
    ALERTAS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = WaterMeterRepository(
        apartmentDao = database.apartmentDao(),
        meterReadingDao = database.meterReadingDao(),
        condoSettingsDao = database.condoSettingsDao()
    )
    private val geminiReader = GeminiMeterReader(application)

    // Current month format YYYY-MM
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val currentMonthKey: String = monthFormat.format(Date())

    private val _selectedMonth = MutableStateFlow(currentMonthKey)
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _currentTab = MutableStateFlow(ScreenTab.LEITURA)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    private val _selectedApartment = MutableStateFlow<Apartment?>(null)
    val selectedApartment: StateFlow<Apartment?> = _selectedApartment.asStateFlow()

    // Drafting state for photo reading
    private val _draftBlackDigits = MutableStateFlow("00184")
    val draftBlackDigits: StateFlow<String> = _draftBlackDigits.asStateFlow()

    private val _draftRedDigits = MutableStateFlow("72")
    val draftRedDigits: StateFlow<String> = _draftRedDigits.asStateFlow()

    private val _draftPhotoUri = MutableStateFlow<Uri?>(null)
    val draftPhotoUri: StateFlow<Uri?> = _draftPhotoUri.asStateFlow()

    private val _draftNotes = MutableStateFlow("")
    val draftNotes: StateFlow<String> = _draftNotes.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _lastAiResult = MutableStateFlow<MeterRecognitionResult?>(null)
    val lastAiResult: StateFlow<MeterRecognitionResult?> = _lastAiResult.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _reportFilter = MutableStateFlow(ReportFilter.TODOS)
    val reportFilter: StateFlow<ReportFilter> = _reportFilter.asStateFlow()

    private val _snackBarMessage = MutableStateFlow<String?>(null)
    val snackBarMessage: StateFlow<String?> = _snackBarMessage.asStateFlow()

    val apartments: StateFlow<List<Apartment>> = repository.apartments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReadings: StateFlow<List<MeterReading>> = repository.allReadings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<CondoSettings> = repository.settings
        .map { it ?: CondoSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CondoSettings())

    // Calculated Monthly Summary reactive to apartments, readings, month, and settings
    val monthlySummary: StateFlow<CondoMonthlySummary?> = combine(
        selectedMonth,
        apartments,
        allReadings,
        settings
    ) { month: String, aptList: List<Apartment>, readingList: List<MeterReading>, condoSettings: CondoSettings ->
        if (aptList.isEmpty()) null
        else MonthlyReportCalculator.generateCondoSummary(month, aptList, readingList, condoSettings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Available months list
    val recordedMonths: StateFlow<List<String>> = repository.recordedMonths
        .map { recorded: List<String> ->
            val set = sortedSetOf<String>(Comparator.reverseOrder())
            set.add(currentMonthKey)
            set.addAll(recorded)
            // Add previous 2 months as well for convenience
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -1)
            set.add(monthFormat.format(cal.time))
            cal.add(Calendar.MONTH, -1)
            set.add(monthFormat.format(cal.time))
            set.toList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(currentMonthKey))

    init {
        viewModelScope.launch {
            repository.ensureDefaultData()
        }
    }

    fun selectTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun selectMonth(month: String) {
        _selectedMonth.value = month
    }

    fun selectApartment(apartment: Apartment?) {
        _selectedApartment.value = apartment
        if (apartment != null) {
            // Check if there is an existing reading for this month to pre-fill
            viewModelScope.launch {
                val existing = repository.getReadingForApartmentAndMonth(apartment.id, _selectedMonth.value)
                if (existing != null) {
                    _draftBlackDigits.value = existing.blackDigits.toString().padStart(5, '0')
                    _draftRedDigits.value = existing.redDigits.toString().padStart(2, '0')
                    _draftNotes.value = existing.notes
                    existing.photoPath?.let { _draftPhotoUri.value = Uri.parse(it) }
                } else {
                    val prev = repository.getPreviousReading(apartment.id, _selectedMonth.value)
                    val base = prev?.readingM3 ?: apartment.initialReadingM3
                    val baseBlack = base.toInt()
                    val baseRed = ((base - baseBlack) * 100).toInt()
                    _draftBlackDigits.value = baseBlack.toString().padStart(5, '0')
                    _draftRedDigits.value = baseRed.toString().padStart(2, '0')
                    _draftNotes.value = ""
                    _draftPhotoUri.value = null
                    _lastAiResult.value = null
                }
            }
        }
    }

    fun setDraftDigits(black: String, red: String) {
        _draftBlackDigits.value = black
        _draftRedDigits.value = red
    }

    fun setDraftNotes(notes: String) {
        _draftNotes.value = notes
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setReportFilter(filter: ReportFilter) {
        _reportFilter.value = filter
    }

    fun clearSnackBar() {
        _snackBarMessage.value = null
    }

    fun processImageUri(uri: Uri) {
        _draftPhotoUri.value = uri
        _isAnalyzing.value = true
        _lastAiResult.value = null

        viewModelScope.launch {
            try {
                val result = geminiReader.analyzeMeterImage(uri)
                _lastAiResult.value = result
                if (result.success) {
                    _draftBlackDigits.value = result.blackDigits
                    _draftRedDigits.value = result.redDigits
                    _snackBarMessage.value = "Leitura detectada: ${result.totalM3} m³"
                } else {
                    _snackBarMessage.value = result.errorMessage ?: "Erro ao analisar o relógio"
                }
            } catch (e: Exception) {
                _snackBarMessage.value = "Erro: ${e.localizedMessage}"
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun processSampleMeterImage() {
        _isAnalyzing.value = true
        _lastAiResult.value = null

        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val bitmap = BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.sample_water_meter_1792608832729
                )

                // Save sample bitmap to temporary file to get a Uri
                val tempFile = File(context.cacheDir, "sample_meter_temp.jpg")
                FileOutputStream(tempFile).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                _draftPhotoUri.value = Uri.fromFile(tempFile)

                val result = geminiReader.analyzeMeterBitmap(bitmap)
                _lastAiResult.value = result
                if (result.success) {
                    _draftBlackDigits.value = result.blackDigits
                    _draftRedDigits.value = result.redDigits
                    _snackBarMessage.value = "Hidrômetro de teste reconhecido: ${result.totalM3} m³"
                } else {
                    _snackBarMessage.value = result.errorMessage ?: "Falha ao reconhecer"
                }
            } catch (e: Exception) {
                _snackBarMessage.value = "Erro no teste: ${e.localizedMessage}"
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun saveCurrentReading() {
        val apt = _selectedApartment.value
        if (apt == null) {
            _snackBarMessage.value = "Selecione uma unidade/apartamento primeiro."
            return
        }

        val black = _draftBlackDigits.value.toIntOrNull() ?: 0
        val redStr = _draftRedDigits.value.ifBlank { "00" }
        val red = redStr.toIntOrNull() ?: 0
        val divisor = Math.pow(10.0, redStr.length.toDouble())
        val totalM3 = black + (red / divisor)

        viewModelScope.launch {
            val reading = MeterReading(
                apartmentId = apt.id,
                referenceMonth = _selectedMonth.value,
                readingDate = System.currentTimeMillis(),
                readingM3 = totalM3,
                blackDigits = black,
                redDigits = red,
                photoPath = _draftPhotoUri.value?.toString(),
                aiRawDetected = _lastAiResult.value?.rawResponse,
                aiConfidence = _lastAiResult.value?.confidence,
                verifiedByUser = true,
                notes = _draftNotes.value
            )

            repository.insertReading(reading)
            _snackBarMessage.value = "Leitura de ${apt.displayTitle} salva com sucesso!"

            // Automatically pick next pending apartment in current month if available!
            val aptList = apartments.value
            val readingsList = allReadings.value
            val currentMonth = _selectedMonth.value
            val nextPending = aptList.firstOrNull { candidate ->
                candidate.id != apt.id && readingsList.none { it.apartmentId == candidate.id && it.referenceMonth == currentMonth }
            }
            if (nextPending != null) {
                selectApartment(nextPending)
            }
        }
    }

    fun saveApartment(apartment: Apartment) {
        viewModelScope.launch {
            if (apartment.id == 0L) {
                val newId = repository.insertApartment(apartment)
                _snackBarMessage.value = "Apartamento adicionado com sucesso!"
                val added = repository.getApartmentById(newId)
                if (_selectedApartment.value == null) {
                    _selectedApartment.value = added
                }
            } else {
                repository.updateApartment(apartment)
                _snackBarMessage.value = "Dados da unidade atualizados!"
                if (_selectedApartment.value?.id == apartment.id) {
                    _selectedApartment.value = apartment
                }
            }
        }
    }

    fun deleteApartment(apartment: Apartment) {
        viewModelScope.launch {
            repository.deleteApartment(apartment)
            _snackBarMessage.value = "Unidade excluída."
            if (_selectedApartment.value?.id == apartment.id) {
                _selectedApartment.value = null
            }
        }
    }

    fun saveSettings(newSettings: CondoSettings) {
        viewModelScope.launch {
            repository.saveSettings(newSettings)
            _snackBarMessage.value = "Configurações e tarifas atualizadas!"
        }
    }

    fun resetToSampleData() {
        viewModelScope.launch {
            repository.seedSampleData()
            _snackBarMessage.value = "Dados de demonstração recarregados com sucesso!"
        }
    }
}
