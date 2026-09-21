package com.mmushtaq04.coupoop.data.review

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Wraps Play's In-App Review API (not a custom "rate us" dialog — Google's
 * own flow has a much higher completion rate). Triggers itself only at the
 * happiest moment in the app (a celebration/sync moment), and only once per
 * install: Play's API self-throttles server-side too, but we shouldn't ask
 * on every session regardless.
 */
object RatingManager {
    private const val PREFS = "rating_prefs"
    private const val KEY_HAS_PROMPTED = "has_prompted_review"
    private const val KEY_CELEBRATION_COUNT = "celebration_count"

    // Ask after a few good moments, not the very first one.
    private const val CELEBRATIONS_BEFORE_PROMPT = 2

    fun onCelebration(context: Context) {
        val activity = context as? Activity ?: return
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_HAS_PROMPTED, false)) return

        val count = prefs.getInt(KEY_CELEBRATION_COUNT, 0) + 1
        prefs.edit().putInt(KEY_CELEBRATION_COUNT, count).apply()
        if (count < CELEBRATIONS_BEFORE_PROMPT) return

        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (!request.isSuccessful) return@addOnCompleteListener
            val reviewInfo = request.result
            manager.launchReviewFlow(activity, reviewInfo)
            // Play doesn't confirm whether the user actually rated — mark as
            // prompted regardless so we never ask again this install.
            prefs.edit().putBoolean(KEY_HAS_PROMPTED, true).apply()
        }
    }
}
