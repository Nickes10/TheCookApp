package com.example.thecookapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.thecookapp.R
import com.example.thecookapp.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize the binding object
        // Use ViewBinding to inflate the layout

        return  inflater.inflate(R.layout.fragment_profile, container, false)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
