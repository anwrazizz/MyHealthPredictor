package com.example.myhealthpredictor.Prediction

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myhealthpredictor.R

class PredictionResultActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESULT = "extra_result"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prediction_result)

        val resultTextView = findViewById<TextView>(R.id.tv_result_title)
        val restartButton = findViewById<Button>(R.id.btn_restart)
        val finishButton = findViewById<Button>(R.id.btn_finish)

        val result = intent.getStringExtra(EXTRA_RESULT) ?: "Tidak Diketahui"

        // Mengganti spasi dengan newline agar formatnya seperti di gambar
        val formattedResult = result.replace(" ", "\n")
        resultTextView.text = formattedResult

        // Mulai baru -> kembali ke halaman kuesioner
        restartButton.setOnClickListener {
            finish() // Menutup halaman hasil, kembali ke PredictionActivity
        }

        // Selesai -> kembali ke dashboard utama
        finishButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}