package com.example.thecookapp.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thecookapp.Adapter.PostAdapter
import com.example.thecookapp.R
import com.example.thecookapp.R.layout.fragment_home
import com.example.thecookapp.Recipe
import com.example.thecookapp.databinding.FragmentHomeBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var postAdapter: PostAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Use ViewBinding to inflate the layout
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        recyclerView = binding.recyclerViewPosts
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val userId = "8yUGPtbsxrRFcsdbHGHjyRy76iv1"  // Update with the actual logged-in user's ID

        // Fetch posts
        val adapter = PostAdapter(emptyList())

        recyclerView.adapter = adapter

        fetchPosts(userId)

        return root
    }

    private fun fetchPosts(userId: String) {
        ApiClient.recipeApi.get_post(userId).enqueue(object : Callback<List<Recipe>> {
            override fun onResponse(call: Call<List<Recipe>>, response: Response<List<Recipe>>) {
                if (response.isSuccessful) {
                    val posts = response.body()
                    if (posts != null) {
                        postAdapter = PostAdapter(posts)
                        recyclerView.adapter = postAdapter
                    }
                } else {
                    Log.e("HomeFragment", "Error: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Recipe>>, t: Throwable) {
                Log.e("HomeFragment", "Failure: ${t.message}")
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
