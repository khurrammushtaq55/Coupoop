package com.mmushtaq04.coupoop.data

import com.google.firebase.Timestamp

data class Pairing(
    val id: String = "",
    val inviteCode: String = "",
    val memberIds: List<String> = emptyList(),
    val createdAt: Timestamp? = null
)
