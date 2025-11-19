package com.pachira.prog7313poepachira

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.pachira.prog7313poepachira.adapters.Category_Spending_Widget
import com.pachira.prog7313poepachira.adapters.TransactionAdapter
import com.pachira.prog7313poepachira.data.Transaction
import com.pachira.prog7313poepachira.data.Category
import com.pachira.prog7313poepachira.adapters.WalletAdapter
import com.pachira.prog7313poepachira.data.Wallet
import java.text.SimpleDateFormat
import java.util.*

class TransactionsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TransactionsActivity"
    }

    //Declare UI components
    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var startDateLayout: LinearLayout
    private lateinit var endDateLayout: LinearLayout
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var btnApplyFilter: Button
    private lateinit var rvTransactions: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout

    private lateinit var walletFilterSection: LinearLayout
    private lateinit var btnWalletFilter: LinearLayout
    private lateinit var tvSelectedWallet: TextView

    //Declare Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    //Declare Transaction Adapter and data
    private val transactions = mutableListOf<Transaction>()
    private lateinit var transactionAdapter: TransactionAdapter

    // Use the same date range approach as TrendsActivity
    private var startDate = Calendar.getInstance().apply {
        add(Calendar.MONTH, -1) // Default to 1 month ago
        set(Calendar.DAY_OF_MONTH, 1) // Start of month
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

    private val wallets = mutableListOf<Wallet>()
    private var selectedWallet: Wallet? = null

    //Initialise transaction type
    private var transactionType = "" // "income" or "expense" or "" for all

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Check if user is authenticated
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "User not authenticated!")
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        Log.d(TAG, "User authenticated: ${currentUser.uid}")

        // Initialize UI elements
        initializeViews()

        // Get transaction type from intent
        transactionType = intent.getStringExtra("transactionType") ?: ""
        Log.d(TAG, "Transaction type from intent: '$transactionType'")

        // Set title based on transaction type
        when (transactionType) {
            "income" -> tvTitle.text = "INCOME TRANSACTIONS"
            "expense" -> tvTitle.text = "EXPENSE TRANSACTIONS"
            else -> tvTitle.text = "ALL TRANSACTIONS"
        }

        // Set up RecyclerView with FIXED adapter constructor
        setupRecyclerView()

        // Set up date displays
        updateDateDisplays()

        // Set up click listeners
        setupClickListeners()

        // Load wallets
        loadWallets()

        // Load transactions directly (no need to load categories first)
        loadTransactions()
    }

    private fun initializeViews() {
        try {
            btnBack = findViewById(R.id.btnBack)
            tvTitle = findViewById(R.id.tvTitle)
            startDateLayout = findViewById(R.id.startDateLayout)
            endDateLayout = findViewById(R.id.endDateLayout)
            tvStartDate = findViewById(R.id.tvStartDate)
            tvEndDate = findViewById(R.id.tvEndDate)
            btnApplyFilter = findViewById(R.id.btnApplyFilter)
            rvTransactions = findViewById(R.id.rvTransactions)
            emptyStateLayout = findViewById(R.id.emptyStateLayout)

            walletFilterSection = findViewById(R.id.walletFilterSection)
            btnWalletFilter = findViewById(R.id.btnWalletFilter)
            tvSelectedWallet = findViewById(R.id.tvSelectedWallet)

            Log.d(TAG, "All views initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            Toast.makeText(this, "Error initializing interface", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // FIXED: Use simplified TransactionAdapter constructor
    private fun setupRecyclerView() {
        try {
            transactionAdapter = TransactionAdapter(
                transactions = transactions,
                onItemClick = { transaction ->
                    Log.d(TAG, "Transaction clicked: ${transaction.id}")
                }
            )
            rvTransactions.layoutManager = LinearLayoutManager(this)
            rvTransactions.adapter = transactionAdapter
            Log.d(TAG, "RecyclerView setup successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up RecyclerView", e)
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        startDateLayout.setOnClickListener {
            showDatePicker(true)
        }

        endDateLayout.setOnClickListener {
            showDatePicker(false)
        }

        btnApplyFilter.setOnClickListener {
            Log.d(TAG, "Apply filter clicked - reloading transactions")
            loadTransactions()
        }

        btnWalletFilter.setOnClickListener {
            showWalletSelectionDialog()
        }
    }


    private fun updateDateDisplays() {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        tvStartDate.text = dateFormat.format(startDate.time)
        tvEndDate.text = dateFormat.format(endDate.time)
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = if (isStartDate) startDate else endDate

        val customCalendarDialog = CustomCalendarDialog(
            this,
            calendar
        ) { newDate ->
            if (isStartDate) {
                startDate.set(
                    newDate.get(Calendar.YEAR),
                    newDate.get(Calendar.MONTH),
                    newDate.get(Calendar.DAY_OF_MONTH),
                    0, 0, 0
                )
                startDate.set(Calendar.MILLISECOND, 0)
            } else {
                endDate.set(
                    newDate.get(Calendar.YEAR),
                    newDate.get(Calendar.MONTH),
                    newDate.get(Calendar.DAY_OF_MONTH),
                    23, 59, 59
                )
                endDate.set(Calendar.MILLISECOND, 999)
            }
            updateDateDisplays()
        }

        customCalendarDialog.show()

        // Apply custom dialog styling
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

                    // Set first wallet as default if none selected
                    if (selectedWallet == null && wallets.isNotEmpty()) {
                        selectedWallet = wallets.first()
                        tvSelectedWallet.text = selectedWallet?.name ?: "Select Wallet"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to load wallets: ${error.message}")
                }
            })
    }

    private fun showWalletSelectionDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_wallet_selection, null)
        bottomSheetDialog.setContentView(dialogView)

        val rvWallets = dialogView.findViewById<RecyclerView>(R.id.rvWallets)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)

        val walletAdapter = WalletAdapter(wallets) { wallet ->
            selectedWallet = wallet
            tvSelectedWallet.text = wallet.name
            loadTransactions() // Reload transactions for selected wallet
            bottomSheetDialog.dismiss()
        }

        rvWallets.layoutManager = LinearLayoutManager(this)
        rvWallets.adapter = walletAdapter

        // Set current selection
        selectedWallet?.let { wallet ->
            walletAdapter.setSelectedWallet(wallet.id)
        }

        btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }


    private fun loadTransactions() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Clear existing transactions
        transactions.clear()

        Log.d(TAG, "=== LOADING TRANSACTIONS ===")
        Log.d(TAG, "Transaction type: '$transactionType'")
        Log.d(TAG, "User ID: $userId")
        Log.d(TAG, "Date range: ${startDate.timeInMillis} to ${endDate.timeInMillis}")

        when (transactionType) {
            "income" -> {
                Log.d(TAG, "Loading from income node only")
                loadFromNode("income", userId)
            }
            "expense" -> {
                Log.d(TAG, "Loading from expenses node only")
                loadFromNode("expenses", userId)
            }
            else -> {
                Log.d(TAG, "Loading from both income and expenses nodes")
                loadAllTransactionsSequentially(userId)
            }
        }
    }

    private fun loadAllTransactionsSequentially(userId: String) {
        // Load income first, then expenses
        database.child("users").child(userId).child("income")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d(TAG, "Income transactions: ${snapshot.childrenCount}")
                    processTransactionSnapshot(snapshot, "income")

                    // Now load expenses
                    database.child("users").child(userId).child("expenses")
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(expenseSnapshot: DataSnapshot) {
                                Log.d(TAG, "Expense transactions: ${expenseSnapshot.childrenCount}")
                                processTransactionSnapshot(expenseSnapshot, "expense")

                                // Sort and update UI
                                transactions.sortByDescending { it.date }
                                Log.d(TAG, "FINAL TOTAL: ${transactions.size} transactions")
                                updateTransactionsUI()
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e(TAG, "Failed to load expenses: ${error.message}")
                                updateTransactionsUI()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to load income: ${error.message}")
                    updateTransactionsUI()
                }
            })
    }

    private fun loadFromNode(nodeName: String, userId: String) {
        Log.d(TAG, "Loading from path: users/$userId/$nodeName")

        database.child("users").child(userId).child(nodeName)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d(TAG, "$nodeName snapshot: exists=${snapshot.exists()}, count=${snapshot.childrenCount}")

                    val transactionType = if (nodeName == "income") "income" else "expense"
                    processTransactionSnapshot(snapshot, transactionType)

                    transactions.sortByDescending { it.date }
                    Log.d(TAG, "FINAL COUNT for $nodeName: ${transactions.size} transactions")
                    updateTransactionsUI()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to load $nodeName: ${error.message}")
                    updateTransactionsUI()
                }
            })
    }

    private fun processTransactionSnapshot(snapshot: DataSnapshot, type: String) {
        if (!snapshot.exists()) {
            Log.w(TAG, "No $type transactions found")
            return
        }

        var processed = 0
        var added = 0

        for (transactionSnapshot in snapshot.children) {
            processed++

            val transaction = transactionSnapshot.getValue(Transaction::class.java)
            if (transaction == null) {
                Log.w(TAG, "Failed to parse transaction: ${transactionSnapshot.key}")
                continue
            }

            // Filter by selected wallet
            if (selectedWallet != null && transaction.walletId != selectedWallet?.id) {
                Log.d(TAG, "Skipping transaction ${transaction.id} - wallet mismatch")
                continue
            }

            Log.d(TAG, "Processing $type transaction: ${transaction.id} - ${transaction.amount}")

            val correctedTransaction = transaction.copy(type = type)
            transactions.add(correctedTransaction)
            added++
            Log.d(TAG, "Added $type transaction: ${correctedTransaction.id}")
        }

        Log.d(TAG, "Processed $processed transactions, added $added for wallet: ${selectedWallet?.name}")
    }

    private fun updateTransactionsUI() {
        try {
            Log.d(TAG, "=== UPDATING UI ===")
            Log.d(TAG, "Transaction count: ${transactions.size}")

            if (transactions.isEmpty()) {
                rvTransactions.visibility = View.GONE
                emptyStateLayout.visibility = View.VISIBLE
                Log.d(TAG, "Showing empty state")
                Toast.makeText(this, "No transactions found for the selected criteria", Toast.LENGTH_SHORT).show()
            } else {
                rvTransactions.visibility = View.VISIBLE
                emptyStateLayout.visibility = View.GONE

                // Filter transactions by date range
                val filteredTransactions = transactions.filter { transaction ->
                    transaction.date >= startDate.timeInMillis && transaction.date <= endDate.timeInMillis
                }

                Log.d(TAG, "Filtered from ${transactions.size} to ${filteredTransactions.size} transactions by date")

                // FIXED: Use updateTransactions method instead of updateData
                transactionAdapter.updateTransactions(filteredTransactions)
                Log.d(TAG, "Updated adapter with ${filteredTransactions.size} transactions")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error updating transactions UI", e)
            Toast.makeText(this, "Error updating UI: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveTransaction() {
        // Your existing save transaction code...

        // After successful save, update widgets
        Category_Spending_Widget.updateWidgets(this)

        // Also update the balance widget if you have it
        // Avaliable_Balance_Widget.updateWidget(this)
    }
}
