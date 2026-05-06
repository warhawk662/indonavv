package com.example.indonavv.backend

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import java.io.File
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.*
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import java.net.InetAddress

private val dataFile = File("map_data.json")
private val uploadsDir = File(System.getProperty("user.dir"), "uploads").apply { if (!exists()) mkdirs() }
private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

private var firestore: Firestore? = null
private var mapData = runBlocking { loadData() }

// Connected WebSocket sessions
private val sessions = Collections.newSetFromMap(ConcurrentHashMap<DefaultWebSocketSession, Boolean>())

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    val host = "0.0.0.0"
    
    println("--- indonavv Backend Initialization ---")
    
    // Initialize Firebase
    try {
        val serviceAccount = File("service-account.json")
        val options = if (serviceAccount.exists()) {
            println("Using service-account.json for Firebase")
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount.inputStream()))
                .build()
        } else {
            println("service-account.json not found, attempting default credentials")
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .build()
        }
        FirebaseApp.initializeApp(options)
        firestore = FirestoreClient.getFirestore()
        
        // Refresh mapData from Firestore now that it's initialized
        runBlocking {
            mapData = loadData()
        }
    } catch (e: Exception) {
        println("Firebase initialization failed: ${e.message}")
    }
    
    try {
        val localhost = InetAddress.getLocalHost()
        println("Local IP (Try this in App): ${localhost.hostAddress}")
    } catch (e: Exception) {}
    
    println("Listening on: http://$host:$port")
    println("Loaded ${mapData.nodes.size} nodes, ${mapData.edges.size} edges, ${mapData.pois.size} POIs")

    embeddedServer(Netty, port = port, host = host, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { json(json) }
    
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Delete)
        allowNonSimpleContentTypes = true
    }

    routing {
        get("/") { 
            call.respondText("indonavv backend is active.\nData: ${mapData.nodes.size} nodes, ${mapData.pois.size} POIs, ${mapData.floors.size} floors") 
        }
        
        staticFiles("/uploads", uploadsDir)

        webSocket("/map/updates") {
            sessions.add(this)
            println("New WebSocket client connected. Total sessions: ${sessions.size}")
            try {
                for (frame in incoming) {
                    // Keep connection open
                }
            } catch (e: Exception) {
                println("WebSocket error: ${e.message}")
            } finally {
                sessions.remove(this)
                println("WebSocket client disconnected.")
            }
        }

        get("/map") { call.respond(mapData) }

        post("/map/node") {
            val node = call.receive<Node>()
            mapData = mapData.copy(nodes = mapData.nodes.filter { it.id != node.id } + node)
            saveAndNotify()
            call.respond(HttpStatusCode.Created, node)
        }

        post("/map/edge") {
            val edge = call.receive<Edge>()
            mapData = mapData.copy(edges = mapData.edges.filter { it.id != edge.id } + edge)
            saveAndNotify()
            call.respond(HttpStatusCode.Created, edge)
        }

        post("/map/poi") {
            val poi = call.receive<POI>()
            mapData = mapData.copy(pois = mapData.pois.filter { it.id != poi.id } + poi)
            saveAndNotify()
            call.respond(HttpStatusCode.Created, poi)
        }

        post("/map/floor") {
            val floor = call.receive<Floor>()
            mapData = mapData.copy(floors = mapData.floors.filter { it.id != floor.id } + floor)
            saveAndNotify()
            call.respond(HttpStatusCode.Created, floor)
        }

        delete("/map/clear") {
            mapData = MapData(floors = listOf(Floor("lg", "Lower Ground", 0)))
            saveAndNotify()
            call.respond(HttpStatusCode.OK, "Cleared")
        }
        
        // Routes for app sync
        get("/maps/{id}/nodes") { 
            call.respond(mapData.nodes) 
        }
        get("/maps/{id}/edges") { 
            call.respond(mapData.edges) 
        }
        get("/maps/{id}/pois") { 
            call.respond(mapData.pois) 
        }
        get("/maps/{id}/roomblocks") { 
            call.respond(mapData.roomBlocks) 
        }
        get("/maps/{id}/floors") { 
            call.respond(mapData.floors) 
        }
    }
}

private suspend fun loadData(): MapData {
    // 1. Try Firestore First
    firestore?.let { fs ->
        try {
            val docRef = fs.collection("configs").document("map_data").get().get()
            if (docRef.exists()) {
                val content = docRef.getString("content")
                if (content != null) {
                    println("Successfully loaded map data from Firestore")
                    return json.decodeFromString<MapData>(content)
                }
            }
        } catch (e: Exception) {
            println("Firestore load failed: ${e.message}. Falling back to file.")
        }
    }

    // 2. Fallback to local file
    val searchPaths = listOf("map_data.json", "backend/map_data.json", "../map_data.json")
    var foundFile: File? = null
    for (path in searchPaths) {
        val file = File(path)
        if (file.exists()) {
            foundFile = file
            break
        }
    }

    if (foundFile == null) {
        println("Warning: map_data.json not found. Using empty data.")
        return MapData(floors = listOf(Floor("lg", "Lower Ground", 0)))
    }
    
    println("Loading data from: ${foundFile.absolutePath}")
    return try { 
        val content = foundFile.readText()
        val data = json.decodeFromString<MapData>(content)
        if (data.floors.isEmpty()) {
            data.copy(floors = listOf(Floor("lg", "Lower Ground", 0)))
        } else data
    } catch (e: Exception) { 
        println("Error parsing map_data.json: ${e.message}")
        MapData(floors = listOf(Floor("lg", "Lower Ground", 0))) 
    }
}

private suspend fun saveAndNotify() {
    try {
        val jsonContent = json.encodeToString(MapData.serializer(), mapData)
        
        // Save to Firestore
        firestore?.let { fs ->
            withContext(Dispatchers.IO) {
                fs.collection("configs").document("map_data")
                    .set(mapOf("content" to jsonContent, "updatedAt" to System.currentTimeMillis()))
                    .get() // Wait for completion
            }
            println("Map data saved to Firestore")
        }

        // Also save to local file as backup if possible
        try {
            dataFile.writeText(jsonContent)
        } catch (e: Exception) {
            // Might fail in cloud environment, ignore
        }

        notifyClients()
    } catch (e: Exception) {
        println("Save failed: ${e.message}")
    }
}

private suspend fun notifyClients() {
    val message = "updated"
    val deadSessions = mutableListOf<DefaultWebSocketSession>()
    sessions.forEach { session ->
        try {
            session.send(Frame.Text(message))
        } catch (e: Exception) {
            deadSessions.add(session)
        }
    }
    sessions.removeAll(deadSessions)
}
