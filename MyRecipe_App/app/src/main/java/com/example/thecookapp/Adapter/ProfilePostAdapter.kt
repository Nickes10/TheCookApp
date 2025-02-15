package com.example.thecookapp.Adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.thecookapp.PostDetailsActivity
import com.example.thecookapp.Recipe
import com.example.thecookapp.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException

class ProfilePostAdapter(
    private val context: Context,
    private val postList: List<Recipe>,
    private val postDetailsLauncher: ActivityResultLauncher<Intent>? = null
) : RecyclerView.Adapter<ProfilePostAdapter.ProfilePostViewHolder>() {

    inner class ProfilePostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImageView: ImageView = itemView.findViewById(R.id.profile_posted_picture)
        // Initialize ProgressBar for the image
        val progressBar = itemView.findViewById<ProgressBar>(R.id.progress_bar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfilePostViewHolder {
        val displayMetrics = context?.resources?.displayMetrics
        val screenWidth = displayMetrics?.widthPixels
        val spacing = 4 // Adjust as needed (same as ItemDecoration)
        val columns = 3 // Change this to match the number of grid columns
        val totalSpacing = spacing * (columns + 1) // Total space including edges
        val itemSize = (screenWidth!! - totalSpacing) / columns // Calculate item width dynamically
        // Inflate new layout
        val itemView = LayoutInflater.from(context).inflate(R.layout.posts_profile_layout, parent, false)

        // Set dynamic width and height
        val cardView = itemView.findViewById<CardView>(R.id.profile_post_layout)
        val params = cardView.layoutParams
        params.width = itemSize
        params.height = itemSize
        cardView.layoutParams = params


        return ProfilePostViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProfilePostViewHolder, position: Int) {
        val currentPost = postList[position]



        // Load the Post image using Glide
        if (!currentPost.image_url.isNullOrEmpty()) {
            // Show the ProgressBar
            holder.progressBar.visibility = View.VISIBLE

            Glide.with(context)
                .load(currentPost.image_url)
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
                        holder.postImageView.setImageResource(R.drawable.plate_knife_fork)
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
                .into(holder.postImageView)
        }
        // Add click listener to open PostDetailsActivity
        holder.postImageView.setOnClickListener {
            val intent = Intent(context, PostDetailsActivity::class.java)
            intent.putExtra("POST_DETAILS", currentPost)

            if (postDetailsLauncher != null) {
                // Launcher used to reload the Profile page if the user deletes a post
                postDetailsLauncher.launch(intent)
            } else {
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int {
        return postList.size
    }
}
