package com.pachira.prog7313poepachira

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.pachira.prog7313poepachira.adapters.Avaliable_Balance_Widget
import com.pachira.prog7313poepachira.adapters.CategoryAdapter
import com.pachira.prog7313poepachira.data.Category
import com.pachira.prog7313poepachira.data.Transaction
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import com.pachira.prog7313poepachira.adapters.Category_Spending_Widget
import com.pachira.prog7313poepachira.adapters.WalletAdapter
import com.pachira.prog7313poepachira.data.Wallet

class AddIncomeActivity : AppCompatActivity() {

    private lateinit var etAmount: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnSave: Button
    private lateinit var tvDate: TextView
    private lateinit var btnDatePicker: LinearLayout
    private lateinit var rvCategories: RecyclerView
    private lateinit var btnAddCategory: View
    private lateinit var btnBack: ImageButton
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var btnPreviousMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton

    private lateinit var btnWalletSelector: LinearLayout
    private lateinit var tvSelectedWallet: TextView
    private val wallets = mutableListOf<Wallet>()
    private var selectedWalletId: String? = null

    private val categories = mutableListOf<Category>()
    private var selectedDate = Calendar.getInstance()
    private var selectedCategoryId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_income)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize UI elements
        etAmount = findViewById(R.id.etAmount)
        etDescription = findViewById(R.id.etDescription)
        btnSave = findViewById(R.id.btnSave)
        tvDate = findViewById(R.id.tvDate)
        btnDatePicker = findViewById(R.id.btnDatePicker)
        rvCategories = findViewById(R.id.rvCategories)
        btnAddCategory = findViewById(R.id.btnAddCategory)
        btnBack = findViewById(R.id.btnBack)
        btnPreviousMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)

        btnWalletSelector = findViewById(R.id.btnWalletSelector)
        tvSelectedWallet = findViewById(R.id.tvSelectedWallet)

        if (savedInstanceState != null) {
            selectedCategoryId = savedInstanceState.getString("selectedCategoryId")
        }

        // Set up RecyclerView for categories
        categoryAdapter = CategoryAdapter(categories) { category ->
            selectedCategoryId = category.id
            // Update UI to show selected category
            categoryAdapter.setSelectedCategory(category.id)
        }

        rvCategories.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvCategories.adapter = categoryAdapter

        // Set up date picker
        updateDateDisplay()
        btnDatePicker.setOnClickListener {
            showDatePicker()
        }

        // Set up add category button
        btnAddCategory.setOnClickListener {
            showAddCategoryBottomSheet()
        }

        // Set up back button
        btnBack.setOnClickListener {
            finish()
        }

        // Set up save button
        btnSave.setOnClickListener {
            saveIncome()
        }

        btnPreviousMonth.setOnClickListener{
            selectedDate.add(Calendar.MONTH,-1)
            updateDateDisplay()
        }
        btnNextMonth.setOnClickListener{
            selectedDate.add(Calendar.MONTH,1)
            updateDateDisplay()
        }

        btnWalletSelector.setOnClickListener {
            showWalletSelectionBottomSheet()
        }

        // Load categories from Firebase
        loadCategories()

        loadWallets()
    }

    //Reference: Based on code from Mayank Shukla (2023),
    // "Managing Orientation Changes with onSaveInstanceState and onRestoreInstanceState"
    // https://www.youtube.com/watch?v=HFbsBJ-ERYU
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the selected category
        outState.putString("selectedCategoryId", selectedCategoryId)
    }

    //Reference: Based on code from Mayank Shukla (2023),
    // "Managing Orientation Changes with onSaveInstanceState and onRestoreInstanceState"
    // https://www.youtube.com/watch?v=HFbsBJ-ERYU
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Restore the selected category
        selectedCategoryId = savedInstanceState.getString("selectedCategoryId")
    }

    //Reference: Based on code from Coding with Dev (2024),
    //android date picker dialog example | DatePickerDialog - Android Studio Tutorial | Kotlin
    //https://www.youtube.com/watch?v=DpL8DhCNKdE
    // Updates the displayed date in the TextView to reflect the currently selected date.
    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("MMMM - yyyy", Locale.getDefault())
        val dayFormat = SimpleDateFormat("d", Locale.getDefault())

        val monthYear = dateFormat.format(selectedDate.time)
        val day = dayFormat.format(selectedDate.time)

        tvDate.text = monthYear

        // Update calendar view (in a real implementation, you would update the calendar grid)
    }

    //Reference: Based on code from Coding with Dev (2024),
    //android date picker dialog example | DatePickerDialog - Android Studio Tutorial | Kotlin
    //https://www.youtube.com/watch?v=DpL8DhCNKdE
    private fun showDatePicker() {
        val customCalendarDialog = CustomCalendarDialog(
            this,
            selectedDate
        ) { newDate ->
            selectedDate.timeInMillis = newDate.timeInMillis
            updateDateDisplay()
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

    //Reference: Based on code from Android Knowledge (2024),
    //"CRUD using Firebase Realtime Database in Android Studio using Kotlin | Create, Read, Update, Delete"
    //https://www.youtube.com/watch?v=oGyQMBKPuNY
    // Retrieves the income categories for the currently authenticated user.
    // If no categories are found, it adds default categories.
    private fun loadCategories() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val categoriesRef = database.reference
            .child("users")
            .child(userId)
            .child("categories")
            .orderByChild("type")
            .equalTo("income")

        categoriesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categories.clear()

                for (categorySnapshot in snapshot.children) {
                    val category = categorySnapshot.getValue(Category::class.java)
                    category?.let {
                        categories.add(it)
                    }
                }

                // If no categories exist, add default ones
                if (categories.isEmpty()) {
                    addDefaultCategories()
                } else {
                    categoryAdapter.notifyDataSetChanged()
                }

                if (selectedCategoryId != null) {
                    categoryAdapter.setSelectedCategory(selectedCategoryId!!)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AddIncomeActivity, "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
        })
    }

    //Reference: Based on code from Android Knowledge (2024),
    //"CRUD using Firebase Realtime Database in Android Studio using Kotlin | Create, Read, Update, Delete"
    //https://www.youtube.com/watch?v=oGyQMBKPuNY
    // Adds default income categories
    private fun addDefaultCategories() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val categoriesRef = database.reference.child("users").child(userId).child("categories")

        val defaultCategories = listOf(
            Category(
                id = UUID.randomUUID().toString(),
                name = "Salary",
                colorHex = "#36D1DC",
                iconName = "ic_category_salary",
                type = "income",
                budgetLimit = 0.0
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Gifts",
                colorHex = "#FFDD00",
                iconName = "ic_category_gift",
                type = "income",
                budgetLimit = 0.0
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Investment",
                colorHex = "#FF5733",
                iconName = "ic_category_investment",
                type = "income",
                budgetLimit = 0.0
            )
        )

        for (category in defaultCategories) {
            categoriesRef.child(category.id).setValue(category)
        }
    }

    // Displays a bottom sheet dialog for creating or editing a category.
    private fun showAddCategoryBottomSheet(categoryToEdit: Category? = null) {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_category, null)
        bottomSheetDialog.setContentView(dialogView)

        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialog_title)
        val etCategoryName = dialogView.findViewById<EditText>(R.id.et_category_name)
        val budgetLimitContainer = dialogView.findViewById<LinearLayout>(R.id.budget_limit_container)
        val colorPreview = dialogView.findViewById<View>(R.id.color_preview)
        val colorContainer = dialogView.findViewById<LinearLayout>(R.id.color_container)
        val iconContainer = dialogView.findViewById<LinearLayout>(R.id.icon_container)
        val btnCreateCategory = dialogView.findViewById<Button>(R.id.btn_create_category)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btn_close)

        // Hide budget limit for income categories
        budgetLimitContainer.visibility = View.GONE

        // Set up for editing if a category is provided
        if (categoryToEdit != null) {
            dialogTitle.text = "Edit Category"
            etCategoryName.setText(categoryToEdit.name)
            btnCreateCategory.text = "Update Category"
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

        var selectedColor = categoryToEdit?.colorHex ?: colors[9] // Default to green or existing color
        var selectedIcon = categoryToEdit?.iconName ?: icons[6] // Default to salary icon for income or existing icon

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
            if (categoryToEdit == null || name != categoryToEdit.name) {
                if (isDuplicateCategory(name)) {
                    Toast.makeText(this, "A category with this name already exists", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // For income categories, budget limit is not applicable
            val budgetLimit = 0.0

            if (categoryToEdit != null) {
                // Update existing category
                updateCategory(categoryToEdit.id, name, selectedColor, selectedIcon, budgetLimit)
            } else {
                // Add new category
                addNewCategory(name, selectedColor, selectedIcon, budgetLimit)
            }

            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun isDuplicateCategory(name: String): Boolean {
        return categories.any {
            it.name.equals(name, ignoreCase = true)
        }
    }

    //Reference: Based on code from Android Knowledge (2024),
    //"CRUD using Firebase Realtime Database in Android Studio using Kotlin | Create, Read, Update, Delete"
    //https://www.youtube.com/watch?v=oGyQMBKPuNY
    private fun addNewCategory(name: String, colorHex: String, iconName: String, budgetLimit: Double) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val categoriesRef = database.reference.child("users").child(userId).child("categories")
        val newCategoryId = categoriesRef.push().key ?: UUID.randomUUID().toString()

        val newCategory = Category(
            id = newCategoryId,
            name = name,
            colorHex = colorHex,
            iconName = iconName,
            type = "income",
            budgetLimit = budgetLimit
        )

        categoriesRef.child(newCategoryId).setValue(newCategory)
            .addOnSuccessListener {
                Toast.makeText(this, "Category added successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add category: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    //Reference: Based on code from Android Knowledge (2024),
    //"CRUD using Firebase Realtime Database in Android Studio using Kotlin | Create, Read, Update, Delete"
    //https://www.youtube.com/watch?v=oGyQMBKPuNY
    //Updates an existing category
    private fun updateCategory(categoryId: String, name: String, colorHex: String, iconName: String, budgetLimit: Double) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val categoryRef = database.reference.child("users").child(userId).child("categories").child(categoryId)

        // Update each field individually
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

    //Reference: Based on code from Android Knowledge (2024),
    //"CRUD using Firebase Realtime Database in Android Studio using Kotlin | Create, Read, Update, Delete"
    //https://www.youtube.com/watch?v=oGyQMBKPuNY
    //Saves  and validates the new expense ensuring all required fields are filled
    private fun saveIncome() {
        val amountStr = etAmount.text.toString().replace("[^0-9.]".toRegex(), "")
        val description = etDescription.text.toString()

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedCategoryId == null) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedWalletId == null) {
            Toast.makeText(this, "Please select a wallet", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to add income", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        val userId = currentUser.uid
        val incomeRef = database.reference.child("users").child(userId).child("income").push()
        val incomeId = incomeRef.key ?: UUID.randomUUID().toString()

        // Find the selected category
        val selectedCategory = categories.find { it.id == selectedCategoryId }

        val transaction = Transaction(
            id = incomeId,
            amount = amount,
            category = selectedCategory?.name ?: "",
            categoryId = selectedCategoryId ?: "",
            description = description,
            date = selectedDate.timeInMillis,
            type = "income",
            walletId = selectedWalletId ?: ""
        )

        incomeRef.setValue(transaction)
            .addOnSuccessListener {
                // Update wallet balance
                updateWalletBalance(selectedWalletId!!, amount, true)
                Toast.makeText(this, "Income added successfully", Toast.LENGTH_SHORT).show()

                // Update spending widget
                Category_Spending_Widget.updateWidgets(this@AddIncomeActivity)
                Avaliable_Balance_Widget.updateWidget(this)

                finish()
            }
            .addOnFailureListener {
                btnSave.isEnabled = true
                btnSave.text = getString(R.string.add_income)
                Toast.makeText(this, "Failed to add income: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateWalletBalance(walletId: String, amount: Double, isIncome: Boolean) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val walletRef = database.reference.child("users").child(userId).child("wallets").child(walletId)

        walletRef.child("balance").get().addOnSuccessListener { snapshot ->
            val currentBalance = snapshot.getValue(Double::class.java) ?: 0.0
            val newBalance = if (isIncome) currentBalance + amount else currentBalance - amount
            walletRef.child("balance").setValue(newBalance)
        }
    }

    private fun loadWallets() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        database.reference.child("users").child(userId).child("wallets")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    wallets.clear()

                    for (walletSnapshot in snapshot.children) {
                        val wallet = walletSnapshot.getValue(Wallet::class.java)
                        wallet?.let {
                            wallets.add(it)
                        }
                    }

                    // Select first wallet if none selected
                    if (selectedWalletId == null && wallets.isNotEmpty()) {
                        selectedWalletId = wallets[0].id
                        updateSelectedWalletUI()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AddIncomeActivity, "Failed to load wallets", Toast.LENGTH_SHORT).show()
                }
            })
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
        } else {
            tvSelectedWallet.text = "Select Wallet"
        }
    }
}
