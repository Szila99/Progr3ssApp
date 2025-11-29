package com.example.androidproject.ui.home

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.os.bundleOf
import com.example.androidproject.R
import com.example.androidproject.databinding.FragmentHomeBinding
import com.example.androidproject.repository.ScheduleRepository
import java.time.LocalDate

class HomeViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val repository = ScheduleRepository(context)
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: HomeScheduleAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = HomeViewModelFactory(requireContext())
        viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]
    }
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
        setupObservers()
        loadSchedules()
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupUi() {
        adapter = HomeScheduleAdapter { schedule ->
            // Navigate to schedule details
            val bundle = bundleOf("scheduleId" to schedule.id)
            findNavController().navigate(
                R.id.action_homeFragment_to_scheduleDetailsFragment,
                bundle
            )
        }
        binding.rvSchedules.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HomeFragment.adapter
        }
        
        // FAB click listener
        binding.fabAddSchedule.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_createScheduleFragment)
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadSchedules() {
        val today = try { 
            LocalDate.now().toString() 
        } catch (e: Exception) { 
            "2025-11-19" 
        }
        Log.d("HomeFragment", "Loading schedules for: $today")
        viewModel.getScheduleByDay(today)
    }
    
    private fun setupObservers() {
        viewModel.schedules.observe(viewLifecycleOwner) { schedules ->
            if (!schedules.isNullOrEmpty()) {
                adapter.submitList(schedules)
                binding.rvSchedules.visibility = View.VISIBLE
                binding.tvEmpty.visibility = View.GONE
                Log.d("HomeFragment", "Showing ${schedules.size} schedules")
            } else {
                adapter.submitList(emptyList())
                binding.rvSchedules.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
                Log.d("HomeFragment", "No schedules to show")
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                Log.e("HomeFragment", "Error: $it")
                viewModel.clearError()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}