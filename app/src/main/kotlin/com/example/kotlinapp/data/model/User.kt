package com.example.kotlinapp.data.model

enum class UserRole(val displayName: String, val description: String) {
    INVITADO("Invitado", "Consulta sin operaciones protegidas"),
    GENERADOR("Generador de Residuos", "Publica podas, troncos y madera para aprovechamiento"),
    RECEPTOR("Receptor / Aprovechador", "Solicita material para carpintería, biomasa o compost"),
    MIXTO("Usuario Mixto", "Genera y aprovecha residuos leñosos"),
    SOPORTE("Soporte y moderación", "Atiende casos y cuida las publicaciones de la comunidad"),
    ADMIN("Administrador", "Supervisión global, moderación y métricas")
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
    val isSupport: Boolean
        get() = uid == "yDGu1WUesuc01uUbUzc3By4qyL02" && email.equals("cookiestech@woodmatch.org", ignoreCase = true)
    val navigationRole: String
        get() = if (isSupport) UserRole.SOPORTE.name else role

    val isAdmin: Boolean
        get() = !isSupport && (role.equals("admin", ignoreCase = true) || role == UserRole.ADMIN.name)

    val canPublish: Boolean
        get() = !isSupport && role in listOf(UserRole.GENERADOR.name, UserRole.MIXTO.name, UserRole.ADMIN.name, "admin", "user")

    val canRequest: Boolean
        get() = !isSupport && role in listOf(UserRole.RECEPTOR.name, UserRole.MIXTO.name, UserRole.ADMIN.name, "admin", "user")

    val typedRole: UserRole
        get() = try {
            UserRole.valueOf(navigationRole.uppercase())
        } catch (_: Exception) {
            if (role.equals("admin", ignoreCase = true)) UserRole.ADMIN else UserRole.INVITADO
        }
}
