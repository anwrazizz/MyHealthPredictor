package com.example.myhealthpredictor

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        // Mengatur padding untuk system bars (status bar, nav bar) agar tidak menimpa konten
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inisialisasi view tombol menu (menggunakan View karena layout menggunakan CardView/MaterialCardView)
        val btnPhysicalActivity = findViewById<View>(R.id.btn_physical_activity)
        val btnWeight = findViewById<View>(R.id.btn_weight)
        val btnObesityPrediction = findViewById<View>(R.id.btn_obesity_prediction)

        // Set listener untuk navigasi
        btnPhysicalActivity.setOnClickListener {
            startActivity(Intent(this, PhysicalActivityActivity::class.java))
        }

        btnWeight.setOnClickListener {
            startActivity(Intent(this, WeightLogActivity::class.java))
        }

        btnObesityPrediction.setOnClickListener {
            startActivity(Intent(this, PredictionActivity::class.java))
        }
    }
}
