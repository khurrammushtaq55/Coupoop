package com.mmushtaq04.coupoop.data.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.mmushtaq04.coupoop.data.purchase.PremiumManager

object AdMobManager {
    private const val BANNER_AD_UNIT_ID = "ca-app-pub-5966317256433132/6315959425"
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-5966317256433132/6714669812"
    private const val MIN_INTERSTITIAL_DELAY_MS = 45_000L

    private var interstitialAd: InterstitialAd? = null
    private var lastShownInterstitialAt = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    private var schedulePosted = false

    fun initialize(context: Context) {
        MobileAds.initialize(context) {}
        preloadInterstitial(context)
    }

    fun scheduleInterstitial(activity: Activity) {
        if (schedulePosted || PremiumManager.isPremium(activity)) return
        schedulePosted = true
        mainHandler.postDelayed({
            maybeShowInterstitial(activity)
        }, MIN_INTERSTITIAL_DELAY_MS)
    }

    fun preloadInterstitial(context: Context) {
        if (PremiumManager.isPremium(context) || interstitialAd != null) return
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun maybeShowInterstitial(activity: Activity) {
        if (PremiumManager.isPremium(activity)) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastShownInterstitialAt < MIN_INTERSTITIAL_DELAY_MS) return
        val ad = interstitialAd ?: run {
            preloadInterstitial(activity)
            return
        }
        interstitialAd = null
        lastShownInterstitialAt = now
        ad.show(activity)
        preloadInterstitial(activity)
    }

    @Composable
    fun BannerAd() {
        val context = LocalContext.current
        if (PremiumManager.isPremium(context)) return

        var isLoaded by remember { mutableStateOf(false) }

        if (!isLoaded) {
            AndroidView(
                factory = { ctx ->
                    AdView(ctx).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = BANNER_AD_UNIT_ID
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                isLoaded = true
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                isLoaded = false
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            return
        }

        AndroidView(
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = BANNER_AD_UNIT_ID
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            isLoaded = true
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isLoaded = false
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )
    }
}
