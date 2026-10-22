package com.example.kotlinapp

import com.example.kotlinapp.data.model.RequestStatus
import com.example.kotlinapp.data.model.SubaSectors
import com.example.kotlinapp.data.model.User
import com.example.kotlinapp.data.model.UserRole
import com.example.kotlinapp.data.model.WasteCategory
import com.example.kotlinapp.data.model.WasteStatus
import com.example.kotlinapp.data.model.WithdrawalRequest
import com.example.kotlinapp.data.model.WoodWasteItem
import com.example.kotlinapp.data.network.CircularEconomyAiService
import com.example.kotlinapp.data.repository.WoodWasteRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CircularEconomyTest {

    @Test
    fun testUserRolePermissions() {
        val generador = User(uid = "g1", role = UserRole.GENERADOR.name)
        assertTrue(generador.canPublish)
        assertFalse(generador.isAdmin)

        val receptor = User(uid = "r1", role = UserRole.RECEPTOR.name)
        assertTrue(receptor.canRequest)
        assertFalse(receptor.isAdmin)

        val admin = User(uid = "a1", role = UserRole.ADMIN.name)
        assertTrue(admin.isAdmin)
        assertTrue(admin.canPublish)
        assertTrue(admin.canRequest)
    }

    @Test
    fun testStructuredQuantitiesFormatting() {
        val item1 = WoodWasteItem(
            quantityValue = 2.5,
            quantityUnit = "m³",
            quantityEstimated = true
        )
        assertEquals("2.5 m³ (Aprox.)", item1.formattedQuantity)

        val item2 = WoodWasteItem(
            quantityValue = 50.0,
            quantityUnit = "kg",
            quantityEstimated = false
        )
        assertEquals("50 kg", item2.formattedQuantity)
    }

    @Test
    fun testSubaSectorsCatalog() {
        assertTrue(SubaSectors.ALL.isNotEmpty())
        assertNotNull(SubaSectors.findByName("Suba Centro"))
        assertNotNull(SubaSectors.findByName("Niza / La Colina"))
        assertNotNull(SubaSectors.findByName("El Rincón"))
        assertNotNull(SubaSectors.findByName("Tibabuyes"))
    }

    @Test
    fun testUnauthenticatedUserCannotPublishOrRequest() = runBlocking {
        assertFalse(WoodWasteRepository.publishWasteItem(WoodWasteItem(title = "Material de prueba")).isSuccess)
        assertFalse(WoodWasteRepository.submitWithdrawalRequest(
            "missing", User(uid = "forged", role = UserRole.RECEPTOR.name),
            "Compost", "2026-10-25", "1 kg", "Prueba"
        ).isSuccess)
    }

    @Test
    fun testAiValorizationDisclaimerAndCategory() = runBlocking {
        val suggestion = CircularEconomyAiService.suggestValorization(
            wasteType = "Troncos y rollizos",
            materialState = "Seco / Estacionado",
            speciesName = "Acacia melanoxylon"
        )

        assertNotNull(suggestion)
        assertEquals(WasteCategory.FINA, suggestion.recommendedCategory)
        assertTrue(suggestion.suggestedUses.isNotBlank())
        // Verificar que incluye el aviso obligatorio de no certificación estructural
        assertTrue(suggestion.disclaimer.contains("No constituye certificación estructural"))
    }
}
