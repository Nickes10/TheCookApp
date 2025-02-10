package com.example.thecookapp

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.thecookapp.R.id.signin_link_btn
import com.example.thecookapp.R.id.signup_btn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso


class SignUpActivity : AppCompatActivity() {

    private lateinit var selectedImageUri: Uri
    private lateinit var profileImageView: ImageView
    private lateinit var storageReference: StorageReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

//        val fixedButtonContainer = findViewById<LinearLayout>(R.id.fixed_button_container)
//        val rootView: View = findViewById(android.R.id.content)
//        rootView.viewTreeObserver.addOnGlobalLayoutListener {
//            val rect = Rect()
//            rootView.getWindowVisibleDisplayFrame(rect)
//
//            val screenHeight = rootView.rootView.height
//            val keyboardHeight = screenHeight - rect.bottom
//
//            // Check if the keyboard is visible (arbitrary threshold)
//            if (keyboardHeight > screenHeight * 0.15) {
//                Log.e("Pasqualo", "Keyboard opened")
//                // Adjust the height of the fixed button container when the keyboard is visible
//                val params = fixedButtonContainer.layoutParams as RelativeLayout.LayoutParams
//                params.height = 500
//                Log.e("Pasqualo", params.toString())
//                fixedButtonContainer.layoutParams = params
//                fixedButtonContainer.requestLayout()
//                Log.e("DEBUG", "New height: ${fixedButtonContainer.layoutParams.height}")
//
//            } else {
//                // Reset to default height when the keyboard is hidden
//                val params = fixedButtonContainer.layoutParams as LinearLayout.LayoutParams
//                params.height = 2054
//
//                fixedButtonContainer.layoutParams = params
//                fixedButtonContainer.requestLayout()
//            }
//        }

        profileImageView = findViewById(R.id.profile_image)
        val selectImageBtn: TextView = findViewById(R.id.select_image_btn)
        val signInBtn: TextView = findViewById(signin_link_btn)
        signInBtn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        val signUpBtn: Button = findViewById(signup_btn)
        signUpBtn.setOnClickListener {
            createAccount()
        }

        storageReference = FirebaseStorage.getInstance().reference

        signInBtn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        signUpBtn.setOnClickListener {
            createAccount()
        }

        selectImageBtn.setOnClickListener {
            // Launch image picker to choose profile image
            openImagePicker()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        openImagePickerLauncher.launch(intent)
    }

    private val openImagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                selectedImageUri = imageUri
                Picasso.get().load(selectedImageUri).into(profileImageView)
            }
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
                            uploadProfileImage(fullNameString, userNameString, emailString, progressDialog)
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

    private fun uploadProfileImage(fullNameString: String, userNameString: String, emailString: String, progressDialog: ProgressDialog) {
        if (::selectedImageUri.isInitialized) {
            val filePath = storageReference.child("ProfileImages").child("${FirebaseAuth.getInstance().currentUser!!.uid}.jpg")
            filePath.putFile(selectedImageUri)
                .addOnSuccessListener {
                    filePath.downloadUrl.addOnSuccessListener { uri ->
                        saveUsersInfo(fullNameString, userNameString, emailString, uri.toString(), progressDialog)
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error uploading image: ${exception.message}", Toast.LENGTH_SHORT).show()
                    progressDialog.dismiss()
                }
        } else {
            saveUsersInfo(fullNameString, userNameString, emailString, "", progressDialog)
        }
    }


    private fun saveUsersInfo(fullNameString: String, userNameString: String, emailString: String, imageUrl: String, progressDialog: ProgressDialog) {
        val currentUserID = FirebaseAuth.getInstance().currentUser!!.uid
        val userRef : DatabaseReference = FirebaseDatabase.getInstance().reference.child("Users")

        val userMap = HashMap<String, Any>()
        userMap["uid"] = currentUserID
        userMap["fullname"] = fullNameString.lowercase()
        userMap["username"] = userNameString
        userMap["email"] = emailString
        userMap["bio"] = "Hey I am a professional cooker and I want to share with you some incredible dishes"
        userMap["image"] = if (imageUrl.isNotEmpty()) imageUrl else "https://firebasestorage.googleapis.com/v0/b/thecookingapp-4857b.appspot.com/o/Default%20Images%2FdefaultImageProfile.jpg?alt=media&token=d66fbe7f-effb-44f5-9174-d52c4d6d412f"

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