package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "condo_settings")
data class CondoSettings(
    @PrimaryKey
    val id: Int = 1,
    val condoName: String = "Condomínio Residencial",
    val waterRatePerM3: Double = 6.80,    // Tarifa de água por m³ em R$
    val sewageRatePercent: Double = 80.0, // Taxa de esgoto (% sobre água, comum 80% a 100%)
    val fixedFeePerUnit: Double = 10.00,  // Taxa fixa / taxa de administração por unidade em R$
    val leakThresholdM3: Double = 15.0    // Limite para alerta de alto consumo / vazamento (m³/mês)
) {
    // Tarifa total combinada por m³ (água + esgoto)
    val totalRatePerM3: Double
        get() = waterRatePerM3 * (1.0 + (sewageRatePercent / 100.0))
}
