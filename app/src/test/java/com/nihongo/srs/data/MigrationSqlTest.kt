package com.nihongo.srs.data

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The hand-written migration SQL must match Room's generated schema (schemas/.../3.json). */
class MigrationSqlTest {
    @Test
    fun migrationMatchesExportedSchema() {
        val schema = File("schemas/com.nihongo.srs.data.AppDatabase/3.json").readText()
        listOf(
            MigrationSql.CREATE_STUDY_DAY.replace("`study_day`", "`\${TABLE_NAME}`"),
            MigrationSql.CREATE_EXAM_RESULT.replace("`exam_result`", "`\${TABLE_NAME}`"),
        ).forEach { sql -> assertTrue("Schema mismatch for:\n$sql", schema.contains(sql)) }
    }
}
