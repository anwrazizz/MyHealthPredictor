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

class PhysicalActivityActivity : AppCompatActivity() {

    private lateinit var viewModel: PhysicalActivityViewModel
    private lateinit var adapter: PhysicalActivityAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_physical)
        
        val btnBack = findViewById<ImageView>(R.id.btn_back)
        btnBack.setOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.rv_physical_activities)
        adapter = PhysicalActivityAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel = ViewModelProvider(this)[PhysicalActivityViewModel::class.java]
        viewModel.allActivities.observe(this) { activities ->
            activities?.let { adapter.submitList(it) }
        }

        findViewById<FloatingActionButton>(R.id.fab_add_physical_activity).setOnClickListener {
            showAddActivityDialog()
        }
    }

    private fun showAddActivityDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_physical_activity, null)
        val nameEditText = dialogView.findViewById<TextInputEditText>(R.id.et_activity_name)
        val durationEditText = dialogView.findViewById<TextInputEditText>(R.id.et_activity_duration)

        AlertDialog.Builder(this)
            .setTitle("Tambah Aktivitas Fisik")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val name = nameEditText.text.toString()
                val durationStr = durationEditText.text.toString()

                if (name.isNotEmpty() && durationStr.isNotEmpty()) {
                    val duration = durationStr.toIntOrNull() ?: 0
                    val activity = PhysicalActivity(name = name, duration = duration, date = System.currentTimeMillis())
                    viewModel.insert(activity)
                    Toast.makeText(this, "Aktivitas tersimpan", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Mohon isi semua data", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
