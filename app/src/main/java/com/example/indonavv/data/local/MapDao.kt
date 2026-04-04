package com.example.indonavv.data.local

import androidx.room.*
import com.example.indonavv.data.model.Edge
import com.example.indonavv.data.model.Node
import com.example.indonavv.data.model.POI
import com.example.indonavv.data.model.RoomBlock
import kotlinx.coroutines.flow.Flow

@Dao
interface MapDao {
    @Query("SELECT * FROM nodes")
    fun getAllNodes(): Flow<List<Node>>

    @Query("SELECT * FROM nodes WHERE floorId = :floorId")
    fun getNodesByFloor(floorId: String): Flow<List<Node>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<Node>)

    @Query("SELECT * FROM edges")
    fun getAllEdges(): Flow<List<Edge>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdges(edges: List<Edge>)

    @Query("SELECT * FROM pois")
    fun getAllPOIs(): Flow<List<POI>>

    @Query("SELECT * FROM pois WHERE name LIKE '%' || :query || '%'")
    fun searchPOIs(query: String): Flow<List<POI>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPOIs(pois: List<POI>)

    @Query("SELECT * FROM room_blocks")
    fun getAllRoomBlocks(): Flow<List<RoomBlock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoomBlocks(blocks: List<RoomBlock>)

    @Transaction
    suspend fun clearAndInsertMapData(
        nodes: List<Node>, 
        edges: List<Edge>, 
        pois: List<POI>,
        roomBlocks: List<RoomBlock> = emptyList()
    ) {
        clearAllNodes()
        clearAllEdges()
        clearAllPOIs()
        clearAllRoomBlocks()
        insertNodes(nodes)
        insertEdges(edges)
        insertPOIs(pois)
        insertRoomBlocks(roomBlocks)
    }

    @Query("DELETE FROM nodes")
    suspend fun clearAllNodes()

    @Query("DELETE FROM edges")
    suspend fun clearAllEdges()

    @Query("DELETE FROM pois")
    suspend fun clearAllPOIs()

    @Query("DELETE FROM room_blocks")
    suspend fun clearAllRoomBlocks()
}
