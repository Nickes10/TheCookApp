package com.example.thecookapp.ui.search

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thecookapp.Adapter.ProfilePostAdapter
import com.example.thecookapp.Adapter.UserAdapter
import com.example.thecookapp.AppObject.User
import com.example.thecookapp.R
import com.example.thecookapp.R.id.*
import com.example.thecookapp.Recipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import retrofit2.Call
import retrofit2.Response
import retrofit2.Callback

class SearchBarFragment : Fragment() {

    private val database = FirebaseDatabase.getInstance().reference
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid // Get the UID of the current user (the one doing the search)

    // Variable for the Search Bar
    private var searchRecyclerView: RecyclerView? = null
    private var userAdapter: UserAdapter? = null
    private var listUsers: MutableList<User>? = null

    // Variable for the Top 3 Chefs
    private var topChefsRecyclerView: RecyclerView? = null
    private var topChefsAdapter: UserAdapter? = null
    private var topChefsList: MutableList<User>? = null

    // Variable for the Trending Recipes
    private var recipesRecyclerView: RecyclerView? = null
    private var recipesAdapter: ProfilePostAdapter? = null
    private var recipesList: MutableList<Recipe>? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialize the binding object
        // Use ViewBinding to inflate the layout
        val view = inflater.inflate(R.layout.fragment_search_bar, container, false)
        
        // Implementation of the logic for the search Bar
        searchRecyclerView = view.findViewById(search_bar_recycler_view)
        searchRecyclerView?.setHasFixedSize(true)
        searchRecyclerView?.layoutManager = LinearLayoutManager(context)

        listUsers= ArrayList()
        userAdapter = context?.let{ UserAdapter(it, listUsers as ArrayList<User>, true)}
        searchRecyclerView?.adapter = userAdapter

        // Implementation of the logic for the Top 3 Chefs
        topChefsRecyclerView = view.findViewById(top_chefs_recycler_view)
        topChefsRecyclerView?.setHasFixedSize(true)
        topChefsRecyclerView?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        topChefsList= ArrayList()
        topChefsAdapter = context?.let{ UserAdapter(it, topChefsList as ArrayList<User>, false)}
        topChefsRecyclerView?.adapter = topChefsAdapter

        // Implementation of the logic for the Treding Recipes
        recipesRecyclerView = view.findViewById(trending_recipes_grid)
        recipesRecyclerView?.setHasFixedSize(true)
        val gridLayoutManager:LinearLayoutManager= GridLayoutManager(context,3)
        recipesRecyclerView?.layoutManager = gridLayoutManager

        recipesList= ArrayList()
        recipesAdapter = context.let{ ProfilePostAdapter(it!!, recipesList as ArrayList<Recipe>) }
        recipesRecyclerView?.adapter = recipesAdapter

        val querySearch: EditText = view.findViewById(search_input)

        querySearch.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (querySearch.text.toString() != "") {
                    searchRecyclerView?.visibility = View.VISIBLE
                    view.findViewById<TextView>(R.id.top_chefs_title).visibility = View.GONE
                    topChefsRecyclerView?.visibility = View.GONE

                    view.findViewById<TextView>(R.id.trending_recipes_title).visibility = View.GONE
                    recipesRecyclerView?.visibility = View.GONE

                    searchUser(s.toString().lowercase())
                }
                else{
                    // When user remove what searched for, all users in database displayed
                    getAllUsers()
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        // To fill the recyclerView with the top 3 chefs and the trending recipes recyclerView
        getTopThreeChefs()
        fetchGlobalPosts()

        return  view
    }


