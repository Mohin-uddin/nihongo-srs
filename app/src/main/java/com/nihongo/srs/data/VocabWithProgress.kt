package com.nihongo.srs.data

import androidx.room.Embedded
import androidx.room.Relation

/** A vocab item together with its (optional) SRS progress row. */
data class VocabWithProgress(
    @Embedded val vocab: VocabEntity,
    @Relation(parentColumn = "id", entityColumn = "vocabId")
    val progress: UserProgress?,
)
