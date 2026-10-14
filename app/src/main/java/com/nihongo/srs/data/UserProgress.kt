package com.nihongo.srs.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Spaced-repetition progress for one vocab item.
 *
 * A row exists only after the user has *learned* the word (Day 0).
 * Words with no row are "new" and appear in the Learn queue.
 *
 * Timestamps are epoch millis. `nextReview` is always the START of the due day
 * (local midnight), so "due today" simply means `nextReview <= end of today`.
 */
@Entity(
    tableName = "user_progress",
    foreignKeys = [
        ForeignKey(
            entity = VocabEntity::class,
            parentColumns = ["id"],
            childColumns = ["vocabId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("nextReview")]
)
data class UserProgress(
    @PrimaryKey
    val vocabId: Long,
    val stage: Int,
    val lastReviewed: Long,
    val nextReview: Long,
    val isMastered: Boolean,
)
