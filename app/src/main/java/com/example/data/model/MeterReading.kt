package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meter_readings",
    foreignKeys = [
        ForeignKey(
            entity = Apartment::class,
            parentColumns = ["id"],
            childColumns = ["apartmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("apartmentId"),
        Index(value = ["apartmentId", "referenceMonth"], unique = true)
    ]
)
data class MeterReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val apartmentId: Long,
    val readingDate: Long = System.currentTimeMillis(),
    val referenceMonth: String,    // Format "YYYY-MM", ex: "2026-10"
    val readingM3: Double,         // Total reading in m³, ex: 184.72
    val blackDigits: Int,          // M³ inteiros (roletes pretos, ex: 184)
    val redDigits: Int,            // Litros / frações (roletes vermelhos, ex: 72 ou 720)
    val photoPath: String? = null, // URI or internal file path to captured photo
    val aiRawDetected: String? = null,
    val aiConfidence: String? = null,
    val verifiedByUser: Boolean = true,
    val notes: String = ""
)
