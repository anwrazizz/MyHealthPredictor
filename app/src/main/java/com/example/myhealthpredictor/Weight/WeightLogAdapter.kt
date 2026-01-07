package com.example.myhealthpredictor.WeightLog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myhealthpredictor.R
import java.text.SimpleDateFormat
import java.util.*

class WeightLogAdapter(
    private var weightLogs: MutableList<WeightLog>,
    private val onEditClick: (WeightLog) -> Unit,
    private val onDeleteClick: (WeightLog) -> Unit
) : RecyclerView.Adapter<WeightLogAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDay: TextView = itemView.findViewById(R.id.tv_day)
        val tvMonth: TextView = itemView.findViewById(R.id.tv_month)
        val tvWeight: TextView = itemView.findViewById(R.id.tv_weight)
        val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        val btnEdit: ImageView = itemView.findViewById(R.id.btn_edit)
        val btnDelete: ImageView = itemView.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weight_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val weightLog = weightLogs[position]
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = weightLog.date

        // Format tanggal
        val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        holder.tvDay.text = dayFormat.format(calendar.time)
        holder.tvMonth.text = monthFormat.format(calendar.time)
        holder.tvWeight.text = String.format("%.1f", weightLog.weight)
        holder.tvTime.text = "${timeFormat.format(calendar.time)} WIB"

        // Edit button click
        holder.btnEdit.setOnClickListener {
            onEditClick(weightLog)
        }

        // Delete button click
        holder.btnDelete.setOnClickListener {
            onDeleteClick(weightLog)
        }
    }

    override fun getItemCount(): Int = weightLogs.size

    fun updateData(newWeightLogs: List<WeightLog>) {
        weightLogs = newWeightLogs.toMutableList()
        notifyDataSetChanged()
    }

    fun removeItem(weightLog: WeightLog) {
        val position = weightLogs.indexOf(weightLog)
        if (position != -1) {
            weightLogs.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}