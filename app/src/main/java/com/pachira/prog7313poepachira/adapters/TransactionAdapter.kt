package com.pachira.prog7313poepachira.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pachira.prog7313poepachira.utility.ImageUtils
import com.pachira.prog7313poepachira.R
import com.pachira.prog7313poepachira.data.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import com.pachira.prog7313poepachira.TransactionDetailsDialog
import androidx.core.content.ContextCompat

//(Android Developer 2025)
class TransactionAdapter(
    private var transactions: MutableList<Transaction>,               // List of transactions to display
    private val onItemClick: (Transaction) -> Unit                    // Callback when an item is clicked
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    // ViewHolder class holds views for each transaction item
    //(Android Developer 2025)
    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)           // Displays amount
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)       // Displays category
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription) // Displays description
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)               // Displays date
        val ivReceipt: ImageView = itemView.findViewById(R.id.ivReceipt)        // Displays receipt image
        val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon) // Displays category icon
        val categoryColorIndicator: View = itemView.findViewById(R.id.categoryColorIndicator) // Category color indicator
    }

    // Inflates the layout for each item in the RecyclerView
    //(Android Developer 2025)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    // Binds data to each item view
    //(Android Developer 2025)
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]

        // Format the amount to ZAR currency
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        val amountText = if (transaction.type == "expense") {
            "-${format.format(transaction.amount)}"                      // Show minus for expenses
        } else {
            format.format(transaction.amount)                            // No minus for income
        }

        // Format the date to a readable format
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val dateText = dateFormat.format(Date(transaction.date))

        // Bind values to UI elements
        holder.tvAmount.text = amountText
        holder.tvCategory.text = transaction.category
        holder.tvDescription.text = transaction.description
        holder.tvDate.text = dateText

        // Set color depending on transaction type (income or expense)
        val textColor = if (transaction.type == "expense") {
            holder.itemView.context.getColor(R.color.expense_red)
        } else {
            holder.itemView.context.getColor(R.color.income_green)
        }
        holder.tvAmount.setTextColor(textColor)

        // Set category icon based on transaction.category field
        setCategoryIcon(holder, transaction.category)

        // Set category color indicator based on transaction type and category
        setCategoryColor(holder, transaction)

        // Load and display the receipt image if present
        if (transaction.imageData.isNotEmpty()) {
            val bitmap = ImageUtils.decodeBase64ToBitmap(transaction.imageData)
            if (bitmap != null) {
                holder.ivReceipt.setImageBitmap(bitmap)
                holder.ivReceipt.visibility = View.VISIBLE
            } else {
                holder.ivReceipt.visibility = View.GONE
            }
        } else {
            holder.ivReceipt.visibility = View.GONE
        }

        // Set click listener to show a detailed dialog when an item is clicked
        holder.itemView.setOnClickListener {
            showTransactionDetails(holder.itemView.context, transaction)
            onItemClick(transaction) // Optional additional action
        }
    }

    // Sets the appropriate category icon based on category name
    //(Android Developer 2025)
    private fun setCategoryIcon(holder: TransactionViewHolder, categoryName: String) {
        val iconResourceId = when (categoryName.lowercase().trim()) {
            // Food & Dining
            "food", "dining", "restaurant", "groceries", "food & dining", "meals" -> R.drawable.ic_category_food

            // Transportation
            "transport", "transportation", "fuel", "car", "taxi", "uber", "petrol" -> R.drawable.ic_category_transport

            // Shopping
            "shopping", "clothes", "retail", "clothing", "electronics" -> R.drawable.ic_category_shopping

            // Health & Medical
            "health", "medical", "pharmacy", "healthcare", "doctor", "medicine" -> R.drawable.ic_category_health

            // Education
            "education", "school", "books", "learning", "tuition" -> R.drawable.ic_category_education

            // Housing & Home
            "rent", "housing", "mortgage", "home", "utilities" -> R.drawable.ic_home

            // Savings & Investment
            "savings", "investment", "retirement" -> R.drawable.ic_category_investment

            // Entertainment
            "entertainment", "movies", "games", "music" -> R.drawable.ic_category_entertainment

            // Bills & Utilities
            "bills", "utilities", "phone", "insurance" -> R.drawable.ic_category_bills

            // Income categories
            "salary", "wage", "income", "bonus", "freelance" -> R.drawable.ic_category_salary

            // Gift category
            "gift", "gifts", "present" -> R.drawable.ic_category_gift

            // Default fallback
            else -> R.drawable.ic_category_default
        }

        holder.ivCategoryIcon.setImageResource(iconResourceId)
    }

    // Shows a dialog with detailed transaction information
    //(Android Developer 2025)
    private fun showTransactionDetails(context: android.content.Context, transaction: Transaction) {
        val detailsDialog = TransactionDetailsDialog(context)
        detailsDialog.show(transaction)
    }

    // Returns the total number of items in the list
    //(Android Developer 2025)
    override fun getItemCount(): Int = transactions.size

    // Method to update transactions list for dynamic updates
    //(Android Developer 2025)
    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions.clear()
        transactions.addAll(newTransactions)
        notifyDataSetChanged()
    }

    // Method to update both transactions and categories (for compatibility)
    //(Android Developer 2025)
    fun updateData(newTransactions: List<Transaction>, categories: Map<String, Any>) {
        transactions.clear()
        transactions.addAll(newTransactions)
        notifyDataSetChanged()
    }

    // Method to update categories (for compatibility with existing code)
    //(Android Developer 2025)
    fun updateCategories(categories: Map<String, Any>) {
        // This method exists for compatibility but doesn't need implementation
        // since we're using transaction.category directly
        notifyDataSetChanged()
    }

    // Sets the category color indicator based on transaction type and category
    private fun setCategoryColor(holder: TransactionViewHolder, transaction: Transaction) {
        val color = getDefaultCategoryColor(holder, transaction.category, transaction.type)
        holder.categoryColorIndicator.setBackgroundColor(color)
    }

    // Gets default colors for categories using color resource IDs
    private fun getDefaultCategoryColor(holder: TransactionViewHolder, categoryName: String, type: String): Int {
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

            // Default colors based on type
            else -> if (type == "income")
                ContextCompat.getColor(context, R.color.income_green)
            else
                ContextCompat.getColor(context, R.color.purple)
        }
    }
}

//REFERENCES
// Android Developer. 2025. "Create Dynamic Lists with RecyclerView". 2025
// <https://developer.android.com/develop/ui/views/layout/recyclerview>
// [accessed 30 April 2025].
