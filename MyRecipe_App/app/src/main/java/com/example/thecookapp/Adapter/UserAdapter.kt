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
import com.example.thecookapp.FirebaseUtils
import com.example.thecookapp.PostDetailsActivity
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
                   private var isProfile: Boolean = true): RecyclerView.Adapter<UserAdapter.ViewHolder>()
{
    private var firebaseUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser

    class ViewHolder (@NonNull itemView: View): RecyclerView.ViewHolder(itemView){
        var username: TextView = itemView.findViewById(searched_user_username)
        var fullname: TextView = itemView.findViewById(searched_user_full_name)
        var item: RelativeLayout = itemView.findViewById(searched_user_layout)
        var rank_number: TextView = itemView.findViewById(classification_rank_number)
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

        holder.username.text = "@${user.getUsername().trim()}"
        holder.fullname.text = user.getFullname()
        // For return the image
        Picasso.get().load(user.getImage()).placeholder(R.drawable.default_image_profile).into(holder.profileimage)

        if(!isProfile) {
            Log.e("UserAdapter", "isProfile is false")
            holder.rank_number.visibility = View.VISIBLE
            holder.rank_number.text = (position + 1).toString()
        } else {
            holder.rank_number.visibility = View.GONE
        }

        // If user clicks everywhere on the user item
        holder.item.setOnClickListener(View.OnClickListener {
            Log.e("Profile Adapter", "User item clicked: ${user.getUsername()}, and ${user.getUid()}")

            val preference = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
            preference.putString("profileId", user.getUid())
            preference.apply()

            if (mContext is PostDetailsActivity) {
                Log.e("Profile Adapter", "PostDetailsActivity found")
                // Verify if the activity is PostDetailsActivity
                val fragmentManager = (mContext as FragmentActivity).supportFragmentManager
                val existingFragment = fragmentManager.findFragmentByTag("ProfileFragment")
                Log.e("Profile Adapter", "ExistingFragment: $existingFragment")

                if (existingFragment is ProfileFragment) {
                    Log.e("Profile Adapter", "ProfileFragment found")
                    // If exist a ProfileFragment, update the profile
                    existingFragment.updateProfile(user.getUid())
                } else {
                    // Otherwise, create a new ProfileFragment
                    fragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ProfileFragment(), "ProfileFragment")
                        .commit()
                }
            } else {
                // Behaviour for the other activity
                (mContext as FragmentActivity).supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ProfileFragment())
                    .commit()
            }
        })

        if (user.getUid() == firebaseUser?.uid || !isProfile) {
            // Hide the follow button
            holder.followButton.visibility = View.GONE
        } else{
            // Show the follow button
            FirebaseUtils.checkFollowingStatus(holder.itemView.context, firebaseUser?.uid ?: return, user.getUid(), holder.followButton)
            holder.followButton.visibility = View.VISIBLE
        }

        // If user clicks on the follow button
        holder.followButton.setOnClickListener {
            FirebaseUtils.handleFollowButtonClick(holder.itemView.context, firebaseUser?.uid ?: return@setOnClickListener, user.getUid(), holder.followButton)
        }
    }
}