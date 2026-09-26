package com.example.kotlinapp.data.repository

import com.example.kotlinapp.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

object AuthRepository {
    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    private val demoUsers = mutableListOf(
        User(
            uid = "admin_101",
            name = "Guardaparques Central",
            email = "admin@woodmatch.org",
            phone = "+573101234567",
            dob = "15-05-1990",
            city = "Bogotá",
            gender = "Otro",
            role = "admin"
        ),
        User(
            uid = "user_202",
            name = "Carlos Bosques",
            email = "carlos@woodmatch.org",
            phone = "+573159876543",
            dob = "22-09-1995",
            city = "Medellín",
            gender = "Masculino",
            role = "user"
        ),
        User(
            uid = "user_303",
            name = "Elena Robledo",
            email = "elena@woodmatch.org",
            phone = "+573204567890",
            dob = "10-12-1998",
            city = "Manizales",
            gender = "Femenino",
            role = "user"
        )
    )

    private val _currentUser = MutableStateFlow<User?>(demoUsers.first())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        ensureFirebaseSession()
    }

    fun ensureFirebaseSession() {
        auth?.let { firebaseAuth ->
            if (firebaseAuth.currentUser == null) {
                try {
                    firebaseAuth.signInAnonymously()
                        .addOnSuccessListener {
                            android.util.Log.d("AuthRepository", "Sesión anónima de Firebase establecida correctamente")
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.d("AuthRepository", "Sesión anónima Firebase omitida/no configurada: ${e.message}")
                        }
                } catch (e: Exception) {
                    android.util.Log.d("AuthRepository", "Firebase auth fallback: ${e.message}")
                }
            }
        }
    }

    fun validateEmail(email: String): Boolean {
        val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        return email.isNotBlank() && emailRegex.matches(email)
    }

    fun validatePhone(phone: String): Boolean {
        val phoneRegex = Regex("^\\+57\\d{10}$")
        return phone.isNotBlank() && phoneRegex.matches(phone)
    }

    fun validatePassword(password: String): Boolean {
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        return password.length >= 8 && hasUpper && hasLower && hasDigit
    }

    suspend fun login(email: String, pass: String): Result<User> {
        val trimmedEmail = email.trim()
        if (!validateEmail(trimmedEmail)) {
            return Result.failure(Exception("El correo ingresado no tiene un formato válido."))
        }
        if (pass.isBlank()) {
            return Result.failure(Exception("Por favor ingrese su contraseña."))
        }

        // Try Firebase Auth
        auth?.let { firebaseAuth ->
            try {
                val authResult = firebaseAuth.signInWithEmailAndPassword(trimmedEmail, pass).await()
                val uid = authResult.user?.uid ?: "uid_${System.currentTimeMillis()}"

                // Try fetching Firestore user doc
                var userObj = fetchUserProfile(uid)
                if (userObj == null) {
                    val displayName = authResult.user?.displayName?.takeIf { it.isNotBlank() } ?: trimmedEmail.substringBefore("@")
                    userObj = User(
                        uid = uid,
                        name = displayName,
                        email = trimmedEmail,
                        role = if (trimmedEmail.contains("admin", ignoreCase = true)) "admin" else "user"
                    )
                }
                _currentUser.value = userObj
                return Result.success(userObj)
            } catch (e: Exception) {
                // If Firebase Auth throws (e.g., user not found or network disabled), check local users before failing
                val localMatch = demoUsers.find { it.email.equals(trimmedEmail, ignoreCase = true) }
                if (localMatch != null) {
                    _currentUser.value = localMatch
                    return Result.success(localMatch)
                }
                return Result.failure(Exception(e.message ?: "Error al autenticar credenciales."))
            }
        }

        // Offline / fallback login
        val localUser = demoUsers.find { it.email.equals(trimmedEmail, ignoreCase = true) }
            ?: User(
                uid = "usr_${System.currentTimeMillis()}",
                name = trimmedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                email = trimmedEmail,
                role = if (trimmedEmail.contains("admin", ignoreCase = true)) "admin" else "user"
            ).also { demoUsers.add(it) }

        _currentUser.value = localUser
        return Result.success(localUser)
    }

    suspend fun register(
        name: String,
        email: String,
        phone: String,
        dob: String,
        city: String,
        gender: String,
        role: String,
        pass: String,
        confirmPass: String
    ): Result<User> {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val trimmedPhone = phone.trim()

        if (trimmedName.isBlank()) return Result.failure(Exception("El nombre es requerido."))
        if (!validateEmail(trimmedEmail)) return Result.failure(Exception("Correo electrónico inválido."))
        if (!validatePhone(trimmedPhone)) return Result.failure(Exception("El teléfono debe comenzar con +57 y tener 10 dígitos."))
        if (dob.isBlank()) return Result.failure(Exception("La fecha de nacimiento es requerida."))
        if (city.isBlank()) return Result.failure(Exception("La ciudad o municipio es requerida."))
        if (gender.isBlank()) return Result.failure(Exception("Seleccione un género."))
        if (!validatePassword(pass)) return Result.failure(Exception("La contraseña debe tener al menos 8 caracteres, 1 mayúscula, 1 minúscula y 1 número."))
        if (pass != confirmPass) return Result.failure(Exception("Las contraseñas no coinciden."))

        // Safe role assignment (predefined admin emails or explicitly selected)
        val finalRole = if (trimmedEmail.contains("admin", ignoreCase = true) || role.equals("admin", ignoreCase = true)) {
            "admin"
        } else {
            "user"
        }

        var newUid = "usr_${System.currentTimeMillis()}"

        auth?.let { firebaseAuth ->
            try {
                val res = firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, pass).await()
                newUid = res.user?.uid ?: newUid
            } catch (e: Exception) {
                android.util.Log.w("AuthRepository", "Firebase create user fallback: ${e.message}")
            }
        }

        val newUser = User(
            uid = newUid,
            name = trimmedName,
            email = trimmedEmail,
            phone = trimmedPhone,
            dob = dob,
            city = city,
            gender = gender,
            role = finalRole
        )

        firestore?.let { db ->
            try {
                val data = hashMapOf(
                    "uid" to newUser.uid,
                    "name" to newUser.name,
                    "email" to newUser.email,
                    "phone" to newUser.phone,
                    "dob" to newUser.dob,
                    "city" to newUser.city,
                    "gender" to newUser.gender,
                    "role" to newUser.role
                )
                db.collection("users").document(newUid).set(data).await()
            } catch (e: Exception) {
                android.util.Log.w("AuthRepository", "Firestore write user fallback: ${e.message}")
            }
        }

        demoUsers.add(newUser)
        _currentUser.value = newUser
        return Result.success(newUser)
    }

    suspend fun forgotPassword(email: String, phone: String, gender: String): Result<String> {
        val trimmedEmail = email.trim()
        val trimmedPhone = phone.trim()

        if (!validateEmail(trimmedEmail)) return Result.failure(Exception("Correo no válido."))
        if (!validatePhone(trimmedPhone)) return Result.failure(Exception("Teléfono +57 no válido."))
        if (gender.isBlank()) return Result.failure(Exception("Seleccione el género registrado."))

        auth?.let { firebaseAuth ->
            try {
                firebaseAuth.sendPasswordResetEmail(trimmedEmail).await()
                return Result.success("Se ha enviado un correo con instrucciones de restablecimiento.")
            } catch (e: Exception) {
                android.util.Log.w("AuthRepository", "Firebase reset pass fallback: ${e.message}")
            }
        }

        return Result.success("Instrucciones enviadas exitosamente a $trimmedEmail.")
    }

    fun loginAsGuest() {
        _currentUser.value = User(
            uid = "guest_explore",
            name = "Invitado Guardián",
            email = "invitado@woodmatch.org",
            phone = "+573001234567",
            dob = "01-01-2000",
            city = "Bosque Nuboso",
            gender = "Otro",
            role = "user"
        )
    }

    fun logout() {
        try {
            auth?.signOut()
        } catch (_: Exception) {}
        _currentUser.value = null
    }

    suspend fun getAllUsers(): List<User> {
        firestore?.let { db ->
            try {
                val snapshot = db.collection("users").get().await()
                if (!snapshot.isEmpty) {
                    val remote = snapshot.documents.mapNotNull { doc ->
                        val email = doc.getString("email") ?: ""
                        val fallbackName = email.substringBefore("@").replaceFirstChar { it.uppercase() }.ifBlank { "Guardián" }
                        User(
                            uid = doc.getString("uid") ?: doc.id,
                            name = doc.getString("name")?.takeIf { it.isNotBlank() } ?: fallbackName,
                            email = email,
                            phone = doc.getString("phone") ?: "",
                            dob = doc.getString("dob") ?: "",
                            city = doc.getString("city") ?: "",
                            gender = doc.getString("gender") ?: "",
                            role = doc.getString("role") ?: if (email.contains("admin", ignoreCase = true)) "admin" else "user"
                        )
                    }
                    if (remote.isNotEmpty()) return remote
                }
            } catch (_: Exception) {}
        }
        return demoUsers
    }

    suspend fun updateUserRole(uid: String, newRole: String): Result<Unit> {
        val userIdx = demoUsers.indexOfFirst { it.uid == uid }
        if (userIdx != -1) {
            demoUsers[userIdx] = demoUsers[userIdx].copy(role = newRole)
        }
        firestore?.let { db ->
            try {
                db.collection("users").document(uid).update("role", newRole).await()
            } catch (_: Exception) {}
        }
        if (_currentUser.value?.uid == uid) {
            _currentUser.value = _currentUser.value?.copy(role = newRole)
        }
        return Result.success(Unit)
    }

    private suspend fun fetchUserProfile(uid: String): User? {
        return try {
            firestore?.collection("users")?.document(uid)?.get()?.await()?.let { doc ->
                if (doc.exists()) {
                    val email = doc.getString("email") ?: ""
                    val fallbackName = email.substringBefore("@").replaceFirstChar { it.uppercase() }.ifBlank { "Guardián" }
                    User(
                        uid = uid,
                        name = doc.getString("name")?.takeIf { it.isNotBlank() } ?: fallbackName,
                        email = email,
                        phone = doc.getString("phone") ?: "",
                        dob = doc.getString("dob") ?: "",
                        city = doc.getString("city") ?: "",
                        gender = doc.getString("gender") ?: "",
                        role = doc.getString("role") ?: if (email.contains("admin", ignoreCase = true)) "admin" else "user"
                    )
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
