package com.example.androidproject.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.androidproject.databinding.FragmentResetPasswordBinding
import com.example.androidproject.repository.AuthRepository
import kotlinx.coroutines.launch

class ResetPasswordFragment : Fragment() {

    private var _binding: FragmentResetPasswordBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: AuthRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResetPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = AuthRepository(requireContext())
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnResetPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Email is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Please enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            resetPassword(email)
        }

        binding.tvBackToLogin.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun resetPassword(email: String) {
        binding.btnResetPassword.isEnabled = false
        binding.btnResetPassword.text = "Sending..."

        lifecycleScope.launch {
            try {
                val response = repository.resetPassword(email)
                
                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(
                        requireContext(),
                        response.body()?.message ?: "Password reset instructions sent to $email",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.d("ResetPassword", "Success: ${response.body()?.message}")
                    
                    // Navigate back to login
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to send reset email. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("ResetPassword", "Error: ${response.code()} - ${response.message()}")
                    binding.btnResetPassword.isEnabled = true
                    binding.btnResetPassword.text = "Send Reset Instructions"
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Network error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("ResetPassword", "Exception", e)
                binding.btnResetPassword.isEnabled = true
                binding.btnResetPassword.text = "Send Reset Instructions"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
