//package com.example.thecookapp.Adapter
//
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.example.thecookapp.FirebaseUtils
//import com.example.thecookapp.R
//import com.example.thecookapp.Recipe
//import com.google.firebase.database.FirebaseDatabase
//import com.squareup.picasso.Picasso
//
//class PostAdapter(private val post: Recipe) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {
//
//    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
//        val userName: TextView = itemView.findViewById(R.id.postUserName)
//        val title: TextView = itemView.findViewById(R.id.postTitle)
//        val description: TextView = itemView.findViewById(R.id.postDescription)
//        val image: ImageView = itemView.findViewById(R.id.imagePost)
//        //val ingredientsTextView: TextView = itemView.findViewById(R.id.postIngredients)
//        //val instructionsTextView: TextView = itemView.findViewById(R.id.postInstructions)
//        val difficultyTextView: TextView = itemView.findViewById(R.id.postDifficulty)
//        val servingsTextView: TextView = itemView.findViewById(R.id.postServings)
//        val timeTextView: TextView = itemView.findViewById(R.id.postTime)
//        val createdAtTextView: TextView = itemView.findViewById(R.id.postCreatedAt)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
//        return PostViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
//        updateUI(holder, post)
//    }
//
//    private fun updateUI(holder: PostViewHolder, post: Recipe) {
//        FirebaseUtils.fetchUsername(post.user_id) { username ->
//            holder.userName.text = username
//        }
//
//        FirebaseUtils.fetchProfileImage(post.user_id) { profileImageUrl ->
//            Picasso.get()
//                .load(profileImageUrl)
//                .placeholder(R.drawable.default_image_profile)
//                .into(holder.profileImage)
//        }
//
//        holder.title.text = post.title
//        holder.description.text = post.description
//        //holder.ingredientsTextView.text = "Ingredients: ${post.ingredients}"
//        //holder.instructionsTextView.text = "Instructions: ${post.instructions}"
//        holder.difficultyTextView.text = post.difficulty
//        holder.servingsTextView.text = post.servings
//        holder.timeTextView.text = post.time_to_do
//        holder.createdAtTextView.text = "Created At: ${post.created_at}"
//        Picasso.get()
//            .load(post.image_url)
//            .into(holder.image)
//    }
//
//    override fun getItemCount(): Int = 1
//}