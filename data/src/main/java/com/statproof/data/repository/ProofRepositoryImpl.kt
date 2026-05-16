package com.statproof.data.repository

import com.statproof.data.assets.ProofAssetLoader
import com.statproof.data.db.dao.FavoriteDao
import com.statproof.data.db.dao.RecentHistoryDao
import com.statproof.data.db.dao.TheoremDao
import com.statproof.data.db.entities.TheoremEntity
import com.statproof.domain.repository.ProofRepository
import com.statproof.proofengine.engine.ProofEngine
import com.statproof.proofengine.models.Difficulty
import com.statproof.proofengine.models.Proof
import com.statproof.proofengine.models.ProofMode
import com.statproof.proofengine.models.SearchResult
import com.statproof.proofengine.models.TheoremDefinition
import com.statproof.proofengine.models.Topic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProofRepositoryImpl @Inject constructor(
    private val theoremDao: TheoremDao,
    private val favoriteDao: FavoriteDao,
    private val recentHistoryDao: RecentHistoryDao,
    private val assetLoader: ProofAssetLoader,
    private val proofEngine: ProofEngine,
) : ProofRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // In-memory cache of full theorem definitions (loaded from assets)
    private var theoremCache: Map<String, TheoremDefinition>? = null

    private suspend fun getCache(): Map<String, TheoremDefinition> {
        return theoremCache ?: assetLoader.loadAllTheorems()
            .associateBy { it.id }
            .also { theoremCache = it }
    }

    override fun getAllTheorems(): Flow<List<TheoremDefinition>> {
        return theoremDao.getAllTheorems().map { entities ->
            entities.mapNotNull { entity ->
                getCache()[entity.id]
            }
        }
    }

    override fun getTheoremsByTopic(topic: Topic): Flow<List<TheoremDefinition>> {
        return theoremDao.getTheoremsByTopic(topic.name).map { entities ->
            entities.mapNotNull { entity -> getCache()[entity.id] }
        }
    }

    override suspend fun getTheoremById(id: String): TheoremDefinition? {
        return getCache()[id]
    }

    override suspend fun searchTheorems(
        query: String,
        topics: Set<Topic>,
        difficulties: Set<Difficulty>,
    ): List<SearchResult> {
        val topicNames = topics.map { it.name }
        val difficultyNames = difficulties.map { it.name }

        val entities: List<TheoremEntity> = when {
            query.isNotBlank() && topics.isNotEmpty() -> {
                theoremDao.searchTheoremsByTopics(
                    sanitizeFtsQuery(query),
                    topicNames,
                )
            }
            query.isNotBlank() -> theoremDao.searchTheorems(sanitizeFtsQuery(query))
            topics.isNotEmpty() && difficulties.isNotEmpty() ->
                theoremDao.filterByTopicsAndDifficulties(topicNames, difficultyNames)
            topics.isNotEmpty() -> theoremDao.filterByTopics(topicNames)
            difficulties.isNotEmpty() -> theoremDao.filterByDifficulties(difficultyNames)
            else -> return emptyList()
        }

        val cache = getCache()
        return entities.mapIndexedNotNull { index, entity ->
            val definition = cache[entity.id] ?: return@mapIndexedNotNull null
            SearchResult(
                theoremId = entity.id,
                title = entity.title,
                topic = definition.topic,
                difficulty = definition.difficulty,
                snippet = buildSnippet(definition, query),
                score = (entities.size - index).toFloat() / entities.size,
                tags = json.decodeFromString(entity.tags),
            )
        }
    }

    override suspend fun generateProof(theoremId: String, mode: ProofMode): Proof? {
        val definition = getCache()[theoremId] ?: return null
        return proofEngine.generateProof(definition, mode)
    }

    override fun getRecentProofIds(): Flow<List<String>> =
        recentHistoryDao.getRecentIds()

    override suspend fun recordProofView(theoremId: String) {
        recentHistoryDao.recordViewWithPrune(theoremId)
        theoremDao.incrementViewCount(theoremId)
    }

    override fun getFavoriteIds(): Flow<Set<String>> =
        favoriteDao.getFavoriteIds().map { it.toSet() }

    override suspend fun toggleFavorite(theoremId: String): Boolean =
        favoriteDao.toggleFavorite(theoremId)

    override suspend fun isFavorite(theoremId: String): Boolean =
        favoriteDao.isFavorite(theoremId)

    override suspend fun getAvailableTopics(): List<Topic> {
        return theoremDao.getDistinctTopics().mapNotNull { name ->
            runCatching { Topic.valueOf(name) }.getOrNull()
        }
    }

    override suspend fun getAllTags(): List<String> {
        return theoremDao.getAllTagStrings()
            .flatMap { tagStr ->
                runCatching { json.decodeFromString<List<String>>(tagStr) }.getOrDefault(emptyList())
            }
            .distinct()
            .sorted()
    }

    override suspend fun getTheoremCount(): Int = theoremDao.getTheoremCount()

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Sanitize query for FTS4 MATCH syntax. */
    private fun sanitizeFtsQuery(query: String): String {
        val terms = query.trim().split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .map { it.replace("\"", "") }
        return if (terms.size == 1) "${terms[0]}*" else terms.joinToString(" ") { "$it*" }
    }

    /** Build a short snippet for search results. */
    private fun buildSnippet(definition: TheoremDefinition, query: String): String {
        val text = "${definition.title}. ${definition.intuition.ifBlank { definition.subtopic }}"
        return if (text.length <= MAX_SNIPPET_LENGTH) text
        else text.take(MAX_SNIPPET_LENGTH - ELLIPSIS.length) + ELLIPSIS
    }

    companion object {
        private const val MAX_SNIPPET_LENGTH = 120
        private const val ELLIPSIS = "…"
    }
}
