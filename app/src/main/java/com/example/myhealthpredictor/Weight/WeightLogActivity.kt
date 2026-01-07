package com.example.myhealthpredictor.WeightLog

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myhealthpredictor.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class WeightLogActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var btnBack: ImageView
    private lateinit var tvCurrentWeight: TextView
    private lateinit var tvLastUpdate: TextView
    private lateinit var tvTotalEntries: TextView
    private lateinit var rvWeightHistory: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var fabAddWeight: FloatingActionButton

    private lateinit var adapter: WeightLogAdapter
    private val weightLogs = mutableListOf<WeightLog>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weight_log)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        btnBack = findViewById(R.id.btn_back)
        tvCurrentWeight = findViewById(R.id.tv_current_weight)
        tvLastUpdate = findViewById(R.id.tv_last_update)
        tvTotalEntries = findViewById(R.id.tv_total_entries)
        rvWeightHistory = findViewById(R.id.rv_weight_history)
        layoutEmptyState = findViewById(R.id.layout_empty_state)
        fabAddWeight = findViewById(R.id.fab_add_weight)

        // Setup RecyclerView
        setupRecyclerView()

        // Load data
        loadWeightLogs()

        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // FAB click
        fabAddWeight.setOnClickListener {
            showAddWeightDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = WeightLogAdapter(
            mutableListOf(),
            onEditClick = { showEditWeightDialog(it) },
            onDeleteClick = { showDeleteConfirmDialog(it) }
        )

        rvWeightHistory.layoutManager = LinearLayoutManager(this)
        rvWeightHistory.adapter = adapter
    }

    private fun loadWeightLogs() {
        val currentUser = auth.currentUser ?: return

        db.collection("weight_logs")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->

                if (e != null) {
                    Toast.makeText(this, "Load error: ${e.message}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                val newList = mutableListOf<WeightLog>()

                snapshots?.documents?.forEach { document ->
                    val weightLog = document.toObject(WeightLog::class.java)
                    weightLog?.let {
                        it.id = document.id
                        newList.add(it)
                    }
                }

                adapter.updateData(newList)
                updateUI(newList)
            }
    }

    private fun updateUI(data: List<WeightLog>) {
        if (data.isEmpty()) {
            rvWeightHistory.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
            tvCurrentWeight.text = "--"
            tvLastUpdate.text = "Belum ada data"
            tvTotalEntries.text = "0 entri"
        } else {
            rvWeightHistory.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE

            val latest = data.first()
            tvCurrentWeight.text = String.format("%.1f", latest.weight)

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            tvLastUpdate.text = "Terakhir update: ${dateFormat.format(Date(latest.date))}"

            tvTotalEntries.text = "${data.size} entri"
        }
    }

    private fun showAddWeightDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_weight, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        val etWeight = dialogView.findViewById<TextInputEditText>(R.id.et_weight)
        val etDate = dialogView.findViewById<TextInputEditText>(R.id.et_date)
        val btnMinus = dialogView.findViewById<ImageView>(R.id.btn_minus)
        val btnPlus = dialogView.findViewById<ImageView>(R.id.btn_plus)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_save)

        tvDialogTitle.text = "Tambah Berat Badan"

        // Set default weight
        etWeight.setText("60.0")

        // Set default date (today)
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        etDate.setText(dateFormat.format(calendar.time))

        // Plus/Minus buttons
        btnMinus.setOnClickListener {
            val currentWeight = etWeight.text.toString().toDoubleOrNull() ?: 60.0
            val newWeight = (currentWeight - 0.5).coerceAtLeast(0.0)
            etWeight.setText(String.format("%.1f", newWeight))
        }

        btnPlus.setOnClickListener {
            val currentWeight = etWeight.text.toString().toDoubleOrNull() ?: 60.0
            val newWeight = (currentWeight + 0.5).coerceAtMost(300.0)
            etWeight.setText(String.format("%.1f", newWeight))
        }

        // Date picker
        etDate.setOnClickListener {
            showDatePicker(calendar) { selectedDate ->
                etDate.setText(dateFormat.format(selectedDate.time))
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val weight = etWeight.text.toString().toDoubleOrNull()

            if (weight == null || weight <= 0) {
                Toast.makeText(this, "Berat badan tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveWeightLog(weight, calendar.timeInMillis)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEditWeightDialog(weightLog: WeightLog) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_weight, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        val etWeight = dialogView.findViewById<TextInputEditText>(R.id.et_weight)
        val etDate = dialogView.findViewById<TextInputEditText>(R.id.et_date)
        val btnMinus = dialogView.findViewById<ImageView>(R.id.btn_minus)
        val btnPlus = dialogView.findViewById<ImageView>(R.id.btn_plus)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_save)

        tvDialogTitle.text = "Edit Berat Badan"
        btnSave.text = "Update"

        // Set existing data
        etWeight.setText(String.format("%.1f", weightLog.weight))

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = weightLog.date
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        etDate.setText(dateFormat.format(calendar.time))

        // Plus/Minus buttons
        btnMinus.setOnClickListener {
            val currentWeight = etWeight.text.toString().toDoubleOrNull() ?: 60.0
            val newWeight = (currentWeight - 0.5).coerceAtLeast(0.0)
            etWeight.setText(String.format("%.1f", newWeight))
        }

        btnPlus.setOnClickListener {
            val currentWeight = etWeight.text.toString().toDoubleOrNull() ?: 60.0
            val newWeight = (currentWeight + 0.5).coerceAtMost(300.0)
            etWeight.setText(String.format("%.1f", newWeight))
        }

        // Date picker
        etDate.setOnClickListener {
            showDatePicker(calendar) { selectedDate ->
                etDate.setText(dateFormat.format(selectedDate.time))
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val weight = etWeight.text.toString().toDoubleOrNull()

            if (weight == null || weight <= 0) {
                Toast.makeText(this, "Berat badan tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updateWeightLog(weightLog.id, weight, calendar.timeInMillis)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirmDialog(weightLog: WeightLog) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Data")
            .setMessage("Apakah Anda yakin ingin menghapus data berat badan ini?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteWeightLog(weightLog)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDatePicker(calendar: Calendar, onDateSelected: (Calendar) -> Unit) {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                onDateSelected(calendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveWeightLog(weight: Double, date: Long) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        val weightLog = hashMapOf(
            "userId" to currentUser.uid,
            "weight" to weight,
            "date" to date,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("weight_logs")
            .add(weightLog)
            .addOnSuccessListener {
                Toast.makeText(this, "Berat badan berhasil disimpan", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal simpan: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateWeightLog(id: String, weight: Double, date: Long) {
        db.collection("weight_logs")
            .document(id)
            .update(
                mapOf(
                    "weight" to weight,
                    "date" to date
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Berat badan berhasil diupdate", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal update: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteWeightLog(weightLog: WeightLog) {
        db.collection("weight_logs")
            .document(weightLog.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Berat badan berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}