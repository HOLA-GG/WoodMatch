package com.example.kotlinapp

import com.example.kotlinapp.data.repository.AuthRepository
import com.example.kotlinapp.data.repository.SpeciesRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {

    @Test
    fun loginCannotInventAnAccountWhenFirebaseIsUnavailable() = kotlinx.coroutines.runBlocking {
        val result = AuthRepository.login("unregistered@example.org", "AnyPassword123")
        assertFalse(result.isSuccess)
        assertFalse(AuthRepository.isAuthenticated)
    }

    @Test
    fun guestCannotPublishOrRequest() {
        val guest = com.example.kotlinapp.data.model.User(uid = "guest_explore", role = "INVITADO")
        assertFalse(guest.canPublish)
        assertFalse(guest.canRequest)
        assertFalse(guest.isAdmin)
    }

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

        val shrubResults = SpeciesRepository.searchSpecies("", "Todos", "Arbusto")
        assertTrue(shrubResults.isNotEmpty())
        assertTrue(shrubResults.all { it.category == "Arbusto" })

        val andinaResults = SpeciesRepository.searchSpecies("", "Todos", "Todos", "Andina")
        assertTrue(andinaResults.isNotEmpty())
        assertTrue(andinaResults.all { it.regionGroup == "Andina" || it.nativeRegion.contains("Andina", ignoreCase = true) })
    }

    @Test
    fun testPlantNetCandidateToSpecies() {
        val candidate = com.example.kotlinapp.data.network.PlantNetCandidate(
            scientificName = "Handroanthus chrysanthus",
            authorship = "(Jacq.) S.O.Grose",
            family = "Bignoniaceae",
            commonNames = listOf("guayacán amarillo", "araguaney"),
            score = 0.942,
            imageUrl = "https://example.com/flower.jpg",
            organ = "flower"
        )

        org.junit.Assert.assertEquals(94, candidate.scorePercentage)
        org.junit.Assert.assertEquals("Guayacán amarillo", candidate.primaryCommonName)

        val species = candidate.toTreeSpecies("Caribe")
        org.junit.Assert.assertEquals("Guayacán amarillo", species.commonName)
        org.junit.Assert.assertEquals("Bignoniaceae", species.family)
        org.junit.Assert.assertEquals("Caribe", species.regionGroup)
        assertTrue(species.description.contains("94%"))
    }

    @Test
    fun testForestAiConsultantResponse() = kotlinx.coroutines.runBlocking {
        val response = com.example.kotlinapp.data.network.ForestAiService.askBotanicalConsultant("¿Qué árboles absorben más CO2?")
        assertTrue(response.isFailure)
        assertTrue(response.exceptionOrNull()?.message?.contains("Inicia sesión") == true)
    }
}
