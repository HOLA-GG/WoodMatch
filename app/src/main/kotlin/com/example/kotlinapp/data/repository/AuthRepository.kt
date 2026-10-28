package com.example.kotlinapp.data.repository

import com.example.kotlinapp.data.model.User
import com.example.kotlinapp.BuildConfig
import com.example.kotlinapp.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import kotlinx.coroutines.CancellationException
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

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun ensureFirebaseSession() { /* No crear sesiones anónimas para operaciones protegidas. */ }

    suspend fun restoreSession() {
        val uid = auth?.currentUser?.takeUnless { it.isAnonymous }?.uid ?: return
        _currentUser.value = fetchUserProfile(uid)
    }

    val isAuthenticated: Boolean
        get() = auth?.currentUser?.let { !it.isAnonymous && it.uid == _currentUser.value?.uid } == true

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

        val firebaseAuth = auth ?: return Result.failure(
            Exception("El inicio de sesión todavía no está disponible en esta versión.")
        )
        return try {
            val res = firebaseAuth.signInWithEmailAndPassword(trimmedEmail, pass).await()
            val uid = res.user?.uid ?: error("No se pudo iniciar sesión.")
            val profile = fetchUserProfile(uid) ?: error("No se pudo cargar tu perfil. Comprueba tu conexión e inténtalo de nuevo.")
            _currentUser.value = profile
            Result.success(profile)
        } catch (e: Exception) {
            firebaseAuth.signOut()
            _currentUser.value = null
            val message = when {
                e.message?.contains("password", ignoreCase = true) == true ||
                e.message?.contains("credential", ignoreCase = true) == true ->
                    "Contraseña o credenciales incorrectas."
                e.message?.contains("user-not-found", ignoreCase = true) == true ||
                e.message?.contains("no user record", ignoreCase = true) == true ->
                    "No se encontró ninguna cuenta registrada con este correo."
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Error de conexión de red."
                else -> "No se pudo iniciar sesión. Revisa tus datos y tu conexión."
            }
            Result.failure(Exception(message))
        }
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

        if (trimmedName.isBlank() || trimmedName.length > 120) return Result.failure(Exception("El nombre debe tener entre 1 y 120 caracteres."))
        if (!validateEmail(trimmedEmail)) return Result.failure(Exception("Correo electrónico inválido."))
        if (!validatePhone(trimmedPhone)) return Result.failure(Exception("El teléfono debe comenzar con +57 y tener 10 dígitos."))
        if (city.isBlank()) return Result.failure(Exception("La ciudad o municipio es requerida."))
        if (!validatePassword(pass)) return Result.failure(Exception("La contraseña debe tener al menos 8 caracteres, 1 mayúscula, 1 minúscula y 1 número."))
        if (pass != confirmPass) return Result.failure(Exception("Las contraseñas no coinciden."))

        // SEGURIDAD: Prevenir auto-asignación de ADMIN en registro
        val sanitizedRole = when (role.uppercase()) {
            UserRole.RECEPTOR.name -> UserRole.RECEPTOR.name
            UserRole.MIXTO.name -> UserRole.MIXTO.name
            else -> UserRole.GENERADOR.name
        }

        val firebaseAuth = auth ?: return Result.failure(
            Exception("El registro todavía no está disponible en esta versión.")
        )
        var newUid = ""

        try {
            val res = firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, pass).await()
            newUid = res.user?.uid ?: error("No se creó la cuenta.")
        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("email-already-in-use", ignoreCase = true) == true ->
                    "Este correo electrónico ya se encuentra registrado."
                e.message?.contains("weak-password", ignoreCase = true) == true ->
                    "La contraseña es demasiado débil para crear tu cuenta."
                else -> e.localizedMessage ?: (e.message ?: "Error al registrar usuario.")
            }
            return Result.failure(Exception(msg))
        }

        val newUser = User(
            uid = newUid,
            name = trimmedName,
            email = trimmedEmail,
            phone = trimmedPhone,
            dob = "",
            city = city,
            gender = "",
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
                    "sectorSuba" to newUser.sectorSuba,
                    "privacyVersion" to com.example.kotlinapp.ui.components.PRIVACY_VERSION,
                    "privacyAcceptedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                db.collection("users").document(newUid).set(data).await()
            } catch (e: Exception) {
                runCatching { firebaseAuth.currentUser?.delete()?.await() }
                firebaseAuth.signOut()
                return Result.failure(Exception("No se pudo guardar el perfil. Vuelve a intentar el registro."))
            }
        }

        demoUsers.add(newUser)
        _currentUser.value = newUser
        return Result.success(newUser)
    }

    fun switchUserForTesting(role: UserRole) {
        if (!BuildConfig.DEBUG || auth != null) return
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
            role = "INVITADO",
            sectorSuba = "Suba Centro"
        )
    }

    /** Does not disclose whether a mailbox has an account. */
    @Suppress("UNUSED_PARAMETER")
    suspend fun forgotPassword(email: String, phone: String = "", gender: String = ""): Result<String> {
        val trimmedEmail = email.trim()
        if (!validateEmail(trimmedEmail)) return Result.failure(Exception("Escribe un correo válido."))
        val firebaseAuth = auth ?: return Result.failure(Exception("La recuperación no está disponible en esta instalación."))
        val neutralMessage = "Si este correo tiene una cuenta, recibirás un enlace para restablecer tu contraseña. Revisa también Spam o Correo no deseado."
        return try {
            firebaseAuth.setLanguageCode("es")
            firebaseAuth.sendPasswordResetEmail(trimmedEmail).await()
            Result.success(neutralMessage)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: FirebaseAuthInvalidUserException) {
            Result.success(neutralMessage)
        } catch (error: Exception) {
            Result.failure(Exception(authenticationMessage(error, recovery = true)))
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        if (!isAuthenticated) return Result.failure(Exception("Inicia sesión para cambiar tu contraseña."))
        if (currentPassword.isBlank()) return Result.failure(Exception("Escribe tu contraseña actual."))
        if (!validatePassword(newPassword)) return Result.failure(Exception("Usa al menos 8 caracteres, una mayúscula, una minúscula y un número."))
        if (currentPassword == newPassword) return Result.failure(Exception("Elige una contraseña diferente a la actual."))
        val firebaseUser = auth?.currentUser ?: return Result.failure(Exception("Vuelve a iniciar sesión para continuar."))
        val email = firebaseUser.email ?: return Result.failure(Exception("Esta cuenta no tiene un correo disponible para confirmar tu identidad."))
        if (firebaseUser.providerData.none { it.providerId == EmailAuthProvider.PROVIDER_ID })
            return Result.failure(Exception("Esta cuenta administra su contraseña con su proveedor de acceso."))
        return try {
            firebaseUser.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword)).await()
            firebaseUser.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(Exception(authenticationMessage(error)))
        }
    }

    private fun authenticationMessage(error: Exception, recovery: Boolean = false): String =
        when (error) {
            is FirebaseNetworkException -> "No hay conexión con Firebase. Comprueba Internet e inténtalo de nuevo."
            is FirebaseTooManyRequestsException -> "Hay demasiados intentos. Espera unos minutos antes de volver a intentar."
            is FirebaseAuthException -> when (error.errorCode) {
                "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS" ->
                    if (recovery) "No se pudo procesar la solicitud. Inténtalo de nuevo."
                    else "No se pudo confirmar tu contraseña actual. Revísala e inténtalo de nuevo."
                "ERROR_WEAK_PASSWORD" -> "La contraseña no cumple la política de seguridad de la cuenta."
                "ERROR_REQUIRES_RECENT_LOGIN", "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN" ->
                    "Tu sesión necesita renovarse. Vuelve a iniciar sesión."
                "ERROR_USER_DISABLED" -> "Esta cuenta no está disponible. Consulta con el responsable del piloto."
                "ERROR_OPERATION_NOT_ALLOWED" -> "El servicio de contraseñas no está habilitado. Contacta al responsable del piloto."
                else -> if (recovery) "No se pudo solicitar el correo. Inténtalo más tarde."
                    else "No se pudo cambiar la contraseña. Inténtalo de nuevo."
            }
            else -> if (recovery) "No se pudo solicitar el correo. Comprueba la conexión e inténtalo de nuevo."
                else "No se pudo cambiar la contraseña. Comprueba la conexión e inténtalo de nuevo."
        }

    fun logout() {
        try {
            auth?.signOut()
        } catch (_: Exception) {}
        _currentUser.value = null
    }

    suspend fun getAllUsers(): List<User> {
        if (!isAuthenticated || _currentUser.value?.isAdmin != true) return emptyList()
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
        return emptyList()
    }

    /** Older call sites keep this signature; role and region remain the registered values. */
    @Suppress("UNUSED_PARAMETER")
    suspend fun updateCurrentUserProfile(name: String, phone: String, sectorSuba: String, role: String): Result<User> {
        val current = _currentUser.value ?: return Result.failure(Exception("No hay una sesión activa."))
        if (!isAuthenticated) return Result.failure(Exception("Inicia sesión para editar tu perfil."))
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()
        if (trimmedName.isBlank() || trimmedName.length > 120)
            return Result.failure(Exception("El nombre debe tener entre 1 y 120 caracteres."))
        if (!validatePhone(trimmedPhone))
            return Result.failure(Exception("El teléfono debe comenzar con +57 y tener 10 dígitos."))
        if (role != current.role)
            return Result.failure(Exception("El rol se elige al registrarte y no se cambia desde el perfil."))
        val db = firestore ?: return Result.failure(Exception("No se puede guardar el perfil en esta instalación."))
        return try {
            db.collection("users").document(current.uid).update(
                mapOf(
                    "name" to trimmedName,
                    "phone" to trimmedPhone,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                )
            ).await()
            val updated = current.copy(name = trimmedName, phone = trimmedPhone)
            _currentUser.value = updated
            Result.success(updated)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.failure(Exception("No se pudo guardar el perfil. Comprueba tu conexión e inténtalo de nuevo."))
        }
    }

    suspend fun updateProfile(name: String, phone: String): Result<User> {
        val current = _currentUser.value ?: return Result.failure(Exception("No hay una sesión activa."))
        return updateCurrentUserProfile(name, phone, current.sectorSuba, current.role)
    }

    suspend fun deleteCurrentAccount(): Result<Unit> = runCatching {
        val user = _currentUser.value ?: error("No hay una sesión activa.")
        com.example.kotlinapp.data.network.PilotApi.call("deleteAccount")
        ReportRepository.deleteUserReports(user.uid)
        WoodWasteRepository.clearReceipts(user.uid)
        logout()
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun updateUserRole(uid: String, newRole: String): Result<Unit> =
        Result.failure(Exception("El rol se elige al registrarte y no se cambia desde el perfil."))

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

