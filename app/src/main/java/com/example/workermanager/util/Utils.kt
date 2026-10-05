package com.example.workermanager.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object Utils {
    private val rupeeFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun formatCurrency(amount: Double): String {
        return rupeeFormat.format(amount)
    }

    fun formatDate(date: LocalDate): String {
        return date.format(dateFormatter)
    }
}
