package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Apartment
import com.example.data.model.CondoSettings
import com.example.data.model.MeterReading
import com.example.domain.MonthlyReportCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("HidroLeitura", appName)
    }

    @Test
    fun `calculate apartment consumption and financial cost correctly`() {
        val apt = Apartment(
            id = 1L,
            block = "Bloco A",
            unitNumber = "101",
            residentName = "Mariana Siqueira",
            initialReadingM3 = 100.0
        )

        val previousReading = MeterReading(
            apartmentId = 1L,
            referenceMonth = "2026-09",
            readingM3 = 120.0,
            blackDigits = 120,
            redDigits = 0
        )

        val currentReading = MeterReading(
            apartmentId = 1L,
            referenceMonth = "2026-10",
            readingM3 = 135.5,
            blackDigits = 135,
            redDigits = 500
        )

        val settings = CondoSettings(
            waterRatePerM3 = 10.00,
            sewageRatePercent = 80.0,
            fixedFeePerUnit = 5.00,
            leakThresholdM3 = 20.0
        )

        val report = MonthlyReportCalculator.generateApartmentReport(
            apartment = apt,
            referenceMonth = "2026-10",
            allReadings = listOf(previousReading, currentReading),
            settings = settings
        )

        // Consumption = 135.5 - 120.0 = 15.5 m³
        assertEquals(15.5, report.consumptionM3, 0.001)
        assertEquals(15500.0, report.consumptionLiters, 0.001)

        // Water cost = 15.5 * 10 = 155.00
        assertEquals(155.00, report.waterCost, 0.01)

        // Sewage cost = 155.00 * 80% = 124.00
        assertEquals(124.00, report.sewageCost, 0.01)

        // Total cost = 155 + 124 + 5 = 284.00
        assertEquals(284.00, report.totalCost, 0.01)
    }

    @Test
    fun `store apartment and readings using Room DAOs`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val aptDao = db.apartmentDao()
        val readingDao = db.readingDao()

        val aptId = aptDao.insertApartment(
            Apartment(
                block = "Torre Sul",
                unitNumber = "302",
                residentName = "Felipe Ramos",
                meterSerialNumber = "WTR-9941"
            )
        )

        val reading = com.example.data.model.Reading(
            apartmentId = aptId,
            referenceMonth = "2026-10",
            readingM3 = 145.2,
            blackDigits = 145,
            redDigits = 200,
            verifiedByUser = true
        )

        val readingId = readingDao.insertReading(reading)
        assertTrue(readingId > 0)

        val retrieved = readingDao.getReadingForApartmentAndMonth(aptId, "2026-10")
        org.junit.Assert.assertNotNull(retrieved)
        assertEquals(145.2, retrieved!!.readingM3, 0.001)
        assertEquals(aptId, retrieved.apartmentId)

        db.close()
    }
}
