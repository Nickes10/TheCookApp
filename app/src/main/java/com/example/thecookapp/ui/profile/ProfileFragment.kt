package com.example.thecookapp.ui.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections
import com.example.thecookapp.EditProfileActivity
import com.example.thecookapp.Adapter.ProfilePostAdapter
import com.example.thecookapp.Adapter.UserAdapter
import com.example.thecookapp.AppObject.User
import com.example.thecookapp.FirebaseUtils
import com.example.thecookapp.MainActivity
import com.example.thecookapp.R
import com.example.thecookapp.R.id.*
import com.example.thecookapp.Recipe
import com.example.thecookapp.SignInActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {

    private lateinit var viewedProfileId: String // the Id of the profile displayed
    private lateinit var signInUser: FirebaseUser // the user of Firebase SignIn in this moment (the one that is using the app)
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    // Declaration of Posts Variables
    private lateinit var profilePostAdapter:ProfilePostAdapter
    private lateinit var profilePostRecyclerView: RecyclerView
    private val profilePostList = mutableListOf<Recipe>()

    // Declaration of Following Variables
    private var followingAdapter: UserAdapter? = null
    private lateinit var followingRecyclerView: RecyclerView
    private var listFollowing: MutableList<User>? = null

    // Declaration of Followers Variables
    private var followersAdapter: UserAdapter? = null
    private lateinit var followersRecyclerView: RecyclerView
    private var listFollowers: MutableList<User>? = null

    private lateinit var postDetailsLauncher: ActivityResultLauncher<Intent>



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Use ViewBinding to inflate the layout
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        postDetailsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val postDeleted = result.data?.getBooleanExtra("post_deleted", false) ?: false
                if (postDeleted) {
                    refreshPosts() // Refresh the list when a post is deleted
                }
            }
        }

        // Initialize Google Sign-In client
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        // Initialize Post RecyclerViews
        profilePostRecyclerView = view.findViewById(recycler_view_posts)
        val gridLayoutManager:LinearLayoutManager= GridLayoutManager(context,3)
        profilePostRecyclerView.layoutManager = gridLayoutManager

        profilePostAdapter = context?.let { ProfilePostAdapter(it, profilePostList, postDetailsLauncher) }!!
        profilePostRecyclerView.adapter = profilePostAdapter

        // Initialize Following RecyclerViews
        followingRecyclerView = view.findViewById(recycler_view_following)
        followingRecyclerView.setHasFixedSize(true)
        followingRecyclerView.layoutManager = LinearLayoutManager(context)

        listFollowing = ArrayList()
        followingAdapter = context?.let { UserAdapter(it, listFollowing as ArrayList<User>, true) }
        followingRecyclerView.adapter = followingAdapter

        // Initialize Followers RecyclerViews
        followersRecyclerView = view.findViewById(recycler_view_followers)
        followersRecyclerView.setHasFixedSize(true)
        followersRecyclerView.layoutManager = LinearLayoutManager(context)

        listFollowers = ArrayList()
        followersAdapter = context?.let { UserAdapter(it, listFollowers as ArrayList<User>, true) }
        followersRecyclerView.adapter = followersAdapter

        val logout = view.findViewById<Button>(logout_button)
        val editProfileButton = view.findViewById<Button>(edit_profile_button)

        // Get the current authenticated user
        signInUser = FirebaseAuth.getInstance().currentUser!!

        signInUser?.let {
            val sharedPreferences = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)
            // Retrieve the profileId from SharedPreferences
            viewedProfileId = sharedPreferences?.getString("profileId", "none") ?: "none"

            Log.e("Profile Adapter", "ProfileId: $viewedProfileId, and the sharedPreferenzes: $sharedPreferences")

            // Check if the viewed profile belongs to the current user
            if (viewedProfileId == signInUser.uid) {
                updateProfileButtonText("Edit Profile")
                logout.visibility = View.VISIBLE
            } else if (viewedProfileId != signInUser.uid){
                FirebaseUtils.checkFollowingStatus(requireContext(), signInUser.uid, viewedProfileId, editProfileButton)
                logout.visibility = View.GONE
            }
        }


        logout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            mGoogleSignInClient.revokeAccess().addOnCompleteListener(requireActivity()) {
                mGoogleSignInClient.signOut().addOnCompleteListener(requireActivity()) {
                    Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(requireContext(), SignInActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }
        }

        editProfileButton.setOnClickListener{
            val getButtonText = editProfileButton.text.toString()
            Log.d("ProfileFragment", "Button text: $getButtonText")

            if (getButtonText == "Edit Profile") {
                startActivity(Intent(context, EditProfileActivity::class.java))
            } else {
                FirebaseUtils.handleFollowButtonClick(requireContext(), signInUser.uid, viewedProfileId, view.findViewById(edit_profile_button))
            }
        }

        // Fill the data necessary in the profile
        getUserInfo(view)
        getFollowers()
        getFollowing()
        takePosts(viewedProfileId)

        // Initialize TabLayout
        val tabLayout = view.findViewById<TabLayout>(tabs_profile_sections)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // Posts tab
                        loadPosts()
                    }
                    1 -> { // Following tab
                        loadFollowing()
                    }
                    2 -> { // Followers tab
                        loadFollowers()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        return  view
    }

    private fun refreshPosts() {
        takePosts(viewedProfileId)
    }

    private fun loadPosts() {
        profilePostRecyclerView.visibility = View.VISIBLE
        followingRecyclerView.visibility = View.GONE
        followersRecyclerView.visibility = View.GONE

        // Fill the profilePostRecyclerView
        takePosts(viewedProfileId)
    }

    private fun loadFollowing() {
        profilePostRecyclerView.visibility = View.GONE
        followingRecyclerView.visibility = View.VISIBLE
        followersRecyclerView.visibility = View.GONE

        Log.e("ProfileFragment", "We are in loadFollowing and the profile Id is $viewedProfileId ")

        // Fetch following data if not already loaded
        //if (listFollowing.isNullOrEmpty()) {

            Log.e("ProfileFragment", "the profile Id is $viewedProfileId")
            val followingRef = FirebaseDatabase.getInstance().reference
                .child("Follow").child(viewedProfileId).child("Following")

            followingRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val totalChildren = snapshot.childrenCount.toInt()
                    var processedCount = 0
                    listFollowing?.clear()
                    for (child in snapshot.children) {
                        val userId = child.key ?: continue
                        fetchUserDetails(userId, listFollowing!!) {
                            processedCount++
                            if (processedCount == totalChildren) {
                                followingAdapter?.notifyDataSetChanged()
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        //}

    }

    private fun loadFollowers() {
        profilePostRecyclerView.visibility = View.GONE
        followingRecyclerView.visibility = View.GONE
        followersRecyclerView.visibility = View.VISIBLE

        Log.e("ProfileFragment", "We are in loadFollowers and the profile Id is $viewedProfileId ")
        // Fetch following data if not already loaded
        //if (listFollowers.isNullOrEmpty()) {
            val followersRef = FirebaseDatabase.getInstance().reference
                .child("Follow").child(viewedProfileId).child("Followers")

            followersRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val totalChildren = snapshot.childrenCount.toInt()
                    var processedCount = 0
                    listFollowers?.clear()
                    for (child in snapshot.children) {
                        val userId = child.key ?: continue
                        fetchUserDetails(userId, listFollowers!!) {
                            processedCount++
                            if (processedCount == totalChildren) {
                                followersAdapter?.notifyDataSetChanged()
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        //}

    }

    private fun fetchUserDetails(userId: String, list: MutableList<User>, onComplete: () -> Unit) {
        // Fetch user details from the database in the following/followers list
        val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    list.add(user)
                }
                // Add one to processedCount to make sure all users fetched before calling update Adpater
                onComplete()
            }

            override fun onCancelled(error: DatabaseError) {
                onComplete()
            }
        })
    }

    private fun updateProfileButtonText(text: String) {
        view?.findViewById<Button>(R.id.edit_profile_button)?.text = text
    }

    private fun updateFollowCount(followType: String) {
        val followRef = FirebaseDatabase.getInstance().reference
            .child("Follow").child(viewedProfileId)
            .child(followType)

        Log.e("ProfileFragment", "Sono nel updateFollowCount con followType $followType")

        followRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                // Handle error if needed
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                Log.e("ProfileFragment", "Sono entrato onDatachange e lo snapshot è $snapshot e esiste: ${snapshot.exists()}")

                val tabLayout = view?.findViewById<TabLayout>(tabs_profile_sections)
                Log.e("ProfileFragment", "Sono nel if di onDatachange, il followType è $followType e il snapchot.ChildrenCount è ${snapshot.childrenCount}")

                // Determine which tab to update based on followType
                when (followType) {
                    "Followers" -> {
                        // Update the Followers tab
                        tabLayout?.getTabAt(2)?.text = "${snapshot.childrenCount} Followers"
                    }
                    "Following" -> {
                        // Update the Following tab
                        tabLayout?.getTabAt(1)?.text = "${snapshot.childrenCount} Following"
                    }
                }

            }
        })
    }

    private fun getUserInfo(view: View) {
        // Function for set all the info in the user Profile
        val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(viewedProfileId)

        Log.e("ProfileFragment", "UserRef: $userRef")

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.exists()) {
                    val user = snapshot.getValue<User>(User::class.java)
                    Log.e("ProfileFragment", "User: $user, and the username is ${user?.getUsername()} and the fullname is ${user?.getFullname()}")

                    val imageProfile = view.findViewById<CircleImageView>(profile_photo)
                    Log.e("AccountSetting", "Image URL: ${user!!.getImage()}")
                    Picasso.get()
                        .load(user!!.getImage())
                        .placeholder(R.drawable.default_image_profile)
                        .into(imageProfile)

                    view.findViewById<TextView>(username).text = user.getUsername()
                    view.findViewById<TextView>(user_fullname).text = user.getFullname()
                    view.findViewById<TextView>(bio).text = user.getBio()
                }
            }
        })
    }

    private fun getFollowers() {
        // to take the number of Followers of the profile
        updateFollowCount("Followers")
    }

    private fun getFollowing() {
        // to take the number of Following of the profile
        updateFollowCount("Following")
    }

    private fun takePosts(userId: String) {
        ApiClient.recipeApi.get_post(userId).enqueue(object : Callback<List<Recipe>> {
            override fun onResponse(call: Call<List<Recipe>>, response: Response<List<Recipe>>) {
                if (response.isSuccessful) {
                    val posts = response.body()
                    if (posts != null) {
                        // Clear the current postList and add the fetched posts
                        (profilePostList as ArrayList<Recipe>).clear()
                        (profilePostList as ArrayList<Recipe>).addAll(posts)

                        // Reverse the list to show the most recent posts first
                        Collections.reverse(profilePostList)

                        // Update Post Number
                        val tabLayout = view?.findViewById<TabLayout>(tabs_profile_sections)
                        tabLayout?.getTabAt(0)?.text = "${profilePostList.size} Posts"
                        Log.e("ProfileFragment", "Posts Number: ${profilePostList.size}")

                        // Notify the adapter of data changes
                        profilePostAdapter?.notifyDataSetChanged()
                    }
                } else {
                    // Handle the case where no posts are found
                    Log.e("ProfileFragment", "Error: ${response.errorBody()?.string()}")
                    (profilePostList as ArrayList<Recipe>).clear() // Clear the list
                    val tabLayout = view?.findViewById<TabLayout>(tabs_profile_sections)
                    tabLayout?.getTabAt(0)?.text = "0 Posts"
                    // Notify the adapter of data changes
                    profilePostAdapter?.notifyDataSetChanged()
                }
            }

            override fun onFailure(call: Call<List<Recipe>>, t: Throwable) {
                Log.e("ProfileFragment", "Failure: ${t.message}")
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun updateProfile(newProfileId: String) {
        // Function to reuse the same ProfileFragment in the case we are in the PostDetailsActivity
        if (viewedProfileId != newProfileId) {
            viewedProfileId = newProfileId
            Log.e("ProfileFragment", "Profile updated to new profileId: $newProfileId and viewedProfileId: $viewedProfileId")

            // Update the informations of the profile and all the other variables
            getUserInfo(requireView())
            getFollowers()
            getFollowing()
            takePosts(newProfileId)
            loadFollowers()
            loadFollowing()

            // Get the TabLayout and select the first tab (position 0)
            val tabLayout = view?.findViewById<TabLayout>(R.id.tabs_profile_sections)
            tabLayout?.selectTab(tabLayout.getTabAt(0))

            tabLayout?.clearOnTabSelectedListeners()
            tabLayout?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    when (tab?.position) {
                        0 -> { // Posts tab
                            loadPosts()
                        }
                        1 -> { // Following tab
                            loadFollowing()
                        }
                        2 -> { // Followers tab
                            loadFollowers()
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })

            val currentUser = FirebaseAuth.getInstance().currentUser!!
            val editProfileButton = view?.findViewById<Button>(edit_profile_button)

            // Check if the viewed profile belongs to the current user
            if (viewedProfileId == currentUser.uid) {
                updateProfileButtonText("Edit Profile")
            } else if (viewedProfileId != signInUser.uid){
                FirebaseUtils.checkFollowingStatus(requireContext(), signInUser.uid, viewedProfileId, editProfileButton!!)
            }

            Log.e("ProfileFragment", "Profile updated to new profileId: $newProfileId")
        }
    }


    override fun onStop() {
        // Function called when Fragment is no longer visible to the user
        super.onStop()

        val sharedPreferences = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)
        val actualProfileId = sharedPreferences?.getString("profileId", "none") ?: "none"

        Log.e("Profile Fragment", "onStop called and actualProfileId is $actualProfileId and viewedProfileId is $viewedProfileId")

        if (actualProfileId == "none" || actualProfileId == viewedProfileId) {
            val pref = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)?.edit()
            pref?.putString("profileId", signInUser.uid)
            pref?.apply()
        }
    }

    override fun onPause() {
        // Function called when we change fragment
        super.onPause()
        val sharedPreferences = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)
        val actualProfileId = sharedPreferences?.getString("profileId", "none") ?: "none"

        Log.e("Profile Fragment", "onPause called and actualProfileId is $actualProfileId and viewedProfileId is $viewedProfileId")

        if (actualProfileId == "none" || actualProfileId == viewedProfileId) {
            val pref = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)?.edit()
            pref?.putString("profileId", signInUser.uid)
            pref?.apply()
        }
    }

    override fun onDestroy() {
        // Function called when the Fragment is destroyed
        super.onDestroy()
        val sharedPreferences = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)
        val actualProfileId = sharedPreferences?.getString("profileId", "none") ?: "none"

        Log.e("Profile Fragment", "onDestroy called and actualProfileId is $actualProfileId and viewedProfileId is $viewedProfileId")

        if (actualProfileId == "none" || actualProfileId == viewedProfileId) {
            // When the ProfileFragment is actually destroyed and there is not a change with another User Profile
            val pref = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)?.edit()
            pref?.putString("profileId", signInUser.uid)
            pref?.apply()
        }
    }

}


