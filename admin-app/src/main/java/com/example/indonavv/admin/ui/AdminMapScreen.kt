package com.example.indonavv.admin.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.indonavv.data.model.Edge
import com.example.indonavv.data.model.Node
import com.example.indonavv.data.model.NodeType
import com.example.indonavv.data.model.POI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMapScreen(viewModel: AdminMapViewModel) {
    val userPosition by viewModel.userPosition.collectAsStateWithLifecycle()
    val userHeading by viewModel.userHeading.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    
    val nodes by viewModel.nodes.collectAsStateWithLifecycle()
    val edges by viewModel.edges.collectAsStateWithLifecycle()
    val pois by viewModel.pois.collectAsStateWithLifecycle()
    val stepLength by viewModel.stepLength.collectAsStateWithLifecycle()
    
    val floors by viewModel.floors.collectAsStateWithLifecycle()
    val currentFloorId by viewModel.currentFloorId.collectAsStateWithLifecycle()

    var poiDialogOpen by remember { mutableStateOf(false) }
    var calibrationDialogOpen by remember { mutableStateOf(false) }
    var floorDialogOpen by remember { mutableStateOf(false) }
    
    var showMap by remember { mutableStateOf(true) }
    var showPOIList by remember { mutableStateOf(false) }
    var clearConfirmOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0A0A0F),
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Indonavv Admin", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text(
                            floors.find { it.id == currentFloorId }?.name ?: "Unknown Floor",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { floorDialogOpen = true }) {
                        Icon(Icons.Rounded.Layers, contentDescription = "Floors", tint = Color(0xFF00E676))
                    }
                    IconButton(onClick = { clearConfirmOpen = true }) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear All", tint = Color(0xFFFF5252))
                    }
                    IconButton(onClick = { showPOIList = !showPOIList }) {
                        Icon(if (showPOIList) Icons.Rounded.Close else Icons.Rounded.List, contentDescription = "POI List", tint = Color.White)
                    }
                    IconButton(onClick = { calibrationDialogOpen = true }) {
                        Icon(Icons.Rounded.Tune, contentDescription = "Calibration", tint = Color.White)
                    }
                    IconButton(onClick = { showMap = !showMap }) {
                        Icon(
                            if (showMap) Icons.Rounded.Analytics else Icons.Rounded.Map,
                            contentDescription = "Toggle View",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { poiDialogOpen = true },
                    containerColor = Color(0xFF9C27B0),
                    contentColor = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) { Icon(Icons.Rounded.AddLocation, contentDescription = "Add POI") }
                
                SmallFloatingActionButton(
                    onClick = { viewModel.addEdgeToCurrentPosition() },
                    containerColor = Color(0xFF2196F3),
                    contentColor = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) { Icon(Icons.Rounded.Link, contentDescription = "Add Edge") }
                
                FloatingActionButton(
                    onClick = { viewModel.addNodeAtCurrentPosition(NodeType.JUNCTION) },
                    containerColor = Color(0xFF00E676),
                    contentColor = Color.White
                ) { Icon(Icons.Rounded.Add, contentDescription = "Add Node") }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            
            if (showMap) {
                MapCanvas(
                    modifier = Modifier.fillMaxSize(),
                    nodes = nodes,
                    edges = edges,
                    pois = pois,
                    userPosition = userPosition,
                    userHeading = userHeading
                )
            } else if (showPOIList) {
                POIListScreen(pois = pois)
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(Modifier.size(200.dp).background(Color.White.copy(alpha = 0.05f), CircleShape))
                        Icon(
                            Icons.Rounded.Navigation, 
                            null, 
                            Modifier.size(64.dp).graphicsLayer { rotationZ = userHeading }, 
                            tint = Color(0xFF00E676)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("Mapping Status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Current PDR: (${userPosition.x.toInt()}, ${userPosition.y.toInt()})", color = Color.Gray, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    Text("Heading: ${userHeading.toInt()}°", color = Color.Gray)
                    Text("Step Length: ${"%.1f".format(stepLength)}", color = Color.Gray)
                    
                    Spacer(Modifier.height(32.dp))
                    
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Instructions:", color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                            Text("1. Use 'Layers' icon to select or add a floor.", color = Color.LightGray, fontSize = 12.sp)
                            Text("2. Walk to starting point, tap '+' to add first node.", color = Color.LightGray, fontSize = 12.sp)
                            Text("3. Walk to next junction, tap 'Link' to add node & path.", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(24.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.1f)
            ) { 
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(if(syncStatus.contains("Ready")) Color(0xFF00E676) else Color.Yellow, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(syncStatus, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                }
            }

            if (floorDialogOpen) {
                var newFloorName by remember { mutableStateOf("") }
                var newFloorLevel by remember { mutableStateOf("0") }
                
                AlertDialog(
                    onDismissRequest = { floorDialogOpen = false },
                    containerColor = Color(0xFF1E1E1E),
                    title = { Text("Floors", color = Color.White) },
                    text = {
                        Column {
                            Text("Select Floor:", color = Color.Gray, fontSize = 12.sp)
                            LazyColumn(Modifier.heightIn(max = 200.dp)) {
                                items(floors) { floor ->
                                    Row(
                                        Modifier.fillMaxWidth().clickable { viewModel.selectFloor(floor.id); floorDialogOpen = false }.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.Layers, null, tint = if(floor.id == currentFloorId) Color(0xFF00E676) else Color.Gray)
                                        Spacer(Modifier.width(12.dp))
                                        Text(floor.name, color = Color.White)
                                    }
                                }
                            }
                            Divider(Modifier.padding(vertical = 8.dp), color = Color.Gray)
                            Text("Add New Floor:", color = Color.Gray, fontSize = 12.sp)
                            TextField(value = newFloorName, onValueChange = { newFloorName = it }, label = { Text("Name (e.g. 2nd Floor)") })
                            TextField(value = newFloorLevel, onValueChange = { newFloorLevel = it }, label = { Text("Level (e.g. 1)") })
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (newFloorName.isNotBlank()) {
                                viewModel.addFloor(newFloorName, newFloorLevel.toIntOrNull() ?: 0)
                                newFloorName = ""
                            }
                        }) { Text("Add Floor") }
                    },
                    dismissButton = {
                        TextButton(onClick = { floorDialogOpen = false }) { Text("Close") }
                    }
                )
            }

            if (poiDialogOpen) {
                var name by remember { mutableStateOf("") }
                var category by remember { mutableStateOf("Room") }
                AlertDialog(
                    onDismissRequest = { poiDialogOpen = false },
                    containerColor = Color(0xFF1E1E1E),
                    title = { Text("Add Point of Interest", color = Color.White) },
                    text = {
                        Column {
                            TextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("POI Name") },
                                colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            Spacer(Modifier.height(8.dp))
                            TextField(
                                value = category,
                                onValueChange = { category = it },
                                label = { Text("Category (e.g. Room, Clinic)") },
                                colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.addPOIAtCurrentPosition(name, category)
                            poiDialogOpen = false
                        }) { Text("Save POI") }
                    },
                    dismissButton = {
                        TextButton(onClick = { poiDialogOpen = false }) { Text("Cancel", color = Color.Gray) }
                    }
                )
            }

            if (calibrationDialogOpen) {
                AlertDialog(
                    onDismissRequest = { calibrationDialogOpen = false },
                    containerColor = Color(0xFF1E1E1E),
                    title = { Text("Sensor Calibration", color = Color.White) },
                    text = {
                        Column {
                            Text("Step Length: ${"%.1f".format(stepLength)}", color = Color.White)
                            Slider(
                                value = stepLength,
                                onValueChange = { viewModel.setStepLength(it) },
                                valueRange = 5f..20f,
                                steps = 15
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("Heading Correction", color = Color.White, fontWeight = FontWeight.Bold)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Button(onClick = { viewModel.alignHeading(0f) }) { Text("N (0°)") }
                                Button(onClick = { viewModel.alignHeading(90f) }) { Text("E (90°)") }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Button(onClick = { viewModel.alignHeading(180f) }) { Text("S (180°)") }
                                Button(onClick = { viewModel.alignHeading(270f) }) { Text("W (270°)") }
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.snapToLastNode() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                            ) {
                                Icon(Icons.Rounded.MyLocation, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Snap to Last Node")
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { calibrationDialogOpen = false }) { Text("Done") }
                    }
                )
            }

            if (clearConfirmOpen) {
                AlertDialog(
                    onDismissRequest = { clearConfirmOpen = false },
                    containerColor = Color(0xFF2D2D2D),
                    title = { Text("Clear Map?", color = Color.White) },
                    text = { Text("This will permanently delete all nodes, edges, and POIs from the server. This cannot be undone.", color = Color.LightGray) },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.clearMap()
                                clearConfirmOpen = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                        ) { Text("Delete Everything") }
                    },
                    dismissButton = {
                        TextButton(onClick = { clearConfirmOpen = false }) { Text("Cancel", color = Color.White) }
                    }
                )
            }
        }
    }
}

