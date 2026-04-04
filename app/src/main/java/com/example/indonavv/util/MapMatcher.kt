package com.example.indonavv.util

import androidx.compose.ui.geometry.Offset
import com.example.indonavv.data.model.Edge
import com.example.indonavv.data.model.Node
import kotlin.math.*

class MapMatcher {
    private var lastMatchedPosition: Offset? = null
    // Adjusted threshold for snapping to the graph (in pixels)
    // 1 meter = 15 pixels. 5 meters = 75 pixels. 
    private val snapThreshold = 100f 

    /**
     * Enhanced Map Matching with Active Path Bias.
     * Snaps the raw PDR position to the navigation graph.
     */
    fun matchToGraph(
        position: Offset, 
        nodes: List<Node>, 
        edges: List<Edge>, 
        userHeading: Float? = null,
        activePath: List<Node> = emptyList()
    ): Offset {
        if (nodes.isEmpty()) return position

        var bestMatch = position
        var highestScore = -Float.MAX_VALUE
        var foundAnySnap = false

        // 1. Prioritize snapping to the ACTIVE navigation path if available
        if (activePath.size >= 2) {
            for (i in 0 until activePath.size - 1) {
                val n1 = activePath[i]
                val n2 = activePath[i+1]
                val p1 = Offset(n1.x, n1.y)
                val p2 = Offset(n2.x, n2.y)
                
                val projection = projectPointOnSegment(position, p1, p2)
                val distanceToEdge = (position - projection).getDistance()

                // High bias for staying on the calculated path
                if (distanceToEdge < snapThreshold * 1.5f) {
                    val score = 10.0f / (1.0f + distanceToEdge)
                    if (score > highestScore) {
                        highestScore = score
                        bestMatch = projection
                        foundAnySnap = true
                    }
                }
            }
        }

        // 2. If not on active path or score is low, check all edges
        if (!foundAnySnap || highestScore < 2.0f) {
            edges.forEach { edge ->
                val fromNode = nodes.find { it.id == edge.fromNodeId }
                val toNode = nodes.find { it.id == edge.toNodeId }
                
                if (fromNode != null && toNode != null) {
                    val p1 = Offset(fromNode.x, fromNode.y)
                    val p2 = Offset(toNode.x, toNode.y)
                    
                    val projection = projectPointOnSegment(position, p1, p2)
                    val distanceToEdge = (position - projection).getDistance()

                    if (distanceToEdge > snapThreshold) return@forEach
                    
                    val distanceScore = 1.0f / (1.0f + distanceToEdge)
                    
                    // Heading alignment score
                    var headingScore = 0.5f
                    if (userHeading != null) {
                        val angle = atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble())
                        val edgeHeading = (Math.toDegrees(angle).toFloat() + 360f) % 360f
                        val diff = abs(userHeading - edgeHeading)
                        val minDiff = min(diff, 360f - diff)
                        val bidirectionalDiff = min(minDiff, abs(180f - minDiff))
                        headingScore = cos(Math.toRadians(bidirectionalDiff.toDouble())).toFloat().coerceAtLeast(0.0f)
                    }

                    val totalScore = (distanceScore * 0.7f) + (headingScore * 0.3f)

                    if (totalScore > highestScore) {
                        highestScore = totalScore
                        bestMatch = projection
                        foundAnySnap = true
                    }
                }
            }
        }

        // 3. Last fallback: snap to nearest node if user is near a junction
        if (!foundAnySnap) {
            val nearestNode = nodes.minByOrNull { (Offset(it.x, it.y) - position).getDistance() }
            if (nearestNode != null) {
                val nodePos = Offset(nearestNode.x, nearestNode.y)
                if ((position - nodePos).getDistance() < snapThreshold) {
                    bestMatch = nodePos
                }
            }
        }

        lastMatchedPosition = bestMatch
        return bestMatch
    }

    private fun projectPointOnSegment(p: Offset, a: Offset, b: Offset): Offset {
        val ab = b - a
        val lengthSquared = ab.x * ab.x + ab.y * ab.y
        if (lengthSquared == 0f) return a
        val ap = p - a
        var t = (ap.x * ab.x + ap.y * ab.y) / lengthSquared
        t = t.coerceIn(0f, 1f)
        return a + ab * t
    }
}
