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
import com.pachira.prog7313poepachira.data.Wallet
import java.text.NumberFormat
import java.util.*

/**
 * RecyclerView Adapter to display a list of wallets with icon, name, and balance,
 * and allow the user to select a wallet.
 */
class WalletAdapter(
    private val wallets: List<Wallet>, // List of wallets to display
    private val onWalletSelected: (Wallet) -> Unit // Callback when a wallet is selected
) : RecyclerView.Adapter<WalletAdapter.WalletViewHolder>() {

    // Stores the ID of the currently selected wallet
    private var selectedWalletId: String? = null

    // ViewHolder class to hold and bind UI components for each wallet item
    inner class WalletViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardWallet)
        val tvWalletName: TextView = itemView.findViewById(R.id.tvWalletName)
        val tvWalletBalance: TextView = itemView.findViewById(R.id.tvWalletBalance)
        val iconView: ImageView = itemView.findViewById(R.id.ivWalletIcon)
        val categoryColorIndicator: View = itemView.findViewById(R.id.categoryColorIndicator)

        // Binds a Wallet object to the views
        fun bind(wallet: Wallet) {
            tvWalletName.text = wallet.name // Set wallet name

            // Format and display wallet balance
            val formattedBalance = formatCurrency(wallet.balance)
            tvWalletBalance.text = formattedBalance

            // Set the color indicator based on wallet's color
            try {
                categoryColorIndicator.setBackgroundColor(Color.parseColor(wallet.colorHex))
            } catch (e: Exception) {
                // Fallback color if parsing fails
                categoryColorIndicator.setBackgroundColor(Color.parseColor("#6200EE"))
            }

            // Set wallet icon from drawable resource
            val resourceId = itemView.context.resources.getIdentifier(
                wallet.iconName, "drawable", itemView.context.packageName
            )
            if (resourceId != 0) {
                iconView.setImageResource(resourceId)
                iconView.visibility = View.VISIBLE
            } else {
                // Fallback icon based on wallet type
                val fallbackIcon = when (wallet.type) {
                    "bank" -> R.drawable.ic_bank
                    "credit" -> R.drawable.ic_credit_card
                    "cash" -> R.drawable.ic_cash
                    "savings" -> R.drawable.ic_savings
                    else -> R.drawable.ic_wallet_default
                }
                iconView.setImageResource(fallbackIcon)
            }

            // Highlight the selected wallet with a stroke
            val isSelected = wallet.id == selectedWalletId
            cardView.strokeWidth = if (isSelected) 4 else 0
            cardView.strokeColor = if (isSelected) Color.parseColor("#6200EE") else Color.TRANSPARENT

            // Handle item click: update selection and notify listener
            itemView.setOnClickListener {
                selectedWalletId = wallet.id
                onWalletSelected(wallet)
                notifyDataSetChanged() // Refresh to update UI selection state
            }
        }

        // Helper function to format currency
        private fun formatCurrency(amount: Double): String {
            val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
            return format.format(amount)
        }
    }

    // Inflate the wallet item layout and return the ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wallet, parent, false)
        return WalletViewHolder(view)
    }

    // Bind wallet data to the ViewHolder at the given position
    override fun onBindViewHolder(holder: WalletViewHolder, position: Int) {
        holder.bind(wallets[position])
    }

    // Return the total number of wallet items
    override fun getItemCount(): Int = wallets.size

    // Public method to programmatically set selected wallet and refresh UI
    fun setSelectedWallet(walletId: String) {
        selectedWalletId = walletId
        notifyDataSetChanged()
    }
}