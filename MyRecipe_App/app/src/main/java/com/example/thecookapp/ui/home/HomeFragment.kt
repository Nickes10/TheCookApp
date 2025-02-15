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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var postPreviewAdapter: PostPreviewAdapter
    private lateinit var recyclerView: RecyclerView

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    private var isGlobalSelected = true
    private var isRefreshing = false

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

        postPreviewAdapter = PostPreviewAdapter(requireContext(), emptyList()) { post ->
            openFullPost(post)
        }
        recyclerView.adapter = postPreviewAdapter

        setupTabListeners()

        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            fetchGlobalPosts(userId)
        } else {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshPosts()
        }

        return root
    }

    private fun refreshPosts() {
        if (isRefreshing) return

        isRefreshing = true
        binding.swipeRefreshLayout.isRefreshing = true

        // Trigger the refresh, re-fetching the posts from the server
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            if(isGlobalSelected){
                fetchGlobalPosts(userId)
            } else {
                fetchFollowingPosts(userId)
            }
        } else {
            binding.swipeRefreshLayout.isRefreshing = false
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            isRefreshing = false
        }
    }

    private fun setupTabListeners() {

        binding.globalTab.setOnClickListener {
            isGlobalSelected = true
            val userId = firebaseAuth.currentUser?.uid
            if (userId != null) {
                fetchGlobalPosts(userId)
            }
            updateTabSelection()
        }

        binding.followingTab.setOnClickListener {
            isGlobalSelected = false
            val userId = firebaseAuth.currentUser?.uid
            if (userId != null) {
                fetchFollowingPosts(userId)
            }
            updateTabSelection()
        }
    }

    private fun updateTabSelection() {
        binding.globalTab.setTextColor(
            if (isGlobalSelected) resources.getColor(R.color.colorApp, null) else resources.getColor(R.color.black, null)
        )
        binding.followingTab.setTextColor(
            if (!isGlobalSelected) resources.getColor(R.color.colorApp, null) else resources.getColor(R.color.black, null)
        )
    }


    private fun fetchGlobalPosts(userId: String) {
        // Fetch posts from all users
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val followingList = withContext(Dispatchers.IO) {
                    val snapshot = database.child("Follow").child(userId).child("Following").get().await()
                    snapshot.children.mapNotNull { it.key }.toSet()
                }

                val globalUsers = withContext(Dispatchers.IO) {
                    val usersSnapshot = database.child("Users").get().await()
                    usersSnapshot.children.mapNotNull { it.key }
                        .filterNot { it in followingList || it == userId }
                }

                fetchPostsForUsers(globalUsers)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("HomeFragment", "Failed to fetch data: ${e.message}")
                    Toast.makeText(requireContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchFollowingPosts(userId: String) {
        // Fetch posts from the users the user is following
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val followingList = withContext(Dispatchers.IO) {
                    val snapshot = database.child("Follow").child(userId).child("Following").get().await()
                    snapshot.children.mapNotNull { it.key }
                }
                fetchPostsForUsers(followingList)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("HomeFragment", "Error fetching following list: ${e.message}")
                    Toast.makeText(requireContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun fetchPostsForUsers(userIds: List<String>) {
        if (userIds.isEmpty()) {
            withContext(Dispatchers.Main) {
                val textView = view?.findViewById<TextView>(R.id.no_followed_users_message)
                textView?.visibility = View.VISIBLE
                textView?.text = "You don’t follow anyone, explore the global"
                updateRecyclerView(emptyList()) // Clear RecyclerView
            }
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val allPosts = mutableListOf<Recipe>()

            val deferredPosts = userIds.map { userId ->
                async {
                    try {
                        val response = ApiClient.recipeApi.get_post(userId).execute()
                        if (response.isSuccessful) {
                            response.body() ?: emptyList()
                        } else {
                            Log.e("HomeFragment", "Error fetching posts for $userId: ${response.errorBody()?.string()}")
                            emptyList()
                        }
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Failed to fetch posts for $userId: ${e.message}")
                        emptyList()
                    }
                }
            }

            val results = deferredPosts.awaitAll() // Wait for all API calls to finish
            allPosts.addAll(results.flatten())

            withContext(Dispatchers.Main) {
                updateRecyclerView(allPosts)
            }
        }
    }

    private fun updateRecyclerView(posts: List<Recipe>) {
        if (!isAdded || context == null) return // Ensure the fragment is attached before updating UI

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

        binding.swipeRefreshLayout.isRefreshing = false
        isRefreshing = false
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
