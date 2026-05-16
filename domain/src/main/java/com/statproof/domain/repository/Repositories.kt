package com.statproof.domain.repository

import com.statproof.proofengine.models.Difficulty
import com.statproof.proofengine.models.Proof
import com.statproof.proofengine.models.ProofMode
import com.statproof.proofengine.models.SearchResult
import com.statproof.proofengine.models.TheoremDefinition
import com.statproof.proofengine.models.Topic
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for theorem and proof data.
 *
 * Abstracts the data source (Room DB + JSON assets) from the domain layer.
 * Implementations live in the :data module.
 */
interface ProofRepository {

    /**
     * Load all theorem definitions from the offline dataset.
     * Results are returned as a [Flow] so the UI can observe changes.
     */
    fun getAllTheorems(): Flow<List<TheoremDefinition>>

    /**
     * Load theorems filtered by topic.
     */
    fun getTheoremsByTopic(topic: Topic): Flow<List<TheoremDefinition>>

    /**
     * Load a single theorem by ID.
     * Returns null if the ID is not found.
     */
    suspend fun getTheoremById(id: String): TheoremDefinition?

    /**
     * Search theorems by query string.
     * Searches title, tags, keywords, and description.
     *
     * @param query the search query
     * @param topics optional topic filter
     * @param difficulties optional difficulty filter
     * @return ordered list of [SearchResult] by relevance
     */
    suspend fun searchTheorems(
        query: String,
        topics: Set<Topic> = emptySet(),
        difficulties: Set<Difficulty> = emptySet(),
    ): List<SearchResult>

    /**
     * Generate a full [Proof] for the given theorem ID.
     *
     * @param theoremId the theorem to prove
     * @param mode presentation mode
     * @return the generated [Proof], or null if theorem not found
     */
    suspend fun generateProof(theoremId: String, mode: ProofMode): Proof?

    /**
     * Get recently viewed proof IDs, most recent first.
     */
    fun getRecentProofIds(): Flow<List<String>>

    /**
     * Record that a proof was viewed (for recents tracking).
     */
    suspend fun recordProofView(theoremId: String)

    /**
     * Get all favorited theorem IDs.
     */
    fun getFavoriteIds(): Flow<Set<String>>

    /**
     * Toggle favorite status for a theorem.
     * @return the new favorite state (true = now favorited)
     */
    suspend fun toggleFavorite(theoremId: String): Boolean

    /**
     * Check if a theorem is currently favorited.
     */
    suspend fun isFavorite(theoremId: String): Boolean

    /**
     * Get all available topics that have at least one theorem.
     */
    suspend fun getAvailableTopics(): List<Topic>

    /**
     * Get all available tags across the dataset.
     */
    suspend fun getAllTags(): List<String>

    /**
     * Get the total number of theorems in the dataset.
     */
    suspend fun getTheoremCount(): Int
}

/**
 * Repository interface for user preferences and settings.
 */
interface PreferencesRepository {

    /**
     * Observe the user's preferred [ProofMode].
     */
    fun getPreferredMode(): Flow<ProofMode>

    /**
     * Set the user's preferred [ProofMode].
     */
    suspend fun setPreferredMode(mode: ProofMode)

    /**
     * Observe whether dark mode is forced (null = system default).
     */
    fun getDarkModeOverride(): Flow<Boolean?>

    /**
     * Set dark mode override.
     */
    suspend fun setDarkModeOverride(isDark: Boolean?)

    /**
     * Observe the math rendering font size scale (1.0 = default).
     */
    fun getFontScale(): Flow<Float>

    /**
     * Set math rendering font size scale.
     */
    suspend fun setFontScale(scale: Float)

    /**
     * Observe whether dynamic color (Material You) is enabled.
     */
    fun getDynamicColorEnabled(): Flow<Boolean>

    /**
     * Enable or disable dynamic color.
     */
    suspend fun setDynamicColorEnabled(enabled: Boolean)

    /**
     * Observe whether substep hints are shown by default.
     */
    fun getShowHintsByDefault(): Flow<Boolean>

    /**
     * Set whether substep hints are shown by default.
     */
    suspend fun setShowHintsByDefault(show: Boolean)
}
