package com.mmushtaq04.coupoop.data.purchase

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.firebase.firestore.SetOptions
import com.mmushtaq04.coupoop.PrefsManager
import com.mmushtaq04.coupoop.data.firebase.AuthManager
import com.mmushtaq04.coupoop.data.firebase.FirestoreRepository

object PremiumManager {
    const val PRODUCT_ID = "premium_remove_ads"

    private var billingClient: BillingClient? = null
    private var clientReady = false
    private var appContext: Context? = null

    private fun ensureClient(context: Context) {
        appContext = context.applicationContext
        if (billingClient != null) return
        billingClient = BillingClient.newBuilder(context)
            .setListener { _, purchases ->
                val userId = AuthManager.currentUser()?.uid ?: return@setListener
                val hasPurchase = purchases?.any { it.purchaseState == Purchase.PurchaseState.PURCHASED } == true
                if (hasPurchase) {
                    grantPremium(userId)
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                clientReady = billingResult.responseCode == BillingClient.BillingResponseCode.OK
            }

            override fun onBillingServiceDisconnected() {
                clientReady = false
                billingClient = null
            }
        })
    }

    fun initialize(context: Context) {
        ensureClient(context.applicationContext)
    }

    fun isPremium(context: Context): Boolean {
        return PrefsManager.isPremium(context)
    }

    fun syncWithGooglePlay(context: Context, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val userId = AuthManager.currentUser()?.uid
        if (userId == null) {
            onResult(false, "Sign in required")
            return
        }

        ensureClient(context.applicationContext)
        val client = billingClient ?: run {
            onResult(false, "Billing client not ready")
            return
        }

        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                onResult(false, billingResult.debugMessage)
                return@queryPurchasesAsync
            }
            val hasPremium = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            PrefsManager.setPremium(context, hasPremium)
            FirestoreRepository.userDoc(userId)
                .set(mapOf("premium" to hasPremium), SetOptions.merge())
                .addOnSuccessListener { onResult(true, null) }
                .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
        }
    }

    fun launchPurchaseFlow(activity: Activity, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val userId = AuthManager.currentUser()?.uid
        if (userId == null) {
            onResult(false, "Sign in required")
            return
        }

        ensureClient(activity.applicationContext)
        val client = billingClient ?: run {
            onResult(false, "Billing client not ready")
            return
        }
        if (!clientReady) {
            onResult(false, "Google Play Billing is not ready yet")
            return
        }

        val queryParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        client.queryProductDetailsAsync(queryParams) { result, productDetails ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK || productDetails.isEmpty()) {
                onResult(false, result.debugMessage)
                return@queryProductDetailsAsync
            }

            val product = productDetails.firstOrNull() ?: run {
                onResult(false, "Premium product not found")
                return@queryProductDetailsAsync
            }

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(product)
                            .build()
                    )
                )
                .build()

            val response = client.launchBillingFlow(activity, flowParams)
            if (response.responseCode != BillingClient.BillingResponseCode.OK) {
                onResult(false, response.debugMessage)
            } else {
                onResult(true, null)
            }
        }
    }

    private fun grantPremium(userId: String) {
        val context = appContext ?: return
        PrefsManager.setPremium(context, true)
        FirestoreRepository.userDoc(userId)
            .set(mapOf("premium" to true), SetOptions.merge())
            .addOnFailureListener { }
    }
}
