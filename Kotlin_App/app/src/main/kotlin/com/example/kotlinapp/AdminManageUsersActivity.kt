package com.example.kotlinapp

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kotlinapp.databinding.ActivityAdminManageUsersBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AdminManageUsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminManageUsersBinding
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityAdminManageUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Firebase.firestore

        binding.btnBackAdmin.setOnClickListener {
            finish()
        }

        loadUsers()
    }

    private fun loadUsers() {
        db.collection("users").get()
            .addOnSuccessListener { result ->
                binding.usersListContainer.removeAllViews()
                for (document in result) {
                    val name = document.getString("name") ?: "Sin nombre"
                    val email = document.getString("email") ?: "Sin correo"
                    val role = document.getString("role") ?: "user"

                    val userView = TextView(this).apply {
                        text = "Nombre: $name\nCorreo: $email\nRol: $role\n"
                        textSize = 16f
                        setPadding(0, 8, 0, 8)
                    }
                    binding.usersListContainer.addView(userView)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al cargar usuarios: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
