package com.example.indonavv.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

@Entity(tableName = "room_blocks")
@JsonClass(generateAdapter = true)
data class RoomBlock(
    @PrimaryKey val id: String,
    val name: String,
    val points: List<Point>,
    val color: String
)

@JsonClass(generateAdapter = true)
data class Point(val x: Float, val y: Float)

class Converters {
    private val moshi = Moshi.Builder().build()
    private val pointListType = Types.newParameterizedType(List::class.java, Point::class.java)
    private val adapter = moshi.adapter<List<Point>>(pointListType)

    @TypeConverter
    fun fromPointList(value: List<Point>): String {
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toPointList(value: String): List<Point>? {
        return adapter.fromJson(value)
    }
}
