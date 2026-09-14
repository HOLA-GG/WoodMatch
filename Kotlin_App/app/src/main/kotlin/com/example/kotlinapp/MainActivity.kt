package com.example.kotlinapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kotlinapp.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = Firebase.auth
        db = Firebase.firestore
        
        setupListeners()
        
        if (auth.currentUser != null) {
            showHomeScreen()
        } else {
            showLoginScreen()
        }
    }
    
    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            clearLoginErrors()
            val email = binding.loginEmail.text.toString().trim()
            val password = binding.loginPassword.text.toString()
            
            var hasError = false
            
            if (email.isEmpty()) {
                binding.loginEmailLayout.error = "Por favor ingresa tu correo"
                hasError = true
            } else if (!isValidEmail(email)) {
                binding.loginEmailLayout.error = "Formato de correo inválido"
                hasError = true
            }
            
            if (password.isEmpty()) {
                binding.loginPasswordLayout.error = "Por favor ingresa tu contraseña"
                hasError = true
            }

            if (hasError) return@setOnClickListener
            
            binding.btnLogin.isEnabled = false
            binding.loginErrorText.visibility = View.GONE
            
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    binding.btnLogin.isEnabled = true
                    if (task.isSuccessful) {
                        showSuccessPopupAndNavigate("Inicio exitoso")
                    } else {
                        binding.loginErrorText.text = task.exception?.message ?: "Error al iniciar sesión"
                        binding.loginErrorText.visibility = View.VISIBLE
                    }
                }
        }
        
        binding.btnNavigateToRegister.setOnClickListener {
            showRegisterScreen()
        }

        binding.btnNavigateToForgotPassword.setOnClickListener {
            showForgotPasswordScreen()
        }
        
        binding.btnRegister.setOnClickListener {
            clearRegisterErrors()
            
            val name = binding.registerName.text.toString().trim()
            val email = binding.registerEmail.text.toString().trim()
            val phone = binding.registerPhone.text.toString().trim() 
            val dob = binding.registerDob.text.toString().trim()
            val city = binding.registerCity.text.toString().trim()
            val password = binding.registerPassword.text.toString()
            val confirmPassword = binding.registerConfirmPassword.text.toString()
            
            val selectedRoleId = binding.registerRoleGroup.checkedRadioButtonId
            val role = if (selectedRoleId == binding.radioRoleAdmin.id) "admin" else "user"

            val selectedGenderId = binding.registerGenderGroup.checkedRadioButtonId
            val gender = when (selectedGenderId) {
                binding.radioGenderMale.id -> "Masculino"
                binding.radioGenderFemale.id -> "Femenino"
                else -> "Otro"
            }

            var hasError = false

            if (!isValidName(name)) {
                binding.registerNameLayout.error = "Nombre inválido (Solo letras y espacios)"
                hasError = true
            }
            if (!isValidEmail(email)) {
                binding.registerEmailLayout.error = "Correo electrónico inválido"
                hasError = true
            }
            if (!isValidColombianPhone(phone)) {
                binding.registerPhoneLayout.error = "Formato: +57 seguido de 10 números"
                hasError = true
            }
            if (!isValidDate(dob)) {
                binding.registerDobLayout.error = "Formato: DD-MM-AAAA (Ej: 01-01-2000)"
                hasError = true
            }
            if (!isValidCity(city)) {
                binding.registerCityLayout.error = "Ciudad inválida (Solo letras)"
                hasError = true
            }
            if (!isSecurePassword(password)) {
                binding.registerPasswordLayout.error = "Mínimo 8 caracteres, 1 mayús, 1 minús, 1 número"
                hasError = true
            }
            if (password != confirmPassword || confirmPassword.isEmpty()) {
                binding.registerConfirmPasswordLayout.error = "Las contraseñas no coinciden"
                hasError = true
            }

            if (hasError) return@setOnClickListener
            
            binding.btnRegister.isEnabled = false
            binding.registerErrorText.visibility = View.GONE

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                            
                        user?.updateProfile(profileUpdates)
                            ?.addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    val userData = hashMapOf(
                                        "name" to name,
                                        "email" to email,
                                        "phone" to phone,
                                        "dob" to dob,
                                        "city" to city,
                                        "gender" to gender,
                                        "role" to role
                                    )
                                    
                                    db.collection("users").document(user.uid)
                                        .set(userData)
                                        .addOnSuccessListener {
                                            binding.btnRegister.isEnabled = true
                                            showSuccessPopupAndNavigate("Registrado con éxito")
                                        }
                                        .addOnFailureListener { e ->
                                            binding.btnRegister.isEnabled = true
                                            binding.registerErrorText.text = "Error guardar: ${e.message}"
                                            binding.registerErrorText.visibility = View.VISIBLE
                                        }
                                } else {
                                    binding.btnRegister.isEnabled = true
                                    binding.registerErrorText.text = profileTask.exception?.message ?: "Error guardar nombre"
                                    binding.registerErrorText.visibility = View.VISIBLE
                                }
                            }
                    } else {
                        binding.btnRegister.isEnabled = true
                        binding.registerErrorText.text = task.exception?.message ?: "Error al registrar"
                        binding.registerErrorText.visibility = View.VISIBLE
                    }
                }
        }
        
        binding.btnNavigateToLogin.setOnClickListener {
            showLoginScreen()
        }

        binding.btnBackToLoginFromForgot.setOnClickListener {
            showLoginScreen()
        }

        binding.btnRecoverPassword.setOnClickListener {
            clearForgotErrors()
            
            val email = binding.forgotEmail.text.toString().trim()
            val phone = binding.forgotPhone.text.toString().trim()

            val selectedGenderId = binding.forgotGenderGroup.checkedRadioButtonId
            val gender = when (selectedGenderId) {
                binding.radioForgotMale.id -> "Masculino"
                binding.radioForgotFemale.id -> "Femenino"
                else -> "Otro"
            }

            var hasError = false

            if (!isValidEmail(email)) {
                binding.forgotEmailLayout.error = "Correo electrónico inválido"
                hasError = true
            }
            if (!isValidColombianPhone(phone)) {
                binding.forgotPhoneLayout.error = "Formato: +57 seguido de 10 números"
                hasError = true
            }

            if (hasError) return@setOnClickListener

            binding.btnRecoverPassword.isEnabled = false
            binding.forgotErrorText.visibility = View.GONE

            db.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("phone", phone)
                .whereEqualTo("gender", gender)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener { task ->
                                binding.btnRecoverPassword.isEnabled = true
                                if (task.isSuccessful) {
                                    showSuccessPopupAndNavigate("Se ha enviado un correo con instrucciones para restablecer tu contraseña. Revisa tu bandeja de entrada.")
                                } else {
                                    binding.forgotErrorText.text = "Error al enviar correo: ${task.exception?.message}"
                                    binding.forgotErrorText.visibility = View.VISIBLE
                                }
                            }
                    } else {
                        binding.btnRecoverPassword.isEnabled = true
                        binding.forgotErrorText.text = "Los datos ingresados no coinciden con ninguna cuenta"
                        binding.forgotErrorText.visibility = View.VISIBLE
                    }
                }
                .addOnFailureListener { e ->
                    binding.btnRecoverPassword.isEnabled = true
                    binding.forgotErrorText.text = "Error de conexión: ${e.message}"
                    binding.forgotErrorText.visibility = View.VISIBLE
                }
        }
    }
    
    private fun showLoginScreen() {
        binding.loginContainer.visibility = View.VISIBLE
        binding.registerContainer.visibility = View.GONE
        binding.forgotPasswordContainer.visibility = View.GONE
        clearAllInputs()
        clearLoginErrors()
    }
    
    private fun showRegisterScreen() {
        binding.loginContainer.visibility = View.GONE
        binding.registerContainer.visibility = View.VISIBLE
        binding.forgotPasswordContainer.visibility = View.GONE
        clearAllInputs()
        clearRegisterErrors()
    }

    private fun showForgotPasswordScreen() {
        binding.loginContainer.visibility = View.GONE
        binding.registerContainer.visibility = View.GONE
        binding.forgotPasswordContainer.visibility = View.VISIBLE
        clearAllInputs()
        clearForgotErrors()
    }
    
    private fun showSuccessPopupAndNavigate(message: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_success, null)
        val titleText = dialogView.findViewById<android.widget.TextView>(R.id.dialogSuccessTitle)
        val messageText = dialogView.findViewById<android.widget.TextView>(R.id.dialogSuccessMessage)
        
        titleText.text = "¡Éxito!"
        messageText.text = message

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
            
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
            showHomeScreen()
        }, 2000)
    }

    private fun showHomeScreen() {
        startActivity(Intent(this, SecondActivity::class.java))
        finish()
    }

    private fun clearLoginErrors() {
        binding.loginEmailLayout.error = null
        binding.loginPasswordLayout.error = null
        binding.loginErrorText.visibility = View.GONE
    }
    
    private fun clearRegisterErrors() {
        binding.registerNameLayout.error = null
        binding.registerEmailLayout.error = null
        binding.registerPhoneLayout.error = null
        binding.registerDobLayout.error = null
        binding.registerCityLayout.error = null
        binding.registerPasswordLayout.error = null
        binding.registerConfirmPasswordLayout.error = null
        binding.registerErrorText.visibility = View.GONE
    }
    
    private fun clearForgotErrors() {
        binding.forgotEmailLayout.error = null
        binding.forgotPhoneLayout.error = null
        binding.forgotErrorText.visibility = View.GONE
    }

    private fun clearAllInputs() {
        binding.loginEmail.text?.clear()
        binding.loginPassword.text?.clear()
        
        binding.registerName.text?.clear()
        binding.registerEmail.text?.clear()
        binding.registerPhone.text?.clear()
        binding.registerDob.text?.clear()
        binding.registerCity.text?.clear()
        binding.registerPassword.text?.clear()
        binding.registerConfirmPassword.text?.clear()

        binding.forgotEmail.text?.clear()
        binding.forgotPhone.text?.clear()
    }
    
    private fun isValidName(name: String): Boolean {
        return name.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]{2,}\$".toRegex())
    }

    private fun isValidCity(city: String): Boolean {
        return city.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]{2,}\$".toRegex())
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidColombianPhone(phone: String): Boolean {
        return phone.matches("^\\+57[0-9]{10}\$".toRegex())
    }

    private fun isValidDate(date: String): Boolean {
        return date.matches("^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])-(19|20)\\d\\d\$".toRegex())
    }

    private fun isSecurePassword(password: String): Boolean {
        val regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}\$".toRegex()
        return password.matches(regex)
    }
}
