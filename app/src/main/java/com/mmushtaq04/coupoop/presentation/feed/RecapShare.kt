package com.mmushtaq04.coupoop.presentation.feed

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.google.firebase.Timestamp
import com.mmushtaq04.coupoop.data.firebase.FirestoreRepository
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.concurrent.TimeUnit

object RecapShare {

    /**
     * Pulls the pairing's recent logs from Firestore for the weekly count, and
     * the real "sync streak" (partners logging within the Cloud Function's sync
     * window — see functions/index.js onLogCreate) from streaks/sync, then
     * generates and shares the recap image.
     */
    fun shareWeeklyRecap(context: Context, pairingId: String) {
        val db = FirestoreRepository.db()
        val pairingRef = db.collection("pairings").document(pairingId)
        val sevenDaysAgo = Timestamp(Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)))

        pairingRef.collection("logs")
            .whereGreaterThanOrEqualTo("timestamp", sevenDaysAgo)
            .get()
            .addOnSuccessListener { logsSnap ->
                val weeklyCount = logsSnap.size()

                pairingRef.collection("streaks").document("sync").get()
                    .addOnSuccessListener { streakSnap ->
                        val streakDays = (streakSnap.getLong("currentStreak") ?: 0L).toInt()
                        generateAndShare(context, weeklyCount, streakDays)
                    }
                    .addOnFailureListener {
                        generateAndShare(context, weeklyCount, streakDays = 0)
                    }
            }
            .addOnFailureListener {
                // Don't fall back to fake numbers — show zero rather than a stale/invented stat.
                generateAndShare(context, weeklyCount = 0, streakDays = 0)
            }
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
