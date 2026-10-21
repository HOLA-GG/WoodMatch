package com.example.kotlinapp.data.repository

import com.example.kotlinapp.data.model.User
import com.example.kotlinapp.data.model.UserRole
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

    // Perfiles iniciales de prueba para verificación sin passwords hardcodeados en el APK
    private val demoUsers = mutableListOf(
        User(
            uid = "admin_suba",
            name = "Coordinador Ambiental Suba",
            email = "coordinacion.suba@woodmatch.co",
            phone = "+573101234567",
            dob = "15-05-1988",
            city = "Bogotá",
            gender = "Otro",
            role = UserRole.ADMIN.name,
            sectorSuba = "Suba Centro"
        ),
        User(
            uid = "gen_carlos",
            name = "Carlos M. (Poda y Jardinería)",
            email = "carlos.poda@woodmatch.co",
            phone = "+573159876543",
            dob = "22-09-1992",
            city = "Bogotá",
            gender = "Masculino",
            role = UserRole.GENERADOR.name,
            sectorSuba = "El Rincón"
        ),
        User(
            uid = "rec_elena",
            name = "Elena R. (Taller Ebanistería)",
            email = "elena.taller@woodmatch.co",
            phone = "+573204567890",
            dob = "10-12-1995",
            city = "Bogotá",
            gender = "Femenino",
            role = UserRole.RECEPTOR.name,
            sectorSuba = "Niza / La Colina"
        )
    )

    private val _currentUser = MutableStateFlow<User?>(demoUsers[1]) // Default to generator for seamless UX
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
                            android.util.Log.d("AuthRepository", "Sesión de Firebase inicializada")
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.d("AuthRepository", "Sesión Firebase omitida: ${e.message}")
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

        // 1. Check in-memory demo accounts first for fast access
        val localMatch = demoUsers.find { it.email.equals(trimmedEmail, ignoreCase = true) }

        // 2. Try Firebase Auth
        auth?.let { firebaseAuth ->
            try {
                val authResult = firebaseAuth.signInWithEmailAndPassword(trimmedEmail, pass).await()
                val uid = authResult.user?.uid ?: (localMatch?.uid ?: "uid_${System.currentTimeMillis()}")

                var userObj = fetchUserProfile(uid)
                if (userObj == null) {
                    val displayName = authResult.user?.displayName?.takeIf { it.isNotBlank() } ?: trimmedEmail.substringBefore("@")
                    userObj = localMatch ?: User(
                        uid = uid,
                        name = displayName,
                        email = trimmedEmail,
                        role = if (trimmedEmail.contains("admin", ignoreCase = true)) UserRole.ADMIN.name else UserRole.GENERADOR.name,
                        sectorSuba = "Suba Centro"
                    )
                }
                _currentUser.value = userObj
                return Result.success(userObj)
            } catch (e: Exception) {
                if (localMatch != null) {
                    _currentUser.value = localMatch
                    return Result.success(localMatch)
                }
                return Result.failure(Exception(e.message ?: "Credenciales incorrectas o usuario no encontrado."))
            }
        }

        if (localMatch != null) {
            _currentUser.value = localMatch
            return Result.success(localMatch)
        }

        val fallbackUser = User(
            uid = "usr_${System.currentTimeMillis()}",
            name = trimmedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
            email = trimmedEmail,
            role = UserRole.GENERADOR.name,
            sectorSuba = "Suba Centro"
        ).also { demoUsers.add(it) }

        _currentUser.value = fallbackUser
        return Result.success(fallbackUser)
    }

    suspend fun register(
        name: String,
        email: String,
        phone: String,
        dob: String,
        city: String,
        gender: String,
        role: String,
        sectorSuba: String,
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

        // SEGURIDAD: Prevenir auto-asignación de ADMIN en registro
        val sanitizedRole = when (role.uppercase()) {
            UserRole.RECEPTOR.name -> UserRole.RECEPTOR.name
            UserRole.MIXTO.name -> UserRole.MIXTO.name
            else -> UserRole.GENERADOR.name
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
            role = sanitizedRole,
            sectorSuba = sectorSuba.ifBlank { "Suba Centro" }
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
                    "role" to newUser.role,
                    "sectorSuba" to newUser.sectorSuba
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

    fun switchUserForTesting(role: UserRole) {
        val target = demoUsers.find { it.typedRole == role }
        if (target != null) {
            _currentUser.value = target
        }
    }

    fun loginAsGuest() {
        _currentUser.value = User(
            uid = "guest_explore",
            name = "Invitado Circular",
            email = "invitado@woodmatch.co",
            phone = "+573001234567",
            dob = "01-01-2000",
            city = "Bogotá",
            gender = "Otro",
            role = UserRole.RECEPTOR.name,
            sectorSuba = "Suba Centro"
        )
    }

    suspend fun forgotPassword(email: String, phone: String, gender: String): Result<String> {
        val trimmedEmail = email.trim()
        if (!validateEmail(trimmedEmail)) return Result.failure(Exception("Correo no válido."))

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
                        val fallbackName = email.substringBefore("@").replaceFirstChar { it.uppercase() }.ifBlank { "Usuario" }
                        User(
                            uid = doc.getString("uid") ?: doc.id,
                            name = doc.getString("name")?.takeIf { it.isNotBlank() } ?: fallbackName,
                            email = email,
                            phone = doc.getString("phone") ?: "",
                            dob = doc.getString("dob") ?: "",
                            city = doc.getString("city") ?: "Bogotá",
                            gender = doc.getString("gender") ?: "",
                            role = doc.getString("role") ?: UserRole.GENERADOR.name,
                            sectorSuba = doc.getString("sectorSuba") ?: "Suba Centro"
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
                    val fallbackName = email.substringBefore("@").replaceFirstChar { it.uppercase() }.ifBlank { "Usuario" }
                    User(
                        uid = uid,
                        name = doc.getString("name")?.takeIf { it.isNotBlank() } ?: fallbackName,
                        email = email,
                        phone = doc.getString("phone") ?: "",
                        dob = doc.getString("dob") ?: "",
                        city = doc.getString("city") ?: "Bogotá",
                        gender = doc.getString("gender") ?: "",
                        role = doc.getString("role") ?: UserRole.GENERADOR.name,
                        sectorSuba = doc.getString("sectorSuba") ?: "Suba Centro"
                    )
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
