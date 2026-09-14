package com.example.kotlinapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.kotlinapp.databinding.ActivityReportBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentPhotoPath: String? = null
    private var photoUri: Uri? = null

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            dispatchTakePictureIntent()
        } else {
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || 
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            submitReportWithLocation()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado. Guardando sin GPS.", Toast.LENGTH_SHORT).show()
            saveReportToFirestore(null, null)
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            binding.ivReportPhoto.setImageURI(photoUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        db = Firebase.firestore
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        binding.btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent()
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnSubmitReport.setOnClickListener {
            val description = binding.etReportDescription.text.toString().trim()
            if (description.isEmpty()) {
                binding.layoutReportDescription.error = "Ingresa una descripción"
                return@setOnClickListener
            }
            binding.layoutReportDescription.error = null

            if (currentPhotoPath == null) {
                binding.tvReportError.text = "Por favor toma una foto primero"
                binding.tvReportError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            binding.tvReportError.visibility = View.GONE

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                submitReportWithLocation()
            } else {
                requestLocationPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: Exception) {
                Toast.makeText(this, "Error al crear archivo", Toast.LENGTH_SHORT).show()
                null
            }
            
            photoFile?.also {
                photoUri = FileProvider.getUriForFile(
                    this,
                    "com.example.kotlinapp.fileprovider",
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                takePictureLauncher.launch(takePictureIntent)
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun submitReportWithLocation() {
        binding.btnSubmitReport.isEnabled = false
        binding.tvReportError.text = "Obteniendo ubicación GPS..."
        binding.tvReportError.visibility = View.VISIBLE
        
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    saveReportToFirestore(location?.latitude, location?.longitude)
                }
                .addOnFailureListener {
                    saveReportToFirestore(null, null)
                }
        } catch (e: SecurityException) {
             saveReportToFirestore(null, null)
        }
    }

    private fun saveReportToFirestore(lat: Double?, lng: Double?) {
        val description = binding.etReportDescription.text.toString().trim()
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(this, "Error de sesión", Toast.LENGTH_SHORT).show()
            binding.btnSubmitReport.isEnabled = true
            return
        }

        binding.tvReportError.text = "Guardando reporte en la nube..."

        val reportData = hashMapOf(
            "userId" to user.uid,
            "description" to description,
            "localImagePath" to currentPhotoPath,
            "latitude" to lat,
            "longitude" to lng,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        db.collection("reports")
            .add(reportData)
            .addOnSuccessListener {
                showSuccessPopupAndNavigate()
            }
            .addOnFailureListener { e ->
                binding.btnSubmitReport.isEnabled = true
                binding.tvReportError.text = "Error al guardar: ${e.message}"
                binding.tvReportError.visibility = View.VISIBLE
            }
    }

    private fun showSuccessPopupAndNavigate() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_success, null)
        val titleText = dialogView.findViewById<android.widget.TextView>(R.id.dialogSuccessTitle)
        val messageText = dialogView.findViewById<android.widget.TextView>(R.id.dialogSuccessMessage)
        
        titleText.text = "¡Almacenado!"
        messageText.text = "El reporte ha sido guardado exitosamente."

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
            
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
            finish() 
        }, 2000)
    }
}
