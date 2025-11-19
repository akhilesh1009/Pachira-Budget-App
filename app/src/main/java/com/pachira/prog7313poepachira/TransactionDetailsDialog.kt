package com.pachira.prog7313poepachira

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.pachira.prog7313poepachira.R
import com.pachira.prog7313poepachira.data.Transaction
import com.pachira.prog7313poepachira.utility.ImageUtils
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.ImageButton
import android.widget.ZoomControls

class TransactionDetailsDialog(private val context: Context) {

    private lateinit var dialog: Dialog
    private var fullscreenDialog: Dialog? = null
    private var transactionBitmap: Bitmap? = null

    fun show(transaction: Transaction) {
        dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_transaction_details)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Set up views
        val btnClose = dialog.findViewById<ImageView>(R.id.btnClose)
        val tvTransactionType = dialog.findViewById<TextView>(R.id.tvTransactionType)
        val tvTransactionAmount = dialog.findViewById<TextView>(R.id.tvTransactionAmount)
        val tvTransactionCategory = dialog.findViewById<TextView>(R.id.tvTransactionCategory)
        val tvTransactionDate = dialog.findViewById<TextView>(R.id.tvTransactionDate)
        val tvTransactionDescription = dialog.findViewById<TextView>(R.id.tvTransactionDescription)

        // Image related views
        val ivReceiptThumbnail = dialog.findViewById<ImageView>(R.id.ivReceiptThumbnail)
        val tvNoReceipt = dialog.findViewById<TextView>(R.id.tvNoReceipt)
        val btnViewFullImage = dialog.findViewById<Button>(R.id.btnViewFullImage)

        // Format amount
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        val amountText = format.format(transaction.amount)

        // Format date
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val dateText = dateFormat.format(Date(transaction.date))

        // Set text values
        tvTransactionType.text = transaction.type.capitalize(Locale.ROOT)
        tvTransactionAmount.text = amountText
        tvTransactionCategory.text = transaction.category
        tvTransactionDate.text = dateText
        tvTransactionDescription.text = transaction.description.ifEmpty { context.getString(R.string.NoDescription) }

        // Set text color based on transaction type
        val textColor = if (transaction.type == "expense") {
            context.getColor(R.color.expense_red)
        } else {
            context.getColor(R.color.income_green)
        }
        tvTransactionAmount.setTextColor(textColor)

        // Handle receipt image if available
        if (transaction.imageData.isNotEmpty()) {
            //Data Type for transactionBitMap is "Bitmap"
            transactionBitmap = ImageUtils.decodeBase64ToBitmap(transaction.imageData)
            if (transactionBitmap != null) {
                ivReceiptThumbnail.setImageBitmap(transactionBitmap)
                ivReceiptThumbnail.visibility = View.VISIBLE
                btnViewFullImage.visibility = View.VISIBLE
                tvNoReceipt.visibility = View.GONE
            } else {
                ivReceiptThumbnail.visibility = View.GONE
                btnViewFullImage.visibility = View.GONE
                tvNoReceipt.visibility = View.VISIBLE
            }
        } else {
            ivReceiptThumbnail.visibility = View.GONE
            btnViewFullImage.visibility = View.GONE
            tvNoReceipt.visibility = View.VISIBLE
        }

        // Set click listeners
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        btnViewFullImage.setOnClickListener {
            showFullscreenImage()
        }

        dialog.show()
    }

    private fun showFullscreenImage() {
        if (transactionBitmap == null) return

        fullscreenDialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        fullscreenDialog?.setContentView(R.layout.dialog_fullscreen_image)

        val imageView = fullscreenDialog?.findViewById<ImageView>(R.id.fullscreenImageView)
        val zoomControls = fullscreenDialog?.findViewById<ZoomControls>(R.id.zoomControls)
        val btnClose = fullscreenDialog?.findViewById<ImageButton>(R.id.btnCloseFullscreen)

        imageView?.setImageBitmap(transactionBitmap)

        // Set up zoom controls
        var scaleFactor = 1.0f

        zoomControls?.setOnZoomInClickListener {
            scaleFactor *= 1.25f
            imageView?.scaleX = scaleFactor
            imageView?.scaleY = scaleFactor
        }

        zoomControls?.setOnZoomOutClickListener {
            if (scaleFactor > 0.8f) {
                scaleFactor *= 0.8f
                imageView?.scaleX = scaleFactor
                imageView?.scaleY = scaleFactor
            }
        }

        btnClose?.setOnClickListener {
            fullscreenDialog?.dismiss()
        }

        fullscreenDialog?.show()
    }

}
