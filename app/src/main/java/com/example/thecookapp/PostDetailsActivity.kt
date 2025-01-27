package com.example.thecookapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.example.thecookapp.ui.profile.ProfileFragment
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import android.view.MenuItem
import com.bumptech.glide.Glide


class PostDetailsActivity : AppCompatActivity() {

    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.item_post)

        val post = intent.getParcelableExtra<Recipe>("POST_DETAILS")

        if (post != null) {
            updateUI(post)
        } else {
            Toast.makeText(this, "Error loading post details", Toast.LENGTH_SHORT).show()
            finish()
        }


    }

    private fun updateUI(post: Recipe) {
        val userId = firebaseAuth.currentUser?.uid

        val menuOptions = findViewById<ImageView>(R.id.menuOptions)
        if (post.user_id == userId) {
            menuOptions.visibility = View.VISIBLE
        } else {
            menuOptions.visibility = View.GONE
        }

        menuOptions.setOnClickListener {
            showMenuOptions(post)
        }

        FirebaseUtils.fetchUsername(post.user_id) { username ->
            val formattedUsername = username.split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            findViewById<TextView>(R.id.postUserName).text = formattedUsername
            findViewById<TextView>(R.id.postUserName).setOnClickListener {
                showProfileFragment(post.user_id)
            }
        }

        FirebaseUtils.fetchProfileImage(post.user_id) { profileImageUrl ->
            Picasso.get()
                .load(profileImageUrl)
                .placeholder(R.drawable.default_image_profile)
                .into(findViewById<ImageView>(R.id.profileImage))
            findViewById<TextView>(R.id.postUserName).setOnClickListener {
                showProfileFragment(post.user_id)
            }
        }
        if(post.latitude != null){
            findViewById<TextView>(R.id.locationInput).text =
                "lat: ${post.latitude}, lon: ${post.longitude}"
        }
        findViewById<TextView>(R.id.postTitle).text = post.title
        findViewById<TextView>(R.id.postDescription).text = post.description
        //findViewById<TextView>(R.id.postIngredients).text = "Ingredients: ${post.ingredients}"
        //findViewById<TextView>(R.id.postInstructions).text = "Instructions: ${post.instructions}"
        findViewById<TextView>(R.id.postDifficulty).text = post.difficulty
        findViewById<TextView>(R.id.postServings).text = post.servings
        findViewById<TextView>(R.id.postTime).text = post.time_to_do

        val originalFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val targetFormat = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
        val date = originalFormat.parse(post.created_at)
        val formattedDate = targetFormat.format(date)

        findViewById<TextView>(R.id.postCreatedAt).text = "Created at: $formattedDate"

        Glide.with(this)
            .load(post.image_url)
            .placeholder(R.drawable.plate_knife_fork)
            .into(findViewById<ImageView>(R.id.imagePost))


//        Picasso.get()
//            .load(post.image_url)
//            .placeholder(R.drawable.plate_knife_fork)
//            .into(findViewById<ImageView>(R.id.imagePost))

        val ingredientsContainer = findViewById<LinearLayout>(R.id.postIngredientsContainer)
        ingredientsContainer.removeAllViews() // Clear any previous ingredients

        for ((ingredient, value) in post.ingredients) {
            val ingredientRow = layoutInflater.inflate(R.layout.ingredient_row, null)
            val ingredientLabel = ingredientRow.findViewById<TextView>(R.id.ingredientKey)
            val ingredientValue = ingredientRow.findViewById<TextView>(R.id.ingredientValue)

            ingredientLabel.text = "$ingredient:"
            ingredientValue.text = value

            ingredientsContainer.addView(ingredientRow)
        }

        val instructionsContainer = findViewById<LinearLayout>(R.id.postInstructionsContainer)
        instructionsContainer.removeAllViews() // Clear any previous instructions

        post.instructions.forEachIndexed { index, instruction ->
            val instructionRow = layoutInflater.inflate(R.layout.instruction_row, null)
            val instructionLabel = instructionRow.findViewById<TextView>(R.id.instructionLabel)
            val instructionValue = instructionRow.findViewById<TextView>(R.id.instructionValue)

            instructionLabel.text = "Step ${index + 1}:"
            instructionValue.text = instruction

            instructionsContainer.addView(instructionRow)
        }
    }

    private fun showMenuOptions(post: Recipe) {
        val popupMenu = PopupMenu(this, findViewById(R.id.menuOptions))
        popupMenu.menuInflater.inflate(R.menu.post_menu , popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.modifyPost -> {
                    modifyPost(post)
                    true
                }
                R.id.deletePost -> {
                    deletePost(post)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun modifyPost(post: Recipe) {
        val intent = Intent(this, AddPostActivity::class.java)
        intent.putExtra("RECIPE_DETAILS", post)
        startActivity(intent)
    }

    private fun deletePost(post: Recipe) {
        ApiClient.recipeApi.deletePost(post.user_id, post.post_id).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Log.e("API_SUCCESS", "Recipe deleted: ${response.body()}")
                    runOnUiThread {
                        Toast.makeText(this@PostDetailsActivity, "Post deleted successfully!", Toast.LENGTH_LONG).show()
                        val resultIntent = Intent()
                        resultIntent.putExtra("post_deleted", true) // Optional: Pass additional data if needed
                        setResult(Activity.RESULT_OK, resultIntent) // Set the result code
                        finish() // Close the current activity and return to the previous screen
                    }
                } else {
                    Log.e("API_ERROR", "Error: ${response.errorBody()?.string()}")
                    runOnUiThread {
                        Toast.makeText(this@PostDetailsActivity, "Failed to delete the post. Please try again.", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Log.e("API_ERROR", "Failed to delete recipe", t)
                runOnUiThread {
                    Toast.makeText(this@PostDetailsActivity, "An error occurred. Please try again.", Toast.LENGTH_LONG).show()
                }
            }
        })
    }


    private fun showProfileFragment(userId: String) {
        // Save profile ID in SharedPreferences
        val preferences = getSharedPreferences("PREFS", Context.MODE_PRIVATE)
        preferences.edit().putString("profileId", userId).apply()

        // Replace the fragment container with the ProfileFragment
        val fragment = ProfileFragment()
        val args = Bundle().apply {
            putString("profileId", userId)
        }
        fragment.arguments = args

        supportFragmentManager.beginTransaction()
            .replace(R.id.profileFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()

        // Show the profile fragment container and hide the post details container
        findViewById<View>(R.id.profileFragmentContainer).visibility = View.VISIBLE
        findViewById<View>(R.id.postDetailsContainer).visibility = View.GONE
    }

    override fun onBackPressed() {
        val profileFragmentContainer = findViewById<View>(R.id.profileFragmentContainer)
        val postDetailsContainer = findViewById<View>(R.id.postDetailsContainer)

        if (profileFragmentContainer.visibility == View.VISIBLE) {
            // Hide profile fragment and show post details container
            profileFragmentContainer.visibility = View.GONE
            postDetailsContainer.visibility = View.VISIBLE
        } else {
            finish()
            super.onBackPressed() // Normal back press behavior
        }
    }
}