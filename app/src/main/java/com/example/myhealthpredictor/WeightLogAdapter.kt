package com.example.myhealthpredictor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class WeightLogAdapter : ListAdapter<WeightLog, WeightLogAdapter.LogViewHolder>(WeightLogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_weight_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = getItem(position)
        holder.bind(log)
    }

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val weightValueTextView: TextView = itemView.findViewById(R.id.tv_weight_value)
        private val weightDateTextView: TextView = itemView.findViewById(R.id.tv_weight_date)

        fun bind(log: WeightLog) {
            weightValueTextView.text = "${log.weight} kg"
            weightDateTextView.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(log.date))
        }
    }

    class WeightLogDiffCallback : DiffUtil.ItemCallback<WeightLog>() {
        override fun areItemsTheSame(oldItem: WeightLog, newItem: WeightLog): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WeightLog, newItem: WeightLog): Boolean {
            return oldItem == newItem
        }
    }
}
