package com.example.indonavv.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "nodes")
@JsonClass(generateAdapter = true)
data class Node(
    @PrimaryKey val id: String,
    val floorId: String,
    val x: Float,
    val y: Float,
    val type: NodeType
)

enum class NodeType {
    ROOM, JUNCTION, ELEVATOR, STAIRS, ENTRANCE, RESTROOM, CLINIC, PHARMACY
}
