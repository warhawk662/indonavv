package com.example.indonavv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
fun Block(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(200.dp)
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, color)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Text(subtitle, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
fun Arrow(text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, fontSize = 10.sp, color = Color.LightGray)
        Icon(Icons.Rounded.ArrowDownward, null, tint = Color.Gray)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
fun FunctionalFlowDiagram() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .padding(24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "indonavv Functional Workflow",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(24.dp))

            // Step 1
            Block(
                "1. Map Creation",
                "Admin Panel: Create nodes,\nedges, and POIs over CAD.",
                Icons.Rounded.EditRoad,
                Color(0xFFFFC107)
            )

            Arrow("Publish to Cloud")

            // Step 2
            Block(
                "2. Data Sync",
                "App fetches JSON data and\ncaches it in Room DB.",
                Icons.Rounded.CloudDownload,
                Color(0xFF00E676)
            )

            Arrow("User Opens Map")

            // Step 3
            Block(
                "3. Localization",
                "PDR Engine tracks motion\nvia phone sensors.",
                Icons.Rounded.MyLocation,
                Color(0xFF2196F3)
            )

            Arrow("Select Destination")

            // Step 4
            Block(
                "4. Navigation",
                "Dijkstra algorithm computes\noptimal path across floors.",
                Icons.Rounded.Navigation,
                Color(0xFF9C27B0)
            )

            Arrow("Follow Guidance")

            // Step 5
            Block(
                "5. Arrived",
                "Haptic & Voice feedback\nupon reaching POI.",
                Icons.Rounded.CheckCircle,
                Color(0xFFE91E63)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
fun ProjectArchitectureDiagram() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .padding(24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "indonavv System Architecture",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(32.dp))

            // Admin Layer
            Block(
                "Admin Web Panel",
                "JS, HTML5, Canvas API\nCAD Edge Detection",
                Icons.Rounded.AdminPanelSettings,
                Color(0xFFFFC107)
            )

            Arrow("POST Map Data (JSON)")

            // Backend Layer
            Block(
                "Ktor Backend Server",
                "Kotlin, Netty, REST API\nStorage: map_data.json",
                Icons.Rounded.Storage,
                Color(0xFF00E676)
            )

            Arrow("Sync & Fetch")

            // App Layer
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Block(
                        "Android User App",
                        "Kotlin, Jetpack Compose\n3D Navigation UI",
                        Icons.Rounded.PhoneAndroid,
                        Color(0xFF2196F3)
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row {
                        Block(
                            "PDR Engine",
                            "Sensor Fusion\nStep Detection",
                            Icons.Rounded.DirectionsWalk,
                            Color(0xFF9C27B0),
                            modifier = Modifier.width(140.dp)
                        )
                        Block(
                            "Map Matcher",
                            "Dijkstra Pathfinding\nGraph Snapping",
                            Icons.Rounded.AltRoute,
                            Color(0xFFE91E63),
                            modifier = Modifier.width(140.dp)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Block(
                "Local Storage",
                "Room Database (v2)\nOffline Map Cache",
                Icons.Rounded.SdStorage,
                Color(0xFF607D8B)
            )
        }
    }
}
