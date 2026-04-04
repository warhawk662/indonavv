package com.example.indonavv.util

import com.example.indonavv.data.model.Edge
import com.example.indonavv.data.model.Node
import java.util.PriorityQueue

class PathFinder {

    data class PathResult(
        val nodes: List<Node>,
        val totalDistance: Float
    )

    fun findPath(
        startNodeId: String,
        endNodeId: String,
        nodes: List<Node>,
        edges: List<Edge>
    ): PathResult? {
        val nodeMap = nodes.associateBy { it.id }
        if (!nodeMap.containsKey(startNodeId) || !nodeMap.containsKey(endNodeId)) return null

        val adj = mutableMapOf<String, MutableList<Pair<String, Float>>>()
        edges.forEach { edge ->
            adj.getOrPut(edge.fromNodeId) { mutableListOf() }.add(edge.toNodeId to edge.distance)
            adj.getOrPut(edge.toNodeId) { mutableListOf() }.add(edge.fromNodeId to edge.distance)
        }

        val distances = mutableMapOf<String, Float>().withDefault { Float.MAX_VALUE }
        val previous = mutableMapOf<String, String?>()
        val pq = PriorityQueue<Pair<String, Float>>(compareBy { it.second })

        distances[startNodeId] = 0f
        pq.add(startNodeId to 0f)

        while (pq.isNotEmpty()) {
            val (currentId, currentDist) = pq.poll()!!

            if (currentId == endNodeId) break
            if (currentDist > (distances[currentId] ?: Float.MAX_VALUE)) continue

            adj[currentId]?.forEach { (neighborId, weight) ->
                val newDist = currentDist + weight
                if (newDist < (distances[neighborId] ?: Float.MAX_VALUE)) {
                    distances[neighborId] = newDist
                    previous[neighborId] = currentId
                    pq.add(neighborId to newDist)
                }
            }
        }

        if (!previous.containsKey(endNodeId) && startNodeId != endNodeId) return null

        val path = mutableListOf<Node>()
        var curr: String? = endNodeId
        while (curr != null) {
            nodeMap[curr]?.let { path.add(0, it) }
            curr = previous[curr]
        }

        return PathResult(path, distances[endNodeId] ?: 0f)
    }
}
