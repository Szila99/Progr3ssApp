package com.example.androidproject.ui.home

import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.androidproject.R
import com.example.androidproject.databinding.ItemHomeScheduleBinding
import com.example.androidproject.model.ScheduleResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HomeScheduleAdapter(
    private val onItemClick: (ScheduleResponse) -> Unit
) :
    ListAdapter<ScheduleResponse, HomeScheduleAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeScheduleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }
    
    class ViewHolder(
        private val binding: ItemHomeScheduleBinding,
        private val onItemClick: (ScheduleResponse) -> Unit
    ) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: ScheduleResponse) {
            // Click listener
            binding.root.setOnClickListener {
                onItemClick(item)
            }
            
            // Title
            binding.tvTitle.text = item.habit?.name ?: "Unknown Habit"
            
            // Status
            binding.tvStatus.text = item.status ?: "Planned"
            binding.tvStatus.setBackgroundColor(getStatusColor(item.status))
            
            // Status icon with background
            binding.ivStatusIcon.setImageResource(getStatusIcon(item.status))

            // Set circular background color based on status
            val drawable = binding.ivStatusIcon.context.getDrawable(R.drawable.bg_status_icon)?.mutate()
            drawable?.setTint(getStatusColor(item.status))
            binding.ivStatusIcon.background = drawable

            // Time
            val timeText = formatTime(item.startTime, item.endTime, item.durationMinutes)
            binding.tvTime.text = timeText
            
            // Notes
            if (!item.notes.isNullOrEmpty()) {
                binding.tvNotes.visibility = View.VISIBLE
                binding.tvNotes.text = item.notes
            } else {
                binding.tvNotes.visibility = View.GONE
            }
            
            // Participants
            if (!item.participants.isNullOrEmpty()) {
                binding.llParticipants.visibility = View.VISIBLE
                val names = item.participants.joinToString(", ") { it.name }
                binding.tvParticipants.text = "With $names"
            } else {
                binding.llParticipants.visibility = View.GONE
            }
        }
        
        private fun getStatusColor(status: String?): Int {
            return when (status?.lowercase()) {
                "completed" -> Color.parseColor("#4CAF50") // Green
                "planned" -> Color.parseColor("#2196F3") // Blue
                "skipped" -> Color.parseColor("#FF9800") // Orange
                else -> Color.parseColor("#9E9E9E") // Gray
            }
        }
        
        private fun getStatusIcon(status: String?): Int {
            return when (status?.lowercase()) {
                "completed" -> R.drawable.ic_completed
                "planned" -> R.drawable.ic_planned
                "skipped" -> R.drawable.ic_skipped
                else -> R.drawable.ic_planned
            }
        }
        
        private fun formatTime(startTime: String?, endTime: String?, duration: Int?): String {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val start = startTime?.let { parseDateTime(it) }
                    val end = endTime?.let { parseDateTime(it) }
                    
                    when {
                        start != null && end != null -> {
                            val startFormatted = start.format(DateTimeFormatter.ofPattern("HH:mm"))
                            val endFormatted = end.format(DateTimeFormatter.ofPattern("HH:mm"))
                            val durationText = duration?.let { " ($it min)" } ?: ""
                            "$startFormatted - $endFormatted$durationText"
                        }
                        start != null -> {
                            val startFormatted = start.format(DateTimeFormatter.ofPattern("HH:mm"))
                            val durationText = duration?.let { " ($it min)" } ?: ""
                            "$startFormatted$durationText"
                        }
                        duration != null -> "Duration: $duration min"
                        else -> "Time not set"
                    }
                } else {
                    duration?.let { "Duration: $it min" } ?: "Time not set"
                }
            } catch (e: Exception) {
                duration?.let { "Duration: $it min" } ?: "Time not set"
            }
        }
        
        @RequiresApi(Build.VERSION_CODES.O)
        private fun parseDateTime(dateTime: String): LocalDateTime? {
            return try {
                LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_DATE_TIME)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<ScheduleResponse>() {
        override fun areItemsTheSame(oldItem: ScheduleResponse, newItem: ScheduleResponse): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ScheduleResponse, newItem: ScheduleResponse): Boolean {
            return oldItem == newItem
        }
    }
}