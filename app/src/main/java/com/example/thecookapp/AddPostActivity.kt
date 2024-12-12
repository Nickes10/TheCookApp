package com.example.thecookapp
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thecookapp.Adapter.IngredientAdapter
import com.example.thecookapp.Adapter.IngredientItem
import java.util.Collections

class AddPostActivity : AppCompatActivity() {

    private lateinit var ingredientAdapter: IngredientAdapter
    private lateinit var ingredientRecyclerView: RecyclerView
    private val ingredients = mutableListOf<IngredientItem>() // List to hold ingredient items
    private var isEditing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_post)

        // Initialize RecyclerView
        ingredientRecyclerView = findViewById(R.id.dynamic_ingredient_recycler_view)
        ingredientRecyclerView.layoutManager = LinearLayoutManager(this)

        // Set up adapter with initial data
        ingredientAdapter = IngredientAdapter(this, ingredients)
        ingredientRecyclerView.adapter = ingredientAdapter

        // Enable drag-and-drop functionality
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
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
                    Log.e("Move-Remove", "Moving from $fromPosition to $toPosition, ingredients: $ingredients")
                    ingredientAdapter.notifyItemMoved(fromPosition, toPosition)
                }
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not used
            }
        })
        itemTouchHelper.attachToRecyclerView(ingredientRecyclerView)


        // Set up the Edit button
        val editButton = findViewById<TextView>(R.id.edit_button_ingredients)
        editButton.setOnClickListener {
            isEditing = !isEditing
            editButton.text = if (isEditing) "Done" else "Edit"
            ingredientAdapter.toggleIconVisibility(isEditing, ingredientRecyclerView)
        }

        // Set up the Add Ingredient button
        val addIngredientButton = findViewById<Button>(R.id.add_ingredient_button)
        addIngredientButton.setOnClickListener {
            addIngredientItem()
        }

        // Ensure at least one default item is present
        if (ingredients.isEmpty()) {
            addIngredientItem()
        }
    }

    private fun addIngredientItem() {
        // Add a new item to the list and notify the adapter
        ingredients.add(IngredientItem("", "", isEditing))
        ingredientAdapter.notifyItemInserted(ingredients.size - 1)
    }
}
