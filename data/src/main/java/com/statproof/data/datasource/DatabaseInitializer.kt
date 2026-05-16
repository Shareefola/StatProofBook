package com.statproof.data.datasource

import com.statproof.data.assets.ProofAssetLoader
import com.statproof.data.db.dao.TheoremDao
import com.statproof.data.db.entities.TheoremEntity
import com.statproof.proofengine.models.TheoremDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the Room database with theorem metadata from JSON asset files.
 *
 * This should be called once on first launch (or after a data version bump).
 * Subsequent runs will use the cached Room data for fast queries.
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    private val assetLoader: ProofAssetLoader,
    private val theoremDao: TheoremDao,
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Seed the database if it's empty.
     * Safe to call multiple times — skips if data already present.
     */
    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        val count = theoremDao.getTheoremCount()
        if (count == 0) {
            seed()
        }
    }

    /**
     * Force a full re-seed from assets (used after data updates).
     */
    suspend fun seed() = withContext(Dispatchers.IO) {
        val theorems = assetLoader.loadAllTheorems()
        val entities = theorems.map { it.toEntity() }
        theoremDao.insertTheorems(entities)
    }

    private fun TheoremDefinition.toEntity(): TheoremEntity = TheoremEntity(
        id = id,
        title = title,
        topic = topic.name,
        subtopic = subtopic,
        difficulty = difficulty.name,
        tags = json.encodeToString(tags),
        statementLatex = statement,
        conclusionLatex = conclusion,
        keywords = json.encodeToString(searchKeywords),
    )
}
