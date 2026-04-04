package com.example.indonavv.`data`.local

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performInTransactionSuspending
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.example.indonavv.`data`.model.Converters
import com.example.indonavv.`data`.model.Edge
import com.example.indonavv.`data`.model.Node
import com.example.indonavv.`data`.model.NodeType
import com.example.indonavv.`data`.model.POI
import com.example.indonavv.`data`.model.Point
import com.example.indonavv.`data`.model.RoomBlock
import javax.`annotation`.processing.Generated
import kotlin.Float
import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class MapDao_Impl(
  __db: RoomDatabase,
) : MapDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfNode: EntityInsertAdapter<Node>

  private val __insertAdapterOfEdge: EntityInsertAdapter<Edge>

  private val __insertAdapterOfPOI: EntityInsertAdapter<POI>

  private val __insertAdapterOfRoomBlock: EntityInsertAdapter<RoomBlock>

  private val __converters: Converters = Converters()
  init {
    this.__db = __db
    this.__insertAdapterOfNode = object : EntityInsertAdapter<Node>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `nodes` (`id`,`floorId`,`x`,`y`,`type`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Node) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.floorId)
        statement.bindDouble(3, entity.x.toDouble())
        statement.bindDouble(4, entity.y.toDouble())
        statement.bindText(5, __NodeType_enumToString(entity.type))
      }
    }
    this.__insertAdapterOfEdge = object : EntityInsertAdapter<Edge>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `edges` (`id`,`fromNodeId`,`toNodeId`,`distance`,`weight`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Edge) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.fromNodeId)
        statement.bindText(3, entity.toNodeId)
        statement.bindDouble(4, entity.distance.toDouble())
        statement.bindDouble(5, entity.weight.toDouble())
      }
    }
    this.__insertAdapterOfPOI = object : EntityInsertAdapter<POI>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `pois` (`id`,`name`,`description`,`nodeId`,`category`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: POI) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        val _tmpDescription: String? = entity.description
        if (_tmpDescription == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpDescription)
        }
        statement.bindText(4, entity.nodeId)
        statement.bindText(5, entity.category)
      }
    }
    this.__insertAdapterOfRoomBlock = object : EntityInsertAdapter<RoomBlock>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `room_blocks` (`id`,`name`,`points`,`color`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: RoomBlock) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        val _tmp: String = __converters.fromPointList(entity.points)
        statement.bindText(3, _tmp)
        statement.bindText(4, entity.color)
      }
    }
  }

  public override suspend fun insertNodes(nodes: List<Node>): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfNode.insert(_connection, nodes)
  }

  public override suspend fun insertEdges(edges: List<Edge>): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfEdge.insert(_connection, edges)
  }

  public override suspend fun insertPOIs(pois: List<POI>): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfPOI.insert(_connection, pois)
  }

  public override suspend fun insertRoomBlocks(blocks: List<RoomBlock>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfRoomBlock.insert(_connection, blocks)
  }

  public override suspend fun clearAndInsertMapData(
    nodes: List<Node>,
    edges: List<Edge>,
    pois: List<POI>,
    roomBlocks: List<RoomBlock>,
  ): Unit = performInTransactionSuspending(__db) {
    super@MapDao_Impl.clearAndInsertMapData(nodes, edges, pois, roomBlocks)
  }

  public override fun getAllNodes(): Flow<List<Node>> {
    val _sql: String = "SELECT * FROM nodes"
    return createFlow(__db, false, arrayOf("nodes")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfFloorId: Int = getColumnIndexOrThrow(_stmt, "floorId")
        val _columnIndexOfX: Int = getColumnIndexOrThrow(_stmt, "x")
        val _columnIndexOfY: Int = getColumnIndexOrThrow(_stmt, "y")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _result: MutableList<Node> = mutableListOf()
        while (_stmt.step()) {
          val _item: Node
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpFloorId: String
          _tmpFloorId = _stmt.getText(_columnIndexOfFloorId)
          val _tmpX: Float
          _tmpX = _stmt.getDouble(_columnIndexOfX).toFloat()
          val _tmpY: Float
          _tmpY = _stmt.getDouble(_columnIndexOfY).toFloat()
          val _tmpType: NodeType
          _tmpType = __NodeType_stringToEnum(_stmt.getText(_columnIndexOfType))
          _item = Node(_tmpId,_tmpFloorId,_tmpX,_tmpY,_tmpType)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getNodesByFloor(floorId: String): Flow<List<Node>> {
    val _sql: String = "SELECT * FROM nodes WHERE floorId = ?"
    return createFlow(__db, false, arrayOf("nodes")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, floorId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfFloorId: Int = getColumnIndexOrThrow(_stmt, "floorId")
        val _columnIndexOfX: Int = getColumnIndexOrThrow(_stmt, "x")
        val _columnIndexOfY: Int = getColumnIndexOrThrow(_stmt, "y")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _result: MutableList<Node> = mutableListOf()
        while (_stmt.step()) {
          val _item: Node
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpFloorId: String
          _tmpFloorId = _stmt.getText(_columnIndexOfFloorId)
          val _tmpX: Float
          _tmpX = _stmt.getDouble(_columnIndexOfX).toFloat()
          val _tmpY: Float
          _tmpY = _stmt.getDouble(_columnIndexOfY).toFloat()
          val _tmpType: NodeType
          _tmpType = __NodeType_stringToEnum(_stmt.getText(_columnIndexOfType))
          _item = Node(_tmpId,_tmpFloorId,_tmpX,_tmpY,_tmpType)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllEdges(): Flow<List<Edge>> {
    val _sql: String = "SELECT * FROM edges"
    return createFlow(__db, false, arrayOf("edges")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfFromNodeId: Int = getColumnIndexOrThrow(_stmt, "fromNodeId")
        val _columnIndexOfToNodeId: Int = getColumnIndexOrThrow(_stmt, "toNodeId")
        val _columnIndexOfDistance: Int = getColumnIndexOrThrow(_stmt, "distance")
        val _columnIndexOfWeight: Int = getColumnIndexOrThrow(_stmt, "weight")
        val _result: MutableList<Edge> = mutableListOf()
        while (_stmt.step()) {
          val _item: Edge
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpFromNodeId: String
          _tmpFromNodeId = _stmt.getText(_columnIndexOfFromNodeId)
          val _tmpToNodeId: String
          _tmpToNodeId = _stmt.getText(_columnIndexOfToNodeId)
          val _tmpDistance: Float
          _tmpDistance = _stmt.getDouble(_columnIndexOfDistance).toFloat()
          val _tmpWeight: Float
          _tmpWeight = _stmt.getDouble(_columnIndexOfWeight).toFloat()
          _item = Edge(_tmpId,_tmpFromNodeId,_tmpToNodeId,_tmpDistance,_tmpWeight)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllPOIs(): Flow<List<POI>> {
    val _sql: String = "SELECT * FROM pois"
    return createFlow(__db, false, arrayOf("pois")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfNodeId: Int = getColumnIndexOrThrow(_stmt, "nodeId")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _result: MutableList<POI> = mutableListOf()
        while (_stmt.step()) {
          val _item: POI
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpNodeId: String
          _tmpNodeId = _stmt.getText(_columnIndexOfNodeId)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          _item = POI(_tmpId,_tmpName,_tmpDescription,_tmpNodeId,_tmpCategory)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun searchPOIs(query: String): Flow<List<POI>> {
    val _sql: String = "SELECT * FROM pois WHERE name LIKE '%' || ? || '%'"
    return createFlow(__db, false, arrayOf("pois")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, query)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfNodeId: Int = getColumnIndexOrThrow(_stmt, "nodeId")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _result: MutableList<POI> = mutableListOf()
        while (_stmt.step()) {
          val _item: POI
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String?
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          }
          val _tmpNodeId: String
          _tmpNodeId = _stmt.getText(_columnIndexOfNodeId)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          _item = POI(_tmpId,_tmpName,_tmpDescription,_tmpNodeId,_tmpCategory)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllRoomBlocks(): Flow<List<RoomBlock>> {
    val _sql: String = "SELECT * FROM room_blocks"
    return createFlow(__db, false, arrayOf("room_blocks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfPoints: Int = getColumnIndexOrThrow(_stmt, "points")
        val _columnIndexOfColor: Int = getColumnIndexOrThrow(_stmt, "color")
        val _result: MutableList<RoomBlock> = mutableListOf()
        while (_stmt.step()) {
          val _item: RoomBlock
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpPoints: List<Point>
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfPoints)
          val _tmp_1: List<Point>? = __converters.toPointList(_tmp)
          if (_tmp_1 == null) {
            error("Expected NON-NULL 'kotlin.collections.List<com.example.indonavv.`data`.model.Point>', but it was NULL.")
          } else {
            _tmpPoints = _tmp_1
          }
          val _tmpColor: String
          _tmpColor = _stmt.getText(_columnIndexOfColor)
          _item = RoomBlock(_tmpId,_tmpName,_tmpPoints,_tmpColor)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearAllNodes() {
    val _sql: String = "DELETE FROM nodes"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearAllEdges() {
    val _sql: String = "DELETE FROM edges"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearAllPOIs() {
    val _sql: String = "DELETE FROM pois"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearAllRoomBlocks() {
    val _sql: String = "DELETE FROM room_blocks"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __NodeType_enumToString(_value: NodeType): String = when (_value) {
    NodeType.ROOM -> "ROOM"
    NodeType.JUNCTION -> "JUNCTION"
    NodeType.ELEVATOR -> "ELEVATOR"
    NodeType.STAIRS -> "STAIRS"
    NodeType.ENTRANCE -> "ENTRANCE"
    NodeType.RESTROOM -> "RESTROOM"
    NodeType.CLINIC -> "CLINIC"
    NodeType.PHARMACY -> "PHARMACY"
  }

  private fun __NodeType_stringToEnum(_value: String): NodeType = when (_value) {
    "ROOM" -> NodeType.ROOM
    "JUNCTION" -> NodeType.JUNCTION
    "ELEVATOR" -> NodeType.ELEVATOR
    "STAIRS" -> NodeType.STAIRS
    "ENTRANCE" -> NodeType.ENTRANCE
    "RESTROOM" -> NodeType.RESTROOM
    "CLINIC" -> NodeType.CLINIC
    "PHARMACY" -> NodeType.PHARMACY
    else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: " + _value)
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
