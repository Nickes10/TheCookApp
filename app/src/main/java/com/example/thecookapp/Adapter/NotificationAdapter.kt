package com.example.thecookapp.Adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.thecookapp.AppObject.Notification
import com.example.thecookapp.AppObject.User
import com.example.thecookapp.FirebaseUtils
import com.example.thecookapp.R
import com.example.thecookapp.ui.profile.ProfileFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class NotificationAdapter(
    private var mContext: Context,
    private var listNotification:List<Notification>)
    : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    private var firebaseUser = FirebaseAuth.getInstance().currentUser

    class ViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        var username: TextView = itemView.findViewById(R.id.notification_username)
        var notifyText: TextView = itemView.findViewById(R.id.notification_text)
        var profileimage: CircleImageView = itemView.findViewById(R.id.notification_image_profile)
        var postimg: ImageView = itemView.findViewById(R.id.notification_posted_image)
        var followbutton: Button = itemView.findViewById(R.id.notification_follow_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view= LayoutInflater.from(mContext).inflate(R.layout.notification_item,parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return listNotification.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = listNotification[position]
        holder.notifyText.text=notification.getText()

        publisherInfo(holder.profileimage, holder.username, notification.getUserId())

        if(notification.getIsLikeNotification()) {
            holder.postimg.visibility=View.VISIBLE
            holder.followbutton.visibility=View.GONE
            // Load the image of the post liked
            Picasso.get()
                .load(notification.getPostUrl())
                .placeholder(R.drawable.default_image_profile)
                .into(holder.postimg)
        } else {
            holder.postimg.visibility=View.GONE
            holder.followbutton.visibility=View.VISIBLE
            FirebaseUtils.checkFollowingStatus(holder.itemView.context, firebaseUser!!.uid, notification.getUserId(), holder.followbutton)
        }

        holder.username.setOnClickListener {
            // If user click on the username, open the username profile
            val preference = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
            preference.putString("profileId", notification.getUserId())
            preference.apply()

            (mContext as FragmentActivity).supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment())
                .commit()
        }

        holder.profileimage.setOnClickListener {
            // If user click on the profile image, open the correspondent user profile
            val preference = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
            preference.putString("profileId", notification.getUserId())
            preference.apply()

            (mContext as FragmentActivity).supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment())
                .commit()
        }

        holder.followbutton.setOnClickListener {
            // Call the function for follow or unfollow the user in FirebaseUtils
            FirebaseUtils.handleFollowButtonClick(holder.itemView.context, firebaseUser!!.uid, notification.getUserId(), holder.followbutton)
        }
    }

    private fun publisherInfo(imgView: CircleImageView, username: TextView, publisherid: String) {
        val userRef= FirebaseDatabase.getInstance().reference.child("Users").child(publisherid)
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue<User>(User::class.java)

                Picasso.get()
                    .load(user!!.getImage())
                    .placeholder(R.drawable.default_image_profile)
                    .into(imgView)
                username.text =(user.getUsername())
            }
        })
    }


}