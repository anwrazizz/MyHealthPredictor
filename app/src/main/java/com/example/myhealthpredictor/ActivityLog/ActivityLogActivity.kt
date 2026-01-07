package com.example.myhealthpredictor.ActivityLog

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myhealthpredictor.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ActivityLogActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: ActivityLogAdapter
    private var listener: ListenerRegistration? = null

    // UI Elements
    private lateinit var rvActivity: RecyclerView
    private lateinit var tvTotalActivity: TextView
    private lateinit var tvTotalDuration: TextView
    private lateinit var emptyState: LinearLayout
    private lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activity_log)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI
        rvActivity = findViewById(R.id.rv_activity)
        tvTotalActivity = findViewById(R.id.tv_total_activity)
        tvTotalDuration = findViewById(R.id.tv_total_duration)
        emptyState = findViewById(R.id.layout_empty_state)
        btnBack = findViewById(R.id.btn_back)

        setupBackButton()
        setupRecyclerView()
        setupFAB()
        loadDataRealtime()
    }

    private fun setupBackButton() {
        btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = ActivityLogAdapter(
            onEdit = { showDialog(it) },
            onDelete = { deleteActivity(it) }
        )

        rvActivity.layoutManager = LinearLayoutManager(this)
        rvActivity.adapter = adapter
    }

    private fun setupFAB() {
        val fab = findViewById<FloatingActionButton>(R.id.fab_add)
        fab.setOnClickListener {
            showDialog(null)
        }
    }

    // ================= REALTIME FIRESTORE =================
    private fun loadDataRealtime() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "User tidak terautentikasi", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // DEBUG LOG
        android.util.Log.d("ActivityLog", "Loading data for UID: $uid")

        listener = db.collection("activity_logs")
            .whereEqualTo("userId", uid)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("ActivityLog", "Error listening: ${error.message}")
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    android.util.Log.d("ActivityLog", "Documents received: ${snapshot.size()}")

                    val newList = snapshot.documents.mapNotNull { doc ->
                        try {
                            android.util.Log.d("ActivityLog", "Doc ${doc.id}: ${doc.data}")
                            doc.toObject(ActivityLog::class.java)?.apply {
                                id = doc.id
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ActivityLog", "Error parsing doc: ${e.message}")
                            null
                        }
                    }

                    android.util.Log.d("ActivityLog", "Parsed list size: ${newList.size}")

                    // Update adapter
                    adapter.updateData(newList)

                    // Update summary
                    updateSummary(newList)

                    // Show/hide empty state
                    if (newList.isEmpty()) {
                        emptyState.visibility = View.VISIBLE
                        rvActivity.visibility = View.GONE
                        android.util.Log.d("ActivityLog", "Showing empty state")
                    } else {
                        emptyState.visibility = View.GONE
                        rvActivity.visibility = View.VISIBLE
                        android.util.Log.d("ActivityLog", "Showing RecyclerView with ${newList.size} items")
                    }
                } else {
                    android.util.Log.w("ActivityLog", "Snapshot is null")
                }
            }
    }

    private fun updateSummary(list: List<ActivityLog>) {
        val totalActivities = list.size
        val totalDuration = list.sumOf { it.duration }

        tvTotalActivity.text = totalActivities.toString()
        tvTotalDuration.text = "$totalDuration mnt"

        android.util.Log.d("ActivityLog", "Summary - Activities: $totalActivities, Duration: $totalDuration")
    }

    // ================= ADD / EDIT DIALOG =================
    private fun showDialog(data: ActivityLog?) {
        val view = layoutInflater.inflate(R.layout.dialog_add_activity, null)

        val etActivity = view.findViewById<EditText>(R.id.et_activity)
        val etDuration = view.findViewById<EditText>(R.id.et_duration)

        if (data != null) {
            etActivity.setText(data.activity)
            etDuration.setText(data.duration.toString())
        }

        AlertDialog.Builder(this)
            .setTitle(if (data == null) "Tambah Aktivitas" else "Edit Aktivitas")
            .setView(view)
            .setPositiveButton("Simpan") { _, _ ->
                val activityName = etActivity.text.toString().trim()
                val durationText = etDuration.text.toString().trim()

                if (activityName.isEmpty()) {
                    Toast.makeText(this, "Nama aktivitas tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val duration = durationText.toIntOrNull()
                if (duration == null || duration <= 0) {
                    Toast.makeText(this, "Durasi harus angka positif", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (data == null) {
                    addActivity(activityName, duration)
                } else {
                    updateActivity(data.id, activityName, duration)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ================= FIRESTORE ACTIONS =================
    private fun addActivity(name: String, duration: Int) {
        val uid = auth.currentUser?.uid ?: return

        val data = hashMapOf(
            "userId" to uid,
            "activity" to name,
            "duration" to duration,
            "date" to System.currentTimeMillis()
        )

        android.util.Log.d("ActivityLog", "Adding activity: $data")

        db.collection("activity_logs")
            .add(data)
            .addOnSuccessListener { docRef ->
                android.util.Log.d("ActivityLog", "Activity added with ID: ${docRef.id}")
                Toast.makeText(this, "✅ Aktivitas ditambahkan", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ActivityLog", "Failed to add: ${e.message}")
                Toast.makeText(this, "❌ Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateActivity(id: String, name: String, duration: Int) {
        if (id.isEmpty()) {
            Toast.makeText(this, "ID tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        android.util.Log.d("ActivityLog", "Updating activity $id: $name, $duration")

        db.collection("activity_logs")
            .document(id)
            .update(
                mapOf(
                    "activity" to name,
                    "duration" to duration
                )
            )
            .addOnSuccessListener {
                android.util.Log.d("ActivityLog", "Activity updated successfully")
                Toast.makeText(this, "✅ Aktivitas diperbarui", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ActivityLog", "Failed to update: ${e.message}")
                Toast.makeText(this, "❌ Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteActivity(data: ActivityLog) {
        if (data.id.isEmpty()) {
            Toast.makeText(this, "ID tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Hapus Aktivitas")
            .setMessage("Yakin ingin menghapus ${data.activity}?")
            .setPositiveButton("Hapus") { _, _ ->
                android.util.Log.d("ActivityLog", "Deleting activity: ${data.id}")

                db.collection("activity_logs")
                    .document(data.id)
                    .delete()
                    .addOnSuccessListener {
                        android.util.Log.d("ActivityLog", "Activity deleted successfully")
                        Toast.makeText(this, "✅ Aktivitas dihapus", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("ActivityLog", "Failed to delete: ${e.message}")
                        Toast.makeText(this, "❌ Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.remove()
        android.util.Log.d("ActivityLog", "Activity destroyed, listener removed")
    }
}