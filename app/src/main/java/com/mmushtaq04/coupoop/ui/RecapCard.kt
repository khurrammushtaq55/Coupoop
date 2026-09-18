package com.mmushtaq04.coupoop.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.createBitmap

/**
 * Simple local recap card generator that returns a Bitmap with basic text.
 * For MVP this draws simple stats; replace with Compose-based rendering as needed.
 */
fun generateWeeklyRecapBitmap(context: Context, title: String = "Weekly Recap"): Bitmap {
    val width = 1080
    val height = 1080
    val bm = createBitmap(width, height)
    val canvas = Canvas(bm)
    canvas.drawColor(Color.WHITE)

    val paint = Paint().apply {
        color = Color.BLACK
        textSize = 64f
        isAntiAlias = true
    }

    canvas.drawText(title, 60f, 120f, paint)
    canvas.drawText("You went 14 times this week 🔥", 60f, 220f, paint)
    canvas.drawText("Sync streak: 12 days", 60f, 320f, paint)

    return bm
}
