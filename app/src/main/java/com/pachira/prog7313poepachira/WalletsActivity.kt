package com.pachira.prog7313poepachira

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pachira.prog7313poepachira.adapters.WalletAdapter
import com.pachira.prog7313poepachira.data.Wallet
import java.util.UUID

class WalletsActivity : BaseActivity() {

    companion object {
        private const val TAG = "WalletsActivity"
        private const val MAX_WALLET_NAME_LENGTH = 30
        private const val MAX_BALANCE = 999999999.99
    }

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    // UI Components
    private lateinit var rvWallets: RecyclerView
    private lateinit var btnAddWallet: MaterialButton
    private lateinit var emptyStateView: View

    // Data
    private val wallets = mutableListOf<Wallet>()
    private lateinit var walletAdapter: WalletAdapter

    // Bottom Sheet Variables
    private var selectedWalletType = "cash"
    private var selectedColorHex = "#6200EE" // Default purple color

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallets)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Check if user is authenticated
        if (auth.currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize UI components
        initializeViews()
        setupRecyclerView()
        setupListeners()
        setupBottomNavigation()
        setSelectedNavItem(R.id.nav_wallets)

        // Load wallets from Firebase
        loadWallets()
    }

    private fun initializeViews() {
        rvWallets = findViewById(R.id.rvWallets)
        btnAddWallet = findViewById(R.id.btnAddWallet)
        emptyStateView = findViewById(R.id.emptyStateView)
    }

    private fun setupRecyclerView() {
        walletAdapter = WalletAdapter(wallets) { wallet ->
            // Handle wallet selection
            Toast.makeText(this, "Selected: ${wallet.name}", Toast.LENGTH_SHORT).show()
        }

        rvWallets.apply {
            layoutManager = LinearLayoutManager(this@WalletsActivity)
            adapter = walletAdapter
        }
    }

    private fun setupListeners() {
        btnAddWallet.setOnClickListener {
            showAddWalletBottomSheet()
        }
    }

    private fun loadWallets() {
        val userId = auth.currentUser?.uid ?: return

        database.child("users").child(userId).child("wallets")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    wallets.clear()

                    for (walletSnapshot in snapshot.children) {
                        try {
                            val wallet = walletSnapshot.getValue(Wallet::class.java)
                            wallet?.let {
                                // Validate wallet data before adding
                                if (isValidWallet(it)) {
                                    wallets.add(it)
                                } else {
                                    Log.w(TAG, "Invalid wallet data: ${it.id}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing wallet data", e)
                        }
                    }

                    // Sort wallets by creation date (newest first)
                    wallets.sortByDescending { it.createdAt }

                    // Update UI based on whether we have wallets
                    updateWalletsUI()

                    // Notify adapter
                    walletAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to load wallets", error.toException())
                    Toast.makeText(this@WalletsActivity, "Failed to load wallets", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun isValidWallet(wallet: Wallet): Boolean {
        return wallet.id.isNotEmpty() &&
                wallet.name.isNotEmpty() &&
                wallet.balance >= 0 &&
                wallet.colorHex.isNotEmpty() &&
                wallet.type.isNotEmpty()
    }

    private fun updateWalletsUI() {
        if (wallets.isEmpty()) {
            emptyStateView.visibility = View.VISIBLE
            rvWallets.visibility = View.GONE
        } else {
            emptyStateView.visibility = View.GONE
            rvWallets.visibility = View.VISIBLE
        }
    }

    private fun showAddWalletBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_add_wallet, null)
        bottomSheetDialog.setContentView(view)

        // Initialize bottom sheet views
        val btnCloseSheet: ImageButton = view.findViewById(R.id.btnCloseSheet)
        val btnCreateWallet: MaterialButton = view.findViewById(R.id.btnCreateWallet)
        val etWalletName: EditText = view.findViewById(R.id.etWalletName)
        val tilWalletName: TextInputLayout = view.findViewById(R.id.tilWalletName)

        // Wallet type cards
        val cardCashWallet: MaterialCardView = view.findViewById(R.id.cardCashWallet)
        val cardBankWallet: MaterialCardView = view.findViewById(R.id.cardBankWallet)
        val cardCreditWallet: MaterialCardView = view.findViewById(R.id.cardCreditWallet)
        val cardEWallet: MaterialCardView = view.findViewById(R.id.cardEWallet)

        // Color cards
        val colorPurple: MaterialCardView = view.findViewById(R.id.colorPurple)
        val colorBlue: MaterialCardView = view.findViewById(R.id.colorBlue)
        val colorGreen: MaterialCardView = view.findViewById(R.id.colorGreen)
        val colorOrange: MaterialCardView = view.findViewById(R.id.colorOrange)
        val colorRed: MaterialCardView = view.findViewById(R.id.colorRed)
        val colorTeal: MaterialCardView = view.findViewById(R.id.colorTeal)

        // Set initial selections
        selectedWalletType = "cash"
        selectedColorHex = "#6200EE" // Purple

        // Setup wallet type selection
        setupWalletTypeSelection(cardCashWallet, cardBankWallet, cardCreditWallet, cardEWallet)

        // Setup color selection
        setupColorSelection(colorPurple, colorBlue, colorGreen, colorOrange, colorRed, colorTeal)

        // Input validation
        setupInputValidation(etWalletName, tilWalletName)

        // Close button
        btnCloseSheet.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        // Create wallet button
        btnCreateWallet.setOnClickListener {
            val walletName = etWalletName.text.toString().trim()


            if (validateInputs(walletName, tilWalletName)) {
                val initialBalance = 0.0

                // Check for duplicate wallet names
                if (wallets.any { it.name.equals(walletName, ignoreCase = true) }) {
                    tilWalletName.error = "Wallet name already exists"
                    return@setOnClickListener
                }

                // Disable button to prevent double creation
                btnCreateWallet.isEnabled = false
                btnCreateWallet.text = "Creating..."

                createNewWallet(walletName, initialBalance) { success ->
                    if (success) {
                        bottomSheetDialog.dismiss()
                    } else {
                        btnCreateWallet.isEnabled = true
                        btnCreateWallet.text = "Create Wallet"
                    }
                }
            }
        }

        bottomSheetDialog.show()
    }

    private fun setupWalletTypeSelection(
        cardCash: MaterialCardView,
        cardBank: MaterialCardView,
        cardCredit: MaterialCardView,
        cardEWallet: MaterialCardView
    ) {
        val typeCards = listOf(
            Pair(cardCash, "cash"),
            Pair(cardBank, "bank"),
            Pair(cardCredit, "credit"),
            Pair(cardEWallet, "ewallet")
        )

        // Set initial selection state
        updateWalletTypeSelection(typeCards, selectedWalletType)

        typeCards.forEach { (card, type) ->
            card.setOnClickListener {
                // Update selected type
                selectedWalletType = type

                // Update visual selection for all cards
                updateWalletTypeSelection(typeCards, selectedWalletType)
            }
        }
    }

    private fun updateWalletTypeSelection(
        typeCards: List<Pair<MaterialCardView, String>>,
        selectedType: String
    ) {
        typeCards.forEach { (card, type) ->
            if (type == selectedType) {
                // Selected state
                card.strokeWidth = 4
                card.strokeColor = ContextCompat.getColor(this, R.color.purple)
                card.cardElevation = 8f
            } else {
                // Unselected state
                card.strokeWidth = 1
                card.strokeColor = ContextCompat.getColor(this, R.color.gray_medium)
                card.cardElevation = 2f
            }
        }
    }

    private fun setupColorSelection(
        colorPurple: MaterialCardView,
        colorBlue: MaterialCardView,
        colorGreen: MaterialCardView,
        colorOrange: MaterialCardView,
        colorRed: MaterialCardView,
        colorTeal: MaterialCardView
    ) {
        val colorCards = listOf(
            Pair(colorPurple, "#6200EE"),
            Pair(colorBlue, "#2196F3"),
            Pair(colorGreen, "#4CAF50"),
            Pair(colorOrange, "#FF9800"),
            Pair(colorRed, "#F44336"),
            Pair(colorTeal, "#009688")
        )

        // Set initial selection state
        updateColorSelection(colorCards, selectedColorHex)

        colorCards.forEach { (card, colorHex) ->
            card.setOnClickListener {
                // Update selected color
                selectedColorHex = colorHex

                // Update visual selection for all cards
                updateColorSelection(colorCards, selectedColorHex)
            }
        }
    }

    private fun updateColorSelection(
        colorCards: List<Pair<MaterialCardView, String>>,
        selectedColor: String
    ) {
        colorCards.forEach { (card, colorHex) ->
            if (colorHex == selectedColor) {
                // Selected state
                card.strokeWidth = 4
                card.strokeColor = ContextCompat.getColor(this, R.color.purple)
                card.cardElevation = 8f
            } else {
                // Unselected state
                card.strokeWidth = 1
                card.strokeColor = ContextCompat.getColor(this, R.color.gray_light)
                card.cardElevation = 2f
            }
        }
    }

    private fun setupInputValidation(
        etWalletName: EditText,
        tilWalletName: TextInputLayout
    ) {
        etWalletName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString()?.trim() ?: ""
                when {
                    text.isEmpty() -> tilWalletName.error = "Wallet name is required"
                    text.length > MAX_WALLET_NAME_LENGTH -> tilWalletName.error = "Name too long (max $MAX_WALLET_NAME_LENGTH characters)"
                    wallets.any { it.name.equals(text, ignoreCase = true) } -> tilWalletName.error = "Wallet name already exists"
                    else -> tilWalletName.error = null
                }
            }
        })
    }

    private fun validateInputs(
        walletName: String,
        tilWalletName: TextInputLayout
    ): Boolean {
        var isValid = true

        // Validate wallet name
        when {
            walletName.isEmpty() -> {
                tilWalletName.error = "Wallet name is required"
                isValid = false
            }
            walletName.length > MAX_WALLET_NAME_LENGTH -> {
                tilWalletName.error = "Name too long (max $MAX_WALLET_NAME_LENGTH characters)"
                isValid = false
            }
            wallets.any { it.name.equals(walletName, ignoreCase = true) } -> {
                tilWalletName.error = "Wallet name already exists"
                isValid = false
            }
            else -> tilWalletName.error = null
        }

        return isValid
    }

    private fun createNewWallet(name: String, initialBalance: Double, callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            callback(false)
            return
        }

        // Generate wallet ID
        val walletId = UUID.randomUUID().toString()

        // Determine icon name based on wallet type
        val iconName = when (selectedWalletType) {
            "cash" -> "money"
            "bank" -> "ic_bank"
            "credit" -> "ic_credit_card"
            "ewallet" -> "ic_smartphone"
            else -> "ic_wallet_default"
        }

        // Create wallet object
        val wallet = Wallet(
            id = walletId,
            name = name,
            balance = initialBalance,
            iconName = iconName,
            colorHex = selectedColorHex,
            type = selectedWalletType,
            isActive = true,
            createdAt = System.currentTimeMillis()
        )

        // Save to Firebase with timeout
        database.child("users").child(userId).child("wallets").child(walletId)
            .setValue(wallet)
            .addOnSuccessListener {
                Toast.makeText(this, "Wallet created successfully", Toast.LENGTH_SHORT).show()
                callback(true)
            }
            .addOnFailureListener { e ->
                val errorMessage = when {
                    e.message?.contains("permission", ignoreCase = true) == true -> "Permission denied. Please check your account."
                    e.message?.contains("network", ignoreCase = true) == true -> "Network error. Please check your connection."
                    else -> "Failed to create wallet: ${e.message}"
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error creating wallet", e)
                callback(false)
            }
    }
}
