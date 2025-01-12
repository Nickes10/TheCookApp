package com.example.thecookapp.ui.home

import android.content.Intent
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
import com.example.thecookapp.Adapter.PostPreviewAdapter
import com.example.thecookapp.PostDetailsActivity
import com.example.thecookapp.R
import com.example.thecookapp.R.layout.fragment_home
import com.example.thecookapp.Recipe
import com.example.thecookapp.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var postPreviewAdapter: PostPreviewAdapter
    private lateinit var recyclerView: RecyclerView

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        recyclerView = binding.recyclerViewPosts
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        postPreviewAdapter = PostPreviewAdapter(requireContext(), emptyList()) { post ->
            openFullPost(post)
        }
        recyclerView.adapter = postPreviewAdapter

        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            fetchFollowingPosts(userId)
        } else {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
        }

        return root
    }

    private fun fetchFollowingPosts(userId: String) {
        database.child("Follow").child(userId).child("Following").get().addOnSuccessListener { snapshot ->
            val followingList = snapshot.children.mapNotNull { it.key }
            fetchPostsForUsers(followingList)
        }.addOnFailureListener {
            Log.e("HomeFragment", "Failed to fetch following list: ${it.message}")
            Toast.makeText(requireContext(), "Failed to load following list", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchPostsForUsers(userIds: List<String>) {
        val allPosts = mutableListOf<Recipe>()
        var fetchedUsersCount = 0

        for (userId in userIds) {
            ApiClient.recipeApi.get_post(userId).enqueue(object : Callback<List<Recipe>> {
                override fun onResponse(call: Call<List<Recipe>>, response: Response<List<Recipe>>) {
                    if (response.isSuccessful) {
                        val userPosts = response.body() ?: emptyList()
                        allPosts.addAll(userPosts)
                    } else {
                        Log.e("HomeFragment", "Error fetching posts for user $userId: ${response.errorBody()?.string()}")
                    }

                    fetchedUsersCount++
                    if (fetchedUsersCount == userIds.size) {
                        updateRecyclerView(allPosts)
                    }
                }

                override fun onFailure(call: Call<List<Recipe>>, t: Throwable) {
                    Log.e("HomeFragment", "Failed to fetch posts for user $userId: ${t.message}")

                    fetchedUsersCount++
                    if (fetchedUsersCount == userIds.size) {
                        updateRecyclerView(allPosts)
                    }
                }
            })
        }
    }

    private fun updateRecyclerView(posts: List<Recipe>) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val sortedPosts = posts.sortedByDescending { post ->
            post.created_at?.let {
                dateFormat.parse(it)?.time
            } ?: 0L
        }
        postPreviewAdapter = PostPreviewAdapter(requireContext(), sortedPosts) { post ->
            openFullPost(post)
        }
        recyclerView.adapter = postPreviewAdapter
    }

    private fun openFullPost(post: Recipe) {
        val intent = Intent(context, PostDetailsActivity::class.java)
        intent.putExtra("POST_DETAILS", post)
        requireContext().startActivity(intent)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
