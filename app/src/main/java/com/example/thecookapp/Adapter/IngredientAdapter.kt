package com.example.thecookapp.Adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.thecookapp.R

data class IngredientItem(
    var ingredient: String = "",
    var amount: String = "",
    var isEditMode: Boolean = false // Toggle for showing/hiding move/delete icons
)

class IngredientAdapter(
    private val context: Context,
    private val ingredients: MutableList<IngredientItem>
) : RecyclerView.Adapter<IngredientAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ingredientEditText: EditText = view.findViewById(R.id.ingredient_input)
        val amountEditText: EditText = view.findViewById(R.id.amount_input)
        val moveIcon: ImageView = view.findViewById(R.id.moving_icon)
        val deleteIcon: ImageView = view.findViewById(R.id.delete_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.ingredient_item_template, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ingredient = ingredients[position]

        // Set initial values for EditText
        holder.ingredientEditText.setText(ingredient.ingredient)
        holder.amountEditText.setText(ingredient.amount)

        // Clear previous listeners to avoid duplicate callbacks
        holder.ingredientEditText.setOnFocusChangeListener(null)
        holder.amountEditText.setOnFocusChangeListener(null)

        // Update the data model when the focus is lost
        holder.ingredientEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                Log.e("Move-Remove", "Before ingredient: ${ingredient.ingredient}")
                ingredient.ingredient = holder.ingredientEditText.text.toString()
                Log.e("Move-Remove", "Updated ingredient: ${ingredient.ingredient}, ingredients: $ingredients")
            }
        }

        holder.amountEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                ingredient.amount = holder.amountEditText.text.toString()
            }
        }

        // Toggle visibility of icons based on edit mode
        holder.moveIcon.visibility = if (ingredient.isEditMode) View.GONE else View.VISIBLE
        holder.deleteIcon.visibility = if (ingredient.isEditMode) View.VISIBLE else View.GONE

        // Delete item on delete icon click
        holder.deleteIcon.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) { // Verify if the position is valid
                removeItemAt(currentPosition)
            }
        }
    }

    override fun getItemCount(): Int = ingredients.size

    private fun removeItemAt(position: Int) {
        if (position in ingredients.indices) {
            ingredients.removeAt(position)
            Log.e("Move-Remove", "Removed item at position $position, ingredients: $ingredients")
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, ingredients.size) // Ensure proper updates
        }
    }

    // Update all items for edit mode toggle
    fun toggleIconVisibility(isEditMode: Boolean, recyclerView: RecyclerView) {
        for (i in 0 until recyclerView.childCount) {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(i) as? ViewHolder
            viewHolder?.let {
                it.moveIcon.visibility = if (isEditMode) View.GONE else View.VISIBLE
                it.deleteIcon.visibility = if (isEditMode) View.VISIBLE else View.GONE
            }
            ingredients[i].isEditMode = isEditMode
        }
    }
}
