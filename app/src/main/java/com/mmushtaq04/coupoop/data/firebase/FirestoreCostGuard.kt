package com.mmushtaq04.coupoop.data.firebase

import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap

object FirestoreCostGuard {
    private val lastWriteTimes = ConcurrentHashMap<String, Long>()

    fun canWrite(key: String, minIntervalMs: Long = 800L): Boolean {
        val now = SystemClock.elapsedRealtime()
        val last = lastWriteTimes[key]
        if (last != null && now - last < minIntervalMs) {
            return false
        }
        lastWriteTimes[key] = now
        return true
    }

    fun canWriteOncePerWindow(key: String, minIntervalMs: Long = 800L, block: () -> Unit) {
        if (canWrite(key, minIntervalMs)) {
            block()
        }
    }
}
