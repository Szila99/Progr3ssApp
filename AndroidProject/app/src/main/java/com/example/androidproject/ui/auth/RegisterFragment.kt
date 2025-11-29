package com.example.androidproject.ui.auth

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.androidproject.R
import com.example.androidproject.databinding.FragmentRegisterBinding
import com.google.android.material.textfield.TextInputLayout

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button to navigate to login
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val passwordConfirm = binding.etPasswordConfirm.text.toString()

            // Reset error states
            binding.tilPassword.error = null
            binding.tilPasswordConfirm.error = null
            resetInputLayoutColor(binding.tilPassword)
            resetInputLayoutColor(binding.tilPasswordConfirm)

            // Validate fields
            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
                Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check password mismatch
            if (password != passwordConfirm) {
                setInputLayoutError(binding.tilPassword, "Passwords do not match")
                setInputLayoutError(binding.tilPasswordConfirm, "Passwords do not match")
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("RegisterFragment", "Attempt register: username=$username, email=$email")

            authViewModel.register(username, email, password)
        }

        authViewModel.authResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { _ ->
                Toast.makeText(requireContext(), "Registration successful!", Toast.LENGTH_SHORT).show()
                // Navigate to home screen
                findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
            }

            result.onFailure { error ->
                Toast.makeText(requireContext(), "Registration failed: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("RegisterFragment", "Registration error", error)
            }
        }
    }

    private fun setInputLayoutError(layout: TextInputLayout, message: String) {
        layout.error = message
        layout.boxStrokeColor = Color.RED
    }

    private fun resetInputLayoutColor(layout: TextInputLayout) {
        layout.boxStrokeColor = resources.getColor(
            com.google.android.material.R.color.design_default_color_primary, null
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}