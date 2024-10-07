package com.example.thecookapp.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.thecookapp.AccountSettingsActivity
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
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        view.findViewById<Button>(R.id.edit_profile_button).setOnClickListener{
            startActivity(Intent(context, AccountSettingsActivity::class.java))
        }
        return  view
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
