package com.example.thecookapp.Adapter

import android.view.LayoutInflater
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thecookapp.Recipe
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.example.thecookapp.FirebaseUtils
import com.example.thecookapp.R
import com.squareup.picasso.Picasso
import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.example.thecookapp.ui.profile.ProfileFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class PostPreviewAdapter(
    private val mContext: Context,
    private val posts: List<Recipe>,
    private val onPostClick: (Recipe) -> Unit
) : RecyclerView.Adapter<PostPreviewAdapter.PostPreviewViewHolder>() {

    private val firebaseAuth = FirebaseAuth.getInstance()

    class PostPreviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.postUserName)
        val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        val title: TextView = itemView.findViewById(R.id.postTitle)
        val image: ImageView = itemView.findViewById(R.id.imagePost)
        val createdAt: TextView = itemView.findViewById(R.id.postCreatedAt)
        val likeButton: ImageView = itemView.findViewById(R.id.likeButton)
        val numberLikes: TextView = itemView.findViewById(R.id.likeCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostPreviewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post_preview, parent, false)
        return PostPreviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostPreviewViewHolder, position: Int) {
        val currentUserId = firebaseAuth.currentUser?.uid
        val post = posts[position]

        // To put username with the first letter capitalized
        FirebaseUtils.fetchUsername(post.user_id) { username ->
            val formattedUsername = username.split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            holder.userName.text = formattedUsername
        }

        FirebaseUtils.fetchProfileImage(post.user_id) { profileImageUrl ->
            Picasso.get()
                .load(profileImageUrl)
                .placeholder(R.drawable.default_image_profile)
                .into(holder.profileImage)
        }

        holder.title.text = post.title

        Log.e("Upload Image", "Image URL: ${post.image_url}")

        Glide.with(mContext)
            .load(post.image_url)
            .placeholder(R.drawable.plate_knife_fork)
            .into(holder.image)

//        Picasso.get()
//            .load(post.image_url)
//            .placeholder(R.drawable.plate_knife_fork)
//            .into(holder.image)

        val originalFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val targetFormat = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
        val date = originalFormat.parse(post.created_at)
        val formattedDate = targetFormat.format(date)
        holder.createdAt.text = "Created at: $formattedDate"

        isLiked(post.user_id, post.post_id.toString(), holder.likeButton)
        getNumberLikes(post.user_id, post.post_id.toString(), holder.numberLikes)

        holder.itemView.setOnClickListener {
            onPostClick(post)
        }

        holder.userName.setOnClickListener {
            openProfileFragment(post.user_id)
        }

        holder.profileImage.setOnClickListener {
            openProfileFragment(post.user_id)
        }

        holder.likeButton.setOnClickListener{
            if (holder.likeButton.tag.toString()=="like") {
                FirebaseDatabase.getInstance().reference.child("Likes")
                    .child(post.user_id)
                    .child(post.post_id.toString())
                    .child(currentUserId!!)
                    .setValue(true)
                
                FirebaseUtils.pushNotification(post.user_id, post.image_url, true)
            } else {
                FirebaseDatabase.getInstance().reference.child("Likes")
                    .child(post.user_id)
                    .child(post.post_id.toString())
                    .child(currentUserId!!)
                    .removeValue()

                FirebaseUtils.removeNotification(post.user_id, post.image_url, isLikeNotification = true)
            }
        }
    }


    override fun getItemCount(): Int = posts.size

    private fun openProfileFragment(userId: String) {
        val preference = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
        preference.putString("profileId", userId)
        preference.apply()

        (mContext as FragmentActivity).supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ProfileFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun isLiked(userid: String, postid:String, likeImageView: ImageView) {
        // Function to verify if the post is liked by the app user
        val currentUser=FirebaseAuth.getInstance().currentUser
        val postRef=FirebaseDatabase.getInstance().reference.child("Likes").child(userid).child(postid)

        postRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(datasnapshot: DataSnapshot) {
                if (datasnapshot.child(currentUser!!.uid).exists()) {
                    likeImageView.setImageResource(R.drawable.like_icon_full)
                    likeImageView.tag =" liked"
                }
                else {
                    likeImageView.setImageResource(R.drawable.like_icon_black)
                    likeImageView.tag = "like"
                }
            }
        })
    }

    private fun getNumberLikes(userid: String, postid: String, Nlikes: TextView) {
        // To set number of likes of the post
        val postRef=FirebaseDatabase.getInstance().reference.child("Likes").child(userid).child(postid)

        postRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
            }

            override fun onDataChange(datasnapshot: DataSnapshot) {
                Nlikes.text = datasnapshot.childrenCount.toString()
            }
        })
    }

}
