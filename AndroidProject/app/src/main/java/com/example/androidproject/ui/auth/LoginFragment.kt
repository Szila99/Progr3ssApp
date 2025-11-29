package com.example.androidproject.ui.auth

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
import com.example.androidproject.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeAuthResult()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Both fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("LoginFragment", "Attempt login: email=$email, password=$password")

            authViewModel.login(email, password)
        }

        // TODO: Uncomment after IDE cache refresh
        binding.tvForgotPassword?.setOnClickListener {
            // Navigate to Reset Password Screen
            findNavController().navigate(R.id.action_loginFragment_to_resetPasswordFragment)
        }

        binding.tvRegisterPrompt?.setOnClickListener {
            // Navigate to Register Screen
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun observeAuthResult() {
        authViewModel.authResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { _ ->
                Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()

                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            }

            result.onFailure { error ->
                Toast.makeText(requireContext(), "Login failed: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("LoginFragment", "Login error", error)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
