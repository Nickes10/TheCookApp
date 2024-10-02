package com.example.thecookapp.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.thecookapp.R

class SearchBarFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialize the binding object
        // Use ViewBinding to inflate the layout
        val view = inflater.inflate(R.layout.fragment_search_bar, container, false)

        return  view
    }

}