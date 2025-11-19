package com.pachira.prog7313poepachira.utility

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {

    /**
     * Decode a Base64 string into a Bitmap
     *
     * @param base64String The Base64 encoded string to be decoded.
     * @return A Bitmap object decoded from the Base64 string, or null if decoding fails.
     */
    //(Anil Kr Mourya 2024)
    fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            // Decode the Base64 string into a byte array
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            // Convert the byte array into a Bitmap
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            // If an error occurs during decoding, print the stack trace and return null
            e.printStackTrace()
            null
        }
    }

    /**
     * Encode a Bitmap to Base64 string
     *
     * @param bitmap The Bitmap object to be encoded.
     * @param quality The quality of the JPEG compression (default is 70).
     * @return A Base64 encoded string representation of the Bitmap.
     */
    //(Anil Kr Mourya 2024)
    fun encodeBitmapToBase64(bitmap: Bitmap, quality: Int = 70): String {
        // Create a ByteArrayOutputStream to hold the compressed byte array
        val byteArrayOutputStream = ByteArrayOutputStream()
        // Compress the Bitmap to a JPEG format with the specified quality
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        // Convert the compressed byte array to a Base64 string
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}

//REFERENCES
// Anil Kr Mourya. 2024. “How to Convert Base64 String to Bitmap and Bitmap to Base64 String”.
// <https://mrappbuilder.medium.com/how-to-convert-base64-string-to-bitmap-and-bitmap-to-base64-string-7a30947b0494>
// [accessed 30 April 2025].
