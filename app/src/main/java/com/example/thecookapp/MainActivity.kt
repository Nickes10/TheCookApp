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