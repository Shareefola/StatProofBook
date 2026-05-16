package com.statproof.domain.usecase

import com.statproof.domain.repository.PreferencesRepository
import com.statproof.domain.repository.ProofRepository
import com.statproof.proofengine.models.Difficulty
import com.statproof.proofengine.models.Proof
import com.statproof.proofengine.models.ProofMode
import com.statproof.proofengine.models.SearchResult
import com.statproof.proofengine.models.TheoremDefinition
import com.statproof.proofengine.models.Topic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Retrieves all theorems for the home screen.
 */
class GetAllTheoremsUseCase(
    private val repository: ProofRepository,
) {
    operator fun invoke(): Flow<List<TheoremDefinition>> = repository.getAllTheorems()
}

/**
 * Retrieves theorems grouped by topic for the library screen.
 */
class GetTheoremsByTopicUseCase(
    private val repository: ProofRepository,
) {
    operator fun invoke(topic: Topic): Flow<List<TheoremDefinition>> =
        repository.getTheoremsByTopic(topic)
}

/**
 * Generates a full proof for the viewer screen.
 *
 * Combines the theorem definition with the user's preferred proof mode
 * unless a mode is explicitly specified.
 */
class GenerateProofUseCase(
    private val proofRepository: ProofRepository,
    private val preferencesRepository: PreferencesRepository,
) {
    suspend operator fun invoke(
        theoremId: String,
        modeOverride: ProofMode? = null,
    ): Proof? {
        val mode = modeOverride ?: proofRepository.generateProof(theoremId, ProofMode.STANDARD)
            ?.mode ?: ProofMode.STANDARD
        return proofRepository.generateProof(theoremId, mode)
    }
}

/**
 * Searches the theorem database with optional filters.
 */
class SearchTheoremsUseCase(
    private val repository: ProofRepository,
) {
    suspend operator fun invoke(
        query: String,
        topics: Set<Topic> = emptySet(),
        difficulties: Set<Difficulty> = emptySet(),
    ): List<SearchResult> {
        if (query.isBlank() && topics.isEmpty() && difficulties.isEmpty()) {
            return emptyList()
        }
        return repository.searchTheorems(query, topics, difficulties)
    }
}

/**
 * Retrieves the home screen data: recents + favorites.
 */
class GetHomeDataUseCase(
    private val repository: ProofRepository,
) {
    fun getRecentTheorems(): Flow<List<TheoremDefinition>> {
        return repository.getRecentProofIds().map { ids ->
            ids.mapNotNull { repository.getTheoremById(it) }
        }
    }

    fun getFavoriteTheorems(): Flow<List<TheoremDefinition>> {
        return repository.getFavoriteIds().map { ids ->
            ids.mapNotNull { repository.getTheoremById(it) }
        }
    }

    fun getHomeState(): Flow<HomeData> {
        return combine(
            repository.getRecentProofIds(),
            repository.getFavoriteIds(),
        ) { recentIds, favoriteIds ->
            HomeData(
                recentIds = recentIds.take(MAX_RECENTS),
                favoriteIds = favoriteIds,
            )
        }
    }

    companion object {
        const val MAX_RECENTS = 10
    }
}

data class HomeData(
    val recentIds: List<String>,
    val favoriteIds: Set<String>,
)

/**
 * Records that a user viewed a proof (for recents tracking).
 */
class RecordProofViewUseCase(
    private val repository: ProofRepository,
) {
    suspend operator fun invoke(theoremId: String) {
        repository.recordProofView(theoremId)
    }
}

/**
 * Toggles the favorite status of a theorem.
 */
class ToggleFavoriteUseCase(
    private val repository: ProofRepository,
) {
    suspend operator fun invoke(theoremId: String): Boolean =
        repository.toggleFavorite(theoremId)
}

/**
 * Checks whether a theorem is currently favorited.
 */
class CheckFavoriteUseCase(
    private val repository: ProofRepository,
) {
    suspend operator fun invoke(theoremId: String): Boolean =
        repository.isFavorite(theoremId)
}

/**
 * Retrieves all available topics for filtering.
 */
class GetAvailableTopicsUseCase(
    private val repository: ProofRepository,
) {
    suspend operator fun invoke(): List<Topic> = repository.getAvailableTopics()
}

/**
 * Gets user preferences for the settings screen.
 */
class GetUserPreferencesUseCase(
    private val repository: PreferencesRepository,
) {
    fun getPreferredMode(): Flow<ProofMode> = repository.getPreferredMode()
    fun getDarkModeOverride(): Flow<Boolean?> = repository.getDarkModeOverride()
    fun getFontScale(): Flow<Float> = repository.getFontScale()
    fun getDynamicColorEnabled(): Flow<Boolean> = repository.getDynamicColorEnabled()
    fun getShowHintsByDefault(): Flow<Boolean> = repository.getShowHintsByDefault()
}

/**
 * Updates user preferences from the settings screen.
 */
class UpdateUserPreferencesUseCase(
    private val repository: PreferencesRepository,
) {
    suspend fun setPreferredMode(mode: ProofMode) = repository.setPreferredMode(mode)
    suspend fun setDarkModeOverride(isDark: Boolean?) = repository.setDarkModeOverride(isDark)
    suspend fun setFontScale(scale: Float) = repository.setFontScale(scale)
    suspend fun setDynamicColorEnabled(enabled: Boolean) = repository.setDynamicColorEnabled(enabled)
    suspend fun setShowHintsByDefault(show: Boolean) = repository.setShowHintsByDefault(show)
}

/**
 * Retrieves dataset statistics for the About screen.
 */
class GetDatasetStatsUseCase(
    private val repository: ProofRepository,
) {
    suspend fun getTheoremCount(): Int = repository.getTheoremCount()
    suspend fun getTopics(): List<Topic> = repository.getAvailableTopics()
    suspend fun getTags(): List<String> = repository.getAllTags()
}
