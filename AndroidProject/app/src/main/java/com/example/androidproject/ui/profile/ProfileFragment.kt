package com.example.androidproject.ui.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.androidproject.R
import com.example.androidproject.databinding.FragmentProfileBinding
import com.example.androidproject.model.ProfileResponseDto
import com.example.androidproject.repository.ProfileRepository
import com.example.androidproject.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ProfileViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @RequiresApi(Build.VERSION_CODES.O)
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            val repository = ProfileRepository(context)
            return ProfileViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProfileViewModel
    private lateinit var habitAdapter: HabitAdapter

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadProfileImage(uri)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = ProfileViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)[ProfileViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
        viewModel.loadProfile()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        // Refresh habits when returning to this fragment
        android.util.Log.d("ProfileFragment", "onResume: Refreshing habits")
        viewModel.refreshHabits()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupUI() {
        // Setup RecyclerView
        habitAdapter = HabitAdapter()
        binding.rvHabits.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = habitAdapter
        }

        // Add Habit button
        binding.btnAddHabit.setOnClickListener {
            showAddHabitDialog()
        }

        // Edit Profile button
        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        // Profile Image click
        binding.ivProfileImage.setOnClickListener {
            openImagePicker()
        }

        // Logout button
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupObservers() {
        viewModel.profile.observe(viewLifecycleOwner) { profile ->
            displayProfile(profile)
        }

        viewModel.habits.observe(viewLifecycleOwner) { habits ->
            android.util.Log.d("ProfileFragment", "Habits observer triggered: ${habits?.size ?: 0} habits")
            habits?.forEach {
                android.util.Log.d("ProfileFragment", "  - ${it.name} (id=${it.id})")
            }
            if (habits.isNullOrEmpty()) {
                binding.rvHabits.visibility = View.GONE
                binding.tvNoHabits.visibility = View.VISIBLE
            } else {
                binding.rvHabits.visibility = View.VISIBLE
                binding.tvNoHabits.visibility = View.GONE
                android.util.Log.d("ProfileFragment", "Submitting ${habits.size} habits to adapter")
                habitAdapter.submitList(habits) {
                    // List submitted, RecyclerView will handle its own scrolling
                    android.util.Log.d("ProfileFragment", "RecyclerView list updated, item count: ${habitAdapter.itemCount}")
                }
            }
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

        viewModel.logoutResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                it.onSuccess { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    // Clear session and navigate to login
                    SessionManager(requireContext()).clearTokens()
                    // Navigate to login screen and clear back stack
                    findNavController().navigate(
                        R.id.loginFragment,
                        null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.nav_graph, true)
                            .build()
                    )
                }
                it.onFailure { error ->
                    Toast.makeText(requireContext(), "Logout failed: ${error.message}", Toast.LENGTH_LONG).show()
                }
                viewModel.clearLogoutResult()
            }
        }

        viewModel.habitCreationResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                it.onSuccess { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
                it.onFailure { error ->
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
                viewModel.clearHabitCreationResult()
            }
        }

        viewModel.profileUpdateResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                it.onSuccess { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
                it.onFailure { error ->
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
                viewModel.clearProfileUpdateResult()
            }
        }
    }

    private fun displayProfile(profile: ProfileResponseDto) {
        binding.tvUsername.text = profile.username
        binding.tvEmail.text = profile.email
        
        if (!profile.description.isNullOrEmpty()) {
            binding.tvDescription.text = profile.description
            binding.tvDescription.visibility = View.VISIBLE
        } else {
            binding.tvDescription.visibility = View.GONE
        }
        
        // Load profile image
        if (!profile.profileImageBase64.isNullOrEmpty()) {
            // Decode base64 and display
            try {
                val imageBytes = android.util.Base64.decode(profile.profileImageBase64, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                binding.ivProfileImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                // Fallback to default image
                binding.ivProfileImage.setImageResource(android.R.drawable.ic_menu_myplaces)
            }
        } else if (!profile.profileImageUrl.isNullOrEmpty()) {
            // TODO: Load from URL with Glide/Coil if needed
            // For now using default
            binding.ivProfileImage.setImageResource(android.R.drawable.ic_menu_myplaces)
        } else {
            // Default image
            binding.ivProfileImage.setImageResource(android.R.drawable.ic_menu_myplaces)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showAddHabitDialog() {
        android.util.Log.d("ProfileFragment", "showAddHabitDialog called")
        
        // Load categories if not loaded
        if (viewModel.categories.value.isNullOrEmpty()) {
            android.util.Log.d("ProfileFragment", "Loading categories...")
            viewModel.loadCategories()
            Toast.makeText(requireContext(), "Loading categories...", Toast.LENGTH_SHORT).show()
        }
        
        // Wait a moment for categories to load
        val categories = viewModel.categories.value
        if (categories.isNullOrEmpty()) {
            android.util.Log.w("ProfileFragment", "No categories available, waiting for load...")
            // Observe categories and show dialog when loaded
            viewModel.categories.observe(viewLifecycleOwner) { loadedCategories ->
                if (!loadedCategories.isNullOrEmpty()) {
                    android.util.Log.d("ProfileFragment", "Categories loaded: ${loadedCategories.size}")
                    showAddHabitDialogInternal(loadedCategories)
                }
            }
            return
        }
        
        android.util.Log.d("ProfileFragment", "Categories already loaded: ${categories.size}")
        showAddHabitDialogInternal(categories)
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showAddHabitDialogInternal(categories: List<com.example.androidproject.model.HabitCategoryResponseDto>) {
        android.util.Log.d("ProfileFragment", "showAddHabitDialogInternal with ${categories.size} categories")
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_habit, null)
        
        val etHabitName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etHabitName)
        val etDescription = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etHabitDescription)
        val etGoal = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etHabitGoal)
        val actvCategory = dialogView.findViewById<android.widget.AutoCompleteTextView>(R.id.actvCategory)

        // Setup category dropdown with loaded categories
        val categoryNames = categories.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
        actvCategory.setAdapter(adapter)
        
        var selectedCategoryId: Long? = null
        actvCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryId = categories[position].id
            android.util.Log.d("ProfileFragment", "Category selected: ${categories[position].name} (id=${categories[position].id})")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add New Habit")
            .setView(dialogView)
            .setPositiveButton("Create") { dialog, _ ->
                val name = etHabitName.text.toString().trim()
                val description = etDescription.text.toString().takeIf { it.isNotBlank() }
                val goal = etGoal.text.toString().trim()
                val categoryText = actvCategory.text.toString().trim()
                
                android.util.Log.d("ProfileFragment", "Create habit clicked: name='$name', goal='$goal', categoryText='$categoryText', selectedCategoryId=$selectedCategoryId")
                
                if (name.isBlank()) {
                    Toast.makeText(requireContext(), "Habit name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (goal.isBlank()) {
                    Toast.makeText(requireContext(), "Goal is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // Ha a user csak beírt, de nem választott, próbáljuk megtalálni a kategóriát
                if (selectedCategoryId == null && categoryText.isNotEmpty()) {
                    selectedCategoryId = viewModel.categories.value?.firstOrNull { 
                        it.name.equals(categoryText, ignoreCase = true) 
                    }?.id
                }
                
                if (selectedCategoryId == null) {
                    Toast.makeText(requireContext(), "Please select a valid category from the list", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                try {
                    android.util.Log.d("ProfileFragment", "Calling createHabit with categoryId=$selectedCategoryId")
                    viewModel.createHabit(name, description, selectedCategoryId!!, goal)
                    dialog.dismiss()
                } catch (e: Exception) {
                    android.util.Log.e("ProfileFragment", "Exception in createHabit call", e)
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { dialog, _ ->
                viewModel.logout()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showEditProfileDialog() {
        // Create inline layout with proper styling
        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 50, 60, 20)
        }
        
        // Username TextInputLayout
        val tilUsername = com.google.android.material.textfield.TextInputLayout(
            requireContext(),
            null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            hint = "Username*"
            boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
        }
        val etUsername = com.google.android.material.textfield.TextInputEditText(tilUsername.context)
        etUsername.setText(viewModel.profile.value?.username ?: "")
        tilUsername.addView(etUsername)
        
        // Add margin between fields
        val usernameLayoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, 32)
        }
        tilUsername.layoutParams = usernameLayoutParams
        
        // Description TextInputLayout
        val tilDescription = com.google.android.material.textfield.TextInputLayout(
            requireContext(),
            null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            hint = "Description (optional)"
            boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
        }
        val etDescription = com.google.android.material.textfield.TextInputEditText(tilDescription.context)
        etDescription.setText(viewModel.profile.value?.description ?: "")
        etDescription.minLines = 2
        etDescription.maxLines = 4
        tilDescription.addView(etDescription)
        
        layout.addView(tilUsername)
        layout.addView(tilDescription)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Profile")
            .setView(layout)
            .setPositiveButton("Save") { dialog, _ ->
                val username = etUsername.text.toString().takeIf { it.isNotBlank() }
                val description = etDescription.text.toString().takeIf { it.isNotBlank() }
                
                if (username.isNullOrBlank()) {
                    Toast.makeText(requireContext(), "Username is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                viewModel.updateProfile(username, description)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun uploadProfileImage(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().cacheDir, "profile_image_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { output ->
                inputStream?.copyTo(output)
            }
            
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("profileImage", file.name, requestFile)
            
            viewModel.uploadProfileImage(body)
            // A fájlt NE töröljük azonnal – hagyjuk, hogy az OkHttp streaming befejeződjön
            // Törlés opcionálisan siker után kezelhető (observer-ben), ha szükséges
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to process image: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
