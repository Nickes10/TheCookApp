package com.example.thecookapp.Adapter

import android.content.Context
import android.util.Log
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.thecookapp.Recipe
import com.example.thecookapp.R
import com.example.thecookapp.R.id.profile_post_layout
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso

class ProfilePostAdapter(
    private val context: Context,
    private val postList: List<Recipe>
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
        params.width = imageWidth
        params.height = imageHeight

        linearLayout.layoutParams = params

        return ProfilePostViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProfilePostViewHolder, position: Int) {
        val currentPost = postList[position]

        // Load the Post image using Picasso
        if (!currentPost.image_url.isNullOrEmpty()) {
            Picasso.get()
                .load(currentPost.image_url)
                .fit()
                .centerCrop()
                .into(holder.postImageView, object : Callback {
                    override fun onSuccess() {
                        Log.e("ProfileFragment", "Image loaded successfully")
                    }

                    override fun onError(e: Exception?) {
                        e?.printStackTrace()
                        Log.e("ProfileFragment", "Error loading image: ${e?.message}")

                        Toast.makeText(
                            holder.itemView.context,
                            "Unable to load image, check your connection",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Impose a default image in case of error
                        holder.postImageView.setImageResource(R.drawable.plate_knife_fork)
                    }
                })

        }
    }

    override fun getItemCount(): Int {
        return postList.size
    }
}
