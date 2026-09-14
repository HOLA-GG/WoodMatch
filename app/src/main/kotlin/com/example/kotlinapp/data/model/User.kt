package com.example.kotlinapp.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val dob: String = "",
    val city: String = "",
    val gender: String = "",
    val role: String = "user" // "user" or "admin"
) {
    val isAdmin: Boolean
        get() = role.equals("admin", ignoreCase = true)
}
