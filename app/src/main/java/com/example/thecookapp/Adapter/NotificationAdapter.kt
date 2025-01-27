package com.example.thecookapp.Adapter

import android.content.Context
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
import com.example.thecookapp.R
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


    class ViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        var username: TextView = itemView.findViewById(R.id.notification_username)
        var notifyText: TextView = itemView.findViewById(R.id.notification_text)
        var profileimage: CircleImageView = itemView.findViewById(R.id.notification_image_profile)
        var postimg: ImageView = itemView.findViewById(R.id.posted_image)
        var followbutton: Button = itemView.findViewById(R.id.notify_follow_button)
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
        if(notification.getIsLikedNotification())
        {
            holder.postimg.visibility=View.VISIBLE
            //getPostedImg(holder.postimg,notification.getPostId())
            Picasso.get()
                .load(R.drawable.default_image_profile)
                .placeholder(R.drawable.default_image_profile)
                .into(holder.postimg)
        }
        else
        {
            holder.postimg.visibility=View.GONE
            holder.followbutton.visibility=View.VISIBLE
        }


//        holder.postimg.setOnClickListener {
//            if(notification.getIsLikedNotification()) {
//                val pref = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
//                pref.putString("postid", notification.getPostId())
//                pref.apply()
//
//                (mContext as FragmentActivity).supportFragmentManager.beginTransaction()
//                    .replace(R.id.fragment_container, PostDetailFragment()).commit()
//            }
//            else
//            {
//
//                val pref=mContext.getSharedPreferences("PREFS",Context.MODE_PRIVATE).edit()
//                pref.putString("profileid",notification.getUserId())
//                pref.apply()
//
//                (mContext as FragmentActivity).supportFragmentManager.beginTransaction()
//                    .replace(R.id.fragment_container, ProfileFragment()).commit()
//            }
//        }
    }

    private fun publisherInfo(imgView: CircleImageView, username: TextView, publisherid: String) {

        val userRef= FirebaseDatabase.getInstance().reference.child("Users").child(publisherid)
        userRef.addValueEventListener(object : ValueEventListener
        {
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

//    private fun getPostedImg(postimg:ImageView, postid:String?) {
//
//        val postRef= FirebaseDatabase.getInstance().reference.child("Posts").child(postid!!)
//
//        postRef.addValueEventListener(object : ValueEventListener {
//            override fun onCancelled(error: DatabaseError) {
//            }
//            override fun onDataChange(snapshot: DataSnapshot)
//            {
//                val post = snapshot.getValue(Post::class.java)
//                Picasso.get().load(post!!.getPostImage()).into(postimg)
//            }
//        })
//    }
}