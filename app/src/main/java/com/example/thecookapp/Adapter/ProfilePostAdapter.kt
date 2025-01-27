package com.example.thecookapp.Adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.recyclerview.widget.RecyclerView
import com.example.thecookapp.PostDetailsActivity
import com.example.thecookapp.Recipe
import com.example.thecookapp.R
import com.example.thecookapp.R.id.profile_post_layout
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException

class ProfilePostAdapter(
    private val context: Context,
    private val postList: List<Recipe>,
    private val postDetailsLauncher: ActivityResultLauncher<Intent>
) : RecyclerView.Adapter<ProfilePostAdapter.ProfilePostViewHolder>() {

    inner class ProfilePostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImageView: ImageView = itemView.findViewById(R.id.profile_posted_picture)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfilePostViewHolder {
        val displayMetrics = context?.resources?.displayMetrics
        val screenWidth = displayMetrics?.widthPixels
        val imageWidth = screenWidth?.div(3) ?: 0
        val imageHeight = imageWidth

        val itemView = LayoutInflater.from(context).inflate(R.layout.profilepost_layout, parent, false)
        val linearLayout = itemView.findViewById<LinearLayout>(profile_post_layout)
        val params = linearLayout.layoutParams

        // Set the width and height of the layout to be adaptable to each screen
        params.width = imageWidth
        params.height = imageHeight

        linearLayout.layoutParams = params

        return ProfilePostViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProfilePostViewHolder, position: Int) {
        val currentPost = postList[position]

        // Load the Post image using Glide
        if (!currentPost.image_url.isNullOrEmpty()) {

            Glide.with(context)
                .load(currentPost.image_url)
                .placeholder(R.drawable.plate_knife_fork)
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
                        return false // Return false to let Glide handle setting the image
                    }
                })
                .into(holder.postImageView)
        }
        // Add click listener to open PostDetailsActivity
        holder.postImageView.setOnClickListener {
            val intent = Intent(context, PostDetailsActivity::class.java)
            intent.putExtra("POST_DETAILS", currentPost)
            postDetailsLauncher.launch(intent)
        }
    }

    override fun getItemCount(): Int {
        return postList.size
    }
}
