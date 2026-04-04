package com.example.indonavv.data.remote

import com.example.indonavv.data.model.Edge
import com.example.indonavv.data.model.Node
import com.example.indonavv.data.model.POI
import com.example.indonavv.data.model.RoomBlock
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MapApiService {
    @GET("maps/{hospitalId}/nodes")
    suspend fun getNodes(@Path("hospitalId") hospitalId: String): List<Node>

    @GET("maps/{hospitalId}/edges")
    suspend fun getEdges(@Path("hospitalId") hospitalId: String): List<Edge>

    @GET("maps/{hospitalId}/pois")
    suspend fun getPOIs(@Path("hospitalId") hospitalId: String): List<POI>

    @GET("maps/{hospitalId}/roomblocks")
    suspend fun getRoomBlocks(@Path("hospitalId") hospitalId: String): List<RoomBlock>

    @GET("maps/{hospitalId}/image")
    suspend fun getBackgroundImageUrl(@Path("hospitalId") hospitalId: String): ResponseBody

    @POST("map/node")
    suspend fun addNode(@Body node: Node): Node

    @POST("map/edge")
    suspend fun addEdge(@Body edge: Edge): Edge

    @POST("map/poi")
    suspend fun addPOI(@Body poi: POI): POI

    @POST("map/roomblock")
    suspend fun addRoomBlock(@Body roomBlock: RoomBlock): RoomBlock

    @DELETE("map/clear")
    suspend fun clearMap(): ResponseBody
}