    private fun searchUser(input:String) {
        // Function to search users in database based on input String
        val query=FirebaseDatabase.getInstance().reference
            .child("Users")
            .orderByChild("fullname")
            .startAt(input)
            .endAt(input + "\uf8ff")

        query.addValueEventListener(object:ValueEventListener
        {
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context,"Could not read from Database",Toast.LENGTH_LONG).show()

            }
            override fun onDataChange(datasnapshot: DataSnapshot) {
                listUsers?.clear()
                for(snapshot in datasnapshot.children)
                {
                    //searching all users
                    val user=snapshot.getValue(User::class.java)
                    if(user!=null && checkNotCurrentUser(user))
                    {
                        listUsers?.add(user)
                    }
                }
                userAdapter?.notifyDataSetChanged()
            }
        })
    }


    private fun getAllUsers(){
        // Function to save all the users in the Database in the Array<User>
        val usersDatabaseRef= FirebaseDatabase.getInstance().reference.child("Users")

        usersDatabaseRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                listUsers?.clear()

                // Cycles on all registered users
                for (snapShot in dataSnapshot.children) {
                    // Convert Each Firebase DataSnapshot into a User Object
                    val user = snapShot.getValue(User::class.java)
                    if (user != null && checkNotCurrentUser(user)) {
                        listUsers?.add(user)
                    }
                }
                // Telling to RecyclerView adapter that the data has changed, so to refresh the display.
                userAdapter?.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context,"Could not read from Database",Toast.LENGTH_LONG).show()
            }

        })
    }

    private fun getTopThreeChefs() {
        // Function to get the first three user based on number of Followers
        val usersDatabaseRef = FirebaseDatabase.getInstance().reference.child("Users")
        val followDatabaseRef = FirebaseDatabase.getInstance().reference.child("Follow")
        val userFollowersCount = HashMap<String, Int>()
        val allUsers = mutableListOf<User>()

        usersDatabaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                for (snapShot in dataSnapshot.children) {
                    val user = snapShot.getValue(User::class.java)
                    if (user != null) {
                        allUsers.add(user)
                        userFollowersCount[user.getUid()] = 0 // Initialize count to 0
                    }
                }

                // Fetch follower counts
                for (user in allUsers) {
                    followDatabaseRef.child(user.getUid()).child("Followers")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                userFollowersCount[user.getUid()] = snapshot.childrenCount.toInt()

                                // Check if we have counted all users
                                if (userFollowersCount.size == allUsers.size) {
                                    updateTopThreeChefs(allUsers, userFollowersCount)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Could not read from Database", Toast.LENGTH_LONG).show()
            }
        })
        
    }

    private fun updateTopThreeChefs(users: List<User>, followersCount: HashMap<String, Int>) {
        topChefsList?.clear()

        // Sort users by followers count (Descending order)
        val sortedUsers = users.sortedByDescending { followersCount[it.getUid()] ?: 0 }

        // Get top 3 users
        topChefsList?.addAll(sortedUsers.take(3))

        // Notify RecyclerView adapter
        topChefsAdapter?.notifyDataSetChanged()
    }

    // Fetch all posts globally (from all users)
    private fun fetchGlobalPosts() {
        // Return a list of all userid in the Firebase database
        database.child("Users").get()
            .addOnSuccessListener { usersSnapshot ->
                val allUsers = usersSnapshot.children.mapNotNull { it.key }
                fetchPostsForUsers(allUsers)
            }
            .addOnFailureListener { error ->
                Log.e("HomeFragment", "Failed to fetch users: ${error.message}")
                Toast.makeText(requireContext(), "Failed to load users list", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchPostsForUsers(userIds: List<String>) {
        // Fetch posts for each user
        val allPosts = mutableListOf<Recipe>()
        var fetchedUsersCount = 0

        for (userId in userIds) {
            ApiClient.recipeApi.get_post(userId).enqueue(object : Callback<List<Recipe>> {
                override fun onResponse(call: Call<List<Recipe>>, response: Response<List<Recipe>>) {
                    if (response.isSuccessful) {
                        allPosts.addAll(response.body() ?: emptyList())
                    } else {
                        Log.e("HomeFragment", "Error fetching posts for user $userId: ${response.errorBody()?.string()}")
                    }

                    fetchedUsersCount++
                    if (fetchedUsersCount == userIds.size) {
                        fetchLikesForPosts(allPosts)
                    }
                }

                override fun onFailure(call: Call<List<Recipe>>, t: Throwable) {
                    Log.e("HomeFragment", "Failed to fetch posts for user $userId: ${t.message}")
                    fetchedUsersCount++
                    if (fetchedUsersCount == userIds.size) {
                        fetchLikesForPosts(allPosts)
                    }
                }
            })
        }
    }

    private fun fetchLikesForPosts(posts: List<Recipe>) {
        // Reorder the post list by number of likes
        val postsWithLikes = mutableMapOf<Recipe, Int>()
        var processedPosts = 0

        if (posts.isEmpty()) {
            updateRecyclerView(postsWithLikes.keys.toList())
            return
        }

        for (post in posts) {
            database.child("Likes").child(post.user_id).child(post.post_id.toString()).get().addOnSuccessListener { snapshot ->
                postsWithLikes[post] = snapshot.childrenCount.toInt()
                processedPosts++

                if (processedPosts == posts.size) {
                    // Sort posts by number of likes in descending order
                    val sortedPosts = postsWithLikes.toList().sortedByDescending { it.second }.map { it.first }
                    updateRecyclerView(sortedPosts)
                }
            }.addOnFailureListener { e ->
                Log.e("HomeFragment", "Error fetching likes for post ${post.post_id}: ${e.message}")
                processedPosts++
                if (processedPosts == posts.size) {
                    val sortedPosts = postsWithLikes.toList().sortedByDescending { it.second }.map { it.first }
                    updateRecyclerView(sortedPosts)
                }
            }
        }
    }


    private fun updateRecyclerView(sortedPosts: List<Recipe>) {
        recipesList?.clear()
        recipesList?.addAll(sortedPosts)
        recipesAdapter?.notifyDataSetChanged()
    }

    private fun checkNotCurrentUser(user: User): Boolean {
        return user.getUid() != currentUserId
    }
}