package com.example.thecookapp
import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.TextUtils
import com.squareup.picasso.Transformation
import android.widget.TextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thecookapp.Adapter.IngredientAdapter
import com.example.thecookapp.Adapter.IngredientItem
import com.example.thecookapp.Adapter.InstructionAdapter
import com.example.thecookapp.Adapter.StepItem
import com.example.thecookapp.ui.home.HomeFragment
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Collections
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.gson.Gson
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.await
import java.io.File

class AddPostActivity : AppCompatActivity() {
    // user id
    private lateinit var signInUser: FirebaseUser

    // gps location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latitude: Double? = null
    private var longitude: Double? = null

    // Ingredients variable
    private lateinit var ingredientAdapter: IngredientAdapter
    private lateinit var ingredientRecyclerView: RecyclerView
    private val ingredients = mutableListOf<IngredientItem>() // List to hold ingredient items
    private var isEditingIngredient = false

    // Instructions variable
    private lateinit var instructionAdapter: InstructionAdapter
    private lateinit var instructionRecyclerView: RecyclerView
    private val steps = mutableListOf<StepItem>() // List to hold steps items
    private var isEditingInstruction = false

    // Mofication post variable
    private var recipe_modify : Recipe? = null

    // Image variable
    private var imageUri: Uri? = null
    private lateinit var recipeImageView: ImageView
    private val getImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it // Initialize imageUri for gallery selection

            // Use Picasso to load and display the image in the ImageView
            Picasso.get()
                .load(imageUri)
                .fit() // Resize to fit ImageView dimensions
                .placeholder(R.drawable.plate_knife_fork) // Placeholder image
                .transform(RoundedCornersTransformation(22f, 12f)) // Radius = 22dp, Margin = 8dp
                .into(recipeImageView)

