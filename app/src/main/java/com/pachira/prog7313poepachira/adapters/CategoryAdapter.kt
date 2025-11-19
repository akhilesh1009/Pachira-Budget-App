package com.pachira.prog7313poepachira.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.pachira.prog7313poepachira.R
import com.pachira.prog7313poepachira.data.Category

/**
 * RecyclerView Adapter to display a list of categories with icon and color,
 * and allow the user to select a category.
 */

//(Android Developer 2025)
class CategoryAdapter(
    private val categories: List<Category>, // List of categories to display
    private val onCategorySelected: (Category) -> Unit // Callback when a category is selected
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    // Stores the ID of the currently selected category
    private var selectedCategoryId: String? = null

    // ViewHolder class to hold and bind UI components for each category item
    //(Android Developer 2025)
    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardCategory)
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val iconView: ImageView = itemView.findViewById(R.id.ivCategoryIcon)

        // Binds a Category object to the views
        fun bind(category: Category) {
            tvCategoryName.text = category.name // Set category name

            // Set card background color using hex string from category
            try {
                cardView.setCardBackgroundColor(Color.parseColor(category.colorHex))
            } catch (e: Exception) {
                // Fallback color if parsing fails
                cardView.setCardBackgroundColor(Color.parseColor("#3F51B5"))
            }

            // Set category icon from drawable resource
            val resourceId = itemView.context.resources.getIdentifier(
                category.iconName, "drawable", itemView.context.packageName
            )
            if (resourceId != 0) {
                iconView.setImageResource(resourceId)
                iconView.visibility = View.VISIBLE
            } else {
                // Fallback icon if resource is not found
                iconView.setImageResource(R.drawable.ic_default_icon)
            }

            // Highlight the selected category with a stroke
            val isSelected = category.id == selectedCategoryId
            cardView.strokeWidth = if (isSelected) 4 else 0

            // Handle item click: update selection and notify listener
            itemView.setOnClickListener {
                selectedCategoryId = category.id
                onCategorySelected(category)
                notifyDataSetChanged() // Refresh to update UI selection state
            }
        }
    }

    // Inflate the category item layout and return the ViewHolder
    //(Android Developer 2025)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    // Bind category data to the ViewHolder at the given position
    //(Android Developer 2025)
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    // Return the total number of category items
    //(Android Developer 2025)
    override fun getItemCount(): Int = categories.size

    // Public method to programmatically set selected category and refresh UI
    //(Android Developer 2025)
    fun setSelectedCategory(categoryId: String) {
        selectedCategoryId = categoryId
        notifyDataSetChanged()
    }
}


//REFERENCES
// Android Developer. 2025. “Create Dynamic Lists with RecyclerView”. 2025
// <https://developer.android.com/develop/ui/views/layout/recyclerview>
// [accessed 30 April 2025].