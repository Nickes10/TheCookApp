package com.example.thecookapp

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.example.thecookapp.ui.profile.ProfileFragment
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Locale

class PostDetailsActivity : AppCompatActivity() {

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

        Picasso.get()
            .load(post.image_url)
            .placeholder(R.drawable.plate_knife_fork)
            .into(findViewById<ImageView>(R.id.imagePost))

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
            super.onBackPressed() // Normal back press behavior
        }
    }
}