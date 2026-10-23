package com.example.kotlinapp

import com.example.kotlinapp.data.network.ValidadorReportesService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorReportesServiceTest {

    @Test
    fun testFiltroLocalTextoDemasiadoCorto() = runBlocking {
        val service = ValidadorReportesService(apiKeyProvider = { "" })
        val resultado = service.procesarReporte("hola")
        assertFalse(resultado.valido)
        assertTrue(resultado.error?.contains("demasiado corto") == true)
    }

    @Test
    fun testExtraccionHeuristicaSubaHumedalLaConejera() = runBlocking {
        val service = ValidadorReportesService(apiKeyProvider = { "" })
        val resultado = service.procesarReporte("Basura en la entrada de la conejera por la 170")
        assertTrue(resultado.valido)
        assertNotNull(resultado.punto)
        assertEquals(4.7560, resultado.punto!!.latitude, 0.001)
        assertEquals(-74.0920, resultado.punto!!.longitude, 0.001)
    }

    @Test
    fun testExtraccionHeuristicaSubaJuanAmarillo() = runBlocking {
        val service = ValidadorReportesService(apiKeyProvider = { "" })
        val resultado = service.procesarReporte("Escombros y cemento tirados cerca al humedal Juan Amarillo")
        assertTrue(resultado.valido)
        assertNotNull(resultado.punto)
        assertEquals(4.7180, resultado.punto!!.latitude, 0.001)
        assertEquals(-74.1020, resultado.punto!!.longitude, 0.001)
        assertEquals("Escombros", resultado.tipo)
    }

    @Test
    fun testTextoInvalidoFueraDeSuba() = runBlocking {
        val service = ValidadorReportesService(apiKeyProvider = { "" })
        val resultado = service.procesarReporte("Texto aleatorio sin relacion alguna")
        assertFalse(resultado.valido)
        assertTrue(resultado.error?.contains("Suba") == true)
    }
}
