package com.pachira.prog7313poepachira

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.pachira.prog7313poepachira.AddExpenseActivity
import com.pachira.prog7313poepachira.adapters.Avaliable_Balance_Widget
import com.pachira.prog7313poepachira.adapters.CategorySummaryAdapter
import com.pachira.prog7313poepachira.adapters.Category_Spending_Widget
import com.pachira.prog7313poepachira.adapters.WalletAdapter
import com.pachira.prog7313poepachira.data.Category
import com.pachira.prog7313poepachira.data.CategorySummary
import com.pachira.prog7313poepachira.data.Transaction
import com.pachira.prog7313poepachira.data.Wallet
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : BaseActivity() {

    // Firebase references
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    // UI elements
    private lateinit var tvBalanceAmount: TextView
    private lateinit var tvIncomeAmount: TextView
    private lateinit var tvExpenseAmount: TextView
    private lateinit var btnAddIncome: MaterialButton
    private lateinit var btnAddExpense: MaterialButton
    private lateinit var btnLogout: LinearLayout
    private lateinit var cardIncome: CardView
    private lateinit var cardExpenses: CardView
    private lateinit var rvCategorySummaries: RecyclerView
    private lateinit var tvNoExpenses: TextView
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var btnDateFilter: LinearLayout //test
    private lateinit var tvDateRange: TextView
    private lateinit var btnClearFilters: ImageButton

    // Adapters
    private lateinit var categorySummaryAdapter: CategorySummaryAdapter

    // Data variables
    private var totalIncome = 0.0
    private var totalExpenses = 0.0
    private var currentBalance = 0.0
    private val categorySummaries = mutableListOf<CategorySummary>()
    private val categories = mutableListOf<Category>()
    private val categoryAmounts = mutableListOf<Pair<String, Double>>()
    private val allTransactions = mutableListOf<Transaction>()

    // Filter variables
    private var selectedCategoryId: String? = null
    private var startDate: Calendar? = null
    private var endDate: Calendar? = null
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    private val wallets = mutableListOf<Wallet>()
    private var selectedWalletId: String? = null
    private lateinit var btnWalletSelector: LinearLayout
    private lateinit var tvSelectedWallet: TextView

    // ValueEventListener references to remove when activity is destroyed
    private var expensesListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Check if user is logged in
        if (auth.currentUser == null) {
            // User is not logged in, redirect to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Initialize UI elements
        tvBalanceAmount = findViewById(R.id.tvBalanceAmount)
        tvIncomeAmount = findViewById(R.id.tvIncomeAmount)
        tvExpenseAmount = findViewById(R.id.tvExpenseAmount)
        btnAddIncome = findViewById(R.id.btnAddIncome)
        btnAddExpense = findViewById(R.id.btnAddExpense)
        btnLogout = findViewById(R.id.btnLogout)
        cardIncome = findViewById(R.id.cardIncome)
        cardExpenses = findViewById(R.id.cardExpenses)
        rvCategorySummaries = findViewById(R.id.rvCategorySummaries)
        tvNoExpenses = findViewById(R.id.tvNoExpenses)
        filterChipGroup = findViewById(R.id.filterChipGroup)
        btnDateFilter = findViewById(R.id.btnDateFilter)
        tvDateRange = findViewById(R.id.tvDateRange)
        btnClearFilters = findViewById(R.id.btnClearFilters)

        btnWalletSelector = findViewById(R.id.btnWalletSelector)
        tvSelectedWallet = findViewById(R.id.tvSelectedWallet)

        // Set up RecyclerView
        categorySummaryAdapter = CategorySummaryAdapter(
            categorySummaries,
            { categorySummary ->
                // Handle category summary click
                val intent = Intent(this, TransactionsActivity::class.java)
                intent.putExtra("transactionType", categorySummary.type)
                intent.putExtra("categoryId", categorySummary.categoryId)
                startActivity(intent)
            },
            { categorySummary ->
                // Handle edit click
                val category = categories.find { it.id == categorySummary.categoryId }
                category?.let {
                    showEditCategoryBottomSheet(it)
                }
            }
        )

        rvCategorySummaries.layoutManager = LinearLayoutManager(this)
        rvCategorySummaries.adapter = categorySummaryAdapter

        // Set click listeners
        btnAddIncome.setOnClickListener {
            startActivity(Intent(this, AddIncomeActivity::class.java))
        }

        btnAddExpense.setOnClickListener {
            if (currentBalance <= 0) {
                Toast.makeText(
                    this,
                    "You have no available balance to add expenses",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val intent = Intent(this, AddExpenseActivity::class.java)
                intent.putExtra("availableBalance", currentBalance)
                startActivity(intent)
            }
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        cardIncome.setOnClickListener {
            val intent = Intent(this, TransactionsActivity::class.java)
            intent.putExtra("transactionType", "income")
            startActivity(intent)
        }

        cardExpenses.setOnClickListener {
            val intent = Intent(this, TransactionsActivity::class.java)
            intent.putExtra("transactionType", "expense")
            startActivity(intent)
        }

        // Set up filter listeners
        btnDateFilter.setOnClickListener {
            showCustomDateRangeDialog()
        }

        btnClearFilters.setOnClickListener {
            clearAllFilters()
        }

        btnWalletSelector.setOnClickListener {
            showWalletSelectionBottomSheet()
        }

        // Load data from Firebase - this will be called after wallets are loaded
        loadWallets()

        // Setup bottom navigation
        setupBottomNavigation()
    }

    // Displays a custom confirmation dialog for logging out
    private fun showLogoutConfirmationDialog() {
        // Create a custom dialog
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.custom_logout_dialog)
        dialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(this, R.drawable.logout_dialog)
        )

        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.85).toInt()
        dialog.window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        dialog.window?.setDimAmount(0.9f)

        // Set up the buttons
        val yesButton = dialog.findViewById<Button>(R.id.btn_yes)
        val noButton = dialog.findViewById<Button>(R.id.btn_no)

        // Handle "Yes" click: Sign out and redirect to login screen
        yesButton.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Successfully logged out", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            dialog.dismiss()
        }

        // Handle "No" click: Dismiss the dialog
        noButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Remove listeners to prevent memory leaks
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            expensesListener?.let {
                database.child("users").child(userId).child("expenses").removeEventListener(it)
            }
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // First load wallets, then transactions

        // Listen for income changes
        database.child("users").child(userId).child("income")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    totalIncome = 0.0
                    for (incomeSnapshot in snapshot.children) {
                        val income = incomeSnapshot.getValue(Transaction::class.java)
                        income?.let {
                            // Filter by selected wallet if one is selected
                            if (selectedWalletId == null || it.walletId == selectedWalletId) {
                                totalIncome += it.amount
                            }
                        }
                    }
                    updateIncomeUI()
                    calculateBalance()
                    // Update spending widget
                    Category_Spending_Widget.updateWidgets(this@DashboardActivity)
                    Avaliable_Balance_Widget.updateWidget(this@DashboardActivity)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })

        // Listen for expense changes
        database.child("users").child(userId).child("expenses")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    totalExpenses = 0.0
                    allTransactions.clear()

                    for (expenseSnapshot in snapshot.children) {
                        val expense = expenseSnapshot.getValue(Transaction::class.java)
                        expense?.let {
                            // Filter by selected wallet if one is selected
                            if (selectedWalletId == null || it.walletId == selectedWalletId) {
                                totalExpenses += it.amount
                                allTransactions.add(it)
                            }
                        }
                    }
                    updateExpenseUI()
                    calculateBalance()
                    Avaliable_Balance_Widget.updateWidget(this@DashboardActivity)
                    applyFilters()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })

        // Load category summaries
        loadCategorySummaries()
    }

    // Loads user-defined categories and creates filter chips
    private fun loadCategorySummaries() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // First, load all categories
        database.child("users").child(userId).child("categories")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    categories.clear()
                    filterChipGroup.removeAllViews()

                    // Add "All Categories" chip
                    val allCategoriesChip = createCategoryChip("All Categories", null, true)
                    filterChipGroup.addView(allCategoriesChip)

                    for (categorySnapshot in snapshot.children) {
                        val category = categorySnapshot.getValue(Category::class.java)
                        category?.let {
                            categories.add(it)

                            // Only add expense categories to filter
                            if (it.type == "expense") {
                                val chip = createCategoryChip(it.name, it.id, false)
                                filterChipGroup.addView(chip)
                            }
                        }
                    }

                    // Now load transactions to calculate amounts per category
                    calculateCategorySummaries(categories)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    // Creates a category chip for filtering transactions
    private fun createCategoryChip(name: String, id: String?, isSelected: Boolean): Chip {
        val chip = Chip(this).apply {
            text = name
            isCheckable = true
            isChecked = isSelected

            // Style the chip
            setTextColor(Color.BLACK) // Set text color to black

            // Set initial stroke based on selection state
            chipStrokeWidth = if (isSelected) 2f else 0f
            chipStrokeColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.purple))

            chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.chip_background)
            elevation = 5f
            rippleColor = ColorStateList.valueOf(Color.TRANSPARENT)
            isCheckedIconVisible = false
            isCloseIconVisible = false
        }

        // Handle chip selection - ensure only one chip is selected at a time
        chip.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // Uncheck all other chips and remove their borders
                for (i in 0 until filterChipGroup.childCount) {
                    val otherChip = filterChipGroup.getChildAt(i) as? Chip
                    if (otherChip != buttonView) {
                        otherChip?.isChecked = false
                        otherChip?.chipStrokeWidth = 0f // Remove border from other chips
                    }
                }

                // Add border to selected chip
                (buttonView as Chip).chipStrokeWidth = 2f

                selectedCategoryId = id
                applyFilters()
            } else if (filterChipGroup.checkedChipId == View.NO_ID) {
                // If no chip is selected, keep this one selected
                buttonView.isChecked = true
                (buttonView as Chip).chipStrokeWidth = 2f
            } else {
                // Remove border when unchecked
                (buttonView as Chip).chipStrokeWidth = 0f
            }
        }

        return chip
    }

    // Shows a custom dialog to select a date range filter
    private fun showCustomDateRangeDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_date_range_picker, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val tvFromDate = dialogView.findViewById<TextView>(R.id.tvFromDate)
        val tvToDate = dialogView.findViewById<TextView>(R.id.tvToDate)
        val btnOk = dialogView.findViewById<Button>(R.id.btnOk)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        val fromCalendar = Calendar.getInstance()
        val toCalendar = Calendar.getInstance()

        // Show custom calendar dialog when "From" date is clicked
        tvFromDate.setOnClickListener {
            showCustomCalendarDialog(true, fromCalendar) { selected ->
                startDate = selected
                tvFromDate.text = formatCalendarDate(selected)
            }
        }

        // Show custom calendar dialog when "To" date is clicked
        tvToDate.setOnClickListener {
            showCustomCalendarDialog(false, toCalendar) { selected ->
                endDate = selected
                tvToDate.text = formatCalendarDate(selected)
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        // Apply filter when both dates are selected
        btnOk.setOnClickListener {
            if (startDate != null && endDate != null) {
                updateDateRangeUI()
                applyFilters()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please select both dates", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    // Shows a custom calendar popup and returns the selected date
    private fun showCustomCalendarDialog(
        isStartDate: Boolean,
        calendar: Calendar,
        onDateSelected: (Calendar) -> Unit
    ) {
        val customCalendarDialog = CustomCalendarDialog(this, calendar) { newDate ->
            if (isStartDate) {
                newDate.set(Calendar.HOUR_OF_DAY, 0)
                newDate.set(Calendar.MINUTE, 0)
                newDate.set(Calendar.SECOND, 0)
                newDate.set(Calendar.MILLISECOND, 0)
            } else {
                newDate.set(Calendar.HOUR_OF_DAY, 23)
                newDate.set(Calendar.MINUTE, 59)
                newDate.set(Calendar.SECOND, 59)
                newDate.set(Calendar.MILLISECOND, 999)
            }
            onDateSelected(newDate)
        }

        customCalendarDialog.show()

        customCalendarDialog.window?.apply {
            setBackgroundDrawableResource(R.drawable.logout_dialog)
            setLayout(
                (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setDimAmount(0.9f)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    // Updates the date range text view with the selected range
    private fun updateDateRangeUI() {
        if (startDate != null && endDate != null) {
            val startDateStr = dateFormat.format(startDate!!.time)
            val endDateStr = dateFormat.format(endDate!!.time)
            tvDateRange.text = "$startDateStr - $endDateStr"
            tvDateRange.visibility = View.VISIBLE
        } else {
            tvDateRange.visibility = View.GONE
        }
    }


    private fun clearAllFilters() {
        // Reset category filter
        selectedCategoryId = null
        for (i in 0 until filterChipGroup.childCount) {
            val chip = filterChipGroup.getChildAt(i) as? Chip
            chip?.isChecked = i == 0 // Select only the first chip (All Categories)
            if (i == 0) {
                chip?.chipStrokeWidth = 2f
            } else {
                chip?.chipStrokeWidth = 0f
            }
        }

        // Reset date filter
        startDate = null
        endDate = null
        tvDateRange.visibility = View.GONE

        // Reset to main wallet
        if (wallets.isNotEmpty()) {
            val mainWallet = wallets.find { it.name == "Main Account" } ?: wallets[0]
            selectedWalletId = mainWallet.id
            updateSelectedWalletUI()

            // Reload data with main wallet selected
            loadUserData()
        }
    }


    // Applies active filters to the transactions list
    private fun applyFilters() {
        // Filter transactions based on selected criteria
        val filteredTransactions = allTransactions.filter { transaction ->
            var matchesCategory = true
            var matchesDateRange = true

            // Apply category filter if selected
            if (selectedCategoryId != null) {
                matchesCategory = transaction.categoryId == selectedCategoryId
            }

            // Apply date range filter if selected
            if (startDate != null && endDate != null) {
                val transactionDate = Calendar.getInstance()
                transactionDate.timeInMillis = transaction.date
                matchesDateRange = transactionDate.timeInMillis >= startDate!!.timeInMillis &&
                        transactionDate.timeInMillis <= endDate!!.timeInMillis
            }

            matchesCategory && matchesDateRange
        }

        // Calculate summaries based on filtered transactions
        calculateFilteredCategorySummaries(filteredTransactions)
    }

    // Calculates and updates summaries for each expense category based on filtered transactions
    private fun calculateFilteredCategorySummaries(filteredTransactions: List<Transaction>) {
        // Clear existing summaries
        categorySummaries.clear()
        categoryAmounts.clear()

        // Get only the categories of type "expense"
        val expenseCategories = categories.filter { it.type == "expense" }

        // Calculate total for each category from filtered transactions
        for (transaction in filteredTransactions) {
            val categoryId = transaction.categoryId
            val amount = transaction.amount

            // Find if category already exists in our list
            val existingPairIndex = categoryAmounts.indexOfFirst { pair -> pair.first == categoryId }

            if (existingPairIndex != -1) {
                // Update existing amount
                val existingPair = categoryAmounts[existingPairIndex]
                val newTotal = existingPair.second + amount
                categoryAmounts[existingPairIndex] = Pair(categoryId, newTotal)
            } else {
                // Add new category amount
                categoryAmounts.add(Pair(categoryId, amount))
            }
        }

        // Create category summaries
        for (category in expenseCategories) {
            val amountPair = categoryAmounts.find { it.first == category.id }
            val amount = amountPair?.second ?: 0.0
            val limit = category.budgetLimit
            val percentage = if (limit > 0) ((amount / limit) * 100).toInt().coerceAtMost(100) else 0

            // Only add categories that have transactions after filtering
            if (amount > 0 || selectedCategoryId == category.id) {
                val summary = CategorySummary(
                    categoryId = category.id,
                    name = category.name,
                    colorHex = category.colorHex,
                    iconName = category.iconName,
                    amount = amount,
                    limit = limit,
                    percentage = percentage,
                    type = "expense"
                )

                categorySummaries.add(summary)
            }
        }

        // Sort by percentage of limit (highest first), then by amount
        categorySummaries.sortWith(compareByDescending<CategorySummary> {
            if (it.limit > 0) it.percentage else 0
        }.thenByDescending {
            it.amount
        })

        // Check if there are category summaries exist
        if (categorySummaries.isEmpty()) {
            tvNoExpenses.visibility = View.VISIBLE
            rvCategorySummaries.visibility = View.GONE
        } else {
            tvNoExpenses.visibility = View.GONE
            rvCategorySummaries.visibility = View.VISIBLE
        }

        // Update UI
        categorySummaryAdapter.notifyDataSetChanged()
    }

    // Retrieves all expense transactions, calculates summaries, and applies filters if active
    private fun calculateCategorySummaries(categories: List<Category>) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Clear existing summaries
        categorySummaries.clear()
        categoryAmounts.clear()

        // Process expense categories
        val expenseCategories = categories.filter { it.type == "expense" }

        // Remove previous listener if it exists
        expensesListener?.let {
            database.child("users").child(userId).child("expenses").removeEventListener(it)
        }

        // Create and add new listener
        expensesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Clear category amounts before recalculating
                categoryAmounts.clear()
                allTransactions.clear()

                // Calculate total for each category
                for (transactionSnapshot in snapshot.children) {
                    val transaction = transactionSnapshot.getValue(Transaction::class.java)
                    transaction?.let {
                        // Filter by selected wallet if one is selected
                        if (selectedWalletId == null || it.walletId == selectedWalletId) {
                            allTransactions.add(it)
                            val categoryId = it.categoryId
                            val amount = it.amount

                            // Find if category already exists in our list
                            val existingPairIndex = categoryAmounts.indexOfFirst { pair -> pair.first == categoryId }

                            if (existingPairIndex != -1) {
                                // Update existing amount
                                val existingPair = categoryAmounts[existingPairIndex]
                                val newTotal = existingPair.second + amount
                                categoryAmounts[existingPairIndex] = Pair(categoryId, newTotal)
                            } else {
                                // Add new category amount
                                categoryAmounts.add(Pair(categoryId, amount))
                            }
                        }
                    }
                }

                // Apply filters if any are active
                if (selectedCategoryId != null || (startDate != null && endDate != null)) {
                    applyFilters()
                } else {
                    // Clear existing summaries before adding new ones
                    categorySummaries.clear()

                    // Create category summaries
                    for (category in expenseCategories) {
                        val amountPair = categoryAmounts.find { it.first == category.id }
                        val amount = amountPair?.second ?: 0.0
                        val limit = category.budgetLimit
                        val percentage = if (limit > 0) ((amount / limit) * 100).toInt().coerceAtMost(100) else 0

                        val summary = CategorySummary(
                            categoryId = category.id,
                            name = category.name,
                            colorHex = category.colorHex,
                            iconName = category.iconName,
                            amount = amount,
                            limit = limit,
                            percentage = percentage,
                            type = "expense"
                        )

                        categorySummaries.add(summary)
                    }

                    // Sort by percentage of limit (highest first), then by amount
                    categorySummaries.sortWith(compareByDescending<CategorySummary> {
                        if (it.limit > 0) it.percentage else 0
                    }.thenByDescending {
                        it.amount
                    })

                    //Check if there are category summaries exist
                    if (categorySummaries.isEmpty()) {
                        tvNoExpenses.visibility = View.VISIBLE
                        rvCategorySummaries.visibility = View.GONE
                    } else {
                        tvNoExpenses.visibility = View.GONE
                        rvCategorySummaries.visibility = View.VISIBLE
                    }

                    // Update UI
                    categorySummaryAdapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
                Toast.makeText(this@DashboardActivity, "Failed to load expenses: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Add the listener
        database.child("users").child(userId).child("expenses").addValueEventListener(expensesListener!!)
    }

    // Displays a bottom sheet dialog for creating or editing a category.
    private fun showEditCategoryBottomSheet(category: Category) {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_category, null)
        bottomSheetDialog.setContentView(dialogView)

        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialog_title)
        val etCategoryName = dialogView.findViewById<EditText>(R.id.et_category_name)
        val budgetLimitContainer = dialogView.findViewById<LinearLayout>(R.id.budget_limit_container)
        val etBudgetLimit = dialogView.findViewById<EditText>(R.id.et_budget_limit)
        val colorPreview = dialogView.findViewById<View>(R.id.color_preview)
        val colorContainer = dialogView.findViewById<LinearLayout>(R.id.color_container)
        val iconContainer = dialogView.findViewById<LinearLayout>(R.id.icon_container)
        val btnCreateCategory = dialogView.findViewById<Button>(R.id.btn_create_category)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btn_close)

        // Set up for editing
        dialogTitle.text = "Edit Category"
        etCategoryName.setText(category.name)
        btnCreateCategory.text = "Update Category"

        // Show/hide budget limit based on category type
        if (category.type == "expense") {
            budgetLimitContainer.visibility = View.VISIBLE
            etBudgetLimit.setText(category.budgetLimit.toString())
        } else {
            budgetLimitContainer.visibility = View.GONE
        }

        // Available colors
        val colors = listOf(
            "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
            "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50",
            "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800",
            "#FF5722", "#795548", "#9E9E9E"
        )

        // Available icons
        val icons = listOf(
            "", // No icon option
            "ic_category_food", "ic_category_shopping", "ic_category_transport",
            "ic_category_home", "ic_category_entertainment", "ic_category_health",
            "ic_category_salary", "ic_category_gift", "ic_category_investment"
        )

        var selectedColor = category.colorHex
        var selectedIcon = category.iconName

        // Set initial color preview
        try {
            val drawable = colorPreview.background.mutate()
            drawable.setTint(Color.parseColor(selectedColor))
            colorPreview.background = drawable
        } catch (e: Exception) {
            colorPreview.setBackgroundColor(Color.parseColor(selectedColor))
        }

        // Add color options
        for (color in colors) {
            val colorItemView = LayoutInflater.from(this).inflate(R.layout.item_color_select, colorContainer, false)
            val colorView = colorItemView.findViewById<View>(R.id.color_view)
            val checkIcon = colorItemView.findViewById<ImageView>(R.id.check_icon)

            try {
                val drawable = colorView.background.mutate()
                drawable.setTint(Color.parseColor(color))
                colorView.background = drawable
            } catch (e: Exception) {
                colorView.setBackgroundColor(Color.parseColor(color))
            }

            if (color == selectedColor) {
                checkIcon.visibility = View.VISIBLE
            }

            colorItemView.setOnClickListener {
                // Update selected color
                selectedColor = color

                // Update color preview
                try {
                    val drawable = colorPreview.background.mutate()
                    drawable.setTint(Color.parseColor(color))
                    colorPreview.background = drawable
                } catch (e: Exception) {
                    colorPreview.setBackgroundColor(Color.parseColor(color))
                }

                // Update check icons
                for (i in 0 until colorContainer.childCount) {
                    val child = colorContainer.getChildAt(i)
                    child.findViewById<ImageView>(R.id.check_icon).visibility =
                        if (i == colors.indexOf(color)) View.VISIBLE else View.GONE
                }
            }

            colorContainer.addView(colorItemView)
        }

        // Add icon options (including "No Icon" option)
        for (icon in icons) {
            val iconItemView = LayoutInflater.from(this).inflate(R.layout.item_icon_select, iconContainer, false)
            val iconView = iconItemView.findViewById<ImageView>(R.id.icon_view)
            val selectionIndicator = iconItemView.findViewById<View>(R.id.selection_indicator)

            if (icon.isEmpty()) {
                // "No Icon" option
                iconView.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                iconView.setColorFilter(Color.GRAY)
            } else {
                // Get drawable resource ID
                val resourceId = resources.getIdentifier(icon, "drawable", packageName)
                iconView.setImageResource(resourceId)
            }

            if (icon == selectedIcon) {
                selectionIndicator.visibility = View.VISIBLE
            }

            iconItemView.setOnClickListener {
                // Update selected icon
                selectedIcon = icon

                // Update selection indicators
                for (i in 0 until iconContainer.childCount) {
                    val child = iconContainer.getChildAt(i)
                    child.findViewById<View>(R.id.selection_indicator).visibility =
                        if (i == icons.indexOf(icon)) View.VISIBLE else View.GONE
                }
            }

            iconContainer.addView(iconItemView)
        }

        btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        btnCreateCategory.setOnClickListener {
            val name = etCategoryName.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check for duplicate category name (only if name has changed)
            if (name != category.name && isDuplicateCategory(name, category.type)) {
                Toast.makeText(this, "A category with this name already exists", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Parse budget limit
            val budgetLimitStr = etBudgetLimit.text.toString().trim()
            val budgetLimit = if (budgetLimitStr.isEmpty() || category.type != "expense")
                0.0
            else
                budgetLimitStr.toDoubleOrNull() ?: 0.0

            // Update existing category
            updateCategory(category.id, name, selectedColor, selectedIcon, budgetLimit)

            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun isDuplicateCategory(name: String, type: String): Boolean {
        return categories.any {
            it.name.equals(name, ignoreCase = true) && it.type == type
        }
    }

    private fun updateCategory(categoryId: String, name: String, colorHex: String, iconName: String, budgetLimit: Double) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val categoryRef = database.child("users").child(userId).child("categories").child(categoryId)

        // Instead of using a HashMap, update each field individually
        categoryRef.child("name").setValue(name)
        categoryRef.child("colorHex").setValue(colorHex)
        categoryRef.child("iconName").setValue(iconName)
        categoryRef.child("budgetLimit").setValue(budgetLimit)
            .addOnSuccessListener {
                Toast.makeText(this, "Category updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update category: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateIncomeUI() {
        tvIncomeAmount.text = formatCurrency(totalIncome)
    }

    private fun updateExpenseUI() {
        tvExpenseAmount.text = formatCurrency(totalExpenses)
    }

    private fun calculateBalance() {
        currentBalance = totalIncome - totalExpenses
        // Ensure balance is never negative
        if (currentBalance < 0) {
            currentBalance = 0.0
        }
        tvBalanceAmount.text = formatCurrency(currentBalance)
    }

    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        return format.format(amount)
    }

    //test
    private fun formatCalendarDate(calendar: Calendar): String {
        val day = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
        val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
        val year = calendar.get(Calendar.YEAR)
        return "$day/$month/$year"
    }

    private fun loadWallets() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        database.child("users").child(userId).child("wallets")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    wallets.clear()

                    for (walletSnapshot in snapshot.children) {
                        val wallet = walletSnapshot.getValue(Wallet::class.java)
                        wallet?.let {
                            wallets.add(it)
                        }
                    }

                    // If no wallets exist, create a default one
                    if (wallets.isEmpty()) {
                        createDefaultWallet()
                    } else {
                        // Prioritize "Main Account" wallet, otherwise use the first wallet
                        val mainWallet = wallets.find { it.name == "Main Account" } ?: wallets[0]
                        selectedWalletId = mainWallet.id
                        updateSelectedWalletUI()

                        // Load data for the selected wallet
                        loadUserData()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@DashboardActivity, "Failed to load wallets", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun createDefaultWallet() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val walletRef = database.child("users").child(userId).child("wallets").push()
        val walletId = walletRef.key ?: UUID.randomUUID().toString()

        val defaultWallet = Wallet(
            id = walletId,
            name = "Main Account",
            balance = 0.0,
            iconName = "ic_wallet",
            colorHex = "#6200EE",
            type = "bank",
            isActive = true,
            createdAt = System.currentTimeMillis()
        )

        walletRef.setValue(defaultWallet)
            .addOnSuccessListener {
                selectedWalletId = walletId
                updateSelectedWalletUI()
                Toast.makeText(this, "Default wallet created", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to create default wallet", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showWalletSelectionBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_wallet_selection, null)
        bottomSheetDialog.setContentView(dialogView)

        val rvWallets = dialogView.findViewById<RecyclerView>(R.id.rvWallets)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)
        val btnAddWallet = dialogView.findViewById<Button>(R.id.btnAddWallet)

        val walletAdapter = WalletAdapter(wallets) { wallet ->
            selectedWalletId = wallet.id
            updateSelectedWalletUI()
            loadUserData() // Reload data with new wallet filter
            bottomSheetDialog.dismiss()
        }

        rvWallets.layoutManager = LinearLayoutManager(this)
        rvWallets.adapter = walletAdapter

        // Set currently selected wallet
        selectedWalletId?.let { walletAdapter.setSelectedWallet(it) }

        btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        btnAddWallet.setOnClickListener {
            bottomSheetDialog.dismiss()
            startActivity(Intent(this, WalletsActivity::class.java))
        }

        bottomSheetDialog.show()
    }

    private fun updateSelectedWalletUI() {
        val selectedWallet = wallets.find { it.id == selectedWalletId }
        if (selectedWallet != null) {
            tvSelectedWallet.text = selectedWallet.name
            tvSelectedWallet.visibility = View.VISIBLE
        } else if (wallets.isNotEmpty()) {
            // Fallback to first wallet if selected wallet not found
            selectedWalletId = wallets[0].id
            tvSelectedWallet.text = wallets[0].name
            tvSelectedWallet.visibility = View.VISIBLE
        }
    }
}
