package com.example.kotlinapp

import com.example.kotlinapp.data.model.WithdrawalInput
import org.junit.Assert.*
import org.junit.Test

class WithdrawalInputTest {
    // 2026-11-10 04:30 UTC is still November 9 in Bogota.
    private val now = 1794285000000L

    @Test fun dateUsesBogotaDayAndRejectsImpossibleOrMalformedDates() {
        assertTrue(WithdrawalInput.isCurrentDate("2026-11-09", now))
        assertTrue(WithdrawalInput.isCurrentDate("2028-02-29", now))
        listOf("2026-11-08", "2027-02-29", "2027-02-30", "2027-13-01",
            "2027-1-01", "2027-01-01x", "0000-01-01", "", "x".repeat(10000))
            .forEach { assertFalse(it, WithdrawalInput.isCurrentDate(it, now)) }
    }

    @Test fun requestChecksRequiredFieldsAndBoundsBeforeSending() {
        fun error(use: String = "Construir un banco", date: String = "2027-02-01",
                  quantity: String = "2 unidades", note: String = "") =
            WithdrawalInput.error(use, date, quantity, note, now)
        assertNull(error())
        assertNotNull(error(use = "   "))
        assertNotNull(error(use = "x".repeat(501)))
        assertNotNull(error(date = "2027-02-30"))
        assertNotNull(error(quantity = " "))
        assertNotNull(error(quantity = "x".repeat(101)))
        assertNotNull(error(note = "x".repeat(1001)))
        assertNull(error(use = "x".repeat(500), quantity = "x".repeat(100),
            note = "x".repeat(1000)))
    }
}
