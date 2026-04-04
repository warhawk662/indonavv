package com.example.indonavv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StepBox(
    number: Int,
    title: String,
    desc: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number.toString(), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(16.dp))
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(desc, color = Color.Gray, fontSize = 11.sp, lineHeight = 14.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 950)
@Composable
fun ProjectWorkflowDiagram() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .padding(24.dp)
    ) {
        Text(
            "indonavv Technical Workflow",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Black
        )
        Text("Detailed algorithmic flow of the system", color = Color.Gray, fontSize = 12.sp)
        Spacer(Modifier.height(24.dp))

        Text("PHASE 1: ADMIN DATA MODELING", color = Color(0xFFFFC107), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        StepBox(1, "Map Digitization", "Algorithm: Laplacian Edge Detection. Process: Converts PNG to grayscale, applies binary thresholding, and detects structural wall boundaries.", Icons.Rounded.CloudUpload, Color(0xFFFFC107))
        StepBox(2, "Graph Mapping", "Data Structure: Adjacency List. Process: Manual placement of Nodes (Rooms/Junctions) and weighted Edges (Walkable Paths) to form a Navigation Graph.", Icons.Rounded.AddLocationAlt, Color(0xFFFFC107))
        StepBox(3, "Persistence Sync", "Technology: RESTful API via Ktor. Process: Serializes graph data into structured JSON and persists it in the cloud for user app consumption.", Icons.Rounded.Sync, Color(0xFFFFC107))

        Spacer(Modifier.height(16.dp))
        Text("PHASE 2: NAVIGATION ENGINE", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        StepBox(4, "Interactive Assistant", "Tech: Speech-to-Text (STT). Process: Matches voice query against POI database using fuzzy string matching to determine target destination.", Icons.Rounded.Mic, Color(0xFF2196F3))
        StepBox(5, "Route Optimization", "Algorithm: Dijkstra's Shortest Path. Process: Computes the optimal node sequence from current PDR position to target Room node.", Icons.AutoMirrored.Rounded.AltRoute, Color(0xFF2196F3))
        StepBox(6, "Position Tracking", "Algorithm: Pedestrian Dead Reckoning (PDR). Process: Fuses Rotation Vector and Accelerometer at 100Hz to calculate real-time stride and displacement.", Icons.AutoMirrored.Rounded.DirectionsWalk, Color(0xFF2196F3))
        StepBox(7, "Graph Snap Matching", "Algorithm: Multi-factor Scoring Engine. Process: Uses distance, temporal consistency, and heading cosine similarity to lock user position to paths.", Icons.Rounded.Adjust, Color(0xFF2196F3))

        Spacer(Modifier.height(16.dp))
        Text("PHASE 3: SMART ARRIVAL", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        StepBox(8, "Feedback Loop", "Tech: Haptic/TTS Engine. Process: Proximity detection (<1.5m) triggers Text-to-Speech and 1000ms vibration impact for arrival notification.", Icons.Rounded.CheckCircle, Color(0xFF00E676))
    }
}
