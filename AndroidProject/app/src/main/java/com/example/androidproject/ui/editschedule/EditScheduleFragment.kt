package com.example.androidproject.ui.editschedule

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.androidproject.R
import com.example.androidproject.databinding.FragmentEditScheduleBinding
import com.example.androidproject.model.ScheduleResponse
import com.example.androidproject.repository.ScheduleRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class EditScheduleViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @RequiresApi(Build.VERSION_CODES.O)
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditScheduleViewModel::class.java)) {
            val repository = ScheduleRepository(context)
            return EditScheduleViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class EditScheduleFragment : Fragment() {

    private var _binding: FragmentEditScheduleBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: EditScheduleViewModel
    private var scheduleId: Long = -1
    
    private var selectedDate: LocalDate? = null
    private var selectedStartTime: LocalTime? = null
    private var selectedEndTime: LocalTime? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = EditScheduleViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)[EditScheduleViewModel::class.java]
        
        scheduleId = arguments?.getLong("scheduleId") ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
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
    private fun setupUI() {
        // Date picker
        binding.etDate.setOnClickListener {
            showDatePicker()
        }
        
        // Start time picker
        binding.etStartTime.setOnClickListener {
            showTimePicker(isStartTime = true)
        }
        
        // End time picker
        binding.etEndTime.setOnClickListener {
            showTimePicker(isStartTime = false)
        }
        
        // Cancel button
        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Save button
        binding.btnSave.setOnClickListener {
            saveSchedule()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupObservers() {
        viewModel.schedule.observe(viewLifecycleOwner) { schedule ->
            populateFields(schedule)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSave.isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                it.onSuccess { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                it.onFailure { error ->
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
                viewModel.clearUpdateResult()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun populateFields(schedule: ScheduleResponse) {
        // Title
        binding.tvScheduleTitle.text = "Edit: ${schedule.habit?.name ?: "Schedule"}"
        
        // Date
        schedule.date?.let {
            selectedDate = try {
                LocalDate.parse(it)
            } catch (e: Exception) {
                null
            }
            binding.etDate.setText(it)
        }
        
        // Start time
        schedule.startTime?.let {
            try {
                val dateTime = LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME)
                selectedStartTime = dateTime.toLocalTime()
                binding.etStartTime.setText(selectedStartTime?.format(DateTimeFormatter.ofPattern("HH:mm")))
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
        
        // End time
        schedule.endTime?.let {
            try {
                val dateTime = LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME)
                selectedEndTime = dateTime.toLocalTime()
                binding.etEndTime.setText(selectedEndTime?.format(DateTimeFormatter.ofPattern("HH:mm")))
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
        
        // Duration
        schedule.durationMinutes?.let {
            binding.etDuration.setText(it.toString())
        }
        
        // Status
        when (schedule.status?.lowercase()) {
            "completed" -> binding.rbCompleted.isChecked = true
            "skipped" -> binding.rbSkipped.isChecked = true
            else -> binding.rbPlanned.isChecked = true
        }
        
        // Notes
        binding.etNotes.setText(schedule.notes ?: "")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        selectedDate?.let {
            calendar.set(it.year, it.monthValue - 1, it.dayOfMonth)
        }
        
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                selectedDate = LocalDate.of(year, month + 1, day)
                binding.etDate.setText(selectedDate?.format(DateTimeFormatter.ISO_LOCAL_DATE))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showTimePicker(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()
        val currentTime = if (isStartTime) selectedStartTime else selectedEndTime
        currentTime?.let {
            calendar.set(Calendar.HOUR_OF_DAY, it.hour)
            calendar.set(Calendar.MINUTE, it.minute)
        }
        
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                val time = LocalTime.of(hour, minute)
                if (isStartTime) {
                    selectedStartTime = time
                    binding.etStartTime.setText(time.format(DateTimeFormatter.ofPattern("HH:mm")))
                } else {
                    selectedEndTime = time
                    binding.etEndTime.setText(time.format(DateTimeFormatter.ofPattern("HH:mm")))
                }
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveSchedule() {
        // Get status
        val status = when (binding.rgStatus.checkedRadioButtonId) {
            binding.rbCompleted.id -> "Completed"
            binding.rbSkipped.id -> "Skipped"
            else -> "Planned"
        }
        
        // Get duration
        val duration = binding.etDuration.text.toString().toIntOrNull()
        
        // Get notes
        val notes = binding.etNotes.text.toString().takeIf { it.isNotBlank() }
        
        // Build date-time strings
        val date = selectedDate?.toString()
        val startTimeStr = if (selectedDate != null && selectedStartTime != null) {
            LocalDateTime.of(selectedDate, selectedStartTime).format(DateTimeFormatter.ISO_DATE_TIME)
        } else null
        
        val endTimeStr = if (selectedDate != null && selectedEndTime != null) {
            LocalDateTime.of(selectedDate, selectedEndTime).format(DateTimeFormatter.ISO_DATE_TIME)
        } else null
        
        viewModel.updateSchedule(
            scheduleId = scheduleId,
            startTime = startTimeStr,
            endTime = endTimeStr,
            durationMinutes = duration,
            status = status,
            date = date,
            notes = notes
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
