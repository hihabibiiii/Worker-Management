package com.example.workermanager.util

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

object UpiUtils {

    private const val TAG = "WorkCraftUPI"

    // Common UPI package names in India for launcher fallback
    val KNOWN_UPI_PACKAGES = listOf(
        "com.phonepe.app",                         // PhonePe
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "net.one97.paytm",                        // Paytm
        "in.org.npci.upiapp",                     // BHIM
        "com.dreamplug.androidapp",               // CRED
        "in.amazon.mShop.android.shopping",       // Amazon Pay
        "com.whatsapp",                            // WhatsApp
        "com.csam.icici.bank.imobile",            // ICICI iMobile
        "com.sbi.lotusintouch"                    // YONO SBI
    )

    /**
     * Launches a compatible installed UPI app on the device.
     * Since no recipient UPI ID is requested or generated, this performs a safe
     * app-launch flow to open an installed UPI app's normal home screen.
     */
    fun launchUpiApp(context: Context): Boolean {
        Log.d(TAG, "Attempting to launch an installed UPI app...")
        val packageManager = context.packageManager

        for (pkg in KNOWN_UPI_PACKAGES) {
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    Log.d(TAG, "Successfully launched UPI app home screen: $pkg")
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch package $pkg: ${e.message}", e)
            }
        }

        Log.w(TAG, "No compatible UPI app found on device.")
        Toast.makeText(
            context,
            "Payment record saved, but no compatible UPI app was found on this device.",
            Toast.LENGTH_LONG
        ).show()
        return false
    }
}
