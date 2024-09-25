package com.example.thecookapp

import android.os.Bundle
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                textView.setText("Home")
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_search -> {
                textView.setText("Search")
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_add_post -> {
                textView.setText("Add Post")
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                textView.setText("Notifications")
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_profile -> {
                textView.setText("Profile")
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
        textView = findViewById(R.id.message)
        navView.setOnItemSelectedListener(onNavigationItemSelectedListener)

    }
}