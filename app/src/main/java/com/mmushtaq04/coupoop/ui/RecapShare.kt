package com.mmushtaq04.coupoop.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object RecapShare {
    fun shareWeeklyRecap(context: Context) {
        // generate bitmap
        val bmp = generateWeeklyRecapBitmap(context)

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
