package com.example.myhealthpredictor.ActivityLog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.myhealthpredictor.R
import java.text.SimpleDateFormat
import java.util.*

class ActivityLogAdapter(
    private val onEdit: (ActivityLog) -> Unit,
    private val onDelete: (ActivityLog) -> Unit
) : RecyclerView.Adapter<ActivityLogAdapter.ViewHolder>() {

    private val items = mutableListOf<ActivityLog>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDay: TextView = view.findViewById(R.id.tv_day)
        val tvMonth: TextView = view.findViewById(R.id.tv_month)
        val tvActivity: TextView = view.findViewById(R.id.tv_activity)
        val tvDuration: TextView = view.findViewById(R.id.tv_duration)
        val btnEdit: ImageView = view.findViewById(R.id.btn_edit)
        val btnDelete: ImageView = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        val calendar = Calendar.getInstance().apply {
            timeInMillis = item.date
        }

        holder.tvDay.text = SimpleDateFormat("dd", Locale.getDefault()).format(calendar.time)
        holder.tvMonth.text = SimpleDateFormat("MMM", Locale("id", "ID")).format(calendar.time).uppercase()
        holder.tvActivity.text = item.activity
        holder.tvDuration.text = "${item.duration} menit"

        holder.btnEdit.setOnClickListener { onEdit(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = items.size

    // 🔥 UPDATE DATA DENGAN DIFFUTIL (ANTI LAG)
    fun updateData(newList: List<ActivityLog>) {
        val diffCallback = ActivityDiffCallback(items, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        items.clear()
        items.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    private class ActivityDiffCallback(
        private val oldList: List<ActivityLog>,
        private val newList: List<ActivityLog>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}