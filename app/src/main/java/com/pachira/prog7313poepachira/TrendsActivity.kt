package com.pachira.prog7313poepachira

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.pachira.prog7313poepachira.adapters.WalletAdapter
import com.pachira.prog7313poepachira.data.Category
import com.pachira.prog7313poepachira.data.Transaction
import com.pachira.prog7313poepachira.data.Wallet
import com.pachira.prog7313poepachira.utility.PdfReportGenerator
import com.pachira.prog7313poepachira.views.LineChartView
import com.pachira.prog7313poepachira.views.PieChartView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TrendsActivity : BaseActivity() {

    companion object {
        private const val TAG = "TrendsActivity"
        private const val STORAGE_PERMISSION_REQUEST = 1001
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    // Make views nullable for safer initialization
    private var tvTotalIncome: TextView? = null
    private var tvTotalExpenses: TextView? = null
    // Removed tvNetBalance since it's no longer in the layout
    private var pieChartView: PieChartView? = null
    private var lineChartView: LineChartView? = null
    private var tvCashFlowAmount: TextView? = null
    private var incomeTab: TextView? = null
    private var expenseTab: TextView? = null
    private var legendContainer: LinearLayout? = null
    private var recyclerViewTransactions: RecyclerView? = null
    private var tvEmptyState: TextView? = null
    private var btnExportPdf: Button? = null
    private var periodChipGroup: ChipGroup? = null

    // Wallet selector components (matching DashboardActivity)
    private lateinit var btnWalletSelector: LinearLayout
    private lateinit var tvSelectedWallet: TextView

    private val wallets = mutableListOf<Wallet>()
    private var selectedWalletId: String? = null

    private val allTransactions = mutableListOf<Transaction>()
    private val incomeTransactions = mutableListOf<Transaction>()
    private val expenseTransactions = mutableListOf<Transaction>()
    private val categories = mutableListOf<Category>()
    private val categoriesMap = mutableMapOf<String, Category>()

    private var totalIncome = 0.0
    private var totalExpenses = 0.0
    private var currentTab = "expense" // Default to expense tab
    private var selectedPeriod = "This Month"

    // Data for pie chart - separate for income and expenses
    private val expenseCategoryAmounts = mutableMapOf<String, Double>()
    private val incomeCategoryAmounts = mutableMapOf<String, Double>()
    private var categoriesLoaded = false
    private var transactionsLoaded = false

    // Currency formatter
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    // Date range for filtering
    private var startDate = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1) // Start of current month
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private var endDate = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_trends)
            Log.d(TAG, "Layout set successfully")

            // Initialize Firebase Auth
            auth = FirebaseAuth.getInstance()

            // Check if user is logged in
            if (auth.currentUser == null) {
                Log.w(TAG, "User not logged in")
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return
            }

            // Initialize Firebase Database
            database = FirebaseDatabase.getInstance().reference

            // Initialize views with error checking
            if (!initializeViews()) {
                Log.e(TAG, "Failed to initialize views")
                Toast.makeText(this, "Error loading interface", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Setup UI components
            setupPeriodChips()
            setupTabs()
            setupWalletSelector() // Updated method name
            setupPdfExportButton()

            // Load data from Firebase
            loadCategoriesFromFirebase()
            loadWallets() // Load wallets first, then transactions

            // Setup bottom navigation
            try {
                setupBottomNavigation()
                setSelectedNavItem(R.id.nav_trends)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up navigation", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Failed to load trends", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.trends_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export_pdf -> {
                checkPermissionAndGeneratePdf()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkPermissionAndGeneratePdf() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - Use scoped storage or request MANAGE_EXTERNAL_STORAGE
            if (Environment.isExternalStorageManager()) {
                generatePdfReport()
            } else {
                // Request MANAGE_EXTERNAL_STORAGE permission
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivityForResult(intent, STORAGE_PERMISSION_REQUEST)
                } catch (e: Exception) {
                    // Fallback to legacy permission
                    requestLegacyStoragePermission()
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-10 - Use legacy storage permission
            requestLegacyStoragePermission()
        } else {
            // Below Android 6 - No runtime permissions needed
            generatePdfReport()
        }
    }

    private fun requestLegacyStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_REQUEST
            )
        } else {
            generatePdfReport()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generatePdfReport()
            } else {
                Toast.makeText(this, "Storage permission required to generate PDF", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun generatePdfReport() {
        try {
            Toast.makeText(this, "Generating PDF report...", Toast.LENGTH_SHORT).show()

            val selectedWallet = wallets.find { it.id == selectedWalletId }
            val filteredTransactions = getFilteredTransactions()

            val pdfGenerator = PdfReportGenerator()
            val pdfFile = pdfGenerator.generateFinancialReport(
                chartView = lineChartView!!,
                pieChartView = pieChartView,
                transactions = filteredTransactions,
                wallet = selectedWallet,
                startDate = startDate,
                endDate = endDate,
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                incomeCategoryAmounts = incomeCategoryAmounts,
                expenseCategoryAmounts = expenseCategoryAmounts,
                categoriesMap = categoriesMap
            )

            if (pdfFile != null) {
                Toast.makeText(this, "PDF saved to Downloads folder", Toast.LENGTH_LONG).show()

                // Open the PDF file
                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    pdfFile
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    // If no PDF viewer is available, show a message
                    Toast.makeText(this, "PDF saved successfully. Please install a PDF viewer to open it.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Failed to generate PDF report", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating PDF report", e)
            Toast.makeText(this, "Error generating PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getFilteredTransactions(): List<Transaction> {
        return allTransactions.filter { transaction ->
            val matchesPeriod = isTransactionInPeriod(transaction, selectedPeriod)
            val matchesWallet = selectedWalletId == null || transaction.walletId == selectedWalletId
            matchesPeriod && matchesWallet
        }.sortedByDescending { it.date }
    }

    private fun initializeViews(): Boolean {
        return try {
            tvTotalIncome = findViewById(R.id.tvTotalIncome)
            tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
            // Removed tvNetBalance initialization since it's no longer in the layout
            pieChartView = findViewById(R.id.pieChartView)
            lineChartView = findViewById(R.id.lineChartView)
            tvCashFlowAmount = findViewById(R.id.tvCashFlowAmount)
            incomeTab = findViewById(R.id.incomeTab)
            expenseTab = findViewById(R.id.expenseTab)
            legendContainer = findViewById(R.id.legendContainer)
            recyclerViewTransactions = findViewById(R.id.recyclerViewTransactions)
            tvEmptyState = findViewById(R.id.tvEmptyState)
            btnExportPdf = findViewById(R.id.btnExportPdf)
            periodChipGroup = findViewById(R.id.periodChipGroup)

            // Initialize wallet selector components (matching DashboardActivity)
            btnWalletSelector = findViewById(R.id.btnWalletSelector)
            tvSelectedWallet = findViewById(R.id.tvSelectedWallet)

            // Verify critical views exist (removed tvNetBalance from the list)
            val criticalViews = listOf(
                tvTotalIncome, tvTotalExpenses,
                pieChartView, lineChartView, tvCashFlowAmount,
                incomeTab, expenseTab, legendContainer,
                btnExportPdf, periodChipGroup, btnWalletSelector, tvSelectedWallet
            )

            val missingViews = criticalViews.filter { it == null }
            if (missingViews.isNotEmpty()) {
                Log.e(TAG, "Missing ${missingViews.size} critical views")
                return false
            }

            Log.d(TAG, "All views initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            false
        }
    }

    private fun setupPeriodChips() {
        val periods = arrayOf("This Month", "Last Month", "This Year", "Last Year", "All Time")
        periodChipGroup?.removeAllViews() // Clear any existing chips

        periods.forEachIndexed { index, period ->
            val chip = createPeriodChip(period, index == 0) // First chip selected by default
            periodChipGroup?.addView(chip)
        }
    }

    private fun createPeriodChip(period: String, isSelected: Boolean): Chip {
        val chip = Chip(this).apply {
            text = period
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
                periodChipGroup?.let { chipGroup ->
                    for (i in 0 until chipGroup.childCount) {
                        val otherChip = chipGroup.getChildAt(i) as? Chip
                        if (otherChip != buttonView) {
                            otherChip?.isChecked = false
                            otherChip?.chipStrokeWidth = 0f // Remove border from other chips
                        }
                    }
                }

                // Add border to selected chip
                (buttonView as Chip).chipStrokeWidth = 2f

                selectedPeriod = period
                updateDateRange()
                loadTransactionsForCurrentTab()
            } else if (periodChipGroup?.checkedChipId == View.NO_ID) {
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

    private fun setupTabs() {
        try {
            expenseTab?.setOnClickListener {
                selectTab("expense")
            }

            incomeTab?.setOnClickListener {
                selectTab("income")
            }

            // Set initial tab
            selectTab("expense")
            Log.d(TAG, "Tabs setup successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up tabs", e)
        }
    }

    private fun selectTab(tabType: String) {
        try {
            currentTab = tabType

            if (tabType == "expense") {
                expenseTab?.setBackgroundResource(R.drawable.tab_selected_background)
                expenseTab?.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                incomeTab?.setBackgroundResource(R.drawable.tab_unselected_background)
                incomeTab?.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            } else {
                incomeTab?.setBackgroundResource(R.drawable.tab_selected_background)
                incomeTab?.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                expenseTab?.setBackgroundResource(R.drawable.tab_unselected_background)
                expenseTab?.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            }

            updatePieChart()
            loadTransactionsForCurrentTab()
        } catch (e: Exception) {
            Log.e(TAG, "Error selecting tab", e)
        }
    }

    // Updated wallet selector setup (matching DashboardActivity)
    private fun setupWalletSelector() {
        btnWalletSelector.setOnClickListener {
            showWalletSelectionBottomSheet()
        }
    }

    private fun setupPdfExportButton() {
        btnExportPdf?.setOnClickListener {
            checkPermissionAndGeneratePdf()
        }
    }

    // Load wallets (matching DashboardActivity implementation)
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

                    Log.d(TAG, "Loaded ${wallets.size} wallets")

                    // If no wallets exist, create a default one
                    if (wallets.isEmpty()) {
                        createDefaultWallet()
                    } else {
                        // Prioritize "Main Account" wallet, otherwise use the first wallet
                        val mainWallet = wallets.find { it.name == "Main Account" } ?: wallets[0]
                        selectedWalletId = mainWallet.id
                        updateSelectedWalletUI()

                        // Load data for the selected wallet
                        loadAllTransactions()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to load wallets", error.toException())
                    Toast.makeText(this@TrendsActivity, "Failed to load wallets", Toast.LENGTH_SHORT).show()
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
            loadTransactionsForCurrentTab() // Reload data with new wallet filter
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
        } else if (wallets.isNotEmpty()) {
            // Fallback to first wallet if selected wallet not found
            selectedWalletId = wallets[0].id
            tvSelectedWallet.text = wallets[0].name
        }
    }

    private fun loadCategoriesFromFirebase() {
        database.child("users").child(auth.currentUser!!.uid).child("categories").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categories.clear()
                categoriesMap.clear()
                snapshot.children.forEach { categorySnapshot ->
                    val category = categorySnapshot.getValue(Category::class.java)
                    category?.let {
                        categories.add(it)
                        categoriesMap[it.id] = it
                    }
                }
                categoriesLoaded = true
                tryUpdateTransactions()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load categories", error.toException())
                Toast.makeText(this@TrendsActivity, "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadAllTransactions() {
        val userId = auth.currentUser!!.uid
        val incomeRef = database.child("users").child(userId).child("income")
        val expensesRef = database.child("users").child(userId).child("expenses")

        val incomeListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                incomeTransactions.clear()
                snapshot.children.forEach { transactionSnapshot ->
                    val transaction = transactionSnapshot.getValue(Transaction::class.java)
                    transaction?.let {
                        incomeTransactions.add(it)
                        allTransactions.add(it)
                    }
                }
                transactionsLoaded = categoriesLoaded // Only complete if categories are loaded
                tryUpdateTransactions()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadIncomeTransactions:onCancelled", databaseError.toException())
                Toast.makeText(this@TrendsActivity, "Failed to load income transactions.",
                    Toast.LENGTH_SHORT).show()
                transactionsLoaded = categoriesLoaded
                tryUpdateTransactions()
            }
        }

        val expensesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                expenseTransactions.clear()
                snapshot.children.forEach { transactionSnapshot ->
                    val transaction = transactionSnapshot.getValue(Transaction::class.java)
                    transaction?.let {
                        expenseTransactions.add(it)
                        allTransactions.add(it)
                    }
                }
                transactionsLoaded = categoriesLoaded // Only complete if categories are loaded
                tryUpdateTransactions()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadExpenseTransactions:onCancelled", databaseError.toException())
                Toast.makeText(this@TrendsActivity, "Failed to load expense transactions.",
                    Toast.LENGTH_SHORT).show()
                transactionsLoaded = categoriesLoaded
                tryUpdateTransactions()
            }
        }

        allTransactions.clear() // Clear the list before populating
        incomeRef.addValueEventListener(incomeListener)
        expensesRef.addValueEventListener(expensesListener)
    }

    private fun loadTransactionsForCurrentTab() {
        Log.d(TAG, "loadTransactionsForCurrentTab called - selectedPeriod: $selectedPeriod, selectedWalletId: $selectedWalletId")

        val filteredTransactions = getFilteredTransactions()
        Log.d(TAG, "Filtered transactions count: ${filteredTransactions.size}")

        Log.d(TAG, "Income transactions: ${incomeTransactions.size}, Expense transactions: ${expenseTransactions.size}")
        updateUI()
    }

    private fun updateUI() {
        calculateTotals()
        updatePieChart()
        updateLineChart()
        updateCashFlow()
    }

    private fun calculateTotals() {
        // Filter transactions by selected wallet and period
        val filteredTransactions = getFilteredTransactions()

        totalIncome = 0.0
        totalExpenses = 0.0
        expenseCategoryAmounts.clear()
        incomeCategoryAmounts.clear()

        Log.d(TAG, "Calculating totals for ${filteredTransactions.size} filtered transactions")

        for (transaction in filteredTransactions) {
            if (transaction.type == "income") {
                totalIncome += transaction.amount
                val categoryId = transaction.categoryId
                val currentAmount = incomeCategoryAmounts[categoryId] ?: 0.0
                incomeCategoryAmounts[categoryId] = currentAmount + transaction.amount
            } else {
                totalExpenses += transaction.amount
                val categoryId = transaction.categoryId
                val currentAmount = expenseCategoryAmounts[categoryId] ?: 0.0
                expenseCategoryAmounts[categoryId] = currentAmount + transaction.amount
            }
        }

        Log.d(TAG, "Calculated totals - Income: $totalIncome, Expenses: $totalExpenses")
        Log.d(TAG, "Income categories: ${incomeCategoryAmounts.size}, Expense categories: ${expenseCategoryAmounts.size}")

        updateSummaryCards()
    }

    private fun updateSummaryCards() {
        try {
            tvTotalIncome?.text = currencyFormatter.format(totalIncome)
            tvTotalExpenses?.text = currencyFormatter.format(totalExpenses)

            // Removed net balance calculation and display since it's no longer in the layout

        } catch (e: Exception) {
            Log.e(TAG, "Error updating summary cards", e)
        }
    }

    private fun updatePieChart() {
        Log.d(TAG, "updatePieChart called for tab: $currentTab")

        // Use filtered transactions instead of all transactions
        val filteredTransactions = getFilteredTransactions()
        val transactions = filteredTransactions.filter { it.type == currentTab }

        Log.d(TAG, "Transactions for pie chart: ${transactions.size}")

        val categoryAmounts = if (currentTab == "income") incomeCategoryAmounts else expenseCategoryAmounts
        // categoryAmounts is already populated in calculateTotals() with filtered data

        Log.d(TAG, "Category amounts: $categoryAmounts")

        val total = categoryAmounts.values.sum()
        Log.d(TAG, "Total amount: $total")

        val pieSlices = categoryAmounts.map { (categoryId, amount) ->
            val percentage = if (total > 0) (amount / total * 100).toFloat() else 0f
            val categoryName = categoriesMap[categoryId]?.name ?: "Budget Transfer"
            val color = getCategoryColor(categoryName)
            PieChartView.PieSlice(percentage, color, categoryName)
        }.sortedByDescending { it.value }

        Log.d(TAG, "Pie slices created: ${pieSlices.size}")
        pieSlices.forEach { slice ->
            Log.d(TAG, "Slice: ${slice.label} - ${slice.value}%")
        }

        pieChartView?.setData(pieSlices)
        updateLegendWithIcons(pieSlices)
    }

    private fun updateLineChart() {
        Log.d(TAG, "updateLineChart called for period: $selectedPeriod")

        val incomeDataPoints = mutableListOf<LineChartView.DataPoint>()
        val expenseDataPoints = mutableListOf<LineChartView.DataPoint>()

        val incomeByPeriod = mutableMapOf<String, Double>()
        val expenseByPeriod = mutableMapOf<String, Double>()

        val filteredTransactions = getFilteredTransactions()

        Log.d(TAG, "Filtered ${filteredTransactions.size} transactions for period $selectedPeriod")

        filteredTransactions.forEach { transaction ->
            val periodKey = getFormattedPeriodKey(Date(transaction.date), selectedPeriod)

            if (transaction.type == "income") {
                incomeByPeriod[periodKey] = incomeByPeriod.getOrDefault(periodKey, 0.0) + transaction.amount
            } else {
                expenseByPeriod[periodKey] = expenseByPeriod.getOrDefault(periodKey, 0.0) + transaction.amount
            }
        }

        Log.d(TAG, "Income by period: $incomeByPeriod")
        Log.d(TAG, "Expense by period: $expenseByPeriod")

        generateCompleteDataPoints(incomeByPeriod, expenseByPeriod, incomeDataPoints, expenseDataPoints)

        Log.d(TAG, "Final data points - Income: ${incomeDataPoints.size}, Expense: ${expenseDataPoints.size}")

        if (incomeDataPoints.isNotEmpty()) {
            Log.d(TAG, "First data point: ${incomeDataPoints.first().label}, Last data point: ${incomeDataPoints.last().label}")
        }

        lineChartView?.setData(incomeDataPoints, expenseDataPoints)
    }

    private fun generateCompleteDataPoints(
        incomeByPeriod: Map<String, Double>,
        expenseByPeriod: Map<String, Double>,
        incomeDataPoints: MutableList<LineChartView.DataPoint>,
        expenseDataPoints: MutableList<LineChartView.DataPoint>
    ) {
        when (selectedPeriod) {
            "This Month", "Last Month" -> generateCompleteDailyDataPoints(incomeByPeriod, expenseByPeriod, incomeDataPoints, expenseDataPoints)
            "This Year", "Last Year" -> generateCompleteMonthlyDataPoints(incomeByPeriod, expenseByPeriod, incomeDataPoints, expenseDataPoints)
            "All Time" -> generateCompleteYearlyDataPoints(incomeByPeriod, expenseByPeriod, incomeDataPoints, expenseDataPoints)
        }
    }

    private fun generateCompleteDailyDataPoints(
        incomeByPeriod: Map<String, Double>,
        expenseByPeriod: Map<String, Double>,
        incomeDataPoints: MutableList<LineChartView.DataPoint>,
        expenseDataPoints: MutableList<LineChartView.DataPoint>
    ) {
        val calendar = Calendar.getInstance()
        calendar.time = startDate.time

        Log.d(TAG, "Generating daily data points from ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startDate.time)} to ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endDate.time)}")

        while (calendar.time <= endDate.time) {
            val dayKey = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
            val income = incomeByPeriod.getOrDefault(dayKey, 0.0)
            val expense = expenseByPeriod.getOrDefault(dayKey, 0.0)

            incomeDataPoints.add(
                LineChartView.DataPoint(
                    date = calendar.timeInMillis,
                    value = income.toFloat(),
                    label = dayKey
                )
            )

            expenseDataPoints.add(
                LineChartView.DataPoint(
                    date = calendar.timeInMillis,
                    value = expense.toFloat(),
                    label = dayKey
                )
            )

            Log.d(TAG, "Added data point for day $dayKey: income=$income, expense=$expense")
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        Log.d(TAG, "Generated ${incomeDataPoints.size} daily data points")
    }

    private fun generateCompleteMonthlyDataPoints(
        incomeByPeriod: Map<String, Double>,
        expenseByPeriod: Map<String, Double>,
        incomeDataPoints: MutableList<LineChartView.DataPoint>,
        expenseDataPoints: MutableList<LineChartView.DataPoint>
    ) {
        val calendar = Calendar.getInstance()
        calendar.time = startDate.time
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        Log.d(TAG, "Generating monthly data points from ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startDate.time)} to ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endDate.time)}")

        while (calendar.time <= endDate.time) {
            val monthKey = SimpleDateFormat("MMM", Locale.getDefault()).format(calendar.time)
            val income = incomeByPeriod.getOrDefault(monthKey, 0.0)
            val expense = expenseByPeriod.getOrDefault(monthKey, 0.0)

            incomeDataPoints.add(
                LineChartView.DataPoint(
                    date = calendar.timeInMillis,
                    value = income.toFloat(),
                    label = monthKey
                )
            )

            expenseDataPoints.add(
                LineChartView.DataPoint(
                    date = calendar.timeInMillis,
                    value = expense.toFloat(),
                    label = monthKey
                )
            )

            Log.d(TAG, "Added data point for month $monthKey: income=$income, expense=$expense")
            calendar.add(Calendar.MONTH, 1)
        }

        Log.d(TAG, "Generated ${incomeDataPoints.size} monthly data points")
    }

    private fun generateCompleteYearlyDataPoints(
        incomeByPeriod: Map<String, Double>,
        expenseByPeriod: Map<String, Double>,
        incomeDataPoints: MutableList<LineChartView.DataPoint>,
        expenseDataPoints: MutableList<LineChartView.DataPoint>
    ) {
        val calendar = Calendar.getInstance()
        calendar.time = startDate.time
        calendar.set(Calendar.DAY_OF_YEAR, 1)

        while (calendar.time <= endDate.time) {
            val yearKey = SimpleDateFormat("yyyy", Locale.getDefault()).format(calendar.time)
            val income = incomeByPeriod.getOrDefault(yearKey, 0.0)
            val expense = expenseByPeriod.getOrDefault(yearKey, 0.0)

            incomeDataPoints.add(
                LineChartView.DataPoint(
                    date = calendar.timeInMillis,
                    value = income.toFloat(),
                    label = yearKey
                )
            )

            expenseDataPoints.add(
                LineChartView.DataPoint(
                    date = calendar.timeInMillis,
                    value = expense.toFloat(),
                    label = yearKey
                )
            )

            calendar.add(Calendar.YEAR, 1)
        }
    }

    private fun getFormattedPeriodKey(date: Date, period: String): String {
        return when (period) {
            "This Month", "Last Month" -> {
                String.format("%02d", Calendar.getInstance().apply { time = date }.get(Calendar.DAY_OF_MONTH))
            }
            "This Year", "Last Year" -> {
                SimpleDateFormat("MMM", Locale.getDefault()).format(date)
            }
            "All Time" -> {
                SimpleDateFormat("yyyy", Locale.getDefault()).format(date)
            }
            else -> String.format("%02d", Calendar.getInstance().apply { time = date }.get(Calendar.DAY_OF_MONTH))
        }
    }

    private fun updateCashFlow() {
        val filteredTransactions = getFilteredTransactions()
        val totalIncomeForPeriod = filteredTransactions.filter { it.type == "income" }.sumOf { it.amount }
        val totalExpensesForPeriod = filteredTransactions.filter { it.type == "expense" }.sumOf { it.amount }
        val cashFlow = totalIncomeForPeriod - totalExpensesForPeriod
        tvCashFlowAmount?.text = currencyFormatter.format(cashFlow)
    }

    private fun updateDateRange() {
        when (selectedPeriod) {
            "This Month" -> {
                startDate = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                endDate = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
            }
            "Last Month" -> {
                startDate = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                endDate = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1)
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
            }
            "This Year" -> {
                startDate = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                endDate = Calendar.getInstance().apply {
                    set(Calendar.MONTH, Calendar.DECEMBER)
                    set(Calendar.DAY_OF_MONTH, 31)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
            }
            "Last Year" -> {
                startDate = Calendar.getInstance().apply {
                    add(Calendar.YEAR, -1)
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                endDate = Calendar.getInstance().apply {
                    add(Calendar.YEAR, -1)
                    set(Calendar.MONTH, Calendar.DECEMBER)
                    set(Calendar.DAY_OF_MONTH, 31)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
            }
            else -> {
                startDate = Calendar.getInstance().apply {
                    set(2022, Calendar.JANUARY, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                endDate = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
            }
        }

        Log.d(TAG, "Date range updated: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startDate.time)} to ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endDate.time)}")
    }

    private fun isTransactionInPeriod(transaction: Transaction, period: String): Boolean {
        val transactionDate = Date(transaction.date)

        val isInPeriod = (transactionDate.time >= startDate.timeInMillis) &&
                (transactionDate.time <= endDate.timeInMillis)

        if (!isInPeriod) {
            Log.v(TAG, "Transaction ${transaction.description} (${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(transactionDate)}) not in period ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startDate.time)} to ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endDate.time)}")
        }

        return isInPeriod
    }

    private fun getCategoryColor(category: String): String {
        return when (category.lowercase()) {
            "bills", "utilities", "electricity" -> {
                String.format("#%06X", 0xFFFFFF and ContextCompat.getColor(this, R.color.category_bills))
            }
            "health", "medical", "pharmacy", "healthcare" -> {
                String.format("#%06X", 0xFFFFFF and ContextCompat.getColor(this, R.color.category_healthcare))
            }
            "transport", "fuel", "transportation" -> {
                String.format("#%06X", 0xFFFFFF and ContextCompat.getColor(this, R.color.category_transport))
            }
            "education", "books", "courses" -> {
                String.format("#%06X", 0xFFFFFF and ContextCompat.getColor(this, R.color.category_education))
            }
            "home", "house", "housing" -> {
                String.format("#%06X", 0xFFFFFF and ContextCompat.getColor(this, R.color.category_home))
            }
            "entertainment", "movies", "games" -> {
                String.format("#%06X", 0xFFFFFF and ContextCompat.getColor(this, R.color.category_entertainment))
            }
            "groceries", "grocery" -> {
                String.format("#%06X", 0xFFFFFF and ContextCompat.getColor(this, R.color.category_groceries))
            }
            "food", "dining", "restaurant" -> {
                String.format("#%06X", 0xFFFFFF and ContextCompat.getColor(this, R.color.category_food))
            }
            "salary", "income", "wages" -> {
                String.format("#%06X", 0xFFFFFF and ContextCompat.getColor(this, R.color.category_salary))
            }
            "gift", "donation", "charity" -> {
                String.format("#%06X", 0xFFFFFF and ContextCompat.getColor(this, R.color.category_gift))
            }
            "investment", "stocks", "crypto" -> {
                String.format("#%06X", 0xFFFFFF and ContextCompat.getColor(this, R.color.category_investment))
            }
            "shopping", "clothes", "retail" -> {
                String.format("#%06X", 0xFFFFFF and ContextCompat.getColor(this, R.color.category_shopping))
            }
            else -> "#6366F1" // Default color
        }
    }

    private fun createLegendItemWithIcon(category: String, color: String, percentage: Float): View {
        val legendItem = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 8)
        }

        // Category icon
        val iconView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(48, 48).apply {
                marginEnd = 12
            }
            val iconResId = getCategoryIcon(category)
            if (iconResId != 0) {
                setImageResource(iconResId)
                setColorFilter(Color.parseColor(color))
            } else {
                // Fallback to colored circle if no icon
                setImageResource(R.drawable.circle_background)
                setColorFilter(Color.parseColor(color))
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        // Category details
        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val categoryText = TextView(this).apply {
            text = category
            textSize = 14f
            setTextColor(Color.parseColor("#333333"))
            typeface = Typeface.DEFAULT_BOLD
        }

        val percentageText = TextView(this).apply {
            text = String.format("%.1f%%", percentage)
            textSize = 12f
            setTextColor(Color.parseColor("#666666"))
        }

        textContainer.addView(categoryText)
        textContainer.addView(percentageText)

        legendItem.addView(iconView)
        legendItem.addView(textContainer)

        return legendItem
    }

    private fun updateLegendWithIcons(slices: List<PieChartView.PieSlice>) {
        legendContainer?.removeAllViews()

        slices.forEach { slice ->
            val legendItem = createLegendItemWithIcon(
                slice.label,
                slice.color,
                slice.value
            )
            legendContainer?.addView(legendItem)
        }
    }

    private fun getCategoryIcon(category: String): Int {
        return when (category.lowercase()) {
            "food", "groceries", "dining" -> R.drawable.ic_category_food
            "transport", "fuel", "car" -> R.drawable.ic_category_transport
            "entertainment", "movies", "games" -> R.drawable.ic_category_entertainment
            "shopping", "clothes", "retail" -> R.drawable.ic_category_shopping
            "bills", "utilities", "electricity" -> R.drawable.ic_receipt
            "health", "medical", "pharmacy" -> R.drawable.ic_category_health
            "education", "books", "courses" -> R.drawable.ic_category_education
            "salary", "income", "wages" -> R.drawable.money
            "investment", "stocks", "crypto" -> R.drawable.ic_category_investment
            "gift", "donation", "charity" -> R.drawable.ic_category_gift
            "other", "miscellaneous" -> R.drawable.ic_more_horiz
            else -> R.drawable.ic_category_default // Default category icon
        }
    }

    private fun tryUpdateTransactions() {
        Log.d(TAG, "tryUpdateTransactions called - categoriesLoaded: $categoriesLoaded, transactionsLoaded: $transactionsLoaded")
        Log.d(TAG, "Categories count: ${categories.size}, Transactions count: ${allTransactions.size}")

        if (categoriesLoaded && transactionsLoaded) {
            Log.d(TAG, "Both categories and transactions loaded. Categories: ${categories.size}, Transactions: ${allTransactions.size}")

            // Log some sample data
            if (allTransactions.isNotEmpty()) {
                val sampleTransaction = allTransactions.first()
                Log.d(TAG, "Sample transaction: ${sampleTransaction.description}, amount: ${sampleTransaction.amount}, date: ${sampleTransaction.date}, type: ${sampleTransaction.type}")
            }

            loadTransactionsForCurrentTab()
        } else {
            Log.d(TAG, "Waiting for data: categoriesLoaded=$categoriesLoaded, transactionsLoaded=$transactionsLoaded")
        }
    }
}
