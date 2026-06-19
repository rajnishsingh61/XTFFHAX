package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phoneNumber: String,
    val email: String = "",
    val isFavorite: Boolean = false,
    val avatarColorHex: String = "#1A73E8" // Nice default Google Blue hex
)
