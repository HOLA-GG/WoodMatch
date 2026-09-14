package com.example.kotlinapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlinapp.databinding.ActivityViewReportsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ViewReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewReportsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var reportAdapter: ReportAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        
        binding = ActivityViewReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        db = Firebase.firestore

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        setupRecyclerView()
        showExtractingPopupAndFetch()
    }

    private fun setupRecyclerView() {
        reportAdapter = ReportAdapter(emptyList())
        binding.rvReports.layoutManager = LinearLayoutManager(this)
        binding.rvReports.adapter = reportAdapter
    }

    private fun showExtractingPopupAndFetch() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_success, null)
        val titleText = dialogView.findViewById<android.widget.TextView>(R.id.dialogSuccessTitle)
        val messageText = dialogView.findViewById<android.widget.TextView>(R.id.dialogSuccessMessage)
        
        titleText.text = "Revisando..."
        messageText.text = "Revisando Reportes..."

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
            
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        // Give it a small simulated delay for UX, then fetch real data
        Handler(Looper.getMainLooper()).postDelayed({
            fetchReports()
            dialog.dismiss()
        }, 1500)
    }

    private fun fetchReports() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            binding.tvNoReports.text = "Error de sesión"
            binding.tvNoReports.visibility = View.VISIBLE
            return
        }

        binding.progressBarReports.visibility = View.VISIBLE
        binding.tvNoReports.visibility = View.GONE

        db.collection("reports")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBarReports.visibility = View.GONE
                
                if (documents.isEmpty) {
                    binding.tvNoReports.text = "no tienes reportes :("
                    binding.tvNoReports.visibility = View.VISIBLE
                } else {
                    val reportsList = mutableListOf<ReportModel>()
                    for (document in documents) {
                        val report = document.toObject(ReportModel::class.java).copy(id = document.id)
                        reportsList.add(report)
                    }
                    reportAdapter.updateData(reportsList)
                }
            }
            .addOnFailureListener { e ->
                binding.progressBarReports.visibility = View.GONE
                binding.tvNoReports.text = "Error al cargar reportes: ${e.message}"
                binding.tvNoReports.visibility = View.VISIBLE

                // Fallback query without orderBy if index is missing in Firestore
                fallbackFetchReports(currentUser.uid)
            }
    }

    private fun fallbackFetchReports(userId: String) {
        db.collection("reports")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    binding.tvNoReports.visibility = View.GONE
                    val reportsList = mutableListOf<ReportModel>()
                    for (document in documents) {
                        val report = document.toObject(ReportModel::class.java).copy(id = document.id)
                        reportsList.add(report)
                    }
                    // Sort locally since index failed
                    reportsList.sortByDescending { it.timestamp ?: 0 }
                    reportAdapter.updateData(reportsList)
                }
            }
    }
}
