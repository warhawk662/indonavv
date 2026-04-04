package com.example.indonavv.ui.map

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.indonavv.data.model.NodeType
import com.example.indonavv.data.model.POI
import kotlin.math.atan2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: MapViewModel) {
    val startSearchQuery by viewModel.startSearchQuery.collectAsStateWithLifecycle()
    val destSearchQuery by viewModel.destSearchQuery.collectAsStateWithLifecycle()
    val startSearchResults by viewModel.startSearchResults.collectAsStateWithLifecycle()
    val destSearchResults by viewModel.destSearchResults.collectAsStateWithLifecycle()
    
    val startPOI by viewModel.startPOI.collectAsStateWithLifecycle()
    val destinationPOI by viewModel.destinationPOI.collectAsStateWithLifecycle()
    val isSelectingStart by viewModel.isSelectingStart.collectAsStateWithLifecycle()
    
    val userPosition by viewModel.userPosition.collectAsStateWithLifecycle()
    val userHeading by viewModel.userHeading.collectAsStateWithLifecycle()
    val currentPath by viewModel.currentPath.collectAsStateWithLifecycle()
    val navigationInstruction by viewModel.navigationInstruction.collectAsStateWithLifecycle()
    val isVoiceEnabled by viewModel.isVoiceEnabled.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val isCalibrating by viewModel.isCalibrating.collectAsStateWithLifecycle()
    val isAdminMode by viewModel.isAdminMode.collectAsStateWithLifecycle()
    
    val pois by viewModel.allPOIs.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current
    var showMenu by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var calibrateOpen by remember { mutableStateOf(false) }
    var poiDialogOpen by remember { mutableStateOf(false) }
    var showPOIList by remember { mutableStateOf(false) }
    
    var startFocused by remember { mutableStateOf(false) }
    var destFocused by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0A0A0F),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
                    .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "INDONAVV",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        
                        Row {
                            IconButton(
                                onClick = { showPOIList = true },
                                modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                            ) { Icon(Icons.AutoMirrored.Rounded.List, null, tint = Color.White) }
                            
                            Spacer(Modifier.width(12.dp))
                            
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                            ) { Icon(Icons.Rounded.MoreVert, null, tint = Color.White) }
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color(0xFF1A1A2E))
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isAdminMode) "Exit Admin Mode" else "Admin Mode", color = Color.White) },
                                leadingIcon = { Icon(Icons.Rounded.AdminPanelSettings, null, tint = Color.White) },
                                onClick = { showMenu = false; viewModel.toggleAdminMode() }
                            )
                            DropdownMenuItem(
                                text = { Text("Calibrate Sensors", color = Color.White) },
                                leadingIcon = { Icon(Icons.Rounded.CompassCalibration, null, tint = Color.White) },
                                onClick = { showMenu = false; calibrateOpen = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings", color = Color.White) },
                                leadingIcon = { Icon(Icons.Rounded.Settings, null, tint = Color.White) },
                                onClick = { showMenu = false; settingsOpen = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Sync Data", color = Color.White) },
                                leadingIcon = { Icon(Icons.Rounded.Sync, null, tint = Color.White) },
                                onClick = { showMenu = false; viewModel.triggerSync() }
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(20.dp))
                    
                    if (!isAdminMode && !showPOIList) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SearchField(
                                value = startSearchQuery,
                                onValueChange = { viewModel.onStartSearchQueryChange(it) },
                                onMicClick = { viewModel.triggerAssistant() },
                                onFocusChanged = { startFocused = it },
                                placeholder = "Starting Point",
                                icon = Icons.Rounded.MyLocation,
                                iconColor = Color(0xFF00E676),
                                isActive = startFocused
                            )
                            
                            SearchField(
                                value = destSearchQuery,
                                onValueChange = { viewModel.onDestSearchQueryChange(it) },
                                onMicClick = { viewModel.triggerAssistant() },
                                onFocusChanged = { destFocused = it },
                                placeholder = "Where to?",
                                icon = Icons.Rounded.Place,
                                iconColor = Color(0xFFFF5252),
                                isActive = destFocused
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (showPOIList) {
                POIListScreen(
                    pois = pois,
                    onSelect = { 
                        if (isSelectingStart) viewModel.setStartPOI(it)
                        else viewModel.setDestination(it)
                        showPOIList = false
                    },
                    onBack = { showPOIList = false }
                )
            } else if (isAdminMode) {
                MappingStatusView(userPosition, userHeading)
            } else {
                if (destinationPOI != null) {
                    val nextTarget = if (currentPath.size > 1) Offset(currentPath[1].x, currentPath[1].y) else null
                    val destinationNode = if (currentPath.isNotEmpty()) currentPath.last() else null
                    
                    if (destinationNode != null) {
                        val distanceToDestination = (userPosition - Offset(destinationNode.x, destinationNode.y)).getDistance() / 15f
                        
                        if (nextTarget != null) {
                            val angleToTarget = Math.toDegrees(atan2((nextTarget.y - userPosition.y).toDouble(), (nextTarget.x - userPosition.x).toDouble())).toFloat() + 90f
                            AirtagArrow(
                                heading = (angleToTarget - userHeading + 360f) % 360f,
                                instruction = navigationInstruction,
                                distanceMeters = distanceToDestination
                            )
                        } else if (distanceToDestination < 1.0f) {
                            ArrivalView(destinationPOI!!.name) { viewModel.clearDestination() }
                        } else {
                            // Close but not yet arrived at the final node
                            AirtagArrow(
                                heading = (Math.toDegrees(atan2((destinationNode.y - userPosition.y).toDouble(), (destinationNode.x - userPosition.x).toDouble())).toFloat() + 90f - userHeading + 360f) % 360f,
                                instruction = "Arriving soon",
                                distanceMeters = distanceToDestination
                            )
                        }
                    } else {
                        ArrivalView(destinationPOI!!.name) { viewModel.clearDestination() }
                    }
                } else {
                    AssistantCenterView(onMicClick = { viewModel.triggerAssistant() })
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

            if (destinationPOI != null && !isAdminMode && !showPOIList) {
                LargeCancelButton(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp)) {
                    viewModel.clearDestination()
                }
            }

            // MODALS
            if (settingsOpen) {
                AlertDialog(
                    onDismissRequest = { settingsOpen = false },
                    containerColor = Color(0xFF1E1E1E),
                    title = { Text("Settings", color = Color.White) },
                    text = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Voice Assistant", color = Color.White, modifier = Modifier.weight(1f))
                                Switch(checked = isVoiceEnabled, onCheckedChange = { viewModel.toggleVoice() })
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Version: ${viewModel.appVersion}", color = Color.Gray, fontSize = 12.sp)
                        }
                    },
                    confirmButton = { TextButton(onClick = { settingsOpen = false }) { Text("Close", color = Color(0xFF2196F3)) } }
                )
            }

            if (calibrateOpen) {
                AlertDialog(
                    onDismissRequest = { if (!isCalibrating) calibrateOpen = false },
                    containerColor = Color(0xFF1E1E1E),
                    title = { Text("Sensor Calibration", color = Color.White) },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (isCalibrating) "Calibrating... Please keep the phone flat and still." 
                                else "Place your phone on a flat surface to calibrate accelerometer and gyroscope.",
                                color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            if (isCalibrating) {
                                Spacer(Modifier.height(16.dp))
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF2196F3))
                            }
                        }
                    },
                    confirmButton = {
                        if (!isCalibrating) {
                            Button(onClick = { viewModel.calibrateSensors() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) {
                                Text("Start Calibration")
                            }
                        }
                    },
                    dismissButton = {
                        if (!isCalibrating) {
                            TextButton(onClick = { calibrateOpen = false }) { Text("Cancel", color = Color.Gray) }
                        }
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
        }
    }
}

