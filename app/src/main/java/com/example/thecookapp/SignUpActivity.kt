package com.example.thecookapp

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.thecookapp.R.id.signin_link_btn
import com.example.thecookapp.R.id.signup_btn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        val signInBtn: Button = findViewById(signin_link_btn)
        signInBtn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        val signUpBtn: Button = findViewById(signup_btn)
        signUpBtn.setOnClickListener {
            createAccount()
        }
    }

    private fun createAccount() {
        val fullName: EditText = findViewById(R.id.fullname_signup)
        val userName: EditText = findViewById(R.id.username_signup)
        val email: EditText = findViewById(R.id.email_signup)
        val password: EditText = findViewById(R.id.password_signup)

        val fullNameString = fullName.text.toString()
        val userNameString = userName.text.toString()
        val emailString = email.text.toString()
        val passwordString = password.text.toString()

        when{
            TextUtils.isEmpty(fullNameString) -> Toast.makeText(this, "full name is required!", Toast.LENGTH_LONG).show()
            TextUtils.isEmpty(userNameString) -> Toast.makeText(this, "user name is required!", Toast.LENGTH_LONG).show()
            TextUtils.isEmpty(emailString) -> Toast.makeText(this, "email is required!", Toast.LENGTH_LONG).show()
            TextUtils.isEmpty(passwordString) -> Toast.makeText(this, "password is required!", Toast.LENGTH_LONG).show()

            else -> { //If all the other line are false (so all the val are NOT empty
                val progressDialog = ProgressDialog (this@SignUpActivity)
                progressDialog.setTitle("SignUp")
                progressDialog.setMessage("Please wait...")
                progressDialog.setCanceledOnTouchOutside(false)
                progressDialog.show()
                
                val mAuth: FirebaseAuth = FirebaseAuth.getInstance()

                mAuth.createUserWithEmailAndPassword(emailString, passwordString)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful)
                        {
                            saveUsersInfo(fullNameString, userNameString, emailString, progressDialog)

                        }
                        else
                        {
                            val message = task.exception!!.toString()
                            Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
                            mAuth.signOut()
                            progressDialog.dismiss()
                        }
                    }
            }

        }

    }

    private fun saveUsersInfo(fullNameString: String, userNameString: String, emailString: String, progressDialog: ProgressDialog) {
        val currentUserID = FirebaseAuth.getInstance().currentUser!!.uid
        val userRef : DatabaseReference = FirebaseDatabase.getInstance().reference.child("Users")

        val userMap = HashMap<String, Any>()
        userMap["uid"] = currentUserID
        userMap["fullname"] = fullNameString.lowercase()
        userMap["username"] = userNameString
        userMap["email"] = emailString
        userMap["bio"] = "Hey I am a professional cooker and I want to share with you some incredible dishes"
        userMap["image"] = "https://firebasestorage.googleapis.com/v0/b/thecookingapp-4857b.appspot.com/o/Default%20Images%2FdefaultImageProfile.jpg?alt=media&token=d66fbe7f-effb-44f5-9174-d52c4d6d412f"

        userRef.child(currentUserID).setValue(userMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful)
                {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Account has been created successfully", Toast.LENGTH_LONG).show()

                    val preferences = getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
                    preferences.putString("profileId", currentUserID)
                    preferences.apply()

                    val intent = Intent(this@SignUpActivity, MainActivity::class.java) //to pass from this to the normal app
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