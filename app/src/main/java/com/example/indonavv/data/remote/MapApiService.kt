package com.example.indonavv.data.remote

import com.example.indonavv.data.model.*
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MapApiService {
    @GET("map")
    suspend fun getFullMap(): MapDataResponse

    @GET("maps/{hospitalId}/nodes")
    suspend fun getNodes(@Path("hospitalId") hospitalId: String): List<Node>

    @GET("maps/{hospitalId}/edges")
    suspend fun getEdges(@Path("hospitalId") hospitalId: String): List<Edge>

    @GET("maps/{hospitalId}/pois")
    suspend fun getPOIs(@Path("hospitalId") hospitalId: String): List<POI>

    @GET("maps/{hospitalId}/roomblocks")
    suspend fun getRoomBlocks(@Path("hospitalId") hospitalId: String): List<RoomBlock>

    @GET("maps/{hospitalId}/floors")
    suspend fun getFloors(@Path("hospitalId") hospitalId: String): List<Floor>

    @GET("maps/{floorId}/image")
    suspend fun getBackgroundImageUrl(@Path("floorId") floorId: String): ResponseBody

    @POST("map/node")
    suspend fun addNode(@Body node: Node): Node

    @POST("map/edge")
    suspend fun addEdge(@Body edge: Edge): Edge

    @POST("map/poi")
    suspend fun addPOI(@Body poi: POI): POI

    @POST("map/roomblock")
    suspend fun addRoomBlock(@Body roomBlock: RoomBlock): RoomBlock

    @POST("map/floor")
    suspend fun addFloor(@Body floor: Floor): Floor

    @DELETE("map/clear")
    suspend fun clearMap(): ResponseBody
}

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class MapDataResponse(
    val nodes: List<Node> = emptyList(),
    val edges: List<Edge> = emptyList(),
    val pois: List<POI> = emptyList(),
    val roomBlocks: List<RoomBlock> = emptyList(),
    val floors: List<Floor> = emptyList()
)
