package com.example.thecookapp

import android.content.Intent
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ChangePasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        val changePasswordButton = findViewById<Button>(R.id.confirm_password_button)

        changePasswordButton.setOnClickListener {

        }

    }

    private fun logout() {

    }

}
