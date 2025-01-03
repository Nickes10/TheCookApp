package com.example.thecookapp.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.thecookapp.AccountSettingsActivity
import com.example.thecookapp.AppObject.User
import com.example.thecookapp.R
import com.example.thecookapp.R.id.*
import com.example.thecookapp.databinding.FragmentProfileBinding
import com.google.android.material.tabs.TabItem
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class ProfileFragment : Fragment() {

    private lateinit var viewedProfileId: String // the Id of the profile displayed
    private lateinit var signInUser: FirebaseUser // the user of Firebase SignIn in this moment (the one that is using the app)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Use ViewBinding to inflate the layout
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Get the current authenticated user
        signInUser = FirebaseAuth.getInstance().currentUser!!

        signInUser?.let {
            val sharedPreferences = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)
            // Retrieve the profileId from SharedPreferences
            viewedProfileId = sharedPreferences?.getString("profileId", "none") ?: "none"

            // Check if the viewed profile belongs to the current user
            if (viewedProfileId == signInUser.uid) {
                updateProfileButtonText("Edit Profile")
            } else if (viewedProfileId != signInUser.uid){
                checkFollowOrFollowingButtonStatus()
            }
        }

        view.findViewById<Button>(R.id.edit_profile_button).setOnClickListener{
            val getButtontext = view.findViewById<Button>(R.id.edit_profile_button).text.toString()
            Log.d("ProfileFragment", "Button text: $getButtontext")

            when {
                getButtontext == "Edit Profile" -> startActivity(Intent(context, AccountSettingsActivity::class.java))


                getButtontext == "Follow" -> {
                    signInUser.uid.let { it1 ->
                        FirebaseDatabase.getInstance().reference
                            .child("Follow").child(it1.toString())
                            .child("Following").child(viewedProfileId)
                            .setValue(true)

                    }

                    signInUser.uid.let { it1 ->
                        FirebaseDatabase.getInstance().reference
                            .child("Follow").child(viewedProfileId)
                            .child("Followers").child(it1.toString())
                            .setValue(true)
                    }
                }

                getButtontext == "Following" -> {
                    signInUser.uid.let { it1 ->
                        FirebaseDatabase.getInstance().reference
                            .child("Follow").child(it1.toString())
                            .child("Following").child(viewedProfileId)
                            .removeValue()
                    }

                    signInUser.uid.let { it1 ->
                        FirebaseDatabase.getInstance().reference
                            .child("Follow").child(viewedProfileId)
                            .child("Followers").child(it1.toString())
                            .removeValue()
                    }
                }
            }
        }

        // Fill the data necessary in the profile
        getUserInfo(view)
        getFollowers()
        getFollowing()

        return  view
    }


    private fun updateProfileButtonText(text: String) {
        view?.findViewById<Button>(R.id.edit_profile_button)?.text = text
    }

    private fun checkFollowOrFollowingButtonStatus() {
        // To check if the current user follows or not the other uid user
        val followingRef = signInUser.uid.let {it1 ->
            FirebaseDatabase.getInstance().reference
                .child("Follow").child(it1.toString())
                .child("Following")
        }

        followingRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                // Check if the current user is following the viewed profile

                if (snapshot.child(viewedProfileId).exists()) {
                    updateProfileButtonText("Following")
                } else {
                    updateProfileButtonText("Follow")
                }
            }
        })
    }

    private fun updateFollowCount(followType: String) {
        val followRef = FirebaseDatabase.getInstance().reference
            .child("Follow").child(viewedProfileId)
            .child(followType)

        followRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                // Handle error if needed
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val tabLayout = view?.findViewById<TabLayout>(tabs_profile_sections)

                    // Determine which tab to update based on followType
                    when (followType) {
                        "Followers" -> {
                            // Update the Followers tab
                            tabLayout?.getTabAt(3)?.text = "${snapshot.childrenCount} Followers"
                        }
                        "Following" -> {
                            // Update the Following tab
                            tabLayout?.getTabAt(2)?.text = "${snapshot.childrenCount} Following"
                        }
                    }
                }
            }
        })
    }

    private fun getUserInfo(view: View) {
        // Function for set all the info in the user Profile
        val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(viewedProfileId)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.exists()) {
                    val user = snapshot.getValue<User>(User::class.java)

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


    override fun onStop() {
        // Function called when Fragment is no longer visible to the user
        super.onStop()

        val pref = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)?.edit()
        pref?.putString("profileId", signInUser.uid)
        pref?.apply()
    }

    override fun onPause() {
        // Function called when we change fragment
        super.onPause()

        val pref = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)?.edit()
        pref?.putString("profileId", signInUser.uid)
        pref?.apply()
    }

    override fun onDestroy() {
        // Function called when the Fragment is destroyed
        super.onDestroy()

        val pref = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)?.edit()
        pref?.putString("profileId", signInUser.uid)
        pref?.apply()
    }
}


