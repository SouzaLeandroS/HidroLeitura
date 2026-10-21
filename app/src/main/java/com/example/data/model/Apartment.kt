package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apartments")
data class Apartment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val block: String,             // ex: "Bloco A", "Torre 1"
    val unitNumber: String,        // ex: "101", "102", "201"
    val residentName: String,      // ex: "Carlos Silva"
    val residentPhone: String = "",// ex: "(11) 98765-4321"
    val meterSerialNumber: String = "", // ex: "19J 042371"
    val initialReadingM3: Double = 0.0, // leitura inicial cadastrada
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val displayTitle: String
        get() = if (block.isNotBlank() && block != "Único") "$block - Apto $unitNumber" else "Apto $unitNumber"
}
