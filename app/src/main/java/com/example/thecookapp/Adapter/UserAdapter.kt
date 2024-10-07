package com.example.thecookapp.Adapter

import com.example.thecookapp.AppObject.User
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.example.thecookapp.R
import com.example.thecookapp.R.id.*
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class UserAdapter (private var mContext: Context,
                   private var mUser: List<User>,
                   private var isFragment: Boolean = false): RecyclerView.Adapter<UserAdapter.ViewHolder>()
{
    class ViewHolder (@NonNull itemView: View): RecyclerView.ViewHolder(itemView){
        var username: TextView = itemView.findViewById(searched_user_username)
        var fullname: TextView = itemView.findViewById(searched_user_full_name)
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
    }


}