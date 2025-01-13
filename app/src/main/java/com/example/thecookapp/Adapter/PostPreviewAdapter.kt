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
import androidx.fragment.app.FragmentActivity
import com.example.thecookapp.ui.profile.ProfileFragment
import java.text.SimpleDateFormat
import java.util.*

class PostPreviewAdapter(
    private val mContext: Context,
    private val posts: List<Recipe>,
    private val onPostClick: (Recipe) -> Unit
) : RecyclerView.Adapter<PostPreviewAdapter.PostPreviewViewHolder>() {

    class PostPreviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.postUserName)
        val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        val title: TextView = itemView.findViewById(R.id.postTitle)
        val image: ImageView = itemView.findViewById(R.id.imagePost)
        val createdAt: TextView = itemView.findViewById(R.id.postCreatedAt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostPreviewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post_preview, parent, false)
        return PostPreviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostPreviewViewHolder, position: Int) {
        val post = posts[position]

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

        Picasso.get()
            .load(post.image_url)
            .placeholder(R.drawable.plate_knife_fork)
            .into(holder.image)

        val originalFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val targetFormat = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
        val date = originalFormat.parse(post.created_at)
        val formattedDate = targetFormat.format(date)
        holder.createdAt.text = "Created at: $formattedDate"

        holder.itemView.setOnClickListener {
            onPostClick(post)
        }

        holder.userName.setOnClickListener {
            openProfileFragment(post.user_id)
        }

        holder.profileImage.setOnClickListener {
            openProfileFragment(post.user_id)
        }
    }

    private fun openProfileFragment(userId: String) {
        val preference = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
        preference.putString("profileId", userId)
        preference.apply()

        (mContext as FragmentActivity).supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ProfileFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun getItemCount(): Int = posts.size
}