@Composable
fun POIListScreen(pois: List<POI>, onSelect: (POI) -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
            }
            Text(
                "Hospital Points",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(pois) { poi ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(poi) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(44.dp).background(Color.White.copy(alpha = 0.05f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.LocationOn, null, tint = Color(0xFF9C27B0))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(poi.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(poi.category, color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
            if (pois.isEmpty()) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Searching for points...", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Red.copy(alpha = 0.1f))
            .padding(16.dp)
    ) {
        Text("Admin Mapping Mode", color = Color.Red, fontWeight = FontWeight.Bold)
        Text("Walk to add nodes and create paths", color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun AdminControls(onAddNode: () -> Unit, onAddEdge: () -> Unit, onAddPOI: () -> Unit) {
    Column(horizontalAlignment = Alignment.End) {
        SmallFloatingActionButton(
            onClick = onAddPOI,
            containerColor = Color(0xFF9C27B0),
            contentColor = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        ) { Icon(Icons.Rounded.AddLocation, "Add POI") }
        
        SmallFloatingActionButton(
            onClick = onAddEdge,
            containerColor = Color(0xFF2196F3),
            contentColor = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        ) { Icon(Icons.Rounded.Link, "Add Edge") }
        
        FloatingActionButton(
            onClick = onAddNode,
            containerColor = Color(0xFF00E676),
            contentColor = Color.White
        ) { Icon(Icons.Rounded.Add, "Add Node") }
    }
}

@Composable
fun MappingStatusView(userPosition: Offset, heading: Float) {
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
                Modifier.size(64.dp).graphicsLayer { rotationZ = heading }, 
                tint = Color(0xFF00E676)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text("Current PDR: (${userPosition.x.toInt()}, ${userPosition.y.toInt()})", color = Color.White, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        Text("Heading: ${heading.toInt()}°", color = Color.Gray)
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onMicClick: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    isActive: Boolean
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.Gray, fontSize = 14.sp) },
        leadingIcon = { Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp)) },
        trailingIcon = {
            IconButton(onClick = onMicClick) {
                Icon(Icons.Rounded.Mic, null, tint = if(isActive) Color(0xFF2196F3) else Color.Gray)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { onFocusChanged(it.isFocused) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = if (isActive) Color.White.copy(alpha = 0.05f) else Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun AssistantCenterView(onMicClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "alpha"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(160.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                    .background(Color(0xFF2196F3), CircleShape)
            )
            
            Box(
                Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF2196F3), Color(0xFF1565C0)))
                    )
                    .clickable { onMicClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Mic, null, Modifier.size(48.dp), tint = Color.White)
            }
        }
        
        Spacer(Modifier.height(32.dp))
        Text("Where do you want to go?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Tap to talk to Assistant", color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
fun ArrivalView(name: String, onFinish: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().blur(30.dp).background(Color(0xFF00E676).copy(alpha = 0.2f), CircleShape))
            Box(Modifier.size(100.dp).background(Color(0xFF00E676), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, Modifier.size(60.dp), tint = Color.White)
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("You've Arrived", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(name, color = Color.Gray, fontSize = 18.sp)
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onFinish, 
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.height(56.dp).padding(horizontal = 32.dp)
        ) { 
            Text("Finish", color = Color.Black, fontWeight = FontWeight.Bold) 
        }
    }
}

@Composable
fun LargeCancelButton(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f) )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun AirtagArrow(heading: Float, instruction: String, distanceMeters: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(1f, 1.05f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "scale")

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(
            Modifier
                .size(320.dp)
                .graphicsLayer { 
                    rotationZ = heading
                    scaleX = scale
                    scaleY = scale
                    rotationX = 20f
                }, 
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                
                val leftPath = Path().apply {
                    moveTo(w / 2, 0f)
                    lineTo(w / 2, h * 0.6f)
                    lineTo(0f, h * 0.8f)
                    close()
                }
                val rightPath = Path().apply {
                    moveTo(w / 2, 0f)
                    lineTo(w, h * 0.8f)
                    lineTo(w / 2, h * 0.6f)
                    close()
                }

                drawPath(leftPath, Color(0xFF1565C0))
                drawPath(rightPath, Color(0xFF42A5F5))
                
                drawPath(
                    Path().apply {
                        moveTo(w/2, 0f)
                        lineTo(w * 0.55f, h * 0.1f)
                        lineTo(w * 0.45f, h * 0.1f)
                        close()
                    },
                    Color.White.copy(alpha = 0.5f)
                )
            }
        }
        
        Spacer(Modifier.height(60.dp))
        
        Text(
            "${String.format("%.1f", distanceMeters)}m",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = (-2).sp
            )
        )
        
        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                instruction, 
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(0xFF90CAF9), 
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
