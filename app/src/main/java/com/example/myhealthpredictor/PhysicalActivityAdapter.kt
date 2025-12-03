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

class PhysicalActivityAdapter : ListAdapter<PhysicalActivity, PhysicalActivityAdapter.ActivityViewHolder>(PhysicalActivityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_physical_activity, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val activity = getItem(position)
        holder.bind(activity)
    }

    class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.tv_activity_name)
        private val durationTextView: TextView = itemView.findViewById(R.id.tv_activity_duration)
        private val dateTextView: TextView = itemView.findViewById(R.id.tv_activity_date)

        fun bind(activity: PhysicalActivity) {
            nameTextView.text = activity.name
            durationTextView.text = "${activity.duration} menit"
            dateTextView.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(activity.date))
        }
    }

    class PhysicalActivityDiffCallback : DiffUtil.ItemCallback<PhysicalActivity>() {
        override fun areItemsTheSame(oldItem: PhysicalActivity, newItem: PhysicalActivity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PhysicalActivity, newItem: PhysicalActivity): Boolean {
            return oldItem == newItem
        }
    }
}
