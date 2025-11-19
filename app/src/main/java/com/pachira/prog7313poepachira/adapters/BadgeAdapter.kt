package com.pachira.prog7313poepachira.adapters

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.pachira.prog7313poepachira.R
import com.pachira.prog7313poepachira.data.Badge
import java.text.SimpleDateFormat
import java.util.*

class BadgeAdapter(
    private val badges: List<Badge>,
    private val onBadgeClick: (Badge) -> Unit
) : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    class BadgeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivBadgeIcon: ImageView = itemView.findViewById(R.id.ivBadgeIcon)
        val tvBadgeName: TextView = itemView.findViewById(R.id.tvBadgeName)
        val tvBadgeDescription: TextView = itemView.findViewById(R.id.tvBadgeDescription)
        val tvBadgeRarity: TextView = itemView.findViewById(R.id.tvBadgeRarity)
        val tvEarnedDate: TextView = itemView.findViewById(R.id.tvEarnedDate)
        val viewRarityIndicator: View = itemView.findViewById(R.id.viewRarityIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_badge, parent, false)
        return BadgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val badge = badges[position]

        holder.tvBadgeName.text = badge.name
        holder.tvBadgeDescription.text = badge.description
        holder.tvBadgeRarity.text = badge.rarity.uppercase()

        // Set badge icon
        val iconResId = getBadgeIconResource(badge.iconName)
        holder.ivBadgeIcon.setImageResource(iconResId)

        // Set rarity color
        val rarityColor = getRarityColor(badge.rarity, holder.itemView.context)
        holder.tvBadgeRarity.setTextColor(rarityColor)
        holder.viewRarityIndicator.setBackgroundColor(rarityColor)

        if (badge.earned) {
            // Badge is earned - show in full color
            holder.ivBadgeIcon.colorFilter = null
            holder.tvBadgeName.alpha = 1.0f
            holder.tvBadgeDescription.alpha = 1.0f

            // Show earned date
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            holder.tvEarnedDate.text = "Earned: ${dateFormat.format(Date(badge.earnedAt))}"
            holder.tvEarnedDate.visibility = View.VISIBLE
        } else {
            // Badge is not earned - show grayed out
            val grayFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            holder.ivBadgeIcon.colorFilter = grayFilter
            holder.tvBadgeName.alpha = 0.5f
            holder.tvBadgeDescription.alpha = 0.5f

            // Show requirement instead of earned date
            holder.tvEarnedDate.text = "Requirement: ${badge.requirement}"
            holder.tvEarnedDate.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener {
            onBadgeClick(badge)
        }
    }

    override fun getItemCount(): Int = badges.size

    private fun getBadgeIconResource(iconName: String): Int {
        return when (iconName) {
            "ic_badge_first_goal" -> R.drawable.ic_badge_first_goal
            "ic_badge_goal_master" -> R.drawable.ic_badge_goal_master
            "ic_badge_savings_legend" -> R.drawable.ic_badge_savings_legend
            "ic_badge_thousand" -> R.drawable.ic_badge_thousand
            "ic_badge_ten_thousand" -> R.drawable.ic_badge_ten_thousand
            "ic_badge_hundred_thousand" -> R.drawable.ic_badge_hundred_thousand
            "ic_badge_quick_saver" -> R.drawable.ic_badge_quick_saver
            "ic_badge_lightning" -> R.drawable.ic_badge_lightning
            "ic_badge_consistent" -> R.drawable.ic_badge_consistent
            "ic_badge_dedication" -> R.drawable.ic_badge_dedication
            "ic_badge_big_dreamer" -> R.drawable.ic_badge_big_dreamer
            "ic_badge_perfectionist" -> R.drawable.ic_badge_perfectionist
            else -> R.drawable.ic_badge_default
        }
    }

    private fun getRarityColor(rarity: String, context: android.content.Context): Int {
        return when (rarity.lowercase()) {
            "common" -> ContextCompat.getColor(context, R.color.badge_common)
            "rare" -> ContextCompat.getColor(context, R.color.badge_rare)
            "epic" -> ContextCompat.getColor(context, R.color.badge_epic)
            "legendary" -> ContextCompat.getColor(context, R.color.badge_legendary)
            else -> ContextCompat.getColor(context, R.color.purple)
        }
    }
}
