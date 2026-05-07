package com.example.indonavv.ui.map

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.indonavv.data.local.MapDao
import com.example.indonavv.data.model.Edge
import com.example.indonavv.data.model.Floor
import com.example.indonavv.data.model.Node
import com.example.indonavv.data.model.NodeType
import com.example.indonavv.data.model.POI
import com.example.indonavv.data.model.RoomBlock
import com.example.indonavv.data.remote. MapApiService
import com.example.indonavv.sensor.PdrManager
import com.example.indonavv.util.MapMatcher
import com.example.indonavv.util.PathFinder
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.*

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MapViewModel(
    private val mapDao: MapDao,
    private val application: Application
) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val pdrManager = PdrManager(application)
    private val mapMatcher = MapMatcher()
    private val pathFinder = PathFinder()
    private var tts: TextToSpeech? = null

    // Configuration
    private val useRender = true // Toggle this to true for cloud deployment
    private val renderUrl = "indonavv-backend.onrender.com" // Update this with your actual Render URL

    private val serverIp = if (Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("Emulator") || Build.BRAND.startsWith("generic")) {
        "10.0.2.2"
    } else {
        "192.168.1.3"
    }
    
    private val apiBaseUrl = if (useRender) "https://$renderUrl/" else "http://$serverIp:8080/"
    private val wsUrl = if (useRender) "wss://$renderUrl/map/updates" else "ws://$serverIp:8080/map/updates"
    
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
        
    private val apiService = Retrofit.Builder()
        .baseUrl(apiBaseUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(MapApiService::class.java)

    private val _syncStatus = MutableStateFlow("Initializing...")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    // Data flows from Local DB
    val floors: StateFlow<List<Floor>> = mapDao.getAllFloors()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nodes: StateFlow<List<Node>> = mapDao.getAllNodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val edges: StateFlow<List<Edge>> = mapDao.getAllEdges()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val roomBlocks: StateFlow<List<RoomBlock>> = mapDao.getAllRoomBlocks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPOIs: StateFlow<List<POI>> = mapDao.getAllPOIs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI State
    private val _currentFloorId = MutableStateFlow("lg")
    val currentFloorId: StateFlow<String> = _currentFloorId.asStateFlow()

    private val _startSearchQuery = MutableStateFlow("")
    val startSearchQuery: StateFlow<String> = _startSearchQuery.asStateFlow()

    private val _destSearchQuery = MutableStateFlow("")
    val destSearchQuery: StateFlow<String> = _destSearchQuery.asStateFlow()

    private val _isSelectingStart = MutableStateFlow(true)
    val isSelectingStart: StateFlow<Boolean> = _isSelectingStart.asStateFlow()

    val startSearchResults: StateFlow<List<POI>> = combine(_startSearchQuery, allPOIs) { query, pois ->
        if (query.isBlank()) pois else pois.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val destSearchResults: StateFlow<List<POI>> = combine(_destSearchQuery, allPOIs) { query, pois ->
        if (query.isBlank()) pois else pois.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _bgImageUrl = MutableStateFlow<String?>(null)
    val bgImageUrl: StateFlow<String?> = _bgImageUrl.asStateFlow()

    private val _selectedPOI = MutableStateFlow<POI?>(null)
    val selectedPOI: StateFlow<POI?> = _selectedPOI.asStateFlow()

    private val _startPOI = MutableStateFlow<POI?>(null)
    val startPOI: StateFlow<POI?> = _startPOI.asStateFlow()

    private val _destinationPOI = MutableStateFlow<POI?>(null)
    val destinationPOI: StateFlow<POI?> = _destinationPOI.asStateFlow()

    private val _userPosition = MutableStateFlow(Offset(100f, 100f))
    val userPosition: StateFlow<Offset> = _userPosition.asStateFlow()

    private val _userHeading = MutableStateFlow(0f)
    val userHeading: StateFlow<Float> = _userHeading.asStateFlow()

    private val _isPathfinding = MutableStateFlow(false)
    val isPathfinding: StateFlow<Boolean> = _isPathfinding.asStateFlow()

    private val _currentPath = MutableStateFlow<List<Node>>(emptyList())
    val currentPath: StateFlow<List<Node>> = _currentPath.asStateFlow()

    private val _navigationInstruction = MutableStateFlow("Find a destination to start")
    val navigationInstruction: StateFlow<String> = _navigationInstruction.asStateFlow()

    private val _remainingDistance = MutableStateFlow(0f)
    val remainingDistance: StateFlow<Float> = _remainingDistance.asStateFlow()

    private val _isVoiceEnabled = MutableStateFlow(true)
    val isVoiceEnabled: StateFlow<Boolean> = _isVoiceEnabled.asStateFlow()

    val appVersion = "1.4.1-multilevel"

    private val _isCalibrating = MutableStateFlow(false)
    val isCalibrating: StateFlow<Boolean> = _isCalibrating.asStateFlow()

    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    private val pixelsPerMeter = 15f
    private val stepLengthPixels = 0.7f * pixelsPerMeter
    private var isArrived = false
    private var lastAddedNodeId: String? = null
    private var lastInstruction = ""
    private var webSocket: WebSocket? = null

    var onTriggerVoice: (() -> Unit)? = null

    // Track consecutive steps on the same edge for correction
    private var edgeTrackingCount = 0
    private var lastEdgeId: String? = null

    init {
        tts = TextToSpeech(application, this)
        startPdrTracking()
        observeNavigation()
        syncData()
        connectWebSocket()
    }

    private fun connectWebSocket() {
        val request = Request.Builder().url(wsUrl).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("MapViewModel", "WebSocket Connected to $wsUrl")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text == "updated") {
                    Log.d("MapViewModel", "Realtime update received from server")
                    syncData()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("MapViewModel", "WebSocket error on $wsUrl: ${t.message}")
                viewModelScope.launch {
                    delay(10000)
                    connectWebSocket()
                }
            }
        })
    }

    fun syncData() {
        viewModelScope.launch {
            try {
                _syncStatus.value = "Syncing..."
                Log.d("MapViewModel", "Starting sync from $apiBaseUrl")
                
                // Try to get everything in one call
                val response = try {
                    apiService.getFullMap()
                } catch (e: Exception) {
                    Log.e("MapViewModel", "getFullMap failed, trying individual endpoints: ${e.message}")
                    null
                }

                if (response != null) {
                    Log.d("MapViewModel", "Sync success (Unified): ${response.nodes.size} nodes, ${response.pois.size} POIs")
                    mapDao.clearAndInsertMapData(
                        response.nodes, 
                        response.edges, 
                        response.pois, 
                        response.roomBlocks, 
                        response.floors
                    )
                    _syncStatus.value = "Map Ready"
                } else {
                    // Fallback to individual calls
                    val hospitalId = "h1"
                    val nodesList = apiService.getNodes(hospitalId)
                    val edgesList = apiService.getEdges(hospitalId)
                    val poisList = apiService.getPOIs(hospitalId)
                    val floorsList = apiService.getFloors(hospitalId)
                    val blocksList = try { apiService.getRoomBlocks(hospitalId) } catch (e: Exception) { emptyList() }
                    
                    Log.d("MapViewModel", "Sync success (Individual): ${nodesList.size} nodes")
                    mapDao.clearAndInsertMapData(nodesList, edgesList, poisList, blocksList, floorsList)
                    _syncStatus.value = "Map Ready"
                }
                
                updateBackgroundImage()

                // If user position is at default, snap to first node
                val currentNodes = nodes.value
                if (currentNodes.isNotEmpty() && (_userPosition.value == Offset(100f, 100f) || _userPosition.value == Offset(0f, 0f))) {
                    val initialNode = currentNodes.find { it.floorId == _currentFloorId.value } ?: currentNodes[0]
                    _userPosition.value = Offset(initialNode.x, initialNode.y)
                }
            } catch (e: Exception) {
                Log.e("MapViewModel", "Sync failed COMPLETELY at $apiBaseUrl: ${e.message}")
                _syncStatus.value = "Connection Error"
            }
        }
    }

    private fun updateBackgroundImage() {
        val floor = floors.value.find { it.id == _currentFloorId.value }
        if (floor?.bgImageUrl != null) {
            _bgImageUrl.value = "${apiBaseUrl}${floor.bgImageUrl}?t=${System.currentTimeMillis()}"
        } else {
             _bgImageUrl.value = "${apiBaseUrl}uploads/map_background.png"
        }
    }

    fun selectFloor(floorId: String) {
        _currentFloorId.value = floorId
        updateBackgroundImage()
    }

    private fun startPdrTracking() {
        viewModelScope.launch {
            pdrManager.getPdrUpdates().collect { update ->
                if (update.stepDetected) {
                    val radians = Math.toRadians(update.headingDegrees.toDouble())
                    val dx = (stepLengthPixels * sin(radians)).toFloat()
                    val dy = -(stepLengthPixels * cos(radians)).toFloat()
                    val estimatedPos = Offset(_userPosition.value.x + dx, _userPosition.value.y + dy)
                    
                    val floorNodes = nodes.value.filter { it.floorId == _currentFloorId.value }
                    val snappedPos = mapMatcher.matchToGraph(estimatedPos, floorNodes, edges.value, update.headingDegrees, _currentPath.value)
                    _userPosition.value = snappedPos

                    performMapAidedCorrection(snappedPos, update.headingDegrees)
                }
                _userHeading.value = update.headingDegrees
            }
        }
    }

    private fun performMapAidedCorrection(snappedPos: Offset, sensorHeading: Float) {
        val path = _currentPath.value
        if (path.size < 2) {
            edgeTrackingCount = 0
            return
        }

        var currentPathEdge: Pair<Node, Node>? = null
        for (i in 0 until path.size - 1) {
            val n1 = path[i]
            val n2 = path[i+1]
            if (n1.floorId != _currentFloorId.value) continue
            
            val p1 = Offset(n1.x, n1.y)
            val p2 = Offset(n2.x, n2.y)
            if (distToSegment(snappedPos, p1, p2) < 5.0f) { 
                currentPathEdge = n1 to n2
                break
            }
        }

        if (currentPathEdge != null) {
            val (n1, n2) = currentPathEdge
            val edgeId = "${n1.id}_${n2.id}"
            
            if (edgeId == lastEdgeId) {
                edgeTrackingCount++
                if (edgeTrackingCount >= 5) {
                    val dx = n2.x - n1.x
                    val dy = n2.y - n1.y
                    val angleRad = atan2(dy.toDouble(), dx.toDouble())
                    val corridorHeading = (Math.toDegrees(angleRad).toFloat() + 90f + 360f) % 360f
                    
                    val diff = abs(corridorHeading - sensorHeading)
                    val minDiff = min(diff, 360f - diff)
                    
                    val correctedTarget = if (minDiff < 90f) {
                        corridorHeading
                    } else {
                        (corridorHeading + 180f) % 360f
                    }
                    
                    val finalDiff = abs(correctedTarget - sensorHeading)
                    val minFinalDiff = min(finalDiff, 360f - finalDiff)
                    
                    if (minFinalDiff > 0.5f && minFinalDiff < 15f) {
                        pdrManager.applyMapAidedCorrection(correctedTarget)
                    }
                    
                    edgeTrackingCount = 0
                }
            } else {
                lastEdgeId = edgeId
                edgeTrackingCount = 1
            }
        } else {
            edgeTrackingCount = 0
        }
    }

    private fun distToSegment(p: Offset, a: Offset, b: Offset): Float {
        val l2 = (a - b).getDistance().pow(2)
        if (l2 == 0f) return (p - a).getDistance()
        var t = ((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)) / l2
        t = t.coerceIn(0f, 1f)
        val projection = a + (b - a) * t
        return (p - projection).getDistance()
    }

    private fun observeNavigation() {
        combine(userPosition, startPOI, destinationPOI, nodes, edges) { pos, start, dest, nodeList, edgeList ->
            if (dest != null && nodeList.isNotEmpty()) {
                _isPathfinding.value = true
                val startId = start?.nodeId ?: nodeList.minByOrNull { (Offset(it.x, it.y) - pos).getDistance() }?.id
                if (startId != null) {
                    val result = pathFinder.findPath(startId, dest.nodeId, nodeList, edgeList)
                    Log.d("MapViewModel", "Path from $startId to ${dest.nodeId} found: ${result?.nodes?.size ?: 0} nodes")
                    _isPathfinding.value = false
                    result?.nodes ?: emptyList()
                } else {
                    Log.e("MapViewModel", "Pathfinding: Start node not found near $pos")
                    _isPathfinding.value = false
                    emptyList()
                }
            } else {
                _isPathfinding.value = false
                emptyList()
            }
        }.onEach { path -> 
            _currentPath.value = path
            updateInstructions(path) 
            
            if (path.isNotEmpty()) {
                val firstNode = path[0]
                if (firstNode.floorId != _currentFloorId.value) {
                    selectFloor(firstNode.floorId)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun updateInstructions(path: List<Node>) {
        if (path.isEmpty()) {
            _navigationInstruction.value = "Find a destination to start"
            _remainingDistance.value = 0f
            isArrived = false
            return
        }

        val userPos = _userPosition.value
        
        var minDistToAny = Float.MAX_VALUE
        var closestIdx = 0
        for (i in path.indices) {
            val d = (userPos - Offset(path[i].x, path[i].y)).getDistance()
            if (d < minDistToAny) {
                minDistToAny = d
                closestIdx = i
            }
        }
        
        val nextNodeIdx = (if (minDistToAny < 20f) closestIdx + 1 else closestIdx).coerceIn(0, path.size - 1)

        var totalDistPixels = (userPos - Offset(path[nextNodeIdx].x, path[nextNodeIdx].y)).getDistance()
        for (i in nextNodeIdx until path.size - 1) {
            totalDistPixels += sqrt((path[i].x - path[i+1].x).pow(2) + (path[i].y - path[i+1].y).pow(2))
        }
        _remainingDistance.value = totalDistPixels / pixelsPerMeter

        val destNode = path.last()
        if (destNode.floorId == _currentFloorId.value) {
            val distToDest = (userPos - Offset(destNode.x, destNode.y)).getDistance() / pixelsPerMeter
            if (distToDest < 1.5f && !isArrived) {
                isArrived = true
                updateNavigationText("You have arrived!")
                return
            }
        }

        if (nextNodeIdx < path.size) {
            val targetNode = path[nextNodeIdx]
            
            if (targetNode.floorId != _currentFloorId.value) {
                val targetFloor = floors.value.find { it.id == targetNode.floorId }
                updateNavigationText("Go to ${targetFloor?.name ?: "another floor"}")
                return
            }

            val distToTarget = (userPos - Offset(targetNode.x, targetNode.y)).getDistance() / pixelsPerMeter
            if (nextNodeIdx + 1 < path.size) {
                val nextNode = path[nextNodeIdx + 1]
                
                if (nextNode.floorId != targetNode.floorId) {
                    val verb = when (targetNode.type) {
                        NodeType.ELEVATOR -> "Take the elevator"
                        NodeType.STAIRS -> "Use the stairs"
                        else -> "Change floors"
                    }
                    updateNavigationText("$verb to next floor")
                    return
                }

                val prevNode = path[max(0, nextNodeIdx - 1)]
                val angle1 = atan2((targetNode.y - prevNode.y).toDouble(), (targetNode.x - prevNode.x).toDouble())
                val angle2 = atan2((nextNode.y - targetNode.y).toDouble(), (nextNode.x - targetNode.x).toDouble())
                
                var angleDiff = Math.toDegrees(angle2 - angle1)
                while (angleDiff < -180) angleDiff += 360
                while (angleDiff > 180) angleDiff -= 360
                
                val poiAtTurn = allPOIs.value.find { it.nodeId == targetNode.id }
                val turnRef = if (poiAtTurn != null) " at ${poiAtTurn.name}" else ""

                val command = when {
                    distToTarget < 1.5f && angleDiff > 45 -> "Turn right now$turnRef"
                    distToTarget < 1.5f && angleDiff < -45 -> "Turn left now$turnRef"
                    distToTarget < 4.5f && angleDiff > 45 -> "Ahead, turn right$turnRef"
                    distToTarget < 4.5f && angleDiff < -45 -> "Ahead, turn left$turnRef"
                    distToTarget < 1.5f && abs(angleDiff) > 120 -> "Make a U-turn"
                    else -> "Go straight"
                }
                
                if (command != "Go straight" || lastInstruction == "") {
                    updateNavigationText(command)
                }
            } else {
                updateNavigationText("Destination is ahead")
            }
        }
    }

    private fun updateNavigationText(text: String) {
        if (_navigationInstruction.value != text) {
            _navigationInstruction.value = text
            if (_isVoiceEnabled.value && text != lastInstruction) {
                speak(text)
                lastInstruction = text
            }
        }
    }

    private fun speak(text: String) {
        if (_isVoiceEnabled.value) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun vibrate(durationMillis: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMillis)
        }
    }

    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US }
    fun onStartSearchQueryChange(query: String) { _startSearchQuery.value = query }
    fun onDestSearchQueryChange(query: String) { _destSearchQuery.value = query }
    
    fun setStartPOI(poi: POI?) { 
        _startPOI.value = poi
        if (poi != null) {
            nodes.value.find { it.id == poi.nodeId }?.let { 
                selectFloor(it.floorId)
                _userPosition.value = Offset(it.x, it.y) 
            }
            _isSelectingStart.value = false
            speak("Setting start to ${poi.name}. Where would you like to go?")
        }
    }
    
    fun setDestination(poi: POI?) { 
        _destinationPOI.value = poi 
        _destSearchQuery.value = "" 
        isArrived = false 
        if (poi != null) {
            speak("Destination set to ${poi.name}. Follow the blue arrow.")
        }
    }
    
    fun clearDestination() { 
        _destinationPOI.value = null 
        _startPOI.value = null 
        _currentPath.value = emptyList() 
        isArrived = false 
        _isSelectingStart.value = true 
        speak("Navigation cleared")
    }

    fun calibrateSensors() {
        viewModelScope.launch {
            _isCalibrating.value = true
            speak("Calibrating sensors. Please keep your phone still.")
            vibrate(500)
            delay(3000)
            speak("Calibration complete.")
            vibrate(200)
            _isCalibrating.value = false
        }
    }

    fun processVoiceInput(text: String) {
        val lowerText = text.lowercase()
        if (lowerText.contains("cancel") || lowerText.contains("stop")) {
            clearDestination()
            return
        }

        viewModelScope.launch {
            val pois = allPOIs.value
            val matchedPoi = pois.find { it.name.lowercase().contains(lowerText) }
            
            if (matchedPoi != null) {
                if (_isSelectingStart.value) {
                    setStartPOI(matchedPoi)
                    onTriggerVoice?.invoke()
                } else {
                    setDestination(matchedPoi)
                }
            } else {
                speak("I couldn't find $text. Please try again.")
            }
        }
    }

    fun triggerAssistant() {
        if (_isSelectingStart.value) speak("Where are you starting from?")
        else speak("Where would you like to go?")
        onTriggerVoice?.invoke()
    }

    fun triggerSync() {
        syncData()
    }

    fun toggleVoice() { 
        _isVoiceEnabled.value = !_isVoiceEnabled.value 
        if (_isVoiceEnabled.value) speak("Voice assistant enabled.")
    }

    fun toggleAdminMode() {
        _isAdminMode.value = !_isAdminMode.value
        if (_isAdminMode.value) speak("Admin mode enabled.")
        else speak("Admin mode disabled.")
    }

    fun addNodeAtCurrentPosition(type: NodeType) {
        viewModelScope.launch {
            try {
                val newNode = Node(
                    id = UUID.randomUUID().toString(),
                    floorId = _currentFloorId.value,
                    x = _userPosition.value.x,
                    y = _userPosition.value.y,
                    type = type
                )
                apiService.addNode(newNode)
                lastAddedNodeId = newNode.id
                syncData()
                vibrate(100)
            } catch (e: Exception) {
                Log.e("MapViewModel", "Failed to add node: ${e.message}")
            }
        }
    }

    fun addEdgeToCurrentPosition() {
        val fromId = lastAddedNodeId ?: return
        viewModelScope.launch {
            try {
                val toNode = Node(
                    id = UUID.randomUUID().toString(),
                    floorId = _currentFloorId.value,
                    x = _userPosition.value.x,
                    y = _userPosition.value.y,
                    type = NodeType.JUNCTION
                )
                apiService.addNode(toNode)
                
                val fromNode = mapDao.getAllNodes().first().find { it.id == fromId }
                if (fromNode != null) {
                    val distance = sqrt((toNode.x - fromNode.x).pow(2) + (toNode.y - fromNode.y).pow(2))
                    val newEdge = Edge(
                        id = UUID.randomUUID().toString(),
                        fromNodeId = fromId,
                        toNodeId = toNode.id,
                        distance = distance
                    )
                    apiService.addEdge(newEdge)
                }
                
                lastAddedNodeId = toNode.id
                syncData()
                vibrate(100)
            } catch (e: Exception) {
                Log.e("MapViewModel", "Failed to add edge: ${e.message}")
            }
        }
    }

    fun addPOIAtCurrentPosition(name: String, category: String) {
        val currentNodeId = lastAddedNodeId ?: nodes.value.minByOrNull { (Offset(it.x, it.y) - _userPosition.value).getDistance() }?.id ?: return
        viewModelScope.launch {
            try {
                val newPOI = POI(
                    id = UUID.randomUUID().toString(),
                    nodeId = currentNodeId,
                    name = name,
                    category = category,
                    description = null
                )
                apiService.addPOI(newPOI)
                syncData()
                vibrate(200)
            } catch (e: Exception) {
                Log.e("MapViewModel", "Failed to add POI: ${e.message}")
            }
        }
    }

    override fun onCleared() { 
        super.onCleared()
        tts?.shutdown() 
        webSocket?.close(1000, "ViewModel cleared")
    }
}

class MapViewModelFactory(private val mapDao: MapDao, private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) return MapViewModel(mapDao, application) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
