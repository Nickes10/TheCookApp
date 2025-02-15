package com.example.thecookapp

import android.content.Intent
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var oldPassword: EditText
    private lateinit var newPassword: EditText
    private lateinit var confirmNewPassword: EditText
    private lateinit var changePasswordButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var currentUser: FirebaseUser
    private lateinit var auth: FirebaseAuth
    private lateinit var oldPasswordError: TextView
    private lateinit var passwordMatchError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        // Initialize views
        oldPassword = findViewById(R.id.old_password)
        newPassword = findViewById(R.id.new_password)
        confirmNewPassword = findViewById(R.id.confirm_new_password)
        changePasswordButton = findViewById(R.id.confirm_password_button)
        backButton = findViewById(R.id.back_arrow_button)
        oldPasswordError = findViewById(R.id.old_password_error)
        passwordMatchError = findViewById(R.id.password_match_error)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!

        backButton.setOnClickListener {
            onBackPressed()
        }

        setButtonBackground()

        oldPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable?) {
                setButtonBackground()  // Call the method to check field input
            }
        })

        newPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable?) {
                setButtonBackground()  // Call the method to check field input
            }
        })

        confirmNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable?) {
                setButtonBackground()  // Call the method to check field input
            }
        })


        changePasswordButton.setOnClickListener {
            val oldPass = oldPassword.text.toString().trim()
            val newPass = newPassword.text.toString().trim()
            val confirmPass = confirmNewPassword.text.toString().trim()

            oldPasswordError.visibility = View.GONE
            passwordMatchError.visibility = View.GONE

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                // Toast.makeText(this, "New password and confirmation do not match", Toast.LENGTH_SHORT).show()
                passwordMatchError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Disable button and change its appearance when validation succeeds
            changePasswordButton.isEnabled = false

            // Re-authenticate the user before updating the password
            val credential = EmailAuthProvider.getCredential(currentUser.email!!, oldPass)
            currentUser.reauthenticate(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Update the password
                        currentUser.updatePassword(newPass)
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show()
                                    finish()  // Close the activity
                                } else {
                                    Toast.makeText(this, "Password change failed: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                                // Re-enable the button
                                changePasswordButton.isEnabled = true
                            }
                    } else {
                        // Toast.makeText(this, "Re-authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        oldPasswordError.visibility = View.VISIBLE
                        // Re-enable the button
                        changePasswordButton.isEnabled = true
                    }
                }
        }
    }

    private fun setButtonBackground() {
        val oldPass = oldPassword.text.toString().trim()
        val newPass = newPassword.text.toString().trim()
        val confirmPass = confirmNewPassword.text.toString().trim()

        if (oldPass.isNotEmpty() && newPass.isNotEmpty() && confirmPass.isNotEmpty()) {
            // Set the button background to "buttons_background22" when fields are filled
            changePasswordButton.setBackgroundResource(R.drawable.buttons_background22)
        } else {
            // Set the default button background when fields are not filled
            changePasswordButton.setBackgroundResource(R.drawable.buttons_background11)
        }
    }

}
