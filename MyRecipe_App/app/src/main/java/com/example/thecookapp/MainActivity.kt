package com.example.thecookapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.thecookapp.ui.home.HomeFragment
import com.example.thecookapp.ui.notifications.NotificationsFragment
import com.example.thecookapp.ui.profile.ProfileFragment
import com.example.thecookapp.ui.search.SearchBarFragment

class MainActivity : AppCompatActivity() {

    private lateinit var navView: BottomNavigationView

    private val onNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item ->
        val selectedFragment = when (item.itemId) {
            R.id.navigation_home -> HomeFragment()
            R.id.navigation_search -> SearchBarFragment()
            R.id.navigation_notifications -> NotificationsFragment()
            R.id.navigation_profile -> ProfileFragment()
            else -> null
        }

        selectedFragment?.let {
            replaceFragment(it, addToBackStack = false)
            return@OnNavigationItemSelectedListener true
        }

        if (item.itemId == R.id.navigation_add_post) {
            startActivity(Intent(this@MainActivity, AddPostActivity::class.java))
        }

        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)
        //setSupportActionBar(findViewById(R.id.home_toolbar))

        navView = findViewById(R.id.nav_view)
        navView.setOnItemSelectedListener(onNavigationItemSelectedListener)

        val showProfileFragment = intent.getBooleanExtra("SHOW_PROFILE_FRAGMENT", false)
        if (showProfileFragment) {
            // Navigate to ProfileFragment if the flag is set
            navView.selectedItemId = R.id.navigation_profile
        } else {
            // Default fragment (e.g., HomeFragment)
            navView.selectedItemId = R.id.navigation_home
        }

    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        // Check if not already on HomeFragment
        if (navView.selectedItemId != R.id.navigation_home) {
            navView.selectedItemId = R.id.navigation_home
        } else if (currentFragment !is HomeFragment) {
            replaceFragment(HomeFragment(), addToBackStack = false)
        } else {
            super.onBackPressed() // Quit the app
        }
    }

    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        if (addToBackStack) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
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