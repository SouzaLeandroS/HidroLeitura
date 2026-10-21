package com.example.domain

import com.example.data.model.Apartment
import com.example.data.model.MeterReading

data class ApartmentMonthlyReport(
    val apartment: Apartment,
    val referenceMonth: String,
    val currentReading: MeterReading?,
    val previousReading: MeterReading?,
    val consumptionM3: Double,
    val consumptionLiters: Double,
    val waterCost: Double,
    val sewageCost: Double,
    val fixedFee: Double,
    val totalCost: Double,
    val previousMonthConsumptionM3: Double?,
    val percentageChange: Double?,
    val isLeakAlert: Boolean,
    val isPending: Boolean
)

data class CondoMonthlySummary(
    val referenceMonth: String,
    val monthFormatted: String,
    val totalApartments: Int,
    val readApartments: Int,
    val pendingApartments: Int,
    val totalConsumptionM3: Double,
    val totalCost: Double,
    val averageConsumptionM3: Double,
    val highestConsumptionReport: ApartmentMonthlyReport?,
    val lowestConsumptionReport: ApartmentMonthlyReport?,
    val leakAlertCount: Int,
    val apartmentReports: List<ApartmentMonthlyReport>
)
