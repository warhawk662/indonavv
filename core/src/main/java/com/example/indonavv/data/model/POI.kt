package com.example.indonavv.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "pois")
@JsonClass(generateAdapter = true)
data class POI(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val nodeId: String,
    val category: String
)
