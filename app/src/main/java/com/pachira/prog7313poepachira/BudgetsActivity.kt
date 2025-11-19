package com.pachira.prog7313poepachira

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.pachira.prog7313poepachira.AddExpenseActivity
import com.pachira.prog7313poepachira.adapters.Avaliable_Balance_Widget
import com.pachira.prog7313poepachira.adapters.BudgetGoalAdapter
import com.pachira.prog7313poepachira.adapters.Category_Spending_Widget
import com.pachira.prog7313poepachira.data.BudgetGoal
import com.pachira.prog7313poepachira.data.Transaction
import com.pachira.prog7313poepachira.data.Category
import com.pachira.prog7313poepachira.data.Wallet
import com.pachira.prog7313poepachira.views.ConfettiView
import java.text.NumberFormat
import java.util.*
import com.pachira.prog7313poepachira.utility.BudgetReminderScheduler
import com.pachira.prog7313poepachira.utility.NotificationHelper
import com.pachira.prog7313poepachira.utility.BadgeManager
import com.pachira.prog7313poepachira.data.Badge

class BudgetsActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var rvBudgetGoals: RecyclerView
    private lateinit var fabAddGoal: FloatingActionButton
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var budgetGoalAdapter: BudgetGoalAdapter
    private lateinit var badgeManager: BadgeManager

    private val budgetGoals = mutableListOf<BudgetGoal>()
    private val categories = mutableListOf<Category>()
    private val wallets = mutableListOf<Wallet>()
    private var availableBalance = 0.0

    // Selected values for the form
    private var selectedCategory: Category? = null
    private var selectedWallet: Wallet? = null
    private var selectedRecurrence: String = "Monthly"
    private var setLimitAmount: Double = 0.0 // Store the limit amount as target amount

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budgets)

        // Initialize FirebaseF
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Initialize badge manager
        badgeManager = BadgeManager(this)
        badgeManager.initializeBadges()

        // Check if user is logged in
        if (auth.currentUser == null) {
            finish()
            return
        }

        // Initialize UI elements
        rvBudgetGoals = findViewById(R.id.rvBudgetGoals)
        fabAddGoal = findViewById(R.id.fabAddGoal)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)

        // Set up RecyclerView with the correct adapter initialization including categories and wallets
        budgetGoalAdapter = BudgetGoalAdapter(
            budgetGoals,
            categories,  // Pass the categories list
            wallets,     // Pass the wallets list
            { budgetGoal -> showBudgetGoalDetails(budgetGoal) },
            { budgetGoal -> showAddFundsDialog(budgetGoal) }
        )

        rvBudgetGoals.layoutManager = LinearLayoutManager(this)
        rvBudgetGoals.adapter = budgetGoalAdapter

        // Set click listeners
        fabAddGoal.setOnClickListener {
            showAddBudgetGoalDialog()
        }

        // Load data
        loadAvailableBalance()
        loadBudgetGoals()
        loadCategories()
        loadWallets()

        // Setup bottom navigation
        setupBottomNavigation()
        setSelectedNavItem(R.id.nav_budgets)

        // Handle notification intents
        handleNotificationIntent()

        // Add this temporarily for testing - remove after confirming notifications work
        fabAddGoal.setOnLongClickListener {
            testGoalAchievedNotification()
            true
        }
    }

    // Add this method to test notifications manually
    private fun testGoalAchievedNotification() {
        val testGoal = BudgetGoal(
            id = "test_goal",
            name = "Test Achievement",
            targetAmount = 100.0,
            currentAmount = 100.0,
            createdAt = System.currentTimeMillis()
        )

        val notificationHelper = NotificationHelper(this)
        notificationHelper.showGoalAchievedNotification(testGoal)

        Toast.makeText(this, "Test notification sent - check notification panel", Toast.LENGTH_LONG).show()
    }

    // Handle notification intents
    private fun handleNotificationIntent() {
        val budgetGoalId = intent.getStringExtra("budget_goal_id")
        val showAddFunds = intent.getBooleanExtra("show_add_funds", false)

        if (budgetGoalId != null) {
            // Find the budget goal and show details or add funds dialog
            Handler(Looper.getMainLooper()).postDelayed({
                val budgetGoal = budgetGoals.find { it.id == budgetGoalId }
                if (budgetGoal != null) {
                    if (showAddFunds) {
                        showAddFundsDialog(budgetGoal)
                    } else {
                        showBudgetGoalDetails(budgetGoal)
                    }
                }
            }, 500) // Small delay to ensure data is loaded
        }
    }

    // Load categories from database
    private fun loadCategories() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        database.child("users").child(userId).child("categories")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    categories.clear()
                    // Add "All Expenses" as first option with proper constructor parameters
                    categories.add(Category("all", "All Expenses", "#3F51B5", "ic_all_expenses"))

                    for (categorySnapshot in snapshot.children) {
                        val category = categorySnapshot.getValue(Category::class.java)
                        category?.let {
                            categories.add(it)
                        }
                    }

                    // Refresh adapter when categories are loaded
                    budgetGoalAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@BudgetsActivity, "Failed to load categories", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Load wallets from database
    private fun loadWallets() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        database.child("users").child(userId).child("wallets")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    wallets.clear()
                    // Remove the "All Wallets" option - only show actual wallets

                    for (walletSnapshot in snapshot.children) {
                        val wallet = walletSnapshot.getValue(Wallet::class.java)
                        wallet?.let {
                            wallets.add(it)
                        }
                    }

                    // Refresh adapter when wallets are loaded
                    budgetGoalAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@BudgetsActivity, "Failed to load wallets", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Loads the available balance for the current user by retrieving total income and total expenses
    private fun loadAvailableBalance() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Get total income
        database.child("users").child(userId).child("income")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalIncome = 0.0
                    for (incomeSnapshot in snapshot.children) {
                        val income = incomeSnapshot.getValue(Transaction::class.java)
                        income?.let {
                            totalIncome += it.amount
                        }
                    }

                    // Get total expenses
                    database.child("users").child(userId).child("expenses")
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(expenseSnapshot: DataSnapshot) {
                                var totalExpenses = 0.0
                                for (expenseItem in expenseSnapshot.children) {
                                    val expense = expenseItem.getValue(Transaction::class.java)
                                    expense?.let {
                                        totalExpenses += it.amount
                                    }
                                }

                                // Calculate available balance and ensures the available balance is not negative
                                availableBalance = totalIncome - totalExpenses
                                if (availableBalance < 0) availableBalance = 0.0
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@BudgetsActivity, "Failed to load expenses", Toast.LENGTH_SHORT).show()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@BudgetsActivity, "Failed to load income", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Loads the user's budget goals
    private fun loadBudgetGoals() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        database.child("users").child(userId).child("budgetGoals")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    budgetGoals.clear()

                    for (goalSnapshot in snapshot.children) {
                        val goal = goalSnapshot.getValue(BudgetGoal::class.java)
                        goal?.let {
                            budgetGoals.add(it)
                        }
                    }

                    // Sort goals by progress percentage (highest first)
                    budgetGoals.sortByDescending {
                        if (it.targetAmount > 0) it.currentAmount / it.targetAmount else 0.0
                    }

                    updateBudgetGoalsUI()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@BudgetsActivity, "Failed to load budget goals", Toast.LENGTH_SHORT).show()
                    updateBudgetGoalsUI()
                }
            })
    }

    private fun updateBudgetGoalsUI() {
        if (budgetGoals.isEmpty()) {
            rvBudgetGoals.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            rvBudgetGoals.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            budgetGoalAdapter.notifyDataSetChanged()
        }
    }

    // Displays a bottom sheet dialog for adding a new budget goal
    private fun showAddBudgetGoalDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_budget_goal, null)
        bottomSheetDialog.setContentView(dialogView)

        val etGoalName = dialogView.findViewById<EditText>(R.id.etGoalName)
        val etInitialAmount = dialogView.findViewById<EditText>(R.id.etInitialAmount)
        val btnCreateGoal = dialogView.findViewById<Button>(R.id.btnCreateGoal)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)

        // New UI elements for the additional fields
        val layoutBudgetFor = dialogView.findViewById<LinearLayout>(R.id.layoutBudgetFor)
        val layoutWallets = dialogView.findViewById<LinearLayout>(R.id.layoutWallets)
        val layoutRecurrence = dialogView.findViewById<LinearLayout>(R.id.layoutRecurrence)
        val layoutSetLimit = dialogView.findViewById<LinearLayout>(R.id.layoutSetLimit)

        val tvSelectedCategory = dialogView.findViewById<TextView>(R.id.tvSelectedCategory)
        val tvSelectedWallet = dialogView.findViewById<TextView>(R.id.tvSelectedWallet)
        val tvSelectedRecurrence = dialogView.findViewById<TextView>(R.id.tvSelectedRecurrence)
        val tvLimitPreview = dialogView.findViewById<TextView>(R.id.tvLimitPreview)

        // Set default values
        tvSelectedRecurrence.text = selectedRecurrence
        updateLimitPreview(tvLimitPreview)

        // Set click listeners for the new fields
        layoutBudgetFor.setOnClickListener {
            showCategoriesBottomSheet(tvSelectedCategory)
        }

        layoutWallets.setOnClickListener {
            showWalletsBottomSheet(tvSelectedWallet)
        }

        layoutRecurrence.setOnClickListener {
            showRecurrenceBottomSheet(tvSelectedRecurrence)
        }

        layoutSetLimit.setOnClickListener {
            showSetLimitBottomSheet(tvLimitPreview)
        }

        btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        btnCreateGoal.setOnClickListener {
            val name = etGoalName.text.toString().trim()
            val initialAmountStr = etInitialAmount.text.toString().trim()

            // Validate that the goal name is not empty
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a goal name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate that a limit has been set (this becomes our target amount)
            if (setLimitAmount <= 0) {
                Toast.makeText(this, "Please set a target amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val initialAmount = initialAmountStr.toDoubleOrNull() ?: 0.0

            // Validate that the initial amount is not negative
            if (initialAmount < 0) {
                Toast.makeText(this, "Initial amount cannot be negative", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate that the initial amount doesn't exceed available balance
            if (initialAmount > availableBalance) {
                Toast.makeText(this, "Initial amount cannot exceed available balance", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate that the initial amount doesn't exceed the target amount
            if (initialAmount > setLimitAmount) {
                Toast.makeText(this, "Initial amount cannot exceed target amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create new budget goal with limit amount as target amount
            addBudgetGoal(name, setLimitAmount, initialAmount, selectedCategory, selectedWallet, selectedRecurrence)

            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    // Update the limit preview text
    private fun updateLimitPreview(tvLimitPreview: TextView) {
        if (setLimitAmount > 0) {
            tvLimitPreview.text = formatCurrency(setLimitAmount)
            tvLimitPreview.setTextColor(ContextCompat.getColor(this, R.color.purple))
        } else {
            tvLimitPreview.text = "Tap to set target amount"
            tvLimitPreview.setTextColor(ContextCompat.getColor(this, R.color.darker_gray))
        }
    }

    // SINGLE clearAllRadioButtons method - removed duplicates
    private fun clearAllRadioButtons(radioGroup: RadioGroup) {
        android.util.Log.d("RadioButtons", "Clearing ${radioGroup.childCount} child views")

        for (i in 0 until radioGroup.childCount) {
            val child = radioGroup.getChildAt(i)
            android.util.Log.d("RadioButtons", "Child $i type: ${child::class.simpleName}")

            when (child) {
                is LinearLayout -> {
                    val radioButton = child.findViewById<RadioButton>(R.id.rbCategory)
                    if (radioButton != null) {
                        android.util.Log.d("RadioButtons", "Clearing radio button with ID: ${radioButton.id}")
                        radioButton.isChecked = false
                    } else {
                        android.util.Log.d("RadioButtons", "No radio button found in LinearLayout")
                    }

                    // Also clear select all radio button
                    val rbSelectAll = child.findViewById<RadioButton>(R.id.rbSelectAll)
                    rbSelectAll?.isChecked = false
                }
                is RadioButton -> {
                    android.util.Log.d("RadioButtons", "Clearing direct RadioButton with ID: ${child.id}")
                    child.isChecked = false
                }
            }
        }

        radioGroup.clearCheck()
    }

    // Helper method to recursively clear radio buttons in nested views
    private fun clearRadioButtonsRecursively(view: View) {
        if (view is RadioButton) {
            view.isChecked = false
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                clearRadioButtonsRecursively(view.getChildAt(i))
            }
        }
    }

    // Show categories bottom sheet with icons, names, and radio buttons
    private fun showCategoriesBottomSheet(tvSelectedCategory: TextView) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_categories, null)
        bottomSheetDialog.setContentView(dialogView)

        val radioGroupCategories = dialogView.findViewById<RadioGroup>(R.id.radioGroupCategories)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)
        val layoutSelectAll = dialogView.findViewById<LinearLayout>(R.id.layoutSelectAll)
        val rbSelectAll = dialogView.findViewById<RadioButton>(R.id.rbSelectAll)

        // Close button functionality
        btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        // Clear existing views
        radioGroupCategories.removeAllViews()

        // Re-add Select All option
        radioGroupCategories.addView(layoutSelectAll)

        // Add category items - filter out "All Expenses" since we're using "Select All"
        categories.filter { it.id != "all" }.forEachIndexed { index, category ->
            val categoryView = LayoutInflater.from(this).inflate(R.layout.item_category_selection, radioGroupCategories, false)

            val ivCategoryIcon = categoryView.findViewById<ImageView>(R.id.ivCategoryIcon)
            val tvCategoryName = categoryView.findViewById<TextView>(R.id.tvCategoryName)
            val rbCategory = categoryView.findViewById<RadioButton>(R.id.rbCategory)

            // Set category data
            tvCategoryName.text = category.name

            val iconResId = getIconResourceId(category.iconName)
            ivCategoryIcon.setImageResource(iconResId)

            val categoryColor = getCategoryColor(category.iconName)
            ImageViewCompat.setImageTintList(ivCategoryIcon, ColorStateList.valueOf(categoryColor))

            // Set radio button ID
            rbCategory.id = View.generateViewId()

            // Check if this category is currently selected
            if (selectedCategory?.id == category.id) {
                rbCategory.isChecked = true
                rbSelectAll.isChecked = false
            }

            // Handle category selection
            categoryView.setOnClickListener {
                clearAllRadioButtons(radioGroupCategories)
                rbSelectAll.isChecked = false

                // Select this category
                rbCategory.isChecked = true
                selectedCategory = category
                tvSelectedCategory.text = category.name
                bottomSheetDialog.dismiss()
            }

            radioGroupCategories.addView(categoryView)
        }

        // Handle "Select All" functionality
        layoutSelectAll.setOnClickListener {
            selectAllCategories(radioGroupCategories, rbSelectAll)
            selectedCategory = categories.firstOrNull { it.id == "all" }
            tvSelectedCategory.text = "All Categories"

            // Dismiss after showing the selection for a brief moment
            Handler(Looper.getMainLooper()).postDelayed({
                bottomSheetDialog.dismiss()
            }, 300)
        }

        bottomSheetDialog.show()
    }

    // Add this method to handle "Select All" functionality
    private fun selectAllCategories(radioGroupCategories: RadioGroup, rbSelectAll: RadioButton) {
        // First, clear all existing selections
        clearAllRadioButtons(radioGroupCategories)

        // Set the "Select All" radio button as checked
        rbSelectAll.isChecked = true

        // Iterate through all category items and check their radio buttons
        for (i in 0 until radioGroupCategories.childCount) {
            val child = radioGroupCategories.getChildAt(i)
            if (child is LinearLayout && child.id != R.id.layoutSelectAll) {
                val radioButton = child.findViewById<RadioButton>(R.id.rbCategory)
                radioButton?.isChecked = true
            }
        }
    }

    // Enhanced icon mapping with debugging
    private fun getIconResourceId(iconName: String?): Int {
        android.util.Log.d("CategoryIcons", "Looking for icon: $iconName")

        val iconResId = when (iconName?.lowercase()) {
            "ic_all_expenses", "all_expenses" -> R.drawable.ic_all_expenses
            "ic_bills", "bills", "bills & fees" -> R.drawable.ic_category_bills
            "ic_category_health", "healthcare" -> R.drawable.ic_category_health
            "ic_category_transport", "transport" -> R.drawable.ic_category_transport
            "ic_category_investment", "investment" -> R.drawable.ic_category_investment
            "ic_category_gift", "gift" -> R.drawable.ic_category_gift
            "ic_category_shopping", "shopping" -> R.drawable.ic_category_shopping
            "ic_category_salary", "salary" -> R.drawable.ic_category_salary
            "ic_education", "education" -> R.drawable.ic_education
            "ic_category_home", "home" -> R.drawable.ic_category_home
            "ic_travel", "travel" -> R.drawable.ic_travel
            "ic_category_entertainment", "entertainment" -> R.drawable.ic_category_entertainment
            "ic_groceries", "groceries" -> R.drawable.ic_groceries
            "ic_category_food", "food", "food & drink" -> R.drawable.ic_category_food
            else -> {
                android.util.Log.w("CategoryIcons", "No icon found for: $iconName, using default")
                R.drawable.ic_category_default
            }
        }

        android.util.Log.d("CategoryIcons", "Mapped $iconName to resource ID: $iconResId")
        return iconResId
    }

    // Enhanced color mapping with debugging
    private fun getCategoryColor(iconName: String?): Int {
        android.util.Log.d("CategoryColors", "Looking for color for icon: $iconName")

        return when (iconName?.lowercase()) {
            "ic_bills", "bills", "bills & fees" -> ContextCompat.getColor(this, R.color.category_bills)
            "ic_categegory_health", "healthcare" -> ContextCompat.getColor(this, R.color.category_healthcare)
            "ic_category_transport", "transport" -> ContextCompat.getColor(this, R.color.category_transport)
            "ic_category_investment", "investment" -> ContextCompat.getColor(this, R.color.category_investment)
            "ic_category_home", "home" -> ContextCompat.getColor(this, R.color.category_home)
            "ic_travel", "travel" -> ContextCompat.getColor(this, R.color.category_travel)
            "ic_category_entertainment", "entertainment" -> ContextCompat.getColor(this, R.color.category_entertainment)
            "ic_category_gift", "gifts" -> ContextCompat.getColor(this, R.color.category_gift)
            "ic_category_food", "food", "food & drink" -> ContextCompat.getColor(this, R.color.category_food)
            "ic_category_shopping", "shopping" -> ContextCompat.getColor(this, R.color.category_shopping)
            "ic_category_salary", "salary" -> ContextCompat.getColor(this, R.color.category_salary)
            else -> ContextCompat.getColor(this, R.color.purple)
        }
    }

    // Show wallets bottom sheet
    private fun showWalletsBottomSheet(tvSelectedWallet: TextView) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_wallets, null)
        bottomSheetDialog.setContentView(dialogView)

        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)
        val radioGroupWallets = dialogView.findViewById<RadioGroup>(R.id.radioGroupWallets)

        // Clear existing radio buttons
        radioGroupWallets.removeAllViews()

        var selectedRadioButton: RadioButton? = null

        // Add wallet items for each wallet (no filtering needed since we removed "All Wallets")
        wallets.forEachIndexed { index, wallet ->
            val walletView = LayoutInflater.from(this).inflate(R.layout.item_wallet_radio, radioGroupWallets, false)

            val ivWalletIcon = walletView.findViewById<ImageView>(R.id.ivWalletIcon)
            val tvWalletName = walletView.findViewById<TextView>(R.id.tvWalletName)
            val rbWallet = walletView.findViewById<RadioButton>(R.id.rbWallet)

            // Set wallet data
            tvWalletName.text = wallet.name

            // IMPORTANT: Set a unique ID for each radio button
            rbWallet.id = View.generateViewId()

            // Store the wallet object as a tag on the radio button for easy retrieval
            rbWallet.tag = wallet

            // Set wallet icon based on type
            val iconResId = when (wallet.type?.lowercase()) {
                "cash" -> R.drawable.money
                "bank" -> R.drawable.ic_bank
                "credit" -> R.drawable.ic_credit_card
                "ewallet" -> R.drawable.ic_smartphone
                else -> R.drawable.ic_wallet_default
            }
            ivWalletIcon.setImageResource(iconResId)

            // Set wallet icon color to match wallet's color
            try {
                val walletColor = Color.parseColor(wallet.colorHex ?: "#6200EE")
                ImageViewCompat.setImageTintList(ivWalletIcon, ColorStateList.valueOf(walletColor))
            } catch (e: Exception) {
                // Fallback to default purple if color parsing fails
                val defaultColor = ContextCompat.getColor(this, R.color.purple)
                ImageViewCompat.setImageTintList(ivWalletIcon, ColorStateList.valueOf(defaultColor))
            }

            // Check if this wallet is currently selected
            if (selectedWallet?.id == wallet.id) {
                rbWallet.isChecked = true
                selectedRadioButton = rbWallet
            }

            // Handle wallet selection manually since RadioGroup doesn't work well with custom layouts
            walletView.setOnClickListener {
                // Uncheck the previously selected radio button
                selectedRadioButton?.isChecked = false

                // Check the current radio button
                rbWallet.isChecked = true
                selectedRadioButton = rbWallet

                // Update selected wallet
                selectedWallet = wallet
                tvSelectedWallet.text = wallet.name
                bottomSheetDialog.dismiss()
            }

            // Also handle direct radio button clicks
            rbWallet.setOnClickListener {
                // Uncheck the previously selected radio button
                selectedRadioButton?.isChecked = false

                // Check the current radio button
                rbWallet.isChecked = true
                selectedRadioButton = rbWallet

                // Update selected wallet
                selectedWallet = wallet
                tvSelectedWallet.text = wallet.name
                bottomSheetDialog.dismiss()
            }

            radioGroupWallets.addView(walletView)
        }

        btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    // Show recurrence bottom sheet
    private fun showRecurrenceBottomSheet(tvSelectedRecurrence: TextView) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_recurrence, null)
        bottomSheetDialog.setContentView(dialogView)

        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)
        val radioGroupRecurrence = dialogView.findViewById<RadioGroup>(R.id.radioGroupRecurrence)

        // Set the current selection
        when (selectedRecurrence) {
            "Daily" -> radioGroupRecurrence.check(R.id.rbDaily)
            "Weekly" -> radioGroupRecurrence.check(R.id.rbWeekly)
            "Biweekly" -> radioGroupRecurrence.check(R.id.rbBiweekly)
            "Monthly" -> radioGroupRecurrence.check(R.id.rbMonthly)
            "Yearly" -> radioGroupRecurrence.check(R.id.rbYearly)
        }

        radioGroupRecurrence.setOnCheckedChangeListener { _, checkedId ->
            selectedRecurrence = when (checkedId) {
                R.id.rbDaily -> "Daily"
                R.id.rbWeekly -> "Weekly"
                R.id.rbBiweekly -> "Biweekly"
                R.id.rbMonthly -> "Monthly"
                R.id.rbYearly -> "Yearly"
                else -> "Monthly"
            }
            tvSelectedRecurrence.text = selectedRecurrence
            bottomSheetDialog.dismiss()
        }

        btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    // Show set limit bottom sheet - simplified without category selection
    private fun showSetLimitBottomSheet(tvLimitPreview: TextView) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_set_limit, null)
        bottomSheetDialog.setContentView(dialogView)

        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)
        val etLimitAmount = dialogView.findViewById<EditText>(R.id.etLimitAmount)
        val btnSetLimit = dialogView.findViewById<Button>(R.id.btnSetLimit)

        // Pre-fill with current limit amount if set
        if (setLimitAmount > 0) {
            etLimitAmount.setText(setLimitAmount.toString())
        }

        btnSetLimit.setOnClickListener {
            val limitAmount = etLimitAmount.text.toString().toDoubleOrNull()

            if (limitAmount == null || limitAmount <= 0) {
                Toast.makeText(this, "Please enter a valid target amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Add minimum amount validation
            val minimumAmount = 200.0
            if (limitAmount < minimumAmount) {
                Toast.makeText(this, "Target amount must be at least ${formatCurrency(minimumAmount)}", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Store the limit amount as our target amount
            setLimitAmount = limitAmount

            // Update the preview in the main dialog
            updateLimitPreview(tvLimitPreview)

            // Show confirmation message
            Toast.makeText(this, "Target amount set: ${formatCurrency(limitAmount)}", Toast.LENGTH_SHORT).show()
            bottomSheetDialog.dismiss()
        }

        btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    //Reference: Based on code from Android Knowledge (2024),
    //"CRUD using Firebase Realtime Database in Android Studio using Kotlin | Create, Read, Update, Delete"
    //https://www.youtube.com/watch?v=oGyQMBKPuNY
    // Adds a new budget goal to the database for the current user
    private fun addBudgetGoal(
        name: String,
        targetAmount: Double,
        initialAmount: Double,
        category: Category? = null,
        wallet: Wallet? = null,
        recurrence: String = "Monthly"
    ) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Create a new unique goal entry under the user's budgetGoals
        val goalRef = database.child("users").child(userId).child("budgetGoals").push()
        val goalId = goalRef.key ?: UUID.randomUUID().toString()

        // Create BudgetGoal object with provided data
        val budgetGoal = BudgetGoal(
            id = goalId,
            name = name,
            targetAmount = targetAmount,
            currentAmount = initialAmount,
            createdAt = System.currentTimeMillis(),
            categoryId = category?.id,
            walletId = wallet?.id,
            recurrence = recurrence
        )

        goalRef.setValue(budgetGoal)
            .addOnSuccessListener {
                Toast.makeText(this, "Budget goal added successfully", Toast.LENGTH_SHORT).show()

                //immediate notification
                val notificationHelper = NotificationHelper(this)
                notificationHelper.showBudgetReminderNotification(budgetGoal)

                // Schedule budget reminder
                val reminderScheduler = BudgetReminderScheduler(this)
                reminderScheduler.scheduleRecurringReminder(budgetGoal)

                // If initial amount > 0, create a transaction for it
                if (initialAmount > 0) {
                    createBudgetTransfer(budgetGoal, initialAmount)
                }

                // Check if goal is already achieved with initial amount
                if (initialAmount >= targetAmount) {
                    showConfettiAnimation(budgetGoal)
                }

                // Reset selected values
                selectedCategory = null
                selectedWallet = null
                selectedRecurrence = "Monthly"
                setLimitAmount = 0.0
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add budget goal: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Shows a bottom sheet with details of a specific budget goal
    private fun showBudgetGoalDetails(budgetGoal: BudgetGoal) {
        val dialog = BottomSheetDialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_budget_goal_details, null)
        dialog.setContentView(dialogView)

        val tvGoalName = dialogView.findViewById<TextView>(R.id.tvGoalName)
        val tvProgress = dialogView.findViewById<TextView>(R.id.tvProgress)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val tvRemainingAmount = dialogView.findViewById<TextView>(R.id.tvRemainingAmount)
        val btnAddFunds = dialogView.findViewById<Button>(R.id.btnAddFunds)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)

        tvGoalName.text = budgetGoal.name

        // Calculate progress percentage
        val progress = if (budgetGoal.targetAmount > 0)
            ((budgetGoal.currentAmount / budgetGoal.targetAmount) * 100).toInt()
        else 0

        // Display progress
        tvProgress.text = "$progress%"
        progressBar.progress = progress

        // Show remaining amount to reach goal
        val remainingAmount = budgetGoal.targetAmount - budgetGoal.currentAmount
        tvRemainingAmount.text = "Remaining: ${formatCurrency(remainingAmount)}"

        btnAddFunds.setOnClickListener {
            dialog.dismiss()
            showAddFundsDialog(budgetGoal)
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Displays a bottom sheet dialog to add funds to an existing budget goal
    private fun showAddFundsDialog(budgetGoal: BudgetGoal) {
        val dialog = BottomSheetDialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_funds, null)
        dialog.setContentView(dialogView)

        val tvAvailableBalance = dialogView.findViewById<TextView>(R.id.tvAvailableBalance)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val btnAddFunds = dialogView.findViewById<Button>(R.id.btnAddFunds)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)

        // Display user's available balance
        tvAvailableBalance.text = "Available Balance: ${formatCurrency(availableBalance)}"

        btnAddFunds.setOnClickListener {
            val amountStr = etAmount.text.toString().trim()

            // Validate input
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull() ?: 0.0

            if (amount <= 0) {
                Toast.makeText(this, "Amount must be greater than zero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (amount > availableBalance) {
                Toast.makeText(this, "Amount cannot exceed available balance", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if adding the funds would exceed the target amount
            val newAmount = budgetGoal.currentAmount + amount
            if (newAmount > budgetGoal.targetAmount) {
                Toast.makeText(this, "Funds cannot exceed the target amount of ${formatCurrency(budgetGoal.targetAmount)}", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Add funds to budget goal
            addFundsToBudgetGoal(budgetGoal, amount)

            dialog.dismiss()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    //Reference: Based on code from Android Knowledge (2024),
    //"CRUD using Firebase Realtime Database in Android Studio using Kotlin | Create, Read, Update, Delete"
    //https://www.youtube.com/watch?v=oGyQMBKPuNY
    // Adds a specified amount of funds to an existing budget goal
    private fun addFundsToBudgetGoal(budgetGoal: BudgetGoal, amount: Double) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Calculate new total amount
        val goalRef = database.child("users").child(userId).child("budgetGoals").child(budgetGoal.id)

        // Update current amount
        val newAmount = budgetGoal.currentAmount + amount

        android.util.Log.d("BudgetsActivity", "Adding funds: $amount to goal: ${budgetGoal.name}")
        android.util.Log.d("BudgetsActivity", "Current amount: ${budgetGoal.currentAmount}, Target: ${budgetGoal.targetAmount}")
        android.util.Log.d("BudgetsActivity", "New amount will be: $newAmount")

        goalRef.child("currentAmount").setValue(newAmount)
            .addOnSuccessListener {
                android.util.Log.d("BudgetsActivity", "Successfully updated amount in database")

                // Update wallet balance (subtract the amount since it's being transferred to budget)
                updateWalletBalance(budgetGoal.walletId, amount, false)

                // Create a transaction record for this transfer
                createBudgetTransferRecord(budgetGoal, amount)
                Avaliable_Balance_Widget.updateWidget(this)

                // Update reminder schedule
                val reminderScheduler = BudgetReminderScheduler(this@BudgetsActivity)
                val notificationHelper = NotificationHelper(this@BudgetsActivity)

                if (newAmount >= budgetGoal.targetAmount) {
                    android.util.Log.d("BudgetsActivity", "Goal achieved! Sending notification...")

                    // Cancel reminders
                    reminderScheduler.cancelBudgetReminder(budgetGoal.id)

                    // Create updated budget goal for notification
                    val achievedGoal = budgetGoal.copy(currentAmount = newAmount)

                    // Check and award badges
                    badgeManager.checkAndAwardBadges(achievedGoal) { badge ->
                        // Show badge earned notification
                        Handler(Looper.getMainLooper()).postDelayed({
                            showBadgeEarnedDialog(badge)
                        }, 1000) // Show after confetti
                    }

                    // Show achievement notification with delay to ensure it's processed
                    Handler(Looper.getMainLooper()).postDelayed({
                        notificationHelper.showGoalAchievedNotification(achievedGoal)
                        android.util.Log.d("BudgetsActivity", "Achievement notification sent for: ${budgetGoal.name}")
                    }, 500)

                    // Show confetti animation
                    showConfettiAnimation(budgetGoal)

                    // Update spending widget
                    Category_Spending_Widget.updateWidgets(this@BudgetsActivity)

                    // Show success message
                    Toast.makeText(this, "🎉 Congratulations! Goal achieved! Check your notifications.", Toast.LENGTH_LONG).show()

                } else {
                    android.util.Log.d("BudgetsActivity", "Goal not yet achieved. Updating reminder...")
                    // Update reminder with new amount
                    reminderScheduler.updateBudgetReminder(budgetGoal.copy(currentAmount = newAmount))
                    Toast.makeText(this, "Funds added successfully", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("BudgetsActivity", "Failed to update amount: ${exception.message}")
                Toast.makeText(this, "Failed to add funds: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Helper method to create transaction record for budget transfers
    private fun createBudgetTransferRecord(budgetGoal: BudgetGoal, amount: Double) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val expenseRef = database.child("users").child(userId).child("expenses").push()
        val expenseId = expenseRef.key ?: UUID.randomUUID().toString()

        // Create a transaction object describing the transfer with wallet information
        val transaction = Transaction(
            id = expenseId,
            amount = amount,
            category = "Budget Transfer",
            categoryId = "budget_transfer",
            description = "Transfer to ${budgetGoal.name}",
            date = System.currentTimeMillis(),
            type = "expense",
            walletId = budgetGoal.walletId ?: "" // Use the budget goal's associated wallet
        )

        expenseRef.setValue(transaction)
            .addOnFailureListener {
                Toast.makeText(this, "Failed to record transfer: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Records a budget transfer as an "expense" transaction in the user's transaction history
    private fun createBudgetTransfer(budgetGoal: BudgetGoal, amount: Double) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val expenseRef = database.child("users").child(userId).child("expenses").push()
        val expenseId = expenseRef.key ?: UUID.randomUUID().toString()

        // Create a transaction object describing the transfer with wallet information
        val transaction = Transaction(
            id = expenseId,
            amount = amount,
            category = "Budget Transfer",
            categoryId = "budget_transfer",
            description = "Transfer to ${budgetGoal.name}",
            date = System.currentTimeMillis(),
            type = "expense",
            walletId = budgetGoal.walletId ?: "" // Use the budget goal's associated wallet
        )

        expenseRef.setValue(transaction)
            .addOnSuccessListener {
                // Update the wallet balance after successful transaction recording
                updateWalletBalance(budgetGoal.walletId, amount, false) // false = subtract amount
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to record transfer: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Helper method to update wallet balance
    private fun updateWalletBalance(walletId: String?, amount: Double, isIncome: Boolean) {
        if (walletId.isNullOrEmpty()) return

        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val walletRef = database.child("users").child(userId).child("wallets").child(walletId)

        walletRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val wallet = snapshot.getValue(Wallet::class.java)
                if (wallet != null) {
                    val newBalance = if (isIncome) {
                        wallet.balance + amount
                    } else {
                        wallet.balance - amount
                    }

                    // Ensure balance doesn't go negative
                    val finalBalance = if (newBalance < 0) 0.0 else newBalance

                    walletRef.child("balance").setValue(finalBalance)
                        .addOnSuccessListener {
                            android.util.Log.d("BudgetsActivity", "Wallet balance updated: $finalBalance")
                        }
                        .addOnFailureListener { exception ->
                            android.util.Log.e("BudgetsActivity", "Failed to update wallet balance: ${exception.message}")
                            Toast.makeText(this@BudgetsActivity, "Failed to update wallet balance", Toast.LENGTH_SHORT).show()
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("BudgetsActivity", "Failed to read wallet data: ${error.message}")
            }
        })
    }

    // Helper function used to format currency
    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        return format.format(amount)
    }

    // Method to show confetti animation
    private fun showConfettiAnimation(budgetGoal: BudgetGoal) {
        // Inflate the celebration overlay
        val celebrationView = LayoutInflater.from(this).inflate(R.layout.celebration_overlay, null)

        // Set goal name in the message
        val tvGoalName = celebrationView.findViewById<TextView>(R.id.tvGoalName)
        tvGoalName.text = "You've reached your goal for ${budgetGoal.name}!"

        // Add the view to the activity
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        rootView.addView(celebrationView, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        // Start the confetti animation
        val confettiView = celebrationView.findViewById<ConfettiView>(R.id.confettiView)
        confettiView.start()

        // Set up dismiss button
        val btnDismiss = celebrationView.findViewById<Button>(R.id.btnDismiss)
        btnDismiss.setOnClickListener {
            confettiView.stop()
            rootView.removeView(celebrationView)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (rootView.indexOfChild(celebrationView) != -1) {
                confettiView.stop()
                rootView.removeView(celebrationView)
            }
        }, 8000)
    }

    private fun showBadgeEarnedDialog(badge: Badge) {
        val dialog = BottomSheetDialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_badge_earned, null)
        dialog.setContentView(dialogView)

        val ivBadgeIcon = dialogView.findViewById<ImageView>(R.id.ivBadgeIcon)
        val tvBadgeName = dialogView.findViewById<TextView>(R.id.tvBadgeName)
        val tvBadgeDescription = dialogView.findViewById<TextView>(R.id.tvBadgeDescription)
        val btnViewBadges = dialogView.findViewById<Button>(R.id.btnViewBadges)
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)

        // Set badge details
        ivBadgeIcon.setImageResource(getBadgeIconResource(badge.iconName))
        tvBadgeName.text = badge.name
        tvBadgeDescription.text = badge.description

        btnViewBadges.setOnClickListener {
            dialog.dismiss()
            // Navigate to badges activity
            val intent = Intent(this, BadgesActivity::class.java)
            startActivity(intent)
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
}
