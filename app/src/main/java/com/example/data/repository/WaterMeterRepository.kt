package com.example.data.repository

import com.example.data.local.ApartmentDao
import com.example.data.local.CondoSettingsDao
import com.example.data.local.MeterReadingDao
import com.example.data.model.Apartment
import com.example.data.model.CondoSettings
import com.example.data.model.MeterReading
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WaterMeterRepository(
    private val apartmentDao: ApartmentDao,
    private val meterReadingDao: MeterReadingDao,
    private val condoSettingsDao: CondoSettingsDao
) {
    val apartments: Flow<List<Apartment>> = apartmentDao.getAllApartments()
    val allReadings: Flow<List<MeterReading>> = meterReadingDao.getAllReadings()
    val settings: Flow<CondoSettings?> = condoSettingsDao.getSettings()
    val recordedMonths: Flow<List<String>> = meterReadingDao.getAllRecordedMonths()

    fun getReadingsForMonth(month: String): Flow<List<MeterReading>> =
        meterReadingDao.getReadingsForMonth(month)

    fun getReadingsForApartment(apartmentId: Long): Flow<List<MeterReading>> =
        meterReadingDao.getReadingsForApartment(apartmentId)

    suspend fun getApartmentById(id: Long): Apartment? =
        apartmentDao.getApartmentById(id)

    suspend fun insertApartment(apartment: Apartment): Long =
        apartmentDao.insertApartment(apartment)

    suspend fun updateApartment(apartment: Apartment) =
        apartmentDao.updateApartment(apartment)

    suspend fun deleteApartment(apartment: Apartment) =
        apartmentDao.deleteApartment(apartment)

    suspend fun insertReading(reading: MeterReading): Long =
        meterReadingDao.insertReading(reading)

    suspend fun updateReading(reading: MeterReading) =
        meterReadingDao.updateReading(reading)

    suspend fun deleteReading(reading: MeterReading) =
        meterReadingDao.deleteReading(reading)

    suspend fun getReadingForApartmentAndMonth(apartmentId: Long, month: String): MeterReading? =
        meterReadingDao.getReadingForApartmentAndMonth(apartmentId, month)

    suspend fun getPreviousReading(apartmentId: Long, currentMonth: String): MeterReading? =
        meterReadingDao.getPreviousReading(apartmentId, currentMonth)

    suspend fun getSettingsOnce(): CondoSettings =
        condoSettingsDao.getSettingsOnce() ?: CondoSettings()

    suspend fun saveSettings(settings: CondoSettings) =
        condoSettingsDao.insertOrUpdate(settings)

    suspend fun ensureDefaultData() {
        val count = apartmentDao.countApartments()
        if (count == 0) {
            seedSampleData()
        }
    }

    suspend fun seedSampleData() {
        // Default settings
        condoSettingsDao.insertOrUpdate(
            CondoSettings(
                id = 1,
                condoName = "Condomínio Residencial Jardins",
                waterRatePerM3 = 6.80,
                sewageRatePercent = 80.0,
                fixedFeePerUnit = 12.00,
                leakThresholdM3 = 18.0
            )
        )

        // Seed 6 realistic apartments
        val sampleApartments = listOf(
            Apartment(
                block = "Bloco A",
                unitNumber = "101",
                residentName = "Mariana Siqueira",
                residentPhone = "(11) 98123-4567",
                meterSerialNumber = "19J 042371",
                initialReadingM3 = 160.0,
                notes = "Hidrômetro corredor térreo"
            ),
            Apartment(
                block = "Bloco A",
                unitNumber = "102",
                residentName = "Roberto Almeida",
                residentPhone = "(11) 97234-5678",
                meterSerialNumber = "HYD-88210",
                initialReadingM3 = 110.0,
                notes = "Família com 4 pessoas"
            ),
            Apartment(
                block = "Bloco A",
                unitNumber = "201",
                residentName = "Juliana Mendes",
                residentPhone = "(11) 99345-6789",
                meterSerialNumber = "ACT-55019",
                initialReadingM3 = 95.0,
                notes = "Morador viaja frequentemente"
            ),
            Apartment(
                block = "Bloco B",
                unitNumber = "101",
                residentName = "Lucas Fernando",
                residentPhone = "(11) 96456-7890",
                meterSerialNumber = "HYD-99104",
                initialReadingM3 = 210.0,
                notes = "Hidrômetro individualizado"
            ),
            Apartment(
                block = "Bloco B",
                unitNumber = "102",
                residentName = "Camila & André",
                residentPhone = "(11) 95567-8901",
                meterSerialNumber = "ZNR-33491",
                initialReadingM3 = 140.0,
                notes = "Possui máquina de lavar automática"
            ),
            Apartment(
                block = "Bloco B",
                unitNumber = "201",
                residentName = "Beatriz Vasconcelos",
                residentPhone = "(11) 94678-9012",
                meterSerialNumber = "ACT-77182",
                initialReadingM3 = 80.0,
                notes = "Apto 2º andar"
            )
        )

        val insertedIds = mutableListOf<Long>()
        for (apt in sampleApartments) {
            val id = apartmentDao.insertApartment(apt)
            insertedIds.add(id)
        }

        val cal = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        
        // Month -1 (previous month)
        cal.add(Calendar.MONTH, -1)
        val prevMonth = monthFormat.format(cal.time)
        val prevTimestamp = cal.timeInMillis

        // Month 0 (current month)
        cal.add(Calendar.MONTH, 1)
        val currMonth = monthFormat.format(cal.time)
        val currTimestamp = cal.timeInMillis

        // Previous month readings
        val prevReadings = listOf(
            MeterReading(apartmentId = insertedIds[0], referenceMonth = prevMonth, readingDate = prevTimestamp, readingM3 = 172.50, blackDigits = 172, redDigits = 500, verifiedByUser = true),
            MeterReading(apartmentId = insertedIds[1], referenceMonth = prevMonth, readingDate = prevTimestamp, readingM3 = 125.10, blackDigits = 125, redDigits = 100, verifiedByUser = true),
            MeterReading(apartmentId = insertedIds[2], referenceMonth = prevMonth, readingDate = prevTimestamp, readingM3 = 101.30, blackDigits = 101, redDigits = 300, verifiedByUser = true),
            MeterReading(apartmentId = insertedIds[3], referenceMonth = prevMonth, readingDate = prevTimestamp, readingM3 = 224.80, blackDigits = 224, redDigits = 800, verifiedByUser = true),
            MeterReading(apartmentId = insertedIds[4], referenceMonth = prevMonth, readingDate = prevTimestamp, readingM3 = 153.20, blackDigits = 153, redDigits = 200, verifiedByUser = true),
            MeterReading(apartmentId = insertedIds[5], referenceMonth = prevMonth, readingDate = prevTimestamp, readingM3 = 88.00, blackDigits = 88, redDigits = 0, verifiedByUser = true)
        )
        meterReadingDao.insertReadings(prevReadings)

        // Current month readings (some completed, some pending)
        val currReadings = listOf(
            // Apto 101: 184.72 m³ (consumption: 12.22 m³)
            MeterReading(apartmentId = insertedIds[0], referenceMonth = currMonth, readingDate = currTimestamp, readingM3 = 184.72, blackDigits = 184, redDigits = 720, verifiedByUser = true, notes = "Leitura foto IA confirmada"),
            // Apto 102: 139.80 m³ (consumption: 14.70 m³)
            MeterReading(apartmentId = insertedIds[1], referenceMonth = currMonth, readingDate = currTimestamp, readingM3 = 139.80, blackDigits = 139, redDigits = 800, verifiedByUser = true),
            // Apto 201: 124.90 m³ (consumption: 23.60 m³ - Alerta de Alto Consumo!)
            MeterReading(apartmentId = insertedIds[2], referenceMonth = currMonth, readingDate = currTimestamp, readingM3 = 124.90, blackDigits = 124, redDigits = 900, verifiedByUser = true, notes = "Verificar descarga acoplada vazando"),
            // Apto 101 Bloco B: 236.40 m³ (consumption: 11.60 m³)
            MeterReading(apartmentId = insertedIds[3], referenceMonth = currMonth, readingDate = currTimestamp, readingM3 = 236.40, blackDigits = 236, redDigits = 400, verifiedByUser = true)
            // Apto 102 and 201 Bloco B are left pending for the user to try reading!
        )
        meterReadingDao.insertReadings(currReadings)
    }
}
