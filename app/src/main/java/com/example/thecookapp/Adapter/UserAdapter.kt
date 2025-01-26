package com.example.thecookapp.Adapter

import com.example.thecookapp.AppObject.User
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.thecookapp.R
import com.example.thecookapp.R.id.*
import com.example.thecookapp.ui.profile.ProfileFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class UserAdapter (private var mContext: Context,
                   private var mUser: List<User>,
                   private var isFragment: Boolean = false): RecyclerView.Adapter<UserAdapter.ViewHolder>()
{
    private var firebaseUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser

    class ViewHolder (@NonNull itemView: View): RecyclerView.ViewHolder(itemView){
        var username: TextView = itemView.findViewById(searched_user_username)
        var fullname: TextView = itemView.findViewById(searched_user_full_name)
        var item: RelativeLayout = itemView.findViewById(searched_user_layout)
        var profileimage: CircleImageView = itemView.findViewById(searched_user_profile_image)
        var followButton: Button = itemView.findViewById(searched_follow_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserAdapter.ViewHolder {
        // Inflate the user item layout to create a view for the user item
        val view = LayoutInflater.from(mContext).inflate(R.layout.searched_users, parent, false)

        return UserAdapter.ViewHolder(view)
    }

    override fun getItemCount(): Int {
        // Returns the total number of items to be displayed
        return mUser.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Actual data binding happens, every time a new item scrolls into view
        val user = mUser[position]

        holder.username.text = user.getUsername()
        holder.fullname.text = user.getFullname()
        // For return the image
        Picasso.get().load(user.getImage()).placeholder(R.drawable.default_image_profile).into(holder.profileimage)
        
        checkFollowingStatus(user.getUid(), holder.followButton)

        // If user clicks everywhere on the user item
        holder.item.setOnClickListener(View.OnClickListener {
            Log.e("Profile Adapter", "User item clicked: ${user.getUsername()}, and ${user.getUid()}")
            val preference = mContext.getSharedPreferences("PREFS",Context.MODE_PRIVATE).edit()
            preference.putString("profileId",user.getUid())
            preference.apply()
            val savedId = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).getString("profileId", null)
            Log.e("Profile Adapter", "Saved profileId in SharedPreferences: $savedId")

            (mContext as FragmentActivity).supportFragmentManager.beginTransaction()
                .replace(fragment_container, ProfileFragment()).commit()
        })

        if (user.getUid() == firebaseUser?.uid) {
            // Hide the follow button
            holder.followButton.visibility = View.GONE
        } else {
            // Show the follow button
            holder.followButton.visibility = View.VISIBLE
            checkFollowingStatus(user.getUid(), holder.followButton)
        }

        // If user clicks on the follow button
        holder.followButton.setOnClickListener {
            if (holder.followButton.text.toString() == "Follow")
            {
                firebaseUser?.uid.let { it ->
                    // Searched user added to Following of Current user
                    FirebaseDatabase.getInstance().reference
                        .child("Follow").child(it.toString())
                        .child("Following").child(user.getUid())
                        .setValue(true).addOnCompleteListener { task ->
                            if (task.isSuccessful)
                            {
                                // Current user added to Followers of Searched user
                                firebaseUser?.uid.let { it1 ->
                                    FirebaseDatabase.getInstance().reference
                                        .child("Follow").child(user.getUid())
                                        .child("Followers").child(it1.toString())
                                        .setValue(true).addOnCompleteListener { task ->
                                            if (task.isSuccessful)
                                            {

                                            }
                                        }
                                }
                            }
                        }
                }
            }
            else
            {
                if(holder.followButton.text.toString()=="Following")
                {
                    // Removing searched user from Following of Current user
                    firebaseUser?.uid.let { it1 ->
                        FirebaseDatabase.getInstance().reference
                            .child("Follow").child(it1.toString())
                            .child("Following").child(user.getUid())
                            .removeValue().addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Remove the current user from Followers of searched user
                                    firebaseUser?.uid.let { it1 ->
                                        FirebaseDatabase.getInstance().reference
                                            .child("Follow").child(user.getUid())
                                            .child("Followers").child(it1.toString())
                                            .removeValue().addOnCompleteListener { task ->
                                                if (task.isSuccessful) {

                                                }
                                            }
                                    }
                                }
                            }
                    }
                }
            }

        }
    }


    private fun checkFollowingStatus(uid: String, followButton: Button)
    // Function to check if the Searched is followed or not but by current user
    {
        val followingList = firebaseUser?.uid.let { it1 ->
            FirebaseDatabase.getInstance().reference
                .child("Follow").child(it1.toString())
                .child("Following")
        }

        followingList.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(datasnapshot: DataSnapshot) {
                if (datasnapshot.child(uid).exists()) {
                    followButton.text = "Following"
                }
                else {
                    followButton.text = "Follow"
                }
            }
        })

    }

}