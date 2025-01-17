package com.example.thecookapp

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
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
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import com.example.thecookapp.R.id.edit_profile_button
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class EditProfileActivity : AppCompatActivity() {
    private lateinit var profileImageView: CircleImageView
    private lateinit var changeImageButton: TextView
    private lateinit var imageUri: Uri
    private lateinit var signInUser: FirebaseUser
    private val getImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it // Initialize imageUri for gallery selection
            // Use Picasso to load and display the image in the ImageView
            Picasso.get()
                .load(imageUri)
                .fit() // Resize to fit ImageView dimensions
                .into(profileImageView)
            Log.d("AccountSettings", "Gallery Image URI: $imageUri")
        } ?: run {
            Log.e("AccountSettings", "No image selected from gallery")
        }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess: Boolean ->
        if (isSuccess) {
            Picasso.get()
                .load(imageUri)
                .fit() // Resize to fit ImageView dimensions
                .centerCrop() // Crop to maintain aspect ratio
                .into(profileImageView)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

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

        // Done button functionality
        val doneButton = findViewById<TextView>(R.id.done_button)
        val bio = findViewById<EditText>(R.id.accountSettings_editBio)
        val fullName = findViewById<EditText>(R.id.accountSettings_editTextName)
        val userName = findViewById<EditText>(R.id.accountSettings_editUsername)
        val updates = HashMap<String, Any>()
        val changePassButton = findViewById<Button>(R.id.change_password_button)
        val backButton = findViewById<ImageButton>(R.id.back_edit_button)
        val deleteButton = findViewById<Button>(R.id.delete_account_button)

        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        deleteButton.setOnClickListener{
            val progressDialog = ProgressDialog(this)
            progressDialog.setCancelable(false)
            deleteUserAccount(progressDialog)
        }

        changePassButton.setOnClickListener {
            startActivity( Intent( this, ChangePasswordActivity::class.java))
        }

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

    private fun deleteUserAccount(progressDialog: ProgressDialog) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            // Show progress dialog
            progressDialog.setMessage("Deleting your account...")
            progressDialog.show()

            val currentUserID = currentUser.uid
            val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(currentUserID)

            userRef.removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //delete the user from Follow tree
                    deleteUserFromFollowLists(currentUserID)

                    //delete the user from Firebase
                    currentUser.delete().addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            // Successfully deleted the user
                            progressDialog.dismiss()
                            Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_LONG).show()

                            // Redirect to a screen or log out user
                            val intent = Intent(this, SignInActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            // Handle error while deleting user from Auth
                            progressDialog.dismiss()
                            val message = deleteTask.exception?.message ?: "Unknown error"
                            Toast.makeText(this, "Error deleting account: $message", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    // Handle error while removing data from Realtime Database
                    progressDialog.dismiss()
                    val message = task.exception?.message ?: "Unknown error"
                    Toast.makeText(this, "Error deleting account data: $message", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(this, "No user is currently signed in", Toast.LENGTH_LONG).show()
        }
    }

    private fun deleteUserFromFollowLists(userId: String) {
        val followRef = FirebaseDatabase.getInstance().reference.child("Follow")

        // Remove the user ID from Follow
        val userFollowRef = followRef.child(userId)
        userFollowRef.removeValue()

        followRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Iterate over each user in the Follow node
                for (userSnapshot in snapshot.children) {
                    Log.d("Follow", "User ID: $userId")
                    val currentId = userSnapshot.key ?: ""
                    // Iterate over the Followers of the current user
                    val followersRef = followRef.child(currentId).child("Followers").child(userId)
                    followersRef.removeValue()

                    val followingRef = followRef.child(currentId).child("Following").child(userId)
                    followingRef.removeValue()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error if necessary
                Log.e("Follow", "Error fetching follow data: ${error.message}")
            }
        })
    }

    private fun updateUserProfileInDatabase(updates: HashMap<String, Any>) {
        val userId = signInUser.uid
        FirebaseDatabase.getInstance().reference
            .child("Users")
            .child(userId)
            .updateChildren(updates)
            .addOnSuccessListener {
                //Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
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
                    Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.default_image_profile) // Replace with your placeholder image
                        .error(R.drawable.default_image_profile) // Fallback in case of an error
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
