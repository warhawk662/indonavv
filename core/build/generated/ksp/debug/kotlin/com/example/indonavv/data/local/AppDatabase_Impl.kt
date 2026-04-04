package com.example.indonavv.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _mapDao: Lazy<MapDao> = lazy {
    MapDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(2,
        "a36151f68ecd4d9c287cb4e20828ee68", "d6a324d35511f40aa92d2a776294e96d") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `nodes` (`id` TEXT NOT NULL, `floorId` TEXT NOT NULL, `x` REAL NOT NULL, `y` REAL NOT NULL, `type` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `edges` (`id` TEXT NOT NULL, `fromNodeId` TEXT NOT NULL, `toNodeId` TEXT NOT NULL, `distance` REAL NOT NULL, `weight` REAL NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `pois` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `nodeId` TEXT NOT NULL, `category` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `room_blocks` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `points` TEXT NOT NULL, `color` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'a36151f68ecd4d9c287cb4e20828ee68')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `nodes`")
        connection.execSQL("DROP TABLE IF EXISTS `edges`")
        connection.execSQL("DROP TABLE IF EXISTS `pois`")
        connection.execSQL("DROP TABLE IF EXISTS `room_blocks`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsNodes: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsNodes.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNodes.put("floorId", TableInfo.Column("floorId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNodes.put("x", TableInfo.Column("x", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNodes.put("y", TableInfo.Column("y", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNodes.put("type", TableInfo.Column("type", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysNodes: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesNodes: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoNodes: TableInfo = TableInfo("nodes", _columnsNodes, _foreignKeysNodes,
            _indicesNodes)
        val _existingNodes: TableInfo = read(connection, "nodes")
        if (!_infoNodes.equals(_existingNodes)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |nodes(com.example.indonavv.data.model.Node).
              | Expected:
              |""".trimMargin() + _infoNodes + """
              |
              | Found:
              |""".trimMargin() + _existingNodes)
        }
        val _columnsEdges: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsEdges.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEdges.put("fromNodeId", TableInfo.Column("fromNodeId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEdges.put("toNodeId", TableInfo.Column("toNodeId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEdges.put("distance", TableInfo.Column("distance", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsEdges.put("weight", TableInfo.Column("weight", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysEdges: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesEdges: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoEdges: TableInfo = TableInfo("edges", _columnsEdges, _foreignKeysEdges,
            _indicesEdges)
        val _existingEdges: TableInfo = read(connection, "edges")
        if (!_infoEdges.equals(_existingEdges)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |edges(com.example.indonavv.data.model.Edge).
              | Expected:
              |""".trimMargin() + _infoEdges + """
              |
              | Found:
              |""".trimMargin() + _existingEdges)
        }
        val _columnsPois: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsPois.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPois.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPois.put("description", TableInfo.Column("description", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPois.put("nodeId", TableInfo.Column("nodeId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPois.put("category", TableInfo.Column("category", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysPois: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesPois: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoPois: TableInfo = TableInfo("pois", _columnsPois, _foreignKeysPois, _indicesPois)
        val _existingPois: TableInfo = read(connection, "pois")
        if (!_infoPois.equals(_existingPois)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |pois(com.example.indonavv.data.model.POI).
              | Expected:
              |""".trimMargin() + _infoPois + """
              |
              | Found:
              |""".trimMargin() + _existingPois)
        }
        val _columnsRoomBlocks: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRoomBlocks.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRoomBlocks.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRoomBlocks.put("points", TableInfo.Column("points", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRoomBlocks.put("color", TableInfo.Column("color", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRoomBlocks: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRoomBlocks: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRoomBlocks: TableInfo = TableInfo("room_blocks", _columnsRoomBlocks,
            _foreignKeysRoomBlocks, _indicesRoomBlocks)
        val _existingRoomBlocks: TableInfo = read(connection, "room_blocks")
        if (!_infoRoomBlocks.equals(_existingRoomBlocks)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |room_blocks(com.example.indonavv.data.model.RoomBlock).
              | Expected:
              |""".trimMargin() + _infoRoomBlocks + """
              |
              | Found:
              |""".trimMargin() + _existingRoomBlocks)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "nodes", "edges", "pois",
        "room_blocks")
  }

  public override fun clearAllTables() {
    super.performClear(false, "nodes", "edges", "pois", "room_blocks")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(MapDao::class, MapDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun mapDao(): MapDao = _mapDao.value
}
