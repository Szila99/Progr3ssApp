package com.example.androidproject.ui.schedule

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.androidproject.databinding.FragmentCreateScheduleBinding
import com.example.androidproject.model.HabitResponse
import com.example.androidproject.repository.ScheduleRepository
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class CreateScheduleViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @RequiresApi(Build.VERSION_CODES.O)
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateScheduleViewModel::class.java)) {
            val repository = ScheduleRepository(context)
            return CreateScheduleViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class CreateScheduleFragment : Fragment() {

    private var _binding: FragmentCreateScheduleBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CreateScheduleViewModel
    
    private var selectedHabit: HabitResponse? = null
    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null
    
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = CreateScheduleViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)[CreateScheduleViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupObservers()
        // Habits are loaded in onResume() to ensure fresh data
        viewModel.loadCategories()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        // Refresh habits when returning to this fragment
        android.util.Log.d("CreateScheduleFragment", "onResume: Refreshing habits")
        viewModel.loadHabits()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupUI() {
        // Date picker
        binding.etDate.setOnClickListener {
            showDatePicker()
        }
        
        // Time picker
        binding.etStartTime.setOnClickListener {
            showTimePicker()
        }
        
        // Set default date to today
        selectedDate = LocalDate.now()
        binding.etDate.setText(selectedDate?.format(DateTimeFormatter.ISO_LOCAL_DATE))
        
        // Create new habit button
        binding.btnCreateNewHabit.setOnClickListener {
            showCreateHabitDialog()
        }
        
        // Repeat pattern radio buttons
        binding.rgRepeatPattern.setOnCheckedChangeListener { _: android.widget.RadioGroup, checkedId: Int ->
            handleRepeatPatternChange(checkedId)
        }
        
        // Cancel button
        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Create button
        binding.btnCreate.setOnClickListener {
            createSchedule()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupObservers() {
        viewModel.habits.observe(viewLifecycleOwner) { habits ->
            android.util.Log.d("CreateScheduleFragment", "Habits observer triggered: ${habits.size} habits received")
            if (habits.isNotEmpty()) {
                setupHabitAutoComplete(habits)
            } else {
                android.util.Log.w("CreateScheduleFragment", "No habits available")
                binding.actvHabit.setAdapter(null)
                selectedHabit = null
            }
        }
        
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            // Categories will be used in the create habit dialog
        }
        
        viewModel.createResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                it.onSuccess { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    findNavController().popBackStack()
                }
                it.onFailure { error ->
                    Toast.makeText(
                        requireContext(),
                        "Error: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                viewModel.clearResult()
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnCreate.isEnabled = !isLoading
        }
    }

    private fun setupHabitAutoComplete(habits: List<HabitResponse>) {
        android.util.Log.d("CreateScheduleFragment", "setupHabitAutoComplete called with ${habits.size} habits")
        habits.forEach {
            android.util.Log.d("CreateScheduleFragment", "  - Habit: ${it.name} (id=${it.id})")
        }

        val habitNames = habits.map { it.name }
        android.util.Log.d("CreateScheduleFragment", "Habit names: $habitNames")

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, habitNames)
        binding.actvHabit.setAdapter(adapter)
        
        android.util.Log.d("CreateScheduleFragment", "Adapter set with ${adapter.count} items")

        // Show dropdown on click/focus
        binding.actvHabit.threshold = 1
        binding.actvHabit.setOnClickListener {
            android.util.Log.d("CreateScheduleFragment", "actvHabit clicked, showing dropdown")
            binding.actvHabit.showDropDown()
        }
        binding.actvHabit.setOnFocusChangeListener { _, hasFocus ->
            android.util.Log.d("CreateScheduleFragment", "actvHabit focus changed: hasFocus=$hasFocus, text='${binding.actvHabit.text}'")
            if (hasFocus) {
                binding.actvHabit.showDropDown()
            }
        }

        // When user selects from dropdown
        binding.actvHabit.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position) as? String
            selectedHabit = selectedName?.let { name -> habits.firstOrNull { it.name == name } }
            android.util.Log.d("CreateScheduleFragment", "Habit selected: ${selectedHabit?.name} (id=${selectedHabit?.id})")
        }

        // Track text changes to clear selection if user types something not in list
        binding.actvHabit.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString().orEmpty()
                if (text.isBlank()) {
                    selectedHabit = null
                } else {
                    // Check if exact match exists
                    val match = habits.firstOrNull { it.name.equals(text, ignoreCase = true) }
                    if (match != null) {
                        selectedHabit = match
                    }
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        android.util.Log.d("CreateScheduleFragment", "AutoComplete setup complete with ${habits.size} habits")
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
    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        selectedTime?.let {
            calendar.set(Calendar.HOUR_OF_DAY, it.hour)
            calendar.set(Calendar.MINUTE, it.minute)
        }
        
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                selectedTime = LocalTime.of(hour, minute)
                binding.etStartTime.setText(selectedTime?.format(DateTimeFormatter.ofPattern("HH:mm")))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showCreateHabitDialog() {
        val dialogView = layoutInflater.inflate(
            com.example.androidproject.R.layout.dialog_create_habit,
            null
        )
        
        // Setup category dropdown
        val actvCategory = dialogView.findViewById<android.widget.AutoCompleteTextView>(
            com.example.androidproject.R.id.actvCategory
        )
        
        val categories = viewModel.categories.value ?: emptyList()
        val categoryNames = categories.map { it.name }
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
        actvCategory.setAdapter(categoryAdapter)
        
        var selectedCategoryId: Long? = null
        actvCategory.setOnItemClickListener { _: android.widget.AdapterView<*>, _: android.view.View, position: Int, _: Long ->
            selectedCategoryId = categories[position].id
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create New Habit")
            .setView(dialogView as View)
            .setPositiveButton("Create") { dialog: android.content.DialogInterface, _: Int ->
                val etName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
                    com.example.androidproject.R.id.etHabitName
                )
                val etDesc = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
                    com.example.androidproject.R.id.etHabitDescription
                )
                val etGoal = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
                    com.example.androidproject.R.id.etHabitGoal
                )
                
                val name = etName.text.toString().trim()
                val description = etDesc.text.toString().takeIf { text: String -> text.isNotBlank() }
                val goal = etGoal.text.toString().trim()
                val categoryText = actvCategory.text.toString().trim()
                
                android.util.Log.d("CreateScheduleFragment", "Create habit clicked: name='$name', goal='$goal', categoryText='$categoryText', selectedCategoryId=$selectedCategoryId")
                
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "Habit name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (goal.isEmpty()) {
                    Toast.makeText(requireContext(), "Goal is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // Ha a user csak beírt, de nem választott, próbáljuk megtalálni a kategóriát
                if (selectedCategoryId == null && categoryText.isNotEmpty()) {
                    selectedCategoryId = categories.firstOrNull { 
                        it.name.equals(categoryText, ignoreCase = true) 
                    }?.id
                }
                
                if (selectedCategoryId == null) {
                    Toast.makeText(requireContext(), "Please select a valid category from the list", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                try {
                    android.util.Log.d("CreateScheduleFragment", "Calling createHabit with categoryId=$selectedCategoryId")
                    viewModel.createHabit(
                        name = name,
                        description = description,
                        categoryId = selectedCategoryId!!,
                        goal = goal
                    )
                    dialog.dismiss()
                } catch (e: Exception) {
                    android.util.Log.e("CreateScheduleFragment", "Exception in createHabit call", e)
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel") { dialog: android.content.DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .show()
    }

    private fun handleRepeatPatternChange(checkedId: Int) {
        when (checkedId) {
            binding.rbNone.id -> {
                binding.layoutCustomDays.visibility = View.GONE
                binding.tilRepeatDays.visibility = View.GONE
                binding.tilDate.visibility = View.VISIBLE
            }
            binding.rbDaily.id, binding.rbWeekdays.id, binding.rbWeekends.id -> {
                binding.layoutCustomDays.visibility = View.GONE
                binding.tilRepeatDays.visibility = View.VISIBLE
                binding.tilDate.visibility = View.GONE
            }
            binding.rbCustomDays.id -> {
                binding.layoutCustomDays.visibility = View.VISIBLE
                binding.tilRepeatDays.visibility = View.GONE
                binding.tilDate.visibility = View.GONE
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createSchedule() {
        // Validation
        if (selectedHabit == null) {
            Toast.makeText(requireContext(), "Please select a habit", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedTime == null) {
            Toast.makeText(requireContext(), "Please select start time", Toast.LENGTH_SHORT).show()
            return
        }
        
        val habitId = selectedHabit!!.id
        val duration = binding.etDuration.text.toString().toIntOrNull()
        val notes = binding.etNotes.text.toString().takeIf { text: String -> text.isNotBlank() }
        
        when (binding.rgRepeatPattern.checkedRadioButtonId) {
            binding.rbNone.id -> {
                if (selectedDate == null) {
                    Toast.makeText(requireContext(), "Please select a date", Toast.LENGTH_SHORT).show()
                    return
                }
                val dateTime = LocalDateTime.of(selectedDate, selectedTime)
                viewModel.createCustomSchedule(
                    habitId = habitId,
                    date = dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    startTime = dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    durationMinutes = duration,
                    notes = notes
                )
            }
            binding.rbDaily.id -> {
                val repeatDays = binding.etRepeatDays.text.toString().toIntOrNull() ?: 30
                val startDateTime = LocalDateTime.of(LocalDate.now(), selectedTime)
                viewModel.createRecurringSchedule(
                    habitId = habitId,
                    startTime = startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    repeatPattern = "daily",
                    durationMinutes = duration,
                    repeatDays = repeatDays,
                    notes = notes
                )
            }
            binding.rbWeekdays.id -> {
                val repeatDays = binding.etRepeatDays.text.toString().toIntOrNull() ?: 30
                val startDateTime = LocalDateTime.of(LocalDate.now(), selectedTime)
                viewModel.createRecurringSchedule(
                    habitId = habitId,
                    startTime = startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    repeatPattern = "weekdays",
                    durationMinutes = duration,
                    repeatDays = repeatDays,
                    notes = notes
                )
            }
            binding.rbWeekends.id -> {
                val repeatDays = binding.etRepeatDays.text.toString().toIntOrNull() ?: 30
                val startDateTime = LocalDateTime.of(LocalDate.now(), selectedTime)
                viewModel.createRecurringSchedule(
                    habitId = habitId,
                    startTime = startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    repeatPattern = "weekends",
                    durationMinutes = duration,
                    repeatDays = repeatDays,
                    notes = notes
                )
            }
            binding.rbCustomDays.id -> {
                val daysOfWeek = getSelectedDaysOfWeek()
                if (daysOfWeek.isEmpty()) {
                    Toast.makeText(requireContext(), "Please select at least one day", Toast.LENGTH_SHORT).show()
                    return
                }
                val numberOfWeeks = binding.etNumberOfWeeks.text.toString().toIntOrNull() ?: 4
                val startDateTime = LocalDateTime.of(LocalDate.now(), selectedTime)
                viewModel.createWeekdayRecurringSchedule(
                    habitId = habitId,
                    startTime = startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    daysOfWeek = daysOfWeek,
                    numberOfWeeks = numberOfWeeks,
                    durationMinutes = duration,
                    notes = notes
                )
            }
        }
    }

    private fun getSelectedDaysOfWeek(): List<Int> {
        val days = mutableListOf<Int>()
        val chips = listOf(
            Pair(binding.chipMon, 1),
            Pair(binding.chipTue, 2),
            Pair(binding.chipWed, 3),
            Pair(binding.chipThu, 4),
            Pair(binding.chipFri, 5),
            Pair(binding.chipSat, 6),
            Pair(binding.chipSun, 7)
        )
        chips.forEach { pair: Pair<Chip, Int> ->
            if (pair.first.isChecked) {
                days.add(pair.second)
            }
        }
        return days
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
