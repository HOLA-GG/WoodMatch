package com.example.kotlinapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kotlinapp.databinding.ActivitySecondBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SecondActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecondBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        
        binding = ActivitySecondBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        db = Firebase.firestore

        setupUI()
        setupBottomNavigation()
    }

    private fun setupUI() {
        val user = auth.currentUser
        if (user != null) {
            binding.userName.text = user.displayName ?: user.email
            
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val role = document.getString("role")
                        if (role == "admin") {
                            binding.adminOptions.visibility = View.VISIBLE
                            binding.userOptions.visibility = View.VISIBLE
                        } else {
                            binding.userOptions.visibility = View.VISIBLE
                        }
                    } else {
                        binding.userOptions.visibility = View.VISIBLE
                    }
                }
                .addOnFailureListener {
                    binding.userOptions.visibility = View.VISIBLE
                }
        }

        binding.btnNewReport.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }

        binding.btnMyReports.setOnClickListener {
            startActivity(Intent(this, ViewReportsActivity::class.java))
        }
        
        binding.btnViewMap.setOnClickListener {
             Toast.makeText(this, "Mapa próximamente", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnManageUsers.setOnClickListener {
             startActivity(Intent(this, AdminManageUsersActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.navigation_home
        
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.navigation_home -> {
                    true
                }
                R.id.navigation_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
