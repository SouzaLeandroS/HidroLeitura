package com.example.domain

import com.example.data.model.Apartment
import com.example.data.model.CondoSettings
import com.example.data.model.MeterReading
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

object MonthlyReportCalculator {

    fun generateCondoSummary(
        referenceMonth: String,
        apartments: List<Apartment>,
        allReadings: List<MeterReading>,
        settings: CondoSettings
    ): CondoMonthlySummary {
        val monthFormatted = formatReferenceMonth(referenceMonth)
        val reports = apartments.map { apt ->
            generateApartmentReport(apt, referenceMonth, allReadings, settings)
        }

        val completed = reports.filter { !it.isPending }
        val readCount = completed.size
        val pendingCount = reports.size - readCount
        val totalM3 = completed.sumOf { it.consumptionM3 }
        val totalCost = completed.sumOf { it.totalCost }
        val avgM3 = if (readCount > 0) totalM3 / readCount else 0.0

        val highest = completed.maxByOrNull { it.consumptionM3 }
        val lowest = completed.minByOrNull { it.consumptionM3 }
        val leakAlerts = completed.count { it.isLeakAlert }

        return CondoMonthlySummary(
            referenceMonth = referenceMonth,
            monthFormatted = monthFormatted,
            totalApartments = apartments.size,
            readApartments = readCount,
            pendingApartments = pendingCount,
            totalConsumptionM3 = totalM3,
            totalCost = totalCost,
            averageConsumptionM3 = avgM3,
            highestConsumptionReport = highest,
            lowestConsumptionReport = lowest,
            leakAlertCount = leakAlerts,
            apartmentReports = reports
        )
    }

    fun generateApartmentReport(
        apartment: Apartment,
        referenceMonth: String,
        allReadings: List<MeterReading>,
        settings: CondoSettings
    ): ApartmentMonthlyReport {
        val aptReadings = allReadings.filter { it.apartmentId == apartment.id }
        val currentReading = aptReadings.firstOrNull { it.referenceMonth == referenceMonth }

        // Find immediately preceding reading
        val previousReading = aptReadings
            .filter { it.referenceMonth < referenceMonth }
            .maxByOrNull { it.referenceMonth }

        if (currentReading == null) {
            return ApartmentMonthlyReport(
                apartment = apartment,
                referenceMonth = referenceMonth,
                currentReading = null,
                previousReading = previousReading,
                consumptionM3 = 0.0,
                consumptionLiters = 0.0,
                waterCost = 0.0,
                sewageCost = 0.0,
                fixedFee = settings.fixedFeePerUnit,
                totalCost = settings.fixedFeePerUnit,
                previousMonthConsumptionM3 = null,
                percentageChange = null,
                isLeakAlert = false,
                isPending = true
            )
        }

        val basePrevM3 = previousReading?.readingM3 ?: apartment.initialReadingM3
        val consumptionM3 = max(0.0, currentReading.readingM3 - basePrevM3)
        val consumptionLiters = consumptionM3 * 1000.0

        val waterCost = consumptionM3 * settings.waterRatePerM3
        val sewageCost = waterCost * (settings.sewageRatePercent / 100.0)
        val fixedFee = settings.fixedFeePerUnit
        val totalCost = waterCost + sewageCost + fixedFee

        // Calculate comparison with previous month's consumption
        var prevConsumption: Double? = null
        var pctChange: Double? = null

        if (previousReading != null) {
            val beforePrevReading = aptReadings
                .filter { it.referenceMonth < previousReading.referenceMonth }
                .maxByOrNull { it.referenceMonth }
            val olderBase = beforePrevReading?.readingM3 ?: apartment.initialReadingM3
            val olderConsumption = max(0.0, previousReading.readingM3 - olderBase)
            prevConsumption = olderConsumption
            if (olderConsumption > 0.0) {
                pctChange = ((consumptionM3 - olderConsumption) / olderConsumption) * 100.0
            }
        }

        val isLeakAlert = consumptionM3 >= settings.leakThresholdM3 || (pctChange != null && pctChange > 50.0 && consumptionM3 > 10.0)

        return ApartmentMonthlyReport(
            apartment = apartment,
            referenceMonth = referenceMonth,
            currentReading = currentReading,
            previousReading = previousReading,
            consumptionM3 = consumptionM3,
            consumptionLiters = consumptionLiters,
            waterCost = waterCost,
            sewageCost = sewageCost,
            fixedFee = fixedFee,
            totalCost = totalCost,
            previousMonthConsumptionM3 = prevConsumption,
            percentageChange = pctChange,
            isLeakAlert = isLeakAlert,
            isPending = false
        )
    }

    fun formatReferenceMonth(monthIso: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val date = parser.parse(monthIso) ?: return monthIso
            val formatter = SimpleDateFormat("MMMM 'de' yyyy", Locale("pt", "BR"))
            formatter.format(date).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            monthIso
        }
    }
}
