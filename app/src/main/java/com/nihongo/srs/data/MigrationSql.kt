package com.nihongo.srs.data

/**
 * SQL used by AppDatabase.MIGRATION_2_3. It must match what Room generates for
 * the entities exactly; MigrationSqlTest checks it against the exported schema.
 */
object MigrationSql {
    const val CREATE_STUDY_DAY =
        "CREATE TABLE IF NOT EXISTS `study_day` (" +
            "`date` TEXT NOT NULL, " +
            "`reviewed` INTEGER NOT NULL DEFAULT 0, " +
            "`reviewedCorrect` INTEGER NOT NULL DEFAULT 0, " +
            "`learned` INTEGER NOT NULL DEFAULT 0, " +
            "`exams` INTEGER NOT NULL DEFAULT 0, " +
            "`bestExamPercent` INTEGER NOT NULL DEFAULT -1, " +
            "`spoken` INTEGER NOT NULL DEFAULT 0, " +
            "`kanaQuizzes` INTEGER NOT NULL DEFAULT 0, " +
            "PRIMARY KEY(`date`))"

    const val CREATE_EXAM_RESULT =
        "CREATE TABLE IF NOT EXISTS `exam_result` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`timestamp` INTEGER NOT NULL, " +
            "`scope` TEXT NOT NULL, " +
            "`percent` INTEGER NOT NULL, " +
            "`total` INTEGER NOT NULL)"
}
