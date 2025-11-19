package com.pachira.prog7313poepachira

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.pachira.prog7313poepachira.adapters.BadgeAdapter
import com.pachira.prog7313poepachira.data.Badge
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.Button
import androidx.core.content.ContextCompat

class BadgesActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var rvBadges: RecyclerView
    private lateinit var tvEarnedCount: TextView
    private lateinit var tvTotalCount: TextView
    private lateinit var emptyStateLayout: View

    private val badges = mutableListOf<Badge>()
    private lateinit var badgeAdapter: BadgeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_badges)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Check if user is logged in
        if (auth.currentUser == null) {
            finish()
            return
        }

        initializeViews()
        setupRecyclerView()
        loadBadges()

    }

    private fun initializeViews() {
        rvBadges = findViewById(R.id.rvBadges)
        tvEarnedCount = findViewById(R.id.tvEarnedCount)
        tvTotalCount = findViewById(R.id.tvTotalCount)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
    }

    private fun setupRecyclerView() {
        badgeAdapter = BadgeAdapter(badges) { badge ->
            showBadgeDetails(badge)
        }

        rvBadges.layoutManager = GridLayoutManager(this, 2)
        rvBadges.adapter = badgeAdapter
    }

    private fun loadBadges() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        database.child("users").child(userId).child("badges")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val allBadges = mutableListOf<Badge>()

                    for (badgeSnapshot in snapshot.children) {
                        val badge = badgeSnapshot.getValue(Badge::class.java)
                        badge?.let { allBadges.add(it) }
                    }

                    //store total count of badges
                    val totalBadgeCount = allBadges.size

                    //Filter earned badges
                    val earnedBadges = allBadges.filter { it.earned }
                    badges.clear()
                    badges.addAll(earnedBadges)

                    // Sort badges: earned first, then by rarity
                    badges.sortWith(compareByDescending<Badge> { it.earned }
                        .thenBy { getRarityOrder(it.rarity) }
                        .thenBy { it.name })

                    updateUI()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun updateUI() {
        if (badges.isEmpty()) {
            rvBadges.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            rvBadges.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            badgeAdapter.notifyDataSetChanged()

            // Update counters
            val earnedCount = badges.count { it.earned }
            val totalCount = badges.size

            tvEarnedCount.text = earnedCount.toString()
            tvTotalCount.text = totalCount.toString()
        }
    }

    private fun getRarityOrder(rarity: String): Int {
        return when (rarity.lowercase()) {
            "legendary" -> 0
            "epic" -> 1
            "rare" -> 2
            "common" -> 3
            else -> 4
        }
    }

    private fun showBadgeDetails(badge: Badge) {
        val dialog = BottomSheetDialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_badge_details, null)
        dialog.setContentView(dialogView)

        val ivBadgeIcon = dialogView.findViewById<ImageView>(R.id.ivBadgeIcon)
        val tvBadgeName = dialogView.findViewById<TextView>(R.id.tvBadgeName)
        val tvBadgeDescription = dialogView.findViewById<TextView>(R.id.tvBadgeDescription)
        val tvBadgeRarity = dialogView.findViewById<TextView>(R.id.tvBadgeRarity)
        val tvBadgeRequirement = dialogView.findViewById<TextView>(R.id.tvBadgeRequirement)
        val tvBadgeStatus = dialogView.findViewById<TextView>(R.id.tvBadgeStatus)
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)

        // Set badge details
        ivBadgeIcon.setImageResource(getBadgeIconResource(badge.iconName))
        tvBadgeName.text = badge.name
        tvBadgeDescription.text = badge.description
        tvBadgeRarity.text = badge.rarity.uppercase()
        tvBadgeRequirement.text = badge.requirement

        // Set rarity color
        val rarityColor = getRarityColor(badge.rarity)
        tvBadgeRarity.setTextColor(rarityColor)

        // Set status
        if (badge.earned) {
            tvBadgeStatus.text = "✅ Earned on ${java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(badge.earnedAt))}"
            tvBadgeStatus.setTextColor(ContextCompat.getColor(this, R.color.category_food))
        } else {
            tvBadgeStatus.text = "🔒 Not yet earned"
            tvBadgeStatus.setTextColor(ContextCompat.getColor(this, R.color.darker_gray))
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

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

    private fun getRarityColor(rarity: String): Int {
        return when (rarity.lowercase()) {
            "common" -> ContextCompat.getColor(this, R.color.badge_common)
            "rare" -> ContextCompat.getColor(this, R.color.badge_rare)
            "epic" -> ContextCompat.getColor(this, R.color.badge_epic)
            "legendary" -> ContextCompat.getColor(this, R.color.badge_legendary)
            else -> ContextCompat.getColor(this, R.color.purple)
        }
    }
}
