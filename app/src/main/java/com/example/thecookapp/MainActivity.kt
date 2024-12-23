package com.example.thecookapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.thecookapp.ui.home.HomeFragment
import com.example.thecookapp.ui.notifications.NotificationsFragment
import com.example.thecookapp.ui.profile.ProfileFragment
import com.example.thecookapp.ui.search.SearchBarFragment

class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private var selectedFragment: Fragment? = null

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                moveToFragment(HomeFragment())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_search -> {
                moveToFragment(SearchBarFragment())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_add_post -> {
                item.isChecked=false
                startActivity(Intent(this@MainActivity,AddPostActivity::class.java))
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                moveToFragment(NotificationsFragment())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_profile -> {
                moveToFragment(ProfileFragment())
                return@OnNavigationItemSelectedListener true
            }
        }
        
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)
        //setSupportActionBar(findViewById(R.id.home_toolbar))

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.setOnItemSelectedListener(onNavigationItemSelectedListener)

        val showProfileFragment = intent.getBooleanExtra("SHOW_PROFILE_FRAGMENT", false)
        if (showProfileFragment) {
            // Navigate to ProfileFragment if the flag is set
            moveToFragment(ProfileFragment())
        } else {
            // Default fragment (e.g., HomeFragment)
            moveToFragment(HomeFragment())
        }

        val newRecipe = Recipe(
            user_id = 1,
            post_id = 100, // You could increment this based on the number of recipes posted, e.g., by counting entries in your database
            title = "Pasta Carbonara",
            description = "A classic Italian dish.",
            ingredients = mapOf(
                "Pasta" to "200g",
                "Eggs" to "2",
                "Parmesan" to "50g",
                "Bacon" to "100g",
                "Garlic" to "2 cloves"
            ),
            instructions = listOf("ok pasta.","Fry bacon.", "Mix eggs with cheese.", "Combine all."),
            image_url = "http://example.com/carbonara.jpg",
            difficulty = "Easy",
            servings = 2,
            time = "flask prende current time"
        )

        // Use the API to add the recipe
        ApiClient.recipeApi.addRecipe(newRecipe).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Log.d("API", "Recipe added: ${response.body()}")
                } else {
                    Log.e("API","Error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Log.e("API", "Failed to add recipe", t)
            }
        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            val fragmentNavigation = data?.getStringExtra("fragment_navigation")
            if (fragmentNavigation == "profileFragment") {
                moveToFragment(ProfileFragment())  // Navigate to ProfileFragment
            }
        }
    }

    private fun moveToFragment(fragment:Fragment)
    {
        supportFragmentManager.beginTransaction().replace(
            R.id.fragment_container,
            fragment
        ).commit()
    }

}