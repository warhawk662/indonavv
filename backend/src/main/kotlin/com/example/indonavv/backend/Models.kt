package com.example.indonavv.backend

import kotlinx.serialization.Serializable

@Serializable
enum class NodeType {
    ROOM, JUNCTION, ELEVATOR, STAIRS, ENTRANCE, RESTROOM, CLINIC, PHARMACY
}

@Serializable
data class Node(
    val id: String,
    val x: Float,
    val y: Float,
    val floorId: String,
    val type: NodeType
)

@Serializable
data class Edge(
    val id: String,
    val fromNodeId: String,
    val toNodeId: String,
    val distance: Float,
    val weight: Float = 1.0f
)

@Serializable
data class POI(
    val id: String,
    val name: String,
    val description: String? = null,
    val nodeId: String,
    val category: String
)

@Serializable
data class Geofence(
    val id: String,
    val points: List<Point>
)

@Serializable
data class RoomBlock(
    val id: String,
    val name: String,
    val points: List<Point>,
    val color: String = "#E3F2FD", // Default light blue
    val floorId: String = "h1"
)

@Serializable
data class Floor(
    val id: String,
    val name: String,
    val level: Int,
    val bgImageUrl: String? = null
)

@Serializable
data class Point(val x: Float, val y: Float)

@Serializable
data class MapData(
    val nodes: List<Node> = emptyList(),
    val edges: List<Edge> = emptyList(),
    val pois: List<POI> = emptyList(),
    val bgImageUrl: String? = null, // Legacy, kept for compatibility
    val geofences: List<Geofence> = emptyList(),
    val roomBlocks: List<RoomBlock> = emptyList(),
    val floors: List<Floor> = emptyList()
)
