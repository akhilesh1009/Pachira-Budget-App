package com.pachira.prog7313poepachira.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.pachira.prog7313poepachira.R
import com.pachira.prog7313poepachira.data.BudgetGoal
import com.pachira.prog7313poepachira.data.Category
import com.pachira.prog7313poepachira.data.Wallet
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

//(Android Developer 2025)
class BudgetGoalAdapter(
    private val budgetGoals: List<BudgetGoal>,
    private val categories: List<Category>,
    private val wallets: List<Wallet>,
    private val onItemClick: (BudgetGoal) -> Unit,
    private val onAddFundsClick: (BudgetGoal) -> Unit
) : RecyclerView.Adapter<BudgetGoalAdapter.BudgetGoalViewHolder>() {

    //(Android Developer 2025)
    class BudgetGoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGoalName: TextView = itemView.findViewById(R.id.tvGoalName)
        val tvProgress: TextView = itemView.findViewById(R.id.tvProgress)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        val tvCurrentAmount: TextView = itemView.findViewById(R.id.tvCurrentAmount)
        val tvTargetAmount: TextView = itemView.findViewById(R.id.tvTargetAmount)
        val btnAddFunds: Button = itemView.findViewById(R.id.btnAddFunds)

        // New views for additional data
        val layoutCategoryInfo: LinearLayout = itemView.findViewById(R.id.layoutCategoryInfo)
        val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)

        val layoutWalletInfo: LinearLayout = itemView.findViewById(R.id.layoutWalletInfo)
        val ivWalletIcon: ImageView = itemView.findViewById(R.id.ivWalletIcon)
        val tvWalletName: TextView = itemView.findViewById(R.id.tvWalletName)

        val layoutRecurrenceInfo: LinearLayout = itemView.findViewById(R.id.layoutRecurrenceInfo)
        val tvRecurrence: TextView = itemView.findViewById(R.id.tvRecurrence)
        val tvCreatedDate: TextView = itemView.findViewById(R.id.tvCreatedDate)
    }

    //(Android Developer 2025)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetGoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget_goal, parent, false)
        return BudgetGoalViewHolder(view)
    }

    //(Android Developer 2025)
    override fun onBindViewHolder(holder: BudgetGoalViewHolder, position: Int) {
        val budgetGoal = budgetGoals[position]

        // Format currency
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        // Calculate progress percentage
        val progress = if (budgetGoal.targetAmount > 0)
            ((budgetGoal.currentAmount / budgetGoal.targetAmount) * 100).toInt()
        else 0

        // Set basic text values
        holder.tvGoalName.text = budgetGoal.name
        holder.tvProgress.text = "$progress%"
        holder.tvCurrentAmount.text = format.format(budgetGoal.currentAmount)
        holder.tvTargetAmount.text = "of ${format.format(budgetGoal.targetAmount)}"

        // Set progress
        holder.progressBar.progress = progress

        // Handle category information
        handleCategoryInfo(holder, budgetGoal)

        // Handle wallet information
        handleWalletInfo(holder, budgetGoal)

        // Handle recurrence and date information
        handleRecurrenceInfo(holder, budgetGoal)

        // Set click listeners
        holder.itemView.setOnClickListener {
            onItemClick(budgetGoal)
        }

        holder.btnAddFunds.setOnClickListener {
            onAddFundsClick(budgetGoal)
        }
    }

    private fun handleCategoryInfo(holder: BudgetGoalViewHolder, budgetGoal: BudgetGoal) {
        if (!budgetGoal.categoryId.isNullOrEmpty() && budgetGoal.categoryId != "all") { //possible need to fix
            val category = categories.find { it.id == budgetGoal.categoryId }
            if (category != null) {
                holder.layoutCategoryInfo.visibility = View.VISIBLE
                holder.tvCategoryName.text = category.name

                // Set category icon
                val resourceId = holder.itemView.context.resources.getIdentifier(
                    category.iconName, "drawable", holder.itemView.context.packageName
                )
                if (resourceId != 0) {
                    holder.ivCategoryIcon.setImageResource(resourceId)
                } else {
                    holder.ivCategoryIcon.setImageResource(R.drawable.ic_category_default)
                }
            } else {
                holder.layoutCategoryInfo.visibility = View.GONE
            }
        } else {
            holder.layoutCategoryInfo.visibility = View.GONE
        }
    }

    private fun handleWalletInfo(holder: BudgetGoalViewHolder, budgetGoal: BudgetGoal) {
        if (!budgetGoal.walletId.isNullOrEmpty() && budgetGoal.walletId != "all") { //possible need to fix
            val wallet = wallets.find { it.id == budgetGoal.walletId }
            if (wallet != null) {
                holder.layoutWalletInfo.visibility = View.VISIBLE
                holder.tvWalletName.text = wallet.name

                // Set wallet icon
                val resourceId = holder.itemView.context.resources.getIdentifier(
                    wallet.iconName, "drawable", holder.itemView.context.packageName
                )
                if (resourceId != 0) {
                    holder.ivWalletIcon.setImageResource(resourceId)
                } else {
                    // Fallback icon based on wallet type
                    val fallbackIcon = when (wallet.type) {
                        "bank" -> R.drawable.ic_bank
                        "credit" -> R.drawable.ic_credit_card
                        "cash" -> R.drawable.ic_cash
                        "savings" -> R.drawable.ic_savings
                        else -> R.drawable.ic_wallet_default
                    }
                    holder.ivWalletIcon.setImageResource(fallbackIcon)
                }
            } else {
                holder.layoutWalletInfo.visibility = View.GONE
            }
        } else {
            holder.layoutWalletInfo.visibility = View.GONE
        }
    }

    private fun handleRecurrenceInfo(holder: BudgetGoalViewHolder, budgetGoal: BudgetGoal) {
        // Show recurrence info if we have either recurrence or creation date
        val hasRecurrence = budgetGoal.recurrence.isNotEmpty() //&& budgetGoal.recurrence != "Monthly"
        val hasCreationDate = budgetGoal.createdAt > 0

        if (hasRecurrence || hasCreationDate) {
            holder.layoutRecurrenceInfo.visibility = View.VISIBLE

            // Set recurrence text
            if (hasRecurrence) {
                holder.tvRecurrence.text = "${budgetGoal.recurrence} goal"
                holder.tvRecurrence.visibility = View.VISIBLE
            } else {
                holder.tvRecurrence.visibility = View.GONE
            }

            // Set creation date
            if (hasCreationDate) {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val createdDate = dateFormat.format(Date(budgetGoal.createdAt))
                holder.tvCreatedDate.text = "Created: $createdDate"
                holder.tvCreatedDate.visibility = View.VISIBLE
            } else {
                holder.tvCreatedDate.visibility = View.GONE
            }
        } else {
            holder.layoutRecurrenceInfo.visibility = View.GONE
        }
    }

    //(Android Developer 2025)
    override fun getItemCount(): Int = budgetGoals.size
}

//REFERENCES
// Android Developer. 2025. "Create Dynamic Lists with RecyclerView". 2025
// <https://developer.android.com/develop/ui/views/layout/recyclerview>
// [accessed 30 April 2025].