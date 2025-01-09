package com.example.thecookapp
import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ContentValues
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
import kotlinx.coroutines.launch
import retrofit2.await

class AddPostActivity : AppCompatActivity() {

    // user id
    private lateinit var signInUser: FirebaseUser

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

    // Image variable
    private lateinit var imageUri: Uri
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
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_post)

        recipeImageView = findViewById<ImageView>(R.id.recipeImage)

        // Set up the back button
        val backButton = findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
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
            lifecycleScope.launch {
                create_post()
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
                    Collections.swap(steps, fromPosition, toPosition)
                    Log.e("StepItem", "Step moved: $steps, steps: $steps")
                    instructionAdapter.notifyItemMoved(fromPosition, toPosition)
                    instructionAdapter.updateStepNumbers() // Update step numbers

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
            addIngredientItem()
        }

        // Set up the Add Step button
        val addStepButton = findViewById<Button>(R.id.add_step_button)
        addStepButton.setOnClickListener {
            Log.d("StepItem", "Button clicked")
            addStepItem()
        }

        // Ensure at least one default item is present
        if (ingredients.isEmpty()) {
            addIngredientItem()
        }

        if (steps.isEmpty()) {
            addStepItem()
        }

        Log.d("StepItem", "onCreate called")

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

    private fun addIngredientItem() {
        // Add a new item to the list and notify the adapter
        ingredients.add(IngredientItem("", "", isEditingIngredient))
        ingredientAdapter.notifyItemInserted(ingredients.size - 1)
    }

    private fun addStepItem() {
        // Add a new item to the list and notify the adapter
        val stepDescription = "step"

        // Only add the step if the description is not empty
        if (stepDescription.isNotBlank()) {
            steps.add(StepItem(stepDescription, isEditingInstruction))
            Log.e("StepItem", "Step added, size: ${steps.size}, steps: $steps")
            instructionAdapter.notifyItemInserted(steps.size - 1)
        } else {
            Log.e("StepItem", "Attempted to add an empty step, skipping.")
        }
    }

    private suspend fun create_post() {
        val user_id = signInUser.uid // Assuming `signInUser` is properly initialized

        val isConnected = ApiClient.checkServerConnection()
        if (isConnected) {
            Log.e("API_SUCCESS", "Successfully connected to the Flask server ")
        } else {
            Log.e("API_ERROR", "Failed to connect to the Flask server")
            // MAYBE ADD SOMETHING TO COMUNICATE IN THE APP THE ERROR
        }


        ApiClient.recipeApi.getPostCount(user_id).enqueue(object : Callback<Int> {
            override fun onResponse(call: Call<Int>, response: Response<Int>) {
                Log.e("API_SUCCESS", "Response received")
                if (response.isSuccessful) {
                    val post_id = (response.body() ?: 0) + 1 // Generate the next post ID
                    Log.e("API_SUCCESS", "Next Post ID: $post_id")

                    // Check if steps are properly populated
                    Log.d("Steps", "Steps list before mapping: $steps")

                    // Fetch values from the front-end
                    val instructions: List<String> = steps.filter { it.description.isNotBlank() }
                        .map { it.description }

                    // Log the instructions list for debugging
                    Log.d("Instructions", "Instructions list: $instructions")

                    val ingredients = ingredients.associate { it.ingredient to it.amount }
                    val servings = findViewById<EditText>(R.id.servings_value).text.toString()
                    val title = findViewById<EditText>(R.id.titleInput).text.toString()
                    val description = findViewById<EditText>(R.id.aboutInput).text.toString()
                    val difficulty = findViewById<EditText>(R.id.difficult_value).text.toString()
                    val time = findViewById<EditText>(R.id.time_value).text.toString()

                    // Validate inputs
                    when {
                        title.isEmpty() -> Toast.makeText(this@AddPostActivity, "Title is required!", Toast.LENGTH_LONG).show()
                        description.isEmpty() -> Toast.makeText(this@AddPostActivity, "Description is required!", Toast.LENGTH_LONG).show()
                        //ingredients.isEmpty() -> Toast.makeText(this@AddPostActivity, "Ingredients are required!", Toast.LENGTH_LONG).show()
                        //instructions.isEmpty() -> Toast.makeText(this@AddPostActivity, "Instructions are required!", Toast.LENGTH_LONG).show()
                        else -> {
                            // Create the recipe object
                            val newRecipe = Recipe(
                                user_id = user_id,
                                post_id = post_id,
                                title = title,
                                description = description,
                                ingredients = ingredients,
                                instructions = instructions,
                                image_url = imageUri.toString(),
                                difficulty = difficulty,
                                servings = servings,
                                time_to_do = time,
                                created_at = "SETTED BY SQL"
                            )

                            // Use the API to add the recipe
                            ApiClient.recipeApi.addRecipe(newRecipe).enqueue(object : Callback<Map<String, Any>> {
                                override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                                    if (response.isSuccessful) {
                                        Log.e("API_ERROR", "Recipe added: ${response.body()}")
                                    } else {
                                        Log.e("API_ERROR", "Error: ${response.errorBody()?.string()}")
                                    }
                                }

                                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                                    Log.e("API_ERROR", "Failed to add recipe", t)
                                }
                            })
                        }
                    }
                } else {
                    Log.e("API_ERROR", "Server error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Int>, t: Throwable) {
                Log.e("API_FAILURE", "Failed to fetch post count: ${t.message}")
            }
        })
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

