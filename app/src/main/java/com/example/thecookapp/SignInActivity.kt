package com.example.thecookapp

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.thecookapp.R.id.*
import com.google.firebase.auth.FirebaseAuth

class SignInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)

        val signUpBtn: Button = findViewById(first_signup_btn)
        signUpBtn.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        val loginBtn: Button = findViewById(login_btn)
        loginBtn.setOnClickListener {
            loginUser()
        }

    }

    private fun loginUser() {
        val email: EditText = findViewById(email_login)
        val password: EditText = findViewById(password_login)

        val emailString = email.text.toString()
        val passwordString = password.text.toString()

        when{
            TextUtils.isEmpty(emailString) -> Toast.makeText(this, "email is required!", Toast.LENGTH_LONG).show()
            TextUtils.isEmpty(passwordString) -> Toast.makeText(this, "password is required!", Toast.LENGTH_LONG).show()

            else -> {
                val progressDialog = ProgressDialog (this@SignInActivity)
                progressDialog.setTitle("Login")
                progressDialog.setMessage("Please wait...")
                progressDialog.setCanceledOnTouchOutside(false)
                progressDialog.show()

                val mAuth: FirebaseAuth = FirebaseAuth.getInstance()

                mAuth.signInWithEmailAndPassword(emailString, passwordString)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful)
                        {
                            progressDialog.dismiss()
                            val intent = Intent(this@SignInActivity, MainActivity::class.java) //to pass from this to the normal app
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            finish()
                        }
                        else
                        {
                            val message = task.exception!!.toString()
                            Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
                            FirebaseAuth.getInstance().signOut()
                            progressDialog.dismiss()
                        }

                    }
            }
        }
    }

    /*
    override fun onStart() {
        super.onStart()
        if (FirebaseAuth.getInstance().currentUser != null)
        {
            // Code to move the app from SignUp to the menu, removing the possibility for the user to go back
            val intent = Intent(this@SignInActivity, MainActivity::class.java) //to pass from this to the normal app
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

     */
}