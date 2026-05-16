package com.statproof.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * Cached theorem metadata stored in Room.
 *
 * The full [TheoremDefinition] (with all derivation steps) is loaded from
 * JSON assets on demand. This entity caches only the fields needed for
 * browsing and search without loading the full derivation.
 */
@Entity(tableName = "theorems")
data class TheoremEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "topic")
    val topic: String,                      // Topic enum name

    @ColumnInfo(name = "subtopic")
    val subtopic: String,

    @ColumnInfo(name = "difficulty")
    val difficulty: String,                 // Difficulty enum name

    @ColumnInfo(name = "tags")
    val tags: String,                       // JSON array string

    @ColumnInfo(name = "statement_latex")
    val statementLatex: String,

    @ColumnInfo(name = "conclusion_latex")
    val conclusionLatex: String,

    @ColumnInfo(name = "keywords")
    val keywords: String,                   // JSON array string

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "view_count")
    val viewCount: Int = 0,

    @ColumnInfo(name = "last_viewed_at")
    val lastViewedAt: Long = 0L,            // Unix timestamp ms
)

/**
 * Full-text search index table (Room FTS4).
 *
 * FTS4 provides efficient full-text search over theorem content.
 * This table mirrors [TheoremEntity] fields relevant to search.
 */
@Fts4(contentEntity = TheoremEntity::class)
@Entity(tableName = "theorems_fts")
data class TheoremFtsEntity(
    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "subtopic")
    val subtopic: String,

    @ColumnInfo(name = "tags")
    val tags: String,

    @ColumnInfo(name = "keywords")
    val keywords: String,

    @ColumnInfo(name = "statement_latex")
    val statementLatex: String,
)

/**
 * User favorites junction table.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    @ColumnInfo(name = "theorem_id")
    val theoremId: String,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis(),
)

/**
 * Recently viewed proofs (ring buffer of last 50).
 */
@Entity(tableName = "recent_history")
data class RecentHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "theorem_id")
    val theoremId: String,

    @ColumnInfo(name = "viewed_at")
    val viewedAt: Long = System.currentTimeMillis(),
)
