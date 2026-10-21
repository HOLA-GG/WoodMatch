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
    fun testWithdrawalLifecycleAndConcurrencyControl() = runBlocking {
        // 1. Publicar residuo
        val item = WoodWasteItem(
            id = "test_waste_100",
            title = "Troncos de Pino de Poda",
            quantityValue = 3.0,
            quantityUnit = "m³",
            subaSector = "Suba Centro",
            status = WasteStatus.DISPONIBLE.name
        )
        val pubRes = WoodWasteRepository.publishWasteItem(item)
        assertTrue(pubRes.isSuccess)

        // 2. Receptor 1 solicita retiro
        val receptor1 = User(uid = "rec_01", name = "Taller A", role = UserRole.RECEPTOR.name)
        val req1Res = WoodWasteRepository.submitWithdrawalRequest(
            wasteId = "test_waste_100",
            requester = receptor1,
            intendedUse = "Mesas rústicas",
            proposedDate = "2026-10-25",
            requestedQuantity = "3.0 m³",
            message = "Disponibilidad inmediata de flete"
        )
        assertTrue(req1Res.isSuccess)
        val req1Id = req1Res.getOrNull()!!

        // 3. Generador acepta la solicitud de Receptor 1 -> pasa a RESERVADO
        val acceptRes = WoodWasteRepository.acceptWithdrawalRequest("test_waste_100", req1Id)
        assertTrue(acceptRes.isSuccess)

        val updatedItem = WoodWasteRepository.items.value.find { it.id == "test_waste_100" }
        assertNotNull(updatedItem)
        assertEquals(WasteStatus.RESERVADO.name, updatedItem!!.status)

        // 4. Intentar aceptar una segunda solicitud mientras está reservado debe fallar por concurrencia
        val receptor2 = User(uid = "rec_02", name = "Taller B", role = UserRole.RECEPTOR.name)
        // Intentar solicitar cuando ya no está disponible
        val req2Res = WoodWasteRepository.submitWithdrawalRequest(
            wasteId = "test_waste_100",
            requester = receptor2,
            intendedUse = "Compost",
            proposedDate = "2026-10-26",
            requestedQuantity = "3.0 m³",
            message = "Otro intento"
        )
        assertFalse(req2Res.isSuccess) // Bloqueado porque ya está en reserva

        // 5. Completar retiro físico -> Pasa a APROVECHADO
        val completeRes = WoodWasteRepository.completeWithdrawal("test_waste_100", req1Id, "gen_01")
        assertTrue(completeRes.isSuccess)

        val completedItem = WoodWasteRepository.items.value.find { it.id == "test_waste_100" }
        assertNotNull(completedItem)
        assertEquals(WasteStatus.APROVECHADO.name, completedItem!!.status)
        assertNotNull(completedItem.completedAt)
        assertEquals("gen_01", completedItem.completedBy)
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
