package com.statproof.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.statproof.data.db.entities.FavoriteEntity
import com.statproof.data.db.entities.RecentHistoryEntity
import com.statproof.data.db.entities.TheoremEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TheoremDao {

    @Query("SELECT * FROM theorems ORDER BY title ASC")
    fun getAllTheorems(): Flow<List<TheoremEntity>>

    @Query("SELECT * FROM theorems WHERE topic = :topic ORDER BY difficulty ASC, title ASC")
    fun getTheoremsByTopic(topic: String): Flow<List<TheoremEntity>>

    @Query("SELECT * FROM theorems WHERE id = :id LIMIT 1")
    suspend fun getTheoremById(id: String): TheoremEntity?

    @Query("SELECT COUNT(*) FROM theorems")
    suspend fun getTheoremCount(): Int

    @Query("SELECT DISTINCT topic FROM theorems ORDER BY topic")
    suspend fun getDistinctTopics(): List<String>

    @Query("SELECT DISTINCT tags FROM theorems")
    suspend fun getAllTagStrings(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTheorem(theorem: TheoremEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTheorems(theorems: List<TheoremEntity>)

    @Update
    suspend fun updateTheorem(theorem: TheoremEntity)

    @Query("UPDATE theorems SET view_count = view_count + 1, last_viewed_at = :timestamp WHERE id = :id")
    suspend fun incrementViewCount(id: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Full-text search using FTS4.
     *
     * Returns IDs of matching theorems, ordered by rank (best match first).
     * The MATCH operator uses FTS4 query syntax.
     */
    @Query("""
        SELECT theorems.* 
        FROM theorems 
        INNER JOIN theorems_fts ON theorems.rowid = theorems_fts.rowid 
        WHERE theorems_fts MATCH :query 
        ORDER BY theorems.title ASC
    """)
    suspend fun searchTheorems(query: String): List<TheoremEntity>

    /**
     * Search with topic filter.
     */
    @Query("""
        SELECT theorems.* 
        FROM theorems 
        INNER JOIN theorems_fts ON theorems.rowid = theorems_fts.rowid 
        WHERE theorems_fts MATCH :query 
        AND theorems.topic IN (:topics)
        ORDER BY theorems.title ASC
    """)
    suspend fun searchTheoremsByTopics(query: String, topics: List<String>): List<TheoremEntity>

    /**
     * Search by topic only (no text query).
     */
    @Query("""
        SELECT * FROM theorems 
        WHERE topic IN (:topics) 
        ORDER BY difficulty ASC, title ASC
    """)
    suspend fun filterByTopics(topics: List<String>): List<TheoremEntity>

    /**
     * Search by difficulty only.
     */
    @Query("""
        SELECT * FROM theorems 
        WHERE difficulty IN (:difficulties) 
        ORDER BY topic ASC, title ASC
    """)
    suspend fun filterByDifficulties(difficulties: List<String>): List<TheoremEntity>

    /**
     * Combined filter: topics + difficulties (no text query).
     */
    @Query("""
        SELECT * FROM theorems 
        WHERE topic IN (:topics) AND difficulty IN (:difficulties)
        ORDER BY topic ASC, difficulty ASC, title ASC
    """)
    suspend fun filterByTopicsAndDifficulties(
        topics: List<String>,
        difficulties: List<String>,
    ): List<TheoremEntity>
}

@Dao
interface FavoriteDao {

    @Query("SELECT theorem_id FROM favorites ORDER BY added_at DESC")
    fun getFavoriteIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE theorem_id = :theoremId)")
    suspend fun isFavorite(theoremId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE theorem_id = :theoremId")
    suspend fun removeFavorite(theoremId: String)

    @Transaction
    suspend fun toggleFavorite(theoremId: String): Boolean {
        return if (isFavorite(theoremId)) {
            removeFavorite(theoremId)
            false
        } else {
            addFavorite(FavoriteEntity(theoremId = theoremId))
            true
        }
    }

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun getFavoriteCount(): Int
}

@Dao
interface RecentHistoryDao {

    @Query("SELECT theorem_id FROM recent_history ORDER BY viewed_at DESC LIMIT :limit")
    fun getRecentIds(limit: Int = 20): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordView(entry: RecentHistoryEntity)

    @Transaction
    suspend fun recordViewWithPrune(theoremId: String, maxEntries: Int = 50) {
        recordView(RecentHistoryEntity(theoremId = theoremId))
        pruneOldEntries(maxEntries)
    }

    @Query("""
        DELETE FROM recent_history 
        WHERE theorem_id NOT IN (
            SELECT theorem_id FROM recent_history 
            ORDER BY viewed_at DESC 
            LIMIT :keepCount
        )
    """)
    suspend fun pruneOldEntries(keepCount: Int)

    @Query("DELETE FROM recent_history")
    suspend fun clearHistory()
}
