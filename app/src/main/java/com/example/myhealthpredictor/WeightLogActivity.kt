package com.example.myhealthpredictor

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class WeightLogActivity : AppCompatActivity() {

    private lateinit var viewModel: WeightLogViewModel
    private lateinit var adapter: WeightLogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weight_log)
        
        val btnBack = findViewById<ImageView>(R.id.btn_back)
        btnBack.setOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.rv_weight_logs)
        adapter = WeightLogAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel = ViewModelProvider(this)[WeightLogViewModel::class.java]
        viewModel.allWeightLogs.observe(this) { logs ->
            logs?.let { adapter.submitList(it) }
        }

        findViewById<FloatingActionButton>(R.id.fab_add_weight_log).setOnClickListener {
            showAddWeightLogDialog()
        }
    }

    private fun showAddWeightLogDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_weight_log, null)
        val weightEditText = dialogView.findViewById<TextInputEditText>(R.id.et_weight_value)

        AlertDialog.Builder(this)
            .setTitle("Tambah Log Berat Badan")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val weightStr = weightEditText.text.toString()

                if (weightStr.isNotEmpty()) {
                    val weight = weightStr.toFloatOrNull() ?: 0f
                    val log = WeightLog(weight = weight, date = System.currentTimeMillis())
                    viewModel.insert(log)
                    Toast.makeText(this, "Log berat badan tersimpan", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Mohon isi berat badan", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
