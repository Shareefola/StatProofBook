package com.statproof.data.assets

import android.content.Context
import com.statproof.proofengine.models.TheoremDefinition
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads [TheoremDefinition] objects from JSON files bundled in assets.
 *
 * All JSON files live under `assets/proofs/`. Each file is a JSON array
 * of [TheoremDefinition] objects.
 *
 * Loading is done on [Dispatchers.IO] to avoid blocking the main thread.
 */
@Singleton
class ProofAssetLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    companion object {
        private val PROOF_FILES = listOf(
            "proofs/probability_theory.json",
            "proofs/distribution_theory.json",
            "proofs/statistical_inference.json",
            "proofs/hypothesis_testing.json",
            "proofs/regression_models.json",
            "proofs/bayesian_statistics.json",
            "proofs/sampling_theory.json",
            "proofs/stochastic_processes.json",
            "proofs/information_theory.json",
            "proofs/asymptotics.json",
        )
    }

    /**
     * Load all theorems from all proof JSON files.
     *
     * Results are de-duplicated by theorem ID (last definition wins).
     */
    suspend fun loadAllTheorems(): List<TheoremDefinition> = withContext(Dispatchers.IO) {
        val allTheorems = mutableListOf<TheoremDefinition>()
        for (fileName in PROOF_FILES) {
            try {
                val theorems = loadFile(fileName)
                allTheorems.addAll(theorems)
            } catch (e: Exception) {
                // Log but don't crash if one file fails to parse
                android.util.Log.w("ProofAssetLoader", "Failed to load $fileName: ${e.message}")
            }
        }
        // De-duplicate by ID
        allTheorems.associateBy { it.id }.values.toList()
    }

    /**
     * Load theorems from a single JSON file.
     */
    suspend fun loadFile(assetPath: String): List<TheoremDefinition> = withContext(Dispatchers.IO) {
        val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        json.decodeFromString<List<TheoremDefinition>>(text)
    }

    /**
     * Load a single theorem by ID by scanning all files.
     * Prefer using the Room cache instead for repeated lookups.
     */
    suspend fun loadById(id: String): TheoremDefinition? =
        loadAllTheorems().find { it.id == id }
}
