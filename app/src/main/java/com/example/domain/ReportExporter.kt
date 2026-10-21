package com.example.domain

import android.content.Context
import android.content.Intent
import com.example.data.model.CondoSettings
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    private val currencyFormat = DecimalFormat("R$ #,##0.00")
    private val m3Format = DecimalFormat("#,##0.000")
    private val litersFormat = DecimalFormat("#,##0")
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

    fun formatApartmentReceipt(
        report: ApartmentMonthlyReport,
        settings: CondoSettings
    ): String {
        val apt = report.apartment
        val monthName = MonthlyReportCalculator.formatReferenceMonth(report.referenceMonth)
        val readDateStr = report.currentReading?.readingDate?.let {
            dateFormat.format(Date(it))
        } ?: "Pendente"

        val sb = StringBuilder()
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("   💧 COMPROVANTE DE CONSUMO DE ÁGUA 💧")
        sb.appendLine("   ${settings.condoName}")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("📍 Unidade: ${apt.displayTitle}")
        sb.appendLine("👤 Morador: ${apt.residentName}")
        if (apt.meterSerialNumber.isNotBlank()) {
            sb.appendLine("🔢 Nº Hidrômetro: ${apt.meterSerialNumber}")
        }
        sb.appendLine("📅 Mês de Referência: $monthName")
        sb.appendLine("⏱ Data da Leitura: $readDateStr")
        sb.appendLine("────────────────────────────")

        if (report.isPending) {
            sb.appendLine("⚠️ LEITURA PENDENTE PARA ESTE MÊS")
            sb.appendLine("Taxa fixa básica: ${currencyFormat.format(report.fixedFee)}")
        } else {
            val prevM3 = report.previousReading?.readingM3 ?: apt.initialReadingM3
            val currM3 = report.currentReading?.readingM3 ?: 0.0

            sb.appendLine("📊 Leitura Anterior: ${m3Format.format(prevM3)} m³")
            sb.appendLine("📊 Leitura Atual:    ${m3Format.format(currM3)} m³")
            sb.appendLine("💧 CONSUMO TOTAL:   ${m3Format.format(report.consumptionM3)} m³ (${litersFormat.format(report.consumptionLiters)} L)")

            report.percentageChange?.let { pct ->
                val signal = if (pct >= 0) "+" else ""
                sb.appendLine("📈 Comparativo:      $signal${DecimalFormat("#0.0").format(pct)}% em relação ao mês anterior")
            }

            if (report.isLeakAlert) {
                sb.appendLine("🚨 ATENÇÃO: Consumo elevado detectado! Recomenda-se checar possíveis vazamentos em torneiras e caixas acopladas.")
            }

            sb.appendLine("────────────────────────────")
            sb.appendLine("💰 DEMONSTRATIVO FINANCEIRO:")
            sb.appendLine(" • Água (${m3Format.format(report.consumptionM3)} m³ × ${currencyFormat.format(settings.waterRatePerM3)}): ${currencyFormat.format(report.waterCost)}")
            sb.appendLine(" • Esgoto (${DecimalFormat("#0").format(settings.sewageRatePercent)}%): ${currencyFormat.format(report.sewageCost)}")
            sb.appendLine(" • Taxa de Manutenção/Fixa: ${currencyFormat.format(report.fixedFee)}")
            sb.appendLine("────────────────────────────")
            sb.appendLine("💵 VALOR TOTAL A PAGAR: ${currencyFormat.format(report.totalCost)}")
        }

        sb.appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("Emitido via HidroLeitura • Leitura Inteligente com IA")
        return sb.toString()
    }

    fun formatCondoSummaryText(
        summary: CondoMonthlySummary,
        settings: CondoSettings
    ): String {
        val sb = StringBuilder()
        sb.appendLine("==========================================")
        sb.appendLine("   📋 RELATÓRIO MENSAL DE CONSUMO DE ÁGUA")
        sb.appendLine("   ${settings.condoName}")
        sb.appendLine("==========================================")
        sb.appendLine("📅 Período: ${summary.monthFormatted}")
        sb.appendLine("🏢 Total de Unidades: ${summary.totalApartments}")
        sb.appendLine("✅ Unidades Lidas: ${summary.readApartments}")
        sb.appendLine("⏳ Unidades Pendentes: ${summary.pendingApartments}")
        sb.appendLine("💧 Consumo Total do Condomínio: ${m3Format.format(summary.totalConsumptionM3)} m³")
        sb.appendLine("💰 Faturamento Total: ${currencyFormat.format(summary.totalCost)}")
        sb.appendLine("📊 Consumo Médio por Apto: ${m3Format.format(summary.averageConsumptionM3)} m³")
        if (summary.leakAlertCount > 0) {
            sb.appendLine("🚨 Alertas de Consumo Alto / Vazamento: ${summary.leakAlertCount} unidade(s)")
        }
        sb.appendLine("------------------------------------------")
        sb.appendLine("DETALHAMENTO POR UNIDADE:")

        summary.apartmentReports.forEach { rep ->
            val status = if (rep.isPending) "[PENDENTE]" else "${m3Format.format(rep.consumptionM3)} m³ | ${currencyFormat.format(rep.totalCost)}"
            val alert = if (rep.isLeakAlert) " ⚠️ ALERTA VAZAMENTO" else ""
            sb.appendLine(" • ${rep.apartment.displayTitle} (${rep.apartment.residentName}): $status$alert")
        }

        sb.appendLine("==========================================")
        return sb.toString()
    }

    fun shareText(context: Context, text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
