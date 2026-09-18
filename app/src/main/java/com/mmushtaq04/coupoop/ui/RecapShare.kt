package com.mmushtaq04.coupoop.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object RecapShare {

    private val dayFormat: SimpleDateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getDefault() }
    }

    /**
     * Pulls the pairing's recent logs from Firestore, computes the real weekly
     * count and current day-streak, then generates and shares the recap image.
     * (Previously this drew hardcoded numbers regardless of actual activity.)
     */
    fun shareWeeklyRecap(context: Context, pairingId: String) {
        val db = FirebaseFirestore.getInstance()
        val thirtyDaysAgo = Timestamp(Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)))

        db.collection("pairings").document(pairingId)
            .collection("logs")
            .whereGreaterThanOrEqualTo("timestamp", thirtyDaysAgo)
            .get()
            .addOnSuccessListener { snap ->
                val dates = snap.documents.mapNotNull { (it.get("timestamp") as? Timestamp)?.toDate() }
                val weekAgoMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
                val weeklyCount = dates.count { it.time >= weekAgoMillis }
                val streakDays = computeCurrentStreakDays(dates.map { dayFormat.format(it) }.toSet())

                generateAndShare(context, weeklyCount, streakDays)
            }
            .addOnFailureListener {
                // Don't fall back to fake numbers — show zero rather than a stale/invented stat.
                generateAndShare(context, weeklyCount = 0, streakDays = 0)
            }
    }

    /** Consecutive days (ending today, or yesterday if nothing's logged yet today) with at least one log. */
    private fun computeCurrentStreakDays(loggedDayKeys: Set<String>): Int {
        val cal = Calendar.getInstance()
        if (!loggedDayKeys.contains(dayFormat.format(cal.time))) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        var streak = 0
        while (loggedDayKeys.contains(dayFormat.format(cal.time))) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    private fun generateAndShare(context: Context, weeklyCount: Int, streakDays: Int) {
        val bmp: Bitmap = generateWeeklyRecapBitmap(context, weeklyCount, streakDays)

        // save to cache/images/recap.png
        val imagesDir = File(context.cacheDir, "images")
        if (!imagesDir.exists()) imagesDir.mkdirs()
        val outFile = File(imagesDir, "recap.png")
        FileOutputStream(outFile).use { fos ->
            bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, fos)
            fos.flush()
        }

        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", outFile)
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(share, "Share recap"))
    }
}
