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
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
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
        val fullName: TextView = itemView.findViewById(R.id.postUserFullName)
        val userName: TextView = itemView.findViewById(R.id.postUserName)
        val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        val title: TextView = itemView.findViewById(R.id.postTitle)
        val image: ImageView = itemView.findViewById(R.id.imagePost)
        val createdAt: TextView = itemView.findViewById(R.id.postCreatedAt)
        val likeButton: ImageView = itemView.findViewById(R.id.likeButton)
        val numberLikes: TextView = itemView.findViewById(R.id.likeCount)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar_preview)
        val insideHeart: ImageView = itemView.findViewById(R.id.insideHeart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostPreviewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post_preview, parent, false)
        return PostPreviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostPreviewViewHolder, position: Int) {
        val currentUserId = firebaseAuth.currentUser?.uid
        val post = posts[position]

        // To put username with the first letter capitalized
        FirebaseUtils.fetchUserandFullName(post.user_id) { fullName, userName ->
            val formattedFullname = fullName.split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            holder.fullName.text = formattedFullname

            val formattedUserName = "@${userName.trim()}"
            holder.userName.text = formattedUserName
        }

        FirebaseUtils.fetchProfileImage(post.user_id) { profileImageUrl ->
            Picasso.get()
                .load(profileImageUrl)
                .placeholder(R.drawable.default_image_profile)
                .into(holder.profileImage)
        }

        holder.title.text = post.title.uppercase()

        Log.e("Upload Image", "Image URL: ${post.image_url}")

        if (!post.image_url.isNullOrEmpty()) {
            // Show the ProgressBar
            holder.progressBar.visibility = View.VISIBLE

            Glide.with(mContext)
                .load(post.image_url)
                .centerCrop()
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        e?.printStackTrace()
                        Log.e("ProfileFragment", "Error loading image: ${e?.message}")

                        Toast.makeText(
                            holder.itemView.context,
                            "Unable to load image, check your connection",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Impose a default image in case of error
                        holder.image.setImageResource(R.drawable.plate_knife_fork)
                        holder.progressBar.visibility = View.GONE
                        return false // Return false to allow Glide to handle the error placeholder
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("ProfileFragment", "Image loaded successfully")
                        // When image is loaded, hide the ProgressBar
                        holder.progressBar.visibility = View.GONE
                        return false // Return false to let Glide handle setting the image
                    }
                })
                .into(holder.image)
        }


        val originalFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val targetFormat = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
        val date = originalFormat.parse(post.created_at)
        val formattedDate = targetFormat.format(date)
        holder.createdAt.text = "Created at: $formattedDate"

        FirebaseUtils.isLiked(post.user_id, post.post_id.toString(), holder.likeButton)
        getNumberLikes(post.user_id, post.post_id.toString(), holder.numberLikes)


        holder.fullName.setOnClickListener {
            openProfileFragment(post.user_id)
        }

        holder.userName.setOnClickListener {
            openProfileFragment(post.user_id)
        }

        holder.profileImage.setOnClickListener {
            openProfileFragment(post.user_id)
        }

        holder.likeButton.setOnClickListener{
            FirebaseUtils.handleLikeButtonClick(mContext, post, holder.likeButton)
        }

        val zoomInAnim = AnimationUtils.loadAnimation(mContext, R.anim.zoom_in)
        val zoomOutAnim = AnimationUtils.loadAnimation(mContext, R.anim.zoom_out)

        holder.itemView.setOnClickListener(object : DoubleClickListener() {
            override fun onSingleClick(v: View?) {
                // Single click: start the activity to show full post details.
                onPostClick(post)
            }

            override fun onDoubleClick(v: View?) {
                // Double click: perform the like animation and like the post.
                holder.insideHeart.startAnimation(zoomInAnim)
                holder.insideHeart.startAnimation(zoomOutAnim)
                FirebaseUtils.handleLikeButtonClick(mContext, post, holder.likeButton)
            }
        })
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

    abstract class DoubleClickListener : View.OnClickListener {
        // Class to handle single and double click

        private var lastClickTime: Long = 0
        private val doubleClickTimeDelta: Long = 200 // milliseconds
        private var handler = Handler(Looper.getMainLooper())
        private var singleClickRunnable: Runnable? = null

        override fun onClick(v: View?) {
            val clickTime = System.currentTimeMillis()
            if (clickTime - lastClickTime < doubleClickTimeDelta) {
                // It's a double click: cancel any pending single-click action and trigger double-click
                singleClickRunnable?.let { handler.removeCallbacks(it) }
                onDoubleClick(v)
            } else {
                // Schedule the single-click action after the delay
                singleClickRunnable = Runnable {
                    onSingleClick(v)
                }
                handler.postDelayed(singleClickRunnable!!, doubleClickTimeDelta)
            }
            lastClickTime = clickTime
        }

        abstract fun onSingleClick(v: View?)
        abstract fun onDoubleClick(v: View?)
    }


}
