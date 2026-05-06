package com.example.indonavv.util

import androidx.compose.ui.geometry.Offset
import com.example.indonavv.data.model.Edge
import com.example.indonavv.data.model.Node
import kotlin.math.*

class MapMatcher {
    private var lastMatchedPosition: Offset? = null
    private var lastMatchedEdgeId: String? = null
    
    // Constants for matching logic (1 meter = 15 pixels)
    private val snapThreshold = 120f // ~8 meters
    private val outlierThreshold = 200f // ~13 meters
    
    // Hysteresis weights to prevent jitter
    private val stayOnPathBonus = 1.8f
    private val stayOnEdgeBonus = 1.4f

    /**
     * Advanced Map Matching with Delta-Based Movement Preservation.
     * Snaps the raw PDR position to the navigation graph without losing velocity.
     */
    fun matchToGraph(
        rawPosition: Offset, 
        nodes: List<Node>, 
        edges: List<Edge>, 
        userHeading: Float? = null,
        activePath: List<Node> = emptyList()
    ): Offset {
        if (nodes.isEmpty()) return rawPosition

        // 1. Movement Delta Preservation
        // We calculate how much the PDR wants to move, and we apply that movement
        // primarily along the matched graph edges.
        val lastPos = lastMatchedPosition ?: return rawPosition.also { lastMatchedPosition = it }
        val pdrDelta = rawPosition - lastPos
        val movementMagnitude = pdrDelta.getDistance()
        
        if (movementMagnitude < 0.1f) return lastPos

        // 2. Probabilistic Edge Matching
        var bestMatch = rawPosition
        var highestScore = -Float.MAX_VALUE
        var bestEdgeId: String? = null
        
        val activePathNodeIds = activePath.map { it.id }.toSet()

        edges.forEach { edge ->
            val fromNode = nodes.find { it.id == edge.fromNodeId }
            val toNode = nodes.find { it.id == edge.toNodeId }
            
            if (fromNode != null && toNode != null) {
                val p1 = Offset(fromNode.x, fromNode.y)
                val p2 = Offset(toNode.x, toNode.y)
                
                val projection = projectPointOnSegment(rawPosition, p1, p2)
                val distanceToEdge = (rawPosition - projection).getDistance()

                if (distanceToEdge > snapThreshold) return@forEach
                
                // Scoring Mechanism
                val proximityScore = exp(- (distanceToEdge * distanceToEdge) / (2 * 50f * 50f))
                
                var headingScore = 0.5f
                if (userHeading != null) {
                    val angleRad = atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble())
                    val edgeHeading = (Math.toDegrees(angleRad).toFloat() + 90f + 360f) % 360f
                    
                    val diff = abs(userHeading - edgeHeading)
                    val minDiff = min(diff, 360f - diff)
                    val bidirectionalDiff = min(minDiff, abs(180f - minDiff))
                    headingScore = max(0.1f, cos(Math.toRadians(bidirectionalDiff.toDouble()).toFloat()))
                }

                var topologicalBonus = 1.0f
                val isEdgeOnActivePath = activePathNodeIds.contains(edge.fromNodeId) && 
                                       activePathNodeIds.contains(edge.toNodeId)
                if (isEdgeOnActivePath) topologicalBonus *= stayOnPathBonus
                if (edge.id == lastMatchedEdgeId) topologicalBonus *= stayOnEdgeBonus

                val totalScore = (proximityScore * 0.5f + headingScore * 0.5f) * topologicalBonus

                if (totalScore > highestScore) {
                    highestScore = totalScore
                    bestMatch = projection
                    bestEdgeId = edge.id
                }
            }
        }

        // 3. Fallback to raw if no good match, or damp if outlier
        if (highestScore < 0.15f) {
            if (movementMagnitude > outlierThreshold) {
                bestMatch = lastPos + pdrDelta * (outlierThreshold / movementMagnitude)
            } else {
                bestMatch = rawPosition
            }
            bestEdgeId = null
        }

        // 4. Temporal Continuity (Minimal Smoothing)
        // Only use enough smoothing to prevent visual "jitter" on the line, 
        // but not enough to cause lag. Factor 0.9 means 90% new, 10% old.
        val smoothedMatch = lastPos + (bestMatch - lastPos) * 0.92f

        lastMatchedPosition = smoothedMatch
        lastMatchedEdgeId = bestEdgeId
        
        return smoothedMatch
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
