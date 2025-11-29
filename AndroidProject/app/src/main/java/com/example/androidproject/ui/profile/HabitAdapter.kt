package com.example.androidproject.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.androidproject.databinding.ItemHabitBinding
import com.example.androidproject.model.HabitResponse

class HabitAdapter : ListAdapter<HabitResponse, HabitAdapter.HabitViewHolder>(HabitDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val binding = ItemHabitBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HabitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = getItem(position)
        android.util.Log.d("HabitAdapter", "onBindViewHolder: position=$position, habit=${habit.name} (id=${habit.id})")
        holder.bind(habit)
    }

    override fun getItemCount(): Int {
        val count = super.getItemCount()
        android.util.Log.d("HabitAdapter", "getItemCount: $count items")
        return count
    }

    class HabitViewHolder(private val binding: ItemHabitBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(habit: HabitResponse) {
            binding.tvHabitName.text = habit.name
            
            // Category
            habit.category?.let { category ->
                binding.tvCategory.text = category.name
                binding.tvCategory.visibility = View.VISIBLE
            } ?: run {
                binding.tvCategory.visibility = View.GONE
            }
            
            // Description
            if (!habit.description.isNullOrEmpty()) {
                binding.tvDescription.text = habit.description
                binding.tvDescription.visibility = View.VISIBLE
            } else {
                binding.tvDescription.visibility = View.GONE
            }
            
            // Goal
            if (!habit.goal.isNullOrEmpty()) {
                binding.llGoal.visibility = View.VISIBLE
                binding.tvGoal.text = "Goal: ${habit.goal}"
            } else {
                binding.llGoal.visibility = View.GONE
            }
        }
    }

    class HabitDiffCallback : DiffUtil.ItemCallback<HabitResponse>() {
        override fun areItemsTheSame(oldItem: HabitResponse, newItem: HabitResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HabitResponse, newItem: HabitResponse): Boolean {
            return oldItem == newItem
        }
    }
}
