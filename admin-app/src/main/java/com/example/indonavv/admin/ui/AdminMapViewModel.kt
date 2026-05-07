package com.example.indonavv.admin.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import com.example.indonavv.data.remote.MapApiService
import com.example.indonavv.sensor.PdrManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.*

class AdminMapViewModel(
    private val mapDao: MapDao,
    private val application: Application
) : AndroidViewModel(application) {

    private val pdrManager = PdrManager(application)
    
    // Configuration
    private val useRender = true // Toggle this to true for cloud deployment
    private val renderUrl = "indonavv-backend.onrender.com" // Update this with your actual Render URL

    private val serverIp = if (Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("Emulator") || Build.BRAND.startsWith("generic")) {
        "10.0.2.2"
    } else {
        "192.168.1.3"
    }
    
    private val apiBaseUrl = if (useRender) "https://$renderUrl/" else "http://$serverIp:8080/"
    
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
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

    val floors: StateFlow<List<Floor>> = mapDao.getAllFloors()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentFloorId = MutableStateFlow("lg")
    val currentFloorId: StateFlow<String> = _currentFloorId.asStateFlow()

    val nodes: StateFlow<List<Node>> = combine(mapDao.getAllNodes(), _currentFloorId) { allNodes, floorId ->
        allNodes.filter { it.floorId == floorId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val edges: StateFlow<List<Edge>> = mapDao.getAllEdges()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val pois: StateFlow<List<POI>> = mapDao.getAllPOIs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _userPosition = MutableStateFlow(Offset(100f, 100f))
    val userPosition: StateFlow<Offset> = _userPosition.asStateFlow()

    private val _userHeading = MutableStateFlow(0f)
    val userHeading: StateFlow<Float> = _userHeading.asStateFlow()

    private var lastAddedNodeId: String? = null
    
    private val _stepLength = MutableStateFlow(10.5f)
    val stepLength: StateFlow<Float> = _stepLength.asStateFlow()

    init {
        startPdrTracking()
        syncData()
    }

    fun selectFloor(floorId: String) {
        _currentFloorId.value = floorId
        // Reset position to first node of new floor if it exists
        viewModelScope.launch {
            val floorNodes = nodes.value
            if (floorNodes.isNotEmpty()) {
                _userPosition.value = Offset(floorNodes[0].x, floorNodes[0].y)
            }
        }
    }

    fun addFloor(name: String, level: Int) {
        viewModelScope.launch {
            try {
                val newFloor = Floor(UUID.randomUUID().toString(), name, level)
                apiService.addFloor(newFloor)
                syncData()
            } catch (e: Exception) {
                Log.e("AdminMapViewModel", "Add floor failed: ${e.message}")
            }
        }
    }

    fun setStepLength(value: Float) {
        _stepLength.value = value
    }

    fun syncData() {
        viewModelScope.launch {
            try {
                _syncStatus.value = "Syncing..."
                Log.d("AdminMapViewModel", "Syncing from: $apiBaseUrl")
                
                val response = apiService.getFullMap()
                Log.d("AdminMapViewModel", "Sync success: ${response.nodes.size} nodes, ${response.floors.size} floors")
                
                mapDao.clearAndInsertMapData(
                    response.nodes, 
                    response.edges, 
                    response.pois, 
                    response.roomBlocks, 
                    response.floors
                )
                
                if (response.nodes.isNotEmpty()) {
                    var currentNodes = response.nodes.filter { it.floorId == _currentFloorId.value }
                    if (currentNodes.isEmpty()) {
                        val firstNode = response.nodes[0]
                        _currentFloorId.value = firstNode.floorId
                        currentNodes = response.nodes.filter { it.floorId == firstNode.floorId }
                    }
                    
                    if (currentNodes.isNotEmpty() && (_userPosition.value == Offset(100f, 100f) || _userPosition.value == Offset(0f, 0f))) {
                        _userPosition.value = Offset(currentNodes[0].x, currentNodes[0].y)
                    }
                }
                
                _syncStatus.value = "Map Ready"
            } catch (e: Exception) {
                Log.e("AdminMapViewModel", "Sync failed: ${e.message}")
                
                // Fallback to individual endpoints if /map fails
                try {
                    val hospitalId = "h1"
                    val floorsList = apiService.getFloors(hospitalId)
                    val nodesList = apiService.getNodes(hospitalId)
                    val edgesList = apiService.getEdges(hospitalId)
                    val poisList = apiService.getPOIs(hospitalId)
                    val blocksList = try { apiService.getRoomBlocks(hospitalId) } catch (e: Exception) { emptyList() }
                    
                    mapDao.clearAndInsertMapData(nodesList, edgesList, poisList, blocksList, floorsList)
                    _syncStatus.value = "Map Ready (Partial)"
                } catch (e2: Exception) {
                    Log.e("AdminMapViewModel", "Fallback sync also failed: ${e2.message}")
                    _syncStatus.value = "Sync Error"
                }
            }
        }
    }

    fun clearMap() {
        viewModelScope.launch {
            try {
                _syncStatus.value = "Clearing..."
                apiService.clearMap()
                lastAddedNodeId = null
                syncData()
                vibrate(500)
            } catch (e: Exception) {
                Log.e("AdminMapViewModel", "Failed to clear map: ${e.message}")
                _syncStatus.value = "Clear Failed"
            }
        }
    }

    private fun startPdrTracking() {
        viewModelScope.launch {
            pdrManager.getPdrUpdates().collect { update ->
                if (update.stepDetected) {
                    val radians = Math.toRadians(update.headingDegrees.toDouble())
                    val dx = (_stepLength.value * sin(radians)).toFloat()
                    val dy = -(_stepLength.value * cos(radians)).toFloat()
                    _userPosition.value = Offset(_userPosition.value.x + dx, _userPosition.value.y + dy)
                }
                _userHeading.value = update.headingDegrees
            }
        }
    }

    fun snapToLastNode() {
        val lastId = lastAddedNodeId ?: return
        val lastNode = nodes.value.find { it.id == lastId } ?: return
        _userPosition.value = Offset(lastNode.x, lastNode.y)
        vibrate(300)
    }

    fun alignHeading(targetDegrees: Float) {
        pdrManager.applyMapAidedCorrection(targetDegrees)
        vibrate(300)
    }

    fun addNodeAtCurrentPosition(type: NodeType = NodeType.JUNCTION) {
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
                Log.e("AdminMapViewModel", "Failed to add node: ${e.message}")
                _syncStatus.value = "Add Node Failed"
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
                Log.e("AdminMapViewModel", "Failed to add edge: ${e.message}")
                _syncStatus.value = "Add Edge Failed"
            }
        }
    }

    fun addPOIAtCurrentPosition(name: String, category: String) {
        viewModelScope.launch {
            try {
                val currentNodeId = lastAddedNodeId ?: nodes.value.minByOrNull { 
                    val dx = it.x - _userPosition.value.x
                    val dy = it.y - _userPosition.value.y
                    sqrt(dx*dx + dy*dy)
                }?.id
                
                if (currentNodeId == null) {
                    Log.e("AdminMapViewModel", "Cannot add POI: No nodes found near current position")
                    _syncStatus.value = "Add POI Failed: No Nodes"
                    return@launch
                }

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
                Log.e("AdminMapViewModel", "Failed to add POI: ${e.message}")
                _syncStatus.value = "Add POI Failed"
            }
        }
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
}

class AdminMapViewModelFactory(private val mapDao: MapDao, private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminMapViewModel::class.java)) return AdminMapViewModel(mapDao, application) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
