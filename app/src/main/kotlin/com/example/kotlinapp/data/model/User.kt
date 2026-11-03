package com.example.kotlinapp.data.model

enum class UserRole(val displayName: String, val description: String) {
    INVITADO("Invitado", "Consulta sin operaciones protegidas"),
    GENERADOR("Generador de Residuos", "Publica podas, troncos y madera para aprovechamiento"),
    RECEPTOR("Receptor / Aprovechador", "Solicita material para carpintería, biomasa o compost"),
    MIXTO("Usuario Mixto", "Genera y aprovecha residuos leñosos")
}

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val dob: String = "",
    val city: String = "Bogotá (Suba)",
    val gender: String = "",
    val role: String = UserRole.GENERADOR.name,
    val sectorSuba: String = "Suba Centro"
) {
    val navigationRole: String get() = role
    val isAdmin: Boolean get() = false
    val canPublish: Boolean get() = role in listOf(UserRole.GENERADOR.name, UserRole.MIXTO.name)
    val canRequest: Boolean get() = role in listOf(UserRole.RECEPTOR.name, UserRole.MIXTO.name)

    val typedRole: UserRole
        get() = try {
            UserRole.valueOf(navigationRole.uppercase())
        } catch (_: Exception) {
            UserRole.INVITADO
        }
}
