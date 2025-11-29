package com.example.androidproject.ui.scheduledetails

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.androidproject.databinding.ItemProgressBinding
import com.example.androidproject.model.ProgressResponseDto

class ProgressAdapter : ListAdapter<ProgressResponseDto, ProgressAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProgressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemProgressBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(progress: ProgressResponseDto) {
            // Date
            binding.tvProgressDate.text = progress.date ?: "Unknown Date"

            // Completion badge
            if (progress.isCompleted) {
                binding.tvCompletionBadge.visibility = View.VISIBLE
                binding.tvCompletionBadge.text = "✓ Completed"
            } else {
                binding.tvCompletionBadge.visibility = View.GONE
            }

            // Logged time
            if (progress.loggedTime != null && progress.loggedTime > 0) {
                binding.tvLoggedTime.visibility = View.VISIBLE
                binding.tvLoggedTime.text = "⏱ Logged: ${progress.loggedTime} minutes"
            } else {
                binding.tvLoggedTime.visibility = View.GONE
            }

            // Notes
            if (!progress.notes.isNullOrEmpty()) {
                binding.tvProgressNotes.visibility = View.VISIBLE
                binding.tvProgressNotes.text = "📝 ${progress.notes}"
            } else {
                binding.tvProgressNotes.visibility = View.GONE
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ProgressResponseDto>() {
        override fun areItemsTheSame(
            oldItem: ProgressResponseDto,
            newItem: ProgressResponseDto
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: ProgressResponseDto,
            newItem: ProgressResponseDto
        ): Boolean {
            return oldItem == newItem
        }
    }
}
