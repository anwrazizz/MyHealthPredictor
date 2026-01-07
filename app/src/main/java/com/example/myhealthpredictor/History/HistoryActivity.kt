package com.example.myhealthpredictor.History

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myhealthpredictor.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var btnBack: ImageView
    private lateinit var tvStatTotal: TextView
    private lateinit var tvStatAverage: TextView
    private lateinit var tvStatChange: TextView
    private lateinit var tvTotalEntries: TextView
    private lateinit var lineChart: LineChart
    private lateinit var layoutChartEmpty: LinearLayout
    private lateinit var rvHistory: RecyclerView
    private lateinit var layoutHistoryEmpty: LinearLayout

    private lateinit var adapter: HistoryAdapter

    private val dateLabels = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        btnBack = findViewById(R.id.btn_back)
        tvStatTotal = findViewById(R.id.tv_stat_total)
        tvStatAverage = findViewById(R.id.tv_stat_average)
        tvStatChange = findViewById(R.id.tv_stat_change)
        tvTotalEntries = findViewById(R.id.tv_total_entries)
        lineChart = findViewById(R.id.line_chart)
        layoutChartEmpty = findViewById(R.id.layout_chart_empty)
        rvHistory = findViewById(R.id.rv_history)
        layoutHistoryEmpty = findViewById(R.id.layout_history_empty)

        // Setup RecyclerView
        setupRecyclerView()

        // Setup Chart
        setupChart()

        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Load data
        loadWeightData()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(mutableListOf())
        rvHistory.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = this@HistoryActivity.adapter
            // PENTING: Jangan set setHasFixedSize untuk dynamic content
            isNestedScrollingEnabled = false
        }

        Log.d("HistoryActivity", "RecyclerView setup completed")
    }

    private fun setupChart() {
        lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            legend.isEnabled = false
            setNoDataText("Belum ada data")

            // X Axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = ContextCompat.getColor(this@HistoryActivity, R.color.text_body)
                textSize = 10f
                granularity = 1f
                setLabelCount(5, false)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        return if (index >= 0 && index < dateLabels.size) {
                            dateLabels[index]
                        } else {
                            ""
                        }
                    }
                }
            }

            // Left Y Axis
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(this@HistoryActivity, R.color.divider)
                textColor = ContextCompat.getColor(this@HistoryActivity, R.color.text_body)
                textSize = 10f
                setDrawAxisLine(false)
                axisLineColor = ContextCompat.getColor(this@HistoryActivity, R.color.divider)
            }

            // Right Y Axis
            axisRight.isEnabled = false

            // Extra padding
            extraBottomOffset = 10f
            extraLeftOffset = 10f
            extraRightOffset = 10f
        }
    }

    private fun loadWeightData() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Log.e("HistoryActivity", "User not logged in")
            showEmptyState()
            return
        }

        Log.d("HistoryActivity", "Loading weight data for user: ${currentUser.uid}")

        db.collection("weight_logs")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("HistoryActivity", "Error loading data: ${e.message}", e)
                    showEmptyState()
                    return@addSnapshotListener
                }

                if (snapshots == null || snapshots.isEmpty) {
                    Log.w("HistoryActivity", "No weight data found")
                    showEmptyState()
                    return@addSnapshotListener
                }

                Log.d("HistoryActivity", "Found ${snapshots.size()} weight entries")

                val weights = snapshots.documents.mapNotNull { doc ->
                    try {
                        val weight = doc.getDouble("weight")
                        val date = doc.getLong("date")

                        Log.d("HistoryActivity", "Document ${doc.id}: weight=$weight, date=$date")

                        if (weight != null && date != null) {
                            Pair(date, weight)
                        } else {
                            Log.w("HistoryActivity", "Skipping document ${doc.id}: missing weight or date")
                            null
                        }
                    } catch (ex: Exception) {
                        Log.e("HistoryActivity", "Error parsing document ${doc.id}: ${ex.message}")
                        null
                    }
                }

                Log.d("HistoryActivity", "Parsed ${weights.size} valid weight entries")

                if (weights.isEmpty()) {
                    showEmptyState()
                } else {
                    updateWeightChart(weights)
                    updateWeightStatistics(weights)
                    updateWeightHistory(weights)
                }
            }
    }

    private fun updateWeightChart(weights: List<Pair<Long, Double>>) {
        // Sort by date ascending for chart
        val sortedWeights = weights.sortedBy { it.first }

        // Prepare date labels
        dateLabels.clear()
        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
        sortedWeights.forEach { (date, _) ->
            dateLabels.add(dateFormat.format(Date(date)))
        }

        // Create entries
        val entries = sortedWeights.mapIndexed { index, pair ->
            Entry(index.toFloat(), pair.second.toFloat())
        }

        val dataSet = LineDataSet(entries, "Berat Badan").apply {
            color = ContextCompat.getColor(this@HistoryActivity, R.color.green_primary)
            setCircleColor(ContextCompat.getColor(this@HistoryActivity, R.color.green_primary))
            lineWidth = 3f
            circleRadius = 6f
            circleHoleRadius = 3f
            setDrawCircleHole(true)
            circleHoleColor = Color.WHITE
            valueTextSize = 11f
            valueTextColor = ContextCompat.getColor(this@HistoryActivity, R.color.text_title)
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(this@HistoryActivity, R.color.green_primary)
            fillAlpha = 40
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            setDrawValues(true)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%.1f", value)
                }
            }
        }

        lineChart.data = LineData(dataSet)
        lineChart.animateX(800)
        lineChart.invalidate()

        lineChart.visibility = View.VISIBLE
        layoutChartEmpty.visibility = View.GONE
    }

    private fun updateWeightStatistics(weights: List<Pair<Long, Double>>) {
        val total = weights.size
        val average = weights.map { it.second }.average()

        // Calculate change (latest - oldest)
        val change = if (weights.size >= 2) {
            val latest = weights.first().second
            val oldest = weights.last().second
            latest - oldest
        } else {
            0.0
        }

        tvStatTotal.text = total.toString()
        tvStatAverage.text = String.format("%.1f", average)

        // Format change with sign
        val changeText = if (change > 0) {
            String.format("+%.1f", change)
        } else {
            String.format("%.1f", change)
        }
        tvStatChange.text = changeText

        // Color for change
        tvStatChange.setTextColor(
            when {
                change < 0 -> ContextCompat.getColor(this, R.color.green_primary) // Turun = bagus
                change > 0 -> Color.parseColor("#F44336") // Naik = perhatian
                else -> ContextCompat.getColor(this, R.color.text_body)
            }
        )

        tvTotalEntries.text = "$total entri"
    }

    private fun updateWeightHistory(weights: List<Pair<Long, Double>>) {
        Log.d("HistoryActivity", "Updating weight history with ${weights.size} items")

        val newHistoryItems = weights.map { (date, weight) ->
            HistoryItem(
                type = HistoryType.WEIGHT,
                title = "Berat Badan",
                value = String.format("%.1f kg", weight),
                date = date,
                icon = "⚖️"
            )
        }

        Log.d("HistoryActivity", "History items created: ${newHistoryItems.size}")

        // Update adapter with new data
        adapter.updateData(newHistoryItems)

        // Show/hide based on data
        if (newHistoryItems.isNotEmpty()) {
            rvHistory.visibility = View.VISIBLE
            layoutHistoryEmpty.visibility = View.GONE
        } else {
            rvHistory.visibility = View.GONE
            layoutHistoryEmpty.visibility = View.VISIBLE
        }

        Log.d("HistoryActivity", "RecyclerView visibility: ${rvHistory.visibility}")
        Log.d("HistoryActivity", "Adapter item count: ${adapter.itemCount}")

        // Post to check after layout
        rvHistory.post {
            Log.d("HistoryActivity", "After layout - child count: ${rvHistory.childCount}")
            Log.d("HistoryActivity", "RecyclerView height: ${rvHistory.height}")
        }
    }

    private fun showEmptyState() {
        lineChart.visibility = View.GONE
        layoutChartEmpty.visibility = View.VISIBLE

        rvHistory.visibility = View.GONE
        layoutHistoryEmpty.visibility = View.VISIBLE

        tvStatTotal.text = "0"
        tvStatAverage.text = "--"
        tvStatChange.text = "--"
        tvTotalEntries.text = "0 entri"
    }
}