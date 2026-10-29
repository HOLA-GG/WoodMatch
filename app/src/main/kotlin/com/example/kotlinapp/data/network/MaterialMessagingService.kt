package com.example.kotlinapp.data.network

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.kotlinapp.MainActivity
import com.example.kotlinapp.R
import com.example.kotlinapp.data.repository.AuthRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MaterialMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        val prefs = getSharedPreferences("material_alerts", 0)
        if (!prefs.getBoolean("enabled", false) || prefs.getString("uid", "") != FirebaseAuth.getInstance().currentUser?.uid) return
        if (!AuthRepository.isAuthenticated) return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { PilotApi.call("registerNotificationDevice", mapOf("token" to token, "enabled" to true)) }
        }
    }
    override fun onMessageReceived(message: RemoteMessage) {
        val prefs = getSharedPreferences("material_alerts", 0)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (message.data["recipientId"]?.let { it != uid } == true) return
        if (!prefs.getBoolean("enabled", false) || prefs.getString("uid", "") != uid) return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("materials", "Materiales disponibles", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).putExtra("openNotifications",true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notice = NotificationCompat.Builder(this, "materials").setSmallIcon(R.drawable.ic_home)
            .setContentTitle(message.data["title"] ?: "Nuevo material disponible")
            .setContentText(message.data["body"] ?: "Explora la comunidad de WoodMatch")
            .setContentIntent(intent).setAutoCancel(true).build()
        NotificationManagerCompat.from(this).notify((message.data["notificationId"] ?: message.data["publicationId"])?.hashCode() ?: 1, notice)
    }
}
