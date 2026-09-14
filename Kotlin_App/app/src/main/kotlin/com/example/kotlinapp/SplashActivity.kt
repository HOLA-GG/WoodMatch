package com.example.kotlinapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.splashLogo)
        
        val scaleX = android.animation.ObjectAnimator.ofFloat(logo, "scaleX", 0.7f, 1.2f).apply {
            duration = 1000
            repeatCount = android.animation.ObjectAnimator.INFINITE
            repeatMode = android.animation.ObjectAnimator.REVERSE
        }
        val scaleY = android.animation.ObjectAnimator.ofFloat(logo, "scaleY", 0.7f, 1.2f).apply {
            duration = 1000
            repeatCount = android.animation.ObjectAnimator.INFINITE
            repeatMode = android.animation.ObjectAnimator.REVERSE
        }
        val animatorSet = android.animation.AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.start()

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 3000)
    }
}
