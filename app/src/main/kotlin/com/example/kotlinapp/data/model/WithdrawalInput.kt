package com.example.kotlinapp.data.model

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Client feedback only; the server repeats validation and authorization. */
object WithdrawalInput {
    private val datePattern = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")

    fun isCurrentDate(value: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!datePattern.matches(value)) return false
        val zone = TimeZone.getTimeZone("America/Bogota")
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            isLenient = false
            timeZone = zone
        }
        val position = ParsePosition(0)
        val date = format.parse(value, position) ?: return false
        if (position.index != value.length || format.format(date) != value) return false
        val nextDay = Calendar.getInstance(zone).apply {
            time = date
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        return nowMillis < nextDay
    }

    fun error(
        intendedUse: String,
        proposedDate: String,
        quantity: String,
        message: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): String? = when {
        intendedUse.isBlank() || intendedUse.length > 500 ->
            "Explica el uso del material en hasta 500 caracteres."
        !isCurrentDate(proposedDate, nowMillis) ->
            "Elige una fecha vigente con formato AAAA-MM-DD."
        quantity.isBlank() || quantity.length > 100 ->
            "Indica la cantidad en hasta 100 caracteres."
        message.length > 1000 ->
            "El mensaje admite hasta 1000 caracteres."
        else -> null
    }
}
