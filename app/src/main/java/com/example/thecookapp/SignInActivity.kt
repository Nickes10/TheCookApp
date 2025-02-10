package com.example.thecookapp

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.thecookapp.R.id.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

class SignInActivity : AppCompatActivity() {
    // variable necessary to Google Sign In
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private val reqCode:Int=123


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)

        val signUpBtn: TextView = findViewById(sign_up_btn)
        signUpBtn.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        val loginBtn: Button = findViewById(login_btn)
        loginBtn.setOnClickListener {
            loginUser()
        }

        // Definition of the Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient= GoogleSignIn.getClient(this,gso)

        val googleSignInBtn= findViewById<LinearLayout>(R.id.google_sign_in_layout)
        googleSignInBtn.setOnClickListener{
            signInWithGoogle()
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

                            val user = mAuth.currentUser // Retrieve the current user

                            if (user != null) { // Ensure the user is not null

                                FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                                    if (tokenTask.isSuccessful) {
                                        val fcmToken = tokenTask.result

                                        // Save the FCM token to Firebase Realtime Database for notifications
                                        FirebaseDatabase.getInstance().reference
                                            .child("Users").child(user.uid)
                                            .child("fcmToken").setValue(fcmToken)
                                            .addOnSuccessListener {
                                                Log.d("SignInActivity", "FCM Token saved successfully.")
                                            }
                                            .addOnFailureListener { error ->
                                                Log.e("SignInActivity", "Failed to save FCM token: ${error.message}")
                                            }
                                    }
                                }

                                val preference = getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
                                preference.putString("profileId", user.uid)
                                preference.apply()
                            }

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

    private  fun signInWithGoogle(){
        // Function to sign in with Google
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent,reqCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Function to handle the result of the Google Sign In
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == reqCode) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    updateUI(account)
                }
            } catch (e: ApiException) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
                Log.e("SignInProblem", "Error processing sign in: $e")
            }
        }
    }


    private fun updateUI(account: GoogleSignInAccount){
        // Function to create the user in the database if he sign in with Google
        val credential= GoogleAuthProvider.getCredential(account.idToken,null)
        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener {task->
            if(task.isSuccessful) {
                val user = FirebaseAuth.getInstance().currentUser
                user?.let {
                    val userId = it.uid
                    val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(userId)

                    // Extract name and surname from Google account
                    val givenName = account.givenName ?: "unknown"
                    val familyName = account.familyName ?: "user"
                    val username = "${givenName}_${familyName}"

                    // Create user object with necessary fields
                    val userMap = HashMap<String, Any>()
                    userMap["uid"] = userId
                    userMap["email"] = account.email ?: ""
                    userMap["fullname"] = account.displayName ?: ""
                    userMap["username"] = username
                    userMap["image"] = account.photoUrl?.toString() ?: ""  // Profile image URL from Google
                    userMap["bio"] = "Hey, I am using TheCookApp!" // Default bio


                    // Save the user information to the Realtime Database
                    userRef.setValue(userMap).addOnCompleteListener { saveTask ->
                        if (saveTask.isSuccessful) {
                            // User information saved successfully
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            // Handle the error
                            Toast.makeText(this, "Failed to save user information.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                // Handle sign-in failure
                Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (FirebaseAuth.getInstance().currentUser != null) {
            // Code to move the app from SignUp to the menu, removing the possibility for the user to go back
            val intent = Intent(this@SignInActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
}