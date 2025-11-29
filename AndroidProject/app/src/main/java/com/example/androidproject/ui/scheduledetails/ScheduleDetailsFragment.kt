package com.example.androidproject.ui.scheduledetails

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.os.bundleOf
import com.example.androidproject.R
import com.example.androidproject.databinding.FragmentScheduleDetailsBinding
import com.example.androidproject.model.ScheduleResponse
import com.example.androidproject.repository.ScheduleRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ScheduleDetailsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @RequiresApi(Build.VERSION_CODES.O)
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleDetailsViewModel::class.java)) {
            val repository = ScheduleRepository(context)
            return ScheduleDetailsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class ScheduleDetailsFragment : Fragment() {

    private var _binding: FragmentScheduleDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ScheduleDetailsViewModel
    private lateinit var progressAdapter: ProgressAdapter
    private var scheduleId: Long = -1
    private var isEditingNotes: Boolean = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = ScheduleDetailsViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)[ScheduleDetailsViewModel::class.java]
        
        // Get schedule ID from arguments
        scheduleId = arguments?.getLong("scheduleId") ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        setupUI()
        setupObservers()
        
        if (scheduleId != -1L) {
            viewModel.loadSchedule(scheduleId)
        } else {
            Toast.makeText(requireContext(), "Invalid schedule ID", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.schedule_details_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_delete -> {
                        showDeleteConfirmationDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupUI() {
        // Setup RecyclerView for progress
        progressAdapter = ProgressAdapter()
        binding.rvRecentActivities.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = progressAdapter
        }

        // Edit button click listener
        binding.fabEditSchedule.setOnClickListener {
            val bundle = bundleOf("scheduleId" to scheduleId)
            findNavController().navigate(
                R.id.action_scheduleDetailsFragment_to_editScheduleFragment,
                bundle
            )
        }

        // FAB click listener
        binding.fabAddProgress.setOnClickListener {
            viewModel.schedule.value?.let { schedule ->
                showAddProgressDialog(schedule)
            }
        }

        // Edit notes button
        binding.btnEditNotes.setOnClickListener {
            enterNotesEditMode()
        }

        // Cancel notes edit button
        binding.btnCancelNotesEdit.setOnClickListener {
            exitNotesEditMode()
        }

        // Save notes button
        binding.btnSaveNotes.setOnClickListener {
            saveNotes()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun enterNotesEditMode() {
        isEditingNotes = true
        binding.tvScheduleNotes.visibility = View.GONE
        binding.layoutNotesEdit.visibility = View.VISIBLE
        binding.btnEditNotes.visibility = View.GONE
        
        // Populate the EditText with current notes
        val currentNotes = viewModel.schedule.value?.notes ?: ""
        binding.etScheduleNotes.setText(currentNotes)
        binding.etScheduleNotes.requestFocus()
    }

    private fun exitNotesEditMode() {
        isEditingNotes = false
        binding.tvScheduleNotes.visibility = View.VISIBLE
        binding.layoutNotesEdit.visibility = View.GONE
        binding.btnEditNotes.visibility = View.VISIBLE
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveNotes() {
        val notes = binding.etScheduleNotes.text.toString().takeIf { it.isNotBlank() }
        viewModel.updateScheduleNotes(scheduleId, notes)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupObservers() {
        viewModel.schedule.observe(viewLifecycleOwner) { schedule ->
            displayScheduleDetails(schedule)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.progressResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                it.onSuccess { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
                it.onFailure { error ->
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
                viewModel.clearProgressResult()
            }
        }

        viewModel.updateNotesResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                it.onSuccess { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    exitNotesEditMode()
                }
                it.onFailure { error ->
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
                viewModel.clearUpdateNotesResult()
            }
        }

        viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                it.onSuccess { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                it.onFailure { error ->
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
                viewModel.clearDeleteResult()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun displayScheduleDetails(schedule: ScheduleResponse) {
        // Habit details
        schedule.habit?.let { habit ->
            binding.tvHabitName.text = habit.name
            
            habit.category?.let { category ->
                binding.tvCategory.text = "📂 ${category.name}"
                binding.tvCategory.visibility = View.VISIBLE
            }
            
            if (!habit.description.isNullOrEmpty()) {
                binding.tvDescription.text = habit.description
                binding.tvDescription.visibility = View.VISIBLE
            }
            
            if (!habit.goal.isNullOrEmpty()) {
                binding.llGoal.visibility = View.VISIBLE
                binding.tvGoal.text = "Goal: ${habit.goal}"
            }
        }

        // Schedule information
        binding.tvDate.text = schedule.date ?: "N/A"
        binding.tvTime.text = formatTime(schedule.startTime, schedule.endTime, schedule.durationMinutes)
        binding.tvStatus.text = schedule.status ?: "Planned"

        // Notes section
        if (!schedule.notes.isNullOrEmpty()) {
            binding.layoutNotes.visibility = View.VISIBLE
            binding.tvScheduleNotes.text = schedule.notes
            // Exit edit mode if we were editing and data reloaded
            if (isEditingNotes) {
                exitNotesEditMode()
            }
        } else {
            binding.layoutNotes.visibility = View.VISIBLE
            binding.tvScheduleNotes.text = "No notes yet"
        }

        // Progress overview
        val progress = schedule.progress ?: emptyList()
        if (progress.isNotEmpty()) {
            val completedCount = progress.count { it.isCompleted }
            val percentage = (completedCount * 100) / progress.size
            
            binding.progressBarCompletion.progress = percentage
            binding.tvProgressPercentage.text = "$percentage% Completed ($completedCount/${progress.size})"
            
            // Display recent activities
            progressAdapter.submitList(progress.sortedByDescending { it.date })
            binding.rvRecentActivities.visibility = View.VISIBLE
            binding.tvNoActivities.visibility = View.GONE
        } else {
            binding.progressBarCompletion.progress = 0
            binding.tvProgressPercentage.text = "0% Completed"
            binding.rvRecentActivities.visibility = View.GONE
            binding.tvNoActivities.visibility = View.VISIBLE
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatTime(startTime: String?, endTime: String?, duration: Int?): String {
        return try {
            val start = startTime?.let { parseDateTime(it) }
            val end = endTime?.let { parseDateTime(it) }

            when {
                start != null && end != null -> {
                    val startFormatted = start.format(DateTimeFormatter.ofPattern("HH:mm"))
                    val endFormatted = end.format(DateTimeFormatter.ofPattern("HH:mm"))
                    "$startFormatted - $endFormatted"
                }
                start != null -> {
                    val startFormatted = start.format(DateTimeFormatter.ofPattern("HH:mm"))
                    val durationText = duration?.let { " ($it min)" } ?: ""
                    "$startFormatted$durationText"
                }
                duration != null -> "$duration minutes"
                else -> "Time not set"
            }
        } catch (e: Exception) {
            duration?.let { "$it minutes" } ?: "Time not set"
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showAddProgressDialog(schedule: ScheduleResponse) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_progress, null)

        val tvInfo = dialogView.findViewById<android.widget.TextView>(R.id.tvScheduleInfo)
        tvInfo.text = "Add Progress: ${schedule.habit?.name ?: "Schedule"}"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Progress")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val etLoggedTime = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
                    R.id.etLoggedTime
                )
                val etNotes = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
                    R.id.etProgressNotes
                )
                val cbCompleted = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(
                    R.id.cbCompleted
                )

                val loggedTime = etLoggedTime.text.toString().toIntOrNull()
                val notes = etNotes.text.toString().takeIf { it.isNotBlank() }
                val isCompleted = if (cbCompleted.isChecked) true else null

                val date = schedule.date ?: LocalDate.now().toString()

                viewModel.addProgress(
                    scheduleId = schedule.id,
                    date = date,
                    loggedTime = loggedTime,
                    notes = notes,
                    isCompleted = isCompleted
                )

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Schedule")
            .setMessage("Are you sure you want to delete this schedule? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                viewModel.deleteSchedule(scheduleId)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
