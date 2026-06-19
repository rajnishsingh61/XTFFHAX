package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val callerName: String, // Contact name if known, else empty
    val phoneNumber: String,
    val timestamp: Long,
    val durationSeconds: Int,
    val callType: String, // "INCOMING", "OUTGOING", "MISSED"
    val voiceEffectUsed: String = "Normal"
)
