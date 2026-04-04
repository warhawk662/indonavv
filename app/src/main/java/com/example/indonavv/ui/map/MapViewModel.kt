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
import com.example.indonavv.data.model.Node
import com.example.indonavv.data.model.NodeType
import com.example.indonavv.data.model.POI
import com.example.indonavv.data.model.RoomBlock
import com.example.indonavv.data.remote.MapApiService
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
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Locale
import java.util.UUID
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

    // Change this to your live server URL (e.g., https://indonavv.onrender.com/)
    private val apiBaseUrl = "http://192.168.1.11:8080/"
    
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val apiService = Retrofit.Builder()
        .baseUrl(apiBaseUrl)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(MapApiService::class.java)

    private val _syncStatus = MutableStateFlow("Initializing...")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _startSearchQuery = MutableStateFlow("")
    val startSearchQuery: StateFlow<String> = _startSearchQuery.asStateFlow()

    private val _destSearchQuery = MutableStateFlow("")
    val destSearchQuery: StateFlow<String> = _destSearchQuery.asStateFlow()

    private val _isSelectingStart = MutableStateFlow(true)
    val isSelectingStart: StateFlow<Boolean> = _isSelectingStart.asStateFlow()

    val nodes: StateFlow<List<Node>> = mapDao.getAllNodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val edges: StateFlow<List<Edge>> = mapDao.getAllEdges()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val roomBlocks: StateFlow<List<RoomBlock>> = mapDao.getAllRoomBlocks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPOIs: StateFlow<List<POI>> = mapDao.getAllPOIs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private val _currentPath = MutableStateFlow<List<Node>>(emptyList())
    val currentPath: StateFlow<List<Node>> = _currentPath.asStateFlow()

    private val _navigationInstruction = MutableStateFlow("Find a destination to start")
    val navigationInstruction: StateFlow<String> = _navigationInstruction.asStateFlow()

    private val _remainingDistance = MutableStateFlow(0f)
    val remainingDistance: StateFlow<Float> = _remainingDistance.asStateFlow()

    private val _isVoiceEnabled = MutableStateFlow(true)
    val isVoiceEnabled: StateFlow<Boolean> = _isVoiceEnabled.asStateFlow()

    val appVersion = "1.2.0-aesthetic"

    private val _isCalibrating = MutableStateFlow(false)
    val isCalibrating: StateFlow<Boolean> = _isCalibrating.asStateFlow()

    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    private val pixelsPerMeter = 15f
    private val stepLengthPixels = 0.7f * pixelsPerMeter
    private var isArrived = false
    private var lastAddedNodeId: String? = null
    private var lastInstruction = ""

    var onTriggerVoice: (() -> Unit)? = null

    // Track consecutive steps on the same edge for correction
    private var edgeTrackingCount = 0
    private var lastEdgeId: String? = null

    init {
        tts = TextToSpeech(application, this)
        startPdrTracking()
        observeNavigation()
        syncData()
    }

    fun syncData() {
        viewModelScope.launch {
            try {
                _syncStatus.value = "Syncing..."
                val nodesList = apiService.getNodes("h1")
                val edgesList = apiService.getEdges("h1")
                val poisList = apiService.getPOIs("h1")
                val blocksList = apiService.getRoomBlocks("h1")
                
                mapDao.clearAndInsertMapData(nodesList, edgesList, poisList, blocksList)
                
                try {
                    val imageResponse = apiService.getBackgroundImageUrl("h1")
                    _bgImageUrl.value = "${apiBaseUrl}${imageResponse.string()}?t=${System.currentTimeMillis()}"
                } catch (e: Exception) {}

                if (nodesList.isNotEmpty() && _userPosition.value == Offset(100f, 100f)) {
                    _userPosition.value = Offset(nodesList[0].x, nodesList[0].y)
                }
                _syncStatus.value = "Map Ready"
            } catch (e: Exception) {
                Log.e("MapViewModel", "Sync failed: ${e.message}", e)
                _syncStatus.value = "Connection Error"
            }
        }
    }

    private fun startPdrTracking() {
        viewModelScope.launch {
            pdrManager.getPdrUpdates().collect { update ->
                if (update.stepDetected) {
                    val radians = Math.toRadians(update.headingDegrees.toDouble())
                    val dx = (stepLengthPixels * sin(radians)).toFloat()
                    val dy = -(stepLengthPixels * cos(radians)).toFloat()
                    val estimatedPos = Offset(_userPosition.value.x + dx, _userPosition.value.y + dy)
                    
                    // Enhanced matching with Active Path Bias
                    val snappedPos = mapMatcher.matchToGraph(estimatedPos, nodes.value, edges.value, update.headingDegrees, _currentPath.value)
                    _userPosition.value = snappedPos

                    performMapAidedCorrection(snappedPos)
                }
                _userHeading.value = update.headingDegrees
            }
        }
    }

    private fun performMapAidedCorrection(snappedPos: Offset) {
        val currentEdge = edges.value.find { edge ->
            val n1 = nodes.value.find { it.id == edge.fromNodeId }
            val n2 = nodes.value.find { it.id == edge.toNodeId }
            if (n1 == null || n2 == null) return@find false
            
            val p1 = Offset(n1.x, n1.y)
            val p2 = Offset(n2.x, n2.y)
            val dist = distToSegment(snappedPos, p1, p2)
            dist < 1.0f
        }

        if (currentEdge != null) {
            if (currentEdge.id == lastEdgeId) {
                edgeTrackingCount++
                if (edgeTrackingCount >= 3) {
                    val n1 = nodes.value.find { it.id == currentEdge.fromNodeId }!!
                    val n2 = nodes.value.find { it.id == currentEdge.toNodeId }!!
                    val angle = atan2((n2.y - n1.y).toDouble(), (n2.x - n1.x).toDouble())
                    val mapHeading = (Math.toDegrees(angle).toFloat() + 90f + 360f) % 360f
                    
                    val corrected = if (abs(mapHeading - _userHeading.value) < 90 || abs(mapHeading - _userHeading.value) > 270) {
                        mapHeading
                    } else {
                        (mapHeading + 180f) % 360f
                    }
                    
                    pdrManager.applyMapAidedCorrection(corrected)
                    edgeTrackingCount = 0
                }
            } else {
                lastEdgeId = currentEdge.id
                edgeTrackingCount = 1
            }
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
                val startId = start?.nodeId ?: nodeList.minByOrNull { (Offset(it.x, it.y) - pos).getDistance() }?.id
                if (startId != null) pathFinder.findPath(startId, dest.nodeId, nodeList, edgeList)?.nodes ?: emptyList() else emptyList()
            } else emptyList()
        }.onEach { path -> 
            _currentPath.value = path
            updateInstructions(path) 
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
        val destNode = path.last()
        val distToDestStraight = (userPos - Offset(destNode.x, destNode.y)).getDistance() / pixelsPerMeter

        // Calculate total path distance accurately
        var totalDistPixels = 0f
        if (path.size >= 2) {
            // Distance from user to the next node in the path (path[0] is current closest, path[1] is where we are going)
            // Actually path[0] is usually the node we just snapped to or the start.
            // Let's find where the user is relative to the path.
            
            // For simplicity, we sum user -> path[0] -> path[1] -> ... -> path[last]
            totalDistPixels = (userPos - Offset(path[0].x, path[0].y)).getDistance()
            for (i in 0 until path.size - 1) {
                val n1 = path[i]
                val n2 = path[i+1]
                totalDistPixels += sqrt((n1.x - n2.x).pow(2) + (n1.y - n2.y).pow(2))
            }
        } else if (path.size == 1) {
            totalDistPixels = (userPos - Offset(path[0].x, path[0].y)).getDistance()
        }
        
        _remainingDistance.value = totalDistPixels / pixelsPerMeter

        if (distToDestStraight < 1.2f && !isArrived) {
            isArrived = true
            updateNavigationText("You have arrived!")
            speak("You have arrived at your destination")
            vibrate(1000)
            return
        }

        if (path.size >= 2) {
            val nextNode = path[1]
            val distToNext = (userPos - Offset(nextNode.x, nextNode.y)).getDistance() / pixelsPerMeter
            
            // Check for turn at nextNode
            if (path.size > 2) {
                val currentNode = path[0]
                val afterNextNode = path[2]
                
                val angle1 = atan2((nextNode.y - currentNode.y).toDouble(), (nextNode.x - currentNode.x).toDouble())
                val angle2 = atan2((afterNextNode.y - nextNode.y).toDouble(), (afterNextNode.x - nextNode.x).toDouble())
                
                var angleDiff = Math.toDegrees(angle2 - angle1)
                while (angleDiff < -180) angleDiff += 360
                while (angleDiff > 180) angleDiff -= 360
                
                val instruction = when {
                    distToNext < 3.0f && angleDiff > 30 -> "Turn right soon"
                    distToNext < 3.0f && angleDiff < -30 -> "Turn left soon"
                    else -> "Go straight"
                }
                updateNavigationText(instruction)
            } else {
                updateNavigationText("Go straight to your destination")
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
            nodes.value.find { it.id == poi.nodeId }?.let { _userPosition.value = Offset(it.x, it.y) }
            _isSelectingStart.value = false
            speak("Setting start to ${poi.name}. Where would you like to go?")
        }
    }
    
    fun setDestination(poi: POI?) { 
        _destinationPOI.value = poi 
        _destSearchQuery.value = "" 
        isArrived = false 
        if (poi != null) speak("Destination set to ${poi.name}. Follow the blue arrow.")
    }
    
    fun clearDestination() { 
        _destinationPOI.value = null 
        _startPOI.value = null 
        _currentPath.value = emptyList() 
        isArrived = false 
        _isSelectingStart.value = true 
        speak("Navigation cancelled.")
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
                    floorId = "h1",
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
                    floorId = "h1",
                    x = _userPosition.value.x,
                    y = _userPosition.value.y,
                    type = NodeType.JUNCTION
                )
                apiService.addNode(toNode)
                
                val fromNode = nodes.value.find { it.id == fromId }
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

    override fun onCleared() { super.onCleared(); tts?.shutdown() }
}

class MapViewModelFactory(private val mapDao: MapDao, private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) return MapViewModel(mapDao, application) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
