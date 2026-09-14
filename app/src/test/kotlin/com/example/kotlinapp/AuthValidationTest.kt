package com.example.kotlinapp

import com.example.kotlinapp.data.repository.AuthRepository
import com.example.kotlinapp.data.repository.SpeciesRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {

    @Test
    fun testEmailValidation() {
        assertTrue(AuthRepository.validateEmail("forest@woodmatch.org"))
        assertTrue(AuthRepository.validateEmail("user.name@domain.co"))
        assertFalse(AuthRepository.validateEmail("invalid-email"))
        assertFalse(AuthRepository.validateEmail(""))
    }

    @Test
    fun testColombianPhoneValidation() {
        // Starts with +57 and 10 digits
        assertTrue(AuthRepository.validatePhone("+573001234567"))
        assertTrue(AuthRepository.validatePhone("+573159876543"))
        assertFalse(AuthRepository.validatePhone("3001234567")) // missing +57
        assertFalse(AuthRepository.validatePhone("+57300123456")) // 9 digits
        assertFalse(AuthRepository.validatePhone("+5730012345678")) // 11 digits
    }

    @Test
    fun testPasswordValidation() {
        // Min 8 chars, 1 upper, 1 lower, 1 digit
        assertTrue(AuthRepository.validatePassword("WoodMatch2025*"))
        assertFalse(AuthRepository.validatePassword("short1A"))
        assertFalse(AuthRepository.validatePassword("alllowercase1"))
        assertFalse(AuthRepository.validatePassword("ALLUPPERCASE1"))
        assertFalse(AuthRepository.validatePassword("NoDigitsPassword"))
    }

    @Test
    fun testSpeciesSearch() {
        val robleResults = SpeciesRepository.searchSpecies("Roble", "Todos")
        assertTrue(robleResults.isNotEmpty())
        assertTrue(robleResults.any { it.commonName.contains("Roble", ignoreCase = true) })

        val autumnResults = SpeciesRepository.searchSpecies("", "Otoño")
        assertTrue(autumnResults.isNotEmpty())
        assertTrue(autumnResults.all { it.season == "Otoño" })
    }
}
