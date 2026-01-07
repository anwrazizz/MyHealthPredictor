package com.example.myhealthpredictor.History

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myhealthpredictor.R
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private var historyItems: MutableList<HistoryItem>
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvIcon: TextView = itemView.findViewById(R.id.tv_icon)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        val tvValue: TextView = itemView.findViewById(R.id.tv_value)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyItems[position]

        holder.tvIcon.text = item.icon
        holder.tvTitle.text = item.title
        holder.tvValue.text = item.value

        // Format date
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        holder.tvDate.text = dateFormat.format(Date(item.date))
    }

    override fun getItemCount(): Int = historyItems.size

    fun updateData(newItems: List<HistoryItem>) {
        historyItems.clear()
        historyItems.addAll(newItems)
        notifyDataSetChanged()
    }
}