            Log.d("AccountSettings", "Gallery Image URI: $imageUri")
        } ?: run {
            Log.e("AccountSettings", "No image selected from gallery")
        }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess: Boolean ->
        if (isSuccess) {
            // Use Picasso to load and display the captured photo in the ImageView
            Picasso.get()
                .load(imageUri)
                .fit() // Resize to fit ImageView dimensions
                .placeholder(R.drawable.plate_knife_fork) // Placeholder image
                .transform(RoundedCornersTransformation(22f, 12f))
                .into(recipeImageView)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_post)

        // Set if there is the post to modify
        recipe_modify = intent.getParcelableExtra<Recipe>("RECIPE_DETAILS")

        recipeImageView = findViewById<ImageView>(R.id.recipeImage)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set up the location button
        val locationButton = findViewById<Button>(R.id.location_btn)
        locationButton.setOnClickListener {
            if (checkLocationPermission()) {
                fetchCurrentLocation()
            } else {
                requestLocationPermission()
            }
        }

        // Set up the back button
        val backButton = findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            // A QUANTO PARE COSì FUNZIONA BENE SE STO MODIFICANDO IL POST MA NON SE DEVO AGGIUNGERLO
            supportFragmentManager.beginTransaction().replace(
                R.id.fragment_container,
                HomeFragment()
            ).commit()
        }

        // Set up the Upload Recipe Image
        val uploadImageButton = findViewById<Button>(R.id.uploadButton)
        uploadImageButton.setOnClickListener {
            if (checkPermissions()) {
                openImagePicker()
            } else {
                requestPermissions()
            }
        }

        val postButton = findViewById<TextView>(R.id.next_button)
        signInUser = FirebaseAuth.getInstance().currentUser!!
        postButton.setOnClickListener{
            if (recipe_modify == null) {
                lifecycleScope.launch {
                    create_post()
                }
            } else {
                lifecycleScope.launch {
                    update_post(
                        post_id = recipe_modify!!.post_id,
                        currentImageUrl = recipe_modify!!.image_url
                    )
                }
            }
        }

        // Initialize RecyclerViews
        ingredientRecyclerView = findViewById(R.id.dynamic_ingredient_recycler_view)
        ingredientRecyclerView.layoutManager = LinearLayoutManager(this)

        instructionRecyclerView = findViewById(R.id.dynamic_instruction_recycler_view)
        instructionRecyclerView.layoutManager = LinearLayoutManager(this)

        // Set up adapter with initial data
        ingredientAdapter = IngredientAdapter(this, ingredients)
        ingredientRecyclerView.adapter = ingredientAdapter

        instructionAdapter = InstructionAdapter(this, steps)
        instructionRecyclerView.adapter = instructionAdapter

        // Enable drag-and-drop functionality for Ingredients
        val ingredientItemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition

                // Update the ingredients list
                if (fromPosition in ingredients.indices && toPosition in ingredients.indices) {
                    Collections.swap(ingredients, fromPosition, toPosition)
                    ingredientAdapter.notifyItemMoved(fromPosition, toPosition)
                }
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not used
            }
        })
        ingredientItemTouchHelper.attachToRecyclerView(ingredientRecyclerView)

        // Enable drag-and-drop functionality for Instructions
        val instructionItemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition

                // Save the current value of the EditText before moving
                val fromViewHolder = recyclerView.findViewHolderForAdapterPosition(fromPosition) as? InstructionAdapter.ViewHolder
                fromViewHolder?.let {
                    val currentText = it.descriptionEditText.text.toString()
                    steps[fromPosition].description = currentText
                }

                // Update the steps list
                if (fromPosition in steps.indices && toPosition in steps.indices) {
                    Log.e("StepItem", "Step moved: $steps, steps BEFORE Activity: $steps")
                    steps[fromPosition].number = toPosition
                    steps[toPosition].number = fromPosition
                    Collections.swap(steps, fromPosition, toPosition)
                    Log.e("StepItem", "Step moved: $steps, steps AFTER Activity: $steps")
                    instructionAdapter.notifyItemMoved(fromPosition, toPosition)
                    instructionAdapter.notifyItemRangeChanged(0, steps.size)

                }
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not used
            }
        })
        instructionItemTouchHelper.attachToRecyclerView(instructionRecyclerView)


        // Set up the Edit button
        val editButtonIngredient = findViewById<TextView>(R.id.edit_button_ingredients)
        editButtonIngredient.setOnClickListener {
            isEditingIngredient = !isEditingIngredient
            editButtonIngredient.text = if (isEditingIngredient) "Done" else "Edit"
            ingredientAdapter.toggleIconVisibility(isEditingIngredient, ingredientRecyclerView)
        }

        val editButtonInstruction = findViewById<TextView>(R.id.edit_button_instructions)
        editButtonInstruction.setOnClickListener {
            isEditingInstruction = !isEditingInstruction
            editButtonInstruction.text = if (isEditingInstruction) "Done" else "Edit"
            instructionAdapter.toggleIconVisibility(isEditingInstruction, instructionRecyclerView)
        }

        // Set up the Add Ingredient button
        val addIngredientButton = findViewById<Button>(R.id.add_ingredient_button)
        addIngredientButton.setOnClickListener {
            addIngredientItem("", "", isEditingIngredient)
        }

        // Set up the Add Step button
        val addStepButton = findViewById<Button>(R.id.add_step_button)
        addStepButton.setOnClickListener {
            addStepItem("", isEditingInstruction)
        }

        if(recipe_modify != null) {
            // if there is a recipe to modify populate the UI
            populateUI()
        }

        // Ensure at least one default item is present
        if (ingredients.isEmpty()) {
            addIngredientItem("", "", isEditingIngredient)
        }

        if (steps.isEmpty()) {
            addStepItem("", isEditingInstruction)
        }

    }

    private fun populateUI() {
        // Fill all Front-end with the correct info of the post
        findViewById<EditText>(R.id.titleInput).setText(recipe_modify?.title)
        findViewById<EditText>(R.id.aboutInput).setText(recipe_modify?.description)
        findViewById<EditText>(R.id.time_value).setText(recipe_modify?.time_to_do)
        findViewById<EditText>(R.id.difficult_value).setText(recipe_modify?.difficulty)
        findViewById<EditText>(R.id.servings_value).setText(recipe_modify?.servings)
        val locationInput = findViewById<EditText>(R.id.locationInput)
        locationInput.setText("Lat: ${recipe_modify?.latitude}\nLon: ${recipe_modify?.longitude}")

        // Set Image using Picasso
        Picasso.get()
            .load(recipe_modify?.image_url)
            .fit() // Resize to fit ImageView dimensions
            .placeholder(R.drawable.plate_knife_fork) // Placeholder image
            .transform(RoundedCornersTransformation(22f, 12f)) // Radius = 22dp, Margin = 8dp
            .into(recipeImageView)

        // Set the recyclerView with the ingredient of the recipe
        for ((ingredientName, ingredientAmount) in recipe_modify!!.ingredients) {
            addIngredientItem(ingredientName, ingredientAmount, isEditingIngredient)
        }
        // Set the recyclerView with the instruction of the recipe
        for (stepDescription in recipe_modify!!.instructions) {
            addStepItem(stepDescription, isEditingInstruction)
        }
    }

    private suspend fun update_post(post_id: Int, currentImageUrl: String) {
        // Function to update the post in the database
        val isConnected = ApiClient.checkServerConnection()
        if (!isConnected) {
            Log.e("API_ERROR", "Failed to connect to the Flask server")
            runOnUiThread {
                Toast.makeText(this@AddPostActivity, "Failed to connect to the server. Please try again.", Toast.LENGTH_LONG).show()
            }
            return
        }

        val user_id = signInUser.uid // Assuming `signInUser` is properly initialized

        // Fetch values from the front-end
        val instructions: List<String> = steps.filter { it.description.isNotBlank() }
            .map { it.description }
        val ingredients = ingredients.associate { it.ingredient to it.amount }
        val servings = findViewById<EditText>(R.id.servings_value).text.toString()
        val title = findViewById<EditText>(R.id.titleInput).text.toString()
        val description = findViewById<EditText>(R.id.aboutInput).text.toString()
        val difficulty = findViewById<EditText>(R.id.difficult_value).text.toString()
        val time = findViewById<EditText>(R.id.time_value).text.toString()

        // Validate inputs
        if (title.isEmpty() || description.isEmpty() || ingredients.isEmpty() || instructions.isEmpty()) {
            runOnUiThread {
                Toast.makeText(this@AddPostActivity, "All fields are required!", Toast.LENGTH_LONG).show()
            }
            return
        }

        // Handle image logic
        val imageUrl: String = if (imageUri == null) {
            // If no new image is uploaded, reuse the current image URL
            currentImageUrl
        } else {
            // Upload the new image and get its URL
            uploadImage(imageUri!!) ?: run {
                Log.e("Upload Image", "Failed to upload image")
                Toast.makeText(this@AddPostActivity, "Failed to upload image", Toast.LENGTH_LONG)
                    .show()
                return
            }
        }

        var locationLatitude: Double
        var locationLongitude: Double

        if (latitude == null && longitude == null) {
            locationLatitude = recipe_modify?.latitude!!
            locationLongitude = recipe_modify?.longitude!!
        } else {
            locationLatitude = latitude!!
            locationLongitude = longitude!!
        }

        // Create the updated recipe object
        val updatedRecipe = Recipe(
            user_id = user_id,
            post_id = post_id,
            title = title,
            description = description,
            ingredients = ingredients,
            instructions = instructions,
            image_url = imageUrl,
            difficulty = difficulty,
            servings = servings,
            time_to_do = time,
            latitude = locationLatitude,
            longitude = locationLongitude,
            created_at = "Set by SQL"
        )

        Log.e("AddpostActivity", "latitude is $locationLatitude and longitude is $locationLongitude")

        // Use the API to update the recipe
        ApiClient.recipeApi.updatePost(user_id, post_id, updatedRecipe).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Log.e("API_SUCCESS", "Recipe updated: ${response.body()}")
                    runOnUiThread {
                        Toast.makeText(this@AddPostActivity, "Post updated successfully!", Toast.LENGTH_LONG).show()
                        // Navigate to home page or another activity
                        startActivity(Intent(this@AddPostActivity, MainActivity::class.java))
                        finish() // Close the current activity
                    }
                } else {
                    Log.e("API_ERROR", "Error: ${response.errorBody()?.string()}")
                    runOnUiThread {
                        Toast.makeText(this@AddPostActivity, "Failed to update the post. Please try again.", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Log.e("API_ERROR", "Failed to update recipe", t)
                runOnUiThread {
                    Toast.makeText(this@AddPostActivity, "An error occurred. Please try again.", Toast.LENGTH_LONG).show()
                }
            }
        })
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

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 200
        )
    }

    private fun fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
            if (loc != null) {
                latitude = loc.latitude
                longitude = loc.longitude
                Log.d("Location", "Latitude: $latitude, Longitude: $longitude")

                // Display the coordinates
                val locationInput = findViewById<EditText>(R.id.locationInput)
                locationInput.setText("Lat: $latitude\nLon: $longitude")
            } else {
                Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Log.e("Location", "Failed to get location", exception)
            Toast.makeText(this, "Failed to fetch location", Toast.LENGTH_SHORT).show()
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }


    // Opens a dialog to select or capture an image from gallery
    private fun openImagePicker() {
        val options = arrayOf("Choose from Gallery", "Take a Photo")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Image")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> getImage.launch("image/*")  // Launch the gallery picker
                1 -> {
                    imageUri = createImageUri()  // Create a URI for the photo capture
                    takePhoto.launch(imageUri!!)   // Launch the camera to take a photo
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

    private fun addIngredientItem(ingredient_name: String, amount: String, isEditing: Boolean) {
        // Add a new item to the list and notify the adapter
        ingredients.add(IngredientItem(ingredient_name, amount, isEditing))
        ingredientAdapter.notifyItemInserted(ingredients.size - 1)
    }

    private fun addStepItem(step_description: String, isEditing: Boolean) {
        // Add a new item to the list and notify the adapter
        steps.add(StepItem(step_description, steps.size, isEditing))
        Log.e("StepItem", "Step added, size: ${steps.size}, steps ACTIVITY: $steps")
        instructionAdapter.notifyItemInserted(steps.size - 1)
    }

    private suspend fun create_post() {
        val isConnected = ApiClient.checkServerConnection()
        if (!isConnected) {
            Log.e("API_ERROR", "Failed to connect to the Flask server")
            runOnUiThread {
                Toast.makeText(this@AddPostActivity, "Failed to connect to the server. Please try again.", Toast.LENGTH_LONG).show()
            }
            return
        }

        val user_id = signInUser.uid // Assuming `signInUser` is properly initialized

        // Fetch values from the front-end
        val instructions: List<String> = steps.filter { it.description.isNotBlank() }
            .map { it.description }
        val ingredients = ingredients.associate { it.ingredient to it.amount }
        val servings = findViewById<EditText>(R.id.servings_value).text.toString()
        val title = findViewById<EditText>(R.id.titleInput).text.toString()
        val description = findViewById<EditText>(R.id.aboutInput).text.toString()
        val difficulty = findViewById<EditText>(R.id.difficult_value).text.toString()
        val time = findViewById<EditText>(R.id.time_value).text.toString()

        // Validate inputs
        if (title.isEmpty() || description.isEmpty() || ingredients.isEmpty() || instructions.isEmpty()) {
            runOnUiThread {
                Toast.makeText(this@AddPostActivity, "All fields are required!", Toast.LENGTH_LONG).show()
            }
            return
        }

        // Check if image is upload by user
        if (imageUri == null) {
            Log.e("Upload Image", "Image URI is null")
            runOnUiThread {
                Toast.makeText(this@AddPostActivity, "Image is required!", Toast.LENGTH_LONG).show()
            }
            return
        }

        // Create URL of image - saved on Flask server
        val imageUrl = uploadImage(imageUri!!) ?: run {
            Log.e("Upload Image", "Failed to upload image")
            Toast.makeText(this@AddPostActivity, "Failed to upload image", Toast.LENGTH_LONG)
                .show()
            return
        }

        val locationLatitude = latitude ?: 0.0
        val locationLongitude = longitude ?: 0.0

        ApiClient.recipeApi.getPostCount(user_id).enqueue(object : Callback<Int> {
            override fun onResponse(call: Call<Int>, response: Response<Int>) {
                if (response.isSuccessful) {
                    val post_id = (response.body() ?: 0) + 1 // Generate the next post ID

                    // Create the recipe object
                    val newRecipe = Recipe(
                        user_id = user_id,
                        post_id = post_id,
                        title = title,
                        description = description,
                        ingredients = ingredients,
                        instructions = instructions,
                        image_url = imageUrl,
                        difficulty = difficulty,
                        servings = servings,
                        time_to_do = time,
                        created_at = "SETTED BY SQL",
                        latitude = locationLatitude,
                        longitude = locationLongitude
                    )

                    // Use the API to add the recipe
                    ApiClient.recipeApi.addRecipe(newRecipe).enqueue(object : Callback<Map<String, Any>> {
                        override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                            if (response.isSuccessful) {
                                Log.e("API_SUCCESS", "Recipe added: ${response.body()}")
                                runOnUiThread {
                                    Toast.makeText(this@AddPostActivity, "Post uploaded successfully!", Toast.LENGTH_LONG).show()
                                    // Navigate to home page
                                    startActivity(Intent(this@AddPostActivity, MainActivity::class.java))
                                    finish() // Close the current activity
                                }
                            } else {
                                Log.e("API_ERROR", "Error: ${response.errorBody()?.string()}")
                                runOnUiThread {
                                    Toast.makeText(this@AddPostActivity, "Failed to upload the post. Please try again.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }

                        override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                            Log.e("API_ERROR", "Failed to add recipe", t)
                            runOnUiThread {
                                Toast.makeText(this@AddPostActivity, "An error occurred. Please try again.", Toast.LENGTH_LONG).show()
                            }
                        }
                    })
                } else {
                    Log.e("API_ERROR", "Server error: ${response.errorBody()?.string()}")
                    runOnUiThread {
                        Toast.makeText(this@AddPostActivity, "Failed to fetch post details. Please try again.", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<Int>, t: Throwable) {
                Log.e("API_FAILURE", "Failed to fetch post count: ${t.message}")
                runOnUiThread {
                    Toast.makeText(this@AddPostActivity, "An error occurred. Please try again.", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private suspend fun uploadImage(imageUri: Uri): String? {
        val file = getFileFromUri(this, imageUri)
        if (file == null) {
            Log.e("Upload Image", "Error creating temporary file from URI")
            return null
        }
        Log.e("Upload Image", "File path: ${file}")

        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        return try {
            val response = ApiClient.recipeApi.uploadImage(body)
            if (response.isSuccessful) {
                response.body()?.get("url")?.asString
            } else {
                Log.e("Upload Image", "Error uploading image: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("Upload Image", "Exception during image upload", e)
            null
        }
    }


    private fun getFileFromUri(context: Context, uri: Uri): File? {
        val contentResolver = context.contentResolver
        val fileName = getFileName(contentResolver, uri) ?: return null
        val tempFile = File(context.cacheDir, fileName)
        contentResolver.openInputStream(uri)?.use { inputStream ->
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return tempFile
    }

    private fun getFileName(contentResolver: ContentResolver, uri: Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && it.moveToFirst()) {
                return it.getString(nameIndex)
            }
        }
        return null
    }
}


class RoundedCornersTransformation(private val radius: Float, private val margin: Float) : Transformation {
    override fun transform(source: Bitmap): Bitmap {
        val paint = Paint().apply {
            isAntiAlias = true
            shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }

        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val rect = RectF(margin, margin, source.width - margin, source.height - margin)
        canvas.drawRoundRect(rect, radius, radius, paint)

        source.recycle()
        return output
    }

    override fun key(): String {
        return "rounded_corners(radius=$radius, margin=$margin)"
    }
}

