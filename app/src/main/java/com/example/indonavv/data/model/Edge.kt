package com.example.indonavv.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "edges")
@JsonClass(generateAdapter = true)
data class Edge(
    @PrimaryKey val id: String,
    val fromNodeId: String,
    val toNodeId: String,
    val distance: Float,
    val weight: Float = 1.0f
)
