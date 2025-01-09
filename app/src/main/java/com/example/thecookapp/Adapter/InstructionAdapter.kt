package com.example.thecookapp.Adapter

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thecookapp.R

data class StepItem(
    var description: String = "",
    var number : Int = 0,
    var isEditMode: Boolean = false // Toggle for showing/hiding move/delete icons
)

class InstructionAdapter (
    private val context: Context,
    private val steps: MutableList<StepItem>
) : RecyclerView.Adapter<InstructionAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val stepNumText: TextView = view.findViewById(R.id.step_text)
        val descriptionEditText: EditText = view.findViewById(R.id.step_edit_text_description)
        val moveButton: ImageView = view.findViewById(R.id.moving_button_step)
        val deleteButton: ImageView = view.findViewById(R.id.delete_button_step)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.step_item_template, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.e("StepItem", "Called OnBindViewHolder, list is: $steps")
        val stepItem = steps[position]
        Log.e("StepItem", "Current step position: ${holder.adapterPosition + 1}")
        Log.e("StepItem", "Current step position saved: ${stepItem.number+1}")

        holder.stepNumText.text = " "
        holder.stepNumText.text = "Step ${stepItem.number +1}"
        holder.descriptionEditText.setText(stepItem.description)

        holder.descriptionEditText.clearFocus() // Prevent focus issues during recycling

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val adapterPosition = holder.adapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    steps[adapterPosition].description = s.toString()
                    Log.e("StepItem", "Change of the text: ${steps[adapterPosition]}, steps: $steps")
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        holder.descriptionEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                holder.descriptionEditText.addTextChangedListener(textWatcher)
            } else {
                holder.descriptionEditText.removeTextChangedListener(textWatcher)
            }
        }

        // Toggle visibility of icons based on edit mode
        holder.moveButton.visibility = if (stepItem.isEditMode) View.GONE else View.VISIBLE
        holder.deleteButton.visibility = if (stepItem.isEditMode) View.VISIBLE else View.GONE

        // Delete item on delete icon click
        holder.deleteButton.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) { // Verify if the position is valid
                removeItemAt(currentPosition)
            }
        }
    }

    override fun getItemCount(): Int = steps.size

    private fun removeItemAt(position: Int) {
        if (position in steps.indices) {
            steps.removeAt(position)
            for (i in position until steps.size) {
                steps[i].number -= 1
            }
            Log.e("StepItem", "Removed item at position $position, steps: $steps")
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, steps.size) // Ensure proper updates
        }
    }

    // Update all items for edit mode toggle
    fun toggleIconVisibility(isEditMode: Boolean, recyclerView: RecyclerView) {
        for (i in 0 until recyclerView.childCount) {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(i) as? ViewHolder
            viewHolder?.let {
                it.moveButton.visibility = if (isEditMode) View.GONE else View.VISIBLE
                it.deleteButton.visibility = if (isEditMode) View.VISIBLE else View.GONE
            }
            steps[i].isEditMode = isEditMode
        }
    }

}