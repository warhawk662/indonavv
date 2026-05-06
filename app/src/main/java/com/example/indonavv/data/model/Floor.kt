package com.example.indonavv.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "floors")
@JsonClass(generateAdapter = true)
data class Floor(
    @PrimaryKey val id: String,
    val name: String,
    val level: Int,
    val bgImageUrl: String? = null
)
