package com.example.thecookapp

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.example.thecookapp.R
import com.example.thecookapp.ui.profile.ProfileFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import de.hdodenhof.circleimageview.CircleImageView

class AccountSettingsActivity : AppCompatActivity() {
    private lateinit var profileImageView: CircleImageView
    private lateinit var changeImageButton: TextView
    private lateinit var imageUri: Uri
    private lateinit var signInUser: FirebaseUser
    private val getImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it // Initialize imageUri for gallery selection
            profileImageView.setImageURI(uri) // Display the selected image
            Log.d("AccountSettings", "Gallery Image URI: $imageUri")
        } ?: run {
            Log.e("AccountSettings", "No image selected from gallery")
        }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess: Boolean ->
        if (isSuccess) {
            profileImageView.setImageURI(imageUri)  // Set the image URI after a successful photo capture
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_setting)

        // Profile image and change image button
        profileImageView = findViewById(R.id.accountSettings_image_profile)
        changeImageButton = findViewById(R.id.accountSettings_change_image)
        signInUser = FirebaseAuth.getInstance().currentUser!!

        // Load user data into fields
        loadUserData()

        changeImageButton.setOnClickListener {
            if (checkPermissions()) {
                openImagePicker()
            } else {
                requestPermissions()
            }
        }

        // Log out functionality
        val logoutBtn = findViewById<Button>(R.id.accountSettings_logoutBtn)
        logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this@AccountSettingsActivity, SignInActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        // Done button functionality
        val doneButton = findViewById<TextView>(R.id.done_button)
        val bio = findViewById<EditText>(R.id.accountSettings_editBio)
        val fullName = findViewById<EditText>(R.id.accountSettings_editTextName)
        val userName = findViewById<EditText>(R.id.accountSettings_editUsername)
        val updates = HashMap<String, Any>()

        doneButton.setOnClickListener {
            updates["fullname"] = fullName.text.toString().lowercase()
            updates["username"] = userName.text.toString()
            updates["bio"] = bio.text.toString()
            if (::imageUri.isInitialized) {
                uploadImageToFirebase(imageUri)
                val storageReference = FirebaseStorage.getInstance().reference
                val imageRef = storageReference.child("profile_images/${System.currentTimeMillis()}.jpg")

                imageRef.putFile(imageUri)
                    .addOnSuccessListener {
                        Log.d("Pasqualo", "Image uploaded successfully")
                        imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                            Log.e("Pasqualo", "Download URL: $downloadUrl")
                            Toast.makeText(this, "Download URL: $downloadUrl", Toast.LENGTH_SHORT).show()
                            updates["image"] = downloadUrl.toString()

                            // Update user data in Firebase Database
                            updateUserProfileInDatabase(updates)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Pasqualo", "Image upload failed: ${e.message}")
                        Toast.makeText(this, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                updateUserProfileInDatabase(updates)
            }
        }
    }

    private fun updateUserProfileInDatabase(updates: HashMap<String, Any>) {
        val userId = signInUser.uid
        FirebaseDatabase.getInstance().reference
            .child("Users")
            .child(userId)
            .updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                // Pass data to signal that ProfileFragment should be shown
                intent.putExtra("SHOW_PROFILE_FRAGMENT", true)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("AccountSettings", "Database update failed: ${e.message}")
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
            100
        )
    }

    private fun openImagePicker() {
        val options = arrayOf("Choose from Gallery", "Take a Photo")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Image")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> getImage.launch("image/*")  // Launch the gallery picker
                1 -> {
                    imageUri = createImageUri()  // Create a URI for the photo capture
                    takePhoto.launch(imageUri)   // Launch the camera to take a photo
                }
            }
        }
        builder.show()
    }

    private fun createImageUri(): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "New Image")
            put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
        }
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)!!
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker()
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadImageToFirebase(uri: Uri) {
        val storageReference: StorageReference = FirebaseStorage.getInstance().reference
        val imageRef = storageReference.child("profile_images/${System.currentTimeMillis()}.jpg")

        imageRef.putFile(uri)
            .addOnSuccessListener {
                // Image uploaded successfully
                Toast.makeText(this, "Image uploaded to Firebase", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Failed to upload image
                Toast.makeText(this, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserData() {
        val userId = signInUser.uid
        val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(userId)

        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val fullName = snapshot.child("fullname").value.toString()
                val userName = snapshot.child("username").value.toString()
                val bio = snapshot.child("bio").value.toString()
                val imageUrl = snapshot.child("image").value.toString()

                // Populate the EditTexts
                findViewById<EditText>(R.id.accountSettings_editTextName).setText(fullName)
                findViewById<EditText>(R.id.accountSettings_editUsername).setText(userName)
                findViewById<EditText>(R.id.accountSettings_editBio).setText(bio)

                // Load profile image if available
                if (imageUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.default_image_profile) // Replace with your placeholder image
                        .into(profileImageView)
                }
            } else {
                Log.e("AccountSettings", "User data not found")
            }
        }.addOnFailureListener { e ->
            Log.e("AccountSettings", "Failed to fetch user data: ${e.message}")
            Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show()
        }
    }

}
