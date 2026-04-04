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
import com.example.indonavv.data.model.Node
import com.example.indonavv.data.model.NodeType
import com.example.indonavv.data.model.POI
import com.example.indonavv.data.model.RoomBlock
import com.example.indonavv.data.remote.MapApiService
import com.example.indonavv.sensor.PdrManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.UUID
import kotlin.math.*

class AdminMapViewModel(
    private val mapDao: MapDao,
    private val application: Application
) : AndroidViewModel(application) {

    private val pdrManager = PdrManager(application)
    
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

    val nodes: StateFlow<List<Node>> = mapDao.getAllNodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun setStepLength(value: Float) {
        _stepLength.value = value
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
                
                if (nodesList.isNotEmpty() && _userPosition.value == Offset(100f, 100f)) {
                    _userPosition.value = Offset(nodesList[0].x, nodesList[0].y)
                }
                _syncStatus.value = "Map Ready (${nodesList.size} nodes)"
            } catch (e: Exception) {
                Log.e("AdminMapViewModel", "Sync failed: ${e.message}")
                _syncStatus.value = "Sync Error: Check Connection"
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
                Log.e("AdminMapViewModel", "Failed to add edge: ${e.message}")
                _syncStatus.value = "Add Edge Failed"
            }
        }
    }

    fun addPOIAtCurrentPosition(name: String, category: String) {
        val currentNodeId = lastAddedNodeId ?: return
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
        return AdminMapViewModel(mapDao, application) as T
    }
}
