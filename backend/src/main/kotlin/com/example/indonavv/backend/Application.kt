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
import kotlinx.serialization.json.Json
import java.io.File
import io.ktor.http.content.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val dataFile = File("map_data.json")
private val uploadsDir = File(System.getProperty("user.dir"), "uploads").apply { if (!exists()) mkdirs() }
private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

private var mapData = loadData()

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    println("--- indonavv Backend Starting on port $port ---")
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { json(json) }
    install(IgnoreTrailingSlash)

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
        get("/") { call.respondText("indonavv backend is active") }
        staticFiles("/uploads", uploadsDir)

        get("/map") { call.respond(mapData) }

        post("/map/node") {
            val node = call.receive<Node>()
            mapData = mapData.copy(nodes = mapData.nodes.filter { it.id != node.id } + node)
            saveData()
            call.respond(HttpStatusCode.Created, node)
        }

        post("/map/edge") {
            val edge = call.receive<Edge>()
            mapData = mapData.copy(edges = mapData.edges.filter { it.id != edge.id } + edge)
            saveData()
            call.respond(HttpStatusCode.Created, edge)
        }

        post("/map/poi") {
            val poi = call.receive<POI>()
            mapData = mapData.copy(pois = mapData.pois.filter { it.id != poi.id } + poi)
            saveData()
            call.respond(HttpStatusCode.Created, poi)
        }

        post("/map/geofence") {
            val geofence = call.receive<Geofence>()
            mapData = mapData.copy(geofences = mapData.geofences.filter { it.id != geofence.id } + geofence)
            saveData()
            call.respond(HttpStatusCode.Created, geofence)
        }

        post("/map/roomblock") {
            val room = call.receive<RoomBlock>()
            mapData = mapData.copy(roomBlocks = mapData.roomBlocks.filter { it.id != room.id } + room)
            saveData()
            call.respond(HttpStatusCode.Created, room)
        }

        post("/map/image") {
            val multipart = call.receiveMultipart()
            var imageUrl: String? = null
            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    val file = File(uploadsDir, "map_background.png")
                    withContext(Dispatchers.IO) {
                        part.streamProvider().use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                    }
                    imageUrl = "uploads/map_background.png"
                }
                part.dispose()
            }
            if (imageUrl != null) {
                mapData = mapData.copy(bgImageUrl = imageUrl)
                saveData()
                call.respondText(imageUrl!!, status = HttpStatusCode.OK)
            } else call.respond(HttpStatusCode.BadRequest)
        }

        delete("/map/undo") {
            call.respond(HttpStatusCode.NotImplemented, "Use client-side undo for now")
        }

        delete("/map/clear") {
            mapData = MapData()
            saveData()
            call.respond(HttpStatusCode.OK, "Cleared")
        }
        
        get("/maps/{id}/nodes") { call.respond(mapData.nodes) }
        get("/maps/{id}/edges") { call.respond(mapData.edges) }
        get("/maps/{id}/pois") { call.respond(mapData.pois) }
        get("/maps/{id}/geofences") { call.respond(mapData.geofences) }
        get("/maps/{id}/roomblocks") { call.respond(mapData.roomBlocks) }
        get("/maps/{id}/image") {
            if (mapData.bgImageUrl != null) call.respondText(mapData.bgImageUrl!!)
            else call.respond(HttpStatusCode.NotFound)
        }
    }
}

private fun loadData(): MapData {
    return if (dataFile.exists()) {
        try { json.decodeFromString<MapData>(dataFile.readText()) } catch (e: Exception) { MapData() }
    } else MapData()
}

private fun saveData() {
    try { dataFile.writeText(json.encodeToString(MapData.serializer(), mapData)) } catch (e: Exception) { println("Save failed: ${e.message}") }
}
