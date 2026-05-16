package com.statproof.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.statproof.data.db.dao.FavoriteDao
import com.statproof.data.db.dao.RecentHistoryDao
import com.statproof.data.db.dao.TheoremDao
import com.statproof.data.db.entities.FavoriteEntity
import com.statproof.data.db.entities.RecentHistoryEntity
import com.statproof.data.db.entities.TheoremEntity
import com.statproof.data.db.entities.TheoremFtsEntity

@Database(
    entities = [
        TheoremEntity::class,
        TheoremFtsEntity::class,
        FavoriteEntity::class,
        RecentHistoryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class StatProofDatabase : RoomDatabase() {
    abstract fun theoremDao(): TheoremDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentHistoryDao(): RecentHistoryDao

    companion object {
        const val DATABASE_NAME = "statproof.db"
    }
}