@Composable
fun POIListScreen(pois: List<POI>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .padding(16.dp)
    ) {
        item {
            Text(
                "Points of Interest",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        items(pois) { poi ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Place,
                        contentDescription = null,
                        tint = Color(0xFF9C27B0),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(poi.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(poi.category, color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        }
        if (pois.isEmpty()) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No POIs added yet", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun MapCanvas(
    modifier: Modifier = Modifier,
    nodes: List<Node>,
    edges: List<Edge>,
    pois: List<POI>,
    userPosition: Offset,
    userHeading: Float
) {
    Canvas(modifier = modifier.background(Color(0xFF0A0A0F))) {
        // Center the view on the user's position
        val center = Offset(size.width / 2, size.height / 2)
        
        // Scale factor
        val scale = 2.0f
        
        fun Offset.toCanvas(): Offset {
            return (this - userPosition) * scale + center
        }

        // 1. Draw Edges (Paths)
        edges.forEach { edge ->
            val fromNode = nodes.find { it.id == edge.fromNodeId }
            val toNode = nodes.find { it.id == edge.toNodeId }
            if (fromNode != null && toNode != null) {
                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(fromNode.x, fromNode.y).toCanvas(),
                    end = Offset(toNode.x, toNode.y).toCanvas(),
                    strokeWidth = 4f * scale
                )
            }
        }
        
        // 2. Draw Nodes
        nodes.forEach { node ->
            drawCircle(
                color = Color(0xFF00E676),
                radius = 6f * scale,
                center = Offset(node.x, node.y).toCanvas()
            )
        }
        
        // 3. Draw POIs
        pois.forEach { poi ->
            val node = nodes.find { it.id == poi.nodeId }
            if (node != null) {
                drawCircle(
                    color = Color(0xFF9C27B0),
                    radius = 10f * scale,
                    center = Offset(node.x, node.y).toCanvas()
                )
            }
        }
        
        // 4. Draw User Pointer
        rotate(degrees = userHeading, pivot = center) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(center.x, center.y - 15f * scale)
                lineTo(center.x - 10f * scale, center.y + 10f * scale)
                lineTo(center.x + 10f * scale, center.y + 10f * scale)
                close()
            }
            drawPath(path, color = Color.White)
        }
        
        // Pulsing circle for user
        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = 20f * scale,
            center = center
        )
    }
}
