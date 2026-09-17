package com.mmushtaq04.coupoop.data

import com.google.firebase.Timestamp

data class LogEntry(
    val id: String = "",
    val userId: String = "",
    val timestamp: Timestamp? = null,
    val bristolType: Int? = null,
    val mood: String? = null,
    val note: String? = null
)
