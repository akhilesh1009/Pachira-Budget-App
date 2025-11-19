package com.pachira.prog7313poepachira.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.pachira.prog7313poepachira.R
import com.pachira.prog7313poepachira.data.CategorySummary
import java.text.NumberFormat
import java.util.*

/**
 * Adapter to display a summary of categories with spending data,
 * including progress toward budget limits and edit options.
 */
//(Android Developer 2025)
class CategorySummaryAdapter(
    private val categorySummaries: List<CategorySummary>, // List of category summary data
    private val onItemClick: (CategorySummary) -> Unit,    // Callback for item click
    private val onEditClick: (CategorySummary) -> Unit     // Callback for edit button click
) : RecyclerView.Adapter<CategorySummaryAdapter.CategorySummaryViewHolder>() {

    // ViewHolder to hold UI components for each item
    //(Android Developer 2025)
    class CategorySummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        val tvPercentage: TextView = itemView.findViewById(R.id.tvPercentage)
        val tvLimit: TextView = itemView.findViewById(R.id.tvLimit)
        val btnEditCategory: ImageButton = itemView.findViewById(R.id.btnEditCategory)
        val categoryColorIndicator: View = itemView.findViewById(R.id.categoryColorIndicator)
    }

    //(Android Developer 2025)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategorySummaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_summary, parent, false)
        return CategorySummaryViewHolder(view)
    }
    //(Android Developer 2025)
    override fun onBindViewHolder(holder: CategorySummaryViewHolder, position: Int) {
        val categorySummary = categorySummaries[position]

        // Format currency for South African locale
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        // Bind text data
        holder.tvCategoryName.text = categorySummary.name
        holder.tvAmount.text = format.format(categorySummary.amount)
        holder.tvLimit.text = "Limit: ${format.format(categorySummary.limit)}"

        // Set percentage only if greater than 0
        holder.tvPercentage.text = if (categorySummary.percentage > 0) "${categorySummary.percentage}%" else ""

        // Set progress bar
        holder.progressBar.progress = categorySummary.percentage

        // Hide limit if not set
        if (categorySummary.limit <= 0) {
            holder.tvLimit.visibility = View.GONE
        }

        // Set category color indicator based on category name
        setCategoryColor(holder, categorySummary)

        // Remove any background from the icon to prevent blue box
        holder.ivCategoryIcon.background = null

        // Load icon or show default if icon is unavailable
        if (!categorySummary.iconName.isNullOrEmpty()) {
            try {
                // Choose icon based on common category names
                val iconResourceId = when (categorySummary.name.lowercase(Locale.ROOT)) {
                    "food" -> R.drawable.ic_category_food
                    "housing", "home" -> R.drawable.ic_category_home
                    "transport", "transportation" -> R.drawable.ic_category_transport
                    "shopping" -> R.drawable.ic_category_shopping
                    "entertainment" -> R.drawable.ic_category_entertainment
                    "health" -> R.drawable.ic_category_health
                    "salary" -> R.drawable.ic_category_salary
                    "gift" -> R.drawable.ic_category_gift
                    "investment" -> R.drawable.ic_category_investment
                    "education" -> R.drawable.ic_category_education
                    else -> {
                        // Fall back to dynamic icon name resolution
                        holder.itemView.context.resources.getIdentifier(
                            categorySummary.iconName, "drawable", holder.itemView.context.packageName)
                    }
                }

                if (iconResourceId != 0) {
                    holder.ivCategoryIcon.setImageResource(iconResourceId)
                } else {
                    holder.ivCategoryIcon.setImageResource(R.drawable.ic_category_default)
                }
            } catch (e: Exception) {
                holder.ivCategoryIcon.setImageResource(R.drawable.ic_category_default)
            }
        } else {
            holder.ivCategoryIcon.setImageResource(R.drawable.ic_category_default)
        }

        // Change amount text color and card outline based on usage
        val cardView = holder.itemView as? MaterialCardView
        if (categorySummary.limit > 0) {
            when {
                categorySummary.amount > categorySummary.limit -> {
                    // Over budget: Red
                    holder.tvAmount.setTextColor(Color.parseColor("#E53935"))
                    cardView?.strokeWidth = 6
                    cardView?.strokeColor = Color.parseColor("#E53935")
                }
                categorySummary.percentage >= 80 -> {
                    // Near budget: Amber
                    holder.tvAmount.setTextColor(Color.parseColor("#FFC107"))
                    cardView?.strokeWidth = 3
                    cardView?.strokeColor = Color.parseColor("#FFC107")
                }
                else -> {
                    // Under budget: Default theme color
                    holder.tvAmount.setTextColor(holder.itemView.context.getColor(R.color.purple))
                    cardView?.strokeWidth = 0
                }
            }
        } else {
            // No budget limit set
            holder.tvAmount.setTextColor(holder.itemView.context.getColor(R.color.purple))
            cardView?.strokeWidth = 0
        }

        // Set click listeners
        holder.itemView.setOnClickListener { onItemClick(categorySummary) }
        holder.btnEditCategory.setOnClickListener { onEditClick(categorySummary) }
    }

    // Sets the category color indicator based on category name (similar to TransactionAdapter)
    private fun setCategoryColor(holder: CategorySummaryViewHolder, categorySummary: CategorySummary) {
        val color = getDefaultCategoryColor(holder, categorySummary.name)
        holder.categoryColorIndicator.setBackgroundColor(color)
    }

    // Gets default colors for categories using color resource IDs (similar to TransactionAdapter)
    private fun getDefaultCategoryColor(holder: CategorySummaryViewHolder, categoryName: String): Int {
        val context = holder.itemView.context

        return when (categoryName.lowercase().trim()) {
            // Food & Dining
            "food", "dining", "restaurant", "groceries", "food & dining", "meals" ->
                ContextCompat.getColor(context, R.color.category_food)

            // Transportation
            "transport", "transportation", "fuel", "car", "taxi", "uber", "petrol" ->
                ContextCompat.getColor(context, R.color.category_transport)

            // Shopping
            "shopping", "clothes", "retail", "clothing", "electronics" ->
                ContextCompat.getColor(context, R.color.category_shopping)

            // Health & Medical
            "health", "medical", "pharmacy", "healthcare", "doctor", "medicine" ->
                ContextCompat.getColor(context, R.color.category_healthcare)

            // Education
            "education", "school", "books", "learning", "tuition" ->
                ContextCompat.getColor(context, R.color.category_education)

            // Housing & Home
            "rent", "housing", "mortgage", "home", "utilities" ->
                ContextCompat.getColor(context, R.color.category_home)

            // Savings & Investment
            "savings", "investment", "retirement" ->
                ContextCompat.getColor(context, R.color.category_investment)

            // Entertainment
            "entertainment", "movies", "games", "music" ->
                ContextCompat.getColor(context, R.color.category_entertainment)

            // Bills & Utilities
            "bills", "utilities", "phone", "insurance" ->
                ContextCompat.getColor(context, R.color.category_bills)

            // Income categories
            "salary", "wage", "income", "bonus", "freelance" ->
                ContextCompat.getColor(context, R.color.category_salary)

            // Gift category
            "gift", "gifts", "present" ->
                ContextCompat.getColor(context, R.color.category_gift)

            // Travel category
            "travel", "vacation", "trip", "holiday" ->
                ContextCompat.getColor(context, R.color.category_travel)

            // Car category
            "car", "vehicle", "auto" ->
                ContextCompat.getColor(context, R.color.category_car)

            // Default fallback
            else -> ContextCompat.getColor(context, R.color.purple)
        }
    }

    //(Android Developer 2025)
    override fun getItemCount(): Int = categorySummaries.size
}
//REFERENCES
// Android Developer. 2025. "Create Dynamic Lists with RecyclerView". 2025
// <https://developer.android.com/develop/ui/views/layout/recyclerview>
// [accessed 30 April 2025].