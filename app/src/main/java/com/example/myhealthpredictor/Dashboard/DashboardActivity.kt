package com.example.myhealthpredictor.Dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myhealthpredictor.ActivityLog.ActivityLogActivity
import com.example.myhealthpredictor.Auth.LoginActivity
import com.example.myhealthpredictor.Prediction.PredictionActivity
import com.example.myhealthpredictor.WeightLog.WeightLogActivity
import com.example.myhealthpredictor.R
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvUserName: TextView
    private lateinit var tvTotalPredictions: TextView
    private lateinit var tvStreakDays: TextView
    private lateinit var tvHealthTip: TextView
    private lateinit var cardPrediction: MaterialCardView
    private lateinit var cardWeightLog: MaterialCardView
    private lateinit var cardHistory: MaterialCardView
    private lateinit var cardActivities: MaterialCardView
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Check if user is signed in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            navigateToLogin()
            return
        }

        setContentView(R.layout.activity_dashboard)

        // Initialize views
        tvUserName = findViewById(R.id.tv_user_name)
        tvTotalPredictions = findViewById(R.id.tv_total_predictions)
        tvStreakDays = findViewById(R.id.tv_streak_days)
        tvHealthTip = findViewById(R.id.tv_health_tip)
        cardPrediction = findViewById(R.id.card_prediction)
        cardWeightLog = findViewById(R.id.card_weight_log)
        cardHistory = findViewById(R.id.card_history)
        cardActivities = findViewById(R.id.card_activities)
        btnLogout = findViewById(R.id.btn_logout)

        // Load user data
        loadUserData()

        // Load statistics
        loadStatistics()

        // Set random health tip
        setRandomHealthTip()

        // Menu clicks
        cardPrediction.setOnClickListener {
            val intent = Intent(this, PredictionActivity::class.java)
            startActivity(intent)
        }

        cardWeightLog.setOnClickListener {
            val intent = Intent(this, WeightLogActivity::class.java)
            startActivity(intent)
        }

        cardHistory.setOnClickListener {
            val intent = Intent(this, com.example.myhealthpredictor.History.HistoryActivity::class.java)
            startActivity(intent)
        }

        cardActivities.setOnClickListener {
            val intent = Intent(this, ActivityLogActivity::class.java)
            startActivity(intent)
        }

        // Logout button
        btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser

        currentUser?.let { user ->
            // Prioritas: 1. Firestore fullname, 2. Display name, 3. Email username

            // Ambil dari Firestore terlebih dahulu (paling akurat)
            db.collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val fullname = document.getString("fullname")
                        if (!fullname.isNullOrEmpty()) {
                            tvUserName.text = fullname
                        } else {
                            // Fallback ke display name
                            tvUserName.text = user.displayName
                                ?: user.email?.substringBefore("@")
                                        ?: "User"
                        }
                    } else {
                        // Document tidak ada, gunakan display name atau email
                        tvUserName.text = user.displayName
                            ?: user.email?.substringBefore("@")
                                    ?: "User"
                    }
                }
                .addOnFailureListener {
                    // Jika gagal ambil dari Firestore, gunakan display name atau email
                    tvUserName.text = user.displayName
                        ?: user.email?.substringBefore("@")
                                ?: "User"
                }
        }
    }

    private fun loadStatistics() {
        val currentUser = auth.currentUser ?: return

        // Load total predictions dari Firestore
        db.collection("predictions")
            .whereEqualTo("userId", currentUser.uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("DashboardActivity", "Error loading predictions: ${e.message}")
                    tvTotalPredictions.text = "0"
                    return@addSnapshotListener
                }

                val totalPredictions = snapshots?.size() ?: 0
                tvTotalPredictions.text = totalPredictions.toString()
                Log.d("DashboardActivity", "Total predictions: $totalPredictions")
            }

        // Load streak days (contoh static, bisa dikembangkan lebih lanjut)
        // TODO: Implementasi logic streak days berdasarkan aktivitas harian
        tvStreakDays.text = "7"
    }

    private fun setRandomHealthTip() {
        val healthTips = listOf(
            "Minum minimal 8 gelas air putih setiap hari untuk menjaga metabolisme tubuh tetap optimal.",
            "Konsumsi 5 porsi sayur dan buah setiap hari untuk memenuhi kebutuhan vitamin dan mineral.",
            "Tidur 7-8 jam setiap malam sangat penting untuk pemulihan tubuh dan kesehatan mental.",
            "Olahraga minimal 30 menit setiap hari dapat meningkatkan kesehatan jantung dan mood.",
            "Kurangi konsumsi gula dan garam berlebih untuk mencegah penyakit kronis.",
            "Jangan skip sarapan! Sarapan sehat memberikan energi untuk memulai hari.",
            "Kelola stres dengan meditasi, yoga, atau aktivitas yang Anda sukai.",
            "Rutin cek kesehatan minimal setahun sekali untuk deteksi dini masalah kesehatan."
        )

        val randomTip = healthTips.random()
        tvHealthTip.text = randomTip
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun performLogout() {
        auth.signOut()
        Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Refresh statistics saat kembali ke dashboard
        loadStatistics()
    }
}