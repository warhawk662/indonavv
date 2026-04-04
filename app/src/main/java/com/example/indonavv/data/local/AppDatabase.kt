package com.example.indonavv.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.indonavv.data.model.Converters
import com.example.indonavv.data.model.Edge
import com.example.indonavv.data.model.Node
import com.example.indonavv.data.model.POI
import com.example.indonavv.data.model.RoomBlock

@Database(entities = [Node::class, Edge::class, POI::class, RoomBlock::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mapDao(): MapDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "indonavv_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
